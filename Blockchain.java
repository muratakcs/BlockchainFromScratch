import java.util.ArrayList;
import java.util.HashMap;

public class Blockchain {
    
    public ArrayList<Block> blockchain = new ArrayList<Block>();
    public HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>(); 
    public static int difficulty = 5;
    public static int minimumTransaction = 1;

    // Add transactions to the blockchain
    public void addBlock(Block newBlock) {
        newBlock.mineBlock(difficulty);
        blockchain.add(newBlock);
    }
    public Block getLastBlock() {
        return blockchain.get(blockchain.size()-1);
    }
    

    // Check if the blockchain is valid
    public Boolean isChainValid() {
        Block currentBlock; 
        Block previousBlock;
        String hashTarget = new String(new char[difficulty]).replace('\0', '0');
        HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>(); //a temporary working list of unspent transactions at a given block state.
        tempUTXOs.putAll(UTXOs);
        
        // Loop through blockchain to check hashes:
        for(int i=1; i < blockchain.size(); i++) {
            currentBlock = blockchain.get(i);
            previousBlock = blockchain.get(i-1);
            // Compare registered hash and calculated hash:
            if(!currentBlock.hash.equals(currentBlock.calculateHash()) ){
                System.out.println("#Current Hashes not equal");
                return false;
            }
            // Compare previous hash and registered previous hash
            if(!previousBlock.hash.equals(currentBlock.previousHash) ) {
                System.out.println("#Previous Hashes not equal");
                return false;
            }
            // Check if hash is solved
            if(!currentBlock.hash.substring( 0, difficulty).equals(hashTarget)) {
                System.out.println("#This block hasn't been mined");
                return false;
            }
            
            // Loop thru blockchains transactions:
            TransactionOutput tempOutput;
            for(int t=0; t <currentBlock.transactions.size(); t++) {
                Transaction currentTransaction = currentBlock.transactions.get(t);
                
                if(!currentTransaction.verifySignature()){
                    System.out.println("#Signature on Transaction(" + t + ") is Invalid");
                    return false; 
                }
                if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
                    System.out.println("#Inputs are note equal to outputs on transaction(" + t + ")");
                    return false; 
                }
                
                for(TransactionInput input: currentTransaction.inputs) {
                    tempOutput = tempUTXOs.get(input.transactionOutputId);
                    
                    if(tempOutput == null) {
                        System.out.println("#Referenced input on Transaction(" + t + ") is Missing");
                        return false;
                    }
                    
                    if(input.UTXO.value != tempOutput.value) {
                        System.out.println("#Referenced input Transaction(" + t + ") value is Invalid");
                        return false;
                    }
                    
                    tempUTXOs.remove(input.transactionOutputId);
                }
                
                for(TransactionOutput output: currentTransaction.outputs) {
                    tempUTXOs.put(output.id, output);
                }
                
                if(currentTransaction.outputs.get(0).recipient != currentTransaction.recipient) {
                    System.out.println("#Transaction(" + t + ") output recipient is not who it should be");
                    return false;
                }
                if(currentTransaction.outputs.get(1).recipient != currentTransaction.sender) {
                    System.out.println("#Transaction(" + t + ") output 'change' is not sender.");
                    return false;
                }
                
            }
            
        }
        System.out.println("Blockchain is valid");
        return true;
    }
}
