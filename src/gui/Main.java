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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
import messaging.ResultsRequest;
import messaging.ResultsResponse;
import messaging.StartGameRequest;
import messaging.StartGameResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.kohsuke.args4j.CmdLineParser;

import common.Item;
import common.ItemType;

public class Main {
	// GUI Variables
	private JList list;
	private JScrollPane scroll;
	private JButton button;
	private JPanel gui;
	private JFrame frame;
	private JLabel label;
	private ImageIcon image;
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

	private void printDebugLines(String message) {
		if (this.debug) {
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
			// if the local copy is already with elements...
			if (localBag != null && localBag.size() > 0)
				localBag.clear();
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
			items = temp.toArray(items);
			imageMap = createImageMap(items);
		}
	}

	private void checkGameStatus() throws Exception {
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
	}

	private void sendItemPicked(Item item) throws Exception {
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
	}
	
	private void refreshBag() throws Exception {
		BagRequest request = new BagRequest();
		out.println(request.ToJSON());
		printDebugLines(request.ToJSON());

		msg = in.readLine();
		printDebugLines(msg);
		BagResponse response = new BagResponse();
		response.FromJSON(msg);
		// no item removed
		updateBagList(response.getBag(), -1);
	}
	
	private void sendEndGame() throws Exception {
		ResultsRequest request = new ResultsRequest();
		out.println(request.ToJSON());
		printDebugLines(request.ToJSON());
		
		msg = in.readLine();
		printDebugLines(msg);
		ResultsResponse response = new ResultsResponse();
		response.FromJSON(msg);
		
		// TODO: parse and show with fancy graphics
		showMessage(msg);
	}

	private void getUpdatedBag(int removed) throws Exception {
		BagRequest request = new BagRequest();
		out.println(request.ToJSON());
		printDebugLines(request.ToJSON());

		msg = in.readLine();
		printDebugLines(msg);
		BagResponse response = new BagResponse();
		response.FromJSON(msg);
		updateBagList(response.getBag(), removed);
	}

	// Opens a generic Alert with a message inside...
	private void showMessage(String message) {
		JOptionPane.showMessageDialog(frame, message);
	}

	// The thread will be blocked until some message will be received
	private boolean initTokenRing(String host, int port) throws Exception {
		client = new TokenClient(host, port);
		// by default, all the guis start requesting the token
		client.sendRequestCS();
		// start communication thread
		Thread tTokenRing = new Thread(client);
		tTokenRing.start();

		// main thread will be locked...
		while ((hasToken = client.getHasToken()) != true) {
			Thread.sleep(1000);
		}

		return hasToken;
	}

	// The thread will be blocked again until the token will be granted
	private boolean requestToken() throws Exception {
		client.sendRequestCS();

		// main thread will be locked...
		while ((hasToken = client.getHasToken()) != true) {
			Thread.sleep(1000);
		}

		return hasToken;
	}

	// The thread will be blocked again until the token will be released
	private boolean releaseToken() throws Exception {
		client.sendReleaseCS();

		// main thread will be locked...
		while ((hasToken = client.getHasToken()) != false) {
			Thread.sleep(1000);
		}

		return hasToken;
	}

	// This method changes the visibility of all the elements
	private void changeGUIStatus(boolean isActive) {
		// The order matters...
		scroll.setVisible(isActive);
		button.setEnabled(isActive);
		button.setVisible(isActive);
		label.setVisible(!isActive);

		// update screen...
		if (frame != null)
			frame.repaint();
	}
	
	private void exitGame() {
		System.exit(-1);
	}

	public Main(ArgumentParser args) {
		try {
			// locking instructions!
			if (initTokenRing(args.getTokenHostname(), args.getTokenPort())) {
				initNetworking();
				checkGameStatus();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(-1);
		}

		// Loading image
		image = new ImageIcon(Main.class.getResource("img/loading.gif"));
		label = new JLabel(image);
		image.setImageObserver(label);

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
				if (list.getSelectedIndex() > -1) {
					try {
						// read the picked item
						Item picked = new Item((JSONObject) localBag.get(list
								.getSelectedIndex()));
						// send selection to server
						sendItemPicked(picked);
						// update local bag
						getUpdatedBag(list.getSelectedIndex());
						frame.repaint();
						// checking game state according to bag size...
						if (localBag.size() > 0) {
							// resolve token ring requests
							if (releaseToken()) {
								changeGUIStatus(false);
							}
							if (requestToken()) {
								// refresh bag...
								refreshBag();
								changeGUIStatus(true);
								if (localBag.size() < 1) {
									// TODO: implement something fancy here!!
									sendEndGame();
									exitGame();
								}
							}
						} else {
							// TODO: implement something fancy here!!
							sendEndGame();
							exitGame();
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
		gui.add(label);

		// Set the GUI components to be ready
		changeGUIStatus(true);

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
					map.put(ItemType.AidBox.name().toLowerCase(),
							new ImageIcon(Main.class
									.getResource("img/aidbox.gif")));
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return map;
	}

	public static void main(String[] args) {
		ArgumentParser bean = new ArgumentParser();
		try {
			bean.Parse(args);
		} catch(Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new Main(bean);
			}
		});
	}
}