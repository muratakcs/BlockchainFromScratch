//import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
//import java.util.Base64;

public class StringUtil {

    // method to generate hash of a string
    // public static String generateHash(String data) throws NoSuchAlgorithmException {
    //     MessageDigest digest = MessageDigest.getInstance("SHA-256");
    //     byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
    //     return Base64.getEncoder().encodeToString(hash);
    // }

    // To easily select a certain hash for the whole project
    public static String hash(String input) {
        return applySha256(input);
    }
    
    // Gets a string and returns SHA256 of it.
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

    // Creates a string of zeros of length difficulty.
    public static String getDificultyString(int difficulty) {
        return new String(new char[difficulty]).replace('\0', '0');
    }

    public static String hex2Bin(String hexString) {
        StringBuilder binaryString = new StringBuilder();

        for(int i = 0; i < hexString.length(); i++){
            int hexDigit = Integer.parseInt(hexString.substring(i, i+1), 16);
            String binaryDigit = Integer.toBinaryString(hexDigit);

            // pad with 4 zeros to maintain length consistency in binary string
            while(binaryDigit.length() < 4) {
                binaryDigit = "0" + binaryDigit;
            }

            binaryString.append(binaryDigit);
        }

        return binaryString.toString();
    }

    public static void main(String [] args) throws NoSuchAlgorithmException {
        String x = "Hello World.";
        //System.out.println(generateHash(x));
        //System.out.println(hex2Bin(generateHash(x)));
        System.out.println(applySha256(x));
        System.out.println(hex2Bin(applySha256(x)));
    }
    
}
