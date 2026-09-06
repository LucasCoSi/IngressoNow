/**
 * usuarios.js - Gestão de Usuários (organizador/admin, §3).
 * Integracao da pagina ingressonow-usuarios.html com:
 * GET /api/usuarios (listar), DELETE /api/usuarios/{id} (remover com travas).
 * Convite = cadastro via /api/auth/cadastro (tipo ORGANIZADOR/PARTICIPANTE).
 */

document.addEventListener('DOMContentLoaded', async function() {
    console.log('[Usuarios] Inicializando pagina...');

    const tbody = document.querySelector('tbody');
    const inviteBtn = document.querySelector('[data-od-id="invite-user-cta"]');
    const searchInput = document.querySelector('[data-od-id="global-search"]');
    const metricTotal = document.querySelector('[data-od-id="metric-total"] b');

    let cache = [];

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text == null ? '' : String(text);
        return div.innerHTML;
    }

    function authHeaders(json) {
        const h = { 'Accept': 'application/json' };
        if (json) h['Content-Type'] = 'application/json';
        const t = window.Auth ? window.Auth.getToken() : null;
        if (t) h['Authorization'] = 'Bearer ' + t;
        return h;
    }

    async function api(path, opts) {
        const resp = await fetch(path, { headers: authHeaders(true), ...(opts || {}) });
        if (!resp.ok) {
            const err = await resp.json().catch(() => ({}));
            throw new window.API.APIError(
                err.message || `Falha na operação (HTTP ${resp.status})`, resp.status);
        }
        if (resp.status === 204) return null;
        return resp.json();
    }

    async function carregar() {
        try {
            cache = await api('/api/usuarios');
            if (metricTotal) metricTotal.textContent = cache.length;
            renderizar('');
        } catch (error) {
            window.API.exibirErro(error);
        }
    }

    function renderizar(termo) {
        if (!tbody) return;
        const t = (termo || '').toLowerCase();
        const lista = cache.filter(u =>
            !t || (u.nome || '').toLowerCase().includes(t) ||
            (u.email || '').toLowerCase().includes(t));
        if (lista.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;padding:24px">Nenhum usuário encontrado.</td></tr>';
            return;
        }
        tbody.innerHTML = lista.map(u => `
            <tr data-id="${u.id}">
                <td><strong>${escapeHtml(u.nome)}</strong><br><small>${escapeHtml(u.email)}</small></td>
                <td>${escapeHtml(u.tipo || '')}</td>
                <td><span class="badge badge-confirmed">Ativo</span></td>
                <td style="text-align:right">
                    <button class="secondary" data-action="remover" data-id="${u.id}">Remover</button>
                </td>
            </tr>`).join('');
        tbody.querySelectorAll('[data-action="remover"]').forEach(b =>
            b.addEventListener('click', () => remover(b.dataset.id)));
    }

    async function remover(id) {
        const alvo = cache.find(u => u.id === id);
        if (!confirm(`Remover ${alvo ? alvo.nome : id}?`)) return;
        try {
            await api(`/api/usuarios/${encodeURIComponent(id)}`, { method: 'DELETE' });
            window.API.exibirSucesso('Usuário removido.');
            await carregar();
        } catch (error) {
            window.API.exibirErro(error);
        }
    }

    // Convidar = cadastrar com tipo (o convidado define a senha no cadastro)
    if (inviteBtn) {
        inviteBtn.addEventListener('click', async () => {
            const nome = prompt('Nome do usuário:');
            if (!nome) return;
            const email = prompt('E-mail:');
            if (!email) return;
            const tipo = (prompt('Tipo (ORGANIZADOR ou PARTICIPANTE):', 'PARTICIPANTE') || 'PARTICIPANTE').toUpperCase();
            const senha = prompt('Senha inicial (o usuário pode trocar no perfil):');
            if (!senha) return;
            try {
                await window.API.Auth.cadastro(nome, email, senha, tipo);
                window.API.exibirSucesso('Usuário cadastrado!');
                await carregar();
            } catch (error) {
                window.API.exibirErro(error);
            }
        });
    }

    if (searchInput) searchInput.addEventListener('input', () => renderizar(searchInput.value));

    await carregar();
});
