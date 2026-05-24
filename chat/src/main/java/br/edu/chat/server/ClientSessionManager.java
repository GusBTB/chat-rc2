package br.edu.chat.server;

import br.edu.chat.core.MessageSender;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientSessionManager implements MessageSender {

    private final Map<Integer, ClientHandler> onlineClientsById = new ConcurrentHashMap<>();
    private final Map<String, ClientHandler> onlineClientsByLogin = new ConcurrentHashMap<>();

    public void add(int userId, String login, ClientHandler handler) {
        if (login == null || handler == null) {
            return;
        }

        onlineClientsById.put(userId, handler);
        onlineClientsByLogin.put(normalizeLogin(login), handler);
    }

    public void remove(int userId, String login) {
        onlineClientsById.remove(userId);

        if (login != null) {
            onlineClientsByLogin.remove(normalizeLogin(login));
        }
    }

    public void remove(int userId) {
        ClientHandler handler = onlineClientsById.remove(userId);

        if (handler != null && handler.getLoggedUserLogin() != null) {
            onlineClientsByLogin.remove(normalizeLogin(handler.getLoggedUserLogin()), handler);
        }
    }

    public void removeIfSameHandler(int userId, String login, ClientHandler handler) {
        if (handler == null) {
            return;
        }

        onlineClientsById.remove(userId, handler);

        if (login != null) {
            onlineClientsByLogin.remove(normalizeLogin(login), handler);
        }
    }

    public boolean isConnected(int userId) {
        return onlineClientsById.containsKey(userId);
    }

    public boolean isConnected(String login) {
        if (login == null) {
            return false;
        }

        return onlineClientsByLogin.containsKey(normalizeLogin(login));
    }

    public boolean isCurrentHandler(int userId, String login, ClientHandler handler) {
        if (handler == null) {
            return false;
        }

        ClientHandler byId = onlineClientsById.get(userId);

        if (byId != handler) {
            return false;
        }

        if (login == null) {
            return true;
        }

        ClientHandler byLogin = onlineClientsByLogin.get(normalizeLogin(login));
        return byLogin == handler;
    }

    public void sendToUser(int userId, String message) {
        ClientHandler handler = onlineClientsById.get(userId);

        if (handler != null) {
            handler.send(message);
        }
    }

    @Override
    public void sendToUser(String userLogin, String message) {
        if (userLogin == null) {
            return;
        }

        ClientHandler handler = onlineClientsByLogin.get(normalizeLogin(userLogin));

        if (handler != null) {
            handler.send(message);
        }
    }

    @Override
    public boolean isOnline(String userLogin) {
        return isConnected(userLogin);
    }

    private String normalizeLogin(String login) {
        return login.trim().toLowerCase();
    }
}