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

## Pendente (próxima sessão) — atualização 2026-09-08 (PDF como mestre)

- ✅ Métricas chapadas resolvidas: `dashboard.js` reescrito p/ IDs reais da
  `interface.html` (inscritos, esgotados x/y, taxa, capacidade); `relatorios.js`
  com `atualizarMetricas` (inscritos, taxa, certificados, NPS "—");
  `inscricoes.js` pinta total/confimadas e "—" no resto; `avaliacoes.js` zera
  NPS/melhor; HTMLs com placeholder "—" inicial. Frequência segue sem fonte
  (validador só faz check-in) — placeholders honestos.
- ✅ Deletados `static/login.html`/`cadastro.html` raiz órfãos (rotas
  `/login`, `/cadastro` servem o System Design; sem referências restantes).
- ✅ `nav-link` da área do participante/evento-público: `nav.js` mapeia texto
  → rota real (`/eventos`, `/programacao`, `/area-participante`); "Ver todos"
  do dashboard → `/eventos`.
- ✅ Docs §11 sincronizados ao código Java SE: MATRIZ (D-02/D-03, RF-13
  parcial honesto, RN-20 fuso ✅, ROO-12 25 arqs/92 métodos), DECISOES D-08,
  ARQUITETURA (21 use cases, V1–V10), ENDPOINTS (+certificados RF-32/33/35),
  SEEDS (V1–V10, MigradorSql, fuso), REFATORACOES (92 métodos).
- RF-34 (e-mail), PDF binário de certificado, RF-36 social (P) — backlog consciente.
- RF-13 enforcement por atividade (`Atividade.maxVagas` + V11) — ✅ feito
  (working tree, sem commit): `Atividade` (`configurarLimiteDeVagas`,
  `podeAcomodar`), V11 + `migrations.txt`, `contarPorAtividade` (porta +
  JDBC), `AdicionarAtividadeUseCase` (overload), `EventoHandler`
  (validação + DTO) + agenda DTO, `SelecionarAtividadeUseCase` (422
  lotada quando `vagasPorAtividade`), `atividades.js` (prompt + coluna),
  `SelecionarAtividadeUseCaseTest` (4 casos) + `AtividadeTest` (2 casos),
  MATRIZ RF-13 ✅ + ENDPOINTS. **Validado em Docker 2026-09-08:**
  V1–V11 em base zerada, `mvn test` 86/86 OK (via imagem Maven),
  fluxo manual: atividade `maxVagas=1` → seleção 200 `{ok:true}` →
  2ª seleção 422 "não possui mais vagas disponíveis".
- ✅ RNF-10 validado em Docker 2026-09-08 (working tree, sem commit):
  massa temporária 500 usuários + 100 atividades + 500 inscrições +
  1500 seleções (scripts em `$TEMP/opencode/rnf10_*.sql`, removida após).
  Achados e correções: (1) V12 `idx_ia_atividade` (EXPLAIN confirma
  Bitmap Index Scan); (2) `GET /api/inscricoes` com N+1 (28s p/ 500) →
  memoização (5,8s) → projeção `InscricaoResumo` em 1 query com JOINs
  (**364ms**). Tempos finais: eventos 203ms, programação/100 1,18s,
  inscrições/500 364ms, relatório presenca 811ms. Base de demo limpa
  (só seeds).
- ✅ Escopo D validado em Docker 2026-09-08 (working tree, sem commit):
  RF-33 PDF binário real (`%PDF-1.4`, 1466 bytes, texto extraído com
  nome/código em `PdfBoxCertificadoServiceTest`); fluxo manual:
  critério 0% → emissão 201 → `GET .../pdf` 200 binário → área do
  participante lista e baixa. RF-34: `POST .../enviar` sem SMTP → 422
  com mensagem ("download preservado"); envio real requer servidor
  SMTP (declarado). `mvn test` 91/91. **Fix de higiene:** `.gitignore`
  `out/` genérico escondia `adapters/out/` + `port/out/` do git
  (pushes anteriores sem backend!) — ancorado p/ `/out/`, recomprometido.
- ✅ Menu de conta no `.topbar .profile` (2026-09-08, só `nav.js`, sem editar
  HTML): visitante vê "Visitante / Entrar" + menu "Fazer login" (/login) e
  "Não possui uma conta? / Cadastre-se" (/cadastro); logado vê nome+tipo
  reais (some o mock "Mariana Azevedo") + menu "Meus dados" e "Sair".
  Validado em Docker + harness Node (14/14: toggle, clique fora, Escape,
  destinos, logout). Correção: área do participante usa `.logo-brand` e
  `.profile-section` (não sidebar/topbar) — `nav.js` generalizado p/ as
  duas variantes + logo → `/dashboard` (harness v2 17/17).
- ✅ Modal de evento em `eventos.js` (2026-09-08, sem editar HTML):
  `prompt()`/`confirm()` → modal System Design (título, descrição, local,
  início/fim, vagas + validação inline + erro do servidor). Criar, editar
  (pré-preenche) e excluir (confirmação) no mesmo padrão. Harness Node
  20/20 + fluxo real via API (criar RASCUNHO 201 com detalhes, limpeza).
- ✅ Evento criado não listado (2026-09-08): `GET /api/eventos` só retorna
  PUBLICADO/INSCRICOES_ABERTAS → RASCUNHO sumia. Novo
  `ListarEventosOrganizadorUseCase` + `GET /api/eventos?organizadorId=`
  (JWT do dono; 401/403) + `eventos.js` usa gestão p/ ORGANIZADOR/ADMIN
  (participante/visitante seguem na pública). `mvn test` 93/93; E2E Docker:
  criar→gestão contém, pública não, 403/401 OK, base limpa. ENDPOINTS atualizado.
- ✅ Aviso "2 eventos em rascunho" fixo (2026-09-08): mock chapado no HTML
  (`management-notice`) → `atualizarAvisoRascunhos()` em `eventos.js`
  conta RASCUNHO reais (singular em 1, esconde em 0). Harness 4/4.
- ✅ Ações rápidas do dashboard mortas (2026-09-08): botões/cards sem
  handler em `dashboard.js` → rotas reais (scanner/presença→`/frequencia`,
  certificados/exportar→`/relatorios`, minicurso→`/atividades`; match por
  prefixo ASCII p/ tolerar mojibake; funciona mesmo se métricas falharem).
  Harness 9/9. Decisão usuário: seção "Ações Rápidas de Portaria" removida
  de `interface.html` (duplicava cards + sidebar); `dashboard.js` limpo
  (só cards + CTA minicurso).
- ✅ Dashboard multi-evento (2026-09-08): título mock fixo → seletor de
  evento (`dashboard.js`, lista de gestão p/ organizador, pública p/
  demais, escolha em localStorage); h1/descrição/métricas/vagas x/y/
  presença/grade passam a refletir o evento escolhido; grade mock
  (João Silva etc.) → atividades reais; vazio honesto. Harness 16/16.
- Commitar working tree + push.

**Why:** retomar o debug do frontend sem redescobrir causas; contém fixes aplicados, pendências e armadilhas.
**How to apply:** ler este arquivo + [[sessao-set-2026-migracao-java-se]] ao iniciar; validar no navegador com Ctrl+Shift+R.

**Credenciais seed:** maria.santos@email.com (ORG) / lucas.silva@email.com (PART), senha `senha123`.
