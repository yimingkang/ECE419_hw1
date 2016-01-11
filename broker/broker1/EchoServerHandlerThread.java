import java.net.*;
import java.io.*;
import java.util.*;

public class EchoServerHandlerThread extends Thread {
	private Socket socket = null;
    final private String stockFile = "./nasdaq";

	public EchoServerHandlerThread(Socket socket) {
		super("EchoServerHandlerThread");
		this.socket = socket;
		System.out.println("Created new Thread to handle client");
	}

    public HashMap<String, Long> loadStocks(){
        System.out.println("READING STOCK INFO");
        BufferedReader freader;
        try{
            File dataFile = new File(stockFile);
            FileReader input = new FileReader(dataFile);
            freader = new BufferedReader(input);
            String line;
            HashMap<String, Long> map = new HashMap<String, Long>();
            while((line = freader.readLine()) != null){
                System.out.println(line);
                String [] stock = line.split(" ");
                map.put(stock[0], Long.parseLong(stock[1]));
            }
            freader.close();
            System.out.println("DONE READING STOCK INFO");
            return map;
        } catch(Exception k){
            return null;
        }
    }

	public void run() {
        
        HashMap<String, Long> stockMap = loadStocks();
        System.out.println(stockMap);

		boolean gotByePacket = false;
		
		try {
			/* stream to read from client */
			ObjectInputStream fromClient = new ObjectInputStream(socket.getInputStream());
			BrokerPacket packetFromClient;
			
			/* stream to write back to client */
			ObjectOutputStream toClient = new ObjectOutputStream(socket.getOutputStream());
			

			while (( packetFromClient = (BrokerPacket) fromClient.readObject()) != null) {
				/* create a packet to send reply back to client */
				BrokerPacket packetToClient = new BrokerPacket();
				packetToClient.type = BrokerPacket.BROKER_QUOTE;
				
				/* process message */
				/* just echo in this example */
				if(packetFromClient.type == BrokerPacket.BROKER_REQUEST) {
                    if (stockMap.containsKey(packetFromClient.symbol)){
                        packetToClient.quote = stockMap.get(packetFromClient.symbol);
                    }else{
                        packetToClient.quote = 0L;
                    }

					System.out.println("From Client: " + packetFromClient.symbol);
				
					/* send reply back to client */
					toClient.writeObject(packetToClient);
					
					/* wait for next packet */
					continue;
				}
				
				/* Sending an ECHO_NULL || ECHO_BYE means quit */
				if (packetFromClient.type == BrokerPacket.BROKER_NULL || packetFromClient.type == BrokerPacket.BROKER_BYE) {
					gotByePacket = true;
					packetToClient = new BrokerPacket();
					packetToClient.type = BrokerPacket.BROKER_BYE;
					packetToClient.quote = -1L;
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
