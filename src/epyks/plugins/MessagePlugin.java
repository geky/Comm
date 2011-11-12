package epyks.plugins;

import java.nio.ByteBuffer;

import comm.Contact;
import comm.Event;

import epyks.PeerPanel;
import epyks.Plugin;


public class MessagePlugin extends Plugin {
	
	public MessagePlugin() {
		setName("Messages");
	}
	
	@Override
	public byte usage() {
		return 0x22;
	}

	@Override
	public void pollData(ByteBuffer b) {}

	@Override
	public void doEvent(Contact s, Event e) {
		// TODO Auto-generated method stub
		
	}

}
