package comm;

import java.nio.ByteBuffer;

public interface ContactControl {
	//all methods must be synchronized
	
	//returns the a contact from c with the info from b
	//or returns null if it does not accept the connection
	public Contact makeContact(Connection c, ByteBuffer b);
	
	public void status(String s);
	public void error(String s);
	
	public void setOwnerConnection(Connection c, boolean checked);
}
