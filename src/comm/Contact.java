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

public class Contact {

	public final Connection connection;
	
	private short eventMaskR;
	
	private short eventMaskS;
	private int nextEventBit;
	private final Event[] events;

	private boolean connected = false;
	private ConnectThread sender;
	
	//private Set<Byte> plugins;
	private final StatusListener stat;
	
	public Contact(Connection c) {
		this(c,null);
	}
	
	public Contact(Connection c, StatusListener st) {
		connection = c;
		stat = st;
		events = new Event[Short.SIZE];
	}
	
//	public abstract void setData(ByteBuffer b);
//	
//	protected synchronized void setConnectionData(Set<Byte> set) {
//		plugins = set;
//	}
//	
//	public synchronized Set<Byte> getPlugins() {
//		return plugins;
//	}
	
	public synchronized boolean isActive() {
		return sender != null;
	}
	
	public synchronized boolean isConnected() {
		return connected;
	}
	
	//Overload these to observe changes to connection state
	public void join() {};
	public void lose() {};
	
	public synchronized void reset() {
		nextEventBit = 0;
		eventMaskS = 0;
		eventMaskR = 0;
		Arrays.fill(events, new Event());
	}
	
	protected synchronized void setEvent(Event e) {
		eventMaskS ^= (0x1 << nextEventBit);
		e.setBit(nextEventBit, eventMaskS);
		events[nextEventBit] = e;
		nextEventBit = ++nextEventBit % Short.SIZE;
	}
	
	public synchronized void resolveEvents(List<Event> l,int mask) {
		mask ^= eventMaskS;
		mask &= 0xffff;
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

	public synchronized void join(Comm c) {
		if (stat != null) stat.status("resolving...");
		
		lose();
		reset();
		
		if (sender == null) {
			sender = new ConnectThread(c);
			sender.start();
		} else {
			sender.interrupt();
		}
	}
	
	public synchronized void reactivate(Comm c) {
		connected = true;
		if (sender == null) {
			sender = new ConnectThread(c);
			sender.start();
		}
		join();
	}
	
	public synchronized void deactivate() {
		connected = false;
		if (sender != null) {
			sender.interrupt();
		}
		sender = null;
		lose();
	}
	
	public synchronized void ackTrue() {
		if (sender != null) {
			connected = true;
			sender.ping();
			sender.interrupt();
		}
		join();
	}
	
	public synchronized void ackFalse() {
		deactivate();
		if (stat != null) stat.status("waiting...");
	}
	
	public synchronized void ping() {
		if (sender != null)
			sender.ping();
	}
	
	private class ConnectThread extends Thread {
		private final Comm comm;
		private int timeAtPing;
		private boolean wasPinged = true;
		private int rateDelay;
		
		private ConnectThread(Comm comm) {
			this.comm = comm;
			rateDelay = comm.getFastDelay();
		}
		
		public synchronized void ping() {
			timeAtPing = comm.time();
			wasPinged = true;
		}
		
		public void run() {
			while (isActive()) {
				if (!isConnected()) {
					try {
						ByteBuffer joinPacket = comm.makeJoinRequest();
						
						for (int t=0; t<4; t++) {
							if (stat != null) stat.status("joining...".substring(0, 7+t));
							comm.send(joinPacket,connection);
							
							Thread.sleep(comm.JOIN_TIME_DELAY);
						}
						
						if (comm.NPSERVER != null) {
							ByteBuffer waPacket = comm.makeWorkaroundRequest(connection);
							
							for (int t=0; t<4; t++) {
								if (stat != null) stat.status("workaround...".substring(0, 10+t));
								comm.send(waPacket,comm.NPSERVER);
								
								Thread.sleep(comm.JOIN_TIME_DELAY);
							}
						}
						
						deactivate();
					} catch (InterruptedException e) {}
				}
				
				while (isConnected()) {					
					int tap;
					synchronized (this) {
						tap = comm.time() - timeAtPing;
					}
					
					if (tap > (comm.JOIN_TIME_DELAY*4)) {
						deactivate();
						break;
					}
					
					ByteBuffer data = comm.makeDataPoll(Contact.this);
					
					int delay;
					
					int fd,sd;
					synchronized (comm.delayLock) {
						fd = comm.getFastDelay();
						sd = comm.getSlowDelay();
					}
					
					synchronized (this) {
						if (wasPinged) {
							if (rateDelay > fd) {
								rateDelay -= rateDelay * comm.DECREASE_RT;
								
								if (rateDelay < fd)
									rateDelay = fd;
							}	
						} else {
							if (rateDelay < sd) {
								rateDelay += rateDelay * comm.INCREASE_RT;
								
								if (rateDelay > sd)
									rateDelay = sd;
							}
						}
						
						delay = rateDelay;
						wasPinged = false;
					}
					
					comm.send(data,connection);
				
					try {
						sleep(delay);
					} catch (InterruptedException e) {}
				}
			}
		}
	}
}
