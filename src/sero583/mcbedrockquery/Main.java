package sero583.mcbedrockquery;

import java.util.Random;

public class Main {
	private static String[] DEFAULT_SERVERS = {"24sucht.net:19132"};//"play.lbsg.net:19132", "fallentech.tk:19132"
	private static int DEFAULT_PORT = 19132;
	
	public static void main(String[] args) {
		String server = null;
		Integer port = null;
		
		if(args.length<=0) {
			Random rand = new Random();
			int index = rand.nextInt(DEFAULT_SERVERS.length);
			String data = DEFAULT_SERVERS[index];
			String[] split = data.split(":");
			server = data.split(":")[0];
			port = split.length > 1 ? Integer.parseInt(split[1]) : DEFAULT_PORT;
		} else {
			server = args[0];
			port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;
		}
		
		System.out.println("Querying server (" + server + ":" + port + ")...");
		new QueryRequest(server, port).startQuery();
	}
}
