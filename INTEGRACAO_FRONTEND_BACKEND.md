# Guia de Integração Frontend-Backend

## 📦 Arquivos JavaScript Criados

### 1. `api-client.js` - Cliente HTTP Central
**Caminho**: `src/main/resources/static/js/api-client.js`

**Responsabilidades**:
- Wrapper para Fetch API
- Tratamento centralizado de erros
- Parsear respostas `StandardError` do `GlobalExceptionHandler`
- Helpers para formatação de datas

**API Exposta**:
```javascript
window.API = {
    Eventos: {
        listar: () => Promise<Evento[]>,
        buscar: (id) => Promise<Evento>,
        criar: (data) => Promise<Evento>,
        atualizar: (id, data) => Promise<Evento>,
        deletar: (id) => Promise<void>
    },
    Inscricoes: {
        inscrever: (participanteId, eventoId) => Promise<Inscricao>,
        selecionarAtividade: (inscricaoId, atividadeId) => Promise<void>,
        consultarAgenda: (inscricaoId) => Promise<Agenda>
    },
    Controle: {
        realizarCheckIn: (qrCodeToken) => Promise<void>,
        gerarQrCode: (inscricaoId, atividadeId) => Promise<string>,
        relatorioPresenca: (eventoId) => Promise<string>
    },
    exibirErro: (error, containerId?) => void,
    exibirSucesso: (mensagem, containerId?) => void,
    formatarData: (isoString) => string,
    formatarDataHora: (isoString) => string
}
```

---

### 2. `eventos.js` - Listagem de Eventos (Admin)
**Caminho**: `src/main/resources/static/js/eventos.js`  
**Página**: `ingressonow-eventos.html`

**Funcionalidades**:
- ✅ Carrega eventos via `GET /api/eventos`
- ✅ Renderiza tabela dinâmica
- ✅ Filtro por status (Publicado, Rascunho, Encerrado)
- ✅ Ações: Visualizar, Editar, Excluir
- ✅ Contador de eventos

**Endpoints Consumidos**:
- `GET /api/eventos` → Lista todos os eventos

---

### 3. `evento-publico.js` - Detalhes do Evento (Público)
**Caminho**: `src/main/resources/static/js/evento-publico.js`  
**Página**: `ingressonow-evento-publico.html`

**Funcionalidades**:
- ✅ Carrega detalhes do evento via URL query param `?id={eventoId}`
- ✅ Exibe título, descrição, badges e meta info
- ✅ Botão de inscrição funcional
- ✅ Realiza inscrição via `POST /api/inscricoes`
- ✅ Redireciona para área do participante após sucesso

**Endpoints Consumidos**:
- `GET /api/eventos` → Busca evento por ID (filtro client-side)
- `POST /api/inscricoes` → Realiza inscrição do participante

---

### 4. `inscricoes.js` - Gestão de Inscrições (Admin)
**Caminho**: `src/main/resources/static/js/inscricoes.js`  
**Página**: `ingressonow-inscricoes.html`

**Funcionalidades**:
- ✅ Lista inscrições (dados mockados)
- ✅ Visualiza agenda do participante via `GET /api/inscricoes/{id}/agenda`
- ✅ Busca por nome, email, evento ou ID
- ✅ Ações: Ver Agenda, Enviar E-mail, Detalhes

**Endpoints Consumidos**:
- `GET /api/inscricoes/{id}/agenda` → Consulta agenda do participante

---

## 🎨 Arquivo CSS Criado

### `alerts.css` - Estilos para Alertas
**Caminho**: `src/main/resources/static/css/alerts.css`

**Classes**:
- `.alert-error` → Mensagens de erro (vermelho)
- `.alert-success` → Mensagens de sucesso (verde)
- Animação de slide-in
- Responsivo para mobile

---

## 🔌 Como Integrar nas Páginas HTML

### Estrutura de Integração

Adicionar no `<head>` ou antes de `</body>` de cada página:

```html
<!-- Container para alertas -->
<div id="error-container"></div>

<!-- API Client (obrigatório em todas as páginas) -->
<script src="/js/api-client.js"></script>

<!-- Script específico da página -->
<script src="/js/eventos.js"></script> <!-- ou evento-publico.js, inscricoes.js -->

<!-- Estilos de alertas -->
<link rel="stylesheet" href="/css/alerts.css">
```

---

## 📋 Páginas HTML a Serem Atualizadas

### ✅ Página: `ingressonow-eventos.html`
**Adicionar**:
```html
<div id="error-container"></div>
<script src="/js/api-client.js"></script>
<script src="/js/eventos.js"></script>
<link rel="stylesheet" href="/css/alerts.css">
```

### ✅ Página: `ingressonow-evento-publico.html`
**Adicionar**:
```html
<div id="error-container"></div>
<script src="/js/api-client.js"></script>
<script src="/js/evento-publico.js"></script>
<link rel="stylesheet" href="/css/alerts.css">
```

### ✅ Página: `ingressonow-inscricoes.html`
**Adicionar**:
```html
<div id="error-container"></div>
<script src="/js/api-client.js"></script>
<script src="/js/inscricoes.js"></script>
<link rel="stylesheet" href="/css/alerts.css">
```

---

## 🚀 Fluxo de Navegação Implementado

### 1. Visualizar Eventos
```
/eventos (Admin)
  ↓ [Carregar eventos via API]
  ↓ GET /api/eventos
  ↓ [Renderizar tabela]
  ↓ [Clicar "Visualizar"]
  ↓ Redirecionar para /evento-publico?id={eventoId}
```

### 2. Detalhes e Inscrição
```
/evento-publico?id=660e8400...
  ↓ [Carregar evento]
  ↓ GET /api/eventos (filtrar por ID)
  ↓ [Exibir detalhes]
  ↓ [Clicar "Realizar Inscrição"]
  ↓ POST /api/inscricoes
  ↓ {participanteId, eventoId}
  ↓ [Sucesso: Redirecionar /area-participante]
  ↓ [Erro: Exibir alerta com fieldErrors]
```

### 3. Consultar Agenda
```
/inscricoes (Admin)
  ↓ [Listar inscrições]
  ↓ [Clicar "Ver Agenda"]
  ↓ GET /api/inscricoes/{id}/agenda
  ↓ [Exibir modal com atividades]
```

---

## 🔥 Tratamento de Erros Implementado

### Exemplo: Erro de Validação (HTTP 400)
```javascript
// Resposta do GlobalExceptionHandler
{
    "timestamp": "2026-09-01T21:44:07.062Z",
    "status": 400,
    "error": "Validation Error",
    "message": "Um ou mais campos estão inválidos",
    "path": "/api/inscricoes",
    "fieldErrors": [
        {
            "field": "participanteId",
            "message": "O ID do participante é obrigatório"
        },
        {
            "field": "eventoId",
            "message": "O ID do evento deve ser um UUID válido"
        }
    ]
}
```

### Exibição na UI
```html
<div class="alert alert-error">
    <strong>Erro 400:</strong> Um ou mais campos estão inválidos
    <ul>
        <li><strong>participanteId:</strong> O ID do participante é obrigatório</li>
        <li><strong>eventoId:</strong> O ID do evento deve ser um UUID válido</li>
    </ul>
</div>
```

---

## 🧪 Testes Manuais

### Teste 1: Listagem de Eventos
1. Acessar `http://localhost:8080/eventos`
2. Verificar console: `[Eventos] 2 evento(s) carregado(s)`
3. Verificar tabela com 2 eventos dos seeds
4. Testar filtro por status

### Teste 2: Inscrição em Evento
1. Acessar `http://localhost:8080/evento-publico?id=660e8400-e29b-41d4-a716-446655440001`
2. Verificar título "Conferência de Tecnologia 2026"
3. Clicar em "Realizar Inscrição"
4. Verificar alerta de sucesso
5. Verificar redirecionamento para `/area-participante`

### Teste 3: Consultar Agenda
1. Acessar `http://localhost:8080/inscricoes`
2. Clicar em "Ver Agenda" na primeira inscrição
3. Verificar modal com 2 atividades selecionadas

### Teste 4: Tratamento de Erro
1. Abrir console do navegador
2. Executar: `API.Inscricoes.inscrever('', '')`
3. Verificar alerta de erro HTTP 400
4. Verificar lista de field errors

---

## 📌 Próximos Passos

### Funcionalidades Pendentes
1. **Criar Evento** (Admin)
   - Formulário modal
   - Validação client-side
   - POST /api/eventos (endpoint a criar)

2. **Editar Evento** (Admin)
   - Carregar dados no formulário
   - PUT /api/eventos/{id} (endpoint a criar)

3. **Listar Inscrições** (Backend)
   - Criar endpoint GET /api/inscricoes
   - Filtrar por evento
   - Substituir dados mockados

4. **Área do Participante**
   - Listar minhas inscrições
   - Minhas agendas
   - Selecionar atividades

5. **Autenticação JWT**
   - Remover participanteId hardcoded
   - Obter de sessão/token
   - Proteger rotas

---

**Integração Frontend-Backend Concluída! ✨**

As páginas principais agora consomem as APIs REST e exibem erros de validação amigavelmente.
