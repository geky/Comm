package comm;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class Comm {
	public static final byte JOIN_BYTE = 0x30;
	public static final byte JOIN_ACK_TRUE_BYTE = 0x31;
	public static final byte JOIN_ACK_FALSE_BYTE = 0x3f;
	public static final byte DATA_BYTE = 0x40;
	public static final byte EVENT_BYTE = 0x50;
	public static final byte STREAM_REQUEST_BYTE = 0x52;
	public static final byte KEEP_OPEN_BYTE = 0x0;
	public static final byte SERVER_REQUEST_BYTE = 0x70;
	public static final byte SERVER_REPLY_BYTE = 0x60;
	public static final byte NAT_WORKAROUND_REQUEST_BYTE = 0x71;
	public static final byte NAT_WORKAROUND_FORWARD_BYTE = 0x61;
	
	public final Connection NPSERVER;
	public final Connection NPSERVER_KEEP_OPEN;
	
	public final int BUFFER_SIZE;
	public final boolean DUMP_PACKETS;
	public final byte DUMP_INFO;
	public final PrintStream DUMP_STREAM;

	public final int JOIN_TIME_DELAY;
	public final int SERVER_TIME_DELAY;
	
	public final float INCREASE_RT;
	public final float DECREASE_RT;
	
	private final DatagramSocket SOCKET;
	
	private final long OFFSET_TIME;
	
	protected final Object delayLock = new Object();
	private int fastDelay;
	private int slowDelay;
	
	protected final Object syncherLock = new Object();
	private Thread syncher;
	private boolean synched;
	
	private final Communicable source;
	private final StatusListener stat;
	private final Map<Connection,Contact> contacts = new HashMap<Connection,Contact>();
	
	public Comm(Communicable cm) throws SocketException {
		this(cm,null,null,null);
	}
	
	public Comm(Communicable cm, StatusListener st) throws SocketException {
		this(cm,st,null,null);
	}
	
	public Comm(Communicable cm, StatusListener st, Connection npserver, Connection unused) throws SocketException {
		this(cm, st, 512, 8000, 1000, 1000, 1000, 0.5f, 0.25f, -1, npserver, unused);
	}
	
	public Comm(Communicable cm, StatusListener st, Properties p) throws SocketException {
		source = cm;
		stat = st;
		
		BUFFER_SIZE = Integer.parseInt(p.getProperty("buffer_size","512"));
		
		DUMP_PACKETS = Boolean.parseBoolean(p.getProperty("dump_packets", "false"));
		if (DUMP_PACKETS) {
			String inf = p.getProperty("dump_info","0x1f");
			if (inf.startsWith("0x")) {
				DUMP_INFO = (byte)Integer.parseInt(inf.substring(2),16);
			} else {
				DUMP_INFO = (byte)Integer.parseInt(inf);
			}
			
			String s = p.getProperty("dump_file");
			if (s == null) {
				DUMP_STREAM = System.out;
			} else {
				OutputStream fo;
				try {
					fo = new BufferedOutputStream(new FileOutputStream(s));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					fo = null;
				} 
				DUMP_STREAM = new PrintStream(fo);
			}
		} else {
			DUMP_STREAM = null;
			DUMP_INFO = 0;
		}
		
		SERVER_TIME_DELAY = Integer.parseInt(p.getProperty("server_time_delay","8000"));
		JOIN_TIME_DELAY = Integer.parseInt(p.getProperty("join_time_delay","1000"));
		fastDelay = Integer.parseInt(p.getProperty("fast_delay","1000"));
		slowDelay = Integer.parseInt(p.getProperty("slow_delay","1000"));
		INCREASE_RT = Float.parseFloat(p.getProperty("increase_ratio","0.5f"));
		DECREASE_RT = Float.parseFloat(p.getProperty("decrease_ratio","0.25f"));
		
		String port = p.getProperty("default_port");
		boolean restrict = Boolean.parseBoolean(p.getProperty("restrict_port","false"));
		
		if (port == null) {
			if (restrict)
				throw new SocketException("Restricted to no ports");
			else
				SOCKET = new DatagramSocket();
		} else {
			if (restrict) {
				SOCKET = new DatagramSocket(Integer.parseInt(port));
			} else {
				DatagramSocket sock;
				try {
					sock = new DatagramSocket(Integer.parseInt(port));
				} catch (SocketException e) {
					sock = new DatagramSocket();
				}
				SOCKET = sock;
			}
		}
		
		Connection npserver;
		Connection npunused;
		try {
			String s1 = p.getProperty("server");
			npserver = s1==null ? null : new Connection(s1);
			String s2 = p.getProperty("server_keep_open");
			npunused = s2==null ? null : new Connection(s2);
		} catch (UnknownHostException e) {
			npunused = npserver = null;
		}
		NPSERVER = npserver;
		NPSERVER_KEEP_OPEN = npunused;
		
		OFFSET_TIME = System.currentTimeMillis();
	}
	
	public Comm(Communicable cm, StatusListener st, int buffer_size, int server_time_delay, int join_time_delay, int fast_delay, int slow_delay, float increase_ratio, float decrease_ratio, int port, Connection npserver, Connection npunused) throws SocketException {
		source = cm;
		stat = st;
		
		BUFFER_SIZE = buffer_size;
		
		DUMP_PACKETS = false;
		DUMP_INFO = 0;
		DUMP_STREAM = null;
		
		JOIN_TIME_DELAY = join_time_delay;
		SERVER_TIME_DELAY = server_time_delay;
		fastDelay = fast_delay;
		slowDelay = slow_delay;
		INCREASE_RT = increase_ratio;
		DECREASE_RT = decrease_ratio;
		
		SOCKET = port == -1 ? new DatagramSocket() : new DatagramSocket(port);
		NPSERVER = npserver;
		NPSERVER_KEEP_OPEN = npunused;
		
		OFFSET_TIME = System.currentTimeMillis();
	}
	
	public Comm(Communicable cm, StatusListener st, int buffer_size, int server_time_delay, int join_time_delay, int fast_delay, int slow_delay, float increase_ratio, float decrease_ratio, int port, Connection npserver, Connection npunused, byte dump_info, PrintStream dump_stream) throws SocketException {
		source = cm;
		stat = st;
		
		BUFFER_SIZE = buffer_size;
		
		DUMP_PACKETS = true;
		DUMP_INFO = dump_info;
		DUMP_STREAM = dump_stream;
		
		JOIN_TIME_DELAY = join_time_delay;
		SERVER_TIME_DELAY = server_time_delay;
		fastDelay = fast_delay;
		slowDelay = slow_delay;
		INCREASE_RT = increase_ratio;
		DECREASE_RT = decrease_ratio;
		
		SOCKET = port == -1 ? new DatagramSocket() : new DatagramSocket(port);
		NPSERVER = npserver;
		NPSERVER_KEEP_OPEN = npunused;
		
		OFFSET_TIME = System.currentTimeMillis();
	}
	
	public void start() {
		new Reciever().start();
		synch();
	}
	
	public boolean isSynched() {
		synchronized (syncherLock) {
			return synched;
		}
	}
	
	private class ServerThread extends Thread {
		public void run() {
			while (true) {
				try {
					if (NPSERVER != null) {
						ByteBuffer request = makeServerRequest();
						
						for (int t=0; t<4; t++) {
							if (stat != null) stat.status("synching...".substring(0, 8+t));
							send(request,NPSERVER);
							
							Thread.sleep(JOIN_TIME_DELAY);
						}
					}
				
					Connection c;
					if (stat != null) stat.status("guessing...");
					
					Thread.sleep(JOIN_TIME_DELAY);
					
					try {
						InetAddress me = InetAddress.getLocalHost();
						c = new Connection(me,SOCKET.getLocalPort());
						source.setOwnerConnection(c,false);
						synchronized (syncherLock) {
							synched = true;
						}
					} catch (Exception e) {
						if (stat != null) stat.error("Couldn't obtain IP");
						synchronized (syncherLock) {
							synched = false;
							syncher = null;
						}
						break;
					}
				} catch (InterruptedException e) {}
				
				
				ByteBuffer ko = makeKeepOpen();
				while (isSynched()) {
					if (NPSERVER_KEEP_OPEN != null)
						send(ko,NPSERVER_KEEP_OPEN);
					try {
						Thread.sleep(SERVER_TIME_DELAY);
					} catch (InterruptedException e) {}
				}
			}
		}		
	}
	
	public void synch() {
		synchronized (syncherLock) {
			synched = false;
			if (syncher == null) {
				syncher = new ServerThread();
				syncher.start();
			} else {
				syncher.interrupt();
			}
		}
	}
	
	public void setFastDelay(int d) {
		synchronized (delayLock) {
			fastDelay = d;
		}
	}
	
	public void setSlowDelay(int d) {
		synchronized (delayLock) {
			slowDelay = d;
		}
	}
	
	public int getFastDelay() {
		synchronized (delayLock) {
			return fastDelay;
		}
	}
	
	public int getSlowDelay() {
		synchronized (delayLock) {
			return slowDelay;
		}
	}
	
//	public void updateRateDelay() {
//		int min = DEFAULT_FAST_DELAY;
//		int max = DEFAULT_SLOW_DELAY;
//		int t;
//		for (Usage u:uses.values()) {
//			t = u.getMaximumRateDelay();
//			if (t >= 0 && t < max)
//				max = t;
//			t = u.getMinimumRateDelay();
//			if (t >= 0 && t < min)
//				min = t;
//		}
//		
//		slowDelay = max;
//		fastDelay = min;
//	}

	public ByteBuffer makeBuffer() {
		return ByteBuffer.allocate(BUFFER_SIZE);
	}
	
	public Event makeEvent() {
		return new Event(BUFFER_SIZE);
	}
	
	protected ByteBuffer makeJoinRequest() {
		ByteBuffer joinPacket = makeBuffer();
		joinPacket.put(JOIN_BYTE);
		source.getInitData(joinPacket);
		joinPacket.flip();
		return joinPacket;
	}
	
	protected ByteBuffer makeWorkaroundRequest(Connection c) {
		ByteBuffer waPacket = makeBuffer();
		waPacket.put(NAT_WORKAROUND_REQUEST_BYTE);
		c.toBytes(waPacket);
		source.getInitData(waPacket);
		waPacket.flip();
		return waPacket;
	}
	
	protected ByteBuffer makeJoinAckTrue() {
		ByteBuffer reply = makeBuffer();
		reply.put(JOIN_ACK_TRUE_BYTE);
		source.getInitData(reply);
		reply.flip();
		return reply;
	}
	
	protected ByteBuffer makeJoinAckFalse() {
		ByteBuffer reply = makeBuffer();
		reply.put(JOIN_ACK_FALSE_BYTE).flip();
		return reply;
	}
	
	protected ByteBuffer makeServerRequest() {
		ByteBuffer request = makeBuffer();
		request.put(SERVER_REQUEST_BYTE).flip();
		return request;
	}
	
	protected ByteBuffer makeKeepOpen() {
		ByteBuffer data = makeBuffer();
		data.put(KEEP_OPEN_BYTE).flip();
		return data;
	}
	
	protected ByteBuffer makeDataPoll(Contact c) {
		ByteBuffer data = makeBuffer();
		data.put(Comm.DATA_BYTE);
		data.putShort(c.getEventMask());
		source.pollData(c,data);
		data.flip();
		return data;
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
			dump("Sent" ,dest.toString(), b.array(), b.limit());
		
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
	
	private void dump(String m, String ip, byte[] bs, int len) {
		
		if ((DUMP_INFO & 0x20) != 0) DUMP_STREAM.print(m + " ");
		if ((DUMP_INFO & 0x10) != 0) DUMP_STREAM.print(ip + " ");
		if ((DUMP_INFO & 0x08) != 0) DUMP_STREAM.print("t-" + time() + " ");
		if ((DUMP_INFO & 0x04) != 0) {
			switch (len>0?bs[0]:-1) {
				case JOIN_BYTE: DUMP_STREAM.print("JOIN "); break;
				case JOIN_ACK_TRUE_BYTE: DUMP_STREAM.print("JOIN_ACK_TRUE "); break;
				case JOIN_ACK_FALSE_BYTE: DUMP_STREAM.print("JOIN_ACK_FALSE "); break;
				case EVENT_BYTE: DUMP_STREAM.print("EVENT "); break;
				case DATA_BYTE: DUMP_STREAM.print("DATA "); break;
				case SERVER_REQUEST_BYTE: DUMP_STREAM.print("SERVER_REQUEST "); break;
				case SERVER_REPLY_BYTE: DUMP_STREAM.print("SERVER_REPLY "); break;
				case KEEP_OPEN_BYTE: DUMP_STREAM.print("KEEP_OPEN "); break;
				case NAT_WORKAROUND_REQUEST_BYTE : DUMP_STREAM.print("NAT_WORKAROUND_REQUEST "); break;
				case NAT_WORKAROUND_FORWARD_BYTE : DUMP_STREAM.print("NAT_WORKAROUND_FORWARD "); break;
				default: DUMP_STREAM.print("BAD_INTENT "); break;
			}
		}
		if ((DUMP_INFO & 0x2) != 0) DUMP_STREAM.print("L" + len + " "); 
		if ((DUMP_INFO & 0x1) != 0) {
			DUMP_STREAM.print(" [" + (len<=0?"":(Integer.toHexString((bs[0] & 0xf0) >> 0x4) + Integer.toHexString(bs[0] & 0x0f).toUpperCase())));
			for (int t=1; t<len; t++) {
				DUMP_STREAM.print((" " + Integer.toHexString((bs[t] & 0xf0) >> 0x4) + Integer.toHexString(bs[t] & 0x0f)).toUpperCase());
			}
			DUMP_STREAM.println("]");
		}
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
			Contact con = contacts.remove(c.connection);
			if (con != null) con.deactivate();
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
						dump("Rec ",c.toString(), buffer.array(), buffer.limit());
			        
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
			
			Contact sender;
			synchronized(contacts) {
				sender = contacts.get(c);
			}	
			
			if (sender != null) {
				synchronized (sender) {
					sender.reset();
					sender.reactivate(Comm.this);
				}
				
				ByteBuffer reply = makeJoinAckTrue();					
				send(reply,c);
			} else {
				source.makeContact(c,b);
				
				ByteBuffer reply = makeJoinAckFalse();
				send(reply,c);
			}
		}
		
		private void joinAckTrue(ByteBuffer b, Connection c) {			
			Contact con;
			synchronized (contacts) {
				con = contacts.get(c);
			}
			
			if (con == null)
				return;
			
			source.ackContact(con, b);
			con.ackTrue();
		}
		
		private void joinAckFalse(ByteBuffer b, Connection c) {
			Contact con;
			synchronized (contacts) {
				con = contacts.get(c);
			}
			
			if (con == null)
				return;
			
			con.ackFalse();
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
				source.doEvent(con,e);
		}
		
		private List<Event> bufferOfResolvedEventsToSendInReplyOfDataEventMask = new ArrayList<Event>(Short.SIZE);
		
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
				
				con.ping();
				
				con.resolveEvents(bufferOfResolvedEventsToSendInReplyOfDataEventMask,b.getShort());
			}
			
			for (Event r:bufferOfResolvedEventsToSendInReplyOfDataEventMask) {
				send(r.buffer,c);
			}
			bufferOfResolvedEventsToSendInReplyOfDataEventMask.clear();
			
			source.doData(con, b);
		}
		
		private void serverRequest(ByteBuffer b, Connection c) { 
			//sure why not let the app run as a server, don't actually know what will happen in this case
			ByteBuffer temp = makeBuffer();
			temp.put(SERVER_REPLY_BYTE);
			c.toBytes(temp);
			temp.flip();
			send(temp,c);
		}
		
		private void serverReply(ByteBuffer b, Connection c) {
			try {
				synchronized (syncherLock) {
					synched = true;
					syncher.interrupt();
				}
				Connection i = new Connection(b);
				
				source.setOwnerConnection(i,true);
			} catch (UnknownHostException e) {
				if (stat != null) stat.error("Unusable IP?");
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
