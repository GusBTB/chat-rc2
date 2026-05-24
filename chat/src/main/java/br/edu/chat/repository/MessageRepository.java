package br.edu.chat.repository;

import br.edu.chat.database.DatabaseConnection;
import br.edu.chat.model.Message;
import br.edu.chat.model.MessageDelivery;
import br.edu.chat.model.MessageType;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    public int saveMessage(Message message) {
        String sql = """
                INSERT INTO messages (
                    sender_user_id,
                    receiver_user_id,
                    group_id,
                    content,
                    message_type,
                    created_at
                ) VALUES (?, ?, ?, ?, ?, ?);
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, message.getSenderUserId());

            if (message.getReceiverUserId() == null) {
                statement.setNull(2, Types.INTEGER);
            } else {
                statement.setInt(2, message.getReceiverUserId());
            }

            if (message.getGroupId() == null) {
                statement.setNull(3, Types.INTEGER);
            } else {
                statement.setInt(3, message.getGroupId());
            }

            statement.setString(4, message.getContent());
            statement.setString(5, message.getMessageType().name());

            String createdAt = message.getCreatedAt();

            if (createdAt == null || createdAt.isBlank()) {
                createdAt = LocalDateTime.now().toString();
                message.setCreatedAt(createdAt);
            }

            statement.setString(6, createdAt);

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int id = generatedKeys.getInt(1);
                    message.setId(id);
                    return id;
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao salvar mensagem.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return -1;
    }

    public Message findById(int messageId) {
        String sql = """
                SELECT
                    id,
                    sender_user_id,
                    receiver_user_id,
                    group_id,
                    content,
                    message_type,
                    created_at
                FROM messages
                WHERE id = ?;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, messageId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Integer receiverUserId = getNullableInt(resultSet, "receiver_user_id");
                    Integer groupId = getNullableInt(resultSet, "group_id");

                    return new Message(
                            resultSet.getInt("id"),
                            resultSet.getInt("sender_user_id"),
                            receiverUserId,
                            groupId,
                            resultSet.getString("content"),
                            MessageType.valueOf(resultSet.getString("message_type")),
                            resultSet.getString("created_at"));
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao buscar mensagem por id.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return null;
    }

    public int saveDelivery(int messageId, int receiverUserId, boolean delivered) {
        String sql = """
                INSERT INTO message_deliveries (
                    message_id,
                    receiver_user_id,
                    delivered,
                    delivered_at
                ) VALUES (?, ?, ?, ?);
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, messageId);
            statement.setInt(2, receiverUserId);
            statement.setInt(3, delivered ? 1 : 0);

            if (delivered) {
                statement.setString(4, LocalDateTime.now().toString());
            } else {
                statement.setNull(4, Types.VARCHAR);
            }

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao salvar entrega da mensagem.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return -1;
    }

    public List<MessageDelivery> listPendingDeliveriesForUser(int userId) {
        String sql = """
                SELECT
                    md.id AS delivery_id,
                    md.message_id,
                    md.receiver_user_id,
                    m.sender_user_id,
                    m.group_id,
                    m.content,
                    m.message_type,
                    m.created_at
                FROM message_deliveries md
                INNER JOIN messages m ON m.id = md.message_id
                WHERE md.receiver_user_id = ?
                AND md.delivered = 0
                ORDER BY m.created_at;
                """;

        List<MessageDelivery> deliveries = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Integer groupId = null;

                    int rawGroupId = resultSet.getInt("group_id");
                    if (!resultSet.wasNull()) {
                        groupId = rawGroupId;
                    }

                    MessageDelivery delivery = new MessageDelivery(
                            resultSet.getInt("delivery_id"),
                            resultSet.getInt("message_id"),
                            resultSet.getInt("sender_user_id"),
                            resultSet.getInt("receiver_user_id"),
                            groupId,
                            resultSet.getString("content"),
                            MessageType.valueOf(resultSet.getString("message_type")),
                            resultSet.getString("created_at"));

                    deliveries.add(delivery);
                }
            }

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao listar mensagens pendentes.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return deliveries;
    }

    private Integer getNullableInt(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    public boolean markDelivered(int deliveryId) {
        String sql = """
                UPDATE message_deliveries
                SET delivered = 1,
                    delivered_at = ?
                WHERE id = ?;
                """;

        try (Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, LocalDateTime.now().toString());
            statement.setInt(2, deliveryId);

            return statement.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao marcar mensagem como entregue.");
            System.out.println("Detalhes: " + e.getMessage());
        }

        return false;
    }
}