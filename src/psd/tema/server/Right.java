package psd.tema.server;


public class Right {
	static final int none	 = 0;
	static final int read	 = 1;
	static final int write 	 = 2;
	static final int execute = 4;
	
	private Integer userAccess;
	
	Right() {
		userAccess = none;
	}
	Right(int accessLevel) {
		if (accessLevel != read && accessLevel != write && accessLevel != execute)
			userAccess = none;
		else
			userAccess = accessLevel;
	}
	
	public boolean canRead() {
		return ((userAccess & read) == 0 ? false : true);
	}
	public boolean canWrite() {
		return ((userAccess & write) == 0 ? false : true);
	}
	public boolean canExecute() {
		return ((userAccess & execute) == 0 ? false : true);
	}
	public boolean hasAccess(Access access) {
		System.out.println("[DEBUG] userAccess: can read " + this.canRead() + 
				"can write " + this.canWrite());
		
		switch(access) {
		case NONE:
			return userAccess == none;
		case READ:
			return canRead();
		case WRITE:
			return canWrite();
/*
		case EXECUTE:
			return canExecute();*/
		default:
			return false;
		}
	}
	public void setRead(boolean on) {
		if (on)
			userAccess |= read;
		else
			userAccess &= (~read);
	}
	public void setWrite(boolean on) {
		if (on)
			userAccess |= write;
		else
			userAccess &= (~write);
	}
	public void setExecute(boolean on) {
		if (on)
			userAccess |= execute;
		else
			userAccess &= (~execute);
	}
	public Integer getAccess() {
		return userAccess;
	}
	public boolean setAccess(Integer accessLevel) {
		if (accessLevel != read && accessLevel != write &&
								   accessLevel != execute) {
			userAccess = none;
			return true;
		}
		return false;
	}
}