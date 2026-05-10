package br.edu.chat.testmanual;

import br.edu.chat.database.DatabaseInitializer;
import br.edu.chat.database.DatabaseConnection;
import br.edu.chat.model.User;
import br.edu.chat.model.UserStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class DatabaseUserTest {

    public static void main(String[] args) {
        DatabaseInitializer.initialize();

        User user = new User(
                "Usuario Teste",
                "teste",
                "teste@email.com",
                "123",
                UserStatus.OFFLINE);

        insertUser(user);

        User userFromDatabase = findByLogin("teste");

        if (userFromDatabase != null) {
            System.out.println("Usuario encontrado no banco:");
            System.out.println(userFromDatabase);
        } else {
            System.out.println("Usuario nao encontrado.");
        }
    }

    private static void insertUser(User user) {
        String sql = """
                INSERT INTO users (
                    full_name,
                    login,
                    email,
                    password,
                    status,
                    created_at
                ) VALUES (?, ?, ?, ?, ?, ?);
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, user.getFullName());
            statement.setString(2, user.getLogin());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getPassword());
            statement.setString(5, user.getStatus().name());
            statement.setString(6, LocalDateTime.now().toString());

            statement.executeUpdate();

            System.out.println("Usuario inserido com sucesso.");

        } catch (SQLException e) {
            System.out.println("Erro ao inserir usuario.");
            System.out.println("Detalhes: " + e.getMessage());
        }
    }

    private static User findByLogin(String login) {
        String sql = """
                SELECT
                    id,
                    full_name,
                    login,
                    email,
                    password,
                    status
                FROM users
                WHERE login = ?;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, login);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new User(
                            resultSet.getInt("id"),
                            resultSet.getString("full_name"),
                            resultSet.getString("login"),
                            resultSet.getString("email"),
                            resultSet.getString("password"),
                            UserStatus.valueOf(resultSet.getString("status")));
                }
            }

        } catch (SQLException e) {
            System.out.println("Erro ao buscar usuario por login.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return null;
    }
}