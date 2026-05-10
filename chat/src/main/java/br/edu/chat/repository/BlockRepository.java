package br.edu.chat.repository;

import br.edu.chat.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class BlockRepository {

    public boolean blockUser(int blockerId, int blockedId) {
        String sql = """
                INSERT INTO blocks (
                    blocker_user_id,
                    blocked_user_id,
                    created_at
                ) VALUES (?, ?, ?);
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, blockerId);
            statement.setInt(2, blockedId);
            statement.setString(3, LocalDateTime.now().toString());

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao bloquear usuario.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return false;
    }

    public boolean unblockUser(int blockerId, int blockedId) {
        String sql = """
                DELETE FROM blocks
                WHERE blocker_user_id = ?
                AND blocked_user_id = ?;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, blockerId);
            statement.setInt(2, blockedId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao desbloquear usuario.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return false;
    }

    public boolean isBlockedBetween(int userA, int userB) {
        String sql = """
                SELECT 1
                FROM blocks
                WHERE
                    (blocker_user_id = ? AND blocked_user_id = ?)
                    OR
                    (blocker_user_id = ? AND blocked_user_id = ?);
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userA);
            statement.setInt(2, userB);
            statement.setInt(3, userB);
            statement.setInt(4, userA);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao verificar bloqueio entre usuarios.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return false;
    }
}