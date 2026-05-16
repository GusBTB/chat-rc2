package br.edu.chat.protocol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandParser {

    private static final Pattern GROUP_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_]+");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9_]+");

    public ClientCommand parse(String rawInput) {
        if (rawInput == null) {
            return invalid("", "Comando vazio.");
        }

        String input = rawInput.trim();

        if (input.isEmpty()) {
            return invalid(rawInput, "Comando vazio.");
        }

        if (input.startsWith("@")) {
            return parseDirectMessage(rawInput, input);
        }

        if (input.startsWith("&")) {
            return parseGroupMessageOrDirectInGroup(rawInput, input);
        }

        String lower = input.toLowerCase();

        if (lower.startsWith("cadastro;") || lower.equals("cadastro")) {
            return parseCadastro(rawInput, input);
        }

        if (lower.startsWith("login;") || lower.equals("login")) {
            return parseLogin(rawInput, input);
        }

        if (lower.equals("logout")) {
            return ClientCommand.builder(CommandType.LOGOUT).rawInput(rawInput).build();
        }

        if (lower.startsWith("recuperarsenha;") || lower.equals("recuperarsenha")) {
            return parseRecuperarSenha(rawInput, input);
        }

        if (lower.startsWith("status;") || lower.equals("status")) {
            return parseStatus(rawInput, input);
        }

        if (lower.equals("listausuarios")) {
            return ClientCommand.builder(CommandType.LISTA_USUARIOS).rawInput(rawInput).build();
        }

        if (lower.equals("listagrupos")) {
            return ClientCommand.builder(CommandType.LISTA_GRUPOS).rawInput(rawInput).build();
        }

        if (lower.startsWith("listargrupo ") || lower.equals("listargrupo")) {
            return parseListarGrupo(rawInput, input);
        }

        if (lower.startsWith("novogrupo ") || lower.equals("novogrupo")) {
            return parseNovoGrupo(rawInput, input);
        }

        if (lower.startsWith("inserir ") || lower.equals("inserir")) {
            return parseInserir(rawInput, input);
        }

        if (lower.startsWith("entrar ") || lower.equals("entrar")) {
            return parseEntrar(rawInput, input);
        }

        if (lower.startsWith("sair ") || lower.equals("sair")) {
            return parseSair(rawInput, input);
        }

        if (lower.startsWith("promover ") || lower.equals("promover")) {
            return parsePromover(rawInput, input);
        }

        if (lower.startsWith("bloquear ") || lower.equals("bloquear")) {
            return parseBloquear(rawInput, input, CommandType.BLOQUEAR);
        }

        if (lower.startsWith("desbloquear ") || lower.equals("desbloquear")) {
            return parseBloquear(rawInput, input, CommandType.DESBLOQUEAR);
        }

        if (lower.startsWith("aceitar ") || lower.equals("aceitar")) {
            return parseAceitarRecusar(rawInput, input, CommandType.ACEITAR);
        }

        if (lower.startsWith("recusar ") || lower.equals("recusar")) {
            return parseAceitarRecusar(rawInput, input, CommandType.RECUSAR);
        }

        if (lower.equals("ajuda") || lower.equals("help") || lower.equals("?")) {
            return ClientCommand.builder(CommandType.AJUDA).rawInput(rawInput).build();
        }

        return ClientCommand.builder(CommandType.UNKNOWN)
                .rawInput(rawInput)
                .errorMessage("Comando desconhecido: " + input)
                .build();
    }

    private ClientCommand parseCadastro(String rawInput, String input) {
        String[] parts = input.split(";", -1);
        if (parts.length != 5) {
            return invalid(rawInput,
                    "Formato esperado: cadastro;Nome Completo;login;email;senha");
        }
        String fullName = parts[1].trim();
        String login = parts[2].trim();
        String email = parts[3].trim();
        String password = parts[4];

        if (fullName.isEmpty() || login.isEmpty() || email.isEmpty() || password.isEmpty()) {
            return invalid(rawInput,
                    "Todos os campos de cadastro sao obrigatorios.");
        }

        return ClientCommand.builder(CommandType.CADASTRO)
                .rawInput(rawInput)
                .addArg(fullName)
                .addArg(login)
                .addArg(email)
                .addArg(password)
                .build();
    }

    private ClientCommand parseLogin(String rawInput, String input) {
        String[] parts = input.split(";", -1);
        if (parts.length != 3) {
            return invalid(rawInput, "Formato esperado: login;login;senha");
        }
        String login = parts[1].trim();
        String password = parts[2];

        if (login.isEmpty() || password.isEmpty()) {
            return invalid(rawInput, "Login e senha sao obrigatorios.");
        }

        return ClientCommand.builder(CommandType.LOGIN)
                .rawInput(rawInput)
                .addArg(login)
                .addArg(password)
                .build();
    }

    private ClientCommand parseRecuperarSenha(String rawInput, String input) {
        String[] parts = input.split(";", -1);
        if (parts.length != 2) {
            return invalid(rawInput, "Formato esperado: recuperarsenha;email");
        }
        String email = parts[1].trim();
        if (email.isEmpty()) {
            return invalid(rawInput, "Informe o email para recuperar a senha.");
        }
        return ClientCommand.builder(CommandType.RECUPERAR_SENHA)
                .rawInput(rawInput)
                .addArg(email)
                .build();
    }

    private ClientCommand parseStatus(String rawInput, String input) {
        String[] parts = input.split(";", -1);
        if (parts.length != 2) {
            return invalid(rawInput,
                    "Formato esperado: status;ONLINE|OFFLINE|OCUPADO|AUSENTE");
        }
        String value = parts[1].trim().toUpperCase();
        List<String> validValues = Arrays.asList("ONLINE", "OFFLINE", "OCUPADO", "AUSENTE");
        if (!validValues.contains(value)) {
            return invalid(rawInput,
                    "Status invalido. Valores aceitos: ONLINE, OFFLINE, OCUPADO, AUSENTE.");
        }
        return ClientCommand.builder(CommandType.STATUS)
                .rawInput(rawInput)
                .addArg(value)
                .build();
    }

    private ClientCommand parseListarGrupo(String rawInput, String input) {
        String rest = input.length() > "listargrupo".length()
                ? input.substring("listargrupo".length()).trim()
                : "";
        if (rest.isEmpty()) {
            return invalid(rawInput, "Formato esperado: listargrupo &nomegrupo");
        }
        if (!rest.startsWith("&")) {
            return invalid(rawInput,
                    "Formato esperado: listargrupo &nomegrupo (use & antes do nome).");
        }
        String groupName = rest.substring(1).trim();
        if (!isValidGroupName(groupName)) {
            return invalid(rawInput, "Nome de grupo invalido: " + groupName);
        }
        return ClientCommand.builder(CommandType.LISTAR_GRUPO)
                .rawInput(rawInput)
                .groupName(groupName)
                .build();
    }

    private ClientCommand parseNovoGrupo(String rawInput, String input) {
        String rest = input.length() > "novogrupo".length()
                ? input.substring("novogrupo".length()).trim()
                : "";
        if (rest.isEmpty()) {
            return invalid(rawInput, "Formato esperado: novogrupo nomegrupo");
        }
        if (!isValidGroupName(rest)) {
            return invalid(rawInput,
                    "Nome de grupo invalido. Use apenas letras, numeros e _.");
        }
        return ClientCommand.builder(CommandType.NOVO_GRUPO)
                .rawInput(rawInput)
                .groupName(rest)
                .build();
    }

    private ClientCommand parseInserir(String rawInput, String input) {
        String rest = input.substring("inserir".length()).trim();
        if (rest.isEmpty()) {
            return invalid(rawInput,
                    "Formato esperado: inserir &nomegrupo@usuario1,@usuario2");
        }
        if (!rest.startsWith("&")) {
            return invalid(rawInput,
                    "Formato esperado: inserir &nomegrupo@usuario1,@usuario2");
        }

        int atIdx = rest.indexOf('@');
        if (atIdx <= 1) {
            return invalid(rawInput,
                    "Formato esperado: inserir &nomegrupo@usuario1,@usuario2");
        }

        String groupName = rest.substring(1, atIdx).trim();
        String usersPart = rest.substring(atIdx);

        if (!isValidGroupName(groupName)) {
            return invalid(rawInput, "Nome de grupo invalido: " + groupName);
        }

        List<String> users = extractUsers(usersPart);
        if (users.isEmpty()) {
            return invalid(rawInput,
                    "Informe ao menos um usuario para inserir.");
        }
        for (String u : users) {
            if (!isValidUsername(u)) {
                return invalid(rawInput, "Nome de usuario invalido: " + u);
            }
        }

        return ClientCommand.builder(CommandType.INSERIR_GRUPO)
                .rawInput(rawInput)
                .groupName(groupName)
                .targetUsers(users)
                .build();
    }

    private ClientCommand parseEntrar(String rawInput, String input) {
        String rest = input.substring("entrar".length()).trim();
        if (rest.isEmpty() || !rest.startsWith("&")) {
            return invalid(rawInput, "Formato esperado: entrar &nomegrupo");
        }
        String groupName = rest.substring(1).trim();
        if (!isValidGroupName(groupName)) {
            return invalid(rawInput, "Nome de grupo invalido: " + groupName);
        }
        return ClientCommand.builder(CommandType.ENTRAR_GRUPO)
                .rawInput(rawInput)
                .groupName(groupName)
                .build();
    }

    private ClientCommand parseSair(String rawInput, String input) {
        String rest = input.substring("sair".length()).trim();
        if (rest.isEmpty() || !rest.startsWith("&")) {
            return invalid(rawInput, "Formato esperado: sair &nomegrupo");
        }
        String groupName = rest.substring(1).trim();
        if (!isValidGroupName(groupName)) {
            return invalid(rawInput, "Nome de grupo invalido: " + groupName);
        }
        return ClientCommand.builder(CommandType.SAIR_GRUPO)
                .rawInput(rawInput)
                .groupName(groupName)
                .build();
    }

    private ClientCommand parsePromover(String rawInput, String input) {
        String rest = input.substring("promover".length()).trim();
        if (rest.isEmpty() || !rest.startsWith("&")) {
            return invalid(rawInput, "Formato esperado: promover &nomegrupo@usuario");
        }
        int atIdx = rest.indexOf('@');
        if (atIdx <= 1) {
            return invalid(rawInput, "Formato esperado: promover &nomegrupo@usuario");
        }
        String groupName = rest.substring(1, atIdx).trim();
        String usersPart = rest.substring(atIdx);
        if (!isValidGroupName(groupName)) {
            return invalid(rawInput, "Nome de grupo invalido: " + groupName);
        }
        List<String> users = extractUsers(usersPart);
        if (users.size() != 1) {
            return invalid(rawInput,
                    "Promova um usuario por vez: promover &nomegrupo@usuario");
        }
        if (!isValidUsername(users.get(0))) {
            return invalid(rawInput, "Nome de usuario invalido: " + users.get(0));
        }
        return ClientCommand.builder(CommandType.PROMOVER_ADMIN)
                .rawInput(rawInput)
                .groupName(groupName)
                .targetUsers(users)
                .build();
    }

    private ClientCommand parseBloquear(String rawInput, String input, CommandType type) {
        String keyword = type == CommandType.BLOQUEAR ? "bloquear" : "desbloquear";
        String rest = input.substring(keyword.length()).trim();
        if (rest.isEmpty() || !rest.startsWith("@")) {
            return invalid(rawInput, "Formato esperado: " + keyword + " @usuario");
        }
        String user = rest.substring(1).trim();
        if (!isValidUsername(user)) {
            return invalid(rawInput, "Nome de usuario invalido: " + user);
        }
        return ClientCommand.builder(type)
                .rawInput(rawInput)
                .addTargetUser(user)
                .build();
    }

    private ClientCommand parseAceitarRecusar(String rawInput, String input, CommandType type) {
        String keyword = type == CommandType.ACEITAR ? "aceitar" : "recusar";
        String rest = input.substring(keyword.length()).trim();
        if (rest.isEmpty()) {
            return invalid(rawInput, "Formato esperado: " + keyword + " <id>");
        }
        try {
            int id = Integer.parseInt(rest);
            if (id <= 0) {
                return invalid(rawInput, "Id do pedido deve ser positivo.");
            }
            return ClientCommand.builder(type)
                    .rawInput(rawInput)
                    .requestId(id)
                    .build();
        } catch (NumberFormatException e) {
            return invalid(rawInput, "Id do pedido deve ser um numero inteiro.");
        }
    }

    private ClientCommand parseDirectMessage(String rawInput, String input) {
        int colonIdx = input.indexOf(':');
        if (colonIdx < 0) {
            return invalid(rawInput, "Formato esperado: @usuario: mensagem");
        }
        String userPart = input.substring(1, colonIdx).trim();
        String content = input.substring(colonIdx + 1).trim();

        if (userPart.isEmpty() || content.isEmpty()) {
            return invalid(rawInput, "Formato esperado: @usuario: mensagem");
        }
        if (!isValidUsername(userPart)) {
            return invalid(rawInput, "Nome de usuario invalido: " + userPart);
        }

        return ClientCommand.builder(CommandType.DIRECT_MESSAGE)
                .rawInput(rawInput)
                .addTargetUser(userPart)
                .content(content)
                .build();
    }

    private ClientCommand parseGroupMessageOrDirectInGroup(String rawInput, String input) {
        int colonIdx = input.indexOf(':');
        if (colonIdx < 0) {
            return invalid(rawInput,
                    "Formato esperado: &grupo: mensagem ou &grupo@usuario: mensagem");
        }
        String header = input.substring(1, colonIdx);
        String content = input.substring(colonIdx + 1).trim();

        if (content.isEmpty()) {
            return invalid(rawInput, "Mensagem nao pode ser vazia.");
        }

        int atIdx = header.indexOf('@');

        if (atIdx < 0) {
            String groupName = header.trim();
            if (!isValidGroupName(groupName)) {
                return invalid(rawInput, "Nome de grupo invalido: " + groupName);
            }
            return ClientCommand.builder(CommandType.GROUP_MESSAGE)
                    .rawInput(rawInput)
                    .groupName(groupName)
                    .content(content)
                    .build();
        }

        String groupName = header.substring(0, atIdx).trim();
        String usersPart = header.substring(atIdx);

        if (!isValidGroupName(groupName)) {
            return invalid(rawInput, "Nome de grupo invalido: " + groupName);
        }

        List<String> users = extractUsers(usersPart);
        if (users.isEmpty()) {
            return invalid(rawInput,
                    "Informe ao menos um destinatario: &grupo@usuario: mensagem");
        }
        for (String u : users) {
            if (!isValidUsername(u)) {
                return invalid(rawInput, "Nome de usuario invalido: " + u);
            }
        }

        return ClientCommand.builder(CommandType.GROUP_DIRECT_MESSAGE)
                .rawInput(rawInput)
                .groupName(groupName)
                .targetUsers(users)
                .content(content)
                .build();
    }

    private List<String> extractUsers(String usersPart) {
        List<String> result = new ArrayList<>();
        String[] tokens = usersPart.split(",");
        for (String token : tokens) {
            String t = token.trim();
            if (t.startsWith("@")) {
                t = t.substring(1).trim();
            }
            if (!t.isEmpty()) {
                result.add(t);
            }
        }
        return result;
    }

    private boolean isValidGroupName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        Matcher m = GROUP_NAME_PATTERN.matcher(name);
        return m.matches();
    }

    private boolean isValidUsername(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        Matcher m = USERNAME_PATTERN.matcher(name);
        return m.matches();
    }

    private ClientCommand invalid(String rawInput, String message) {
        return ClientCommand.builder(CommandType.INVALID)
                .rawInput(rawInput)
                .errorMessage(message)
                .build();
    }
}
