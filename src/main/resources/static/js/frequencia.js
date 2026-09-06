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

    function mostrarFeedback(detalhes) {
        if (!feedbackCard) return;
        const el = feedbackCard.querySelector('.feedback-details');
        if (el) el.textContent = detalhes;
        feedbackCard.style.display = 'flex';
        setTimeout(() => { feedbackCard.style.display = 'none'; }, 6000);
    }

    async function realizarCheckIn(qrCodeToken) {
        if (!qrCodeToken || !qrCodeToken.trim()) {
            window.API.exibirErro(new window.API.APIError('Informe o codigo do QR Code.', 400));
            return;
        }
        try {
            await window.API.Controle.realizarCheckIn(qrCodeToken.trim());
            mostrarFeedback(`Check-in registrado às ${new Date().toLocaleTimeString('pt-BR')}.`);
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

    // RF-22: lançamento manual com autoria (organizador logado = responsável)
    const manualBtn = document.querySelector('[data-od-id="manual-checkin-cta"]');
    let manualForm = null;
    function garantirManualForm() {
        if (manualForm) return manualForm;
        manualForm = document.createElement('div');
        manualForm.style.cssText = 'display:grid;gap:8px;max-width:480px;margin-top:12px';
        manualForm.innerHTML = `
            <label style="font-size:12px">ID da inscrição
                <input data-manual="inscricao" style="width:100%;padding:8px" placeholder="uuid da inscrição">
            </label>
            <label style="font-size:12px">ID da atividade
                <input data-manual="atividade" style="width:100%;padding:8px" placeholder="uuid da atividade">
            </label>
            <button class="secondary" data-manual="enviar" style="justify-self:start">Lançar check-in manual</button>`;
        const panel = document.querySelector('[data-od-id="validator-section"]');
        (panel || document.body).appendChild(manualForm);
        manualForm.querySelector('[data-manual="enviar"]').addEventListener('click', async () => {
            const inscricaoId = manualForm.querySelector('[data-manual="inscricao"]').value.trim();
            const atividadeId = manualForm.querySelector('[data-manual="atividade"]').value.trim();
            if (!inscricaoId || !atividadeId) {
                window.API.exibirErro(new window.API.APIError('Informe inscrição e atividade.', 400));
                return;
            }
            try {
                await window.API.Controle.realizarCheckInManual(inscricaoId, atividadeId);
                window.API.exibirSucesso('Check-in manual lançado com autoria!');
            } catch (error) {
                console.error('[Frequencia] Erro no manual:', error);
                window.API.exibirErro(error);
            }
        });
        return manualForm;
    }
    if (manualBtn) {
        manualBtn.addEventListener('click', () => {
            const f = garantirManualForm();
            f.style.display = f.style.display === 'none' ? 'grid' : 'none';
        });
    } else {
        // Página sem CTA dedicado: cria botão junto ao painel validador
        const panel = document.querySelector('[data-od-id="validator-section"]');
        if (panel) {
            const btn = document.createElement('button');
            btn.className = 'secondary';
            btn.textContent = 'Check-in Manual';
            btn.style.marginTop = '8px';
            btn.addEventListener('click', () => {
                const f = garantirManualForm();
                f.style.display = f.style.display === 'none' ? 'grid' : 'none';
            });
            panel.appendChild(btn);
        }
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
