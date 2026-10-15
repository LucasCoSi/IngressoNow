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
    const restoreBtn = document.querySelector('[data-od-id="restore-defaults-btn"]');
    const freqTab = document.querySelector('[data-od-id="tab-content-frequency"]');

    // Preferências locais (MVP): o backend do §11 não tem endpoint de
    // configurações globais nem upload de imagens (RN-02: foto opcional,
    // sem bloqueio). Logo/assinatura ficam como preferência deste
    // navegador com preview; regras de evento vão ao servidor (PUT /api/eventos).
    const PREFS_KEY = 'cfgPrefs';
    const LOGO_MAX = 5 * 1024 * 1024;
    const ASS_MAX = 2 * 1024 * 1024;

    function lerPrefs() {
        try {
            return JSON.parse(localStorage.getItem(PREFS_KEY) || '{}');
        } catch (e) {
            return {};
        }
    }

    function aplicarPreferencias() {
        const p = lerPrefs();
        const setVal = (id, v) => { const el = document.getElementById(id); if (el && v != null) el.value = v; };
        setVal('cfg-org-nome', p.orgNome);
        setVal('cfg-org-cnpj', p.orgCnpj);
        setVal('cfg-org-tel', p.orgTel);
        setVal('cfg-org-email', p.orgEmail);
        setVal('cfg-reserva', p.reserva);
        const setToggle = (odId, v) => {
            const el = document.querySelector(`[data-od-id="${odId}"]`);
            if (el && v != null) el.classList.toggle('active', !!v);
        };
        setToggle('toggle-frequency', p.tglFrequency);
        setToggle('toggle-manual-checkin', p.tglManual);
        setToggle('toggle-late-enrollment', p.tglLate);
        if (p.certTpl) {
            document.querySelectorAll('.cert-card').forEach(c =>
                c.classList.toggle('active', c.dataset.tpl === p.certTpl));
        }
        if (p.orgNome) {
            document.querySelectorAll('.org strong').forEach(el => { el.textContent = p.orgNome; });
        }
        mostrarPreview('logo-dropzone', p.logo, 'PNG, JPG (máx. 5MB)');
        mostrarPreview('signature-dropzone', p.assinatura, 'PNG com fundo transparente (máx. 2MB)');
    }

    function mostrarPreview(odId, dataUrl, hint) {
        const dz = document.querySelector(`[data-od-id="${odId}"]`);
        if (!dz) return;
        let img = dz.querySelector('img.cfg-preview');
        if (dataUrl) {
            if (!img) {
                img = document.createElement('img');
                img.className = 'cfg-preview';
                img.style.cssText = 'max-height:120px;max-width:100%;border-radius:4px;margin-bottom:8px';
                dz.prepend(img);
            }
            img.src = dataUrl;
            const hintEl = dz.querySelector('.dropzone-hint');
            if (hintEl) hintEl.textContent = 'Imagem carregada (preferência local deste navegador).';
        } else if (img) {
            img.remove();
            const hintEl = dz.querySelector('.dropzone-hint');
            if (hintEl && hint) hintEl.textContent = hint;
        }
    }

    function salvarPreferencias() {
        const val = (id) => { const el = document.getElementById(id); return el ? el.value : ''; };
        const tgl = (odId) => {
            const el = document.querySelector(`[data-od-id="${odId}"]`);
            return el ? el.classList.contains('active') : null;
        };
        const p = lerPrefs();
        p.orgNome = val('cfg-org-nome');
        p.orgCnpj = val('cfg-org-cnpj');
        p.orgTel = val('cfg-org-tel');
        p.orgEmail = val('cfg-org-email');
        p.reserva = val('cfg-reserva');
        p.tglFrequency = tgl('toggle-frequency');
        p.tglManual = tgl('toggle-manual-checkin');
        p.tglLate = tgl('toggle-late-enrollment');
        const ativo = document.querySelector('.cert-card.active');
        if (ativo) p.certTpl = ativo.dataset.tpl;
        try {
            localStorage.setItem(PREFS_KEY, JSON.stringify(p));
        } catch (e) {
            window.API.exibirErro(new window.API.APIError(
                'Não coube no armazenamento local (imagem grande demais?). Preferências de texto foram mantidas.', 400));
            return false;
        }
        if (p.orgNome) {
            document.querySelectorAll('.org strong').forEach(el => { el.textContent = p.orgNome; });
        }
        return true;
    }

    function configurarDropzone(odId, chave, maxBytes, tipos) {
        const dz = document.querySelector(`[data-od-id="${odId}"]`);
        if (!dz || dz.dataset.wired) return;
        dz.dataset.wired = '1';
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = tipos.join(',');
        input.style.display = 'none';
        dz.appendChild(input);
        const tratar = (file) => {
            if (!file) return;
            if (!tipos.some(t => file.type === t || file.name.toLowerCase().endsWith(t.replace('image/', '.')))) {
                alert('Formato inválido. Use PNG ou JPG.');
                return;
            }
            if (file.size > maxBytes) {
                alert(`Arquivo maior que ${Math.round(maxBytes / 1024 / 1024)}MB.`);
                return;
            }
            const reader = new FileReader();
            reader.onload = () => {
                const p = lerPrefs();
                p[chave] = reader.result;
                try {
                    localStorage.setItem(PREFS_KEY, JSON.stringify(p));
                } catch (e) {
                    alert('Imagem carregada no preview, mas grande demais para salvar localmente.');
                    p[chave] = null;
                }
                aplicarPreferencias();
            };
            reader.readAsDataURL(file);
        };
        dz.addEventListener('click', () => input.click());
        input.addEventListener('change', () => tratar(input.files[0]));
        ['dragover', 'dragenter'].forEach(ev => dz.addEventListener(ev, (e) => e.preventDefault()));
        dz.addEventListener('drop', (e) => {
            e.preventDefault();
            if (e.dataTransfer.files.length > 0) tratar(e.dataTransfer.files[0]);
        });
    }

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
            // Gestão enxerga rascunhos: prefere a lista do organizador logado.
            const user = window.Auth ? window.Auth.getUser() : null;
            const eventos = (user && user.usuarioId)
                ? await window.API.Eventos.listarPorOrganizador(user.usuarioId).catch(
                    () => window.API.Eventos.listar())
                : await window.API.Eventos.listar();
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
    configurarDropzone('logo-dropzone', 'logo', LOGO_MAX, ['image/png', 'image/jpeg']);
    configurarDropzone('signature-dropzone', 'assinatura', ASS_MAX, ['image/png']);
    document.querySelectorAll('.cert-card').forEach(card => {
        card.addEventListener('click', () => {
            document.querySelectorAll('.cert-card').forEach(c => c.classList.remove('active'));
            card.classList.add('active');
        });
    });
    aplicarPreferencias();
    if (saveBtn) {
        saveBtn.addEventListener('click', () => {
            if (salvarPreferencias()) window.API.exibirSucesso('Preferências salvas neste navegador!');
            const form = document.getElementById('regras-evento-form');
            if (form) form.requestSubmit();
        });
    }
    if (restoreBtn) {
        restoreBtn.addEventListener('click', () => {
            localStorage.removeItem(PREFS_KEY);
            ['cfg-org-nome', 'cfg-org-cnpj', 'cfg-org-tel', 'cfg-org-email', 'cfg-reserva']
                .forEach(id => { const el = document.getElementById(id); if (el) el.value = el.defaultValue; });
            document.querySelectorAll('.toggle').forEach((t, i) => t.classList.toggle('active', i < 2));
            document.querySelectorAll('.cert-card').forEach((c, i) => c.classList.toggle('active', i === 0));
            mostrarPreview('logo-dropzone', null, 'PNG, JPG (máx. 5MB)');
            mostrarPreview('signature-dropzone', null, 'PNG com fundo transparente (máx. 2MB)');
            window.API.exibirSucesso('Padrões restaurados.');
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
