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

    // Modal padrão System Design (substitui prompt/confirm nativos).
    function garantirCssModal() {
        if (document.getElementById('at-modal-css')) return;
        const style = document.createElement('style');
        style.id = 'at-modal-css';
        style.textContent = [
            '.at-overlay{position:fixed;inset:0;background:rgba(2,6,23,.55);display:flex;align-items:center;justify-content:center;padding:20px;z-index:100}',
            '.at-modal{background:#fff;border-radius:12px;max-width:540px;width:100%;max-height:90vh;overflow-y:auto;box-shadow:0 24px 64px rgba(2,6,23,.3)}',
            '.at-head{display:flex;align-items:center;justify-content:space-between;padding:20px 24px 0}',
            '.at-head h2{margin:0;font-size:18px}',
            '.at-close{border:0;background:transparent;font-size:20px;line-height:1;color:var(--muted,#64748B);padding:4px 8px;border-radius:6px}',
            '.at-close:hover{background:#F1F5F9;color:var(--ink,#0F172A)}',
            '.at-body{padding:16px 24px 24px;display:grid;gap:14px}',
            '.at-field{display:grid;gap:6px}',
            '.at-field label{font-size:12px;font-weight:700;color:var(--ink,#0F172A)}',
            '.at-field label small{font-weight:400;color:var(--muted,#64748B)}',
            '.at-field input,.at-field textarea,.at-field select{border:1px solid var(--line,#E2E8F0);border-radius:6px;padding:10px 12px;font:inherit;width:100%;box-sizing:border-box;background:#fff}',
            '.at-field input:focus-visible,.at-field textarea:focus-visible,.at-field select:focus-visible{outline:3px solid var(--mist,#BAE6FD);outline-offset:1px;border-color:#38BDF8}',
            '.at-row{display:grid;grid-template-columns:1fr 1fr;gap:14px}',
            '@media(max-width:560px){.at-row{grid-template-columns:1fr}}',
            '.at-erro{display:none;background:#FEF2F2;border:1px solid #FECACA;color:#B91C1C;border-radius:6px;padding:10px 12px;font-size:13px}',
            '.at-erro.visivel{display:block}',
            '.at-acoes{display:flex;justify-content:flex-end;gap:10px;margin-top:4px}',
            '.at-btn{border:1px solid var(--line,#E2E8F0);border-radius:6px;background:#fff;color:var(--ink,#0F172A);padding:11px 18px;font-size:13px;font-weight:700;cursor:pointer}',
            '.at-btn:hover{background:#F8FAFC}',
            '.at-btn.primary{border:0;background:#2563EB;color:#fff}',
            '.at-btn.primary:hover{background:#1D4ED8}',
            '.at-btn.danger{border:0;background:#DC2626;color:#fff}',
            '.at-btn.danger:hover{background:#B91C1C}',
            '.at-btn:disabled{opacity:.6;cursor:wait}',
            '.at-lista{display:grid;gap:8px}',
            '.at-pessoa{display:flex;align-items:center;justify-content:space-between;gap:10px;border:1px solid var(--line,#E2E8F0);border-radius:6px;padding:8px 12px;font-size:13px}',
            '.at-pessoa small{color:var(--muted,#64748B)}',
            '.at-rm{border:0;background:transparent;color:#B91C1C;font-weight:700;cursor:pointer;font-size:12px}'
        ].join('\n');
        document.head.appendChild(style);
    }

    function abrirOverlay(titulo) {
        garantirCssModal();
        const overlay = document.createElement('div');
        overlay.className = 'at-overlay';
        const modal = document.createElement('div');
        modal.className = 'at-modal';
        modal.setAttribute('role', 'dialog');
        modal.setAttribute('aria-modal', 'true');
        overlay.appendChild(modal);
        document.body.appendChild(overlay);
        const head = document.createElement('div');
        head.className = 'at-head';
        const h2 = document.createElement('h2');
        h2.textContent = titulo;
        const x = document.createElement('button');
        x.className = 'at-close';
        x.type = 'button';
        x.textContent = '×';
        x.setAttribute('aria-label', 'Fechar');
        head.append(h2, x);
        modal.appendChild(head);
        const body = document.createElement('div');
        body.className = 'at-body';
        modal.appendChild(body);
        function fechar() { overlay.remove(); }
        x.addEventListener('click', fechar);
        overlay.addEventListener('mousedown', (ev) => { if (ev.target === overlay) fechar(); });
        document.addEventListener('keydown', function aoEsc(ev) {
            if (ev.key === 'Escape') { fechar(); document.removeEventListener('keydown', aoEsc); }
        });
        return { modal, body, fechar };
    }

    function campo(rotulo, nome, { tipo = 'text', obrigatorio = false, dica = '', valor = '', opcoes = null } = {}) {
        const wrap = document.createElement('div');
        wrap.className = 'at-field';
        const label = document.createElement('label');
        label.textContent = rotulo + (obrigatorio ? ' *' : '');
        if (dica) {
            const small = document.createElement('small');
            small.textContent = ' — ' + dica;
            label.appendChild(small);
        }
        let input;
        if (opcoes) {
            input = document.createElement('select');
            opcoes.forEach(([v, t]) => {
                const opt = document.createElement('option');
                opt.value = v;
                opt.textContent = t;
                input.appendChild(opt);
            });
        } else if (tipo === 'textarea') {
            input = document.createElement('textarea');
        } else {
            input = document.createElement('input');
            input.type = tipo;
        }
        input.name = nome;
        input.value = valor == null ? '' : String(valor);
        if (obrigatorio) input.required = true;
        wrap.append(label, input);
        return { wrap, input };
    }

    function linhaErro() {
        const erro = document.createElement('div');
        erro.className = 'at-erro';
        erro.setAttribute('role', 'alert');
        return erro;
    }

    function botoesAcao(fechar, salvarRotulo, classeSalvar) {
        const acoes = document.createElement('div');
        acoes.className = 'at-acoes';
        const btnCancelar = document.createElement('button');
        btnCancelar.className = 'at-btn';
        btnCancelar.type = 'button';
        btnCancelar.textContent = 'Cancelar';
        btnCancelar.addEventListener('click', fechar);
        const btnSalvar = document.createElement('button');
        btnSalvar.className = 'at-btn ' + (classeSalvar || 'primary');
        btnSalvar.type = 'submit';
        btnSalvar.textContent = salvarRotulo;
        acoes.append(btnCancelar, btnSalvar);
        return { acoes, btnCancelar, btnSalvar };
    }

    function paraDataHoraLocal(iso) {
        if (!iso) return '';
        const s = String(iso).slice(0, 16);
        return /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(s) ? s : '';
    }

    function exigirLogin() {
        const token = window.Auth ? window.Auth.getToken() : null;
        if (!token) {
            window.API.exibirErro(new window.API.APIError('Faça login como organizador para gerenciar atividades.', 403));
            setTimeout(() => { window.location.href = '/login'; }, 1500);
            return false;
        }
        return true;
    }

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
            // Gestão (organizador/admin): todos os status, inclui RASCUNHO.
            // Visitante: lista pública.
            const user = window.Auth ? window.Auth.getUser() : null;
            const tipo = user && user.tipo ? String(user.tipo).toUpperCase() : '';
            const ehGestor = window.Auth && window.Auth.isAuthenticated && window.Auth.isAuthenticated()
                && user && user.usuarioId && (tipo === 'ORGANIZADOR' || tipo === 'ADMIN' || tipo === 'ADMINISTRADOR');
            const eventos = ehGestor
                ? await window.API.Eventos.listarPorOrganizador(user.usuarioId)
                : await window.API.Eventos.listar();
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
                <td><small>${a.maxVagas != null ? escapeHtml(String(a.maxVagas)) + ' vagas' : 'Sem limite'}</small></td>
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
        if (!exigirLogin()) return;
        const eventoId = eventSelect ? eventSelect.value : null;
        const atual = cache.find(a => a.id === atividadeId) || {};
        const { body, fechar } = abrirOverlay('Remarcar atividade');
        const form = document.createElement('form');
        form.className = 'at-body';
        form.noValidate = true;
        const fInicio = campo('Início', 'inicio', { tipo: 'datetime-local', obrigatorio: true, valor: paraDataHoraLocal(atual.inicio) });
        const fFim = campo('Fim', 'fim', { tipo: 'datetime-local', obrigatorio: true, valor: paraDataHoraLocal(atual.fim) });
        const fLocal = campo('Local', 'local', { obrigatorio: true, valor: atual.local || '' });
        const erro = linhaErro();
        const { acoes, btnSalvar } = botoesAcao(fechar, 'Salvar');
        const row = document.createElement('div');
        row.className = 'at-row';
        row.append(fInicio.wrap, fFim.wrap);
        form.append(row, fLocal.wrap, erro, acoes);
        body.appendChild(form);
        fInicio.input.focus();
        form.addEventListener('submit', async (ev) => {
            ev.preventDefault();
            erro.classList.remove('visivel');
            const inicio = fInicio.input.value;
            const fim = fFim.input.value;
            const local = fLocal.input.value.trim();
            if (!inicio || !fim) {
                erro.textContent = 'Informe início e fim.';
                erro.classList.add('visivel');
                return;
            }
            if (fim <= inicio) {
                erro.textContent = 'O fim deve ser depois do início.';
                erro.classList.add('visivel');
                return;
            }
            if (!local) {
                erro.textContent = 'O local é obrigatório.';
                erro.classList.add('visivel');
                return;
            }
            btnSalvar.disabled = true;
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
                fechar();
                await carregarGrade(eventoId);
            } catch (error) {
                window.API.exibirErro(error);
                erro.textContent = error.message || 'Não foi possível remarcar.';
                erro.classList.add('visivel');
            } finally {
                btnSalvar.disabled = false;
            }
        });
    }

    // RF-08: vincular pessoa com papel (lista, adiciona e remove)
    async function gerenciarPessoas(atividadeId) {
        if (!exigirLogin()) return;
        const eventoId = eventSelect ? eventSelect.value : null;
        const atual = cache.find(a => a.id === atividadeId) || {};
        const { body, fechar } = abrirOverlay(`Pessoas — ${atual.titulo || 'atividade'}`);
        const lista = document.createElement('div');
        lista.className = 'at-lista';
        lista.textContent = 'Carregando...';
        body.appendChild(lista);
        async function recarregar() {
            try {
                const resp = await fetch(`/api/eventos/${encodeURIComponent(eventoId)}/atividades/${encodeURIComponent(atividadeId)}/pessoas`, {
                    headers: authHeaders(false)
                });
                if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
                const pessoas = await resp.json();
                lista.innerHTML = '';
                if (!pessoas || pessoas.length === 0) {
                    const vazio = document.createElement('p');
                    vazio.style.cssText = 'margin:0;color:var(--muted,#64748B);font-size:13px';
                    vazio.textContent = 'Nenhuma pessoa vinculada.';
                    lista.appendChild(vazio);
                    return;
                }
                pessoas.forEach(p => {
                    const linha = document.createElement('div');
                    linha.className = 'at-pessoa';
                    const info = document.createElement('span');
                    const nome = document.createElement('strong');
                    nome.textContent = p.nome;
                    const papel = document.createElement('small');
                    papel.textContent = ' · ' + (p.papel || '');
                    info.append(nome, papel);
                    const rm = document.createElement('button');
                    rm.className = 'at-rm';
                    rm.type = 'button';
                    rm.textContent = 'Remover';
                    rm.addEventListener('click', async () => {
                        try {
                            const r = await fetch(`/api/eventos/${encodeURIComponent(eventoId)}/pessoas/${encodeURIComponent(p.id)}`, {
                                method: 'DELETE',
                                headers: authHeaders(false)
                            });
                            if (!r.ok) throw new Error(`HTTP ${r.status}`);
                            window.API.exibirSucesso('Vínculo removido!');
                            await recarregar();
                        } catch (error) {
                            window.API.exibirErro(error);
                        }
                    });
                    linha.append(info, rm);
                    lista.appendChild(linha);
                });
            } catch (error) {
                lista.textContent = 'Não foi possível carregar.';
            }
        }
        const form = document.createElement('form');
        form.className = 'at-body';
        form.style.padding = '0';
        form.noValidate = true;
        const fNome = campo('Nome', 'nome', { obrigatorio: true });
        const fPapel = campo('Papel', 'papel', {
            obrigatorio: true,
            opcoes: [['PALESTRANTE', 'Palestrante'], ['APRESENTADOR', 'Apresentador'], ['RESPONSAVEL', 'Responsável']]
        });
        const erro = linhaErro();
        const { acoes, btnSalvar } = botoesAcao(fechar, 'Vincular');
        form.append(fNome.wrap, fPapel.wrap, erro, acoes);
        body.appendChild(form);
        await recarregar();
        form.addEventListener('submit', async (ev) => {
            ev.preventDefault();
            erro.classList.remove('visivel');
            const nome = fNome.input.value.trim();
            const papel = fPapel.input.value;
            if (!nome || !papel) {
                erro.textContent = 'Informe nome e papel.';
                erro.classList.add('visivel');
                return;
            }
            btnSalvar.disabled = true;
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
                fNome.input.value = '';
                await recarregar();
            } catch (error) {
                window.API.exibirErro(error);
                erro.textContent = error.message || 'Não foi possível vincular.';
                erro.classList.add('visivel');
            } finally {
                btnSalvar.disabled = false;
            }
        });
    }

    // Criar atividade (RF-05/06/19)
    if (createBtn) {
        createBtn.addEventListener('click', async () => {
            if (!exigirLogin()) return;
            const eventoId = eventSelect ? eventSelect.value : null;
            if (!eventoId) {
                window.API.exibirErro(new window.API.APIError('Selecione um evento.', 400));
                return;
            }
            const { body, fechar } = abrirOverlay('Cadastrar atividade');
            const form = document.createElement('form');
            form.className = 'at-body';
            form.noValidate = true;
            const fTitulo = campo('Título', 'titulo', { obrigatorio: true, dica: 'mínimo 3 caracteres' });
            const fInicio = campo('Início', 'inicio', { tipo: 'datetime-local', obrigatorio: true });
            const fFim = campo('Fim', 'fim', { tipo: 'datetime-local', obrigatorio: true });
            const fLocal = campo('Local', 'local', { obrigatorio: true });
            const fTipo = campo('Tipo', 'tipo', {
                dica: 'opcional',
                opcoes: [['', 'Sem tipo'], ['PALESTRA', 'Palestra'], ['WORKSHOP', 'Workshop'], ['PAINEL', 'Painel'], ['MINICURSO', 'Minicurso']]
            });
            const fTrilha = campo('Trilha', 'trilha', { dica: 'opcional' });
            const fPolitica = campo('Critério de presença', 'politica', {
                opcoes: [['UNICA', 'Única (um check-in)'], ['ENTRADA_SAIDA', 'Entrada e saída']]
            });
            const fVagas = campo('Vagas', 'maxVagas', { tipo: 'number', dica: 'vazio = sem limite' });
            fVagas.input.min = '1';
            const erro = linhaErro();
            const { acoes, btnSalvar } = botoesAcao(fechar, 'Cadastrar');
            const row = document.createElement('div');
            row.className = 'at-row';
            row.append(fInicio.wrap, fFim.wrap);
            form.append(fTitulo.wrap, row, fLocal.wrap, fTipo.wrap, fTrilha.wrap, fPolitica.wrap, fVagas.wrap, erro, acoes);
            body.appendChild(form);
            fTitulo.input.focus();
            form.addEventListener('submit', async (ev) => {
                ev.preventDefault();
                erro.classList.remove('visivel');
                const titulo = fTitulo.input.value.trim();
                const inicio = fInicio.input.value;
                const fim = fFim.input.value;
                const local = fLocal.input.value.trim();
                if (titulo.length < 3) {
                    erro.textContent = 'O título deve ter ao menos 3 caracteres.';
                    erro.classList.add('visivel');
                    return;
                }
                if (!inicio || !fim) {
                    erro.textContent = 'Informe início e fim.';
                    erro.classList.add('visivel');
                    return;
                }
                if (fim <= inicio) {
                    erro.textContent = 'O fim deve ser depois do início.';
                    erro.classList.add('visivel');
                    return;
                }
                if (!local) {
                    erro.textContent = 'O local é obrigatório.';
                    erro.classList.add('visivel');
                    return;
                }
                const vagasTxt = fVagas.input.value.trim();
                let maxVagas = null;
                if (vagasTxt !== '') {
                    maxVagas = Number(vagasTxt);
                    if (!Number.isInteger(maxVagas) || maxVagas <= 0) {
                        erro.textContent = 'Vagas deve ser um inteiro positivo.';
                        erro.classList.add('visivel');
                        return;
                    }
                }
                btnSalvar.disabled = true;
                try {
                    const resp = await fetch(`/api/eventos/${encodeURIComponent(eventoId)}/atividades`, {
                        method: 'POST',
                        headers: authHeaders(true),
                        body: JSON.stringify({
                            titulo, inicio, fim, local,
                            tipo: fTipo.input.value || null,
                            trilha: fTrilha.input.value.trim() || null,
                            politicaFrequencia: fPolitica.input.value,
                            maxVagas
                        })
                    });
                    if (!resp.ok) {
                        const err = await resp.json().catch(() => ({}));
                        throw new window.API.APIError(err.message || `HTTP ${resp.status}`, resp.status);
                    }
                    window.API.exibirSucesso('Atividade criada!');
                    fechar();
                    await carregarGrade(eventoId);
                } catch (error) {
                    window.API.exibirErro(error);
                    erro.textContent = error.message || 'Não foi possível criar.';
                    erro.classList.add('visivel');
                } finally {
                    btnSalvar.disabled = false;
                }
            });
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
