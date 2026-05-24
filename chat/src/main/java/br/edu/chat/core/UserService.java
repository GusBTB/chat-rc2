package br.edu.chat.core;

import br.edu.chat.model.MessageDelivery;
import br.edu.chat.model.MessageType;
import br.edu.chat.model.User;
import br.edu.chat.model.UserStatus;
import br.edu.chat.protocol.ServerResponse;
import br.edu.chat.repository.MessageRepository;
import br.edu.chat.repository.UserRepository;
import br.edu.chat.util.PasswordUtils;

import java.security.SecureRandom;
import java.util.List;

public class UserService {

    private final UserRepository userRepo;
    private final MessageRepository messageRepo;
    private final MessageSender sender;

    public UserService(UserRepository userRepo, MessageRepository messageRepo, MessageSender sender) {
        this.userRepo = userRepo;
        this.messageRepo = messageRepo;
        this.sender = sender;
    }

    public String register(String fullName, String login, String email, String password) {
        if (fullName == null || fullName.isBlank()) {
            return ServerResponse.error("Nome completo invalido.");
        }
        if (login == null || login.isBlank()) {
            return ServerResponse.error("Login invalido.");
        }
        if (email == null || email.isBlank()) {
            return ServerResponse.error("Email invalido.");
        }
        if (password == null || password.isBlank()) {
            return ServerResponse.error("Senha invalida.");
        }
        if (userRepo.existsFullName(fullName)) {
            return ServerResponse.error("Nome completo '" + fullName + "' ja esta em uso.");
        }

        if (userRepo.existsLogin(login)) {
            return ServerResponse.error("Login '" + login + "' ja esta em uso.");
        }
        if (userRepo.existsEmail(email)) {
            return ServerResponse.error("Email '" + email + "' ja esta em uso.");
        }

        User user = new User(fullName, login, email, password, UserStatus.OFFLINE);
        int id = userRepo.create(user);

        if (id < 0) {
            return ServerResponse.error("Falha ao cadastrar usuario.");
        }

        return ServerResponse.ok("Cadastro realizado com sucesso. Bem-vindo, " + login + "!");
    }

    public User login(String login, String password) {
        User user = userRepo.findByLogin(login);

        if (user == null) {
            return null;
        }
        if (!PasswordUtils.verify(password, user.getPassword())) {
            return null;
        }

        userRepo.updateStatus(user.getId(), UserStatus.ONLINE);
        user.setStatus(UserStatus.ONLINE);
        return user;
    }

    public String logout(int userId) {
        userRepo.updateStatus(userId, UserStatus.OFFLINE);
        return ServerResponse.ok("Logout realizado com sucesso.");
    }

    public String listUsers() {
        List<User> users = userRepo.listAll();

        if (users.isEmpty()) {
            return ServerResponse.info("Nenhum usuario cadastrado.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(ServerResponse.info("Usuarios cadastrados:"));
        for (User u : users) {
            String statusLabel = u.getStatus() == UserStatus.ONLINE ? "[online]" : "[offline]";
            sb.append("\n  - ").append(u.getLogin()).append(" ").append(statusLabel);
        }

        return sb.toString();
    }

    public String listOnlineUsers() {
        List<User> users = userRepo.listOnline();

        if (users.isEmpty()) {
            return ServerResponse.info("Nenhum usuario online no momento.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(ServerResponse.info("Usuarios online:"));
        for (User u : users) {
            sb.append("\n  - ").append(u.getLogin());
        }

        return sb.toString();
    }

    public String recoverPassword(String email) {
        User user = userRepo.findByEmail(email);

        if (user == null) {
            return ServerResponse.error("Nenhum usuario encontrado com esse email.");
        }

        String temporaryPassword = generateTemporaryPassword();
        boolean updated = userRepo.updatePassword(user.getId(), temporaryPassword);

        if (!updated) {
            return ServerResponse.error("Nao foi possivel redefinir a senha.");
        }

        return ServerResponse.ok("Senha temporaria gerada para o login '" + user.getLogin()
                + "': " + temporaryPassword
                + "\nUse essa senha no proximo login e depois altere-a quando houver comando para troca de senha.");
    }

    public void deliverPendingMessages(User user) {
        List<MessageDelivery> pending = messageRepo.listPendingDeliveriesForUser(user.getId());

        for (MessageDelivery delivery : pending) {
            String formatted;
            if (delivery.getMessageType() == MessageType.DIRECT) {
                User from = userRepo.findById(delivery.getSenderUserId());
                String login = from != null ? from.getLogin() : "desconhecido";
                formatted = ServerResponse.directMessage(login, delivery.getCreatedAt(), delivery.getContent());
            } else {
                formatted = delivery.getContent();
            }

            sender.sendToUser(user.getLogin(), formatted);
            messageRepo.markDelivered(delivery.getDeliveryId());
        }
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            int index = random.nextInt(chars.length());
            password.append(chars.charAt(index));
        }

        return password.toString();
    }

}
