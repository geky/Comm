package epyks;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.LineBorder;

import comm.Connection;
import comm.Event;

import epyks.plugins.MessagePlugin;

import net.miginfocom.swing.MigLayout;



public class Epyks extends JFrame {
	
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		javax.swing.SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					current = new Epyks();	            
				}
			}
		);
	}
	
	private static JPanel peersPanel;
	private static JPanel pendingPeersPanel;
	
	private final Plugin[] plugins; 
	
	public Epyks() {
		super("Epyks");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        
        Plugin[] tempPlugs;
        try {
			Scanner plug = new Scanner(new File("plugins/plugins"));
			ArrayList<Plugin> tempTempPlugs = new ArrayList<Plugin>();
			ClassLoader source = this.getClass().getClassLoader();
			
			while (plug.hasNext()) {
				try {
					tempTempPlugs.add((Plugin)source.loadClass(plug.next()).newInstance());
				} catch (Exception e) {
					System.err.println("Could not create a plugin");
					e.printStackTrace();
				}
			}
			
			tempTempPlugs.add(me);
			
			tempPlugs = new Plugin[tempTempPlugs.size()];
			tempTempPlugs.toArray(tempPlugs);
		} catch (FileNotFoundException e) {
			// TODO try to recreate file and show user
			e.printStackTrace();
			tempPlugs = new Plugin[] {me};
		}
        
        plugins = tempPlugs;
        Map<Byte,Plugin> tempTempTempPlugs = new HashMap<Byte,Plugin>();
        for (Plugin p:tempPlugs) {
        	tempTempTempPlugs.put(p.getByte(),p);
        }
        dataPlugins = Collections.unmodifiableMap(tempTempTempPlugs);
        
        
        JPanel jleft = new JPanel(new BorderLayout());
        
        JTabbedPane jtp = new JTabbedPane();
        for (Plugin p:plugins) {
        	jtp.add(p);
        }
        jtp.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        
        
        JTextField jaddfield = new JTextField(10);
        JButton jaddplus = new JButton("+");
        jaddplus.setMargin(new Insets(0,3,0,3));
        JTextField jaddmsg = new JTextField(10);
        ActionListener add = new Joiner(jaddfield,jaddmsg);
        jaddplus.addActionListener(add);
        jaddfield.addActionListener(add);
        jaddmsg.addActionListener(add);
        
        
        JPanel jadd = new JPanel(new BorderLayout(0,2));
        jadd.add(jaddfield,BorderLayout.CENTER);
        jadd.add(jaddplus,BorderLayout.EAST);
        jadd.add(jaddmsg,BorderLayout.SOUTH);
        jadd.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        
        peers = new LinkedHashMap<Connection,PeerPanel>();
        pendingPeers = new HashMap<Connection,PeerPanel>();
        pendingPeersPanel = new JPanel();
        pendingPeersPanel.setLayout(new BoxLayout(pendingPeersPanel,BoxLayout.Y_AXIS));
        peersPanel = new JPanel();
        peersPanel.setLayout(new BoxLayout(peersPanel,BoxLayout.Y_AXIS));
        
        JPanel peersHolder = new JPanel(new BorderLayout());
        peersHolder.add(pendingPeersPanel,BorderLayout.NORTH);
        peersHolder.add(peersPanel,BorderLayout.CENTER);        
        
        JScrollPane jscp = new JScrollPane(peersHolder,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        me.peer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4,4,4,4), jscp.getBorder()));
        
        jscp.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0,4,0,4), jscp.getBorder()));
        jscp.setPreferredSize(new Dimension(130,200));
        
        jleft.add(jadd,BorderLayout.NORTH);
        jleft.add(jscp,BorderLayout.CENTER);   
        jleft.add(me.peer,BorderLayout.SOUTH);
        
        add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,jleft,jtp));
        pack();
        setVisible(true);
	}
	
	public static int time() {
		return (int)(System.currentTimeMillis() + synchTime - timeStart );
	}
	
	//returns true if event e is put into events
	private static boolean setEvent(Event e) {
		synchronized(events) {
			int min = 0;
			for (int t=1; t<events.length; t++) { //we can start at t=1 without checking because events contains 32 indices
				if (events[t].time == e.time && events[t].usage == e.usage)
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
	}
	
	public static void sendEvent(Event e) {
		e.buffer.put(EVENT_BYTE);
		e.buffer.putInt(e.time = time());
		e.buffer.rewind();
		if (setEvent(e)) {
			send(e.buffer);
		}
	}
	
	public static void sendEvents(Event[] evs) {
		int ti = time();
		for (Event e:evs) {
			e.buffer.put(EVENT_BYTE);
			e.buffer.putInt(ti++);
			e.buffer.rewind();
			if (setEvent(e)) {
				send(e.buffer);
			}
		}
	}
	
	public static ByteBuffer makeBuffer() {
		ByteBuffer b = ByteBuffer.allocate(MAX_BUFFER_SIZE);
		return b;
	}
	
	public static void dump(String m, byte[] bs, int len) {
		while (m.length() < 28) {
			m += "   ";
		}
		PACKET_DUMP.print(m + "\tintent: ");
		switch (bs[0]) {
			case JOIN_BYTE: PACKET_DUMP.print("JOIN          "); break;
			case EVENT_BYTE: PACKET_DUMP.print("EVENT         "); break;
			case DATA_BYTE: PACKET_DUMP.print("DATA          "); break;
			case SERVER_REQUEST_BYTE: PACKET_DUMP.print("SERVER_REQUEST"); break;
			case SERVER_REPLY_BYTE: PACKET_DUMP.print("SERVER_REPLY  "); break;
			case KEEP_OPEN_BYTE: PACKET_DUMP.print("KEEP_OPEN     "); break;
			default: PACKET_DUMP.print("NO_INTENT     "); break;
		}
		
		PACKET_DUMP.print("\ttime: " + time() + "\tdata: [" + (Integer.toHexString((bs[0] & 0xf0) >> 0x4) + Integer.toHexString(bs[0] & 0x0f).toUpperCase()));
		
		for (int t=1; t<len; t++) {
			PACKET_DUMP.print((" " + Integer.toHexString((bs[t] & 0xf0) >> 0x4) + Integer.toHexString(bs[t] & 0x0f)).toUpperCase());
		}
		PACKET_DUMP.println("]");
	}
	
	public static void send(ByteBuffer b) {
		DatagramPacket dp = new DatagramPacket(b.array(),b.limit(),null,DEFAULT_PORT);
		
		if (DUMP_PACKETS)
			dump("Sent to all Peers", b.array(), b.limit());
		
		synchronized (peers) {
			for (Map.Entry<Connection,PeerPanel> p : peers.entrySet()) {
				if (p.getValue().active) {
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
	
	public static void send(ByteBuffer b,Connection dest) {
		DatagramPacket dp = new DatagramPacket(b.array(),b.limit(),null,DEFAULT_PORT);
		dest.setDestination(dp);
		
		if (DUMP_PACKETS)
			dump("Sent to " + dest.toString(), b.array(), b.limit());
		
		try {
			SOCKET.send(dp);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	private class SendingStream extends Thread {		
		@Override
		public void run() {
			for (int synchAttempt = 0; !synched; synchAttempt++) {
				if (synchAttempt < 4) {
					String  m = new String("synching....".toCharArray(), 0, 9+synchAttempt);
					me.status(m, null);
					ByteBuffer request = makeBuffer();
					request.put(SERVER_REQUEST_BYTE);
					request.flip();
					send(request,NPSERVER);
				} else {
					me.status("Can't find server!", Color.RED);
					break;
				}
				
				try {
					sleep(TIME_PING);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			
			while (true) {
				int currentTime = time();
				
				boolean hasPeers = false;
				synchronized (peers) {
					for (PeerPanel p : peers.values()) {
						if (p.active) {
							if (currentTime - p.pingTime > TIME_OUT)
								p.lose();
							else
								hasPeers = true;
						}
					}
				}
				
				if (hasPeers) {
					ByteBuffer data = makeBuffer();
					data.position(9);
					for (int t=0; t<plugins.length; t++) {
						plugins[t].pollData(data);
					}
					if (data.hasRemaining()) {
						data.put((byte)0x0);
					}
					data.flip();
					data.put(DATA_BYTE).putInt(time()).putInt(eventMask);
					data.rewind();
					send(data);
				} else {
					ByteBuffer data = makeBuffer();
					data.put(KEEP_OPEN_BYTE);
					data.flip();
					send(data,NPSERVER_UNUSED);
				}
				
				try {
					sleep(TIME_PING);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static final byte JOIN_BYTE = 0x30;
	public static final byte JOIN_ACK_ACCEPTED_BYTE = 0x31;
	public static final byte JOIN_ACK_NOT_ACCEPTED_BYTE = 0x32;
	public static final byte DATA_BYTE = 0x40;
	public static final byte EVENT_BYTE = 0x50;
	public static final byte KEEP_OPEN_BYTE = 0x0;
	public final static byte SERVER_REQUEST_BYTE = 0x70;
	public final static byte SERVER_REPLY_BYTE = 0x60;
	
	private class Reciever extends Thread {
		public void run() {
			while (true) {
				ByteBuffer buffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);
				DatagramPacket packet = new DatagramPacket(buffer.array(), MAX_BUFFER_SIZE);
		        try {
					SOCKET.receive(packet);
				} catch (IOException e) {
					e.printStackTrace();
					continue;
				}
		        
		        Connection c = new Connection(packet);
		        byte head = buffer.get();
		        System.out.println("recieved " + Integer.toHexString(head) + "!");
		        
		        
		        if (DUMP_PACKETS) {
		        	byte[] temp = buffer.array();
		        	int len = buffer.limit();
		        	while (len > 0  && temp[len-1] == 0) {
		        		len--;
		        	}
					dump("Recieved from " + c.toString(), temp, len);
		        }
		        
		        switch (head) {
		        	case JOIN_BYTE: join(buffer,c); continue;
		        	case EVENT_BYTE: event(buffer,c); continue;
		        	case DATA_BYTE: data(buffer,c); continue;
		        	case SERVER_REQUEST_BYTE: serverRequest(buffer,c); continue;
		        	case SERVER_REPLY_BYTE: serverReply(buffer,c); continue;
		        }
			}
		}
		
		private void join(ByteBuffer b, Connection c) {
			PeerPanel p;
			synchronized (peers) {
				p = peers.get(c);
				if (p == null) {
					byte[] temp = b.array();
					String m = new String(temp,14,temp[13]*2);
					PeerPanel noob = PeerPanel.createPendingPeer(c, m);
					
					addPending(noob);
					
					return;
				} else if (p.active)
					return;
			}
			
			long senderTimeStart = b.getLong();
			int senderMask = b.getInt();
    		 
        	if (senderTimeStart == timeStart && senderMask == eventMask) {
        		p.activate();
        	} else {
        		if (senderTimeStart < timeStart) {
        			timeStart = senderTimeStart;
        			synchronized (events) {
        				eventMask = senderMask;
	        			eventResetTime = System.currentTimeMillis();
        			}
        		}
        		
        		ByteBuffer reply = makeBuffer();
        		reply.putLong(timeStart);
        		reply.putInt(eventMask);
        		reply.flip();
        		
        		send(reply,c);
        	}
		}
		
		private void event(ByteBuffer b, Connection c) {
			synchronized (peers) {
				if (!peers.containsKey(c))
					return;
			}
			
			Event e = new Event(b);
			
			synchronized(events) {
				if (e.time+timeStart < eventResetTime)
					return;
				if (e.masked(eventMask) && events[e.bit].time == e.time && events[e.bit].usage == e.usage)
					return;
				
				if (!e.masked(eventMask)) {
					if (e.time > events[e.bit].time) {
						eventMask ^= 0x1 << e.bit;
						events[e.bit] = e;
					} else
						return;
				} else if (e.time < events[e.bit].time) {
					Event temp = events[e.bit];
					eventMask ^= 0x1 << e.bit;
					events[e.bit] = e;
					if (!setEvent(temp))
						return;
				} else {
					if (!setEvent(e))
						return;
				}
			}
			
			Plugin p = dataPlugins.get(e.usage);
			if (p != null)
				p.doEvent(e);
		}
		
		private void data(ByteBuffer b, Connection c) {
			PeerPanel p;
			synchronized (peers) {
				p = peers.get(c);
				if (p == null)
					return;
				if (!p.active) {
					p.activate();
				}
			}
			
			p.pingTime = time();
			
			int senderTime = b.getInt();
			int senderMask = b.getInt();
			synchronized (events) {
				if (senderTime+timeStart > eventResetTime) {
					senderMask ^= eventMask;
					for (int t=0; senderMask != 0; senderMask >>>= 0x1,t++) {
	        			if ((senderMask & 0x1) != 0)
	        				send(events[t].buffer,c);
	        		}
		        }
			}
			
			byte plug;
			while (b.hasRemaining() && (plug = b.get()) != 0x0) {
				Plugin next = dataPlugins.get(plug);
				if (next != null) {
					next.doData(p, b);
				} else {
					b.position(b.position() + b.get());
				}
			}
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
				synchTime = (int)(b.getLong()-System.currentTimeMillis());
				Connection i = new Connection(b);
				synched = true;
				me.setConnection(i);
			} catch (UnknownHostException e) {
				me.status("Nonfunctional IP?", Color.RED);
				e.printStackTrace();
			}
		}
	}
	
	public static boolean add(final PeerPanel p) {
		synchronized (peers) {
			if (peers.containsKey(p.connection()))
				return false;
			peers.put(p.connection(), p);
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {							
				peersPanel.add(p);
				peersPanel.revalidate();
				peersPanel.repaint();
			}
		});
		
		return true;
	}
	
	public static boolean addPending(final PeerPanel p) {
		synchronized (peers) {
			if (pendingPeers.containsKey(p.connection()))
				return false;
			pendingPeers.put(p.connection(), p);
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {							
				pendingPeersPanel.add(p);
				pendingPeersPanel.revalidate();
				pendingPeersPanel.repaint();
			}
		});
		
		return true;
	}
	
	public static boolean addFromPending(final PeerPanel p) {
		synchronized (peers) {
			if (pendingPeers.remove(p.connection()) == null)
				return false;
			peers.put(p.connection(), p);
		}
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				pendingPeersPanel.remove(p);
				peersPanel.add(p);
				pendingPeersPanel.revalidate();
				peersPanel.revalidate();
				pendingPeersPanel.repaint();
				peersPanel.repaint();
			}
		});
		
		return true;
	}
	
	public static boolean remove(final PeerPanel p) {
		boolean pending = false;
		synchronized (peers) {
			if (peers.remove(p.connection()) == null) {
				if (pendingPeers.remove(p.connection()) != null)
					pending = true;
				else
					return false;
			}
		}
		
		if (pending) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {							
					pendingPeersPanel.remove(p);
					pendingPeersPanel.revalidate();
					pendingPeersPanel.repaint();
				}
			});
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {							
					peersPanel.remove(p);
					peersPanel.revalidate();
					peersPanel.repaint();
				}
			});
		}
		
		return true;
	}
	
	public static void join(String m, Connection to) {
		ByteBuffer request = makeBuffer();
		request.put(JOIN_BYTE);
		request.putLong(timeStart).putInt(eventMask);
		if (m != null)
			request.put((byte)m.length()).put(m.getBytes());
		else 
			request.put((byte)0x0);
		request.flip();
		send(request,to);
	}
	
	private class Joiner implements ActionListener {
		JTextField addressHolder;
		JTextField messageFeild;
		
		public Joiner(JTextField source, JTextField message) {
			addressHolder = source;
			messageFeild = message;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			final String m = messageFeild.getText();
			final String s = addressHolder.getText();
			final PeerPanel togoPeer = PeerPanel.createPeer(s);
			
			new Thread() {
				public void run() {
					Connection temp;
					try {
						temp = new Connection(s,Epyks.DEFAULT_PORT);
						temp.resolve();
					} catch (UnknownHostException e) {
						togoPeer.error("Could not resolve address",true);
						return;
					}
					
					try {
						togoPeer.connect(temp);
					} catch (Exception e) {
						togoPeer.error(e.getMessage(),true);
						e.printStackTrace();
					}
					
					if (!add(togoPeer)) {
						togoPeer.error("Already joined",true);
						return;
					}
					
					for (int synchAttempt = 0; !togoPeer.active && synchAttempt < 4; synchAttempt++) {
						
						Epyks.join(m,temp);
						
						try {
							sleep(TIME_PING);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
			}.start();
		}
	}
}
