/**
 * programacao.js - Grade de atividades do evento
 * Integracao da pagina ingressonow-programacao.html com /api/eventos e /api/inscricoes
 * MVP: lista linear de atividades do evento selecionado, com botao de selecao.
 */

document.addEventListener('DOMContentLoaded', async function() {
    console.log('[Programacao] Inicializando pagina...');

    const eventSelect = document.getElementById('event-select');
    const scheduleGrid = document.querySelector('[data-od-id="schedule-grid"]');

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text == null ? '' : String(text);
        return div.innerHTML;
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
            console.error('[Programacao] Erro ao carregar eventos:', error);
            window.API.exibirErro(error);
        }
    }

    async function carregarGrade(eventoId) {
        try {
            const evento = await window.API.Eventos.buscar(eventoId);
            renderizarGrade(evento);
        } catch (error) {
            console.error('[Programacao] Erro ao carregar grade:', error);
            window.API.exibirErro(error);
        }
    }

    // RF-05/06: filtros client-side por tipo, trilha e local
    let gradeCache = [];
    let filtroTipo = '';
    let filtroTrilha = '';
    let filtroLocal = '';

    function garantirFiltros() {
        const controls = document.querySelector('[data-od-id="schedule-filters"]');
        if (!controls || document.getElementById('filtro-tipo')) return;
        const grupo = document.createElement('div');
        grupo.className = 'filters-group';
        grupo.innerHTML = `
            <select class="filter-select" id="filtro-tipo"><option value="">Todos os tipos</option></select>
            <select class="filter-select" id="filtro-trilha"><option value="">Todas as trilhas</option></select>
            <select class="filter-select" id="filtro-local"><option value="">Todos os locais</option></select>`;
        controls.appendChild(grupo);
        grupo.querySelector('#filtro-tipo').addEventListener('change', (e) => {
            filtroTipo = e.target.value;
            renderizarGradeFiltrada();
        });
        grupo.querySelector('#filtro-trilha').addEventListener('change', (e) => {
            filtroTrilha = e.target.value;
            renderizarGradeFiltrada();
        });
        grupo.querySelector('#filtro-local').addEventListener('change', (e) => {
            filtroLocal = e.target.value;
            renderizarGradeFiltrada();
        });
    }

    function popularOpcoesFiltro() {
        const selTipo = document.getElementById('filtro-tipo');
        const selTrilha = document.getElementById('filtro-trilha');
        const selLocal = document.getElementById('filtro-local');
        if (!selTipo) return;
        const tipos = [...new Set(gradeCache.map(a => a.tipo).filter(Boolean))].sort();
        const trilhas = [...new Set(gradeCache.map(a => a.trilha).filter(Boolean))].sort();
        const locais = [...new Set(gradeCache.map(a => a.local).filter(Boolean))].sort();
        selTipo.innerHTML = '<option value="">Todos os tipos</option>'
            + tipos.map(t => `<option value="${escapeHtml(t)}">${escapeHtml(t)}</option>`).join('');
        selTrilha.innerHTML = '<option value="">Todas as trilhas</option>'
            + trilhas.map(t => `<option value="${escapeHtml(t)}">${escapeHtml(t)}</option>`).join('');
        selLocal.innerHTML = '<option value="">Todos os locais</option>'
            + locais.map(t => `<option value="${escapeHtml(t)}">${escapeHtml(t)}</option>`).join('');
        selTipo.value = filtroTipo;
        selTrilha.value = filtroTrilha;
        selLocal.value = filtroLocal;
    }

    function renderizarGradeFiltrada() {
        if (!scheduleGrid) return;
        const lista = gradeCache.filter(a =>
            (!filtroTipo || a.tipo === filtroTipo) &&
            (!filtroTrilha || a.trilha === filtroTrilha) &&
            (!filtroLocal || a.local === filtroLocal));
        pintarGrade(lista);
    }

    function pintarGrade(atividades) {
        if (atividades.length === 0) {
            scheduleGrid.innerHTML = '<p style="padding:32px;text-align:center">Nenhuma atividade para os filtros selecionados.</p>';
            return;
        }
        scheduleGrid.innerHTML = atividades.map(a => `
            <div class="session-card" data-atividade-id="${a.id}" style="margin:8px 0">
                <div class="session-title">${escapeHtml(a.titulo)}</div>
                <div class="session-meta">
                    <span class="session-category">${escapeHtml(window.API.formatarDataHora(a.inicio))} - ${escapeHtml(window.API.formatarDataHora(a.fim))}</span>
                    <span class="session-capacity">${escapeHtml(a.local || '')}</span>
                    ${a.tipo ? `<span class="session-capacity">${escapeHtml(a.tipo)}</span>` : ''}
                    ${a.trilha ? `<span class="session-capacity">${escapeHtml(a.trilha)}</span>` : ''}
                </div>
                <button class="primary" data-action="selecionar" data-id="${a.id}" style="margin-top:8px">Selecionar atividade</button>
            </div>`).join('');

        scheduleGrid.querySelectorAll('[data-action="selecionar"]').forEach(btn => {
            btn.addEventListener('click', () => selecionarAtividade(btn.dataset.id));
        });
    }

    function renderizarGrade(evento) {
        if (!scheduleGrid) return;
        gradeCache = evento.atividades || [];
        filtroTipo = '';
        filtroTrilha = '';
        filtroLocal = '';
        garantirFiltros();
        popularOpcoesFiltro();
        if (gradeCache.length === 0) {
            scheduleGrid.innerHTML = '<p style="padding:32px;text-align:center">Nenhuma atividade cadastrada para este evento.</p>';
            return;
        }
        pintarGrade(gradeCache);
    }

    /**
     * RF-18: exibe o motivo de conflito de agenda (mensagem do backend)
     * em destaque no card da atividade, sem auto-dismiss.
     */
    function exibirConflito(atividadeId, mensagem) {
        limparConflito();
        const card = scheduleGrid
            ? scheduleGrid.querySelector(`[data-atividade-id="${atividadeId}"]`)
            : null;
        const aviso = document.createElement('div');
        aviso.id = 'conflito-agenda-aviso';
        aviso.setAttribute('role', 'alert');
        aviso.style.cssText = 'border:1px solid #FCA5A5;background:#FEF2F2;color:#991B1B;'
            + 'border-radius:6px;padding:12px 14px;margin:0 0 12px 0;font-size:13px';
        aviso.textContent = '⚠️ Conflito de agenda: ' + mensagem;
        if (scheduleGrid && scheduleGrid.parentNode) {
            scheduleGrid.parentNode.insertBefore(aviso, scheduleGrid);
        }
        if (card) {
            card.style.borderColor = '#DC2626';
            card.style.boxShadow = '0 0 0 2px rgba(220,38,38,.25)';
            card.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
        }
    }

    function limparConflito() {
        const aviso = document.getElementById('conflito-agenda-aviso');
        if (aviso) aviso.remove();
        if (scheduleGrid) {
            scheduleGrid.querySelectorAll('[data-atividade-id]').forEach(c => {
                c.style.borderColor = '';
                c.style.boxShadow = '';
            });
        }
    }

    async function selecionarAtividade(atividadeId) {
        const user = window.Auth ? window.Auth.getUser() : null;
        if (!user || !user.usuarioId) {
            window.API.exibirErro(new window.API.APIError('Voce precisa estar logado.', 401));
            setTimeout(() => { window.location.href = '/login'; }, 1500);
            return;
        }
        limparConflito();
        try {
            // Localiza a inscricao do usuario no evento atual
            const eventoId = eventSelect ? eventSelect.value : null;
            const resp = await fetch(`/api/inscricoes?participanteId=${encodeURIComponent(user.usuarioId)}`, {
                headers: (() => {
                    const h = { 'Accept': 'application/json' };
                    const t = window.Auth.getToken();
                    if (t) h['Authorization'] = 'Bearer ' + t;
                    return h;
                })()
            });
            if (!resp.ok) throw new Error(`Falha ao buscar inscricao (HTTP ${resp.status})`);
            const inscricoes = await resp.json();
            const inscricao = inscricoes.find(i => i.eventoId === eventoId);
            if (!inscricao) {
                window.API.exibirErro(new window.API.APIError('Inscreva-se no evento antes de selecionar atividades.', 422));
                return;
            }
            await window.API.Inscricoes.selecionarAtividade(inscricao.id, atividadeId);
            window.API.exibirSucesso('Atividade selecionada com sucesso!');
        } catch (error) {
            console.error('[Programacao] Erro ao selecionar:', error);
            // 422 de conflito (RF-18/RN-07): motivo em destaque no card
            if (error && error.status === 422) {
                exibirConflito(atividadeId, error.message);
            } else {
                window.API.exibirErro(error);
            }
        }
    }

    if (eventSelect) {
        eventSelect.addEventListener('change', () => carregarGrade(eventSelect.value));
    }

    await carregarEventos();
});
