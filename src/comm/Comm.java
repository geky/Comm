package comm;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;


public class Comm {
	public static final byte JOIN_BYTE = 0x30;
	public static final byte JOIN_ACK_TRUE_BYTE = 0x31;
	public static final byte JOIN_ACK_FALSE_BYTE = 0x3f;
	public static final byte DATA_BYTE = 0x40;
	public static final byte EVENT_BYTE = 0x50;
	public static final byte KEEP_OPEN_BYTE = 0x0;
	public final static byte SERVER_REQUEST_BYTE = 0x70;
	public final static byte SERVER_REPLY_BYTE = 0x60;
	
	public final Connection NPSERVER;
	public final Connection NPSERVER_UNUSED;
	
	public final int BUFFER_SIZE;
	public final int TIME_DELAY;
	public final int DEFAULT_PORT;
	public final boolean DUMP_PACKETS;
	private final DatagramSocket SOCKET;
	
	private Thread syncher;
	
	private final ContactControl source;
	private final Map<Connection,Contact> contacts = new HashMap<Connection,Contact>();
	private final Map<Connection,Thread> joiners = new HashMap<Connection,Thread>();
	
	private final Map<Byte,? extends Usage> uses;
	
	public Comm(ContactControl cm, Map<Byte,? extends Usage> u, Properties p) throws SocketException {
		source = cm;
		
		uses = Collections.unmodifiableMap(u);
		
		BUFFER_SIZE = Integer.parseInt(p.getProperty("buffer_size","512"));
		TIME_DELAY = Integer.parseInt(p.getProperty("time_delay","1000"));
		DUMP_PACKETS = Boolean.parseBoolean(p.getProperty("dump_packets", "false"));
		
		String port = p.getProperty("default_port");
		String portRange = p.getProperty("default_range");
		if (port == null) {
			SOCKET = new DatagramSocket();
			DEFAULT_PORT = SOCKET.getPort();
		} else if (portRange == null) {
			DatagramSocket sock;
			DEFAULT_PORT = Integer.parseInt(port);
			try {
				sock = new DatagramSocket(DEFAULT_PORT);
			} catch (SocketException e) {
				sock = new DatagramSocket();
			}
			SOCKET = sock;
		} else {
			DEFAULT_PORT = Integer.parseInt(port);
			int defaultPortRange = Integer.parseInt(portRange);
			
			DatagramSocket sock = null;
			for (int t=0; t<defaultPortRange && sock==null; t++) {
				try {
					sock = new DatagramSocket(DEFAULT_PORT+t); 
				} catch (SocketException e) {
					sock = null;
				}
			}
			if (sock == null)
				sock = new DatagramSocket();
			SOCKET = sock;
		}
		
		Connection npserver;
		Connection npunused;
		try {
			String s = p.getProperty("server","127.0.0.1:"+DEFAULT_PORT);
			npserver = new Connection(s);
			npunused = new Connection(p.getProperty("server_unused",s.split(":")[0]+":" + (npserver.port-1)));
		} catch (UnknownHostException e) {
			try {
				npserver = new Connection("127.0.0.1:"+DEFAULT_PORT);
				npunused = new Connection("127.0.0.1:"+(DEFAULT_PORT-1));
			} catch (UnknownHostException e2) {
				npunused = npserver = null;
			}
		}
		NPSERVER = npserver;
		NPSERVER_UNUSED = npunused;
	}
	
	public void start() {
		if (NPSERVER != null)
			synch();
		new Sender().start();
		new Reciever().start();
	}
	
	public void synch() {
		source.status("resolving...");
		
		final ByteBuffer request = makeBuffer();
		request.put(SERVER_REQUEST_BYTE).flip();
		
		synchronized (joiners) {
			syncher = new Thread() {
				public void run() {					
					String attempt = "synching....";
					
					for (int t=0; t<4; t++) {
						source.status(attempt.substring(0, attempt.length()-3+t));
						send(request,NPSERVER);
						try {
							Thread.sleep(TIME_DELAY);
						} catch (InterruptedException e) {
							return;
						}
					}
					
					synchronized (joiners) {
						for (Thread t:joiners.values()) {
							t.start();
						}
					}
				}
			};
			syncher.start();
		}
	}
	
	public ByteBuffer makeBuffer() {
		return ByteBuffer.allocate(BUFFER_SIZE);
	}
	
	public Event makeEvent(byte usage) {
		return new Event(usage, BUFFER_SIZE);
	}
	
//	private class Joiner extends Thread {
//		Connection c;
//		ByteBuffer b;
//		
//		public Joiner(Connection tc, ByteBuffer tb) {
//			c = tc;
//			b = tb;
//		}
//		
//		public void run() {		
//			c.resolve();
//			
//			for (int t=0; t<4; t++) {
//				send(b,c);
//				try {
//					Thread.sleep(TIME_DELAY);
//				} catch (InterruptedException e) {
//					return;
//				}
//			}
//		}
//	}
	
	
	
//	public void send(ByteBuffer b) {
//		DatagramPacket dp = new DatagramPacket(b.array(),b.limit(),null,DEFAULT_PORT);
//		
//		if (DUMP_PACKETS)
//			dump("Sent to all Peers", b.array(), b.limit());
//		
//		synchronized (contacts) {
//			for (Map.Entry<Connection,Contact> p : contacts.entrySet()) {
//				if (p.getValue().isActive()) {
//					p.getKey().setDestination(dp);
//					try {
//						SOCKET.send(dp);
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
//			}
//		}
//	}
	
	public void send(ByteBuffer b,Contact c) {
		send(b,c.connection);
	}
	
	public void send(ByteBuffer b,Connection dest) {
		DatagramPacket dp = dest.makePacket(b);
		
		if (DUMP_PACKETS)
			dump("Sent to " + dest.toString(), b.array(), b.limit());
		
		try {
			SOCKET.send(dp);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendEvent(Event e, Contact c) {
		c.setEvent(e);
		send(e.buffer,c.connection);
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
	
	public class Sender extends Thread {
		
		@Override
		public void run() {
			while (true) {
				
				boolean foreverAlone = true;
				synchronized (contacts) {
					for (Contact c : contacts.values()) {
						synchronized (c) {
							if (c.isActive()) {
								if (c.tick())
									c.lose();
								else
									foreverAlone = false;
							}
						}
					}
				}
				
				if (foreverAlone) {
					ByteBuffer data = makeBuffer();
					data.put(KEEP_OPEN_BYTE);
					data.flip();
					send(data,NPSERVER_UNUSED);
				} else {
					ByteBuffer data = makeBuffer();
					data.put(DATA_BYTE);
					data.position(2);
					
					for (Entry<Byte,? extends Usage> entry:uses.entrySet()) {
						data.put(entry.getKey());
						entry.getValue().pollData(data);
					}
					if (data.hasRemaining())
						data.put((byte)0x0);
					data.flip();
					
					synchronized (contacts) {
						for (Contact c:contacts.values()) {
							data.position(1);
							data.put(c.getEventMask());
							send(data,c.connection);
						}
					}
				}
				
				try {
					sleep(TIME_DELAY);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void add(final Contact c) {
		c.status("resolving...");
		c.lose();
		synchronized (contacts) {
			contacts.put(c.connection,c);
		}
		
		final ByteBuffer reply = makeBuffer();
		reply.put(JOIN_BYTE);
		reply.flip();
		
		Thread temp = new Thread() {
			public void run() {	
				String attempt = "joining....";
				
				for (int t=0; t<4; t++) {
					source.status(attempt.substring(0, attempt.length()-3+t));
					send(reply,c.connection);
					try {
						Thread.sleep(TIME_DELAY);
					} catch (InterruptedException e) {
						return;
					}
				}
			}
		};
		
		synchronized (joiners) {
			joiners.put(c.connection,temp);
			if (syncher != null)
				temp.start();
		}
	}
	
//	private class Joiner extends Thread {
//		Connection c;
//		ByteBuffer b;
//		
//		public Joiner(Connection tc, ByteBuffer tb) {
//			c = tc;
//			b = tb;
//		}
//		
//		@Override
//		public void run() {		
//			c.resolve();
//			
//			for (int t=0; t<4; t++) {
//				send(b,c);
//				try {
//					Thread.sleep(TIME_DELAY);
//				} catch (InterruptedException e) {
//					return;
//				}
//			}
//		}
//	}
	
	public void remove(Contact c) {
		synchronized (joiners) {
			joiners.get(c.connection).interrupt();
		}
		synchronized (contacts) {
			contacts.remove(c);
		}
	}
	
	private class Reciever extends Thread {	
		
		@Override
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
		        
		        if (DUMP_PACKETS)
					dump("Recieved from " + c.toString(), buffer.array(), packet.getLength());
		        
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
			
				if (sender == null) {
					Contact temp = source.makeContact(c, b);
					if (temp != null) {
						temp.reset();
						temp.activate();
						contacts.put(c, temp);
						
						ByteBuffer reply = makeBuffer();
						reply.put(JOIN_ACK_TRUE_BYTE).flip();
						send(reply,c);
					} else {
						ByteBuffer reply = makeBuffer();
						reply.put(JOIN_ACK_FALSE_BYTE).flip();
						send(reply,c);
					}
					return;
				}
			}
			
			synchronized (sender) {
				sender.reset();
				sender.activate();
			}
			
			ByteBuffer reply = makeBuffer();
			reply.put(JOIN_ACK_TRUE_BYTE).flip();
			send(reply,c);
			
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
				con.reset();
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
			
			if (doEvent) 
				uses.get(e.usage).doEvent(con,e);
		}
		
		private List<Event> bufferOfResolvedEvents = new ArrayList<Event>(Byte.SIZE);
		//only used in thread to prevent eccesive memory allocation
		
		private void data(ByteBuffer b, Connection c) {
			Contact con;
			synchronized (contacts) {
				con = contacts.get(c);
			}
			if (con == null)
				return;
			
			synchronized (con) {
				if (!con.isActive())
					con.activate();
				con.resetTicks();
				
				con.resolveEvents(bufferOfResolvedEvents,b.get());
			}
			
			for (Event r:bufferOfResolvedEvents) {
				send(r.buffer,c);
			}
			bufferOfResolvedEvents.clear();
			
			byte usage;
			while (b.hasRemaining() && (usage = b.get()) != 0x0) {
				Usage use = uses.get(usage);
				if (use != null) {
					use.doData(con,b);
				} else {
					b.position((b.getShort() & 0xffff)+b.position());
				}
			}
		}
		
		private void serverRequest(ByteBuffer b, Connection c) { 
			//sure why not let the app run as a server, don't actually know what will happen in this case
			ByteBuffer temp = makeBuffer().put(SERVER_REPLY_BYTE);
			temp.putLong(System.currentTimeMillis());
			c.toBytes(temp);
			temp.flip();
			send(temp,c);
		}
		
		private void serverReply(ByteBuffer b, Connection c) {
			try {
				synchronized (joiners) {
					if (syncher != null) {
						syncher.interrupt();
						syncher = null;
					}
				}
				
				b.position(b.position()+8);
				Connection i = new Connection(b);
				
				source.setOwnerConnection(i);
			} catch (UnknownHostException e) {
				source.error("Unusable IP?");
				e.printStackTrace();
			}
		}
	}
}
