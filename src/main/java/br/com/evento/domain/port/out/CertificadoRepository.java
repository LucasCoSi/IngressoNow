package br.com.evento.domain.port.out;

import br.com.evento.domain.model.entity.Certificado;
import br.com.evento.domain.model.entity.CriterioCertificado;

import java.util.List;
import java.util.Optional;

public interface CertificadoRepository {
    CriterioCertificado salvarCriterio(CriterioCertificado criterio);
    Optional<CriterioCertificado> buscarCriterioPorEvento(String eventoId);
    Certificado salvar(Certificado certificado);
    Optional<Certificado> buscarPorId(String id);
    Optional<Certificado> buscarPorCodigo(String codigo);
    List<Certificado> buscarPorEvento(String eventoId);
    List<Certificado> buscarPorParticipante(String participanteId);
}
