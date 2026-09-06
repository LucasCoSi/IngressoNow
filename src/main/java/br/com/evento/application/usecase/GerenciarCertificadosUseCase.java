package br.com.evento.application.usecase;

import br.com.evento.domain.exception.DomainException;
import br.com.evento.domain.model.entity.Certificado;
import br.com.evento.domain.model.entity.CriterioCertificado;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.model.entity.Pessoa;
import br.com.evento.domain.port.out.CertificadoRepository;
import br.com.evento.domain.port.out.EventoRepository;
import br.com.evento.domain.port.out.InscricaoRepository;
import br.com.evento.domain.port.out.PessoaRepository;
import br.com.evento.domain.port.out.UsuarioRepository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * RF-32–35 (D) + CA-08 — certificados de participante e de palestrante.
 * Elegibilidade calculável (RN-16); inelegível recebe justificativa.
 * RF-34 (envio por e-mail): registrado como pendência declarada —
 * o certificado é baixável (texto verificável); sem SMTP no MVP.
 */
public class GerenciarCertificadosUseCase {

    private final EventoRepository eventoRepository;
    private final InscricaoRepository inscricaoRepository;
    private final UsuarioRepository usuarioRepository;
    private final PessoaRepository pessoaRepository;
    private final CertificadoRepository certificadoRepository;

    public GerenciarCertificadosUseCase(EventoRepository eventoRepository,
                                        InscricaoRepository inscricaoRepository,
                                        UsuarioRepository usuarioRepository,
                                        PessoaRepository pessoaRepository,
                                        CertificadoRepository certificadoRepository) {
        this.eventoRepository = eventoRepository;
        this.inscricaoRepository = inscricaoRepository;
        this.usuarioRepository = usuarioRepository;
        this.pessoaRepository = pessoaRepository;
        this.certificadoRepository = certificadoRepository;
    }

    public CriterioCertificado definirCriterio(String eventoId, Integer percentualMinimo,
                                               List<String> obrigatorias) {
        eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new DomainException("Evento não encontrado."));
        CriterioCertificado atual = certificadoRepository.buscarCriterioPorEvento(eventoId)
                .orElse(new CriterioCertificado(eventoId));
        atual.definirPercentualMinimo(percentualMinimo);
        if (obrigatorias != null) {
            for (String atId : obrigatorias) atual.exigirAtividade(atId);
        }
        return certificadoRepository.salvarCriterio(atual);
    }

    /** CA-08: emite ou lança DomainException com a justificativa. */
    public Certificado emitirParaParticipante(String eventoId, String inscricaoId) {
        Evento evento = eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new DomainException("Evento não encontrado."));
        Inscricao inscricao = inscricaoRepository.buscarPorId(inscricaoId)
                .orElseThrow(() -> new DomainException("Inscrição não encontrada."));
        if (!inscricao.getEventoId().equals(eventoId)) {
            throw new DomainException("Inscrição não pertence a este evento.");
        }
        CriterioCertificado criterio = certificadoRepository.buscarCriterioPorEvento(eventoId)
                .orElse(new CriterioCertificado(eventoId));
        Optional<String> motivo = criterio.verificar(inscricao, evento);
        if (motivo.isPresent()) {
            throw new DomainException("Participante inelegível: " + motivo.get());
        }
        String nome = usuarioRepository.buscarPorId(inscricao.getParticipanteId())
                .map(u -> u.getNome()).orElse(inscricao.getParticipanteId());
        int carga = (int) evento.getAtividades().stream()
                .mapToLong(a -> Duration.between(
                        a.getPeriodo().getInicio(), a.getPeriodo().getFim()).toHours())
                .sum();
        Certificado cert = new Certificado(null, gerarCodigo(eventoId, inscricaoId),
                eventoId, inscricao.getParticipanteId(), nome, null, carga, null);
        return certificadoRepository.salvar(cert);
    }

    /** RF-35: declaração para palestrante/apresentador (papel exercido). */
    public Certificado emitirParaPalestrante(String eventoId, String pessoaId) {
        Evento evento = eventoRepository.buscarPorId(eventoId)
                .orElseThrow(() -> new DomainException("Evento não encontrado."));
        Pessoa pessoa = pessoaRepository.buscarPorId(pessoaId)
                .orElseThrow(() -> new DomainException("Pessoa não encontrada."));
        boolean doEvento = evento.getAtividades().stream()
                .anyMatch(a -> a.getId().equals(pessoa.getAtividadeId()));
        if (!doEvento) {
            throw new DomainException("Pessoa não vinculada a este evento.");
        }
        Certificado cert = new Certificado(null, gerarCodigo(eventoId, pessoaId),
                eventoId, pessoaId, pessoa.getNome(), pessoa.getPapel(), 0, null);
        return certificadoRepository.salvar(cert);
    }

    public List<Certificado> listarPorEvento(String eventoId) {
        return certificadoRepository.buscarPorEvento(eventoId);
    }

    public List<Certificado> listarPorParticipante(String participanteId) {
        return certificadoRepository.buscarPorParticipante(participanteId);
    }

    private static String gerarCodigo(String eventoId, String sujeitoId) {
        String base = (eventoId + ":" + sujeitoId + ":" + System.nanoTime()).hashCode() + "";
        return "ING-" + base.replace("-", "N") + "-"
                + Integer.toHexString(sujeitoId.hashCode()).toUpperCase();
    }
}
