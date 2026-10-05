package br.com.evento.adapters.in.desktop;

import br.com.evento.domain.model.entity.Atividade;
import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.model.entity.Organizador;
import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.domain.model.vo.Periodo;
import br.com.evento.infra.FabricaAplicacao;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * D-01 — desktop JavaFX (código puro, sem FXML).
 * Mesmo banco + mesmos use cases da API via FabricaAplicacao (RN-19/RNF-02).
 * DSK-00: login + cadastro + menu por papel (sem login não há gestão).
 * DSK-10: gestão do organizador (CRUD + publicar/encerrar, RF-04/RN-04/RN-20).
 */
public class DesktopApp extends Application {

    private static FabricaAplicacao app;

    private Usuario logado;
    private Stage palco;

    public static void conectar(FabricaAplicacao fabrica) {
        app = fabrica;
    }

    /**
     * Cria cena já com o tema IngressoNow (D-01: CSS externo em
     * {@code /desktop/ingressonow.css}, sem FXML — JavaFX puro).
     */
    private static Scene cena(Parent raiz, double largura, double altura) {
        Scene cena = new Scene(raiz, largura, altura);
        var css = DesktopApp.class.getResource("/desktop/ingressonow.css");
        if (css != null) {
            cena.getStylesheets().add(css.toExternalForm());
        }
        return cena;
    }

    private static Button primario(String texto) {
        var b = new Button(texto);
        b.getStyleClass().add("btn-primary");
        return b;
    }

    private static Label titulo(String texto) {
        var l = new Label(texto);
        l.getStyleClass().add("page-title");
        return l;
    }

    private static Label subtitulo(String texto) {
        var l = new Label(texto);
        l.getStyleClass().add("subtitle");
        l.setWrapText(true);
        return l;
    }

    @Override
    public void start(Stage palco) {
        this.palco = palco;
        palco.setTitle("IngressoNow — Desktop");
        mostrarLogin();
        palco.show();
    }

    // ---------- DSK-00: acesso ----------

    private void mostrarLogin() {
        logado = null;
        shell = null;

        // Faixa de marca (identidade do site: navy-dark + "IN").
        var marca = new Label("IN");
        marca.getStyleClass().add("brand-mark");
        var nomeMarca = new HBox(0);
        var ingresso = new Label("Ingresso");
        ingresso.getStyleClass().add("brand-title");
        var now = new Label("Now");
        now.getStyleClass().add("brand-now");
        nomeMarca.getChildren().addAll(ingresso, now);
        var slogan = new Label("Gestão de eventos acadêmicos\n"
                + "Inscrições • frequência • certificados");
        slogan.getStyleClass().add("brand-sub");
        slogan.setWrapText(true);
        var demo = new Label("Demo — organizadora: maria.santos@email.com\n"
                + "participante: lucas.silva@email.com (senha123)");
        demo.getStyleClass().add("brand-sub");
        demo.setWrapText(true);
        demo.setStyle("-fx-font-size: 11px; -fx-opacity: 0.85;");
        var marcaBox = new VBox(12, marca, nomeMarca, slogan, demo);
        marcaBox.getStyleClass().add("brand-panel");
        marcaBox.setPrefWidth(300);

        // Cartão de acesso.
        var email = new TextField();
        email.setPromptText("voce@email.com");
        var senha = new PasswordField();
        senha.setPromptText("sua senha");
        var entrar = primario("Entrar");
        entrar.setDefaultButton(true);
        entrar.setMaxWidth(Double.MAX_VALUE);
        var irCadastro = new Button("Criar conta");
        irCadastro.setMaxWidth(Double.MAX_VALUE);
        var msg = new Label();
        msg.getStyleClass().add("msg-erro");
        msg.setWrapText(true);
        entrar.setOnAction(e -> {
            try {
                logado = app.autenticarUsuario.executar(email.getText(), senha.getText());
                mostrarPrincipal();
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        });
        irCadastro.setOnAction(e -> mostrarCadastro());
        senha.setOnAction(e -> entrar.fire());
        var form = new VBox(10,
                titulo("Entrar"),
                subtitulo("Acesse com seu e-mail e senha."),
                new Label("E-mail:"), email,
                new Label("Senha:"), senha,
                entrar, irCadastro, msg);
        form.setPadding(new Insets(36, 36, 36, 36));
        form.setFillWidth(true);
        HBox.setHgrow(form, javafx.scene.layout.Priority.ALWAYS);

        var raiz = new HBox(marcaBox, form);
        palco.setScene(cena(raiz, 760, 470));
    }

    private void mostrarCadastro() {
        shell = null;
        var nome = new TextField();
        nome.setPromptText("nome");
        var email = new TextField();
        email.setPromptText("e-mail");
        var senha = new PasswordField();
        senha.setPromptText("senha (mín. conforme política)");
        var tipo = new ChoiceBox<>(FXCollections.observableArrayList("PARTICIPANTE", "ORGANIZADOR"));
        tipo.setValue("PARTICIPANTE");
        var salvar = new Button("Criar conta");
        var voltar = new Button("Voltar");
        var msg = new Label();
        salvar.setOnAction(e -> {
            try {
                logado = app.cadastrarUsuario.executar(
                        nome.getText(), email.getText(), senha.getText(), tipo.getValue());
                mostrarPrincipal();
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        });
        voltar.setOnAction(e -> mostrarLogin());
        var grade = new GridPane();
        grade.setPadding(new Insets(24));
        grade.setVgap(8);
        grade.setHgap(8);
        grade.add(new Label("Nome:"), 0, 0);
        grade.add(nome, 1, 0);
        grade.add(new Label("E-mail:"), 0, 1);
        grade.add(email, 1, 1);
        grade.add(new Label("Senha:"), 0, 2);
        grade.add(senha, 1, 2);
        grade.add(new Label("Tipo:"), 0, 3);
        grade.add(tipo, 1, 3);
        grade.add(salvar, 1, 4);
        grade.add(voltar, 1, 5);
        grade.add(msg, 1, 6);
        palco.setScene(cena(grade, 420, 340));
    }

    // ---------- shell persistente (sidebar + topbar fixas, só o centro troca) ----------

    private BorderPane shell;
    private Button navInicio, navGestao, navNovo, navProg, navInsc, navExplorar;

    private void mostrarPrincipal() {
        if (logado == null) {
            mostrarLogin();
            return;
        }
        montarShell();
        navegar(conteudoInicio(), navInicio);
    }

    private static Button navBotao(String texto) {
        var b = new Button(texto);
        b.getStyleClass().add("nav-btn");
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private void montarShell() {
        boolean org = isOrganizador();
        navInicio = navGestao = navNovo = navProg = navInsc = navExplorar = null;

        // Sidebar navy (espelha o site: marca + navegação + sair).
        var marca = new Label("IN");
        marca.getStyleClass().add("brand-mark");
        var nomeMarca = new HBox(0);
        var ingresso = new Label("Ingresso");
        ingresso.getStyleClass().addAll("brand-title");
        ingresso.setStyle("-fx-font-size: 17px;");
        var now = new Label("Now");
        now.getStyleClass().add("brand-now");
        now.setStyle("-fx-font-size: 17px;");
        nomeMarca.getChildren().addAll(ingresso, now);
        var lado = new VBox(6, marca, nomeMarca);
        lado.getStyleClass().add("sidebar");
        lado.setPadding(new Insets(20, 14, 20, 14));
        lado.setPrefWidth(230);

        var nav = new VBox(4);
        navInicio = navBotao("Início");
        navInicio.setOnAction(e -> navegar(conteudoInicio(), navInicio));
        nav.getChildren().add(navInicio);
        if (org) {
            navGestao = navBotao("Meus eventos");
            navGestao.setOnAction(e -> mostrarGestao());
            navNovo = navBotao("Novo evento");
            navNovo.setOnAction(e -> dialogoEvento(null));
            navProg = navBotao("Programação");
            navProg.setOnAction(e -> dialogoProgramacao());
            navInsc = navBotao("Inscrições");
            navInsc.setOnAction(e -> dialogoInscricoes());
            nav.getChildren().addAll(navGestao, navNovo, navProg, navInsc);
        }
        navExplorar = navBotao("Explorar publicados");
        navExplorar.setOnAction(e -> mostrarExplorar());
        var sair = navBotao("Sair (" + logado.getNome() + ")");
        sair.setOnAction(e -> {
            if (confirmar(palco, "Sair da conta de " + logado.getNome() + "?")) mostrarLogin();
        });
        nav.getChildren().addAll(navExplorar, sair);
        lado.getChildren().add(nav);

        // Barra superior branca: identidade de quem está operando (sem CTAs
        // duplicados — as ações ficam na sidebar).
        var topo = new HBox(10);
        topo.getStyleClass().add("topbar");
        topo.setPadding(new Insets(10, 16, 10, 16));
        topo.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        var quem = new Label(logado.getNome() + " • " + papel());
        quem.getStyleClass().add("subtitle");
        var selo = new Label(org ? "ORGANIZADOR" : "PARTICIPANTE");
        selo.getStyleClass().add("badge");
        topo.getChildren().addAll(quem, selo);

        shell = new BorderPane(null, topo, null, null, lado);
        palco.setScene(cena(shell, 1024, 640));
    }

    /** Troca só o centro do shell (sidebar/topbar permanecem). */
    private void navegar(Parent conteudo, Button ativo) {
        if (logado == null) {
            mostrarLogin();
            return;
        }
        if (shell == null) montarShell();
        var rol = new ScrollPane(conteudo);
        rol.setFitToWidth(true);
        rol.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rol.setPannable(true);
        shell.setCenter(rol);
        for (var b : new Button[]{navInicio, navGestao, navNovo, navProg, navInsc, navExplorar}) {
            if (b != null) b.getStyleClass().remove("active");
        }
        if (ativo != null && !ativo.getStyleClass().contains("active")) {
            ativo.getStyleClass().add("active");
        }
    }

    private VBox conteudoInicio() {
        boolean org = isOrganizador();
        // Conteúdo: boas-vindas + resumo.
        var papel = new Label(org ? "ORGANIZADOR" : "PARTICIPANTE");
        papel.getStyleClass().add("badge");
        var boas = titulo("Olá, " + logado.getNome());
        String resumo = "Operador IngressoNow — mesmo banco e mesmas regras da API/site (RN-19).";
        if (org) {
            try {
                int meus = app.listarEventosOrganizador.executar(logado.getId()).size();
                int publ = app.listarEventosPublicados.executar().size();
                resumo = "Você tem " + meus + " evento(s) • " + publ + " publicado(s) no catálogo.";
            } catch (Exception ignored) {
                // mantém texto padrão
            }
        } else {
            try {
                int publ = app.listarEventosPublicados.executar().size();
                resumo = publ + " evento(s) publicado(s) — explore e inscreva-se.";
            } catch (Exception ignored) {
                resumo = "Explore os publicados e inscreva-se.";
            }
        }
        var centro = new VBox(12, papel, boas, subtitulo(resumo),
                subtitulo(org
                        ? "Gestão isolada por dono (RN-18): só os próprios eventos."
                        : "Visão do participante: explorar e inscrever-se."));
        centro.setPadding(new Insets(32));
        return centro;
    }

    // ---------- DSK-10: gestão do organizador ----------

    private void mostrarGestao() {
        if (!exigirOrganizador()) return;
        List<Evento> meus;
        try {
            meus = app.listarEventosOrganizador.executar(logado.getId());
        } catch (Exception ex) {
            return;
        }
        var lista = new ListView<>(FXCollections.observableArrayList(meus));
        estilizarListaEventos(lista);
        var abrir = primario("Abrir");
        abrir.setOnAction(e -> {
            Evento sel = lista.getSelectionModel().getSelectedItem();
            if (sel != null) mostrarDetalhe(sel.getId(), true);
        });
        var novo = new Button("Novo");
        novo.setOnAction(e -> dialogoEvento(null));
        var editar = new Button("Editar");
        editar.setOnAction(e -> {
            Evento sel = lista.getSelectionModel().getSelectedItem();
            if (sel != null) dialogoEvento(sel.getId());
        });
        var excluir = new Button("Excluir");
        excluir.getStyleClass().add("btn-danger");
        excluir.setOnAction(e -> {
            Evento sel = lista.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            if (!confirmar(palco,
                    "Excluir \"" + sel.getTitulo() + "\"? (bloqueado se houver inscrições)")) return;
            try {
                if (app.inscricoes.contarPorEvento(sel.getId()) > 0) {
                    throw new IllegalStateException(
                            "Evento possui inscrições e não pode ser excluído. Encerre em vez disso.");
                }
                app.eventos.remover(sel.getId());
                mostrarGestao();
            } catch (Exception ex) {
                alerta("Falha: " + mensagem(ex));
            }
        });
        var publicar = new Button("Publicar");
        publicar.setOnAction(e -> acaoEstado(lista, "publicar"));
        var encerrar = new Button("Encerrar");
        encerrar.setOnAction(e -> acaoEstado(lista, "encerrar"));
        var voltar = new Button("Voltar");
        voltar.setOnAction(e -> mostrarPrincipal());
        var seloQtd = new Label(meus.size() + " evento(s)");
        seloQtd.getStyleClass().add("badge");
        var cabec = new HBox(10, titulo("Meus eventos"), seloQtd);
        cabec.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        var vazio = subtitulo(meus.isEmpty()
                ? "Nenhum evento ainda — clique em Novo para criar um RASCUNHO."
                : "Selecione um evento para abrir, editar, publicar ou encerrar.");
        var barra = new HBox(8, abrir, novo, editar, excluir, publicar, encerrar);
        barra.setPadding(new Insets(12, 0, 0, 0));
        var voltarBox = new HBox(8, voltar);
        voltarBox.setPadding(new Insets(12, 0, 0, 0));
        var centro = new VBox(8, cabec, vazio, lista, barra, voltarBox);
        centro.setPadding(new Insets(16));
        navegar(centro, navGestao);
    }

    private void acaoEstado(ListView<Evento> lista, String acao) {
        Evento sel = lista.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            Evento evento = app.eventos.buscarPorId(sel.getId()).orElseThrow();
            exigirDono(evento);
            if ("publicar".equals(acao)) evento.publicar();
            else evento.encerrar();
            app.eventos.salvar(evento);
            mostrarGestao();
        } catch (Exception ex) {
            alerta("Falha: " + mensagem(ex));
        }
    }

    private void dialogoEvento(String eventoId) {
        if (!exigirOrganizador()) return;
        Evento atual = null;
        if (eventoId != null) {
            atual = app.eventos.buscarPorId(eventoId).orElse(null);
            if (atual == null) {
                alerta("Evento não encontrado.");
                return;
            }
            try {
                exigirDono(atual);
            } catch (Exception ex) {
                alerta("Falha: " + mensagem(ex));
                return;
            }
        }
        var titulo = new TextField(atual != null ? atual.getTitulo() : "");
        titulo.setPromptText("título (mín. 3)");
        var descricao = new TextArea(atual != null ? atual.getDescricao() : "");
        descricao.setPromptText("descrição (mín. 10)");
        descricao.setPrefRowCount(3);
        var local = new TextField(atual != null && atual.getLocal() != null ? atual.getLocal() : "");
        local.setPromptText("local / modalidade");
        var inicio = new SeletorDataHora(atual != null && atual.getPeriodo() != null
                ? atual.getPeriodo().getInicio() : null);
        var fim = new SeletorDataHora(atual != null && atual.getPeriodo() != null
                ? atual.getPeriodo().getFim() : null);
        var fuso = new TextField(atual != null && atual.getFuso() != null ? atual.getFuso() : "UTC");
        fuso.setPromptText("IANA ex: America/Sao_Paulo");
        var vagas = new TextField(atual != null && atual.getMaxVagas() != null
                ? String.valueOf(atual.getMaxVagas()) : "");
        vagas.setPromptText("vagas (vazio = sem limite)");
        var exige = new CheckBox("Exige escolha de atividades");
        exige.setSelected(atual != null && atual.isExigeEscolhaAtividades());
        var porAtv = new CheckBox("Vagas por atividade");
        porAtv.setSelected(atual != null && atual.isVagasPorAtividade());
        var msg = new Label();
        msg.getStyleClass().add("msg-erro");
        msg.setWrapText(true);
        var salvar = primario(atual != null ? "Salvar" : "Criar RASCUNHO");
        var voltar = new Button("Voltar");
        Evento editando = atual;
        salvar.setOnAction(e -> {
            try {
                validarFormulario(titulo.getText(), descricao.getText(), vagas.getText(),
                        inicio.obter(), fim.obter(), fuso.getText());
                Periodo periodo = montarPeriodo(inicio.obter(), fim.obter());
                Integer maxVagas = montarVagas(vagas.getText());
                if (editando == null) {
                    Evento criado = app.cadastrarEvento.executar(
                            titulo.getText().trim(), descricao.getText().trim(), logado.getId());
                    aplicarDetalhes(criado, periodo, local.getText(), fuso.getText(),
                            maxVagas, exige.isSelected(), porAtv.isSelected());
                    app.eventos.salvar(criado);
                } else {
                    Evento evento = app.eventos.buscarPorId(editando.getId()).orElseThrow();
                    exigirDono(evento);
                    evento.atualizar(titulo.getText().trim(), descricao.getText().trim());
                    aplicarDetalhes(evento, periodo, local.getText(), fuso.getText(),
                            maxVagas, exige.isSelected(), porAtv.isSelected());
                    app.eventos.salvar(evento);
                }
                mostrarGestao();
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        });
        voltar.setOnAction(e -> mostrarGestao());
        var grade = new GridPane();
        grade.setVgap(8);
        grade.setHgap(8);
        grade.add(new Label("Título:"), 0, 0);
        grade.add(titulo, 1, 0);
        grade.add(new Label("Descrição:"), 0, 1);
        grade.add(descricao, 1, 1);
        grade.add(new Label("Local:"), 0, 2);
        grade.add(local, 1, 2);
        grade.add(new Label("Início:"), 0, 3);
        grade.add(inicio.caixa(), 1, 3);
        grade.add(new Label("Fim:"), 0, 4);
        grade.add(fim.caixa(), 1, 4);
        grade.add(new Label("Fuso:"), 0, 5);
        grade.add(fuso, 1, 5);
        grade.add(new Label("Vagas:"), 0, 6);
        grade.add(vagas, 1, 6);
        grade.add(exige, 1, 7);
        grade.add(porAtv, 1, 8);
        grade.add(salvar, 1, 9);
        grade.add(voltar, 1, 10);
        grade.add(msg, 1, 11);
        boolean editandoModo = editando != null;
        var corpo = new VBox(12,
                titulo(editandoModo ? "Editar evento" : "Novo evento"),
                subtitulo(editandoModo
                        ? "Atualize os dados e salve."
                        : "Criado como RASCUNHO — publique após adicionar atividades."),
                grade);
        corpo.setPadding(new Insets(20));
        navegar(corpo, editandoModo ? navGestao : navNovo);
    }

    // ---------- Explorar + detalhe (todos os papéis) ----------

    private void mostrarExplorar() {
        if (logado == null) {
            mostrarLogin();
            return;
        }
        var lista = new ListView<>(FXCollections.observableArrayList(
                app.listarEventosPublicados.executar()));
        estilizarListaEventos(lista);
        var abrir = primario("Abrir");
        abrir.setOnAction(e -> {
            Evento sel = lista.getSelectionModel().getSelectedItem();
            if (sel != null) mostrarDetalhe(sel.getId(), false);
        });
        var voltar = new Button("Voltar");
        voltar.setOnAction(e -> mostrarPrincipal());
        var seloQtd = new Label(lista.getItems().size() + " publicado(s)");
        seloQtd.getStyleClass().add("badge");
        var cabec = new HBox(10, titulo("Eventos publicados"), seloQtd);
        cabec.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        var dica = subtitulo("Catálogo público — o mesmo do site (RF-10).");
        var barra = new HBox(8, abrir, voltar);
        barra.setPadding(new Insets(12, 0, 0, 0));
        var centro = new VBox(8, cabec, dica, lista, barra);
        centro.setPadding(new Insets(16));
        navegar(centro, navExplorar);
    }

    private void mostrarDetalhe(String eventoId, boolean veioDaGestao) {
        Evento evento = app.eventos.buscarPorId(eventoId).orElseThrow();
        boolean souDono = logado instanceof Organizador
                && evento.getOrganizador() != null
                && logado.getId().equals(evento.getOrganizador().getId());
        var linhas = new VBox(4);
        linhas.setPadding(new Insets(12));
        linhas.getChildren().add(new Label(
                evento.getTitulo() + " [" + evento.getStatus() + "] — "
                        + evento.getAtividades().size() + " atividade(s)"));
        String donoDet = evento.getOrganizador() != null
                ? evento.getOrganizador().getNome() + " (" + evento.getOrganizador().getEmail() + ")"
                : "—";
        linhas.getChildren().add(new Label("Organizador: " + donoDet));

        // DSK-11/12: grade de atividades com elenco (dono gerencia, demais só consultam).
        var listaAtv = new ListView<>(FXCollections.observableArrayList(evento.getAtividades()));
        listaAtv.setPrefHeight(220);
        listaAtv.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Atividade a, boolean vazio) {
                super.updateItem(a, vazio);
                if (vazio || a == null) {
                    setText(null);
                    return;
                }
                var pessoas = app.pessoas.buscarPorAtividade(a.getId());
                String elenco = pessoas.isEmpty() ? "—"
                        : pessoas.stream()
                                .map(p -> p.getNome() + " (" + p.getPapel() + ")")
                                .reduce((x, y) -> x + ", " + y).orElse("—");
                setText("• " + a.getTitulo()
                        + " | " + a.getPeriodo().getInicio() + "–" + a.getPeriodo().getFim()
                        + " | " + a.getLocal()
                        + (a.getTipo() != null ? " | " + a.getTipo() : "")
                        + (a.getTrilha() != null ? " [" + a.getTrilha() + "]" : "")
                        + " | freq:" + a.getNomePoliticaFrequencia()
                        + (a.getMaxVagas() != null ? " | vagas:" + a.getMaxVagas() : "")
                        + "\n    " + elenco);
            }
        });
        linhas.getChildren().add(listaAtv);

        if (souDono) {
            var nova = new Button("Nova atividade");
            nova.setOnAction(e -> dialogoAtividade(eventoId, null, veioDaGestao));
            var remarcar = new Button("Remarcar");
            remarcar.setOnAction(e -> {
                Atividade sel = listaAtv.getSelectionModel().getSelectedItem();
                if (sel != null) dialogoAtividade(eventoId, sel.getId(), veioDaGestao);
            });
            var criterio = new Button("Critério presença");
            criterio.setOnAction(e -> {
                Atividade sel = listaAtv.getSelectionModel().getSelectedItem();
                if (sel != null) dialogoCriterio(eventoId, sel.getId(), veioDaGestao);
            });
            var pessoasBtn = new Button("Pessoas");
            pessoasBtn.setOnAction(e -> {
                Atividade sel = listaAtv.getSelectionModel().getSelectedItem();
                if (sel != null) dialogoPessoas(eventoId, sel.getId(), veioDaGestao);
            });
            var freqBtn = new Button("Frequência");
            freqBtn.setOnAction(e -> dialogoFrequencia(eventoId, veioDaGestao));
            var avalBtn = new Button("Avaliações");
            avalBtn.setOnAction(e -> dialogoAvaliacoes(eventoId, veioDaGestao));
            var relBtn = new Button("Relatórios");
            relBtn.setOnAction(e -> dialogoRelatorios(eventoId, veioDaGestao));
            var barraGestao = new HBox(8, nova, remarcar, criterio, pessoasBtn, freqBtn, avalBtn, relBtn);
            linhas.getChildren().add(barraGestao);
        }

        var msg = new Label();
        var inscrever = new Button("Inscrever-me");
        inscrever.setOnAction(e -> {
            try {
                Inscricao i = app.inscreverParticipante.executar(logado.getId(), eventoId);
                msg.setText("Inscrito! ID: " + i.getId().substring(0, 8));
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        });
        var voltar = new Button("Voltar");
        voltar.setOnAction(e -> {
            if (veioDaGestao) mostrarGestao();
            else mostrarExplorar();
        });
        var botoes = new HBox(8, inscrever, voltar, msg);
        botoes.setPadding(new Insets(8));
        navegar(new BorderPane(linhas, null, null, botoes, null),
                veioDaGestao ? navGestao : navExplorar);
    }

    // ---------- DSK-11: atividades (RF-05/06/07/13/19) ----------

    private void dialogoAtividade(String eventoId, String atividadeId, boolean veioDaGestao) {
        Evento evento = app.eventos.buscarPorId(eventoId).orElse(null);
        if (evento == null) return;
        try {
            exigirDono(evento);
        } catch (Exception ex) {
            alerta("Falha: " + mensagem(ex));
            return;
        }
        Atividade atual = null;
        if (atividadeId != null) {
            atual = evento.getAtividades().stream()
                    .filter(a -> a.getId().equals(atividadeId)).findFirst().orElse(null);
            if (atual == null) {
                alerta("Atividade não encontrada.");
                return;
            }
        }
        boolean edicao = atual != null;
        var titulo = new TextField(edicao ? atual.getTitulo() : "");
        titulo.setPromptText("título (obrigatório)");
        var inicio = new SeletorDataHora(edicao ? atual.getPeriodo().getInicio() : null);
        var fim = new SeletorDataHora(edicao ? atual.getPeriodo().getFim() : null);
        var local = new TextField(edicao ? atual.getLocal() : "");
        local.setPromptText("local (obrigatório)");
        var tipo = new TextField(edicao && atual.getTipo() != null ? atual.getTipo() : "");
        tipo.setPromptText("tipo livre ex: palestra, oficina (opcional)");
        var trilha = new TextField(edicao && atual.getTrilha() != null ? atual.getTrilha() : "");
        trilha.setPromptText("trilha/categoria (opcional)");
        var vagas = new TextField(edicao && atual.getMaxVagas() != null
                ? String.valueOf(atual.getMaxVagas()) : "");
        vagas.setPromptText("vagas (vazio = sem limite)");
        var criterio = new ChoiceBox<>(FXCollections.observableArrayList("UNICA", "ENTRADA_SAIDA"));
        criterio.setValue(edicao ? atual.getNomePoliticaFrequencia() : "UNICA");
        criterio.setDisable(edicao); // critério troca pela ação própria (rastreabilidade RN-17)
        var msg = new Label();
        var salvar = new Button(edicao ? "Remarcar" : "Criar atividade");
        var voltar = new Button("Voltar");
        salvar.setOnAction(e -> {
            try {
                if (titulo.getText() == null || titulo.getText().isBlank()) {
                    throw new IllegalArgumentException("O título da atividade é obrigatório.");
                }
                if (local.getText() == null || local.getText().isBlank()) {
                    throw new IllegalArgumentException("O local é obrigatório.");
                }
                LocalDateTime ini = inicio.obter();
                LocalDateTime fimDt = fim.obter();
                if (ini == null || fimDt == null) {
                    throw new IllegalArgumentException(
                            "Escolha início e fim no calendário (data + hora).");
                }
                if (!fimDt.isAfter(ini)) {
                    throw new IllegalArgumentException("O fim deve ser depois do início.");
                }
                Integer maxVagas = montarVagas(vagas.getText());
                String tipoV = tipo.getText().isBlank() ? null : tipo.getText().trim();
                String trilhaV = trilha.getText().isBlank() ? null : trilha.getText().trim();
                if (!edicao) {
                    Evento salvo = app.adicionarAtividade.executar(eventoId,
                            titulo.getText().trim(), ini, fimDt, local.getText().trim(),
                            tipoV, trilhaV, maxVagas);
                    if (!"UNICA".equals(criterio.getValue())) {
                        Evento recem = app.eventos.buscarPorId(eventoId).orElseThrow();
                        var criada = recem.getAtividades().stream()
                                .max((x, y) -> x.getId().compareTo(y.getId())).orElse(null);
                        if (criada == null) {
                            criada = salvo.getAtividades().get(salvo.getAtividades().size() - 1);
                        }
                        criada.definirPoliticaFrequencia(criterio.getValue());
                        app.eventos.salvar(recem);
                    }
                } else {
                    app.remarcarAtividade.executar(eventoId, atividadeId,
                            ini, fimDt, local.getText().trim());
                }
                mostrarDetalhe(eventoId, veioDaGestao);
            } catch (Exception ex) {
                // RN-07: mesmo local + sobreposição bloqueia com motivo do domínio.
                msg.setText("Falha: " + mensagem(ex));
            }
        });
        voltar.setOnAction(e -> mostrarDetalhe(eventoId, veioDaGestao));
        var grade = new GridPane();
        grade.setPadding(new Insets(16));
        grade.setVgap(8);
        grade.setHgap(8);
        grade.add(new Label("Título:"), 0, 0);
        grade.add(titulo, 1, 0);
        grade.add(new Label("Início:"), 0, 1);
        grade.add(inicio.caixa(), 1, 1);
        grade.add(new Label("Fim:"), 0, 2);
        grade.add(fim.caixa(), 1, 2);
        grade.add(new Label("Local:"), 0, 3);
        grade.add(local, 1, 3);
        grade.add(new Label("Tipo:"), 0, 4);
        grade.add(tipo, 1, 4);
        grade.add(new Label("Trilha:"), 0, 5);
        grade.add(trilha, 1, 5);
        grade.add(new Label("Vagas:"), 0, 6);
        grade.add(vagas, 1, 6);
        grade.add(new Label("Critério:"), 0, 7);
        grade.add(criterio, 1, 7);
        grade.add(salvar, 1, 8);
        grade.add(voltar, 1, 9);
        grade.add(msg, 1, 10);
        navegar(grade, veioDaGestao ? navGestao : navExplorar);
    }

    private void dialogoCriterio(String eventoId, String atividadeId, boolean veioDaGestao) {
        Evento evento = app.eventos.buscarPorId(eventoId).orElse(null);
        if (evento == null) return;
        try {
            exigirDono(evento);
        } catch (Exception ex) {
            alerta("Falha: " + mensagem(ex));
            return;
        }
        var atual = evento.getAtividades().stream()
                .filter(a -> a.getId().equals(atividadeId)).findFirst().orElse(null);
        if (atual == null) {
            alerta("Atividade não encontrada.");
            return;
        }
        var criterio = new ChoiceBox<>(FXCollections.observableArrayList("UNICA", "ENTRADA_SAIDA"));
        criterio.setValue(atual.getNomePoliticaFrequencia());
        var msg = new Label("Troca de critério não reescreve histórico consolidado (RN-17).");
        var salvar = new Button("Aplicar");
        salvar.setOnAction(e -> {
            try {
                Evento recem = app.eventos.buscarPorId(eventoId).orElseThrow();
                exigirDono(recem);
                var alvo = recem.getAtividades().stream()
                        .filter(a -> a.getId().equals(atividadeId)).findFirst()
                        .orElseThrow(() -> new IllegalStateException("Atividade não encontrada."));
                alvo.definirPoliticaFrequencia(criterio.getValue());
                app.eventos.salvar(recem);
                mostrarDetalhe(eventoId, veioDaGestao);
            } catch (Exception ex) {
                alerta("Falha: " + mensagem(ex));
            }
        });
        var voltar = new Button("Voltar");
        voltar.setOnAction(e -> mostrarDetalhe(eventoId, veioDaGestao));
        var caixa = new VBox(8, new Label("Critério de presença (RF-19):"), criterio, msg, salvar, voltar);
        caixa.setPadding(new Insets(16));
        navegar(caixa, veioDaGestao ? navGestao : navExplorar);
    }

    // ---------- DSK-12: pessoas (RF-08) ----------

    private void dialogoPessoas(String eventoId, String atividadeId, boolean veioDaGestao) {
        Evento evento = app.eventos.buscarPorId(eventoId).orElse(null);
        if (evento == null) return;
        try {
            exigirDono(evento);
        } catch (Exception ex) {
            alerta("Falha: " + mensagem(ex));
            return;
        }
        var lista = new ListView<>(FXCollections.observableArrayList(
                app.pessoas.buscarPorAtividade(atividadeId)));
        lista.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(br.com.evento.domain.model.entity.Pessoa p, boolean vazio) {
                super.updateItem(p, vazio);
                setText(vazio || p == null ? null : p.getNome() + " (" + p.getPapel() + ")");
            }
        });
        lista.setPrefHeight(200);
        var nome = new TextField();
        nome.setPromptText("nome (obrigatório)");
        var papel = new TextField();
        papel.setPromptText("papel ex: palestrante (obrigatório)");
        var msg = new Label("Foto não é exigida (RN-02).");
        var vincular = new Button("Vincular");
        vincular.setOnAction(e -> {
            try {
                if (nome.getText() == null || nome.getText().isBlank()) {
                    throw new IllegalArgumentException("O nome da pessoa é obrigatório.");
                }
                if (papel.getText() == null || papel.getText().isBlank()) {
                    throw new IllegalArgumentException("O papel da pessoa é obrigatório.");
                }
                app.vincularPessoa.executar(eventoId, atividadeId,
                        nome.getText().trim(), papel.getText().trim());
                dialogoPessoas(eventoId, atividadeId, veioDaGestao);
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        });
        var remover = new Button("Remover");
        remover.setOnAction(e -> {
            var sel = lista.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            try {
                app.vincularPessoa.remover(sel.getId());
                dialogoPessoas(eventoId, atividadeId, veioDaGestao);
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        });
        var voltar = new Button("Voltar ao evento");
        voltar.setOnAction(e -> mostrarDetalhe(eventoId, veioDaGestao));
        var form = new HBox(8, nome, papel, vincular, remover);
        var caixa = new VBox(8, new Label("Pessoas da atividade:"), lista, form, msg, voltar);
        caixa.setPadding(new Insets(12));
        navegar(caixa, veioDaGestao ? navGestao : navExplorar);
    }

    /**
     * Seletor de data/hora: {@link DatePicker} abre o calendário popup;
     * {@link Spinner}s de hora/minuto funcionam como rodinhas (setas + digitação).
     */
    private static final class SeletorDataHora {
        private final DatePicker data = new DatePicker();
        private final Spinner<Integer> hora = new Spinner<>(0, 23, 9);
        private final Spinner<Integer> minuto = new Spinner<>(0, 59, 0);
        private final HBox caixa = new HBox(6);

        SeletorDataHora(LocalDateTime inicial) {
            if (inicial != null) {
                data.setValue(inicial.toLocalDate());
                hora.getValueFactory().setValue(inicial.getHour());
                minuto.getValueFactory().setValue(inicial.getMinute());
            }
            data.setPromptText("escolher data…");
            hora.setEditable(true);
            minuto.setEditable(true);
            hora.setPrefWidth(75);
            minuto.setPrefWidth(75);
            caixa.getChildren().addAll(data, hora, new Label(":"), minuto);
        }

        HBox caixa() {
            return caixa;
        }

        /** null = data não escolhida. */
        LocalDateTime obter() {
            if (data.getValue() == null) return null;
            return LocalDateTime.of(data.getValue(),
                    LocalTime.of(valorSeguro(hora, 9), valorSeguro(minuto, 0)));
        }

        private static int valorSeguro(Spinner<Integer> s, int padrao) {
            try {
                s.commitValue();
            } catch (Exception ignored) {
                // mantém o valor atual
            }
            Integer v = s.getValue();
            return v != null ? v : padrao;
        }
    }

    // ---------- DSK-30/31/32: frequência do operador (RF-19/20/21/22/23) ----------

    private void dialogoFrequencia(String eventoId, boolean veioDaGestao) {
        Evento evento = app.eventos.buscarPorId(eventoId).orElse(null);
        if (evento == null) return;
        try {
            exigirDono(evento);
        } catch (Exception ex) {
            alerta("Falha: " + mensagem(ex));
            return;
        }
        if (evento.getAtividades().isEmpty()) {
            alerta("Cadastre ao menos uma atividade antes de lançar frequência.");
            return;
        }
        var atvBox = new ChoiceBox<>(FXCollections.observableArrayList(evento.getAtividades()));
        atvBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Atividade a) {
                return a == null ? "" : a.getTitulo() + " [" + a.getNomePoliticaFrequencia() + "]";
            }

            @Override
            public Atividade fromString(String s) {
                return null;
            }
        });
        atvBox.setValue(evento.getAtividades().get(0));
        var lista = new ListView<Inscricao>();
        lista.setPrefHeight(220);
        Runnable recarregar = () -> {
            Atividade atv = atvBox.getValue();
            Evento recem = app.eventos.buscarPorId(eventoId).orElseThrow();
            Atividade atual = recem.getAtividades().stream()
                    .filter(a -> a.getId().equals(atv.getId())).findFirst().orElse(atv);
            lista.setItems(FXCollections.observableArrayList(
                    app.inscricoes.buscarPorEvento(eventoId)));
            lista.setCellFactory(v -> new ListCell<>() {
                @Override
                protected void updateItem(Inscricao i, boolean vazio) {
                    super.updateItem(i, vazio);
                    if (vazio || i == null) {
                        setText(null);
                        return;
                    }
                    setText(nomeParticipante(i.getParticipanteId())
                            + " — check-ins: " + i.getCheckIns(atual.getId()).size()
                            + " — situação: " + i.calcularPresenca(atual));
                }
            });
        };
        atvBox.setOnAction(e -> recarregar.run());
        recarregar.run();

        var msg = new Label();
        var manual = new Button("Check-in manual");
        manual.setOnAction(e -> {
            Inscricao sel = lista.getSelectionModel().getSelectedItem();
            if (sel == null || atvBox.getValue() == null) return;
            try {
                // RF-22: responsável = organizador logado (RN-11/12 rastreabilidade).
                app.realizarCheckInManual.executar(
                        sel.getId(), atvBox.getValue().getId(), logado.getId());
                msg.setText("Presença manual registrada.");
                recarregar.run();
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        });
        var tokenArea = new javafx.scene.control.TextArea();
        tokenArea.setPromptText("token do QR Code…");
        tokenArea.setPrefRowCount(2);
        var gerarQr = new Button("Gerar QR");
        gerarQr.setOnAction(e -> {
            Inscricao sel = lista.getSelectionModel().getSelectedItem();
            if (sel == null || atvBox.getValue() == null) return;
            try {
                // RF-20: token opaco ids+salt Base64, sem PII (RN-10).
                tokenArea.setText(app.qrCodeService.gerarTokenParaCheckIn(
                        sel, atvBox.getValue().getId()));
                msg.setText("QR gerado (sem dados pessoais).");
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        });
        var validarQr = new Button("Validar QR");
        validarQr.setOnAction(e -> {
            try {
                if (tokenArea.getText() == null || tokenArea.getText().isBlank()) {
                    throw new IllegalArgumentException("Cole o token do QR Code.");
                }
                app.realizarCheckIn.executar(tokenArea.getText().trim());
                msg.setText("Check-in por QR registrado.");
                recarregar.run();
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        });
        var voltar = new Button("Voltar ao evento");
        voltar.setOnAction(e -> mostrarDetalhe(eventoId, veioDaGestao));
        var caixa = new VBox(8,
                new Label("Frequência — escolha a atividade:"),
                atvBox,
                new Label("Inscritos:"),
                lista,
                new HBox(8, manual),
                new Label("QR Code (RF-20/21):"),
                tokenArea,
                new HBox(8, gerarQr, validarQr),
                msg, voltar);
        caixa.setPadding(new Insets(12));
        navegar(caixa, veioDaGestao ? navGestao : navExplorar);
    }

    private String nomeParticipante(String participanteId) {
        try {
            return app.usuarios.buscarPorId(participanteId)
                    .map(Usuario::getNome)
                    .orElse(participanteId.substring(0, 8));
        } catch (Exception e) {
            return participanteId;
        }
    }

    // ---------- DSK-40/41/42: avaliações (RF-24–28, CA-06) ----------

    private void dialogoAvaliacoes(String eventoId, boolean veioDaGestao) {
        Evento evento = app.eventos.buscarPorId(eventoId).orElse(null);
        if (evento == null) return;
        try {
            exigirDono(evento);
        } catch (Exception ex) {
            alerta("Falha: " + mensagem(ex));
            return;
        }
        var box = new ChoiceBox<>(FXCollections.observableArrayList(
                app.questionarios.buscarPorEvento(eventoId)));
        box.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(br.com.evento.domain.model.entity.Questionario q) {
                return q == null ? "" : q.getTitulo()
                        + (q.isCongelado() ? " [congelado]" : "");
            }

            @Override
            public br.com.evento.domain.model.entity.Questionario fromString(String s) {
                return null;
            }
        });
        var listaQ = app.questionarios.buscarPorEvento(eventoId);
        if (!listaQ.isEmpty()) box.setValue(listaQ.get(0));
        var msg = new Label();
        var novo = new Button("Novo questionário");
        novo.setOnAction(e -> dialogoNovoQuestionario(eventoId, veioDaGestao));
        var resultados = new Button("Ver resultados");
        resultados.setOnAction(e -> {
            var q = box.getValue();
            if (q == null) {
                msg.setText("Escolha um questionário.");
                return;
            }
            try {
                var r = app.consolidarAvaliacoes.executar(q.getId());
                var sb = new StringBuilder("Avaliações: " + r.totalAvaliacoes() + "\n\n");
                for (var p : r.perguntas()) {
                    sb.append("• ").append(p.enunciado())
                            .append(" [").append(p.tipo()).append("] — ")
                            .append(p.totalRespostas()).append(" resposta(s)\n");
                    if (p.media() != null) sb.append("  média: ").append(p.media()).append("\n");
                    if (p.distribuicao() != null) {
                        p.distribuicao().forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v).append("\n"));
                    }
                    if (p.comentarios() != null) {
                        p.comentarios().forEach(c -> sb.append("  - ").append(c).append("\n"));
                    }
                }
                var area = new javafx.scene.control.TextArea(sb.toString());
                area.setEditable(false);
                area.setPrefSize(560, 380);
                var voltar2 = new Button("Voltar");
                voltar2.setOnAction(e2 -> dialogoAvaliacoes(eventoId, veioDaGestao));
                navegar(new VBox(8,
                        new Label("Resultados — " + q.getTitulo()), area, voltar2), null);
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        });
        var responder = new Button("Responder como inscrito");
        responder.setOnAction(e -> {
            var q = box.getValue();
            if (q == null) {
                msg.setText("Escolha um questionário.");
                return;
            }
            dialogoResponder(eventoId, q.getId(), veioDaGestao);
        });
        var voltar = new Button("Voltar ao evento");
        voltar.setOnAction(e -> mostrarDetalhe(eventoId, veioDaGestao));
        var caixa = new VBox(8,
                new Label("Questionários do evento (RF-24; congela após 1ª resposta, RN-17):"),
                box, new HBox(8, novo, resultados, responder), msg, voltar);
        caixa.setPadding(new Insets(12));
        navegar(caixa, veioDaGestao ? navGestao : navExplorar);
    }

    private void dialogoNovoQuestionario(String eventoId, boolean veioDaGestao) {
        var titulo = new TextField();
        titulo.setPromptText("título (obrigatório)");
        var politica = new ChoiceBox<>(FXCollections.observableArrayList("IDENTIFICADO", "ANONIMO"));
        politica.setValue("IDENTIFICADO");
        var perguntas = FXCollections.<br.com.evento.domain.model.entity.Pergunta>observableArrayList();
        var listaP = new ListView<>(perguntas);
        listaP.setPrefHeight(140);
        listaP.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(br.com.evento.domain.model.entity.Pergunta p, boolean vazio) {
                super.updateItem(p, vazio);
                setText(vazio || p == null ? null
                        : p.getEnunciado() + " [" + p.getTipo() + "]"
                                + (p.isObrigatoria() ? " *" : ""));
            }
        });
        var enunciado = new TextField();
        enunciado.setPromptText("enunciado da pergunta");
        var tipo = new ChoiceBox<>(FXCollections.observableArrayList(
                br.com.evento.domain.model.entity.TipoPergunta.TEXTO_LIVRE,
                br.com.evento.domain.model.entity.TipoPergunta.ESCOLHA_SIMPLES,
                br.com.evento.domain.model.entity.TipoPergunta.ESCALA_NUMERICA));
        tipo.setValue(br.com.evento.domain.model.entity.TipoPergunta.TEXTO_LIVRE);
        var opcoes = new TextField();
        opcoes.setPromptText("opções separadas por vírgula (só escolha única, mín. 2)");
        var escala = new TextField("5");
        escala.setPromptText("escala máxima (só escala, mín. 2)");
        var obrig = new CheckBox("Obrigatória");
        obrig.setSelected(true);
        var msg = new Label("Informe a política antes do envio (RN-15).");
        var add = new Button("Adicionar pergunta");
        add.setOnAction(e -> {
            try {
                if (enunciado.getText() == null || enunciado.getText().isBlank()) {
                    throw new IllegalArgumentException("O enunciado da pergunta é obrigatório.");
                }
                List<String> ops = null;
                Integer max = null;
                if (tipo.getValue() == br.com.evento.domain.model.entity.TipoPergunta.ESCOLHA_SIMPLES) {
                    ops = java.util.Arrays.stream(opcoes.getText().split(","))
                            .map(String::trim).filter(s -> !s.isEmpty()).toList();
                    if (ops.size() < 2) {
                        throw new IllegalArgumentException("Escolha única exige ao menos 2 opções.");
                    }
                }
                if (tipo.getValue() == br.com.evento.domain.model.entity.TipoPergunta.ESCALA_NUMERICA) {
                    max = Integer.parseInt(escala.getText().trim());
                    if (max < 2) throw new IllegalArgumentException("Escala máxima deve ser >= 2.");
                }
                perguntas.add(new br.com.evento.domain.model.entity.Pergunta(
                        null, enunciado.getText().trim(), tipo.getValue(),
                        obrig.isSelected(), ops, max));
                enunciado.clear();
                opcoes.clear();
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        });
        var salvar = new Button("Salvar questionário");
        salvar.setOnAction(e -> {
            try {
                if (titulo.getText() == null || titulo.getText().isBlank()) {
                    throw new IllegalArgumentException("O título do questionário é obrigatório.");
                }
                if (perguntas.isEmpty()) {
                    throw new IllegalArgumentException("Adicione ao menos uma pergunta.");
                }
                app.criarQuestionario.executar(eventoId, titulo.getText().trim(),
                        new java.util.ArrayList<>(perguntas), politica.getValue());
                dialogoAvaliacoes(eventoId, veioDaGestao);
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        });
        var voltar = new Button("Voltar");
        voltar.setOnAction(e -> dialogoAvaliacoes(eventoId, veioDaGestao));
        var grade = new GridPane();
        grade.setPadding(new Insets(12));
        grade.setVgap(8);
        grade.setHgap(8);
        grade.add(new Label("Título:"), 0, 0);
        grade.add(titulo, 1, 0);
        grade.add(new Label("Política:"), 0, 1);
        grade.add(politica, 1, 1);
        grade.add(new Label("Perguntas:"), 0, 2);
        grade.add(listaP, 1, 2);
        grade.add(new Label("Nova:"), 0, 3);
        grade.add(enunciado, 1, 3);
        grade.add(tipo, 1, 4);
        grade.add(opcoes, 1, 5);
        var linhaEscala = new HBox(8, new Label("Escala máx.:"), escala,
                obrig, add);
        grade.add(linhaEscala, 1, 6);
        grade.add(salvar, 1, 7);
        grade.add(voltar, 1, 8);
        grade.add(msg, 1, 9);
        navegar(grade, veioDaGestao ? navGestao : navExplorar);
    }

    private void dialogoResponder(String eventoId, String questionarioId, boolean veioDaGestao) {
        var q = app.questionarios.buscarPorId(questionarioId).orElse(null);
        if (q == null) {
            alerta("Questionário não encontrado.");
            return;
        }
        var inscBox = new ChoiceBox<>(FXCollections.observableArrayList(
                app.inscricoes.buscarPorEvento(eventoId)));
        inscBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Inscricao i) {
                return i == null ? "" : nomeParticipante(i.getParticipanteId());
            }

            @Override
            public Inscricao fromString(String s) {
                return null;
            }
        });
        var caixa = new VBox(8);
        caixa.setPadding(new Insets(12));
        caixa.getChildren().add(new Label("Responder — " + q.getTitulo()
                + " (só inscrito + presença validada, RN-13):"));
        caixa.getChildren().add(inscBox);
        var campos = new java.util.LinkedHashMap<String, TextField>();
        for (var p : q.getPerguntas()) {
            String dica = switch (p.getTipo()) {
                case TEXTO_LIVRE -> "texto livre";
                case ESCOLHA_SIMPLES -> "opções: " + String.join(" | ", p.getOpcoes());
                case ESCALA_NUMERICA -> "número de 1 a " + p.getEscalaMax();
            };
            var f = new TextField();
            f.setPromptText(dica);
            campos.put(p.getId(), f);
            caixa.getChildren().addAll(new Label(p.getEnunciado()
                    + (p.isObrigatoria() ? " *" : "")), f);
        }
        var msg = new Label();
        var enviar = new Button("Enviar avaliação");
        enviar.setOnAction(e -> {
            try {
                var sel = inscBox.getValue();
                if (sel == null) throw new IllegalArgumentException("Escolha a inscrição.");
                var respostas = new java.util.ArrayList<br.com.evento.domain.model.entity.Resposta>();
                for (var p : q.getPerguntas()) {
                    respostas.add(new br.com.evento.domain.model.entity.Resposta(
                            p.getId(), campos.get(p.getId()).getText()));
                }
                app.submeterAvaliacao.executar(questionarioId, sel.getId(), respostas);
                dialogoAvaliacoes(eventoId, veioDaGestao);
            } catch (Exception ex) {
                // RN-13/RN-14: inelegível ou duplicada mostra o motivo.
                msg.setText("Falha: " + mensagem(ex));
            }
        });
        var voltar = new Button("Voltar");
        voltar.setOnAction(e -> dialogoAvaliacoes(eventoId, veioDaGestao));
        caixa.getChildren().addAll(enviar, voltar, msg);
        navegar(caixa, veioDaGestao ? navGestao : navExplorar);
    }

    // ---------- DSK-50/51: relatórios (RF-29/30/31, CA-07) + certificados (RF-32–35, CA-08) ----------

    private void dialogoRelatorios(String eventoId, boolean veioDaGestao) {
        Evento evento = app.eventos.buscarPorId(eventoId).orElse(null);
        if (evento == null) return;
        try {
            exigirDono(evento);
        } catch (Exception ex) {
            alerta("Falha: " + mensagem(ex));
            return;
        }
        var msg = new Label();
        var csvInscritos = new Button("Baixar inscritos (CSV)");
        csvInscritos.setOnAction(e -> {
            try {
                salvarTexto("inscritos-" + eventoId.substring(0, 8) + ".csv",
                        app.relatorioInscritos.executarEmCsv(eventoId));
                msg.setText("CSV de inscritos salvo.");
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        });
        var csvPresenca = new Button("Baixar presença (CSV)");
        csvPresenca.setOnAction(e -> {
            try {
                salvarTexto("presenca-" + eventoId.substring(0, 8) + ".csv",
                        app.relatorioPresenca.executarEmCsv(eventoId));
                msg.setText("CSV de presença salvo.");
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        });

        // RF-32: critério de elegibilidade (percentual mínimo).
        var percentual = new TextField();
        percentual.setPromptText("% mínimo ex: 75 (vazio = padrão)");
        var definirCriterio = new Button("Definir critério");
        definirCriterio.setOnAction(e -> {
            try {
                Integer pct = percentual.getText().isBlank() ? null
                        : Integer.parseInt(percentual.getText().trim());
                app.gerenciarCertificados.definirCriterio(eventoId, pct, null);
                msg.setText("Critério atualizado (RN-16 calculável).");
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        });

        // RF-33/35: emitir + baixar PDF.
        var inscBox = new ChoiceBox<>(FXCollections.observableArrayList(
                app.inscricoes.buscarPorEvento(eventoId)));
        inscBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Inscricao i) {
                return i == null ? "" : nomeParticipante(i.getParticipanteId());
            }

            @Override
            public Inscricao fromString(String s) {
                return null;
            }
        });
        var emitirPart = new Button("Emitir p/ participante");
        emitirPart.setOnAction(e -> {
            try {
                var sel = inscBox.getValue();
                if (sel == null) throw new IllegalArgumentException("Escolha a inscrição.");
                var c = app.gerenciarCertificados.emitirParaParticipante(eventoId, sel.getId());
                msg.setText("Emitido! Código: " + c.getCodigo());
                dialogoRelatorios(eventoId, veioDaGestao);
            } catch (Exception ex) {
                // CA-08: inelegível vê a justificativa.
                msg.setText("Falha: " + mensagem(ex));
            }
        });
        var todasPessoas = new java.util.ArrayList<br.com.evento.domain.model.entity.Pessoa>();
        for (var a : evento.getAtividades()) {
            todasPessoas.addAll(app.pessoas.buscarPorAtividade(a.getId()));
        }
        var pessoaBox = new ChoiceBox<>(FXCollections.observableArrayList(todasPessoas));
        pessoaBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(br.com.evento.domain.model.entity.Pessoa p) {
                return p == null ? "" : p.getNome() + " (" + p.getPapel() + ")";
            }

            @Override
            public br.com.evento.domain.model.entity.Pessoa fromString(String s) {
                return null;
            }
        });
        var emitirPal = new Button("Declaração p/ palestrante");
        emitirPal.setOnAction(e -> {
            try {
                var sel = pessoaBox.getValue();
                if (sel == null) throw new IllegalArgumentException("Escolha a pessoa.");
                var c = app.gerenciarCertificados.emitirParaPalestrante(eventoId, sel.getId());
                msg.setText("Declaração emitida! Código: " + c.getCodigo());
                dialogoRelatorios(eventoId, veioDaGestao);
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        });

        var listaCert = new ListView<>(FXCollections.observableArrayList(
                app.gerenciarCertificados.listarPorEvento(eventoId)));
        listaCert.setPrefHeight(140);
        listaCert.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(br.com.evento.domain.model.entity.Certificado c, boolean vazio) {
                super.updateItem(c, vazio);
                setText(vazio || c == null ? null : c.getCodigo() + " — "
                        + c.getParticipanteNome()
                        + (c.getPapelDeclarado() != null ? " (" + c.getPapelDeclarado() + ")" : ""));
            }
        });
        var baixarPdf = new Button("Baixar PDF");
        baixarPdf.setOnAction(e -> {
            var sel = listaCert.getSelectionModel().getSelectedItem();
            if (sel == null) {
                msg.setText("Escolha um certificado na lista.");
                return;
            }
            try {
                byte[] pdf = app.gerenciarCertificados.gerarPdf(sel.getId());
                var fc = new FileChooser();
                fc.setInitialFileName("certificado-" + sel.getCodigo() + ".pdf");
                var arq = fc.showSaveDialog(palco);
                if (arq != null) Files.write(arq.toPath(), pdf);
                msg.setText("PDF salvo.");
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        });
        var emailDestino = new TextField();
        emailDestino.setPromptText("e-mail de destino");
        var enviar = new Button("Enviar por e-mail");
        enviar.setOnAction(e -> {
            var sel = listaCert.getSelectionModel().getSelectedItem();
            if (sel == null) {
                msg.setText("Escolha um certificado na lista.");
                return;
            }
            try {
                // RF-34: sem SMTP → 422 claro, download preservado.
                var reg = app.gerenciarCertificados.enviarPorEmail(sel.getId(), emailDestino.getText());
                msg.setText("Envio registrado: " + reg.detalhe() + " → " + reg.para());
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        });

        var voltar = new Button("Voltar ao evento");
        voltar.setOnAction(e -> mostrarDetalhe(eventoId, veioDaGestao));
        var caixa = new VBox(8,
                new Label("Relatórios (CA-07):"),
                new HBox(8, csvInscritos, csvPresenca),
                new Label("Certificados — critério e emissão (CA-08, D):"),
                new HBox(8, percentual, definirCriterio),
                new HBox(8, inscBox, emitirPart),
                new HBox(8, pessoaBox, emitirPal),
                new Label("Emitidos:"),
                listaCert,
                new HBox(8, baixarPdf, emailDestino, enviar),
                msg, voltar);
        caixa.setPadding(new Insets(12));
        navegar(caixa, veioDaGestao ? navGestao : navExplorar);
    }

    private void salvarTexto(String nomeSugerido, String conteudo) throws Exception {
        var fc = new FileChooser();
        fc.setInitialFileName(nomeSugerido);
        var arq = fc.showSaveDialog(palco);
        if (arq != null) Files.writeString(arq.toPath(), conteudo);
    }

    // ---------- DSK-13: programação com filtros (RF-09) ----------

    private void dialogoProgramacao() {
        if (!exigirOrganizador()) return;
        var eventos = app.listarEventosOrganizador.executar(logado.getId());
        if (eventos.isEmpty()) {
            alerta("Você ainda não tem eventos. Crie um em Meus eventos.");
            return;
        }
        var eventoBox = new ChoiceBox<>(FXCollections.observableArrayList(eventos));
        eventoBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Evento e) {
                return e == null ? "" : e.getTitulo() + " [" + e.getStatus() + "]";
            }

            @Override
            public Evento fromString(String s) {
                return null;
            }
        });
        eventoBox.setValue(eventos.get(0));
        var fData = new TextField();
        fData.setPromptText("data yyyy-MM-dd (opcional)");
        var fTipo = new TextField();
        fTipo.setPromptText("tipo (opcional)");
        var fTrilha = new TextField();
        fTrilha.setPromptText("trilha (opcional)");
        var fLocal = new TextField();
        fLocal.setPromptText("local (opcional)");
        var lista = new ListView<Atividade>();
        lista.setPrefHeight(280);
        lista.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Atividade a, boolean vazio) {
                super.updateItem(a, vazio);
                if (vazio || a == null) {
                    setText(null);
                    return;
                }
                setText("• " + a.getTitulo()
                        + " | " + a.getPeriodo().getInicio() + "–" + a.getPeriodo().getFim()
                        + " | " + a.getLocal()
                        + (a.getTipo() != null ? " | " + a.getTipo() : "")
                        + (a.getTrilha() != null ? " [" + a.getTrilha() + "]" : ""));
            }
        });
        var msg = new Label("Filtros combináveis (RF-09: data, tipo, trilha, local).");
        msg.getStyleClass().add("subtitle");
        msg.setWrapText(true);
        Runnable filtrar = () -> {
            try {
                Evento ev = app.eventos.buscarPorId(eventoBox.getValue().getId()).orElseThrow();
                String data = fData.getText() == null ? "" : fData.getText().trim();
                java.time.LocalDate dia = null;
                if (!data.isEmpty()) {
                    try {
                        dia = java.time.LocalDate.parse(data);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Data inválida (use yyyy-MM-dd, ex: 2026-10-05).");
                    }
                }
                java.time.LocalDate diaF = dia;
                var res = ev.getAtividades().stream()
                        // Mesmos 4 filtros server-side da API (RF-09).
                        .filter(a -> diaF == null
                                || a.getPeriodo().getInicio().toLocalDate().equals(diaF))
                        .filter(a -> casaFiltro(fTipo.getText(), a.getTipo()))
                        .filter(a -> casaFiltro(fTrilha.getText(), a.getTrilha()))
                        .filter(a -> casaFiltro(fLocal.getText(), a.getLocal()))
                        .toList();
                lista.setItems(FXCollections.observableArrayList(res));
                msg.setText(res.size() + " atividade(s).");
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        };
        var filtrarBtn = primario("Filtrar");
        filtrarBtn.setOnAction(e -> filtrar.run());
        eventoBox.setOnAction(e -> filtrar.run());
        filtrar.run();
        var voltar = new Button("Voltar");
        voltar.setOnAction(e -> mostrarPrincipal());
        var caixa = new VBox(8,
                titulo("Programação"),
                subtitulo("Grade do evento com filtros combináveis (RF-09)."),
                eventoBox,
                new HBox(8, fData, fTipo, fTrilha, fLocal, filtrarBtn),
                lista, msg, voltar);
        caixa.setPadding(new Insets(20));
        navegar(caixa, navProg);
    }

    private static boolean casaFiltro(String filtro, String valor) {
        if (filtro == null || filtro.isBlank()) return true;
        if (valor == null) return false;
        return valor.equalsIgnoreCase(filtro.trim());
    }

    // ---------- DSK-20/21/22: inscrições, vagas e agenda (RF-12–18, CA-02/03) ----------

    private void dialogoInscricoes() {
        if (!exigirOrganizador()) return;
        var eventos = app.listarEventosOrganizador.executar(logado.getId());
        if (eventos.isEmpty()) {
            alerta("Você ainda não tem eventos.");
            return;
        }
        var eventoBox = new ChoiceBox<>(FXCollections.observableArrayList(eventos));
        eventoBox.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Evento e) {
                return e == null ? "" : e.getTitulo() + " [" + e.getStatus() + "]";
            }

            @Override
            public Evento fromString(String s) {
                return null;
            }
        });
        eventoBox.setValue(eventos.get(0));
        var vagasInfo = new Label();
        vagasInfo.getStyleClass().add("badge");
        var lista = new ListView<Inscricao>();
        lista.setPrefHeight(260);
        lista.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Inscricao i, boolean vazio) {
                super.updateItem(i, vazio);
                if (vazio || i == null) {
                    setText(null);
                    return;
                }
                setText(nomeParticipante(i.getParticipanteId())
                        + " — " + i.getAtividadesSelecionadas().size() + " atividade(s)");
            }
        });
        Runnable recarregar = () -> {
            Evento ev = eventoBox.getValue();
            int total = app.inscricoes.contarPorEvento(ev.getId());
            String limite = ev.getMaxVagas() != null ? String.valueOf(ev.getMaxVagas()) : "sem limite";
            // RF-14/RN-06: vagas controladas quando maxVagas definido.
            vagasInfo.setText("Inscritos: " + total + " / " + limite);
            lista.setItems(FXCollections.observableArrayList(
                    app.inscricoes.buscarPorEvento(ev.getId())));
        };
        eventoBox.setOnAction(e -> recarregar.run());
        recarregar.run();
        var msg = new Label();
        msg.getStyleClass().add("msg-erro");
        msg.setWrapText(true);
        var cancelar = new Button("Cancelar inscrição");
        cancelar.getStyleClass().add("btn-danger");
        cancelar.setOnAction(e -> {
            Inscricao sel = lista.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            if (!confirmar(palco,
                    "Cancelar a inscrição de " + nomeParticipante(sel.getParticipanteId())
                            + "? A vaga será liberada (RF-15).")) return;
            try {
                app.cancelarInscricao.executar(sel.getId());
                msg.setText("Inscrição cancelada. Vaga liberada.");
                recarregar.run();
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        });
        var agenda = primario("Ver agenda");
        agenda.setOnAction(e -> {
            Inscricao sel = lista.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            dialogoAgenda(eventoBox.getValue().getId(), sel.getId());
        });
        var voltar = new Button("Voltar");
        voltar.setOnAction(e -> mostrarPrincipal());
        var cabecInsc = new HBox(10, titulo("Inscrições"), vagasInfo);
        cabecInsc.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        var caixa = new VBox(8,
                cabecInsc,
                subtitulo("Inscritos por evento com controle de vagas (RF-14)."),
                eventoBox, lista,
                new HBox(8, cancelar, agenda),
                msg, voltar);
        caixa.setPadding(new Insets(20));
        navegar(caixa, navInsc);
    }

    private void dialogoAgenda(String eventoId, String inscricaoId) {
        try {
            // RF-17/RN-08: agenda deriva das escolhas válidas, em ordem cronológica.
            var agenda = app.consultarAgenda.executar(inscricaoId);
            var lista = new ListView<>(FXCollections.observableArrayList(agenda.getAtividades()));
            lista.setCellFactory(v -> new ListCell<>() {
                @Override
                protected void updateItem(Atividade a, boolean vazio) {
                    super.updateItem(a, vazio);
                    setText(vazio || a == null ? null
                            : a.getPeriodo().getInicio() + " — " + a.getTitulo()
                                    + " | " + a.getLocal());
                }
            });
            var voltar = new Button("Voltar");
            voltar.setOnAction(e -> dialogoInscricoes());
            var caixa = new VBox(8,
                    new Label("Agenda de " + nomeParticipante(
                            app.inscricoes.buscarPorId(inscricaoId)
                                    .map(Inscricao::getParticipanteId).orElse("?"))
                            + (agenda.estaVazia() ? " (vazia)" : "")),
                    lista, voltar);
            caixa.setPadding(new Insets(12));
            navegar(caixa, navInsc);
        } catch (Exception ex) {
            alerta("Falha: " + mensagem(ex));
        }
    }

    // ---------- helpers ----------

    private void estilizarListaEventos(ListView<Evento> lista) {
        lista.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Evento e, boolean vazio) {
                super.updateItem(e, vazio);
                if (vazio || e == null) {
                    setText(null);
                    return;
                }
                String dono = e.getOrganizador() != null ? e.getOrganizador().getNome() : "—";
                setText(e.getTitulo() + " [" + e.getStatus() + "] — " + dono);
            }
        });
    }

    private boolean isOrganizador() {
        return logado instanceof Organizador;
    }

    private String papel() {
        return isOrganizador() ? "ORGANIZADOR" : "PARTICIPANTE";
    }

    private boolean exigirOrganizador() {
        if (logado == null) {
            mostrarLogin();
            return false;
        }
        if (!isOrganizador()) {
            alerta("Apenas organizadores.");
            return false;
        }
        return true;
    }

    private void exigirDono(Evento evento) {
        if (evento.getOrganizador() == null
                || !logado.getId().equals(evento.getOrganizador().getId())) {
            throw new IllegalStateException("Você só pode gerenciar os próprios eventos.");
        }
    }

    private static void validarFormulario(String titulo, String descricao, String vagasTxt,
                                          LocalDateTime ini, LocalDateTime fimDt, String fuso) {
        if (titulo == null || titulo.trim().length() < 3) {
            throw new IllegalArgumentException("O título deve ter ao menos 3 caracteres.");
        }
        if (descricao == null || descricao.trim().length() < 10) {
            throw new IllegalArgumentException("A descrição deve ter ao menos 10 caracteres.");
        }
        if (vagasTxt != null && !vagasTxt.isBlank()) {
            try {
                int v = Integer.parseInt(vagasTxt.trim());
                if (v <= 0) throw new IllegalArgumentException("O número de vagas deve ser positivo.");
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("O número de vagas deve ser um inteiro positivo.");
            }
        }
        boolean iniVazio = ini == null;
        boolean fimVazio = fimDt == null;
        if (iniVazio != fimVazio) {
            throw new IllegalArgumentException("Escolha início e fim no calendário, ou deixe ambos em branco.");
        }
        if (!iniVazio && !fimDt.isAfter(ini)) {
            throw new IllegalArgumentException("O fim deve ser depois do início.");
        }
        if (fuso != null && !fuso.isBlank()) {
            try {
                java.time.ZoneId.of(fuso.trim());
            } catch (Exception e) {
                throw new IllegalArgumentException("Fuso horário inválido (use IANA, ex: America/Sao_Paulo).");
            }
        }
    }

    private static Periodo montarPeriodo(LocalDateTime ini, LocalDateTime fimDt) {
        if (ini == null) return null;
        return new Periodo(ini, fimDt);
    }

    private static Integer montarVagas(String vagasTxt) {
        if (vagasTxt == null || vagasTxt.isBlank()) return null;
        return Integer.parseInt(vagasTxt.trim());
    }

    private static void aplicarDetalhes(Evento evento, Periodo periodo, String local, String fuso,
                                        Integer maxVagas, boolean exigeEscolha, boolean vagasPorAtividade) {
        if (periodo != null || (local != null && !local.isBlank())) {
            evento.definirDetalhes(periodo, local);
        }
        if (fuso != null && !fuso.isBlank()) evento.definirFuso(fuso);
        if (maxVagas != null) evento.configurarLimiteDeVagas(maxVagas);
        evento.configurarInscricao(exigeEscolha, vagasPorAtividade);
    }

    private static String mensagem(Exception ex) {
        String m = ex.getMessage();
        return (m == null || m.isBlank()) ? ex.toString() : m;
    }

    public static void abrir(String[] args) {
        launch(args);
    }

    private void alerta(String texto) {
        var a = new Alert(Alert.AlertType.INFORMATION, texto);
        vestirDialogo(a);
        a.showAndWait();
    }

    /** Aplica o tema IngressoNow no DialogPane (Alert não herda o CSS da cena). */
    private static void vestirDialogo(Alert dialogo) {
        var css = DesktopApp.class.getResource("/desktop/ingressonow.css");
        if (css != null) {
            dialogo.getDialogPane().getStylesheets().add(css.toExternalForm());
        }
    }

    /** Confirmação Sim/Não já tematizada. */
    private static boolean confirmar(javafx.stage.Window dono, String texto) {
        var confirma = new Alert(Alert.AlertType.CONFIRMATION, texto,
                ButtonType.YES, ButtonType.NO);
        confirma.initOwner(dono);
        vestirDialogo(confirma);
        confirma.showAndWait();
        return confirma.getResult() == ButtonType.YES;
    }
}
