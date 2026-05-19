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

public class RequestService {

    private final RequestRepository requestRepo;
    private final UserRepository userRepo;
    private final GroupRepository groupRepo;
    private final DirectPermissionRepository permRepo;
    private final MessageSender sender;

    public RequestService(RequestRepository requestRepo, UserRepository userRepo,
            GroupRepository groupRepo, DirectPermissionRepository permRepo,
            MessageSender sender) {
        this.requestRepo = requestRepo;
        this.userRepo = userRepo;
        this.groupRepo = groupRepo;
        this.permRepo = permRepo;
        this.sender = sender;
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

        User requester = userRepo.findById(req.getRequesterUserId());
        String requesterLogin = requester != null ? requester.getLogin() : "desconhecido";

        if (sender.isOnline(requesterLogin)) {
            sender.sendToUser(requesterLogin,
                    ServerResponse.system("Seu pedido #" + requestId + " foi recusado."));
        }

        return ServerResponse.ok("Pedido #" + requestId + " recusado.");
    }

    private String acceptPermission(PendingRequest req) {
        permRepo.grant(req.getRequesterUserId(), req.getTargetUserId());

        User requester = userRepo.findById(req.getRequesterUserId());
        String requesterLogin = requester != null ? requester.getLogin() : "desconhecido";

        if (sender.isOnline(requesterLogin)) {
            User target = userRepo.findById(req.getTargetUserId());
            String targetLogin = target != null ? target.getLogin() : "desconhecido";
            sender.sendToUser(requesterLogin,
                    ServerResponse.system(targetLogin + " aceitou seu pedido de mensagem direta."));
        }

        return ServerResponse.ok("Pedido aceito. " + requesterLogin + " agora pode te enviar mensagens diretas.");
    }

    private String acceptInvite(PendingRequest req) {
        if (req.getGroupId() == null) {
            return ServerResponse.error("Dados do convite invalidos.");
        }

        Group group = groupRepo.findById(req.getGroupId());
        if (group == null) {
            return ServerResponse.error("Grupo do convite nao encontrado.");
        }

        groupRepo.addMember(group.getId(), req.getTargetUserId(), false);

        User requester = userRepo.findById(req.getRequesterUserId());
        String requesterLogin = requester != null ? requester.getLogin() : "desconhecido";

        if (sender.isOnline(requesterLogin)) {
            User target = userRepo.findById(req.getTargetUserId());
            String targetLogin = target != null ? target.getLogin() : "desconhecido";
            sender.sendToUser(requesterLogin,
                    ServerResponse.system(targetLogin + " aceitou o convite para o grupo '" + group.getName() + "'."));
        }

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

        groupRepo.addMember(group.getId(), req.getRequesterUserId(), false);

        User requester = userRepo.findById(req.getRequesterUserId());
        String requesterLogin = requester != null ? requester.getLogin() : "desconhecido";

        if (sender.isOnline(requesterLogin)) {
            sender.sendToUser(requesterLogin,
                    ServerResponse.system("Seu pedido para entrar no grupo '" + group.getName() + "' foi aceito."));
        }

        return ServerResponse.ok("Pedido aceito. " + requesterLogin + " agora e membro do grupo '" + group.getName() + "'.");
    }
}
