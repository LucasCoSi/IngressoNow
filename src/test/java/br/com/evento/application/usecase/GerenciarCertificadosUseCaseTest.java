package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.Certificado;
import br.com.evento.domain.model.entity.CriterioCertificado;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.Pessoa;
import br.com.evento.domain.model.vo.Email;
import br.com.evento.domain.model.vo.Periodo;
import br.com.evento.domain.model.vo.SenhaHash;
import br.com.evento.domain.port.out.CertificadoRepository;
import br.com.evento.domain.port.out.DadosCertificado;
import br.com.evento.domain.port.out.EmailMsg;
import br.com.evento.domain.port.out.EmailService;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import br.com.evento.domain.port.out.PdfCertificadoService;
import br.com.evento.domain.port.out.PessoaRepository;
import br.com.evento.domain.port.out.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * RF-32–35 (D) + CA-08 — elegibilidade calculável e emissão.
 */
class GerenciarCertificadosUseCaseTest {

    private Organizador org() {
        return new Organizador("o-1", "Maria", new Email("maria@email.com"),
                new SenhaHash("h"));
    }

    private Evento eventoDuasAtividades() {
        Evento evento = new Evento("e-1", "Evento", "descricao do evento", org());
        evento.adicionarAtividade(new Atividade("a-1", "Abertura",
                new Periodo(LocalDateTime.now().minusHours(4), LocalDateTime.now().minusHours(3)),
                "Sala A"));
        evento.adicionarAtividade(new Atividade("a-2", "Palestra",
                new Periodo(LocalDateTime.now().minusHours(2), LocalDateTime.now().minusHours(1)),
                "Sala B"));
        return evento;
    }

    private Inscricao inscricaoComCheckIn(String... atividadeIds) {
        Inscricao inscricao = new Inscricao("i-1", "part-1", "e-1", LocalDateTime.now());
        Evento evento = eventoDuasAtividades();
        for (String atId : atividadeIds) {
            Atividade a = evento.getAtividades().stream()
                    .filter(x -> x.getId().equals(atId)).findFirst().orElseThrow();
            inscricao.selecionarAtividade(a);
            inscricao.registrarCheckIn(a, LocalDateTime.now());
        }
        return inscricao;
    }

    private GerenciarCertificadosUseCase useCase(Evento e, Inscricao i, CriterioCertificado c) {
        EventoRepository eventos = mock(EventoRepository.class);
        InscricaoRepository inscricoes = mock(InscricaoRepository.class);
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        PessoaRepository pessoas = mock(PessoaRepository.class);
        CertificadoRepository certs = mock(CertificadoRepository.class);
        when(eventos.buscarPorId("e-1")).thenReturn(Optional.of(e));
        when(inscricoes.buscarPorId("i-1")).thenReturn(Optional.of(i));
        when(certs.buscarCriterioPorEvento("e-1")).thenReturn(Optional.ofNullable(c));
        when(certs.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        return new GerenciarCertificadosUseCase(eventos, inscricoes, usuarios, pessoas, certs,
                mock(br.com.evento.domain.port.out.PdfCertificadoService.class),
                mock(br.com.evento.domain.port.out.EmailService.class));
    }

    @Test
    void deveEmitirQuandoElegivel() {
        Evento evento = eventoDuasAtividades();
        Inscricao inscricao = inscricaoComCheckIn("a-1", "a-2");
        CriterioCertificado criterio = new CriterioCertificado("e-1");
        criterio.definirPercentualMinimo(50);

        Certificado cert = useCase(evento, inscricao, criterio)
                .emitirParaParticipante("e-1", "i-1");

        assertNotNull(cert.getCodigo());
        assertEquals("e-1", cert.getEventoId());
        assertEquals("part-1", cert.getParticipanteId());
    }

    @Test
    void deveRejeitarComJustificativaQuandoInelegivel() {
        Evento evento = eventoDuasAtividades();
        Inscricao inscricao = inscricaoComCheckIn("a-1"); // 50% de 2
        CriterioCertificado criterio = new CriterioCertificado("e-1");
        criterio.definirPercentualMinimo(100);

        DomainException ex = assertThrows(DomainException.class, () ->
                useCase(evento, inscricao, criterio).emitirParaParticipante("e-1", "i-1"));

        assertTrue(ex.getMessage().contains("50%"));
    }

    @Test
    void deveEmitirDeclaracaoParaPalestrante() {
        Evento evento = eventoDuasAtividades();
        PessoaRepository pessoas = mock(PessoaRepository.class);
        Pessoa pessoa = new Pessoa("p-1", "a-1", "Dra. Helena", "PALESTRANTE");
        when(pessoas.buscarPorId("p-1")).thenReturn(Optional.of(pessoa));

        EventoRepository eventos = mock(EventoRepository.class);
        CertificadoRepository certs = mock(CertificadoRepository.class);
        when(eventos.buscarPorId("e-1")).thenReturn(Optional.of(evento));
        when(certs.salvar(any())).thenAnswer(inv -> inv.getArgument(0));

        Certificado cert = new GerenciarCertificadosUseCase(eventos,
                mock(InscricaoRepository.class), mock(UsuarioRepository.class),
                pessoas, certs,
                mock(PdfCertificadoService.class),
                mock(EmailService.class))
                .emitirParaPalestrante("e-1", "p-1");

        assertEquals("Dra. Helena", cert.getParticipanteNome());
        assertEquals("PALESTRANTE", cert.getPapelDeclarado());
    }

    @Test
    void deveGerarPdfComDadosDoCertificado() {
        EventoRepository eventos = mock(EventoRepository.class);
        CertificadoRepository certs = mock(CertificadoRepository.class);
        PdfCertificadoService pdf = mock(PdfCertificadoService.class);
        when(eventos.buscarPorId("e-1")).thenReturn(Optional.of(eventoDuasAtividades()));
        Certificado salvo = new Certificado("c-1", "ING-1", "e-1", "part-1",
                "Joao", null, 10, LocalDateTime.now());
        when(certs.buscarPorId("c-1")).thenReturn(Optional.of(salvo));
        when(pdf.gerar(any(DadosCertificado.class))).thenReturn(new byte[]{1, 2, 3});

        byte[] bytes = new GerenciarCertificadosUseCase(eventos,
                mock(InscricaoRepository.class), mock(UsuarioRepository.class),
                mock(PessoaRepository.class), certs, pdf, mock(EmailService.class))
                .gerarPdf("c-1");

        assertArrayEquals(new byte[]{1, 2, 3}, bytes);
        verify(pdf).gerar(argThat(d -> d.nome().equals("Joao") && d.codigo().equals("ING-1")));
    }

    @Test
    void deveRegistrarEnvioDeEmailComPdfAnexo() {
        EventoRepository eventos = mock(EventoRepository.class);
        CertificadoRepository certs = mock(CertificadoRepository.class);
        PdfCertificadoService pdf = mock(PdfCertificadoService.class);
        EmailService email = mock(EmailService.class);
        when(eventos.buscarPorId("e-1")).thenReturn(Optional.of(eventoDuasAtividades()));
        Certificado salvo = new Certificado("c-1", "ING-1", "e-1", "part-1",
                "Joao", null, 10, LocalDateTime.now());
        when(certs.buscarPorId("c-1")).thenReturn(Optional.of(salvo));
        when(pdf.gerar(any(DadosCertificado.class))).thenReturn(new byte[]{9, 9});

        GerenciarCertificadosUseCase.RegistroEnvio reg =
                new GerenciarCertificadosUseCase(eventos,
                        mock(InscricaoRepository.class), mock(UsuarioRepository.class),
                        mock(PessoaRepository.class), certs, pdf, email)
                        .enviarPorEmail("c-1", "joao@email.com");

        assertTrue(reg.ok());
        assertEquals("joao@email.com", reg.para());
        verify(email).enviar(argThat((EmailMsg m) ->
                m.para().equals("joao@email.com")
                        && m.anexoNome().equals("certificado-ING-1.pdf")
                        && m.anexoPdf().length == 2));
    }

    @Test
    void deveAplicarPercentualPadraoDaOrganizacaoQuandoEventoSemCriterio() {
        Evento evento = eventoDuasAtividades();
        Inscricao inscricao = inscricaoComCheckIn("a-1"); // 50% de 2
        EventoRepository eventos = mock(EventoRepository.class);
        InscricaoRepository inscricoes = mock(InscricaoRepository.class);
        CertificadoRepository certs = mock(CertificadoRepository.class);
        when(eventos.buscarPorId("e-1")).thenReturn(Optional.of(evento));
        when(inscricoes.buscarPorId("i-1")).thenReturn(Optional.of(inscricao));
        when(certs.buscarCriterioPorEvento("e-1")).thenReturn(Optional.empty());
        br.com.evento.domain.port.out.ConfiguracaoOrganizacaoRepository cfg =
                mock(br.com.evento.domain.port.out.ConfiguracaoOrganizacaoRepository.class);
        when(cfg.buscar()).thenReturn(Optional.of(new br.com.evento.domain.model.entity.ConfiguracaoOrganizacao(
                "Centro de Eventos Sul", "CES", null, null, 75)));

        GerenciarCertificadosUseCase uc = new GerenciarCertificadosUseCase(eventos,
                inscricoes, mock(UsuarioRepository.class), mock(PessoaRepository.class),
                certs, mock(PdfCertificadoService.class), mock(EmailService.class), cfg);

        DomainException ex = assertThrows(DomainException.class, () ->
                uc.emitirParaParticipante("e-1", "i-1"));

        assertTrue(ex.getMessage().contains("75%"));
    }

    @Test
    void deveRejeitarEnvioSemSmtpConfigurado() {
        br.com.evento.adapters.out.email.SmtpEmailService semSmtp =
                new br.com.evento.adapters.out.email.SmtpEmailService("", 587, "", "", "");

        DomainException ex = assertThrows(DomainException.class, () ->
                semSmtp.enviar(new EmailMsg("a@b.com", "s", "c", "x.pdf", new byte[]{1})));

        assertTrue(ex.getMessage().contains("não configurado"));
    }
}
