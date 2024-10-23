import message.Message;
import user.User;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    public static ArrayList<Message> messages = new ArrayList<Message>();
    public static ArrayList<User> users = new ArrayList<User>();
    public static void main(String[] args) throws IOException
    {
        ServerSocket serverSocket = new ServerSocket(8080);
        while(true)
        {
            Socket connection = serverSocket.accept();
            DataInputStream reader = new DataInputStream(connection.getInputStream());
            DataOutputStream writer = new DataOutputStream(connection.getOutputStream());
            String name = reader.readUTF();
            System.out.println(name+" connect");
            User searchedUser = null;
            for(User tmpUser : users)
            {
                if(tmpUser.getUserName().equals(name) && !tmpUser.isActive())
                    searchedUser = tmpUser;
            }
            if(searchedUser == null)
            {
                User newUser = new User(connection, name);
                users.add(newUser);
                writer.writeUTF("Connected!");
                newUser.start();
            }
            else
            {
                searchedUser.setActive(true);
                writer.writeUTF("Connected!");
                searchedUser.start();
            }
            connection.close();
        }
    }
}

