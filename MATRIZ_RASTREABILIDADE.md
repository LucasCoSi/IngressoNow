# Matriz de Rastreabilidade RF × ROO — IngressoNow (POO2)

> Gerada em 2026-09-04; atualizada em 2026-09-06 (pós-migração Java SE + Task #4).
> Base: `especificacao_requisitos_projeto_poo2_gestao_eventos_revisado_poo.pdf` v1.1.
> Convenção de status: ✅ implementado/integrado · ⚠️ parcial/mock · ❌ ausente.

## 1. Requisitos funcionais — núcleo obrigatório (M)

| RF | Requisito (resumo) | Status | Implementação (classe/endpoint) | Teste / demonstração |
|----|--------------------|--------|---------------------------------|----------------------|
| RF-01 | Cadastro usuário (nome, e-mail, senha; e-mail único) | ✅ | `CadastrarUsuarioUseCase`, `POST /api/auth/cadastro`, RN-01 via `existePorEmail` + UNIQUE | Manual via `/cadastro`; validado em Docker |
| RF-02 | Autenticação + permissões por perfil | ✅ | `AutenticarUsuarioUseCase`, `POST /api/auth/login`, JWT (`JwtAuth`, claim `userId`); `GET /api/eventos/**` e `/programacao` públicos, resto autenticado | Manual via `/login`; validado em Docker 2026-09-05/06 |
| RF-03 | Participante consulta/atualiza dados básicos | ✅ | `EditarUsuarioUseCase` + `UsuarioHandler` (`GET/PUT /api/usuarios/{id}`, só próprio perfil, 403 alheio); form no perfil da área do participante | Validado em Docker 2026-09-06 (PUT nome + 403) |
| RF-04 | CRUD + encerrar eventos | ✅ | `Evento` (publicar/encerrar/atualizar), `EventoHandler` (`GET`, `GET/{id}`, `POST`, `PUT/{id}`, `DELETE/{id}` 422 se há inscrições, `POST/{id}/publicar`, `POST/{id}/encerrar`) | Testes `EventoTest`, `CadastrarEventoUseCaseTest`; demo Docker |
| RF-05 | Atividades de tipos variados, sem categoria fixa | ✅ | `Atividade.tipo` (texto livre, V6); `POST /atividades` aceita `tipo`; grade exibe badge | Validado em Docker 2026-09-06 |
| RF-06 | Trilhas/categorias/espaços; filtrar e agrupar | ✅ | `Atividade.trilha` (V6) + filtros client-side (tipo/trilha/local) e server-side (`GET /programacao`) | Validado em Docker 2026-09-06 |
| RF-07 | Horários/locais com detecção de conflito | ✅ | `adicionarAtividade` bloqueia (mesmo local + sobreposição); `remarcarAtividade` revalida (simula antes de mutar); `PUT /atividades/{atId}`; decisão: grade paralela em locais diferentes é permitida | Validado em Docker 2026-09-06 (200 + 422) |
| RF-08 | Pessoas vinculadas com papéis | ✅ | `Pessoa` (nome + papel livre) + `PessoaRepository`/`JdbcPessoaRepository` (V8) + `VincularPessoaUseCase`; `POST/GET .../pessoas`, `DELETE .../pessoas/{pId}`; desktop exibe elenco | Validado em Docker 2026-09-06 (201/list/204) |
| RF-09 | Programação filtrável (data, horário, trilha, tipo, local — ≥3 filtros) | ✅ | `GET /api/eventos/{id}/programacao?data=&tipo=&trilha=&local=` (4 filtros server-side) + filtros client-side | Validado em Docker 2026-09-06 |
| RF-10 | Site público de eventos | ✅ | `HomeController` + `evento-publico.js` consumindo `GET /api/eventos` | Demo validada |
| RF-11 | Programação + destaques no site | ✅ | Programação + pessoas vinculadas (elenco por atividade); destaques = palestrantes via `GET .../pessoas` | Elenco validado via API |
| RF-12 | Cadastro + inscrição pelo site | ✅ | `/cadastro` → `POST /api/auth/cadastro` → `POST /api/inscricoes` | Fluxo CA-02 demonstrável |
| RF-13 | Regra de inscrição configurável (evento/atividade) | ✅ | `Evento.maxVagas` + `getPoliticaInscricao()` (Strategy) + flags `exigeEscolhaAtividades`/`vagasPorAtividade` (V9, expostas em `POST/PUT /api/eventos`); `Atividade.maxVagas` (V11, `POST /atividades`, `AtividadeDTO`) + enforcement em `SelecionarAtividadeUseCase` via `contarPorAtividade` (RN-06, 422 lotada) | `SelecionarAtividadeUseCaseTest` (4 casos) + `AtividadeTest` (limite) + Docker 2026-09-08 (V11 em base zerada; seleção OK 200, lotada 422 "sem mais vagas") |
| RF-14 | Controle de vagas | ✅ | `PoliticaComLimiteDeVagas` + `PoliticaSemLimite`, checado em `InscreverParticipanteUseCase` (RN-06) | `InscreverParticipanteUseCaseTest` |
| RF-15 | Cancelamento de inscrição (libera vaga) | ✅ | `DELETE /api/inscricoes/{id}` via `CancelarInscricaoUseCase` (decisão: remoção física + CASCADE limpa filhas; bloqueia se evento ENCERRADO); botão na área do participante | Validado em Docker (204 + listagem vazia) |
| RF-16 | Seleção de atividades | ✅ | `Inscricao.selecionarAtividade` + `POST /api/inscricoes/{id}/atividades/{atId}` | `InscricaoTest` |
| RF-17 | Agenda cronológica | ✅ | `AgendaDoParticipante` ordena por `periodo.inicio`; `GET /api/inscricoes/{id}/agenda` | `ConsultarAgendaUseCase` (sem teste dedicado — gap menor) |
| RF-18 | Conflito de agenda alertado/impedido | ✅ | `Inscricao.selecionarAtividade` lança `DomainException` (RN-07); `programacao.js` exibe motivo em destaque no card (sem auto-dismiss) | `InscricaoTest` + frontend |
| RF-19 | Critério de presença por atividade | ✅ | `Atividade.politicaFrequencia` (default `FrequenciaUnicaStrategy`; `setPoliticaFrequencia`); existe `FrequenciaEntradaSaidaStrategy` | Testes de check-in |
| RF-20 | QR Code sem dado sensível (RN-10) | ✅ | `QrCodeService`/`SimpleQrCodeService` + `GET /api/controle/qrcode/{insc}/{at}` (token `ids+salt` Base64, sem PII) | Payload verificado em Docker |
| RF-21 | Frequência por QR | ✅ | `POST /api/controle/checkin` → `RealizarCheckInQrCodeUseCase` → `Inscricao.registrarCheckIn` (RN-11: guarda data/hora) | `RealizarCheckInQrCodeUseCaseTest` + Docker |
| RF-22 | Lançamento manual alternativo com autoria | ✅ | `CheckIn.Origem` (QR_CODE/MANUAL) + `responsavelId`; `RealizarCheckInManualUseCase`; `POST /api/controle/manual` (responsável = JWT); V7; form na página de frequência | Validado em Docker 2026-09-06 (audit `MANUAL/maria`) |
| RF-23 | Situação de presença calculada/reproduzível | ✅ | `Inscricao.calcularPresenca` delega à `PoliticaFrequencia` | Coberto indiretamente |
| RF-24 | Questionários configuráveis | ✅ | `Questionario/Pergunta/TipoPergunta` + `JdbcQuestionarioRepository` (V4) + `AvaliacaoHandler` (`POST/GET /eventos/{id}/questionarios`, congela após 1ª resposta RN-17) | Validado em Docker (201 + CASCADE fix) |
| RF-25 | Tipos: texto, escolha única, escala numérica | ✅ | `TipoPergunta` com `ESCALA_NUMERICA` + validação polimórfica (`Pergunta.validarResposta`) | Validado em Docker |
| RF-26 | Elegibilidade: inscrito + presença validada | ✅ | `Avaliacao` construtor + `isAptoParaAvaliacao()` (RN-13) com enforcement via `SubmeterAvaliacaoUseCase` | `AvaliacaoTest` + Docker (422 sem presença) |
| RF-27 | Uma resposta por participante/questionário (RN-14) | ✅ | `existePorQuestionarioEInscricao` + UNIQUE `(questionario_id, inscricao_id)` | Validado em Docker (422 duplicada) |
| RF-28 | Consolidação de resultados | ✅ | `ConsolidarAvaliacoesUseCase` + `GET /api/questionarios/{id}/resultados` (média, distribuição, comentários) | Validado em Docker (`media: 4.0`) |
| RF-29 | Relatório de inscritos | ✅ | `GerarRelatorioInscritosUseCase` + `GET /api/controle/relatorio/inscritos/{eventoId}` (CSV); botão na página de relatórios | Validado em Docker 2026-09-06 |
| RF-30 | Relatório de frequência/participação | ✅ | `GerarRelatorioPresencaUseCase` + `GET /api/controle/relatorio/presenca/{eventoId}` (CSV) | Demo CA-07; CSV conta como RF-31 também |
| RF-31 | Exportação portátil (CSV/PDF) | ✅ | CSV de presença (RNF-10 OK p/ demo) | Baixável via frontend `relatorios.js` |
| RF-32 | Critério de elegibilidade p/ certificado | ✅ (D) | `CriterioCertificado` (% mínimo + obrigatórias); `POST .../certificados/criterio`; RN-16 calculável | `CriterioCertificadoTest` (4 casos) |
| RF-33 | Certificado em PDF p/ elegível | ✅ (D) | `PdfCertificadoService` (porta) + `PdfBoxCertificadoService` (adapter) + `GET .../certificados/{codigo}/pdf` (binário); área do participante lista e baixa | `PdfBoxCertificadoServiceTest` (header %PDF + texto extraído) + Docker |
| RF-34 | Envio de certificado por e-mail | ✅ (D) | `EmailService` (porta) + `SmtpEmailService` (adapter, `SMTP_*`); `POST .../certificados/enviar` retorna registro; sem SMTP → 422 claro, download preservado | Teste com stub (registro + anexo) + teste de rejeição sem SMTP; envio real requer servidor SMTP |
| RF-35 | Certificado/declaração p/ palestrante | ✅ (D) | `POST .../certificados/palestrante` reflete papel exercido (`Pessoa.papel`) | `GerenciarCertificadosUseCaseTest` |
| RF-36 | Social (P) | ❌ | Fora do escopo obrigatório | Não fazer |

## 2. Regras de negócio — pontos de atenção

- RN-04 (estado do evento controla visibilidade/inscrições): ✅ `listar` retorna só PUBLICADO/INSCRICOES_ABERTAS e `InscreverParticipanteUseCase` bloqueia demais status.
- RN-11/RN-12 (rastreabilidade da frequência): ✅ `CheckIn` com `origem` + `responsavelId` (V7); QR registra data/hora, manual registra autoria.
- RN-17 (mudança de regra não reescreve histórico): ✅ questionário congela após 1ª resposta (`congelar()` + `exigirEditavel()`); `politicaFrequencia` documentada como recálculo aceito.
- RN-19 (mesmo banco p/ desktop, API e site): ✅ Postgres (V1–V8) + `MigradorSql`/`migrations.txt`; desktop JavaFX usa `FabricaAplicacao` direta.
- RN-20 (fuso do evento): ✅ `Evento.fuso` (IANA, validado em `definirFuso`, default `UTC`, coluna V9, exposto no DTO); simplificação declarada: horários persistem/exibem em `LocalDateTime` sem conversão de zona.

## 3. Evidências ROO (avaliação — 60% da nota)

| ROO | Evidência atual | Arquivo(s) |
|-----|-----------------|------------|
| ROO-01 modelo rico | `Evento`, `Inscricao`, `Atividade`, `Questionario`, `Pessoa` concentram regras | `entity/Evento.java`, `entity/Inscricao.java`, `entity/Atividade.java`, `entity/Pessoa.java` |
| ROO-02 encapsulamento | Validações em construtores; listas expostas como unmodifiable; **porém** `Atividade.setPoliticaFrequencia` é setter público e `Pergunta/Resposta/Avaliacao` quase sem validação | idem + `Pergunta.java`, `Resposta.java` |
| ROO-03 objetos de valor | `Email`, `Periodo`, `SenhaHash`, `Horario` imutáveis com validação | `model/vo/*` |
| ROO-04 composição | `Evento` compõe `Atividade`; `Inscricao` compõe seleção + check-ins; `AgendaDoParticipante` deriva de `Inscricao` | `Evento.java:131`, `Inscricao.java:111` |
| ROO-05 polimorfismo | `PoliticaInscricao` (SemLimite/ComLimite) e `PoliticaFrequencia` (Unica/EntradaSaida) — reais, selecionadas por configuração | `Politica*.java`, `Frequencia*Strategy.java` |
| ROO-06 herança | `Usuario` ← `Participante`/`Organizador`; relação substituível justificável | `Usuario.java`, `Participante.java`, `Organizador.java` |
| ROO-07 interfaces | Portas `EventoRepository`, `InscricaoRepository`, `UsuarioRepository`, `QrCodeService` — contratos pequenos | `port/out/*`, `port/in/*` |
| ROO-08 SOLID | Inversão de dependência (use cases dependem de portas); SRP por use case; falta registrar 1 refatoração motivada | `application/usecase/*` |
| ROO-09 hexagonal | `domain/` sem imports de framework; `application/` sem anotações (fiação manual em `FabricaAplicacao`); JDBC isolado em `adapters/out`; HTTP e desktop em `adapters/in` | `Poo2Application.java`, `infra/FabricaAplicacao.java`, adapters |
| ROO-10 padrões | ① Strategy (inscrição + frequência) com efeito real; ② Ports & Adapters; candidato a 3º: Builder/Factory p/ `Questionario` ou Specification p/ elegibilidade — Task #2/#4 | — |
| ROO-11 erros de domínio | `DomainException` + `GlobalExceptionHandler` (400/404/422/500) + `StandardError` | `exception/*` |
| ROO-12 testes/refatoração | 27 arquivos / 91 métodos JUnit 5 + Mockito (`mvn test`: 0 falhas); 2 refatorações registradas em `REFATORACOES.md` (R-01 `exigirSemConflito`, R-02 upsert + DELETE de órfãs) | `src/test/**`, `REFATORACOES.md` |

**Dívida de modelagem (resolvida na migração Java SE):** pacotes legados
duplicados foram excluídos — `domain/` contém só `entity/`, `vo/`,
`port/` e `exception/`.

## 4. Decisões D-01–D-08 (rascunho p/ README — Task #5)

- D-01 desktop: **JavaFX** (`adapters/in/desktop/DesktopApp`, código puro sem FXML; login → eventos → detalhe com elenco → inscrever; mesmo banco/use cases via `FabricaAplicacao`; `--headless` = só API). Ver `DECISOES.md`.
- D-02 API/auth: **Java SE + JWT (jjwt)** (migração Spring Boot → Java SE puro, commit `f465e46`, checkpoint `spring-final`; HTTP via `com.sun.net.httpserver`, hash via jBCrypt; contratos `/api/*` preservados).
- D-03 banco: **Postgres + JDBC puro + MigradorSql** (reproduzível V1–V10 via `migrations.txt`; alternativa JPA/Flyway da era Spring descartada na migração pelo custo de manter o container de DI).
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

## 6. Fila de execução — estado em 2026-09-06

1. Task #2 — avaliações persistidas ✅ (V4, repos, handler, escala, anti-duplicidade, consolidação; CASCADE fix validado).
2. Task #3 — inscrição/agenda ✅ (RN-04 no use case, DELETE cancelamento, motivo de conflito no frontend).
3. Task #4 — gaps ✅ backend + frontend (RF-03/05–09/22/29, RN-11/12/17; V6–V8). Restos conscientes: RF-13 regra por atividade, RN-20 fuso.
4. Task #5 — entregáveis §11 ✅ (README, DECISOES D-01–D-08, ENDPOINTS, seeds V3, esta matriz). Fase 4: esqueleto JavaFX ligado (login → eventos → detalhe → inscrever).
