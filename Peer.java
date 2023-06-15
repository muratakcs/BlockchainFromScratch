import java.io.*;
import java.net.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class Peer {

    public static Peer[] peers; // In a real system, there must be a listing of peer ids to broadcast, here we keep all peers in this array
    public static Random rand;
    
    public int id;
    private static int countPeers = 0; //to assign an automatic id

    //Every peer has its own copy of the blockchain
    //Possibly can occasionally become a little different from others
    //but must eventually become the same as all others
    //which is the whole idea of the blockchain
    public Blockchain blockchain;

    //Every peer has a wallet.
    //In fact, a peer can have multiple wallets
    //But for this application, to keep it simple, we implement only one
    protected Wallet wallet;      // This is the wallet of this peer that holds the keys and UTXOs to be used later
    

    private BlockingQueue<String> queue;    //To keep incoming messages
    
    private ServerSocket serverSocket;
    private Socket clientSocket;
    
    
    
    // Creates a peer, assigns the next available id, initializes random generator
    // Initializes transactions list and starts the server socket
    public Peer() throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
        id = countPeers++;
        queue = new LinkedBlockingQueue<>();
        rand = new Random(System.currentTimeMillis());
        wallet = new Wallet(this); // Normally a wallet should be anonymous, or may not be, but we add this for debugging
        blockchain = new Blockchain(this);
        try {
            serverSocket = new ServerSocket(6000 + id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(String s) {
        for(int i=0; i<id; i++) {
            System.out.print(" ");
        }
        System.out.println("PEER "+id+": ");
        System.out.println(s);
    }

    



/*  // Method to add a transaction to the mempool
    public void addToMempool(Transaction tx) {
        this.blockchain.mempool.add(tx);
    }

    // Method to get the mempool
    public ArrayList<Transaction> getMempool() {
        return this.blockchain.mempool;
    }
*/


    /////////////////////////////////////////////////////////////////////////////////
    ///////////////////////// THE LIVING SIMULATION OF PEER /////////////////////////
    /////////////////////////////////////////////////////////////////////////////////

    // This is a user simulation (it randomly initializes some transactions from time to time)
    public void startPeerTransactions() {
        new Thread(() -> {
            while (true) {
                int waitForNextTransactionTime = rand.nextInt(10000)+3000;
                try {
                    //Wait for some random duration
                    Thread.sleep(waitForNextTransactionTime);
                    // Make a random transaction
                    
                    // Randomly choose "someone else" to send some money.
                    int receiver = rand.nextInt(peers.length);
                    while(receiver==this.id) receiver = rand.nextInt(peers.length); // I said "someone else", not myself

                    // Randomly choose an amount. Slightly more than balance is possible
                    // so that we can check whether frauds are caught or not.
                    int bal = this.wallet.getBalance();
                    if(bal>0) {
                        int amount = rand.nextInt((int)(bal*1.2));

                        Transaction newTx = new Transaction(this.wallet.publicKey, 
                                                            Peer.peers[receiver].wallet.publicKey,
                                                            amount,
                                                            this); // If we send this, why also send pubkey?
                        

                        System.out.println("Peer " + this.id +" t:"+ newTx.timeStamp+ ": Transaction broadcasted -> ID:"+newTx.transactionId+" val:"+newTx.value);
                        
                        this.broadcastToAllPeers(newTx.toString());
                    }
                    
                } catch (InterruptedException | NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }                
            }
        }).start();
    }


    

    /////////////////////////////////////////////////////////////////////////////////
    ///////////////////////// BLOCKCHAIN INTERACTION METHODS ////////////////////////
    /////////////////////////////////////////////////////////////////////////////////

    public Blockchain getBlockchain() { return blockchain; }

    public PublicKey getPublicKey() { return wallet.publicKey; }

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


                    Transaction receivedTx = Transaction.fromString(message, this);

                    // Process the message
                    System.out.println("Peer " + this.id + ": Transaction received -> " + Wallet.getStringFromPublicKey(receivedTx.sender).substring(0,5)+" ==> Number of txs in my mempool: "+blockchain.mempool.size());
                    if(receivedTx.processTransaction())
                        blockchain.mempool.add(receivedTx); // For now, just add it to the end of transactions
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////// COMMUNICATION METHODS /////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////

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
            //if(peerid!=this.id) { // LET IT ALSO SEND TO ITSELF
                this.connectToPeer(Peer.peers[peerid]);
                new Thread(() -> {
                    try {
                        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                        out.println(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            //}
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
