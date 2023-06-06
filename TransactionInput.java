class TransactionInput {
    // Objects of this class are just like pointers to Tx output objects
    public String transactionOutputId; // Reference to retrieve the TransactionOutput from UTXO list
    public TransactionOutput UTXO; // Pointer to the referred UTXO

    public TransactionInput(String transactionOutputId) {
        this.transactionOutputId = transactionOutputId;
    }
}