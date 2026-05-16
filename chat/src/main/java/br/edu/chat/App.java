package br.edu.chat;

import br.edu.chat.database.DatabaseInitializer;

public class App {
    public static void main(String[] args) {
        System.out.println("Iniciando Chat TCP...");

        DatabaseInitializer.initialize();

        System.out.println("Aplicacao finalizada.");
    }
}
