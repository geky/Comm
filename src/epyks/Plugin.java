package epyks;

import java.nio.ByteBuffer;
import java.util.Properties;

import javax.swing.JComponent;
import javax.swing.JPanel;

import comm.Comm;
import comm.Contact;
import comm.Event;

public abstract class Plugin extends JComponent {

	public abstract byte usage();
	
	public void pollData(Contact s, ByteBuffer b) {}
	public void doData(Contact s, ByteBuffer b) {}
	public void doEvent(Contact s, Event e) {}
	public void setComm(Comm c) {}
	public int getMinimumRateDelay() {return -1;}
	public int getMaximumRateDelay() {return -1;}
	
	public JPanel settings(Properties p) {return null;}
}
