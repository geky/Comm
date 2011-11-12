package epyks;

import java.nio.ByteBuffer;

import javax.swing.JComponent;

import comm.Contact;
import comm.Event;
import comm.Usage;

public abstract class Plugin extends JComponent implements Usage {

	@Override
	public void pollData(ByteBuffer b) {}
	@Override
	public void doData(Contact s, ByteBuffer b) {}	
}
