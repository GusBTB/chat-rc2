package br.edu.chat.core;

import br.edu.chat.model.Group;
import br.edu.chat.model.Message;
import br.edu.chat.model.MessageType;
import br.edu.chat.model.PendingRequest;
import br.edu.chat.model.RequestStatus;
import br.edu.chat.model.RequestType;
import br.edu.chat.model.User;
import br.edu.chat.protocol.ServerResponse;
import br.edu.chat.repository.BlockRepository;
import br.edu.chat.repository.DirectPermissionRepository;
import br.edu.chat.repository.GroupRepository;
import br.edu.chat.repository.MessageRepository;
import br.edu.chat.repository.RequestRepository;
import br.edu.chat.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

public class MessageService {

    private final MessageRepository messageRepo;
    private final UserRepository userRepo;
    private final GroupRepository groupRepo;
    private final BlockRepository blockRepo;
    private final DirectPermissionRepository permRepo;
    private final RequestRepository requestRepo;
    private final MessageSender sender;

    public MessageService(MessageRepository messageRepo, UserRepository userRepo,
            GroupRepository groupRepo, BlockRepository blockRepo,
            DirectPermissionRepository permRepo, RequestRepository requestRepo,
            MessageSender sender) {
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.groupRepo = groupRepo;
        this.blockRepo = blockRepo;
        this.permRepo = permRepo;
        this.requestRepo = requestRepo;
        this.sender = sender;
    }

    public String sendDirect(int senderId, String targetLogin, String content) {
        User target = userRepo.findByLogin(targetLogin);
        if (target == null) {
            return ServerResponse.error("Usuario '" + targetLogin + "' nao encontrado.");
        }

        if (senderId == target.getId()) {
            return ServerResponse.error("Voce nao pode enviar mensagem direta para si mesmo.");
        }

        if (blockRepo.isBlockedBetween(senderId, target.getId())) {
            return ServerResponse.error(
                    "Nao e possivel enviar mensagem para '" + targetLogin + "'. Existe bloqueio entre os usuarios.");
        }

        if (!permRepo.hasPermission(senderId, target.getId())) {
            if (requestRepo.existsPending(RequestType.PERMISSION, senderId, target.getId(), null)) {
                return ServerResponse.info("Ja existe um pedido de permissao pendente para '" + targetLogin
                        + "'. A mensagem ainda nao foi enviada.");
            }

            String now = LocalDateTime.now().toString();
            Message pendingMessage = new Message(senderId, target.getId(), null, content, MessageType.DIRECT, now);
            int pendingMessageId = messageRepo.saveMessage(pendingMessage);

            if (pendingMessageId < 0) {
                return ServerResponse.error("Falha ao registrar mensagem pendente de aceite.");
            }

            PendingRequest req = new PendingRequest(
                    RequestType.PERMISSION,
                    senderId,
                    target.getId(),
                    null,
                    pendingMessageId,
                    RequestStatus.PENDING,
                    now);

            int reqId = requestRepo.save(req);
            if (reqId < 0) {
                return ServerResponse.error("Falha ao criar pedido de permissao.");
            }

            User senderUser = userRepo.findById(senderId);
            String senderLogin = senderUser != null ? senderUser.getLogin() : "desconhecido";

            String notification = ServerResponse.privateRequest(reqId,
                    senderLogin + " quer enviar uma mensagem direta para voce. Use 'aceitar " + reqId
                            + "' ou 'recusar " + reqId + "'.");

            if (sender.isOnline(targetLogin)) {
                sender.sendToUser(targetLogin, notification);
            }

            return ServerResponse.info("Pedido de permissao enviado para '" + targetLogin
                    + "'. A mensagem sera enviada apos aceite.");
        }

        String now = LocalDateTime.now().toString();
        Message msg = new Message(senderId, target.getId(), null, content, MessageType.DIRECT, now);
        int msgId = messageRepo.saveMessage(msg);
        if (msgId < 0) {
            return ServerResponse.error("Falha ao enviar mensagem.");
        }

        if (sender.isOnline(targetLogin)) {
            User senderUser = userRepo.findById(senderId);
            String senderLogin = senderUser != null ? senderUser.getLogin() : "desconhecido";
            sender.sendToUser(targetLogin, ServerResponse.directMessage(senderLogin, now, content));
            messageRepo.saveDelivery(msgId, target.getId(), true);
            return ServerResponse
                    .ok("Mensagem enviada para '" + targetLogin + "'. Recebeu agora: " + targetLogin + ".");
        }

        messageRepo.saveDelivery(msgId, target.getId(), false);
        return ServerResponse.ok("Mensagem salva para '" + targetLogin + "'. O usuario recebera quando ficar online.");
    }

    public String sendGroup(int senderId, String groupName, String content) {
        Group group = groupRepo.findByName(groupName);
        if (group == null) {
            return ServerResponse.error("Grupo '" + groupName + "' nao encontrado.");
        }

        if (!groupRepo.isMember(group.getId(), senderId)) {
            return ServerResponse.error("Voce nao e membro do grupo '" + groupName + "'.");
        }

        String now = LocalDateTime.now().toString();
        Message msg = new Message(senderId, null, group.getId(), content, MessageType.GROUP, now);
        int msgId = messageRepo.saveMessage(msg);
        if (msgId < 0) {
            return ServerResponse.error("Falha ao enviar mensagem para o grupo.");
        }

        User senderUser = userRepo.findById(senderId);
        String senderLogin = senderUser != null ? senderUser.getLogin() : "desconhecido";
        String formatted = ServerResponse.groupMessage(groupName, senderLogin, now, content);

        int deliveredNow = 0;
        int deliveredLater = 0;
        int blocked = 0;

        List<User> members = groupRepo.listMembers(group.getId());
        for (User member : members) {
            if (member.getId() == senderId) {
                continue;
            }

            if (blockRepo.isBlockedBetween(senderId, member.getId())) {
                blocked++;
                continue;
            }

            if (sender.isOnline(member.getLogin())) {
                sender.sendToUser(member.getLogin(), formatted);
                messageRepo.saveDelivery(msgId, member.getId(), true);
                deliveredNow++;
            } else {
                messageRepo.saveDelivery(msgId, member.getId(), false);
                deliveredLater++;
            }
        }

        return ServerResponse.ok("Mensagem enviada para o grupo '" + groupName + "'. Receberam agora: "
                + deliveredNow + ". Receberao depois: " + deliveredLater + ". Bloqueados/nao entregues: " + blocked
                + ".");
    }

    public String sendGroupDirect(int senderId, String groupName, List<String> targetLogins, String content) {
        Group group = groupRepo.findByName(groupName);
        if (group == null) {
            return ServerResponse.error("Grupo '" + groupName + "' nao encontrado.");
        }

        if (!groupRepo.isMember(group.getId(), senderId)) {
            return ServerResponse.error("Voce nao e membro do grupo '" + groupName + "'.");
        }

        String now = LocalDateTime.now().toString();
        User senderUser = userRepo.findById(senderId);
        String senderLogin = senderUser != null ? senderUser.getLogin() : "desconhecido";
        String formatted = ServerResponse.groupDirectMessage(groupName, senderLogin, now, content);

        int deliveredNow = 0;
        int deliveredLater = 0;
        int invalid = 0;
        int blocked = 0;

        for (String targetLogin : targetLogins) {
            User target = userRepo.findByLogin(targetLogin);
            if (target == null || !groupRepo.isMember(group.getId(), target.getId()) || target.getId() == senderId) {
                invalid++;
                continue;
            }

            if (blockRepo.isBlockedBetween(senderId, target.getId())) {
                blocked++;
                continue;
            }

            Message msg = new Message(senderId, target.getId(), group.getId(), content, MessageType.GROUP_DIRECT, now);
            int msgId = messageRepo.saveMessage(msg);
            if (msgId < 0) {
                invalid++;
                continue;
            }

            if (sender.isOnline(targetLogin)) {
                sender.sendToUser(targetLogin, formatted);
                messageRepo.saveDelivery(msgId, target.getId(), true);
                deliveredNow++;
            } else {
                messageRepo.saveDelivery(msgId, target.getId(), false);
                deliveredLater++;
            }
        }

        if (deliveredNow == 0 && deliveredLater == 0) {
            return ServerResponse.error("Nenhum destinatario recebeu a mensagem privada no grupo. Invalidos: "
                    + invalid + ". Bloqueados: " + blocked + ".");
        }

        return ServerResponse.ok("Mensagem privada enviada no grupo '" + groupName + "'. Receberam agora: "
                + deliveredNow + ". Receberao depois: " + deliveredLater + ". Invalidos: " + invalid
                + ". Bloqueados/nao entregues: " + blocked + ".");
    }
}