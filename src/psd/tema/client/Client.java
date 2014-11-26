package psd.tema.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import utils.Error;

public class Client {

    private Integer serverPort    = 16587;
	private String  serverAddress = "localhost";
    private Socket  clientSocket  = null;
    
    /* Debugging & testing purpose only */
    private String name = "";
    private String cmdFile = "";
    
    public Client() {}

    public Client(String serverAddress, Integer serverPort) {
        this.serverPort    = serverPort;
        this.serverAddress = serverAddress;
    }
    
    /**
     * Debugging purposes
     * @param name Client name
     */
    public void setName(String name) {
    	this.name = name;
    }
    
    public void run() {
    	String command, response;
    	BufferedReader console, recv;
    	PrintWriter send;
    	
	    try {
	    	console = new BufferedReader(new InputStreamReader(System.in));
			
	    	System.out.println("[Client] Connecting client at address " +
                    			this.serverAddress + ":" + serverPort);
            /* Connect to server */
		    this.clientSocket = new Socket(this.serverAddress, this.serverPort);
		    
		    /* Open streams for reading and writing */
		    recv = new BufferedReader(new InputStreamReader(
									  this.clientSocket.getInputStream()));
		    send = new PrintWriter(this.clientSocket.getOutputStream());

		    while (true) { 
			    /* Read commands from the console */
			    command = console.readLine();
			    
			    /* DEBUG */
		    	if (command == null)
		    		continue;
			    if (command.equalsIgnoreCase("exit")) {
			    	send.println(command);
				    send.flush();
				    
			    	send.close();
				    recv.close();
			    	console.close();
			    	this.clientSocket.close();
			    
			    	return;
			    }
			    /* Send commands to the server */
			    send.println(command);
			    send.flush();
			    
			    response = recv.readLine();
			    if (response != null) {
			    	Integer readLines = Integer.parseInt(response);
			    	if (readLines <= 0) {
			    		System.out.println("[Client][Status] " + Error.values()[-readLines].name());
			    		continue;
			    	}
			    	while (readLines > 0) {
			    		response = recv.readLine();
			    		System.out.println("[Client][Response] " + response);
			    		readLines--;
			    	}
			    	readLines = Integer.parseInt(recv.readLine());
			    	System.out.println("[Client][Status] " + Error.values()[-readLines].name());
			    }
		    }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setTestFile(String cmdFile) {
		this.cmdFile = cmdFile;
	}

	private void removeChildren(File directory) {
		if (directory.isDirectory()) {
			for (File child : directory.listFiles())
				removeChildren(child);
		}
		directory.delete();
	}
	private void emptyHome() {
		File f = new File("resources/" + this.name);
    	for (File child : f.listFiles())
    		removeChildren(child);
	}
	public void runTestFile() {
		if (this.cmdFile.isEmpty()) {
			System.err.println("[" + name +
							"] Cannot run test file -- missing command file");
			run();
			return;
		}
		
		String command, response;
    	BufferedReader cmdBuffer, recv;
    	PrintWriter send;
    	Boolean desiredStatus = null;
	    try {
	    	emptyHome();
	    	cmdBuffer = new BufferedReader(new FileReader(cmdFile));
			
	    	System.out.println("[" + name + "] Connecting client at address " +
                    			this.serverAddress + ":" + serverPort);
            /* Connect to server */
		    this.clientSocket = new Socket(this.serverAddress, this.serverPort);
		    
		    /* Open streams for reading and writing */
		    recv = new BufferedReader(new InputStreamReader(
									  this.clientSocket.getInputStream()));
		    send = new PrintWriter(this.clientSocket.getOutputStream());

		    while (cmdBuffer.ready()) {
		    	/* Read commands from the test file */
		    	command = cmdBuffer.readLine().trim();
		    	desiredStatus = new Boolean(cmdBuffer.readLine());
		    	if (command == null)
		    		continue;
		    	
		    	System.out.println("[" + name + "][Command] " + command);
			    
		    	if (command.equalsIgnoreCase("exit")) {
			    	send.close();
				    recv.close();
			    	cmdBuffer.close();
			    	this.clientSocket.close();
			    
			    	return;
			    }
			    /* Send commands to the server */
			    send.println(command);
			    send.flush();
			    
			    response = recv.readLine();
			    if (response != null) {
			    	Integer readLines = Integer.parseInt(response);
			    	if (readLines <= 0) {
			    		System.out.println("[" +name + "][Status] " + response);
			    		if ((Integer.parseInt(response) == 0 && desiredStatus) ||
				    		(Integer.parseInt(response) < 0 && (!desiredStatus))) {
				    		System.out.println("..................... [PASSED]...................");
			    		} else {
				    		System.out.println(".....................:( [FAILED] :)...................");
				    	}
			    		continue;
			    	}
			    	while (readLines > 0) {
			    		response = recv.readLine();
			    		System.out.println("[" +name + "][Reply] " + response);
			    		readLines--;
			    	}
			    	response = recv.readLine();
			    	System.out.println("[" +name + "][Status] " + response);
			    	if ((Integer.parseInt(response) == 0 && desiredStatus) ||
			    		(Integer.parseInt(response) == 0 && (!desiredStatus)))
			    		System.out.println("..................... [PASSED]...................");
			    	else
			    		System.out.println(".....................:( [FAILED] :)...................");
			    }
			    
		    }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		Client alice = new Client();
        alice.setName("alice");
//        alice.setTestFile("resources/test/alice_cmds.txt");
        alice.runTestFile();
	}
}
