/**
 * avaliacoes.js - Questionários e consolidação (organizador) + resposta (participante).
 * Integracao da pagina ingressonow-avaliacoes.html com:
 * POST/GET /api/eventos/{id}/questionarios, POST /api/avaliacoes,
 * GET /api/questionarios/{id}/resultados.
 * RN-15: política de identificação exibida antes do envio.
 */

document.addEventListener('DOMContentLoaded', async function() {
    console.log('[Avaliacoes] Inicializando pagina...');

    const eventSelect = document.getElementById('event-select');
    const createBtn = document.querySelector('[data-od-id="create-survey-cta"]');
    const ratingMetric = document.querySelector('[data-od-id="metric-rating"] b');
    const responseMetric = document.querySelector('[data-od-id="metric-response"] b');
    const rankingBox = document.querySelector('.activities-ranking');
    const feedbackBox = document.querySelector('.feedback-feed');

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
        return resp.json();
    }

    async function carregarEventos() {
        try {
            const eventos = await window.API.Eventos.listar();
            if (eventSelect) {
                eventSelect.innerHTML = eventos.map(e =>
                    `<option value="${e.id}">${escapeHtml(e.titulo)}</option>`).join('');
            }
            if (eventos.length > 0) await carregarConsolidado(eventos[0].id);
        } catch (error) {
            window.API.exibirErro(error);
        }
    }

    // Consolidação do 1º questionário do evento (RF-28)
    async function carregarConsolidado(eventoId) {
        try {
            const questionarios = await api(`/api/eventos/${encodeURIComponent(eventoId)}/questionarios`);
            if (!questionarios || questionarios.length === 0) {
                pintarVazio();
                return;
            }
            const q = questionarios[0];
            const resultado = await api(`/api/questionarios/${encodeURIComponent(q.id)}/resultados`);
            pintarResultado(q, resultado);
        } catch (error) {
            console.error('[Avaliacoes] Erro ao consolidar:', error);
            window.API.exibirErro(error);
        }
    }

    function pintarVazio() {
        if (ratingMetric) ratingMetric.textContent = '—';
        if (responseMetric) responseMetric.textContent = '0';
        if (rankingBox) rankingBox.innerHTML = '<div class="activity-header">Sem questionários neste evento</div>';
        if (feedbackBox) feedbackBox.innerHTML = '<div class="feedback-header">Sem comentários</div>';
    }

    function pintarResultado(questionario, resultado) {
        const escalas = (resultado.perguntas || []).filter(p => p.tipo === 'ESCALA_NUMERICA');
        const textos = (resultado.perguntas || []).filter(p => p.tipo !== 'ESCALA_NUMERICA');
        const mediaGeral = escalas.length > 0
            ? (escalas.reduce((s, p) => s + (p.media || 0), 0) / escalas.length) : null;
        const totalResp = (resultado.perguntas || []).reduce((s, p) => s + (p.totalRespostas || 0), 0);
        if (ratingMetric) {
            ratingMetric.textContent = mediaGeral !== null
                ? `${mediaGeral.toFixed(1)} ★` : '—';
        }
        if (responseMetric) responseMetric.textContent = `${resultado.totalAvaliacoes || 0}`;
        if (rankingBox) {
            rankingBox.innerHTML = '<div class="activity-header">Perguntas — '
                + escapeHtml(questionario.titulo)
                + ` <small>(política: ${escapeHtml(questionario.politicaIdentificacao || 'IDENTIFICADO')})</small></div>`
                + ((resultado.perguntas || []).map(p => `
                    <div class="activity-item">
                        <div class="activity-name">
                            <span class="activity-title">${escapeHtml(p.enunciado)}</span>
                            <span class="activity-speaker">${escapeHtml(p.tipo || '')}</span>
                        </div>
                        <div class="activity-meta">
                            <span class="rating-info">${p.media !== null && p.media !== undefined
                                ? Number(p.media).toFixed(2) : (p.totalRespostas || 0) + ' resp.'}<br>
                                ${p.totalRespostas || 0} votos</span>
                        </div>
                    </div>`).join('') || '<div class="activity-item">Sem perguntas</div>');
        }
        if (feedbackBox) {
            const comentarios = [];
            (resultado.perguntas || []).forEach(p => (p.comentarios || []).forEach(c =>
                comentarios.push({ texto: c, pergunta: p.enunciado })));
            feedbackBox.innerHTML = '<div class="feedback-header">Comentários & Feedbacks</div>'
                + (comentarios.map(c => `
                    <div class="feedback-item">
                        <div class="feedback-avatar">•</div>
                        <div class="feedback-content">
                            <div class="feedback-header-line">
                                <span class="feedback-name">${escapeHtml(c.pergunta)}</span>
                            </div>
                            <p class="feedback-text">${escapeHtml(c.texto)}</p>
                        </div>
                    </div>`).join('') || '<div class="feedback-item">Sem comentários ainda.</div>');
        }
        void totalResp;
    }

    // Criar pesquisa (organizador): 1 escala + 1 texto, com política RN-15
    if (createBtn) {
        createBtn.addEventListener('click', async () => {
            const eventoId = eventSelect ? eventSelect.value : null;
            if (!eventoId) {
                window.API.exibirErro(new window.API.APIError('Selecione um evento.', 400));
                return;
            }
            const titulo = prompt('Título do questionário:');
            if (!titulo) return;
            const enunciado = prompt('Pergunta de escala (1-5):', 'Como você avalia o evento?');
            if (!enunciado) return;
            const politica = (prompt('Política de identificação (IDENTIFICADO ou ANONIMO):', 'IDENTIFICADO') || 'IDENTIFICADO').toUpperCase();
            try {
                await api(`/api/eventos/${encodeURIComponent(eventoId)}/questionarios`, {
                    method: 'POST',
                    body: JSON.stringify({
                        titulo,
                        politicaIdentificacao: politica,
                        perguntas: [
                            { enunciado, tipo: 'ESCALA_NUMERICA', obrigatoria: true, escalaMax: 5 },
                            { enunciado: 'Comentários', tipo: 'TEXTO_LIVRE', obrigatoria: false }
                        ]
                    })
                });
                window.API.exibirSucesso('Questionário criado!');
                await carregarConsolidado(eventoId);
            } catch (error) {
                window.API.exibirErro(error);
            }
        });
    }

    if (eventSelect) eventSelect.addEventListener('change', () => carregarConsolidado(eventSelect.value));

    await carregarEventos();
});
