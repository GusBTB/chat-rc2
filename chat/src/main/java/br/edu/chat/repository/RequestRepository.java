package br.edu.chat.repository;

import br.edu.chat.database.DatabaseConnection;
import br.edu.chat.model.PendingRequest;
import br.edu.chat.model.RequestStatus;
import br.edu.chat.model.RequestType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RequestRepository {

    public int save(PendingRequest request) {
        String sql = """
                INSERT INTO pending_requests (
                    request_type,
                    requester_user_id,
                    target_user_id,
                    group_id,
                    status,
                    created_at
                ) VALUES (?, ?, ?, ?, ?, ?);
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, request.getRequestType().name());
            statement.setInt(2, request.getRequesterUserId());

            if (request.getTargetUserId() != null) {
                statement.setInt(3, request.getTargetUserId());
            } else {
                statement.setNull(3, Types.INTEGER);
            }

            if (request.getGroupId() != null) {
                statement.setInt(4, request.getGroupId());
            } else {
                statement.setNull(4, Types.INTEGER);
            }

            statement.setString(5, request.getStatus().name());
            statement.setString(6, LocalDateTime.now().toString());

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    request.setId(id);
                    return id;
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao salvar pedido pendente.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return -1;
    }

    public PendingRequest findById(int id) {
        String sql = """
                SELECT id, request_type, requester_user_id, target_user_id, group_id, status, created_at
                FROM pending_requests
                WHERE id = ?;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSet(resultSet);
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao buscar pedido por id.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return null;
    }

    public List<PendingRequest> findPendingByTarget(int targetUserId) {
        String sql = """
                SELECT id, request_type, requester_user_id, target_user_id, group_id, status, created_at
                FROM pending_requests
                WHERE target_user_id = ?
                AND status = 'PENDING';
                """;

        List<PendingRequest> requests = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, targetUserId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    requests.add(mapResultSet(resultSet));
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao buscar pedidos pendentes.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return requests;
    }

    public List<PendingRequest> findPendingByRequester(int requesterUserId) {
        String sql = """
                SELECT id, request_type, requester_user_id, target_user_id, group_id, status, created_at
                FROM pending_requests
                WHERE requester_user_id = ?
                AND status = 'PENDING';
                """;

        List<PendingRequest> requests = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, requesterUserId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    requests.add(mapResultSet(resultSet));
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao buscar pedidos enviados pendentes.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return requests;
    }

    public boolean updateStatus(int id, RequestStatus status) {
        String sql = """
                UPDATE pending_requests
                SET status = ?
                WHERE id = ?;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status.name());
            statement.setInt(2, id);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao atualizar status do pedido.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return false;
    }

    private PendingRequest mapResultSet(ResultSet resultSet) throws SQLException {
        int targetId = resultSet.getInt("target_user_id");
        Integer targetUserId = resultSet.wasNull() ? null : targetId;

        int gId = resultSet.getInt("group_id");
        Integer groupId = resultSet.wasNull() ? null : gId;

        return new PendingRequest(
                resultSet.getInt("id"),
                RequestType.valueOf(resultSet.getString("request_type")),
                resultSet.getInt("requester_user_id"),
                targetUserId,
                groupId,
                RequestStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("created_at"));
    }
}
