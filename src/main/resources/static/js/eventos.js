/**
 * eventos.js - Listagem de Eventos (Admin)
 * Integração da página ingressonow-eventos.html com /api/eventos
 */

document.addEventListener('DOMContentLoaded', async function() {
    console.log('[Eventos] Inicializando página...');

    const tbody = document.querySelector('.events-table tbody');
    const counterElement = document.querySelector('.counter b');
    const filterStatus = document.getElementById('filter-status');

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

            const eventos = await window.API.Eventos.listar();
            eventosCache = eventos;

            console.log(`[Eventos] ${eventos.length} evento(s) carregado(s)`);
            renderizarEventos(eventos);
        } catch (error) {
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
     * Renderiza lista de eventos na tabela
     */
    function renderizarEventos(eventos) {
        if (eventos.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:40px;color:var(--muted);">Nenhum evento encontrado</td></tr>';
            if (counterElement) counterElement.textContent = '0';
            return;
        }

        tbody.innerHTML = eventos.map(evento => `
            <tr data-evento-id="${evento.id}">
                <td>
                    <span class="event-name">${escapeHtml(evento.titulo)}</span>
                    <span class="event-location">Online & Presencial</span>
                </td>
                <td class="event-date">-</td>
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
                        <button class="action-btn" title="Editar" data-action="edit" data-id="${evento.id}">
                            <svg class="icon"><use href="#i-edit"/></svg>
                        </button>
                        <button class="action-btn" title="Excluir" data-action="delete" data-id="${evento.id}">
                            <svg class="icon"><use href="#i-trash"/></svg>
                        </button>
                    </div>
                </td>
            </tr>
        `).join('');

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
        }
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
        const tituloAtual = evento ? evento.titulo : '';
        const novoTitulo = prompt('Novo titulo do evento:', tituloAtual);
        if (novoTitulo === null || novoTitulo.trim() === '') return;
        try {
            await window.API.Eventos.atualizar(eventoId, { titulo: novoTitulo.trim() });
            window.API.exibirSucesso('Evento atualizado com sucesso!');
            await carregarEventos();
        } catch (error) {
            console.error('[Eventos] Erro ao atualizar:', error);
            window.API.exibirErro(error);
        }
    }

    async function excluirEvento(eventoId) {
        if (!exigirLogin()) return;
        if (!confirm('Tem certeza que deseja excluir este evento?')) return;
        try {
            await window.API.Eventos.deletar(eventoId);
            window.API.exibirSucesso('Evento excluido com sucesso!');
            await carregarEventos();
        } catch (error) {
            console.error('[Eventos] Erro ao excluir:', error);
            window.API.exibirErro(error);
        }
    }

    /**
     * Filtro por status
     */
    if (filterStatus) {
        filterStatus.addEventListener('change', function() {
            const statusSelecionado = this.value;

            if (!statusSelecionado) {
                renderizarEventos(eventosCache);
                return;
            }

            const eventosFiltrados = eventosCache.filter(evento => {
                if (statusSelecionado === 'published') return evento.status === 'PUBLICADO';
                if (statusSelecionado === 'draft') return evento.status === 'RASCUNHO';
                if (statusSelecionado === 'closed') return evento.status === 'ENCERRADO';
                return true;
            });

            renderizarEventos(eventosFiltrados);
        });
    }

    // Carregar eventos ao iniciar
    await carregarEventos();
});
