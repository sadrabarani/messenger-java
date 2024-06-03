import lombok.Getter;
import lombok.Setter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

@Getter
@Setter
public class User extends Thread
{
    private ArrayList<Message> messages;
    private ArrayList<User> users;
    private String userName;
    private Socket connection;
    private DataInputStream reader;
    private DataOutputStream writer;
    private boolean isActive;

    public User(Socket connection, String name) throws IOException
    {
        this.userName = name;
        this.connection = connection;
        this.reader = new DataInputStream(connection.getInputStream());
        this.writer = new DataOutputStream(connection.getOutputStream());
        this.messages = Server.messages;
        this.users = Server.users;
        this.isActive = true;
        writer.writeUTF(showAllMessages());
    }

    @Override
    public void run()
    {
        String prompt = reader.readUTF();
        while (!prompt.equals("exit"))
        {
            System.out.println(prompt);
            String[] promptsArray = prompt.split(" -");
            switch (promptsArray[0])
            {
                case "newMessage":
                    addMessage();
                    break;
                case "newWhisper":
                    addWhisper();
                    break;
                case "exit":
                    isActive = false;
                    interrupt();
                    break;
                default:
                    writer.writeUTF("Invalid format");
                    break;

            }
            prompt = reader.readUTF();
        }
        connection.close();
    }

    private String showAllMessages()
    {
        return "All messages";
    }
    private void addMessage()
    {

    }
    private void addWhisper()
    {

    }
    public void showNewMessage(Message newMessage) throws IOException
    {
        if(newMessage.getReciever() == null || newMessage.getReciever().equals(this))
            writer.writeUTF(newMessage.toString());
        else
            writer.writeUTF("whisper for " + newMessage.getReciever().getUserName());
    }

}
