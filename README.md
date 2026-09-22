# IngressoNow — Plataforma de Gestão de Eventos (POO2)

Projeto da disciplina Programação Orientada a Objetos II: gestão de eventos
com inscrições, agenda, frequência por QR Code, avaliações e relatórios.

## Arquitetura

Java SE puro (sem Spring), hexagonal:

```
domain/          entidades ricas, VOs, portas (sem imports de framework)
application/     use cases + services (fiação manual, sem DI container)
adapters/in/http API REST sobre com.sun.net.httpserver (JDK)
adapters/in/desktop  app JavaFX (Fase 4, D-01)
adapters/out/jdbc    persistência JDBC pura (Postgres)
adapters/out/security BCrypt via jBCrypt
infra/           AppConfig, ConnectionFactory, FabricaAplicacao, MigradorSql
```

Mesmo banco (Postgres) para desktop, API e site (RN-19). Domínio sem
dependência de framework (ROO-09); use cases dependem só de portas (ROO-08).

## Subindo com Docker

```bash
docker-compose up -d --build
docker logs poo2-app   # [Migrador] V1..Vn + [API] ouvindo em :8080/api
```

Variáveis (compose já define): `DB_URL`, `DB_USER`, `DB_PASS`,
`JWT_SECRET`, `PORT`. Sem Docker: `java -jar target/*.jar --headless`
(só API) ou sem flag (API + desktop JavaFX).

## Desktop JavaFX (DSK-00–DSK-51, operador ORGANIZADOR)

```bash
# Requer JDK 25 (natives Windows *-win já embutidos no shade, ~22 MB)
java --enable-native-access=ALL-UNNAMED -jar target/plataforma-eventos-0.0.1-SNAPSHOT.jar
# Só API (Docker/CI sem display):
java -jar target/*.jar --headless
# Sem Maven instalado: build via imagem Docker
docker run --rm -v "%cd%:/app" -w /app maven:3.9.6-eclipse-temurin-17 mvn package -DskipTests
```

Mesmos use cases via `FabricaAplicacao` direta (sem HTTP, RN-19/RNF-02);
mesmo Postgres da API/site. Telas: login/cadastro, Meus eventos
(Novo/Editar/Excluir/Publicar/Encerrar), atividades (Nova/Remarcar/
critério/pessoas), programação com filtros, inscrições/agenda,
frequência + QR + manual, avaliações, relatórios CSV, certificados
PDF/e-mail. Detalhe em `BACKLOG_DESKTOP.md`.

## Credenciais seed (V3)

- Organizadora: `maria.santos@email.com` / `senha123`
- Participante: `lucas.silva@email.com` / `senha123`

## Endpoints

Ver `ENDPOINTS.md` (contratos, auth, exemplos curl).

## Testes

```bash
# Local (requer Maven + Postgres em localhost:5432)
mvn test   # 28 arquivos / 93 métodos, 0 falhas

# Sem Maven: via imagem Docker (na raiz do projeto)
docker run --rm -v "%cd%:/app" -w /app maven:3.9.6-eclipse-temurin-17 mvn test
```

Regras centrais cobertas sem depender da interface (ROO-12);
refatorações registradas em `REFATORACOES.md`.

## Frontend

Páginas estáticas em `src/main/resources/static/` (System Design v2):
catálogo público em `/` + detalhe `/evento-publico?id=` (grade e elenco
reais, sem mock), programação com filtros + Adicionar à Grade/Exportar
Agenda/Grade-Lista, área do participante (perfil, ingressos, avaliações,
certificados PDF), frequência (check-in manual), avaliações (Criar
Pesquisa/Exportar NPS), relatórios (presença + inscritos). Sem fonte no
MVP = "—"/"a definir" honesto (ex.: métricas da frequência).

## Decisões de projeto (D-01–D-08)

Ver `DECISOES.md`.

## Rastreabilidade

`MATRIZ_RASTREABILIDADE.md` (RF × ROO, RNs, cenários CA-01–CA-08,
coluna Desktop DSK-00–DSK-51).
