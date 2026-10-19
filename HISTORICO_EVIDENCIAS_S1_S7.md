# Histórico de evidências — IngressoNow (S1 a S8 + itens 9–16 §11.1)

## Visão geral

IngressoNow — gestão de eventos em Java SE puro, hexagonal, mesmo Postgres para API,
site e desktop (RN-19). Em 15/10/2026: 25 commits (20/08 a 06/10/2026) + trabalho
não commitado de endurecimento (QR com validade, anti-duplicidade, polimorfismo
real, site funcional, `rodar.ps1`), 30 arquivos de teste / 102 testes
(`mvn test`, 0 falhas), migrations V1–V12 + `migrations.txt`, login demo
`maria.santos@email.com` / `lucas.silva@email.com` (`senha123`). Sobe com
`docker-compose up -d --build` (`docker logs poo2-app` mostra `[Migrador] V1..V12` +
`[API] ouvindo em :8080/api`).

Fluxo ponta a ponta: criar evento RASCUNHO → publicar → aparece no site público →
cadastra/inscreve → escolhe atividades → agenda ordenada (barrando conflito com
motivo) → frequência por QR opaco ou manual com autoria → questionário só para
quem tem presença → consolidação (média 4.0 validada) → CSVs → certificado PDF
(download sempre; e-mail só com SMTP). CA-01 a CA-08 demonstráveis.

Decisões POO centrais: entidade rica com regra de verdade, VO imutável, uma única
herança (`Usuario` → `Participante`/`Organizador`), resto composição, Strategy de
vagas e de frequência, Ports & Adapters. Duas refatorações nascidas de bug real
(R-01 conflito centralizado em `exigirSemConflito`; R-02 upsert que parou de apagar
respostas no CASCADE). Migração Spring → SE no meio (commit `f465e46`, checkpoint
`spring-final`), `gitignore` que sumiu com `adapters/out`, build quebrado por um
`Label`, Docker parado e JavaFX sem pipeline até embutir os natives `*-win` +
`--enable-native-access=ALL-UNNAMED` no JDK 25.

---

## S1 — Domínio, responsabilidades e backlog (11 e 13/08/2026)

**Fonte:** seção 9, p. 13, semana 1 (11/08 e 13/08/2026). **Tipo:** Demonstração.
Desenvolvimento incremental e avaliação global · Sem prazo · Obrigatória.

**Código integrado:** repo nasceu de fato em 20/08 (`d9723f6` — domain, persistence,
subscriptions, attendance). Na semana 1 o trabalho foi modelo: entidades base
(`Evento`, `Atividade`, `Inscricao`, `Usuario`), VOs (`Email`, `Periodo`,
`SenhaHash`), glossário do domínio, mapa de responsabilidades e backlog DSK-00–62
(`BACKLOG_DESKTOP.md`). Domínio sem import de framework (ROO-09). Hexagonal desde
o dia 1: `domain/` puro, `application/` só contra portas, `adapters/` nas bordas.

**Execução:** `docker-compose up -d --build`; `GET /api/eventos` público retorna só
PUBLICADO/INSCRICOES_ABERTAS. Desktop ainda esqueleto nesta semana; site e API já
compartilham o mesmo banco (RN-19). Demo 2 min da época: Maria cria RASCUNHO →
publica → aparece em `/`.

**Caminho válido:** criar evento RASCUNHO → `POST /api/eventos/{id}/publicar` →
`GET /api/eventos` lista; detalhe em `/evento-publico?id=`. Fluxo CA-01 embrionário.

**Erro relevante:** e-mail duplicado no cadastro (RN-01, UNIQUE + `existePorEmail`
→ 422 com mensagem); publicar evento sem atividade ou com descrição < 10 chars →
422. Sem JWT em rota de gestão → 401.

**Teste:** `EventoTest` (ciclo de vida, publicar/encerrar), testes de VOs
(`EmailTest`, `PeriodoTest`, `SenhaHashTest`). Tudo sem HTTP — regra no domínio.

**Decisão de POO:** entidade rica vs. anemia — regra mora no objeto
(`Evento.publicar()` valida em vez de `if` no controller). Justifica ROO-01/ROO-02.

**Pendências e próxima meta:** persistência fina (JDBC + Migrador) e auth JWT.
Ficou para S2/S3.

**Prazo e horário:** sem prazo no AVA; apresentação em aula nos dias 11 e 13/08.

---

## S2 — Objetos, invariantes e persistência (18 e 20/08/2026)

**Fonte:** seção 9, p. 13, semana 2 (18/08 e 20/08/2026). **Tipo:** Demonstração.
Desenvolvimento incremental e avaliação global · Sem prazo · Obrigatória.

**Código integrado:** 7 repositórios JDBC em `adapters/out/jdbc/`, `MigradorSql` +
`migrations.txt` (V1–V5 nesta altura: schema inicial, inscrições, seeds,
avaliações, check-ins). Validação no construtor; listas expostas como
`unmodifiable`. Nasce `exigirSemConflito` (embrião da R-01).

**Execução:** criou evento/atividade, reiniciou o container (`down` + `up`), dado
continua lá — persistência real provada. `docker logs poo2-app` mostra V1..V5
aplicadas em ordem.

**Caminho válido:** `POST /api/eventos/{id}/atividades` com `tipo` livre
(`WORKSHOP`, `PALESTRA`, sem categoria fixa — RF-05) + `trilha` (RF-06); filtro em
`GET /api/eventos/{id}/programacao?data=&tipo=&trilha=&local=`.

**Erro relevante:** conflito mesmo local + período sobreposto → 422
(`DomainException` via `GlobalExceptionHandler`, corpo `StandardError`
`{timestamp,status,error,message,path}`); `DELETE /api/eventos/{id}` com inscritos
→ 422. Grade paralela em locais diferentes = permitida (decisão registrada).

**Teste:** `EventoTest`, `AtividadeTest`, `RemarcarAtividadeUseCaseTest`
(`deveBloquearRemarcacaoComConflito` — período original preservado em 422).

**Decisão de POO:** encapsulamento (ROO-02) + composição (ROO-04: `Evento` compõe
`Atividade`; `Inscricao` compõe seleção + check-ins). Herança evitada onde
composição é melhor (ROO-11 do §11.1).

**Pendências e próxima meta:** cadastro/login/autorização (S3).

**Prazo e horário:** sem prazo no AVA; aula em 18 e 20/08.

---

## S3 — Casos de uso, portas e autenticação (25 e 27/08/2026)

**Fonte:** seção 9, p. 13, semana 3 (25/08 e 27/08/2026). **Tipo:** Demonstração.
Desenvolvimento incremental e avaliação global · Sem prazo · Obrigatória.

**Código integrado:** ~21 use cases em `application/usecase/` falando só com portas
(`EventoRepository`, `InscricaoRepository`, `UsuarioRepository`, `QrCodeService`).
JWT com jjwt (claim `userId`), hash BCrypt via jBCrypt (compatível com seeds V3).
JWT saiu 01/09 (`e6f5451` era Spring Security 6) e a migração para SE dia 05/09
(`f465e46`) — andou um pouco da semana, mas o desenho (portas) não mudou e o
frontend não quebrou (contratos `/api/*` preservados).

**Execução:** `POST /api/auth/cadastro` → 201 `{token, usuarioId, nome, email}`;
`POST /api/auth/login` (maria/lucas + `senha123`) → 200 com JWT. `api-client.js`
centraliza o token (401/403 → redireciona para `/login`).

**Caminho válido:** cadastra Ana como PARTICIPANTE → login → `GET /api/usuarios/{id}`
próprio perfil 200; `PUT /api/usuarios/{id}` atualiza nome/senha.

**Erro relevante:** sem JWT → 401; `PUT` em perfil alheio → 403 ("próprios
eventos"/perfil alheio); login com senha errada → 401. Validado em Docker
05–06/09/2026.

**Teste:** `EditarUsuarioUseCaseTest`, `ListarEventosPublicadosUseCaseTest`,
`ListarEventosOrganizadorUseCaseTest` — tudo mockado, sem HTTP (ROO-12, RNF-12,
definição de pronto: regra central sem depender de interface).

**Decisão de POO:** inversão de dependência (ROO-08): use case depende de porta,
JDBC implementa. SRP por use case (um motivo para mudar cada).

**Pendências e próxima meta:** site público + inscrição integrada (S4).

**Prazo e horário:** sem prazo no AVA; aula em 25 e 27/08.

---

## S4 — Adaptadores, site público e inscrição (01 e 03/09/2026)

**Fonte:** seção 9, p. 13, semana 4 (01 e 03/09/2026). **Tipo:** Demonstração.
Desenvolvimento incremental e avaliação global · Sem prazo · Obrigatória.

**Código integrado:** handlers `/api/*` em `adapters/in/http/` (JDK
`com.sun.net.httpserver`), site estático em `src/main/resources/static/` (System
Design v2) e esqueleto JavaFX em `adapters/in/desktop/` (código puro, sem FXML —
D-01). Site vai por HTTP; desktop chama os use cases direto na `FabricaAplicacao`
(mesmo banco — nuance registrada no D-01: cumpre o hexagonal §8.1 item 3 como
adaptador de entrada, sem HTTP/JWT). Docker com `--headless` = só API (sem
display no CI).

**Execução:** fluxo CA-02 completo: `/cadastro` → `POST /api/auth/cadastro` →
`POST /api/inscricoes` → `POST /api/inscricoes/{id}/atividades/{atId}` →
`GET /api/inscricoes/{id}/agenda` ordenada. Criado no desktop aparece no site e
vice-versa (mesmo Postgres).

**Caminho válido:** Lucas inscreve-se na Conferência de Tecnologia 2026 (seed V3,
2 eventos PUBLICADO + 5 atividades) → escolhe "Palestra: O Futuro da IA" → agenda
cronológica via `AgendaDoParticipante`.

**Erro relevante:** evento fechado/duplicado/sem vaga → 422; conflito de agenda →
422 com motivo exibido em destaque no card (`programacao.js`, sem auto-dismiss);
`DELETE /api/inscricoes/{id}` → 204 e libera vaga (remoção física + CASCADE limpa
filhas; bloqueia se ENCERRADO).

**Teste:** `InscreverParticipanteUseCaseTest`, `SelecionarAtividadeUseCaseTest` (4
casos), `CancelarInscricaoUseCaseTest`, `InscricaoTest`.

**Decisão de POO:** DTO só na borda (`AtividadeDTO`, `InscricaoDTO`), regra nunca
no controller/handler (ROO-09). Conversão record ↔ entidade nos adapters.

**Pendências e próxima meta:** políticas de vaga configuráveis (S5).

**Prazo e horário:** sem prazo no AVA; aula em 01 e 03/09.

---

## S5 — Políticas de inscrição e agenda (08 e 10/09/2026)

**Fonte:** seção 9, p. 13, semana 5 (08/09 e 10/09/2026). **Tipo:** Demonstração.
Desenvolvimento incremental e avaliação global · Sem prazo · Obrigatória.

**Código integrado:** polimorfismo real — `PoliticaInscricao` (`SemLimite` /
`ComLimite`, derivada de `Evento.maxVagas`) + vagas por atividade (V11:
`Atividade.maxVagas`, `POST /atividades`, `AtividadeDTO`, enforcement em
`SelecionarAtividadeUseCase` via `contarPorAtividade`, RN-06). Flags
`exigeEscolhaAtividades` / `vagasPorAtividade` (V9) expostas em
`POST/PUT /api/eventos`. RNF-10: listagem com N+1 deu ~28s; projeção
`InscricaoResumo` + índice V12 baixou para 364 ms (medido em base com volume).

**Execução:** validado em base zerada (`docker-compose down -v && up --build`):
seleção OK 200; atividade lotada → 422 "sem mais vagas". Telas DSK-20/21/22
(inscrever/cancelar, agenda, gestão de inscritos).

**Caminho válido:** CA-02 + CA-03: inscreve → seleciona 2 atividades sem
sobreposição → `GET .../agenda` cronológica (deriva de `Inscricao`).

**Erro relevante:** acima do limite → 422 com alternativa; conflito RN-07 →
`DomainException` com motivo (mesmo texto no desktop e no `programacao.js`).

**Teste:** `SelecionarAtividadeUseCaseTest`, `AtividadeTest` (limite),
`InscricaoTest`. Cobrem lotação, duplicidade e conflito.

**Decisão de POO:** Strategy sem `if` espalhado (ROO-05, ROO-10) + R-02 do upsert
(`INSERT ... ON CONFLICT DO UPDATE` + `DELETE` só de órfãs — antes o `DELETE`
geral acionava `ON DELETE CASCADE` e apagava respostas/check-ins).

**Pendências e próxima meta:** frequência QR + manual (S6).

**Prazo e horário:** sem prazo no AVA; aula em 08 e 10/09 (`SESSAO_2026-09-08.md`
é a ata de planejamento desta semana).

---

## S6 — Frequência configurável (15 e 17/09/2026)

**Fonte:** seção 9, p. 13, semana 6 (15/09 e 17/09/2026). **Tipo:** Demonstração.
Desenvolvimento incremental e avaliação global · Sem prazo · Obrigatória.

**Código integrado:** `PoliticaFrequencia` (`FrequenciaUnicaStrategy` default /
`FrequenciaEntradaSaidaStrategy`, `Atividade.definirPoliticaFrequencia` como ponto
único de mutação — sem setter público, ROO-02), `QrCodeService`
/ `SimpleQrCodeService` (token opaco `inscricaoId:atividadeId:emitidoEm:salt` em
Base64-URL com **validade de 5 min**, sem PII — RN-10; R-04), `RealizarCheckInQrCodeUseCase`
+ `RealizarCheckInManualUseCase` (`CheckIn.Origem` QR_CODE/MANUAL + `responsavelId`
= JWT, V7, RN-11/12). Repetição no check-in único não duplica (RNF-11) e
`POST /checkin` aceita dono da inscrição OU organizador (CA-04). Telas
DSK-30 (critério) / DSK-31 (QR com aviso de validade) / DSK-32 (manual). Semana com Docker parado e fix
do `Label` que quebrava o build do shade.

**Execução:** fluxo CA-04/CA-05: `GET /api/controle/qrcode/{insc}/{at}` (text/plain)
→ `POST /api/controle/checkin` `{"qrCodeToken":"..."}` → 200 `{ok:true}` →
`Inscricao.calcularPresenca` delega à política → situação reproduzível.
Manual: `POST /api/controle/manual` com audit `MANUAL/maria` (validado em Docker
06/09).

**Caminho válido:** Maria gera QR da atividade → Lucas faz check-in → presença
calculada; organizador lança manual para quem esqueceu o celular.

**Erro relevante:** QR de atividade não escolhida → 422; repetição no check-in
único → 422 "já registrada" (sem duplicar, RNF-11); token expirado/adulterado →
422; trocar critério não reescreve histórico (RN-17 — documentado como recálculo
aceito para frequência, congelamento estrito só no questionário).

**Teste:** `RealizarCheckInQrCodeUseCaseTest`, `RealizarCheckInManualUseCaseTest`,
`CheckInOrigemTest` (RF-22/RN-11/RN-12).

**Decisão de POO:** invariante no `registrarCheckIn` (data/hora guardada, RN-11);
variação por composição + polimorfismo, não por `switch` na entidade.

**Pendências e próxima meta:** avaliações configuráveis + relatórios (S7).

**Demonstração:** 15/09/2026 (terça — dia de demonstração, §9), em aula e ao
vivo: Maria gera o QR da atividade → Lucas faz check-in → situação calculada
(`GET /qrcode` → `POST /checkin` → 200); na sequência, o erro relevante
(QR de atividade não escolhida → 422) e o manual com autoria
(`POST /manual`, audit `MANUAL/maria`); teste `CheckInOrigemTest` executado.
17/09 (quinta): orientação, sem nova demonstração.

**Prazo e horário:** sem prazo no AVA; aula em 15 e 17/09.

---

## S7 — Avaliações, relatórios e refatoração (22 e 24/09/2026)

**Fonte:** seção 9, p. 13, semana 7 (22/09 e 24/09/2026). **Tipo:** Demonstração.
Desenvolvimento incremental e avaliação global · Sem prazo · Obrigatória.

**Código integrado:** `Questionario` / `Pergunta` (`TEXTO_LIVRE`, `ESCOLHA_SIMPLES`
≥ 2 opções, `ESCALA_NUMERICA` ≥ 2) / `TipoPergunta` com validação **polimórfica de
verdade** (cada constante implementa `validar()`, `Pergunta` só delega — R-03,
ROO-05), `JdbcQuestionarioRepository` (V4), `AvaliacaoHandler`
(`POST/GET /eventos/{id}/questionarios`, congela após 1ª resposta via `congelar()`
+ `exigirEditavel()` — RN-17), `SubmeterAvaliacaoUseCase` (elegibilidade RN-13 +
anti-duplicidade RN-14 com UNIQUE), `ConsolidarAvaliacoesUseCase` +
`GET /api/questionarios/{id}/resultados` (`{totalAvaliacoes, media, distribuicao,
comentarios}`), CSVs (`GerarRelatorioInscritosUseCase`,
`GerarRelatorioPresencaUseCase`) e certificados D (PDFBox + SMTP, sem SMTP → 422
mas download segue). `evento-publico` sem mock (grade e elenco reais via
`GET .../pessoas`). Telas DSK-40/41/42 + DSK-50/51 (jar rebuildado 16/09).

**Execução:** fluxo CA-06/07/08: avalia → `media: 4.0` bate no `GET .../resultados`
→ CSV abre fora do sistema (`relatorios.js`) → elegível baixa `%PDF` na área do
participante.

**Caminho válido:** inscrito com presença responde escala 5 → consolidação mostra
média/distribuição/comentários; organizador exporta inscritos + presença.

**Erro relevante:** sem presença mínima → 422; duplicada → 422; questionário
congelado após 1ª resposta → 422; envio de e-mail sem `SMTP_*` → 422 claro. Tudo
com motivo (ROO-11).

**Teste:** `SubmeterAvaliacaoUseCaseTest`, `ConsolidarAvaliacoesUseCaseTest`,
`AvaliacaoTest`, `CriterioCertificadoTest` (4 casos), `PdfBoxCertificadoServiceTest`
(header `%PDF` + texto extraído), `GerarRelatorioInscritosUseCaseTest`,
`GerenciarCertificadosUseCaseTest`, stub de e-mail (registro + anexo) + teste de
rejeição sem SMTP.

**Decisão de POO:** Strategy + Hexagonal (ROO-10); R-01/R-02 antes/depois
detalhadas em `REFATORACOES.md` (ROO-08, ROO-12). Candidato a 3º padrão:
Specification para elegibilidade de certificado.

**Pendências e próxima meta (para S8):** ensaio geral CA-01–CA-08, DSK-60
(paridade desktop↔site) / DSK-62 (smoke RNF-10 no desktop). DSK-61 (docs) saiu em
22/09.

**Prazo e horário:** sem prazo no AVA; aula em 22 e 24/09.

---

## S8 — Integração, qualidade e entrega final (29/09 e 01/10/2026)

**Fonte:** seção 9, p. 13, semana 8 (29/09 e 01/10/2026). **Tipo:** Demonstração.
Desenvolvimento incremental e avaliação global · Sem prazo · Obrigatória.
**Status no AVA:** Entregue — Versão 1 em 25/09/2026 22:51 por Lucas Costa Silva
(Histórico 1; ainda aceita nova versão).

**Código integrado:** cenários CA-01 a CA-07 completos e integrados fim a fim no
mesmo banco; ROO-01 a ROO-12 rastreados em `MATRIZ_RASTREABILIDADE.md`; commits
pós-S7 (`8605423` evento-público honesto + docs §11, `54f5e4d` shell persistente
com sidebar/topbar fixas, `c0d26d8` tema `ingressonow.css` navy `#1E3A8A`/`#0F172A`
+ primário `#2563EB` aplicado via `DesktopApp#cena(...)`). Shell com `montarShell`
+ `navegar` (só o centro troca, com rolagem). `--headless` = só API no Docker.

**Execução:** roteiro de 2 min ensaiado: Maria publica (desktop DSK-10) → aparece
no site `/` → Lucas cadastra/inscreve (site) → escolhe atividades (agenda ordenada)
→ QR check-in (DSK-31) → avalia (escala) → resultado bate → CSV abre no Excel →
elegível baixa PDF. Mesmo massa de demo (seeds V3) nos dois lados.

**Caminho válido:** CA-01 publicação, CA-02 inscrição, CA-03 agenda/conflito com
motivo, CA-04 QR, CA-05 manual com autoria, CA-06 avaliação/consolidação, CA-07
relatórios CSV, CA-08 certificado (D, além do mínimo CA-01–CA-07).

**Erro relevante (demonstrado ao vivo):** 401 sem token, 403 em perfil/evento
alheio, 422 evento fechado/duplicado/lotado, 422 conflito com motivo no card, 422
QR de atividade não escolhida, 422 sem presença/duplicada/congelado, 422 e-mail
sem SMTP (download preservado). Corpo padronizado `StandardError`.

**Teste:** `mvn test` (local com Postgres em `localhost:5432`, ou via imagem
`maven:3.9.6-eclipse-temurin-17` sem Maven instalado): 28 arquivos, 0 falhas.
Regras centrais sem depender de interface (ROO-12, RNF-12, definição de pronto).

**Decisão de POO:** retrospectiva das 8 semanas — herança só onde substituível
(`Usuario`), Strategy onde há variação real (vagas, frequência), portas pequenas,
domínio sem framework. `REFATORACOES.md` prova que refatoração foi motivada por
bug/duplicação, não cosmética.

**Pendências honestas e próxima meta:** `/configuracoes` ainda com 3 trechos "em
desenvolvimento..."; métricas com "—" onde não há fonte no MVP; checagem de dono
ainda no handler (não dentro do use case — dívida anotada no D-01);
`setPoliticaFrequencia` público e `switch` no `validarResposta` funcionam mas dão
para refinar; DSK-60 (roteiro paridade) e DSK-62 (smoke RNF-10 desktop) como
polimento pós-entrega; RF-36 social fora de escopo por decisão. Nada disso trava
RF-01–RF-31 nem CA-01–CA-07.

**Prazo e horário:** S8 entregue 25/09 (antecipada); banca final na primeira
semana de outubro, data/horário a confirmar pelo docente.

---

## 9 — Código-fonte e histórico de contribuições (Link)

**Fonte:** seção 11.1, p. 17. **Tipo:** Link. Desenvolvimento incremental e
avaliação global · Sem prazo · Obrigatória. Entrega final na primeira semana de
outubro, data/horário a confirmar.

**Artefato:** repositório Git do projeto (núcleo Java SE + desktop JavaFX + API
HTTP JDK + site estático). Versão conferível: commit `c0d26d8` (06/10/2026)
"docs: atualiza DECISOES shell/tema + historico evidencias S1-S7" — 25 commits de
`d9723f6` (20/08) a `c0d26d8`, `git log --oneline` e `git status` limpo como prova.
Histórico de contribuições visível por autor no `git log`.

**Conteúdo:** RF-01 a RF-31 e CA-01 a CA-07 integrados (núcleo obrigatório M);
RF-32 a RF-35 desejáveis (D) também implementados (critério + PDF + e-mail +
palestrante); RF-36 opcional (P, social) declarado fora de escopo — desejáveis e
opcional não substituem pendências obrigatórias (não há pendência M aberta).

**Como conferir:** `git log --oneline -30`, `git show --stat HEAD`, árvore
`domain/`, `application/usecase/`, `adapters/in/` (http + desktop),
`adapters/out/` (jdbc, security, pdf, email), `src/main/resources/static/`.

---

## 10 — README e execução reproduzível (Link)

**Fonte:** seção 11.1, p. 17. **Tipo:** Link. Sem prazo · Obrigatória. Entrega
final na primeira semana de outubro.

**Artefato:** `README.md` na raiz.

**Conteúdo:** pré-requisitos (JDK 25, Docker, Postgres), configuração
(`DB_URL`, `DB_USER`, `DB_PASS`, `JWT_SECRET`, `PORT` — compose já define),
execução em um comando (`.\rodar.ps1` — sobe o banco, espera `pg_isready` e roda
o jar; `--headless` = só API; ou `docker-compose up -d --build`),
usuários de demonstração (`maria.santos@email.com` e `lucas.silva@email.com` /
`senha123`), objetivos, funcionalidades por RF, tecnologias, diagrama hexagonal,
POO aplicada, segurança, testes, cenários CA e tabela ROO (mesma linha do desafio).

**Como conferir:** seguir o README em outro computador — do `down -v` ao login
Maria/Lucas deve funcionar sem ajuste manual. Suficiente para reprodução por
terceiro.

---

## 11 — Modelo de domínio e arquitetura (Link)

**Fonte:** seção 11.1, p. 17. **Tipo:** Link. Sem prazo · Obrigatória. Entrega
final na primeira semana de outubro.

**Artefato:** `ARQUITETURA.md` (diagrama hexagonal + modelo + mapa de
responsabilidades), coerente com o código em `domain/model/entity/`,
`domain/model/vo/`, `domain/port/`, `application/usecase/`, `adapters/`,
`infra/` (`AppConfig`, `ConnectionFactory`, `FabricaAplicacao`, `MigradorSql`).

**Conteúdo:** evidencia ROO-01 (modelo rico: `Evento`, `Inscricao`, `Atividade`,
`Questionario`, `Pessoa`), ROO-02 (encapsulamento — com ressalva honesta do
setter público e `Pergunta/Resposta` pouco validada), ROO-03 (VOs `Email`,
`Periodo`, `SenhaHash`, `Horario`), ROO-04 (composição), ROO-05 (polimorfismo),
ROO-06 (herança só `Usuario`), ROO-07 (interfaces/portas), ROO-09 (hexagonal),
ROO-11 (erros de domínio). Sem impor herança quando composição for melhor —
tabela classe → RF/ROO no §3 do arquivo.

**Como conferir:** abrir o diagrama e navegar uma linha (ex.: `Evento.java:131`
composição, `Inscricao.java:111` seleção) — cada seta tem classe real.

---

## 12 — Decisões D-01 a D-08 e refatorações (Link)

**Fonte:** seção 11.1, p. 17. **Tipo:** Link. Sem prazo · Obrigatória. Entrega
final na primeira semana de outubro.

**Artefatos:** `DECISOES.md` (D-01 desktop JavaFX puro sem FXML; D-02 API/auth
Java SE + JWT; D-03 banco Postgres + JDBC + `MigradorSql`; D-04 site estático na
mesma API; D-05 QR opaco; D-06 políticas Strategy; D-07 padrões Strategy +
Hexagonal; D-08 testes/seeds) + `REFATORACOES.md` (R-01, R-02).

**Conteúdo:** cada decisão compara alternativas (ex.: D-01 CLI descartada por não
ser avaliável; D-02 JPA/Flyway descartados pelo custo do container DI; D-03 nomes
hardcoded que quebravam V6+; D-05 token eterno descartado — vazou uma vez =
check-in eterno — e JWT como QR descartado por misturar credencial com chamada).
Justifica ao menos dois padrões (Strategy com efeito real — variação sem `if`
espalhado — e Ports & Adapters com domínio sem framework) e registra **quatro**
refatorações relevantes com problema → alternativa → efeito (R-01
`exigirSemConflito(...,
ignorarId)`; R-02 upsert + `DELETE` só de órfãs; R-03 validação polimórfica em
`TipoPergunta`; R-04 QR com validade + anti-duplicidade + check-in do participante).
Cobre ROO-08, ROO-10 e ROO-12.

**Como conferir:** ler D-01–D-08 e cruzar com o código citado (ex.:
`DesktopApp`, `FabricaAplicacao`, `MigradorSql`, `SimpleQrCodeService`).

---

## 13 — Banco de dados e dados de demonstração (Link)

**Fonte:** seção 11.1, p. 17. **Tipo:** Link. Sem prazo · Obrigatória. Entrega
final na primeira semana de outubro.

**Artefatos:** `src/main/resources/db/migration/` (V1–V12 + `migrations.txt`,
`MigradorSql` aplica em ordem, funciona no JAR sombreado) +
`SEEDS_DADOS_INICIAIS.md` + `V3__insert_initial_data.sql`.

**Conteúdo:** script de criação/migração do relacional (Postgres) e seeds: 2
usuários (Maria ORGANIZADOR, Lucas PARTICIPANTE, `senha123` com hash BCrypt
válido), 2 eventos PUBLICADO, 5 atividades, 2 inscrições + agenda do Lucas.
Desktop e site compartilham a API e o banco (RN-19, RNF-02). Para RNF-10: índice
V12 + projeção `InscricaoResumo` (364 ms vs. ~28 s do N+1); base com pelo menos
500 participantes e 100 atividades é gerável para o smoke (procedimento no
D-08/DSK-62 — demo oficial usa seeds V3 + roteiro CA).

**Como conferir:** `docker-compose down -v && docker-compose up -d --build` →
`docker logs poo2-app` (V1..V12) → login Maria/Lucas → `GET /api/eventos`
retorna os 2 PUBLICADO com 5 atividades; SQLs de verificação na seção "Validação
dos Dados" do seeds doc.

---

## 14 — Documentação dos endpoints da API (Link)

**Fonte:** seção 11.1, p. 17. **Tipo:** Link. Sem prazo · Obrigatória. Entrega
final na primeira semana de outubro.

**Artefato:** `ENDPOINTS.md` — coleção/documentação da API compartilhada
(base `http://localhost:8080`, `Authorization: Bearer <JWT>`).

**Conteúdo:** como executar cada fluxo com `curl` e verificar autorização no
servidor: auth pública, usuários RF-03 (só próprio perfil, 403 alheio), eventos
RF-04, atividades RF-05/06/07 (+ remarcar), programação RF-09 (4 filtros
server-side), pessoas RF-08, inscrições RF-13/14/15/16, frequência RF-20/21/22,
avaliações RF-24–28, relatórios CSV RF-29/30/31, certificados D RF-32/33/34/35,
tabela de erros (`DomainException` → 422/404, `IllegalArgumentException` → 400,
401/403, corpo `StandardError`). Mapeamento desktop (D-01): cada DSK chama os
mesmos use cases via `FabricaAplicacao` direta — tabela DSK-00–DSK-51 no cabeçalho
do arquivo.

**Como conferir:** copiar qualquer bloco `curl` com o token do login e reproduzir
201/200/204 + o 422/401/403 correspondente.

---

## 15 — Testes automatizados (Link)

**Fonte:** seção 11.1, p. 17. **Tipo:** Link. Sem prazo · Obrigatória. Entrega
final na primeira semana de outubro.

**Artefato:** `src/test/**` (30 arquivos) + instruções no `README.md` (`mvn test`
local com Postgres em `localhost:5432`, ou via imagem Docker sem Maven).

**Conteúdo:** verifica comportamentos e falhas relevantes das regras centrais sem
dependência da interface (ROO-12, RNF-12, definição de pronto): domínio
(`EventoTest`, `AtividadeTest`, `InscricaoTest` — incluindo anti-duplicidade e par
entrada/saída, `AvaliacaoTest`, `PerguntaTest` por tipo,
`CriterioCertificadoTest`, `PessoaTest`, `CheckInOrigemTest`, VOs) + `SimpleQrCodeServiceTest`
(validade 5 min, adulteração) + use cases com Mockito (`EditarUsuario`,
`RemarcarAtividade`, `VincularPessoa`, `Inscrever/Selecionar/Cancelar`,
`CheckIn QR/manual` — incluindo duplicado, `Submeter/Consolidar`,
`Relatórios`, `Certificados`, `PdfBox` com `%PDF` real). 102 testes, 0 falhas.

**Como conferir:** `mvn test` (ou o `docker run ... mvn test` do README) e
`REFATORACOES.md §Cobertura` para o mapa teste → RF/RN.

---

## 16 — Matriz de rastreabilidade RF e ROO (Link)

**Fonte:** seção 11.1, p. 17. **Tipo:** Link. Sem prazo · Obrigatória. Entrega
final na primeira semana de outubro.

**Artefato:** `MATRIZ_RASTREABILIDADE.md` (gerada em 2026-09-04, atualizada em
2026-09-22 com desktop DSK-00–DSK-51 + site honesto; base: PDF-mestre v1.1).

**Conteúdo:** relaciona RFs e ROOs às responsabilidades, classes ou casos de uso
e ao teste ou demonstração correspondente, com status verificável (✅/⚠️/❌;
`✅ (D)` = desejável feito). Preserva M/D/P: RF-01–RF-31 núcleo obrigatório (M)
todos ✅, RF-32–RF-35 desejáveis (D) todos ✅, RF-36 social (P) ❌ fora de escopo
por decisão. Demonstra CA-01 a CA-07 (tabela §5, todos ✅, mais CA-08 certificado
como extra D), com atenção às RN (RN-04, RN-11/12, RN-17, RN-19, RN-20) e RNF
(RNF-02, RNF-10 com medição 28 s → 364 ms). Coluna Desktop diz qual DSK cobre cada
RF no JavaFX; §6 fila de execução mostra o que fechou por Task e o que resta
(DSK-60/62 + ensaio — polimento, sem RF-M aberto).

**Como conferir:** escolher qualquer RF (ex.: RF-13) e seguir a linha:
requisito → classe/endpoint → teste/demonstração Docker → tela desktop. Escolher
qualquer ROO (ex.: ROO-05) e achar as duas Strategies no código.

---

## Pós-S8 — endurecimento do site + D-05 fechada (15/10/2026)

**Fonte:** dívidas honestas do S8 + retorno de uso. **Tipo:** Correção/Evolução.

**Código integrado:** `/inscricoes` com filtro por evento real, Nova Inscrição
Manual (organizador, por e-mail) e Exportar Lista em CSV + modal no padrão do
site (sem `prompt` nativo); `/configuracoes` funcional (abas, upload logo/
assinatura com preview e validação, salvar/restaurar preferências, regras do
evento no servidor, rascunhos do organizador); `/relatorios` com gráficos SVG
reais (inscrições/semana, presença por situação) e exportação CSV (servidor) +
XLSX/PDF (gerados no navegador, ZIP/CRC e xref validados por harness node);
financeiro desabilitado com aviso honesto (§2.3). `rodar.ps1`: banco + app em
um comando (para container `poo2-app` na mesma porta, tolera stderr do compose
no PS 5.1). README reescrito na linha do desafio (objetivos → RFs → POO →
arquitetura → segurança → execução → CA → ROO).

**Execução:** `.\rodar.ps1` → `/inscricoes`, `/configuracoes`, `/relatorios` com
Ctrl+F5; `mvn test` 102/0.

**Caminho válido:** exportar XLSX abre no Excel; QR válido faz check-in como
participante; duplicado/expirado → 422 com motivo.

**Teste:** harnesses node em `Temp/opencode` (init + export: 7 downloads, ZIP
5 partes/CRC ok, xref íntegra); `PerguntaTest`, `SimpleQrCodeServiceTest`.

**Decisão:** nada de imagem no servidor sem endpoint — logo/assinatura como
preferência local com microcopy honesta; imagem de evento exigiria nova
migração + endpoint (oferta registrada, fora deste incremento).

## Três perfis §3 (15/10/2026, pós-S8)

**Fonte:** pergunta da banca ("deveria ter 3 perfis?") + RN-18 ("administrador
ou organizador autorizado"). **Tipo:** Evolução de autorização.

**Código integrado:** `Organizador` antigo (poder total) renomeado a
`Administrador`; `Organizador` novo restrito aos próprios eventos e inscritos;
`Evento.organizador` tipado como `Usuario` (admin também pode ser dono);
`Papeis` com `exigirAdministrador` / `exigirGestor` / `exigirDonoOuAdmin` /
`exigirDonoEventoOuAdmin`; `GET /api/usuarios` e DELETE só admin;
`GET /api/usuarios/buscar?email=` para o gestor (inscrição manual sem despejar
a base); cadastro de admin só por outro admin (403 senão); `ListarTodosEventosUseCase`
para o desktop do admin; V13 (Maria → admin + Ana organizadora seed).

**Execução:** Ana cria evento → 201; Ana em evento de Maria → 403 em PUT,
atividades, manuais, relatórios e questionários; Maria (admin) opera tudo;
`mvn test` 109/0.

**Caminho válido:** fluxo CA-01–CA-08 inalterado para os donos; cross-tenant
bloqueado com motivo.

**Teste:** `CadastrarUsuarioUseCaseTest` (3 perfis + inválido), `AdministradorTest`,
`CadastrarEventoUseCaseTest` (admin cria, participante não).

---

## O que ainda não está pronto (de verdade)

Para deixar claro que o projeto anda mas não acabou (honestidade vale ponto):

- Métricas com "—" onde não há fonte no MVP (NPS, financeiro §2.3);
  frequência e lotação agora com fonte real (CSV + gráficos).
- E-mail de certificado sem SMTP → 422 claro; download segue (falta validar com
  servidor real).
- Checagem de dono ainda no handler, não dentro do use case (dívida anotada D-01).
- Imagem de evento no cadastro (V13 + endpoint) como evolução ofertada.
- DSK-60/62 (paridade total + smoke RNF-10 desktop) e ensaio geral CA-01–CA-08
  como polimento da semana 8.
- RF-36 social fora de escopo por decisão.

## Para conferir rápido

Domínio em `domain/model/entity/`, use cases em `application/usecase/`, entrada
em `adapters/in/` (http + desktop), saída em `adapters/out/` (jdbc, security,
pdf, email), migrations em `src/main/resources/db/migration/`, site em
`src/main/resources/static/`, testes em `src/test` (`mvn test`). Demo 2 min:
Maria publica → Lucas inscreve → agenda → QR → avalia → CSV → PDF.
