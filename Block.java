import java.util.*;

public class Block {

    public String hash;
    public String previousHash;
    public String merkleRoot;
    public List<Transaction> transactions = new ArrayList<>();
    public long timeStamp; 
    public int nonce;
 
    // Block Constructor.
    public Block(String previousHash, List<Transaction> transactions) {
        this.previousHash = previousHash;
        this.transactions = transactions;
        this.timeStamp = new Date().getTime();

        // compute merkle root
        this.merkleRoot = StringUtil.getMerkleRoot(transactions);

        // compute the block hash
        this.hash = calculateHash();
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
    public void mineBlock(int difficulty) {
        merkleRoot = StringUtil.getMerkleRoot(transactions);
        String target = StringUtil.getDificultyString(difficulty); 
        while(!hash.substring( 0, difficulty).equals(target)) {
            nonce ++;
            hash = calculateHash();
        }
        System.out.println("Block Mined!!! : " + hash);
    }

    //Add transactions to this block
    public boolean addTransaction(Transaction transaction) {
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
    }
}
