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

        // Preenche perfil via GET /api/usuarios/{id} (RF-03) + form de edição
        await carregarPerfil(user.usuarioId, user);

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
        const badge = document.querySelector('[data-od-id="tab-active"] .tab-badge');
        if (badge) badge.textContent = String(inscricoes.length);
        if (inscricoes.length === 0) {
            activeTab.innerHTML = '<p style="padding:32px;text-align:center">Voce ainda nao possui inscricoes. <a href="/eventos">Ver eventos</a></p>';
            return;
        }
        // Monta o card do ingresso do zero (o HTML só tem o placeholder
        // "Carregando inscrições..." — não há card-modelo para clonar).
        activeTab.innerHTML = '';
        for (const inscricao of inscricoes) {
            let agenda = null;
            try {
                agenda = await window.API.Inscricoes.consultarAgenda(inscricao.id);
            } catch (e) {
                console.warn('[Area Participante] Agenda indisponivel para', inscricao.id);
            }
            const atividades = agenda && agenda.atividades ? agenda.atividades : [];
            const card = document.createElement('div');
            card.className = 'ticket-card';
            const nomeEvento = inscricao.eventoTitulo || inscricao.eventoId;
            const codigo = '#' + inscricao.id.substring(0, 8).toUpperCase();
            const detalhes = document.createElement('div');
            detalhes.className = 'ticket-details';
            const nomeEl = document.createElement('h3');
            nomeEl.className = 'ticket-event-name';
            nomeEl.textContent = nomeEvento;
            const codeEl = document.createElement('div');
            codeEl.className = 'ticket-code';
            codeEl.textContent = codigo;
            detalhes.appendChild(nomeEl);
            detalhes.appendChild(codeEl);
            card.appendChild(detalhes);
            // Botao RF-15: cancelar inscricao (libera vaga)
            const btnCancelar = document.createElement('button');
            btnCancelar.textContent = 'Cancelar inscrição';
            btnCancelar.style.cssText = 'margin-top:8px';
            btnCancelar.addEventListener('click', async () => {
                if (!confirm('Cancelar sua inscrição neste evento? A vaga será liberada.')) return;
                try {
                    await window.API.Inscricoes.cancelar(inscricao.id);
                    window.API.exibirSucesso('Inscrição cancelada. Vaga liberada.');
                    await carregarMinhasInscricoes();
                } catch (e) {
                    window.API.exibirErro(e);
                }
            });
            card.appendChild(btnCancelar);
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

    /**
     * RF-03: carrega o perfil do backend e injeta form de edição
     * (nome + nova senha) na aba de perfil.
     */
    async function carregarPerfil(usuarioId, sessao) {
        const tabProfile = document.querySelector('[data-od-id="tab-content-profile"]');
        let perfil = { nome: sessao.nome, email: sessao.email };
        try {
            perfil = await window.API.Usuarios.buscarPerfil(usuarioId);
            window.Auth.setUser({ usuarioId, nome: perfil.nome, email: perfil.email });
        } catch (e) {
            console.warn('[Area Participante] Perfil do backend indisponivel, usando sessao');
        }
        if (profileValues && profileValues.length >= 2) {
            profileValues[0].textContent = perfil.nome || '-';
            profileValues[1].textContent = perfil.email || '-';
        }
        if (!tabProfile || document.getElementById('perfil-edit-form')) return;
        const card = document.createElement('div');
        card.className = 'profile-section-card';
        card.innerHTML = `
            <div class="profile-section-title">Editar Meus Dados</div>
            <form id="perfil-edit-form" style="display:grid;gap:10px;max-width:420px">
                <label style="font-size:12px">Nome
                    <input id="perfil-nome" style="width:100%;padding:8px" value="" maxlength="120">
                </label>
                <label style="font-size:12px">Nova senha (min. 6 caracteres, vazio = manter)
                    <input id="perfil-senha" type="password" style="width:100%;padding:8px" autocomplete="new-password">
                </label>
                <button class="secondary" type="submit" style="justify-self:start">Salvar alterações</button>
            </form>`;
        tabProfile.querySelector('.profile-tab-content').appendChild(card);
        card.querySelector('#perfil-nome').value = perfil.nome || '';
        card.querySelector('#perfil-edit-form').addEventListener('submit', async (ev) => {
            ev.preventDefault();
            const nome = card.querySelector('#perfil-nome').value.trim();
            const senha = card.querySelector('#perfil-senha').value;
            try {
                const atualizado = await window.API.Usuarios.atualizarPerfil(usuarioId, {
                    nome: nome || null,
                    senha: senha || null
                });
                window.Auth.setUser({ usuarioId, nome: atualizado.nome, email: atualizado.email });
                if (profileValues && profileValues.length >= 2) {
                    profileValues[0].textContent = atualizado.nome || '-';
                    profileValues[1].textContent = atualizado.email || '-';
                }
                card.querySelector('#perfil-senha').value = '';
                window.API.exibirSucesso('Perfil atualizado com sucesso!');
            } catch (e) {
                window.API.exibirErro(e);
            }
        });
    }

    await carregarMinhasInscricoes();
    await carregarCertificados();
});

/**
 * RF-33/CA-08 (D): lista certificados reais do participante
 * (GET /api/certificados?participanteId=) com download do PDF.
 * Sem certificados, mostra estado honesto em vez dos mocks estáticos.
 */
async function carregarCertificados() {
    const secao = document.querySelector('[data-od-id="tab-content-history"] .certificates-section');
    if (!secao) return;
    const user = window.Auth ? window.Auth.getToken() && window.Auth.getUser() : null;
    const headers = { 'Accept': 'application/json' };
    const token = window.Auth ? window.Auth.getToken() : null;
    if (token) headers['Authorization'] = 'Bearer ' + token;
    const usuarioId = user && user.usuarioId ? user.usuarioId : null;
    if (!usuarioId) return;
    secao.innerHTML = '<h2 class="section-title">Certificados de Participação</h2>'
        + '<p style="padding:16px">Carregando certificados...</p>';
    let certs = [];
    try {
        const resp = await fetch(`/api/certificados?participanteId=${encodeURIComponent(usuarioId)}`, { headers });
        if (resp.ok) certs = await resp.json();
    } catch (e) {
        console.warn('[Area Participante] Certificados indisponíveis');
    }
    if (!certs || certs.length === 0) {
        secao.innerHTML = '<h2 class="section-title">Certificados de Participação</h2>'
            + '<p style="padding:16px">Nenhum certificado emitido ainda. '
            + 'Participantes elegíveis recebem o documento após a apuração da frequência.</p>';
        return;
    }
    const esc = (t) => {
        const div = document.createElement('div');
        div.textContent = t == null ? '' : String(t);
        return div.innerHTML;
    };
    secao.innerHTML = '<h2 class="section-title">Certificados de Participação</h2>'
        + certs.map(c => `
            <div class="certificate-card">
                <div class="certificate-info">
                    <div class="certificate-name">${esc(c.papelDeclarado ? 'Declaração — ' + c.papelDeclarado : 'Certificado de Participação')}</div>
                    <div class="certificate-details">Código: <strong>${esc(c.codigo)}</strong> • Carga horária: <strong>${esc(c.cargaHoraria)}h</strong></div>
                </div>
                <div class="certificate-actions">
                    <button class="download-btn" data-cert-evento="${esc(c.eventoId)}" data-cert-codigo="${esc(c.codigo)}">Baixar (PDF)</button>
                </div>
            </div>`).join('');
    secao.querySelectorAll('.download-btn[data-cert-codigo]').forEach(btn => {
        btn.addEventListener('click', async () => {
            try {
                const resp = await fetch(`/api/eventos/${encodeURIComponent(btn.dataset.certEvento)}/certificados/${encodeURIComponent(btn.dataset.certCodigo)}/pdf`, { headers });
                if (!resp.ok) throw new Error(`Falha ao baixar (HTTP ${resp.status})`);
                const blob = await resp.blob();
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `certificado-${btn.dataset.certCodigo}.pdf`;
                document.body.appendChild(a);
                a.click();
                a.remove();
                URL.revokeObjectURL(url);
            } catch (e) {
                window.API.exibirErro(e);
            }
        });
    });
}
