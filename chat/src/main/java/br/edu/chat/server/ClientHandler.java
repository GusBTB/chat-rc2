package br.edu.chat.server;

import br.edu.chat.core.CommandProcessor;
import br.edu.chat.model.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ClientSessionManager sessionManager;
    private final CommandProcessor commandProcessor;

    private BufferedReader input;
    private PrintWriter output;
    private Integer loggedUserId;
    private String loggedUserLogin;

    private volatile boolean disconnected;

    public ClientHandler(Socket socket, ClientSessionManager sessionManager, CommandProcessor commandProcessor) {
        this.socket = socket;
        this.sessionManager = sessionManager;
        this.commandProcessor = commandProcessor;
    }

    @Override
    public void run() {
        try {
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);

            send("Bem-vindo ao Chat TCP.");
            send("Digite ajuda para ver os comandos disponiveis.");

            String line;
            while ((line = input.readLine()) != null) {
                String sanitizedLine = sanitizeInputLine(line);

                List<String> responses = commandProcessor.process(sanitizedLine, this);

                for (String response : responses) {
                    send(response);
                }
            }

        } catch (IOException e) {
            System.out.println("Cliente desconectado: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    public synchronized void send(String message) {
        if (output == null || message == null) {
            return;
        }

        String normalizedMessage = message.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalizedMessage.split("\n", -1);

        for (String line : lines) {
            output.println(line);
        }

        if (output.checkError()) {
            disconnect();
        }
    }

    private String sanitizeInputLine(String line) {
        if (line == null) {
            return null;
        }

        StringBuilder cleaned = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '\b' || c == 127) {
                if (cleaned.length() > 0) {
                    cleaned.deleteCharAt(cleaned.length() - 1);
                }
                continue;
            }

            if (Character.isISOControl(c)) {
                continue;
            }

            cleaned.append(c);
        }

        return cleaned.toString().trim();
    }

    public boolean isLoggedIn() {
        return loggedUserId != null;
    }

    public Integer getLoggedUserId() {
        return loggedUserId;
    }

    public String getLoggedUserLogin() {
        return loggedUserLogin;
    }

    public void setLoggedUser(User user) {
        if (user == null) {
            clearLoggedUser();
            return;
        }

        this.loggedUserId = user.getId();
        this.loggedUserLogin = user.getLogin();
    }

    public void clearLoggedUser() {
        this.loggedUserId = null;
        this.loggedUserLogin = null;
    }

    public void closeConnection() {
        disconnect();
    }

    private synchronized void disconnect() {
        if (disconnected) {
            return;
        }

        disconnected = true;

        commandProcessor.disconnect(this);

        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Erro ao fechar socket do cliente: " + e.getMessage());
        }
    }
}