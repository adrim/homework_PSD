package psd.tema;

import psd.tema.client.Client;
import psd.tema.server.Server;

public class Main {
	public static void runTest() {
		Server server = new Server();
		server.run();

		Client alice = new Client();
        alice.setName("alice");
        alice.setTestFile("resources/test/alice_cmd.txt");
        alice.runTestFile();
        
        Client bob = new Client();
        bob.setName("bob");
        bob.setTestFile("resources/test/bob_cmd.txt");
        bob.runTestFile();

	}
	private static void run() {
		new Thread(new Runnable() {
			public void run() {
				Server server = new Server();
				server.run();		
			}
		}).start();

		new Thread(new Runnable() {
			public void run() {
				Client alice = new Client();
				alice.setName("alice");
				alice.run();		
			}
		}).start();
		
        /*
        Client bob = new Client();
        bob.setName("bob");
        bob.run();*/

	}
	public static void main(String[] args) {
        run();
//        runTest();
	}

}
