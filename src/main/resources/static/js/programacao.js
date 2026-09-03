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

    function renderizarGrade(evento) {
        if (!scheduleGrid) return;
        const atividades = evento.atividades || [];
        if (atividades.length === 0) {
            scheduleGrid.innerHTML = '<p style="padding:32px;text-align:center">Nenhuma atividade cadastrada para este evento.</p>';
            return;
        }
        scheduleGrid.innerHTML = atividades.map(a => `
            <div class="session-card" data-atividade-id="${a.id}" style="margin:8px 0">
                <div class="session-title">${escapeHtml(a.titulo)}</div>
                <div class="session-meta">
                    <span class="session-category">${escapeHtml(window.API.formatarDataHora(a.inicio))} - ${escapeHtml(window.API.formatarDataHora(a.fim))}</span>
                    <span class="session-capacity">${escapeHtml(a.local || '')}</span>
                </div>
                <button class="primary" data-action="selecionar" data-id="${a.id}" style="margin-top:8px">Selecionar atividade</button>
            </div>`).join('');

        scheduleGrid.querySelectorAll('[data-action="selecionar"]').forEach(btn => {
            btn.addEventListener('click', () => selecionarAtividade(btn.dataset.id));
        });
    }

    async function selecionarAtividade(atividadeId) {
        const user = window.Auth ? window.Auth.getUser() : null;
        if (!user || !user.usuarioId) {
            window.API.exibirErro(new window.API.APIError('Voce precisa estar logado.', 401));
            setTimeout(() => { window.location.href = '/login'; }, 1500);
            return;
        }
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
            window.API.exibirErro(error);
        }
    }

    if (eventSelect) {
        eventSelect.addEventListener('change', () => carregarGrade(eventSelect.value));
    }

    await carregarEventos();
});
