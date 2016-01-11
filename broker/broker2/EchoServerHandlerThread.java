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
            return map;
        } catch(Exception k){
            return null;
        }
    }

    public void saveStocks(HashMap<String, Long> map){
        BufferedWriter fwriter;
        try{
            File dataFile = new File(stockFile);
            FileWriter input = new FileWriter(dataFile, false);
            fwriter = new BufferedWriter(input);
            String line;
            for (String key : map.keySet()){
                fwriter.write(key+" "+map.get(key).toString());
                fwriter.newLine();
            }
            fwriter.close();
            return;
        } catch(Exception k){
            return;
        }
    }

	public void run() {
        HashMap<String, Long> stockMap = loadStocks();
        System.out.println("Initial stock:");
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
				if(packetFromClient.type == BrokerPacket.EXCHANGE_ADD) {
                    if (stockMap.containsKey(packetFromClient.symbol)){
                        System.out.println("ADD symbol already exists: " + packetFromClient.symbol);
                        packetToClient.type = BrokerPacket.ERROR_SYMBOL_EXISTS;
                        packetToClient.symbol = packetFromClient.symbol;
                    }else{
                        System.out.println("ADD " + packetFromClient.symbol);
                        packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
                        packetToClient.symbol = packetFromClient.symbol;
                        packetToClient.quote = 0L;
                        stockMap.put(packetToClient.symbol, packetToClient.quote);
                        saveStocks(stockMap);
                        stockMap=loadStocks();
                    }
					/* send reply back to client */
					toClient.writeObject(packetToClient);
					continue;
                } else if(packetFromClient.type == BrokerPacket.EXCHANGE_UPDATE) {
                    long quote = packetFromClient.quote;
                    if (!stockMap.containsKey(packetFromClient.symbol)){
                        System.out.println("UPDATE gets invalid symbol: " + packetFromClient.symbol);
                        packetToClient.type = BrokerPacket.ERROR_INVALID_SYMBOL;
                        packetToClient.symbol = packetFromClient.symbol;
                    }else if (quote < 1 || quote >300){
                        System.out.println("UPDATE gets invalid quote: " + packetFromClient.quote);
                        packetToClient.type = BrokerPacket.ERROR_OUT_OF_RANGE;
                        packetToClient.symbol = packetFromClient.symbol;
                    }else{
                        System.out.println("UPDATE " + packetFromClient.symbol + " to " + packetFromClient.quote);
                        packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
                        packetToClient.symbol = packetFromClient.symbol;
                        packetToClient.quote = packetFromClient.quote;
                        stockMap.put(packetToClient.symbol, packetToClient.quote);
                        saveStocks(stockMap);
                        stockMap=loadStocks();
                    }
					/* send reply back to client */
					toClient.writeObject(packetToClient);
					continue;
                } else if(packetFromClient.type == BrokerPacket.EXCHANGE_REMOVE) {
                    if (!stockMap.containsKey(packetFromClient.symbol)){
                        System.out.println("REMOVE gets invalid symbol: " + packetFromClient.symbol);
                        packetToClient.type = BrokerPacket.ERROR_INVALID_SYMBOL;
                        packetToClient.symbol = packetFromClient.symbol;
                    }else{
                        System.out.println("REMOVE " + packetFromClient.symbol);
                        packetToClient.type = BrokerPacket.EXCHANGE_REPLY;
                        packetToClient.symbol = packetFromClient.symbol;
                        stockMap.remove(packetToClient.symbol);
                        saveStocks(stockMap);
                        stockMap=loadStocks();
                    }
					/* send reply back to client */
					toClient.writeObject(packetToClient);
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
				System.err.println("ERROR: Unknown packet!!");
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
