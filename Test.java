import java.util.Random;

public class Test {
    public static void main(String[] args) {

        Random rand = new Random(System.currentTimeMillis());
        
        // Creating peers.
        Peer.peers = new Peer[4];
        for(int i=0; i<4; i++) {
            Peer.peers[i] = new Peer();
            Peer.peers[i].startServer();
            Peer.peers[i].startConsumer();
            Peer.peers[i].startPeerTransactions();
        }
        
        // Wait to make sure servers are properly opened
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Sending a few random messages
        int s,r;
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

        

        
    }
    
}
