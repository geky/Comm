package epyks;

import java.nio.ByteBuffer;
import java.util.Properties;

import javax.swing.JComponent;
import javax.swing.JPanel;

import comm.Comm;
import comm.Contact;
import comm.Event;
import comm.Usage;

public abstract class Plugin extends JComponent implements Usage {

	@Override
	public void pollData(Contact s, ByteBuffer b) {}
	@Override
	public void doData(Contact s, ByteBuffer b) {}
	@Override
	public void doEvent(Contact s, Event e) {}
	
	public void setComm(Comm c) {}	
	
	public JPanel settings(Properties p) {return null;}
}
