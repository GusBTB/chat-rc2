package br.edu.chat.core;

import br.edu.chat.model.Group;
import br.edu.chat.model.PendingRequest;
import br.edu.chat.model.RequestStatus;
import br.edu.chat.model.RequestType;
import br.edu.chat.model.User;
import br.edu.chat.protocol.ServerResponse;
import br.edu.chat.repository.GroupRepository;
import br.edu.chat.repository.RequestRepository;
import br.edu.chat.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

public class GroupService {

    private final GroupRepository groupRepo;
    private final UserRepository userRepo;
    private final RequestRepository requestRepo;
    private final MessageSender sender;

    public GroupService(GroupRepository groupRepo, UserRepository userRepo,
            RequestRepository requestRepo, MessageSender sender) {
        this.groupRepo = groupRepo;
        this.userRepo = userRepo;
        this.requestRepo = requestRepo;
        this.sender = sender;
    }

    public String createGroup(int creatorId, String groupName) {
        if (groupName == null || groupName.isBlank()) {
            return ServerResponse.error("Nome do grupo invalido.");
        }

        if (groupRepo.findByName(groupName) != null) {
            return ServerResponse.error("Ja existe um grupo com o nome '" + groupName + "'.");
        }

        int groupId = groupRepo.createGroup(groupName, creatorId);
        if (groupId < 0) {
            return ServerResponse.error("Falha ao criar o grupo.");
        }

        groupRepo.addMember(groupId, creatorId, true);
        return ServerResponse.ok("Grupo '" + groupName + "' criado com sucesso. Voce e administrador do grupo.");
    }

    public String insertMember(int adminId, String groupName, String targetLogin) {
        Group group = groupRepo.findByName(groupName);
        if (group == null) {
            return ServerResponse.error("Grupo '" + groupName + "' nao encontrado.");
        }

        if (!groupRepo.isAdmin(group.getId(), adminId)) {
            return ServerResponse.error("Voce nao tem permissao de administrador no grupo '" + groupName + "'.");
        }

        User target = userRepo.findByLogin(targetLogin);
        if (target == null) {
            return ServerResponse.error("Usuario '" + targetLogin + "' nao encontrado.");
        }

        if (groupRepo.isMember(group.getId(), target.getId())) {
            return ServerResponse.error("'" + targetLogin + "' ja e membro do grupo '" + groupName + "'.");
        }

        if (requestRepo.existsPending(RequestType.INVITE, adminId, target.getId(), group.getId())) {
            return ServerResponse.info(
                    "Ja existe um convite pendente para '" + targetLogin + "' entrar no grupo '" + groupName + "'.");
        }

        PendingRequest req = new PendingRequest(
                RequestType.INVITE, adminId, target.getId(),
                group.getId(), RequestStatus.PENDING, LocalDateTime.now().toString());
        int reqId = requestRepo.save(req);

        User adminUser = userRepo.findById(adminId);
        String adminLogin = adminUser != null ? adminUser.getLogin() : "desconhecido";
        String notification = ServerResponse.invite(reqId,
                adminLogin + " te convidou para o grupo '" + groupName + "'. Use 'aceitar " + reqId + "' ou 'recusar "
                        + reqId + "'.");

        if (sender.isOnline(targetLogin)) {
            sender.sendToUser(targetLogin, notification);
        }

        return ServerResponse.ok("Convite enviado para '" + targetLogin + "'.");
    }

    public String requestJoin(int userId, String groupName) {
        Group group = groupRepo.findByName(groupName);
        if (group == null) {
            return ServerResponse.error("Grupo '" + groupName + "' nao encontrado.");
        }

        if (groupRepo.isMember(group.getId(), userId)) {
            return ServerResponse.error("Voce ja e membro do grupo '" + groupName + "'.");
        }

        if (requestRepo.existsPendingGroupJoinForRequester(userId, group.getId())) {
            return ServerResponse.info("Ja existe um pedido pendente para voce entrar no grupo '" + groupName + "'.");
        }

        List<User> members = groupRepo.listMembers(group.getId());
        if (members.isEmpty()) {
            return ServerResponse.error("O grupo '" + groupName + "' nao possui membros para aprovar sua entrada.");
        }

        User requester = userRepo.findById(userId);
        String requesterLogin = requester != null ? requester.getLogin() : "desconhecido";
        int createdRequests = 0;

        for (User member : members) {
            PendingRequest req = new PendingRequest(
                    RequestType.GROUP_JOIN, userId, member.getId(),
                    group.getId(), RequestStatus.PENDING, LocalDateTime.now().toString());
            int reqId = requestRepo.save(req);
            if (reqId < 0) {
                continue;
            }

            createdRequests++;
            String notification = ServerResponse.groupJoinRequest(reqId,
                    requesterLogin + " quer entrar no grupo '" + groupName + "'. Use 'aceitar " + reqId
                            + "' ou 'recusar " + reqId + "'.");

            if (sender.isOnline(member.getLogin())) {
                sender.sendToUser(member.getLogin(), notification);
            }
        }

        if (createdRequests == 0) {
            return ServerResponse.error("Nao foi possivel criar os pedidos de entrada no grupo.");
        }

        return ServerResponse
                .ok("Pedido de entrada enviado para todos os membros atuais do grupo '" + groupName + "'.");
    }

    public String leaveGroup(int userId, String groupName) {
        Group group = groupRepo.findByName(groupName);
        if (group == null) {
            return ServerResponse.error("Grupo '" + groupName + "' nao encontrado.");
        }

        if (!groupRepo.isMember(group.getId(), userId)) {
            return ServerResponse.error("Voce nao e membro do grupo '" + groupName + "'.");
        }

        if (groupRepo.isAdmin(group.getId(), userId) && groupRepo.listAdmins(group.getId()).size() == 1) {
            return ServerResponse.error("Voce e o unico administrador do grupo. Promova outro usuario antes de sair.");
        }

        boolean ok = groupRepo.removeMember(group.getId(), userId);
        if (!ok) {
            return ServerResponse.error("Falha ao sair do grupo.");
        }

        User user = userRepo.findById(userId);
        String userLogin = user != null ? user.getLogin() : "desconhecido";
        notifyGroupMembers(group.getId(), userId,
                ServerResponse.system(userLogin + " saiu do grupo '" + groupName + "'."));

        return ServerResponse.ok("Voce saiu do grupo '" + groupName + "'.");
    }

    public String promoteAdmin(int adminId, String groupName, String targetLogin) {
        Group group = groupRepo.findByName(groupName);
        if (group == null) {
            return ServerResponse.error("Grupo '" + groupName + "' nao encontrado.");
        }

        if (!groupRepo.isAdmin(group.getId(), adminId)) {
            return ServerResponse.error("Voce nao tem permissao de administrador no grupo '" + groupName + "'.");
        }

        User target = userRepo.findByLogin(targetLogin);
        if (target == null) {
            return ServerResponse.error("Usuario '" + targetLogin + "' nao encontrado.");
        }

        if (!groupRepo.isMember(group.getId(), target.getId())) {
            return ServerResponse.error("'" + targetLogin + "' nao e membro do grupo '" + groupName + "'.");
        }

        if (groupRepo.isAdmin(group.getId(), target.getId())) {
            return ServerResponse.error("'" + targetLogin + "' ja e administrador do grupo '" + groupName + "'.");
        }

        if (requestRepo.existsPending(RequestType.ADMIN_PROMOTION, adminId, target.getId(), group.getId())) {
            return ServerResponse.info("Ja existe um convite pendente para '" + targetLogin
                    + "' se tornar administrador do grupo '" + groupName + "'.");
        }

        PendingRequest req = new PendingRequest(
                RequestType.ADMIN_PROMOTION, adminId, target.getId(),
                group.getId(), RequestStatus.PENDING, LocalDateTime.now().toString());
        int reqId = requestRepo.save(req);

        User adminUser = userRepo.findById(adminId);
        String adminLogin = adminUser != null ? adminUser.getLogin() : "desconhecido";
        String notification = ServerResponse.invite(reqId,
                adminLogin + " quer tornar voce administrador do grupo '" + groupName + "'. Use 'aceitar " + reqId
                        + "' ou 'recusar " + reqId + "'.");

        if (sender.isOnline(targetLogin)) {
            sender.sendToUser(targetLogin, notification);
        }

        return ServerResponse.ok("Convite de administracao enviado para '" + targetLogin + "'.");
    }

    public String listGroups() {
        List<Group> groups = groupRepo.listGroups();

        if (groups.isEmpty()) {
            return ServerResponse.info("Nenhum grupo cadastrado.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(ServerResponse.info("Grupos disponiveis:"));
        for (Group g : groups) {
            sb.append("\n  - ").append(g.getName());
        }

        return sb.toString();
    }

    public String listGroupMembers(String groupName) {
        Group group = groupRepo.findByName(groupName);
        if (group == null) {
            return ServerResponse.error("Grupo '" + groupName + "' nao encontrado.");
        }

        List<User> members = groupRepo.listMembers(group.getId());
        List<User> admins = groupRepo.listAdmins(group.getId());

        if (members.isEmpty()) {
            return ServerResponse.info("O grupo '" + groupName + "' nao tem membros.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(ServerResponse.info("Membros do grupo '" + groupName + "':"));
        for (User u : members) {
            boolean isAdmin = admins.stream().anyMatch(a -> a.getId() == u.getId());
            String adminTag = isAdmin ? " [admin]" : "";
            String statusTag = " [" + u.getStatus().name().toLowerCase() + "]";

            sb.append("\n  - ")
                    .append(u.getLogin())
                    .append(adminTag)
                    .append(statusTag);
        }

        return sb.toString();
    }

    private void notifyGroupMembers(int groupId, int exceptUserId, String message) {
        List<User> members = groupRepo.listMembers(groupId);
        for (User member : members) {
            if (member.getId() == exceptUserId) {
                continue;
            }
            if (sender.isOnline(member.getLogin())) {
                sender.sendToUser(member.getLogin(), message);
            }
        }
    }
}