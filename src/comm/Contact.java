package comm;

import java.util.Arrays;

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
}
