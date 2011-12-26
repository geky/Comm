package comm;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;

import epyks.Epyks;

public class Event {	
	public final ByteBuffer buffer;
	public byte bit; //changes to these variables may or may not be reflected in the byte buffer
	public byte bitval;
	
	protected Event() {
		buffer = ByteBuffer.allocate(0);
		bit = bitval = 0;
	}
	
	public Event(int size) {
		buffer = ByteBuffer.allocate(size);
		buffer.put(Comm.EVENT_BYTE);
		buffer.position(2);
		bit = bitval = 0;
	}
	
	public Event(Event e) {
		e.buffer.mark();
		buffer = ByteBuffer.allocate(e.buffer.limit());
		buffer.put(e.buffer);
		buffer.flip();
		e.buffer.reset();
		
		bit = e.bit;
		bitval = e.bitval;
	}
	
	protected Event(ByteBuffer b) {
		b.position(1);
		bit = bitval = b.get();
		bitval &= 0x1;
		bit >>>= 0x2;
		buffer = b;
	}
	
	protected void setBit(int b, int mask) {
		bit = (byte)b;
		bitval = (byte)((mask >>> b) & 0x1);
		buffer.array()[1] = (byte)(bit<<0x2 | bitval);
	}
	
	public boolean masked(int m) {
		return (((m >>> bit) & 0x1) ^ bitval) == 0;
	}
}
