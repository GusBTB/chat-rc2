package br.edu.chat.core;

public interface MessageSender {

    void sendToUser(String userLogin, String message);

    boolean isOnline(String userLogin);
}
