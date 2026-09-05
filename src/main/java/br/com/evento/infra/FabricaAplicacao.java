package br.com.evento.infra;

import br.com.evento.adapters.out.jdbc.JdbcAvaliacaoRepository;
import br.com.evento.adapters.out.jdbc.JdbcEventoRepository;
import br.com.evento.adapters.out.jdbc.JdbcInscricaoRepository;
import br.com.evento.adapters.out.jdbc.JdbcQuestionarioRepository;
import br.com.evento.adapters.out.jdbc.JdbcUsuarioRepository;
import br.com.evento.adapters.out.security.BCryptHasher;
import br.com.evento.application.service.SimpleQrCodeService;
import br.com.evento.application.usecase.AdicionarAtividadeUseCase;
import br.com.evento.application.usecase.AutenticarUsuarioUseCase;
import br.com.evento.application.usecase.CadastrarEventoUseCase;
import br.com.evento.application.usecase.CadastrarUsuarioUseCase;
import br.com.evento.application.usecase.CancelarInscricaoUseCase;
import br.com.evento.application.usecase.ConsolidarAvaliacoesUseCase;
import br.com.evento.application.usecase.ConsultarAgendaUseCase;
import br.com.evento.application.usecase.CriarQuestionarioUseCase;
import br.com.evento.application.usecase.GerarRelatorioPresencaUseCase;
import br.com.evento.application.usecase.InscreverParticipanteUseCase;
import br.com.evento.application.usecase.ListarEventosPublicadosUseCase;
import br.com.evento.application.usecase.RealizarCheckInQrCodeUseCase;
import br.com.evento.application.usecase.SelecionarAtividadeUseCase;
import br.com.evento.application.usecase.SubmeterAvaliacaoUseCase;
import br.com.evento.domain.port.out.AvaliacaoRepository;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import br.com.evento.domain.port.out.PasswordHasher;
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
    public final PasswordHasher hasher;
    public final SimpleQrCodeService qrCodeService;

    public final CadastrarUsuarioUseCase cadastrarUsuario;
    public final AutenticarUsuarioUseCase autenticarUsuario;
    public final CadastrarEventoUseCase cadastrarEvento;
    public final ListarEventosPublicadosUseCase listarEventosPublicados;
    public final AdicionarAtividadeUseCase adicionarAtividade;
    public final InscreverParticipanteUseCase inscreverParticipante;
    public final CancelarInscricaoUseCase cancelarInscricao;
    public final SelecionarAtividadeUseCase selecionarAtividade;
    public final ConsultarAgendaUseCase consultarAgenda;
    public final RealizarCheckInQrCodeUseCase realizarCheckIn;
    public final GerarRelatorioPresencaUseCase relatorioPresenca;
    public final CriarQuestionarioUseCase criarQuestionario;
    public final SubmeterAvaliacaoUseCase submeterAvaliacao;
    public final ConsolidarAvaliacoesUseCase consolidarAvaliacoes;

    public FabricaAplicacao(AppConfig config) {
        this.config = config;
        this.connections = new ConnectionFactory(config.dbUrl(), config.dbUser(), config.dbPass());

        this.usuarios = new JdbcUsuarioRepository(connections);
        this.eventos = new JdbcEventoRepository(connections);
        this.inscricoes = new JdbcInscricaoRepository(connections);
        this.questionarios = new JdbcQuestionarioRepository(connections);
        this.avaliacoes = new JdbcAvaliacaoRepository(connections);
        this.hasher = new BCryptHasher();
        this.qrCodeService = new SimpleQrCodeService();

        this.cadastrarUsuario = new CadastrarUsuarioUseCase(usuarios, hasher);
        this.autenticarUsuario = new AutenticarUsuarioUseCase(usuarios, hasher);
        this.cadastrarEvento = new CadastrarEventoUseCase(eventos, usuarios);
        this.listarEventosPublicados = new ListarEventosPublicadosUseCase(eventos);
        this.adicionarAtividade = new AdicionarAtividadeUseCase(eventos);
        this.inscreverParticipante = new InscreverParticipanteUseCase(eventos, usuarios, inscricoes);
        this.cancelarInscricao = new CancelarInscricaoUseCase(inscricoes, eventos);
        this.selecionarAtividade = new SelecionarAtividadeUseCase(inscricoes, eventos);
        this.consultarAgenda = new ConsultarAgendaUseCase(inscricoes);
        this.realizarCheckIn = new RealizarCheckInQrCodeUseCase(qrCodeService, inscricoes, eventos);
        this.relatorioPresenca = new GerarRelatorioPresencaUseCase(eventos, inscricoes);
        this.criarQuestionario = new CriarQuestionarioUseCase(eventos, questionarios);
        this.submeterAvaliacao = new SubmeterAvaliacaoUseCase(inscricoes, questionarios, avaliacoes);
        this.consolidarAvaliacoes = new ConsolidarAvaliacoesUseCase(questionarios, avaliacoes);
    }
}
