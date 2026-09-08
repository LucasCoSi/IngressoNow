/**
 * evento-publico.js - Detalhes do Evento (Visualização Pública)
 * Integração da página ingressonow-evento-publico.html
 */

document.addEventListener('DOMContentLoaded', async function() {
    console.log('[Evento Público] Inicializando página...');

    // Obter ID do evento da URL
    const urlParams = new URLSearchParams(window.location.search);
    const eventoId = urlParams.get('id');

    // RF-10: sem ?id= a raiz "/" vira catálogo (vitrine Sympla-like) com os
    // eventos publicados reais; com ?id=, detalhe do evento como antes.
    if (!eventoId) {
        await mostrarCatalogo();
        return;
    }

    function garantirCssCatalogo() {
        if (document.getElementById('ev-catalogo-css')) return;
        const style = document.createElement('style');
        style.id = 'ev-catalogo-css';
        style.textContent = [
            '.ev-catalogo{max-width:1100px;margin:0 auto;padding:40px 32px 64px}',
            '.ev-hero{background:linear-gradient(135deg,#1E3A8A 0%,#0F172A 100%);color:#fff;border-radius:14px;padding:36px 32px;margin-bottom:28px}',
            '.ev-hero h1{font-size:30px;letter-spacing:-.02em;margin:0 0 8px}',
            '.ev-hero p{margin:0;color:#BAE6FD;font-size:14px}',
            '.ev-grade{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:20px}',
            '.ev-card{background:#fff;border:1px solid var(--line,#E2E8F0);border-radius:12px;padding:22px;display:flex;flex-direction:column;gap:10px;box-shadow:0 2px 8px rgba(2,6,23,.05);transition:transform .15s ease,box-shadow .15s ease;overflow:hidden}',
            '.ev-card:hover{transform:translateY(-3px);box-shadow:0 12px 28px rgba(2,6,23,.12)}',
            '.ev-topo{height:6px;border-radius:3px;background:linear-gradient(90deg,#2563EB,#38BDF8);margin:-22px -22px 2px}',
            '.ev-card h2{margin:0;font-size:19px;color:var(--navy,#1E3A8A)}',
            '.ev-vagas{align-self:flex-start;background:var(--cyan-soft,#E0F2FE);color:#075985;font-size:11px;font-weight:700;padding:4px 10px;border-radius:999px}',
            '.ev-card .ev-desc{color:var(--muted,#64748B);font-size:14px;margin:0;flex:1}',
            '.ev-card .ev-meta{display:flex;flex-wrap:wrap;gap:8px 16px;font-size:13px;color:var(--ink,#0F172A);border-top:1px solid var(--line,#E2E8F0);padding-top:10px}',
            '.ev-card .ev-cta{margin-top:6px;border:0;border-radius:6px;background:#2563EB;color:#fff;padding:12px;font-size:14px;font-weight:700;cursor:pointer;transition:background .15s ease}',
            '.ev-card .ev-cta:hover{background:#1D4ED8}',
            '.ev-vazio{background:#fff;border:1px dashed var(--line,#E2E8F0);border-radius:12px;padding:40px;text-align:center;color:var(--muted,#64748B)}'
        ].join('\n');
        document.head.appendChild(style);
    }

    async function mostrarCatalogo() {
        garantirCssCatalogo();
        document.title = 'Eventos disponíveis - IngressoNow';
        // Esconde o detalhe mockado; entra a vitrine.
        ['[data-od-id="hero-banner"]', '[data-od-id="event-tabs"]', '[data-od-id="main-content"]']
            .forEach(sel => {
                const el = document.querySelector(sel);
                if (el) el.style.display = 'none';
            });
        const header = document.querySelector('[data-od-id="public-header"]');
        const secao = document.createElement('section');
        secao.className = 'ev-catalogo';
        secao.setAttribute('data-od-id', 'catalogo');
        const hero = document.createElement('div');
        hero.className = 'ev-hero';
        const h1 = document.createElement('h1');
        h1.textContent = 'Eventos disponíveis';
        const sub = document.createElement('p');
        sub.textContent = 'Explore a programação, inscreva-se e gerencie seus ingressos na área do participante.';
        hero.append(h1, sub);
        const grade = document.createElement('div');
        grade.className = 'ev-grade';
        grade.textContent = 'Carregando eventos...';
        secao.append(hero, grade);
        if (header && header.parentNode) header.parentNode.insertBefore(secao, header.nextSibling);
        else document.body.appendChild(secao);
        try {
            const eventos = await window.API.Eventos.listar();
            grade.innerHTML = '';
            if (!eventos || eventos.length === 0) {
                const vazio = document.createElement('div');
                vazio.className = 'ev-vazio';
                vazio.textContent = 'Nenhum evento publicado no momento. Volte em breve!';
                grade.appendChild(vazio);
                return;
            }
            sub.textContent = eventos.length === 1
                ? '1 evento publicado. Explore a programação, inscreva-se e gerencie seus ingressos na área do participante.'
                : `${eventos.length} eventos publicados. Explore a programação, inscreva-se e gerencie seus ingressos na área do participante.`;
            eventos.forEach(ev => {
                const card = document.createElement('article');
                card.className = 'ev-card';
                const topo = document.createElement('div');
                topo.className = 'ev-topo';
                topo.setAttribute('aria-hidden', 'true');
                const t = document.createElement('h2');
                t.textContent = ev.titulo;
                const pill = document.createElement('span');
                pill.className = 'ev-vagas';
                pill.textContent = ev.maxVagas != null ? `${ev.maxVagas} vagas` : 'Vagas livres';
                const d = document.createElement('p');
                d.className = 'ev-desc';
                const desc = ev.descricao || '';
                d.textContent = desc.length > 140 ? desc.slice(0, 137) + '...' : (desc || 'Sem descrição.');
                const meta = document.createElement('div');
                meta.className = 'ev-meta';
                const periodo = ev.periodoInicio
                    ? window.API.formatarData(ev.periodoInicio)
                    : 'Datas a definir';
                const spanData = document.createElement('span');
                spanData.textContent = periodo;
                const spanLocal = document.createElement('span');
                spanLocal.textContent = ev.local || 'Local a definir';
                meta.append(spanData, spanLocal);
                const cta = document.createElement('button');
                cta.className = 'ev-cta';
                cta.type = 'button';
                cta.textContent = 'Ver detalhes e inscrever-se';
                cta.addEventListener('click', () => {
                    window.location.href = `/evento-publico?id=${encodeURIComponent(ev.id)}`;
                });
                card.append(topo, t, pill, d, meta, cta);
                grade.appendChild(card);
            });
        } catch (error) {
            console.error('[Evento Público] Erro no catálogo:', error);
            grade.textContent = 'Não foi possível carregar os eventos. Tente novamente.';
        }
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
    async function renderizarEvento(evento) {
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

        // Atualizar meta info (RF-04/RN-20: período, local e fuso reais do evento)
        if (heroMeta) {
            const periodo = evento.periodoInicio
                ? `${window.API.formatarData(evento.periodoInicio)} — ${window.API.formatarData(evento.periodoFim || evento.periodoInicio)}`
                : 'Datas a definir';
            heroMeta.innerHTML = `
                <span class="hero-meta-item">
                    <svg class="icon"><use href="#i-calendar"/></svg>
                    ${periodo}${evento.fuso && evento.fuso !== 'UTC' ? ` (${evento.fuso})` : ''}
                </span>
                <span class="hero-meta-item">
                    <svg class="icon"><use href="#i-map-pin"/></svg>
                    ${evento.local || 'Local a definir'}
                </span>
                <span class="hero-meta-item">
                    <svg class="icon"><use href="#i-users"/></svg>
                    ${evento.maxVagas || 'Vagas livres'}
                </span>
            `;
        }

        // Configurar botão: organizador gerencia, participante inscreve-se.
        // (Backend bloqueia organizador com 422; evita o clique no erro.)
        // Tipo vem do login (AuthHandler.resposta); fallback: busca o perfil.
        if (ctaButton) {
            const user = window.Auth ? window.Auth.getUser() : null;
            let tipo = user ? user.tipo : null;
            if (!tipo && user && user.usuarioId && window.API.Usuarios) {
                try {
                    const perfil = await window.API.Usuarios.buscarPerfil(user.usuarioId);
                    tipo = perfil.tipo;
                    window.Auth.setUser({ ...user, tipo });
                } catch (e) {
                    console.warn('[Evento Público] Tipo indisponível, assume participante');
                }
            }
            if (tipo === 'ORGANIZADOR') {
                ctaButton.textContent = 'Gerenciar Evento';
                ctaButton.onclick = () => { window.location.href = '/eventos'; };
            } else {
                ctaButton.textContent = 'Realizar Inscrição';
                ctaButton.onclick = () => realizarInscricao(evento.id);
            }
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
        const user = window.Auth ? window.Auth.getUser() : null;
        const participanteId = user && user.usuarioId ? user.usuarioId : null;

        if (!participanteId) {
            window.API.exibirErro(new window.API.APIError('Voce precisa estar logado para se inscrever.', 401));
            setTimeout(() => { window.location.href = '/login'; }, 1500);
            return;
        }

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
