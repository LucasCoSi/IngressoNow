---
name: sessao-set-2026-migracao-java-se
description: Migração Spring Boot → Java SE puro (set/2026) — ponto de retomada atual
metadata:
  type: project
---

# Sessão set/2026 — migração Java SE puro (PONTO DE RETOMADA ATUAL)

**Branch:** master. **Commits:** `f465e46` (migração Java SE), antes `c400b0e` (tag `spring-final`, checkpoint Spring).
**Working tree atual:** Task #4 em progresso, NÃO commitado (~20 arquivos: modified + untracked). Skill `validar-docker` salva em `.claude/skills/`.

## O que foi feito na migração (commit f465e46)

- **pom.xml:** sem `spring-boot-starter-parent`; Java 17 puro (`postgresql`, `javafx`, `jjwt`, `jackson`, `jbcrypt`, JUnit/Mockito). `maven-compiler-plugin` com `useIncrementalCompilation=false` (ver armadilhas).
- **Poo2Application:** `AppConfig` → `ConnectionFactory` → `MigradorSql` → `FabricaAplicacao` → `ServidorApi`. Flag `--headless` (Docker usa).
- **`adapters/in/http/`:** `ServidorApi` + `Auth/Evento/Inscricao/Controle/Avaliacao/UsuarioHandler` + `JwtAuth` + `Json` + `HttpErrors`.
- **`infra/`:** `AppConfig`, `ConnectionFactory`, `FabricaAplicacao` (fiação manual), `MigradorSql` (lê `migrations.txt`, aplica V1..Vn, baseline Flyway).
- **`adapters/out/jdbc/` + `security/`:** 5 repos JDBC + `BCryptHasher`.
- **Fixes validados:** `DELETE /api/inscricoes/{id}` (length 1); CASCADE em `JdbcQuestionario/EventoRepository` (upsert + DELETE de órfãs).
- **Docker:** compose com `DB_URL/DB_USER/DB_PASS/JWT_SECRET/PORT`; `Dockerfile` com `--headless`; sem `version` obsoleta.

## Task #4 — gaps (EM PROGRESSO, validado em Docker 2026-09-06)

- ✅ RF-18: `programacao.js` exibe motivo de conflito (422) em destaque no card, sem auto-dismiss (`exibirConflito`/`limparConflito`).
- ✅ RF-03: `EditarUsuarioUseCase` + `UsuarioHandler` (`GET/PUT /api/usuarios/{id}`, só próprio perfil, 403 alheio). Nome da classe é `EditarUsuarioUseCase` (ver armadilhas).
- ✅ RF-05/06: `Atividade.tipo` + `trilha` (domínio, V6, JDBC upsert, `EventoHandler` request/DTO, agenda DTO). `JdbcEvento/InscricaoRepository` toleram bancos pré-V6 (`temColuna`).
- ✅ RF-22: `CheckIn.Origem` (QR_CODE/MANUAL) + `responsavelId`; `registrarCheckInManual`; `RealizarCheckInManualUseCase`; `POST /api/controle/manual` (responsável = JWT logado); V7. JDBC tolera bancos pré-V7.
- ✅ RF-29: `GerarRelatorioInscritosUseCase` + `GET /api/controle/relatorio/inscritos/{eventoId}` (CSV).
- ✅ `MigradorSql` lê `db/migration/migrations.txt` (funciona no JAR; nomes hardcoded quebravam V6+). **Ao criar V8+, acrescentar linha no manifesto.**
- ❌ Aberto: RF-07 `remarcar` sem revalidação; RF-08 pessoas/papéis; RF-09 filtros server-side; frontend perfil (chamar PUT /usuarios), relatório inscritos (botão), check-in manual (botão), tipo/trilha (form + filtros).

## Pendências maiores

- **Fase 4:** desktop JavaFX (D-01) não ligado.
- **Task #5:** README, D-01–D-08, coleção de endpoints, seeds, matriz final.
- **Matriz RF×ROO:** ver `MATRIZ_RASTREABILIDADE.md` — atualizar statuses RF-03/05/06/22/29 (✅) após commit.

## Armadilhas (não repetir)

- **Classe `Atualizar*UseCase` não compila no Docker:** `cannot find symbol` persistente só p/ esse nome (rename p/ `EditarUsuarioUseCase` resolveu; causa raiz desconhecida). Evitar criar classes `Atualizar*`.
- **Nunca editar migration aplicada** (criar V8 em diante + linha no `migrations.txt`).
- **PowerShell 5.1 `Set-Content -Encoding utf8` grava COM BOM** → quebra javac. [[powershell-bom]]
- **curl.exe inline no PowerShell mangla aspas** → usar `-d @arquivo.json` com arquivos em `$CLAUDE_JOB_DIR/tmp`.
- PUT/DELETE exigem JWT; frontend tem `exigirLogin()`.

**Why:** retomar sem reler o diff; contém estado validado, pendências e armadilhas.
**How to apply:** ler este arquivo + [[sessao-set-2026-frontend-integration]] ao iniciar; commitar Task #4 após frontend.

**Credenciais seed:** maria.santos@email.com / lucas.silva@email.com, senha `senha123`.
