import javafx.util.Pair;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecvMes {
    private  static long globalTimeout = 10000;
    private volatile DatagramSocket self;
    private Map<Pair<Integer, String>, List<String>> neigh;
    private Map<String, String> messages;
    private ReadNode readNode;
    private Map<Pair<String, String>, Long>  received;
    private Integer loss;
    private FindSafeNode findSafeNode = FindSafeNode.getInstance();

    public RecvMes(Integer loss, ReadNode readNode, DatagramSocket self, Map<Pair<Integer, String>, List<String>> neigh,  Map<String, String> messages) {
        this.readNode = readNode;
        this.self = self;
        this.messages = messages;
        this.neigh = neigh;
        received = new HashMap<>();
        this.loss = loss;
    }

    private void sendAnswer(String guid, String mes, DatagramPacket packet) throws IOException {
        byte[] byteMes = ("2\n" + guid + "\n" + mes).getBytes();
        DatagramPacket answer = new DatagramPacket(byteMes, byteMes.length, packet.getAddress(), packet.getPort());
        synchronized (RecvMes.class) {
            self.send(answer);
        }
    }

    private void isSafeChanged() {
        Pair<Integer, String> oldSafe = findSafeNode.getSafeNode(new Pair<>(self.getLocalPort(), "127.0.0.1"));
        if(oldSafe != null) {
            findSafeNode.findSafeNode(new Pair<>(self.getLocalPort(), "127.0.0.1"), neigh.keySet());
            Pair<Integer, String> newSafe = findSafeNode.getSafeNode(new Pair<>(self.getLocalPort(), "127.0.0.1"));
            if(newSafe != null) {
                if(!oldSafe.equals(newSafe)) {
                    findSafeNode.findSafeNode(new Pair<>(self.getLocalPort(), "127.0.0.1"), neigh.keySet());
                    readNode.sendSafe();
                }
                else {
                    findSafeNode.findSafeNode(new Pair<>(self.getLocalPort(), "127.0.0.1"), neigh.keySet());
                    readNode.sendSafe();
                }
            }
        } else {
            findSafeNode.findSafeNode(new Pair<>(self.getLocalPort(), "127.0.0.1"), neigh.keySet());
            readNode.sendSafe();
        }
    }

//clean byte mass
    public void recvMes() {
        int count = 0;

        while(true) {
            byte[] mes = new byte[1024];
            DatagramPacket packet = new DatagramPacket(mes, mes.length);

            try {
                self.receive(packet);
                if((Math.random()*99) < loss)
                    continue;

                String dryMes = new String(packet.getData());
                String[] splitMes = dryMes.split("\n");
               // if(splitMes.length != 3)
               //     continue;
                //String ip = packet.getAddress().toString();
                String ip = "127.0.0.1";
                Integer port = packet.getPort();
                Pair<Integer, String> pair = new Pair<>(port, ip);
                //пришло сообщение о надежном соседе
                if(Integer.parseInt(splitMes[0]) == 0) {
                    String[] newSafe = splitMes[2].split(":");
                    if(newSafe.length == 2) {
                        findSafeNode.registerSafeNode(pair, new Pair<>(Integer.parseInt(newSafe[0]), newSafe[1]));
                    }
                    sendAnswer(splitMes[1], splitMes[2], packet);
                    if(!received.containsKey(new Pair<>(splitMes[1], splitMes[2]))) {
                        System.out.println("safe node got: " + splitMes[2] + " with guid: " + splitMes[1]);
                        received.put(new Pair<>(splitMes[1], splitMes[2]), System.currentTimeMillis());
                    }
                }

                if(Integer.parseInt(splitMes[0]) == 3) {
                    synchronized (Main.class) {
                        if (!neigh.containsKey(pair)) {
                            if(neigh != null) {
                                neigh.put(pair, new ArrayList<>());
                                findSafeNode.findSafeNode(new Pair<>(self.getLocalPort(), "127.0.0.1"), neigh.keySet());
                                readNode.sendSafe();
                                //             readNode.startResend();
                                count++;
                            }
                        }
                    }
                }
                //обычное сообщение
                if(Integer.parseInt(splitMes[0]) == 1) {
                    synchronized (Main.class) {
                        if (!neigh.containsKey(pair)) {
                            if(neigh != null) {
                                neigh.put(pair, new ArrayList<>());
                                findSafeNode.findSafeNode(new Pair<>(self.getLocalPort(), "127.0.0.1"), neigh.keySet());
                                readNode.sendSafe();
                                count++;
                            }
                        } else {
                           isSafeChanged();
                        }
                    }

                    sendAnswer(splitMes[1], splitMes[2], packet);
                    readNode.sendNeigh(splitMes[2], new Pair<Integer, String>(packet.getPort(), "127.0.0.1"));
                    if(splitMes[2].length() != 0)
                        System.out.println("mes recved : " + splitMes[2] + " with guid: " + splitMes[1]);

                }
                //ответное сообщение
                if(Integer.parseInt(splitMes[0]) == 2) {
                    synchronized (Main.class) {
                        messages.remove(splitMes[1]);
                        List<Pair<Integer, String>> arr = new ArrayList<>(neigh.keySet());
                        for(int j =0; j < arr.size(); j ++) {
                            if(arr.get(j).getKey().equals(pair.getKey())) {
                                if(neigh.get(arr.get(j)) != null)
                                    neigh.get(arr.get(j)).remove(splitMes[1]);
                            }
                        }
                    }
                    System.out.println("answer got: " + splitMes[1]);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            List<Pair<String, String>> recKeys = new ArrayList<>(received.keySet());
            for(int i = 0; i < recKeys.size(); i++) {
                if((System.currentTimeMillis() - received.get(recKeys.get(i))) > globalTimeout) {
                    received.remove(recKeys.get(i));
                }
            }
        }
    }
}
