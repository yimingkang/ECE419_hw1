import java.io.*;
import java.net.*;

public class BrokerClient {
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
            if (userInput.equals("x")){
                System.out.println("BYE!");
                BrokerPacket packetToServer = new BrokerPacket();
                packetToServer.type = BrokerPacket.BROKER_NULL;
                packetToServer.symbol = "x";
                out.writeObject(packetToServer);
                echoSocket.close();
                return;
            }
			/* make a new request packet */
			BrokerPacket packetToServer = new BrokerPacket();
			packetToServer.type = BrokerPacket.BROKER_REQUEST;
			packetToServer.symbol = userInput;
			out.writeObject(packetToServer);

			/* print server reply */
			BrokerPacket packetFromServer;
			packetFromServer = (BrokerPacket) in.readObject();

			if (packetFromServer.type == BrokerPacket.BROKER_QUOTE)
				System.out.println("echo: " + packetFromServer.quote);

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
