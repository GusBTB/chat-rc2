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
                    pending_message_id,
                    status,
                    created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?);
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, request.getRequestType().name());
            statement.setInt(2, request.getRequesterUserId());
            setNullableInt(statement, 3, request.getTargetUserId());
            setNullableInt(statement, 4, request.getGroupId());
            setNullableInt(statement, 5, request.getPendingMessageId());
            statement.setString(6, request.getStatus().name());

            String createdAt = request.getCreatedAt();
            if (createdAt == null || createdAt.isBlank()) {
                createdAt = LocalDateTime.now().toString();
                request.setCreatedAt(createdAt);
            }

            statement.setString(7, createdAt);

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
                SELECT id, request_type, requester_user_id, target_user_id, group_id,
                       pending_message_id, status, created_at
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
                SELECT id, request_type, requester_user_id, target_user_id, group_id,
                       pending_message_id, status, created_at
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
                SELECT id, request_type, requester_user_id, target_user_id, group_id,
                       pending_message_id, status, created_at
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

    public boolean existsPending(RequestType requestType, int requesterUserId, Integer targetUserId, Integer groupId) {
        String sql = """
                SELECT 1
                FROM pending_requests
                WHERE request_type = ?
                AND requester_user_id = ?
                AND status = 'PENDING'
                AND (
                    (? IS NULL AND target_user_id IS NULL)
                    OR target_user_id = ?
                )
                AND (
                    (? IS NULL AND group_id IS NULL)
                    OR group_id = ?
                );
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, requestType.name());
            statement.setInt(2, requesterUserId);
            setNullableInt(statement, 3, targetUserId);
            setNullableInt(statement, 4, targetUserId);
            setNullableInt(statement, 5, groupId);
            setNullableInt(statement, 6, groupId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao verificar pedido pendente.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return false;
    }

    public boolean existsPendingGroupJoinForRequester(int requesterUserId, int groupId) {
        String sql = """
                SELECT 1
                FROM pending_requests
                WHERE request_type = 'GROUP_JOIN'
                AND requester_user_id = ?
                AND group_id = ?
                AND status = 'PENDING';
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, requesterUserId);
            statement.setInt(2, groupId);

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao verificar pedido pendente de entrada no grupo.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return false;
    }

    public int countPendingGroupJoinApprovals(int requesterUserId, int groupId) {
        String sql = """
                SELECT COUNT(*) AS total
                FROM pending_requests
                WHERE request_type = 'GROUP_JOIN'
                AND requester_user_id = ?
                AND group_id = ?
                AND status = 'PENDING';
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, requesterUserId);
            statement.setInt(2, groupId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("total");
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao contar aprovacoes pendentes de entrada no grupo.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return 0;
    }

    public boolean refusePendingGroupJoinApprovals(int requesterUserId, int groupId) {
        String sql = """
                UPDATE pending_requests
                SET status = 'REFUSED'
                WHERE request_type = 'GROUP_JOIN'
                AND requester_user_id = ?
                AND group_id = ?
                AND status = 'PENDING';
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, requesterUserId);
            statement.setInt(2, groupId);

            statement.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao cancelar aprovacoes pendentes de entrada no grupo.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return false;
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

    private void setNullableInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private Integer getNullableInt(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private PendingRequest mapResultSet(ResultSet resultSet) throws SQLException {
        return new PendingRequest(
                resultSet.getInt("id"),
                RequestType.valueOf(resultSet.getString("request_type")),
                resultSet.getInt("requester_user_id"),
                getNullableInt(resultSet, "target_user_id"),
                getNullableInt(resultSet, "group_id"),
                getNullableInt(resultSet, "pending_message_id"),
                RequestStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("created_at"));
    }
}