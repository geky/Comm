package comm;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;

import epyks.Epyks;

public class Event {

	public ByteBuffer buffer;
	public int time; //changes to these variables may or may not be reflected in the byte buffer
	public byte usage;
	public byte bit;
	public byte bitval;
	
	protected Event() {
		buffer = ByteBuffer.allocate(0);
		time = usage = bit = bitval = 0;
	}
	
	public Event(byte e, int size) {
		buffer = ByteBuffer.allocate(size);
		buffer.position(10);
		buffer.put(usage=e);
	}
	
	protected Event(ByteBuffer b) {
		time = b.getInt();
		bit = bitval = b.get();
		bitval &= 0x1;
		bit >>>= 0x2;
		usage = b.get();
		buffer = b;
	}
	
	protected void setBit(int b, int mask) {
		int temp = buffer.position();
		buffer.position(6);
		bit = (byte)b;
		bitval = (byte)((mask >>> b) & 0x1);
		buffer.put((byte)(bit<<0x2 & bitval));
		buffer.position(temp);
	}
	
	public boolean masked(int m) {
		return (((m >>> bit) & 0x1) ^ bitval) == 0;
	}
}
