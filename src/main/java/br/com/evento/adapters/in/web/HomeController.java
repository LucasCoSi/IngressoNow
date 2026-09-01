package br.com.evento.adapters.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "forward:/System Desgin v2__19-08-26/ingressonow-login.html";
    }

    @GetMapping("/cadastro")
    public String cadastro() {
        return "forward:/System Desgin v2__19-08-26/ingressonow-cadastro.html";
    }

    @GetMapping("/eventos")
    public String eventos() {
        return "forward:/System Desgin v2__19-08-26/ingressonow-eventos.html";
    }

    @GetMapping("/evento-publico")
    public String eventoPublico() {
        return "forward:/System Desgin v2__19-08-26/ingressonow-evento-publico.html";
    }

    @GetMapping("/atividades")
    public String atividades() {
        return "forward:/System Desgin v2__19-08-26/ingressonow-atividades.html";
    }

    @GetMapping("/inscricoes")
    public String inscricoes() {
        return "forward:/System Desgin v2__19-08-26/ingressonow-inscricoes.html";
    }

    @GetMapping("/area-participante")
    public String areaParticipante() {
        return "forward:/System Desgin v2__19-08-26/ingressonow-area-participante.html";
    }

    @GetMapping("/frequencia")
    public String frequencia() {
        return "forward:/System Desgin v2__19-08-26/ingressonow-frequencia.html";
    }

    @GetMapping("/programacao")
    public String programacao() {
        return "forward:/System Desgin v2__19-08-26/ingressonow-programacao.html";
    }

    @GetMapping("/avaliacoes")
    public String avaliacoes() {
        return "forward:/System Desgin v2__19-08-26/ingressonow-avaliacoes.html";
    }

    @GetMapping("/relatorios")
    public String relatorios() {
        return "forward:/System Desgin v2__19-08-26/ingressonow-relatorios.html";
    }

    @GetMapping("/usuarios")
    public String usuarios() {
        return "forward:/System Desgin v2__19-08-26/ingressonow-usuarios.html";
    }

    @GetMapping("/configuracoes")
    public String configuracoes() {
        return "forward:/System Desgin v2__19-08-26/ingressonow-configuracoes.html";
    }

    @GetMapping("/interface")
    public String interface_() {
        return "forward:/System Desgin v2__19-08-26/ingressonow-interface.html";
    }
}
