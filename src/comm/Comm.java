package comm;

import java.awt.Color;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class Comm {
	public static final byte JOIN_BYTE = 0x30;
	public static final byte JOIN_ACK_TRUE_BYTE = 0x31;
	public static final byte JOIN_ACK_FALSE_BYTE = 0x3f;
	public static final byte DATA_BYTE = 0x40;
	public static final byte EVENT_BYTE = 0x50;
	public static final byte KEEP_OPEN_BYTE = 0x0;
	public final static byte SERVER_REQUEST_BYTE = 0x70;
	public final static byte SERVER_REPLY_BYTE = 0x60;
	
	public final Connection NPSERVER = null; //TODO assign these
	public final Connection NPSERVER_UNUSED = null;
	
	public final int BUFFER_SIZE = 512; //TODO adjust this?
	public final int TIME_DELAY = 1000; //TODO
	public final int DEFAULT_PORT;
	private final DatagramSocket SOCKET;
	
	private volatile int synchTime;
	private Thread syncher;
	
	private final ContactMaker source;
	private final Map<Connection,Contact> contacts = new HashMap<Connection,Contact>();
	private final Map<Connection,Thread> joiners = new HashMap<Connection,Thread>();
	
	private boolean verbose = false;
	
	public Comm(ContactMaker cm) throws SocketException {
		SOCKET = new DatagramSocket();
		DEFAULT_PORT = SOCKET.getPort();
		source = cm;
		
		synch();
	}
	
	public Comm(int defaultPort, ContactMaker cm) throws SocketException {
		DEFAULT_PORT = defaultPort;
		source = cm;
		
		DatagramSocket sock;
		try {
			sock = new DatagramSocket(defaultPort);
		} catch (SocketException e) {
			sock = new DatagramSocket();
		} 
		SOCKET = sock;
		
		synch();
	}
	
	public Comm(int defaultPort, int defaultRange, ContactMaker cm) throws SocketException {
		DEFAULT_PORT = defaultPort;
		source = cm;
		
		DatagramSocket sock = null;
		for (int t=0; t<defaultRange && sock==null; t++) {
			try {
				sock = new DatagramSocket(defaultPort++); 
			} catch (SocketException e) {
				sock = null;
			}
		}
		if (sock == null)
			sock = new DatagramSocket();
		SOCKET = sock;
		
		synch();
	}
	
	public void synch() {
		ByteBuffer request = makeBuffer();
		request.put(SERVER_REQUEST_BYTE).flip();
		syncher = new Repeater(NPSERVER,request);
		syncher.start();
	}
	
	public ByteBuffer makeBuffer() {
		return ByteBuffer.allocate(BUFFER_SIZE);
	}
	
	public int time(Contact c) {
		return (int)(System.currentTimeMillis() + synchTime - c.getTimeOrigin());
	}
	
	private class Repeater extends Thread {
		Connection c;
		ByteBuffer b;
		
		public Repeater(Connection tc, ByteBuffer tb) {
			c = tc;
			b = tb;
		}
		
		public void run() {		
			c.resolve();
			
			for (int t=0; t<4; t++) {
				send(b,c);
				try {
					Thread.sleep(TIME_DELAY);
				} catch (InterruptedException e) {
					return;
				}
			}
		}
	}
	
	public void send(ByteBuffer b) {
		DatagramPacket dp = new DatagramPacket(b.array(),b.limit(),null,DEFAULT_PORT);
		
		if (verbose)
			dump("Sent to all Peers", b.array(), b.limit());
		
		synchronized (contacts) {
			for (Map.Entry<Connection,Contact> p : contacts.entrySet()) {
				if (p.getValue().isActive()) {
					p.getKey().setDestination(dp);
					try {
						SOCKET.send(dp);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public void send(ByteBuffer b,Connection dest) {
		DatagramPacket dp = new DatagramPacket(b.array(),b.limit(),null,DEFAULT_PORT);
		dest.setDestination(dp);
		
		if (verbose)
			dump("Sent to " + dest.toString(), b.array(), b.limit());
		
		try {
			SOCKET.send(dp);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void dump(String m, byte[] bs, int len) {
		while (m.length() < 28) {
			m += "   ";
		}
		System.out.print(m + "\tintent: ");
		switch (bs[0]) {
			case JOIN_BYTE: System.out.print("JOIN          "); break;
			case JOIN_ACK_TRUE_BYTE: System.out.print("JOIN_ACK_TRUE "); break;
			case JOIN_ACK_FALSE_BYTE: System.out.print("JOIN_ACK_FALSE"); break;
			case EVENT_BYTE: System.out.print("EVENT         "); break;
			case DATA_BYTE: System.out.print("DATA          "); break;
			case SERVER_REQUEST_BYTE: System.out.print("SERVER_REQUEST"); break;
			case SERVER_REPLY_BYTE: System.out.print("SERVER_REPLY  "); break;
			case KEEP_OPEN_BYTE: System.out.print("KEEP_OPEN     "); break;
			default: System.out.print("NO_INTENT     "); break;
		}
		
		System.out.print("\ttime: " + System.currentTimeMillis() + "\tdata: [" + (Integer.toHexString((bs[0] & 0xf0) >> 0x4) + Integer.toHexString(bs[0] & 0x0f).toUpperCase()));
		
		for (int t=1; t<len; t++) {
			System.out.print((" " + Integer.toHexString((bs[t] & 0xf0) >> 0x4) + Integer.toHexString(bs[t] & 0x0f)).toUpperCase());
		}
		System.out.println("]");
	}
	
	public void add(final Contact c) {
		c.lose();
		synchronized (contacts) {
			contacts.put(c.connection,c);
		}
		
		long time = System.currentTimeMillis() + synchTime;
		long oldTime = c.getTimeOrigin();
		if (oldTime < time)
			time = oldTime;
		
		final ByteBuffer reply = makeBuffer();
		reply.put(JOIN_BYTE);
		reply.putLong(time);
		reply.flip();
		
		Thread temp = new Repeater(c.connection,reply);
		temp.start();
		
		synchronized (joiners) {
			joiners.put(c.connection,temp);
		}
	}
	
	public void remove(Contact c) {
		synchronized (joiners) {
			joiners.get(c.connection).interrupt();
		}
		synchronized (contacts) {
			contacts.remove(c);
		}
	}
	
	private class Reciever extends Thread {		
		public void run() {
			while (true) {
				ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
				DatagramPacket packet = new DatagramPacket(buffer.array(), BUFFER_SIZE);
		        try {
					SOCKET.receive(packet);
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
		        
		        Connection c = new Connection(packet);
		        byte head = buffer.get();
		        
		        if (verbose) {
		        	byte[] temp = buffer.array();
		        	int len = buffer.limit();
		        	while (len > 0  && temp[len-1] == 0) {
		        		len--;
		        	}
					dump("Recieved from " + c.toString(), temp, len);
		        }
		        
		        switch (head) {
		        	case JOIN_BYTE: join(buffer,c); continue;
		        	case JOIN_ACK_TRUE_BYTE: joinAckTrue(buffer, c); continue;
		        	case JOIN_ACK_FALSE_BYTE: joinAckFalse(buffer, c); continue;
		        	case EVENT_BYTE: event(buffer,c); continue;
		        	case DATA_BYTE: data(buffer,c); continue;
		        	case SERVER_REQUEST_BYTE: serverRequest(buffer,c); continue;
		        	case SERVER_REPLY_BYTE: serverReply(buffer,c); continue;
		        }
			}
		}
		
		private void join(ByteBuffer b, Connection c) {
			Contact sender;
			synchronized (contacts) {
				sender = contacts.get(c);
			
				if (sender != null) {
					long time = b.getLong();
					synchronized (sender) {
						long oldTime = sender.getTimeOrigin();
						if (oldTime < time)
							time = oldTime;
						sender.reset(time);
						sender.activate();
					}
					
					ByteBuffer reply = makeBuffer();
					reply.put(JOIN_ACK_TRUE_BYTE).putLong(time).flip();
					send(reply,c);
					return;
				}
				
				Contact temp = source.makeContact(c, b);
				if (temp != null) {
					long time = b.getLong();
					temp.reset(time);
					contacts.put(c, temp);
					
					ByteBuffer reply = makeBuffer();
					reply.put(JOIN_ACK_TRUE_BYTE).putLong(time).flip();
					send(reply,c);
				} else {
					ByteBuffer reply = makeBuffer();
					reply.put(JOIN_ACK_FALSE_BYTE).flip();
					send(reply,c);
				}
			}
		}
		
		private void joinAckTrue(ByteBuffer b, Connection c) {
			Contact con;
			synchronized (joiners) {
				joiners.get(c).interrupt();
			}
			synchronized (contacts) {
				con = contacts.get(c);
			}
			synchronized (con) {
				long time = b.getLong();
				long oldTime = con.getTimeOrigin();
				if (oldTime < time)
					time = oldTime;
				con.reset(time);
				con.activate();
			}
		}
		
		private void joinAckFalse(ByteBuffer b, Connection c) {
			synchronized (joiners) {
				joiners.get(c).interrupt();
			}
		}
		
		private void event(ByteBuffer b, Connection c) {
			Contact con;
			synchronized (contacts) {
				con = contacts.get(c);
			}
			if (con == null)
				return;
			
			Event e = new Event(b);
			
			boolean doEvent;
			synchronized (con) {
				doEvent = con.collideEvent(e);
			}
			
			//if (doEvent) 
				//Handle events here; TODO
		}
		
		private void data(ByteBuffer b, Connection c) {
			Contact con;
			synchronized (contacts) {
				con = contacts.get(c);
			}
			if (con == null)
				return;
			
			List<Event> responses;
			synchronized (con) {
				if (!con.isActive())
					con.activate();
				
				responses = con.resolveEvents(b.get());
			}
			
			for (Event r:responses) {
				send(r.buffer,c);
			}
			
			
			//TODO handle events
		}
		
		private void serverRequest(ByteBuffer b, Connection c) { 
			//sure why not let the app run as a server, don't actually know what will happen in this case
			ByteBuffer temp = makeBuffer().putLong(System.currentTimeMillis() + synchTime);
			c.toBytes(temp);
			temp.flip();
			send(temp,c);
		}
		
		private void serverReply(ByteBuffer b, Connection c) {
			try {
				//yessssss, I know it is very risky to not synchronize the "syncher" in any way
				//the "syncher" is actually a thread to get the time from a server, so the naming was probably a bad choice
				//the only reason this is safe is because the only thread that can set syncher to null is this one
				//it may be created in other threads, but the cache for multiple processesors will get flushed with the update
				//of the volatile variable synchtime
				synchTime = (int)(b.getLong()-System.currentTimeMillis());
				if (syncher != null) {
					syncher.interrupt();
					syncher = null;
				}
				Connection i = new Connection(b);
				//me.setConnection(i);
				//TODO set up user
			} catch (UnknownHostException e) {
				//me.status("Nonfunctional IP?", Color.RED);
				e.printStackTrace();
			}
		}
	}
}
