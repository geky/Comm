package comm;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;

public abstract class Contact implements StatusObserver {

	public final Connection connection;
	
	private byte eventMaskR;
	
	private short eventMaskS;
	private int nextEventBit;
	private final Event[] events;

	private boolean connected = false;
	private Connect sender;
	
	private Set<Byte> plugins;
	
	public Contact(Connection c) {
		connection = c;
		events = new Event[Byte.SIZE];
	}
	
	public abstract void setData(ByteBuffer b);
	
	public synchronized void setPlugins(Set<Byte> set) {
		plugins = set;
	}
	
	public synchronized Set<Byte> getPlugins() {
		return plugins;
	}
	
	public synchronized boolean isActive() {
		return sender != null;
	}
	
	public synchronized boolean isConnected() {
		return connected;
	}
	
	public abstract void connect();
	
	public abstract void lose();
	
	public synchronized void reset() {
		nextEventBit = 0;
		eventMaskS = 0;
		eventMaskR = 0;
		Arrays.fill(events, new Event());
	}
	
	public synchronized void event(Event e) {
		if (sender != null) 
			sender.event(e);
	}
	
	protected synchronized void setEvent(Event e) {
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
	
	public synchronized boolean collideEvent(Event e) {
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
		status("resolving...");
		
		lose();
		reset();
		
		if (sender == null) {
			sender = new Connect(c);
			sender.start();
		} else {
			sender.interrupt();
		}
	}
	
	public synchronized void reactivate(Comm c) {
		connected = true;
		connect();
		
		if (sender == null) {
			sender = new Connect(c);
			sender.start();
		}
	}
	
	public synchronized void deactivate() {
		connected = false;
		if (sender != null) {
			sender.interrupt();
		}
		sender = null;
	}
	
	public synchronized void ackTrue() {
		if (sender != null) {
			connected = true;
			sender.ping();
			sender.interrupt();
		}
	}
	
	public synchronized void ackFalse() {
		if (sender != null) {
			Thread temp = sender;
			deactivate();
			temp.interrupt();
		}
	}
	
	public synchronized void ping(int t1, int t2) {
		if (sender != null)
			sender.ping(t1,t2);
	}
	
	private class Connect extends Thread {
		private final Comm comm;
		private int pingTime = -1;
		private int timeAtPing;
		private int ping;
		
		private int tick = 0;
		
		private final Queue<Event> minEvents = new LinkedList<Event>();
		private final Queue<Event> normEvents = new LinkedList<Event>();
		private final Queue<Event> maxEvents = new LinkedList<Event>();
		
		private Connect(Comm comm) {
			this.comm = comm;
		}
		
		public synchronized void ping() {
			timeAtPing = comm.time();
		}
		
		public synchronized void ping(int t1, int t2) {
			pingTime = t2;
			timeAtPing = comm.time();
		}
		
		public synchronized void event(Event e) {
			switch (e.priority) {
				case Event.MAX_PRIORITY : maxEvents.add(e); return;
				case Event.NORM_PRIORITY : normEvents.add(e); return;
				case Event.MIN_PRIORITY : minEvents.add(e); return;
			}
		}
		
		public synchronized Event nextTask(int tap) {
			if (!maxEvents.isEmpty())
				return maxEvents.poll();
			else if (!normEvents.isEmpty() && tap < comm.DATA_SLOW_TIME_DELAY)
				return normEvents.poll();
			else if (!minEvents.isEmpty() && tap < comm.DATA_FAST_TIME_DELAY*2)
				return minEvents.poll();
			else
				return null;
		}
		
		public void run() {
			while (isActive()) {
				try {
					String attempt = "joining....";
					ByteBuffer joinPacket = comm.makeBuffer();
					joinPacket.put(Comm.JOIN_BYTE);
					comm.getData(joinPacket);
					joinPacket.flip();
					
					for (int t=0; t<4; t++) {
						status(attempt.substring(0, attempt.length()-3+t));
						comm.send(joinPacket,connection);
						
						Thread.sleep(comm.JOIN_TIME_DELAY);
					}
					
					attempt = "workaround....";
					ByteBuffer waPacket = comm.makeBuffer();
					waPacket.put(Comm.NAT_WORKAROUND_REQUEST_BYTE);
					connection.toBytes(waPacket);
					comm.getData(waPacket);
					waPacket.flip();
					
					for (int t=0; t<4; t++) {
						status(attempt.substring(0, attempt.length()-3+t));
						comm.send(waPacket,comm.NPSERVER);
						
						Thread.sleep(comm.JOIN_TIME_DELAY);
					}
					
					deactivate();
				} catch (InterruptedException e) {}
				
				while (isConnected()) {
					int tap;
					Event next;
					synchronized (this) {
						tap = comm.time() - timeAtPing;
						next = nextTask(tap);
					}
					
					if (tap > (comm.JOIN_TIME_DELAY*4)) {
						deactivate();
						break;
					} 
					
					if (next != null) {
						setEvent(next);
						comm.send(next.buffer,connection);
					} else {
						ByteBuffer data = comm.makeBuffer();
						data.put(Comm.DATA_BYTE);
						data.position(9);
						data.putShort(getEventMask());
						comm.poll(Contact.this,data);
						data.flip();
						
						data.position(1);
					
						synchronized (this) {	
							if (pingTime != -1) {
								data.putInt((comm.time()-timeAtPing) + pingTime);
								pingTime = -1;
							} else {
								data.putInt(-1);
							}
						}
						data.putInt(comm.time());
						
						comm.send(data,connection);
					}
					
					try {
						sleep(comm.DATA_FAST_TIME_DELAY);
					} catch (InterruptedException e) {}
				}
			}
		}
	}
}
