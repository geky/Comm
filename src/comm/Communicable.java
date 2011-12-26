package comm;

import java.nio.ByteBuffer;

public interface Communicable {
	//all methods are accessed asynchronously
	
	//returns the a contact from c with the info from b
	//or returns null if it does not accept the connection
	public void makeContact(Connection c, ByteBuffer initData);
	public void ackContact(Contact s, ByteBuffer initData);
	public void setOwnerConnection(Connection c, boolean checked);
	
	public void getInitData(ByteBuffer b);
	
	public void pollData(Contact s, ByteBuffer b);
	public void doData(Contact s, ByteBuffer b);
	
	public void doEvent(Contact s, Event e);	
}
