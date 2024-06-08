package User;

import lombok.Getter;
import lombok.Setter;
import Message.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.stream.Collectors;

import DataBase.ServerDB;

@Getter
@Setter
public class User extends Thread {
    private ServerDB database;
    private ArrayList<Message> messages;
    private ArrayList<User> users;
    private ArrayList<User> blockList;
    private String userName;
    private Socket connection;
    private DataInputStream reader;
    private DataOutputStream writer;
    private boolean isActive;
    private User inPvUser;

    public User(Socket connection, String name, boolean isActive) throws IOException {
        this.database = Server.database;
        this.inPvUser = null;
        this.userName = name;
        this.connection = connection;
        this.reader = new DataInputStream(connection.getInputStream());
        this.writer = new DataOutputStream(connection.getOutputStream());
        this.messages = Server.messages;
        this.users = Server.users;
        blockList = new ArrayList<>();
        for (Block tmpBlock : Server.blocks)
            if (tmpBlock.getBlocker().equals(this.getUserName())) {
                blockList.add(database.getUserByUsername(tmpBlock.getBlocked()));
            }
        this.isActive = isActive;
        showAllMessages();
    }

    @Override
    public void run() {
        try {
            String prompt = reader.readUTF();
            while (!prompt.equals("exit")) {
                System.out.println(prompt);
                String[] promptsArray = prompt.split(" -");
                switch (promptsArray[0]) {
                    case "newMessage":
                        addMessage(promptsArray[1]);
                        break;
                    case "newWhisper":
                        addWhisper(promptsArray[1], promptsArray[2]);
                        break;
                    case "ping":
                        writer.writeUTF("ping :");
                        break;
                    case "pv":
                        writer.writeUTF(showAllUsers());
                        User secUser = chooseUserForPv();
                        if (secUser != null) {
                            showAllPvMessages();
                            pvCmd();
                        }
                        break;
                    case "search":
                        String searchQuery = reader.readUTF();
                        searchMessages(searchQuery);
                        break;
                    default:
                        writer.writeUTF("Invalid format");
                        break;

                }
                prompt = reader.readUTF();
            }
            isActive = false;
            connection.close();
            interrupt();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void searchMessages(String searchQuery) throws IOException {
        String[] searchArray = searchQuery.split(" ");
        if (searchArray.length == 1) {
            writer.writeUTF(database.searchByName(searchArray[0]));
        } else {
            String[] startTimeArray = searchArray[0].split(":");
            String[] endTimeArray = searchArray[2].split(":");
            LocalDateTime startDateTime = LocalDateTime.of(LocalDate.now(),
                    LocalTime.of(Integer.parseInt(startTimeArray[0]), Integer.parseInt(startTimeArray[1]), Integer.parseInt(startTimeArray[2]), 0));

            LocalDateTime endDateTime = LocalDateTime.of(LocalDate.now(),
                    LocalTime.of(Integer.parseInt(endTimeArray[0]), Integer.parseInt(endTimeArray[1]), Integer.parseInt(endTimeArray[2]), 0));

            writer.writeUTF(database.searchByTime(startDateTime, endDateTime));
        }
    }

    private void showAllPvMessages() throws IOException {
        writer.writeUTF(database.showAllPvMessage(this.getUserName(), inPvUser.getUserName()));
    }

    private void pvCmd() throws IOException {
        String prompt = reader.readUTF();
        while (!prompt.equals("finish")) {
            System.out.println(prompt);
            String[] promptsArray = prompt.split(" -");
            switch (promptsArray[0]) {
                case "newMessage":
                    pvNewMessage(promptsArray[1]);
                    break;
                case "clearHistory":
                    clearHistory();
                    break;
                case "block":
                    blockUser();
                    break;
                default:
                    writer.writeUTF("Invalid format");
                    break;
            }
            prompt = reader.readUTF();
        }
        inPvUser.inPvUser = null;
        inPvUser = null;
        showAllMessages(); // showing all the chatroom messages after quiting the pv
    }


    private void blockUser() {
        synchronized (blockList) {
            blockList.add(inPvUser);
            database.addBlockToDB(new Block(this.userName, inPvUser.getUserName()));
        }
    }

    private void clearHistory() {
        database.clearPvHistory(this.userName, this.inPvUser.getUserName());
    }

    private void pvNewMessage(String content) throws IOException {
        if (inPvUser.blockList.contains(this)) {
            writer.writeUTF("You are blocked by this user!");
            return;
        }
        synchronized (messages) {
            messages.add(new Message(content, LocalDateTime.now(), this, inPvUser, 1));
            database.writeMessageInDB(messages.getLast());
            synchronized (inPvUser) {
                inPvUser.writer.writeUTF(content);
            }
        }
    }

    private User chooseUserForPv() throws IOException {
        String name = reader.readUTF();
        for (User user : users) {
            if (user.getUserName().equals(name)) {
                this.inPvUser = user;
                user.inPvUser = this;
                return user;
            }
        }
        return null;
    }

    private String showAllUsers() {
        StringBuilder res = new StringBuilder();
        for (User user : users) {
            res.append(user.getUserName()).append("\n");
        }
        return String.valueOf(res);
    }

    private void showAllMessages() throws IOException {
        writer.writeUTF(database.showAllChatRoomMessage());
    }

    private void addMessage(String content) throws IOException {
        synchronized (messages) {
            messages.add(new Message(content, LocalDateTime.now(), this, null, 0));
            database.writeMessageInDB(messages.getLast());
            for (User tmpUser : users)
                if (tmpUser.inPvUser == null) {
                    if (!tmpUser.equals(this)) {
                        synchronized (tmpUser) {
                            System.out.println(tmpUser.getUserName());
                            tmpUser.showNewMessage(messages.getLast());
                        }
                    }
                }
        }
    }

    private void addWhisper(String content, String receiverUsername) throws IOException {
        synchronized (messages) {
            User receiver = users.stream().filter(user -> user.getUserName().equals(receiverUsername))
                    .findFirst().orElse(null);
            if (receiver != null) {
                messages.add(new Message(content, LocalDateTime.now(), this, receiver, 0));
                database.writeMessageInDB(messages.getLast());
                for (User tmpUser : users)
                    if (tmpUser.inPvUser == null) {
                        synchronized (tmpUser) {
                            tmpUser.showNewMessage(messages.getLast());
                        }
                    }
            }
        }
    }

    public void showNewMessage(Message newMessage) throws IOException {
        if (newMessage.getReceiver() == null || newMessage.getReceiver().equals(this))
            writer.writeUTF(newMessage.toString());
        else
            writer.writeUTF("whisper for " + newMessage.getReceiver().getUserName());
    }

}
