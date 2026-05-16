package br.edu.chat.protocol;

public class ServerResponse {

    public static final String PREFIX_OK = "[OK]";
    public static final String PREFIX_ERR = "[ERRO]";
    public static final String PREFIX_INFO = "[INFO]";
    public static final String PREFIX_SYS = "[SISTEMA]";
    public static final String PREFIX_INVITE = "[CONVITE";
    public static final String PREFIX_REQUEST = "[PEDIDO";
    public static final String PREFIX_GROUP_JOIN = "[SOLICITACAO";

    private ServerResponse() {
    }

    public static String ok(String message) {
        return PREFIX_OK + " " + message;
    }

    public static String error(String message) {
        return PREFIX_ERR + " " + message;
    }

    public static String info(String message) {
        return PREFIX_INFO + " " + message;
    }

    public static String system(String message) {
        return PREFIX_SYS + " " + message;
    }

    public static String invite(int requestId, String message) {
        return PREFIX_INVITE + " " + requestId + "] " + message;
    }

    public static String privateRequest(int requestId, String message) {
        return PREFIX_REQUEST + " " + requestId + "] " + message;
    }

    public static String groupJoinRequest(int requestId, String message) {
        return PREFIX_GROUP_JOIN + " " + requestId + "] " + message;
    }

    public static String directMessage(String sender, String time, String content) {
        return sender + " (" + time + "): " + content;
    }

    public static String groupMessage(String groupName, String sender, String time, String content) {
        return "[" + groupName + "] " + sender + " (" + time + "): " + content;
    }

    public static String groupDirectMessage(String groupName, String sender, String time, String content) {
        return "[" + groupName + " | privada] " + sender + " (" + time + "): " + content;
    }
}
