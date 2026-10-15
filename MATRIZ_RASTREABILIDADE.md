# Matriz de Rastreabilidade RF × ROO — IngressoNow (POO2)

> Gerada em 2026-09-04; atualizada em 2026-09-22 (desktop DSK-00–DSK-51 + site honesto).
> Base: `especificacao_requisitos_projeto_poo2_gestao_eventos_revisado_poo.pdf` v1.1.
> Convenção de status: ✅ implementado/integrado · ⚠️ parcial/mock · ❌ ausente.
> Coluna Desktop: história que cobre o RF no JavaFX (`BACKLOG_DESKTOP.md`); site = sem tela de operador.

## 1. Requisitos funcionais — núcleo obrigatório (M)

| RF | Requisito (resumo) | Status | Implementação (classe/endpoint) | Teste / demonstração | Desktop |
|----|--------------------|--------|---------------------------------|----------------------|---------|
| RF-01 | Cadastro usuário (nome, e-mail, senha; e-mail único) | ✅ | `CadastrarUsuarioUseCase`, `POST /api/auth/cadastro`, RN-01 via `existePorEmail` + UNIQUE | Manual via `/cadastro`; validado em Docker | DSK-00 |
| RF-02 | Autenticação + permissões por perfil | ✅ | `AutenticarUsuarioUseCase`, `POST /api/auth/login`, JWT (`JwtAuth`, claim `userId`); `GET /api/eventos/**` e `/programacao` públicos, resto autenticado | Manual via `/login`; validado em Docker 2026-09-05/06 | DSK-00 |
| RF-03 | Participante consulta/atualiza dados básicos | ✅ | `EditarUsuarioUseCase` + `UsuarioHandler` (`GET/PUT /api/usuarios/{id}`, só próprio perfil, 403 alheio); form no perfil da área do participante | Validado em Docker 2026-09-06 (PUT nome + 403) | DSK-00 (demo) |
| RF-04 | CRUD + encerrar eventos | ✅ | `Evento` (publicar/encerrar/atualizar), `EventoHandler` (`GET`, `GET/{id}`, `POST`, `PUT/{id}`, `DELETE/{id}` 422 se há inscrições, `POST/{id}/publicar`, `POST/{id}/encerrar`) | Testes `EventoTest`, `CadastrarEventoUseCaseTest`; demo Docker | DSK-10 |
| RF-05 | Atividades de tipos variados, sem categoria fixa | ✅ | `Atividade.tipo` (texto livre, V6); `POST /atividades` aceita `tipo`; grade exibe badge | Validado em Docker 2026-09-06 | DSK-11 |
| RF-06 | Trilhas/categorias/espaços; filtrar e agrupar | ✅ | `Atividade.trilha` (V6) + filtros client-side (tipo/trilha/local) e server-side (`GET /programacao`) | Validado em Docker 2026-09-06 | DSK-11 |
| RF-07 | Horários/locais com detecção de conflito | ✅ | `adicionarAtividade` bloqueia (mesmo local + sobreposição); `remarcarAtividade` revalida (simula antes de mutar); `PUT /atividades/{atId}`; decisão: grade paralela em locais diferentes é permitida | Validado em Docker 2026-09-06 (200 + 422) | DSK-11 |
| RF-08 | Pessoas vinculadas com papéis | ✅ | `Pessoa` (nome + papel livre) + `PessoaRepository`/`JdbcPessoaRepository` (V8) + `VincularPessoaUseCase`; `POST/GET .../pessoas`, `DELETE .../pessoas/{pId}`; desktop exibe elenco | Validado em Docker 2026-09-06 (201/list/204) | DSK-12 |
| RF-09 | Programação filtrável (data, horário, trilha, tipo, local — ≥3 filtros) | ✅ | `GET /api/eventos/{id}/programacao?data=&tipo=&trilha=&local=` (4 filtros server-side) + filtros client-side | Validado em Docker 2026-09-06 | DSK-13 |
| RF-10 | Site público de eventos | ✅ | `HomeController` + `evento-publico.js` consumindo `GET /api/eventos` | Demo validada | site (operador: -) |
| RF-11 | Programação + destaques no site | ✅ | Programação + pessoas vinculadas (elenco por atividade); destaques = palestrantes via `GET .../pessoas` | Elenco validado via API | site (operador: -) |
| RF-12 | Cadastro + inscrição pelo site | ✅ | `/cadastro` → `POST /api/auth/cadastro` → `POST /api/inscricoes` | Fluxo CA-02 demonstrável | DSK-20 |
| RF-13 | Regra de inscrição configurável (evento/atividade) | ✅ | `Evento.maxVagas` + `getPoliticaInscricao()` (Strategy) + flags `exigeEscolhaAtividades`/`vagasPorAtividade` (V9, expostas em `POST/PUT /api/eventos`); `Atividade.maxVagas` (V11, `POST /atividades`, `AtividadeDTO`) + enforcement em `SelecionarAtividadeUseCase` via `contarPorAtividade` (RN-06, 422 lotada) | `SelecionarAtividadeUseCaseTest` (4 casos) + `AtividadeTest` (limite) + Docker 2026-09-08 (V11 em base zerada; seleção OK 200, lotada 422 "sem mais vagas") | DSK-10/11/20 |
| RF-14 | Controle de vagas | ✅ | `PoliticaComLimiteDeVagas` + `PoliticaSemLimite`, checado em `InscreverParticipanteUseCase` (RN-06) | `InscreverParticipanteUseCaseTest` | DSK-20 |
| RF-15 | Cancelamento de inscrição (libera vaga) | ✅ | `DELETE /api/inscricoes/{id}` via `CancelarInscricaoUseCase` (decisão: remoção física + CASCADE limpa filhas; bloqueia se evento ENCERRADO); botão na área do participante | Validado em Docker (204 + listagem vazia) | DSK-20 |
| RF-16 | Seleção de atividades | ✅ | `Inscricao.selecionarAtividade` + `POST /api/inscricoes/{id}/atividades/{atId}` | `InscricaoTest` | DSK-21 |
| RF-17 | Agenda cronológica | ✅ | `AgendaDoParticipante` ordena por `periodo.inicio`; `GET /api/inscricoes/{id}/agenda` | `ConsultarAgendaUseCase` (sem teste dedicado — gap menor) | DSK-21 |
| RF-18 | Conflito de agenda alertado/impedido | ✅ | `Inscricao.selecionarAtividade` lança `DomainException` (RN-07); `programacao.js` exibe motivo em destaque no card (sem auto-dismiss) | `InscricaoTest` + frontend | DSK-21 |
| RF-19 | Critério de presença por atividade | ✅ | `Atividade.politicaFrequencia` (default `FrequenciaUnicaStrategy`; `setPoliticaFrequencia`); existe `FrequenciaEntradaSaidaStrategy` | Testes de check-in | DSK-30 |
| RF-20 | QR Code sem dado sensível (RN-10) | ✅ | `QrCodeService`/`SimpleQrCodeService` (`VALIDADE=5min`, token `ids+emitidoEm+salt` Base64-URL, sem PII) + `GET /api/controle/qrcode/{insc}/{at}` | `SimpleQrCodeServiceTest` (decodifica, expira, rejeita adulterado) + payload verificado em Docker | DSK-31 |
| RF-21 | Frequência por QR | ✅ | `POST /api/controle/checkin` (dono OU organizador, CA-04) → `RealizarCheckInQrCodeUseCase` → `Inscricao.registrarCheckIn` (RN-11: data/hora; RNF-11: repetição no check-in único → 422 sem duplicar; token expirado → 422) | `RealizarCheckInQrCodeUseCaseTest` (sucesso + inválido + anti-duplicidade) + `InscricaoTest` (único bloqueia, entrada/saída admite par) + Docker | DSK-31 |
| RF-22 | Lançamento manual alternativo com autoria | ✅ | `CheckIn.Origem` (QR_CODE/MANUAL) + `responsavelId`; `RealizarCheckInManualUseCase`; `POST /api/controle/manual` (responsável = JWT); V7; form na página de frequência | Validado em Docker 2026-09-06 (audit `MANUAL/maria`) | DSK-32 |
| RF-23 | Situação de presença calculada/reproduzível | ✅ | `Inscricao.calcularPresenca` delega à `PoliticaFrequencia` | Coberto indiretamente | DSK-30 |
| RF-24 | Questionários configuráveis | ✅ | `Questionario/Pergunta/TipoPergunta` + `JdbcQuestionarioRepository` (V4) + `AvaliacaoHandler` (`POST/GET /eventos/{id}/questionarios`, congela após 1ª resposta RN-17) | Validado em Docker (201 + CASCADE fix) | DSK-40 |
| RF-25 | Tipos: texto, escolha única, escala numérica | ✅ | `TipoPergunta` com `ESCALA_NUMERICA` + validação polimórfica (`Pergunta.validarResposta`) | Validado em Docker | DSK-40 |
| RF-26 | Elegibilidade: inscrito + presença validada | ✅ | `Avaliacao` construtor + `isAptoParaAvaliacao()` (RN-13) com enforcement via `SubmeterAvaliacaoUseCase` | `AvaliacaoTest` + Docker (422 sem presença) | DSK-41 |
| RF-27 | Uma resposta por participante/questionário (RN-14) | ✅ | `existePorQuestionarioEInscricao` + UNIQUE `(questionario_id, inscricao_id)` | Validado em Docker (422 duplicada) | DSK-41 |
| RF-28 | Consolidação de resultados | ✅ | `ConsolidarAvaliacoesUseCase` + `GET /api/questionarios/{id}/resultados` (média, distribuição, comentários) | Validado em Docker (`media: 4.0`) | DSK-42 |
| RF-29 | Relatório de inscritos | ✅ | `GerarRelatorioInscritosUseCase` + `GET /api/controle/relatorio/inscritos/{eventoId}` (CSV); botão na página de relatórios | Validado em Docker 2026-09-06 | DSK-50 |
| RF-30 | Relatório de frequência/participação | ✅ | `GerarRelatorioPresencaUseCase` + `GET /api/controle/relatorio/presenca/{eventoId}` (CSV) | Demo CA-07; CSV conta como RF-31 também | DSK-50 |
| RF-31 | Exportação portátil (CSV/PDF) | ✅ | CSV de presença (RNF-10 OK p/ demo) | Baixável via frontend `relatorios.js` | DSK-50 |
| RF-32 | Critério de elegibilidade p/ certificado | ✅ (D) | `CriterioCertificado` (% mínimo + obrigatórias); `POST .../certificados/criterio`; RN-16 calculável | `CriterioCertificadoTest` (4 casos) | DSK-51 |
| RF-33 | Certificado em PDF p/ elegível | ✅ (D) | `PdfCertificadoService` (porta) + `PdfBoxCertificadoService` (adapter) + `GET .../certificados/{codigo}/pdf` (binário); área do participante lista e baixa | `PdfBoxCertificadoServiceTest` (header %PDF + texto extraído) + Docker | DSK-51 |
| RF-34 | Envio de certificado por e-mail | ✅ (D) | `EmailService` (porta) + `SmtpEmailService` (adapter, `SMTP_*`); `POST .../certificados/enviar` retorna registro; sem SMTP → 422 claro, download preservado | Teste com stub (registro + anexo) + teste de rejeição sem SMTP; envio real requer servidor SMTP | DSK-51 |
| RF-35 | Certificado/declaração p/ palestrante | ✅ (D) | `POST .../certificados/palestrante` reflete papel exercido (`Pessoa.papel`) | `GerenciarCertificadosUseCaseTest` | DSK-51 |
| RF-36 | Social (P) | ❌ | Fora do escopo obrigatório | Não fazer | fora de escopo |

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
| ROO-02 encapsulamento | Validações em construtores; listas expostas como unmodifiable; mutação de `Atividade` só via `definirPoliticaFrequencia`/`configurarLimiteDeVagas`/`remarcar` (sem setter público da estratégia); `Pergunta/Resposta` validam no construtor + `validarResposta` | idem + `Pergunta.java`, `Resposta.java`, `TipoPergunta.java` |
| ROO-03 objetos de valor | `Email`, `Periodo`, `SenhaHash`, `Horario` imutáveis com validação | `model/vo/*` |
| ROO-04 composição | `Evento` compõe `Atividade`; `Inscricao` compõe seleção + check-ins; `AgendaDoParticipante` deriva de `Inscricao` | `Evento.java:131`, `Inscricao.java:111` |
| ROO-05 polimorfismo | `PoliticaInscricao` (SemLimite/ComLimite), `PoliticaFrequencia` (Unica/EntradaSaida) e `TipoPergunta` (validação por constante: texto/escolha/escala) — reais, sem switch espalhado | `Politica*.java`, `Frequencia*Strategy.java`, `TipoPergunta.java` |
| ROO-06 herança | `Usuario` ← `Participante`/`Organizador`; relação substituível justificável | `Usuario.java`, `Participante.java`, `Organizador.java` |
| ROO-07 interfaces | Portas `EventoRepository`, `InscricaoRepository`, `UsuarioRepository`, `QrCodeService` — contratos pequenos | `port/out/*`, `port/in/*` |
| ROO-08 SOLID | Inversão de dependência (use cases dependem de portas); SRP por use case; falta registrar 1 refatoração motivada | `application/usecase/*` |
| ROO-09 hexagonal | `domain/` sem imports de framework; `application/` sem anotações (fiação manual em `FabricaAplicacao`); JDBC isolado em `adapters/out`; HTTP e desktop em `adapters/in` | `Poo2Application.java`, `infra/FabricaAplicacao.java`, adapters |
| ROO-10 padrões | ① Strategy (inscrição + frequência) com efeito real; ② Ports & Adapters; candidato a 3º: Builder/Factory p/ `Questionario` ou Specification p/ elegibilidade — Task #2/#4 | — |
| ROO-11 erros de domínio | `DomainException` + `GlobalExceptionHandler` (400/404/422/500) + `StandardError` | `exception/*` |
| ROO-12 testes/refatoração | 30 arquivos / 102 testes JUnit 5 + Mockito (`mvn test`: 0 falhas); 4 refatorações registradas em `REFATORACOES.md` (R-01 `exigirSemConflito`, R-02 upsert + DELETE de órfãs, R-03 validação polimórfica em `TipoPergunta`, R-04 QR com validade + anti-duplicidade) | `src/test/**`, `REFATORACOES.md` |

**Dívida de modelagem (resolvida na migração Java SE):** pacotes legados
duplicados foram excluídos — `domain/` contém só `entity/`, `vo/`,
`port/` e `exception/`.

## 4. Decisões D-01–D-08 (rascunho p/ README — Task #5)

- D-01 desktop: **JavaFX** (`adapters/in/desktop/DesktopApp`, código puro sem FXML; DSK-00–DSK-51: login, gestão eventos/atividades/pessoas, programação, inscrições/agenda, frequência/QR/manual, avaliações, relatórios CSV, certificados PDF/email; mesmos use cases via `FabricaAplicacao`, `--headless` = só API). Ver `DECISOES.md`.
- D-02 API/auth: **Java SE + JWT (jjwt)** (migração Spring Boot → Java SE puro, commit `f465e46`, checkpoint `spring-final`; HTTP via `com.sun.net.httpserver`, hash via jBCrypt; contratos `/api/*` preservados).
- D-03 banco: **Postgres + JDBC puro + MigradorSql** (reproduzível V1–V10 via `migrations.txt`; alternativa JPA/Flyway da era Spring descartada na migração pelo custo de manter o container de DI).
- D-04 site: HTML/JS estático consumindo `/api/*` (mesma API/banco — RNF-02).
- D-05 QR: token opaco `inscricaoId+atividadeId` assinado, sem PII (RN-10); expiração a definir na Task #4.
- D-06 políticas: Strategy p/ inscrição e frequência; Specification p/ elegibilidade de certificado (quando D).
- D-07 padrões: Strategy + Hexagonal (efeito: variação sem `if/switch` espalhado).
- D-08 testes/seeds: JUnit 5 no domínio + seeds V3 (`senha123`, 2 eventos, 5 atividades).

## 5. Cenários CA-01–CA-08

| Cenário | Status |
|---------|--------|
| CA-01 publicação | ✅ (site + DSK-10/11/12/13) |
| CA-02 inscrição | ✅ (site + DSK-20) |
| CA-03 agenda/conflito | ✅ (motivo no `programacao.js` + DSK-21) |
| CA-04 frequência QR | ✅ (site + DSK-31) |
| CA-05 manual | ✅ (página frequência + DSK-32) |
| CA-06 avaliação | ✅ (site validado em produção + DSK-40/41/42) |
| CA-07 relatório | ✅ (CSV inscritos + presença + DSK-50) |
| CA-08 certificado | ✅ (PDF + e-mail + DSK-51; sem SMTP → 422 claro) |

## 6. Fila de execução — estado em 2026-09-22

1. Task #2 — avaliações persistidas ✅ (V4, repos, handler, escala, anti-duplicidade, consolidação; CASCADE fix validado).
2. Task #3 — inscrição/agenda ✅ (RN-04 no use case, DELETE cancelamento, motivo de conflito no frontend).
3. Task #4 — gaps ✅ backend + frontend (RF-03/05–09/22/29, RN-11/12/17; V6–V8). Restos conscientes: RF-13 regra por atividade, RN-20 fuso.
4. Task #5 — entregáveis §11 ✅ (README, DECISOES D-01–D-08, ENDPOINTS, seeds V3, esta matriz). Fase 4: esqueleto JavaFX ligado (login → eventos → detalhe → inscrever).
5. RF-13 vagas por atividade ✅ (V11, `maxVagas`, 422 lotada) + RNF-10 ✅ (V12 + projeção `InscricaoResumo`, 364ms) + escopo D ✅ (RF-32–35, PDFBox + SMTP).
6. Desktop Sem 6 ✅ DSK-00/10/11/12/30/31/32 + DSK-13/20/21/22 + DSK-40/41/42 + DSK-50/51 (jar rebuildado; RN-18 dono no handler). Site honesto ✅ (mocks removidos; `programacao.js`/`avaliacoes.js`/`evento-publico.js` ligados).
7. Resta: DSK-60/61/62 (paridade, docs, RNF-10 smoke desktop) + ensaio CA-01–CA-08 (Sem 7/8).
