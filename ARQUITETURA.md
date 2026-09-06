# IngressoNow — Modelo de Domínio, Responsabilidades e Arquitetura (§11.3)

## 1. Diagrama arquitetural (hexagonal)

```
                    ┌─────────────────────────────┐
                    │        ADAPTERS IN          │
                    │  http/ (REST com.sun.       │
                    │   net.httpserver)           │
                    │  desktop/ (JavaFX)          │
                    └──────────────┬──────────────┘
                                   │ usa
                    ┌──────────────▼──────────────┐
                    │         APPLICATION         │
                    │  20 use cases (orquestram   │
                    │  domínio via portas)        │
                    └──────────────┬──────────────┘
                                   │ depende de
              ┌────────────────────▼────────────────────┐
              │              DOMAIN (puro)              │
              │  entity/  vo/  port/in  port/out        │
              │  exception/                             │
              └────────────────────┬────────────────────┘
                                   │ implementado por
                    ┌──────────────▼──────────────┐
                    │        ADAPTERS OUT         │
                    │  jdbc/ (5 repos + pessoas)  │
                    │  security/ (BCryptHasher)   │
                    └──────────────┬──────────────┘
                                   │ via
                    ┌──────────────▼──────────────┐
                    │            INFRA            │
                    │  AppConfig, Connection-     │
                    │  Factory, FabricaAplicacao  │
                    │  (fiação manual),           │
                    │  MigradorSql + V1–V9        │
                    └─────────────────────────────┘

  Site estático (static/) ──HTTP──▶ /api/* ──▶ handlers ──▶ use cases
  Desktop JavaFX ──direto──▶ FabricaAplicacao (mesmos use cases, RN-19)
  Postgres ◀──JDBC── adapters/out (mesmo banco p/ todos, RN-19)
```

Regra: `domain/` não importa framework; `application/` não importa
adapter; adapters convertem (DTO/record ↔ entidade) e delegam.

## 2. Modelo de domínio (vocabulário dos requisitos)

```
Usuario (abstrata) ◀── Participante | Organizador
  - id, nome, email(Email VO), senha(SenhaHash VO)
  - alterarNome / alterarSenha (RF-03)

Evento ──<> compõe Atividade[] (1..*)
  - status: RASCUNHO → PUBLICADO → INSCRICOES_ABERTAS → ENCERRADO (RN-04)
  - maxVagas → PoliticaInscricao (Strategy: SemLimite/ComLimite) (RF-13/14)
  - periodo, local, fuso (RF-04/RN-20), exigeEscolhaAtividades (RF-13)
  - adicionarAtividade / remarcarAtividade validam conflito (RN-07)

Atividade ──<> tem Pessoa[] (papel livre, RF-08)
  - periodo(Periodo VO), local, tipo, trilha (RF-05/06)
  - politicaFrequencia (Strategy: Unica/EntradaSaida, RF-19)

Inscricao (participante + evento)
  - selecionarAtividade (bloqueia conflito, RF-18)
  - registrarCheckIn / registrarCheckInManual (RF-21/22, RN-11/12)
  - gerarAgenda → AgendaDoParticipante (cronológica, RF-17)
  - isAptoParaAvaliacao (RN-13), calcularPresenca (RF-23)

CheckIn: atividadeId + dataHora + origem(QR_CODE/MANUAL) + responsavelId
Pergunta: tipo(TEXTO/ESCOLHA/ESCALA) + validarResposta polimórfica (RF-25)
Questionario: congela após 1ª resposta (RN-17) + politicaIdentificacao (RN-15)
Avaliacao: elegibilidade no construtor (RN-13), duplicidade no use case (RN-14)

VOs imutáveis: Email, Periodo (conflitaCom), SenhaHash
```

## 3. Mapa de responsabilidades (classe → RF/ROO)

| Classe | Responsabilidade | RF / ROO |
|---|---|---|
| `Evento` | ciclo de vida, conflito local/horário, regras de inscrição | RF-04/07/13/14 · ROO-01/02/05 |
| `Atividade` | período/local/tipo/trilha, critério de presença | RF-05/06/19 · ROO-01/02 |
| `Inscricao` | seleção, conflito de agenda, check-ins, elegibilidade | RF-16/17/18/21/22, RN-13 · ROO-01/02 |
| `Questionario/Pergunta` | estrutura configurável, validação por tipo, congelamento | RF-24/25, RN-15/17 · ROO-02/05 |
| `Avaliacao/Resposta` | invariantes de submissão | RF-26/27 · ROO-02 |
| `Pessoa` | vínculo + papel livre | RF-08 · ROO-01 |
| `CheckIn` | rastreabilidade (origem/responsável) | RF-22, RN-11/12 · ROO-03 |
| `Politica*` (Strategy) | variação sem if/switch | RF-13/14/19 · ROO-05/10 |
| Use cases (20) | orquestração via portas | todos · ROO-08/09 |
| `MigradorSql` + V1–V9 | esquema reproduzível | RNF-04 · ROO-09 |

Herança só em `Usuario ← Participante/Organizador` (substituível,
ROO-06). Composição no resto (ROO-04). Erros via `DomainException` +
`HttpErrors` (400/401/403/404/422) (ROO-11).
