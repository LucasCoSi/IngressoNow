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
        } catch (error) {
            console.error('[Relatorios] Erro ao carregar eventos:', error);
            window.API.exibirErro(error);
        }
    }

    async function baixarRelatorio() {
        const eventoId = eventSelect ? eventSelect.value : null;
        if (!eventoId) {
            window.API.exibirErro(new window.API.APIError('Selecione um evento.', 400));
            return;
        }
        try {
            const token = window.Auth ? window.Auth.getToken() : null;
            const headers = {};
            if (token) headers['Authorization'] = 'Bearer ' + token;
            const resposta = await fetch(`/api/controle/relatorio/presenca/${encodeURIComponent(eventoId)}`, {
                method: 'GET',
                headers
            });
            if (!resposta.ok) throw new Error(`Falha ao gerar relatorio (HTTP ${resposta.status})`);
            const csv = await resposta.text();
            const blob = new Blob([csv], { type: 'text/csv' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `presenca-${eventoId}.csv`;
            document.body.appendChild(a);
            a.click();
            a.remove();
            URL.revokeObjectURL(url);
            window.API.exibirSucesso('Relatorio baixado com sucesso!');
        } catch (error) {
            console.error('[Relatorios] Erro ao baixar:', error);
            window.API.exibirErro(error);
        }
    }

    if (generateBtn) generateBtn.addEventListener('click', baixarRelatorio);

    await carregarEventos();
});
