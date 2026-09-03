/**
 * area-participante.js - Minhas inscricoes e agendas
 * Integracao da pagina ingressonow-area-participante.html com /api/inscricoes
 */

document.addEventListener('DOMContentLoaded', async function() {
    console.log('[Area Participante] Inicializando pagina...');

    const activeTab = document.querySelector('[data-od-id="tab-content-active"]');
    const profileValues = document.querySelectorAll('[data-od-id="tab-content-profile"] .profile-value');

    function authHeaders() {
        const headers = { 'Accept': 'application/json' };
        const token = window.Auth ? window.Auth.getToken() : null;
        if (token) headers['Authorization'] = 'Bearer ' + token;
        return headers;
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text == null ? '' : String(text);
        return div.innerHTML;
    }

    async function carregarMinhasInscricoes() {
        const user = window.Auth ? window.Auth.getUser() : null;
        if (!user || !user.usuarioId) {
            window.API.exibirErro(new window.API.APIError('Voce precisa estar logado.', 401));
            setTimeout(() => { window.location.href = '/login'; }, 1500);
            return;
        }

        // Preenche perfil com dados da sessao
        if (profileValues && profileValues.length >= 2) {
            profileValues[0].textContent = user.nome || '-';
            profileValues[1].textContent = user.email || '-';
        }

        try {
            const resposta = await fetch(`/api/inscricoes?participanteId=${encodeURIComponent(user.usuarioId)}`, {
                method: 'GET',
                headers: authHeaders()
            });
            if (!resposta.ok) throw new Error(`Falha ao carregar inscricoes (HTTP ${resposta.status})`);
            const inscricoes = await resposta.json();
            console.log(`[Area Participante] ${inscricoes.length} inscricao(oes) carregada(s)`);
            await renderizarInscricoes(inscricoes);
        } catch (error) {
            console.error('[Area Participante] Erro ao carregar:', error);
            window.API.exibirErro(error);
        }
    }

    async function renderizarInscricoes(inscricoes) {
        if (!activeTab) return;
        if (inscricoes.length === 0) {
            activeTab.innerHTML = '<p style="padding:32px;text-align:center">Voce ainda nao possui inscricoes. <a href="/eventos">Ver eventos</a></p>';
            return;
        }
        // Mantem o primeiro card estatico como modelo e replica para as demais
        const modelo = activeTab.querySelector('[data-od-id="ticket-active"]');
        activeTab.innerHTML = '';
        for (const inscricao of inscricoes) {
            let agenda = null;
            try {
                agenda = await window.API.Inscricoes.consultarAgenda(inscricao.id);
            } catch (e) {
                console.warn('[Area Participante] Agenda indisponivel para', inscricao.id);
            }
            const atividades = agenda && agenda.atividades ? agenda.atividades : [];
            const card = modelo ? modelo.cloneNode(true) : document.createElement('div');
            const nomeEvento = inscricao.eventoTitulo || inscricao.eventoId;
            const nomeEl = card.querySelector('.ticket-event-name');
            if (nomeEl) nomeEl.textContent = nomeEvento;
            const codeEl = card.querySelector('.ticket-code');
            if (codeEl) codeEl.textContent = '#' + inscricao.id.substring(0, 8).toUpperCase();
            // Lista de atividades da agenda abaixo do card
            const lista = document.createElement('ul');
            lista.style.cssText = 'margin:12px 0 24px 0;padding-left:20px';
            if (atividades.length === 0) {
                const li = document.createElement('li');
                li.textContent = 'Nenhuma atividade selecionada ainda.';
                lista.appendChild(li);
            } else {
                atividades.forEach(a => {
                    const li = document.createElement('li');
                    li.innerHTML = `${escapeHtml(a.titulo)} — ${escapeHtml(window.API.formatarDataHora(a.inicio))}`;
                    lista.appendChild(li);
                });
            }
            activeTab.appendChild(card);
            activeTab.appendChild(lista);
        }
    }

    await carregarMinhasInscricoes();
});
