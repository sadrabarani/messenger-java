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
    private Thread recieverThread;

    public Client(String userName) {
        this.userName = userName;
    }

    public void connectClient() throws IOException {
        socket = new Socket("127.0.0.1", 8080);
        System.out.println("connect");
        setOut(new DataOutputStream(getSocket().getOutputStream()));
        setServerInput(new DataInputStream(getSocket().getInputStream()));
        out.writeUTF(userName);
        SendMessege send = new SendMessege(userName);
        senderThread = new Thread(send);
        senderThread.start();
        ReceiveMessege receive = new ReceiveMessege(userName);
        recieverThread = new Thread(receive);
        recieverThread.start();
    }

    public void closeEveryThing() {
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
}

class SendMessege extends Client implements Runnable {

    public SendMessege(String userName) throws IOException {
        super(userName);
    }

    @Override
    public void run() {
        try {
            while (true) {
                Scanner scanner = new Scanner(System.in);
                String str = scanner.nextLine();
                if (str.equals("exit")) {
                    getOut().writeUTF(str);
                    closeEveryThing();
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

    public ReceiveMessege(String userName) throws IOException {
        super(userName);
    }

    @Override
    public void run() {
        while (true) {
            try {
                System.out.println(getServerInput().readUTF());
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}