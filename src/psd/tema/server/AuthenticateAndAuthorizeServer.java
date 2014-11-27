package psd.tema.server;

import java.util.ArrayList;

import utils.Pair;
import utils.Error;

public class AuthenticateAndAuthorizeServer {
	private Database db 	   = null;
	private String 	 adminName = "root";
	
	public AuthenticateAndAuthorizeServer() {
		this.db = new Database();
	}
	public AuthenticateAndAuthorizeServer(Database db) {
		this.db = db;
	}
	
	public Integer authenticate(String userName, String password) throws Exception {
		Integer userId = db.authenticate(userName, password);
		
		if (userId == null)
			throw new Exception("Authentication failed!");
		
		return userId;
	}
	public Integer authenticateAdmin(String userName, String password) throws Exception {
		if (!userName.equals(adminName)) {
			throw new Exception("Authentication failed!");
		}
		return authenticate(userName, password);
	}
	public ArrayList<Integer>getUserRoles(String userName) {
		return db.getRolesForUser(userName);
	}
	public ArrayList<Integer>getUserRoles(Integer userID) {
		return db.getRolesForUser(userID);
	}
	public ArrayList<Pair<Integer, String>> getUsersWithRole(String roleName) {
		return db.getUsersWithRole(roleName);
	}
	public ArrayList<Pair<Integer, String>> getUsersWithRole(Integer roleId) {
		return db.getUsersWithRole(roleId);
	}
	/**
	 * Check whether a user is the owner of a resource
	 * @param userName Owner
	 * @param resourceName Resource
	 * @return
	 */
	public boolean isOwner(String userName, String resourceName) {
		Integer ind = -1;
		String res = "";

		ind = resourceName.indexOf('/');
        if (ind != -1) {
        	res = resourceName.substring(0,ind);
        } else {
        	res = resourceName;
        }
        /* Check whether the resource is in the user's home */
        if (res.equals(userName)) {
            return true;
        }
        return false;
	}
	public Error checkAccess(String resourceName, String requestedAccess,
						     ArrayList<Integer> userRoles) {
		/* Check for read or write access rights */
        Error err = db.getAccessToRes(userRoles, requestedAccess, resourceName);
        
        if (err == Error.OK)
        	return err;
        
        Integer ind = resourceName.lastIndexOf('/');
		if (ind != -1)
			return checkAccess(resourceName.substring(0, ind), requestedAccess, userRoles);
	
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
	public Error checkAccess(Integer userId, String userName, String resourceName,
    						  String requestedAccess, Boolean create) {
		ArrayList<Integer> userRoles = db.getRolesForUser(userId);
		Error err;
		
		if (isOwner(userName, resourceName)) {
            return Error.OK;
        }
		/* A user cannot create files and folders in another users' home */
		if (create)
			return Error.ACCESS_DENIED;
		
		err =  db.getAccessToRes(userRoles, requestedAccess, resourceName);
		if (err != Error.OK) {
			return checkAccess(resourceName, requestedAccess, userRoles);
		}
		
		return err;
	}
	
	public Error checkAccess(String userName,		 String resourceName,
   						  	 String requestedAccess, Boolean create) {
		ArrayList<Integer> userRoles = db.getRolesForUser(userName);
		
		if (isOwner(userName, resourceName)) {
           return Error.OK;
       }
		/* A user cannot create files and folders in another users' home */
		if (create || userRoles.isEmpty())
			return Error.ACCESS_DENIED;
		
		return db.getAccessToRes(userRoles, requestedAccess, resourceName);
	}	
}
