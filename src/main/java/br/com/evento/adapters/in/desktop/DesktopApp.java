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
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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
        var email = new TextField();
        email.setPromptText("e-mail");
        var senha = new PasswordField();
        senha.setPromptText("senha");
        var entrar = new Button("Entrar");
        var irCadastro = new Button("Cadastrar");
        var msg = new Label();
        entrar.setOnAction(e -> {
            try {
                logado = app.autenticarUsuario.executar(email.getText(), senha.getText());
                mostrarPrincipal();
            } catch (Exception ex) {
                msg.setText("Falha: " + mensagem(ex));
            }
        });
        irCadastro.setOnAction(e -> mostrarCadastro());
        var grade = new GridPane();
        grade.setPadding(new Insets(24));
        grade.setVgap(8);
        grade.setHgap(8);
        grade.add(new Label("E-mail:"), 0, 0);
        grade.add(email, 1, 0);
        grade.add(new Label("Senha:"), 0, 1);
        grade.add(senha, 1, 1);
        grade.add(entrar, 1, 2);
        grade.add(irCadastro, 1, 3);
        grade.add(msg, 1, 4);
        palco.setScene(new Scene(grade, 380, 260));
    }

    private void mostrarCadastro() {
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
        palco.setScene(new Scene(grade, 420, 340));
    }

    private void mostrarPrincipal() {
        if (logado == null) {
            mostrarLogin();
            return;
        }
        boolean org = isOrganizador();
        var titulo = new Label("Olá, " + logado.getNome() + " (" + papel() + ")");
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        var botoes = new VBox(8);
        botoes.setPadding(new Insets(16));
        botoes.getChildren().add(titulo);
        if (org) {
            var gestao = new Button("Meus eventos (gestão)");
            gestao.setOnAction(e -> mostrarGestao());
            var novo = new Button("Novo evento (RASCUNHO)");
            novo.setOnAction(e -> dialogoEvento(null));
            botoes.getChildren().addAll(gestao, novo);
        }
        var explorar = new Button("Explorar publicados");
        explorar.setOnAction(e -> mostrarExplorar());
        var sair = new Button("Sair");
        sair.setOnAction(e -> mostrarLogin());
        botoes.getChildren().addAll(explorar, sair);
        var msg = new Label(org
                ? "Gestão: só os próprios eventos (isolamento por dono, RN-18)."
                : "Visão do participante: explore e inscreva-se.");
        botoes.getChildren().add(msg);
        palco.setScene(new Scene(botoes, 480, 340));
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
        var abrir = new Button("Abrir");
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
        excluir.setOnAction(e -> {
            Evento sel = lista.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            var confirma = new Alert(Alert.AlertType.CONFIRMATION,
                    "Excluir \"" + sel.getTitulo() + "\"? (bloqueado se houver inscrições)",
                    ButtonType.YES, ButtonType.NO);
            confirma.showAndWait();
            if (confirma.getResult() != ButtonType.YES) return;
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
        var topo = new HBox(8, new Label("Meus eventos — " + logado.getNome()), voltar);
        topo.setPadding(new Insets(8));
        var barra = new HBox(8, abrir, novo, editar, excluir, publicar, encerrar);
        barra.setPadding(new Insets(8));
        var raiz = new BorderPane(lista, topo, null, barra, null);
        palco.setScene(new Scene(raiz, 720, 460));
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
        var salvar = new Button(atual != null ? "Salvar" : "Criar RASCUNHO");
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
        grade.setPadding(new Insets(16));
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
        palco.setScene(new Scene(grade, 560, 620));
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
        var abrir = new Button("Abrir");
        abrir.setOnAction(e -> {
            Evento sel = lista.getSelectionModel().getSelectedItem();
            if (sel != null) mostrarDetalhe(sel.getId(), false);
        });
        var voltar = new Button("Voltar");
        voltar.setOnAction(e -> mostrarPrincipal());
        var topo = new HBox(8, new Label("Publicados"), voltar);
        topo.setPadding(new Insets(8));
        var barra = new HBox(8, abrir);
        barra.setPadding(new Insets(8));
        palco.setScene(new Scene(new BorderPane(lista, topo, null, barra, null), 620, 420));
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
            var barraGestao = new HBox(8, nova, remarcar, criterio, pessoasBtn, freqBtn);
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
        palco.setScene(new Scene(new BorderPane(linhas, null, null, botoes, null), 680, 480));
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
        palco.setScene(new Scene(grade, 540, 560));
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
        palco.setScene(new Scene(caixa, 420, 280));
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
        palco.setScene(new Scene(caixa, 640, 480));
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
        palco.setScene(new Scene(caixa, 680, 640));
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
        a.showAndWait();
    }
}
