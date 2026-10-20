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
        'nav-area-participante': '/area-participante',
        // Atalho injetado nas páginas de participante quando quem navega é
        // organizador/admin (senão a gestão ficaria inacessível dali).
        'nav-voltar-gestao': '/dashboard'
    };

    // Ícones que faltam em algumas páginas (botões invisíveis sem o symbol).
    // Inclui os ícones da navegação de gestão: as páginas de participante não
    // têm <defs>/<symbol>, e a sidebar de gestão é reconstruída nelas.
    const ICONES_EXTRAS = {
        'i-eye': '<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8Z"/><circle cx="12" cy="12" r="3"/>',
        'i-check': '<polyline points="20 6 9 17 4 12"/>',
        'i-x': '<line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>',
        'i-plus': '<line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>',
        'i-mail': '<rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 7-10 6L2 7"/>',
        'i-download': '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>',
        'i-camera': '<path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2Z"/><circle cx="12" cy="13" r="4"/>',
        // Navegação de gestão (mesmos ids/símbolos das páginas administrativas).
        'i-home': '<path d="m3 10 9-7 9 7v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1Z"/><path d="M9 21v-7h6v7"/>',
        'i-calendar': '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 11h18"/>',
        'i-grid': '<rect x="4" y="4" width="6" height="6" rx="1"/><rect x="14" y="4" width="6" height="6" rx="1"/><rect x="4" y="14" width="6" height="6" rx="1"/><rect x="14" y="14" width="6" height="6" rx="1"/>',
        'i-schedule': '<circle cx="12" cy="12" r="8"/><path d="M12 8v5l3 2"/>',
        'i-inscrição': '<path d="M4 7a2 2 0 0 0 0 4v2a2 2 0 0 0 0 4v1h16v-1a2 2 0 0 0 0-4v-2a2 2 0 0 0 0-4V6H4Z"/><path d="M13 7v10"/>',
        'i-scan': '<path d="M5 9V5h4M15 5h4v4M19 15v4h-4M9 19H5v-4"/><path d="M8 11h3v3H8zM14 10h2v2h-2zM14 14h2v2h-2z"/>',
        'i-star': '<path d="m12 3 2.7 5.5 6.1.9-4.4 4.3 1 6.1-5.4-2.8-5.4 2.8 1-6.1-4.4-4.3 6.1-.9Z"/>',
        'i-chart': '<path d="M4 19V5M4 19h16"/><path d="M8 16v-4M12 16V8M16 16v-7"/>',
        'i-users': '<circle cx="9" cy="8" r="3"/><path d="M3 20c0-3.3 2.7-6 6-6s6 2.7 6 6M16 5.3a3 3 0 0 1 0 5.4M18 14c1.8.8 3 2.6 3 4.7"/>',
        'i-settings': '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-2.1 2.1-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.5v.2h-3v-.2a1.7 1.7 0 0 0-1-1.5 1.7 1.7 0 0 0-1.9.3l-.1.1-2.1-2.1.1-.1A1.7 1.7 0 0 0 7 15a1.7 1.7 0 0 0-1.5-1H5.3v-3h.2A1.7 1.7 0 0 0 7 10a1.7 1.7 0 0 0-.3-1.9l-.1-.1 2.1-2.1.1.1a1.7 1.7 0 0 0 1.9.3 1.7 1.7 0 0 0 1-1.5v-.2h3v.2a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.9-.3l.1-.1 2.1 2.1-.1.1a1.7 1.7 0 0 0-.3 1.9 1.7 1.7 0 0 0 1.5 1h.2v3h-.2a1.7 1.7 0 0 0-1.4 1Z"/>'
    };

    const SVGNS = 'http://www.w3.org/2000/svg';

    function garantirIcones() {
        let defs = document.querySelector('svg defs');
        if (!defs) {
            // Páginas de participante não trazem <svg><defs>: cria um oculto
            // (senão os botões de gestão ficam sem ícone).
            const svg = document.createElementNS(SVGNS, 'svg');
            svg.setAttribute('aria-hidden', 'true');
            svg.style.cssText = 'position:absolute;width:0;height:0;overflow:hidden';
            defs = document.createElementNS(SVGNS, 'defs');
            svg.appendChild(defs);
            document.body.appendChild(svg);
        }
        for (const [id, conteudo] of Object.entries(ICONES_EXTRAS)) {
            if (!document.getElementById(id)) {
                const sym = document.createElementNS(SVGNS, 'symbol');
                sym.setAttribute('id', id);
                sym.setAttribute('viewBox', '0 0 24 24');
                sym.innerHTML = conteudo;
                defs.appendChild(sym);
            }
        }
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
            // Organizador/admin: atalho de volta ao painel em qualquer página.
            if (papelUsuario() === 'ORGANIZADOR') {
                menu.append(itemMenu('Painel de gestão', '/dashboard'));
            }
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

    function isAdmin() {
        const auth = window.Auth || null;
        const u = auth && auth.getUser ? auth.getUser() : null;
        return String((u && u.tipo) || '').toUpperCase() === 'ADMINISTRADOR';
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
        // Organizador/admin em página de participante: mantém a navegação de
        // gestão (uma única barra, a mesma do painel).
        else padronizarSidebarGestao();
        // /usuarios é só do admin (§3): esconde o atalho dos organizadores
        // (a API também barra com 403).
        if (!isAdmin()) {
            document.querySelectorAll('[data-od-id="nav-users"]').forEach(b => {
                b.style.display = 'none';
            });
            if (path === '/usuarios') {
                window.location.href = '/eventos';
                return;
            }
        }
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

    const CSS_RETORNO_GESTAO = [
        '.sidebar .nav .nav-group{margin:19px 10px 7px;color:#93A7C6;font:600 10px var(--font-mono, Consolas, monospace);letter-spacing:.08em;text-transform:uppercase}'
    ].join('\n');

    function garantirCssRetornoGestao() {
        if (document.getElementById('nav-gestao-css')) return;
        const style = document.createElement('style');
        style.id = 'nav-gestao-css';
        style.textContent = CSS_RETORNO_GESTAO;
        document.head.appendChild(style);
    }

    // Botão idêntico ao da sidebar do painel: ícone (<use>) + rótulo.
    function botaoNav(odId, rotulo, icone) {
        const btn = document.createElement('button');
        btn.className = 'nav-btn';
        btn.setAttribute('data-od-id', odId);
        if (icone) {
            const svg = document.createElementNS(SVGNS, 'svg');
            svg.setAttribute('class', 'icon');
            svg.setAttribute('aria-hidden', 'true');
            const use = document.createElementNS(SVGNS, 'use');
            use.setAttribute('href', '#' + icone);
            svg.appendChild(use);
            btn.appendChild(svg);
        }
        btn.appendChild(document.createTextNode(rotulo));
        return btn;
    }

    // Itens da navegação de gestão (espelham as páginas administrativas).
    const ITENS_GESTAO = [
        ['nav-dashboard', 'Dashboard', 'i-home'],
        ['nav-events', 'Eventos', 'i-calendar'],
        ['nav-activities', 'Atividades', 'i-grid'],
        ['nav-schedule', 'Programação', 'i-schedule'],
        ['nav-enrollments', 'Inscrições', 'i-inscrição'],
        ['nav-attendance', 'Frequência', 'i-scan'],
        ['nav-evaluations', 'Avaliações', 'i-star'],
        ['nav-reports', 'Relatórios', 'i-chart'],
        ['nav-users', 'Usuários', 'i-users'],
        ['nav-settings', 'Configurações', 'i-settings']
    ];

    // Organizador/admin em páginas de participante (/, /evento-publico,
    // /area-participante): a sidebar ali é a do participante. Reconstrói com a
    // MESMA navegação de gestão do painel (ícones + grupo "Administração"),
    // para haver uma única barra — sem duplicar itens de participante.
    function padronizarSidebarGestao() {
        const nav = document.querySelector('.sidebar .nav');
        if (!nav || nav.dataset.gestao === 'ok') return;
        // Já é uma página de gestão (a sidebar real): nada a fazer.
        if (nav.querySelector('[data-od-id="nav-dashboard"]')) return;
        nav.dataset.gestao = 'ok';
        garantirCssRetornoGestao();
        garantirIcones();
        nav.innerHTML = '';
        ITENS_GESTAO.forEach(([odId, rotulo, icone]) => {
            if (odId === 'nav-users') {
                const grupo = document.createElement('p');
                grupo.className = 'nav-group';
                grupo.textContent = 'Administração';
                nav.appendChild(grupo);
            }
            nav.appendChild(botaoNav(odId, rotulo, icone));
        });
        // Rodapé igual ao do painel (a página de participante traz outro).
        const rodape = document.querySelector('.sidebar .org, .sidebar .part-org');
        if (rodape) {
            rodape.innerHTML = '';
            rodape.appendChild(document.createTextNode('Organização acadêmica'));
            const forte = document.createElement('strong');
            forte.textContent = 'Centro de Eventos Sul';
            rodape.appendChild(forte);
        }
    }

    // §3: o rodapé da gestão mostra o nome da instituição configurado pelo
    // admin em /api/configuracoes. Falha silenciosa mantém o texto do HTML.
    async function atualizarIdentidadeOrganizacao() {
        const papel = papelUsuario();
        if (papel !== 'ORGANIZADOR' && papel !== 'ADMINISTRADOR') return;
        const auth = window.Auth;
        const token = auth && auth.getToken ? auth.getToken() : null;
        if (!token) return;
        try {
            const resp = await fetch('/api/configuracoes', {
                headers: { 'Accept': 'application/json', 'Authorization': 'Bearer ' + token }
            });
            if (!resp.ok) return;
            const cfg = await resp.json();
            if (!cfg || !cfg.nomeInstituicao) return;
            document.querySelectorAll('.sidebar .org strong').forEach(el => {
                el.textContent = cfg.nomeInstituicao;
            });
        } catch (ignorado) {
            // Mantém o texto estático do HTML.
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
            atualizarIdentidadeOrganizacao();
        });
    } else {
        aplicarControleAcesso();
        garantirIcones();
        garantirFavicon();
        ligarNav();
        configurarPerfil();
        atualizarIdentidadeOrganizacao();
    }
})();
