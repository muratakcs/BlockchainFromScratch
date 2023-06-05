import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Peer {

    public static Peer[] peers;

    private BlockingQueue<String> queue;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private int id;
    private static int countPeers = 0;

    public Peer() {
        id = countPeers++;
        queue = new LinkedBlockingQueue<>();
        try {
            serverSocket = new ServerSocket(6000 + id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startServer() {
        new Thread(() -> {
            while (true) {
                try {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> receiveMessage(socket)).start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void startConsumer() {
        new Thread(() -> {
            while (true) {
                try {
                    String message = queue.take(); // blocks if queue is empty
                    // Process the message
                    System.out.println("Peer " + this.id + ": Processed message: " + message);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void connectToPeer(Peer peer) {
        try {
            this.clientSocket = new Socket("localhost", 6000 + peer.id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveMessage(Socket socket) {
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String message;
            while ((message = input.readLine()) != null) {
                queue.put(message);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(int peerid, String message) {
        this.connectToPeer(Peer.peers[peerid]);
        new Thread(() -> {
            try {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                out.println(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }


    public void broadcastToAllPeers(String message) {
        for(int peerid=0; peerid<peers.length; peerid++){
            this.connectToPeer(Peer.peers[peerid]);
            new Thread(() -> {
                try {
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    out.println(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
