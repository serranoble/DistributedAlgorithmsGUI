package gui;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class TokenClient implements Runnable {
	
	// Connection parameters
	private String host;
	private int port;
	
	// Networking variables
	private Socket socket;
	private InputStream inStream;
	private OutputStream outStream;
	private PrintWriter out;
	private BufferedReader in;
	
	// Connectivity stuff...
	private String message;
	private boolean keepConnection;
	private boolean hasToken;
	
	public TokenClient(String tHost, int tPort) {
		host = tHost;
		port = tPort;
		message = "NOMSG";
		keepConnection = true;
		hasToken = false;
	}
	
	public void sendRequestCS() {
		message = "requestCS";
	}
	
	public void sendReleaseCS() {
		message = "releaseCS";
	}
	
	public void closeConnection() {
		keepConnection = false;
	}
	
	public boolean getHasToken() {
		return hasToken;
	}

	@Override
	public void run() {
		try {
			// start the networking stuff...
			socket = new Socket(host, port);
			inStream = socket.getInputStream();
			outStream = socket.getOutputStream();
			out = new PrintWriter(outStream, true);
			in = new BufferedReader(new InputStreamReader(inStream));
			
			while(keepConnection) {
				switch(message) {
					case "requestCS":
					case "releaseCS":
						// message is sent to token ring
						out.println(message);
						// handle the answer properly...
						String answer = in.readLine();
						System.out.println("Token Ring answer = " + answer);
						if (answer.equals("cs"))
							hasToken = true;
						else
							hasToken = false;
						break;
					default:
						// the message doesn't make sense... do nothing!
						continue;
				}
				// message is wiped to avoid repetitions
				message = "NOMSG";
			}
			
			// closing things... (redundant because jvm does it!)
			if (!socket.isClosed()) {
				in.close();
				out.close();
				inStream.close();
				outStream.close();
				socket.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
