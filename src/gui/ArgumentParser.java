package gui;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class ArgumentParser {
	
	@Option(name = "-players", usage = "Sets number of players")
	private int players;

	@Option(name = "-port", usage = "Sets server port")
	private int serverPort;

	@Option(name = "-server", usage = "Sets remote IP to connect to")
	private String server = null;

	@Option(name = "-tokenport", usage = "Sets client port")
	private int tokenPort;

	@Option(name = "-tokenserver", usage = "Sets remote IP to connect to")
	private String tokenServer = null;
	
	@Option(name = "-debug", usage = "Sets if is in debugging mode or not")
	private String debug;

	public void Parse(String[] args) {
		CmdLineParser parser = new CmdLineParser(this);

		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("--------Invalid arguments---------");
			System.exit(1);
		}
		
		if (!(debug.toLowerCase().equals("true") || debug.toLowerCase().equals("false"))){
			debug = "false";
		}

	}

	public ArgumentParser() {
		serverPort = 0;
		tokenPort = 0;
		server = "";
		tokenServer = "";
		debug = "false";
		players = 0;
	}

	public void setPlayers(int players) {
		this.players = players;
	}

	public int getPlayers() {
		return players;
	}
	
	public int getServerPort() {
		return serverPort;
	}

	public int getTokenPort() {
		return tokenPort;
	}
	
	public String getHostname() {
		return server;
	}
	
	public String getTokenHostname() {
		return tokenServer;
	}
	
	public boolean isDebugging(){
		return this.debug.toLowerCase().equals("true") ? true : false;
	}
}
