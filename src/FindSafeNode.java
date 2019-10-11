import javafx.util.Pair;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class FindSafeNode {
    private Map<Pair<Integer, String>, Pair<Integer, String>> recvedSafe;
    private static volatile FindSafeNode instance;

    private FindSafeNode() {
        recvedSafe = new HashMap<>();
    }
    public static FindSafeNode getInstance() {
        if(instance == null) {
            synchronized (FindSafeNode.class) {
                if(instance == null)
                    instance = new FindSafeNode();
            }
        }
        return instance;
    }

    public void checkSafery(Pair<Integer, String> poisonedSafe) {
        synchronized (FindSafeNode.class) {
            List<Pair<Integer, String>> keys = new ArrayList<>(recvedSafe.keySet());
            for(int i = 0; i < keys.size(); i++) {
                if(recvedSafe.get(keys.get(i)).equals(poisonedSafe))
                    recvedSafe.remove(keys.get(i));
            }
        }
    }

    public void removeSafeNode(Pair<Integer, String> key) {
        recvedSafe.remove(key);
    }

    public Pair<Integer, String> findSafeNode(Pair<Integer, String> key, Set<Pair<Integer, String>> neigh) {
        if(neigh.size() == 0)
            return null;

        List<Pair<Integer, String>> neighList = new ArrayList<>(neigh);
        Pair<Integer, String> value = null;

        synchronized (FindSafeNode.class) {
            value = neighList.get(0);
            for(int i = 1; i < neigh.size(); i++) {
                  if(neighList.get(i).getKey() < value.getKey())
                      value = neighList.get(i);
            }
        }
        //put new safe node with self
        if(value != null) {
            synchronized (FindSafeNode.class) {
                recvedSafe.put(key, value);
            }
        }
        return value;
    }

    public Pair<Integer, String> getSafeNode(Pair<Integer, String> sender) {
        return recvedSafe.get(sender);
    }

    public void registerSafeNode(Pair<Integer, String> sender, Pair<Integer, String> safeNode) {
        synchronized (FindSafeNode.class) {
            recvedSafe.put(sender, safeNode);
        }
    }

    //check if we don't have safe node
    public int sendSafeNode(DatagramSocket self, Pair<Integer, String> sender, Pair<Integer, String> dest, String guid) {
        Pair<Integer, String> safeOne = recvedSafe.get(sender);
        if(safeOne == null)
            return 1;
        if(safeOne.equals(dest))
            return 1;
        byte[] byteMes = ("0\n" + guid + "\n" + safeOne.getKey() + ":" + safeOne.getValue() + "\n").getBytes();
        try {
            InetAddress adr = InetAddress.getByName(dest.getValue());
            DatagramPacket packet = new DatagramPacket(byteMes, byteMes.length, adr, dest.getKey());
            synchronized (RecvMes.class) {
                self.send(packet);
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return 1;
        } catch (IOException e) {
            e.printStackTrace();
            return 1;
        }
        return 0;
    }
}
