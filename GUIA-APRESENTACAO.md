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

## 2. Roteiro sugerido para a demonstração

1. Inicie a aplicação com `mvn exec:java`. A janela Swing apresenta as contas
   de demonstração na primeira execução; as contas e operações ficam no banco
   H2 local em `data/sistema-bancario.mv.db`.
2. Selecione uma conta e mostre o saldo e o extrato. Faça um depósito, depois
   um saque válido e uma transferência para outra conta. Mostre que o saldo e
   o extrato refletem cada operação.
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
7. Feche e abra a aplicação novamente para mostrar que os dados salvos no H2
   continuam disponíveis.

Para executar a bateria automatizada de verificações, use `mvn test`; para
compilar, testar e empacotar, use `mvn clean package`.

## 3. Como explicar as camadas

| Camada | Responsabilidade |
|---|---|
| `domain` | Regras de negócio e entidades `Cliente`, `Conta` e `Transacao`, sem dependência de JDBC ou arquivos. |
| `service` | `BancoService` coordena as operações entre domínio e repositórios. |
| `persistence` | Interfaces `ContaRepository` e `TransacaoRepository` e suas implementações em memória e JDBC. |
| `simulacao` | `OperacaoBancariaThread` implementa `Runnable`; `SimulacaoRunner` inicia as tarefas e confere o resultado. |
| `simulacao.demo` | Implementação deliberadamente insegura, isolada e usada somente para explicar a corrida. |
| `io` | `ExtratoExporter` grava transações recebidas em arquivo UTF-8. |
| `ui` | A janela Swing coleta entradas e apresenta saldos, operações, simulações e extratos; delega as regras ao serviço. |
| `app` | `Main` prepara o banco, compõe os repositórios e abre a interface Swing. |

O script `src/main/resources/schema.sql` cria as tabelas `contas` e
`transacoes` e o índice usado nas consultas de extrato.

## 4. Concorrência, saldo e transferências

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

## 5. Persistência e extrato

`JdbcContaRepository` e `JdbcTransacaoRepository` isolam o SQL do restante do
sistema. As consultas usam `PreparedStatement`, e cada operação abre e fecha
sua própria conexão. A gravação JDBC da operação e suas transações usa uma
transação de banco: os saldos afetados e os registros da operação são
confirmados juntos. Por isso, o serviço não repete a atualização de saldo
depois do commit JDBC. Uma transferência grava seus dois registros em conjunto.
Na inicialização, `ConnectionFactory` executa `schema.sql`. O padrão é o banco
H2 em arquivo; os testes usam bases H2 em memória isoladas.

O extrato consultado pela interface vem do repositório de transações. A
exportação é feita por `ExtratoExporter`, com codificação UTF-8 e fechamento
do arquivo por `try-with-resources`; a classe não acessa banco de dados.

## 6. Simulação e validação do resultado

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

## 7. Requisitos e evidências no projeto

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

## 8. Perguntas prováveis

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

**O que foi adicionado além do núcleo acadêmico?**  
A interface Swing estilizada com FlatLaf, cadastro de contas pela janela, H2
local para simplificar a execução, repositórios em memória para testes e uma
demonstração reproduzível da corrida. Esses itens apoiam o uso e a apresentação;
não substituem os requisitos obrigatórios.

## 9. Arquivos para consultar durante a apresentação

- `Especificacao_6_Sistema_Bancario_Concorrente.pdf`: enunciado da atividade.
- `AGENT.md`: regras detalhadas de arquitetura e concorrência do projeto.
- `README.md`: compilação, execução e uso da interface.
- `RELATORIO-RACE-CONDITION.md`: comparação antes/depois.
- `src/main/resources/schema.sql`: estrutura do banco.
- `docs/extrato-exemplo.txt`: formato de extrato exportado.
