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
        'nav-settings': '/configuracoes',
        'nav-eventos-disponiveis': '/',
        'nav-programacao-part': '/programacao',
        'nav-area-participante': '/area-participante'
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
        'explorar eventos': '/',
        'programação': '/programacao',
        'programacao': '/programacao',
        'minhas inscrições': '/area-participante',
        'meus ingressos': '/area-participante',
        'área do participante': '/area-participante',
        'area do participante': '/area-participante',
        'validar certificado': '/area-participante'
    };

    // Favicon da marca (logo "IN") na guia do navegador.
    function garantirFavicon() {
        if (document.querySelector('link[rel="icon"]')) return;
        const link = document.createElement('link');
        link.rel = 'icon';
        link.type = 'image/svg+xml';
        link.href = '/favicon.svg';
        document.head.appendChild(link);
    }

    // Menu de conta no canto superior direito.
    // Páginas admin usam `.topbar .profile`; área do participante usa
    // `.profile-section[data-od-id="profile-menu"]` (estrutura diferente).
    // Logado: nome real + tipo; menu com "Meus dados" e "Sair".
    // Visitante (primeiro acesso, sem token): "Visitante / Entrar";
    // menu com "Fazer login" e "Não possui uma conta? Cadastre-se".
    const CSS_MENU_CONTA = [
        '.topbar .profile{position:relative;cursor:pointer;border-radius:8px}',
        '.topbar .profile:focus-visible{outline:3px solid var(--mist, #38BDF8);outline-offset:2px}',
        '.profile-section[data-od-id="profile-menu"]{position:relative;cursor:pointer;border-radius:8px}',
        '.profile-section[data-od-id="profile-menu"]:focus-visible{outline:3px solid var(--mist, #38BDF8);outline-offset:2px}',
        '.menu-seta{font-size:10px;color:inherit;opacity:.7}',
        '.menu-conta{position:absolute;top:calc(100% + 8px);right:0;min-width:230px;background:#fff;color:var(--ink, #0F172A);border:1px solid var(--line, #E2E8F0);border-radius:10px;box-shadow:0 12px 32px rgba(2,6,23,.18);padding:6px;z-index:50;display:none}',
        '.menu-conta.aberto{display:block}',
        '.menu-conta .menu-info{padding:10px 12px;border-bottom:1px solid var(--line, #E2E8F0);margin-bottom:4px}',
        '.menu-conta .menu-info strong{display:block;font-size:13px}',
        '.menu-conta .menu-info small{color:var(--muted, #64748B);font-size:12px}',
        '.menu-conta a.menu-item{display:flex;align-items:center;gap:8px;padding:10px 12px;border-radius:8px;font-size:13px;font-weight:600;color:var(--ink, #0F172A);text-decoration:none}',
        '.menu-conta a.menu-item:hover{background:#F1F5F9}',
        '.menu-conta .menu-hint{padding:8px 12px 2px;font-size:12px;color:var(--muted, #64748B)}',
        '.menu-conta .menu-divisor{border:0;border-top:1px solid var(--line, #E2E8F0);margin:4px 0}'
    ].join('\n');

    function garantirCssMenuConta() {
        if (document.getElementById('menu-conta-css')) return;
        const style = document.createElement('style');
        style.id = 'menu-conta-css';
        style.textContent = CSS_MENU_CONTA;
        document.head.appendChild(style);
    }

    function iniciaisDe(nome) {
        const partes = (nome || '').trim().split(/\s+/).filter(Boolean);
        if (!partes.length) return '?';
        if (partes.length === 1) return partes[0].slice(0, 2).toUpperCase();
        return (partes[0][0] + partes[partes.length - 1][0]).toUpperCase();
    }

    function rotuloTipo(tipo) {
        const t = (tipo || '').toUpperCase();
        if (t === 'ORGANIZADOR') return 'Organizador';
        if (t === 'PARTICIPANTE') return 'Participante';
        if (t === 'ADMIN' || t === 'ADMINISTRADOR') return 'Administrador';
        return t ? t : '';
    }

    function itemMenu(texto, destino, acao) {
        const a = document.createElement('a');
        a.className = 'menu-item';
        a.href = destino || '#';
        a.textContent = texto;
        a.addEventListener('click', (ev) => {
            ev.preventDefault();
            ev.stopPropagation();
            if (acao) acao();
            else if (destino) window.location.href = destino;
        });
        return a;
    }

    // Alvos do menu de conta: páginas admin + área do participante.
    const ALVOS_PERFIL = [
        { seletor: '.topbar .profile', avatarClasse: 'avatar', variante: 'admin' },
        { seletor: '.profile-section[data-od-id="profile-menu"]', avatarClasse: 'avatar-sm', variante: 'participante' }
    ];

    function configurarPerfil() {
        garantirCssMenuConta();
        ALVOS_PERFIL.forEach(cfg => {
            const el = document.querySelector(cfg.seletor);
            if (el) montarMenuConta(el, cfg);
        });
    }

    function montarMenuConta(perfil, cfg) {
        if (!perfil || perfil.dataset.menuConta === 'ok') return;
        perfil.dataset.menuConta = 'ok';

        const auth = window.Auth || null;
        const autenticado = !!(auth && auth.isAuthenticated && auth.isAuthenticated());
        const usuario = autenticado && auth.getUser ? auth.getUser() : null;
        const nomeExibido = (autenticado && usuario && usuario.nome) || (autenticado ? 'Minha conta' : 'Visitante');
        const papelExibido = (autenticado && usuario)
            ? (rotuloTipo(usuario.tipo) || usuario.email || '')
            : 'Entrar';

        perfil.innerHTML = '';
        const avatar = document.createElement('span');
        avatar.className = cfg.avatarClasse;
        avatar.textContent = autenticado ? iniciaisDe(nomeExibido) : 'V';

        // Rótulo respeita a estrutura de cada variante (classes do System Design).
        let rotulo;
        if (cfg.variante === 'participante') {
            rotulo = document.createElement('div');
            rotulo.className = 'profile-text';
            const nome = document.createElement('span');
            nome.className = 'profile-name';
            nome.textContent = nomeExibido;
            const papel = document.createElement('span');
            papel.className = 'profile-role';
            papel.textContent = papelExibido;
            rotulo.append(nome, papel);
        } else {
            rotulo = document.createElement('span');
            const nome = document.createElement('span');
            nome.textContent = nomeExibido;
            const sub = document.createElement('small');
            sub.textContent = papelExibido;
            const quebra = document.createElement('br');
            rotulo.append(nome, quebra, sub);
        }

        const seta = document.createElement('span');
        seta.className = 'menu-seta';
        seta.textContent = ' ▾';
        seta.setAttribute('aria-hidden', 'true');

        const menu = document.createElement('div');
        menu.className = 'menu-conta';
        menu.setAttribute('role', 'menu');

        if (autenticado && usuario) {
            const info = document.createElement('div');
            info.className = 'menu-info';
            const infoNome = document.createElement('strong');
            infoNome.textContent = usuario.nome || 'Minha conta';
            const infoEmail = document.createElement('small');
            infoEmail.textContent = usuario.email || rotuloTipo(usuario.tipo);
            info.append(infoNome, infoEmail);
            menu.append(info);
            menu.append(itemMenu('Meus dados', '/area-participante#perfil'));
            menu.append(itemMenu('Sair', null, () => auth.logout()));
        } else {
            menu.append(itemMenu('Fazer login', '/login'));
            const divisor = document.createElement('hr');
            divisor.className = 'menu-divisor';
            const hint = document.createElement('div');
            hint.className = 'menu-hint';
            hint.textContent = 'Não possui uma conta?';
            menu.append(divisor, hint);
            menu.append(itemMenu('Cadastre-se', '/cadastro'));
        }

        perfil.append(avatar, rotulo, seta, menu);
        perfil.setAttribute('role', 'button');
        perfil.setAttribute('tabindex', '0');
        perfil.setAttribute('aria-haspopup', 'menu');
        perfil.setAttribute('aria-expanded', 'false');

        function alternar(abrir) {
            const vaiAbrir = typeof abrir === 'boolean' ? abrir : !menu.classList.contains('aberto');
            menu.classList.toggle('aberto', vaiAbrir);
            perfil.setAttribute('aria-expanded', vaiAbrir ? 'true' : 'false');
        }

        perfil.addEventListener('click', (ev) => {
            if (menu.contains(ev.target)) return;
            ev.stopPropagation();
            alternar();
        });
        perfil.addEventListener('keydown', (ev) => {
            if (ev.key === 'Enter' || ev.key === ' ') {
                ev.preventDefault();
                alternar();
            } else if (ev.key === 'Escape') {
                alternar(false);
            }
        });
        document.addEventListener('click', (ev) => {
            if (!perfil.contains(ev.target)) alternar(false);
        });
        document.addEventListener('keydown', (ev) => {
            if (ev.key === 'Escape') alternar(false);
        });
    }

    // Controle de acesso por papel (§3): gestão é do organizador.
    // Sem login em página de gestão → /login; participante → /area-participante.
    // Públicas: /, /evento-publico, /programacao, /login, /cadastro, /area-participante.
    const PAGINAS_GESTAO = ['/dashboard', '/eventos', '/atividades', '/inscricoes',
        '/frequencia', '/avaliacoes', '/relatorios', '/usuarios', '/configuracoes'];

    function papelUsuario() {
        const auth = window.Auth || null;
        if (!auth || !auth.isAuthenticated || !auth.isAuthenticated()) return null;
        const u = auth.getUser ? auth.getUser() : null;
        if (!u) return null;
        const t = String(u.tipo || '').toUpperCase();
        if (t === 'ORGANIZADOR' || t === 'ADMIN' || t === 'ADMINISTRADOR') return 'ORGANIZADOR';
        return 'PARTICIPANTE';
    }

    function aplicarControleAcesso() {
        const path = window.location.pathname;
        const papel = papelUsuario();
        if (PAGINAS_GESTAO.includes(path)) {
            if (!papel) {
                window.location.href = '/login';
                return;
            }
            if (papel !== 'ORGANIZADOR') {
                window.location.href = '/area-participante';
                return;
            }
        }
        // Sidebar padrão do participante (idêntica em todas as telas):
        // Eventos disponíveis, Programação, Área do Participante.
        // Reconstrói qualquer sidebar (ex.: a admin da programação) quando
        // quem vê não é organizador. O organizador mantém a dele.
        if (papel !== 'ORGANIZADOR') padronizarSidebarParticipante();
    }

    function padronizarSidebarParticipante() {
        const nav = document.querySelector('.sidebar .nav');
        if (!nav || nav.dataset.padronizada === 'ok') return;
        nav.dataset.padronizada = 'ok';
        nav.innerHTML = '';
        [
            ['nav-eventos-disponiveis', 'Eventos disponíveis'],
            ['nav-programacao-part', 'Programação'],
            ['nav-area-participante', 'Área do Participante']
        ].forEach(([odId, rotulo]) => {
            const btn = document.createElement('button');
            btn.className = 'nav-btn';
            btn.setAttribute('data-od-id', odId);
            btn.textContent = rotulo;
            nav.appendChild(btn);
        });
        const rodape = document.querySelector('.sidebar .org, .sidebar .part-org');
        if (rodape) {
            rodape.className = 'part-org';
            rodape.innerHTML = '';
            const nome = document.createElement('span');
            nome.textContent = 'IngressoNow';
            const forte = document.createElement('strong');
            forte.textContent = 'Portal do participante';
            rodape.append(nome, forte);
        }
    }

    function ligarNav() {
        // Logo → página principal de cada papel: organizador volta ao
        // dashboard; participante/visitante volta aos Eventos disponíveis (/).
        // (Antes ia sempre p/ /dashboard, e o guarda rebatia o participante
        // para a área do participante.)
        const destinoLogo = papelUsuario() === 'ORGANIZADOR' ? '/dashboard' : '/';
        const tituloLogo = destinoLogo === '/dashboard' ? 'Ir para o Dashboard' : 'Ir para Eventos disponíveis';
        document.querySelectorAll('.sidebar .brand, .logo[data-od-id="logo-brand"]').forEach(logo => {
            logo.style.cursor = 'pointer';
            logo.setAttribute('title', tituloLogo);
            logo.setAttribute('role', 'link');
            logo.setAttribute('tabindex', '0');
            logo.addEventListener('click', () => {
                window.location.href = destinoLogo;
            });
            logo.addEventListener('keydown', (ev) => {
                if (ev.key === 'Enter' || ev.key === ' ') {
                    ev.preventDefault();
                    window.location.href = destinoLogo;
                }
            });
        });
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
            aplicarControleAcesso();
            garantirIcones();
            garantirFavicon();
            ligarNav();
            configurarPerfil();
        });
    } else {
        aplicarControleAcesso();
        garantirIcones();
        garantirFavicon();
        ligarNav();
        configurarPerfil();
    }
})();
