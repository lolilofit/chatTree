import javafx.util.Pair;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

public class Ping implements Runnable{
    private volatile int count = 0;
    private DatagramSocket self;
    private Map<Pair<Integer, String>, List<String>> neigh;

    public Ping(DatagramSocket self, Map<Pair<Integer, String>, List<String>> neigh) {
        this.neigh = neigh;
        this.self = self;
    }

    @Override
    public void run() {
        while (true) {
            synchronized (Main.class) {
                neigh.entrySet().forEach(el -> {
                    byte[] byteMes = ("3\n" + self.getLocalPort() + ":1:"+count + "\n\n").getBytes();
                    try {
                        InetAddress adr = InetAddress.getByName(el.getKey().getValue());
                        DatagramPacket packet = new DatagramPacket(byteMes, byteMes.length, adr, el.getKey().getKey());
                        self.send(packet);
                    } catch (UnknownHostException e) { e.printStackTrace(); } catch (IOException e) {
                        e.printStackTrace();
                    }
                    count++;
                });
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
