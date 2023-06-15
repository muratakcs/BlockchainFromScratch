import java.security.*;
import java.util.*;
import java.util.stream.Collectors;



class Transaction {
    public Peer peer;   // Who initiated this transaction?
                        //Normally we wouldn't need this but for the simulation we do 
    public String transactionId; //Contains a hash of transaction
    public PublicKey sender; //Sender's address/public key.
    public PublicKey recipient; //Recipient's address/public key.
    public long timeStamp; // Time of first creation
    public int value; // The amount to be transferred
    public byte[] signature; // Signature to prevent others from spending funds in our wallet
    
    // Transaction inputs (The UTXOs coming to the sender and will be spent in this Tx)
    public List<TransactionInput> inputs;

    // UTXOs that will be generated by this Tx. The money that will be paid and possibly the change.
    public List<TransactionOutput> outputs;



    //There must be at least three type of constructors for different
    //situations where transaction objects are created:
    // 1. When a peer wants to pay someone else so requests a transaction
    // 2. When the miner is preparing the coinbase transaction to itself
    // 3. When a peer gets a tx request from others

    // Constructor for creating a coinbase transaction (mining reward tx) (2)
    public Transaction(PublicKey to, int value, Peer p) throws NoSuchAlgorithmException {
        this.peer = p;
        this.sender = null; // There is no sender for a mining reward transaction
        this.recipient = to;
        this.value = value;
        this.timeStamp = System.currentTimeMillis();
        this.transactionId = calculateHash();   // Setting id before finding out outputs a little misleading, will be fixed later.
        inputs = null;//new ArrayList<TransactionInput>();
        outputs = new ArrayList<TransactionOutput>();
        this.peer.wallet.arrangeFunds(to, value, outputs, this.transactionId);
    }

    // Constructor to be used when peer is initially creating the transaction (1)
    public Transaction(PublicKey from, PublicKey to, int value, Peer p) throws NoSuchAlgorithmException {
        this.peer = p;
        this.sender = from;
        this.recipient = to;
        this.value = value;
        this.timeStamp = System.currentTimeMillis();
        this.transactionId = calculateHash();
        inputs = new ArrayList<TransactionInput>();
        outputs = new ArrayList<TransactionOutput>();
        this.peer.wallet.arrangeFunds(to, value, inputs, outputs, this.transactionId);
    }

    // Constructor to be used when it is an incoming transaction to be copied to our list (3)
    public Transaction(PublicKey from, PublicKey to, int value, long timeStamp, 
                   List<TransactionInput> in, List<TransactionOutput> out, String transactionId, Peer p) {
        this.peer = p;
        this.sender = from;
        this.recipient = to;
        this.value = value;
        this.timeStamp = timeStamp;
        this.transactionId = transactionId;
        inputs = new ArrayList<TransactionInput>();
        outputs = new ArrayList<TransactionOutput>();
        this.inputs = in;   //We may need to
        this.outputs = out; //clone these two??
    }


    // This Calculates the transaction hash 
    public String calculateHash() throws NoSuchAlgorithmException {
        String dataToHash = "" + this.sender + this.recipient + this.value + this.timeStamp;// There is a subtle caveat here!
        
        // Add inputs to the hash
        //for (TransactionInput input : this.inputs) {
        //    dataToHash += input.transactionOutputId;
        //}
        
        // Add outputs to the hash
        //for (TransactionOutput output : this.outputs) {
        //    dataToHash += output.id;
        //}
        
        /*MessageDigest digest;
        byte[] hash = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            hash = digest.digest(dataToHash.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return bytesToHex(hash);*/
        return StringUtil.hash(dataToHash);
    }
    
    
    /*public static String bytesToHex(byte[] hash) {
        BigInteger number = new BigInteger(1, hash);
        StringBuilder hexString = new StringBuilder(number.toString(16));
        while (hexString.length() < 32) {
            hexString.insert(0, '0');
        }
        return hexString.toString();
    }*/

    
    
    // Prepare input for signing and call signature function
    public void generateSignature(PrivateKey privateKey) throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
        // Prepare data
        String data = Wallet.getStringFromPublicKey(sender)
                    + Wallet.getStringFromPublicKey(recipient)
                    + Integer.toString(value) 
                    + Long.toString(timeStamp);
        for (TransactionInput input : this.inputs)      { data += input.transactionOutputId; }
        for (TransactionOutput output : this.outputs)   { data += output.id; }
        
        //Sign
        signature = peer.wallet.sign(data);
        //Now, the signature for this transaction is ready.
    }

    

    
    // Prepare the input for signature verification
    public boolean verifySignature() throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {
        //Prepare data
        String data = Wallet.getStringFromPublicKey(sender) 
                    + Wallet.getStringFromPublicKey(recipient)
                    + Integer.toString(value) 
                    + Long.toString(timeStamp);
        for (TransactionInput input : this.inputs) { data += input.transactionOutputId; }
        for (TransactionOutput output : this.outputs) { data += output.id; }

        //Verify
        if(this.sender==null) return true; //coinbase transaction => no signature at all
        return peer.wallet.verify(sender,data,signature);
        //return verifyECDSASig(Wallet.getStringFromPublicKey(sender), data, signature);      
    }

    

    public boolean processTransaction() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        // If the signature is not valid, reject the transaction
        if(!verifySignature()) {
            System.out.println("!!! Transaction Signature failed to verify.");
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
//        transactionId = calculateHash();
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
    /*public PublicKey getPublicKeyFromString(String key) {
        try {
            byte[] byteKey = Base64.getDecoder().decode(key.getBytes());
            X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(byteKey);
            KeyFactory kf = KeyFactory.getInstance("EC");
    
            return kf.generatePublic(X509publicKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }*/

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
        .filter(i -> i.UTXO != null)
        .map(i -> i.transactionOutputId + ":" + i.UTXO.value)
        .collect(Collectors.joining(","));
        String outputString = outputs.stream()
            .map(o -> o.id + ":" + o.value)
            .collect(Collectors.joining(","));

        String senderString = sender == null ? "COINBASE" : Base64.getEncoder().encodeToString(sender.getEncoded());
        String recipientString = Base64.getEncoder().encodeToString(recipient.getEncoded());

        return transactionId + ";" + senderString + ";" + recipientString + ";" + value + ";"  + timeStamp + ";" + inputString + ";" + outputString;
    }


    // Reconstructs the transaction from a delimited string.
    public static Transaction fromString(String s, Peer peer) throws NumberFormatException, Exception {
        String[] parts = s.split(";");
        PublicKey senderKey = parts[1].equals("COINBASE") ? null : Wallet.getPublicKeyFromString(parts[1]);
        PublicKey recipientKey = Wallet.getPublicKeyFromString(parts[2]);

        Transaction t = new Transaction(senderKey,
                                        recipientKey,
                                        Integer.parseInt(parts[3]),
                                        Long.parseLong(parts[4]),
                                        new ArrayList<>(),
                                        new ArrayList<>(),
                                        parts[0],
                                        peer);

        int inpindex=5;
        int outindex=6;
        if (parts.length > inpindex && !parts[inpindex].isEmpty()) {
            for (String input : parts[inpindex].split(",")) {
                String[] inputParts = input.split(":");
                t.inputs.add(new TransactionInput(inputParts[0]));
            }
        }

        if (parts.length > outindex && !parts[outindex].isEmpty()) {
            for (String output : parts[outindex].split(",")) {
                String[] outputParts = output.split(":");
                t.outputs.add(new TransactionOutput(t.recipient, Integer.parseInt(outputParts[1]), t.transactionId));
            }
        }

        return t;
    }
}