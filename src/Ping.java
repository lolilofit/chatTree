import javafx.util.Pair;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
        List<CheckAnswer> l = new ArrayList<>();

        while (true) {
            synchronized (Main.class) {
                neigh.entrySet().forEach(el -> {
                    byte[] byteMes = ("1\n" + self.getLocalPort() + ":1:"+count + "\n\n").getBytes();
                    try {
                        InetAddress adr = InetAddress.getByName(el.getKey().getValue());
                        DatagramPacket packet = new DatagramPacket(byteMes, byteMes.length, adr, el.getKey().getKey());
                        self.send(packet);
                        CheckAnswer ch = new CheckAnswer(self, 1, el.getKey(), neigh, self.getLocalPort() + ":1:"+count, "");
                        l.add(ch);
                        List<String> list = neigh.get(el.getKey());
                        if(list != null)
                            list.add(self.getLocalPort() + ":1:"+count);

                    } catch (UnknownHostException e) { e.printStackTrace(); } catch (IOException e) {
                        e.printStackTrace();
                    }
                    count++;
                });
            }

            for(int i = 0; i < l.size(); i++) {
                l.get(i).startResend();
                while(l.get(i).resend() == 1) {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                l.get(i).isDead();
            }

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
