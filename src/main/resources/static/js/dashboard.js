/**
 * dashboard.js - Painel do organizador (ingressonow-interface.html).
 * Métricas reais via API: eventos publicados, inscrições, presenças.
 * Sem backend de métricas dedicado — agrega listagens existentes.
 */

document.addEventListener('DOMContentLoaded', async function() {
    console.log('[Dashboard] Inicializando...');

    function authHeaders() {
        const h = { 'Accept': 'application/json' };
        const t = window.Auth ? window.Auth.getToken() : null;
        if (t) h['Authorization'] = 'Bearer ' + t;
        return h;
    }

    async function get(path) {
        const resp = await fetch(path, { headers: authHeaders() });
        if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
        return resp.json();
    }

    function setMetric(odId, valor) {
        const el = document.querySelector(`[data-od-id="${odId}"] b`);
        if (el) el.textContent = valor;
    }

    try {
        const eventos = await window.API.Eventos.listar();
        setMetric('metric-active-events', eventos.length);

        let inscricoes = 0;
        let presentes = 0;
        for (const e of eventos) {
            try {
                const lista = await get(`/api/inscricoes?eventoId=${encodeURIComponent(e.id)}`);
                inscricoes += lista.length;
                const csv = await window.API.Controle.relatorioPresenca
                    ? await fetch(`/api/controle/relatorio/presenca/${encodeURIComponent(e.id)}`,
                        { headers: authHeaders() }).then(r => r.ok ? r.text() : '')
                    : '';
                presentes += (csv.match(/PRESENTE/g) || []).length;
            } catch (err) {
                console.warn('[Dashboard] Evento sem dados:', e.id);
            }
        }
        // Reaproveita os cards existentes com números reais
        const metrics = document.querySelectorAll('[data-od-id="metrics-summary"] .metric b');
        if (metrics.length >= 1) metrics[0].textContent = inscricoes;
        if (metrics.length >= 2) metrics[1].textContent = presentes;
        setMetric('metric-total-inscritos', inscricoes);
        setMetric('metric-presentes', presentes);
    } catch (error) {
        console.error('[Dashboard] Erro:', error);
        if (window.API) window.API.exibirErro(error);
    }
});
