import javafx.util.Pair;

import java.net.*;
import java.util.*;

public class Main {
    public static void main(String[] argv) throws SocketException, UnknownHostException {
/*
        if(argv.length < 4) {
            System.out.println("enter ip and port as args");
            return;
        }
        Sting name = argv[1];
        Integer loss = Integer.parseInt(argv[2].replace("%", ""));
        Integer port = Integer.parseInt(argv[3]);

*/
        Scanner in = new Scanner(System.in);
        String ip = "127.0.0.1";
        Integer loss = Integer.valueOf(in.nextLine());
        Integer port = Integer.valueOf(in.nextLine());
        String connectToIp = in.nextLine();
        Integer connectToPort = Integer.valueOf(in.nextLine());

        /*
        if(argv.length == 6) {
            connectToIp = argv[4];
            connectToPort = Integer.parseInt(argv[5]);
        }
        */
        InetAddress adr =  InetAddress.getByName(ip);
        DatagramSocket self = new DatagramSocket(null);
        InetSocketAddress address = new InetSocketAddress(ip, port);
        self.bind(address);

        Map<Pair<Integer, String>, List<String>> neigh = new HashMap<>();
        if(!connectToIp.equals(""))
            neigh.put(new Pair<>(connectToPort, connectToIp), new ArrayList<>());

        Map<String, Integer> mesAnswerCount = new HashMap<>();
        Map<String, String> messages = new HashMap<>();

        ReadNode readNode = new ReadNode(self, neigh, messages);
        RecvMes recvMes = new RecvMes(loss, readNode, self, neigh, messages);

        Thread thread = new Thread(readNode);
        thread.start();
        recvMes.recvMes();
    }
}
