import lombok.Getter;
import lombok.Setter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Getter
@Setter
public class User extends Thread {
    private static ArrayList<Message> messages;
    private static ArrayList<User> users;
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
        users.add(this);
        writer.writeUTF(showAllMessages());
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
                        addMessage(prompt);
                        break;
                    case "newWhisper":
                        addWhisper();
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
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private String showAllMessages() {
        return "All messages";
    }

    private void addMessage(String prompt) throws IOException {
        messages.add(new Message(prompt, LocalDateTime.now(),this,null));
        for (User user:users){
            if (!user.equals(this)){
                user.writer.writeUTF(prompt);
            }
        }
    }

    private void addWhisper() {

    }

    public void showNewMessage(Message newMessage) throws IOException {
        if (newMessage.getReciever() == null || newMessage.getReciever().equals(this))
            writer.writeUTF(newMessage.toString());
        else
            writer.writeUTF("whisper for " + newMessage.getReciever().getUserName());
    }

}
