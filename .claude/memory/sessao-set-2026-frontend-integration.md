---
name: sessao-set-2026-frontend-integration
description: Sessão de integração frontend-backend e debug Docker (set/2026) — ponto de retomada
metadata:
  type: project
---

# Sessão set/2026 — integração frontend + debug em Docker (PONTO DE RETOMADA)

**Branch:** master. **Últimos commits:** a1aae99, 4549757, 9e3af53, e6f5451.
**Caminho A mantido:** schema_ingressonow.sql é só referência (OneDrive), projeto usa Flyway V1..V3.

## Estado validado em Docker (2026-09-03, tudo funcionando)

- Login real no system design (login/cadastro chamam /api/auth). Credenciais seed:
  Maria (ORGANIZADOR) maria.santos@email.com / Lucas (PARTICIPANTE) lucas.silva@email.com, senha `senha123`.
- CRUD eventos: GET by id, POST, PUT (só RASCUNHO), DELETE (bloqueia 422 se há inscrições), POST /atividades, /publicar, /encerrar.
- GET /api/inscricoes?participanteId|eventoId com InscricaoDTO enriquecido (participanteNome, eventoTitulo).
- Páginas integradas: eventos, evento-publico, inscricoes, frequencia, area-participante (novo area-participante.js), programacao (novo), relatorios CSV (novo).
- Avaliações: NÃO implementado (SubmeterAvaliacaoUseCase é mock sem repository/controller) — decisão consciente.

## Armadilhas aprendidas (não repetir)

- **Nunca editar migration aplicada:** V3 foi editada (hash BCrypt) → Flyway checksum mismatch → app em Restarting/connection refused. Resolvido com UPDATE no flyway_schema_history. Próxima vez: criar V4.
- **Hash seed:** o hash antigo de "senha123" na V3 era inválido. Hash válido atual: `$2a$10$TW7F2EXj42YabuptHEuSi.hj1KR.J/TrRz0WtjO6yE0UUhlgPKUMO` (banco rodando atualizado manualmente + V3 corrigida).
- **PowerShell 5.1 `Set-Content -Encoding utf8` grava COM BOM** → javac falha (`illegal character: '﻿'`). Arquivos criados via shell precisam de remoção de BOM. [[powershell-bom]]
- **Ordem dos `<script>`:** api-client.js ANTES dos scripts inline que usam window.API/Auth (login/cadastro quebraram por isso).
- **GlobalExceptionHandler mascarava infra:** /favicon.ico e /error caíam no handler → 500. Fix: relança exceção para esses paths.
- **PUT/DELETE exigem JWT;** frontend tem exigirLogin() com redirect. Editar barra 422 em evento PUBLICADO (regra correta).
- **Sem mvn/docker local no assistente:** validação é via `docker-compose up -d --build` pelo usuário + logs.

## Working tree pendente de commit (13 arquivos, ~2026-09-03 noite)

EventoRestController (bloqueio delete), AtualizarEventoRequest (BOM fix), GlobalExceptionHandler (favicon/error), SecurityConfig (favicon/error), V3 (hash), login/cadastro reais, api-client (401/403), eventos.js (exigirLogin), 3 páginas + area-participante.js/programacao.js/relatorios.js/frequencia.js.

## Próxima frente sugerida

Botão/modal "criar evento" no frontend (POST /api/eventos pronto) para fechar o ciclo: criar RASCUNHO → editar → atividade → publicar → inscrever → encerrar.

**Why:** retomar sem reler tudo; contém credenciais, estado validado e armadilhas.
**How to apply:** ler este arquivo ao iniciar a próxima sessão; commitar o working tree primeiro.
