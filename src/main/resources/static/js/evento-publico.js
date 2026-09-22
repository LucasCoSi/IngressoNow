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

            // Buscar evento completo (inclui atividades); fallback p/ lista pública.
            let evento = null;
            try {
                evento = await window.API.Eventos.buscar(eventoId);
            } catch (e) {
                console.warn('[Evento Público] buscar falhou, tentando lista:', e);
                const eventos = await window.API.Eventos.listar();
                evento = eventos.find(ev => ev.id === eventoId);
            }

            if (!evento) {
                exibirMensagem('Evento não encontrado.');
                return;
            }

            console.log('[Evento Público] Evento carregado:', evento);
            await renderizarEvento(evento);

        } catch (error) {
            console.error('[Evento Público] Erro ao carregar:', error);
            exibirMensagem(`Erro ao carregar evento: ${error.message}`);
        }
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text == null ? '' : String(text);
        return div.innerHTML;
    }

    function formatarStatus(status) {
        const mapa = {
            RASCUNHO: 'Rascunho',
            PUBLICADO: 'Publicado',
            INSCRICOES_ABERTAS: 'Inscrições abertas',
            ENCERRADO: 'Encerrado',
            CANCELADO: 'Cancelado'
        };
        return mapa[status] || status || 'Evento';
    }

    function iniciais(nome) {
        if (!nome) return '?';
        const partes = String(nome).trim().split(/\s+/);
        const prim = partes[0] ? partes[0][0] : '';
        const ult = partes.length > 1 ? partes[partes.length - 1][0] : '';
        return (prim + ult).toUpperCase();
    }

    /** Grade real agrupada por dia (substitui o mock fixo Dia 1/2). */
    function renderizarProgramacaoReal(atividades) {
        const preview = document.querySelector('[data-od-id="section-schedule-preview"]');
        if (!preview) return;
        // Preserva o título, troca o corpo (remove placeholder "Carregando..."/mock).
        preview.querySelectorAll('.schedule-day, .section-text').forEach(el => el.remove());
        if (!atividades || atividades.length === 0) {
            const vazio = document.createElement('p');
            vazio.className = 'section-text';
            vazio.textContent = 'Programação a definir. Volte em breve!';
            preview.appendChild(vazio);
            return;
        }
        const ordenadas = [...atividades].sort((a, b) =>
            String(a.inicio || '').localeCompare(String(b.inicio || '')));
        const porDia = new Map();
        ordenadas.forEach(a => {
            const dia = a.inicio ? String(a.inicio).slice(0, 10) : 'datas-a-definir';
            if (!porDia.has(dia)) porDia.set(dia, []);
            porDia.get(dia).push(a);
        });
        porDia.forEach((lista, dia) => {
            const bloco = document.createElement('div');
            bloco.className = 'schedule-day';
            const titulo = document.createElement('div');
            titulo.className = 'schedule-day-title';
            let rotulo = dia;
            try {
                const d = new Date(dia.length === 10 ? dia + 'T12:00:00' : dia);
                if (!isNaN(d)) rotulo = d.toLocaleDateString('pt-BR', { day: '2-digit', month: 'long' });
            } catch (e) { /* mantém yyyy-MM-dd */ }
            titulo.textContent = rotulo;
            bloco.appendChild(titulo);
            lista.forEach(a => {
                const item = document.createElement('div');
                item.className = 'schedule-item';
                const hora = a.inicio ? String(a.inicio).slice(11, 16) : '--:--';
                item.innerHTML =
                    `<div class="schedule-time">${escapeHtml(hora)}</div>` +
                    `<div class="schedule-activity">` +
                    `<div class="schedule-activity-name">${escapeHtml(a.titulo || 'Atividade')}</div>` +
                    `<div class="schedule-activity-location">${escapeHtml(
                        [a.local, a.tipo, a.trilha].filter(Boolean).join(' • ') || 'Local a definir'
                    )}</div></div>`;
                bloco.appendChild(item);
            });
            preview.appendChild(bloco);
        });
    }

    /** Elenco real (GET .../atividades/{atId}/pessoas); sem mock de 6 palestrantes. */
    async function renderizarPalestrantesReal(eventoId, atividades) {
        const grade = document.querySelector('.speakers-grid');
        if (!grade) return;
        grade.innerHTML = '<p class="section-text">Carregando palestrantes...</p>';
        const elenco = [];
        for (const a of (atividades || [])) {
            if (!a.id) continue;
            try {
                const resp = await fetch(
                    `/api/eventos/${encodeURIComponent(eventoId)}/atividades/${encodeURIComponent(a.id)}/pessoas`);
                if (!resp.ok) continue;
                const pessoas = await resp.json();
                (pessoas || []).forEach(p => elenco.push({ ...p, atividadeTitulo: a.titulo }));
            } catch (e) {
                console.warn('[Evento Público] pessoas indisponíveis p/ atividade', a.id);
            }
        }
        grade.innerHTML = '';
        if (elenco.length === 0) {
            grade.innerHTML = '<p class="section-text">Elenco a definir.</p>';
            return;
        }
        elenco.slice(0, 12).forEach(p => {
            const card = document.createElement('div');
            card.className = 'speaker-card';
            card.innerHTML =
                `<div class="speaker-photo">${escapeHtml(iniciais(p.nome))}</div>` +
                `<h3 class="speaker-name">${escapeHtml(p.nome)}</h3>` +
                `<p class="speaker-title">${escapeHtml(p.papel || 'Participação')}</p>` +
                `<p class="speaker-org">${escapeHtml(p.atividadeTitulo || '')}</p>`;
            grade.appendChild(card);
        });
    }

    /** Abas placeholder ("em desenvolvimento...") → conteúdo real do evento. */
    function preencherAbasReais(evento, atividades) {
        const setTab = (nome, html) => {
            const el = document.querySelector(`[data-od-id="tab-content-${nome}"] .section-text`);
            if (el) el.innerHTML = html;
        };
        const total = (atividades || []).length;
        setTab('schedule', total === 0
            ? 'Programação a definir. Volte em breve!'
            : `${escapeHtml(String(total))} ${total === 1 ? 'atividade publicada' : 'atividades publicadas'} — veja o resumo na grade acima ou a programação completa em <a href="/programacao">/programacao</a>.`);
        setTab('speakers', 'Elenco carregado acima a partir das pessoas vinculadas às atividades (GET .../pessoas).');
        setTab('registration', evento.status === 'ENCERRADO'
            ? 'Inscrições encerradas para este evento.'
            : 'Use o botão de inscrição ao lado para garantir sua vaga. Acompanhe tudo em <a href="/area-participante">/area-participante</a>.');
        setTab('location', `${escapeHtml(evento.local || 'Local a definir')}${evento.fuso && evento.fuso !== 'UTC' ? ` (${escapeHtml(evento.fuso)})` : ''}`);
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

        // Atualizar badges (reais: status + vagas + nº de atividades; sem chapado)
        if (heroBadges) {
            const totalAtiv = (evento.atividades || []).length;
            heroBadges.innerHTML = `
                <span class="hero-badge">${escapeHtml(formatarStatus(evento.status))}</span>
                <span class="hero-badge">${evento.maxVagas != null ? `${escapeHtml(String(evento.maxVagas))} vagas` : 'Vagas livres'}</span>
                <span class="hero-badge">${totalAtiv === 0 ? 'Programação a definir' : `${escapeHtml(String(totalAtiv))} ${totalAtiv === 1 ? 'atividade' : 'atividades'}`}</span>
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
        const descricaoSection = document.querySelector('[data-od-id="section-about"] .section-text');
        if (descricaoSection) {
            descricaoSection.innerHTML = evento.descricao
                ? `<p>${escapeHtml(evento.descricao)}</p>`
                : '<p>Descrição a definir.</p>';
        }

        // Grade + elenco + abas reais (sem mock fixo).
        renderizarProgramacaoReal(evento.atividades || []);
        preencherAbasReais(evento, evento.atividades || []);
        await renderizarPalestrantesReal(evento.id, evento.atividades || []);
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
