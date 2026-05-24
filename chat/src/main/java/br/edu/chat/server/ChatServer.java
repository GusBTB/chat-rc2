package br.edu.chat.server;

import br.edu.chat.core.CommandProcessor;
import br.edu.chat.database.DatabaseInitializer;
import br.edu.chat.model.UserStatus;
import br.edu.chat.repository.UserRepository;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer {

    private final int port;
    private final ClientSessionManager sessionManager;
    private final CommandProcessor commandProcessor;

    public ChatServer(int port) {
        this.port = port;
        this.sessionManager = new ClientSessionManager();
        this.commandProcessor = new CommandProcessor(sessionManager);
    }

    public void start() throws IOException {
        DatabaseInitializer.initialize();
        new UserRepository().updateAllStatuses(UserStatus.OFFLINE);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor iniciado na porta " + port);
            System.out.println("Aguardando conexoes de clientes...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado: " + clientSocket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(clientSocket, sessionManager, commandProcessor);
                Thread clientThread = new Thread(handler);
                clientThread.start();
            }
        }
    }
}