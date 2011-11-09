package epyks;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import comm.Connection;
import comm.Contact;

import net.miginfocom.swing.MigLayout;

public class PeerPanel extends JPanel {
	
	public static final ImageIcon DEFAULT_PIC;
	
	static {
		ImageIcon temp;
		try {
			temp = new ImageIcon(ImageIO.read(new File("data"+File.separator+"defaultpic.png")));
		} catch (IOException e) {
			temp = null;
			e.printStackTrace();
		}
		DEFAULT_PIC = temp;
	}
	
	public static ImageIcon loadImage(Connection c, int p) throws IOException {
		return p==0 ? DEFAULT_PIC : new ImageIcon(ImageIO.read(new File("data/" + c==null?"user":c.toString() + "pic" + p + ".png")));
	}
	
	protected JLabel jpic;
	protected JLabel jname;
	protected JLabel jaddress;
	protected JLabel jping;
	
	private Connection conn;
	private String name;
	private ImageIcon pic;
	
	public volatile int pingTime = -1;
	public volatile boolean active = false;
	
	public PeerPanel() {
		super(new GridBagLayout());
	}
	
	public static PeerPanel createUserPeer() {
		PeerPanel p = new PeerPanel();
		p.makeUserPanel();
		return p;
	}
	
	public static PeerPanel createPendingPeer(Connection c, String n) {
		PeerPanel p = new PeerPanel();
		p.conn = c;
		p.name = n;
		p.makePendingPanel();
		return p;
	}
	
	public static PeerPanel createPeer(String s) {
		PeerPanel p = new PeerPanel();
		p.name = s;
		p.makeLostPanel("resolving...");
		//run thread in Epyks to resolve connection
		return p;
	}
	
	public synchronized String getName() {
		return name;
	}
	
	public void setName(final String s) {
		synchronized(this) {
			name = s;
		}
		
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						jname.setText(s);
						repaint();
					}
				}
			);
	}
	
	public synchronized ImageIcon getPic() {
		return pic;
	}
	
	public void setPic(final ImageIcon i) {
		synchronized(this) {
			pic = i;
		}
		
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						jpic.setIcon(i);
						repaint();
					}
				}
			);
	}
	
	public void ping() {
		pingTime = Epyks.time();
	}
	
	public synchronized Connection connection() {
		return conn;
	}
	
	private void makeUserPanel() {		
		GridBagConstraints gbc = new GridBagConstraints();
		
		jpic = new JLabel(pic!=null?pic:DEFAULT_PIC);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		add(jpic,gbc);

		jping = null;
        
		jname = new JLabel(name!=null?name:"Username");
		jname.setFont(jname.getFont().deriveFont(Font.BOLD,12));
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		add(jname,gbc);
		
		jaddress = new JLabel("initializing...");
		jaddress.setFont(jaddress.getFont().deriveFont(10f));
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		add(jaddress,gbc);
		
		setMaximumSize(this.getPreferredSize());
	}
	
	private void makeActivePanel() {
		GridBagConstraints gbc = new GridBagConstraints();
		
		jpic = new JLabel(pic!=null?pic:DEFAULT_PIC);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 2;
		add(jpic,gbc);
		
		JButton jexit = new JButton("x");
        jexit.setMargin(new Insets(0,4,0,3));
        jexit.addActionListener(new Exit());
        gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
        add(jexit,gbc);
        
        jping = new JLabel();
        gbc.gridx = 1;
		gbc.gridy = 1;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		add(jping,gbc);
        
		jname = new JLabel(name);
		jname.setFont(jname.getFont().deriveFont(Font.BOLD,12));
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		add(jname,gbc);
		
		jaddress = new JLabel(conn.toString());
		jaddress.setFont(jaddress.getFont().deriveFont(10f));
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbc.gridwidth = 2;
		gbc.gridheight = 1;
		add(jaddress,gbc);
		
		setMaximumSize(this.getPreferredSize());
	}
	
	private void makeLostPanel(String m) {
		GridBagConstraints gbc = new GridBagConstraints();
		
		jpic = new JLabel("Not Connected");
		jpic.setFont(jpic.getFont().deriveFont(Font.BOLD,12));
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		add(jpic,gbc);
		
		JButton jretry = new JButton("\u21BB");
		jretry.setMargin(new Insets(0,4,0,3));
        gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
        add(jretry,gbc);
		
		JButton jexit = new JButton("x");
        jexit.setMargin(new Insets(0,4,0,3));
        jexit.addActionListener(new Exit());
        gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
        add(jexit,gbc);
        
        jping = null;
        
		jname = new JLabel(name);
		jname.setFont(jname.getFont().deriveFont(Font.BOLD,12));
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 3;
		gbc.gridheight = 1;
		add(jname,gbc);
		
		jaddress = new JLabel(m!=null?m:conn.toString());
		jaddress.setFont(jaddress.getFont().deriveFont(10f));
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 3;
		gbc.gridheight = 1;
		add(jaddress,gbc);
		
		setMaximumSize(this.getPreferredSize());
	}
	
	private void makePendingPanel() {
		GridBagConstraints gbc = new GridBagConstraints();
		
		jpic = new JLabel("Pending");
		jpic.setFont(jpic.getFont().deriveFont(Font.BOLD,12));
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
		add(jpic,gbc);
		
		JButton jaccept = new JButton("\u2713");
        jaccept.setMargin(new Insets(0,4,0,3));
        gbc.gridx = 1;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
        add(jaccept,gbc);
		
        JButton jexit = new JButton("x");
        jexit.setMargin(new Insets(0,4,0,3));
        jexit.addActionListener(new Exit());
        gbc.gridx = 2;
		gbc.gridy = 0;
		gbc.gridwidth = 1;
		gbc.gridheight = 1;
        add(jexit,gbc);
        
        jping = null;
        
		jname = new JLabel("status");
		jname.setFont(jname.getFont().deriveFont(Font.BOLD,12));
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 3;
		gbc.gridheight = 1;
		add(jname,gbc);
		
		jaddress = new JLabel(conn.toString());
		jaddress.setFont(jaddress.getFont().deriveFont(10f));
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.gridwidth = 3;
		gbc.gridheight = 1;
		add(jaddress,gbc);
		
		setMaximumSize(this.getPreferredSize());
	}
	
	
	public void lose() {
		active = false;
		pingTime = -1;
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					removeAll();
					synchronized (PeerPanel.this) {
						makeLostPanel(null);
					}
					revalidate();
					repaint();
				}
			}
		);
	}
	
	public void activate() {
		active = true;
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					removeAll();
					synchronized (PeerPanel.this) {
						makeActivePanel();
					}
					revalidate();
					repaint();
				}
			}
		);
	}
	
	public void connect(final Connection c) throws InvocationTargetException, InterruptedException {
		SwingUtilities.invokeAndWait(
			new Runnable() {
				public void run() {
					synchronized (PeerPanel.this) {
						conn = c;
						jaddress.setText(c.toString());
						jaddress.setForeground(Color.BLACK);
					}
					repaint();
				}
			}
		);
	}
	
	public void status(final String m,final boolean overwrite) {		
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					synchronized (PeerPanel.this) {
						if (conn != null && !overwrite)
							return;
						jaddress.setText(m);
						jaddress.setForeground(Color.BLACK);
					}
					repaint();
				}
			}
		);
	}
	
	public void error(final String m,final boolean overwrite) {
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					synchronized (PeerPanel.this) {
						if (conn != null && !overwrite)
							return;
						jaddress.setText(m);
						jaddress.setForeground(Color.RED);
					}
					repaint();
				}
			}
		);
	}
	
	public void paint(Graphics g) {
//		if (pingTime > -1) {
//			jping.setText(""+pingTime);
//			jping.setForeground(new Color(pingTime<511?pingTime>255?pingTime-256:0:255,pingTime<255?255-pingTime:0,0));
//		}
		super.paint(g);
	}
	
	private class Exit implements ActionListener {
		private Contact owner;
		
		public Exit(Contact o) {
			owner = o;
		}
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			owner.active = false;
			Epyks.remove(owner);
		}
	}
	
//	private class Exit implements ActionListener {
//		@Override
//		public void actionPerformed(ActionEvent arg0) {
//			Peer.this.active = false;
//			Epyks.remove(Peer.this);
//		}
//	}
}
