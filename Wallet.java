import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.Base64;

public class Wallet {
    public PrivateKey privateKey;   // Private key associated with the wallet
    public PublicKey publicKey;     // Public key associated with the wallet
    public int peerid;              // The id of the peer that has this wallet
    public HashMap<String,TransactionOutput> UTXOs; // Unspent txs available for this wallet to spend

    public Wallet(int id){
        peerid = id;
        generateKeyPair();
        UTXOs = new HashMap<String,TransactionOutput>(); // Only UTXOs owned by this wallet
    }

    public void generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair pair = keyGen.generateKeyPair();
            this.privateKey = pair.getPrivate();
            this.publicKey = pair.getPublic();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // This method calculates the balance of this wallet while
    // also adding incoming UTXOs of this wallet to UTXO list 
    public int getBalance() {
        int total = 0;	
        for (Map.Entry<String, TransactionOutput> item: Peer.peers[peerid].blockchain.UTXOs.entrySet()){
            TransactionOutput UTXO = item.getValue();
            if(UTXO.isMine(publicKey)) { // If output belongs to me (if coins belong to me)
                UTXOs.put(UTXO.id,UTXO); // Add it to our list of unspent transactions
                total += UTXO.value;
            }
        }  
        return total;
    }


    public Transaction sendFunds(PublicKey _recipient, int value ) {

        // First, gather UTXOs and balance and check whether we have enough funds to send
        if(getBalance() < value) { 
            System.out.println("!!! Not Enough funds to send transaction. Transaction Discarded.");
            return null;
        }

        // Create array list of Tx inputs
        List<TransactionInput> inputs = new ArrayList<>();
    
        int total = 0;
        for (Map.Entry<String, TransactionOutput> item: UTXOs.entrySet()){
            TransactionOutput UTXO = item.getValue();
            total += UTXO.value;
            inputs.add(new TransactionInput(UTXO.id));
            if(total > value) break; // When we have enough of the UTXOs to afford value, stop
        }
        
        // Prepare the transaction
        Transaction newTransaction = new Transaction(publicKey, _recipient, value, inputs, Peer.peers[peerid]);
        
        // Sign the new transaction with our private key
        newTransaction.generateSignature(privateKey);
        
        // If a UTXO is spent with this tx, remove it from UTXOs list
        for(TransactionInput input: inputs){
            UTXOs.remove(input.transactionOutputId);
        }
        return newTransaction;
    }

    // Return the private key of this wallet
    public String getPrivateKeyString() {
        return Base64.getEncoder().encodeToString(this.privateKey.getEncoded());
    }

    // Return the public key of this wallet
    public String getPublicKeyString() {
        return Base64.getEncoder().encodeToString(this.publicKey.getEncoded());
    }


    // STATIC UTILITY FUNCTIONS REGARDING KEY-STRING TRANSFORMATIONS

    // A utility function to turn a private key into a string
    public static String getStringFromPrivateKey(PrivateKey pk) {
        return Base64.getEncoder().encodeToString(pk.getEncoded());
    }

    // A utility function to turn a public key into a string
    public static String getStringFromPublicKey(PublicKey pk) {
        return Base64.getEncoder().encodeToString(pk.getEncoded());
    }

    // A utility function to turn a string back into a private key
    public static PrivateKey getPrivateKeyFromString(String key) throws Exception {
        byte[] byteKey = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(byteKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    // A utility function to turn a string back into a public key
    public static PublicKey getPublicKeyFromString(String key) throws Exception {
        byte[] byteKey = Base64.getDecoder().decode(key);
        
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(byteKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

}
