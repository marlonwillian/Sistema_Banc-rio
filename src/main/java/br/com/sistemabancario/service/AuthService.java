package br.com.sistemabancario.service;

import br.com.sistemabancario.domain.Cliente;
import br.com.sistemabancario.domain.Conta;
import br.com.sistemabancario.domain.Perfil;
import br.com.sistemabancario.domain.Usuario;
import br.com.sistemabancario.persistence.ContaRepository;
import br.com.sistemabancario.persistence.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

public final class AuthService {
    private static final String ARQUIVO_CONFIGURACAO = "/config.properties";

    private final UsuarioRepository usuarioRepository;
    private final ContaRepository contaRepository;

    public AuthService(UsuarioRepository usuarioRepository, ContaRepository contaRepository) {
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository, "O repositorio de usuarios e obrigatorio.");
        this.contaRepository = Objects.requireNonNull(contaRepository, "O repositorio de contas e obrigatorio.");
    }

    public Usuario autenticar(String login, char[] senha) {
        if (senha == null) {
            throw new IllegalArgumentException("Informe a senha para continuar.");
        }

        try {
            String loginNormalizado = normalizarLogin(login);
            if (senha.length == 0) {
                throw new IllegalArgumentException("Informe a senha para continuar.");
            }
            Optional<Usuario> usuario = usuarioRepository.buscarPorLogin(loginNormalizado);
            if (usuario.isEmpty()) {
                throw new SecurityException("Login ou senha invalidos.");
            }

            String senhaTexto = new String(senha);
            Usuario usuarioAutenticado = usuario.get();
            if (!BCrypt.checkpw(senhaTexto, usuarioAutenticado.senhaHash())) {
                throw new SecurityException("Login ou senha invalidos.");
            }

            SessaoUsuario.abrir(usuarioAutenticado);
            return usuarioAutenticado;
        } finally {
            Arrays.fill(senha, '\0');
        }
    }

    public Usuario cadastrarUsuario(String login, char[] senha, String nomeTitular) {
        if (senha == null) {
            throw new IllegalArgumentException("Informe uma senha.");
        }

        try {
            String loginNormalizado = normalizarLogin(login);
            String titularNormalizado = normalizarTitular(nomeTitular);
            if (loginNormalizado.length() > 80) {
                throw new IllegalArgumentException("O login pode ter no maximo 80 caracteres.");
            }
            if (titularNormalizado.length() > 160) {
                throw new IllegalArgumentException("O nome pode ter no maximo 160 caracteres.");
            }
            if (senha.length < 6) {
                throw new IllegalArgumentException("A senha deve ter pelo menos 6 caracteres.");
            }
            if (usuarioRepository.buscarPorLogin(loginNormalizado).isPresent()) {
                throw new IllegalArgumentException("Esse login ja esta cadastrado.");
            }

            String senhaTexto = new String(senha);
            long contaId = Math.incrementExact(contaRepository.listarTodas().stream()
                    .mapToLong(Conta::getId)
                    .max()
                    .orElse(0));
            Conta conta = new Conta(contaId, new Cliente(contaId, titularNormalizado), BigDecimal.ZERO);
            Usuario usuario = new Usuario(
                    null, loginNormalizado, BCrypt.hashpw(senhaTexto, BCrypt.gensalt()), Perfil.USUARIO, contaId);
            contaRepository.salvar(conta);

            try {
                usuarioRepository.salvar(usuario);
            } catch (RuntimeException exception) {
                try {
                    contaRepository.excluir(contaId);
                } catch (RuntimeException falhaExclusao) {
                    exception.addSuppressed(falhaExclusao);
                }
                throw exception;
            }
            return usuario;
        } finally {
            Arrays.fill(senha, '\0');
        }
    }

    public Usuario garantirAdminPadrao() {
        String login = configurar("sistema.admin.login", "admin");
        String senha = configurar("sistema.admin.senha", "admin123");
        return usuarioRepository.buscarPorLogin(login)
                .orElseGet(() -> {
                    Usuario admin = new Usuario(null, login, BCrypt.hashpw(senha, BCrypt.gensalt()), Perfil.ADMIN, null);
                    usuarioRepository.salvar(admin);
                    return admin;
                });
    }

    public int maxTentativas() {
        return Integer.parseInt(configurar("sistema.auth.maxTentativas", "3"));
    }

    private static String normalizarLogin(String login) {
        if (login == null) {
            throw new IllegalArgumentException("O login e obrigatorio.");
        }
        String valor = login.trim();
        if (valor.isBlank()) {
            throw new IllegalArgumentException("Informe o login.");
        }
        return valor;
    }

    private static String normalizarTitular(String titular) {
        if (titular == null || titular.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do titular.");
        }
        return titular.trim();
    }

    private static String configurar(String chave, String padrao) {
        Properties propriedades = new Properties();
        try (InputStream entrada = AuthService.class.getResourceAsStream(ARQUIVO_CONFIGURACAO)) {
            if (entrada != null) {
                propriedades.load(new java.io.InputStreamReader(entrada, StandardCharsets.UTF_8));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel ler a configuracao do sistema.", exception);
        }

        String valor = System.getProperty(chave);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(chave.replace('.', '_').toUpperCase());
        }
        if (valor == null || valor.isBlank()) {
            valor = propriedades.getProperty(chave, padrao);
        }
        return valor == null || valor.isBlank() ? padrao : valor;
    }
}
