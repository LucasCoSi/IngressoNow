# IngressoNow — Plataforma de Gestão de Eventos (POO2)

=======
| Integrantes |
| :--- |
| Lucas Costa |
| Johnathan Ramos |
| Samuel Antonio |

Plataforma de gestão de eventos acadêmicos e profissionais em **Java SE puro**,
com foco em **Programação Orientada a Objetos, arquitetura hexagonal, SOLID,
padrões de projeto, persistência, testes e integração entre aplicações**.

Organizadores cadastram eventos, atividades, locais, horários, trilhas e pessoas
vinculadas, publicam a programação e gerenciam inscrições, frequência, avaliações,
relatórios e certificados. Participantes criam conta, inscrevem-se, montam a
agenda, registram presença por QR Code (com alternativa manual) e avaliam
atividades quando elegíveis. Desktop (JavaFX), API e site compartilham o mesmo
banco PostgreSQL (RN-19).

Projeto da disciplina **Programação Orientada a Objetos II**, baseado em
`especificacao_requisitos_projeto_poo2_gestao_eventos_revisado_poo.pdf` v1.1.

---

## Objetivos

- aplicar conceitos de Programação Orientada a Objetos no núcleo avaliado em Java;
- representar o domínio com objetos de responsabilidades bem definidas;
- proteger invariantes por encapsulamento e validar com objetos de valor imutáveis;
- usar composição e polimorfismo nos pontos reais de variação (inscrição, frequência, perguntas);
- aplicar princípios SOLID e ao menos dois padrões com efeito demonstrável;
- separar domínio, aplicação, entrada e persistência por arquitetura hexagonal;
- expor uma API consumida pelo site e pelo desktop, com o mesmo banco de dados;
- persistir em banco relacional com esquema reproduzível e dados de demonstração;
- autenticar por JWT com senhas em hash apropriado e autorização no servidor;
- cobrir regras centrais com testes automatizados independentes da interface;
- documentar decisões (D-01–D-11), refatorações e rastreabilidade (RF × ROO).

---

## Funcionalidades

### Usuários e autenticação (RF-01/02/03)

- cadastro público de participante com nome, e-mail e senha (mínimo de 8 caracteres);
  e-mail duplicado rejeitado (RN-01), inclusive em condição de corrida;
- três perfis (§3): PARTICIPANTE, ORGANIZADOR (só os próprios eventos e
  inscritos) e ADMINISTRADOR (tudo); organizador e administrador **só nascem de
  uma conta administradora** (regra no núcleo, não só na tela) — o cadastro
  público não oferece perfil de gestão;
- login com JWT (`userId`) e mensagem genérica de credencial inválida (sem
  enumerar e-mails); rate limit em memória em login/cadastro (429 + `Retry-After`);
- participante consulta e atualiza os próprios dados; administrador redefine a
  senha de qualquer usuário (fluxo assistido do “Esqueceu a senha?”);
- páginas de Termos de Uso (`/termos`) e Política de Privacidade (`/privacidade`).

### Eventos, atividades e programação (RF-04–09)

- CRUD de eventos com ciclo RASCUNHO → PUBLICADO → ENCERRADO (RN-04);
- atividades de tipo livre (palestra, oficina, pôster...) com trilha, local e período (RF-05/06);
- conflito mesmo local + período sobreposto bloqueado com 422 e motivo (RF-07, RN-07);
- pessoas vinculadas com papel livre (palestrante/apresentador/responsável), foto opcional (RF-08, RN-02);
- programação filtrável por data, tipo, trilha e local — server-side (RF-09);
- site público, **sem login**, com catálogo, detalhe, grade e elenco reais (RF-10/11/12).

### Inscrições e agenda (RF-13–18)

- regra configurável por evento e por atividade (`maxVagas`, flags);
- controle de vagas com 422 ao esgotar (RN-06); cancelamento libera vaga (RF-15);
- seleção de atividades com agenda cronológica (RN-08) e conflito explicado (RN-07).

### Frequência (RF-19–23)

- critério por atividade: check-in único ou entrada+saída (Strategy, RN-09);
- QR opaco `ids+instante+salt` em Base64-URL, validade de **5 min**, sem PII (RN-10);
- `POST /checkin` aceita dono da inscrição OU organizador (CA-04);
- repetição no check-in único não duplica: 422 "Presença já registrada" (RNF-11);
- manual alternativo com `origem` + `responsavelId` = JWT (RN-11/12, CA-05);
- situação reproduzível a partir dos registros e da política (RF-23).

### Avaliações e relatórios (RF-24–31)

- questionários configuráveis (texto, escolha única, escala) com validação polimórfica;
- elegibilidade: inscrito + presença validada (RN-13); anti-duplicidade (RN-14);
- consolidação com média, distribuição e comentários (RF-28);
- relatórios de inscritos e frequência em CSV; XLSX/PDF gerados no navegador (RF-31);
- gráficos SVG reais (inscrições/semana, presença por situação); sem fonte = "—" honesto.

### Certificados — desejável (RF-32–35)

- critério configurável (% mínima + obrigatórias, RN-16);
- PDF baixável para elegíveis (inelegível vê justificativa, CA-08);
- envio por SMTP registrado; sem SMTP → 422 claro, download preservado;
- declaração para palestrante/apresentador refletindo o papel (RF-35).

### Configurações e parâmetros (D-11)

- `/configuracoes` com abas por papel: organizador vê regras, frequência e
  certificados dos próprios eventos; administrador vê os de qualquer evento;
- identidade da instituição (nome, sigla, contato, site) e percentual padrão de
  certificado persistidos no servidor em `configuracoes_organizacao` (V14);
- leitura liberada a gestores e escrita só do administrador (`PUT /api/configuracoes`);
- percentual padrão vira fallback real na elegibilidade (RF-32/RN-16) quando o
  evento não define critério; o nome da instituição alimenta o rodapé da navegação.

---

## Tecnologias utilizadas

### Backend e desktop

- **Java 25** (núcleo avaliado de POO — RNF-03), **Maven**
- **JDK HttpServer** (`com.sun.net.httpserver`, sem Spring)
- **JDBC puro** + **PostgreSQL 16**
- **JJWT** (JWT), **jBCrypt** (hash de senha — RNF-05)
- **JavaFX** (desktop, código puro sem FXML — D-01)
- **PDFBox** (certificados RF-33), **Jakarta Mail SMTP** (envio RF-34)
- **JUnit 5 + Mockito** (testes — RNF-12)

### Site

- **HTML/JS/CSS estáticos** servidos pelo próprio jar (System Design v2)
- `api-client.js` centraliza JWT (401/403 → login); sem acesso direto ao banco (RNF-02)

### Infraestrutura

- Docker e Docker Compose, `rodar.ps1` / `rodar.cmd` (sobe banco + app em um comando)
- `MigradorSql` + `migrations.txt` (V1–V14), seeds V3

---

## Arquitetura

Hexagonal (Ports and Adapters) — referência §8.1 do documento:

```
                ┌───────────────────────────────┐
                │        Adaptadores IN         │
                │  HTTP (JDK) │ JavaFX desktop  │  Site ──HTTP──▶ /api/*
                └──────────────┬────────────────┘
                               │ usa
                ┌──────────────▼────────────────┐
                │          APPLICATION          │
                │  24 use cases (orquestram o   │
                │  domínio exclusivamente       │
                │  através de portas)           │
                └──────────────┬────────────────┘
                               │ depende de
                ┌──────────────▼────────────────┐
                │         DOMAIN (puro)         │
                │  entity/  vo/  port/in|out    │
                │  exception/  (sem framework)  │
                └──────────────┬────────────────┘
                               │ implementado por
                ┌──────────────▼────────────────┐
                │         ADAPTERS OUT          │
                │  jdbc/ (8 repos)  security/  │
                │  pdf/ (PDFBox)  email/ (SMTP) │
                └──────────────┬────────────────┘
                               │ via
                ┌──────────────▼────────────────┐
                │  INFRA (AppConfig,            │
                │  ConnectionFactory,          │
                │  FabricaAplicacao, MigradorSql│
                └──────────────┬────────────────┘
                               ▼
                          PostgreSQL (único — RN-19)
```

Estrutura:

```
src/main/java/br/com/evento/
├── domain/model/entity/   Evento, Atividade, Inscricao, CheckIn, Pessoa,
│                          Questionario, Pergunta, Avaliacao, Politica*Strategy
├── domain/model/vo/       Email, Periodo, SenhaHash (imutáveis)
├── domain/port/           in/ (QrCodeService)  out/ (repositories, Email, Pdf)
├── application/usecase/   24 casos de uso (falam só com portas)
├── application/service/   SimpleQrCodeService (QR opaco, validade 5 min)
├── adapters/in/http/      handlers /api/*, JwtAuth, HttpErrors
├── adapters/in/desktop/   JavaFX (mesmos use cases, sem HTTP)
├── adapters/out/          jdbc/, security/ (BCrypt), pdf/, email/
└── infra/                 fiação manual (sem container DI)
src/main/resources/static/ site (HTML/JS por página) + db/migration/ (V1–V14)
```

Regra: `domain/` não importa framework; `application/` não importa adapter;
adapters convertem (DTO/record ↔ entidade) e delegam.

---

## Principais conceitos de POO aplicados

### Encapsulamento (ROO-02)

Validações nos construtores; listas expostas como imutáveis; mutação de
`Atividade` só por `definirPoliticaFrequencia`/`configurarLimiteDeVagas`/`remarcar`
(sem setter público da estratégia); `Questionario` congela após a 1ª resposta (RN-17).

### Composição (ROO-04)

`Evento` compõe `Atividade[]`; `Inscricao` compõe seleção + check-ins;
`AgendaDoParticipante` deriva de `Inscricao`. Herança só onde há relação
substituível: `Usuario` ← `Participante`/`Organizador` (ROO-06).

### Polimorfismo (ROO-05)

- `PoliticaInscricao`: `SemLimite` / `ComLimite` (derivada de `maxVagas`);
- `PoliticaFrequencia`: `Unica` / `EntradaSaida` (selecionada por configuração);
- `TipoPergunta`: cada constante valida seu tipo (texto/escolha/escala).
- Variação sem `if/switch` espalhado nos use cases.

### Interfaces e inversão de dependência (ROO-07/08)

Portas pequenas orientadas ao cliente (`EventoRepository`, `InscricaoRepository`,
`UsuarioRepository`, `QrCodeService`, `EmailService`, `PdfCertificadoService`);
use cases dependem das portas, JDBC/SMTP/PDFBox as implementam; um use case por
responsabilidade (SRP).

---

## Padrões e princípios

- **Strategy** — políticas de inscrição e frequência intercambiáveis com efeito
  real (troca por configuração, sem condicionais espalhadas);
- **Ports & Adapters (Hexagonal)** — domínio sem framework, substituição de
  infraestrutura por adaptadores (ex.: stub de e-mail nos testes);
- **SOLID** — coesão por use case, inversão de dependência nas fronteiras,
  extensível sem modificar regra existente (ex.: novo `TipoPergunta` = nova
  constante). Refatorações motivadas em `REFATORACOES.md` (R-01–R-04).

---

## Persistência

PostgreSQL com `MigradorSql` + manifesto `migrations.txt` (funciona no jar
sombreado), ordem garantida V1–V14:

```
V1  esquema inicial (usuários, eventos, atividades)
V2  inscrições            V3  seeds (Maria/Lucas, 2 eventos, 5 atividades)
V4  avaliações            V5  check-ins (RN-11)
V6  tipo/trilha           V7  origem/responsável do check-in (RN-12)
V8  pessoas (RF-08)       V9  detalhes do evento, fuso (RN-20), flags RF-13
V10 certificados (D)      V11 maxVagas por atividade      V12 índices (RNF-10)
V13 terceiro perfil: Maria → administradora + organizadora seed (Ana)
V14 parâmetros gerais da organização (D-11) + seed
```

Upsert por ID + DELETE só de órfãs (DELETE geral acionava `ON DELETE CASCADE`
e apagava respostas — bug real corrigido, R-02). Seeds em `SEEDS_DADOS_INICIAIS.md`.

---

## Segurança

- JWT com `userId`, expiração e segredo ≥ 256 bits; BCrypt com seeds compatíveis;
- autorização verificada **no servidor** (401 sem JWT, 403 em recurso alheio — RNF-06);
  criar organizador/administrador exige JWT de administrador, inclusive fora da tela;
- política única de senha (mínimo 8) no site, no desktop e na API;
- rate limit em memória no login (10/min) e no cadastro (5/10 min) → 429 + `Retry-After`;
- login responde sempre “Credenciais inválidas” (não revela se o e-mail existe) e
  e-mail duplicado em corrida vira 400 (UNIQUE do banco, não 500);
- SQL 100% parametrizado; entradas inválidas rejeitadas com mensagens claras (RNF-07);
- QR sem senha ou dado sensível; falhas internas retornam mensagem genérica
  sem expor SQL ou credenciais (RNF-14); coleta mínima de dados (RNF-08);
- “Esqueceu a senha?” → `/recuperar-senha`; redefinição assistida pelo admin em
  `/usuarios` (SMTP desligado no MVP, então não há e-mail automático).

---

## Desktop e site

- **Desktop JavaFX** (D-01, operador ORGANIZADOR): login, gestão de eventos/
  atividades/pessoas, programação com filtros, inscrições/agenda, frequência
  (QR com validade visível + manual com autoria), avaliações, relatórios CSV,
  certificados PDF/e-mail — mesmos use cases via `FabricaAplicacao` (RN-19).
- **Site**: catálogo público **antes do login**, programação, área do participante,
  gestão (`/inscricoes` com inscrição manual e exportação), `/configuracoes`
  (abas por papel: regras/frequência/certificados e parâmetros gerais persistidos
  no servidor — D-11) e `/relatorios` (gráficos SVG + CSV/XLSX/PDF). Páginas de
  apoio: `/termos`, `/privacidade`, `/recuperar-senha`. Detalhes em `BACKLOG_DESKTOP.md`.

---

## Testes

35 arquivos / **128 testes, 0 falhas** (`mvn test`), independentes da interface
(RNF-12, ROO-12). Cobrem:

```
Domínio: Evento, Atividade, Inscricao (seleção, conflito, agenda, anti-duplicidade),
  Avaliacao (elegibilidade), Pergunta (validação por tipo), CheckIn (origem/autoria),
  Pessoa, CriterioCertificado, VOs (Email, Periodo, SenhaHash)
Use cases (Mockito): inscrever/selecionar/cancelar, remarcar, vincular pessoa,
  check-in QR/manual (sucesso, inválido, duplicado), QR com validade e adulteração,
  submeter/consolidar avaliações, relatórios, certificados, configurações
  (leitura por papel, escrita do admin, validação de percentual), PDF real (%PDF)
```

Refatorações R-01–R-04 registradas em `REFATORACOES.md` (problema → alternativa → efeito).

---

## Documentação

```
ARQUITETURA.md              diagrama, modelo de domínio e mapa classe → RF/ROO
DECISOES.md                 D-01 desktop · D-02 API/auth · D-03 banco · D-04 site ·
                            D-05 QR/validade/duplicidade · D-06 políticas ·
                            D-07 padrões · D-08 testes/seeds · D-09 perfis ·
                            D-10 navegação · D-11 configurações por papel
ENDPOINTS.md                contratos /api/*, auth, papéis e exemplos curl
MATRIZ_RASTREABILIDADE.md   RF × ROO × implementação × teste, CA-01–CA-08, coluna desktop
BACKLOG_DESKTOP.md          DSK-00–DSK-62 (operador organizador)
REFATORACOES.md             R-01–R-04 com evidência de melhoria
SEEDS_DADOS_INICIAIS.md     massa de demonstração + SQLs de verificação
HISTORICO_EVIDENCIAS_S1_S7.md  incrementos semanais S1–S8 (§9)
SESSAO_2026-10-19.md        registro da sessão: configurações por papel + execução
```

---

## Como executar

### Pré-requisitos

JDK 25, Docker + Docker Compose (banco), PowerShell. Maven só para build/teste
(há distribuição local ou imagem Docker — ver abaixo).

### Via script (recomendado)

```powershell
.\rodar.cmd             # no cmd.exe — API + desktop
.\rodar.cmd --headless  # no cmd.exe — só API (CI/sem display)
.\rodar.ps1             # no PowerShell — API + desktop
.\rodar.ps1 --headless  # no PowerShell — só API
```

O script abre o Docker Desktop se preciso, sobe só o `postgres`, para o container
`poo2-app` (conflita na porta 8080 com o jar), aguarda o `pg_isready` e executa:

```powershell
java --enable-native-access=ALL-UNNAMED -jar target\plataforma-eventos-0.0.1-SNAPSHOT.jar
```

Se o PowerShell bloquear: use `.\rodar.cmd` (já chama com
`-ExecutionPolicy Bypass`) ou `powershell -ExecutionPolicy Bypass -File .\rodar.ps1`.
O script também confere as portas **5432** e **8080** antes de subir: se outro
projeto estiver usando, avisa e para em vez de esperar 90s; se a 8080 estiver com
uma instância do próprio app, mostra o `pid` para encerrá-la.

### Via Docker (app + banco em containers)

```bash
docker-compose up -d --build
docker logs poo2-app   # [Migrador] V1..V14 + [API] ouvindo em :8080/api
```

Variáveis (o compose já define): `DB_URL`, `DB_USER`, `DB_PASS`,
`JWT_SECRET`, `PORT`; SMTP (`SMTP_*`) vazio = envio desligado, download segue.

### Build e testes

```bash
# Com Maven local (ou a distribuição em ~/.m2/wrapper/dists):
mvn test      # 128 testes, 0 falhas (usa Postgres em localhost:5432)
mvn package   # shade ~22 MB com natives Windows do JavaFX

# Sem Maven: via imagem Docker (na raiz do projeto)
docker run --rm -v "%cd%:/app" -w /app maven:3.9.6-eclipse-temurin-17 mvn test
```

> `mvn package` com JDK 25 pode falhar no plugin `jar` antigo; nesse caso o
> jar sombreado é atualizado via `jar uf` com as classes de `target/classes`.

### Credenciais seed (V3 + V13)

- Administradora: `maria.santos@email.com` / `senha123`
- Organizadora: `ana.organizadora@email.com` / `senha123`
- Participante: `lucas.silva@email.com` / `senha123`

API em `http://localhost:8080/api` (contratos em `ENDPOINTS.md`).

---

## Cenários principais (demonstração §7/§11)

| Cenário | Fluxo demonstrado |
|---|---|
| CA-01 Publicação | Organizadora cria evento/atividades/pessoas, publica, aparece no site |
| CA-02 Inscrição | Visitante cadastra conta, inscreve-se, confirmação consultável |
| CA-03 Agenda | Participante escolhe atividades, conflito explicado, agenda cronológica |
| CA-04 Frequência QR | Elegível registra presença por QR válido; situação calculada pela política |
| CA-05 Manual | Organizador lança/corrige com autoria e horário preservados |
| CA-06 Avaliação | Inscrito+presente avalia; inelegível/duplicada bloqueados com motivo |
| CA-07 Relatório | Organizador consulta e exporta CSV/XLSX/PDF coerentes com os registros |
| CA-08 Certificado (D) | Elegível baixa PDF; inelegível vê justificativa |

Aceitação do núcleo: CA-01–CA-07 integrados no mesmo banco; desktop ↔ site com
a mesma massa de demonstração; caminho feliz + um erro relevante por cenário.

---

## Requisitos de qualidade (ROO-01–ROO-12)

| ROO | Evidência |
|---|---|
| ROO-01 Modelo rico | `Evento`, `Inscricao`, `Atividade`, `Questionario`, `Pessoa` concentram regras |
| ROO-02 Encapsulamento | construtores validam; coleções imutáveis; mutação por métodos nomeados |
| ROO-03 Objetos de valor | `Email`, `Periodo`, `SenhaHash` imutáveis com validação |
| ROO-04 Composição | agregados compõem; herança só em `Usuario` (ROO-06) |
| ROO-05 Polimorfismo | políticas de inscrição/frequência + validação por `TipoPergunta` |
| ROO-07 Interfaces | portas pequenas nas fronteiras e integrações |
| ROO-08 SOLID | DI manual por portas; SRP por use case; R-01–R-04 motivadas |
| ROO-09 Hexagonal | domínio sem framework; JDBC/HTTP/JavaFX isolados em adapters |
| ROO-10 Padrões | Strategy + Ports & Adapters (efeito real, sem ornamento) |
| ROO-11 Erros do domínio | `DomainException` → 400/404/422 + `StandardError`, sem vazar dados |
| ROO-12 Testes/refatoração | 128 testes sem interface + 4 refatorações registradas |

Rastreabilidade completa em `MATRIZ_RASTREABILIDADE.md`.

---

## Status do projeto

Núcleo obrigatório (RF-01–RF-31, CA-01–CA-07) implementado e integrado; desejáveis
(RF-32–35, CA-08) também entregues; social (RF-36, P) fora de escopo por decisão
(§2.3). `/configuracoes` agora é por papel e persiste no servidor (D-11), e a
suíte tem 128 testes. Dívidas declaradas: checagem de dono no handler (mover ao
use case), DSK-60/62 como polimento e envio de e-mail desligado no MVP
(recuperação assistida). Nada trava o núcleo.
