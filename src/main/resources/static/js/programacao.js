/**
 * programacao.js - Grade de atividades do evento
 * Integracao da pagina ingressonow-programacao.html com /api/eventos e /api/inscricoes
 * MVP: lista linear de atividades do evento selecionado, com botao de selecao.
 */

document.addEventListener('DOMContentLoaded', async function() {
    console.log('[Programacao] Inicializando pagina...');

    const eventSelect = document.getElementById('event-select');
    const scheduleGrid = document.querySelector('[data-od-id="schedule-grid"]');
    const addBtn = document.querySelector('[data-od-id="add-to-schedule-cta"]');
    const exportBtn = document.querySelector('[data-od-id="export-agenda-btn"]');
    const viewToggle = document.querySelector('[data-od-id="view-toggle"]');
    // 'grade' = cards, 'lista' = tabela
    let modoVisao = 'grade';

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

    function listaFiltrada() {
        return gradeCache.filter(a =>
            (!filtroTipo || a.tipo === filtroTipo) &&
            (!filtroTrilha || a.trilha === filtroTrilha) &&
            (!filtroLocal || a.local === filtroLocal));
    }

    function renderizarGradeFiltrada() {
        if (!scheduleGrid) return;
        pintarGrade(listaFiltrada());
    }

    // Métricas do banner/rodapé derivadas dos dados reais (sem mocks)
    function atualizarMetricas(atividades) {
        const total = atividades.length;
        const locais = new Set(atividades.map(a => a.local).filter(Boolean)).size;
        const elSessoes = document.getElementById('metric-sessoes');
        const elLocais = document.getElementById('metric-locais');
        const elTotal = document.getElementById('metric-total');
        if (elSessoes) elSessoes.textContent = String(total);
        if (elLocais) elLocais.textContent = String(locais);
        if (elTotal) elTotal.textContent = `${total} ${total === 1 ? 'sessão cadastrada' : 'sessões cadastradas'}`;
    }

    function garantirCssLista() {
        if (document.getElementById('pg-list-css')) return;
        const style = document.createElement('style');
        style.id = 'pg-list-css';
        style.textContent = [
            '.pg-table{width:100%;border-collapse:collapse;background:#fff;font-size:13px}',
            '.pg-table th{text-align:left;font-size:11px;text-transform:uppercase;letter-spacing:.04em;color:#64748B;',
            'border-bottom:1px solid #E2E8F0;padding:10px 12px}',
            '.pg-table td{border-bottom:1px solid #F1F5F9;padding:10px 12px;vertical-align:top}',
            '.pg-table tr:last-child td{border-bottom:0}',
            '.pg-mini{border:1px solid #E2E8F0;border-radius:6px;background:#fff;color:#1E3A8A;',
            'padding:7px 10px;font-size:12px;font-weight:700;cursor:pointer;white-space:nowrap}',
            '.pg-mini:hover{border-color:#0284C7;color:#0284C7}'
        ].join('\n');
        document.head.appendChild(style);
    }

    function pintarGrade(atividades) {
        atualizarMetricas(atividades);
        if (atividades.length === 0) {
            scheduleGrid.innerHTML = '<p style="padding:32px;text-align:center">Nenhuma atividade para os filtros selecionados.</p>';
            return;
        }
        if (modoVisao === 'lista') {
            garantirCssLista();
            scheduleGrid.innerHTML = `<table class="pg-table"><thead><tr>
                <th>Atividade</th><th>Horário</th><th>Local</th><th>Tipo / Trilha</th><th></th>
                </tr></thead><tbody>` + atividades.map(a => `
                <tr data-atividade-id="${a.id}">
                    <td><strong>${escapeHtml(a.titulo)}</strong></td>
                    <td>${escapeHtml(window.API.formatarDataHora(a.inicio))}<br>
                        <small style="color:#64748B">${escapeHtml(window.API.formatarDataHora(a.fim))}</small></td>
                    <td>${escapeHtml(a.local || '—')}</td>
                    <td>${escapeHtml(a.tipo || '—')}${a.trilha ? ' • ' + escapeHtml(a.trilha) : ''}</td>
                    <td style="text-align:right"><button class="pg-mini" data-action="selecionar" data-id="${a.id}">Selecionar</button></td>
                </tr>`).join('') + '</tbody></table>';
        } else {
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
        }

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
            atualizarMetricas([]);
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

    // ---------- Visão Grade/Lista ----------
    if (viewToggle && viewToggle.options.length >= 2) {
        viewToggle.options[0].value = 'grade';
        viewToggle.options[1].value = 'lista';
        viewToggle.value = modoVisao;
        viewToggle.addEventListener('change', () => {
            modoVisao = viewToggle.value === 'lista' ? 'lista' : 'grade';
            limparConflito();
            renderizarGradeFiltrada();
        });
    }

    // ---------- Exportar Agenda (CSV client-side) ----------
    function exportarAgenda() {
        const lista = listaFiltrada();
        if (lista.length === 0) {
            window.API.exibirErro(new window.API.APIError('Nada para exportar com os filtros atuais.', 400));
            return;
        }
        const esc = v => `"${String(v == null ? '' : v).replace(/"/g, '""')}"`;
        const linhas = lista.map(a => [a.titulo, a.inicio, a.fim, a.local, a.tipo, a.trilha].map(esc).join(';'));
        const csv = '﻿Titulo;Inicio;Fim;Local;Tipo;Trilha\n' + linhas.join('\n');
        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        const eventoId = eventSelect ? eventSelect.value : 'evento';
        link.href = url;
        link.download = `agenda-${eventoId}.csv`;
        document.body.appendChild(link);
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(url), 5000);
        window.API.exibirSucesso(`Agenda exportada (${lista.length} ${lista.length === 1 ? 'atividade' : 'atividades'})!`);
    }
    if (exportBtn) exportBtn.addEventListener('click', exportarAgenda);

    // ---------- Adicionar à Grade (modal, RF-05/06/19) ----------
    function garantirCssModal() {
        if (document.getElementById('pg-modal-css')) return;
        const style = document.createElement('style');
        style.id = 'pg-modal-css';
        style.textContent = [
            '.pg-overlay{position:fixed;inset:0;background:rgba(2,6,23,.55);display:flex;align-items:center;justify-content:center;padding:20px;z-index:100}',
            '.pg-modal{background:#fff;border-radius:12px;max-width:540px;width:100%;max-height:90vh;overflow-y:auto;box-shadow:0 24px 64px rgba(2,6,23,.3)}',
            '.pg-head{display:flex;align-items:center;justify-content:space-between;padding:20px 24px 0}',
            '.pg-head h2{margin:0;font-size:18px}',
            '.pg-close{border:0;background:transparent;font-size:20px;line-height:1;color:#64748B;padding:4px 8px;border-radius:6px}',
            '.pg-close:hover{background:#F1F5F9;color:#0F172A}',
            '.pg-body{padding:16px 24px 24px;display:grid;gap:14px}',
            '.pg-field{display:grid;gap:6px}',
            '.pg-field label{font-size:12px;font-weight:700;color:#0F172A}',
            '.pg-field input,.pg-field select{border:1px solid #E2E8F0;border-radius:6px;padding:10px 12px;font:inherit;width:100%;box-sizing:border-box;background:#fff}',
            '.pg-row{display:grid;grid-template-columns:1fr 1fr;gap:14px}',
            '@media(max-width:560px){.pg-row{grid-template-columns:1fr}}',
            '.pg-erro{display:none;background:#FEF2F2;border:1px solid #FECACA;color:#B91C1C;border-radius:6px;padding:10px 12px;font-size:13px}',
            '.pg-erro.visivel{display:block}',
            '.pg-acoes{display:flex;justify-content:flex-end;gap:10px;margin-top:4px}',
            '.pg-btn{border:1px solid #E2E8F0;border-radius:6px;background:#fff;color:#0F172A;padding:11px 18px;font-size:13px;font-weight:700;cursor:pointer}',
            '.pg-btn.primary{border:0;background:#2563EB;color:#fff}',
            '.pg-btn:disabled{opacity:.6;cursor:wait}'
        ].join('\n');
        document.head.appendChild(style);
    }

    function abrirModal(titulo) {
        garantirCssModal();
        const overlay = document.createElement('div');
        overlay.className = 'pg-overlay';
        overlay.innerHTML = `<div class="pg-modal" role="dialog" aria-modal="true">
            <div class="pg-head"><h2></h2><button class="pg-close" type="button" aria-label="Fechar">×</button></div>
            <div class="pg-body"></div></div>`;
        overlay.querySelector('h2').textContent = titulo;
        document.body.appendChild(overlay);
        const body = overlay.querySelector('.pg-body');
        function fechar() { overlay.remove(); }
        overlay.querySelector('.pg-close').addEventListener('click', fechar);
        overlay.addEventListener('mousedown', (ev) => { if (ev.target === overlay) fechar(); });
        return { body, fechar };
    }

    function campoModal(rotulo, nome, { tipo = 'text', obrigatorio = false, valor = '', opcoes = null } = {}) {
        const wrap = document.createElement('div');
        wrap.className = 'pg-field';
        const label = document.createElement('label');
        label.textContent = rotulo + (obrigatorio ? ' *' : '');
        let input;
        if (opcoes) {
            input = document.createElement('select');
            opcoes.forEach(([v, t]) => {
                const opt = document.createElement('option');
                opt.value = v;
                opt.textContent = t;
                input.appendChild(opt);
            });
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

    function abrirCriarAtividade() {
        const token = window.Auth ? window.Auth.getToken() : null;
        if (!token) {
            window.API.exibirErro(new window.API.APIError('Faça login como organizador para adicionar atividades.', 403));
            setTimeout(() => { window.location.href = '/login'; }, 1500);
            return;
        }
        const eventoId = eventSelect ? eventSelect.value : null;
        if (!eventoId) {
            window.API.exibirErro(new window.API.APIError('Selecione um evento.', 400));
            return;
        }
        const { body, fechar } = abrirModal('Adicionar à grade');
        const form = document.createElement('form');
        form.className = 'pg-body';
        form.style.padding = '0';
        form.noValidate = true;
        const fTitulo = campoModal('Título', 'titulo', { obrigatorio: true });
        const fInicio = campoModal('Início', 'inicio', { tipo: 'datetime-local', obrigatorio: true });
        const fFim = campoModal('Fim', 'fim', { tipo: 'datetime-local', obrigatorio: true });
        const fLocal = campoModal('Local', 'local', { obrigatorio: true });
        const fTipo = campoModal('Tipo', 'tipo', {
            opcoes: [['', 'Sem tipo'], ['PALESTRA', 'Palestra'], ['WORKSHOP', 'Workshop'], ['PAINEL', 'Painel'], ['MINICURSO', 'Minicurso']]
        });
        const fTrilha = campoModal('Trilha', 'trilha');
        const fPolitica = campoModal('Critério de presença', 'politica', {
            opcoes: [['UNICA', 'Única (um check-in)'], ['ENTRADA_SAIDA', 'Entrada e saída']]
        });
        const fVagas = campoModal('Vagas (vazio = sem limite)', 'maxVagas', { tipo: 'number' });
        fVagas.input.min = '1';
        const erro = document.createElement('div');
        erro.className = 'pg-erro';
        erro.setAttribute('role', 'alert');
        const acoes = document.createElement('div');
        acoes.className = 'pg-acoes';
        const btnCancelar = document.createElement('button');
        btnCancelar.className = 'pg-btn';
        btnCancelar.type = 'button';
        btnCancelar.textContent = 'Cancelar';
        btnCancelar.addEventListener('click', fechar);
        const btnSalvar = document.createElement('button');
        btnSalvar.className = 'pg-btn primary';
        btnSalvar.type = 'submit';
        btnSalvar.textContent = 'Adicionar';
        acoes.append(btnCancelar, btnSalvar);
        const row = document.createElement('div');
        row.className = 'pg-row';
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
                    headers: {
                        'Accept': 'application/json',
                        'Content-Type': 'application/json',
                        'Authorization': 'Bearer ' + token
                    },
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
                window.API.exibirSucesso('Atividade adicionada à grade!');
                fechar();
                await carregarGrade(eventoId);
            } catch (error) {
                window.API.exibirErro(error);
                erro.textContent = error.message || 'Não foi possível adicionar.';
                erro.classList.add('visivel');
            } finally {
                btnSalvar.disabled = false;
            }
        });
    }
    if (addBtn) addBtn.addEventListener('click', abrirCriarAtividade);

    await carregarEventos();
});
