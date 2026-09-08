/**
 * dashboard.js - Painel do organizador (ingressonow-interface.html).
 * Métricas reais via API: total de inscritos, eventos esgotados,
 * taxa de presença e resumo de participação.
 * Sem fonte no MVP: carga horária total (exibe "—").
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
        setMetric('metric-active-events', '0');
        setMetric('metric-published-events', `0/${eventos.length}`);
        setMetric('metric-activities', '—');
        setMetric('metric-participants', '—');

        let inscricoes = 0;
        let presentes = 0;
        let esgotados = 0;
        for (const e of eventos) {
            try {
                const lista = await get(`/api/inscricoes?eventoId=${encodeURIComponent(e.id)}`);
                inscricoes += lista.length;
                if (e.maxVagas != null && lista.length >= e.maxVagas) esgotados += 1;
                const csv = await fetch(`/api/controle/relatorio/presenca/${encodeURIComponent(e.id)}`,
                    { headers: authHeaders() }).then(r => r.ok ? r.text() : '');
                presentes += (csv.match(/PRESENTE/g) || []).length;
            } catch (err) {
                console.warn('[Dashboard] Evento sem dados:', e.id);
            }
        }
        const taxa = inscricoes > 0
            ? (Math.round(presentes / inscricoes * 1000) / 10).toString().replace('.', ',') + '%'
            : '—';
        setMetric('metric-active-events', inscricoes);
        setMetric('metric-published-events', `${esgotados}/${eventos.length}`);
        setMetric('metric-activities', taxa);
        setMetric('metric-participants', '—');
        const capacity = document.querySelectorAll('[data-od-id="capacity-summary"] .capacity-line b');
        if (capacity.length >= 1) capacity[0].textContent = inscricoes > 0
            ? `${presentes} (${taxa})` : `${presentes}`;
        if (capacity.length >= 2) capacity[1].textContent = taxa;
    } catch (error) {
        console.error('[Dashboard] Erro:', error);
        if (window.API) window.API.exibirErro(error);
    }
});
