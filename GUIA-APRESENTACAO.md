# Guia de apresentação — Sistema Bancário Concorrente

Este guia serve como roteiro para demonstrar o sistema e explicar suas
principais decisões técnicas. O enunciado da disciplina e o `AGENT.md` são as
referências dos requisitos; funcionalidades visuais e conveniências adicionais
estão identificadas como extras.

## 1. Objetivo do trabalho

O sistema modela contas bancárias que recebem depósitos, saques e
transferências, inclusive quando várias threads tentam operar sobre as mesmas
contas ao mesmo tempo. O desafio central é evitar que uma atualização de saldo
se perca por uma *race condition*, impedir saldo negativo e evitar deadlock em
transferências simultâneas. As contas e movimentações também são persistidas
com JDBC, e o extrato pode ser exportado para texto.

## 2. Preparar e executar em outro computador

### Pré-requisitos

- JDK 17 ou superior e Maven 3.9 ou superior.
- MySQL Server instalado e iniciado; o Workbench é opcional.
- Ambiente com interface gráfica para a aplicação Swing.
- Internet na primeira compilação, para o Maven baixar as dependências.
- Ollama e o modelo `qwen2.5` somente se for demonstrar o assistente IA.

Clone o repositório e entre na pasta que contém `pom.xml`:

```powershell
git clone https://github.com/marlonwillian/Sistema_Banc-rio.git
Set-Location .\Sistema_Banc-rio
java -version
mvn -version
```

No Workbench, conectado ao MySQL Server, crie os bancos uma vez:

```sql
CREATE DATABASE sistema_bancario CHARACTER SET utf8mb4;
CREATE DATABASE sistema_bancario_test CHARACTER SET utf8mb4;
```

`sistema_bancario` guarda os dados da aplicação. `sistema_bancario_test` é
descartável: os testes JDBC recriam suas tabelas. **Nunca aponte
`MYSQL_TEST_URL` para o banco da apresentação ou para dados que queira manter.**
O usuário MySQL precisa ter permissão de criar e alterar tabelas nos dois bancos.

### Executar testes e abrir o sistema em uma sequência

No VS Code, abra o terminal integrado com **Ctrl+Shift+`** (não use o botão
“Run Code”), entre na raiz do projeto e cole o bloco. Ele solicita a senha de
forma oculta, executa a suíte e abre a aplicação somente se os testes passarem.
Deixe a janela Maven aberta enquanto apresenta; feche a janela do sistema para
encerrar.

```powershell
$env:MYSQL_TEST_URL = "jdbc:mysql://localhost:3306/sistema_bancario_test?serverTimezone=UTC"
$env:SISTEMA_BANCO_URL = "jdbc:mysql://localhost:3306/sistema_bancario?serverTimezone=UTC"
$usuarioMySql = Read-Host "Usuário MySQL"
$env:MYSQL_TEST_USER = $usuarioMySql
$env:SISTEMA_BANCO_USUARIO = $usuarioMySql
$senha = Read-Host "Senha MySQL" -AsSecureString
$ponteiro = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($senha)

try {
    $senhaTexto = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ponteiro)
    $env:MYSQL_TEST_PASSWORD = $senhaTexto
    $env:SISTEMA_BANCO_SENHA = $senhaTexto
    mvn clean test exec:java
    $resultado = $LASTEXITCODE
} finally {
    Remove-Item Env:MYSQL_TEST_URL, Env:MYSQL_TEST_USER, Env:MYSQL_TEST_PASSWORD,
        Env:SISTEMA_BANCO_URL, Env:SISTEMA_BANCO_USUARIO, Env:SISTEMA_BANCO_SENHA `
        -ErrorAction SilentlyContinue
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ponteiro)
    $senha.Dispose()
    Remove-Variable senhaTexto -ErrorAction SilentlyContinue
}

if ($resultado -eq 0) {
    Write-Host "Execução concluída."
} else {
    Write-Host "O Maven encerrou com código $resultado. Leia as mensagens [ERROR] acima."
}
Read-Host "Pressione Enter para voltar ao PowerShell"
```

Para apenas iniciar o sistema, sem rodar testes, use as variáveis de aplicação
e `mvn clean compile exec:java`. Por exemplo, depois de preencher credenciais
válidas:

```powershell
$env:SISTEMA_BANCO_URL = "jdbc:mysql://localhost:3306/sistema_bancario?serverTimezone=UTC"
$env:SISTEMA_BANCO_USUARIO = "root"
$env:SISTEMA_BANCO_SENHA = Read-Host "Senha MySQL"
mvn clean compile exec:java
```

Esse exemplo simples deixa a senha visível enquanto é digitada; prefira o
bloco seguro acima. Também é possível configurar a conexão por propriedades
Java `sistema.banco.url`, `sistema.banco.usuario` e `sistema.banco.senha`.

Na primeira inicialização, `Main` executa `schema.sql` e
`migration-usuarios.sql`. Se ainda não houver contas, cria as contas de
demonstração. As contas e operações permanecem no banco quando o sistema fecha.

### Login, cadastro e perfis

- Em um banco novo, o administrador padrão é `admin` / `admin123`, definidos
  inicialmente em `src/main/resources/config.properties`. Ele só é criado se
  ainda não existir; alterar a configuração não troca a senha de um admin já
  gravado no banco.
- Para criar um acesso comum, clique em **Criar conta** na janela de login e
  informe nome, login e senha (mínimo de seis caracteres). O sistema cria uma
  conta bancária vinculada, com saldo inicial zero. Depois, entre usando o
  login e a senha cadastrados; faça um depósito para testar operações e IA.
  Usuários comuns só consultam e movimentam a própria conta; a camada de
  serviço também bloqueia consultas e operações sobre contas alheias. Para
  transferir, informe o número da conta destino: o débito sempre parte da
  conta autenticada. Depósito credita a própria conta, sem debitar outra.
  Criação de contas e simulação ficam restritas ao administrador.
- As senhas são armazenadas como hash BCrypt. O usuário comum só pode consultar
  e operar a conta vinculada; o serviço também valida a permissão antes das
  operações. O login permite três tentativas. **Sair** encerra a sessão e volta
  à tela de login.
- O administrador pode consultar as contas e movimentações disponíveis e
  operar em qualquer conta. A tabela administrativa exibe conta, titular e
  saldo. O modelo atual não possui campos de tipo ou status da conta.
- A tela mantém rolagem vertical e posiciona o aviso e os botões de operação em
  linhas separadas para evitar que os controles sejam cortados quando a janela
  fica mais estreita.

### Assistente IA (opcional)

O assistente conversa com o modelo Qwen localmente; as operações bancárias não
dependem dele. No Windows, baixe e execute o instalador oficial em
[ollama.com/download/windows](https://ollama.com/download/windows). Depois
feche e reabra o PowerShell/VS Code para atualizar o `PATH`. Confirme a
instalação e abra o aplicativo Ollama pelo menu Iniciar:

```powershell
ollama --version
```

Com o serviço ativo, baixe e confira o modelo:

```powershell
ollama pull qwen2.5
ollama list
```

Se o aplicativo não iniciar o servidor automaticamente, execute `ollama serve`
em outro terminal e mantenha-o aberto. Se a porta já estiver em uso, o serviço
provavelmente já está ativo. Confira o endpoint local:

```powershell
Invoke-RestMethod http://localhost:11434/api/tags
```

Depois abra o sistema e acesse **Assistente IA**. Faça uma movimentação antes
de perguntar; sem transações no histórico, não há dados para analisar. O usuário
comum envia apenas seu extrato; o administrador envia o histórico das contas.
A interface mostra o estado da consulta e não congela enquanto espera. A
integração envia até 25 registros por pergunta.

As configurações padrão estão em `src/main/resources/config.properties`:
endpoint `http://localhost:11434/api/chat`, modelo `qwen2.5` e timeout de 30
segundos. Podem ser sobrescritas por propriedades Java ou variáveis de ambiente
`SISTEMA_IA_URL`, `SISTEMA_IA_MODELO` e `SISTEMA_IA_TIMEOUTSEGUNDOS`.
Se não conectar, confirme que o serviço está ativo, que `ollama list` mostra
`qwen2.5` e que o endpoint local responde. Sem Ollama, o restante do sistema
continua funcionando; apenas o chat fica indisponível.

Se o terminal do VS Code mostrar apenas que o processo terminou com código 1,
isso não informa a causa. Leia as mensagens anteriores no terminal e procure
`[ERROR]`; falha Maven/teste pode impedir a aplicação de abrir, enquanto a falta
do comando Ollama afeta somente a IA. Use o terminal integrado e mantenha a
pausa final do bloco para ler a saída.

## 3. Roteiro sugerido para a demonstração

1. Inicie o sistema e entre como `admin` ou cadastre um usuário comum. Mostre
   que o usuário comum tem uma conta própria, enquanto o administrador consulta
   todas as contas.
2. Selecione uma conta e mostre o saldo e o extrato. Faça um depósito, depois
   um saque válido e uma transferência para outra conta. Como usuário comum,
   digite o número da conta destino; como administrador, selecione-a na lista.
   Mostre que o saldo e o extrato refletem cada operação.
3. Tente sacar mais do que o saldo disponível. A operação deve ser recusada e
   o saldo não deve ficar negativo.
4. Use **Simular**. A aplicação dispara operações bancárias concorrentes em
   quatro threads e apresenta quantidade realizada/recusada, saldos esperados
   e saldos obtidos. A confirmação de sucesso indica que os valores conferem.
5. Use **Race condition** para comparar dois depósitos sem sincronização com
   dois depósitos pela classe `Conta`. O exemplo força as leituras concorrentes
   no caso inseguro e apresenta o resultado comparativo.
6. Use **Exportar extrato** e abra o `.txt` salvo. O arquivo de exemplo do
   projeto está em `docs/extrato-exemplo.txt`.
7. Se Ollama estiver instalado, pergunte ao assistente sobre o histórico. Se
   não estiver, explique que é uma integração local opcional.
8. Use **Sair**, entre com outro perfil se houver e demonstre que a sessão
   retorna ao login. Feche e abra a aplicação novamente para mostrar que os
   dados salvos no MySQL continuam disponíveis.

Para executar só os testes, use `mvn test`. Para compilar, testar e empacotar,
use `mvn clean package`. Os testes de integração JDBC são ignorados quando as
variáveis `MYSQL_TEST_URL`, `MYSQL_TEST_USER` e `MYSQL_TEST_PASSWORD` não estão
configuradas. Na última execução local, `mvn test` reportou 30 testes, 0
falhas e 4 ignorados por não haver configuração JDBC de teste. A falha anterior
dos testes JDBC ao apagar `contas` com a tabela `usuarios` ainda referenciando-a
foi corrigida: a preparação agora remove `transacoes`, `usuarios` e `contas`,
nesta ordem. Essa correção ainda precisa ser confirmada executando a suíte com
as variáveis `MYSQL_TEST_*` apontadas para o banco descartável `_test`.

## 4. Como explicar as camadas

| Camada | Responsabilidade |
|---|---|
| `domain` | Regras de negócio e entidades `Cliente`, `Conta`, `Transacao`, `Usuario` e `Perfil`, sem dependência de JDBC ou arquivos. |
| `service` | `BancoService` coordena operações e permissões; `AuthService` trata login, cadastro e BCrypt; `AssistenteIAService` prepara perguntas e chama Ollama. |
| `persistence` | Interfaces `ContaRepository`, `TransacaoRepository` e `UsuarioRepository`, com implementações em memória e JDBC. |
| `simulacao` | `OperacaoBancariaThread` implementa `Runnable`; `SimulacaoRunner` inicia as tarefas e confere o resultado. |
| `simulacao.demo` | Implementação deliberadamente insegura, isolada e usada somente para explicar a corrida. |
| `io` | `ExtratoExporter` grava transações recebidas em arquivo UTF-8. |
| `ui` | A interface Swing inclui login/cadastro, painel bancário e chat IA; coleta entradas e delega regras aos serviços. |
| `app` | `Main` inicializa schema/migração, compõe repositórios e serviços e abre o login. |

`src/main/resources/schema.sql` cria as tabelas de contas, usuários e
transações; `migration-usuarios.sql` também garante a tabela de usuários em
bancos já existentes. As consultas JDBC usam `PreparedStatement`.

## 5. Concorrência, saldo e transferências

- `Conta` mantém o saldo privado e usa `BigDecimal`, sem arredondar valores
  inválidos: operações precisam ser positivas e ter no máximo duas casas
  decimais.
- Depósitos e saques sincronizam a alteração do saldo e a inclusão da
  transação no extrato no mesmo monitor. Um saque valida o saldo dentro da
  seção sincronizada.
- `transferirPara` precisa bloquear duas contas. Para evitar deadlock, bloqueia
  primeiro a conta de menor ID e depois a de maior ID, independentemente do
  sentido da transferência. Só depois valida e altera os dois saldos e os dois
  extratos.
- O serviço mantém locks por conta durante a operação e a persistência; numa
  transferência, adquire os locks pela mesma ordem crescente de ID. A
  infraestrutura não é chamada enquanto os monitores internos de `Conta`
  estão retidos.
- Uma transferência gera dois registros: saída na conta de origem e entrada
  na conta de destino. Assim cada extrato mostra a movimentação da perspectiva
  da própria conta.
- `ContaInsegura` demonstra a falha de propósito, mas não é usada pelas
  operações bancárias reais. A `CyclicBarrier` força as duas threads a lerem o
  mesmo saldo antes de escrever, tornando reproduzível a atualização perdida
  sem depender de `Thread.sleep()`.

## 6. Persistência e extrato

`JdbcContaRepository` e `JdbcTransacaoRepository` isolam o SQL do restante do
sistema. As consultas usam `PreparedStatement`, e cada operação abre e fecha
sua própria conexão. A gravação JDBC da operação e suas transações usa uma
transação de banco: os saldos afetados e os registros da operação são
confirmados juntos. Por isso, o serviço não repete a atualização de saldo
depois do commit JDBC. Uma transferência grava seus dois registros em conjunto.
Na inicialização, `ConnectionFactory` executa `schema.sql` e
`migration-usuarios.sql`. A aplicação usa o MySQL local por padrão e permite
configurar URL, usuário e senha por propriedades Java ou variáveis de ambiente.
Os testes JDBC usam um banco MySQL separado e descartável.

O extrato consultado pela interface vem do repositório de transações. A
exportação é feita por `ExtratoExporter`, com codificação UTF-8 e fechamento
do arquivo por `try-with-resources`; a classe não acessa banco de dados.

## 7. Simulação e validação do resultado

`SimulacaoRunner` cria operações e as distribui entre tarefas
`OperacaoBancariaThread`. As threads aguardam uma barreira de início para
concorrer de fato. Falhas de saldo insuficiente são contadas como operações
recusadas, sem interromper as demais. Falhas inesperadas são propagadas como
erro da simulação.

Ao final, o runner calcula os saldos esperados a partir dos saldos iniciais e
das operações que terminaram com sucesso, consulta os saldos finais e os
compara por conta. O resultado é válido quando os mapas de saldos esperados e
obtidos são iguais. Os testes também exercitam depósitos concorrentes,
transferências em sentidos opostos, persistência JDBC e exportação.

## 8. Requisitos e evidências no projeto

| Requisito do enunciado | Evidência |
|---|---|
| `Conta`, `Cliente` e `Transacao` | `domain/Conta.java`, `domain/Cliente.java`, `domain/Transacao.java` |
| Saldo inicial e operações bancárias com validação | Construtor e métodos de `Conta`; orquestração em `BancoService` |
| Saldo protegido e sincronização contra corrida | Métodos sincronizados de depósito/saque e seção crítica de `Conta.transferirPara` |
| Transferência sem deadlock | Ordem crescente de ID em `Conta.transferirPara` e nos locks do serviço |
| Threads concorrentes e `Runnable` | `simulacao/OperacaoBancariaThread.java` e `SimulacaoRunner.java` |
| Saldo esperado igual ao obtido | `ResultadoSimulacao.saldosCorretos()` e testes de simulação |
| Race condition antes/depois | `simulacao/demo` e `RELATORIO-RACE-CONDITION.md`; demonstração acessível pela interface |
| Persistência JDBC de contas e transações | `persistence/jdbc` e `src/main/resources/schema.sql` |
| Extrato persistido e exportável | Consulta pelo `BancoService`, `io/ExtratoExporter.java` e `docs/extrato-exemplo.txt` |
| Pelo menos cinco testes JUnit, incluindo concorrência | `src/test/java`; executar `mvn test` para validar o estado atual |
| Organização em pacotes | `domain`, `persistence`, `simulacao`, `app`, com `service`, `io` e `ui` como camadas complementares |
| Cadastro/autenticação e acesso por perfil | `ui/LoginDialog.java`, `ui/CadastroUsuarioDialog.java`, `service/AuthService.java`, `service/BancoService.java` |
| Hash de senha e armazenamento JDBC de usuário | BCrypt em `AuthService`, `persistence/jdbc/JdbcUsuarioRepository.java` e tabela `usuarios` |
| Assistente IA sem bloquear Swing | `service/AssistenteIAService.java`, `ui/AssistenteIADialog.java` e teste HTTP simulado |

## 9. Perguntas prováveis

**O que é uma race condition neste sistema?**  
É quando duas threads leem o mesmo saldo e calculam novos valores a partir
dele. Se ambas escrevem sem sincronização, a segunda gravação pode apagar o
efeito da primeira. O relatório e a demonstração visual mostram esse caso.

**Por que usar `BigDecimal` em vez de `double`?**  
`BigDecimal` representa valores monetários decimais sem os erros de precisão
binária comuns em `double`. A validação também restringe valores a duas casas
decimais.

**Por que a ordem dos locks evita deadlock?**  
Uma transferência entre A e B sempre bloqueia primeiro a conta de menor ID,
mesmo quando a direção da transferência é invertida. Assim, as threads não
formam um ciclo de espera pelos dois locks.

**Por que uma transferência tem dois registros?**  
Ela afeta duas contas. Um registro representa o débito da origem e outro o
crédito do destino, permitindo que cada extrato seja consultado isoladamente.

**Como se verifica o saldo esperado da simulação?**  
O runner parte do saldo inicial de cada conta, aplica as variações das
operações concluídas e compara esse resultado com os saldos finais do serviço.
Operações recusadas por saldo insuficiente não entram no cálculo.

**O sistema inseguro pode afetar uma conta real?**  
Não. `ContaInsegura` pertence ao pacote de demonstração e só é invocada pelo
fluxo específico de comparação. O serviço bancário usa `Conta`.

**Como funciona o cadastro comum?**
O cadastro cria um usuário de perfil `USUARIO`, associa-o a uma nova conta com
saldo zero e grava a senha como hash BCrypt. O serviço valida o vínculo da
sessão antes de permitir consultar ou operar outras contas.

**Por que não consigo entrar na IA ou ela não responde?**
O chat depende do Ollama local e do modelo `qwen2.5`. Confirme que o Ollama está
ativo, que `ollama list` mostra o modelo e que existe ao menos uma transação no
histórico. O teste automatizado usa um servidor HTTP local simulado; ele valida
o formato da chamada, mas não substitui a execução real do Qwen.

**O sistema precisa de IA para fazer operações?**
Não. Ollama é uma integração opcional. Login, contas, depósito, saque,
transferência, simulação e extrato funcionam independentemente do chat.

**Quantos testes passaram?**
Na última execução local, `mvn test` concluiu com 30 testes e 0 falhas; 4 testes
JDBC foram ignorados porque as variáveis `MYSQL_TEST_*` não estavam disponíveis
nesse ambiente. A ordem de remoção das tabelas foi corrigida para resolver a
falha de chave estrangeira encontrada anteriormente, mas a execução JDBC com
MySQL configurado ainda precisa ser repetida.

**O que foi adicionado além do núcleo acadêmico?**  
A interface Swing estilizada com FlatLaf, cadastro e login por perfil,
integração opcional com Ollama/Qwen, configuração do MySQL por ambiente,
repositórios em memória para testes e demonstração reproduzível da corrida.
Esses itens complementam o núcleo acadêmico.

## 10. Arquivos para consultar durante a apresentação

- `Especificacao_6_Sistema_Bancario_Concorrente.pdf`: enunciado da atividade.
- `AGENT.md`: regras detalhadas de arquitetura e concorrência do projeto.
- `README.md`: compilação, execução e uso da interface.
- `src/main/resources/config.properties`: credenciais iniciais do admin e configurações do Ollama.
- `src/main/resources/migration-usuarios.sql`: migração da tabela de usuários.
- `RELATORIO-RACE-CONDITION.md`: comparação antes/depois.
- `src/main/resources/schema.sql`: estrutura do banco.
- `docs/extrato-exemplo.txt`: formato de extrato exportado.
