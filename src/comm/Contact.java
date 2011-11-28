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
}
