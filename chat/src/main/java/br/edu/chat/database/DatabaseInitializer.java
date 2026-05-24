package br.edu.chat.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    public static void initialize() {
        try (Connection connection = DatabaseConnection.getConnection();
                Statement statement = connection.createStatement()) {

            enableForeignKeys(statement);

            createUsersTable(statement);
            createBlocksTable(statement);
            createDirectPermissionsTable(statement);
            createGroupsTable(statement);
            createGroupMembersTable(statement);
            createMessagesTable(statement);
            createMessageDeliveriesTable(statement);
            createPendingRequestsTable(statement);
            ensurePendingRequestsPendingMessageIdColumn(connection, statement);

            System.out.println("[OK] Banco de dados inicializado com sucesso.");

        } catch (SQLException e) {
            System.out.println("[ERRO] Falha ao inicializar o banco de dados.");
            e.printStackTrace();
        }
    }

    private static void enableForeignKeys(Statement statement) throws SQLException {
        statement.execute("PRAGMA foreign_keys = ON;");
    }

    private static void createUsersTable(Statement statement) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    full_name TEXT NOT NULL UNIQUE,
                    login TEXT NOT NULL UNIQUE,
                    email TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'OFFLINE',
                    created_at TEXT NOT NULL
                );
                """;

        statement.execute(sql);
    }

    private static void createBlocksTable(Statement statement) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS blocks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    blocker_user_id INTEGER NOT NULL,
                    blocked_user_id INTEGER NOT NULL,
                    created_at TEXT NOT NULL,

                    UNIQUE(blocker_user_id, blocked_user_id),

                    FOREIGN KEY(blocker_user_id) REFERENCES users(id),
                    FOREIGN KEY(blocked_user_id) REFERENCES users(id)
                );
                """;

        statement.execute(sql);
    }

    private static void createDirectPermissionsTable(Statement statement) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS direct_permissions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    requester_user_id INTEGER NOT NULL,
                    target_user_id INTEGER NOT NULL,
                    accepted INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT NOT NULL,

                    UNIQUE(requester_user_id, target_user_id),

                    FOREIGN KEY(requester_user_id) REFERENCES users(id),
                    FOREIGN KEY(target_user_id) REFERENCES users(id)
                );
                """;

        statement.execute(sql);
    }

    private static void createGroupsTable(Statement statement) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS groups_chat (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    created_by_user_id INTEGER NOT NULL,
                    created_at TEXT NOT NULL,

                    FOREIGN KEY(created_by_user_id) REFERENCES users(id)
                );
                """;

        statement.execute(sql);
    }

    private static void createGroupMembersTable(Statement statement) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS group_members (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    is_admin INTEGER NOT NULL DEFAULT 0,
                    joined_at TEXT NOT NULL,

                    UNIQUE(group_id, user_id),

                    FOREIGN KEY(group_id) REFERENCES groups_chat(id),
                    FOREIGN KEY(user_id) REFERENCES users(id)
                );
                """;

        statement.execute(sql);
    }

    private static void createMessagesTable(Statement statement) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sender_user_id INTEGER NOT NULL,
                    receiver_user_id INTEGER,
                    group_id INTEGER,
                    content TEXT NOT NULL,
                    message_type TEXT NOT NULL,
                    created_at TEXT NOT NULL,

                    FOREIGN KEY(sender_user_id) REFERENCES users(id),
                    FOREIGN KEY(receiver_user_id) REFERENCES users(id),
                    FOREIGN KEY(group_id) REFERENCES groups_chat(id)
                );
                """;

        statement.execute(sql);
    }

    private static void createMessageDeliveriesTable(Statement statement) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS message_deliveries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    message_id INTEGER NOT NULL,
                    receiver_user_id INTEGER NOT NULL,
                    delivered INTEGER NOT NULL DEFAULT 0,
                    delivered_at TEXT,

                    FOREIGN KEY(message_id) REFERENCES messages(id),
                    FOREIGN KEY(receiver_user_id) REFERENCES users(id)
                );
                """;

        statement.execute(sql);
    }

    private static void createPendingRequestsTable(Statement statement) throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS pending_requests (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    request_type TEXT NOT NULL,
                    requester_user_id INTEGER NOT NULL,
                    target_user_id INTEGER,
                    group_id INTEGER,
                    pending_message_id INTEGER,
                    status TEXT NOT NULL DEFAULT 'PENDING',
                    created_at TEXT NOT NULL,

                    FOREIGN KEY(requester_user_id) REFERENCES users(id),
                    FOREIGN KEY(target_user_id) REFERENCES users(id),
                    FOREIGN KEY(group_id) REFERENCES groups_chat(id),
                    FOREIGN KEY(pending_message_id) REFERENCES messages(id)
                );
                """;

        statement.execute(sql);
    }

    private static void ensurePendingRequestsPendingMessageIdColumn(Connection connection, Statement statement)
            throws SQLException {
        if (!columnExists(connection, "pending_requests", "pending_message_id")) {
            statement.execute("ALTER TABLE pending_requests ADD COLUMN pending_message_id INTEGER;");
        }
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName)
            throws SQLException {
        String sql = "PRAGMA table_info(" + tableName + ");";

        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                String currentColumn = resultSet.getString("name");

                if (columnName.equalsIgnoreCase(currentColumn)) {
                    return true;
                }
            }
        }

        return false;
    }

}
