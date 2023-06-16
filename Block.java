import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.*;

public class Block {
    public Blockchain blockchain; // reference to the containing blockchain
    public int index; // index of this block within the blockchain
    public String previousHash;
    public List<Transaction> transactions = new ArrayList<>();
    public String merkleRoot;
    public int nonce;
    public long timeStamp; 
    public String hash;
    public List<Block> next;//The list of next blocks. For iterating. Not used yet.
    
    // Constructor for genesis block
    public Block(PublicKey recipient, Blockchain bc) throws NoSuchAlgorithmException {
        this.blockchain = bc;
        this.previousHash = "0";
        Transaction coinbase = new Transaction(recipient, blockchain.MININGREWARD, blockchain.peer);
        coinbase.transactionId = "coinbase";
        this.transactions.add(coinbase);
        this.merkleRoot="0";
        this.timeStamp = 0;
        this.index=0;
        this.nonce=0;
        this.hash = calculateHash(); // Calculate the hash of the genesis block.
    }

 
    // Block Constructor.
    public Block(String previousHash, List<Transaction> transactions, int index, Blockchain blockchain) throws NoSuchAlgorithmException {
        this.previousHash = previousHash;
        this.index = index;
        this.transactions = transactions;
        this.blockchain = blockchain;

        // Create mining reward transaction
        Transaction miningReward = new Transaction( this.blockchain.peer.getPublicKey(),
                                                    this.blockchain.MININGREWARD/(2^(index/this.blockchain.HALVINGPERIOD)),
                                                    this.blockchain.peer);

        // Add mining reward as the first transaction
        this.transactions.add(0, miningReward);

        // compute merkle root
        this.merkleRoot = getMerkleRoot();

        // Take the timestamp just before taking the hash
        this.timeStamp = new Date().getTime();
    }
    
    public String calculateHash() {
        String dataToHash = previousHash +
                            Long.toString(timeStamp) +
                            Integer.toString(nonce) +
                            merkleRoot;
        return StringUtil.hash(dataToHash);
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
        this.blockchain.peer.write(" => Block Mined!!! : " + this.hash);
    }


    // Calculates the Merkle root of all given transactions
    // Normally, the length of transactions must be a power of 2 for this to work properly
    // Otherwise the last element and possibly some other elements may not affect the root
    public String getMerkleRoot() {
        int count = transactions.size();
        ArrayList<String> previousTreeLayer = new ArrayList<String>();
        for(Transaction transaction : transactions) {
            previousTreeLayer.add(transaction.transactionId);
        }
        ArrayList<String> treeLayer = previousTreeLayer;
        while(count > 1) {
            treeLayer = new ArrayList<String>();
            for(int i=1; i < previousTreeLayer.size(); i+=2) {
                treeLayer.add(StringUtil.applySha256(previousTreeLayer.get(i-1) + previousTreeLayer.get(i)));
            }
            count = treeLayer.size();
            previousTreeLayer = treeLayer;
        }
        String merkleRoot = (treeLayer.size() == 1) ? treeLayer.get(0) : "";
        return merkleRoot;
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
