package gui;

import java.awt.Dimension;
import java.awt.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import messaging.BagRequest;
import messaging.BagResponse;
import messaging.CheckAvailabilityRequest;
import messaging.CheckAvailabilityResponse;
import messaging.PlayRequest;
import messaging.PlayResponse;
import messaging.StartGameRequest;
import messaging.StartGameResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import common.Item;
import common.ItemType;

public class Main {
	// GUI Variables
	private JList list;
	private JScrollPane scroll;
	private JButton button;
	private JPanel gui;
	private JFrame frame;
	private boolean debug = true;

	// Functionality
	private String msg;
	private Socket socket;
	private InputStream inStream;
	private OutputStream outStream;
	private PrintWriter out;
	private BufferedReader in;

	// Networking parameters
	private final String host = "localhost";
	private final int port = 6262;
	
	// Game variables
	private int numPlayers = 1;
	private/* final */Map<String, ImageIcon> imageMap;
	private String token;
	private String[] items;
	private JSONArray localBag;
	
	// Token Ring parameters
	private TokenClient client;
	private String tHost = "localhost";
	private int tPort = 2250;
	private boolean hasToken = false;
	
	private void printDebugLines(String message){
		if (this.debug){
			System.out.println(message);
		}		
	}
	
	private void initNetworking() throws Exception {
		socket = new Socket(host, port);
		inStream = socket.getInputStream();
		outStream = socket.getOutputStream();
		out = new PrintWriter(outStream, true);
		in = new BufferedReader(new InputStreamReader(inStream));
	}

	private boolean isAvailable(PrintWriter out, BufferedReader in)
			throws Exception {
		// check server availability
		CheckAvailabilityRequest request = new CheckAvailabilityRequest();
		out.println(request.ToJSON());
		printDebugLines(request.ToJSON());

		// get response and proceed
		msg = in.readLine();
		printDebugLines(msg);
		CheckAvailabilityResponse result = new CheckAvailabilityResponse();
		result.FromJSON(msg);

		return result.getBusy();
	}
	
	private void updateBagList(JSONArray bag) {
		if (bag != null) {
			// clone it to keep a local copy
			localBag = (JSONArray) bag.clone();
			// parse it to create the list on screen
			ArrayList<String> temp = new ArrayList<String>();
			for (Object obj : bag) {
				Item item = new Item((JSONObject) obj);
				if (item.getAmount() > 0) {
					temp.add(item.getItem().getName());
				}
			}
			items = new String[temp.size()];
			temp.toArray(items);
			imageMap = createImageMap(items);
		}
	}
	
	private void updateBagList(JSONArray bag, int removed) {
		if (bag != null) {
			// clone it to keep a local copy
			localBag = (JSONArray) bag.clone();
			// parse it to create the list on screen
			ArrayList<String> temp = new ArrayList<String>();
			int i = 0;
			for (Object obj : bag) {
				if (i++ == removed)
					continue;
				Item item = new Item((JSONObject) obj);
				if (item.getAmount() > 0) {
					temp.add(item.getItem().getName());
				}
			}
//			items = new String[temp.size()];
			items = temp.toArray(items);
			imageMap = createImageMap(items);
		}
	}

	private void checkGameStatus() throws Exception {
//		socket = new Socket(host, port);
//		inStream = socket.getInputStream();
//		outStream = socket.getOutputStream();
//		out = new PrintWriter(outStream, true);
//		in = new BufferedReader(new InputStreamReader(inStream));

		// Check if there is any game running
		if (!isAvailable(out, in)) {
			// Create a new game
			StartGameRequest request = new StartGameRequest(numPlayers);
			out.println(request.ToJSON());
			printDebugLines(request.ToJSON());

			msg = in.readLine();
			printDebugLines(msg);
			StartGameResponse response = new StartGameResponse();
			response.FromJSON(msg);

			token = response.getToken();
			updateBagList(response.getBag());
		}

//		out.close();
//		in.close();
//		socket.close();
	}

	private void sendItemPicked(Item item) throws Exception {
		if (token.equals(""))
			throw new TokenException();

//		socket = new Socket(host, port);
//		inStream = socket.getInputStream();
//		outStream = socket.getOutputStream();
//		out = new PrintWriter(outStream, true);
//		in = new BufferedReader(new InputStreamReader(inStream));

		PlayRequest request = new PlayRequest(token, InetAddress.getLocalHost()
				.toString());
		// create the JSONArray with the selected item
		JSONArray array = new JSONArray();
		// convert the item to json object
		JSONObject json = new JSONObject();
		json.put("item", item.getItem().getName());
		json.put("amount", 1); // it's always 1!
		// add it to the request
	    array.add(json);
		request.setItems(array);
		
		out.println(request.ToJSON());
		printDebugLines(request.ToJSON());
		
		msg = in.readLine();
		printDebugLines(msg);
		PlayResponse response = new PlayResponse();
		response.FromJSON(msg);

//		out.close();
//		in.close();
//		socket.close();
	}
	
	private void getUpdatedBag(int removed) throws Exception {
		if (token.equals(""))
			throw new TokenException();
		
//		socket = new Socket(host, port);
//		inStream = socket.getInputStream();
//		outStream = socket.getOutputStream();
//		out = new PrintWriter(outStream, true);
//		in = new BufferedReader(new InputStreamReader(inStream));
		
		BagRequest request = new BagRequest();
		out.println(request.ToJSON());
		printDebugLines(request.ToJSON());
		
		msg = in.readLine();
		printDebugLines(msg);
		BagResponse response = new BagResponse();
		response.FromJSON(msg);
		updateBagList(response.getBag(), removed);
		
//		out.close();
//		in.close();
//		socket.close();
	}
	
	private void showMessage(String message) {
		JOptionPane.showMessageDialog(frame, message);
	}
	
	// The thread will be blocked until some message will be received
	private boolean initTokenRing() throws Exception {
		client = new TokenClient(tHost, tPort);
		// by default, all the guis start requesting the token
		client.sendRequestCS();
		// start communication thread
		Thread tTokenRing = new Thread(client);
		tTokenRing.start();
		
		// main thread will be locked...
		while((hasToken = client.getHasToken()) != true) {
			Thread.sleep(1000);
		}
		
		return hasToken;
	}
	
	// The thread will be blocked again until the token will be granted
	private boolean requestToken() throws Exception {
		client.sendRequestCS();
		
		// main thread will be locked...
		while((hasToken = client.getHasToken()) != true) {
			Thread.sleep(1000);
		}
		
		return hasToken;
	}
		
	// The thread will be blocked again until the token will be released
	private boolean releaseToken() throws Exception {
		client.sendReleaseCS();
		
		// main thread will be locked...
		while((hasToken = client.getHasToken()) != false) {
			Thread.sleep(1000);
		}
		
		return hasToken;
	}

	public Main() {
		try {
			// locking instructions!
			if (initTokenRing()) {
				initNetworking();
				checkGameStatus();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(-1);
		}

		// Image list
		list = new JList(items);
		list.setCellRenderer(new ImagesRenderer(imageMap));
		scroll = new JScrollPane(list);
		scroll.setPreferredSize(new Dimension(300, 400));

		// "Take and pass" button
		button = new JButton("Take and Pass");
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (list.getSelectedIndex() > 0) {
					try {
						// read the picked item
						Item picked = new Item((JSONObject) localBag.get(list
								.getSelectedIndex()));
						// send selection to server
						sendItemPicked(picked);
						// update local bag
						getUpdatedBag(list.getSelectedIndex());
						// update screen
						frame.repaint();
						// token is cleaned
						token = "";
						// checking game state according to bag size...
						if (localBag.size() > 1) {
							// resolve token ring requests
							if (releaseToken()) {
								// button is lock to avoid picking
								button.setEnabled(false);
							}
							//TODO: do this better!
							if (requestToken()) {
								// refresh bag...
								button.setEnabled(true);
							}
						} else {
							//TODO: implement something to show the results
							showMessage("Game Over!");
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				} else {
					showMessage("Pick an item first!");
				}
			}
		});

		// General container panel to all the elements
		gui = new JPanel();
		gui.setPreferredSize(new Dimension(300, 450));
		gui.add(scroll);
		gui.add(button);

		// Final frame with all the containers
		frame = new JFrame("Zombies from The Andes (Demo)");
		frame.setContentPane(gui);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private Map<String, ImageIcon> createImageMap(String[] list) {
		Map<String, ImageIcon> map = new HashMap<>();
		try {
			for (String name : list) {
				if (name == null)
					continue;
				if (name.equalsIgnoreCase(ItemType.Knife.name())) {
					map.put(ItemType.Knife.name().toLowerCase(), new ImageIcon(
							Main.class.getResource("img/knife.jpg")));
				}
				if (name.equalsIgnoreCase(ItemType.Gun.name())) {
					map.put(ItemType.Gun.name().toLowerCase(), new ImageIcon(
							Main.class.getResource("img/gun.png")));
				}
				if (name.equalsIgnoreCase(ItemType.AidBox.name())) {
					map.put(ItemType.AidBox.name().toLowerCase(), new ImageIcon(
							Main.class.getResource("img/aidbox.gif")));
				}
			}
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return map;
	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new Main();
			}
		});
	}
}