package psd.tema.server;

import java.sql.*;
import java.util.ArrayList;

import utils.Pair;

public class Database {
	Connection conn = null;
	
	private String userName   = "admin";
	private String password   = "student";
	private String dbName     = "psd"; 
	private String url 		  = "jdbc:mysql://localhost:3306/";
	private String jdbcDriver = "com.mysql.jdbc.Driver";
			
	public Database() {}
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
	public Integer authenticate(String userName, String password) {
		String query = " select UserID"	 +
					   " from UserTable" +
					   " where UserName = ? and Password = ?";
		PreparedStatement ps = null;
		Integer userId = null;
		try {
			ps = conn.prepareStatement(query);
			ps.setString(1, userName);
			ps.setString(2, password);
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				System.out.println("---------------");
				userId  = rs.getInt("UserID");
			}
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return userId;
	}
	public Error getAccessToRes(ArrayList<Integer> rolesIds, String access, String resourceName) {
		Array  array;
		String query = " select RightID" +
					   " from ResourceACLTable " +
					   " where RoleID in (?) and RightID = ? and " +
					   "       ResourceID = " +
					   "       (select ResourceID from ResourceTable where ResourceName = ?)";
		PreparedStatement ps = null;
		Error err = Error.ACCESS_DENIED;
		
		try {
			ps	  = conn.prepareStatement(query);
			array = conn.createArrayOf("number", rolesIds.toArray());
			
			ps.setArray(1, array);
			ps.setString(2, access);
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
				return getRolesForUser(userId);
			}
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	public ArrayList<Integer> getRolesForUser(Integer userId) {
		String query = " select RoleID"							+
					   " from   RoleTable"						+
					   " where  RoleID = "						+
					   "	(select RoleID from UserRoleTable"	+
					   "	 where UserID = ?)";
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
	public ArrayList<Pair<Integer, String>>	getRolesForUser2(Integer userId) {
		String query = " select RoleID, RoleName "				+
					   " from RoleTable"						+
					   " where RoleID = "						+
					   "	(select RoleID from UserRoleTable"	+
					   "	 where UserID = ?)";
		PreparedStatement ps = null;
		ArrayList<Pair<Integer, String>> response = 
				new ArrayList<Pair<Integer, String>>();
		
		try {
			ps = conn.prepareStatement(query);
			ps.setInt(1, userId);
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				Integer roleId  = rs.getInt("RoleID");
				String roleName = rs.getString("RoleName");
				
				response.add(new Pair<Integer, String>(roleId, roleName));
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
		// TODO Auto-generated method stub
		return null;
	}
	public void addRight(String rightName) {
		String query = "insert into AccessRightTable (RightName) value (?)";
		PreparedStatement ps = null;

		try {
			ps = conn.prepareStatement(query);
			ps.setString(1, rightName);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
