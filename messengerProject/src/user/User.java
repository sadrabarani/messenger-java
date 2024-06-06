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
    private ArrayList<User> blockList;
    private String userName;
    private Socket connection;
    private DataInputStream reader;
    private DataOutputStream writer;
    private boolean isActive;
    private User inPvUser;

    public User(Socket connection, String name) throws IOException {
        this.inPvUser = null;
        this.userName = name;
        this.connection = connection;
        this.reader = new DataInputStream(connection.getInputStream());
        this.writer = new DataOutputStream(connection.getOutputStream());
        this.messages = Server.messages;
        this.users = Server.users;
        this.blockList = new ArrayList<>(); // todo : get from database
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
                            showAllPvMessages();
                            pvCmd();
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


    private void showAllPvMessages() throws IOException {
        for(Message tmpMessage : messages)
            if(tmpMessage.getType() == 1 && tmpMessage.getReciever().equals(inPvUser))
            {
                showNewMessage(tmpMessage);
            }
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
        inPvUser = null;
        showAllMessages(); // showing all the chatroom messages after quiting the pv
    }


    private void blockUser()
    {
        synchronized (blockList) {
            blockList.add(inPvUser);
        }
    }
    private void clearHistory()
    {
        synchronized (messages)
        {
            for(Message tmpMessage : messages)
            {
                if(tmpMessage.getType() == 1 && tmpMessage.getReciever().equals(inPvUser))
                {
                    synchronized (tmpMessage) {
                        messages.remove(tmpMessage);
                    }
                }
            }
        }
    }

    private void pvNewMessage(String content) throws IOException {
        if(inPvUser.blockList.contains(this))
        {
            writer.writeUTF("You are blocked by this user!");
            return;
        }
        synchronized (messages) {
            messages.add(new Message(content, LocalDateTime.now(), this, inPvUser, 1));
            synchronized (inPvUser) {
                inPvUser.writer.writeUTF(messages.toString());
            }
        }
    }

    private User chooseUserForPv() throws IOException {
        String name = reader.readUTF();
        for (User user : users) {
            if (user.getUserName().equals(name)) {
                this.inPvUser = user;
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
        for (Message tmpMessage : messages) if(tmpMessage.getType() == 0) {
            showNewMessage(tmpMessage);
        }
    }

    private void addMessage(String content) throws IOException {
        synchronized (messages) {
            messages.add(new Message(content, LocalDateTime.now(), this, null, 0));
            for (User tmpUser : users) if(tmpUser.inPvUser != null) {
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
                messages.add(new Message(content, LocalDateTime.now(), this, reciever, 0));
                for (User tmpUser : users) if(tmpUser.inPvUser != null) {
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
