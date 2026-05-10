package br.edu.chat.testmanual;

import br.edu.chat.database.DatabaseInitializer;
import br.edu.chat.model.Group;
import br.edu.chat.model.Message;
import br.edu.chat.model.MessageDelivery;
import br.edu.chat.model.MessageType;
import br.edu.chat.model.User;
import br.edu.chat.model.UserStatus;
import br.edu.chat.repository.BlockRepository;
import br.edu.chat.repository.GroupRepository;
import br.edu.chat.repository.MessageRepository;
import br.edu.chat.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

public class RepositoryTestMain {

    public static void main(String[] args) {
        DatabaseInitializer.initialize();

        UserRepository userRepository = new UserRepository();
        GroupRepository groupRepository = new GroupRepository();
        BlockRepository blockRepository = new BlockRepository();
        MessageRepository messageRepository = new MessageRepository();

        System.out.println();
        System.out.println("===== TESTE DOS REPOSITORIOS =====");

        User augusto = getOrCreateUser(
                userRepository,
                "Augusto Silva",
                "augusto",
                "augusto@email.com",
                "123");

        User robson = getOrCreateUser(
                userRepository,
                "Robson Souza",
                "robson",
                "robson@email.com",
                "123");

        System.out.println();
        System.out.println("Usuarios criados ou encontrados:");
        System.out.println(augusto);
        System.out.println(robson);

        System.out.println();
        System.out.println("Buscando usuario por login:");
        User userByLogin = userRepository.findByLogin("augusto");
        System.out.println(userByLogin);

        System.out.println();
        System.out.println("Alterando status de Augusto para ONLINE:");
        boolean statusUpdated = userRepository.updateStatus(augusto.getId(), UserStatus.ONLINE);
        System.out.println("Status atualizado? " + statusUpdated);

        User augustoUpdated = userRepository.findByLogin("augusto");
        System.out.println(augustoUpdated);

        System.out.println();
        System.out.println("Usuarios online:");
        List<User> onlineUsers = userRepository.listOnline();
        for (User user : onlineUsers) {
            System.out.println(user);
        }

        System.out.println();
        System.out.println("Criando ou buscando grupo amigos:");
        Group grupoAmigos = getOrCreateGroup(groupRepository, "amigos", augusto.getId());
        System.out.println(grupoAmigos);

        System.out.println();
        System.out.println("Inserindo Augusto como administrador do grupo:");
        if (!groupRepository.isMember(grupoAmigos.getId(), augusto.getId())) {
            boolean added = groupRepository.addMember(grupoAmigos.getId(), augusto.getId(), true);
            System.out.println("Augusto inserido no grupo? " + added);
        } else {
            System.out.println("Augusto ja era membro do grupo.");
        }

        System.out.println();
        System.out.println("Augusto eh admin?");
        System.out.println(groupRepository.isAdmin(grupoAmigos.getId(), augusto.getId()));

        System.out.println();
        System.out.println("Listando grupos:");
        List<Group> groups = groupRepository.listGroups();
        for (Group group : groups) {
            System.out.println(group);
        }

        System.out.println();
        System.out.println("Listando membros do grupo amigos:");
        List<User> members = groupRepository.listMembers(grupoAmigos.getId());
        for (User member : members) {
            System.out.println(member);
        }

        System.out.println();
        System.out.println("Criando bloqueio entre Augusto e Robson:");
        if (!blockRepository.isBlockedBetween(augusto.getId(), robson.getId())) {
            boolean blocked = blockRepository.blockUser(augusto.getId(), robson.getId());
            System.out.println("Bloqueio criado? " + blocked);
        } else {
            System.out.println("Ja existe bloqueio entre Augusto e Robson.");
        }

        System.out.println();
        System.out.println("Existe bloqueio entre Augusto e Robson?");
        System.out.println(blockRepository.isBlockedBetween(augusto.getId(), robson.getId()));

        System.out.println();
        System.out.println("Existe bloqueio entre Robson e Augusto?");
        System.out.println(blockRepository.isBlockedBetween(robson.getId(), augusto.getId()));

        System.out.println();
        System.out.println("Salvando mensagem pendente de Augusto para Robson:");

        Message message = new Message(
                augusto.getId(),
                robson.getId(),
                null,
                "Mensagem pendente de teste",
                MessageType.DIRECT,
                LocalDateTime.now().toString());

        int messageId = messageRepository.saveMessage(message);
        System.out.println("ID da mensagem salva: " + messageId);

        if (messageId != -1) {
            int deliveryId = messageRepository.saveDelivery(messageId, robson.getId(), false);
            System.out.println("ID da entrega pendente: " + deliveryId);
        }

        System.out.println();
        System.out.println("Consultando mensagens pendentes de Robson:");
        List<MessageDelivery> pendingDeliveries = messageRepository.listPendingDeliveriesForUser(robson.getId());

        for (MessageDelivery delivery : pendingDeliveries) {
            System.out.println(delivery);
        }

        if (!pendingDeliveries.isEmpty()) {
            System.out.println();
            System.out.println("Marcando primeira entrega pendente como entregue:");
            MessageDelivery firstDelivery = pendingDeliveries.get(0);
            boolean marked = messageRepository.markDelivered(firstDelivery.getDeliveryId());
            System.out.println("Marcada como entregue? " + marked);
        }

        System.out.println();
        System.out.println("Mensagens pendentes de Robson apos marcar uma como entregue:");
        List<MessageDelivery> pendingAfterUpdate = messageRepository.listPendingDeliveriesForUser(robson.getId());

        for (MessageDelivery delivery : pendingAfterUpdate) {
            System.out.println(delivery);
        }

        System.out.println();
        System.out.println("===== FIM DO TESTE =====");
    }

    private static User getOrCreateUser(UserRepository userRepository,
            String fullName,
            String login,
            String email,
            String password) {
        User existingUser = userRepository.findByLogin(login);

        if (existingUser != null) {
            return existingUser;
        }

        User newUser = new User(
                fullName,
                login,
                email,
                password,
                UserStatus.OFFLINE);

        int id = userRepository.create(newUser);

        if (id == -1) {
            throw new RuntimeException("Nao foi possivel criar usuario: " + login);
        }

        return userRepository.findById(id);
    }

    private static Group getOrCreateGroup(GroupRepository groupRepository,
            String groupName,
            int creatorId) {
        Group existingGroup = groupRepository.findByName(groupName);

        if (existingGroup != null) {
            return existingGroup;
        }

        int groupId = groupRepository.createGroup(groupName, creatorId);

        if (groupId == -1) {
            throw new RuntimeException("Nao foi possivel criar grupo: " + groupName);
        }

        return groupRepository.findByName(groupName);
    }
}