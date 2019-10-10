import javafx.util.Pair;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class CheckAnswer implements Runnable{
    private  static long globalTimeout = 10000;
    private DatagramSocket self;
    private Pair<Integer, String> checkFrom;
    private Map<Pair<Integer, String>, List<String>> neigh;
    private String guid;
    private String mes;
    private int type;

    public CheckAnswer(DatagramSocket self, int type, Pair<Integer, String> checkFrom, Map<Pair<Integer, String>, List<String>> neigh, String guid, String mes) {
        this.self = self;
        this.type = type;
        this.checkFrom = checkFrom;
        this.neigh = neigh;
        this.guid = guid;
        this.mes = mes;
    }

    @Override
    public void run() {
        FindSafeNode findSafeNode = FindSafeNode.getInstance();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) { e.printStackTrace(); }

        long start = System.currentTimeMillis();
        while(true) {
        synchronized (Main.class) {
            //npe
            if ((System.currentTimeMillis()-start < globalTimeout) && (neigh.get(checkFrom).contains(guid))) {
                byte[]  byteMes = (type + "\n" + guid + "\n" + mes).getBytes();
                try {
                    InetAddress adr = InetAddress.getByName(checkFrom.getValue());
                    DatagramPacket packet = new DatagramPacket(byteMes, byteMes.length, adr, checkFrom.getKey());
                    synchronized (RecvMes.class) {
                        self.send(packet);
                    }
                    System.out.println("resended : " + guid + " :  " + mes);
                    Thread.sleep(2000);
                } catch (UnknownHostException | InterruptedException e) {
                    //do smth
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            }
            synchronized (Main.class) {
                List<String> nullCheck = neigh.get(checkFrom);
                if(nullCheck != null) {
                    if (nullCheck.contains(guid)) {
                        System.out.println("dead" + checkFrom.getKey() + " guid: " + guid);
                        neigh.remove(checkFrom);
                        Pair<Integer, String> selfPair = new Pair<>(self.getLocalPort(), "127.0.0.1");
                        Pair<Integer, String> currentSafe = findSafeNode.getSafeNode(checkFrom);
                        if (currentSafe != null) {
                            if (currentSafe.equals(checkFrom)) {
                                findSafeNode.removeSafeNode(selfPair);
                                currentSafe = findSafeNode.findSafeNode(selfPair, neigh.keySet());
                            }
                        }
                        if (currentSafe != null && !checkFrom.equals(currentSafe)) {
                            checkFrom = currentSafe;
                            //try to resend safe
                            neigh.put(checkFrom, new ArrayList<>());
                            neigh.get(checkFrom).add(guid);
                            try {
                                byte[] byteMes = ("1\n" + self.getLocalPort() + ":1:" + guid + "\n" + "").getBytes();
                                InetAddress adr = InetAddress.getByName(checkFrom.getValue());
                                DatagramPacket packet = new DatagramPacket(byteMes, byteMes.length, adr, checkFrom.getKey());
                                synchronized (RecvMes.class) {
                                    self.send(packet);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            //neigh.keySet().forEach(el -> findSafeNode.sendSafeNode(self, selfPair, el, guid));
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            start = System.currentTimeMillis();
                            continue;
                        } else {
                            break;
                        }
                    }
                }
            }
            break;
        }

    }
}
