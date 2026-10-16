# Sistema Bancário com Transações Concorrentes

Aplicação acadêmica em Java 17 com interface desktop Swing para testar contas,
depósitos, saques e transferências. O domínio protege os saldos contra acessos
concorrentes; a persistência JDBC guarda contas e movimentações entre execuções.

## Requisitos para executar

- JDK 17 ou superior;
- Maven 3.9 ou superior;
- ambiente desktop com suporte a Swing.

As dependências FlatLaf, H2 e JUnit são resolvidas pelo Maven.

## Compilar e executar

Na pasta raiz do projeto, no terminal do IntelliJ, Eclipse ou VS Code:

```powershell
mvn clean package
mvn exec:java
```

O primeiro comando compila o projeto e executa os testes. O segundo abre a
janela Swing. Também é possível abrir o projeto no IntelliJ/Eclipse e executar
`br.com.sistemabancario.app.Main`.

Na primeira execução são criadas duas contas de demonstração. A base H2 fica em
`data/sistema-bancario.mv.db`; as contas e transações permanecem salvas quando
a aplicação é fechada. Para iniciar uma base H2 em memória para uma execução
manual, passe outra URL JDBC ao Maven:

```powershell
mvn "-Dsistema.banco.url=jdbc:h2:mem:banco;DB_CLOSE_DELAY=-1" exec:java
```

## Executar os testes

```powershell
mvn test
```

Uma classe de teste:

```powershell
mvn "-Dtest=ContaTest" test
```

Um método específico:

```powershell
mvn "-Dtest=ContaTest#depositarAtualizaSaldoERegistraTransacao" test
```

Os testes de JDBC usam bases H2 isoladas em memória. Os testes de exportação
usam diretórios temporários JUnit.

## Usar a aplicação

- Selecione uma conta no cartão de saldo.
- Use **Nova conta** para cadastrar titular e saldo inicial; a nova conta é
  persistida e fica disponível nos seletores de operações.
- Informe o valor em reais; são aceitos formatos como `25,50` e `25.50`.
- Use **Depositar**, **Sacar** ou **Transferir**. Para transferência, escolha
  também uma conta de destino.
- Consulte as movimentações recentes na tabela de extrato.
- Use **Exportar extrato** para salvar um arquivo `.txt` em UTF-8.
- Use **Simular** para disparar depósitos, saques e transferências em quatro
  threads e conferir saldos calculados versus saldos finais.
- Use **Race condition** na navegação lateral para comparar, de forma
  reproduzível, o resultado inseguro sem sincronização e o saldo da `Conta`
  sincronizada.

## Arquitetura

- `domain`: `Cliente`, `Conta`, `Transacao`, tipos e validações de negócio.
  `Conta` é o único objeto que modifica o saldo; ela protege saldo e extrato
  com o mesmo monitor.
- `persistence`: contratos `ContaRepository` e `TransacaoRepository`, além
  das implementações em memória e JDBC.
- `persistence.jdbc`: `ConnectionFactory`, `JdbcContaRepository` e
  `JdbcTransacaoRepository`. Cada chamada abre e fecha sua própria conexão.
  A gravação de uma movimentação e a atualização atômica do saldo ocorrem em
  transação JDBC; os dois registros de uma transferência são gravados juntos.
- `simulacao`: operações de teste concorrente, `OperacaoBancariaThread`,
  `SimulacaoRunner` e relatório de saldos esperados e obtidos.
- `simulacao.demo`: conta deliberadamente insegura, isolada da conta real,
  utilizada somente para mostrar a perda de atualização.
- `service`: `BancoService` coordena domínio e repositórios. Locks de serviço,
  ordenados por ID em transferências, mantêm a ordem entre a mudança no domínio
  e a persistência sem manter monitores de `Conta` durante JDBC.
- `io`: `ExtratoExporter` grava o extrato recebido em UTF-8 sem acessar banco.
- `ui`: interface Swing; coleta entradas e delega operações ao serviço.
- `app`: `Main` compõe persistência, serviço e janela Swing.
- `src/main/resources/schema.sql`: tabelas de contas/transações e índice de
  consulta de extrato.

## Requisitos entregues

O projeto inclui operações monetárias com `BigDecimal`, validações de saldo e
valor, registros imutáveis, simulação multithread, contagem de saques recusados,
conferência de saldos, proteção contra deadlock, persistência JDBC, exportação
de extrato e testes JUnit de domínio, serviço, concorrência, JDBC e I/O.

O arquivo `RELATORIO-RACE-CONDITION.md` registra a comparação “antes e depois”;
`docs/extrato-exemplo.txt` é um exemplo de extrato exportado.

## Recursos adicionais

- Interface desktop com FlatLaf, painel-resumo, navegação lateral e extrato em
  tabela. O visual é uma implementação original inspirada no
  [FlatLaf Sample UI Design](https://github.com/DJ-Raven/flatlaf-sample-ui-design),
  cuja licença MIT está publicada no repositório de referência.
- Banco H2 em arquivo local, sem precisar instalar ou configurar um servidor
  externo. A conexão pode ser trocada por outra URL JDBC.
- Repositórios em memória para testes rápidos sem banco real.
- Demonstração determinística da race condition: uma barreira força as duas
  threads inseguras a lerem o mesmo saldo antes de gravar, expondo a atualização
  perdida sem depender de `Thread.sleep()`.
- Simulação e demonstração podem ser iniciadas pela própria interface Swing.

## Arquivos importantes

- `AGENT.md`: especificação detalhada e regras arquiteturais do projeto.
- `Especificacao_6_Sistema_Bancario_Concorrente.pdf`: enunciado da atividade.
- `GUIA-APRESENTACAO.md`: roteiro de demonstração, arquitetura e respostas para
  dúvidas comuns.
- `RELATORIO-RACE-CONDITION.md`: evidência da corrida e da correção.
- `docs/extrato-exemplo.txt`: exemplo de extrato no formato exportado.
