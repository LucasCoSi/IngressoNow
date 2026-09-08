# IngressoNow — Coleção de Endpoints (§11)

Base: `http://localhost:8080`. Auth: `Authorization: Bearer <JWT>`
(login em `/api/auth/login`). `GET /api/eventos` e
`GET /api/eventos/{id}/programacao` são públicos; resto exige JWT.

## Auth (público)

```bash
# Cadastro
curl -X POST /api/auth/cadastro -H 'Content-Type: application/json' \
  -d '{"nome":"Ana","email":"ana@email.com","senha":"senha123","tipo":"PARTICIPANTE"}'
# → 201 {token, usuarioId, nome, email}

# Login
curl -X POST /api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"lucas.silva@email.com","senha":"senha123"}'
# → 200 {token, usuarioId, nome, email}
```

## Usuários — RF-03 (JWT, só próprio perfil)

```bash
curl /api/usuarios/{id} -H "Authorization: Bearer $T"
# → 200 {id, nome, email, tipo}

curl -X PUT /api/usuarios/{id} -H "Authorization: Bearer $T" \
  -H 'Content-Type: application/json' -d '{"nome":"Novo Nome","senha":"nova123"}'
# → 200 perfil atualizado (campos opcionais; 403 em perfil alheio)
```

## Eventos — RF-04

```bash
curl /api/eventos                          # público, só PUBLICADO/INSCRICOES_ABERTAS
curl "/api/eventos?organizadorId={id}" -H "Authorization: Bearer $T"
# → gestão: todos os status do organizador (401 sem JWT, 403 em id alheio)
curl /api/eventos/{id}                     # público
curl -X POST /api/eventos -H "Authorization: Bearer $T" \
  -d '{"titulo":"...","descricao":"... (≥10 chars)","organizadorId":"...","maxVagas":50}'
# → 201 RASCUNHO
curl -X PUT /api/eventos/{id} -H "Authorization: Bearer $T" \
  -d '{"titulo":"...","descricao":"..."}'  # só RASCUNHO
curl -X DELETE /api/eventos/{id} -H "Authorization: Bearer $T"
# → 204 (422 se há inscrições)
curl -X POST /api/eventos/{id}/publicar -H "Authorization: Bearer $T"
curl -X POST /api/eventos/{id}/encerrar -H "Authorization: Bearer $T"
```

## Atividades — RF-05/06/07 (JWT p/ escrita)

```bash
curl -X POST /api/eventos/{id}/atividades -H "Authorization: Bearer $T" \
  -d '{"titulo":"...","inicio":"2026-09-10T09:00","fim":"2026-09-10T11:00",
       "local":"Auditório A","tipo":"WORKSHOP","trilha":"Backend"}'
# → 201 (422 se conflito mesmo local + período sobreposto)

# RF-13: vagas por atividade (só enforced com vagasPorAtividade=TRUE no evento)
curl -X POST /api/eventos/{id}/atividades -H "Authorization: Bearer $T" \
  -d '{"titulo":"...","inicio":"2026-09-10T09:00","fim":"2026-09-10T11:00",
       "local":"Lab 1","maxVagas":30}'
# → 201; POST /api/inscricoes/{id}/atividades/{atId} → 422 lotada (RN-06)

# RF-07: remarcar com revalidação
curl -X PUT /api/eventos/{id}/atividades/{atId} -H "Authorization: Bearer $T" \
  -d '{"inicio":"2026-09-10T10:00","fim":"2026-09-10T12:00","local":"Auditório A"}'
# → 200 (422 em conflito)
```

## Programação — RF-09 (público, filtros server-side)

```bash
curl '/api/eventos/{id}/programacao'
curl '/api/eventos/{id}/programacao?data=2026-09-10&tipo=WORKSHOP&trilha=Backend&local=Auditório%20A'
# filtros opcionais, case-insensitive; data no formato yyyy-MM-dd
```

## Pessoas — RF-08 (GET público; escrita JWT)

```bash
curl -X POST /api/eventos/{id}/atividades/{atId}/pessoas \
  -H "Authorization: Bearer $T" -d '{"nome":"Dra. Helena","papel":"PALESTRANTE"}'
# → 201 {id, atividadeId, nome, papel}
curl /api/eventos/{id}/atividades/{atId}/pessoas   # → 200 [...]
curl -X DELETE /api/eventos/{id}/pessoas/{pId} -H "Authorization: Bearer $T"  # → 204
```

## Inscrições — RF-13/14/15/16 (JWT)

```bash
curl -X POST /api/inscricoes -H "Authorization: Bearer $T" \
  -d '{"participanteId":"...","eventoId":"..."}'
# → 200 InscricaoDTO (422 se evento não publicado, sem vagas ou duplicada)
curl '/api/inscricoes?eventoId=...' -H "Authorization: Bearer $T"
curl '/api/inscricoes?participanteId=...' -H "Authorization: Bearer $T"
curl -X POST /api/inscricoes/{id}/atividades/{atId} -H "Authorization: Bearer $T"
# → 200 {ok:true} (422 com motivo em conflito de agenda — RF-18)
curl /api/inscricoes/{id}/agenda -H "Authorization: Bearer $T"
curl -X DELETE /api/inscricoes/{id} -H "Authorization: Bearer $T"  # → 204
```

## Frequência — RF-20/21/22, RN-11/12 (JWT)

```bash
curl /api/controle/qrcode/{inscId}/{atId} -H "Authorization: Bearer $T"
# → text/plain token opaco (ids + salt, sem PII — RN-10)
curl -X POST /api/controle/checkin -H "Authorization: Bearer $T" \
  -d '{"qrCodeToken":"..."}'                      # → 200 {ok:true}
curl -X POST /api/controle/manual -H "Authorization: Bearer $T" \
  -d '{"inscricaoId":"...","atividadeId":"..."}'  # → 200 {ok:true} (responsável = JWT)
```

## Avaliações — RF-24–28 (JWT)

```bash
curl -X POST /api/eventos/{id}/questionarios -H "Authorization: Bearer $T" \
  -d '{"titulo":"...","perguntas":[{"enunciado":"...","tipo":"ESCALA_NUMERICA",
       "obrigatoria":true,"escalaMax":5},{"enunciado":"...","tipo":"TEXTO_LIVRE"}]}'
# tipos: TEXTO_LIVRE, ESCOLHA_SIMPLES (exige ≥2 opções), ESCALA_NUMERICA (escalaMax ≥2)
curl /api/eventos/{id}/questionarios -H "Authorization: Bearer $T"
curl -X POST /api/avaliacoes -H "Authorization: Bearer $T" \
  -d '{"questionarioId":"...","inscricaoId":"...",
       "respostas":[{"perguntaId":"...","valor":"5"}]}'
# → 201 (422 se sem presença mínima RN-13 ou duplicada RN-14)
curl /api/questionarios/{id}/resultados -H "Authorization: Bearer $T"
# → {totalAvaliacoes, perguntas:[{totalRespostas, media, distribuicao, comentarios}]}
```

## Relatórios — RF-29/30/31 (JWT, CSV)

```bash
curl /api/controle/relatorio/presenca/{eventoId} -H "Authorization: Bearer $T"
# InscricaoID,ParticipanteID,AtividadeID,StatusPresenca
curl /api/controle/relatorio/inscritos/{eventoId} -H "Authorization: Bearer $T"
# InscricaoID,ParticipanteID,ParticipanteNome,EventoID,EventoTitulo,DataInscricao,AtividadesSelecionadas
```

## Certificados — RF-32/33/35 (D, JWT; escrita = organizador)

```bash
curl -X POST /api/eventos/{id}/certificados/criterio -H "Authorization: Bearer $T" \
  -d '{"percentualMinimo":75,"obrigatorias":[]}'
# → 200 critério configurado (RF-32)
curl -X POST /api/eventos/{id}/certificados/participante -H "Authorization: Bearer $T" \
  -d '{"inscricaoId":"..."}'
# → 201 certificado (422 se inelegível, com motivo — CA-08)
curl /api/eventos/{id}/certificados/{codigo}/pdf -H "Authorization: Bearer $T"
# → 200 application/pdf (RF-33, download; área do participante lista e baixa)
curl -X POST /api/eventos/{id}/certificados/enviar -H "Authorization: Bearer $T" \
  -d '{"certificadoId":"...","email":"aluno@email.com"}'
# → 200 registro do envio (RF-34; sem SMTP_* configurado → 422 claro, download segue OK)
curl -X POST /api/eventos/{id}/certificados/palestrante -H "Authorization: Bearer $T" \
  -d '{"pessoaId":"..."}'
# → 201 reflete papel exercido (RF-35)
curl /api/eventos/{id}/certificados -H "Authorization: Bearer $T"  # → 200 [...]
curl '/api/certificados?participanteId=...' -H "Authorization: Bearer $T"
curl '/api/certificados?codigo=...' -H "Authorization: Bearer $T"  # validação do identificador
```

## Erros

`DomainException` → 422 (404 se "não encontrad*"); `IllegalArgumentException`
→ 400; sem JWT → 401; perfil alheio → 403. Corpo: `{timestamp, status,
error, message, path}`.
