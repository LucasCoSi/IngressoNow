# IngressoNow — Refatorações Registradas (ROO-12)

Duas refatorações motivadas, aplicadas na migração Java SE / Task #4.
Nenhuma muda comportamento observável — só estrutura.

## R-01: Extração de `exigirSemConflito` em `Evento`

**Motivo:** a regra de conflito (RN-07: mesmo local + período sobreposto)
estava inline em `adicionarAtividade`. Ao implementar a remarcação
(RF-07), o mesmo loop seria duplicado em `remarcarAtividade`.

**Mudança:** extraído o método privado
`exigirSemConflito(Atividade candidata, String ignorarId)`, reutilizado
por `adicionarAtividade` (ignorarId = null) e por `remarcarAtividade`
(ignora a própria atividade, simulada antes de mutar).

**Efeito:** regra RN-07 em ponto único; remarcação valida antes de
mutar (período original preservado em caso de 422 — coberto por
`RemarcarAtividadeUseCaseTest.deveBloquearRemarcacaoComConflito`).

Arquivo: `domain/model/entity/Evento.java`.

## R-02: Upsert + DELETE de órfãs nos repositories JDBC

**Motivo:** `JdbcQuestionarioRepository.salvar` e
`JdbcEventoRepository.salvar` faziam `DELETE` geral da tabela filha +
re-INSERT. O DELETE acionava `ON DELETE CASCADE` e apagava
silenciosamente `respostas` (bug real: 0 linhas após submeter) e
ameaçava `checkins` (RN-11).

**Mudança:** trocado por `INSERT ... ON CONFLICT (id) DO UPDATE`
(upsert por ID) + `DELETE ... WHERE id NOT IN (...)` só das órfãs
(removidas do agregado). Mesmo padrão nos dois repositories.

**Efeito:** integridade referencial preservada; pergunta/atividade
removida do agregado continua sendo deletada. Validado em Docker
(respostas persistem, consolidação com `media: 4.0`).

Arquivos: `adapters/out/jdbc/JdbcQuestionarioRepository.java`,
`adapters/out/jdbc/JdbcEventoRepository.java`.

## Cobertura

26 arquivos / 86 métodos JUnit 5 + Mockito (`mvn test`: 0 falhas), incluindo:
- `EditarUsuarioUseCaseTest` (RF-03), `RemarcarAtividadeUseCaseTest` (RF-07),
  `VincularPessoaUseCaseTest` (RF-08), `RealizarCheckInManualUseCaseTest` (RF-22),
  `GerarRelatorioInscritosUseCaseTest` (RF-29),
  `PessoaTest` (RF-08), `CheckInOrigemTest` (RF-22/RN-11/RN-12).
