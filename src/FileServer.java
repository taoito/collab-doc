import java.io.*;
import java.util.*;
import java.net.*;

public class FileServer { //implements Runnable{

    static int versionNo;
    static String fileName;
    static File file;

    public static void main(String args[]) throws Exception{

        if (args.length != 3) {
            System.out.println("Syntax error: Include [Server Listening Port] and [Collector IP] & [Port]");
            System.exit(0);
        }

        System.out.println("======================================");
        System.out.println("Starting the File Server... ID = "+args[0]);
        System.out.println("======================================");
        fileName = "doc/doc" + args[0] + ".txt";
        file = new File(fileName);

        if (file.exists()) { 
            System.out.println("File " + fileName + " already exists");
            System.out.println("Deleting previous file, and creating new file ... ");
            file.delete();
        } 
        try{
            file.createNewFile();
            System.out.println("File replica " + fileName + " created.");
        } catch (Exception e) {
            System.out.println(e);
            System.exit(0);
        }
        versionNo = 0;

        int port = Integer.parseInt(args[0]);
        String collectorIP = args[1];
        int collectorPort = Integer.parseInt(args[2]);

        contactCollector(collectorIP,collectorPort,port);

        Socket connection;
        ServerSocket serverSocket = null; 
        serverSocket = new ServerSocket( port );

        while (true) {
            System.out.println(" File Server ID: " + port + " Listening on port: " + port);
            try {
                connection = serverSocket.accept();
                BufferedReader inFromCollector =
                    new BufferedReader(new InputStreamReader(connection.getInputStream()));
                DataOutputStream outToCollector = new DataOutputStream(connection.getOutputStream());
                String received = inFromCollector.readLine();
                //System.out.println("Received: " + received);
                String response = considerInput(received);
                //System.out.println("Sending back to client: "+ response);
                outToCollector.writeBytes(response + "\n");	
                outToCollector.close();
                inFromCollector.close();
            } catch (Exception e) {
                System.out.println("Thread cannot serve connection");
                System.exit(0);
            }
        }
    }

    // Contact collector to join the File Server lists
    public static void contactCollector(String ip, int port, int myPort) {
        try {
            Socket sendingSocket = new Socket(ip,port);
            DataOutputStream out = new DataOutputStream(sendingSocket.getOutputStream());
            BufferedReader inFromCollector = new BufferedReader(new InputStreamReader(sendingSocket.getInputStream()));

            String myIP = InetAddress.getLocalHost().getHostAddress();
            out.writeBytes("imFileServer/" + myIP + "/" + myPort + "\n");

            String result = inFromCollector.readLine();
            System.out.println("Collector Response: " + result);
            out.close();
            inFromCollector.close();
            sendingSocket.close(); 
        } catch (Exception e) {
            System.out.println("Cannot connect to the Collector");
            System.exit(0);
        }
    }

    public static String considerInput(String received) throws Exception {
        String[] tokens = received.split("/");
        String responseToCollector = "";

        if (tokens[0].equals("readDoc")) {
            responseToCollector = getDocContents(); 
        }
        else if (tokens[0].equals("writeDoc")) {
            responseToCollector = appendToDoc(tokens[1]);
        }
        else if (tokens[0].equals("getVersionNo")) {
            responseToCollector = "" + versionNo;
        }
        else if (tokens[0].equals("replaceDoc")) {
            versionNo = Integer.parseInt(tokens[1]);
            responseToCollector = replaceDoc(tokens[2]);
        }
        //System.out.println("outResponse for " + tokens[0] + ": " + responseToCollector);
        return responseToCollector;
    }

    public static String getDocContents() {
        String contents="";
        try{
            FileInputStream fstream = new FileInputStream(fileName);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            contents = br.readLine();
            //String strLine;
            //while ((strLine = br.readLine()) != null) {
            //	contents = contents + strLine + "\";
            //}			
            br.close();
            in.close();
            fstream.close();
        } catch (Exception e) {
            System.out.println(e);
            return "Cannot read from document";
        }
        System.out.println("Read File. Returning content: ["+contents+"] to the Collector");
        return contents;
    }

    public static String appendToDoc(String newText) throws Exception {
        try {
            FileWriter fstream = new FileWriter(fileName,true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(newText);
            out.close();
            fstream.close();
        } catch (Exception e) {
            System.out.println(e);
            return "Error: Can't write to document";
        }
        versionNo++;
        System.out.println("Write File. Wrote [" + newText + "] to this replica " + fileName);
        System.out.println("New version number: " + versionNo);
        return "Written. New version number: " + versionNo;
    }

    public static String replaceDoc(String newText) throws Exception {
        try {
            file.delete();
            file.createNewFile();
            FileWriter fstream = new FileWriter(fileName,true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(newText);
            out.close();
            fstream.close();
        } catch (Exception e) {
            System.out.println(e);
            return "Error: Can't replace document";
        }
        System.out.println("Replace File. Updated to newest contents [" + newText + "] at this replica " + fileName);
        System.out.println("New version number: " + versionNo);
        return "Replaced. Replica Updated to newest copy version " + versionNo;
    }
}
