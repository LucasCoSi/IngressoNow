/**
 * relatorios.js - Download do relatorio CSV de presenca por evento
 * Integracao da pagina ingressonow-relatorios.html com /api/controle/relatorio/presenca/{eventoId}
 */

document.addEventListener('DOMContentLoaded', async function() {
    console.log('[Relatorios] Inicializando pagina...');

    const eventSelect = document.getElementById('event-select');
    const generateBtn = document.querySelector('[data-od-id="generate-report-cta"]');

    async function carregarEventos() {
        try {
            const eventos = await window.API.Eventos.listar();
            if (eventSelect) {
                eventSelect.innerHTML = eventos.map(e =>
                    `<option value="${e.id}">${e.titulo}</option>`).join('');
            }
            if (eventos.length > 0) await atualizarMetricas(eventos[0].id);
            else pintarVazio();
        } catch (error) {
            console.error('[Relatorios] Erro ao carregar eventos:', error);
            window.API.exibirErro(error);
        }
    }

    function setMetrica(odId, valor) {
        const el = document.querySelector(`[data-od-id="${odId}"] b`);
        if (el) el.textContent = valor;
    }

    function pintarVazio() {
        setMetrica('metric-inscribed', '0');
        setMetrica('metric-attendance', '—');
        setMetrica('metric-certificates', '0');
        setMetrica('metric-nps', '—');
    }

    // Métricas reais do evento selecionado (RF-29/30 + certificados).
    // NPS não tem fonte no backend do MVP — exibe "—".
    async function atualizarMetricas(eventoId) {
        const token = window.Auth ? window.Auth.getToken() : null;
        const headers = { 'Accept': 'application/json' };
        if (token) headers['Authorization'] = 'Bearer ' + token;
        try {
            const insc = await fetch(`/api/inscricoes?eventoId=${encodeURIComponent(eventoId)}`,
                { headers }).then(r => r.ok ? r.json() : []);
            setMetrica('metric-inscribed', insc.length);
            const csv = await fetch(`/api/controle/relatorio/presenca/${encodeURIComponent(eventoId)}`,
                { headers }).then(r => r.ok ? r.text() : '');
            const linhas = csv.split('\n').slice(1).filter(l => l.trim() !== '');
            const presentes = linhas.filter(l => l.includes('PRESENTE')).length;
            setMetrica('metric-attendance', linhas.length > 0
                ? (Math.round(presentes / linhas.length * 1000) / 10).toString().replace('.', ',') + '%'
                : '—');
            const certs = await fetch(`/api/eventos/${encodeURIComponent(eventoId)}/certificados`,
                { headers }).then(r => r.ok ? r.json() : []);
            setMetrica('metric-certificates', Array.isArray(certs) ? certs.length : '—');
        } catch (err) {
            console.warn('[Relatorios] Métricas indisponíveis:', err);
        }
        setMetrica('metric-nps', '—');
    }

    async function baixarCsv(rota, prefixo, eventoId) {
        const token = window.Auth ? window.Auth.getToken() : null;
        const headers = {};
        if (token) headers['Authorization'] = 'Bearer ' + token;
        const resposta = await fetch(`/api/controle/relatorio/${rota}/${encodeURIComponent(eventoId)}`, {
            method: 'GET',
            headers
        });
        if (!resposta.ok) throw new Error(`Falha ao gerar relatorio (HTTP ${resposta.status})`);
        const csv = await resposta.text();
        const blob = new Blob([csv], { type: 'text/csv' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${prefixo}-${eventoId}.csv`;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
    }

    async function baixarRelatorio() {
        const eventoId = eventSelect ? eventSelect.value : null;
        if (!eventoId) {
            window.API.exibirErro(new window.API.APIError('Selecione um evento.', 400));
            return;
        }
        try {
            await baixarCsv('presenca', 'presenca', eventoId);
            window.API.exibirSucesso('Relatorio de presença baixado com sucesso!');
        } catch (error) {
            console.error('[Relatorios] Erro ao baixar:', error);
            window.API.exibirErro(error);
        }
    }

    // RF-29: botão de relatório de inscritos ao lado do CTA principal
    async function baixarRelatorioInscritos() {
        const eventoId = eventSelect ? eventSelect.value : null;
        if (!eventoId) {
            window.API.exibirErro(new window.API.APIError('Selecione um evento.', 400));
            return;
        }
        try {
            await baixarCsv('inscritos', 'inscritos', eventoId);
            window.API.exibirSucesso('Relatorio de inscritos baixado com sucesso!');
        } catch (error) {
            console.error('[Relatorios] Erro ao baixar inscritos:', error);
            window.API.exibirErro(error);
        }
    }

    if (generateBtn) {
        generateBtn.addEventListener('click', baixarRelatorio);
        const btnInscritos = document.createElement('button');
        btnInscritos.className = 'secondary';
        btnInscritos.textContent = 'Baixar Inscritos (CSV)';
        btnInscritos.addEventListener('click', baixarRelatorioInscritos);
        generateBtn.parentNode.insertBefore(btnInscritos, generateBtn.nextSibling);
    }

    if (eventSelect) eventSelect.addEventListener('change', () => atualizarMetricas(eventSelect.value));

    await carregarEventos();
});
