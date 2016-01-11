import java.net.*;
import java.io.*;
import java.util.*;

public class EchoServerHandlerThread extends Thread {
	private Socket socket = null;
    final private String stockFile = "./stock.info";

	public EchoServerHandlerThread(Socket socket) {
		super("EchoServerHandlerThread");
		this.socket = socket;
		System.out.println("Created new Thread to handle client");
	}

    public HashMap<String, Integer> loadStocks(){
        BufferedReader freader;
        try{
            File dataFile = new File(stockFile);
            FileReader input = new FileReader(dataFile);
            freader = new BufferedReader(input);
            String line;
            HashMap<String, Integer> map = new HashMap<String, Integer>();
            while((line = freader.readLine()) != null){
                String [] stock = line.split(" ");
                map.put(stock[0], Integer.parseInt(stock[1]));
            }
            freader.close();
            return map;
        } catch(Exception k){
            return null;
        }
    }

	public void run() {
        
        HashMap<String, Integer> stockMap = loadStocks();
        System.out.println(stockMap);

		boolean gotByePacket = false;
		
		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
			EchoPacket packetFromClient;
			
			/* stream to write back to client */
			ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
			

			while (( packetFromClient = (EchoPacket) fromClient.readObject()) != null) {
				/* create a packet to send reply back to client */
				EchoPacket packetToClient = new EchoPacket();
				packetToClient.type = EchoPacket.ECHO_REPLY;
				
				/* process message */
				/* just echo in this example */
				if(packetFromClient.type == EchoPacket.ECHO_REQUEST) {
                    if (stockMap.containsKey(packetFromClient.message)){
                        packetToClient.message = stockMap.get(packetFromClient.message).toString();
                    }else{
                        packetToClient.message = "0";
                    }

					System.out.println("From Client: " + packetFromClient.message);
				
					/* send reply back to client */
					toClient.writeObject(packetToClient);
					
					/* wait for next packet */
					continue;
				}
				
				/* Sending an ECHO_NULL || ECHO_BYE means quit */
				if (packetFromClient.type == EchoPacket.ECHO_NULL || packetFromClient.type == EchoPacket.ECHO_BYE) {
					gotByePacket = true;
					packetToClient = new EchoPacket();
					packetToClient.type = EchoPacket.ECHO_BYE;
					packetToClient.message = "Bye!";
					toClient.writeObject(packetToClient);
					break;
				}
				
				/* if code comes here, there is an error in the packet */
				System.err.println("ERROR: Unknown ECHO_* packet!!");
				System.exit(-1);
			}
			
			/* cleanup when client exits */
			fromClient.close();
			toClient.close();
			socket.close();

		} catch (IOException e) {
			if(!gotByePacket)
				e.printStackTrace();
		} catch (ClassNotFoundException e) {
			if(!gotByePacket)
				e.printStackTrace();
		}
	}
}
