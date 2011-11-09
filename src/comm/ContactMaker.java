package comm;

import java.nio.ByteBuffer;

public interface ContactMaker {
	//returns the a contact from c with the info from c
	//or returns null if it does not accept the connection
	public Contact makeContact(Connection c, ByteBuffer b);
}
