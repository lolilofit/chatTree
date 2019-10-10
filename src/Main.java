import javafx.util.Pair;

import java.net.*;
import java.util.*;

public class Main {
    public static void main(String[] argv) throws SocketException, UnknownHostException {
/*
        if(argv.length < 3) {
            System.out.println("enter ip and port as args");
            return;
        }
        String ip = argv[1];
        Integer port = Integer.parseInt(argv[2]);
*/
        Scanner in = new Scanner(System.in);
        String ip = "127.0.0.1";
        Integer port = Integer.valueOf(in.nextLine());
        String connectToIp = in.nextLine();
        Integer connectToPort = Integer.valueOf(in.nextLine());

        /*
        if(argv.length == 5) {
            connectToIp = argv[3];
            connectToPort = Integer.parseInt(argv[4]);
        }
        */
        InetAddress adr =  InetAddress.getByName(ip);
        DatagramSocket self = new DatagramSocket(null);
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", port);
        self.bind(address);

        //self.setSoTimeout(15000);
        Map<Pair<Integer, String>, List<String>> neigh = new HashMap<>();
        if(!connectToIp.equals(""))
            neigh.put(new Pair<>(connectToPort, connectToIp), new ArrayList<>());

        Map<String, Integer> mesAnswerCount = new HashMap<>();
        Map<String, String> messages = new HashMap<>();

        ReadNode readNode = new ReadNode(self, neigh, messages);
        RecvMes recvMes = new RecvMes(readNode, self, neigh, mesAnswerCount, messages);

        Thread thread = new Thread(readNode);
        thread.start();
        recvMes.recvMes();
    }
}
