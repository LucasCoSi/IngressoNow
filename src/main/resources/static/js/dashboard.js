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

    // Ações rápidas: cards estáticos no protótipo → rotas reais.
    // Scanner → validador da frequência; certificados → relatórios
    // (métricas + CSV por evento). A seção "de Portaria" foi removida
    // (duplicava cards + sidebar).
    function tornarClicavel(el, destino) {
        if (!el || !destino || el.dataset.acaoLigada === 'ok') return;
        el.dataset.acaoLigada = 'ok';
        el.style.cursor = 'pointer';
        el.setAttribute('role', 'link');
        el.setAttribute('tabindex', '0');
        el.addEventListener('click', () => { window.location.href = destino; });
        el.addEventListener('keydown', (ev) => {
            if (ev.key === 'Enter' || ev.key === ' ') {
                ev.preventDefault();
                window.location.href = destino;
            }
        });
    }

    function ligarAcoesRapidas() {
        // Cards "Abrir Scanner QR" / "Liberar Certificados" (Ações Rápidas).
        tornarClicavel(document.querySelector('[data-od-id="session-opening"]'), '/frequencia');
        tornarClicavel(document.querySelector('[data-od-id="session-workshop"]'), '/relatorios');
        // "+ Cadastrar Minicurso" → gestão de atividades.
        tornarClicavel(document.querySelector('[data-od-id="create-event-cta"]'), '/atividades');
    }

    // Seletor de evento: o painel é multi-evento (o título fixo
    // "VI Simpósio..." era mock). Gestão vê os próprios eventos em
    // qualquer status; visitante/participante vê a lista pública.
    const CHAVE_EVENTO = 'dashboard_evento_id';

    function garantirCssSeletor() {
        if (document.getElementById('db-evento-css')) return;
        const style = document.createElement('style');
        style.id = 'db-evento-css';
        style.textContent = [
            '.db-evento{display:grid;gap:4px;justify-items:end}',
            '.db-evento label{font-size:11px;font-weight:700;color:var(--muted,#64748B)}',
            '.db-evento select{height:40px;border:1px solid var(--line,#E2E8F0);border-radius:6px;background:#fff;padding:0 12px;font:inherit;max-width:min(360px,60vw)}'
        ].join('\n');
        document.head.appendChild(style);
    }

    function cabecalho() {
        const h = document.querySelector('[data-od-id="dashboard-heading"]');
        if (!h) return {};
        return { titulo: h.querySelector('h1'), descricao: h.querySelector('p:not(.context-label)') };
    }

    function montarSeletor(eventos, selecionado, aoTrocar) {
        garantirCssSeletor();
        const h = document.querySelector('[data-od-id="dashboard-heading"]');
        if (!h || h.querySelector('[data-od-id="evento-select"]')) return;
        const wrap = document.createElement('div');
        wrap.className = 'db-evento';
        const label = document.createElement('label');
        label.textContent = 'Evento';
        label.setAttribute('for', 'db-evento-select');
        const select = document.createElement('select');
        select.id = 'db-evento-select';
        select.setAttribute('data-od-id', 'evento-select');
        eventos.forEach(ev => {
            const opt = document.createElement('option');
            opt.value = ev.id;
            opt.textContent = ev.titulo;
            if (ev.id === selecionado.id) opt.selected = true;
            select.appendChild(opt);
        });
        select.addEventListener('change', () => {
            const ev = eventos.find(e => e.id === select.value);
            if (!ev) return;
            try { localStorage.setItem(CHAVE_EVENTO, ev.id); } catch (e) { /* sem storage */ }
            aoTrocar(ev);
        });
        wrap.append(label, select);
        const cta = h.querySelector('[data-od-id="create-event-cta"]');
        h.insertBefore(wrap, cta || null);
    }

    function fmtHora(iso) {
        const s = String(iso || '');
        const m = s.match(/T(\d{2}:\d{2})/);
        return m ? m[1] : '—';
    }

    function renderizarGrade(atividades) {
        const lista = document.querySelector('[data-od-id="upcoming-events"] .event-list');
        if (!lista) return;
        lista.innerHTML = '';
        if (!atividades || atividades.length === 0) {
            const p = document.createElement('p');
            p.style.cssText = 'padding:16px;color:var(--muted,#64748B)';
            p.textContent = 'Nenhuma atividade cadastrada neste evento.';
            lista.appendChild(p);
            return;
        }
        atividades.forEach(a => {
            const art = document.createElement('article');
            art.className = 'event';
            const info = document.createElement('div');
            const titulo = document.createElement('span');
            titulo.className = 'event-title';
            titulo.textContent = a.titulo || 'Atividade';
            const meta = document.createElement('span');
            meta.className = 'event-meta';
            meta.textContent = [a.local, a.tipo].filter(Boolean).join(' • ') || '—';
            info.append(titulo, meta);
            const hora = document.createElement('span');
            hora.className = 'event-cell';
            hora.textContent = fmtHora(a.inicio);
            const lot = document.createElement('span');
            lot.className = 'event-cell';
            // Lotação por atividade sem fonte no MVP (honesto "—").
            lot.textContent = a.maxVagas != null ? `—/${a.maxVagas}` : '—';
            art.append(info, hora, lot);
            lista.appendChild(art);
        });
    }

    function ajustarRotuloVagas() {
        const card = document.querySelector('[data-od-id="metric-published-events"]');
        if (!card || card.dataset.rotuloOk === 'ok') return;
        card.dataset.rotuloOk = 'ok';
        const span = card.querySelector('span');
        const small = card.querySelector('small');
        if (span) span.textContent = 'Vagas Ocupadas';
        if (small) small.textContent = 'Inscritos / limite';
    }

    async function mostrarEvento(ev) {
        const { titulo, descricao } = cabecalho();
        if (titulo) titulo.textContent = ev.titulo;
        if (descricao) descricao.textContent = ev.descricao || 'Gestão de inscrições, programação, frequência e certificados.';
        setMetric('metric-active-events', '…');
        setMetric('metric-published-events', '…');
        setMetric('metric-activities', '…');
        setMetric('metric-participants', '—');
        let inscritos = 0;
        let presentes = 0;
        try {
            const lista = await get(`/api/inscricoes?eventoId=${encodeURIComponent(ev.id)}`);
            inscritos = lista.length;
            const csv = await fetch(`/api/controle/relatorio/presenca/${encodeURIComponent(ev.id)}`,
                { headers: authHeaders() }).then(r => r.ok ? r.text() : '');
            presentes = (csv.match(/PRESENTE/g) || []).length;
        } catch (err) {
            console.warn('[Dashboard] Evento sem dados:', ev.id);
        }
        const taxa = inscritos > 0
            ? (Math.round(presentes / inscritos * 1000) / 10).toString().replace('.', ',') + '%'
            : '—';
        ajustarRotuloVagas();
        setMetric('metric-active-events', inscritos);
        setMetric('metric-published-events', ev.maxVagas != null ? `${inscritos}/${ev.maxVagas}` : `${inscritos}`);
        setMetric('metric-activities', taxa);
        const capacity = document.querySelectorAll('[data-od-id="capacity-summary"] .capacity-line b');
        if (capacity.length >= 1) capacity[0].textContent = inscritos > 0
            ? `${presentes} (${taxa})` : `${presentes}`;
        if (capacity.length >= 2) capacity[1].textContent = taxa;
        renderizarGrade(ev.atividades);
    }

    function estadoVazio() {
        const { titulo, descricao } = cabecalho();
        if (titulo) titulo.textContent = 'Nenhum evento por aqui (ainda)';
        if (descricao) descricao.textContent = 'Crie seu primeiro evento na página de Eventos para ver métricas, grade e frequência.';
        ['metric-active-events', 'metric-published-events', 'metric-activities', 'metric-participants']
            .forEach(id => setMetric(id, '—'));
        renderizarGrade([]);
    }

    try {
        const user = window.Auth ? window.Auth.getUser() : null;
        const tipo = user && user.tipo ? String(user.tipo).toUpperCase() : '';
        const ehGestor = window.Auth && window.Auth.isAuthenticated && window.Auth.isAuthenticated()
            && user && user.usuarioId && (tipo === 'ORGANIZADOR' || tipo === 'ADMIN' || tipo === 'ADMINISTRADOR');
        const eventos = ehGestor
            ? await window.API.Eventos.listarPorOrganizador(user.usuarioId)
            : await window.API.Eventos.listar();
        if (!eventos || eventos.length === 0) {
            estadoVazio();
            return;
        }
        let salvo = null;
        try { salvo = localStorage.getItem(CHAVE_EVENTO); } catch (e) { /* sem storage */ }
        const selecionado = (salvo && eventos.find(e => e.id === salvo)) || eventos[0];
        montarSeletor(eventos, selecionado, (ev) => { mostrarEvento(ev).catch(err => console.error('[Dashboard] Erro:', err)); });
        await mostrarEvento(selecionado);
    } catch (error) {
        console.error('[Dashboard] Erro:', error);
        if (window.API) window.API.exibirErro(error);
    } finally {
        // Botões funcionam mesmo se as métricas falharem.
        try { ligarAcoesRapidas(); } catch (e) { console.warn('[Dashboard] Ações rápidas:', e); }
    }
});
