package br.com.sistemabancario.service;

import br.com.sistemabancario.domain.Cliente;
import br.com.sistemabancario.domain.Conta;
import br.com.sistemabancario.domain.Perfil;
import br.com.sistemabancario.domain.Usuario;
import br.com.sistemabancario.persistence.UsuarioRepository;
import br.com.sistemabancario.persistence.ContaRepository;
import br.com.sistemabancario.persistence.memory.InMemoryContaRepository;
import br.com.sistemabancario.persistence.memory.InMemoryTransacaoRepository;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {
    @Test
    void adminPadraoERegistradoNaPrimeiraExecucao() {
        UsuarioRepository repositorio = new InMemoryUsuarioRepository();
        AuthService authService = new AuthService(repositorio, new InMemoryContaRepository());

        Usuario admin = authService.garantirAdminPadrao();

        assertEquals(Perfil.ADMIN, admin.perfil());
        assertTrue(repositorio.buscarPorLogin("admin").isPresent());
        SessaoUsuario.fechar();
    }

    @Test
    void usuarioComumNaoPodeAcessarOutraConta() {
        UsuarioRepository repositorio = new InMemoryUsuarioRepository();
        String senha = "123456";
        Usuario usuario = new Usuario(10L, "ana", BCrypt.hashpw(senha, BCrypt.gensalt()), Perfil.USUARIO, 1L);
        repositorio.salvar(usuario);

        AuthService authService = new AuthService(repositorio, new InMemoryContaRepository());
        authService.autenticar("ana", senha.toCharArray());

        Conta contaPropria = new Conta(1, new Cliente(1L, "Ana"), new BigDecimal("100.00"));
        Conta contaAlheia = new Conta(2, new Cliente(2L, "Bruno"), new BigDecimal("200.00"));
        BancoService service = new BancoService(
                List.of(contaPropria, contaAlheia),
                new InMemoryTransacaoRepository());

        assertEquals(List.of(1L), service.listarContas().stream().map(Conta::getId).toList());
        assertThrows(SecurityException.class, () -> service.buscarContaPorId(2));
        assertThrows(SecurityException.class,
                () -> service.criarConta(3, new Cliente(3L, "Carlos"), BigDecimal.ZERO));
        assertThrows(SecurityException.class, () -> service.depositar(2, new BigDecimal("15.00")));

        service.transferir(1, 2, new BigDecimal("15.00"));
        assertThrows(SecurityException.class,
                () -> service.transferir(2, 1, new BigDecimal("10.00")));
        SessaoUsuario.fechar();
        assertEquals(new BigDecimal("85.00"), service.buscarContaPorId(1).orElseThrow().getSaldo());
        assertEquals(new BigDecimal("215.00"), service.buscarContaPorId(2).orElseThrow().getSaldo());
    }

    @Test
    void cadastroCriaUsuarioComumVinculadoAContaPropria() {
        UsuarioRepository usuarios = new InMemoryUsuarioRepository();
        ContaRepository contas = new InMemoryContaRepository();
        contas.salvar(new Conta(1, new Cliente(1L, "Conta existente"), new BigDecimal("100.00")));
        AuthService authService = new AuthService(usuarios, contas);
        char[] senha = "senha123".toCharArray();

        Usuario usuario = authService.cadastrarUsuario("ana", senha, "Ana Silva");

        assertEquals(Perfil.USUARIO, usuario.perfil());
        assertEquals(2L, usuario.contaId());
        assertTrue(java.util.Arrays.equals(new char[senha.length], senha));
        assertTrue(BCrypt.checkpw("senha123", usuario.senhaHash()));
        assertEquals(2, contas.listarTodas().size());

        authService.autenticar("ana", "senha123".toCharArray());
        BancoService bancoService = new BancoService(contas, new InMemoryTransacaoRepository());
        bancoService.depositar(usuario.contaId(), new BigDecimal("10.00"));
        assertThrows(SecurityException.class,
                () -> bancoService.depositar(1, new BigDecimal("10.00")));
        SessaoUsuario.fechar();
    }

    @Test
    void cadastroRecusaLoginDuplicadoSemCriarContaExtra() {
        UsuarioRepository usuarios = new InMemoryUsuarioRepository();
        ContaRepository contas = new InMemoryContaRepository();
        contas.salvar(new Conta(1, new Cliente(1L, "Conta existente"), BigDecimal.ZERO));
        AuthService authService = new AuthService(usuarios, contas);

        authService.cadastrarUsuario("ana", "senha123".toCharArray(), "Ana");
        assertThrows(IllegalArgumentException.class,
                () -> authService.cadastrarUsuario("ana", "senha456".toCharArray(), "Outra Ana"));

        assertEquals(2, contas.listarTodas().size());
    }

    private static final class InMemoryUsuarioRepository implements UsuarioRepository {
        private final List<Usuario> usuarios = new ArrayList<>();

        @Override
        public void salvar(Usuario usuario) {
            usuarios.add(usuario);
        }

        @Override
        public Optional<Usuario> buscarPorId(long id) {
            return usuarios.stream().filter(usuario -> usuario.id() != null && usuario.id() == id).findFirst();
        }

        @Override
        public Optional<Usuario> buscarPorLogin(String login) {
            return usuarios.stream().filter(usuario -> usuario.login().equalsIgnoreCase(login)).findFirst();
        }

        @Override
        public List<Usuario> listarTodos() {
            return List.copyOf(usuarios);
        }
    }
}
