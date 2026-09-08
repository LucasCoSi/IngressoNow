/**
 * nav.js - Navegação lateral global (System Design).
 * Os botões .nav-btn do protótipo só alternavam classe CSS — aqui
 * cada data-od-id mapeia para a rota real servida pelo StaticHandler.
 * Incluído em todas as páginas (antes do JS específico).
 */
(function() {
    const ROTAS = {
        'nav-dashboard': '/dashboard',
        'nav-events': '/eventos',
        'nav-activities': '/atividades',
        'nav-schedule': '/programacao',
        'nav-enrollments': '/inscricoes',
        'nav-attendance': '/frequencia',
        'nav-evaluations': '/avaliacoes',
        'nav-reports': '/relatorios',
        'nav-users': '/usuarios',
        'nav-settings': '/configuracoes'
    };

    // Ícones que faltam em algumas páginas (botões invisíveis sem o symbol).
    const ICONES_EXTRAS = {
        'i-eye': '<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8Z"/><circle cx="12" cy="12" r="3"/>',
        'i-check': '<polyline points="20 6 9 17 4 12"/>',
        'i-x': '<line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>',
        'i-plus': '<line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>',
        'i-mail': '<rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-10 6L2 7"/>',
        'i-download': '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>',
        'i-camera': '<path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2Z"/><circle cx="12" cy="13" r="4"/>'
    };

    function garantirIcones() {
        let defs = document.querySelector('svg defs');
        if (!defs) return;
        const svg = defs.closest('svg');
        for (const [id, conteudo] of Object.entries(ICONES_EXTRAS)) {
            if (!document.getElementById(id)) {
                const sym = document.createElementNS('http://www.w3.org/2000/svg', 'symbol');
                sym.setAttribute('id', id);
                sym.setAttribute('viewBox', '0 0 24 24');
                sym.innerHTML = conteudo;
                defs.appendChild(sym);
            }
        }
        void svg;
    }

    // Links textuais do header do participante / evento público (href="#"
    // no protótipo). Mapeia pelo texto visível para as rotas reais.
    // "Validar Certificado" leva à área do participante (aba de
    // certificados) — sem página dedicada no MVP.
    const NAV_LINK_POR_TEXTO = {
        'explorar eventos': '/eventos',
        'programação': '/programacao',
        'programacao': '/programacao',
        'minhas inscrições': '/area-participante',
        'meus ingressos': '/area-participante',
        'área do participante': '/area-participante',
        'area do participante': '/area-participante',
        'validar certificado': '/area-participante'
    };

    function ligarNav() {
        document.querySelectorAll('.nav-btn[data-od-id]').forEach(btn => {
            const destino = ROTAS[btn.getAttribute('data-od-id')];
            if (!destino) return;
            btn.addEventListener('click', () => {
                window.location.href = destino;
            });
        });
        // Marca ativo conforme a URL atual
        const path = window.location.pathname;
        document.querySelectorAll('.nav-btn[data-od-id]').forEach(btn => {
            btn.classList.toggle('active', ROTAS[btn.getAttribute('data-od-id')] === path);
        });
        document.querySelectorAll('a.nav-link').forEach(a => {
            const texto = (a.textContent || '').trim().toLowerCase();
            const destino = NAV_LINK_POR_TEXTO[texto];
            if (!destino) return;
            a.setAttribute('href', destino);
            if (destino === path) a.classList.add('active');
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => {
            garantirIcones();
            ligarNav();
        });
    } else {
        garantirIcones();
        ligarNav();
    }
})();
