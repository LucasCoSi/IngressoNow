---
name: validar-docker
description: Valida a aplicação Java SE em Docker — build, healthcheck, logs, testes manuais com credenciais seed
---

# Skill: validar-docker

Roda o ciclo completo de validação do IngressoNow (Java SE) em Docker, checando build, healthcheck do Postgres, logs da aplicação e endpoints principais com as credenciais seed.

**Usa esta skill após mudanças de infra/migração ou quando suspeitar que algo quebrou na API.**

## 1. Build e subida dos containers

```bash
docker-compose down -v
docker-compose up -d --build
```

- `down -v` limpa volumes (banco zerado → migrations V1..V5 vão rodar do zero).
- `--build` força rebuild da imagem (importante após mudar pom.xml ou código Java).

## 2. Healthcheck do Postgres

Aguarda até o Postgres estar pronto (healthcheck configurado no compose):

```bash
docker-compose ps
```

Verifica que `poo2-postgres` está `Up (healthy)`. Se demorar >30s ou ficar `unhealthy`, checar logs:

```bash
docker logs poo2-postgres
```

## 3. Logs da aplicação

```bash
docker logs -f poo2-app
```

**Sinais de sucesso:**
- `[Migrador] aplicada V1` ... `V5` (ou `baseline Flyway importado: V5` se banco já existia)
- `[API] ouvindo em http://localhost:XXXX/api`
- Sem stacktrace de `java.sql.SQLException`, `Connection refused`, `Flyway checksum mismatch`

**Armadilhas conhecidas (memória `sessao-set-2026-frontend-integration`):**
- **Flyway checksum mismatch:** migration editada após aplicada → `UPDATE flyway_schema_history SET checksum = ...` (gambiarra) OU criar V6 nova (correto).
- **Connection refused / Restarting loop:** Postgres não subiu ou app iniciou antes do healthcheck → aguardar ou `docker-compose restart app`.
- **Illegal character '﻿':** BOM no `.java` (PowerShell 5.1 `Set-Content -Encoding utf8` COM BOM) → remover BOM ou recriar arquivo.

Se logs pararem em erro, **não seguir** — diagnosticar primeiro.

## 4. Teste manual da API (credenciais seed)

**Credenciais válidas (V3):**
- Maria Santos (ORGANIZADOR): `maria.santos@email.com` / `senha123`
- Lucas Silva (PARTICIPANTE): `lucas.silva@email.com` / `senha123`

Hash seed de `senha123`: `$2a$10$TW7F2EXj42YabuptHEuSi.hj1KR.J/TrRz0WtjO6yE0UUhlgPKUMO`

### 4.1. Login (POST /api/auth/login)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"maria.santos@email.com","senha":"senha123"}'
```

Espera: `200 OK` + JSON com `{"token":"eyJ...","usuarioId":"...","nome":"Maria Santos","email":"...","tipo":"ORGANIZADOR"}`.

Se `401 Unauthorized` ou `500`: credenciais inválidas (checar hash na tabela `usuarios`) ou erro no `AutenticarUsuarioUseCase`/`BCryptHasher`.

### 4.2. Listar eventos publicados (GET /api/eventos — público)

```bash
curl http://localhost:8080/api/eventos
```

Espera: `200 OK` + array JSON (vazio se nenhum evento PUBLICADO; seeds V3 criam eventos em RASCUNHO).

### 4.3. Criar evento (POST /api/eventos — autenticado, ORGANIZADOR)

Usa o token do login de Maria:

```bash
curl -X POST http://localhost:8080/api/eventos \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN_MARIA>" \
  -d '{"titulo":"Teste Docker","descricao":"validacao","maxVagas":50}'
```

Espera: `201 Created` + JSON do evento criado.

Se `401`/`403`: JWT inválido ou expirado (relogar) ou Maria não é ORGANIZADOR (checar coluna `tipo` na tabela).
Se `422`: validação de domínio (título vazio, maxVagas < 0, etc.) — esperado se payload inválido.

### 4.4. Frontend (se existir static/)

Abrir no navegador: `http://localhost:8080/login.html` e `http://localhost:8080/cadastro.html`.

Testar login com Maria → redireciona para área correta + token no `sessionStorage`.

**Armadilhas frontend (memória):**
- Ordem dos `<script>`: `api-client.js` ANTES dos inline que usam `window.API`.
- PUT/DELETE exigem JWT: frontend tem `exigirLogin()` com redirect.
- 401/403 → `api-client.js` redireciona para `/login.html`.

## 5. Parar containers (preservando dados)

```bash
docker-compose stop
```

Próximo `up` reutiliza o banco (migrations já aplicadas não rodam de novo).

Para **limpar tudo** (banco zerado):

```bash
docker-compose down -v
```

## 6. Relatório de validação

Ao final, reportar:
- ✅ Build OK / ❌ Build falhou (+ erro)
- ✅ Postgres healthy / ❌ Unhealthy (+ logs)
- ✅ Migrations aplicadas (V1..V5) / ❌ Erro de migração
- ✅ API respondendo / ❌ Connection refused / Stacktrace
- ✅ Login seed OK (Maria/Lucas) / ❌ 401/500
- ✅ Endpoints principais OK / ❌ Erro (especificar)
- ✅ Frontend carrega (se testado) / ❌ 404 / JS error

**Se tudo ✅ → working tree pode ser commitado.**
**Se qualquer ❌ → diagnosticar antes de commitar.**
