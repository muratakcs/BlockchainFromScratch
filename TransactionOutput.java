import java.util.ArrayList;
import java.util.List;

//We can think of transaction outputs as a check that can be turned into cash and spent later.
class TransactionOutput {
    public String id;
    public String recipient;
    public float value;
    
    public TransactionOutput(String recipient, float value, String parentTransactionId) {
        this.recipient = recipient;
        this.value = value;
        this.id = calculateHash();
    }
    
    //Calculates the hash by using recipient + value + parentTransactionId
    public String calculateHash() {
        // hash will be calculated here...
        return "Calculated hash value";
    }




    public class Main {
        public static List<TransactionOutput> UTXOs = new ArrayList<>(); //list of all unspent transactions. 
    
        public static void main(String[] args) {
            Transaction transaction = new Transaction("Alice", "Bob", 5f);
            TransactionOutput output = new TransactionOutput("Bob", 5f, transaction.transactionId);
            UTXOs.add(output); //adding to list of all unspent transactions.
        }
    }


}