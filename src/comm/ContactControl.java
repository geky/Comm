package comm;

import java.nio.ByteBuffer;

public interface ContactControl extends StatusObserver {
	//all methods must be synchronized
	
	//returns the a contact from c with the info from b
	//or returns null if it does not accept the connection
	public Contact makeContact(Connection c, ByteBuffer b);
	
	public void getData(ByteBuffer b);
	
	public void setOwnerConnection(Connection c, boolean checked);
}
