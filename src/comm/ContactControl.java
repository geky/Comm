package comm;

import java.nio.ByteBuffer;

public interface ContactControl {
	//returns the a contact from c with the info from c
	//or returns null if it does not accept the connection
	public Contact makeContact(Connection c, ByteBuffer b);
	
	public void status(String s);
	public void error(String s);
	
	//every event or data stream is tagged with a byte representing who sent it
	// the byte 0x0 should be reserved as an end of stream byte
	public doData(byte usage, ByteBuffer b);
	public doEvent(byte usage, Event e);
	
	public void setOwnerConnection(Connection c);
}
