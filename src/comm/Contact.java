package comm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class Contact {

	public final Connection connection;
	
	private byte eventMask;
	private final Event[] events;
	
	private int pingTime = -1;
	private long timeOrigin;
	private boolean active = false;
	
	public Contact(Connection c) {
		connection = c;
		events = new Event[Byte.SIZE];
		eventMask = 0;
		Arrays.fill(events, new Event());
		timeOrigin = Long.MAX_VALUE;
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
	
	public synchronized long getTimeOrigin() {
		return timeOrigin;
	}
	
	public synchronized void reset(long time) {
		eventMask = 0;
		Arrays.fill(events, new Event());
		timeOrigin = time;
	}
	
	public synchronized boolean setEvent(Event e) {
		int min = 0;
		for (int t=1; t<events.length; t++) { //we can start at t=1 without checking because events must contain 8 indices
			if (events[t].time == e.time && events[t].event == e.event)
				return false;
			if (events[t].time < events[min].time)
				min = t;
		}
		if (events[min].time > e.time)
			return false;
		
		events[min] = e;
		eventMask ^= 0x1 << min;
		e.setBit(min,eventMask);
		return true;
	}
	
	public synchronized boolean collideEvent(Event e) {
		if (e.masked(eventMask) && events[e.bit].time == e.time && events[e.bit].event == e.event)
			return false;
		
		if (!e.masked(eventMask)) {
			if (e.time > events[e.bit].time) {
				eventMask ^= 0x1 << e.bit;
				events[e.bit] = e;
				return true;
			} else
				return false;
		} else if (e.time < events[e.bit].time) {
			Event temp = events[e.bit];
			eventMask ^= 0x1 << e.bit;
			events[e.bit] = e;
			return setEvent(temp);
		} else {
			return setEvent(e);
		}
	}
	
	public synchronized void resolveEvents(List<Event> l, byte mask) {
		mask ^= eventMask;
		for (int t=0; mask != 0; mask >>>= 0x1,t++) {
    		if ((mask & 0x1) != 0)
    			l.add(events[t]);
    	}
	}
}
