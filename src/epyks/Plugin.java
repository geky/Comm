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
	@Override
	public void setComm(Comm c) {}
	@Override
	public int getMinimumRateDelay() {return -1;}
	@Override
	public int getMaximumRateDelay() {return -1;}
	
	public JPanel settings(Properties p) {return null;}
}
