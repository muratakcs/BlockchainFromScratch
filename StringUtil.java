import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

public class StringUtil {

    public static String applySha256(String input){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");	        
            byte[] hash = digest.digest(input.getBytes("UTF-8"));	        
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if(hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Calculates the Merkle root of all given transactions
    // Normally, the length of transactions must be a power of 2 for this to work properly
    // Otherwise the last element and possibly some other elements may not affect the root
    public static String getMerkleRoot(List<Transaction> transactions) {
        int count = transactions.size();
        ArrayList<String> previousTreeLayer = new ArrayList<String>();
        for(Transaction transaction : transactions) {
            previousTreeLayer.add(transaction.transactionId);
        }
        ArrayList<String> treeLayer = previousTreeLayer;
        while(count > 1) {
            treeLayer = new ArrayList<String>();
            for(int i=1; i < previousTreeLayer.size(); i+=2) {
                treeLayer.add(applySha256(previousTreeLayer.get(i-1) + previousTreeLayer.get(i)));
            }
            count = treeLayer.size();
            previousTreeLayer = treeLayer;
        }
        String merkleRoot = (treeLayer.size() == 1) ? treeLayer.get(0) : "";
        return merkleRoot;
    }

    // This method simply creates a string of zeros the length of which is equal to the difficulty
    public static String getDificultyString(int difficulty) {
        return new String(new char[difficulty]).replace('\0', '0');
    }

    /*public static PublicKey stringToPublicKey(String publicKeyString) {
        try {
            byte[] byteKey = Base64.getDecoder().decode(publicKeyString.getBytes());
            X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(byteKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");
    
            return kf.generatePublic(X509publicKey);
        } catch (Exception e) {
            throw new RuntimeException("Error reconstructing public key from string representation", e);
        }
    }

    public static String publicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    
    */
    


}
