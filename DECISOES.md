# IngressoNow — Decisões de Projeto (D-01–D-08)

## D-01 desktop: JavaFX (código puro, sem FXML)

Mesmo banco + mesmos use cases via `FabricaAplicacao` direta (sem HTTP,
RN-19). Alternativa CLI descartada: cumpre RN-19 mas não demonstra
interface desktop avaliável. JavaFX roda no modo sem `--headless`;
Docker usa `--headless` (só API). Código puro em vez de FXML: menos
arquivos, sem risco de quebra no shade plugin. Natives Windows (`*-win`)
embutidos no shade — sem eles, `java -jar` falha com "no suitable
pipeline found". No JDK 25, rodar com `--enable-native-access=ALL-UNNAMED`
(validado 2026-09-16: login Maria OK, DSK-00/DSK-10).
Integração além do literal do §9-Semana-4 ("desktop consumindo a API"):
o desktop chama os **mesmos use cases/portas via `FabricaAplicacao`
direta** (hexagonal §8.1 item 3, adaptador de entrada), sem HTTP/JWT —
mesmas regras e mesmo banco (RN-19/RNF-02), com checagem de dono
repetida na tela (RN-18). Tema `desktop/ingressonow.css` (mesma
identidade do site: navy `#1E3A8A`/`#0F172A`, primário `#2563EB`),
aplicado via `DesktopApp#cena(...)` em todas as telas; login em
coluna dupla (marca + cartão) e principal com sidebar navy. Navegação
em shell persistente (`montarShell` + `navegar`): sidebar e topbar
fixas com item ativo destacado, só o centro troca (com rolagem).

## D-02 API/auth: Java SE + JWT (jjwt)

Migração Spring Boot → Java SE puro (commit `f465e46`, retorno em
`spring-final`). Motivo: disciplina exige Java SE; HTTP via
`com.sun.net.httpserver` (JDK), JWT via jjwt (claim `userId`),
hash via jBCrypt (compatível com seeds V3). Contratos `/api/*`
preservados — frontend não mudou na migração.

## D-03 banco: Postgres + JDBC puro + MigradorSql

`MigradorSql` minimalista + manifesto `migrations.txt` (funciona no JAR
sombreado; nomes hardcoded quebravam V6+). Baseline de
`flyway_schema_history` importado p/ bancos da era Spring. JDBC upsert
por ID + DELETE só de órfãs (DELETE geral acionava `ON DELETE CASCADE`
e apagava respostas/check-ins — bug real corrigido e validado).

## D-04 site: HTML/JS estático consumindo `/api/*`

Mesma API/banco (RN-19, RNF-02). `api-client.js` centraliza JWT
(401/403 → login). Filtros de programação client-side no MVP;
server-side em `GET /programacao` (RF-09).

## D-05 QR: token opaco sem PII (RN-10)

`inscricaoId:atividadeId:salt` em Base64. Sem nome/e-mail/documento.
`POST /checkin` decodifica, valida seleção e registra com data/hora
(RN-11). Manual alternativo com `origem` + `responsavelId` (RN-12).

## D-06 políticas: Strategy p/ inscrição e frequência

`PoliticaInscricao` (SemLimite/ComLimite, derivada de `maxVagas`) e
`PoliticaFrequencia` (Unica/EntradaSaida). Variação sem `if/switch`
espalhado (ROO-05, ROO-10).

## D-07 padrões: Strategy + Hexagonal (+ Ports & Adapters)

Efeito: domínio sem framework, use cases contra portas, JDBC isolado
em `adapters/out`. Candidato a 3º padrão: Specification p/
elegibilidade de certificado (D, pós-núcleo).

## D-08 testes/seeds: JUnit 5 no domínio + seeds V3

28 arquivos de teste, 93 métodos (`mvn test`), cobrindo domínio e use
cases (`src/test/**`: RF-03/07/08/13/14/22/29/32–35, RN-11/12, certificados
CA-08). Seeds V3: Maria (ORGANIZADOR) / Lucas (PARTICIPANTE),
`senha123`, 2 eventos PUBLICADO com 5 atividades. Refatorações
registradas em `REFATORACOES.md` (R-01 `exigirSemConflito`, R-02 upsert
+ DELETE de órfãs). Desktop validado em banco real (login Maria,
DSK-00/10; jar DSK-11–51 rebuildado 2026-09-16).
