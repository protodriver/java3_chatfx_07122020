package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

public class ClientHandler {
    Logger logger = Logger.getLogger(this.getClass().getName());
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private String nickname;
    private String login;
    private static ExecutorService service = Executors.newCachedThreadPool();

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            Future future = service.submit(() -> {
                try {
                    socket.setSoTimeout(120000);
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.startsWith("/reg ")) {
                                String[] token = str.split("\\s", 4);
                                boolean b = server.getAuthService()
                                        .registration(token[1], token[2], token[3]);
                                if (b) {
                                    sendMsg("/regok");
                                } else {
                                    sendMsg("/regno");
                                }
                            }
                            if (str.startsWith("/reNick ")) {
                                String[] token = str.split("\\s", 4);
                                boolean b = server.getAuthService()
                                        .reNickation(token[1], token[2], token[3]);
                                if (b) {
                                    sendMsg("/reNickOk");
                                } else {
                                    sendMsg("/reNickNo");
                                }
                            }

                            if (str.startsWith("/auth ")) {
                                String[] token = str.split("\\s", 3);
                                String newNick = server.getAuthService()
                                        .getNicknameByLoginAndPassword(token[1], token[2]);
                                if (newNick != null) {
                                    login = token[1];
                                    if (!server.isloginAuthenticated(login)) {
                                        nickname = newNick;
                                        out.writeUTF("/authok " + nickname);
                                        server.subscribe(this);
                                        socket.setSoTimeout(0);
                                        logger.info("Подключился юзер " + nickname);
                                        break;
                                    } else {
                                        out.writeUTF("Учетная запись уже используется");
                                    }
                                } else {
                                    out.writeUTF("Неверный логин / пароль");
                                }
                            }
                        }
                    }

                    //Цикл работы
                    while (true) {
                        String str = in.readUTF();
                        logger.info(nickname + " что-то написал");

                        if (str.startsWith("/")) {
                            if (str.startsWith("/w")) {
                                String[] token = str.split("\\s+", 3);
                                if (token.length < 3) {
                                    continue;
                                }
                                server.privateMsg(this, token[1], token[2]);
                            }

                            if (str.equals("/end")) {
                                out.writeUTF("/end");
                                break;
                            }
                        } else {
                            server.broadcastMsg(this, str);
                        }
                    }
                } catch (SocketTimeoutException e){
                    sendMsg("/end");
                } catch (IOException e) {
                    logger.info(nickname + "некая ошибка случилась " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    System.out.println("Client disconnected!");
                    server.unsubscribe(this);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getNickname() {
        return nickname;
    }

    public String getLogin() {
        return login;
    }
}
