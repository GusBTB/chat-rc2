package br.edu.chat.testmanual;

import br.edu.chat.model.Group;
import br.edu.chat.model.Message;
import br.edu.chat.model.MessageType;
import br.edu.chat.model.User;
import br.edu.chat.model.UserStatus;

import java.time.LocalDateTime;

public class ModelTest {

    public static void main(String[] args) {
        User user = new User(
                "Robson Silva",
                "robson",
                "robson@email.com",
                "123",
                UserStatus.ONLINE);

        Group group = new Group(
                "amigos",
                1);

        Message message = new Message(
                1,
                2,
                null,
                "Ola, tudo bem?",
                MessageType.DIRECT,
                LocalDateTime.now().toString());

        System.out.println(user);
        System.out.println(group);
        System.out.println(message);

        System.out.println("Status do usuario: " + user.getStatus());
        System.out.println("Tipo da mensagem: " + message.getMessageType());
    }
}