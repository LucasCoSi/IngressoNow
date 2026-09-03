/**
 * inscricoes.js - Gestão de Inscrições (Admin)
 * Integração da página ingressonow-inscricoes.html com /api/inscricoes
 */

document.addEventListener('DOMContentLoaded', async function() {
    console.log('[Inscrições] Inicializando página...');

    const tbody = document.querySelector('.enrollments-table tbody');
    const eventDropdown = document.getElementById('event-selector');
    const searchInput = document.querySelector('.search-box.full .search');

    if (!tbody) {
        console.warn('[Inscrições] Tabela não encontrada no DOM');
        return;
    }

    // Estado da aplicação
    let inscricoesCache = [];

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
                participante: { nome: i.participanteId, email: '', cpf: '-' },
                evento: i.eventoId,
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

    /**
     * Renderiza lista de inscrições na tabela
     */
    function renderizarInscricoes(inscricoes) {
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
                alert(`Enviar e-mail para inscrição ${inscricaoId} (funcionalidade a ser implementada)`);
                break;
            case 'view':
                alert(`Ver detalhes da inscrição ${inscricaoId} (funcionalidade a ser implementada)`);
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

            console.log('[Inscrições] Agenda carregada:', agenda);

            // Exibir modal ou redirecionar
            const atividadesTexto = agenda.atividades.map(a =>
                `- ${a.titulo} (${a.inicio} - ${a.fim}) em ${a.local}`
            ).join('\n');

            alert(`Agenda do Participante:\n\n${atividadesTexto || 'Nenhuma atividade selecionada'}`);

        } catch (error) {
            console.error('[Inscrições] Erro ao consultar agenda:', error);
            window.API.exibirErro(error);
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

    // Carregar inscrições ao iniciar
    await carregarInscricoes();
});
