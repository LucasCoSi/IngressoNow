---
name: sessao-set-2026-migracao-java-se
description: Migração Spring Boot → Java SE puro (set/2026) — ponto de retomada atual
metadata:
  type: project
---

# Sessão set/2026 — migração Java SE puro (PONTO DE RETOMADA ATUAL)

**Branch:** master. **Tag:** `spring-final` = `c400b0e` (checkpoint: estado Spring validado antes da migração).
**Working tree atual:** migração Java SE em progresso, NÃO commitada (~40 arquivos: modified + deleted + untracked).

## O que foi feito na migração

- **pom.xml:** removido `spring-boot-starter-parent` e starters (web, validation, data-jpa, flyway). Agora Java 17 puro com: `postgresql:42.7.10`, `javafx-controls`/`javafx-fxml:17.0.6`, `jjwt-api/impl/jackson:0.12.6`, `jackson-databind:2.15.4`, `jbcrypt:0.4` (org.mindrot), JUnit 5 + Mockito.
- **Poo2Application.java:** sem Spring; monta `AppConfig` → `ConnectionFactory` → `MigradorSql.migrar()` → `FabricaAplicacao` → `ServidorApi.iniciar(porta)`. Flag `--headless` (só API, p/ Docker/web); sem flag imprime "desktop JavaFX ainda não ligado nesta fase" e também só sobe a API.
- **Deletado `adapters/in/web/` (Spring):** Auth, Avaliacao, ControlePresenca, Evento, Home, Inscricao controllers + dto/* (11 arquivos) + exception/* (2) + security/* (3) = 22 arquivos `D`.
- **Novo `adapters/in/http/` (10 arquivos, untracked):** `ServidorApi` (com.sun.net.httpserver do JDK, contexts /api/auth, /api/eventos, /api/inscricoes, /api/controle, /api/avaliacoes, /api/questionarios), `AuthHandler`, `EventoHandler`, `InscricaoHandler`, `ControleHandler`, `AvaliacaoHandler`, `JwtAuth` (jjwt, claim userId, valida Bearer), `Json` (jackson), `HttpErrors` + `StandardError`.
- **Novo `infra/` (4 arquivos, untracked):** `AppConfig` (lê `config.properties` + env DB_URL/DB_USER/DB_PASS/JWT_SECRET/PORT), `ConnectionFactory` (DriverManager puro), `FabricaAplicacao` (fiação manual — substitui DI do Spring: 5 repos JDBC + BCryptHasher + SimpleQrCodeService + 14 use cases), `MigradorSql` (substitui Flyway: tabela `schema_version`, aplica V1..Vn em ordem, importa baseline de `flyway_schema_history` p/ bancos da era Spring).
- **Novo `adapters/out/jdbc/` + `security/`:** `JdbcUsuario/Evento/Inscricao/Questionario/AvaliacaoRepository` + `BCryptHasher` (jBCrypt, hashes compatíveis com seeds V3 `$2a$...`).
- **Novo `src/main/resources/config.properties` (untracked):** db.url/user/pass, jwt.secret/expiration, server.port — sobrescrevível por env.
- **Domínio/application (modificados, só remoção de Spring):** use cases perderam `@Service`/`@Transactional`; `Autenticar/CadastrarUsuarioUseCase` trocaram `PasswordEncoder` (Spring) por porta `PasswordHasher`; `SimpleQrCodeService` perdeu `@Service`. Regra preservada.
- **Ajustes de domínio p/ JDBC:** `Evento.restaurarEstado(StatusEvento)` (reconstituição sem validar transição) e `Inscricao.registrarCheckIn` agora compara atividade por `getId().equals` (stream anyMatch) em vez de `contains` (equals por referência quebrava com objetos reidratados do JDBC) + null-check.

## Pendências conhecidas (não repetir / fazer a seguir)

- **Fase 4 pendente:** desktop JavaFX (D-01) ainda não ligado — `Poo2Application` só sobe a API nos dois modos.
- **Lixo da era Spring ainda no disco:** `src/main/resources/application.properties` (props spring.*) continua existindo — remover ou ignorar; `docker-compose.yml` ainda injeta `SPRING_DATASOURCE_*` (AppConfig lê `DB_URL/DB_USER/DB_PASS`) — atualizar compose; `Dockerfile` ENTRYPOINT `java -jar app.jar` sem `--headless` — adicionar flag no compose/ENTRYPOINT.
- **Validação pendente:** sem mvn/docker local no assistente — validar via `docker-compose up -d --build` pelo usuário + logs, como nas sessões anteriores. [[sessao-set-2026-frontend-integration]]
- **Armadilhas herdadas:** nunca editar migration aplicada (criar V6 em diante); PowerShell 5.1 `Set-Content -Encoding utf8` grava COM BOM → quebra javac; PUT/DELETE exigem JWT (frontend `exigirLogin()`).
- **Matriz RF×ROO:** ver `MATRIZ_RASTREABILIDADE.md` (Tasks #2/#3/#4, fila de execução) — a migração não muda RFs, só infra.

**Why:** retomar a migração sem reler o diff inteiro; contém tag de retorno, mapa do working tree e pendências Docker/JavaFX.
**How to apply:** ler este arquivo + [[sessao-set-2026-frontend-integration]] ao iniciar; validar com Docker pelo usuário; commitar a migração só após API SE responder.
