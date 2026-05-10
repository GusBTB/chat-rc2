package br.edu.chat.repository;

import br.edu.chat.database.DatabaseConnection;
import br.edu.chat.model.Group;
import br.edu.chat.model.User;
import br.edu.chat.model.UserStatus;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class GroupRepository {

    public int createGroup(String name, int creatorId) {
        String sql = """
                INSERT INTO groups_chat (
                    name,
                    created_by_user_id,
                    created_at
                ) VALUES (?, ?, ?);
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, name);
            statement.setInt(2, creatorId);
            statement.setString(3, LocalDateTime.now().toString());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao criar grupo.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return -1;
    }

    public Group findByName(String name) {
        String sql = """
                SELECT id, name, created_by_user_id
                FROM groups_chat
                WHERE name = ?;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, name);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new Group(
                            resultSet.getInt("id"),
                            resultSet.getString("name"),
                            resultSet.getInt("created_by_user_id"));
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao buscar grupo por nome.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return null;
    }

    public List<Group> listGroups() {
        String sql = """
                SELECT id, name, created_by_user_id
                FROM groups_chat
                ORDER BY name;
                """;

        List<Group> groups = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                groups.add(new Group(
                        resultSet.getInt("id"),
                        resultSet.getString("name"),
                        resultSet.getInt("created_by_user_id")));
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao listar grupos.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return groups;
    }

    public boolean addMember(int groupId, int userId, boolean admin) {
        String sql = """
                INSERT INTO group_members (
                    group_id,
                    user_id,
                    is_admin,
                    joined_at
                ) VALUES (?, ?, ?, ?);
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, groupId);
            statement.setInt(2, userId);
            statement.setInt(3, admin ? 1 : 0);
            statement.setString(4, LocalDateTime.now().toString());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao adicionar membro ao grupo.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return false;
    }

    public boolean removeMember(int groupId, int userId) {
        String sql = """
                DELETE FROM group_members
                WHERE group_id = ?
                AND user_id = ?;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, groupId);
            statement.setInt(2, userId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao remover membro do grupo.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return false;
    }

    public List<User> listMembers(int groupId) {
        String sql = """
                SELECT u.id, u.full_name, u.login, u.email, u.password, u.status
                FROM users u
                INNER JOIN group_members gm ON gm.user_id = u.id
                WHERE gm.group_id = ?
                ORDER BY u.login;
                """;

        List<User> users = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, groupId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(mapResultSetToUser(resultSet));
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao listar membros do grupo.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return users;
    }

    public List<User> listAdmins(int groupId) {
        String sql = """
                SELECT u.id, u.full_name, u.login, u.email, u.password, u.status
                FROM users u
                INNER JOIN group_members gm ON gm.user_id = u.id
                WHERE gm.group_id = ?
                AND gm.is_admin = 1
                ORDER BY u.login;
                """;

        List<User> users = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, groupId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    users.add(mapResultSetToUser(resultSet));
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao listar administradores do grupo.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return users;
    }

    public boolean isMember(int groupId, int userId) {
        String sql = """
                SELECT 1
                FROM group_members
                WHERE group_id = ?
                AND user_id = ?;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, groupId);
            statement.setInt(2, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao verificar membro do grupo.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return false;
    }

    public boolean isAdmin(int groupId, int userId) {
        String sql = """
                SELECT 1
                FROM group_members
                WHERE group_id = ?
                AND user_id = ?
                AND is_admin = 1;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, groupId);
            statement.setInt(2, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao verificar administrador do grupo.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return false;
    }

    public boolean makeAdmin(int groupId, int userId) {
        String sql = """
                UPDATE group_members
                SET is_admin = 1
                WHERE group_id = ?
                AND user_id = ?;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, groupId);
            statement.setInt(2, userId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao tornar usuario administrador.");
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