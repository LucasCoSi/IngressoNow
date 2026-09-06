/**
 * API Client - IngressoNow
 * Cliente centralizado para consumo dos endpoints REST
 */

const API_BASE_URL = '/api';

/**
 * Gerenciamento de Token JWT
 */
const Auth = {
    TOKEN_KEY: 'jwt_token',
    USER_KEY: 'user_data',

    setToken(token) {
        localStorage.setItem(this.TOKEN_KEY, token);
    },

    getToken() {
        return localStorage.getItem(this.TOKEN_KEY);
    },

    removeToken() {
        localStorage.removeItem(this.TOKEN_KEY);
        localStorage.removeItem(this.USER_KEY);
    },

    setUser(userData) {
        localStorage.setItem(this.USER_KEY, JSON.stringify(userData));
    },

    getUser() {
        const userData = localStorage.getItem(this.USER_KEY);
        return userData ? JSON.parse(userData) : null;
    },

    isAuthenticated() {
        return !!this.getToken();
    },

    logout() {
        this.removeToken();
        window.location.href = '/login';
    }
};

/**
 * Configuração padrão para requisições Fetch
 */
function getHeaders() {
    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
    };

    const token = Auth.getToken();
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    return headers;
}

/**
 * Classe de erro customizada para respostas da API
 */
class APIError extends Error {
    constructor(message, status, fieldErrors = null) {
        super(message);
        this.name = 'APIError';
        this.status = status;
        this.fieldErrors = fieldErrors;
    }
}

/**
 * Handler centralizado de erros da API
 */
async function handleAPIError(response) {
    // Se status 401, redireciona para login
    if (response.status === 401) {
        Auth.logout();
        throw new APIError('Sessao expirada. Faca login novamente.', 401);
    }

    // 403 quase sempre = sem token ou token invalido no MVP
    if (response.status === 403) {
        throw new APIError('Acesso negado. Faca login para continuar.', 403);
    }

    let errorData;
    try {
        const text = await response.text();
        errorData = text ? JSON.parse(text) : {};
    } catch (e) {
        throw new APIError(
            'Erro ao processar resposta do servidor',
            response.status
        );
    }

    // Busca mensagem de erro nos diferentes formatos
    const message = errorData.erro || errorData.message || 'Erro desconhecido';
    const fieldErrors = errorData.detalhes || errorData.fieldErrors || null;

    throw new APIError(message, response.status, fieldErrors);
}

/**
 * Wrapper para requisições GET
 */
async function get(endpoint) {
    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            method: 'GET',
            headers: getHeaders()
        });

        if (!response.ok) {
            await handleAPIError(response);
        }

        return await response.json();
    } catch (error) {
        if (error instanceof APIError) {
            throw error;
        }
        throw new APIError('Erro de conexão com o servidor', 0);
    }
}

/**
 * Wrapper para requisições POST
 */
async function post(endpoint, data) {
    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            method: 'POST',
            headers: getHeaders(),
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            await handleAPIError(response);
        }

        // Retorna vazio se status 204 (No Content)
        if (response.status === 204) {
            return null;
        }

        return await response.json();
    } catch (error) {
        if (error instanceof APIError) {
            throw error;
        }
        throw new APIError('Erro de conexão com o servidor', 0);
    }
}

/**
 * Wrapper para requisições PUT
 */
async function put(endpoint, data) {
    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            method: 'PUT',
            headers: getHeaders(),
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            await handleAPIError(response);
        }

        if (response.status === 204) {
            return null;
        }

        return await response.json();
    } catch (error) {
        if (error instanceof APIError) {
            throw error;
        }
        throw new APIError('Erro de conexão com o servidor', 0);
    }
}

/**
 * Wrapper para requisições DELETE
 */
async function del(endpoint) {
    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            method: 'DELETE',
            headers: getHeaders()
        });

        if (!response.ok) {
            await handleAPIError(response);
        }

        return response.status === 204 ? null : await response.json();
    } catch (error) {
        if (error instanceof APIError) {
            throw error;
        }
        throw new APIError('Erro de conexão com o servidor', 0);
    }
}

/**
 * API de Autenticação
 */
const AuthAPI = {
    login: async (email, senha) => {
        const response = await post('/auth/login', { email, senha });
        if (response.token) {
            Auth.setToken(response.token);
            Auth.setUser({
                usuarioId: response.usuarioId,
                nome: response.nome,
                email: response.email
            });
        }
        return response;
    },

    cadastro: async (nome, email, senha, tipo) => {
        const response = await post('/auth/cadastro', { nome, email, senha, tipo });
        if (response.token) {
            Auth.setToken(response.token);
            Auth.setUser({
                usuarioId: response.usuarioId,
                nome: response.nome,
                email: response.email
            });
        }
        return response;
    },

    logout: () => {
        Auth.logout();
    }
};

/**
 * API de Eventos
 */
const EventosAPI = {
    listar: () => get('/eventos'),
    buscar: (id) => get(`/eventos/${id}`),
    criar: (data) => post('/eventos', data),
    atualizar: (id, data) => put(`/eventos/${id}`, data),
    deletar: (id) => del(`/eventos/${id}`)
};

/**
 * API de Inscrições
 */
const InscricoesAPI = {
    inscrever: (participanteId, eventoId) => post('/inscricoes', {
        participanteId,
        eventoId
    }),
    cancelar: (inscricaoId) => del(`/inscricoes/${inscricaoId}`),
    selecionarAtividade: (inscricaoId, atividadeId) =>
        post(`/inscricoes/${inscricaoId}/atividades/${atividadeId}`),
    consultarAgenda: (inscricaoId) => get(`/inscricoes/${inscricaoId}/agenda`)
};

/**
 * API de Controle de Presença
 */
const ControleAPI = {
    realizarCheckIn: (qrCodeToken) => post('/controle/checkin', {
        qrCodeToken
    }),
    realizarCheckInManual: (inscricaoId, atividadeId) => post('/controle/manual', {
        inscricaoId,
        atividadeId
    }),
    gerarQrCode: (inscricaoId, atividadeId) =>
        get(`/controle/qrcode/${inscricaoId}/${atividadeId}`),
    relatorioPresenca: (eventoId) =>
        fetch(`${API_BASE_URL}/controle/relatorio/presenca/${eventoId}`)
            .then(r => r.text()),
    relatorioInscritos: (eventoId) =>
        fetch(`${API_BASE_URL}/controle/relatorio/inscritos/${eventoId}`)
            .then(r => r.text())
};

/**
 * API de Usuários (RF-03 — próprio perfil)
 */
const UsuariosAPI = {
    buscarPerfil: (id) => get(`/usuarios/${id}`),
    atualizarPerfil: (id, data) => put(`/usuarios/${id}`, data)
};

/**
 * Exibe mensagem de erro amigável na UI
 */
function exibirErro(error, containerId = 'error-container') {
    const container = document.getElementById(containerId);
    if (!container) {
        console.error('Container de erro não encontrado:', containerId);
        return;
    }

    let html = `
        <div class="alert alert-error" role="alert">
            <strong>Erro ${error.status || ''}:</strong> ${error.message}
    `;

    // Exibir erros de campo específicos
    if (error.fieldErrors && error.fieldErrors.length > 0) {
        html += '<ul>';
        error.fieldErrors.forEach(fieldError => {
            html += `<li><strong>${fieldError.field}:</strong> ${fieldError.message}</li>`;
        });
        html += '</ul>';
    }

    html += '</div>';

    container.innerHTML = html;

    // Auto-remover após 8 segundos
    setTimeout(() => {
        container.innerHTML = '';
    }, 8000);
}

/**
 * Exibe mensagem de sucesso na UI
 */
function exibirSucesso(mensagem, containerId = 'error-container') {
    const container = document.getElementById(containerId);
    if (!container) return;

    container.innerHTML = `
        <div class="alert alert-success" role="alert">
            ${mensagem}
        </div>
    `;

    setTimeout(() => {
        container.innerHTML = '';
    }, 5000);
}

/**
 * Formata data para exibição (ISO -> PT-BR)
 */
function formatarData(isoString) {
    if (!isoString) return '-';
    const data = new Date(isoString);
    return data.toLocaleDateString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
    });
}

/**
 * Formata data e hora para exibição
 */
function formatarDataHora(isoString) {
    if (!isoString) return '-';
    const data = new Date(isoString);
    return data.toLocaleString('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Exportar para uso global
window.API = {
    Auth: AuthAPI,
    Eventos: EventosAPI,
    Inscricoes: InscricoesAPI,
    Controle: ControleAPI,
    Usuarios: UsuariosAPI,
    exibirErro,
    exibirSucesso,
    formatarData,
    formatarDataHora,
    APIError
};

// Exportar objeto Auth separadamente para facilitar uso
window.Auth = Auth;
