package comm;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Contact implements StatusObserver {

	public final Connection connection;
	
	private byte eventMaskR;
	
	private byte eventMaskS;
	private int nextEventBit;
	private final Event[] events;
	
	private int ticks;
	private boolean active = false;
	
	private Set<Byte> plugins;
	
	private Sender sender;
	
	private int ping;
	
	public Contact(Connection c) {
		connection = c;
		events = new Event[Byte.SIZE];
		nextEventBit = 0;
		eventMaskS = 0;
		eventMaskR = 0;
		Arrays.fill(events, new Event());
		ticks = 0;
	}
	
	public abstract void setData(ByteBuffer b);
	
	public synchronized void setPlugins(Set<Byte> set) {
		plugins = set;
	}
	
	public synchronized boolean hasPlugin(byte b) {
		return plugins.contains(b);
	}
	
	public synchronized boolean isActive() {
		return active;
	}
	
	public synchronized void activate() {
		active = true;
	}
	
	public synchronized void lose() {
		active = false;
	}
	
	public synchronized boolean tick() {
		return ticks++ > 4;
	}
	
	public synchronized void resetTicks() {
		ticks = 0;
	}
	
	public synchronized void reset() {
		nextEventBit = 0;
		eventMaskS = 0;
		eventMaskR = 0;
		Arrays.fill(events, new Event());
		resetTicks();
	}
	
	public synchronized void setEvent(Event e) {
		eventMaskS ^= (0x1 << nextEventBit);
		e.setBit(nextEventBit, eventMaskS);
		events[nextEventBit] = e;
		nextEventBit = ++nextEventBit % Byte.SIZE;
	}
	
	public synchronized void resolveEvents(List<Event> l, byte mask) {
		mask ^= eventMaskS;
		for (int t=0; mask != 0; mask >>>= 0x1,t++) {
    		if ((mask & 0x1) != 0)
    			l.add(events[t]);
    	}
	}
	
	public boolean collideEvent(Event e) {
		if (!e.masked(eventMaskR)) {
			eventMaskR ^= (0x1 << e.bit);
			return true;
		}
		return false;
	}
	
	public synchronized byte getEventMask() {
		return eventMaskR;
	}
	
	//if you overload this,
	//try not to break anything...
	public synchronized void join(Comm c) {
		if (sender == null) {
			sender = new Sender(c);
			sender.start();
		}
	
	}
	
	private class Sender extends Thread {
		private final Comm comm;
		volatile ByteBuffer buffer;
		volatile int task = Comm.JOIN_TASK;
		
		private Sender(Comm comm) {
			this.comm = comm;
		}
		
		public void run() {
			while (true) {//TODO not make this run infinitely
				int temp = task;
				task = Comm.TIMEOUT_DATA_TASK;
				
				switch (temp) {
//					case Comm.JOIN_TASK: joinTask(); break;
//					case Comm.ACK_DATA_TASK: ackTask(); break;
//					case Comm.TIMEOUT_DATA_TASK: timeoutTask(); break;
				}
			}
		}
	}
		
//		void joinTask() {
//			try {
//				String attempt = "joining....";
//				
//				ByteBuffer reply = comm.makeBuffer();
//				reply.put(Comm.JOIN_BYTE);
//				for (byte byt:uses.keySet()) {
//					reply.put(byt);
//				}
//				reply.put((byte)0x0);
//				source.getData(reply);
//				reply.flip();
//								
//				for (int t=0; t<4; t++) {
//					c.status(attempt.substring(0, attempt.length()-3+t));
//					send(reply,c.connection);
//					Thread.sleep(JOIN_TIME_DELAY);
//				}
//				
//				attempt = "workaround....";
//				
//				reply.clear();
//				reply.put(Comm.NAT_WORKAROUND_REQUEST_BYTE);
//				c.connection.toBytes(reply);
//				for (byte byt:uses.keySet()) {
//					reply.put(byt);
//				}
//				reply.put((byte)0x0);
//				source.getData(reply);
//				reply.flip();
//				
//				for (int t=0; t<4; t++) {
//					c.status(attempt.substring(0, attempt.length()-3+t));
//					send(reply,NPSERVER);
//					Thread.sleep(JOIN_TIME_DELAY);
//				}
//				
//				c.status("no response");
//				
//			} catch (InterruptedException e) {
//			} finally {
//				synchronized (joiners) {
//					joiners.remove(c.connection);
//				}
//			}
//		}
//	}
}
