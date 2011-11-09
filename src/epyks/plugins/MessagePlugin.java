package epyks.plugins;

import java.nio.ByteBuffer;

import comm.Event;

import epyks.PeerPanel;
import epyks.Plugin;


public class MessagePlugin extends Plugin {
	
	public MessagePlugin() {
		setName("Messages");
	}
	
	@Override
	public byte getByte() {
		return 0x22;
	}

	@Override
	public void pollData(ByteBuffer b) {}

	@Override
	public void doEvent(Event e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void doData(PeerPanel p, ByteBuffer b) {
		// TODO Auto-generated method stub
		
	}

}
