# Relatório: race condition no saldo

## Objetivo

Demonstrar que duas operações de depósito concorrentes podem perder uma
atualização quando leem e escrevem o saldo sem sincronização, e verificar a
correção na classe `domain.Conta`.

## Procedimento

`simulacao.demo.DemonstracaoRaceCondition` executa dois depósitos de R$ 1,00.
Na demonstração insegura, `ContaInsegura` lê o saldo e aguarda uma `CyclicBarrier`
antes de gravar. A barreira garante que as duas threads leiam R$ 0,00 antes de
qualquer gravação. No caso corrigido, as mesmas duas threads usam `Conta`, cujo
depósito é sincronizado.

## Resultado reproduzível

```text
Depósitos concorrentes: 2 x R$ 1,00
Saldo esperado:          R$ 2,00
Sem sincronização:       R$ 1,00
Com Conta sincronizada:  R$ 2,00
```

## Conclusão

Na versão insegura, ambas as threads calculam R$ 1,00 a partir do mesmo saldo
inicial e uma gravação sobrescreve a outra. Em `Conta`, saldo e extrato são
alterados dentro da seção crítica do mesmo monitor; os depósitos concorrentes
produzem o total correto. `ContaInsegura` fica isolada em
`simulacao.demo` e não participa das operações da aplicação bancária.

O cenário também é verificado por `DemonstracaoRaceConditionTest` e
`ContaConcorrenciaTest`.
