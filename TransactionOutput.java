import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.math.BigInteger;

//We can think of transaction outputs as a check that can be turned into cash and spent later.
class TransactionOutput {
    public String id;
    public PublicKey recipient;
    public int value;
    public String parentTransactionId;

    public TransactionOutput(PublicKey recipient, int value, String parentTransactionId) {
        this.recipient = recipient;
        this.value = value;
        this.parentTransactionId = parentTransactionId;
        this.id = calculateHash();
    }
    
    //Calculates the hash by using recipient + value + parentTransactionId
    public String calculateHash() {
        String dataToHash = "" + this.recipient + this.value + this.parentTransactionId;
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

    public boolean isMine(PublicKey publicKey) {
        return (publicKey.equals(recipient));
    }
}