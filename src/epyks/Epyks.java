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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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

import comm.Comm;
import comm.Connection;
import comm.Contact;
import comm.ContactControl;
import comm.Event;

import epyks.plugins.MessagePlugin;

import net.miginfocom.swing.MigLayout;



public class Epyks extends JFrame implements ContactControl {
	
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		javax.swing.SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					new Epyks();	            
				}
			}
		);
	}
	
	private JPanel peersPanel;
	private JPanel pendingPeersPanel;
	private final Map<Connection, Peer> peers;
	private final Map<Connection, PeerPanel> pendingPeers;
	
	private Comm comm;
	
	private Settings settings;
	private PeerPanel user;
	
	
	public Epyks() {
		super("Epyks");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        Properties props = new Properties();
        try {
        	props.load(new FileReader("data/config"));
        } catch (FileNotFoundException fe) {
        	System.err.println("No config file!");
        } catch (IOException e) {
			System.err.println("IOException reading config!");
		}
        
         
        for (Enumeration<?> enu = props.propertyNames(); enu.hasMoreElements();)
        	System.out.println(enu.nextElement());
        
        settings = new Settings();
        
        
        Map<Byte,Plugin> plugins = new HashMap<Byte,Plugin>();
        JTabbedPane jtp = new JTabbedPane();
        
        try {
			Scanner plug = new Scanner(new File("plugins/plugins"));
			ClassLoader source = this.getClass().getClassLoader();
			
			while (plug.hasNext()) {
				String nextName = plug.next();
				Plugin next;
				
				try {
					next = (Plugin)source.loadClass(nextName).newInstance();
				} catch (Exception e) {
					System.err.println("Could not find plugin " + nextName);
					continue;
				}
				
				plugins.put(next.usage(), next);
				jtp.add(next);
			}
		} catch (FileNotFoundException e) {
			// TODO try to recreate file and show user
			e.printStackTrace();
		} finally {
			plugins.put(settings.usage(), settings);
			jtp.add(settings);
		}
        
        

        jtp.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        
        
        JTextField jaddfield = new JTextField(10);
        JButton jaddplus = new JButton("+");
        jaddplus.setMargin(new Insets(0,3,0,3));
        JTextField jaddmsg = new JTextField(10);
//        ActionListener add = new Joiner(jaddfield,jaddmsg);
//        jaddplus.addActionListener(add);
//        jaddfield.addActionListener(add);
//        jaddmsg.addActionListener(add);
        
        JPanel jadd = new JPanel(new BorderLayout(0,2));
        jadd.add(jaddfield,BorderLayout.CENTER);
        jadd.add(jaddplus,BorderLayout.EAST);
        jadd.add(jaddmsg,BorderLayout.SOUTH);
        jadd.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        
        peers = new HashMap<Connection,Peer>();
        pendingPeers = new HashMap<Connection,PeerPanel>();
        
        pendingPeersPanel = new JPanel();
        pendingPeersPanel.setLayout(new BoxLayout(pendingPeersPanel,BoxLayout.Y_AXIS));
        peersPanel = new JPanel();
        peersPanel.setLayout(new BoxLayout(peersPanel,BoxLayout.Y_AXIS));
        
        JPanel peersHolder = new JPanel(new BorderLayout());
        peersHolder.add(pendingPeersPanel,BorderLayout.NORTH);
        peersHolder.add(peersPanel,BorderLayout.CENTER);        
        
        JScrollPane jscp = new JScrollPane(peersHolder,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        user.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4,4,4,4), jscp.getBorder()));
        
        jscp.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0,4,0,4), jscp.getBorder()));
        jscp.setPreferredSize(new Dimension(130,200));
        
        JPanel jleft = new JPanel(new BorderLayout());
        jleft.add(jadd,BorderLayout.NORTH);
        jleft.add(jscp,BorderLayout.CENTER);   
        jleft.add(user,BorderLayout.SOUTH);
        
        add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,jleft,jtp));
        pack();
        setVisible(true);
        
        try {
			comm = new Comm(this, plugins, props);
		} catch (SocketException e) {
			System.err.println("Failed to greate Sockets");
		}
        comm.start();
	}
	
	private class Settings extends Plugin {		
		Properties props;
		
		String name;
		ImageIcon pic;
		Connection connection;
		
		public Settings() {
			setName("Settings");
			
			props = new Properties();
			File settings = new File("data/settings");
			
			try { //really java?
				props.load(new FileReader(settings));
			} catch (FileNotFoundException e) {
				try {
					settings.createNewFile();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			name = props.getProperty("name");
			try {
				String picname = props.getProperty("pic");
				if (picname != null)
					pic = new ImageIcon(ImageIO.read(new File("data/" + picname)));
			} catch (IOException e) {
				pic = null;
			}
			
			user = new PeerPanel();
			user.makeUserPanel(pic, name);
		}
		
		@Override
		public synchronized void doEvent(Contact s, Event e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public byte usage() {
			return (byte)0xff;
		}
		
		public void setUserName(String n) {
			synchronized (this) {
				name = n;
			}
			
			user.setName(n);
		}
		
		public void setPic(ImageIcon i) {
			synchronized (this) {
				pic = i;
			}
			
			user.setPic(i);
		}

		public void setConnection(Connection c) {
			synchronized (this) {
				connection = c;
			}
			
			user.message(connection.toString(), Color.BLACK);
		}
		
		public synchronized void save() {
			try {
				props.store(new FileWriter("data/settings"), null);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public Contact makeContact(Connection c, ByteBuffer b) {
		synchronized (peers) {
			Contact ret = peers.get(c);
			if (ret != null)
				return ret;
			
			if (!pendingPeers.containsKey(c)) {
				PeerPanel pending = new PeerPanel();
				pending.makePendingPanel(c);
				pendingPeers.put(c,pending);
			}
			return null;
		}
	}

	@Override
	public void status(String s) {
		user.message(s, Color.BLACK);
	}

	@Override
	public void error(String s) {
		user.message(s, Color.BLACK);
	}

	@Override
	public void setOwnerConnection(Connection c) {
		settings.setConnection(c);
	}
}
