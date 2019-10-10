
import javafx.util.Pair;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class ReadNode implements Runnable {
    private DatagramSocket self;
    private Map<Pair<Integer, String>, List<String>> neigh;
    private Map<String, String> messages;
    private volatile int count = 0;
    private volatile Stack<CheckAnswer> check;

    public ReadNode(DatagramSocket self, Map<Pair<Integer, String>, List<String>> neigh, Map<String, String> messages) {
        this.self = self;
        this.messages = messages;
        this.neigh = neigh;
        check = new Stack<>();
    }

    public void sendNeigh(String mes, Pair<Integer, String> except) {

    synchronized (ReadNode.class) {
        synchronized (Main.class) {
            for (Map.Entry<Pair<Integer, String>, List<String>> entry : neigh.entrySet()) {
                //npe
               // if(!(entry.getKey().equals(except.getKey())) || !(entry.getValue().equals(except.getValue()))) {
                if(!entry.getKey().equals(except)) {
                    String guid = "";
                    synchronized (RecvMes.class) {
                        guid = self.getLocalPort() + ":1" + ":" + count;
                    }
                    byte[] byteMes = new byte[1024];
                    byteMes = ("1" + "\n" + guid + "\n" + mes).getBytes();
                    messages.put(guid, mes);
                    entry.getValue().add(guid);
                    try {
                        InetAddress adr = InetAddress.getByName(entry.getKey().getValue());
                        DatagramPacket packet = new DatagramPacket(byteMes, byteMes.length, adr, entry.getKey().getKey());
                        //синхронизация на всех send
                        synchronized (RecvMes.class) {
                            self.send(packet);
                        }
                        System.out.println("sended : " + guid);
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    synchronized (RecvMes.class) {
                        entry.getValue().add(self.getLocalPort() + ":" + count);
                        CheckAnswer oneOf = new CheckAnswer(self, 1, entry.getKey(), neigh, guid, mes);
                        check.push(oneOf);
                    }
                    count++;
                }
            }
        }
    }
    }

    public void startResend() {
        synchronized (ReadNode.class) {
            for (int i = 0; i < check.size(); i++) {
                Thread resend = new Thread(check.pop());
                resend.start();
            }
        }
    }

    public void sendSafe() {
        FindSafeNode findSafeNode = FindSafeNode.getInstance();
        List<Pair<Integer, String>> key = null;

        findSafeNode.findSafeNode(new Pair<Integer, String>(self.getLocalPort(), "127.0.0.1"), neigh.keySet());
        key = new ArrayList<>(neigh.keySet());

        if(key != null) {
            for (int i = 0; i < neigh.size(); i++) {
                Pair<Integer, String> el = key.get(i);
                String guid = self.getLocalPort() + ":1" + ":" + count;
                Pair<Integer, String> sender = new Pair<>(self.getLocalPort(), "127.0.0.1");
                Pair<Integer, String> dest = findSafeNode.getSafeNode(sender);

                messages.put(guid, dest.getKey()+":"+dest.getValue());

                if(findSafeNode.sendSafeNode(self, sender, el, guid) == 0) {
                    CheckAnswer ch = new CheckAnswer(self, 0, dest, neigh, guid, dest.getKey() + ":" + dest.getValue());
                    synchronized (ReadNode.class) {
                        check.push(ch);
                    }
                }
                else {
                    messages.remove(guid);
                }
                count++;
            }
        }
    }

    @Override
    public void run() {
       // sendSafe();
        startResend();
        //setalarm
        Scanner in = new Scanner(System.in);
        String mes = "";

        synchronized (Main.class) {
            neigh.entrySet().forEach(el -> {
                byte[] byteMes = ("1\n" + self.getLocalPort() + ":1:"+count + "\n"+ "").getBytes();
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

        while(true) {
            mes = in.nextLine();
            sendNeigh(mes, new Pair<>(-1, ""));
            startResend();
        }
    }

}
