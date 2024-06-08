import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your user name:");
        String userName = scanner.nextLine();
        Client client = new Client(userName);
        client.connectClient();
    }
}

class Client {
    private String userName;
    private static Socket socket;
    private static DataOutputStream out = null;
    private static DataInputStream serverInput;
    private Thread senderThread;
    private Thread receiverThread;
    private volatile boolean running = true;

    public Client(String userName) {
        this.userName = userName;
    }

    public void connectClient() throws IOException {
        socket = new Socket("127.0.0.1", 8080);
        System.out.println("connect");
        setOut(new DataOutputStream(getSocket().getOutputStream()));
        setServerInput(new DataInputStream(getSocket().getInputStream()));
        out.writeUTF(userName);
        SendMessege send = new SendMessege(userName, this);
        senderThread = new Thread(send);
        senderThread.start();
        ReceiveMessege receive = new ReceiveMessege(userName, this);
        receiverThread = new Thread(receive);
        receiverThread.start();
    }

    public void closeEverything() {
        running = false;
        try {
            socket.close();
            out.close();
            serverInput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static long ping(DataOutputStream output, DataInputStream serverInput) {
        try {
            long startTime = System.currentTimeMillis();
            output.writeUTF("ping");
            long endTime = System.currentTimeMillis();
            return endTime - startTime;
        } catch (IOException e) {
            System.out.println("Ping failed: " + e.getMessage());
            return -1;
        }
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public void setOut(DataOutputStream out) {
        this.out = out;
    }

    public void setServerInput(DataInputStream serverInput) {
        this.serverInput = serverInput;
    }

    public Socket getSocket() {
        return socket;
    }

    public DataOutputStream getOut() {
        return out;
    }

    public DataInputStream getServerInput() {
        return serverInput;
    }

    public boolean isRunning() {
        return running;
    }
}

class SendMessege extends Client implements Runnable {
    private Client client;

    public SendMessege(String userName, Client client) throws IOException {
        super(userName);
        this.client = client;
    }

    @Override
    public void run() {
        try {
            while (client.isRunning()) {
                Scanner scanner = new Scanner(System.in);
                String str = scanner.nextLine();
                if (str.equals("exit")) {
                    getOut().writeUTF(str);
                    client.closeEverything();
                    break;
                } else if (str.equals("ping")) {
                    long roundTripTime = ping(getOut(), getServerInput());
                    System.out.println(roundTripTime + " ms");
                } else {
                    getOut().writeUTF(str);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


class ReceiveMessege extends Client implements Runnable {
    private Client client;

    public ReceiveMessege(String userName, Client client) throws IOException {
        super(userName);
        this.client = client;
    }

    @Override
    public void run() {
        while (client.isRunning()) {
            try {
                System.out.println(getServerInput().readUTF());
            } catch (IOException e) {
                if (!client.isRunning()) {
                    System.out.println("Connection closed.");
                } else {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
}