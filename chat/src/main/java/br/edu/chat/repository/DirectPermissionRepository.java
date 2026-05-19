package br.edu.chat.repository;

import br.edu.chat.database.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;

public class DirectPermissionRepository {

    public boolean grant(int requesterUserId, int targetUserId) {
        String sql = """
                INSERT INTO direct_permissions (
                    requester_user_id,
                    target_user_id,
                    accepted,
                    created_at
                ) VALUES (?, ?, 1, ?)
                ON CONFLICT(requester_user_id, target_user_id)
                DO UPDATE SET accepted = 1;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, requesterUserId);
            statement.setInt(2, targetUserId);
            statement.setString(3, LocalDateTime.now().toString());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao conceder permissao de mensagem direta.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return false;
    }

    public boolean revoke(int requesterUserId, int targetUserId) {
        String sql = """
                UPDATE direct_permissions
                SET accepted = 0
                WHERE requester_user_id = ?
                AND target_user_id = ?;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, requesterUserId);
            statement.setInt(2, targetUserId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao revogar permissao de mensagem direta.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return false;
    }

    public boolean hasPermission(int requesterUserId, int targetUserId) {
        String sql = """
                SELECT 1
                FROM direct_permissions
                WHERE requester_user_id = ?
                AND target_user_id = ?
                AND accepted = 1;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, requesterUserId);
            statement.setInt(2, targetUserId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao verificar permissao de mensagem direta.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return false;
    }

    public boolean hasPendingRequest(int requesterUserId, int targetUserId) {
        String sql = """
                SELECT 1
                FROM direct_permissions
                WHERE requester_user_id = ?
                AND target_user_id = ?;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, requesterUserId);
            statement.setInt(2, targetUserId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao verificar pedido de permissao.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return false;
    }
}
