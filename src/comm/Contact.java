package comm;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

public abstract class Contact implements StatusObserver {

	public final Connection connection;
	
	private byte eventMaskR;
	
	private short eventMaskS;
	private int nextEventBit;
	private final Event[] events;
	
	private int ticks;
	private boolean connected = false;
	private boolean active = false;
	
	private Set<Byte> plugins;
	
	private Connect sender;
	
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
	
	public synchronized boolean isConnected() {
		return connected;
	}
	
	public synchronized void connect() {
		connected = true;
	}
	
	public synchronized void lose() {
		connected = false;
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
		nextEventBit = ++nextEventBit % Short.SIZE;
	}
	
	public synchronized void resolveEvents(List<Event> l, short mask) {
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
	
	public synchronized short getEventMask() {
		return eventMaskR;
	}
	
	//if you overload this,
	//try not to break anything...
	public synchronized void join(Comm c) {
		active = true;
		if (sender == null) {
			sender = new Connect(c);
			sender.start();
		} else {
			sender.interrupt();
		}
	}
	
	public synchronized void deactivate() {
		active = false;
		connected = false;
		if (sender != null) {
			sender.interrupt();
		}
	}
	
	public synchronized void ping(int t) {
		if (sender != null)
			sender.ping(t);
	}
	
	private class Connect extends Thread {
		private final Comm comm;
		private int pingTime = -1;
		private int timeAtPing;
		private int ping;
		
		private int tick = 0;
		
		private Connect(Comm comm) {
			this.comm = comm;
		}
		
		public synchronized void ping(int t) {
			pingTime = t;
			timeAtPing = comm.time();
		}
		
		public void run() {
			while (isActive()) {
				try {
					String attempt = "joining....";
					ByteBuffer joinPacket = comm.getData();
					
					for (int t=0; t<4; t++) {
						status(attempt.substring(0, attempt.length()-3+t));
						comm.send(joinPacket,connection);
						Thread.sleep(comm.JOIN_TIME_DELAY);
					}
					
					attempt = "workaround....";
					ByteBuffer waPacket = comm.makeBuffer();
					waPacket.put(Comm.NAT_WORKAROUND_REQUEST_BYTE);
					connection.toBytes(waPacket);
					waPacket.put(joinPacket.array(),1,joinPacket.limit()-1);
					waPacket.flip();
					
					for (int t=0; t<4; t++) {
						status(attempt.substring(0, attempt.length()-3+t));
						comm.send(waPacket,comm.NPSERVER);
						Thread.sleep(comm.JOIN_TIME_DELAY);
					}
				} catch (InterruptedException e) {}
				
				while (isConnected()) {
					ByteBuffer data = comm.makeBuffer();
					data.put(Comm.DATA_BYTE);
					data.position(Integer.SIZE*2);
					data.putShort(getEventMask());
					comm.poll(Contact.this,data);
					data.flip();
					
					data.position(1);
					synchronized (this) {
						if (pingTime > 0) {
							data.putInt((comm.time()-timeAtPing) + pingTime);
							pingTime = -1;
						} else {
							data.putInt(-1);
						}
					}
					data.putInt(comm.time());
					
					comm.send(data);
					
					try {
						sleep(comm.DATA_FAST_TIME_DELAY);
					} catch (InterruptedException e) {}
				}
			}
		}
	}
}
