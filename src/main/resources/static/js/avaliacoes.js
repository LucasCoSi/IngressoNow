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
    const exportNpsBtn = document.querySelector('[data-od-id="export-nps-btn"]');
    const ratingMetric = document.querySelector('[data-od-id="metric-rating"] b');
    const responseMetric = document.querySelector('[data-od-id="metric-response"] b');
    // Sem fonte no backend do MVP: NPS e melhor atividade (placeholders honestos).
    const npsMetric = document.querySelector('[data-od-id="metric-nps"] b');
    const bestMetric = document.querySelector('[data-od-id="metric-best"] b');
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
        if (npsMetric) npsMetric.textContent = '—';
        if (bestMetric) bestMetric.textContent = '—';
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
        if (npsMetric) npsMetric.textContent = '—';
        if (bestMetric) bestMetric.textContent = '—';
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

    function exigirLogin() {
        const token = window.Auth ? window.Auth.getToken() : null;
        if (!token) {
            window.API.exibirErro(new window.API.APIError('Faça login como organizador para gerenciar pesquisas.', 403));
            setTimeout(() => { window.location.href = '/login'; }, 1500);
            return false;
        }
        return true;
    }

    function garantirCssModal() {
        if (document.getElementById('av-modal-css')) return;
        const style = document.createElement('style');
        style.id = 'av-modal-css';
        style.textContent = [
            '.av-overlay{position:fixed;inset:0;background:rgba(2,6,23,.55);display:flex;align-items:center;justify-content:center;padding:20px;z-index:100}',
            '.av-modal{background:#fff;border-radius:12px;max-width:540px;width:100%;max-height:90vh;overflow-y:auto;box-shadow:0 24px 64px rgba(2,6,23,.3)}',
            '.av-head{display:flex;align-items:center;justify-content:space-between;padding:20px 24px 0}',
            '.av-head h2{margin:0;font-size:18px}',
            '.av-close{border:0;background:transparent;font-size:20px;line-height:1;color:#64748B;padding:4px 8px;border-radius:6px}',
            '.av-close:hover{background:#F1F5F9;color:#0F172A}',
            '.av-body{padding:16px 24px 24px;display:grid;gap:14px}',
            '.av-field{display:grid;gap:6px}',
            '.av-field label{font-size:12px;font-weight:700;color:#0F172A}',
            '.av-field input,.av-field select{border:1px solid #E2E8F0;border-radius:6px;padding:10px 12px;font:inherit;width:100%;box-sizing:border-box;background:#fff}',
            '.av-check{display:flex;align-items:center;gap:8px;font-size:13px}',
            '.av-erro{display:none;background:#FEF2F2;border:1px solid #FECACA;color:#B91C1C;border-radius:6px;padding:10px 12px;font-size:13px}',
            '.av-erro.visivel{display:block}',
            '.av-acoes{display:flex;justify-content:flex-end;gap:10px;margin-top:4px}',
            '.av-btn{border:1px solid #E2E8F0;border-radius:6px;background:#fff;color:#0F172A;padding:11px 18px;font-size:13px;font-weight:700;cursor:pointer}',
            '.av-btn.primary{border:0;background:#2563EB;color:#fff}',
            '.av-btn:disabled{opacity:.6;cursor:wait}'
        ].join('\n');
        document.head.appendChild(style);
    }

    function abrirModal(titulo) {
        garantirCssModal();
        const overlay = document.createElement('div');
        overlay.className = 'av-overlay';
        overlay.innerHTML = `<div class="av-modal" role="dialog" aria-modal="true">
            <div class="av-head"><h2></h2><button class="av-close" type="button" aria-label="Fechar">×</button></div>
            <div class="av-body"></div></div>`;
        overlay.querySelector('h2').textContent = titulo;
        document.body.appendChild(overlay);
        const body = overlay.querySelector('.av-body');
        function fechar() { overlay.remove(); }
        overlay.querySelector('.av-close').addEventListener('click', fechar);
        overlay.addEventListener('mousedown', (ev) => { if (ev.target === overlay) fechar(); });
        return { body, fechar };
    }

    // Criar pesquisa (organizador): 1 escala + 1 texto opcional, com política RN-15
    if (createBtn) {
        createBtn.addEventListener('click', async () => {
            if (!exigirLogin()) return;
            const eventoId = eventSelect ? eventSelect.value : null;
            if (!eventoId) {
                window.API.exibirErro(new window.API.APIError('Selecione um evento.', 400));
                return;
            }
            const { body, fechar } = abrirModal('Criar pesquisa');
            const form = document.createElement('form');
            form.className = 'av-body';
            form.style.padding = '0';
            form.noValidate = true;
            function campo(rotulo, nome, { tipo = 'text', obrigatorio = false, valor = '', opcoes = null } = {}) {
                const wrap = document.createElement('div');
                wrap.className = 'av-field';
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
                input.value = valor;
                if (obrigatorio) input.required = true;
                wrap.append(label, input);
                return { wrap, input };
            }
            const fTitulo = campo('Título do questionário', 'titulo', { obrigatorio: true });
            const fEnunciado = campo('Pergunta de escala', 'enunciado', { obrigatorio: true, valor: 'Como você avalia o evento?' });
            const fEscala = campo('Escala máxima (mínimo 2)', 'escalaMax', { tipo: 'number', valor: '5' });
            fEscala.input.min = '2';
            const fPolitica = campo('Identificação das respostas (RN-15)', 'politica', {
                opcoes: [['IDENTIFICADO', 'Identificado'], ['ANONIMO', 'Anônimo']]
            });
            const checkWrap = document.createElement('label');
            checkWrap.className = 'av-check';
            const checkTexto = document.createElement('input');
            checkTexto.type = 'checkbox';
            checkTexto.checked = true;
            checkWrap.append(checkTexto, document.createTextNode('Incluir pergunta aberta de comentários'));
            const erro = document.createElement('div');
            erro.className = 'av-erro';
            erro.setAttribute('role', 'alert');
            const acoes = document.createElement('div');
            acoes.className = 'av-acoes';
            const btnCancelar = document.createElement('button');
            btnCancelar.className = 'av-btn';
            btnCancelar.type = 'button';
            btnCancelar.textContent = 'Cancelar';
            btnCancelar.addEventListener('click', fechar);
            const btnSalvar = document.createElement('button');
            btnSalvar.className = 'av-btn primary';
            btnSalvar.type = 'submit';
            btnSalvar.textContent = 'Criar';
            acoes.append(btnCancelar, btnSalvar);
            form.append(fTitulo.wrap, fEnunciado.wrap, fEscala.wrap, fPolitica.wrap, checkWrap, erro, acoes);
            body.appendChild(form);
            fTitulo.input.focus();
            form.addEventListener('submit', async (ev) => {
                ev.preventDefault();
                erro.classList.remove('visivel');
                const titulo = fTitulo.input.value.trim();
                const enunciado = fEnunciado.input.value.trim();
                const escalaMax = Number(fEscala.input.value);
                if (titulo.length < 3) {
                    erro.textContent = 'O título deve ter ao menos 3 caracteres.';
                    erro.classList.add('visivel');
                    return;
                }
                if (!enunciado) {
                    erro.textContent = 'Informe a pergunta de escala.';
                    erro.classList.add('visivel');
                    return;
                }
                if (!Number.isInteger(escalaMax) || escalaMax < 2) {
                    erro.textContent = 'Escala máxima deve ser um inteiro ≥ 2.';
                    erro.classList.add('visivel');
                    return;
                }
                const perguntas = [
                    { enunciado, tipo: 'ESCALA_NUMERICA', obrigatoria: true, escalaMax }
                ];
                if (checkTexto.checked) {
                    perguntas.push({ enunciado: 'Comentários', tipo: 'TEXTO_LIVRE', obrigatoria: false });
                }
                btnSalvar.disabled = true;
                try {
                    await api(`/api/eventos/${encodeURIComponent(eventoId)}/questionarios`, {
                        method: 'POST',
                        body: JSON.stringify({ titulo, politicaIdentificacao: fPolitica.input.value, perguntas })
                    });
                    window.API.exibirSucesso('Questionário criado!');
                    fechar();
                    await carregarConsolidado(eventoId);
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

    // Exportar NPS (CSV client-side da consolidação RF-28)
    async function exportarNps() {
        const eventoId = eventSelect ? eventSelect.value : null;
        if (!eventoId) {
            window.API.exibirErro(new window.API.APIError('Selecione um evento.', 400));
            return;
        }
        try {
            const questionarios = await api(`/api/eventos/${encodeURIComponent(eventoId)}/questionarios`);
            if (!questionarios || questionarios.length === 0) {
                window.API.exibirErro(new window.API.APIError('Nada para exportar: evento sem questionários.', 400));
                return;
            }
            const q = questionarios[0];
            const resultado = await api(`/api/questionarios/${encodeURIComponent(q.id)}/resultados`);
            const esc = v => `"${String(v == null ? '' : v).replace(/"/g, '""')}"`;
            const linhas = [`Questionario;${q.titulo}`, `Total de avaliacoes;${resultado.totalAvaliacoes || 0}`, ''];
            linhas.push('Pergunta;Tipo;Respostas;Media;Distribuicao');
            (resultado.perguntas || []).forEach(p => {
                const dist = p.distribuicao
                    ? Object.entries(p.distribuicao).map(([v, n]) => `${v}:${n}`).join(' | ')
                    : '';
                const media = (p.media !== null && p.media !== undefined) ? Number(p.media).toFixed(2) : '';
                linhas.push([p.enunciado, p.tipo, p.totalRespostas, media, dist].map(esc).join(';'));
            });
            linhas.push('', 'Pergunta;Comentario');
            (resultado.perguntas || []).forEach(p => (p.comentarios || []).forEach(c =>
                linhas.push([p.enunciado, c].map(esc).join(';'))));
            const csv = '﻿' + linhas.join('\n');
            const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `nps-${eventoId}.csv`;
            document.body.appendChild(link);
            link.click();
            link.remove();
            setTimeout(() => URL.revokeObjectURL(url), 5000);
            window.API.exibirSucesso('NPS exportado!');
        } catch (error) {
            window.API.exibirErro(error);
        }
    }
    if (exportNpsBtn) exportNpsBtn.addEventListener('click', exportarNps);

    if (eventSelect) eventSelect.addEventListener('change', () => carregarConsolidado(eventSelect.value));

    await carregarEventos();
});
