import java.util.*;
import java.io.*;
import java.net.*;

public class Client{
	public static void writeDoc(String newText, String ip, int port) throws Exception {
		try {
		Socket sendingSocket = new Socket(ip,port);
		DataOutputStream out = new DataOutputStream(sendingSocket.getOutputStream());
		BufferedReader inFromCollector = new BufferedReader(new InputStreamReader(sendingSocket.getInputStream()));

		out.writeBytes("writeDoc/" + newText + "\n");

		String result = inFromCollector.readLine();
		System.out.println(result);
		out.close();
		inFromCollector.close();
		sendingSocket.close(); 
		} catch (Exception e) {
			System.out.println("Cannot connect to the Collector");
		}
	}

	public static void readDoc(String ip, int port) throws Exception {
		try {
		Socket sendingSocket = new Socket(ip,port);
		DataOutputStream out = new DataOutputStream(sendingSocket.getOutputStream());
		BufferedReader inFromCollector = new BufferedReader(new InputStreamReader(sendingSocket.getInputStream()));

		out.writeBytes("readDoc\n");

		String result = inFromCollector.readLine();
		System.out.println("");
		System.out.println("Current document contents: " + result);
		out.close();
		inFromCollector.close();
		sendingSocket.close(); 
		} catch (Exception e) {
			System.out.println("Cannot connect to the Collector");
		}
	}

	public static void main(String args[]) throws Exception {
		if (args.length != 2) {
			System.out.println("Syntax error: Include Collector IP Address and Port");
			System.exit(0);
		}
		DataInputStream din = new DataInputStream (System.in);
		String collectorIP = args[0];
		int collectorPort = Integer.parseInt(args[1]);

		while (true) {
			System.out.println("");
			System.out.println("Choose options:");
			System.out.println("1 - Write new text to the document");
			System.out.println("2 - Read the document");
			System.out.println("3 - Exit");
			System.out.print("Choice : ");
			String line = din.readLine();

			if (line.equals("1")) {
				System.out.println("Enter new text to append: ");
				String newText = din.readLine();
				writeDoc(newText,collectorIP,collectorPort);
			}
			else if (line.equals("2")) {
				readDoc(collectorIP,collectorPort);
			}
			else if (line.equals("3")) {
				System.exit(0);
			}
			else {
				System.out.println("Invalid option");
				System.out.println("");
			}
		}
	}
}
