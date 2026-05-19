package br.edu.chat.testmanual;

import br.edu.chat.core.BlockService;
import br.edu.chat.core.GroupService;
import br.edu.chat.core.MessageSender;
import br.edu.chat.core.MessageService;
import br.edu.chat.core.RequestService;
import br.edu.chat.core.UserService;
import br.edu.chat.database.DatabaseInitializer;
import br.edu.chat.model.PendingRequest;
import br.edu.chat.model.User;
import br.edu.chat.repository.BlockRepository;
import br.edu.chat.repository.DirectPermissionRepository;
import br.edu.chat.repository.GroupRepository;
import br.edu.chat.repository.MessageRepository;
import br.edu.chat.repository.RequestRepository;
import br.edu.chat.repository.UserRepository;

import java.util.ArrayList;
import java.util.List;

public class ServiceTestMain {

    static List<String> sentMessages = new ArrayList<>();

    static MessageSender stubSender = new MessageSender() {
        @Override
        public void sendToUser(String userLogin, String message) {
            String entry = "[-> " + userLogin + "] " + message;
            sentMessages.add(entry);
            System.out.println("  MSG ENVIADA: " + entry);
        }

        @Override
        public boolean isOnline(String userLogin) {
            return true;
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
                requestRepo, userRepo, groupRepo, permRepo, stubSender);

        System.out.println();
        System.out.println("===== TESTE DOS SERVICOS =====");

        System.out.println();
        System.out.println("--- Cadastro de usuarios ---");
        System.out.println(userService.register("Alice Ferreira", "alice", "alice@email.com", "senha123"));
        System.out.println(userService.register("Bruno Carvalho", "bruno", "bruno@email.com", "senha123"));
        System.out.println(userService.register("Alice Ferreira", "alice", "alice2@email.com", "senha123"));

        System.out.println();
        System.out.println("--- Login ---");
        User alice = userService.login("alice", "senha123");
        User bruno = userService.login("bruno", "senha123");
        System.out.println("Alice logada: " + (alice != null ? alice.getLogin() : "FALHA"));
        System.out.println("Bruno logado: " + (bruno != null ? bruno.getLogin() : "FALHA"));
        System.out.println("Login invalido: " + (userService.login("alice", "errada") != null ? "AUTENTICOU" : "RECUSADO"));

        System.out.println();
        System.out.println("--- Listar usuarios ---");
        System.out.println(userService.listUsers());

        System.out.println();
        System.out.println("--- Mensagem direta (sem permissao) ---");
        System.out.println(messageService.sendDirect(alice.getId(), "bruno", "Ola Bruno!"));

        System.out.println();
        System.out.println("--- Aceitar pedido de permissao ---");
        List<PendingRequest> permRequests = requestRepo.findPendingByTarget(bruno.getId());
        if (!permRequests.isEmpty()) {
            System.out.println(requestService.accept(bruno.getId(), permRequests.get(0).getId()));
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
        List<PendingRequest> inviteRequests = requestRepo.findPendingByTarget(bruno.getId());
        if (!inviteRequests.isEmpty()) {
            System.out.println(requestService.accept(bruno.getId(), inviteRequests.get(0).getId()));
        } else {
            System.out.println("[AVISO] Nenhum convite pendente para Bruno.");
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
        System.out.println("--- Bruno pede para entrar no grupo ---");
        System.out.println(groupService.requestJoin(bruno.getId(), "devs"));

        System.out.println();
        System.out.println("--- Alice aceita pedido de entrada ---");
        List<PendingRequest> joinRequests = requestRepo.findPendingByTarget(alice.getId());
        if (!joinRequests.isEmpty()) {
            System.out.println(requestService.accept(alice.getId(), joinRequests.get(0).getId()));
        } else {
            System.out.println("[AVISO] Nenhum pedido de entrada pendente para Alice.");
        }
        System.out.println(groupService.listGroupMembers("devs"));

        System.out.println();
        System.out.println("--- Promover Bruno a admin ---");
        System.out.println(groupService.promoteAdmin(alice.getId(), "devs", "bruno"));
        System.out.println(groupService.listGroupMembers("devs"));

        System.out.println();
        System.out.println("--- Logout ---");
        System.out.println(userService.logout(alice.getId()));
        System.out.println(userService.logout(bruno.getId()));

        System.out.println();
        System.out.println("===== FIM DO TESTE DE SERVICOS =====");
    }
}
