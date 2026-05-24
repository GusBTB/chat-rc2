package br.edu.chat.testmanual;

import br.edu.chat.core.BlockService;
import br.edu.chat.core.GroupService;
import br.edu.chat.core.MessageSender;
import br.edu.chat.core.MessageService;
import br.edu.chat.core.RequestService;
import br.edu.chat.core.UserService;
import br.edu.chat.database.DatabaseInitializer;
import br.edu.chat.model.MessageDelivery;
import br.edu.chat.model.PendingRequest;
import br.edu.chat.model.RequestType;
import br.edu.chat.model.User;
import br.edu.chat.repository.BlockRepository;
import br.edu.chat.repository.DirectPermissionRepository;
import br.edu.chat.repository.GroupRepository;
import br.edu.chat.repository.MessageRepository;
import br.edu.chat.repository.RequestRepository;
import br.edu.chat.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServiceTestMain {

    static List<String> sentMessages = new ArrayList<>();

    private static final Set<String> onlineUsers = new HashSet<>();
    private static final MessageSender stubSender = new MessageSender() {
        @Override
        public boolean isOnline(String login) {
            return onlineUsers.contains(login);
        }

        @Override
        public void sendToUser(String login, String message) {
            System.out.println("  MSG ENVIADA: [-> " + login + "] " + message);
        }
    };

    public static void main(String[] args) {
        DatabaseInitializer.initialize();

        UserRepository userRepo = new UserRepository();
        GroupRepository groupRepo = new GroupRepository();
        BlockRepository blockRepo = new BlockRepository();
        MessageRepository messageRepo = new MessageRepository();
        DirectPermissionRepository permRepo = new DirectPermissionRepository();
        RequestRepository requestRepo = new RequestRepository();

        UserService userService = new UserService(userRepo, messageRepo, stubSender);
        BlockService blockService = new BlockService(blockRepo, userRepo);
        MessageService messageService = new MessageService(
                messageRepo, userRepo, groupRepo, blockRepo, permRepo, requestRepo, stubSender);
        GroupService groupService = new GroupService(groupRepo, userRepo, requestRepo, stubSender);
        RequestService requestService = new RequestService(
                requestRepo, userRepo, groupRepo, permRepo, messageRepo, stubSender);

        System.out.println();
        System.out.println("===== TESTE DOS SERVICOS =====");

        System.out.println();
        System.out.println("--- Cadastro de usuarios ---");
        System.out.println(userService.register("Alice Ferreira", "alice", "alice@email.com", "senha123"));
        System.out.println(userService.register("Bruno Carvalho", "bruno", "bruno@email.com", "senha123"));
        System.out.println(userService.register("Carla Mendes", "carla", "carla@email.com", "senha123"));
        System.out.println(userService.register("Daniel Rocha", "daniel", "daniel@email.com", "senha123"));
        System.out.println(userService.register("Alice Ferreira", "alice", "alice2@email.com", "senha123"));

        System.out.println();
        System.out.println("--- Login ---");
        User alice = userService.login("alice", "senha123");
        User bruno = userService.login("bruno", "senha123");
        User carla = userService.login("carla", "senha123");
        User daniel = userService.login("daniel", "senha123");

        System.out.println("Alice logada: " + (alice != null ? alice.getLogin() : "FALHA"));
        System.out.println("Bruno logado: " + (bruno != null ? bruno.getLogin() : "FALHA"));
        System.out.println("Carla logada: " + (carla != null ? carla.getLogin() : "FALHA"));
        System.out.println("Daniel logado: " + (daniel != null ? daniel.getLogin() : "FALHA"));
        System.out.println(
                "Login invalido: " + (userService.login("alice", "errada") != null ? "AUTENTICOU" : "RECUSADO"));

        if (alice != null) {
            onlineUsers.add(alice.getLogin());
        }
        if (bruno != null) {
            onlineUsers.add(bruno.getLogin());
        }
        if (carla != null) {
            onlineUsers.add(carla.getLogin());
        }
        if (daniel != null) {
            onlineUsers.add(daniel.getLogin());
        }

        System.out.println();
        System.out.println("--- Listar usuarios ---");
        System.out.println(userService.listUsers());

        System.out.println();
        System.out.println("--- Mensagem direta (sem permissao) ---");
        System.out.println(messageService.sendDirect(alice.getId(), "bruno", "Ola Bruno!"));

        System.out.println();
        System.out.println("--- Aceitar pedido de permissao ---");
        List<PendingRequest> permRequests = requestRepo.findPendingByTarget(bruno.getId());
        PendingRequest permissionRequest = null;

        for (PendingRequest request : permRequests) {
            if (request.getRequestType() == RequestType.PERMISSION
                    && request.getRequesterUserId() == alice.getId()) {
                permissionRequest = request;
                break;
            }
        }

        if (permissionRequest != null) {
            System.out.println(requestService.accept(bruno.getId(), permissionRequest.getId()));
        } else {
            System.out.println("[AVISO] Nenhum pedido de permissao pendente para Bruno.");
        }

        System.out.println();
        System.out.println("--- Mensagem direta (com permissao) ---");
        System.out.println(messageService.sendDirect(alice.getId(), "bruno", "Agora posso te enviar mensagens!"));

        System.out.println();
        System.out.println("--- Bloquear usuario ---");
        System.out.println(blockService.block(bruno.getId(), "alice"));
        System.out.println("Alice bloqueada por Bruno? " + blockService.isBlockedBetween(alice.getId(), bruno.getId()));

        System.out.println();
        System.out.println("--- Mensagem direta apos bloqueio ---");
        System.out.println(messageService.sendDirect(alice.getId(), "bruno", "Tentando enviar apos bloqueio"));

        System.out.println();
        System.out.println("--- Desbloquear ---");
        System.out.println(blockService.unblock(bruno.getId(), "alice"));

        System.out.println();
        System.out.println("--- Criar grupo ---");
        System.out.println(groupService.createGroup(alice.getId(), "devs"));
        System.out.println(groupService.createGroup(alice.getId(), "devs"));

        System.out.println();
        System.out.println("--- Listar grupos ---");
        System.out.println(groupService.listGroups());

        System.out.println();
        System.out.println("--- Convidar Bruno para o grupo ---");
        System.out.println(groupService.insertMember(alice.getId(), "devs", "bruno"));

        System.out.println();
        System.out.println("--- Bruno aceita convite ---");
        List<PendingRequest> brunoInviteRequests = requestRepo.findPendingByTarget(bruno.getId());
        PendingRequest brunoInvite = null;

        for (PendingRequest request : brunoInviteRequests) {
            if (request.getRequestType() == RequestType.INVITE
                    && request.getRequesterUserId() == alice.getId()) {
                brunoInvite = request;
                break;
            }
        }

        if (brunoInvite != null) {
            System.out.println(requestService.accept(bruno.getId(), brunoInvite.getId()));
        } else {
            System.out.println("[AVISO] Nenhum convite pendente para Bruno.");
        }

        System.out.println();
        System.out.println("--- Convidar Carla para o grupo ---");
        System.out.println(groupService.insertMember(alice.getId(), "devs", "carla"));

        System.out.println();
        System.out.println("--- Carla aceita convite ---");
        List<PendingRequest> carlaInviteRequests = requestRepo.findPendingByTarget(carla.getId());
        PendingRequest carlaInvite = null;

        for (PendingRequest request : carlaInviteRequests) {
            if (request.getRequestType() == RequestType.INVITE
                    && request.getRequesterUserId() == alice.getId()) {
                carlaInvite = request;
                break;
            }
        }

        if (carlaInvite != null) {
            System.out.println(requestService.accept(carla.getId(), carlaInvite.getId()));
        } else {
            System.out.println("[AVISO] Nenhum convite pendente para Carla.");
        }

        System.out.println();
        System.out.println("--- Listar membros do grupo ---");
        System.out.println(groupService.listGroupMembers("devs"));

        System.out.println();
        System.out.println("--- Mensagem para o grupo ---");
        System.out.println(messageService.sendGroup(alice.getId(), "devs", "Ola galera!"));

        System.out.println();
        System.out.println("--- Bruno sai do grupo ---");
        System.out.println(groupService.leaveGroup(bruno.getId(), "devs"));
        System.out.println(groupService.listGroupMembers("devs"));

        System.out.println();
        System.out.println("--- TESTE: pedido de entrada com mais de um membro no grupo ---");
        System.out.println("Neste momento, Alice e Carla devem estar no grupo. Bruno esta fora.");
        System.out.println(groupService.requestJoin(bruno.getId(), "devs"));

        System.out.println();
        System.out.println("--- Alice aceita pedido de entrada de Bruno ---");
        List<PendingRequest> aliceJoinRequests = requestRepo.findPendingByTarget(alice.getId());
        PendingRequest aliceJoinRequest = null;

        for (PendingRequest request : aliceJoinRequests) {
            if (request.getRequestType() == RequestType.GROUP_JOIN
                    && request.getRequesterUserId() == bruno.getId()) {
                aliceJoinRequest = request;
                break;
            }
        }

        if (aliceJoinRequest != null) {
            System.out.println(requestService.accept(alice.getId(), aliceJoinRequest.getId()));
        } else {
            System.out.println("[AVISO] Nenhum pedido de entrada pendente para Alice.");
        }

        System.out.println();
        System.out.println("--- Conferir membros apos apenas Alice aceitar ---");
        System.out.println("Bruno ainda NAO deve aparecer como membro, pois Carla ainda nao aceitou.");
        System.out.println(groupService.listGroupMembers("devs"));

        System.out.println();
        System.out.println("--- Carla aceita pedido de entrada de Bruno ---");
        List<PendingRequest> carlaJoinRequests = requestRepo.findPendingByTarget(carla.getId());
        PendingRequest carlaJoinRequest = null;

        for (PendingRequest request : carlaJoinRequests) {
            if (request.getRequestType() == RequestType.GROUP_JOIN
                    && request.getRequesterUserId() == bruno.getId()) {
                carlaJoinRequest = request;
                break;
            }
        }

        if (carlaJoinRequest != null) {
            System.out.println(requestService.accept(carla.getId(), carlaJoinRequest.getId()));
        } else {
            System.out.println("[AVISO] Nenhum pedido de entrada pendente para Carla.");
        }

        System.out.println();
        System.out.println("--- Conferir membros apos todos aceitarem ---");
        System.out.println("Agora Bruno deve aparecer como membro.");
        System.out.println(groupService.listGroupMembers("devs"));

        System.out.println();
        System.out.println("--- TESTE: promover Bruno a admin sem promover automaticamente ---");
        System.out.println(groupService.promoteAdmin(alice.getId(), "devs", "bruno"));
        System.out.println("Antes do aceite, Bruno NAO deve aparecer como admin:");
        System.out.println(groupService.listGroupMembers("devs"));

        System.out.println();
        System.out.println("--- Bruno aceita convite de admin ---");
        List<PendingRequest> adminRequests = requestRepo.findPendingByTarget(bruno.getId());
        PendingRequest adminRequest = null;

        for (PendingRequest request : adminRequests) {
            if (request.getRequestType() == RequestType.ADMIN_PROMOTION
                    && request.getRequesterUserId() == alice.getId()) {
                adminRequest = request;
                break;
            }
        }

        if (adminRequest != null) {
            System.out.println(requestService.accept(bruno.getId(), adminRequest.getId()));
        } else {
            System.out.println("[AVISO] Nenhum convite de admin pendente para Bruno.");
        }

        System.out.println();
        System.out.println("--- Conferir membros apos Bruno aceitar admin ---");
        System.out.println("Agora Bruno deve aparecer como admin.");
        System.out.println(groupService.listGroupMembers("devs"));

        System.out.println();
        System.out.println("--- TESTE: bloqueio em mensagem de grupo ---");
        System.out.println(
                "Bruno bloqueia Alice. Alice envia mensagem no grupo. Bruno NAO deve receber, Carla deve receber.");
        System.out.println(blockService.block(bruno.getId(), "alice"));
        System.out.println(
                messageService.sendGroup(alice.getId(), "devs", "Mensagem de grupo com Bruno bloqueando Alice."));
        System.out.println(blockService.unblock(bruno.getId(), "alice"));

        System.out.println();
        System.out.println("--- TESTE: mensagem offline ---");
        System.out.println("Bruno faz logout. Alice envia mensagem direta para Bruno. A mensagem deve ficar pendente.");
        System.out.println(userService.logout(bruno.getId()));
        onlineUsers.remove("bruno");
        System.out.println(messageService.sendDirect(alice.getId(), "bruno", "Mensagem offline para Bruno."));

        System.out.println();
        System.out.println("--- Consultar mensagens pendentes de Bruno ---");
        List<MessageDelivery> pendingForBruno = messageRepo.listPendingDeliveriesForUser(bruno.getId());
        System.out.println("Quantidade de mensagens pendentes para Bruno: " + pendingForBruno.size());
        for (MessageDelivery delivery : pendingForBruno) {
            System.out.println(delivery);
        }

        System.out.println();
        System.out.println("--- Bruno faz login novamente e recebe pendencias ---");
        bruno = userService.login("bruno", "senha123");
        if (bruno != null) {
            onlineUsers.add(bruno.getLogin());
            System.out.println("Bruno logado novamente: " + bruno.getLogin());
            userService.deliverPendingMessages(bruno);
        } else {
            System.out.println("[ERRO] Bruno nao conseguiu logar novamente.");
        }

        System.out.println();
        System.out.println("--- Consultar mensagens pendentes de Bruno apos entrega ---");
        List<MessageDelivery> pendingAfterDelivery = messageRepo.listPendingDeliveriesForUser(bruno.getId());
        System.out.println("Quantidade de mensagens pendentes para Bruno apos entrega: " + pendingAfterDelivery.size());
        for (MessageDelivery delivery : pendingAfterDelivery) {
            System.out.println(delivery);
        }

        System.out.println();
        System.out.println("--- Logout final ---");
        System.out.println(userService.logout(alice.getId()));
        System.out.println(userService.logout(bruno.getId()));
        System.out.println(userService.logout(carla.getId()));
        System.out.println(userService.logout(daniel.getId()));

        System.out.println();
        System.out.println("===== FIM DO TESTE DE SERVICOS =====");
    }
}
