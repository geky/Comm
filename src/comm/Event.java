package comm;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;

import epyks.Epyks;

public class Event implements Comparable {

	public static final byte MAX_PRIORITY = 1;
	public static final byte NORM_PRIORITY = 0;
	public static final byte MIN_PRIORITY = -1;
	
	public final ByteBuffer buffer;
	public byte usage; //changes to these variables may or may not be reflected in the byte buffer
	public byte bit;
	public byte bitval;
	public byte priority = NORM_PRIORITY; 
	protected boolean active = false;
	
	protected Event() {
		buffer = ByteBuffer.allocate(0);
		usage = bit = bitval = 0;
		priority = MIN_PRIORITY;
	}
	
	public Event(byte e, int size, byte p) {
		buffer = ByteBuffer.allocate(size+3);
		buffer.put(Comm.EVENT_BYTE);
		buffer.position(2);
		buffer.put(usage=e);
		bit = bitval = 0;
		priority = p;
	}
	
	public Event(Event e) {
		e.buffer.mark();
		buffer = ByteBuffer.allocate(e.buffer.limit());
		buffer.put(e.buffer);
		buffer.flip();
		e.buffer.reset();
		
		usage = e.usage;
		bit = e.bit;
		bitval = e.bitval;
		
		priority = e.priority;
	}
	
	protected Event(ByteBuffer b) {
		bit = bitval = b.get();
		bitval &= 0x1;
		bit >>>= 0x2;
		usage = b.get();
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

	@Override
	public int compareTo(Object arg) {
		Event e2 = (Event)arg;
		int v;
		
		byte[] arr1 = buffer.array();
		byte[] arr2 = e2.buffer.array();
		
		v = arr1.length - arr2.length;
		if (v != 0)
			return v;
		
		for (int t=2; t<arr1.length; t++) {
			v = arr1[t] - arr2[t];
			if (v != 0)
				return v;
		}
		return 0;
	}
}
