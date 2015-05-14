package gui;

import java.awt.Dimension;
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
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
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
			items = new String[bag.size()];
			int i = 0;
			for (Object obj : bag) {
				Item item = new Item((JSONObject) obj);
				items[i] = item.getItem().getName();
				i++;
			}
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
	
	private void getBag() throws Exception {
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
		updateBagList(response.getBag());
		
//		out.close();
//		in.close();
//		socket.close();
	}

	public Main() {
		try {
			initNetworking();
			checkGameStatus();
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
				try {
					// read the picked item
					Item picked = new Item((JSONObject) localBag.get(list
							.getSelectedIndex()));
					// send selection to server
					sendItemPicked(picked);
					// update bag
					getBag();
					// update screen
					frame.repaint();
					// token is cleaned
					token = "";
					// button is lock to avoid picking
					// button.setEnabled(false);
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});

		// General container panel to all the elements
		gui = new JPanel();
		gui.setPreferredSize(new Dimension(300, 450));
		gui.add(scroll);
		gui.add(button);

		// Final frame with all the containers
		frame = new JFrame("Zombies Demo");
		frame.setContentPane(gui);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	private Map<String, ImageIcon> createImageMap(String[] list) {
		Map<String, ImageIcon> map = new HashMap<>();
		try {
			map.put(ItemType.Knife.name().toLowerCase(), new ImageIcon(new URL(
					"http://i.stack.imgur.com/NCsHu.png")));
			map.put(ItemType.Gun.name().toLowerCase(), new ImageIcon(new URL(
					"http://i.stack.imgur.com/UvHN4.png")));
			map.put(ItemType.AidBox.name().toLowerCase(), new ImageIcon(
					new URL("http://i.stack.imgur.com/s89ON.png")));
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