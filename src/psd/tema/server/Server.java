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
import java.util.HashMap;
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
//	private static final String res = Server.class.
	private static final String res			= "resources/";
	private static final String listOfFiles = "resources/files.txt";
	private static final String shadow 	 	= "resources/shadow.txt";
	private static final String access  	= "resources/access.txt";
    private static HashMap<String, Right> accessPolicy =
                            new HashMap<String, Right>();
    private static HashMap<String, String> shadowFile =
                            new HashMap<String, String>();

    private static Node fileSystem = new Node();

	private Socket socket = null;
	private Random random = null;
		
	
	public ClientThread(Socket socket) {
		this.socket = socket;
		this.random = new Random();
	}
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

    private Error checkAccess(String username, String resourceName, Access requestedAccess) {
		BufferedReader br = null;
		Error   ret = Error.OK;
		Integer ind = -1;

		try {
			/* check to see whether a file or folder exists */ 
			if (requestedAccess == Access.NONE) {
				System.out.println("grrr saldkaslda");
                if (accessPolicy.containsKey(resourceName)) {
                    ret = Error.FILE_EXISTS;
                } else {
                	String str;
                	
    				br = new BufferedReader(new FileReader(access));
	    			while(br.ready()) {
			    		str = br.readLine();
				    	
					    if (str.substring(0, str.lastIndexOf(' ')).equals(resourceName)) {
						    ret = Error.FILE_EXISTS;
	    					break;
		    			}
			    	}
                }
			} else {
				System.out.println("res name " + resourceName  + " ind " + resourceName.indexOf('/'));
				ind = resourceName.indexOf('/');
		        if (ind != -1) {
		            /* Check whether the resource is in the user's home
		             * If so, the user has full access
		             */
		        	System.out.println("grrr " + resourceName.substring(0, ind) + " vs user " + username);
		            if (resourceName.substring(0,ind).equals(username))
		                return Error.OK;
		        } else {
		        	System.out.println("ind = -1??? you crazy mate/?? " + resourceName.indexOf('\\'));
		        }
				/* Check for read or write access rights */
                if (accessPolicy.containsKey(resourceName)) {
                	Right accessRights = accessPolicy.get(resourceName);
                    ret = accessRights.hasAccess(requestedAccess) ? Error.OK : Error.ACCESS_DENIED;
                } else {
				    br  = new BufferedReader(new FileReader(access));
                    ret = Error.ACCESS_DENIED;
				
    				while(br.ready()) {
	    			    String str = br.readLine();
	    			    System.out.println("[DEBUG] str " + str);
		    			ind = str.lastIndexOf(' ');
			    		String file = str.substring(0, ind);
				    	Right accessRights = new Right(Integer.parseInt(str.substring(ind+1)));
					
					    /* If the user doesn't have the privileges requested, return ACCESS_DENIED error */
    					if (file.equals(resourceName)) {
	    					ret = accessRights.hasAccess(requestedAccess) ? Error.OK : Error.ACCESS_DENIED;
                            accessPolicy.put(resourceName, accessRights);
		    			}
			    	}
                }
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
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
	 * 
	 * @param username Username making the request
	 * @param password Password of the user
	 * @param resName  Path of the desired resource
	 * @param type     Type of res: 0 for files, 1 for directories
	 * @param value    Value to be written to file, ignored for directories
	 * @return Error cde
	 */
	private Error createResource(String username, String resName, int type, String value) {
		Error ret = Error.UNKNOWN_ERROR;
		File f;
		PrintWriter pw;
		
		f = new File(res + resName);
		if (f.exists())
			return Error.FILE_EXISTS;
		
		/* A user can create a resource if he's the owner or has WRITE
		 * permission on the directory 
		 */
		//ret = checkAccess(username, resName.substring(0, resName.lastIndexOf('/')), Access.WRITE);
		ret = checkAccess(username, resName, Access.WRITE);
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
				pw = new PrintWriter(new FileWriter(access, true));
				pw.append(resName + ' ' + Right.write + '\n');
				pw.flush();
				pw.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Error.OK;
	}
	private Pair<Error, String> readResource(String username, String resourceName) {
		Error 	ret = checkAccess(username, resourceName, Access.READ);
        String	value = "";
        
        if (ret != Error.OK)
            return new Pair<Error, String>(ret, value);
		
        File f = new File(res + resourceName);
        
        /* Read on the directory is equivalent to list files */
        if (f.isDirectory()) {
		} else {
			/* Read the file */
	        try {
				BufferedReader br = new BufferedReader(new FileReader(f));
				
				while (br.ready())
					value += br.readLine();
				
				br.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return new Pair<Error, String>(ret, value);
	}
	private Error writeResource(String username, String resourceName,
								String value) {
		Error ret	= checkAccess(username, resourceName, Access.WRITE);
        File f		= new File(res + resourceName);
        
        if (ret != Error.OK)
            return ret;
        
        /* Can't write to a directory */
        if (f.isDirectory())
        	return Error.ACCESS_DENIED;
        
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
    private Error deleteResource(String username, String resourcePath) {
		Error ret; 
        File path = new File(res + resourcePath);
        
        if (!path.exists())
    		return Error.FILE_NOT_FOUND;
        
        ret = checkAccess(username, resourcePath, Access.WRITE);
        if (ret != Error.OK)
            return ret;
        
        /* Delete the file or folder and remove it from the access policy */
        try {
        	if (path.isDirectory() && path.list().length > 0)
        		return Error.FOLDER_NOT_EMPTY;
        	
            System.out.println("Delete? " + path.delete());

            String str = null;
			Integer ind;
            File in  = new File(access);
            File out = new File(access + ".tmp");

            BufferedReader br = new BufferedReader(new FileReader(in));
            PrintWriter    pw = new PrintWriter(out);

            while (br.ready()) {
                str = br.readLine();
				ind = str.lastIndexOf(' ');
					
                /* copy other files to the temporary file */
                if (!str.substring(0, ind).equals(resourcePath)) {
                    pw.println(str);
                }
            }
            br.close();
            pw.close();
            
            /* delete old access file and replace it with the new temp file */
            in.delete();
            out.renameTo(in);
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        boolean found = false;
		Error err;
        Access requestAccess = Access.NONE;

        if (newRights.equals("RDONLY"))
            requestAccess = Access.READ;
        else if (newRights.equals("WRONLY"))
            requestAccess = Access.WRITE;
        else if (newRights.equals("RDWR"))
            requestAccess = Access.READ_WRITE;

        err = checkAccess(username, resourcePath, requestAccess);
        
        if (err != Error.OK)
            return err;

        try {
            String str = null;
            Integer noFiles;
            File in  = new File(access);
            File out = new File(access + ".tmp");

            BufferedReader br = new BufferedReader(new FileReader(in));
            PrintWriter    pw = new PrintWriter(out);

            while (br.ready()) {
				Integer ind;

                str = br.readLine();
			    ind = str.lastIndexOf(' ');
				
                if (str.substring(0, ind).equals(resourcePath)) {
                    Integer rights = Integer.parseInt(str.substring(ind)+1);
                    
                    found = true;
                    
                    /* Change access */
                    pw.println(str + ' ' + requestAccess.ordinal());

                } else {
                    /* Copy other files to the temporary file */
                    pw.println(str);
                }
            }
            if (!found)
            	pw.println(resourcePath + ' ' + requestAccess.ordinal()*2);
            br.close();
            pw.close();
            
            /* delete old access file and replace it with the new temp file */
            in.delete();
            out.renameTo(in);
        } catch (IOException e) {
            e.printStackTrace();
        }

        /* 
         * If the file or folder is in the user's home, then accept any rights
         * Otherwise, if
         *
         */
         return err;
    }
    public Error processCommand(String cmd) {
		Error err = Error.OK;
		String []tokens = cmd.split(" ");

        /* The user needs to authenticate + command + resource */
        if (tokens.length < 3)
            return Error.ACCESS_DENIED;

		/* Check if the user is recognized in the system */
        System.out.println("tokens 0 : " + tokens[0] + " tokens 1: " + tokens[1]);
		err = checkCredentials(tokens[0], tokens[1]);
	    if (err != Error.OK)
	    	return err;
	    
	    System.out.println("cmd " + tokens[2]);
    	switch(tokens[2]) {
    	case "create":
    		if (tokens.length < 6)
    			return Error.UNKNOWN_COMMAND;
            err = createResource(tokens[0], tokens[3],
            					 Integer.parseInt(tokens[4]), tokens[5]);
    		break;
    	case "delete":
    		if (tokens.length < 4)
    			return Error.UNKNOWN_COMMAND;
            err = deleteResource(tokens[0], tokens[3]);
    		break;
    	case "read":
    		if (tokens.length < 4)
    			return Error.UNKNOWN_COMMAND;
            Pair<Error, String> ret = readResource(tokens[0], tokens[3]);
            try {
				this.socket.getOutputStream().write(ret.getSecond().getBytes());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		break;
    	case "write":
    		if (tokens.length < 5)
    			return Error.UNKNOWN_COMMAND;
            err = writeResource(tokens[0], tokens[3], tokens[4]);
    		break;
    	case "changeRights":
    		if (tokens.length < 5)
    			return Error.UNKNOWN_COMMAND;
            err = changeRights(tokens[0], tokens[3], tokens[4]);
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
        	
        		System.out.println("[Server] Command: " + command);
        		/* Execute the commands */
        		
        		/* Send the response to the client */
                err = processCommand(command);
                System.out.println(err + " : " + err.ordinal());
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
	private ArrayList<ClientThread> clients;
	private ServerSocket serverSock;
	
	private Integer serverPort = 16587;
	
	public void print(String name) {
		System.out.println("Client " + name + " is connected");
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
		// TODO Auto-generated method stub
		
		//clients = new ArrayList<String>();
		Server server = new Server();
		server.run();
	}
}
