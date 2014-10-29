package psd.tema;

import java.io.BufferedReader;
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
    
    public Client() {}

    public Client(String serverAddress, Integer serverPort) {
        this.serverPort    = serverPort;
        this.serverAddress = serverAddress;
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
			    if (response != null)
			    	System.out.println("response " + response);
		    }
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		/*
        assert (args.length == 2) :
                "Usage: java Client <serverAddress> <serverPort>";
        String  serverAddress = args[0];
        Integer serverPort    = Integer.parseInt(args[1]); */
        Client client = new Client();
        /*
		Client client = new Client(serverAddress, serverPort); */
        client.run();
	}

}
