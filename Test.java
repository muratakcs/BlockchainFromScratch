//import java.util.Random;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

public class Test {
    public static void main(String[] args) throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {

        //Random rand = new Random(System.currentTimeMillis());
        
        // Creating peers.
        Peer.peers = new Peer[4];
        for(int i=0; i<4; i++) {
            Peer.peers[i] = new Peer();
        }

        // One of the peers creates the genesis block
        // Here, peers[0] is our Satoshi Nakamoto...
        Peer.peers[0].createGenesisBlock();

        // Now, our blockchain is ready for new blocks being added
        // so all peers start sending transactions back and forth,
        // mining, etc.
        for(int i=0; i<4; i++) {
            Peer.peers[i].startConsumer();
            Peer.peers[i].startServer();
            Peer.peers[i].startPeerTransactions();
        }
        
        // Wait to make sure servers are properly opened
        /*try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        // Sending a few random messages
        /*int s,r;
        for(int i=0; i<10; i++) {
            s = rand.nextInt(4);
            r = s;
            while(r==s) r=rand.nextInt(4);
            Peer.peers[s].sendMessage(r, "Hello from Peer "+s+"!");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        // Random broadcasting experiment
        Peer.peers[2].broadcastToAllPeers("Hello from peer "+2);

        */

        
    }
    
}
