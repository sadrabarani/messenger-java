package User;

import DataBase.ServerDB;
import Message.Message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {


    public static ArrayList<User> users = new ArrayList<>();
    public static ArrayList<Message> messages = new ArrayList<>();
    public static ArrayList<Block> blocks = new ArrayList<>();
    public static ServerDB database = new ServerDB();
    public static void main(String[] args) throws IOException
    {
        ArrayList<String> usernames = database.getUsernames();
//        for(String username : usernames)
//        {
//            users.add(new User(null, username, false));
//        }
        messages = database.getMessages();
        blocks = database.getBlocks();
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
                User newUser = new User(connection, name, true);
                users.add(newUser);
                database.addUserToDB(newUser.getUserName());
                writer.writeUTF("Connected!");
                newUser.start();
            }
            else
            {
                searchedUser.setActive(true);
                searchedUser.setConnection(connection);
                writer.writeUTF("Connected!");
                searchedUser.start();
            }
            connection.close();
        }
    }
}

