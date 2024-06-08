package DataBase;

import Message.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import User.*;

public class ServerDB {
    private DatabaseManager dbManager;

    public ServerDB() {
        dbManager = new DatabaseManager();
    }

    public String writeMessageInDB(Message message) {
        String sqlCom;
        if (message.getReceiver() == null)
            sqlCom = String.format("INSERT INTO messages (sender, receiver, content, dateTime, contentType) VALUES ('%s', '%s', '%s', '%s', %d)",
                    message.getSender().getUserName(), null, message.getContent(), message.getDateTime().toString(), 0);

        else
            sqlCom = String.format("INSERT INTO messages (sender, receiver, content, dateTime, contentType) VALUES ('%s', '%s', '%s', '%s', %d)",
                    message.getSender().getUserName(), message.getReceiver().getUserName(), message.getContent(), message.getDateTime().toString(), 1);

        try {
            exeDB(sqlCom);
            return "add succesful";
        } catch (SQLException e) {
            e.printStackTrace();
            return "error";
        }
    }

    public String addBlockToDB(Block block) {
        String sqlCom = String.format("INSERT INTO `blocks` (`blocker`, `blocked`) VALUES ('%s','%s')", block.getBlocker(), block.getBlocked());
        try {
            exeDB(sqlCom);
            return "add succesful";
        } catch (SQLException e) {
            e.printStackTrace();
            return "error";
        }
    }

    public String addUserToDB(String username) {
        String sqlCom = String.format("INSERT INTO `users` (`name`) VALUES ('%s')", username);
        try {
            exeDB(sqlCom);
            return "add succesful";
        } catch (SQLException e) {
            e.printStackTrace();
            return "error";
        }
    }

    public String searchByTime(LocalDateTime startTime, LocalDateTime endTime) {
        String sqlCmd = "SELECT sender, receiver, content, dateTime, contentType FROM messages WHERE dateTime BETWEEN ? AND ?";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder res = new StringBuilder();

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlCmd)) {

            pstmt.setString(1, startTime.format(formatter));
            pstmt.setString(2, endTime.format(formatter));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    res.append("Sender: ").append(rs.getString("sender")).append(", receiver: ")
                            .append(rs.getString("receiver")).append(", content: ")
                            .append(rs.getString("content")).append(", date Time: ")
                            .append(rs.getString("dateTime")).append(", contentType: ")
                            .append(rs.getString("contentType")).append("\n");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return res.toString();
    }


    public String searchByName(String userName) {
        String sqlCmd = String.format("SELECT sender, receiver, content, dateTime, contentType FROM messages WHERE sender = '%s' AND contentType = %d AND receiver = '%s'", userName, 0, null);
        return exeQuery(sqlCmd);
    }

    public String showAllChatRoomMessage() {
        String sqlCmd = String.format("SELECT sender, receiver, content, dateTime, contentType FROM messages WHERE contentType = '%s'", 0);
        return exeQuery(sqlCmd);
    }

    public String showAllPvMessage(String sender, String receiver) {
        String sqlCmd = String.format("SELECT sender, receiver, content, dateTime, contentType FROM messages WHERE sender = '%s' AND receiver = '%s' AND contentType = %d", sender, receiver, 1);
        return exeQuery(sqlCmd);
    }

    public void clearPvHistory(String sender, String receiver) {
        String sqlCmd = String.format("DELETE FROM messages WHERE (sender = '%s' OR receiver = '%s') AND contentType = '%s'", sender, receiver, 1);
        try {
            Statement statement = dbManager.getConnection().createStatement();
            int rowsAffected = statement.executeUpdate(sqlCmd);
            System.out.println("Rows affected: " + rowsAffected);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String exeQuery(String sqlCmd) {
        StringBuilder res = new StringBuilder();
        try {
            Connection conn = dbManager.getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(sqlCmd);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    res.append("Sender: " + rs.getString("sender") + ", receiver: " + rs.getString("receiver")
                            + ", content: " + rs.getString("content") + ", date Time: " + rs.getString("dateTime")
                            + rs.getString("contentType") + "\n");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return String.valueOf(res);
    }


    public ArrayList<String> getUsernames() {
        ArrayList<String> usernames = new ArrayList<>();
        String sqlCmd = "SELECT name FROM users";

        try {
            Connection conne = dbManager.getConnection();
            PreparedStatement preparedStatement = conne.prepareStatement(sqlCmd);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    usernames.add(rs.getString("name"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return usernames;
    }

    public User getUserByUsername(String username) {
        User result = Server.users.stream().filter(u -> u.getUserName().equals(username)).findFirst().orElse(null);
        return result;
    }

    public ArrayList<Message> getMessages() {
        ArrayList<Message> allMessages = new ArrayList<>();
        String sqlCmd = "SELECT * FROM messages";

        try {
            Connection conn = dbManager.getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(sqlCmd);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {

                    String sender = rs.getString("sender");
                    String receiver = rs.getString("receiver");
                    String content = rs.getString("content");
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime dateTime = LocalDateTime.parse(rs.getString("dateTime"), formatter);
                    int type = Integer.parseInt(rs.getString("contentType"));
                    allMessages.add(new Message(content, dateTime, getUserByUsername(sender), getUserByUsername(receiver), type));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return allMessages;
    }

    public ArrayList<Block> getBlocks() {
        ArrayList<Block> allBlocks = new ArrayList<>();
        String sqlCmd = "SELECT * FROM blocks";

        try {
            Connection conn = dbManager.getConnection();
            PreparedStatement preparedStatement = conn.prepareStatement(sqlCmd);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {

                    String blocker = rs.getString("blocker");
                    String blocked = rs.getString("blocked");
                    allBlocks.add(new Block(blocker, blocked));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return allBlocks;
    }

    public void exeDB(String sqlCmd) throws SQLException {
        Connection conn = dbManager.getConnection();
        Statement statement = conn.prepareStatement(sqlCmd);
        statement.execute(sqlCmd);
    }
}

class DatabaseManager {
    private String URL = "jdbc:mysql://localhost/servermessengerdb";
    private String UserName = "root";
    private String Password = "";
    private Connection connection;

    public DatabaseManager() {
        try {
            connection = DriverManager.getConnection(URL, UserName, Password);
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("connected");
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("not connected !!");
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void closeConnection() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        System.out.println("finished");
    }
}
