# IngressoNow — Decisões de Projeto (D-01–D-10)

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

## D-05 QR: token opaco com validade curta + anti-duplicidade (RN-10/RN-11/RNF-11)

`SimpleQrCodeService` gera `inscricaoId:atividadeId:emitidoEm:salt` em
Base64-URL (`VALIDADE = 5 min`); `decodificarToken` rejeita adulterado
("inválido ou corrompido") e expirado ("Código expirado. Solicite a
chamada atual."). Sem nome/e-mail/documento no payload (RN-10).
`POST /api/controle/checkin` aceita dono da inscrição OU organizador
(CA-04: participante registra a própria presença; manual continua só
organizador com `origem` + `responsavelId` = JWT, RN-12). Repetição no
check-in único não duplica: `Inscricao.registrarCheckIn` lança
"Presença já registrada" (RNF-11); `ENTRADA_SAIDA` admite o par.
Alternativa descartada: token eterno sem instante (vazou uma vez =
check-in eterno) e JWT como QR (misturaria credencial com chamada).

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

## D-09 cadastro/perfis: participante self-service, gestão criada por admin

O cadastro público (`POST /api/auth/cadastro` e telas de cadastro) cria **apenas
PARTICIPANTE**. Organizador e administrador exigem JWT de administrador — regra
no núcleo (`CadastrarUsuarioUseCase` recebe o solicitante), não só escondida na
tela; a interface de `/usuarios` **não oferece criar administrador**. Motivo:
auto-cadastro de organizador permitia a qualquer visitante publicar eventos.
O primeiro admin vem das seeds (V13). Alternativa descartada: aprovação de
organizador pendente (exigiria migration V14 + tela), adiada por custo/benefício.

Decisões associadas: política única de senha (mínimo 8 no site, desktop e API);
mensagem genérica de login ("Credenciais inválidas") para não enumerar e-mails;
e-mail duplicado em corrida → 400 via SQLState 23505 (antes 500); rate limit em
memória (login 10/min, cadastro 5/10 min → 429 + `Retry-After`); recuperação de
senha **assistida pelo admin** em `/usuarios` (página `/recuperar-senha`), porque
o SMTP está desligado no MVP — verificação de e-mail e auto-recuperação por link
ficam como trabalho futuro condicionado ao SMTP. Páginas legais `/termos` e
`/privacidade` publicadas.

## D-10 navegação: sessão preserva o papel e a sidebar de gestão não some

**Causa raiz do bug “sai da área do participante e vai para o index”:** em
`area-participante.js`, ao carregar/editar o perfil, a sessão era regravada com
`Auth.setUser({ usuarioId, nome, email })` — um objeto **sem `tipo`**. Na página
seguinte o `nav.js` lia `tipo` vazio e tratava organizador/admin como
**PARTICIPANTE**: o logo passava a apontar para `/` (index), a sidebar virava a
de participante e as páginas de gestão redirecionavam para `/area-participante`
(ou seja, não havia como voltar ao painel). Correção: `Auth.updateUser(patch)`
mescla os campos no usuário salvo (`...getUser(), ...patch`), preservando
`tipo`; os dois `setUser` de `area-participante.js` passaram a usar
`updateUser`. `evento-publico.js` já fazia `{ ...user, tipo }`.

**Sidebar:** nas páginas de participante (`/`, `/evento-publico`,
`/area-participante`) a sidebar do HTML é a do participante. Para organizador/
admin ela é **trocada** pela navegação de gestão reconstruída **idêntica à do
painel** — mesmos itens de `ITENS_GESTAO` (ícones via `<symbol>` injetados por
`garantirIcones`, já que essas páginas não têm `<defs>`), grupo
“Administração” e o mesmo rodapé. Assim existe **uma única barra** em todas as
telas, sem duplicar itens. Páginas de gestão não mudam (já têm `nav-dashboard`);
participante não muda. O menu de conta oferece “Painel de gestão”.

**Links corrigidos:** o estado vazio de `/area-participante` (“Ver eventos”) e o
pós-cadastro apontavam para `/eventos` (gestão); como o cadastro público só cria
PARTICIPANTE, o guard rebatia de volta e o clique parecia “não fazer nada”.
Agora apontam para `/` (catálogo público).
