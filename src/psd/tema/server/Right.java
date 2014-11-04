package psd.tema.server;


public class Right {
	static final int none	 = 0;
	static final int read	 = 4;
	static final int write 	 = 2;
	static final int execute = 1;
	
	private Integer userAccess;
	
	Right() {
		userAccess = none;
	}
	Right(int accessLevel) {
		if (accessLevel < 0 && accessLevel > 6)
			userAccess = none;
		else
			userAccess = accessLevel;
	}
	
	public Right(Access access) {
		switch (access) {
		case READ:
			userAccess = read;
		case WRITE:
			userAccess = write;
			break;
		case READ_WRITE:
			userAccess = read | write;
			break;
		default:
			userAccess = none;
		}
	}
	public Right(String newRights) {
        if (newRights.equals("RDONLY"))
            userAccess = read;
        else if (newRights.equals("WRONLY"))
            userAccess = write;
        else if (newRights.equals("RDWR"))
            userAccess = read | write;
        else
        	userAccess = none;
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
	public String toString() {
		return "" + userAccess;
	}
}
