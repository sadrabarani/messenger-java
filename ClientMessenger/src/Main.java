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
    }
}


class Client {
    private String userName;
    private Socket socket;
    private DataOutputStream out = null;
    private DataInputStream serverInput;
    private Thread senderThread;
    private Thread recieverThread;

    public Client(String userName) throws IOException {
        socket = new Socket("127.0.0.1", 1234);
        this.userName = userName;
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
            senderThread.interrupt();
            recieverThread.interrupt();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    public static long ping(DataOutputStream output, DataInputStream serverInput) {
        try {
            long startTime = System.currentTimeMillis();
            output.writeUTF("ping");
            String response = serverInput.readUTF();
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
            setOut(new DataOutputStream(getSocket().getOutputStream()));
            while (true) {
                Scanner scanner = new Scanner(System.in);
                String str = scanner.nextLine();
                getOut().writeUTF(str);
                if (str.equals("exit")) {
                    closeEveryThing();
                    break;
                }else if (str.equals("ping")) {
                    long roundTripTime = ping(getOut(), getServerInput());
                    System.out.println("ping: " + roundTripTime + " ms");
                }
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}


class ReceiveMessege extends Client implements Runnable {

    public ReceiveMessege(String userName) throws IOException {
        super(userName);
    }

    @Override
    public void run() {
        try {
            setServerInput(new DataInputStream(getSocket().getInputStream()));
            System.out.println(getServerInput().readUTF());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}