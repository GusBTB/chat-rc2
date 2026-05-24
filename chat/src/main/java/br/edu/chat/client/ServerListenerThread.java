package br.edu.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ServerListenerThread implements Runnable {

    private final Socket socket;

    public ServerListenerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;

            while ((line = input.readLine()) != null) {
                System.out.println();
                System.out.println(line);
                System.out.print("> ");
            }

        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.out.println();
                System.out.println("Conexao com servidor encerrada: " + e.getMessage());
                System.out.print("> ");
            }
        }
    }
}