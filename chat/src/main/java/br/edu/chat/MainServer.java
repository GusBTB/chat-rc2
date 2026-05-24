package br.edu.chat;

import br.edu.chat.server.ChatServer;

public class MainServer {

    public static void main(String[] args) {
        int port = 6789;

        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Porta invalida. Usando porta padrao 6789.");
            }
        }

        try {
            new ChatServer(port).start();
        } catch (Exception e) {
            System.out.println("Erro ao iniciar servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
}