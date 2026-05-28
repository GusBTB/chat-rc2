package br.edu.chat.core;

import br.edu.chat.model.User;
import br.edu.chat.model.UserStatus;
import br.edu.chat.protocol.ClientCommand;
import br.edu.chat.protocol.CommandParser;
import br.edu.chat.protocol.CommandType;
import br.edu.chat.protocol.ServerResponse;
import br.edu.chat.repository.BlockRepository;
import br.edu.chat.repository.DirectPermissionRepository;
import br.edu.chat.repository.GroupRepository;
import br.edu.chat.repository.MessageRepository;
import br.edu.chat.repository.RequestRepository;
import br.edu.chat.repository.UserRepository;
import br.edu.chat.server.ClientHandler;
import br.edu.chat.server.ClientSessionManager;

import java.util.ArrayList;
import java.util.List;

public class CommandProcessor {

    private final CommandParser parser;
    private final ClientSessionManager sessionManager;
    private final UserRepository userRepo;
    private final UserService userService;
    private final GroupService groupService;
    private final MessageService messageService;
    private final BlockService blockService;
    private final RequestService requestService;

    public CommandProcessor(ClientSessionManager sessionManager) {
        this.parser = new CommandParser();
        this.sessionManager = sessionManager;

        this.userRepo = new UserRepository();
        GroupRepository groupRepo = new GroupRepository();
        BlockRepository blockRepo = new BlockRepository();
        MessageRepository messageRepo = new MessageRepository();
        DirectPermissionRepository permRepo = new DirectPermissionRepository();
        RequestRepository requestRepo = new RequestRepository();

        this.userService = new UserService(userRepo, messageRepo, sessionManager);
        this.groupService = new GroupService(groupRepo, userRepo, requestRepo, sessionManager);
        this.messageService = new MessageService(
                messageRepo, userRepo, groupRepo, blockRepo, permRepo, requestRepo, sessionManager);
        this.blockService = new BlockService(blockRepo, userRepo);
        this.requestService = new RequestService(requestRepo, userRepo, groupRepo, permRepo, messageRepo,
                sessionManager);
    }

    public List<String> process(String line, ClientHandler clientHandler) {
        List<String> responses = new ArrayList<>();
        ClientCommand command = parser.parse(line);

        if (command.isInvalid()) {
            responses.add(ServerResponse.error(command.getErrorMessage()));
            return responses;
        }

        if (command.isUnknown()) {
            responses.add(buildUnknownCommandMessage(line, command.getErrorMessage()));
            return responses;
        }

        if (command.getType() == CommandType.AJUDA) {
            responses.add(help());
            return responses;
        }

        if (command.getType() == CommandType.CADASTRO) {
            responses.add(processCadastro(command));
            return responses;
        }

        if (command.getType() == CommandType.LOGIN) {
            responses.add(processLogin(command, clientHandler));
            return responses;
        }

        if (command.getType() == CommandType.RECUPERAR_SENHA) {
            responses.add(userService.recoverPassword(command.getArgs().get(0)));
            return responses;
        }

        if (!clientHandler.isLoggedIn()) {
            responses.add(ServerResponse.error("Voce precisa fazer login antes de usar este comando."));
            return responses;
        }

        switch (command.getType()) {
            case LOGOUT:
                responses.add(processLogout(clientHandler));
                break;

            case STATUS:
                responses.add(processStatus(command, clientHandler));
                break;

            case LISTA_USUARIOS:
                responses.add(userService.listOnlineUsers());
                break;

            case LISTA_GRUPOS:
                responses.add(groupService.listGroups());
                break;

            case LISTAR_GRUPO:
                responses.add(groupService.listGroupMembers(command.getGroupName()));
                break;

            case DIRECT_MESSAGE:
                responses.add(processIfCanSend(clientHandler,
                        () -> messageService.sendDirect(
                                clientHandler.getLoggedUserId(),
                                command.getTargetUsers().get(0),
                                command.getContent())));
                break;

            case GROUP_MESSAGE:
                responses.add(processIfCanSend(clientHandler,
                        () -> messageService.sendGroup(
                                clientHandler.getLoggedUserId(),
                                command.getGroupName(),
                                command.getContent())));
                break;

            case GROUP_DIRECT_MESSAGE:
                responses.add(processIfCanSend(clientHandler,
                        () -> messageService.sendGroupDirect(
                                clientHandler.getLoggedUserId(),
                                command.getGroupName(),
                                command.getTargetUsers(),
                                command.getContent())));
                break;

            case NOVO_GRUPO:
                responses.add(processIfCanSend(clientHandler,
                        () -> groupService.createGroup(
                                clientHandler.getLoggedUserId(),
                                command.getGroupName())));
                break;

            case INSERIR_GRUPO:
                responses.addAll(processInsertGroup(command, clientHandler));
                break;

            case ENTRAR_GRUPO:
                responses.add(processIfCanSend(clientHandler,
                        () -> groupService.requestJoin(
                                clientHandler.getLoggedUserId(),
                                command.getGroupName())));
                break;

            case SAIR_GRUPO:
                responses.add(groupService.leaveGroup(
                        clientHandler.getLoggedUserId(),
                        command.getGroupName()));
                break;

            case PROMOVER_ADMIN:
                responses.add(processIfCanSend(clientHandler,
                        () -> groupService.promoteAdmin(
                                clientHandler.getLoggedUserId(),
                                command.getGroupName(),
                                command.getTargetUsers().get(0))));
                break;

            case BLOQUEAR:
                responses.add(blockService.block(
                        clientHandler.getLoggedUserId(),
                        command.getTargetUsers().get(0)));
                break;

            case DESBLOQUEAR:
                responses.add(blockService.unblock(
                        clientHandler.getLoggedUserId(),
                        command.getTargetUsers().get(0)));
                break;

            case ACEITAR:
                responses.add(requestService.accept(
                        clientHandler.getLoggedUserId(),
                        command.getRequestId()));
                break;

            case RECUSAR:
                responses.add(requestService.refuse(
                        clientHandler.getLoggedUserId(),
                        command.getRequestId()));
                break;

            default:
                responses.add(ServerResponse.error("Comando ainda nao implementado: " + command.getType()));
                break;
        }

        return responses;
    }

    public void disconnect(ClientHandler clientHandler) {
        if (clientHandler == null || !clientHandler.isLoggedIn()) {
            return;
        }

        Integer userId = clientHandler.getLoggedUserId();
        String login = clientHandler.getLoggedUserLogin();

        sessionManager.removeIfSameHandler(userId, login, clientHandler);
        userService.logout(userId);
        clientHandler.clearLoggedUser();

        System.out.println("Usuario desconectado e marcado como OFFLINE: " + login);
    }

    private String processCadastro(ClientCommand command) {
        return userService.register(
                command.getArgs().get(0),
                command.getArgs().get(1),
                command.getArgs().get(2),
                command.getArgs().get(3));
    }

    private String processLogin(ClientCommand command, ClientHandler clientHandler) {
        if (clientHandler.isLoggedIn()) {
            return ServerResponse.error("Voce ja esta logado como '" + clientHandler.getLoggedUserLogin() + "'.");
        }

        String login = command.getArgs().get(0);

        if (sessionManager.isConnected(login)) {
            return ServerResponse.error("O usuario '" + login + "' ja esta logado em outro cliente.");
        }

        User user = userService.login(login, command.getArgs().get(1));
        if (user == null) {
            return ServerResponse.error("Login ou senha invalidos.");
        }

        clientHandler.setLoggedUser(user);
        sessionManager.add(user.getId(), user.getLogin(), clientHandler);
        userService.deliverPendingMessages(user);
        requestService.deliverPendingForUser(user.getId());

        return ServerResponse.ok("Login realizado com sucesso. Usuario atual: " + user.getLogin() + ".");
    }

    private String processLogout(ClientHandler clientHandler) {
        Integer userId = clientHandler.getLoggedUserId();
        String login = clientHandler.getLoggedUserLogin();

        sessionManager.removeIfSameHandler(userId, login, clientHandler);
        String response = userService.logout(userId);
        clientHandler.clearLoggedUser();

        System.out.println("Usuario fez logout e foi marcado como OFFLINE: " + login);

        return response;
    }

    private String processStatus(ClientCommand command, ClientHandler clientHandler) {
        UserStatus status = UserStatus.valueOf(command.getArgs().get(0));
        Integer userId = clientHandler.getLoggedUserId();
        String login = clientHandler.getLoggedUserLogin();

        boolean updated = userRepo.updateStatus(userId, status);
        if (!updated) {
            return ServerResponse.error("Nao foi possivel atualizar o status.");
        }

        if (status == UserStatus.ONLINE) {
            sessionManager.add(userId, login, clientHandler);
            User user = userRepo.findById(userId);
            if (user != null) {
                userService.deliverPendingMessages(user);
                requestService.deliverPendingForUser(userId);
            }
        } else {
            sessionManager.remove(userId, login);
        }

        return ServerResponse.ok("Status alterado para " + status.name() + ".");
    }

    private List<String> processInsertGroup(ClientCommand command, ClientHandler clientHandler) {
        List<String> responses = new ArrayList<>();

        String canSendResult = canSend(clientHandler);
        if (canSendResult != null) {
            responses.add(canSendResult);
            return responses;
        }

        for (String targetUser : command.getTargetUsers()) {
            responses.add(groupService.insertMember(
                    clientHandler.getLoggedUserId(),
                    command.getGroupName(),
                    targetUser));
        }

        return responses;
    }

    private String processIfCanSend(ClientHandler clientHandler, CommandAction action) {
        String error = canSend(clientHandler);
        if (error != null) {
            return error;
        }

        return action.execute();
    }

    private String canSend(ClientHandler clientHandler) {
        if (!sessionManager.isConnected(clientHandler.getLoggedUserId())) {
            return ServerResponse
                    .error("Seu status atual nao permite enviar mensagens ou executar esta acao. Altere para ONLINE.");
        }

        return null;
    }

    private String help() {
        return """
                Comandos disponiveis:
                  cadastro;Nome Completo;login;email;senha
                  login;login;senha
                  logout
                  recuperarsenha;email
                  status;ONLINE|OFFLINE|OCUPADO|AUSENTE
                  listausuarios
                  listagrupos
                  listargrupo &nomegrupo
                  @usuario: mensagem
                  novogrupo nomegrupo
                  inserir &nomegrupo@usuario1,@usuario2
                  entrar &nomegrupo
                  &nomegrupo: mensagem
                  &nomegrupo@usuario1,@usuario2: mensagem
                  promover &nomegrupo@usuario
                  sair &nomegrupo
                  bloquear @usuario
                  desbloquear @usuario
                  aceitar id
                  recusar id""";
    }

    private String buildUnknownCommandMessage(String line, String originalErrorMessage) {
        String firstToken = extractFirstToken(line);
        String suggestion = findClosestCommand(firstToken);

        if (suggestion == null) {
            return ServerResponse.error(originalErrorMessage + "\nDigite ajuda para ver os comandos disponiveis.");
        }

        return ServerResponse.error(originalErrorMessage
                + "\nTalvez voce quis dizer: " + suggestion
                + "\nDigite ajuda para ver os comandos disponiveis.");
    }

    private String extractFirstToken(String line) {
        if (line == null) {
            return "";
        }

        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        int semicolonIndex = trimmed.indexOf(';');
        int spaceIndex = trimmed.indexOf(' ');
        int colonIndex = trimmed.indexOf(':');

        int end = trimmed.length();

        if (semicolonIndex >= 0) {
            end = Math.min(end, semicolonIndex);
        }

        if (spaceIndex >= 0) {
            end = Math.min(end, spaceIndex);
        }

        if (colonIndex >= 0) {
            end = Math.min(end, colonIndex);
        }

        return trimmed.substring(0, end).toLowerCase();
    }

    private String findClosestCommand(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String[] commands = {
                "cadastro",
                "login",
                "logout",
                "recuperarsenha",
                "status",
                "listausuarios",
                "listagrupos",
                "listargrupo",
                "novogrupo",
                "inserir",
                "entrar",
                "sair",
                "promover",
                "bloquear",
                "desbloquear",
                "aceitar",
                "recusar",
                "ajuda"
        };

        String bestCommand = null;
        int bestDistance = Integer.MAX_VALUE;

        for (String command : commands) {
            int distance = levenshteinDistance(token, command);

            if (distance < bestDistance) {
                bestDistance = distance;
                bestCommand = command;
            }
        }

        if (bestDistance <= 2) {
            return bestCommand;
        }

        return null;
    }

    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;

                dp[i][j] = Math.min(
                        Math.min(
                                dp[i - 1][j] + 1,
                                dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }

        return dp[a.length()][b.length()];
    }

    private interface CommandAction {
        String execute();
    }
}