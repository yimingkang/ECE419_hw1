import java.io.*;
import java.net.*;

public class BrokerExchange {
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {

		Socket echoSocket = null;
		ObjectOutputStream out = null;
		ObjectInputStream in = null;

		try {
			/* variables for hostname/port */
			String hostname = "localhost";
			int port = 4444;
			
			if(args.length == 2 ) {
				hostname = args[0];
				port = Integer.parseInt(args[1]);
			} else {
				System.err.println("ERROR: Invalid arguments!");
				System.exit(-1);
			}
			echoSocket = new Socket(hostname, port);

			out = new ObjectOutputStream(echoSocket.getOutputStream());
			in = new ObjectInputStream(echoSocket.getInputStream());

		} catch (UnknownHostException e) {
			System.err.println("ERROR: Don't know where to connect!!");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("ERROR: Couldn't get I/O for the connection.");
			System.exit(1);
		}

		BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
		String userInput;

		System.out.print("CONSOLE>");
		while ((userInput = stdIn.readLine()) != null
				&& userInput.toLowerCase().indexOf("bye") == -1) {
            String[] cmds = userInput.split(" ");
            if (cmds[0].equals("add") && cmds.length==2){
                /* make a new request packet */
                BrokerPacket packetToServer = new BrokerPacket();
                packetToServer.type = BrokerPacket.EXCHANGE_ADD;
                packetToServer.symbol = cmds[1];
                out.writeObject(packetToServer);

                /* print server reply */
                BrokerPacket packetFromServer;
                packetFromServer = (BrokerPacket) in.readObject();
                if (packetFromServer.type == BrokerPacket.ERROR_SYMBOL_EXISTS)
                    System.out.println(packetFromServer.symbol+" exists!");
                else
                    System.out.println(packetFromServer.symbol+" added");
            } else if (cmds[0].equals("update") && cmds.length==3){
                /* make a new request packet */
                BrokerPacket packetToServer = new BrokerPacket();
                packetToServer.type = BrokerPacket.EXCHANGE_UPDATE;
                packetToServer.symbol = cmds[1];
                packetToServer.quote = Long.parseLong(cmds[2]);
                out.writeObject(packetToServer);

                /* print server reply */
                BrokerPacket packetFromServer;
                packetFromServer = (BrokerPacket) in.readObject();
                if (packetFromServer.type == BrokerPacket.ERROR_OUT_OF_RANGE)
                    System.out.println("update "+packetFromServer.symbol+" out of range!");
                else if (packetFromServer.type == BrokerPacket.ERROR_INVALID_SYMBOL)
                    System.out.println(packetFromServer.symbol+" invalid.");
                else
                    System.out.println(packetFromServer.symbol+" update to "+packetFromServer.quote);
            } else if (cmds[0].equals("remove") && cmds.length==2){
                /* make a new request packet */
                BrokerPacket packetToServer = new BrokerPacket();
                packetToServer.type = BrokerPacket.EXCHANGE_REMOVE;
                packetToServer.symbol = cmds[1];
                out.writeObject(packetToServer);

                /* print server reply */
                BrokerPacket packetFromServer;
                packetFromServer = (BrokerPacket) in.readObject();
                if (packetFromServer.type == BrokerPacket.ERROR_INVALID_SYMBOL)
                    System.out.println(packetFromServer.symbol+" invalid.");
                else
                    System.out.println("remove "+packetFromServer.symbol);
            } else if (userInput.equals("x")){
                System.out.println("BYE!");
                BrokerPacket packetToServer = new BrokerPacket();
                packetToServer.type = BrokerPacket.BROKER_NULL;
                packetToServer.symbol = "x";
                out.writeObject(packetToServer);
                echoSocket.close();
                return;
            }

			/* re-print console prompt */
			System.out.print("CONSOLE>");
		}

		/* tell server that i'm quitting */
		BrokerPacket packetToServer = new BrokerPacket();
		packetToServer.type = BrokerPacket.BROKER_BYE;
		packetToServer.symbol = "Bye!";
		out.writeObject(packetToServer);

		out.close();
		in.close();
		stdIn.close();
		echoSocket.close();
	}
}
