package sero583.mcbedrockquery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class QueryRequest implements Runnable {
	public static final int DEFAULT_PORT = 19132;
	
	private boolean running = false;
	
	public void setRunning() {
		this.setRunning(true);
	}
	
	public void setRunning(boolean state) {
		this.running = state;
	}
	
	public boolean isRunning() {
		return this.running;
	}
	
	private String serverIP;
	private int port;
	private String result;
	
	public QueryRequest(String serverIP) {
		this(serverIP, DEFAULT_PORT);
	}
	
	public QueryRequest(String serverIP, int port) {
		this.serverIP = serverIP;
		this.port = port;
	}
	
	
	public String getServerIP() {
		return serverIP;
	}
	
	public void setServerIP(String serverIP) {
		this.serverIP = serverIP;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	private Thread thread;
	
	public void startQuery() {
		System.out.println("Starting query...");
		if(this.thread==null) {
			System.out.println("Creating thread...");
			this.thread = new Thread(this);
			System.out.println("Created thread!");
			this.setRunning();
			this.thread.start();
			System.out.println("Set thread to running and started execution...");
		} else {
			this.thread.interrupt();
			this.thread.start();
		}
	}

	private boolean success = false;
	private static final byte[] QUERY_COMMAND = {(byte) 0xFE, (byte) 0xFD, (byte) 0x09, (byte) 0x10, (byte) 0x20, (byte) 0x30, (byte) 0x40, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0x01};
	private static final int BUFFER_SIZE_RECEIVE = 10240;
	private static final int TIMEOUT = 3500;
	private DatagramSocket udpSocket;
	private InetAddress serverAddress;
	
	private QueryResult qRes = new QueryResult();
	
	@Override
	public void run() {
		while(this.isRunning()&&this.success==false) {
			try {
				System.out.println("Initializing connection to destination...");
				InetSocketAddress local = new InetSocketAddress(this.serverAddress, this.port);
				
				if(this.serverAddress==null) {
					this.serverAddress = InetAddress.getByName(this.serverIP);
				}
				if(this.udpSocket==null) {
					this.udpSocket = new DatagramSocket();
					//this.udpSocket.setSoTimeout(5*1000);
					this.udpSocket.connect(this.serverAddress, this.port);
				}
				if(this.udpSocket.isConnected()==true) {
					System.out.println("Connected to server!");
				} else {
					System.out.println("Server is offline!");
				}
				System.out.println("Initialized connection to destination!");
				

				System.out.println("Setting up components...");
				System.out.println("Components setup!");
				final byte[] receiveData = new byte[BUFFER_SIZE_RECEIVE];
				this.udpSocket.setSoTimeout(TIMEOUT);
				sendPacket(this.udpSocket, local, QUERY_COMMAND);

				final int challengeInteger; 
				{
					receivePacket(this.udpSocket, receiveData);
					byte byte1 = -1;
					int i = 0;
					byte[] buffer = new byte[11];
					for(int count = 5; (byte1 = receiveData[count++]) != 0;)
						buffer[i++] = byte1;
					challengeInteger = Integer.parseInt(new String(buffer).trim());
				}
				sendPacket(this.udpSocket, local, 0xFE, 0xFD, 0x00, 0x01, 0x01, 0x01, 0x01, challengeInteger >> 24, challengeInteger >> 16, challengeInteger >> 8, challengeInteger, 0x00, 0x00, 0x00, 0x00);

				

				final int length = receivePacket(this.udpSocket, receiveData).getLength();
				Map<String, String> values = new HashMap<String, String>();
				final AtomicInteger cursor = new AtomicInteger(5);
				while(cursor.get() < length) {
					final String s = readString(receiveData, cursor);
					if(s.length() == 0)
						break;
					else {
						final String v = readString(receiveData, cursor);
						values.put(s, v);
					}
				}

				readString(receiveData, cursor);
				final Set<String> players = new HashSet<String>();

				while(cursor.get() < length) {
					final String name = readString(receiveData, cursor);
					if(name.length() > 0)
						players.add(name);
				}

				String[] onlineUsernames = players.toArray(new String[players.size()]);
				this.udpSocket.close();
				this.success = true;
				System.out.println("Collected data, closed connection!");
				System.out.println("Disabling thread, dumping result data and filling query result...");
				
				this.setRunning(false);
				Thread.currentThread().interrupt();
				
				for(Map.Entry<String, String> entry : values.entrySet()) {
					System.out.println(entry.getKey() + ":" + entry.getValue());
					this.qRes.addData(entry.getKey(), entry.getValue());
				}
				
				for(int i = 0; i < onlineUsernames.length; i++) {
					System.out.println("Player " + (i+1) + ":" + onlineUsernames[i]);
					this.qRes.addData("player" + i, onlineUsernames[i]);
				}
				
			} catch(Exception e) {
				//be quiet
			}
		}
	}
	
	private final static void sendPacket(DatagramSocket socket, InetSocketAddress targetAddress, byte... data) throws IOException {
		DatagramPacket sendPacket = new DatagramPacket(data, data.length, targetAddress.getAddress(), targetAddress.getPort());
		socket.send(sendPacket);
	}
	
	private final static void sendPacket(DatagramSocket socket, InetSocketAddress targetAddress, int... data) throws IOException {
		final byte[] d = new byte[data.length];
		int i = 0;
		for(int j : data)
			d[i++] = (byte)(j & 0xff);
		sendPacket(socket, targetAddress, d);

	}

	private final static DatagramPacket receivePacket(DatagramSocket socket, byte[] buffer) throws IOException {
		final DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
		socket.receive(dp);
		return dp;
	}

	private final static String readString(byte[] array, AtomicInteger cursor) {
		final int startPosition = cursor.incrementAndGet();
		for(; cursor.get() < array.length && array[cursor.get()] != 0; cursor.incrementAndGet());
		return new String(Arrays.copyOfRange(array, startPosition, cursor.get()));
	}
	
	public QueryResult getResult() {
		return this.qRes;
	}

	@Override
	public String toString() {
		return "QueryRequest(IP=" + this.serverIP + ", Port=" + this.port + ")";
	}
}
