/**
 * relatorios.js - Relatórios & Analíticos (RF-29/30/31).
 * Gráficos SVG com dados reais da API + exportação CSV (servidor) e
 * XLSX/PDF (gerados no navegador, sem dependências).
 * Sem fonte no backend, sem gráfico: financeiro (§2.3) e NPS exibem "—".
 */

document.addEventListener('DOMContentLoaded', async function() {
    console.log('[Relatorios] Inicializando pagina...');

    const eventSelect = document.getElementById('event-select');
    const periodSelect = document.getElementById('period-select');
    const generateBtn = document.querySelector('[data-od-id="generate-report-cta"]');
    const scheduleBtn = document.querySelector('[data-od-id="schedule-email-btn"]');
    const grafLinha = document.getElementById('graf-inscricoes');
    const grafDonut = document.getElementById('graf-presenca');
    const legendaDonut = document.getElementById('legenda-presenca');

    let inscricoesCache = [];
    let presencaCache = [];

    function authHeaders(json) {
        const h = { 'Accept': 'application/json' };
        if (json) h['Content-Type'] = 'application/json';
        const t = window.Auth ? window.Auth.getToken() : null;
        if (t) h['Authorization'] = 'Bearer ' + t;
        return h;
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text == null ? '' : String(text);
        return div.innerHTML;
    }

    function eventoId() {
        return eventSelect ? eventSelect.value : '';
    }

    function exigirEvento() {
        const id = eventoId();
        if (!id) window.API.exibirErro(new window.API.APIError('Selecione um evento.', 400));
        return id;
    }

    /* ---------------- eventos + métricas ---------------- */

    async function carregarEventos() {
        try {
            const user = window.Auth ? window.Auth.getUser() : null;
            const eventos = (user && user.usuarioId)
                ? await window.API.Eventos.listarPorOrganizador(user.usuarioId).catch(
                    () => window.API.Eventos.listar())
                : await window.API.Eventos.listar();
            if (eventSelect) {
                eventSelect.innerHTML = eventos.length === 0
                    ? '<option value="">Nenhum evento encontrado</option>'
                    : eventos.map(e =>
                        `<option value="${e.id}">${escapeHtml(e.titulo)}</option>`).join('');
            }
            if (eventos.length > 0) await atualizarTudo(eventos[0].id);
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
        if (grafLinha) grafLinha.textContent = 'Sem dados.';
        if (grafDonut) grafDonut.textContent = 'Sem dados.';
        if (legendaDonut) legendaDonut.textContent = '';
    }

    async function atualizarTudo(id) {
        const headers = authHeaders();
        try {
            inscricoesCache = await fetch(`/api/inscricoes?eventoId=${encodeURIComponent(id)}`,
                { headers }).then(r => r.ok ? r.json() : []);
            setMetrica('metric-inscribed', inscricoesCache.length);
            const csv = await fetch(`/api/controle/relatorio/presenca/${encodeURIComponent(id)}`,
                { headers }).then(r => r.ok ? r.text() : '');
            presencaCache = csv.split('\n').slice(1).map(l => l.trim()).filter(Boolean)
                .map(l => l.split(','));
            const presentes = presencaCache.filter(c => c[3] === 'PRESENTE').length;
            setMetrica('metric-attendance', presencaCache.length > 0
                ? (Math.round(presentes / presencaCache.length * 1000) / 10).toString().replace('.', ',') + '%'
                : '—');
            const certs = await fetch(`/api/eventos/${encodeURIComponent(id)}/certificados`,
                { headers }).then(r => r.ok ? r.json() : []);
            setMetrica('metric-certificates', Array.isArray(certs) ? certs.length : '—');
        } catch (err) {
            console.warn('[Relatorios] Métricas indisponíveis:', err);
        }
        setMetrica('metric-nps', '—');
        desenharGraficos();
    }

    /* ---------------- gráficos SVG ---------------- */

    function semanaDe(iso) {
        const d = new Date(iso);
        if (isNaN(d)) return null;
        const dia = String(d.getDate()).padStart(2, '0');
        const mes = String(d.getMonth() + 1).padStart(2, '0');
        return `${dia}/${mes}`;
    }

    function desenharLinha() {
        if (!grafLinha) return;
        const dias = (periodSelect && periodSelect.value === 'full') ? Infinity : 30;
        const corte = Date.now() - dias * 86400000;
        const grupos = new Map();
        inscricoesCache.forEach(i => {
            const t = new Date(i.dataInscricao).getTime();
            if (isNaN(t) || t < corte) return;
            const rot = semanaDe(i.dataInscricao);
            if (rot) grupos.set(rot, (grupos.get(rot) || 0) + 1);
        });
        const pts = [...grupos.entries()].slice(-12);
        if (pts.length === 0) {
            grafLinha.textContent = 'Sem inscrições no período.';
            return;
        }
        const W = 400, H = 300, P = 34;
        const max = Math.max(...pts.map(p => p[1]), 1);
        const x = (i) => pts.length === 1 ? W / 2 : P + i * (W - 2 * P) / (pts.length - 1);
        const y = (v) => H - P - (v / max) * (H - 2 * P - 20);
        const linha = pts.map((p, i) => `${x(i).toFixed(1)},${y(p[1]).toFixed(1)}`).join(' ');
        const area = `${P},${H - P} ${linha} ${x(pts.length - 1).toFixed(1)},${H - P}`;
        grafLinha.innerHTML = `
        <svg viewBox="0 0 ${W} ${H}" width="100%" height="300" style="display:block">
          <defs><linearGradient id="rl-grad" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0" stop-color="#0284C7" stop-opacity=".3"/>
            <stop offset="1" stop-color="#0284C7" stop-opacity="0"/>
          </linearGradient></defs>
          <polygon points="${area}" fill="url(#rl-grad)"/>
          <polyline points="${linha}" fill="none" stroke="#0284C7" stroke-width="3" stroke-linejoin="round"/>
          ${pts.map((p, i) => `
            <circle cx="${x(i).toFixed(1)}" cy="${y(p[1]).toFixed(1)}" r="4" fill="#0284C7">
              <title>${escapeHtml(p[0])}: ${p[1]}</title></circle>
            <text x="${x(i).toFixed(1)}" y="${(y(p[1]) - 10).toFixed(1)}" text-anchor="middle" font-size="11" fill="#0F172A" font-weight="700">${p[1]}</text>
            <text x="${x(i).toFixed(1)}" y="${H - 12}" text-anchor="middle" font-size="10" fill="#64748B">${escapeHtml(p[0])}</text>`).join('')}
        </svg>`;
    }

    function desenharDonut() {
        if (!grafDonut) return;
        const cont = { PRESENTE: 0, AUSENTE: 0, PENDENTE_SAIDA: 0, OUTROS: 0 };
        presencaCache.forEach(c => {
            const s = (c[3] || '').trim();
            if (s in cont) cont[s] += 1;
            else cont.OUTROS += 1;
        });
        const total = Object.values(cont).reduce((a, b) => a + b, 0);
        if (total === 0) {
            grafDonut.textContent = 'Sem registros de presença.';
            if (legendaDonut) legendaDonut.textContent = '';
            return;
        }
        const segs = [
            { rot: 'Presentes', v: cont.PRESENTE, cor: '#0284C7' },
            { rot: 'Ausentes', v: cont.AUSENTE, cor: '#94A3B8' },
            { rot: 'Saída pendente', v: cont.PENDENTE_SAIDA, cor: '#38BDF8' },
            { rot: 'Outros', v: cont.OUTROS, cor: '#BAE6FD' }
        ].filter(s => s.v > 0);
        const C = 2 * Math.PI * 90;
        let acum = 0;
        const top = segs[0];
        grafDonut.innerHTML = `
        <svg viewBox="0 0 300 300" width="100%" height="300" style="display:block;max-width:250px;margin:0 auto">
          ${segs.map(s => {
                const fracao = s.v / total;
                const dash = `${(fracao * C).toFixed(1)} ${C.toFixed(1)}`;
                const off = (-acum * C).toFixed(1);
                acum += fracao;
                return `<circle cx="150" cy="150" r="90" fill="none" stroke="${s.cor}"
                  stroke-width="60" stroke-dasharray="${dash}" stroke-dashoffset="${off}">
                  <title>${escapeHtml(s.rot)}: ${s.v}</title></circle>`;
            }).join('')}
          <text x="150" y="160" text-anchor="middle" font-size="32" font-weight="700" fill="#0F172A">${Math.round(top.v / total * 100)}%</text>
        </svg>`;
        if (legendaDonut) {
            legendaDonut.textContent = segs.map(s =>
                `${s.rot} ${Math.round(s.v / total * 100)}%`).join(' • ');
        }
    }

    function desenharGraficos() {
        desenharLinha();
        desenharDonut();
    }

    /* ---------------- download ---------------- */

    function baixar(blob, nome) {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = nome;
        document.body.appendChild(a);
        a.click();
        a.remove();
        URL.revokeObjectURL(url);
    }

    async function baixarCsvServidor(rota, prefixo, id) {
        const t = window.Auth ? window.Auth.getToken() : null;
        const headers = {};
        if (t) headers['Authorization'] = 'Bearer ' + t;
        const resposta = await fetch(`/api/controle/relatorio/${rota}/${encodeURIComponent(id)}`, { headers });
        if (!resposta.ok) throw new Error(`Falha ao gerar relatório (HTTP ${resposta.status})`);
        baixar(await resposta.blob(), `${prefixo}-${id.substring(0, 8)}.csv`);
    }

    function tabelaParaCsv(cabecalho, linhas) {
        const cel = (v) => {
            const s = v == null ? '' : String(v);
            return /[";\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
        };
        return '﻿' + [cabecalho, ...linhas].map(l => l.map(cel).join(';')).join('\n');
    }

    /* XLSX mínimo (ZIP com entradas stored + CRC32), sem dependências. */
    const CRC_T = (() => {
        const t = new Uint32Array(256);
        for (let n = 0; n < 256; n++) {
            let c = n;
            for (let k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
            t[n] = c >>> 0;
        }
        return t;
    })();

    function crc32(bytes) {
        let c = 0xFFFFFFFF;
        for (let i = 0; i < bytes.length; i++) c = CRC_T[(c ^ bytes[i]) & 0xFF] ^ (c >>> 8);
        return (c ^ 0xFFFFFFFF) >>> 0;
    }

    function tabelaParaXlsx(nomeAba, cabecalho, linhas) {
        const esc = (s) => String(s == null ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        const col = (i) => {
            let s = '', n = i;
            do { s = String.fromCharCode(65 + (n % 26)) + s; n = Math.floor(n / 26) - 1; } while (n >= 0);
            return s;
        };
        const celula = (v, ref) => {
            if (typeof v === 'number') return `<c r="${ref}"><v>${v}</v></c>`;
            return `<c r="${ref}" t="inlineStr"><is><t>${esc(v)}</t></is></c>`;
        };
        const lin = (valores, r) =>
            `<row r="${r}">${valores.map((v, i) => celula(v, col(i) + r)).join('')}</row>`;
        const todas = [cabecalho, ...linhas];
        const folha = `<?xml version="1.0" encoding="UTF-8"?>` +
            `<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">` +
            `<sheetData>${todas.map((l, i) => lin(l, i + 1)).join('')}</sheetData></worksheet>`;
        const arquivos = [
            ['[Content_Types].xml',
                `<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>`],
            ['_rels/.rels',
                `<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>`],
            ['xl/workbook.xml',
                `<?xml version="1.0" encoding="UTF-8"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="${esc(nomeAba).substring(0, 31)}" sheetId="1" r:id="rId1"/></sheets></workbook>`],
            ['xl/_rels/workbook.xml.rels',
                `<?xml version="1.0" encoding="UTF-8"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>`],
            ['xl/worksheets/sheet1.xml', folha]
        ];
        const enc = new TextEncoder();
        const partes = [];
        const central = [];
        let off = 0;
        arquivos.forEach(([nome, texto]) => {
            const dados = enc.encode(texto);
            const crc = crc32(dados);
            const nomeB = enc.encode(nome);
            const local = [];
            const push = (b) => { local.push(b); off += b.length; };
            const cab = new Uint8Array(30);
            const cv = new DataView(cab.buffer);
            cv.setUint32(0, 0x04034b50, true);
            cv.setUint16(4, 20, true);
            cv.setUint16(6, 0, true);
            cv.setUint16(8, 0, true);
            cv.setUint16(10, 0, true);
            cv.setUint16(12, 0, true);
            cv.setUint32(14, crc, true);
            cv.setUint32(18, dados.length, true);
            cv.setUint32(22, dados.length, true);
            cv.setUint16(26, nomeB.length, true);
            cv.setUint16(28, 0, true);
            push(cab); push(nomeB); push(dados);
            central.push({ nomeB, crc, tam: dados.length, off: off - dados.length - nomeB.length - 30 });
        });
        const iniCentral = off;
        const blobs = [];
        const pushC = (b) => { blobs.push(b); off += b.length; };
        // reemite locais na ordem (já somados em off); central agora:
        central.forEach(c => {
            const cab = new Uint8Array(46);
            const cv = new DataView(cab.buffer);
            cv.setUint32(0, 0x02014b50, true);
            cv.setUint16(4, 20, true);
            cv.setUint16(6, 20, true);
            cv.setUint16(8, 0, true);
            cv.setUint16(10, 0, true);
            cv.setUint16(12, 0, true);
            cv.setUint16(14, 0, true);
            cv.setUint32(16, c.crc, true);
            cv.setUint32(20, c.tam, true);
            cv.setUint32(24, c.tam, true);
            cv.setUint16(28, c.nomeB.length, true);
            cv.setUint16(30, 0, true);
            cv.setUint16(32, 0, true);
            cv.setUint16(34, 0, true);
            cv.setUint16(36, 0, true);
            cv.setUint32(38, 0, true);
            cv.setUint32(42, c.off, true);
            pushC(cab); pushC(c.nomeB);
        });
        const fim = new Uint8Array(22);
        const fv = new DataView(fim.buffer);
        fv.setUint32(0, 0x06054b50, true);
        fv.setUint16(8, central.length, true);
        fv.setUint16(10, central.length, true);
        fv.setUint32(12, off - iniCentral, true);
        fv.setUint32(16, iniCentral, true);
        pushC(fim);
        // Junta locais + central: reconstroi na ordem (locais foram medidos, reemite)
        const locais = [];
        arquivos.forEach(([nome, texto]) => {
            const dados = enc.encode(texto);
            const nomeB = enc.encode(nome);
            const cab = new Uint8Array(30);
            const cv = new DataView(cab.buffer);
            cv.setUint32(0, 0x04034b50, true);
            cv.setUint16(4, 20, true);
            cv.setUint32(14, crc32(dados), true);
            cv.setUint32(18, dados.length, true);
            cv.setUint32(22, dados.length, true);
            cv.setUint16(26, nomeB.length, true);
            locais.push(cab, nomeB, dados);
        });
        return new Blob([...locais, ...blobs], {
            type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        });
    }

    /* PDF mínimo (texto, Helvetica, A4), sem dependências. */
    function tabelaParaPdf(titulo, cabecalho, linhas) {
        const limpa = (s) => String(s == null ? '' : s).normalize('NFD')
            .replace(/[̀-ͯ]/g, '').replace(/[^\x20-\x7E]/g, '-');
        const esc = (s) => limpa(s).replace(/\\/g, '\\\\').replace(/\(/g, '\\(').replace(/\)/g, '\\)');
        const W = 595, H = 842, M = 48, LH = 14, FS = 10;
        const porPag = Math.floor((H - 2 * M - 40) / LH);
        const blocos = [];
        for (let i = 0; i < linhas.length; i += porPag) blocos.push(linhas.slice(i, i + porPag));
        if (blocos.length === 0) blocos.push([]);
        const objs = [];
        objs[1] = '<< /Type /Catalog /Pages 2 0 R >>';
        const kids = blocos.map((_, i) => `${3 + i * 2} 0 R`).join(' ');
        objs[2] = `<< /Type /Pages /Kids [${kids}] /Count ${blocos.length} >>`;
        const conteudos = [];
        blocos.forEach((bloco, p) => {
            const num = 3 + p * 2;
            objs[num] = `<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${W} ${H}] ` +
                `/Resources << /Font << /F1 ${3 + blocos.length * 2} 0 R /F2 ${4 + blocos.length * 2} 0 R >> >> ` +
                `/Contents ${num + 1} 0 R >>`;
            let y = H - M;
            let st = `BT /F2 14 Tf ${M} ${y} Td (${esc(titulo)}) Tj ET\n`;
            y -= 26;
            st += `BT /F1 ${FS} Tf ${M} ${y} Td (${esc(cabecalho.join(' | ').substring(0, 110))}) Tj ET\n`;
            y -= LH;
            bloco.forEach(l => {
                st += `BT /F1 ${FS} Tf ${M} ${y} Td (${esc(l.join(' | ').substring(0, 110))}) Tj ET\n`;
                y -= LH;
            });
            conteudos.push(st);
            objs[num + 1] = `<< /Length ${new TextEncoder().encode(st).length} >>\nstream\n${st}endstream`;
        });
        const f1 = 3 + blocos.length * 2, f2 = 4 + blocos.length * 2;
        objs[f1] = '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>';
        objs[f2] = '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>';
        let pdf = '%PDF-1.4\n';
        const offs = [0];
        for (let i = 1; i <= f2; i++) {
            offs[i] = new TextEncoder().encode(pdf).length;
            pdf += `${i} 0 obj\n${objs[i]}\nendobj\n`;
        }
        const iniXref = new TextEncoder().encode(pdf).length;
        pdf += `xref\n0 ${f2 + 1}\n0000000000 65535 f \n`;
        for (let i = 1; i <= f2; i++) pdf += `${String(offs[i]).padStart(10, '0')} 00000 n \n`;
        pdf += `trailer\n<< /Size ${f2 + 1} /Root 1 0 R >>\nstartxref\n${iniXref}\n%%EOF`;
        return new Blob([new TextEncoder().encode(pdf)], { type: 'application/pdf' });
    }

    /* ---------------- exportações por modelo ---------------- */

    async function exportarFrequencia(formato) {
        const id = exigirEvento();
        if (!id) return;
        if (formato === 'csv') {
            await baixarCsvServidor('presenca', 'presenca', id);
        } else {
            if (presencaCache.length === 0) {
                window.API.exibirErro(new window.API.APIError('Sem registros de presença para exportar.', 400));
                return;
            }
            const cab = ['InscricaoID', 'ParticipanteID', 'AtividadeID', 'StatusPresenca'];
            const nome = `presenca-${id.substring(0, 8)}`;
            if (formato === 'xlsx') baixar(tabelaParaXlsx('Frequencia', cab, presencaCache), `${nome}.xlsx`);
            else baixar(tabelaParaPdf('Lista de Frequencia e Check-in', cab, presencaCache), `${nome}.pdf`);
        }
        window.API.exibirSucesso('Relatório de frequência exportado!');
    }

    async function exportarSatisfacao(formato) {
        const id = exigirEvento();
        if (!id) return;
        try {
            const headers = authHeaders();
            const quests = await fetch(`/api/eventos/${encodeURIComponent(id)}/questionarios`,
                { headers }).then(r => r.ok ? r.json() : []);
            if (!quests || quests.length === 0) {
                window.API.exibirErro(new window.API.APIError('Evento sem questionário para consolidar.', 400));
                return;
            }
            const qid = quests[0].id || quests[0].questionarioId;
            const res = await fetch(`/api/questionarios/${encodeURIComponent(qid)}/resultados`,
                { headers }).then(r => {
                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                return r.json();
            });
            const cab = ['Pergunta', 'Tipo', 'Respostas', 'Media', 'Distribuicao'];
            const linhas = (res.perguntas || []).map(p => [
                p.enunciado,
                p.tipo,
                p.totalRespostas,
                p.media == null ? '—' : p.media,
                Object.entries(p.distribuicao || {}).map(([k, v]) => `${k}:${v}`).join(' ')
            ]);
            const nome = `satisfacao-${id.substring(0, 8)}`;
            if (formato === 'csv') baixar(new Blob([tabelaParaCsv(cab, linhas)], { type: 'text/csv' }), `${nome}.csv`);
            else if (formato === 'xlsx') baixar(tabelaParaXlsx('Satisfacao', cab, linhas), `${nome}.xlsx`);
            else baixar(tabelaParaPdf('Consolidado de Satisfacao', cab, linhas), `${nome}.pdf`);
            window.API.exibirSucesso(`Satisfação exportada (${res.totalAvaliacoes || 0} avaliações)!`);
        } catch (error) {
            console.error('[Relatorios] Erro satisfação:', error);
            window.API.exibirErro(error);
        }
    }

    async function exportarCertificados(formato) {
        const id = exigirEvento();
        if (!id) return;
        try {
            const headers = authHeaders();
            const certs = await fetch(`/api/eventos/${encodeURIComponent(id)}/certificados`,
                { headers }).then(r => {
                if (!r.ok) throw new Error(`HTTP ${r.status}`);
                return r.json();
            });
            if (!certs || certs.length === 0) {
                window.API.exibirErro(new window.API.APIError('Nenhum certificado emitido para este evento.', 400));
                return;
            }
            const cab = ['Codigo', 'Evento', 'EmitidoEm'];
            const linhas = certs.map(c => [c.codigo, c.eventoId, c.emitidoEm || c.dataEmissao || '—']);
            const nome = `certificados-${id.substring(0, 8)}`;
            if (formato === 'csv') baixar(new Blob([tabelaParaCsv(cab, linhas)], { type: 'text/csv' }), `${nome}.csv`);
            else if (formato === 'xlsx') baixar(tabelaParaXlsx('Certificados', cab, linhas), `${nome}.xlsx`);
            else baixar(tabelaParaPdf('Mapeamento de Certificados', cab, linhas), `${nome}.pdf`);
            window.API.exibirSucesso('Certificados exportados!');
        } catch (error) {
            console.error('[Relatorios] Erro certificados:', error);
            window.API.exibirErro(error);
        }
    }

    function ligar(odId, fn) {
        document.querySelectorAll(`[data-od-id="${odId}"]`).forEach(b => {
            if (b.disabled) return;
            b.addEventListener('click', async () => {
                try { await fn(); }
                catch (error) {
                    console.error('[Relatorios] Erro ao exportar:', error);
                    window.API.exibirErro(error);
                }
            });
        });
    }

    ligar('export-1-csv', () => exportarFrequencia('csv'));
    ligar('export-1-pdf', () => exportarFrequencia('pdf'));
    ligar('export-1-xlsx', () => exportarFrequencia('xlsx'));
    ligar('export-3-csv', () => exportarSatisfacao('csv'));
    ligar('export-3-pdf', () => exportarSatisfacao('pdf'));
    ligar('export-4-csv', () => exportarCertificados('csv'));
    ligar('export-4-xlsx', () => exportarCertificados('xlsx'));
    // report-2 (financeiro): sem fonte no backend (§2.3) — botões desabilitados no HTML.

    if (generateBtn) {
        generateBtn.addEventListener('click', async () => {
            const id = exigirEvento();
            if (!id) return;
            try {
                await baixarCsvServidor('presenca', 'presenca', id);
                window.API.exibirSucesso('Relatório de presença baixado com sucesso!');
            } catch (error) {
                window.API.exibirErro(error);
            }
        });
    }
    if (scheduleBtn) {
        scheduleBtn.addEventListener('click', () => {
            window.API.exibirErro(new window.API.APIError(
                'Agendamento de e-mail indisponível no MVP (envio é manual pela área de certificados).', 400));
        });
    }

    if (eventSelect) eventSelect.addEventListener('change', () => atualizarTudo(eventSelect.value));
    if (periodSelect) periodSelect.addEventListener('change', desenharGraficos);

    await carregarEventos();
});
