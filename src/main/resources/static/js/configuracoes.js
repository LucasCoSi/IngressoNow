/**
 * configuracoes.js - Regras de inscrição/frequência por evento (organizador).
 * Integracao da aba "Regras de Inscrição & Frequência" com:
 * GET /api/eventos/{id}, PUT /api/eventos/{id} (detalhes RF-04/13, fuso RN-20).
 * Demais abas (perfil org, certificados, SMTP, webhooks) são decorativas no MVP.
 */

document.addEventListener('DOMContentLoaded', async function() {
    console.log('[Configuracoes] Inicializando...');

    const eventSelect = document.getElementById('event-select')
        || document.querySelector('[data-od-id="event-dropdown"]');
    const saveBtn = document.querySelector('[data-od-id="save-settings-cta"]');
    const freqTab = document.querySelector('[data-od-id="tab-content-frequency"]');

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text == null ? '' : String(text);
        return div.innerHTML;
    }

    function authHeaders() {
        const h = { 'Accept': 'application/json', 'Content-Type': 'application/json' };
        const t = window.Auth ? window.Auth.getToken() : null;
        if (t) h['Authorization'] = 'Bearer ' + t;
        return h;
    }

    // Injeta form funcional na aba de regras (se ainda não existe)
    function garantirForm() {
        if (!freqTab || document.getElementById('regras-evento-form')) return;
        const form = document.createElement('form');
        form.id = 'regras-evento-form';
        form.style.cssText = 'display:grid;gap:10px;max-width:520px;margin-top:12px';
        form.innerHTML = `
            <label style="font-size:12px">Período início (yyyy-MM-ddTHH:mm, vazio = manter)
                <input id="cfg-periodo-inicio" style="width:100%;padding:8px"></label>
            <label style="font-size:12px">Período fim (yyyy-MM-ddTHH:mm, vazio = manter)
                <input id="cfg-periodo-fim" style="width:100%;padding:8px"></label>
            <label style="font-size:12px">Local / modalidade
                <input id="cfg-local" style="width:100%;padding:8px"></label>
            <label style="font-size:12px">Fuso (IANA, ex: America/Sao_Paulo)
                <input id="cfg-fuso" style="width:100%;padding:8px"></label>
            <label style="font-size:12px"><input id="cfg-exige" type="checkbox">
                Exigir escolha de atividades na inscrição (RF-13)</label>
            <label style="font-size:12px"><input id="cfg-vagas-at" type="checkbox">
                Vagas controladas por atividade (RF-13)</label>
            <button class="secondary" type="submit" style="justify-self:start">Salvar regras do evento</button>`;
        freqTab.appendChild(form);
        form.addEventListener('submit', salvarRegras);
    }

    async function salvarRegras(ev) {
        ev.preventDefault();
        const eventoId = eventSelect ? eventSelect.value : null;
        if (!eventoId) {
            window.API.exibirErro(new window.API.APIError('Selecione um evento.', 400));
            return;
        }
        const body = {};
        const pi = document.getElementById('cfg-periodo-inicio').value.trim();
        const pf = document.getElementById('cfg-periodo-fim').value.trim();
        const local = document.getElementById('cfg-local').value.trim();
        const fuso = document.getElementById('cfg-fuso').value.trim();
        if (pi) body.periodoInicio = pi;
        if (pf) body.periodoFim = pf;
        if (local) body.local = local;
        if (fuso) body.fuso = fuso;
        body.exigeEscolhaAtividades = document.getElementById('cfg-exige').checked;
        body.vagasPorAtividade = document.getElementById('cfg-vagas-at').checked;
        try {
            const resp = await fetch(`/api/eventos/${encodeURIComponent(eventoId)}`, {
                method: 'PUT', headers: authHeaders(), body: JSON.stringify(body)
            });
            if (!resp.ok) {
                const err = await resp.json().catch(() => ({}));
                throw new window.API.APIError(err.message || `HTTP ${resp.status}`, resp.status);
            }
            const evento = await resp.json();
            preencher(evento);
            window.API.exibirSucesso('Regras salvas!');
        } catch (error) {
            window.API.exibirErro(error);
        }
    }

    function preencher(evento) {
        if (!evento) return;
        const set = (id, v) => { const el = document.getElementById(id); if (el) el.value = v || ''; };
        set('cfg-periodo-inicio', evento.periodoInicio);
        set('cfg-periodo-fim', evento.periodoFim);
        set('cfg-local', evento.local);
        set('cfg-fuso', evento.fuso);
        const exige = document.getElementById('cfg-exige');
        const vagas = document.getElementById('cfg-vagas-at');
        if (exige) exige.checked = !!evento.exigeEscolhaAtividades;
        if (vagas) vagas.checked = !!evento.vagasPorAtividade;
    }

    async function carregarEventos() {
        try {
            const eventos = await window.API.Eventos.listar();
            if (eventSelect && eventSelect.tagName === 'SELECT') {
                eventSelect.innerHTML = eventos.map(e =>
                    `<option value="${e.id}">${escapeHtml(e.titulo)}</option>`).join('');
                if (eventos.length > 0) {
                    const detalhe = await window.API.Eventos.buscar(eventos[0].id);
                    preencher(detalhe);
                }
            }
        } catch (error) {
            window.API.exibirErro(error);
        }
    }

    garantirForm();
    if (saveBtn) {
        saveBtn.addEventListener('click', () => {
            const form = document.getElementById('regras-evento-form');
            if (form) form.requestSubmit();
            else window.API.exibirSucesso('Nada a salvar no MVP.');
        });
    }
    if (eventSelect) {
        eventSelect.addEventListener('change', async () => {
            try {
                preencher(await window.API.Eventos.buscar(eventSelect.value));
            } catch (error) {
                window.API.exibirErro(error);
            }
        });
    }

    await carregarEventos();
});
