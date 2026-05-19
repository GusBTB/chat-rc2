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
        return ServerResponse.ok("Grupo '" + groupName + "' criado com sucesso.");
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

        PendingRequest req = new PendingRequest(
                RequestType.INVITE, adminId, target.getId(),
                group.getId(), RequestStatus.PENDING, LocalDateTime.now().toString());
        int reqId = requestRepo.save(req);

        User adminUser = userRepo.findById(adminId);
        String adminLogin = adminUser != null ? adminUser.getLogin() : "desconhecido";
        String notification = ServerResponse.invite(reqId,
                adminLogin + " te convidou para o grupo '" + groupName + "'. Use 'aceitar " + reqId + "' ou 'recusar " + reqId + "'.");

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

        User creator = userRepo.findById(group.getCreatedByUserId());
        if (creator == null) {
            return ServerResponse.error("Nao foi possivel encontrar o administrador do grupo.");
        }

        PendingRequest req = new PendingRequest(
                RequestType.GROUP_JOIN, userId, creator.getId(),
                group.getId(), RequestStatus.PENDING, LocalDateTime.now().toString());
        int reqId = requestRepo.save(req);

        User requester = userRepo.findById(userId);
        String requesterLogin = requester != null ? requester.getLogin() : "desconhecido";
        String notification = ServerResponse.groupJoinRequest(reqId,
                requesterLogin + " quer entrar no grupo '" + groupName + "'. Use 'aceitar " + reqId + "' ou 'recusar " + reqId + "'.");

        if (sender.isOnline(creator.getLogin())) {
            sender.sendToUser(creator.getLogin(), notification);
        }

        return ServerResponse.ok("Pedido de entrada enviado para o administrador do grupo '" + groupName + "'.");
    }

    public String leaveGroup(int userId, String groupName) {
        Group group = groupRepo.findByName(groupName);
        if (group == null) {
            return ServerResponse.error("Grupo '" + groupName + "' nao encontrado.");
        }

        if (!groupRepo.isMember(group.getId(), userId)) {
            return ServerResponse.error("Voce nao e membro do grupo '" + groupName + "'.");
        }

        if (group.getCreatedByUserId() == userId) {
            return ServerResponse.error("O criador do grupo nao pode sair. Transfira a administracao antes.");
        }

        boolean ok = groupRepo.removeMember(group.getId(), userId);
        if (!ok) {
            return ServerResponse.error("Falha ao sair do grupo.");
        }

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

        boolean ok = groupRepo.makeAdmin(group.getId(), target.getId());
        if (!ok) {
            return ServerResponse.error("Falha ao promover usuario.");
        }

        return ServerResponse.ok("'" + targetLogin + "' promovido a administrador do grupo '" + groupName + "'.");
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
            String tag = isAdmin ? " [admin]" : "";
            sb.append("\n  - ").append(u.getLogin()).append(tag);
        }

        return sb.toString();
    }
}
