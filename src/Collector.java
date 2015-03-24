import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;  

public class Collector implements Runnable{
    static List<Request> requestQueue = new ArrayList<Request>(); 
    static List<String> fileServerList = new ArrayList<String>();

    private Socket connection = null;
    static ServerSocket serverSocket = null; 
    private int ID;

    private	static int NR = 0;
    private	static int NW = 0;	

    //Loggers
    private static		Logger 		logger 		= null;
    private static		FileHandler 	fh		= null;

    //time measurement
    static long start,end;
    double runningTime;
    //for performance testing
    static int numReq=0; 
    static int numMsg=0;

    public Collector(Socket newConnection, int id){
        this.connection = newConnection;
        this.ID = id;
    }

    public static void main(String args[]) throws Exception{

        if (args.length != 1) {
            System.out.println("Syntax error: Include collector Listening Port");
            System.exit(0);
        }	

        System.out.println("===========================");
        System.out.println("Starting the Collector ...");
        System.out.println("===========================");

        readConfigFile();
        System.out.println("Current configuration: NR " + NR + " NW " + NW);

        logger = Logger.getLogger("CollectorLog");
        fh = new FileHandler("bin/temp.log");	
        logger.addHandler(fh);  
        SimpleFormatter formatter = new SimpleFormatter();  
        fh.setFormatter(formatter);
        logger.info("Starting the log");

        int port = Integer.parseInt(args[0]);

        int count = 0;
        Socket temp = null;
        Runnable runnable = new Collector(temp,0);
        Thread thread = new Thread(runnable);
        thread.start(); // Start main thread to process requestQueue

        try {
            serverSocket = new ServerSocket( port );
        } catch (IOException e) {
            System.out.println("Could not listen on port " + port);
            System.exit(0);
        }
        while (true) {
            Socket newCon = serverSocket.accept();
            System.out.println("A Client requesting ...");
            count++;
            Runnable runnable2 = new Collector(newCon,count);
            Thread t = new Thread(runnable2);
            t.start(); // start new thread to accept each connection
        }
    }

    public void run() {
        if (this.ID == 0) { // If this is the main processing thread
            boolean isQueueEmpty = true;
            Request next = null;
            while (true) {
                try {
                    Thread.sleep(2000);
                    //if (numReq == 4) { //uncommented for performance testing
                    //start = System.nanoTime(); //uncommented for performance testing

                    synchronized (requestQueue) {
                        isQueueEmpty = requestQueue.isEmpty();
                        if (!isQueueEmpty) 
                            next = requestQueue.remove(0);
                    }
                    while(!isQueueEmpty) {
                        System.out.println("");
                        System.out.println("Processing Next Request from the Request Queue: " + next.getTask());
                        processRequest(next);
                        synchronized (requestQueue) {
                            isQueueEmpty = requestQueue.isEmpty();
                            if (!isQueueEmpty) 
                                next = requestQueue.remove(0);
                        }
                    }
                    /*end = System.nanoTime();// performance testing 
                      numReq = 0; // performance testing
                      runningTime = (end - start) / 1000000.0; // performance testing 
                      System.out.println("Running time in milliseconds: " + runningTime); // performance testing
                      System.out.println("Number of messages sent between Collector and File Servers: " + numMsg); //performance testing
                      numMsg = 0; //performance testing
                    } //performance testing */
                } catch (Exception e) {}
            }
        }
        else { // If this is a connection handler thread
            try {
                BufferedReader inFromClient =
                    new BufferedReader(new InputStreamReader(connection.getInputStream()));
                DataOutputStream outToClient = new DataOutputStream(connection.getOutputStream());
                String received = inFromClient.readLine();
                //System.out.println("Received: " + received);
                considerInput(received,outToClient);

            } catch (Exception e) {
                System.out.println("Thread cannot serve connection");
            }
        }
    }

    public static void processRequest(Request request) throws Exception {
        String[] tokens = request.getTask().split("/");
        if (tokens[0].equals("readDoc")) {
            Random rand = new Random();
            int randID = rand.nextInt(fileServerList.size());
            int numFS = fileServerList.size();
            int id=0;

            int maxVersion = 0;
            int currentVersion;
            String mostRecentFS="";
            String currentFS;
            //Assemble Read Quorum and find the most up-to-date File Server
            System.out.print("Assembling Read Quorum of these File Servers: ");
            for (int i = 0; i < NR; i++) {
                id = (randID + i) % numFS;			
                currentFS = fileServerList.get(id);
                String[] fsTok = currentFS.split(":");
                currentVersion = Integer.parseInt(contactFileServer(currentFS,"getVersionNo"));
                System.out.print(fsTok[1] + " ver:"+ currentVersion + " | ");
                if (currentVersion >= maxVersion) {
                    maxVersion = currentVersion;
                    mostRecentFS = currentFS;
                }
            }
            System.out.println("");
            //Contact this most up-to-date File Server and retrieve the document
            String[] fsTok = mostRecentFS.split(":");
            System.out.println("Asking File Server ID:" +fsTok[1] + " to Read Doc");
            String docContents = contactFileServer(mostRecentFS,"readDoc");
            System.out.println("Received doc: [" + docContents+"] and send back to Client");
            try {
                request.getClientContact().writeBytes(docContents + "\n");
                request.getClientContact().close();
            } catch (Exception e) {}

        }
        else if (tokens[0].equals("writeDoc")) {

            Random rand = new Random();
            int randID = rand.nextInt(fileServerList.size());
            int numFS = fileServerList.size();
            int id=0;

            int maxVersion = 0;
            int currentVersion;
            int[] writeFSver = new int[NW];
            String[] writeFS = new String[NW];
            //Assemble Write Quorum and find the most up-to-date version 
            System.out.print("Assembling Write Quorum of these File Servers: ");
            for (int i = 0; i < NW; i++) {
                id = (randID + i) % numFS;			
                writeFS[i] = fileServerList.get(id);
                writeFSver[i] = Integer.parseInt(contactFileServer(writeFS[i],"getVersionNo"));
                String[] fsTok = writeFS[i].split(":");
                System.out.print(fsTok[1] + " ver:"+ writeFSver[i] + " | ");
                currentVersion = writeFSver[i];
                if (currentVersion >= maxVersion) {
                    maxVersion = currentVersion;
                }
            }
            System.out.println("");

            List<String> updatedFSlist = new ArrayList<String>();
            List<String> outdatedFSlist = new ArrayList<String>();
            //Arrange into list of updated write FS and outdated FS
            for (int i = 0; i < NW; i++) {
                if (writeFSver[i] == maxVersion)
                    updatedFSlist.add(writeFS[i]);
                else
                    outdatedFSlist.add(writeFS[i]);
            }

            //Contact all the up-to-date File Server and append the text
            System.out.print("Asking these up-to-date File Servers to append new text: ");
            for (int i = 0; i < updatedFSlist.size(); i++) {
                String[] fsTok = updatedFSlist.get(i).split(":");
                System.out.print(fsTok[1] + " ");
                contactFileServer(updatedFSlist.get(i),request.getTask());
            }
            System.out.println("");
            try {
                request.getClientContact().writeBytes("Written to the doc\n");
                request.getClientContact().close();
            } catch (Exception e) {}

            //If there are some outdated write File Server
            if (outdatedFSlist.size() > 0) {
                //Get one most up-to-date copy of the document
                String docContents = contactFileServer(updatedFSlist.get(0),"readDoc");
                int newestVer = Integer.parseInt(contactFileServer(updatedFSlist.get(0),"getVersionNo"));

                //Contact all outdated File Server and replace the document
                System.out.print("Asking these outdated File Servers to replace with new copy: ");
                for (int i = 0; i < outdatedFSlist.size(); i++) {
                    String[] fsTok = outdatedFSlist.get(i).split(":");
                    System.out.print(fsTok[1] + " ");
                    contactFileServer(outdatedFSlist.get(i),"replaceDoc/"+newestVer+"/"+docContents);
                }
                System.out.println("");
            }
        }
    }

    public static String contactFileServer(String fs, String message) throws Exception {
        numMsg+=2;
        //System.out.println("Contacting " + fs + " to send " + message);
        String[] fsInfo = fs.split(":");
        String result = "";

        try {
            Socket sendingSocket = new Socket(fsInfo[0],Integer.parseInt(fsInfo[1]));
            DataOutputStream out = new DataOutputStream(sendingSocket.getOutputStream());
            BufferedReader inFromFS = new BufferedReader(new InputStreamReader(sendingSocket.getInputStream()));

            out.writeBytes(message + "\n");

            result = inFromFS.readLine();
            out.close();
            inFromFS.close();
            sendingSocket.close(); 
        } catch (Exception e) {
            System.out.println("Cannot connect to the FS");
        }
        return result;
    }

    public static void considerInput(String received, DataOutputStream backToClient) throws Exception {
        String[] tokens = received.split("/");

        if (tokens[0].equals("imFileServer")) {
            fileServerList.add(tokens[1]+":"+tokens[2]);
            backToClient.writeBytes("Added you to File Server list\n");
            backToClient.close();
            System.out.println("Current Number of File Servers: " + fileServerList.size());
        } 
        else if (tokens[0].equals("readDoc")) {
            Request newRequest = new Request(received,backToClient);
            synchronized (requestQueue) {
                requestQueue.add(newRequest);
            }
            numReq++;
        }
        else if (tokens[0].equals("writeDoc")) {
            Request newRequest = new Request(received,backToClient);
            synchronized (requestQueue) {
                requestQueue.add(newRequest);
            }
            numReq++;
        }
    }

    public static void readConfigFile() {
        try{
            String fileName = "config.txt";
            FileInputStream fstream = new FileInputStream(fileName);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String[] tokens; 
            String strLine;
            while ((strLine = br.readLine()) != null) {
                tokens = strLine.split("=");
                if (tokens[0].equals("NR"))
                    NR = Integer.parseInt(tokens[1]);
                else if (tokens[0].equals("NW"))
                    NW = Integer.parseInt(tokens[1]);
            }			
            in.close();
            fstream.close();
            br.close();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
