package epyks;

import java.awt.Color;
import java.awt.Component;
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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import comm.Connection;
import comm.Contact;

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
	
	public static ImageIcon loadImage(Connection c) throws IOException {
		return new ImageIcon(ImageIO.read(new File("data/" + c==null?"user":c.toString() + "pic.png")));
	}
	
	private JLabel jpic;
	private JLabel jname;
	private JLabel jaddress;
	
	private final Epyks source;
	
	public PeerPanel(Epyks e) {
		super();
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
		setAlignmentX(Component.LEFT_ALIGNMENT);
		
		source = e;
	}
	
	public void makeUserPanel(ImageIcon pic,String name) {		
		removeAll();
				
		JPanel layer = new JPanel();
		layer.setLayout(new BoxLayout(layer,BoxLayout.X_AXIS));
        layer.setAlignmentX(LEFT_ALIGNMENT);
		
		jpic = new JLabel(pic!=null?pic:DEFAULT_PIC);
		jpic.setAlignmentY(TOP_ALIGNMENT);
		layer.add(jpic);
		
		layer.add(Box.createHorizontalGlue());

		JButton jretry = new JButton("\u21BB");
		jretry.setMargin(new Insets(0,2,0,3));
		jretry.addActionListener(source.new Resynch());
		jretry.setAlignmentY(TOP_ALIGNMENT);
        layer.add(jretry);
        
        add(layer);
        
		jname = new JLabel(name!=null?name:"Username");
		jname.setFont(jname.getFont().deriveFont(Font.BOLD,12));
		jname.setAlignmentX(LEFT_ALIGNMENT);
		add(jname);
		
		jaddress = new JLabel("initializing...");
		jaddress.setFont(jaddress.getFont().deriveFont(10f));
		jaddress.setAlignmentX(LEFT_ALIGNMENT);
		add(jaddress);
		
		setMaximumSize(new Dimension(Integer.MAX_VALUE,getPreferredSize().height));
		revalidate();
		repaint();
	}
	
	public void makeActivePanel(ImageIcon pic, String name, Connection conn) {
		removeAll();
		
		JPanel layer = new JPanel();
		layer.setLayout(new BoxLayout(layer,BoxLayout.X_AXIS));
		layer.setAlignmentX(LEFT_ALIGNMENT);
		
		jpic = new JLabel(pic!=null?pic:DEFAULT_PIC);
		jpic.setAlignmentY(TOP_ALIGNMENT);
		layer.add(jpic);
		
		layer.add(Box.createHorizontalGlue());
		
		JButton jexit = new JButton("x");
        jexit.setMargin(new Insets(0,4,0,3));
        jexit.addActionListener(source.new Remover(conn));
        jexit.setAlignmentY(TOP_ALIGNMENT);
        layer.add(jexit);

        add(layer);
        
		jname = new JLabel(name!=null?name:conn.toString());
		jname.setFont(jname.getFont().deriveFont(Font.BOLD,12));
		jname.setAlignmentX(LEFT_ALIGNMENT);
		add(jname);
		
		jaddress = new JLabel(conn.toString());
		jaddress.setFont(jaddress.getFont().deriveFont(10f));
		jaddress.setAlignmentX(LEFT_ALIGNMENT);
		add(jaddress);
		
		setMaximumSize(new Dimension(Integer.MAX_VALUE,getPreferredSize().height));
		revalidate();
		repaint();
	}
	
	public void makeLostPanel(String name, Connection conn) {
		removeAll();
		
		JPanel layer = new JPanel();
		layer.setLayout(new BoxLayout(layer,BoxLayout.X_AXIS));
		layer.setAlignmentX(LEFT_ALIGNMENT);
		
		jpic = new JLabel("Lost");
		jpic.setFont(jpic.getFont().deriveFont(Font.BOLD,12));
		jpic.setAlignmentX(TOP_ALIGNMENT);
		layer.add(jpic);
		
		layer.add(Box.createHorizontalGlue());
		
		JButton jretry = new JButton("\u21BB");
		jretry.setMargin(new Insets(0,2,0,3));
		jretry.addActionListener(source.new Retry(conn));
		jretry.setAlignmentX(TOP_ALIGNMENT);
        layer.add(jretry);
		
		JButton jexit = new JButton("x");
        jexit.setMargin(new Insets(0,4,0,3));
        jexit.addActionListener(source.new Remover(conn));
        jexit.setAlignmentX(TOP_ALIGNMENT);
        layer.add(jexit);
        
        add(layer);
        
		jname = new JLabel(name!=null?name:conn.toString());
		jname.setFont(jname.getFont().deriveFont(Font.BOLD,12));
		jname.setAlignmentX(LEFT_ALIGNMENT);
		add(jname);
		
		jaddress = new JLabel(conn.toString());
		jaddress.setFont(jaddress.getFont().deriveFont(10f));
		jaddress.setAlignmentX(LEFT_ALIGNMENT);
		add(jaddress);
		
		setMaximumSize(new Dimension(Integer.MAX_VALUE,getPreferredSize().height));
		revalidate();
		repaint();
	}
	
	public void makePendingPanel(String name, Connection conn) {
		removeAll();
		
		JPanel layer = new JPanel();
		layer.setLayout(new BoxLayout(layer,BoxLayout.X_AXIS));
		layer.setAlignmentX(LEFT_ALIGNMENT);
		
		jpic = new JLabel("Pending");
		jpic.setFont(jpic.getFont().deriveFont(Font.BOLD,12));
		jpic.setAlignmentX(TOP_ALIGNMENT);
		layer.add(jpic);
		
		layer.add(Box.createHorizontalGlue());
		
		JButton jaccept = new JButton("\u2713");
        jaccept.setMargin(new Insets(0,4,0,3));
        jaccept.addActionListener(source.new Accept(conn));
        jaccept.setAlignmentX(TOP_ALIGNMENT);
        layer.add(jaccept);
		
        JButton jexit = new JButton("x");
        jexit.setMargin(new Insets(0,4,0,3));
        jexit.addActionListener(source.new Remover(conn));
        jexit.setAlignmentX(TOP_ALIGNMENT);
        layer.add(jexit);
        
        add(layer);
        
		jname = new JLabel(name!=null?name:conn.toString());
		jname.setFont(jname.getFont().deriveFont(Font.BOLD,12));
		jname.setAlignmentX(LEFT_ALIGNMENT);
		add(jname);
		
		jaddress = new JLabel(conn.toString());
		jaddress.setFont(jaddress.getFont().deriveFont(10f));
		jname.setAlignmentX(LEFT_ALIGNMENT);
		add(jaddress);
		
		
		setMaximumSize(new Dimension(Integer.MAX_VALUE,getPreferredSize().height));
		revalidate();
		repaint();
	}
	
	public void setName(final String s) {
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						jname.setText(s);
						repaint();
					}
				}
			);
	}
	
	public void setPic(final ImageIcon i) {
		SwingUtilities.invokeLater(
				new Runnable() {
					public void run() {
						jpic.setIcon(i);
						repaint();
					}
				}
			);
	}
	
	public void message(final String m, final Color c) {		
		SwingUtilities.invokeLater(
			new Runnable() {
				public void run() {
					jaddress.setText(m);
					jaddress.setForeground(c);
					repaint();
				}
			}
		);
	}
	
//	private class Exit implements ActionListener {
//		private Contact owner;
//		
//		public Exit(Contact o) {
//			owner = o;
//		}
//		
//		@Override
//		public void actionPerformed(ActionEvent arg0) {
//			owner.active = false;
//			Epyks.remove(owner);
//		}
//	}
	
//	private class Exit implements ActionListener {
//		@Override
//		public void actionPerformed(ActionEvent arg0) {
//			Peer.this.active = false;
//			Epyks.remove(Peer.this);
//		}
//	}
}
