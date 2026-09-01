/**
 * evento-publico.js - Detalhes do Evento (Visualização Pública)
 * Integração da página ingressonow-evento-publico.html
 */

document.addEventListener('DOMContentLoaded', async function() {
    console.log('[Evento Público] Inicializando página...');

    // Obter ID do evento da URL
    const urlParams = new URLSearchParams(window.location.search);
    const eventoId = urlParams.get('id');

    if (!eventoId) {
        console.warn('[Evento Público] ID do evento não fornecido');
        exibirMensagem('Evento não encontrado. Selecione um evento válido.');
        return;
    }

    // Elementos do DOM
    const heroTitle = document.querySelector('.hero-title');
    const heroBadges = document.querySelector('.hero-badges');
    const heroMeta = document.querySelector('.hero-meta');
    const ctaButton = document.querySelector('.cta-button');
    const scheduleContainer = document.querySelector('.schedule-day');

    /**
     * Carrega dados do evento
     */
    async function carregarEvento() {
        try {
            console.log(`[Evento Público] Carregando evento ID: ${eventoId}`);

            // Buscar evento da API
            const eventos = await window.API.Eventos.listar();
            const evento = eventos.find(e => e.id === eventoId);

            if (!evento) {
                exibirMensagem('Evento não encontrado.');
                return;
            }

            console.log('[Evento Público] Evento carregado:', evento);
            renderizarEvento(evento);

        } catch (error) {
            console.error('[Evento Público] Erro ao carregar:', error);
            exibirMensagem(`Erro ao carregar evento: ${error.message}`);
        }
    }

    /**
     * Renderiza dados do evento na página
     */
    function renderizarEvento(evento) {
        // Atualizar título
        if (heroTitle) {
            heroTitle.textContent = evento.titulo;
            document.title = `${evento.titulo} - IngressoNow`;
        }

        // Atualizar badges
        if (heroBadges) {
            heroBadges.innerHTML = `
                <span class="hero-badge">Evento Acadêmico</span>
                <span class="hero-badge">Presencial & Online</span>
                <span class="hero-badge">Inscrição Gratuita</span>
            `;
        }

        // Atualizar meta info
        if (heroMeta) {
            heroMeta.innerHTML = `
                <span class="hero-meta-item">
                    <svg class="icon"><use href="#i-calendar"/></svg>
                    15-16 de Setembro, 2026
                </span>
                <span class="hero-meta-item">
                    <svg class="icon"><use href="#i-map-pin"/></svg>
                    Centro de Eventos Sul
                </span>
                <span class="hero-meta-item">
                    <svg class="icon"><use href="#i-users"/></svg>
                    ${evento.maxVagas || 200} vagas
                </span>
            `;
        }

        // Configurar botão de inscrição
        if (ctaButton) {
            ctaButton.textContent = 'Realizar Inscrição';
            ctaButton.onclick = () => realizarInscricao(evento.id);
        }

        // Atualizar descrição (se existir seção)
        const descricaoSection = document.querySelector('.section-text');
        if (descricaoSection && evento.descricao) {
            descricaoSection.innerHTML = `
                <p>${evento.descricao}</p>
            `;
        }
    }

    /**
     * Realiza inscrição no evento
     */
    async function realizarInscricao(eventoId) {
        // SIMULAÇÃO: ID do participante (em produção viria da sessão)
        const participanteId = '550e8400-e29b-41d4-a716-446655440001';

        try {
            console.log(`[Evento Público] Inscrevendo participante ${participanteId} no evento ${eventoId}`);

            const inscricao = await window.API.Inscricoes.inscrever(participanteId, eventoId);

            console.log('[Evento Público] Inscrição realizada:', inscricao);

            window.API.exibirSucesso('Inscrição realizada com sucesso! Você receberá um e-mail de confirmação.');

            // Redirecionar para área do participante após 2 segundos
            setTimeout(() => {
                window.location.href = '/area-participante';
            }, 2000);

        } catch (error) {
            console.error('[Evento Público] Erro na inscrição:', error);
            window.API.exibirErro(error);
        }
    }

    /**
     * Exibe mensagem de erro na página
     */
    function exibirMensagem(mensagem) {
        if (heroTitle) {
            heroTitle.textContent = 'Erro';
        }
        if (heroBadges) {
            heroBadges.innerHTML = `<p style="color:#fff;margin:20px 0;">${mensagem}</p>`;
        }
        if (ctaButton) {
            ctaButton.disabled = true;
            ctaButton.textContent = 'Evento Indisponível';
        }
    }

    // Carregar evento ao iniciar
    await carregarEvento();
});
