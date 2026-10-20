/**
 * configuracoes.js — Configurações por papel (§3).
 *
 * Organizador: regras do evento (RF-04/RF-13), critério de presença por
 * atividade (RF-19) e elegibilidade de certificado (RF-32); identidade da
 * organização em modo leitura.
 * Administrador: tudo acima (em qualquer evento) + parâmetros gerais da
 * organização (identidade + percentual padrão de certificado).
 *
 * Persistência: /api/eventos (regras), /api/eventos/{id}/atividades/{atId}
 * (frequência), /api/eventos/{id}/certificados/criterio (RF-32) e
 * /api/configuracoes (parâmetros gerais, PUT só do admin).
 */
document.addEventListener('DOMContentLoaded', async function () {
    const API = window.API;
    const Auth = window.Auth;
    const user = Auth && Auth.getUser ? Auth.getUser() : null;
    const tipo = String((user && user.tipo) || '').toUpperCase();
    const isAdmin = tipo === 'ADMINISTRADOR';
    const isOrganizador = tipo === 'ORGANIZADOR';

    // O nav.js já redireciona quem não é gestor; por segurança, não renderiza.
    if (!isAdmin && !isOrganizador) return;

    const state = { config: null, eventos: [], evento: null, criterio: null };

    const el = (id) => document.getElementById(id);

    function esc(valor) {
        const div = document.createElement('div');
        div.textContent = valor == null ? '' : String(valor);
        return div.innerHTML;
    }

    function vazio(mensagem) {
        const div = document.createElement('div');
        div.className = 'empty';
        div.textContent = mensagem;
        return div;
    }

    function secao(titulo, descricao, corpoHtml) {
        const sec = document.createElement('section');
        sec.className = 'settings-section';
        sec.innerHTML = `<h2>${esc(titulo)}</h2><p>${esc(descricao)}</p>${corpoHtml}`;
        return sec;
    }

    function quando(iso) {
        if (!iso) return '';
        const d = new Date(iso);
        return isNaN(d.getTime()) ? iso : d.toLocaleString('pt-BR');
    }

    const TABS = isAdmin
        ? [
            ['geral', 'Parâmetros Gerais'],
            ['evento', 'Regras do Evento'],
            ['frequencia', 'Frequência'],
            ['certificados', 'Certificados']
        ]
        : [
            ['evento', 'Regras do Evento'],
            ['frequencia', 'Frequência'],
            ['certificados', 'Certificados'],
            ['organizacao', 'Organização']
        ];
    let abaAtiva = TABS[0][0];
    // Permite abrir direto em uma aba (ex.: /configuracoes?aba=certificados).
    const abaPedida = new URLSearchParams(window.location.search).get('aba');
    if (abaPedida && TABS.some(([id]) => id === abaPedida)) abaAtiva = abaPedida;

    // ------------------------------------------------------------------
    // Cabeçalho / abas
    // ------------------------------------------------------------------
    function cabecalho() {
        if (isAdmin) {
            el('cfg-context-label').textContent = 'Administração do sistema';
            el('cfg-title').textContent = 'Configurações do Sistema';
            el('cfg-subtitle').textContent =
                'Parâmetros gerais da organização e regras de inscrição, frequência e certificados dos eventos.';
            document.title = 'Configurações do Sistema - IngressoNow';
        } else {
            el('cfg-context-label').textContent = 'Gestão de eventos';
            el('cfg-title').textContent = 'Configurações dos Eventos';
            el('cfg-subtitle').textContent =
                'Regras de inscrição, critério de presença e elegibilidade de certificado dos seus eventos.';
            document.title = 'Configurações dos Eventos - IngressoNow';
        }
    }

    function renderTabs() {
        const host = el('cfg-tabs');
        host.innerHTML = '';
        TABS.forEach(([id, rotulo]) => {
            const btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'settings-tab' + (id === abaAtiva ? ' active' : '');
            btn.setAttribute('role', 'tab');
            btn.setAttribute('data-od-id', 'tab-' + id);
            btn.textContent = rotulo;
            btn.addEventListener('click', () => trocarAba(id));
            host.appendChild(btn);
        });
    }

    async function trocarAba(id) {
        abaAtiva = id;
        renderTabs();
        await renderPainel();
    }

    // ------------------------------------------------------------------
    // Dados
    // ------------------------------------------------------------------
    function atualizarIdentidade() {
        const nome = state.config && state.config.nomeInstituicao;
        if (!nome) return;
        const alvo = document.querySelector('[data-od-id="sidebar-org"]')
            || document.querySelector('.sidebar .org strong');
        if (alvo) alvo.textContent = nome;
    }

    async function carregarConfig() {
        state.config = await API.Configuracoes.obter();
        atualizarIdentidade();
    }

    async function carregarEventos() {
        const listaBruta = isAdmin
            ? await API.Configuracoes.listarEventos(true).catch(() => API.Eventos.listar())
            : await API.Eventos.listarPorOrganizador(user.usuarioId)
                .catch(() => API.Eventos.listar());
        state.eventos = Array.isArray(listaBruta) ? listaBruta : [];
        if (state.eventos.length > 0) {
            await selecionarEvento(state.eventos[0].id);
        } else {
            state.evento = null;
            state.criterio = null;
        }
    }

    async function selecionarEvento(id) {
        if (!id) {
            state.evento = null;
            state.criterio = null;
            return;
        }
        state.evento = await API.Eventos.buscar(id);
        state.criterio = null;
    }

    async function garantirCriterio() {
        if (!state.evento || state.criterio !== null) return;
        try {
            state.criterio = await API.Configuracoes.buscarCriterio(state.evento.id);
        } catch (erro) {
            state.criterio = { obrigatorias: [] };
            API.exibirErro(erro);
        }
    }

    function barraEvento() {
        const wrap = document.createElement('div');
        wrap.className = 'event-bar';
        const grupo = document.createElement('div');
        grupo.className = 'form-group';
        const label = document.createElement('label');
        label.className = 'form-label';
        label.setAttribute('for', 'cfg-evento');
        label.textContent = 'Evento';
        const select = document.createElement('select');
        select.className = 'form-select';
        select.id = 'cfg-evento';
        if (state.eventos.length === 0) {
            select.innerHTML = '<option value="">Nenhum evento disponível</option>';
            select.disabled = true;
        } else {
            select.innerHTML = state.eventos.map(e =>
                `<option value="${esc(e.id)}">${esc(e.titulo)} — ${esc(e.status)}</option>`).join('');
            select.value = state.evento ? state.evento.id : state.eventos[0].id;
        }
        select.addEventListener('change', async () => {
            try {
                await selecionarEvento(select.value);
                await renderPainel();
            } catch (erro) {
                API.exibirErro(erro);
            }
        });
        grupo.append(label, select);
        wrap.appendChild(grupo);
        return wrap;
    }

    // ------------------------------------------------------------------
    // Painéis
    // ------------------------------------------------------------------
    async function renderPainel() {
        if (abaAtiva === 'certificados') await garantirCriterio();
        const host = el('cfg-panels');
        host.innerHTML = '';
        switch (abaAtiva) {
            case 'geral': host.appendChild(painelGeral()); break;
            case 'evento': await painelEvento(host); break;
            case 'frequencia': await painelFrequencia(host); break;
            case 'certificados': await painelCertificados(host); break;
            case 'organizacao': host.appendChild(painelOrganizacao()); break;
            default: host.appendChild(vazio('Aba não encontrada.'));
        }
    }

    // ---- Parâmetros gerais (admin) ----
    function painelGeral() {
        const c = state.config || {};
        const percentual = c.percentualCertificadoPadrao == null ? '' : c.percentualCertificadoPadrao;
        const atualizado = c.atualizadoEm
            ? `<p class="form-hint" style="margin-top:14px">Última atualização: ${esc(quando(c.atualizadoEm))}</p>`
            : '';
        const sec = secao('Parâmetros Gerais da Organização',
            'Informações da instituição e padrões aplicados aos eventos. Somente o administrador altera.',
            `
            <div class="form-group">
                <label class="form-label" for="cfg-nome">Nome da instituição</label>
                <input class="form-input" id="cfg-nome" value="${esc(c.nomeInstituicao)}" placeholder="Ex.: Universidade Federal do Sul">
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label class="form-label" for="cfg-sigla">Sigla</label>
                    <input class="form-input" id="cfg-sigla" maxlength="20" value="${esc(c.sigla)}" placeholder="Ex.: UFS">
                </div>
                <div class="form-group">
                    <label class="form-label" for="cfg-email">E-mail de contato</label>
                    <input class="form-input" id="cfg-email" type="email" value="${esc(c.emailContato)}" placeholder="contato@instituicao.br">
                </div>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label class="form-label" for="cfg-site">Site</label>
                    <input class="form-input" id="cfg-site" value="${esc(c.site)}" placeholder="https://...">
                </div>
                <div class="form-group">
                    <label class="form-label" for="cfg-pct">Percentual padrão de certificado (%)</label>
                    <input class="form-input" id="cfg-pct" type="number" min="0" max="100" value="${esc(percentual)}">
                    <p class="form-hint">Aplicado quando o evento não define critério próprio. Vazio = sem padrão.</p>
                </div>
            </div>
            <div class="form-actions">
                <button class="primary" id="cfg-salvar-geral" type="button">Salvar parâmetros</button>
            </div>
            ${atualizado}
            <div class="notice" style="margin-top:20px">
                <strong>Acesso administrativo.</strong> A criação/edição de usuários organizadores e a redefinição
                de senhas ficam em <a href="/usuarios">Usuários</a>. Não há criação de administrador pela interface.
            </div>`);
        sec.querySelector('#cfg-salvar-geral').addEventListener('click', salvarGeral);
        return sec;
    }

    async function salvarGeral() {
        const nome = el('cfg-nome').value.trim();
        if (nome.length < 3) {
            API.exibirErro(new API.APIError('Informe o nome da instituição (mínimo 3 caracteres).', 400));
            return;
        }
        const pctTexto = el('cfg-pct').value.trim();
        let percentual = null;
        if (pctTexto !== '') {
            percentual = Number(pctTexto);
            if (!Number.isInteger(percentual) || percentual < 0 || percentual > 100) {
                API.exibirErro(new API.APIError('O percentual padrão deve ser um inteiro entre 0 e 100.', 400));
                return;
            }
        }
        try {
            state.config = await API.Configuracoes.atualizar({
                nomeInstituicao: nome,
                sigla: el('cfg-sigla').value.trim() || null,
                emailContato: el('cfg-email').value.trim() || null,
                site: el('cfg-site').value.trim() || null,
                percentualCertificadoPadrao: percentual
            });
            atualizarIdentidade();
            API.exibirSucesso('Parâmetros gerais salvos.');
            await renderPainel();
        } catch (erro) {
            API.exibirErro(erro);
        }
    }

    // ---- Regras do evento (RF-04/RF-13) ----
    async function painelEvento(host) {
        if (!state.evento) {
            host.appendChild(vazio('Nenhum evento disponível para configurar.'));
            return;
        }
        host.appendChild(barraEvento());
        const e = state.evento;
        const inicio = (e.periodoInicio || '').slice(0, 16);
        const fim = (e.periodoFim || '').slice(0, 16);
        const sec = secao('Regras do Evento',
            'RF-04/RF-13 — período, local, fuso e como a inscrição controla atividades e vagas.',
            `
            <div class="form-row">
                <div class="form-group">
                    <label class="form-label" for="cfg-inicio">Início do evento</label>
                    <input class="form-input" id="cfg-inicio" type="datetime-local" value="${esc(inicio)}">
                </div>
                <div class="form-group">
                    <label class="form-label" for="cfg-fim">Fim do evento</label>
                    <input class="form-input" id="cfg-fim" type="datetime-local" value="${esc(fim)}">
                </div>
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label class="form-label" for="cfg-local">Local / modalidade</label>
                    <input class="form-input" id="cfg-local" value="${esc(e.local)}" placeholder="Ex.: Auditório central / Online">
                </div>
                <div class="form-group">
                    <label class="form-label" for="cfg-fuso">Fuso horário (IANA)</label>
                    <input class="form-input" id="cfg-fuso" value="${esc(e.fuso)}" placeholder="America/Sao_Paulo">
                </div>
            </div>
            <div class="check-list" style="margin-bottom:18px">
                <label class="check-item">
                    <input type="checkbox" id="cfg-exige" ${e.exigeEscolhaAtividades ? 'checked' : ''}>
                    <span>Exigir escolha de atividades na inscrição</span>
                </label>
                <label class="check-item">
                    <input type="checkbox" id="cfg-vagas-at" ${e.vagasPorAtividade ? 'checked' : ''}>
                    <span>Controlar vagas por atividade (RF-13)</span>
                </label>
            </div>
            <div class="form-actions">
                <button class="primary" id="cfg-salvar-evento" type="button">Salvar regras do evento</button>
            </div>`);
        sec.querySelector('#cfg-salvar-evento').addEventListener('click', salvarEvento);
        host.appendChild(sec);
    }

    async function salvarEvento() {
        if (!state.evento) return;
        const inicio = el('cfg-inicio').value.trim();
        const fim = el('cfg-fim').value.trim();
        if ((inicio && !fim) || (!inicio && fim)) {
            API.exibirErro(new API.APIError('Informe o início e o fim do evento juntos.', 400));
            return;
        }
        const body = {
            exigeEscolhaAtividades: el('cfg-exige').checked,
            vagasPorAtividade: el('cfg-vagas-at').checked
        };
        if (inicio) body.periodoInicio = inicio;
        if (fim) body.periodoFim = fim;
        const local = el('cfg-local').value.trim();
        const fuso = el('cfg-fuso').value.trim();
        if (local) body.local = local;
        if (fuso) body.fuso = fuso;
        try {
            state.evento = await API.Eventos.atualizar(state.evento.id, body);
            API.exibirSucesso('Regras do evento salvas.');
            await renderPainel();
        } catch (erro) {
            API.exibirErro(erro);
        }
    }

    // ---- Frequência (RF-19) ----
    async function painelFrequencia(host) {
        if (!state.evento) {
            host.appendChild(vazio('Nenhum evento disponível para configurar.'));
            return;
        }
        host.appendChild(barraEvento());
        const atividades = state.evento.atividades || [];
        if (atividades.length === 0) {
            host.appendChild(vazio('Cadastre atividades no evento para definir o critério de presença.'));
            return;
        }
        const linhas = atividades.map(a => {
            const politica = a.politicaFrequencia || 'UNICA';
            return `<tr>
                <td><strong>${esc(a.titulo)}</strong><br><small>${esc(API.formatarDataHora(a.inicio))} — ${esc(a.local)}</small></td>
                <td><span class="badge">${esc(a.tipo || 'Sem tipo')}</span></td>
                <td>
                    <select class="form-select" data-atividade="${esc(a.id)}">
                        <option value="UNICA" ${politica === 'UNICA' ? 'selected' : ''}>Check-in único</option>
                        <option value="ENTRADA_SAIDA" ${politica === 'ENTRADA_SAIDA' ? 'selected' : ''}>Entrada e saída</option>
                    </select>
                </td>
            </tr>`;
        }).join('');
        const sec = secao('Critério de Presença por Atividade',
            'RF-19 — escolha como a presença é validada em cada atividade do evento.',
            `
            <div class="notice">
                <strong>Atenção:</strong> trocar o critério não reescreve o histórico já consolidado.
                <em>Check-in único</em>: uma leitura por atividade. <em>Entrada e saída</em>: duas leituras (par).
            </div>
            <table class="table">
                <thead><tr><th>Atividade</th><th>Tipo</th><th>Critério</th></tr></thead>
                <tbody>${linhas}</tbody>
            </table>
            <div class="form-actions" style="margin-top:16px">
                <button class="primary" id="cfg-salvar-freq" type="button">Salvar critérios</button>
            </div>`);
        sec.querySelector('#cfg-salvar-freq').addEventListener('click', salvarFrequencia);
        host.appendChild(sec);
    }

    async function salvarFrequencia() {
        if (!state.evento) return;
        const seletores = Array.from(document.querySelectorAll('#cfg-panels select[data-atividade]'));
        const original = new Map((state.evento.atividades || []).map(a =>
            [a.id, a.politicaFrequencia || 'UNICA']));
        const alterados = seletores.filter(s => s.value !== original.get(s.dataset.atividade));
        if (alterados.length === 0) {
            API.exibirSucesso('Nenhuma alteração de critério.');
            return;
        }
        try {
            for (const s of alterados) {
                await API.Configuracoes.definirPoliticaFrequencia(
                    state.evento.id, s.dataset.atividade, s.value);
            }
            state.evento = await API.Eventos.buscar(state.evento.id);
            API.exibirSucesso('Critérios de presença salvos.');
            await renderPainel();
        } catch (erro) {
            API.exibirErro(erro);
        }
    }

    // ---- Certificados (RF-32) ----
    async function painelCertificados(host) {
        if (!state.evento) {
            host.appendChild(vazio('Nenhum evento disponível para configurar.'));
            return;
        }
        host.appendChild(barraEvento());
        const criterio = state.criterio || { obrigatorias: [] };
        const obrigatorias = criterio.obrigatorias || [];
        const atividades = state.evento.atividades || [];
        const percentual = criterio.percentualMinimo == null ? '' : criterio.percentualMinimo;
        const padrao = state.config && state.config.percentualCertificadoPadrao;
        const avisoPadrao = padrao == null
            ? 'A organização não definiu percentual padrão. Vale apenas o critério salvo abaixo.'
            : `Padrão da organização: <strong>${padrao}%</strong>, usado apenas quando este evento não tem critério próprio.`;
        const checks = atividades.length === 0
            ? '<span class="form-hint">Evento sem atividades.</span>'
            : atividades.map(a => `
                <label class="check-item">
                    <input type="checkbox" value="${esc(a.id)}" ${obrigatorias.includes(a.id) ? 'checked' : ''}>
                    <span>${esc(a.titulo)}</span>
                    <small>${esc(API.formatarDataHora(a.inicio))}</small>
                </label>`).join('');
        const sec = secao('Elegibilidade para Certificado',
            'RF-32 — percentual mínimo de presença e/ou atividades obrigatórias. Tudo opcional.',
            `
            <div class="notice">${avisoPadrao}</div>
            <div class="form-row">
                <div class="form-group">
                    <label class="form-label" for="cfg-cert-pct">Percentual mínimo de presença (%)</label>
                    <input class="form-input" id="cfg-cert-pct" type="number" min="0" max="100" value="${esc(percentual)}">
                    <p class="form-hint">Vazio = sem mínimo por percentual.</p>
                </div>
            </div>
            <div class="form-group">
                <label class="form-label">Atividades obrigatórias</label>
                <div class="check-list" id="cfg-cert-ats">${checks}</div>
                <p class="form-hint">Marque as atividades em que a presença é obrigatória para emitir o certificado.</p>
            </div>
            <div class="form-actions">
                <button class="primary" id="cfg-salvar-cert" type="button">Salvar critério</button>
            </div>`);
        sec.querySelector('#cfg-salvar-cert').addEventListener('click', salvarCriterio);
        host.appendChild(sec);
    }

    async function salvarCriterio() {
        if (!state.evento) return;
        const texto = el('cfg-cert-pct').value.trim();
        let percentual = null;
        if (texto !== '') {
            percentual = Number(texto);
            if (!Number.isInteger(percentual) || percentual < 0 || percentual > 100) {
                API.exibirErro(new API.APIError('O percentual deve ser um inteiro entre 0 e 100.', 400));
                return;
            }
        }
        const obrigatorias = Array.from(document.querySelectorAll('#cfg-cert-ats input:checked'))
            .map(i => i.value);
        try {
            state.criterio = await API.Configuracoes.definirCriterio(
                state.evento.id, percentual, obrigatorias);
            API.exibirSucesso('Critério de certificado salvo.');
            await renderPainel();
        } catch (erro) {
            API.exibirErro(erro);
        }
    }

    // ---- Organização (somente leitura p/ organizador) ----
    function painelOrganizacao() {
        const c = state.config || {};
        const percentual = c.percentualCertificadoPadrao == null
            ? 'Sem padrão definido'
            : `${c.percentualCertificadoPadrao}%`;
        return secao('Identidade da Organização',
            'Parâmetros gerais definidos pelo administrador do sistema.',
            `
            <dl class="kv">
                <dt>Nome da instituição</dt><dd>${esc(c.nomeInstituicao || '—')}</dd>
                <dt>Sigla</dt><dd>${esc(c.sigla || '—')}</dd>
                <dt>E-mail de contato</dt><dd>${esc(c.emailContato || '—')}</dd>
                <dt>Site</dt><dd>${esc(c.site || '—')}</dd>
                <dt>Percentual padrão de certificado</dt><dd>${esc(percentual)}</dd>
            </dl>
            <div class="notice" style="margin-top:18px">
                Somente o administrador altera estes parâmetros.
            </div>`);
    }

    // ------------------------------------------------------------------
    // Inicialização
    // ------------------------------------------------------------------
    cabecalho();
    renderTabs();
    try {
        await carregarConfig();
    } catch (erro) {
        API.exibirErro(erro);
    }
    try {
        await carregarEventos();
    } catch (erro) {
        API.exibirErro(erro);
    }
    await renderPainel();

    el('cfg-recarregar').addEventListener('click', async () => {
        try {
            await carregarConfig();
            await carregarEventos();
            await renderPainel();
            API.exibirSucesso('Dados recarregados.');
        } catch (erro) {
            API.exibirErro(erro);
        }
    });
});
