package br.edu.chat.client;

import java.io.IOException;
import java.net.Socket;

public class ChatClient {

    private final String host;
    private final int port;

    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() throws IOException {
        Socket socket = new Socket(host, port);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (!socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.out.println("Erro ao fechar conexao no encerramento do cliente: " + e.getMessage());
            }
        }));

        System.out.println("Cliente conectado ao servidor " + host + ":" + port);
        System.out.println("Digite comandos para enviar ao servidor.");
        System.out.println("Digite saircliente para encerrar apenas o cliente.");
        System.out.print("> ");

        Thread listenerThread = new Thread(new ServerListenerThread(socket));
        Thread inputThread = new Thread(new ConsoleInputThread(socket));

        listenerThread.start();
        inputThread.start();

        try {
            inputThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Cliente interrompido.");
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Erro ao fechar conexao do cliente: " + e.getMessage());
            }
        }
    }
}