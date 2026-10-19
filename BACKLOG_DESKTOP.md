# Backlog — Aplicação Desktop (JavaFX) — IngressoNow

> Base: `especificacao_requisitos_projeto_poo2_gestao_eventos_revisado_poo.pdf` v1.1 (PDF-mestre).
> Contexto: `SESSAO_2026-09-08.md`. Backend + site já cobrem RF-01–RF-35 (§11 MATRIZ ✅).
> Gap: `adapters/in/desktop/DesktopApp.java` (145 linhas) só faz login → lista publicados → detalhe → inscrever-me.
> Data: 2026-09-16 (Semana 6 do §9). Prazo final: 1ª semana out/2026.

## Regras arquiteturais (não negociáveis)

- **D-01:** JavaFX código puro, sem FXML. Mesmo processo: `Poo2Application` sobe API + desktop; `--headless` = só API (Docker).
- **RN-19 / RNF-02 / §8.1:** desktop é **adaptador de entrada** — usa os **mesmos use cases via `FabricaAplicacao`** e o **mesmo Postgres** que a API/site. Não acessar JDBC direto, não duplicar regra no controller.
- **RNF-06:** autorização no use case/servidor, não só escondendo botão. Reusar `Papeis` / validações de dono.
- **Divisão de papéis (decisão):** desktop = **operador gestor (ORGANIZADOR restrito aos próprios eventos, ADMIN com todos)**; site = visitante/participante. Participante no desktop só para demonstração de paridade.
- **DoD por história (§10.2):** Implementado + Integrado (banco real) + Validado (caminho feliz + 1 erro) + Demonstrável (CA) + Sustentado por POO (regra no domínio, sem `if/switch` espalhado). Teste automatizado só quando há regra de domínio nova (regra já coberta no backend não precisa de reteste).

## E1 — Fundação (Sem 6, antes de tudo)

| ID | História | RF/RN/CA | Aceite |
|----|----------|----------|--------|
| DSK-00 | Shell desktop: login + cadastro + guarda de sessão + menu por papel | RF-01/02/03, RN-01/18 | Login `maria/lucas/senha123` entra; e-mail duplicado mostra msg clara; sem login telas de gestão rebatem p/ login; ORG vê gestão, PART vê visão simplificada |
| DSK-01 | Navegação base: Eventos (gestão) / Programação / Frequência / Avaliações / Relatórios | RNF-09 | Troca de tela sem restart; erro de use case aparece em `Alert`/label, sem stacktrace na tela (RNF-14) |

## E2 — Eventos e programação (CA-01: publicação)

| ID | História | RF/RN/CA | Aceite |
|----|----------|----------|--------|
| DSK-10 | CRUD + publicar/encerrar evento (título, descrição, período, local/modalidade, fuso, maxVagas, flags) | RF-04/13, RN-04/20, CA-01 | Criar RASCUNHO → aparece na gestão, **não** no site público; publicar → aparece em `/`; encerrar bloqueia inscrição; fuso IANA validado |
| DSK-11 | CRUD atividade (tipo livre, trilha, local, período, maxVagas) + detecção de conflito | RF-05/06/07/13, RN-03/07, CA-01 | Mesmo local + sobreposição = 422 com msg; locais diferentes em paralelo = permitido (decisão documentada); `maxVagas` por atividade (V11) |
| DSK-12 | Vincular pessoas com papel livre + listar elenco | RF-08, RN-02, CA-01 | Papel texto livre (palestrante/apresentador/responsável); sem foto não bloqueia; elenco visível no detalhe |
| DSK-13 | Programação com ≥3 filtros combináveis (data/tipo/trilha/local) | RF-09, CA-01 | Mesmos filtros do `GET /programacao`; resultado confere com o site |

## E3 — Inscrições, vagas e agenda (CA-02/CA-03)

| ID | História | RF/RN/CA | Aceite |
|----|----------|----------|--------|
| DSK-20 | Inscrever / cancelar inscrição + controle de vagas | RF-12/13/14/15, RN-05/06, CA-02 | Acima do limite = 422 + msg alternativa; cancelamento libera vaga; evento ENCERRADO bloqueia |
| DSK-21 | Agenda do participante (cronológica) + conflito | RF-16/17/18, RN-07/08, CA-03 | Agenda só com escolhas válidas ordenadas; conflito explica motivo (mesmo texto do `programacao.js`) |
| DSK-22 | Gestão de inscritos por evento/atividade (lista do organizador) | RF-29 parcial, RN-18 | ORG lista inscritos; PART só vê os próprios; 403 cruzado |

## E4 — Frequência (CA-04/CA-05) — foco da Semana 6 (15–17/09)

| ID | História | RF/RN/CA | Aceite |
|----|----------|----------|--------|
| DSK-30 | Configurar critério por atividade (check-in único / entrada+saída / manual) | RF-19, RN-09/17 | Troca de critério não reescreve histórico consolidado (avisar em tela) |
| DSK-31 | Exibir QR sem dado sensível + check-in por QR | RF-20/21/23, RN-10/11, CA-04 | Token `ids+salt` Base64, sem PII; registro persiste participante+atividade+data/hora+tipo; situação reproduzível |
| DSK-32 | Lançamento manual com autoria e correção rastreável | RF-22, RN-11/12, CA-05 | `origem=MANUAL + responsavelId=JWT`; correção preserva autoria/horário anterior visível |

## E5 — Avaliações (CA-06) — Semana 7 (22–24/09)

| ID | História | RF/RN/CA | Aceite |
|----|----------|----------|--------|
| DSK-40 | Criar questionário (título + perguntas texto/escolha única/escala) | RF-24/25, RN-15/17 | Congela após 1ª resposta; política de anonimato informada antes do envio |
| DSK-41 | Responder como elegível / bloquear inelegível com motivo | RF-26/27, RN-13/14, CA-06 | Só inscrito + presença validada avalia; duplicada = 422 ou edição conforme regra documentada |
| DSK-42 | Consolidar resultados (média, distribuição, comentários) | RF-28, CA-06 | Números conferem com `GET .../resultados` do site |

## E6 — Relatórios e certificados (CA-07/CA-08) — Semana 7

| ID | História | RF/RN/CA | Aceite |
|----|----------|----------|--------|
| DSK-50 | Relatórios inscritos + frequência + export CSV/PDF | RF-29/30/31, CA-07 | CSV abre fora do sistema; totais conferem com telas de gestão |
| DSK-51 | Certificados D: critério + PDF + envio + declaração palestrante | RF-32/33/34/35, RN-16, CA-08 | Elegível baixa `%PDF`; inelegível vê justificativa; sem SMTP = 422 claro + download preservado; declaração reflete papel |

## E7 — Endurecimento / entrega final — Semana 8 (29/09–01/10, §11)

| ID | História | RF/RN/CA | Aceite |
|----|----------|----------|--------|
| DSK-60 | Paridade desktop↔site (roteiro CA-01–CA-07) | §7, §11.2 | Mesmo massa de demo: criar no desktop aparece no site e vice-versa; roteiro de 2 min + testes executados em banca |
| DSK-61 | Docs: README (como rodar desktop), D-01, ENDPOINTS, MATRIZ coluna desktop | §11.1, D-01–D-08 | `mvn javafx:run` / `java -jar ...` sem flag; `--headless` documentado; matriz marca quais RF têm tela desktop |
| DSK-62 | RNF-10 smoke no desktop (500/100) | RNF-10 | Listas paginadas ou com filtro; sem travar; sem N+1 (reusar `InscricaoResumo`/projeção) |

## Ordem de execução sugerida

1. **Sem 6 (agora):** DSK-00 ✅ → DSK-10 ✅ (validado desktop real 2026-09-16) → DSK-11/12 ✅ → DSK-30/31/32 ✅ → DSK-13 ✅ + DSK-20/21/22 ✅ (jar 2026-09-16, falta teste real).
2. **Sem 7:** DSK-13/20/21/22 → DSK-40/41/42 ✅ (jar 2026-09-16, falta teste real) → DSK-50/51 ✅ (jar 2026-09-16, falta teste real).
3. **Sem 8:** DSK-51 (D) → DSK-60/62 (integração + RNF-10 smoke + ensaio). DSK-61 docs ✅ 2026-09-22 (README desktop/headless, D-01/D-08, ENDPOINTS, MATRIZ coluna desktop).

## Fora de escopo

- RF-36 social (P), app móvel nativo, recuperação de senha, pagamento/NF, alta disponibilidade (§2.3).
- Não reescrever regras de domínio — só chamar use cases existentes via `FabricaAplicacao`.
- Sem FXML, sem novo framework, sem acesso direto ao banco pela tela.

## Riscos (atualizado 2026-09-16 — corrigidos)

- ~~JavaFX no Docker/CI sem display~~ → mantido `--headless` (só API), desktop testado localmente.
- ~~`Poo2Application` cria `FabricaAplicacao` duplicada~~ → ✅ corrigido: `FabricaAplicacao(config, connections)` reusa a mesma `ConnectionFactory` do startup (compatível com `new FabricaAplicacao(config)` nos testes).
- ~~Autorização entre organizadores~~ → ✅ corrigido na API: POST usa o JWT como dono (body divergente = 403); PUT/DELETE/publicar/encerrar/atividades/pessoas exigem `exigirDonoEvento` (403 "próprios eventos"); desktop exibe dono (lista + detalhe). Dívida restante: mover a checagem de dono para dentro dos use cases (hoje é no handler, desktop futuro deve repetir a checagem).
