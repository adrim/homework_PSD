package psd.tema.server;

import java.sql.*;
import java.util.ArrayList;

import utils.Pair;
import utils.Error;

public class Database {
	Connection conn = null;
	
	private String userName   = "admin";
	private String password   = "student";
	private String dbName     = "psd"; 
	private String url 		  = "jdbc:mysql://localhost:3306/";
	private String jdbcDriver = "com.mysql.jdbc.Driver";
			
	public Database() {
		this.initializeDriver();
		this.connect();
	}
	
	public Database(Connection conn) {
		this.conn = conn;
	}
	
	public void initializeDriver() {
		try {
			Class.forName(jdbcDriver);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	public void connect() {
		try {
			conn = DriverManager.getConnection(url + dbName, userName, password);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void closeConnection() {
		try {
			if (conn != null)
				conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void createStartingTables() {
		
	}
	public void createTable(String tableQuery) {
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(tableQuery);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * Create resource with no ACL associated
	 * @param userName The owner of the resource
	 * @param resourceName The name of the resource to be created
	 */
	public void createResource(String resourceName) {
		String query = "insert into ResourceTable (ResourceName) value (?)";
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(query);
			ps.setString(1, resourceName);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public void deleteResource(String resourceName) {
		String query = "delete from ResourceTable where ResourceName = ?";
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(query);
			ps.setString(1, resourceName);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public Integer authenticate(String userName, String password) {
		String query = " select UserID"	 +
					   " from UserTable" +
					   " where UserName = ? and Password = ?";
		PreparedStatement ps = null;
		Integer userId = null;
		
		System.out.println("[Database] Authenticating user '" + userName + "'");
		try {
			ps = conn.prepareStatement(query);
			ps.setString(1, userName);
			ps.setString(2, password);
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				userId  = rs.getInt("UserID");
			}
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return userId;
	}
	
	public Error getAccessToRes(ArrayList<Integer> rolesIds, String access, String resourceName) {
		String query = " select distinct RightID"						+
					   " from ResourceACLTable "						+
					   " where RoleID in (?) and RightID = ? and "		+
					   "       ResourceID = "							+
					   "       (select ResourceID from ResourceTable "	+
					   "		where ? like concat(ResourceName, '%')"	+
					   "		order by length(ResourceName) desc limit 1)";
		Error  err = Error.ACCESS_DENIED;
		String myarr = rolesIds.toString();
		PreparedStatement ps = null;

		try {
			ps	  = conn.prepareStatement(query);
			ps.setString(1, myarr.substring(1, myarr.length()-1));
			ps.setString(2, access.toUpperCase());
			ps.setString(3, resourceName);
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				err =  Error.OK;
			}
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return err;
	}
	public ArrayList<Integer> getRolesForUser(String userName) {
		String query = " select UserID from UserTable where UserName = ?"; 
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(query);
			ps.setString(1, userName);
			
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				Integer userId  = rs.getInt("UserID");
				ps.close();
				return getRolesForUser(userId);
			}
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	public ArrayList<Integer> getRolesForUser(Integer userId) {
		String query = "select RoleID from UserRoleTable where UserID = ?";
		PreparedStatement ps = null;
		ArrayList<Integer> response = 
				new ArrayList<Integer>();
		
		try {
			ps = conn.prepareStatement(query);
			ps.setInt(1, userId);
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				response.add(rs.getInt("RoleID"));
			}
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return response;
	}
	public ArrayList<String>	getRoleNamesForUser(Integer userId) {
		String query = " select RoleName "						+
					   " from   RoleTable  "					+
					   " where RoleID =  "						+
					   "	(select RoleID from UserRoleTable"	+
					   "	 where UserID = ?)";
		PreparedStatement ps = null;
		ArrayList<String> response = 
				new ArrayList<String>();
		
		try {
			ps = conn.prepareStatement(query);
			ps.setInt(1, userId);
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				String roleName = rs.getString("RoleName");
				
				response.add(roleName);
			}
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return response;
	}
	public ArrayList<String>	getRoleNamesForUser(String userName, boolean selectAll) {
		String query = " select RoleName from RoleTable";
		if (!selectAll) {
			query += " where RoleID =  "					+
					 "	(select RoleID from UserRoleTable"	+
					 "	 where UserID = "					+
					 "	      (select UserID from UserTable"+
					 "		   where UserName = ?)";
		}
		PreparedStatement ps = null;
		ArrayList<String> response = 
				new ArrayList<String>();
		
		try {
			ps = conn.prepareStatement(query);
			
			if (!selectAll) {
				ps.setString(1, userName);
			}
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				response.add(rs.getString("RoleName"));
			}
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return response;
	}
	public ArrayList<Pair<Integer, String>>	getUsersWithRole(Integer roleID) {
		String query = " select UserID, UserName "				+
				   	   " from UserTable"						+
				       " where UserID = "						+
				       "	(select UserID from UserRoleTable"	+
				       "	 where RoleID = ?)";
		PreparedStatement ps = null;
		ArrayList<Pair<Integer, String>> response = 
				new ArrayList<Pair<Integer, String>>();
		
		try {
			ps = conn.prepareStatement(query);
			ps.setInt(1, roleID);
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				Integer userId  = rs.getInt("UserID");
				String userName = rs.getString("UserName");
				
				response.add(new Pair<Integer, String>(userId, userName));
			}
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return response;
	}
	public ArrayList<Pair<Integer, String>> getUsersWithRole(String roleName) {
		String query = " select RoleID from RoleTable where RoleName = ?"; 
		PreparedStatement ps = null;
		try {
			ps = conn.prepareStatement(query);
			ps.setString(1, roleName);
			
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				Integer roleId  = rs.getInt("RoleID");
				ps.close();
				
				return getUsersWithRole(roleId);
			}
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	public boolean createRight(String rightName) {
		String query = "insert into AccessRightTable (RightName) value (?)";
		PreparedStatement ps = null;

		try {
			ps = conn.prepareStatement(query);
			ps.setString(1, rightName);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
			
			return false;
		}
		return true;
	}
	public boolean deleteRight(String rightName) {
		String query = "delete from AccessRightTable where RightName = ?";
		PreparedStatement ps = null;

		try {
			ps = conn.prepareStatement(query);
			ps.setString(1, rightName);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
			
			return false;
		}
		return true;
	}
	public void addRightToRes(String resourceName, String roleName, String rightName) {
		CallableStatement cs;

		try {
			cs=conn.prepareCall("{call addRights(?,?,?)}");
			cs.setString(1, resourceName);
			cs.setString(2, roleName);
			cs.setString(3, rightName);
			cs.executeUpdate();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void addRightToRes2(String roleName, String resourceName, String rightName) {
		String query =
				  "insert into ResourceACLTable value"
				+ " (ifnull((select RoleID 	  from RoleTable 	 where RoleName = ?), "
				+ "  (select ResourceID from ResourceTable where ResourceName = ?),"
				+ "  (select RightID    from AccessRightTable where RightName = ?))";
		PreparedStatement ps;
		
		try {
			ps  = conn.prepareStatement(query);

			ps.setString(1, roleName);
			ps.setString(2, resourceName);
			ps.setString(3, rightName);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void resetFilesInFolder(String resourceName) {
		String query = 
				"delete from ResourceTable where ResourceName like ?";
		String like=resourceName+"/%";
		PreparedStatement ps;
		
		try {
			ps  = conn.prepareStatement(query);

			ps.setString(1, like);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public boolean addRoleToUser(String roleName, String userName) {
		String query = " insert into UserRoleTable (UserID, RoleID) values"	 +
					   " ((select UserID from UserTable where UserName = ?)," +
					   "  (select RoleID from RoleTable where RoleName = ?))";
		PreparedStatement ps;
		
		try {
			ps  = conn.prepareStatement(query);
	
			ps.setString(1, userName);
			ps.setString(2, roleName);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
			
			return false;
		}
		return true;
	}
	public boolean deleteRoleFromUser(String roleName, String userName) {
		String query = " delete from UserRoleTable "							  +
					   " where UserID ="										  +
					   "       (select UserID from UserTable where UserName = ?)" +
					   "       and RoleID ="									  +
					   "	    (select RoleID from RoleTable where RoleName = ?)";
		PreparedStatement ps;
		
		try {
			ps  = conn.prepareStatement(query);
	
			ps.setString(1, userName);
			ps.setString(2, roleName);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
			
			return false;
		}
		return true;
	}	
	public boolean createRole(String roleName) {
		String query = "insert into RoleTable(RoleName) values (?)";
		PreparedStatement ps;
		try {
			ps  = conn.prepareStatement(query);
			ps.setString(1, roleName);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public boolean deleteRole(String roleName) {
		String query = "delete from RoleTable where RoleName = ?";
		PreparedStatement ps;
		
		try {
			ps  = conn.prepareStatement(query);
			ps.setString(1, roleName);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;		
	}
	public boolean createUser(String userName, String password) {
		String query = "insert into UserTable(UserName, Password) values (?, ?)";
		PreparedStatement ps;
		try {
			ps  = conn.prepareStatement(query);
	
			ps.setString(1, userName);
			ps.setString(2, password);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public boolean deleteUser(String userName) {
		String query = "delete from UserTable where UserName = ?";
		PreparedStatement ps;
		try {
			ps  = conn.prepareStatement(query);
			ps.setString(1, userName);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
