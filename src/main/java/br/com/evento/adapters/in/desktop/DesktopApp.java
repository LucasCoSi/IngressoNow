package br.com.evento.adapters.in.desktop;

import br.com.evento.domain.model.entity.Evento;
import br.com.evento.domain.model.entity.Inscricao;
import br.com.evento.domain.model.entity.Usuario;
import br.com.evento.infra.FabricaAplicacao;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * D-01 — desktop JavaFX (código puro, sem FXML).
 * Mesmo banco + mesmos use cases da API via FabricaAplicacao (RN-19).
 * Fluxo: login → eventos publicados → detalhe (atividades + pessoas) →
 * inscrever / check-in manual.
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

    private void mostrarLogin() {
        var email = new TextField();
        email.setPromptText("e-mail");
        var senha = new PasswordField();
        senha.setPromptText("senha");
        var entrar = new Button("Entrar");
        var msg = new Label();
        entrar.setOnAction(e -> {
            try {
                logado = app.autenticarUsuario.executar(email.getText(), senha.getText());
                mostrarEventos();
            } catch (Exception ex) {
                msg.setText("Falha: " + ex.getMessage());
            }
        });
        var grade = new GridPane();
        grade.setPadding(new Insets(24));
        grade.setVgap(8);
        grade.setHgap(8);
        grade.add(new Label("E-mail:"), 0, 0);
        grade.add(email, 1, 0);
        grade.add(new Label("Senha:"), 0, 1);
        grade.add(senha, 1, 1);
        grade.add(entrar, 1, 2);
        grade.add(msg, 1, 3);
        palco.setScene(new Scene(grade, 380, 220));
    }

    private void mostrarEventos() {
        var lista = new ListView<>(FXCollections.observableArrayList(
                app.listarEventosPublicados.executar()));
        lista.setCellFactory(v -> new ListCell<>() {
            @Override
            protected void updateItem(Evento e, boolean vazio) {
                super.updateItem(e, vazio);
                setText(vazio || e == null ? null
                        : e.getTitulo() + " [" + e.getStatus() + "]");
            }
        });
        var abrir = new Button("Abrir");
        abrir.setOnAction(e -> {
            Evento sel = lista.getSelectionModel().getSelectedItem();
            if (sel != null) mostrarDetalhe(sel.getId());
        });
        var topo = new HBox(8, new Label("Logado: " + logado.getNome()), abrir);
        topo.setPadding(new Insets(8));
        var raiz = new BorderPane(lista, topo, null, null, null);
        palco.setScene(new Scene(raiz, 620, 420));
    }

    private void mostrarDetalhe(String eventoId) {
        Evento evento = app.eventos.buscarPorId(eventoId).orElseThrow();
        var linhas = new VBox(4);
        linhas.setPadding(new Insets(12));
        linhas.getChildren().add(new Label(
                evento.getTitulo() + " — " + evento.getAtividades().size() + " atividade(s)"));
        for (var a : evento.getAtividades()) {
            var pessoas = app.pessoas.buscarPorAtividade(a.getId());
            String elenco = pessoas.isEmpty() ? "—"
                    : pessoas.stream()
                            .map(p -> p.getNome() + " (" + p.getPapel() + ")")
                            .reduce((x, y) -> x + ", " + y).orElse("—");
            linhas.getChildren().add(new Label("• " + a.getTitulo()
                    + " | " + a.getPeriodo().getInicio() + " | " + a.getLocal()
                    + (a.getTipo() != null ? " | " + a.getTipo() : "")
                    + (a.getTrilha() != null ? " [" + a.getTrilha() + "]" : "")
                    + "\n    " + elenco));
        }
        var msg = new Label();
        var inscrever = new Button("Inscrever-me");
        inscrever.setOnAction(e -> {
            try {
                Inscricao i = app.inscreverParticipante.executar(logado.getId(), eventoId);
                msg.setText("Inscrito! ID: " + i.getId().substring(0, 8));
            } catch (Exception ex) {
                msg.setText("Falha: " + ex.getMessage());
            }
        });
        var voltar = new Button("Voltar");
        voltar.setOnAction(e -> mostrarEventos());
        var botoes = new HBox(8, inscrever, voltar, msg);
        botoes.setPadding(new Insets(8));
        var raiz = new BorderPane(linhas, null, null, botoes, null);
        palco.setScene(new Scene(raiz, 680, 480));
        // Check-in manual (RF-22) fica na tela de frequência do operador;
        // aqui o participante só se inscreve e consulta a grade.
    }

    public static void abrir(String[] args) {
        launch(args);
    }

    private void alerta(String texto) {
        var a = new Alert(Alert.AlertType.INFORMATION, texto);
        a.showAndWait();
    }
}
