/**
 * atividades.js - Gestão de Atividades (organizador).
 * Integracao da pagina ingressonow-atividades.html com a API:
 * GET /api/eventos/{id} (grade), POST .../atividades (criar com
 * tipo/trilha/politica), PUT .../atividades/{atId} (remarcar RF-07),
 * POST/DELETE pessoas (RF-08).
 */

document.addEventListener('DOMContentLoaded', async function() {
    console.log('[Atividades] Inicializando pagina...');

    const eventSelect = document.getElementById('event-select');
    const tbody = document.querySelector('.activities-table tbody');
    const searchInput = document.querySelector('[data-od-id="activity-search"]');
    const createBtn = document.querySelector('[data-od-id="create-activity-cta"]');
    const pills = document.querySelectorAll('[data-od-id="type-filters"] .pill');

    let cache = [];
    let filtroTipo = '';

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

    async function carregarEventos() {
        try {
            const eventos = await window.API.Eventos.listar();
            if (eventSelect) {
                eventSelect.innerHTML = eventos.map(e =>
                    `<option value="${e.id}">${escapeHtml(e.titulo)}</option>`).join('');
            }
            if (eventos.length > 0) await carregarGrade(eventos[0].id);
        } catch (error) {
            window.API.exibirErro(error);
        }
    }

    async function carregarGrade(eventoId) {
        try {
            const evento = await window.API.Eventos.buscar(eventoId);
            cache = evento.atividades || [];
            renderizar();
        } catch (error) {
            window.API.exibirErro(error);
        }
    }

    function listaFiltrada() {
        const termo = searchInput ? searchInput.value.toLowerCase() : '';
        return cache.filter(a =>
            (!filtroTipo || (a.tipo || '').toUpperCase() === filtroTipo) &&
            (!termo || (a.titulo || '').toLowerCase().includes(termo) ||
                (a.local || '').toLowerCase().includes(termo)));
    }

    function renderizar() {
        if (!tbody) return;
        const lista = listaFiltrada();
        if (lista.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:24px">Nenhuma atividade encontrada.</td></tr>';
            return;
        }
        tbody.innerHTML = lista.map(a => `
            <tr data-atividade-id="${a.id}">
                <td><strong>${escapeHtml(a.titulo)}</strong><br>
                    <small>${escapeHtml(a.tipo || '—')}${a.trilha ? ' • ' + escapeHtml(a.trilha) : ''}</small></td>
                <td>${escapeHtml(window.API.formatarDataHora(a.inicio))}<br>
                    <small>${escapeHtml(a.local || '')}</small></td>
                <td><small>${escapeHtml(a.politicaFrequencia || 'UNICA')}</small></td>
                <td>—</td>
                <td><span class="badge badge-confirmed">Ativa</span></td>
                <td style="text-align:right">
                    <button class="secondary" data-action="remarcar" data-id="${a.id}">Remarcar</button>
                    <button class="secondary" data-action="pessoas" data-id="${a.id}">Pessoas</button>
                </td>
            </tr>`).join('');
        tbody.querySelectorAll('[data-action="remarcar"]').forEach(b =>
            b.addEventListener('click', () => abrirRemarcar(b.dataset.id)));
        tbody.querySelectorAll('[data-action="pessoas"]').forEach(b =>
            b.addEventListener('click', () => gerenciarPessoas(b.dataset.id)));
    }

    // RF-07: remarcar com revalidação (PUT retorna 422 em conflito)
    async function abrirRemarcar(atividadeId) {
        const eventoId = eventSelect ? eventSelect.value : null;
        const atual = cache.find(a => a.id === atividadeId);
        const inicio = prompt('Novo início (yyyy-MM-ddTHH:mm):', atual ? atual.inicio : '');
        if (inicio === null) return;
        const fim = prompt('Novo fim (yyyy-MM-ddTHH:mm):', atual ? atual.fim : '');
        if (fim === null) return;
        const local = prompt('Novo local:', atual ? atual.local : '');
        if (local === null) return;
        try {
            const resp = await fetch(`/api/eventos/${encodeURIComponent(eventoId)}/atividades/${encodeURIComponent(atividadeId)}`, {
                method: 'PUT',
                headers: authHeaders(true),
                body: JSON.stringify({ inicio, fim, local })
            });
            if (!resp.ok) {
                const err = await resp.json().catch(() => ({}));
                throw new window.API.APIError(err.message || `HTTP ${resp.status}`, resp.status);
            }
            window.API.exibirSucesso('Atividade remarcada!');
            await carregarGrade(eventoId);
        } catch (error) {
            window.API.exibirErro(error);
        }
    }

    // RF-08: vincular pessoa com papel
    async function gerenciarPessoas(atividadeId) {
        const eventoId = eventSelect ? eventSelect.value : null;
        const nome = prompt('Nome da pessoa:');
        if (!nome) return;
        const papel = prompt('Papel (ex: PALESTRANTE, APRESENTADOR, RESPONSAVEL):');
        if (!papel) return;
        try {
            const resp = await fetch(`/api/eventos/${encodeURIComponent(eventoId)}/atividades/${encodeURIComponent(atividadeId)}/pessoas`, {
                method: 'POST',
                headers: authHeaders(true),
                body: JSON.stringify({ nome, papel })
            });
            if (!resp.ok) {
                const err = await resp.json().catch(() => ({}));
                throw new window.API.APIError(err.message || `HTTP ${resp.status}`, resp.status);
            }
            const pessoa = await resp.json();
            window.API.exibirSucesso(`${pessoa.nome} (${pessoa.papel}) vinculado!`);
        } catch (error) {
            window.API.exibirErro(error);
        }
    }

    // Criar atividade (RF-05/06/19)
    if (createBtn) {
        createBtn.addEventListener('click', async () => {
            const eventoId = eventSelect ? eventSelect.value : null;
            if (!eventoId) {
                window.API.exibirErro(new window.API.APIError('Selecione um evento.', 400));
                return;
            }
            const titulo = prompt('Título da atividade:');
            if (!titulo) return;
            const inicio = prompt('Início (yyyy-MM-ddTHH:mm):');
            if (!inicio) return;
            const fim = prompt('Fim (yyyy-MM-ddTHH:mm):');
            if (!fim) return;
            const local = prompt('Local:');
            if (!local) return;
            const tipo = prompt('Tipo (ex: PALESTRA, WORKSHOP) — vazio = sem tipo:') || null;
            const trilha = prompt('Trilha — vazio = sem trilha:') || null;
            const politica = prompt('Critério de presença (UNICA ou ENTRADA_SAIDA):', 'UNICA') || 'UNICA';
            try {
                const resp = await fetch(`/api/eventos/${encodeURIComponent(eventoId)}/atividades`, {
                    method: 'POST',
                    headers: authHeaders(true),
                    body: JSON.stringify({ titulo, inicio, fim, local, tipo, trilha, politicaFrequencia: politica })
                });
                if (!resp.ok) {
                    const err = await resp.json().catch(() => ({}));
                    throw new window.API.APIError(err.message || `HTTP ${resp.status}`, resp.status);
                }
                window.API.exibirSucesso('Atividade criada!');
                await carregarGrade(eventoId);
            } catch (error) {
                window.API.exibirErro(error);
            }
        });
    }

    // Pills de tipo (client-side)
    const pillMap = { 'filter-all': '', 'filter-lectures': 'PALESTRA', 'filter-workshops': 'WORKSHOP', 'filter-panels': 'PAINEL' };
    pills.forEach(pill => pill.addEventListener('click', () => {
        filtroTipo = pillMap[pill.getAttribute('data-od-id')] ?? '';
        renderizar();
    }));

    if (searchInput) searchInput.addEventListener('input', renderizar);
    if (eventSelect) eventSelect.addEventListener('change', () => carregarGrade(eventSelect.value));

    await carregarEventos();
});
