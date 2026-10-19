package br.com.evento.infra;

import br.com.evento.adapters.out.jdbc.JdbcAvaliacaoRepository;
import br.com.evento.adapters.out.jdbc.JdbcEventoRepository;
import br.com.evento.adapters.out.jdbc.JdbcInscricaoRepository;
import br.com.evento.adapters.out.jdbc.JdbcCertificadoRepository;
import br.com.evento.adapters.out.jdbc.JdbcPessoaRepository;
import br.com.evento.adapters.out.jdbc.JdbcQuestionarioRepository;
import br.com.evento.adapters.out.jdbc.JdbcUsuarioRepository;
import br.com.evento.adapters.out.email.SmtpEmailService;
import br.com.evento.adapters.out.pdf.PdfBoxCertificadoService;
import br.com.evento.adapters.out.security.BCryptHasher;
import br.com.evento.application.service.SimpleQrCodeService;
import br.com.evento.application.usecase.AdicionarAtividadeUseCase;
import br.com.evento.application.usecase.AutenticarUsuarioUseCase;
import br.com.evento.application.usecase.EditarUsuarioUseCase;
import br.com.evento.application.usecase.CadastrarEventoUseCase;
import br.com.evento.application.usecase.CadastrarUsuarioUseCase;
import br.com.evento.application.usecase.CancelarInscricaoUseCase;
import br.com.evento.application.usecase.ConsolidarAvaliacoesUseCase;
import br.com.evento.application.usecase.ConsultarAgendaUseCase;
import br.com.evento.application.usecase.CriarQuestionarioUseCase;
import br.com.evento.application.usecase.GerenciarCertificadosUseCase;
import br.com.evento.application.usecase.GerenciarUsuariosUseCase;
import br.com.evento.application.usecase.GerarRelatorioInscritosUseCase;
import br.com.evento.application.usecase.GerarRelatorioPresencaUseCase;
import br.com.evento.application.usecase.InscreverParticipanteUseCase;
import br.com.evento.application.usecase.ListarEventosOrganizadorUseCase;
import br.com.evento.application.usecase.ListarEventosPublicadosUseCase;
import br.com.evento.application.usecase.ListarTodosEventosUseCase;
import br.com.evento.application.usecase.RealizarCheckInManualUseCase;
import br.com.evento.application.usecase.RemarcarAtividadeUseCase;
import br.com.evento.application.usecase.VincularPessoaUseCase;
import br.com.evento.application.usecase.RealizarCheckInQrCodeUseCase;
import br.com.evento.application.usecase.SelecionarAtividadeUseCase;
import br.com.evento.application.usecase.SubmeterAvaliacaoUseCase;
import br.com.evento.domain.port.out.AvaliacaoRepository;
import br.com.evento.domain.port.out.EmailService;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.PdfCertificadoService;
import br.com.evento.domain.port.out.InscricaoRepository;
import br.com.evento.domain.port.out.CertificadoRepository;
import br.com.evento.domain.port.out.PasswordHasher;
import br.com.evento.domain.port.out.PessoaRepository;
import br.com.evento.domain.port.out.QuestionarioRepository;
import br.com.evento.domain.port.out.UsuarioRepository;

/**
 * Fiação manual da aplicação (substitui o container DI do Spring).
 * Monta repositórios JDBC + hasher + QrCode + todos os use cases.
 */
public class FabricaAplicacao {

    public final AppConfig config;
    public final ConnectionFactory connections;

    public final UsuarioRepository usuarios;
    public final EventoRepository eventos;
    public final InscricaoRepository inscricoes;
    public final QuestionarioRepository questionarios;
    public final AvaliacaoRepository avaliacoes;
    public final PessoaRepository pessoas;
    public final CertificadoRepository certificados;
    public final PasswordHasher hasher;
    public final SimpleQrCodeService qrCodeService;
    public final PdfCertificadoService pdfService;
    public final EmailService emailService;

    public final CadastrarUsuarioUseCase cadastrarUsuario;
    public final AutenticarUsuarioUseCase autenticarUsuario;
    public final EditarUsuarioUseCase atualizarUsuario;
    public final GerenciarUsuariosUseCase gerenciarUsuarios;
    public final CadastrarEventoUseCase cadastrarEvento;
    public final ListarEventosPublicadosUseCase listarEventosPublicados;
    public final ListarEventosOrganizadorUseCase listarEventosOrganizador;
    public final ListarTodosEventosUseCase listarTodosEventos;
    public final AdicionarAtividadeUseCase adicionarAtividade;
    public final RemarcarAtividadeUseCase remarcarAtividade;
    public final VincularPessoaUseCase vincularPessoa;
    public final GerenciarCertificadosUseCase gerenciarCertificados;
    public final InscreverParticipanteUseCase inscreverParticipante;
    public final CancelarInscricaoUseCase cancelarInscricao;
    public final SelecionarAtividadeUseCase selecionarAtividade;
    public final ConsultarAgendaUseCase consultarAgenda;
    public final RealizarCheckInQrCodeUseCase realizarCheckIn;
    public final RealizarCheckInManualUseCase realizarCheckInManual;
    public final GerarRelatorioPresencaUseCase relatorioPresenca;
    public final GerarRelatorioInscritosUseCase relatorioInscritos;
    public final CriarQuestionarioUseCase criarQuestionario;
    public final SubmeterAvaliacaoUseCase submeterAvaliacao;
    public final ConsolidarAvaliacoesUseCase consolidarAvaliacoes;

    public FabricaAplicacao(AppConfig config) {
        this(config, new ConnectionFactory(config.dbUrl(), config.dbUser(), config.dbPass()));
    }

    /** Reusa a mesma {@link ConnectionFactory} do startup (evita duplicar config/pool). */
    public FabricaAplicacao(AppConfig config, ConnectionFactory connections) {
        this.config = config;
        this.connections = connections;

        this.usuarios = new JdbcUsuarioRepository(connections);
        this.eventos = new JdbcEventoRepository(connections);
        this.inscricoes = new JdbcInscricaoRepository(connections);
        this.questionarios = new JdbcQuestionarioRepository(connections);
        this.avaliacoes = new JdbcAvaliacaoRepository(connections);
        this.pessoas = new JdbcPessoaRepository(connections);
        this.certificados = new JdbcCertificadoRepository(connections);
        this.hasher = new BCryptHasher();
        this.qrCodeService = new SimpleQrCodeService();
        this.pdfService = new PdfBoxCertificadoService();
        this.emailService = new SmtpEmailService(config.smtpHost(), config.smtpPort(),
                config.smtpUser(), config.smtpPass(), config.smtpFrom());

        this.cadastrarUsuario = new CadastrarUsuarioUseCase(usuarios, hasher);
        this.autenticarUsuario = new AutenticarUsuarioUseCase(usuarios, hasher);
        this.atualizarUsuario = new EditarUsuarioUseCase(usuarios, hasher);
        this.gerenciarUsuarios = new GerenciarUsuariosUseCase(usuarios, eventos, inscricoes);
        this.cadastrarEvento = new CadastrarEventoUseCase(eventos, usuarios);
        this.listarEventosPublicados = new ListarEventosPublicadosUseCase(eventos);
        this.listarEventosOrganizador = new ListarEventosOrganizadorUseCase(eventos);
        this.listarTodosEventos = new ListarTodosEventosUseCase(eventos);
        this.adicionarAtividade = new AdicionarAtividadeUseCase(eventos);
        this.remarcarAtividade = new RemarcarAtividadeUseCase(eventos);
        this.vincularPessoa = new VincularPessoaUseCase(eventos, pessoas);
        this.gerenciarCertificados = new GerenciarCertificadosUseCase(
                eventos, inscricoes, usuarios, pessoas, certificados, pdfService, emailService);
        this.inscreverParticipante = new InscreverParticipanteUseCase(eventos, usuarios, inscricoes);
        this.cancelarInscricao = new CancelarInscricaoUseCase(inscricoes, eventos);
        this.selecionarAtividade = new SelecionarAtividadeUseCase(inscricoes, eventos);
        this.consultarAgenda = new ConsultarAgendaUseCase(inscricoes);
        this.realizarCheckIn = new RealizarCheckInQrCodeUseCase(qrCodeService, inscricoes, eventos);
        this.realizarCheckInManual = new RealizarCheckInManualUseCase(inscricoes, eventos, usuarios);
        this.relatorioPresenca = new GerarRelatorioPresencaUseCase(eventos, inscricoes);
        this.relatorioInscritos = new GerarRelatorioInscritosUseCase(eventos, inscricoes, usuarios);
        this.criarQuestionario = new CriarQuestionarioUseCase(eventos, questionarios);
        this.submeterAvaliacao = new SubmeterAvaliacaoUseCase(inscricoes, questionarios, avaliacoes);
        this.consolidarAvaliacoes = new ConsolidarAvaliacoesUseCase(questionarios, avaliacoes);
    }
}
