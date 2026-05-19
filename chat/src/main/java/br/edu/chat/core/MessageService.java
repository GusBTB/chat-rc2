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

        if (blockRepo.isBlockedBetween(senderId, target.getId())) {
            return ServerResponse.error("Nao e possivel enviar mensagem para '" + targetLogin + "'.");
        }

        if (!permRepo.hasPermission(senderId, target.getId())) {
            if (!permRepo.hasPendingRequest(senderId, target.getId())) {
                PendingRequest req = new PendingRequest(
                        RequestType.PERMISSION, senderId, target.getId(),
                        null, RequestStatus.PENDING, LocalDateTime.now().toString());
                int reqId = requestRepo.save(req);
                permRepo.grant(senderId, target.getId());

                User senderUser = userRepo.findById(senderId);
                String senderLogin = senderUser != null ? senderUser.getLogin() : "desconhecido";
                String notification = ServerResponse.privateRequest(reqId,
                        senderLogin + " quer enviar mensagens diretas para voce. Use 'aceitar " + reqId + "' ou 'recusar " + reqId + "'.");
                if (sender.isOnline(targetLogin)) {
                    sender.sendToUser(targetLogin, notification);
                }
            }
            return ServerResponse.info("Pedido de permissao enviado para '" + targetLogin + "'. Aguarde a resposta.");
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
        } else {
            messageRepo.saveDelivery(msgId, target.getId(), false);
        }

        return ServerResponse.ok("Mensagem enviada para '" + targetLogin + "'.");
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

        List<User> members = groupRepo.listMembers(group.getId());
        for (User member : members) {
            if (member.getId() == senderId) {
                continue;
            }
            if (sender.isOnline(member.getLogin())) {
                sender.sendToUser(member.getLogin(), formatted);
                messageRepo.saveDelivery(msgId, member.getId(), true);
            } else {
                messageRepo.saveDelivery(msgId, member.getId(), false);
            }
        }

        return ServerResponse.ok("Mensagem enviada para o grupo '" + groupName + "'.");
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

        int sent = 0;
        for (String targetLogin : targetLogins) {
            User target = userRepo.findByLogin(targetLogin);
            if (target == null || !groupRepo.isMember(group.getId(), target.getId())) {
                continue;
            }
            if (blockRepo.isBlockedBetween(senderId, target.getId())) {
                continue;
            }

            Message msg = new Message(senderId, target.getId(), group.getId(), content, MessageType.GROUP_DIRECT, now);
            int msgId = messageRepo.saveMessage(msg);
            if (msgId < 0) {
                continue;
            }

            if (sender.isOnline(targetLogin)) {
                sender.sendToUser(targetLogin, formatted);
                messageRepo.saveDelivery(msgId, target.getId(), true);
            } else {
                messageRepo.saveDelivery(msgId, target.getId(), false);
            }
            sent++;
        }

        if (sent == 0) {
            return ServerResponse.error("Nenhum destinatario valido encontrado no grupo.");
        }

        return ServerResponse.ok("Mensagem privada enviada no grupo '" + groupName + "'.");
    }
}
