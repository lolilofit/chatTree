import javafx.util.Pair;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecvMes {
    private volatile DatagramSocket self;
    private Map<Pair<Integer, String>, List<String>> neigh;
    private Map<String, Integer> mesAnswerCount;
    private Map<String, String> messages;
    private ReadNode readNode;

    public RecvMes(ReadNode readNode, DatagramSocket self, Map<Pair<Integer, String>, List<String>> neigh, Map<String, Integer> mesAnswerCount, Map<String, String> messages) {
        this.readNode = readNode;
        this.self = self;
        this.mesAnswerCount = mesAnswerCount;
        this.messages = messages;
        this.neigh = neigh;
    }
//clean byte mass
    public void recvMes() {
        int count = 0;
        byte[] mes = new byte[1024];
        DatagramPacket packet = new DatagramPacket(mes, mes.length);
        FindSafeNode findSafeNode = FindSafeNode.getInstance();

        while(true) {
            try {
                self.receive(packet);
                String dryMes = new String(packet.getData());
                String[] splitMes = dryMes.split("\n");
                if(splitMes.length != 3)
                    continue;
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
                    byte[] byteMes = ("2\n" + splitMes[1] + "\n" + splitMes[2]).getBytes();
                    DatagramPacket answer = new DatagramPacket(byteMes, byteMes.length, packet.getAddress(), packet.getPort());
                    synchronized (RecvMes.class) {
                        self.send(answer);
                    }
                    System.out.println("safe node got : " + splitMes[1] + " : "+ splitMes[2]);
                }

                //обычное сообщение
                if(Integer.parseInt(splitMes[0]) == 1) {
                    synchronized (Main.class) {
                        if(packet.getPort() == 8084)
                            System.out.println("   ");
                        if (!neigh.containsKey(pair)) {
                            neigh.put(pair, new ArrayList<>());
                            readNode.sendSafe();
                            readNode.startResend();
                            count++;
                        }
                        else {
                            if(splitMes[1].length() > 0)
                                System.out.println(splitMes[1] + " : " + splitMes[2]);
                        }
                    }

                    byte[] byteMes = ("2\n" + splitMes[1] + "\n" + splitMes[2]).getBytes();
                    DatagramPacket answer = new DatagramPacket(byteMes, byteMes.length, packet.getAddress(), packet.getPort());
                    synchronized (RecvMes.class) {
                        self.send(answer);
                    }

                     readNode.sendNeigh(splitMes[2], new Pair<Integer, String>(packet.getPort(), "127.0.0.1"));
                     readNode.startResend();
                     System.out.println("mes recved : " + pair.getKey() + ", guid: " + splitMes[1] + ", " + splitMes[2]);

                }
                //ответное сообщение
                if(Integer.parseInt(splitMes[0]) == 2) {
                    synchronized (Main.class) {
                        messages.remove(splitMes[1]);
                        List<String> el = neigh.get(pair);
                        if(el != null)
                            el.remove(splitMes[1]);
                    }
                    System.out.println("answer got: " + splitMes[1]);
                }
                //mes = new byte[1024];
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
