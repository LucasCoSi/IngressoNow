# IngressoNow — Refatorações Registradas (ROO-12)

Quatro refatorações motivadas (R-01/R-02 na migração Java SE/Task #4;
R-03/R-04 no endurecimento D-05/ROO-02/ROO-05).
Nenhuma muda comportamento observável além do exigido pelo documento — só estrutura
(R-04 fecha a validade exigida em D-05/RNF-11).

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

## R-03: Validação polimórfica em `TipoPergunta` (ROO-05)

**Motivo:** `Pergunta.validarResposta` usava `switch (tipo)` — variação
por condicional espalhada, contra §8.5 (questionários: "validação
polimórfica de respostas") e o comentário que já prometia polimorfismo.

**Mudança:** cada constante de `TipoPergunta` implementa
`validar(enunciado, obrigatoria, opcoes, escalaMax, valor)`; `Pergunta`
só delega (`tipo.validar(...)`), mantendo a checagem de obrigatoriedade.

**Efeito:** novo tipo de pergunta = nova constante, sem tocar
`Pergunta`/use cases; coberto por `PerguntaTest` (escala, escolha,
texto + obrigatoriedade).

Arquivos: `domain/model/entity/TipoPergunta.java`, `domain/model/entity/Pergunta.java`.

## R-04: QR com validade + anti-duplicidade (D-05/RNF-11)

**Motivo:** token eterno (`ids+salt`) + `registrarCheckIn` sem guarda =
repetição acidental duplicava presença (viola RNF-11) e vazamento do
token valia para sempre (viola D-05/RF-20). Além disso o `POST /checkin`
exigia organizador, bloqueando o CA-04 (participante registra a própria
presença).

**Mudança:** token `ids:emitidoEm:salt` com `VALIDADE=5min` validada em
`decodificarToken`; `Inscricao` rejeita 2º check-in na política única
("Presença já registrada"); `ControleHandler` autoriza dono OU
organizador no check-in (manual segue só organizador).

**Efeito:** repetição não duplica; expirado/adulterado → 422 com motivo;
CA-04 demonstrável pelo participante. Cobertura: `SimpleQrCodeServiceTest`,
`InscricaoTest` (único × entrada/saída), `RealizarCheckInQrCodeUseCaseTest`.

Arquivos: `application/service/SimpleQrCodeService.java`,
`domain/port/in/DadosQrCode.java`, `domain/model/entity/Inscricao.java`,
`adapters/in/http/ControleHandler.java`.

## Cobertura

32 arquivos / 109 testes JUnit 5 + Mockito (`mvn test`: 0 falhas), incluindo:
- `EditarUsuarioUseCaseTest` (RF-03), `RemarcarAtividadeUseCaseTest` (RF-07),
  `VincularPessoaUseCaseTest` (RF-08), `RealizarCheckInManualUseCaseTest` (RF-22),
  `GerarRelatorioInscritosUseCaseTest` (RF-29),
  `PessoaTest` (RF-08), `CheckInOrigemTest` (RF-22/RN-11/RN-12).
