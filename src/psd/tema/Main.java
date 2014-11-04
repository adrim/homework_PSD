package psd.tema;

import psd.tema.client.Client;
import psd.tema.server.Server;

public class Main {
	public static void run() {
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
	}
	public static void runTest() {
		new Thread(new Runnable() {
			public void run() {
				Server server = new Server();
				server.run();		
			}
		}).start();

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		
		new Thread(new Runnable() {
			public void run() {
				Client alice = new Client();
		        alice.setName("alice");
		        alice.setTestFile("resources/test/alice_cmds.txt");
		        alice.runTestFile();		
			}
		}).start();
		
	}
	public static void main(String[] args) {
//        Main.run();
        Main.runTest();
	}

}
