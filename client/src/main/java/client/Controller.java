package client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.ResourceBundle;
import java.util.stream.Stream;

public class Controller implements Initializable {
    @FXML
    private TextArea textArea;
    @FXML
    private TextField textField;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private HBox authPanel;
    @FXML
    private HBox msgPanel;
    @FXML
    private ListView<String> clientList;

    private Socket socket;
    private final String IP_ADDRESS = "localhost";
    private final int PORT = 8189;

    private DataInputStream in;
    private DataOutputStream out;

    private boolean authenticated;
    private String nickname;
    private Stage stage;
    private Stage regStage;
    private Stage reNickStage;
    private RegController regController;
    private RegController reNickController;

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        msgPanel.setManaged(authenticated);
        msgPanel.setVisible(authenticated);
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        clientList.setManaged(authenticated);
        clientList.setVisible(authenticated);
        if (!authenticated) {
            nickname = "";
        }
        setTitle(nickname);
        textArea.clear();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        createRegWindow();
        createReNickWindow();
        Platform.runLater(() -> {
            stage = (Stage) textField.getScene().getWindow();
            stage.setOnCloseRequest(event -> {
                System.out.println("bye");
                if (socket != null && !socket.isClosed()) {
                    try {
                        out.writeUTF("/end");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        });
        setAuthenticated(false);
    }

    private void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(() -> {
                try {
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.equals("/regok")) {
                                regController.addMessage("Регистрация прошла успешно");
                            }
                            if (str.equals("/regno")) {
                                regController.addMessage("Регистрация не получилась\n" +
                                        "Возможно предложенные лоин или никнейм уже заняты");
                            }
                            if (str.equals("/reNickOk")) {
                                reNickController.addMessage("Ник изменен");
                            }
                            if (str.equals("/reNickNo")) {
                                reNickController.addMessage("Ник не изменен\n" +
                                        "Возможно никнейм уже занят, или ошибка пары логин-пароль");
                            }


                            if (str.startsWith("/authok ")) {
                                nickname = str.split("\\s")[1];
                                setAuthenticated(true);
                                break;
                            }

                            if(str.equals("/end")){
                                throw new RuntimeException("Сервер нас вырубил по таймауту");
                            }

                        } else {
                            textArea.appendText(str + "\n");
                        }
                    }

                    InputStreamReader inputLog = new InputStreamReader(new FileInputStream("log_" + nickname + ".txt"), StandardCharsets.UTF_8);
                    BufferedReader reader = new BufferedReader(inputLog);
                    ArrayList<String> arr = new ArrayList<>();
                    while (reader.ready()) {
                        arr.add(reader.readLine() + "\n");
                    }
                    ListIterator<String> it = arr.listIterator(arr.size());
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; it.hasPrevious() && i < 100; i++) {
                        sb.insert(0, it.previous());
                    }
                    textArea.appendText(sb.toString());
                    //Цикл работы
                    while (true) {
                        String str=in.readUTF();
                        if (str.startsWith("/")) {
                            if (str.startsWith("/clientlist ")) {
                                String[] token = str.split("\\s");
                                Platform.runLater(() -> {
                                    clientList.getItems().clear();
                                    for (int i = 1; i < token.length; i++) {
                                        clientList.getItems().add(token[i]);
                                    }
                                });
                            }
                            if (str.equals("/end")) {
                                break;
                            }
                        } else {
                            textArea.appendText(str + "\n");
                        }
                    }
                } catch (RuntimeException e) {
                    System.out.println(e.getMessage());
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        FileOutputStream fileLog = new FileOutputStream("log_" + nickname + ".txt", true);
                        fileLog.write(textArea.getText().getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    setAuthenticated(false);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void sendMsg() {
        try {
            out.writeUTF(textField.getText());
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String d = "";
    }

    @FXML
    public void tryToAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }

        String msg = String.format("/auth %s %s", loginField.getText().trim(), passwordField.getText().trim());
        try {
            out.writeUTF(msg);
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setTitle(String username) {
        String title = String.format("СпэйсЧат [ %s ]", username);
        if (username.equals("")) {
            title = "СпэйсЧат";
        }
        String chatTitle = title;
        Platform.runLater(() -> {
            stage.setTitle(chatTitle);
        });
    }

    @FXML
    public void clickClientlist(MouseEvent mouseEvent) {
        String msg = String.format("/w %s ", clientList.getSelectionModel().getSelectedItem());
        textField.setText(msg);
    }

    private void createRegWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reg.fxml"));
            Parent root = fxmlLoader.load();
            regStage = new Stage();
            regStage.setTitle("СпэйсЧат Регистрация");
            regStage.setScene(new Scene(root, 350, 300));
            regStage.initModality(Modality.APPLICATION_MODAL);

            regController = fxmlLoader.getController();
            regController.setController(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void createReNickWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/reNick.fxml"));
            Parent root = fxmlLoader.load();
            reNickStage = new Stage();
            reNickStage.setTitle("СпэйсЧат переназваться");
            reNickStage.setScene(new Scene(root, 350, 300));
            reNickStage.initModality(Modality.APPLICATION_MODAL);

            reNickController = fxmlLoader.getController();
            reNickController.setController(this);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void showRegWindow(ActionEvent actionEvent) {
        regStage.show();
    }
    @FXML
    public void showReNickWindow(ActionEvent actionEvent) {
        reNickStage.show();
    }

    public void tryToReg(String login, String password, String nickname) {
        String msg = String.format("/reg %s %s %s", login, password, nickname);

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void tryToReNick(String login, String password, String nickname) {
        String msg = String.format("/reNick %s %s %s", login, password, nickname);

        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
