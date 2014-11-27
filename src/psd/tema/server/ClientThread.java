package psd.tema.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import utils.Error;
import utils.Node;
import utils.Pair;

public class ClientThread extends Thread {
	private static final String res			= "resources/";
    private static Map<String, Right> accessPolicy = null;

	private Socket 							socket		= null;
	private AuthenticateAndAuthorizeServer 	authServer 	= null;
	private Database db;
	private static final String adminName 	  = "root";
	private static final String adminPassword = "student";
	
	public ClientThread(Socket socket) {
		this.socket 	= socket;
		this.db			= new Database();
		this.authServer = new AuthenticateAndAuthorizeServer(db);
	}
	public ClientThread(Socket socket, AuthenticateAndAuthorizeServer authServer) {
		this.socket		= socket;
		this.authServer = authServer;
		this.db			= new Database();
	}
	public ClientThread(Socket socket, AuthenticateAndAuthorizeServer authServer, Database db) {
		this.socket		= socket;
		this.authServer = authServer;
		this.db 		= db;
	}
	
	/**
	 * Check credentials in the database
	 * 
	 * @param username
	 * @param password
	 * @return Error.OK if authentication was successful
	 */
	private Error checkCredentials(String username, String password) {
		try {
			Integer resp = authServer.authenticate(username, password);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			return Error.INVALID_CREDENTIALS;
		}
			
		return Error.OK;
	}
	private Error checkAdminCredentials(String username, String password) {
		try {
			Integer resp = authServer.authenticateAdmin(username, password);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			return Error.INVALID_CREDENTIALS;
		}
		return Error.OK;
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
	private Error checkAccessOLD(String username, String resourceName,
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
		PrintWriter pw;
		
		f = new File(res + resName);
		if (f.exists())
			return Error.FILE_EXISTS;
		
		/* A user can create a resource if he's the owner or has WRITE
		 * permission on the parent directory 
		 */
		ret = authServer.checkAccess(username, resName, null, true);
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
		
		ret = authServer.checkAccess(username, resourceName, Access.READ.name(), false);
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
				e.printStackTrace();
			} catch (IOException e) {
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
        	
        ret = authServer.checkAccess(username, resourceName, Access.WRITE.name(), false);
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
        
        ret = authServer.checkAccess(username, resourcePath, Access.WRITE.name(), false);
        if (ret != Error.OK)
            return ret;
        
        /* Delete the file or folder and remove it from the access policy */
    	if (path.isDirectory() && path.list().length > 0) {
    		return Error.FOLDER_NOT_EMPTY;
    	}
    	db.deleteResource(resourcePath);
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
	private Error changeRights(	String username, String resourceName,
								String roleName, String newRights) {
    	String resource;
    	Error err = Error.OK;
    	
    	/* Check whether the file exists or not */
        File  f = new File(res + resourceName);
        if (!f.exists())
        	return Error.FILE_NOT_FOUND;
    	
    	Integer ind = resourceName.indexOf('/');
        if (ind == -1) {
        	resource = resourceName;
        } else {
        	resource = resourceName.substring(0,ind);
        }
        /* Check whether the resource is in the user's home
         * If not, the user cannot change the file rights
         */
        if (!resource.equals(username))
            return Error.ACCESS_DENIED;
        
        /* the owner can set the desired access for the other users */
        db.addRightToRes(resourceName, roleName, newRights);
        
        if (f.isDirectory())
        	db.resetFilesInFolder(resourceName);
        return err;
    }
	
	private Error addRightsToRes(String roleName, String resourceName, String right) {
		db.addRightToRes(resourceName, roleName, right);
		
		return Error.ACCESS_DENIED;
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
    		if (tokens.length < 6)
    			return Error.UNKNOWN_COMMAND;
    		fileName = processFileName(tokens[3]);
            err 	 = changeRights(tokens[0], fileName, tokens[4], tokens[5]);
    		break;
    	case "add":
    		if (tokens.length < 6)
    			return Error.UNKNOWN_COMMAND;
    		fileName = processFileName(tokens[3]);
//            err 	 = addRights(tokens[0], fileName, tokens[4], tokens[5]);
    		break;
    	case "showRoles":
    		boolean all = true;
    		if (tokens.length < 6)
    			return Error.UNKNOWN_COMMAND;
    		switch (tokens[3]) {
    		case "existing":
    			all = true;
    			break;
    		case "own":
    			all = false;
    			break;
    		default:
    			return Error.UNKNOWN_COMMAND;
    		}
			Pair<Error, ArrayList<String>> retval = getRoles(tokens[0], all);
            ArrayList<String> cont = retval.getSecond();
        	
            err = retval.getFirst();
            if (cont != null && cont.size() > 0) {
            	send.println(cont.size());
        		send.flush();
        		
        		for (String line : cont) {
    	        	send.println(line);
    	        	send.flush();
            	}
    		}
            break;
    	default:
    		return adminProcessCommand(cmd, send);
    	}
    	
    	return err;
    }
	private Pair<Error, ArrayList<String>> getRoles(String userName, boolean all) {
		return new Pair<Error, ArrayList<String>>
					(Error.OK, db.getRoleNamesForUser(userName, all));
	}
	/**
	 * Process the commands received from a client
	 *  
	 * @param cmd Command to be executed
	 * @param send
	 * @return
	 */
	public Error adminProcessCommand(String cmd, PrintWriter send) {
		Error err = Error.OK;
		String []tokens = cmd.split(" ");

		/* The user needs to authenticate + command + resource */
        if (tokens.length < 3)
            return Error.ACCESS_DENIED;
        
        /* Check if the user is recognized in the system */
		err = checkAdminCredentials(tokens[0], tokens[1]);
	    if (err != Error.OK)
	    	return err;
	    
    	switch(tokens[2]) {
    	case "createRole":
    		if (tokens.length < 3)
    			return Error.UNKNOWN_COMMAND;
            createRole(tokens[3]);
    		break;
    	case "deleteRole":
    		if (tokens.length < 4)
    			return Error.UNKNOWN_COMMAND;
            deleteRole(tokens[3]);
    		break;
    	case "createUser":
    		if (tokens.length < 5)
    			return Error.UNKNOWN_COMMAND;
            createUser(tokens[3], tokens[4]);
    		break;
    	case "deleteUser":
    		if (tokens.length < 4)
    			return Error.UNKNOWN_COMMAND;
            deleteUser(tokens[3]);
    		break;
    	case "addRoleToUser":
    		if (tokens.length < 5)
    			return Error.UNKNOWN_COMMAND;
            addRoleToUser(tokens[3], tokens[4]);
    		break;
    	case "delRoleFromUser":
    		if (tokens.length < 5)
    			return Error.UNKNOWN_COMMAND;
            deleteRoleFromUser(tokens[3], tokens[4]);
    		break;
    	default:
    		return Error.UNKNOWN_COMMAND;
    	}
    	return err;
    	
	}
	private boolean addRoleToUser(String roleName, String userName) {
		return db.addRoleToUser(roleName, userName);
	}
	private boolean deleteRoleFromUser(String roleName, String userName) {
		return db.deleteRoleFromUser(roleName, userName);
	}	
	private boolean addRolesToUser(String userName, ArrayList<String> roles) {
		boolean ret = true;
		
		for (String roleName : roles) {
			ret &= db.addRoleToUser(userName, roleName);
		}
		return ret;
	}
	private boolean createUser(String userName, String password) {
		return db.createUser(userName, password);
	}
	private boolean deleteUser(String userName) {
		return db.deleteUser(userName);
	}
	private boolean createUsers(ArrayList<Pair<String, String>> users) {
		boolean ret = true;
		for (Pair<String, String> user : users) {
			ret &= db.createUser(user.getFirst(), user.getSecond());
		}
		return ret;
	}
	private boolean deleteUsers(ArrayList<String> users) {
		boolean ret = true;
		
		for (String userName : users) {
			ret &= db.deleteUser(userName);
		}
		return ret;
	}
	private boolean createRole(String roleName) {
		return db.createRole(roleName);
	}
	private boolean createRoles(ArrayList<String> roles) {
		boolean ret = true;
		for (String roleName : roles) {
			ret &= db.createRole(roleName);
		}
		return ret;
	}
	private boolean deleteRole(String roleName) {
		return db.deleteRole(roleName);
	}
	private boolean deleteRole(ArrayList<String> roles) {
		boolean ret = true;
		for (String roleName : roles) {
			ret &= db.deleteRole(roleName);
		}
		return ret;
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
			//e.printStackTrace();
		}
	}
}

