package server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SimpleAuthService implements AuthService {
    private class UserData {
        String login;
        String password;
        String nickname;

        public UserData(String login, String password, String nickname) {
            this.login = login;
            this.password = password;
            this.nickname = nickname;
        }
    }

    private static Connection connection;
    private static Statement statement;
    public static void connect() throws ClassNotFoundException, SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:chat.db");
        statement = connection.createStatement();
    }
    private List<UserData> users;
    public SimpleAuthService() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        users = new ArrayList<>();
        users.add(new UserData("qwe", "qwe", "qwe"));
        users.add(new UserData("asd", "asd", "asd"));
        users.add(new UserData("zxc", "zxc", "zxc"));
        for (int i = 1; i <= 10; i++) {
            users.add(new UserData("login" + i, "pass" + i, "nick" + i));
        }
    }

    private void disconnect() {
        try {
            statement.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        try {
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    @Override
    public String getNicknameByLoginAndPassword(String login, String password) {
        String result = "";
        try {
            connect();
            ResultSet res = statement.executeQuery(
                    "SELECT nick FROM users WHERE login = \"" + login +"\" AND password = \"" + password + "\"");
            result = res.getString("nick");
            return result;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            disconnect();
        }
        return null;
//        for (UserData user : users) {
//            if(user.login.equals(login) && user.password.equals(password)){
//                return user.nickname;
//            }
//        }
//        return null;
    }

    @Override
    public boolean registration(String login, String password, String nickname) {
        int res = -1;
        try {
            connect();
            res = statement.executeUpdate("insert into  users (login, password, nick) values (\"" + login
                    + "\", \"" + password + "\", \"" + nickname + "\");");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            disconnect();
        }
        return res > 0;
//        for (UserData user : users) {
//            if(user.login.equals(login) || user.nickname.equals(nickname)){
//                return false;
//            }
//        }
//
//        users.add(new UserData(login, password, nickname));
//        return true;
    }
    @Override
    public boolean reNickation(String login, String password, String nickname) {
        int res = -1;
        try {
            connect();
            res = statement.executeUpdate("UPDATE users SET nick = \"" + nickname + "\" " +
                    "WHERE login = \"" + login + "\" and password = \"" + password + "\";");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            disconnect();
        }
        return res > 0;
    }
}
