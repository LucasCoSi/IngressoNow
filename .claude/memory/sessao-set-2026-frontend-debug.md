---
name: sessao-set-2026-frontend-debug
description: Debug do frontend System Design (set/2026) — StaticHandler, nav, encoding, mocks
metadata:
  type: project
---

# Sessão set/2026 — debug do frontend (CONTEXTO ATUAL)

**Branch:** master. **Último commit:** `389964c` (gaps M, páginas órfãs, arquitetura, certificados).
**Working tree:** SUJO (não commitado) — StaticHandler, nav.js, fixes eventos/evento-publico, limpeza de mocks, tipo no login. Ver lista abaixo.

## O que foi feito nesta sessão

1. **StaticHandler (novo, untracked):** `adapters/in/http/StaticHandler.java` serve `static/` do classpath + 15 rotas amigáveis (`/login`, `/eventos`, `/programacao`...). Registrado como context `/` por último no `ServidorApi`. Motivo: migração Java SE nunca serviu estáticos (Spring fazia automático) — todas as telas davam 404.
2. **Login/cadastro System Design:** `/login` e `/cadastro` serviam páginas genéricas raiz; trocados para `ingressonow-login/cadastro.html`. `login.html`/`cadastro.html` raiz seguem órfãos (deletar ou ignorar).
3. **`tipo` no login (novos usuários):** `AuthHandler.resposta` retorna `tipo` (Organizador→ORGANIZADOR); `api-client.js` guarda na sessão; `evento-publico.js` usa `user.tipo` (fallback: busca perfil). Antes: heurística de e-mail da Maria.
4. **eventos.js:** botões por status (RASCUNHO→Editar/Excluir; PUBLICADO→Encerrar), botão Criar Novo Evento (RASCUNHO), data/local reais, Publicar/Encerrar via `postAcao`.
5. **evento-publico.js:** CTA por tipo (organizador→Gerenciar, participante→Inscrever); meta com período/local/fuso reais.
6. **nav.js (novo, untracked):** sidebar `.nav-btn` navega para rotas reais (protótipo só trocava CSS); injeta símbolos SVG faltantes (`i-eye`, `i-check`, `i-x`, `i-plus`, `i-mail`, `i-download`, `i-camera`); marca ativo pela URL. Incluído nas 12 páginas via `Edit`.
7. **no-store:** `StaticHandler` envia `Cache-Control: no-store` (evita página velha cacheada).
8. **Limpeza de mocks:** 5 `<tbody>` esvaziados (eventos, frequência, inscrições, atividades, usuários) + grade programacao + ranking/feedback avaliacoes + ticket area-participante → placeholders "Carregando...". Card feedback frequência começa escondido (`mostrarFeedback` exibe o real). Métricas chapadas (`1.320`, `1.842`, NPS 88) ainda no HTML de relatórios/dashboard/interface — pendente.
9. **Validados em Docker:** V1–V10 em base zerada; login tipo; admin usuarios (list 200 Maria, 403 Lucas); certificados CA-08 (0%→422, 33%→422, 67%→201); 80 testes OK.

## Armadilhas (não repetir)

- **NUNCA `Set-Content`/`Out-File` em HTML:** PowerShell 5.1 regrava UTF-8 como ANSI → mojibake (`ê`→`Ãª`) em 14 páginas. Só `Edit` em HTML. [[powershell-bom]]
- **`Write-Output` com `|` e aspas no PowerShell via ferramenta:** parser reclama; preferir scripts `.sh`/`.py` via Docker ou `Edit`.
- **curl.exe inline mangla aspas:** usar `-d @arquivo.json` com arquivos em `$CLAUDE_JOB_DIR/tmp`.
- **Classe `Atualizar*UseCase` não compila no Docker:** rename p/ outro nome resolveu (causa desconhecida). Evitar prefixo `Atualizar`.
- **`MigradorSql` lê `migrations.txt`:** ao criar V11+, acrescentar linha no manifesto.
- **Sessões antigas sem `tipo`:** fallback busca perfil; logout/login resolve.

## Pendente (próxima sessão)

- Métricas chapadas em relatórios/dashboard/interface (zerar ou ligar no `dashboard.js`).
- Deletar `login.html`/`cadastro.html` raiz órfãos (ou manter como fallback).
- `nav-link` da área do participante (Explorar/Validar Certificado com `href="#"`) — sem destino.
- RF-34 (e-mail), PDF binário de certificado, RF-36 social (P) — backlog consciente.
- Commitar working tree + push.

**Why:** retomar o debug do frontend sem redescobrir causas; contém fixes aplicados, pendências e armadilhas.
**How to apply:** ler este arquivo + [[sessao-set-2026-migracao-java-se]] ao iniciar; validar no navegador com Ctrl+Shift+R.

**Credenciais seed:** maria.santos@email.com (ORG) / lucas.silva@email.com (PART), senha `senha123`.
