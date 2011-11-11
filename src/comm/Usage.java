package comm;

import java.nio.ByteBuffer;

import epyks.PeerPanel;

public interface Usage {
	public void pollData(ByteBuffer b);
	public void doData(ByteBuffer b);
	public void doEvent(Event e);
	public byte usage();
}
