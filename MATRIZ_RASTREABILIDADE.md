# Matriz de Rastreabilidade RF × ROO — IngressoNow (POO2)

> Gerada em 2026-09-04 a partir da auditoria do código vs
> `especificacao_requisitos_projeto_poo2_gestao_eventos_revisado_poo.pdf` v1.1.
> Convenção de status: ✅ implementado/integrado · ⚠️ parcial/mock · ❌ ausente.

## 1. Requisitos funcionais — núcleo obrigatório (M)

| RF | Requisito (resumo) | Status | Implementação (classe/endpoint) | Teste / demonstração |
|----|--------------------|--------|---------------------------------|----------------------|
| RF-01 | Cadastro usuário (nome, e-mail, senha; e-mail único) | ✅ | `CadastrarUsuarioUseCase`, `POST /api/auth/cadastro`, `UsuarioPersistenceAdapter`, RN-01 via `existePorEmail` + UNIQUE no banco | Manual via `/cadastro`; sem teste automatizado do use case — gap |
| RF-02 | Autenticação + permissões por perfil | ✅ | `AutenticarUsuarioUseCase`, `POST /api/auth/login`, JWT (`JwtTokenProvider`, `JwtAuthenticationFilter`), `SecurityConfig` (GET `/api/eventos/**` público; resto autenticado) | Manual via `/login`; RNF-06 atendido no servidor |
| RF-03 | Participante consulta/atualiza dados básicos | ❌ | `Usuario.alterarNome/alterarSenha` existem no domínio, mas **sem endpoint** (sem `PUT /api/usuarios/{id}` nem controller de usuário) | Nada demonstrável — gap Task #4 |
| RF-04 | CRUD + encerrar eventos | ✅ | `Evento` (publicar/encerrar/atualizar), `EventoRestController` (`GET`, `GET/{id}`, `POST`, `PUT/{id}`, `DELETE/{id}` com bloqueio 422 se há inscrições, `POST/{id}/publicar`, `POST/{id}/encerrar`) | Testes `EventoTest`, `CadastrarEventoUseCaseTest`; demo CRUD validada em Docker 2026-09-03 |
| RF-05 | Atividades de tipos variados, sem categoria fixa | ⚠️ | `Atividade` tem só `titulo/periodo/local` — **tipo é implícito no título**, sem campo `tipo` nem composição de características | `AdicionarAtividadeUseCaseTest`; gap: adicionar campo `tipo` configurável — Task #4 |
| RF-06 | Trilhas/categorias/espaços; filtrar e agrupar | ❌ | Nada — sem entidade `Trilha/Categoria`, sem filtro | Gap Task #4 (mínimo: campo `trilha` + filtro na programação) |
| RF-07 | Horários/locais com detecção de conflito | ⚠️ | `Evento.adicionarAtividade` bloqueia conflito **mesmo local + período sobreposto** (`Periodo.conflitaCom`); `Atividade.remarcar` **não revalida** contra irmãs | `EventoTest` cobre conflito; gap: documentar decisão + revalidar remarcação — Task #4 |
| RF-08 | Pessoas vinculadas com papéis | ❌ | Nada — sem `Pessoa/Papel` (palestrante, apresentador, responsável) | Gap Task #4 |
| RF-09 | Programação filtrável (data, horário, trilha, tipo, local — ≥3 filtros) | ❌ | `GET /api/eventos` lista tudo; frontend `programacao.js` filtra client-side; sem filtros server-side | Gap Task #4 |
| RF-10 | Site público de eventos | ✅ | `HomeController` + `evento-publico.js` consumindo `GET /api/eventos` | Demo validada |
| RF-11 | Programação + destaques no site | ⚠️ | Programação existe; **pessoas em destaque** impossível (RF-08 ausente) | Parcial |
| RF-12 | Cadastro + inscrição pelo site | ✅ | `/cadastro` → `POST /api/auth/cadastro` → `POST /api/inscricoes` | Fluxo CA-02 demonstrável |
| RF-13 | Regra de inscrição configurável (evento/atividade) | ⚠️ | `Evento.maxVagas` + `getPoliticaInscricao()` (Strategy); **sem** regra por atividade nem flag "exige escolha de atividades" | Gap Task #3 |
| RF-14 | Controle de vagas | ✅ | `PoliticaComLimiteDeVagas` + `PoliticaSemLimite`, checado em `InscreverParticipanteUseCase` (RN-06) | `InscreverParticipanteUseCaseTest` |
| RF-15 | Cancelamento de inscrição (libera vaga) | ❌ | Sem `DELETE /api/inscricoes/{id}`, sem `Inscricao.cancelar()`, sem estado/situação | Gap Task #3 (exige decisão: estado CANCELADA vs remoção) |
| RF-16 | Seleção de atividades | ✅ | `Inscricao.selecionarAtividade` + `POST /api/inscricoes/{id}/atividades/{atId}` | `InscricaoTest` |
| RF-17 | Agenda cronológica | ✅ | `AgendaDoParticipante` ordena por `periodo.inicio`; `GET /api/inscricoes/{id}/agenda` | `ConsultarAgendaUseCase` (sem teste dedicado — gap menor) |
| RF-18 | Conflito de agenda alertado/impedido | ✅ | `Inscricao.selecionarAtividade` lança `DomainException` em conflito (RN-07) | `InscricaoTest`; falta exibir motivo no frontend — Task #3 |
| RF-19 | Critério de presença por atividade | ✅ | `Atividade.politicaFrequencia` (default `FrequenciaUnicaStrategy`; `setPoliticaFrequencia`); existe `FrequenciaEntradaSaidaStrategy` | Testes de check-in |
| RF-20 | QR Code sem dado sensível (RN-10) | ✅ | `QrCodeService`/`SimpleQrCodeService` + `GET /api/controle/qrcode/{insc}/{at}` | Verificar payload do token na demo CA-04 |
| RF-21 | Frequência por QR | ✅ | `POST /api/controle/checkin` → `RealizarCheckInQrCodeUseCase` → `Inscricao.registrarCheckIn` (RN-11: guarda data/hora) | `RealizarCheckInQrCodeUseCaseTest` |
| RF-22 | Lançamento manual alternativo com autoria | ❌ | Sem endpoint; `CheckIn` **não guarda origem/responsável** (RN-11/RN-12 descobertos) | Gap Task #4: estender `CheckIn` + `POST /api/controle/manual` |
| RF-23 | Situação de presença calculada/reproduzível | ✅ | `Inscricao.calcularPresenca` delega à `PoliticaFrequencia` | Coberto indiretamente |
| RF-24 | Questionários configuráveis | ⚠️ | `Questionario/Pergunta/TipoPergunta` existem no domínio; **sem persistência, sem endpoint** | `AvaliacaoTest` (domínio); gap Task #2 |
| RF-25 | Tipos: texto, escolha única, escala numérica | ⚠️ | `TipoPergunta` tem só `ESCOLHA_SIMPLES, TEXTO_LIVRE` — **falta escala numérica** + validação polimórfica por tipo | Gap Task #2 |
| RF-26 | Elegibilidade: inscrito + presença validada | ✅ (domínio) / ❌ (API) | `Avaliacao` construtor + `Inscricao.isAptoParaAvaliacao()` (RN-13); sem enforcement via API (sem controller) | `AvaliacaoTest` cobre os dois ramos |
| RF-27 | Uma resposta por participante/questionário (RN-14) | ❌ | Sem repositório de avaliações → sem verificação de duplicidade | Gap Task #2 |
| RF-28 | Consolidação de resultados | ❌ | Nada | Gap Task #2 (mínimo: `GET` com contagens + comentários) |
| RF-29 | Relatório de inscritos | ❌ | Só listagem `GET /api/inscricoes?eventoId=`; sem relatório dedicado | Gap Task #4 |
| RF-30 | Relatório de frequência/participação | ✅ | `GerarRelatorioPresencaUseCase` + `GET /api/controle/relatorio/presenca/{eventoId}` (CSV) | Demo CA-07; CSV conta como RF-31 também |
| RF-31 | Exportação portátil (CSV/PDF) | ✅ | CSV de presença (RNF-10 OK p/ demo) | Baixável via frontend `relatorios.js` |
| RF-32–35 | Certificados (D) | ❌ | Fora do núcleo; não iniciar antes do núcleo estável | Backlog pós-núcleo |
| RF-36 | Social (P) | ❌ | Fora do escopo obrigatório | Não fazer |

## 2. Regras de negócio — pontos de atenção

- RN-04 (estado do evento controla visibilidade/inscrições): ⚠️ `listar` retorna só publicados, mas `InscreverParticipanteUseCase` **não verifica** se evento está publicado/aberto — gap Task #3.
- RN-11/RN-12 (rastreabilidade da frequência): ❌ `CheckIn` sem `origem/responsável` — Task #4.
- RN-17 (mudança de regra não reescreve histórico): ⚠️ trocar `politicaFrequencia` recalcula tudo; aceitar como decisão documentada ou congelar situação — Task #4.
- RN-19 (mesmo banco p/ desktop, API e site): ✅ Postgres + Flyway V1–V3.
- RN-20 (fuso do evento): ❌ `LocalDateTime` sem zona — documentar como simplificação declarada.

## 3. Evidências ROO (avaliação — 60% da nota)

| ROO | Evidência atual | Arquivo(s) |
|-----|-----------------|------------|
| ROO-01 modelo rico | `Evento`, `Inscricao`, `Atividade` concentram regras | `entity/Evento.java`, `entity/Inscricao.java`, `entity/Atividade.java` |
| ROO-02 encapsulamento | Validações em construtores; listas expostas como unmodifiable; **porém** `Atividade.setPoliticaFrequencia` é setter público e `Pergunta/Resposta/Avaliacao` quase sem validação | idem + `Pergunta.java`, `Resposta.java` |
| ROO-03 objetos de valor | `Email`, `Periodo`, `SenhaHash`, `Horario` imutáveis com validação | `model/vo/*` |
| ROO-04 composição | `Evento` compõe `Atividade`; `Inscricao` compõe seleção + check-ins; `AgendaDoParticipante` deriva de `Inscricao` | `Evento.java:131`, `Inscricao.java:111` |
| ROO-05 polimorfismo | `PoliticaInscricao` (SemLimite/ComLimite) e `PoliticaFrequencia` (Unica/EntradaSaida) — reais, selecionadas por configuração | `Politica*.java`, `Frequencia*Strategy.java` |
| ROO-06 herança | `Usuario` ← `Participante`/`Organizador`; relação substituível justificável | `Usuario.java`, `Participante.java`, `Organizador.java` |
| ROO-07 interfaces | Portas `EventoRepository`, `InscricaoRepository`, `UsuarioRepository`, `QrCodeService` — contratos pequenos | `port/out/*`, `port/in/*` |
| ROO-08 SOLID | Inversão de dependência (use cases dependem de portas); SRP por use case; falta registrar 1 refatoração motivada | `application/usecase/*` |
| ROO-09 hexagonal | `domain/` sem imports Spring; `application/` só com `@Service` (acoplamento leve, documentável); JPA isolado em `adapters/out` | `Poo2Application.java`, adapters |
| ROO-10 padrões | ① Strategy (inscrição + frequência) com efeito real; ② Ports & Adapters; candidato a 3º: Builder/Factory p/ `Questionario` ou Specification p/ elegibilidade — Task #2/#4 | — |
| ROO-11 erros de domínio | `DomainException` + `GlobalExceptionHandler` (400/404/422/500) + `StandardError` | `exception/*` |
| ROO-12 testes/refatoração | 15 testes de comportamento; **zero refatorações registradas** — escrever 2 (ex: extração de `PoliticaInscricao`, extração de `AgendaDoParticipante`) | `src/test/**` |

**Dívida de modelagem:** pacotes legados duplicados (`domain/model/usuario`, `dominio/model/evento`, `dominio/model/inscricao`, `dominio/model/vo`) coexistem com `domain/model/entity` — decidir: excluir ou marcar `@Deprecated`. Recomendado excluir na Task #4.

## 4. Decisões D-01–D-08 (rascunho p/ README — Task #5)

- D-01 desktop: a definir (JavaFX vs CLI demonstrativo; site já existe).
- D-02 API/auth: **Spring Boot + JWT** (justificativa: time já domina, adapters isolados, Bean Validation/RNF-07, alternativa Javalin descartada por falta de ecossistema de segurança).
- D-03 banco: **Postgres + JPA + Flyway** (RNF-04 reproduzível V1–V3; alternativa JDBC puro descartada pelo custo de reescrita dos mappers).
- D-04 site: HTML/JS estático consumindo `/api/*` (mesma API/banco — RNF-02).
- D-05 QR: token opaco `inscricaoId+atividadeId` assinado, sem PII (RN-10); expiração a definir na Task #4.
- D-06 políticas: Strategy p/ inscrição e frequência; Specification p/ elegibilidade de certificado (quando D).
- D-07 padrões: Strategy + Hexagonal (efeito: variação sem `if/switch` espalhado).
- D-08 testes/seeds: JUnit 5 no domínio + seeds V3 (`senha123`, 2 eventos, 5 atividades).

## 5. Cenários CA-01–CA-07

| Cenário | Status |
|---------|--------|
| CA-01 publicação | ✅ |
| CA-02 inscrição | ✅ |
| CA-03 agenda/conflito | ✅ (domínio; falta UX do motivo no frontend) |
| CA-04 frequência QR | ✅ |
| CA-05 manual | ❌ — Task #4 |
| CA-06 avaliação | ❌ — Task #2 |
| CA-07 relatório | ✅ (presença CSV; inscritos — Task #4) |

## 6. Fila de execução

1. Task #2 — avaliações persistidas (RF-24–28, RN-13/14): migration V4 + repositories + `AvaliacaoController` + escala numérica + anti-duplicidade + consolidação.
2. Task #3 — inscrição/agenda (RF-13/15, RN-04/05): checar evento publicado, `DELETE /api/inscricoes/{id}` (cancelamento), regra por atividade (backlog mínimo), motivo de conflito no frontend.
3. Task #4 — gaps (RF-03/05–09/22/29, RN-11/12/17/20): `PUT /api/usuarios/{id}`, `tipo`+`trilha` na atividade, pessoas/papéis, filtros, check-in manual com autoria, relatório de inscritos, remover duplicados legados.
4. Task #5 — entregáveis §11: README, D-01–D-08, coleção de endpoints, seeds, matriz final.
