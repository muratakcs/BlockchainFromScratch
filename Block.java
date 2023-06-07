import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.*;

public class Block {

    public int index; // index of this block within the blockchain
    public Blockchain blockchain; // reference to the containing blockchain
    public String hash;
    public String previousHash;
    public String merkleRoot;
    public List<Transaction> transactions = new ArrayList<>();
    public long timeStamp; 
    public int nonce;
    
    // Constructor for genesis block
    public Block(PublicKey recipient, Blockchain bc) throws NoSuchAlgorithmException {
        this.blockchain = bc;
        this.previousHash = "0";
        Transaction coinbase = new Transaction(recipient, blockchain.MININGREWARD, blockchain.peer);
        coinbase.transactionId = "coinbase";
        this.transactions.add(coinbase);
        this.timeStamp = System.currentTimeMillis();
        this.hash = calculateHash(); // Calculate the hash of the genesis block.
    }

 
    // Block Constructor.
    public Block(String previousHash, List<Transaction> transactions, int index, Blockchain blockchain) throws NoSuchAlgorithmException {
        this.previousHash = previousHash;
        this.index = index;
        this.transactions = transactions;
        this.blockchain = blockchain;

        // Create mining reward transaction
        Transaction miningReward = new Transaction( null,
                                                    this.blockchain.peer.getPublicKey(),
                                                    this.blockchain.MININGREWARD/(2^(index/this.blockchain.HALVINGPERIOD)),
                                                    this.blockchain.peer);

        // Add mining reward as the first transaction
        this.transactions.add(0, miningReward);

        // compute merkle root
        this.merkleRoot = StringUtil.getMerkleRoot(transactions);

        // Take the timestamp just before taking the hash
        this.timeStamp = new Date().getTime();
    }
    
    public String calculateHash() {
        String calculatedhash = StringUtil.applySha256(
                previousHash +
                Long.toString(timeStamp) +
                Integer.toString(nonce) +
                merkleRoot
        );
        return calculatedhash;
    }

    //Increases nonce value until hash target is reached.
    //Hash is generated here, calculating hash in the constructor doesn't make sense
    //because we need to search for a nonce that gives leading zeros
    public void mineBlock() {
        //merkleRoot = StringUtil.getMerkleRoot(transactions);
        String target = StringUtil.getDificultyString(this.blockchain.difficulty);

        // The loop for mining
        while(!this.hash.substring( 0, this.blockchain.difficulty).equals(target)) {
            this.nonce ++;
            this.hash = calculateHash();
        }
        System.out.println("Block Mined!!! : " + this.hash);
    }

    //Add transactions to this block
    /*public boolean addTransaction(Transaction transaction) {
        if(transaction == null) return false;    
        if((previousHash != "0")) {
            if((transaction.processTransaction() != true)) {
                System.out.println("Transaction failed to process. Discarded.");
                return false;
            }
        }
        transactions.add(transaction);
        System.out.println("Transaction Successfully added to Block");
        return true;
    }*/
}
