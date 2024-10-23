package user;

import lombok.Getter;
import lombok.Setter;
import message.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Getter
@Setter
public class User extends Thread {
    private ArrayList<Message> messages;
    private ArrayList<User> users;
    private String userName;
    private Socket connection;
    private DataInputStream reader;
    private DataOutputStream writer;
    private boolean isActive;

    public User(Socket connection, String name) throws IOException {
        this.userName = name;
        this.connection = connection;
        this.reader = new DataInputStream(connection.getInputStream());
        this.writer = new DataOutputStream(connection.getOutputStream());
        this.messages = Server.messages;
        this.users = Server.users;
        this.isActive = true;
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
                            pvCmd(secUser);
                        }
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

    private void pvCmd(User secUser) throws IOException {
        String prompt = reader.readUTF();
        while (!prompt.equals("finish")) {
            System.out.println(prompt);
            String[] promptsArray = prompt.split(" -");
            switch (promptsArray[0]) {
                case "newMessage":
                    pvnewMessage(promptsArray[1], secUser);
                    break;
                case "clear history":
                    //todo
                    break;
            }
            prompt = reader.readUTF();
        }
    }

    private void pvnewMessage(String content, User secUser) throws IOException {
        synchronized (messages) {
            messages.add(new Message(content, LocalDateTime.now(), this, secUser));
            synchronized (secUser) {
                secUser.writer.writeUTF(messages.getLast().toString());
            }
        }
    }

    private User chooseUserForPv() throws IOException {
        String name = reader.readUTF();
        for (User user : users) {
            if (user.getUserName().equals(name)) {
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
        for (Message tmpMessage : messages) {
            showNewMessage(tmpMessage);
        }
    }

    private void addMessage(String content) throws IOException {
        synchronized (messages) {
            messages.add(new Message(content, LocalDateTime.now(), this, null));
            for (User tmpUser : users) {
                synchronized (tmpUser) {
                    tmpUser.showNewMessage(messages.getLast());
                }
            }
        }
    }

    private void addWhisper(String content, String recieverUsername) throws IOException {
        synchronized (messages) {
            User reciever = users.stream().filter(user -> user.getUserName().equals(recieverUsername))
                    .findFirst().orElse(null);
            if (reciever != null) {
                messages.add(new Message(content, LocalDateTime.now(), this, reciever));
                for (User tmpUser : users) {
                    synchronized (tmpUser) {
                        tmpUser.showNewMessage(messages.getLast());
                    }
                }
            }
        }
    }

    public void showNewMessage(Message newMessage) throws IOException {
        if (newMessage.getReciever() == null || newMessage.getReciever().equals(this))
            writer.writeUTF(newMessage.toString());
        else
            writer.writeUTF("whisper for " + newMessage.getReciever().getUserName());
    }

}
