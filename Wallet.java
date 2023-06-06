import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.Base64;

public class Wallet {
    public PrivateKey privateKey;
    public PublicKey publicKey;
    public int peerid;

    public HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>(); // Only UTXOs owned by this wallet.

    public Wallet(int id){
        generateKeyPair();
        peerid=id;  
    }

    public void generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair pair = keyGen.generateKeyPair();
            this.privateKey = pair.getPrivate();
            this.publicKey = pair.getPublic();


            //SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            //KeyPair keyPair = keyGen.generateKeyPair();
            //privateKey = keyPair.getPrivate();
            //publicKey = keyPair.getPublic();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getBalance() {
        int total = 0;	
        for (Map.Entry<String, TransactionOutput> item: Peer.peers[peerid].blockchain.UTXOs.entrySet()){
            TransactionOutput UTXO = item.getValue();
            if(UTXO.isMine(publicKey)) { // If output belongs to me (if coins belong to me)
                UTXOs.put(UTXO.id,UTXO); // Add it to our list of unspent transactions.
                total += UTXO.value ;
            }
        }  
        return total;
    }

    
    

    public Transaction sendFunds(PublicKey _recipient, int value ) {
        if(getBalance() < value) { // Gather balance and check funds.
            System.out.println("#Not Enough funds to send transaction. Transaction Discarded.");
            return null;
        }
        // Create array list of inputs
        List<TransactionInput> inputs = new ArrayList<>();
    
        int total = 0;
        for (Map.Entry<String, TransactionOutput> item: UTXOs.entrySet()){
            TransactionOutput UTXO = item.getValue();
            total += UTXO.value;
            inputs.add(new TransactionInput(UTXO.id));
            if(total > value) break;
        }
        
        Transaction newTransaction = new Transaction(publicKey, _recipient, value, inputs, Peer.peers[peerid]);
        newTransaction.generateSignature(privateKey);
        
        for(TransactionInput input: inputs){
            UTXOs.remove(input.transactionOutputId);
        }
        return newTransaction;
    }

    public String getPrivateKeyString() {
        return Base64.getEncoder().encodeToString(this.privateKey.getEncoded());
    }

    public String getPublicKeyString() {
        return Base64.getEncoder().encodeToString(this.publicKey.getEncoded());
    }

    public static String getStringFromPrivateKey(PrivateKey pk) {
        return Base64.getEncoder().encodeToString(pk.getEncoded());
    }

    public static String getStringFromPublicKey(PublicKey pk) {
        return Base64.getEncoder().encodeToString(pk.getEncoded());
    }

    public static PrivateKey getPrivateKeyFromString(String key) throws Exception {
        byte[] byteKey = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(byteKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    public static PublicKey getPublicKeyFromString(String key) throws Exception {
        byte[] byteKey = Base64.getDecoder().decode(key);
        
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(byteKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

}
