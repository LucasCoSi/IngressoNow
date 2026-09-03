/**
 * frequencia.js - Controle de Frequencia e Check-in
 * Integracao da pagina ingressonow-frequencia.html com /api/controle
 */

document.addEventListener('DOMContentLoaded', function() {
    console.log('[Frequencia] Inicializando pagina...');

    const validatorInput = document.querySelector('[data-od-id="inscricao-validator"]');
    const feedbackCard = document.querySelector('[data-od-id="feedback-success"]');
    const cameraBtn = document.querySelector('[data-od-id="open-camera-cta"]');

    function authHeaders() {
        const headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
        const token = window.Auth ? window.Auth.getToken() : null;
        if (token) headers['Authorization'] = 'Bearer ' + token;
        return headers;
    }

    async function realizarCheckIn(qrCodeToken) {
        if (!qrCodeToken || !qrCodeToken.trim()) {
            window.API.exibirErro(new window.API.APIError('Informe o codigo do QR Code.', 400));
            return;
        }
        try {
            await window.API.Controle.realizarCheckIn(qrCodeToken.trim());
            window.API.exibirSucesso('Check-in realizado com sucesso!');
            if (validatorInput) validatorInput.value = '';
        } catch (error) {
            console.error('[Frequencia] Erro no check-in:', error);
            window.API.exibirErro(error);
        }
    }

    // Validador: Enter confirma o check-in com o token digitado/escaneado
    if (validatorInput) {
        validatorInput.addEventListener('keydown', function(e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                realizarCheckIn(validatorInput.value);
            }
        });
    }

    // Botao de camera: sem WebRTC no MVP, orienta a digitar o token
    if (cameraBtn) {
        cameraBtn.addEventListener('click', function() {
            window.API.exibirErro(new window.API.APIError('Leitura por camera indisponivel no MVP. Digite o token do QR Code no validador.', 400));
            if (validatorInput) validatorInput.focus();
        });
    }

    // Expõe gerador de QR para uso no console/admin: gerarQrCode(inscricaoId, atividadeId)
    window.gerarQrCode = async function(inscricaoId, atividadeId) {
        try {
            const token = await window.API.Controle.gerarQrCode(inscricaoId, atividadeId);
            console.log('[Frequencia] QR token:', token);
            if (validatorInput) validatorInput.value = token;
            return token;
        } catch (error) {
            window.API.exibirErro(error);
            throw error;
        }
    };
});
