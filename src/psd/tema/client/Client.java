package psd.tema.client;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

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
			    	while (readLines > 0) {
			    		response = recv.readLine();
			    		System.out.println("[Client][Response] " + response);
			    		readLines--;
			    	}
			    	System.out.println("[Client][Status] " + response);
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
    	
	    try {
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
		    	command = cmdBuffer.readLine(); 
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
			    	while (readLines > 0) {
			    		response = recv.readLine();
			    		System.out.println("[" +name + "][Reply] " + response);
			    		readLines--;
			    	}
			    	System.out.println("[" +name + "][Status] " + response);
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
        alice.run();
	}
}
