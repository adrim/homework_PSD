package psd.tema.server;

import java.util.ArrayList;

enum RoleEnum {
	
}

public class Role {
	private String roleName = "";
	private ArrayList<Right> rights = new ArrayList<Right>();

	Role(String name) {
		this.roleName = name;
	}
	
	public void associateRights(ArrayList<Right> rights) {
		this.rights = new ArrayList<Right>(rights);
	}
	public void addRight(Right right) {
		this.rights.add(right);
	}
	public ArrayList<Right> getRights() {
		return this.rights;
	}
}