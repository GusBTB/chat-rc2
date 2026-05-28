package br.edu.chat.core;

import br.edu.chat.model.Group;
import br.edu.chat.model.PendingRequest;
import br.edu.chat.model.RequestStatus;
import br.edu.chat.model.RequestType;
import br.edu.chat.model.User;
import br.edu.chat.protocol.ServerResponse;
import br.edu.chat.repository.DirectPermissionRepository;
import br.edu.chat.repository.GroupRepository;
import br.edu.chat.repository.RequestRepository;
import br.edu.chat.repository.UserRepository;
import br.edu.chat.model.Message;
import br.edu.chat.repository.MessageRepository;

public class RequestService {

    private final RequestRepository requestRepo;
    private final UserRepository userRepo;
    private final GroupRepository groupRepo;
    private final DirectPermissionRepository permRepo;
    private final MessageRepository messageRepo;
    private final MessageSender sender;

    public RequestService(RequestRepository requestRepo, UserRepository userRepo,
            GroupRepository groupRepo, DirectPermissionRepository permRepo,
            MessageRepository messageRepo, MessageSender sender) {
        this.requestRepo = requestRepo;
        this.userRepo = userRepo;
        this.groupRepo = groupRepo;
        this.permRepo = permRepo;
        this.messageRepo = messageRepo;
        this.sender = sender;
    }

    public void deliverPendingForUser(int userId) {
        User user = userRepo.findById(userId);
        if (user == null || !sender.isOnline(user.getLogin())) {
            return;
        }

        java.util.List<PendingRequest> pendings = requestRepo.findPendingByTarget(userId);
        for (PendingRequest req : pendings) {
            String notification = formatPendingNotification(req);
            if (notification != null) {
                sender.sendToUser(user.getLogin(), notification);
            }
        }
    }

    private String formatPendingNotification(PendingRequest req) {
        User requester = userRepo.findById(req.getRequesterUserId());
        String requesterLogin = requester != null ? requester.getLogin() : "desconhecido";

        switch (req.getRequestType()) {
            case PERMISSION:
                return ServerResponse.privateRequest(req.getId(),
                        requesterLogin + " quer enviar uma mensagem direta para voce. Use 'aceitar " + req.getId()
                                + "' ou 'recusar " + req.getId() + "'.");
            case INVITE: {
                Group group = req.getGroupId() != null ? groupRepo.findById(req.getGroupId()) : null;
                String groupName = group != null ? group.getName() : "desconhecido";
                return ServerResponse.invite(req.getId(),
                        requesterLogin + " te convidou para o grupo '" + groupName + "'. Use 'aceitar " + req.getId()
                                + "' ou 'recusar " + req.getId() + "'.");
            }
            case GROUP_JOIN: {
                Group group = req.getGroupId() != null ? groupRepo.findById(req.getGroupId()) : null;
                String groupName = group != null ? group.getName() : "desconhecido";
                return ServerResponse.groupJoinRequest(req.getId(),
                        requesterLogin + " quer entrar no grupo '" + groupName + "'. Use 'aceitar " + req.getId()
                                + "' ou 'recusar " + req.getId() + "'.");
            }
            case ADMIN_PROMOTION: {
                Group group = req.getGroupId() != null ? groupRepo.findById(req.getGroupId()) : null;
                String groupName = group != null ? group.getName() : "desconhecido";
                return ServerResponse.invite(req.getId(),
                        requesterLogin + " quer tornar voce administrador do grupo '" + groupName
                                + "'. Use 'aceitar " + req.getId() + "' ou 'recusar " + req.getId() + "'.");
            }
            default:
                return null;
        }
    }

    public String accept(int userId, int requestId) {
        PendingRequest req = requestRepo.findById(requestId);

        if (req == null) {
            return ServerResponse.error("Pedido #" + requestId + " nao encontrado.");
        }

        if (req.getStatus() != RequestStatus.PENDING) {
            return ServerResponse.error("Pedido #" + requestId + " ja foi processado.");
        }

        if (req.getTargetUserId() == null || req.getTargetUserId() != userId) {
            return ServerResponse.error("Voce nao tem permissao para aceitar o pedido #" + requestId + ".");
        }

        requestRepo.updateStatus(requestId, RequestStatus.ACCEPTED);

        if (req.getRequestType() == RequestType.PERMISSION) {
            return acceptPermission(req);
        } else if (req.getRequestType() == RequestType.INVITE) {
            return acceptInvite(req);
        } else if (req.getRequestType() == RequestType.GROUP_JOIN) {
            return acceptGroupJoin(req);
        } else if (req.getRequestType() == RequestType.ADMIN_PROMOTION) {
            return acceptAdminPromotion(req);
        }

        return ServerResponse.error("Tipo de pedido desconhecido.");
    }

    public String refuse(int userId, int requestId) {
        PendingRequest req = requestRepo.findById(requestId);

        if (req == null) {
            return ServerResponse.error("Pedido #" + requestId + " nao encontrado.");
        }

        if (req.getStatus() != RequestStatus.PENDING) {
            return ServerResponse.error("Pedido #" + requestId + " ja foi processado.");
        }

        if (req.getTargetUserId() == null || req.getTargetUserId() != userId) {
            return ServerResponse.error("Voce nao tem permissao para recusar o pedido #" + requestId + ".");
        }

        requestRepo.updateStatus(requestId, RequestStatus.REFUSED);

        if (req.getRequestType() == RequestType.GROUP_JOIN && req.getGroupId() != null) {
            requestRepo.refusePendingGroupJoinApprovals(req.getRequesterUserId(), req.getGroupId());
        }

        User requester = userRepo.findById(req.getRequesterUserId());
        String requesterLogin = requester != null ? requester.getLogin() : "desconhecido";

        if (sender.isOnline(requesterLogin)) {
            if (req.getRequestType() == RequestType.PERMISSION) {
                User target = userRepo.findById(req.getTargetUserId());
                String targetLogin = target != null ? target.getLogin() : "desconhecido";

                sender.sendToUser(requesterLogin,
                        ServerResponse.system(targetLogin
                                + " recusou seu pedido de mensagem direta. A mensagem inicial nao foi entregue."));
            } else {
                sender.sendToUser(requesterLogin,
                        ServerResponse.system("Seu pedido #" + requestId + " foi recusado."));
            }
        }

        return ServerResponse.ok("Pedido #" + requestId + " recusado.");
    }

    private String acceptPermission(PendingRequest req) {
        permRepo.grant(req.getRequesterUserId(), req.getTargetUserId());

        User requester = userRepo.findById(req.getRequesterUserId());
        User target = userRepo.findById(req.getTargetUserId());

        String requesterLogin = requester != null ? requester.getLogin() : "desconhecido";
        String targetLogin = target != null ? target.getLogin() : "desconhecido";

        if (sender.isOnline(requesterLogin)) {
            sender.sendToUser(requesterLogin,
                    ServerResponse.system(targetLogin + " aceitou seu pedido de mensagem direta."));
        }

        if (req.getPendingMessageId() == null) {
            return ServerResponse.ok("Pedido aceito. " + requesterLogin + " agora pode te enviar mensagens diretas.");
        }

        Message pendingMessage = messageRepo.findById(req.getPendingMessageId());

        if (pendingMessage == null) {
            return ServerResponse.ok("Pedido aceito, mas a mensagem inicial nao foi encontrada.");
        }

        if (target == null) {
            return ServerResponse
                    .ok("Pedido aceito, mas o destinatario nao foi encontrado para entrega da mensagem inicial.");
        }

        if (requester == null) {
            return ServerResponse
                    .ok("Pedido aceito, mas o remetente nao foi encontrado para entrega da mensagem inicial.");
        }

        if (sender.isOnline(targetLogin)) {
            sender.sendToUser(targetLogin,
                    ServerResponse.directMessage(requesterLogin, pendingMessage.getCreatedAt(),
                            pendingMessage.getContent()));
            messageRepo.saveDelivery(pendingMessage.getId(), target.getId(), true);

            if (sender.isOnline(requesterLogin)) {
                sender.sendToUser(requesterLogin,
                        ServerResponse.system("Mensagem inicial entregue para " + targetLogin + "."));
            }

            return ServerResponse.ok("Pedido aceito. Mensagem inicial entregue.");
        }

        messageRepo.saveDelivery(pendingMessage.getId(), target.getId(), false);

        if (sender.isOnline(requesterLogin)) {
            sender.sendToUser(requesterLogin,
                    ServerResponse.system("Mensagem inicial salva para " + targetLogin
                            + ". O usuario recebera quando ficar online."));
        }

        return ServerResponse.ok("Pedido aceito. Mensagem inicial salva para entrega posterior.");
    }

    private String acceptInvite(PendingRequest req) {
        if (req.getGroupId() == null) {
            return ServerResponse.error("Dados do convite invalidos.");
        }

        Group group = groupRepo.findById(req.getGroupId());
        if (group == null) {
            return ServerResponse.error("Grupo do convite nao encontrado.");
        }

        if (!groupRepo.isMember(group.getId(), req.getTargetUserId())) {
            groupRepo.addMember(group.getId(), req.getTargetUserId(), false);
        }

        User requester = userRepo.findById(req.getRequesterUserId());
        String requesterLogin = requester != null ? requester.getLogin() : "desconhecido";

        User target = userRepo.findById(req.getTargetUserId());
        String targetLogin = target != null ? target.getLogin() : "desconhecido";

        if (sender.isOnline(requesterLogin)) {
            sender.sendToUser(requesterLogin,
                    ServerResponse.system(targetLogin + " aceitou o convite para o grupo '" + group.getName() + "'."));
        }

        notifyGroupMembers(group.getId(), req.getTargetUserId(),
                ServerResponse.system(targetLogin + " entrou no grupo '" + group.getName() + "'."));

        return ServerResponse.ok("Voce entrou no grupo '" + group.getName() + "'.");
    }

    private String acceptGroupJoin(PendingRequest req) {
        if (req.getGroupId() == null) {
            return ServerResponse.error("Dados do pedido de entrada invalidos.");
        }

        Group group = groupRepo.findById(req.getGroupId());
        if (group == null) {
            return ServerResponse.error("Grupo do pedido nao encontrado.");
        }

        User requester = userRepo.findById(req.getRequesterUserId());
        String requesterLogin = requester != null ? requester.getLogin() : "desconhecido";

        int pendingApprovals = requestRepo.countPendingGroupJoinApprovals(req.getRequesterUserId(), group.getId());
        if (pendingApprovals > 0) {
            return ServerResponse.ok("Pedido aceito. Ainda faltam " + pendingApprovals
                    + " aprovacao(oes) para " + requesterLogin + " entrar no grupo '" + group.getName() + "'.");
        }

        if (!groupRepo.isMember(group.getId(), req.getRequesterUserId())) {
            groupRepo.addMember(group.getId(), req.getRequesterUserId(), false);
        }

        if (sender.isOnline(requesterLogin)) {
            sender.sendToUser(requesterLogin,
                    ServerResponse
                            .system("Seu pedido para entrar no grupo '" + group.getName() + "' foi aceito por todos."));
        }

        notifyGroupMembers(group.getId(), req.getRequesterUserId(),
                ServerResponse.system(requesterLogin + " entrou no grupo '" + group.getName() + "'."));

        return ServerResponse
                .ok("Todos aceitaram. " + requesterLogin + " agora e membro do grupo '" + group.getName() + "'.");
    }

    private void notifyGroupMembers(int groupId, int exceptUserId, String message) {
        java.util.List<User> members = groupRepo.listMembers(groupId);
        for (User member : members) {
            if (member.getId() == exceptUserId) {
                continue;
            }
            if (sender.isOnline(member.getLogin())) {
                sender.sendToUser(member.getLogin(), message);
            }
        }
    }

    private String acceptAdminPromotion(PendingRequest req) {
        if (req.getGroupId() == null) {
            return ServerResponse.error("Dados do convite de administracao invalidos.");
        }

        Group group = groupRepo.findById(req.getGroupId());
        if (group == null) {
            return ServerResponse.error("Grupo do convite de administracao nao encontrado.");
        }

        if (!groupRepo.isMember(group.getId(), req.getTargetUserId())) {
            return ServerResponse.error("Voce nao e membro do grupo '" + group.getName() + "'.");
        }

        groupRepo.makeAdmin(group.getId(), req.getTargetUserId());

        User requester = userRepo.findById(req.getRequesterUserId());
        String requesterLogin = requester != null ? requester.getLogin() : "desconhecido";
        User target = userRepo.findById(req.getTargetUserId());
        String targetLogin = target != null ? target.getLogin() : "desconhecido";

        if (sender.isOnline(requesterLogin)) {
            sender.sendToUser(requesterLogin,
                    ServerResponse
                            .system(targetLogin + " aceitou ser administrador do grupo '" + group.getName() + "'."));
        }

        return ServerResponse.ok("Voce agora e administrador do grupo '" + group.getName() + "'.");
    }
}