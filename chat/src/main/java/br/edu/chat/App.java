package br.edu.chat;

import br.edu.chat.database.DatabaseInitializer;
import br.edu.chat.testmanual.DatabaseUserTest;
import br.edu.chat.testmanual.ModelTest;
import br.edu.chat.testmanual.RepositoryTestMain;
import br.edu.chat.testmanual.ServiceTestMain;

public class App {
    public static void main(String[] args) {
        System.out.println("Iniciando Chat TCP...");

        //DatabaseInitializer.initialize();
        //RepositoryTestMain.main(args);
        //ModelTest.main(args);
        //DatabaseUserTest.main(args);
        ServiceTestMain.main(args);

        System.out.println("Aplicacao finalizada.");
    }
}
