import javafx.util.Pair;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class CheckAnswer implements Comparable<CheckAnswer> {
    private  static long globalTimeout = 10000;
    private DatagramSocket self;
    private Pair<Integer, String> checkFrom;
    private Map<Pair<Integer, String>, List<String>> neigh;
    private String guid;
    private String mes;
    private int type;
    private long start;
    private FindSafeNode findSafeNode = FindSafeNode.getInstance();

    public CheckAnswer(DatagramSocket self, int type, Pair<Integer, String> checkFrom, Map<Pair<Integer, String>, List<String>> neigh, String guid, String mes) {
        this.self = self;
        this.type = type;
        this.checkFrom = checkFrom;
        this.neigh = neigh;
        this.guid = guid;
        this.mes = mes;
    }

    public String getGuid() {
        return guid;
    }
    public void setCheckFrom(Pair<Integer, String> checkFrom) {
        this.checkFrom = checkFrom;
    }
    public void startResend() {
        start = System.currentTimeMillis();
    }

    public int isDead() {
        int retValue = 0;
        synchronized (Main.class) {
            List<String> nullCheck = neigh.get(checkFrom);
            if (nullCheck != null) {
                if (nullCheck.contains(guid)) {
                   //     System.out.println("dead" + checkFrom.getKey() + " guid: " + guid);
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
                        neigh.get(checkFrom).add(guid+":1");
                        try {
                            byte[] byteMes = ("1\n"+guid+":1" + "\n\n").getBytes();
                            InetAddress adr = InetAddress.getByName(checkFrom.getValue());
                            DatagramPacket packet = new DatagramPacket(byteMes, byteMes.length, adr, checkFrom.getKey());
                            synchronized (RecvMes.class) {
                                self.send(packet);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    retValue = 1;
                    if(neigh.size() != 0) {
                        if(!neigh.containsKey(checkFrom)) {
                            retValue = 0;
                            findSafeNode.checkSafery(checkFrom);
                        }
                    }
                }
            }
        }
        return retValue;
    }

    public int resend() {

        synchronized (Main.class) {
            List<String> nullCh = neigh.get(checkFrom);
            if (nullCh != null) {
                if (System.currentTimeMillis() - start < globalTimeout) {
                    if (nullCh.contains(guid)) {
                        byte[] byteMes = (type + "\n" + guid + "\n" + mes + "\n").getBytes();
                        try {
                            InetAddress adr = InetAddress.getByName(checkFrom.getValue());
                            DatagramPacket packet = new DatagramPacket(byteMes, byteMes.length, adr, checkFrom.getKey());
                            synchronized (RecvMes.class) {
                                self.send(packet);
                            }
                       //     System.out.println("resended : " + guid + " :  " + mes);
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return 1;
                    }
                }
            }
        }
            return 0;
        }

    @Override
    public int compareTo(CheckAnswer o) {
        return guid.compareTo(o.guid);
    }
}
