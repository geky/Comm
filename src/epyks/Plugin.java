package epyks;

import java.nio.ByteBuffer;
import javax.swing.JComponent;

import comm.Event;

public abstract class Plugin extends JComponent {
	public abstract void pollData(ByteBuffer b);
	public abstract void doData(PeerPanel p, ByteBuffer b);
	public abstract byte getByte();
	public abstract void doEvent(Event e);
}
