import java.security.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.X509EncodedKeySpec;
import java.math.BigInteger;
import java.util.stream.Collectors;



class Transaction {
    public Peer peer; // Whose copy of this transaction? Normally we won't need this but for the simulation we need it 
    public String transactionId; //Contains a hash of transaction
    public PublicKey sender; //Senders address/public key.
    public PublicKey recipient; //Recipients address/public key.
    public long timeStamp;
    public int value;
    public byte[] signature; // Signature to prevent others from spending funds in our wallet
    

    public List<TransactionInput> inputs = new ArrayList<TransactionInput>();
    public List<TransactionOutput> outputs = new ArrayList<TransactionOutput>();



    // Constructor to be used when peer is first generating a transaction
    public Transaction(PublicKey from, PublicKey to, int value, List<TransactionInput> inputs, Peer p) {
        this.sender = from;
        this.recipient = to;
        this.value = value;
        this.inputs = inputs;
        this.timeStamp=System.currentTimeMillis();
        this.peer=p;
    }

    // Constructor to be used when it is an incoming transaction to be copied to our list
    public Transaction(String transactionId, PublicKey sender, PublicKey recipient, int value, long timeStamp, 
                   List<TransactionInput> inputs, List<TransactionOutput> outputs, Peer p) {
        this.transactionId = transactionId;
        this.sender = sender;
        this.recipient = recipient;
        this.value = value;
        this.timeStamp = timeStamp;
        this.inputs = inputs;
        this.outputs = outputs;
        this.peer=p;
    }


    // This Calculates the transaction hash 
    public String calculateHash() {
        String dataToHash = "" + this.sender + this.recipient + this.value;
        MessageDigest digest;
        byte[] hash = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return bytesToHex(hash); // This function converts byte array into a hexadecimal string.
    }
    
    public static String bytesToHex(byte[] hash) {
        BigInteger number = new BigInteger(1, hash);
        StringBuilder hexString = new StringBuilder(number.toString(16));
        while (hexString.length() < 32) {
            hexString.insert(0, '0');
        }
        return hexString.toString();
    }


    public void generateSignature(PrivateKey privateKey) {
        String data = Wallet.getStringFromPublicKey(sender) + Wallet.getStringFromPublicKey(recipient) + Integer.toString(value);
        signature = applyECDSASig(privateKey, data);        
    }

    private byte[] applyECDSASig(PrivateKey privateKey, String input) {
        Signature dsa;
        byte[] output = new byte[0];
        try {
            dsa = Signature.getInstance("ECDSA", "BC");
            dsa.initSign(privateKey);
            byte[] strByte = input.getBytes();
            dsa.update(strByte);
            byte[] realSig = dsa.sign();
            output = realSig;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return output;
    }

    public boolean verifySignature() {
        String data = Wallet.getStringFromPublicKey(sender) + Wallet.getStringFromPublicKey(recipient) + Integer.toString(value);
        return verifyECDSASig(Wallet.getStringFromPublicKey(sender), data, signature);      
    }

    private boolean verifyECDSASig(String publicKey, String data, byte[] signature) {
        try {
            Signature ecdsaVerify = Signature.getInstance("ECDSA", "BC");
            ecdsaVerify.initVerify(getPublicKeyFromString(publicKey));
            ecdsaVerify.update(data.getBytes());
            return ecdsaVerify.verify(signature);
        }catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean processTransaction() {
        if(!verifySignature()) {
            System.out.println("Transaction Signature failed to verify");
            return false;
        }
        
        // Gather transaction inputs (Make sure they are unspent):
        for(TransactionInput i : inputs) {
            i.UTXO = peer.blockchain.UTXOs.get(i.transactionOutputId);
        }

        // Check if transaction is valid:
        if(getInputsValue() < Blockchain.minimumTransaction) {
            System.out.println("Transaction Inputs too small: " + getInputsValue());
            return false;
        }

        // Generate transaction outputs:
        int leftOver = getInputsValue() - value; //get value of inputs then the left over change:
        transactionId = calculateHash();
        outputs.add(new TransactionOutput(this.recipient, value, transactionId)); //send value to recipient
        outputs.add(new TransactionOutput(this.sender, leftOver, transactionId)); //send the left over 'change' back to sender
        
        // Add outputs to Unspent list
        for(TransactionOutput o : outputs) {
            peer.blockchain.UTXOs.put(o.id , o);
        }

        // Remove transaction inputs from UTXO lists as spent:
        for(TransactionInput i : inputs) {
            if(i.UTXO == null) continue; //if Transaction can't be found skip it 
            peer.blockchain.UTXOs.remove(i.UTXO.id);
        }

        return true;
    }
    public PublicKey getPublicKeyFromString(String key) {
        try {
            byte[] byteKey = Base64.getDecoder().decode(key.getBytes());
            X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(byteKey);
            KeyFactory kf = KeyFactory.getInstance("EC");
    
            return kf.generatePublic(X509publicKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getInputsValue() {
        int total = 0;
        for (TransactionInput i : inputs) {
            if (i.UTXO == null) continue; // if Transaction can't be found skip it
            total += i.UTXO.value;
        }
        return total;
    }

    public int getOutputsValue() {
        int total = 0;
        for (TransactionOutput o : outputs) {
            total += o.value;
        }
        return total;
    }


    // Converts the transaction into a delimited string.
    public String toString() {
        String inputString = inputs.stream()
            .map(i -> i.transactionOutputId + ":" + i.UTXO.value)
            .collect(Collectors.joining(","));
        String outputString = outputs.stream()
            .map(o -> o.id + ":" + o.value)
            .collect(Collectors.joining(","));
    
        String senderString = Base64.getEncoder().encodeToString(sender.getEncoded());
        String recipientString = Base64.getEncoder().encodeToString(recipient.getEncoded());
    
        return transactionId + ";" + senderString + ";" + recipientString + ";" + value + ";" + inputString + ";" + outputString;
    }
    

    // Reconstructs the transaction from a delimited string.
    public static Transaction fromString(String s, Peer peer) throws NumberFormatException, Exception {
        String[] parts = s.split(";");
        PublicKey senderKey = Wallet.getPublicKeyFromString(parts[1]);
        PublicKey recipientKey = Wallet.getPublicKeyFromString(parts[2]);
        
        Transaction t = new Transaction(senderKey, recipientKey, Integer.parseInt(parts[3]), new ArrayList<>(), peer);
        t.transactionId = parts[0];
    
        if (parts.length > 4 && !parts[4].isEmpty()) {
            for (String input : parts[4].split(",")) {
                String[] inputParts = input.split(":");
                t.inputs.add(new TransactionInput(inputParts[0]));
            }
        }
    
        if (parts.length > 5 && !parts[5].isEmpty()) {
                for (String output : parts[5].split(",")) {
                String[] outputParts = output.split(":");
                t.outputs.add(new TransactionOutput(t.recipient, Integer.parseInt(outputParts[1]), t.transactionId));
            }
        }
    
        return t;
    }

}