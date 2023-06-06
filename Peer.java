import java.io.*;
import java.net.*;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class Peer {

    public static Peer[] peers; // In a real system, there must be a listing of peer ids to broadcast, here we keep all peers in this array
    public static Random rand;
    public Blockchain blockchain;  // This is this particular peer's copy of the blockchain
    private Wallet wallet;      // This is the wallet of this peer that holds the keys and UTXOs to be used later

    private BlockingQueue<String> queue;
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private int id;
    private static int countPeers = 0;

    private ArrayList<Transaction> mempool; //https://www.btcturk.com/bilgi-platformu/bitcoin-mempool-nedir/

    // Creates a peer, assigns the next available id, initializes random generator
    // Initializes transactions list and starts the server socket
    public Peer() {
        id = countPeers++;
        queue = new LinkedBlockingQueue<>();
        rand = new Random(System.currentTimeMillis());
        mempool = new ArrayList<>();
        blockchain = new Blockchain();
        wallet = new Wallet(id); // Normally a wallet should be anonymous, or may not be, but we add this for debugging
        try {
            serverSocket = new ServerSocket(6000 + id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to add a transaction to the mempool
    public void addToMempool(Transaction tx) {
        this.mempool.add(tx);
    }

    // Method to get the mempool
    public ArrayList<Transaction> getMempool() {
        return this.mempool;
    }

    //Starts the listening server
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

    public Blockchain getBlockchain() {
        return blockchain;
    }

    public PublicKey getPublicKey() {
        return wallet.publicKey; 
    }

    // While the message queue has new messages, reads them and processes them
    // Here, processing is putting it into the transaction list
    // Beware that this is not a trivial task, remember that the order of 
    // Transactions is very important. This may insert a transaction to a specific position in the list
    public void startConsumer() {
        new Thread(() -> {
            while (true) {
                try {
                    String message = queue.take(); // blocks if queue is empty
                    // Turn the string message back into a transaction



                    mempool.add(Transaction.fromString(message, this)); // For now, just add it to the end of transactions
                    // Process the message
                    System.out.println("Peer " + this.id + ": Transaction added -> " + (message.substring(0,4)+"...")+" ==> Number of txs: "+mempool.size());
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // This is a user simulation (it randomly initializes some transactions from time to time)
    public void startPeerTransactions() {
        new Thread(() -> {
            while (true) {
                int waitForNextTransactionTime=rand.nextInt(10000)+3000;
                try {
                    //Wait for some random duration
                    Thread.sleep(waitForNextTransactionTime);
                    // Make a random transaction
                    int receiver = rand.nextInt(peers.length);
                    while(receiver==this.id) receiver = rand.nextInt(peers.length);
                    int amount = rand.nextInt(1000000); // A better probability distribution makes more sense. Wallet check?
                    //this.broadcastToAllPeers("PAY "+amount+" TO "+receiver);
                    Transaction newTransaction = new Transaction(this.wallet.publicKey, Peer.peers[receiver].wallet.publicKey, amount, new ArrayList<TransactionInput>(), this);
                    System.out.println("Peer " + this.id + ": Transaction broadcasted -> ID:"+newTransaction.transactionId+" val:"+newTransaction.value);
                    
                    this.broadcastToAllPeers(newTransaction.toString());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }                
            }
        }).start();
    }

    // Connects to another peer.
    public void connectToPeer(Peer peer) {
        try {
            this.clientSocket = new Socket("localhost", 6000 + peer.id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // When a message arrives, reads it from the input stream reader and puts it into the queue
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

    // Sends a message to a specific peer
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

    // Broadcasts a message to all peers
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





    // Method to mine a block
    /*public void mineBlock() {
        // As an example, we'll just take all transactions from the mempool.
        // In reality, you'd want to choose transactions based on some criteria,
        // such as the transaction fee.

        // Also, this does not handle the case where a block's size is limited,
        // and the total size of all transactions in the mempool exceeds this limit.

        //We will implement Merkle tree later

        List<Transaction> transactionsToMine = new ArrayList<>(this.mempool);
        this.mempool.clear();

        // Add the transactions to a new block and add it to the blockchain
        Block newBlock = new Block(blockchain.getLastBlock().hash, transactionsToMine, this);
        this.blockchain.addBlock(newBlock);
    }*/

}
