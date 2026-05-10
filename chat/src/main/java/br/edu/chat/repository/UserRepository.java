package br.edu.chat.repository;

import br.edu.chat.database.DatabaseConnection;
import br.edu.chat.model.User;
import br.edu.chat.model.UserStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserRepository {

    public int create(User user) {
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
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, user.getFullName());
            statement.setString(2, user.getLogin());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getPassword());
            statement.setString(5, user.getStatus().name());
            statement.setString(6, LocalDateTime.now().toString());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    user.setId(id);
                    return id;
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao cadastrar usuario.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return -1;
    }

    public User findByLogin(String login) {
        String sql = """
                SELECT id, full_name, login, email, password, status
                FROM users
                WHERE login = ?;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, login);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToUser(resultSet);
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao buscar usuario por login.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return null;
    }

    public User findByEmail(String email) {
        String sql = """
                SELECT id, full_name, login, email, password, status
                FROM users
                WHERE email = ?;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToUser(resultSet);
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao buscar usuario por email.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return null;
    }

    public User findById(int id) {
        String sql = """
                SELECT id, full_name, login, email, password, status
                FROM users
                WHERE id = ?;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToUser(resultSet);
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao buscar usuario por id.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return null;
    }

    public List<User> listAll() {
        String sql = """
                SELECT id, full_name, login, email, password, status
                FROM users
                ORDER BY login;
                """;

        List<User> users = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                users.add(mapResultSetToUser(resultSet));
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao listar usuarios.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return users;
    }

    public List<User> listOnline() {
        String sql = """
                SELECT id, full_name, login, email, password, status
                FROM users
                WHERE status = ?
                ORDER BY login;
                """;

        List<User> users = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, UserStatus.ONLINE.name());

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(mapResultSetToUser(resultSet));
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao listar usuarios online.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return users;
    }

    public boolean updateStatus(int userId, UserStatus status) {
        String sql = """
                UPDATE users
                SET status = ?
                WHERE id = ?;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status.name());
            statement.setInt(2, userId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao atualizar status do usuario.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return false;
    }

    public boolean existsLogin(String login) {
        String sql = """
                SELECT 1
                FROM users
                WHERE login = ?;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, login);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao verificar login.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return false;
    }

    public boolean existsEmail(String email) {
        String sql = """
                SELECT 1
                FROM users
                WHERE email = ?;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao verificar email.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return false;
    }

    private User mapResultSetToUser(ResultSet resultSet) throws SQLException {
        return new User(
                resultSet.getInt("id"),
                resultSet.getString("full_name"),
                resultSet.getString("login"),
                resultSet.getString("email"),
                resultSet.getString("password"),
                UserStatus.valueOf(resultSet.getString("status")));
    }
}