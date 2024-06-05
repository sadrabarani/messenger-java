package DataBase;

import user.*;
import message.*;

import java.sql.*;
import java.time.LocalDateTime;

public class serverDB {
    private DatabaseManager dbManager;

    public serverDB() {
        dbManager = new DatabaseManager();
    }

    public String writeMessageInDB(Message message) {
        String sqlCom = String.format("INSERT INTO `messages` (`sender`, `receiver`, `content`, `dateTime`) VALUES ('%s','%s', '%s', " + message.getDateTime() + ")", message.getSender().getUserName(), message.getReciever().getUserName(), message.getContent());
        try {
            exeDB(sqlCom);
            return "add succesful";
        } catch (SQLException e) {
            e.printStackTrace();
            return "error";
        }
    }

    public String searchByTime(LocalDateTime startTime, LocalDateTime endTime) {
        String sqlCmd = String.format("SELECT sender, receiver, content, dataTime, contentType FROM messages WHERE dateTime BETWEEN '" + startTime + "' AND '" + startTime + "'");
        return exeQuery(sqlCmd);
    }

    public String searchByName(String userName) {
        String sqlCmd = String.format("SELECT sender, receiver, content, dataTime, contentType FROM messages WHERE sender = '%s'", userName);
        return exeQuery(sqlCmd);
    }

    public String showAllChatRoomMessage() {
        String sqlCmd = String.format("SELECT sender, receiver, content, dataTime, contentType FROM messages WHERE contentType = '%s'", "chatRoom");
        return exeQuery(sqlCmd);
    }

    public String showAllPvMessage(String userName) {
        String sqlCmd = String.format("SELECT sender, receiver, content, dataTime, contentType FROM messages WHERE sender = '%s' AND contentType = '%s'", userName, "pv");
        return exeQuery(sqlCmd);
    }

    public void clearPvHistory(String userName) {
        String sqlCmd = String.format("DELETE FROM messages WHERE (sender = '%s' OR receiver = '%s') AND contentType = '%s'", userName, userName, "pv");
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
                    res.append("Sender: " + rs.getString("sender") + ", reciever: " + rs.getString("receiver")
                            + ", content: " + rs.getString("content") + ", date Time: " + rs.getDate("dateTime")
                            + rs.getString("contentType") + "\n");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return String.valueOf(res);
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
