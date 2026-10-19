/**
 * inscricoes.js - Gestão de Inscrições (Admin)
 * Integração da página ingressonow-inscricoes.html com /api/inscricoes
 */

document.addEventListener('DOMContentLoaded', async function() {
    console.log('[Inscrições] Inicializando página...');

    const tbody = document.querySelector('.enrollments-table tbody');
    const eventDropdown = document.getElementById('event-select');
    const searchInput = document.querySelector('.search-box.full .search');
    const btnNova = document.querySelector('[data-od-id="new-enrollment-cta"]');
    const btnExportar = document.querySelector('[data-od-id="export-list-btn"]');

    if (!tbody) {
        console.warn('[Inscrições] Tabela não encontrada no DOM');
        return;
    }

    // Estado da aplicação
    let inscricoesCache = [];
    let eventosCache = [];

    function authHeaders() {
        const headers = { 'Accept': 'application/json' };
        const token = window.Auth ? window.Auth.getToken() : null;
        if (token) headers['Authorization'] = `Bearer ${token}`;
        return headers;
    }

    function usuarioLogado() {
        try {
            const u = window.Auth ? window.Auth.getUser() : null;
            if (!u) return {};
            return { id: u.usuarioId || u.id || u.usuarioID || '', nome: u.nome || '', email: u.email || '' };
        } catch (e) {
            return {};
        }
    }

    /**
     * Carrega eventos do organizador no seletor (troca o mock fixo do HTML).
     * Sem isso o filtro por evento nunca funciona (options sem value real).
     */
    async function carregarEventos() {
        if (!eventDropdown) return;
        try {
            const eu = usuarioLogado();
            const query = eu.id ? `?organizadorId=${encodeURIComponent(eu.id)}` : '';
            const resposta = await fetch(`/api/eventos${query}`, { headers: authHeaders() });
            if (!resposta.ok) throw new Error(`Falha ao carregar eventos (HTTP ${resposta.status})`);
            eventosCache = await resposta.json();
            eventDropdown.innerHTML = eventosCache.length === 0
                ? '<option value="">Nenhum evento encontrado</option>'
                : eventosCache.map(ev =>
                    `<option value="${ev.id}">${escapeHtml(ev.titulo || ev.id)}</option>`).join('');
        } catch (error) {
            console.error('[Inscrições] Erro ao carregar eventos:', error);
        }
    }

    /**
     * Carrega inscrições via GET /api/inscricoes.
     * Usa o evento do seletor como filtro quando presente.
     */
    async function carregarInscricoes() {
        try {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:40px;">Carregando inscrições...</td></tr>';

            const eventoId = eventDropdown ? eventDropdown.value : '';
            const query = eventoId ? `?eventoId=${encodeURIComponent(eventoId)}` : '';
            const resposta = await fetch(`/api/inscricoes${query}`, {
                method: 'GET',
                headers: (() => {
                    const headers = { 'Accept': 'application/json' };
                    const token = window.Auth ? window.Auth.getToken() : null;
                    if (token) headers['Authorization'] = `Bearer ${token}`;
                    return headers;
                })()
            });
            if (!resposta.ok) {
                throw new Error(`Falha ao carregar inscricoes (HTTP ${resposta.status})`);
            }
            const dados = await resposta.json();
            const inscricoes = dados.map(i => ({
                id: i.id,
                participante: { nome: i.participanteNome || i.participanteId, email: '', cpf: '-' },
                evento: i.eventoTitulo || i.eventoId,
                dataInscricao: i.dataInscricao,
                status: 'CONFIRMADA',
                categoria: ''
            }));

            inscricoesCache = inscricoes;

            console.log(`[Inscrições] ${inscricoes.length} inscrição(ões) carregada(s)`);
            renderizarInscricoes(inscricoes);

        } catch (error) {
            console.error('[Inscrições] Erro ao carregar:', error);
            tbody.innerHTML = `
                <tr>
                    <td colspan="6" style="text-align:center;padding:40px;color:var(--muted);">
                        <strong>Erro ao carregar inscrições</strong><br>
                        ${error.message}
                    </td>
                </tr>
            `;
        }
    }

    function pintarMetricas(total) {
        const set = (odId, valor) => {
            const el = document.querySelector(`[data-od-id="${odId}"] b`);
            if (el) el.textContent = valor;
        };
        // Só o total tem fonte na API; status/pagamento não existem no
        // domínio do MVP (toda inscrição listada está confirmada).
        set('metric-total', total);
        set('metric-confirmed', total);
        set('metric-pending', '—');
        set('metric-canceled', '—');
    }

    /**
     * Renderiza lista de inscrições na tabela
     */
    function renderizarInscricoes(inscricoes) {
        pintarMetricas(inscricoes.length);
        if (inscricoes.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:40px;color:var(--muted);">Nenhuma inscrição encontrada</td></tr>';
            return;
        }

        tbody.innerHTML = inscricoes.map(inscricao => {
            const iniciais = inscricao.participante.nome
                .split(' ')
                .map(n => n[0])
                .slice(0, 2)
                .join('');

            return `
                <tr data-inscricao-id="${inscricao.id}">
                    <td>
                        <span class="inscrição-code">#${inscricao.id.substring(0, 8).toUpperCase()}</span>
                    </td>
                    <td>
                        <div class="participant-info">
                            <div class="participant-avatar">${iniciais}</div>
                            <div class="participant-details">
                                <span class="participant-name">${escapeHtml(inscricao.participante.nome)}</span>
                                <span class="participant-contact">${escapeHtml(inscricao.participante.email)}</span>
                                <span class="participant-cpf">CPF: ${inscricao.participante.cpf}</span>
                            </div>
                        </div>
                    </td>
                    <td>${escapeHtml(inscricao.evento)}</td>
                    <td>${window.API.formatarDataHora(inscricao.dataInscricao)}</td>
                    <td>
                        ${getBadgeStatus(inscricao.status)}
                        ${getBadgeCategoria(inscricao.categoria)}
                    </td>
                    <td>
                        <div class="actions">
                            <button class="action-btn" title="Ver Agenda" data-action="agenda" data-id="${inscricao.id}">
                                <svg class="icon"><use href="#i-schedule"/></svg>
                            </button>
                            <button class="action-btn" title="Enviar E-mail" data-action="email" data-id="${inscricao.id}">
                                <svg class="icon"><use href="#i-mail"/></svg>
                            </button>
                            <button class="action-btn" title="Detalhes" data-action="view" data-id="${inscricao.id}">
                                <svg class="icon"><use href="#i-eye"/></svg>
                            </button>
                        </div>
                    </td>
                </tr>
            `;
        }).join('');

        // Adicionar event listeners
        document.querySelectorAll('[data-action]').forEach(btn => {
            btn.addEventListener('click', handleAction);
        });
    }

    /**
     * Retorna badge HTML baseado no status
     */
    function getBadgeStatus(status) {
        const badges = {
            'CONFIRMADA': '<span class="badge badge-confirmed">Confirmada</span>',
            'PENDENTE': '<span class="badge badge-pending">Pendente</span>',
            'CANCELADA': '<span class="badge badge-canceled">Cancelada</span>'
        };
        return badges[status] || '<span class="badge badge-pending">-</span>';
    }

    /**
     * Retorna badge HTML baseado na categoria
     */
    function getBadgeCategoria(categoria) {
        const badges = {
            'Estudante': '<span class="badge badge-student">Estudante</span>',
            'Profissional': '<span class="badge badge-professional">Profissional</span>',
            'VIP': '<span class="badge badge-vip">VIP</span>'
        };
        return badges[categoria] || '';
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
     * Modal no padrão visual do site (substitui prompt/alert nativos).
     * abrirModal({titulo, corpoHTML, botoes:[{rotulo, primario}], comInput})
     * resolve com {botao, valor} ou null ao fechar/cancelar.
     */
    function garantirModalCss() {
        if (document.getElementById('ingw-modal-css')) return;
        const css = document.createElement('style');
        css.id = 'ingw-modal-css';
        css.textContent = `
.ingw-overlay{position:fixed;inset:0;background:rgba(15,23,42,.55);display:flex;align-items:center;justify-content:center;z-index:50;padding:16px}
.ingw-card{background:#fff;border-radius:8px;max-width:440px;width:100%;box-shadow:0 18px 48px rgba(2,32,71,.35);overflow:hidden}
.ingw-head{background:#1E3A8A;color:#fff;padding:14px 18px;font-size:15px;font-weight:700}
.ingw-body{padding:18px;font-size:13px;color:#0F172A}
.ingw-body ul{margin:8px 0 0;padding-left:18px}
.ingw-input{width:100%;border:1px solid #E2E8F0;border-radius:6px;padding:10px 12px;margin-top:8px;font-size:13px}
.ingw-input:focus{border-color:#0284C7;outline:0}
.ingw-foot{display:flex;justify-content:flex-end;gap:10px;padding:0 18px 18px}
.ingw-ok{border:0;border-radius:6px;background:#2563EB;color:#fff;padding:10px 16px;font-size:13px;font-weight:700}
.ingw-cancel{border:1px solid #E2E8F0;border-radius:6px;background:#fff;color:#1E3A8A;padding:10px 16px;font-size:13px;font-weight:700}`;
        document.head.appendChild(css);
    }

    function abrirModal({ titulo, corpoHTML, botoes = [{ rotulo: 'OK', primario: true }], comInput = null }) {
        garantirModalCss();
        return new Promise((resolve) => {
            const overlay = document.createElement('div');
            overlay.className = 'ingw-overlay';
            overlay.innerHTML = `
                <div class="ingw-card" role="dialog" aria-modal="true">
                    <div class="ingw-head">${titulo}</div>
                    <div class="ingw-body">${corpoHTML}
                        ${comInput ? `<input class="ingw-input" placeholder="${comInput.placeholder || ''}" value="${comInput.valor || ''}">` : ''}
                    </div>
                    <div class="ingw-foot"></div>
                </div>`;
            const foot = overlay.querySelector('.ingw-foot');
            const fechar = (ret) => { overlay.remove(); document.removeEventListener('keydown', noTeclado); resolve(ret); };
            botoes.forEach((b, i) => {
                const btn = document.createElement('button');
                btn.textContent = b.rotulo;
                btn.className = b.primario ? 'ingw-ok' : 'ingw-cancel';
                btn.addEventListener('click', () => {
                    const input = overlay.querySelector('.ingw-input');
                    fechar({ botao: i, valor: input ? input.value : null });
                });
                foot.appendChild(btn);
            });
            overlay.addEventListener('mousedown', (e) => { if (e.target === overlay) fechar(null); });
            const noTeclado = (e) => {
                if (e.key === 'Escape') fechar(null);
                if (e.key === 'Enter') {
                    const ok = foot.querySelector('.ingw-ok');
                    if (ok) ok.click();
                }
            };
            document.addEventListener('keydown', noTeclado);
            document.body.appendChild(overlay);
            const input = overlay.querySelector('.ingw-input');
            if (input) input.focus();
        });
    }

    function avisar(titulo, texto) {
        return abrirModal({ titulo, corpoHTML: `<p style="margin:0">${texto}</p>` });
    }

    /**
     * Handler de ações
     */
    async function handleAction(e) {
        const action = e.currentTarget.dataset.action;
        const inscricaoId = e.currentTarget.dataset.id;

        console.log(`[Inscrições] Ação: ${action}, ID: ${inscricaoId}`);

        switch (action) {
            case 'agenda':
                await visualizarAgenda(inscricaoId);
                break;
            case 'email':
                await avisar('Enviar e-mail', `Envio para a inscrição ${inscricaoId} ainda não está disponível nesta versão.`);
                break;
            case 'view':
                await avisar('Detalhes', `Detalhes da inscrição ${inscricaoId} ainda não estão disponíveis nesta versão.`);
                break;
        }
    }

    /**
     * Visualiza agenda do participante
     */
    async function visualizarAgenda(inscricaoId) {
        try {
            console.log(`[Inscrições] Consultando agenda: ${inscricaoId}`);

            const agenda = await window.API.Inscricoes.consultarAgenda(inscricaoId);

            const itens = (agenda.atividades || []).map(a =>
                `<li><strong>${escapeHtml(a.titulo)}</strong> (${escapeHtml(a.inicio)} – ${escapeHtml(a.fim)}) em ${escapeHtml(a.local)}</li>`
            ).join('');
            await abrirModal({
                titulo: 'Agenda do Participante',
                corpoHTML: itens ? `<ul>${itens}</ul>` : '<p style="margin:0">Nenhuma atividade selecionada.</p>'
            });

        } catch (error) {
            console.error('[Inscrições] Erro ao consultar agenda:', error);
            window.API.exibirErro(error);
        }
    }

    /**
     * RF-13 (organizador): inscreve manualmente um participante pelo e-mail.
     * Resolve o usuário via GET /api/usuarios e cria via POST /api/inscricoes.
     */
    async function novaInscricaoManual() {
        try {
            const eventoId = eventDropdown ? eventDropdown.value : '';
            if (!eventoId) {
                await avisar('Nova inscrição', 'Selecione um evento no filtro antes de inscrever.');
                return;
            }
            const escolha = await abrirModal({
                titulo: 'Nova inscrição manual',
                corpoHTML: `<p style="margin:0 0 4px">E-mail do participante a inscrever:</p>`,
                botoes: [{ rotulo: 'Inscrever', primario: true }, { rotulo: 'Cancelar', primario: false }],
                comInput: { placeholder: 'participante@email.com' }
            });
            if (!escolha || escolha.botao !== 0) return;
            const email = (escolha.valor || '').trim();
            if (!email) return;

            // Gestor localiza um usuário por e-mail sem listar a base (só admin lista).
            const respUsuarios = await fetch(`/api/usuarios/buscar?email=${encodeURIComponent(email)}`, { headers: authHeaders() });
            if (respUsuarios.status === 404) {
                await avisar('Nova inscrição', `Nenhum usuário com o e-mail ${escapeHtml(email)}. Peça para ele se cadastrar em /cadastro primeiro.`);
                return;
            }
            if (!respUsuarios.ok) throw new Error(`Falha ao buscar usuário (HTTP ${respUsuarios.status})`);
            const alvo = await respUsuarios.json();
            const participanteId = alvo.id || alvo.usuarioId;
            if (!participanteId) {
                await avisar('Nova inscrição', `Nenhum usuário com o e-mail ${escapeHtml(email)}. Peça para ele se cadastrar em /cadastro primeiro.`);
                return;
            }
            const resp = await fetch('/api/inscricoes', {
                method: 'POST',
                headers: { ...authHeaders(), 'Content-Type': 'application/json' },
                body: JSON.stringify({ participanteId, eventoId })
            });
            if (!resp.ok) {
                const detalhe = await resp.text();
                throw new Error(`Inscrição recusada (HTTP ${resp.status}): ${detalhe}`);
            }
            await avisar('Nova inscrição', `Inscrição de ${escapeHtml(alvo.nome || email)} criada com sucesso.`);
            await carregarInscricoes();
        } catch (error) {
            console.error('[Inscrições] Erro na inscrição manual:', error);
            if (window.API) window.API.exibirErro(error);
            else alert(error.message);
        }
    }

    /**
     * RF-29/RF-31: exporta a lista de inscritos do evento em CSV
     * (GET /api/controle/relatorio/inscritos/{eventoId}, com JWT).
     */
    async function exportarLista() {
        try {
            const eventoId = eventDropdown ? eventDropdown.value : '';
            if (!eventoId) {
                await avisar('Exportar lista', 'Selecione um evento no filtro antes de exportar.');
                return;
            }
            const resposta = await fetch(
                `/api/controle/relatorio/inscritos/${encodeURIComponent(eventoId)}`,
                { headers: authHeaders() });
            if (!resposta.ok) throw new Error(`Falha ao exportar (HTTP ${resposta.status})`);
            const blob = await resposta.blob();
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `inscritos-${eventoId.substring(0, 8)}.csv`;
            document.body.appendChild(link);
            link.click();
            link.remove();
            URL.revokeObjectURL(url);
        } catch (error) {
            console.error('[Inscrições] Erro ao exportar:', error);
            if (window.API) window.API.exibirErro(error);
            else alert(error.message);
        }
    }

    /**
     * Busca/Filtro
     */
    if (searchInput) {
        searchInput.addEventListener('input', function() {
            const termo = this.value.toLowerCase();

            const inscricoesFiltradas = inscricoesCache.filter(inscricao =>
                (inscricao.participante.nome || '').toLowerCase().includes(termo) ||
                (inscricao.participante.email || '').toLowerCase().includes(termo) ||
                (inscricao.evento || '').toLowerCase().includes(termo) ||
                inscricao.id.toLowerCase().includes(termo)
            );

            renderizarInscricoes(inscricoesFiltradas);
        });
    }

    if (eventDropdown) {
        eventDropdown.addEventListener('change', carregarInscricoes);
    }
    if (btnNova) btnNova.addEventListener('click', novaInscricaoManual);
    if (btnExportar) btnExportar.addEventListener('click', exportarLista);

    // Carregar eventos (filtro) e inscrições ao iniciar
    await carregarEventos();
    await carregarInscricoes();
});
