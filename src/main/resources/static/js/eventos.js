/**
 * eventos.js - Listagem de Eventos (Admin)
 * Integração da página ingressonow-eventos.html com /api/eventos
 */

document.addEventListener('DOMContentLoaded', async function() {
    console.log('[Eventos] Inicializando página...');

    const tbody = document.querySelector('.events-table tbody');
    const counterElement = document.querySelector('.counter b');
    const filterStatus = document.getElementById('filter-status');
    const createBtn = document.querySelector('[data-od-id="create-new-event-cta"]');

    // Modal com a identidade do System Design (substitui prompt/confirm nativos).
    function garantirCssModal() {
        if (document.getElementById('ev-modal-css')) return;
        const style = document.createElement('style');
        style.id = 'ev-modal-css';
        style.textContent = [
            '.ev-overlay{position:fixed;inset:0;background:rgba(2,6,23,.55);display:flex;align-items:center;justify-content:center;padding:20px;z-index:100}',
            '.ev-modal{background:#fff;border-radius:12px;max-width:540px;width:100%;max-height:90vh;overflow-y:auto;box-shadow:0 24px 64px rgba(2,6,23,.3)}',
            '.ev-modal-head{display:flex;align-items:center;justify-content:space-between;padding:20px 24px 0}',
            '.ev-modal-head h2{margin:0;font-size:18px}',
            '.ev-modal-close{border:0;background:transparent;font-size:20px;line-height:1;color:var(--muted,#64748B);padding:4px 8px;border-radius:6px}',
            '.ev-modal-close:hover{background:#F1F5F9;color:var(--ink,#0F172A)}',
            '.ev-modal-body{padding:16px 24px 24px;display:grid;gap:14px}',
            '.ev-field{display:grid;gap:6px}',
            '.ev-field label{font-size:12px;font-weight:700;color:var(--ink,#0F172A)}',
            '.ev-field label small{font-weight:400;color:var(--muted,#64748B)}',
            '.ev-field input,.ev-field textarea{border:1px solid var(--line,#E2E8F0);border-radius:6px;padding:10px 12px;font:inherit;width:100%;box-sizing:border-box}',
            '.ev-field textarea{min-height:84px;resize:vertical}',
            '.ev-field input:focus-visible,.ev-field textarea:focus-visible{outline:3px solid var(--mist,#BAE6FD);outline-offset:1px;border-color:#38BDF8}',
            '.ev-row{display:grid;grid-template-columns:1fr 1fr;gap:14px}',
            '@media(max-width:560px){.ev-row{grid-template-columns:1fr}}',
            '.ev-erro{display:none;background:#FEF2F2;border:1px solid #FECACA;color:#B91C1C;border-radius:6px;padding:10px 12px;font-size:13px}',
            '.ev-erro.visivel{display:block}',
            '.ev-acoes{display:flex;justify-content:flex-end;gap:10px;margin-top:4px}',
            '.ev-btn{border:1px solid var(--line,#E2E8F0);border-radius:6px;background:#fff;color:var(--ink,#0F172A);padding:11px 18px;font-size:13px;font-weight:700;cursor:pointer}',
            '.ev-btn:hover{background:#F8FAFC}',
            '.ev-btn.primary{border:0;background:#2563EB;color:#fff}',
            '.ev-btn.primary:hover{background:#1D4ED8}',
            '.ev-btn.danger{border:0;background:#DC2626;color:#fff}',
            '.ev-btn.danger:hover{background:#B91C1C}',
            '.ev-btn:disabled{opacity:.6;cursor:wait}'
        ].join('\n');
        document.head.appendChild(style);
    }

    function abrirOverlay() {
        garantirCssModal();
        const overlay = document.createElement('div');
        overlay.className = 'ev-overlay';
        overlay.setAttribute('role', 'presentation');
        const modal = document.createElement('div');
        modal.className = 'ev-modal';
        modal.setAttribute('role', 'dialog');
        modal.setAttribute('aria-modal', 'true');
        overlay.appendChild(modal);
        document.body.appendChild(overlay);
        function fechar() { overlay.remove(); }
        overlay.addEventListener('mousedown', (ev) => { if (ev.target === overlay) fechar(); });
        document.addEventListener('keydown', function aoEsc(ev) {
            if (ev.key === 'Escape') { fechar(); document.removeEventListener('keydown', aoEsc); }
        });
        return { overlay, modal, fechar };
    }

    function campoTexto(rotulo, nome, { tipo = 'text', obrigatorio = false, dica = '', valor = '' } = {}) {
        const wrap = document.createElement('div');
        wrap.className = 'ev-field';
        const label = document.createElement('label');
        const req = obrigatorio ? ' *' : '';
        label.textContent = rotulo + req;
        if (dica) {
            const small = document.createElement('small');
            small.textContent = ' — ' + dica;
            label.appendChild(small);
        }
        let input;
        if (tipo === 'textarea') input = document.createElement('textarea');
        else { input = document.createElement('input'); input.type = tipo; }
        input.name = nome;
        input.value = valor == null ? '' : String(valor);
        if (obrigatorio) input.required = true;
        wrap.append(label, input);
        return { wrap, input };
    }

    function paraDataHoraLocal(iso) {
        if (!iso) return '';
        const s = String(iso).slice(0, 16);
        return /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(s) ? s : '';
    }

    // Modal de criar/editar evento. Fecha o ciclo: criar RASCUNHO → editar → atividade → publicar.
    function abrirModalEvento(modo, evento) {
        if (!exigirLogin()) return;
        const edicao = modo === 'editar';
        const { modal, fechar } = abrirOverlay();

        const head = document.createElement('div');
        head.className = 'ev-modal-head';
        const h2 = document.createElement('h2');
        h2.textContent = edicao ? 'Editar evento' : 'Criar novo evento';
        const btnFechar = document.createElement('button');
        btnFechar.className = 'ev-modal-close';
        btnFechar.type = 'button';
        btnFechar.textContent = '×';
        btnFechar.setAttribute('aria-label', 'Fechar');
        btnFechar.addEventListener('click', fechar);
        head.append(h2, btnFechar);

        const body = document.createElement('form');
        body.className = 'ev-modal-body';
        body.noValidate = true;

        const fTitulo = campoTexto('Título', 'titulo', { obrigatorio: true, dica: 'mínimo 3 caracteres', valor: evento ? evento.titulo : '' });
        const fDesc = campoTexto('Descrição', 'descricao', { tipo: 'textarea', obrigatorio: true, dica: 'mínimo 10 caracteres', valor: evento ? evento.descricao : '' });
        const fLocal = campoTexto('Local', 'local', { dica: 'opcional', valor: evento ? evento.local : '' });
        const fVagas = campoTexto('Máximo de vagas', 'maxVagas', { tipo: 'number', dica: 'opcional', valor: evento && evento.maxVagas != null ? evento.maxVagas : (edicao ? '' : '100') });
        fVagas.input.min = '1';
        const fInicio = campoTexto('Início', 'periodoInicio', { tipo: 'datetime-local', dica: 'opcional', valor: evento ? paraDataHoraLocal(evento.periodoInicio) : '' });
        const fFim = campoTexto('Fim', 'periodoFim', { tipo: 'datetime-local', dica: 'opcional', valor: evento ? paraDataHoraLocal(evento.periodoFim) : '' });
        const row = document.createElement('div');
        row.className = 'ev-row';
        row.append(fInicio.wrap, fFim.wrap);

        const erro = document.createElement('div');
        erro.className = 'ev-erro';
        erro.setAttribute('role', 'alert');

        const acoes = document.createElement('div');
        acoes.className = 'ev-acoes';
        const btnCancelar = document.createElement('button');
        btnCancelar.className = 'ev-btn';
        btnCancelar.type = 'button';
        btnCancelar.textContent = 'Cancelar';
        btnCancelar.addEventListener('click', fechar);
        const btnSalvar = document.createElement('button');
        btnSalvar.className = 'ev-btn primary';
        btnSalvar.type = 'submit';
        btnSalvar.textContent = edicao ? 'Salvar alterações' : 'Criar evento';
        acoes.append(btnCancelar, btnSalvar);

        body.append(fTitulo.wrap, fDesc.wrap, fLocal.wrap, row, fVagas.wrap, erro, acoes);
        modal.append(head, body);
        fTitulo.input.focus();

        function falhar(msg) {
            erro.textContent = msg;
            erro.classList.add('visivel');
        }

        body.addEventListener('submit', async (ev) => {
            ev.preventDefault();
            erro.classList.remove('visivel');
            const titulo = fTitulo.input.value.trim();
            const descricao = fDesc.input.value.trim();
            const local = fLocal.input.value.trim();
            const inicio = fInicio.input.value || null;
            const fim = fFim.input.value || null;
            const vagasTxt = fVagas.input.value.trim();
            if (titulo.length < 3) return falhar('O título deve ter ao menos 3 caracteres.');
            if (descricao.length < 10) return falhar('A descrição deve ter ao menos 10 caracteres.');
            if (inicio && fim && fim <= inicio) return falhar('O fim deve ser depois do início.');
            let maxVagas = null;
            if (vagasTxt !== '') {
                maxVagas = Number(vagasTxt);
                if (!Number.isInteger(maxVagas) || maxVagas <= 0) return falhar('O número de vagas deve ser um inteiro positivo.');
            }
            btnSalvar.disabled = true;
            try {
                if (edicao) {
                    await window.API.Eventos.atualizar(evento.id, {
                        titulo, descricao, maxVagas,
                        local: local || null, periodoInicio: inicio, periodoFim: fim
                    });
                    window.API.exibirSucesso('Evento atualizado com sucesso!');
                } else {
                    const user = window.Auth ? window.Auth.getUser() : null;
                    await window.API.Eventos.criar({
                        titulo, descricao,
                        organizadorId: user ? user.usuarioId : null,
                        maxVagas: maxVagas != null ? maxVagas : 100,
                        local: local || null, periodoInicio: inicio, periodoFim: fim
                    });
                    window.API.exibirSucesso('Evento criado como RASCUNHO!');
                }
                fechar();
                await carregarEventos();
            } catch (error) {
                console.error('[Eventos] Erro ao salvar:', error);
                window.API.exibirErro(error);
                falhar(error.message || 'Não foi possível salvar o evento.');
            } finally {
                btnSalvar.disabled = false;
            }
        });
    }

    function abrirConfirmacao({ titulo, mensagem, confirmarRotulo, aoConfirmar }) {
        const { modal, fechar } = abrirOverlay();
        const head = document.createElement('div');
        head.className = 'ev-modal-head';
        const h2 = document.createElement('h2');
        h2.textContent = titulo;
        head.appendChild(h2);
        const body = document.createElement('div');
        body.className = 'ev-modal-body';
        const p = document.createElement('p');
        p.style.margin = '0';
        p.textContent = mensagem;
        const acoes = document.createElement('div');
        acoes.className = 'ev-acoes';
        const btnCancelar = document.createElement('button');
        btnCancelar.className = 'ev-btn';
        btnCancelar.type = 'button';
        btnCancelar.textContent = 'Cancelar';
        btnCancelar.addEventListener('click', fechar);
        const btnOk = document.createElement('button');
        btnOk.className = 'ev-btn danger';
        btnOk.type = 'button';
        btnOk.textContent = confirmarRotulo;
        btnOk.addEventListener('click', async () => {
            btnOk.disabled = true;
            try { await aoConfirmar(); fechar(); }
            finally { btnOk.disabled = false; }
        });
        acoes.append(btnCancelar, btnOk);
        body.append(p, acoes);
        modal.append(head, body);
        btnCancelar.focus();
    }

    if (createBtn) {
        createBtn.addEventListener('click', () => abrirModalEvento('criar'));
    }

    if (!tbody) {
        console.warn('[Eventos] Tabela não encontrada no DOM');
        return;
    }

    // Estado da aplicação
    let eventosCache = [];

    /**
     * Carrega eventos da API
     */
    async function carregarEventos() {
        try {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:40px;">Carregando eventos...</td></tr>';

            // Gestão (organizador/admin logado): todos os status, inclui RASCUNHO recém-criado.
            // Visitante ou participante: lista pública (só PUBLICADO/INSCRICOES_ABERTAS).
            const user = window.Auth ? window.Auth.getUser() : null;
            const tipo = user && user.tipo ? String(user.tipo).toUpperCase() : '';
            const ehGestor = window.Auth && window.Auth.isAuthenticated && window.Auth.isAuthenticated()
                && user && user.usuarioId && (tipo === 'ORGANIZADOR' || tipo === 'ADMIN' || tipo === 'ADMINISTRADOR');
            const eventos = ehGestor
                ? await window.API.Eventos.listarPorOrganizador(user.usuarioId)
                : await window.API.Eventos.listar();
            eventosCache = eventos;

            console.log(`[Eventos] ${eventos.length} evento(s) carregado(s)`);
            renderizarEventos(eventos);
            atualizarAvisoRascunhos(eventosCache);
        } catch (error) {
            atualizarAvisoRascunhos([]);
            console.error('[Eventos] Erro ao carregar:', error);
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" style="text-align:center;padding:40px;color:var(--muted);">
                        <strong>Erro ao carregar eventos</strong><br>
                        ${error.message}
                    </td>
                </tr>
            `;
        }
    }

    /**
     * Aviso de rascunhos: conta os RASCUNHO reais (mock "2 eventos" sai
     * quando zera; plural correto em 1). Sem RASCUNHO, esconde o aviso.
     */
    function atualizarAvisoRascunhos(todos) {
        const aviso = document.querySelector('[data-od-id="management-notice"]');
        if (!aviso) return;
        const n = (todos || []).filter(e => e.status === 'RASCUNHO').length;
        if (n === 0) {
            aviso.style.display = 'none';
            return;
        }
        aviso.style.display = '';
        aviso.innerHTML = '';
        const forte = document.createElement('strong');
        forte.textContent = n === 1
            ? '1 evento em rascunho aguarda revisão.'
            : `${n} eventos em rascunho aguardam revisão.`;
        aviso.append(forte, document.createTextNode(
            ' Valide as informações e a política de inscrições antes de publicar.'));
    }

    /**
     * Renderiza lista de eventos na tabela
     */
    function renderizarEventos(eventos) {
        if (eventos.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:40px;color:var(--muted);">Nenhum evento encontrado</td></tr>';
            if (counterElement) counterElement.textContent = '0';
            return;
        }

        tbody.innerHTML = eventos.map(evento => {
            const editavel = evento.status === 'RASCUNHO';
            return `
            <tr data-evento-id="${evento.id}">
                <td>
                    <span class="event-name">${escapeHtml(evento.titulo)}</span>
                    <span class="event-location">${escapeHtml(evento.local || 'Online & Presencial')}</span>
                </td>
                <td class="event-date">${evento.periodoInicio ? escapeHtml(window.API.formatarData(evento.periodoInicio)) : '-'}</td>
                <td>
                    ${getBadgeStatus(evento.status)}
                </td>
                <td>
                    <div class="capacity-bar">
                        <div class="capacity-fill" style="width: 0%"></div>
                    </div>
                    <span class="capacity-text">0 / ${evento.maxVagas || '-'} inscritos</span>
                </td>
                <td>
                    <span class="badge badge-free">Gratuito</span>
                </td>
                <td>
                    <div class="actions">
                        <button class="action-btn" title="Visualizar" data-action="view" data-id="${evento.id}">
                            <svg class="icon"><use href="#i-eye"/></svg>
                        </button>
                        ${editavel ? `
                        <button class="action-btn" title="Editar" data-action="edit" data-id="${evento.id}">
                            <svg class="icon"><use href="#i-edit"/></svg>
                        </button>
                        <button class="action-btn" title="Excluir" data-action="delete" data-id="${evento.id}">
                            <svg class="icon"><use href="#i-trash"/></svg>
                        </button>` : `
                        <button class="action-btn" title="Publicar" data-action="publish" data-id="${evento.id}">
                            <svg class="icon"><use href="#i-check"/></svg>
                        </button>
                        <button class="action-btn" title="Encerrar" data-action="close" data-id="${evento.id}">
                            <svg class="icon"><use href="#i-x"/></svg>
                        </button>`}
                    </div>
                </td>
            </tr>
        `; }).join('');

        if (counterElement) {
            counterElement.textContent = eventos.length;
        }

        // Adicionar event listeners nas ações
        document.querySelectorAll('[data-action]').forEach(btn => {
            btn.addEventListener('click', handleAction);
        });
    }

    /**
     * Retorna badge HTML baseado no status
     */
    function getBadgeStatus(status) {
        const badges = {
            'PUBLICADO': '<span class="badge badge-published">Publicado</span>',
            'RASCUNHO': '<span class="badge badge-draft">Rascunho</span>',
            'ENCERRADO': '<span class="badge badge-closed">Encerrado</span>'
        };
        return badges[status] || '<span class="badge badge-draft">-</span>';
    }

    /**
     * Escapa HTML para prevenir XSS
     */
    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    /**
     * Handler de ações (visualizar, editar, excluir)
     */
    function handleAction(e) {
        const action = e.currentTarget.dataset.action;
        const eventoId = e.currentTarget.dataset.id;

        console.log(`[Eventos] Ação: ${action}, ID: ${eventoId}`);

        switch (action) {
            case 'view':
                window.location.href = `/evento-publico?id=${eventoId}`;
                break;
            case 'edit':
                editarEvento(eventoId);
                break;
            case 'delete':
                excluirEvento(eventoId);
                break;
            case 'publish':
                publicarEvento(eventoId);
                break;
            case 'close':
                encerrarEvento(eventoId);
                break;
        }
    }

    async function postAcao(eventoId, acao, rotulo) {
        if (!exigirLogin()) return;
        if (!confirm(`${rotulo} este evento?`)) return;
        try {
            const resp = await fetch(`/api/eventos/${encodeURIComponent(eventoId)}/${acao}`, {
                method: 'POST',
                headers: (() => {
                    const h = { 'Accept': 'application/json' };
                    const t = window.Auth ? window.Auth.getToken() : null;
                    if (t) h['Authorization'] = 'Bearer ' + t;
                    return h;
                })()
            });
            if (!resp.ok) {
                const err = await resp.json().catch(() => ({}));
                throw new window.API.APIError(err.message || `HTTP ${resp.status}`, resp.status);
            }
            window.API.exibirSucesso(`Evento ${rotulo.toLowerCase()} com sucesso!`);
            await carregarEventos();
        } catch (error) {
            console.error(`[Eventos] Erro ao ${acao}:`, error);
            window.API.exibirErro(error);
        }
    }

    async function publicarEvento(eventoId) {
        await postAcao(eventoId, 'publicar', 'Publicar');
    }

    async function encerrarEvento(eventoId) {
        await postAcao(eventoId, 'encerrar', 'Encerrar');
    }

    function exigirLogin() {
        const token = window.Auth ? window.Auth.getToken() : null;
        if (!token) {
            window.API.exibirErro(new window.API.APIError('Faca login como organizador para gerenciar eventos.', 403));
            setTimeout(() => { window.location.href = '/login'; }, 1500);
            return false;
        }
        return true;
    }

    async function editarEvento(eventoId) {
        if (!exigirLogin()) return;
        const evento = eventosCache.find(ev => ev.id === eventoId);
        if (!evento) {
            window.API.exibirErro(new window.API.APIError('Evento não encontrado na lista.', 404));
            return;
        }
        abrirModalEvento('editar', evento);
    }

    async function excluirEvento(eventoId) {
        if (!exigirLogin()) return;
        const evento = eventosCache.find(ev => ev.id === eventoId);
        const nome = evento ? evento.titulo : 'este evento';
        abrirConfirmacao({
            titulo: 'Excluir evento',
            mensagem: `Tem certeza que deseja excluir "${nome}"? Esta ação não pode ser desfeita.`,
            confirmarRotulo: 'Excluir',
            aoConfirmar: async () => {
                try {
                    await window.API.Eventos.deletar(eventoId);
                    window.API.exibirSucesso('Evento excluido com sucesso!');
                    await carregarEventos();
                } catch (error) {
                    console.error('[Eventos] Erro ao excluir:', error);
                    window.API.exibirErro(error);
                }
            }
        });
    }

    /**
     * Filtro por status
     */
    if (filterStatus) {
        filterStatus.addEventListener('change', function() {
            const statusSelecionado = this.value;

            if (!statusSelecionado) {
                renderizarEventos(eventosCache);
                atualizarAvisoRascunhos(eventosCache);
                return;
            }

            const eventosFiltrados = eventosCache.filter(evento => {
                if (statusSelecionado === 'published') return evento.status === 'PUBLICADO';
                if (statusSelecionado === 'draft') return evento.status === 'RASCUNHO';
                if (statusSelecionado === 'closed') return evento.status === 'ENCERRADO';
                return true;
            });

            renderizarEventos(eventosFiltrados);
            atualizarAvisoRascunhos(eventosCache);
        });
    }

    // Carregar eventos ao iniciar
    await carregarEventos();
});
