class Transaction {
    public String transactionId; //Contains a hash of transaction*
    public String sender; //Senders address/public key.
    public String recipient; //Recipients address/public key.
    public float value;
    public List<TransactionOutput> outputs = new ArrayList<TransactionOutput>();

    // Constructor
    public Transaction(String from, String to, float value) {
        this.sender = from;
        this.recipient = to;
        this.value = value;
    }

    // This Calculates the transaction hash 
    public String calculateHash() {
        //... insert your hash calculation here
    }
}