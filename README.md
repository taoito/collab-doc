Collaborative Document Sharing System
----------------------------
A simple document sharing system that use read/write quorum for the distributed transactions.

Design
----------------------------

There are three main components in this Collaborative Document Sharing System:
Client, File Servers, and Collector

1. Client (src/Client.java)
	The users interact with the Client UI to read and write contents to the shared document. The system can have any number of Clients running, accessing the same document. A Client knows the Collector's IP Address and listening port. There are 3 Options to choose from the UI: (1) Write (2) Read (3) Exit.
	The Write option let user enter new text (currently special character likes \n are not supported) then calls the writeDoc() method and establish a TCP connection to the Collector, sending a message "writeDoc/[newText]", then wait for its response confirmation: "Written to the doc" if the write is successful. 
	The Read option calls the readDoc() method and establish a TCP connection to the Collector, sending a message "readDoc", then waits for its response, which displays "Current document contents: " and current contents of the document.   
 	

2. File Servers (src/FileServer.java)
	Each File Server listens on a seperate port number, and we also use this as their ID for convenience. It knows the Collector's IP and Port and only talks to the Collector. 
	A file server starts by creating a document replica named doc[PortNumber].txt in the local doc/ folder, starting with versionNo 0. It then contacts the Collector by sending a message "imFileServer/[myIP]/[myPort]" to add itself to the File Server List. Then it runs a while loop listening on given port, waiting for request from the Collector.
	An incoming request is passed to the considerInput() method, which parses the message can calls the appropriate method. The method getDocContents() reads the local file replica and return its contents. The method appendToDoc() appends new text to this replica then updates the versionNo. The method replaceDoc() replaces the replica with new contents and update versionNo to the most recent number.


3. Collector (src/Collector.java)
	The Collector is the main contact point between the Clients and File Servers. It reads request message from the Client, put it in the request queue, process this queue in FIFO order, assembling the needed read/write quorum, then query the right file servers, get results and return this back to the requesting client. Collector prints to stdout all these steps with their information, it runs with | tee log.txt to save all these to the log file.
	The Collector starts by readConfigFile() which reads the local Config.txt file. This determines the number of read quorum NR and number of write quorum NW. It starts one main processing thread with thread ID 0, which scans the requestQueue and process requests from there. It then runs a while loop, starting a new thread t eah time it accepts TCP connection from a Client (or File Server wanting to join)

	Thread t passes the received message to the considerInput() method, which parses the request. If it's a "imFileServer" request from File Server, adds this new file server contact information to the fileServerList. For both "readDoc" and "writeDoc" requests, the thread obtains synchronized lock to the requestQueue list and adds this new request to the list. This ensures concurrency control and sequentially consistent order of the requests.

	Main processing thread 0 runs a while loop looking at the requestQueue, obtaining the synchronized lock to remove the requests in FIFO order if it's not empty. Then it calls processRequest() for each one. 
	This method determines if this is a "readDoc" or "writeDoc" request. For this first one, it assembles a read quorum which consists of NR random file servers from the fileServerList. It queries each for their versionNo, printing out their ID and versionNo, determining ones with the maxVersion number. It contacts one of them (most recent file servers) sending "readDoc" message and waits for the result. The result is then passed back to the Client contact.
	For the "writeDoc" request, it assembles a write quorum which consists of NW random file servers from the fileServerList. It queries each for their versionNo, printing out their ID and versionNo, determining the maxVersion number. It groups all file servers with this maxVersion number into an updatedFSlist, and others into an outdatedFSlist. The Collector contacts all in the updatedFSlist to send "writeDoc" to append their local replicas. If the outdatedFSlist has something, the Collector sends "readDoc" to one of the updated file servers to get the most recent document contents, then send "replaceDoc" to all these outdated File Servers so they get the lastest copy. The confirmation is passed back to the requesting Client.

	Main processing thread also has some commented-out lines for performance evaluation purpose. Uncomment these lines, and the Collector only start processing requests if 4 Clients have sent in their requests, it then measures and displays the Number of Messages and Running Time in ms to finish processing all these 4 requests.

