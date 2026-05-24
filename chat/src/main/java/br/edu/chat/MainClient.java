package br.edu.chat;

import br.edu.chat.client.ChatClient;

public class MainClient {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 6789;

        if (args.length > 0) {
            host = args[0];
        }

        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Porta invalida. Usando porta padrao 6789.");
            }
        }

        try {
            new ChatClient(host, port).start();
        } catch (Exception e) {
            System.out.println("Erro ao iniciar cliente: " + e.getMessage());
        }
    }
}