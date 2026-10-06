# Histórico de evidências — IngressoNow (S1 a S7)

## Visão geral

Nosso projeto é o IngressoNow, gestão de eventos em Java SE puro, hexagonal. Mesmo
Postgres pra API, site e desktop. Hoje tá com 22 commits (20/08 a 22/09), 93 testes
passando, migrations V1–V12 e login demo maria/lucas (senha123). Sobe com
`docker-compose up -d --build`.

Funciona de ponta a ponta: cria evento, publica, aparece no site, inscreve, monta
agenda (barrando conflito com motivo), frequência por QR ou manual com autoria,
questionário só pra quem tem presença, CSV e PDF de certificado. CA-01 a CA-08 ok.

De POO o grosso tá em: entidade com regra de verdade, VO imutável, uma herança só
(Usuario -> Participante/Organizador), resto composição, Strategy de vagas e de
frequência, e duas refatorações que nasceram de bug real (conflito centralizado e
upsert que parou de apagar resposta no CASCADE). Perrengues: migração Spring->SE no
meio, gitignore que sumiu com adapters/out, build quebrado por um Label, Docker parado
e JavaFX sem pipeline até embutir os natives. Falta pra final: ensaio geral, DSK-60/62
e SMTP real se der.

## S1 — Domínio e backlog (11 e 13/08)

Começamos atrasados, repo nasceu 20/08. Semana foi mais modelo: entidades base, VOs,
glossário e backlog DSK-00–62. Fluxo: criar evento rascunho -> publicar -> aparece no
público. Erro: e-mail duplicado e publicar sem atividade (422). Teste: EventoTest e VOs.
POO: hexagonal, domínio sem framework. Pendente: persistência fina e auth. Obrigatória,
sem prazo.

## S2 — Invariantes e persistência (18 e 20/08)

Virou código: 7 repos JDBC, Migrador V1–V5, validação no construtor. Prova: criou,
reiniciou, continua lá. Fluxo: atividade com tipo livre e filtro. Erro: conflito mesma
sala/horário (422) e excluir evento com inscrito. Teste: Evento, Atividade, Remarcar.
POO: regra no objeto, lista unmodifiable, nasceu o exigirSemConflito. Pendente: auth.
Obrigatória, sem prazo.

## S3 — Casos de uso e login (25 e 27/08)

Semana do login: 22 use cases só falando com porta, JWT, BCrypt. JWT saiu 01/09 e a
migração pra SE dia 05/09, andou um pouco da semana mas o desenho não mudou. Fluxo:
cadastro 201 com token, login 200. Erro: 401 sem token, 403 em perfil alheio. Teste:
EditarUsuario e listar eventos, tudo mockado sem HTTP. POO: inversão de dependência.
Pendente: site + inscrição. Obrigatória, sem prazo.

## S4 — Site, desktop e inscrição (01 e 03/09)

Semana cheia: handlers /api/*, site estático e esqueleto JavaFX sem FXML. Site vai por
HTTP, desktop chama os use cases direto na FabricaAplicacao, mesmo banco (nuance
registrada no D-01). Docker --headless = só API. Fluxo CA-02: cadastra -> inscreve ->
escolhe atividade -> agenda ordenada. Erro: evento fechado/duplicado/sem vaga 422,
conflito com motivo no card, DELETE 204. Teste: Inscrever, Selecionar, Cancelar. POO:
DTO só na borda, regra nunca no controller. Pendente: políticas de vaga. Obrigatória,
sem prazo.

## S5 — Inscrição e agenda (08 e 10/09)

Polimorfismo de verdade: PoliticaInscricao (Sem/ComLimite) + vagas por atividade (V11).
RNF-10: listagem com N+1 deu 28s, projeção InscricaoResumo baixou pra 364ms (V12).
Validado em base zerada: lotada volta 422 "sem mais vagas". Teste: Selecionar (4),
Atividade, Inscricao. POO: Strategy sem if espalhado + R-02 do upsert. Pendente:
frequência. Obrigatória, sem prazo.

## S6 — Frequência (15 e 17/09)

Foco em presença: PoliticaFrequencia (única / entrada+saída), QR opaco sem PII, manual
com responsavel = JWT, telas DSK-30/31/32. Teve Docker parado e fix do Label que
quebrava o build. Fluxo CA-04/05: gera QR -> check-in 200 -> situação reproduzível.
Erro: QR de atividade não escolhida 422, trocar critério não reescreve histórico.
Teste: QR, manual, CheckInOrigem. POO: invariante no registrarCheckIn. Pendente:
avaliações e relatórios. Obrigatória, sem prazo.

## S7 — Avaliação, relatório e refatoração (22 e 24/09)

Fechamento: questionário configurável (congela após 1ª resposta), elegibilidade +
anti-duplicidade, consolidação (media 4.0 validada), CSVs e certificados D (PDF + SMTP,
sem SMTP dá 422 mas download segue). Evento-publico sem mock. Fluxo CA-06/07/08:
avalia -> resultado bate -> CSV abre fora -> elegível baixa PDF. Erro: sem presença,
duplicada e congelado, tudo 422 com motivo. Teste: Avaliacao, Consolidar, Relatórios,
Criterio, PDF. POO: Strategy + Hexagonal; R-01/R-02 antes/depois no REFATORACOES.
Falta (sem 8): ensaio CA-01–CA-08, DSK-60/62. Obrigatória, sem prazo.

## O que ainda não tá pronto (de verdade)

Pra deixar claro que o projeto tá andando mas não acabou:

- `/configuracoes` com 3 trechos "em desenvolvimento..." — página sem ação ainda.
- Métricas com "—" (frequência, lotação, NPS) — sem fonte no MVP, mostramos traço honesto.
- Envio de certificado por e-mail sem SMTP configurado volta 422 — falta validar com servidor real.
- Checagem de dono ainda no handler, não dentro do use case (dívida anotada).
- `setPoliticaFrequencia` público e `switch` no validarResposta — funciona, mas dá pra refinar.
- Falta DSK-60/62 (paridade desktop x site + smoke RNF-10) e o ensaio geral CA-01–CA-08 da sem 8.
- RF-36 social fora de escopo por decisão.

## Pra conferir rápido

Domínio em `domain/model/entity/`, use cases em `application/usecase/`, entrada em
`adapters/in/` (http + desktop), saída em `adapters/out/` (jdbc etc.), migrations em
`resources/db/migration/`, site em `resources/static/`, testes em `src/test` (`mvn test`).
Demo 2 min: Maria publica -> Lucas inscreve -> agenda -> QR -> avalia -> CSV -> PDF.
