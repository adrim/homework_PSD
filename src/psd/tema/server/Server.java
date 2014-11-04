package psd.tema.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.lang.Thread;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.spi.FileTypeDetector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import utils.Node;
import utils.Pair;

enum Access {
	NONE,
	READ,
	WRITE,
	READ_WRITE
}
enum Error {
	OK,
	INVALID_CREDENTIALS,
	ACCESS_DENIED,
	FILE_NOT_FOUND,
	FILE_EXISTS,
	UNKNOWN_COMMAND,
    UNKNOWN_ERROR,
    FOLDER_NOT_EMPTY
}
class AccessLevel {
	Right rights = new Right();  
}

class ClientThread extends Thread {
	private static final String res			= "resources/";
	private static final String shadow 	 	= "resources/cntrl/shadow.txt";
	private static final String access  	= "resources/cntrl/access.txt";
    private static Map<String, Right> accessPolicy = null;
    private static HashMap<String, String> shadowFile =
                            new HashMap<String, String>();
    private static Node fileSystem = new Node();

	private Socket socket = null;
	
	public ClientThread(Socket socket) {
		this.socket = socket;
		
		readPolicy();
	}
	
	private static void readPolicy() {
		if (accessPolicy != null)
			return;
		
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(access));
			accessPolicy = Collections.synchronizedMap(new HashMap<String, Right>());
			
			while(br.ready()) {
			    String  str = br.readLine();
				Integer ind = str.lastIndexOf(' ');
	    		String file = str.substring(0, ind);
		    	Right accessRights = new Right(Integer.parseInt(str.substring(ind+1)));
			
			    /* If the user doesn't have the privileges requested, return ACCESS_DENIED error */
				
		    	accessPolicy.put(file, accessRights);
	    	}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private static void showPolicy() {
		for (Map.Entry<String, Right> entry : accessPolicy.entrySet()) {
			System.out.println(entry.getKey() + " has right "+ entry.getValue());
		}
	}
	private static void writePolicy() {
		PrintWriter pw;
		try {
			pw = new PrintWriter(access);
			
			for (Entry<String, Right> entry : accessPolicy.entrySet()) {
		    	pw.println(entry.getKey() + ' ' + entry.getValue().getAccess());
	    	}
			pw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Read credentials from file
	 * 
	 * ** ToDo **
	 * Keep [part of] them in memory
	 * 
	 * @param username
	 * @param password
	 * @return
	 */
	private Error checkCredentials(String username, String password) {
		BufferedReader br = null;
		Error ret = Error.INVALID_CREDENTIALS;
		
		try {
			br = new BufferedReader(new FileReader(shadow));
			while(br.ready()) {
				// username
				String user = br.readLine();
				Integer splitIndex = user.indexOf(' ');
				
				assert splitIndex > 0 : "[Server] Corrupt shadow file.";
				
				/* Invalid password */
				if (user.substring(0, splitIndex).equals(username)) {
					if (!user.substring(splitIndex+1).equals(password)) {
						ret = Error.INVALID_CREDENTIALS;
					} else {
						ret = Error.OK;
					}
					
					br.close();
					return ret;
				}
			}
			/* Username and password not found on the server */
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
		return ret;
	}

	/**
	 * Check whether one of the ancestors of the requested resource has the
	 * desired access
	 * 
	 * @param resParent
	 * @param requestedAccess
	 * @return
	 */
	private Error checkParentAccess(String resParent, Access requestedAccess) {
		/* Check for read or write access rights */
        if (accessPolicy.containsKey(resParent)) {
            return (accessPolicy.get(resParent).hasAccess(requestedAccess) ?
            		Error.OK : Error.ACCESS_DENIED);
        }
        Integer ind = resParent.lastIndexOf('/');
		if (ind != -1)
			return checkParentAccess(resParent.substring(0, ind), requestedAccess);
	
    	return Error.ACCESS_DENIED;
	}
   
	/**
	 * Check whether the resource has the desired access
	 * 
	 * @param username Username for which the access rights are verified
	 * @param resourceName Resource on which the access is verified
	 * @param requested Access Access requested by username; to be compared to
	 * 					the access stored for <resourceName>
	 * @param create If true, we check the access for creating a file and need
	 * 				 to check the ancestors rights. Otherwise, only the given
	 * 				 path is checked for the desired access
	 * @return Error.OK if access was granted, another Error otherwise 
	 */
	private Error checkAccess(String username, String resourceName,
    						  Access requestedAccess, Boolean create) {
		Integer ind = -1;
		String res = "";
		
		ind = resourceName.indexOf('/');
        if (ind != -1) {
        	res = resourceName.substring(0,ind);
        } else {
        	res = resourceName;
        }
        /* Check whether the resource is in the user's home
         * If so, the user has full access
         */
        if (res.equals(username)) {
            return Error.OK;
        }
        
		/* Check for read or write access rights */
        synchronized (accessPolicy) {
        	if (create)
        		return checkParentAccess(resourceName, requestedAccess);
        	if (accessPolicy.containsKey(resourceName)) {
                return (accessPolicy.get(resourceName).hasAccess(requestedAccess) ?
                		Error.OK : Error.ACCESS_DENIED);
            }
        }
		return Error.ACCESS_DENIED;
	}
	
    private Right getAccess(String username, String resourceName) {
    	Right accessLevel = new Right();
    	
    	Integer ind = resourceName.indexOf('/');
        if (ind != -1) {
            /* Check whether the resource is in the user's home
             * If so, the user has full access
             */
            if (resourceName.substring(0,ind).equals(username))
                return new Right(Access.READ_WRITE);
        }
    	
    	return accessPolicy.get(resourceName);
    }
    
	/**
	 * Create a new resource
	 * 
	 * @param username Username issuing the command
	 * @param resName  Path of the desired resource
	 * @param type     Type of res: 0 for files, 1 for directories
	 * @param value    Value to be written to file, ignored for directories
	 * @return		   Error.OK if write was successful, or another Error
	 * 				   otherwise
	 */
	private Error createResource(String username, String resName, int type, String value) {
		Error 	ret = Error.ACCESS_DENIED;
		File 	f;
		Integer ind;
		String 	resource;
		PrintWriter pw;
		
		f = new File(res + resName);
		if (f.exists())
			return Error.FILE_EXISTS;
		
		/* A user can create a resource if he's the owner or has WRITE
		 * permission on the parent directory 
		 */
		ret = checkAccess(username, resName, Access.WRITE, true);
		if (ret != Error.OK)
			return ret;
		
		try {
			if (type == FileType.FILE){
				f.getParentFile().mkdirs();
				f.createNewFile();
				pw = new PrintWriter(f);
				pw.println(value);
				pw.close();
			} else {
				f.mkdirs();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/* Don't give other users permission for your files */
		ind = resName.indexOf('/');
        if (ind != -1) {
        	resource = resName.substring(0,ind);
        } else {
        	resource = resName;
        }
        /* Check whether the resource is created in the user's home
         * If so, create it without rights
         * Otherwise, create with read write
         */
        if (resource.equals(username)) {
        	accessPolicy.put(resName, new Right());
        } else {
        	accessPolicy.put(resName, new Right("RDWR"));
        }
//		System.out.println(accessPolicy.toString());
		return Error.OK;
	}
	
	/**
	 * Read a resource
	 * 
	 * @param username Username issuing the command
	 * @param resourceName Resource to be read
	 * @return  Pair of ((Error.OK if read was successful / other Error),
	 * 					 the result of reading the resource)
	 */
	private Pair<Error, ArrayList<String>> readResource(String username, String resourceName) {
		Error ret;
		File  f = new File(res + resourceName);
        ArrayList<String> value = new ArrayList<String>();
        
		if (!f.exists())
			return new Pair<Error, ArrayList<String>>(Error.FILE_NOT_FOUND, value);
		
		ret = checkAccess(username, resourceName, Access.READ, false);
		if (ret != Error.OK)
            return new Pair<Error, ArrayList<String>>(ret, null);
        
        /* Read on the directory is equivalent to list files */
        if (f.isDirectory()) {
        	String []elements = f.list();
        	Collections.addAll(value, elements);
		} else {
			/* Read the file */
	        try {
				BufferedReader br = new BufferedReader(new FileReader(f));
				
				while (br.ready())
					value.add(br.readLine());
				
				br.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return new Pair<Error, ArrayList<String>>(ret, value);
	}
	
	/**
	 * Write a value to a resource
	 * 
	 * @param username User issuing the command
	 * @param resourceName Resource whose contents are to be modified
	 * @param value The value to be written in the resource
	 * @return Error.OK if write was successful, or another Error otherwise
	 */
	private Error writeResource(String username, String resourceName,
								String value) {
		Error ret; 
        File  f = new File(res + resourceName);
        
        if (!f.exists())
        	return Error.FILE_NOT_FOUND;
        
    	/* Can't write to a directory */
        if (f.isDirectory())
        	return Error.ACCESS_DENIED;
        	
        ret = checkAccess(username, resourceName, Access.WRITE, false);
        if (ret != Error.OK)
            return ret;
        
        /* Write value to file */
        try {
			PrintWriter pw = new PrintWriter(f);
			pw.println(value);
			pw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        return ret;
	}
    
	/**
	 * Delete a resource
	 * 
	 * @param username User issuing the delete command 
	 * @param resourcePath Path towards the resource
	 * @return Error.OK if everything went well or another Error otherwise
	 */
	private Error deleteResource(String username, String resourcePath) {
		Error ret; 
        File path = new File(res + resourcePath);
        
        if (!path.exists())
    		return Error.FILE_NOT_FOUND;
        
        ret = checkAccess(username, resourcePath, Access.WRITE, false);
        if (ret != Error.OK)
            return ret;
        
        /* Delete the file or folder and remove it from the access policy */
    	if (path.isDirectory() && path.list().length > 0) {
    		return Error.FOLDER_NOT_EMPTY;
    	}
    	accessPolicy.remove(resourcePath);
    	path.delete();
    	
        return ret;
    }

    /**
     * The owner can change the rights however desired
     * Other users can only change the rights if the file is in their home or
     * if they have execute right [?]
     * 
     * @param username User changing the rights
     * @param resourcePath Resource whose rights will be changed
     * @param newRights New rights to replace the old ones
     * @return Error code
     */
	private Error changeRights(String username, String resourcePath, String newRights) {
    	String resource;
    	Error err = Error.OK;
    	Right accessLevel = new Right(newRights);
    	
    	/* Check whether the file exists or not */
        File  f = new File(res + resourcePath);        
        if (!f.exists())
        	return Error.FILE_NOT_FOUND;
    	
    	Integer ind = resourcePath.indexOf('/');
        if (ind == -1) {
        	resource = resourcePath;
        } else {
        	resource = resourcePath.substring(0,ind);
        }
        /* Check whether the resource is in the user's home
         * If not, the user cannot change the file rights
         */
        if (!resource.equals(username))
            return Error.ACCESS_DENIED;
        
        /* the owner can set the desired access for the other users */
        accessPolicy.put(resourcePath, accessLevel);
        return err;
    }
	
	/**
	 * Remove last '/' from file in order to recognize both strings ending
	 * in '/' or not
	 * 
	 * @param fileName Raw filename
	 * @return Filename with the trailing '/' removed
	 */
	private String processFileName(String fileName) {
		if (fileName.charAt(fileName.length() - 1) == '/') {
			return fileName.substring(0, fileName.length() - 1);
		}
		return fileName;
	}
    
	/**
	 * Process the commands received from a client
	 *  
	 * @param cmd Command to be executed
	 * @param send
	 * @return
	 */
	public Error processCommand(String cmd, PrintWriter send) {
		Error err = Error.OK;
		String 	 fileName;
		String []tokens = cmd.split(" ");

        /* The user needs to authenticate + command + resource */
        if (tokens.length < 3)
            return Error.ACCESS_DENIED;

		/* Check if the user is recognized in the system */
		err = checkCredentials(tokens[0], tokens[1]);
	    if (err != Error.OK)
	    	return err;
	    
    	switch(tokens[2]) {
    	case "create":
    		if (tokens.length < 6)
    			return Error.UNKNOWN_COMMAND;
    		fileName = processFileName(tokens[3]);
            err = createResource(tokens[0], fileName,
            					 Integer.parseInt(tokens[4]), tokens[5]);
    		break;
    	case "delete":
    		if (tokens.length < 4)
    			return Error.UNKNOWN_COMMAND;
    		fileName = processFileName(tokens[3]);
            err = deleteResource(tokens[0], fileName);
    		break;
    	case "read":
    		if (tokens.length < 4)
    			return Error.UNKNOWN_COMMAND;
    		fileName = processFileName(tokens[3]);
            Pair<Error, ArrayList<String>> ret = readResource(tokens[0], fileName);
            ArrayList<String> contents = ret.getSecond();
        	
            err = ret.getFirst();
            if (contents != null && contents.size() > 0) {
            	send.println(contents.size());
        		send.flush();
        		
        		for (String line : contents) {
    	        	send.println(line);
    	        	send.flush();
            	}
    		}
        	
    		break;
    	case "write":
    		if (tokens.length < 5)
    			return Error.UNKNOWN_COMMAND;
    		fileName = processFileName(tokens[3]);
            err 	 = writeResource(tokens[0], fileName, tokens[4]);
    		break;
    	case "change":
    		if (tokens.length < 5)
    			return Error.UNKNOWN_COMMAND;
    		fileName = processFileName(tokens[3]);
            err 	 = changeRights(tokens[0], fileName, tokens[4]);
    		break;
    	case "show":
    		/** DEBUGGING ONLY */
    		showPolicy();
    		break;
    	default:
    		return Error.UNKNOWN_COMMAND;
    	}
    	
    	return err;
    }

	public void run() {
		try {
        	System.out.println("[Server] New connection accepted: address="	+
        						socket.getInetAddress() + ": "		+
        						socket.getPort());
        	/* Open streams for reading and writing */
		    BufferedReader recv = new BufferedReader(
									new InputStreamReader(
										this.socket.getInputStream()));
		    PrintWriter    send = new PrintWriter(
		    						this.socket.getOutputStream());
		    
        	/* Wait for commands from the client*/
        	String command;
            Error err = Error.OK;
        	while ((command = recv.readLine()) != null) {
        		System.out.println("[Server][Command] " + command);

        		if (command.equals("exit")) {
        			recv.close();
                	send.close();
                	socket.close();
        		}
        			
        		/* Execute the commands */
        		err = processCommand(command, send);
        		
        		/* Send the response to the client */
                System.out.println("[Server][Status] " + err + "=-" + err.ordinal());
            	send.println(-err.ordinal());
            	send.flush();
        	}
        	
        	/* Close the connection */
        	recv.close();
        	send.close();
        	socket.close();
		} catch (IOException e){
			e.printStackTrace();
		}
	}
}

public class Server {
	private static final String res			= "resources/";
	private static final String shadow 	 	= "resources/shadow.txt";
	private static final String access  	= "resources/access.txt";
    private static HashMap<String, Right> accessPolicy =
                            new HashMap<String, Right>();

	private ArrayList<ClientThread> clients;
	private ServerSocket serverSock;
	
	private Integer serverPort = 16587;
	
	public void print(String name) {
		System.out.println("Client " + name + " is connected");
	}
	private void readPolicy() {
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(access));
			accessPolicy = new HashMap<String, Right>();
			
			while(br.ready()) {
			    String  str = br.readLine();
				Integer ind = str.lastIndexOf(' ');
	    		String file = str.substring(0, ind);
		    	Right accessRights = new Right(Integer.parseInt(str.substring(ind+1)));
			
			    /* If the user doesn't have the privileges requested, return ACCESS_DENIED error */
				
		    	accessPolicy.put(file, accessRights);
	    	}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void writePolicy() {
		PrintWriter pw;
		try {
			pw = new PrintWriter(access);
			
			for (Entry<String, Right> entry : accessPolicy.entrySet()) {
		    	pw.println(entry.getKey() + ' ' + entry.getValue().getAccess());
	    	}
			pw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void run() {
		Socket clientSocket;
		
		try {
			serverSock = new ServerSocket(serverPort);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		while (true) {
			System.out.println("[Server] Waiting for clients on port " + 
								serverPort);
			try {
				clientSocket = serverSock.accept();
				new ClientThread(clientSocket).start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) {
		Server server = new Server();
		server.run();
	}
}
