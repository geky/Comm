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
	
	private short eventMaskR;
	
	private short eventMaskS;
	private int nextEventBit;
	private final Event[] events;

	private boolean connected = false;
	private Connect sender;
	
	private Set<Byte> plugins;
	private boolean master;
	
	public Contact(Connection c) {
		connection = c;
		events = new Event[Short.SIZE];
	}
	
	public abstract void setData(ByteBuffer b);
	
	protected synchronized void setConnectionData(Set<Byte> set, boolean m) {
		plugins = set;
		master = m;
	}
	
	public synchronized Set<Byte> getPlugins() {
		return plugins;
	}

	public synchronized boolean isMaster() { 
		return master;
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
		lose();
	}
	
	public synchronized void ackTrue() {
		if (sender != null) {
			connected = true;
			sender.ping();
			sender.interrupt();
		}
	}
	
	public synchronized void ackFalse() {
		deactivate();
		status("waiting...");
	}
	
	public synchronized void ping(int t) {
		if (sender != null)
			sender.ping(t);
	}
	
	public synchronized void ping(int t,byte r) {
		if (sender != null)
			sender.ping(t,r);
	}
	
	private class Connect extends Thread {
		private final Comm comm;
		private int pingTime = -1;
		private int timeAtPing;
		private int rtt = 0;
		private int rateDelay;
		
		private Connect(Comm comm) {
			this.comm = comm;
			rateDelay = comm.fastDelay;
		}
		
		public synchronized void ping() {
			timeAtPing = comm.time();
		}
		
		public synchronized void ping(int t) {
			timeAtPing = comm.time();
			pingTime = 0;
			
			if (t < 0) 
				return;
			
			t = timeAtPing - t;
			rtt = (int)((comm.RTT_ALPHA * rtt) + ((1-comm.RTT_ALPHA) * t));
			
			if (rtt > comm.RTT_TIMEOUT) {
				System.out.println("AAAAAAAH");
				rateDelay += rateDelay/2;
				rtt = 0;
			} else {
				rateDelay -= comm.TIME_BLOCK;
			}
			
			if (rateDelay < comm.TIME_BLOCK)
				rateDelay = comm.TIME_BLOCK;
			else if (rateDelay < comm.fastDelay)
				rateDelay = comm.fastDelay;
			
			
			
			System.out.println(t + "  " + rtt + " : " + rateDelay);
		}
		
		public synchronized void ping(int t,int r) {
			timeAtPing = comm.time();
			rateDelay = (r & 0xff) * comm.TIME_BLOCK;
			pingTime = t;
		}
		
		public void run() {
			while (isActive()) {
				if (!isConnected()) {
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
				}
				
				boolean mastering = isMaster();
				
				while (isConnected()) {					
					int tap;
					synchronized (this) {
						tap = comm.time() - timeAtPing;
					}
					
					if (tap > (comm.JOIN_TIME_DELAY*4)) {
						deactivate();
						break;
					}
					
					ByteBuffer data = comm.makeBuffer();
					data.put(Comm.DATA_BYTE);
					data.position(mastering?6:5);
					data.putShort(getEventMask());
					comm.poll(Contact.this,data);
					data.flip();
					
					data.position(1);
					
					int delay;
					synchronized (this) {
						if (mastering) {
							data.putInt(comm.time());
							data.put((byte)(rateDelay/comm.TIME_BLOCK));
							if (pingTime < 0) {
								if (rateDelay < comm.JOIN_TIME_DELAY)
									rateDelay += rateDelay/2;
								else if (rateDelay > comm.JOIN_TIME_DELAY)
									rateDelay = comm.JOIN_TIME_DELAY;
							}
							pingTime = -1;
						} else {
							if (pingTime != -1) {
								data.putInt((comm.time()-timeAtPing) + pingTime);
								pingTime = -1;
							} else {
								data.putInt(-1);
								if (rateDelay < comm.JOIN_TIME_DELAY)
									rateDelay += rateDelay/2;
								else if (rateDelay > comm.JOIN_TIME_DELAY)
									rateDelay = comm.JOIN_TIME_DELAY;
							}
						}
						
						delay = rateDelay;
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
