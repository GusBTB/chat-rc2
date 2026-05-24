package br.edu.chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ConsoleInputThread implements Runnable {

    private final Socket socket;

    public ConsoleInputThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));
                PrintWriter output = new PrintWriter(socket.getOutputStream(), true)) {

            String line;

            while ((line = keyboard.readLine()) != null) {
                if ("saircliente".equalsIgnoreCase(line.trim())) {
                    output.println("logout");
                    output.flush();

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    System.out.println("Encerrando cliente...");
                    break;
                }

                output.println(line);
                System.out.print("> ");
            }

        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.out.println("Erro ao enviar comando para o servidor: " + e.getMessage());
            }
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Erro ao fechar socket do cliente: " + e.getMessage());
            }
        }
    }
}