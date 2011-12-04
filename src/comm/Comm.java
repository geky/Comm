package comm;

import java.io.IOException;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Properties;


public class Comm {
	public static final byte JOIN_BYTE = 0x30;
	public static final byte JOIN_ACK_TRUE_BYTE = 0x31;
	public static final byte JOIN_ACK_FALSE_BYTE = 0x3f;
	public static final byte DATA_BYTE = 0x40;
	public static final byte EVENT_BYTE = 0x50;
	public static final byte KEEP_OPEN_BYTE = 0x0;
	public static final byte SERVER_REQUEST_BYTE = 0x70;
	public static final byte SERVER_REPLY_BYTE = 0x60;
	public static final byte NAT_WORKAROUND_REQUEST_BYTE = 0x71;
	public static final byte NAT_WORKAROUND_FORWARD_BYTE = 0x61;
	
	public final Connection NPSERVER;
	public final Connection NPSERVER_KEEP_OPEN;
	
	public final int BUFFER_SIZE;
	public final int DEFAULT_PORT;
	public final boolean DUMP_PACKETS;
	
	public final int DATA_FAST_TIME_DELAY;
	public final int DATA_SLOW_TIME_DELAY;
	public final int JOIN_TIME_DELAY;
	public final int SERVER_TIME_DELAY;
	
	public final float RTT_ALPHA;
	
	private final DatagramSocket SOCKET;
	
	private final long OFFSET_TIME;
	
	private Thread syncher;
	private boolean synched;
	
	private final ContactControl source;
	private final Map<Connection,Contact> contacts = new HashMap<Connection,Contact>();
	
	private final Map<Byte,? extends Usage> uses;
	
	public Comm(ContactControl cm, Map<Byte,? extends Usage> u, Properties p) throws SocketException {
		source = cm;
		
		uses = Collections.unmodifiableMap(u);
		
		BUFFER_SIZE = Integer.parseInt(p.getProperty("buffer_size","512"));
		DUMP_PACKETS = Boolean.parseBoolean(p.getProperty("dump_packets", "false"));
		
		JOIN_TIME_DELAY = Integer.parseInt(p.getProperty("time_delay","1000"));
		DATA_FAST_TIME_DELAY = Integer.parseInt(p.getProperty("time_fast_delay",""+JOIN_TIME_DELAY/4));
		DATA_SLOW_TIME_DELAY = Integer.parseInt(p.getProperty("time_slow_delay",""+JOIN_TIME_DELAY/2));
		SERVER_TIME_DELAY = Integer.parseInt(p.getProperty("time_delay",""+JOIN_TIME_DELAY*8));
		RTT_ALPHA = Float.parseFloat(p.getProperty("rtt_alpha",""+0.5));
		
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
			npunused = new Connection(p.getProperty("server_keep_open",s.split(":")[0]+":" + (npserver.port-1)));
		} catch (UnknownHostException e) {
			try {
				npserver = new Connection("127.0.0.1:"+DEFAULT_PORT);
				npunused = new Connection("127.0.0.1:"+(DEFAULT_PORT-1));
			} catch (UnknownHostException e2) {
				npunused = npserver = null;
			}
		}
		NPSERVER = npserver;
		NPSERVER_KEEP_OPEN = npunused;
		
		OFFSET_TIME = System.currentTimeMillis();
	}
	
	public void start() {
		new Reciever().start();
		synch();
	}
	
	public synchronized boolean isSynched() {
		return synched;
	}
	
	private class ServerThread extends Thread {
		public void run() {
			while (true) {
				try {
					String attempt = "synching....";
					ByteBuffer request = makeBuffer();
					request.put(SERVER_REQUEST_BYTE).flip();
					
					for (int t=0; t<4; t++) {
						source.status(attempt.substring(0, attempt.length()-3+t));
						send(request,NPSERVER);
						
						Thread.sleep(JOIN_TIME_DELAY);
					}
				
					Connection c;
					source.status("guessing...");
					
					sleep(1000);
					
					try {
						InetAddress me = InetAddress.getLocalHost();
						c = new Connection(me,SOCKET.getLocalPort());
						source.setOwnerConnection(c,false);
						synchronized (Comm.this) {
							synched = true;
						}
					} catch (Exception e) {
						source.error("Couldn't obtain IP");
						synchronized (Comm.this) {
							synched = false;
							syncher = null;
						}
						break;
					}
				} catch (InterruptedException e) {}
				
				while (isSynched()) {
					ByteBuffer data = makeBuffer();
					data.put(KEEP_OPEN_BYTE);
					data.flip();
					send(data,NPSERVER_KEEP_OPEN);
					
					try {
						Thread.sleep(SERVER_TIME_DELAY);
					} catch (InterruptedException e) {}
				}
			}
		}		
	}
	
	public synchronized void synch() {
		synched = false;
		if (syncher == null) {
			syncher = new ServerThread();
			syncher.start();
		} else {
			syncher.interrupt();
		}
	}
	
	public ByteBuffer makeBuffer() {
		return ByteBuffer.allocate(BUFFER_SIZE);
	}
	
	public Event makeEvent(byte usage) {
		return new Event(usage, BUFFER_SIZE-3, Event.NORM_PRIORITY);
	}
	
	public void send(ByteBuffer b) {
		synchronized (contacts) {
			for (Connection c:contacts.keySet()) {
				send(b,c);
			}
		}
	}
	
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
	
	public void sendEvent(Event e) {
		synchronized (contacts) {
			for (Contact c:contacts.values()) {
				sendEvent(new Event(e), c);
			}
		}
	}
	
	public void sendEvent(Event e, Contact c) {
		c.setEvent(e);
		send(e.buffer,c.connection);
	}
	
	private void dump(String m, byte[] bs, int len) {

		System.out.print(m + "\tintent: ");
		switch (len>0?bs[0]:-1) {
			case JOIN_BYTE: System.out.print("JOIN"); break;
			case JOIN_ACK_TRUE_BYTE: System.out.print("JOIN_ACK_TRUE"); break;
			case JOIN_ACK_FALSE_BYTE: System.out.print("JOIN_ACK_FALSE"); break;
			case EVENT_BYTE: System.out.print("EVENT"); break;
			case DATA_BYTE: System.out.print("DATA"); break;
			case SERVER_REQUEST_BYTE: System.out.print("SERVER_REQUEST"); break;
			case SERVER_REPLY_BYTE: System.out.print("SERVER_REPLY"); break;
			case KEEP_OPEN_BYTE: System.out.print("KEEP_OPEN"); break;
			case NAT_WORKAROUND_REQUEST_BYTE : System.out.print("NAT_WORKAROUND_REQUEST"); break;
			case NAT_WORKAROUND_FORWARD_BYTE : System.out.print("NAT_WORKAROUND_FORWARD"); break;
			default: throw new RuntimeException("BAD_INTENT");//System.out.print("BAD_INTENT"); break;
		}
		
		System.out.print("\ttime: " + time());
		System.out.print("\tsize: " + len + " ");
		System.out.print("\tdata: [" + (len<=0?"":(Integer.toHexString((bs[0] & 0xf0) >> 0x4) + Integer.toHexString(bs[0] & 0x0f).toUpperCase())));
		
		for (int t=1; t<len; t++) {
			System.out.print((" " + Integer.toHexString((bs[t] & 0xf0) >> 0x4) + Integer.toHexString(bs[t] & 0x0f)).toUpperCase());
		}
		System.out.println("]");
	}
	
	
	public void getData(ByteBuffer data) {
		for (byte byt:uses.keySet()) {
			data.put(byt);
		}
		data.put((byte)0x0);
		source.getData(data);
	}
	
	protected void poll(Contact c, ByteBuffer b) {
		for (byte byt:c.getPlugins()) {
			uses.get(byt).pollData(c,b);
		}
		if (b.hasRemaining())
			b.put((byte)0x0);
	}
	
	public int time() {
		return (int)(System.currentTimeMillis() - OFFSET_TIME);
	}
	
	public void join(Contact c) {
		synchronized (contacts) {
			if (!contacts.containsKey(c.connection))
				contacts.put(c.connection,c);
		}
		c.join(this);
	}
	
	public void remove(Contact c) {
		synchronized (contacts) {
			contacts.remove(c.connection).deactivate();
		}
	}
	
	private class Reciever extends Thread {	
		
		@Override
		public void run() {
			while (true) {
				try {
					ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
					DatagramPacket packet = new DatagramPacket(buffer.array(), BUFFER_SIZE);
			        try {
						SOCKET.receive(packet);
					} catch (IOException e) {
						e.printStackTrace();
						continue;
					}
			        
			        Connection c = new Connection(packet);
			        buffer.limit(packet.getLength());
			        byte head = buffer.get();
			        
			        if (DUMP_PACKETS)
						dump("Recieved from " + c.toString(), buffer.array(), buffer.limit());
			        
			        switch (head) {
			        	case JOIN_BYTE: join(buffer,c); continue;
			        	case JOIN_ACK_TRUE_BYTE: joinAckTrue(buffer, c); continue;
			        	case JOIN_ACK_FALSE_BYTE: joinAckFalse(buffer, c); continue;
			        	case EVENT_BYTE: event(buffer,c); continue;
			        	case DATA_BYTE: data(buffer,c); continue;
			        	case SERVER_REQUEST_BYTE: serverRequest(buffer,c); continue;
			        	case SERVER_REPLY_BYTE: serverReply(buffer,c); continue;
			        	case NAT_WORKAROUND_REQUEST_BYTE: natWorkaroundRequest(buffer, c); continue;
			        	case NAT_WORKAROUND_FORWARD_BYTE: natWorkaroundForward(buffer, c); continue;
			        }
				} catch (Exception e) {
					//This is generally bad programming practice
					//but for this program we want it to try to function even if a part of it fails
					e.printStackTrace();
				}
			}
		}
		
		private void join(ByteBuffer b, Connection c) {
			
			HashSet<Byte> set = new HashSet<Byte>(uses.size());
			for (byte byt = b.get(); byt != 0x0; byt = b.get()) {
				if (uses.containsKey(byt))
					set.add(byt);
			}
			
			Contact sender;
			synchronized (contacts) {
				sender = contacts.get(c);
			
				if (sender != null) {
					sender.setData(b);
					synchronized (sender) {
						sender.setConnectionData(set,false);
						sender.reset();
						sender.connect();
					}
					
					ByteBuffer reply = makeBuffer();
					reply.put(JOIN_ACK_TRUE_BYTE);
					getData(reply);
					reply.flip();
					
					send(reply,c);
					
					return;
				}
				
						
				Contact temp = source.makeContact(c,b);
				
				if (temp == null) {
					ByteBuffer reply = makeBuffer();
					reply.put(JOIN_ACK_FALSE_BYTE).flip();
					send(reply,c);
					return;
				}
				
				
				temp.setConnectionData(set,false);
				
				temp.reset();
				temp.connect();
				contacts.put(c, temp);
				
				ByteBuffer reply = makeBuffer();
				reply.put(JOIN_ACK_TRUE_BYTE);
				getData(reply);
				reply.flip();
				send(reply,c);
				
				return;
			}		
		}
		
		private void joinAckTrue(ByteBuffer b, Connection c) {
			Contact con;
			synchronized (contacts) {
				con = contacts.get(c);
			}
			
			if (con == null)
				return;
			
			HashSet<Byte> set = new HashSet<Byte>(uses.size());
			for (byte byt = b.get(); byt != 0x0; byt = b.get()) {
				if (uses.containsKey(byt))
					set.add(byt);
			}
			
			con.setData(b);
			
			synchronized (con) {
				con.setConnectionData(set,true);
				
				con.ackTrue();
				con.connect();
			}
		}
		
		private void joinAckFalse(ByteBuffer b, Connection c) {
			Contact con;
			synchronized (contacts) {
				con = contacts.get(c);
			}
			
			if (con == null)
				return;
			
			synchronized (con) {
				con.ackFalse();
				con.status("waiting...");
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
			
			if (con.collideEvent(e)) 
				uses.get(e.usage).doEvent(con,e);
		}
		
		private List<Event> bufferOfResolvedEventsToSendInReplyOfDataEventMask = new ArrayList<Event>();
		
		private void data(ByteBuffer b, Connection c) {
			Contact con;
			synchronized (contacts) {
				con = contacts.get(c);
			}
			if (con == null)
				return;
			
			synchronized (con) {
				if (!con.isConnected())
					con.reactivate(Comm.this);
				
				if (con.isMaster())
					con.ping(b.getInt());
				else
					con.ping(b.getInt(),b.get());
				
				con.resolveEvents(bufferOfResolvedEventsToSendInReplyOfDataEventMask,b.getShort());
			}
			
			for (Event r:bufferOfResolvedEventsToSendInReplyOfDataEventMask) {
				send(r.buffer,c);
			}
			bufferOfResolvedEventsToSendInReplyOfDataEventMask.clear();
			
			byte usage;
			while (b.hasRemaining() && (usage = b.get()) != 0x0) {
				uses.get(usage).doData(con,b);
			}
		}
		
		private void serverRequest(ByteBuffer b, Connection c) { 
			//sure why not let the app run as a server, don't actually know what will happen in this case
			ByteBuffer temp = makeBuffer().put(SERVER_REPLY_BYTE);
			c.toBytes(temp);
			temp.flip();
			send(temp,c);
		}
		
		private void serverReply(ByteBuffer b, Connection c) {
			try {
				synchronized (this) {
					synched = true;
					syncher.interrupt();
				}
				Connection i = new Connection(b);
				
				source.setOwnerConnection(i,true);
			} catch (UnknownHostException e) {
				source.error("Unusable IP?");
				e.printStackTrace();
			}
		}
		
		private void natWorkaroundRequest(ByteBuffer b, Connection c) {
			//once again running as a server, it really shouldn't be used like this though
			Connection target;
			try {
				target = new Connection(b);
			} catch (UnknownHostException e) {
				return;
			}
			ByteBuffer forward = makeBuffer().put(NAT_WORKAROUND_FORWARD_BYTE);
			c.toBytes(forward).put(b).flip();
			send(forward,target);
		}
		
		private void natWorkaroundForward(ByteBuffer b, Connection c) {
			try {
				c = new Connection(b);
			} catch (UnknownHostException e) {
				return;
			}
			join(b,c);
		}
	}
}
