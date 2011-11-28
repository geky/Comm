package epyks;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import comm.Comm;
import comm.Connection;
import comm.Contact;
import comm.ContactControl;
import comm.Event;

public class Epyks extends JFrame implements ContactControl {

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}

		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new Epyks();
			}
		});
	}

	private JPanel peersPanel;
	private JPanel pendingPeersPanel;
	private final Map<Connection, Peer> peers;
	private final Map<Connection, PeerPanel> pendingPeers;

	private Comm comm;

	private Settings settings;

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

		settings = new Settings(props);

		Map<Byte, Plugin> plugins = new HashMap<Byte, Plugin>();
		JTabbedPane jtp = new JTabbedPane();

		try {
			Scanner plug = new Scanner(new File("plugins/plugins"));
			ClassLoader source = this.getClass().getClassLoader();

			while (plug.hasNext()) {
				String nextName = plug.next();
				Plugin next;

				try {
					next = (Plugin) source.loadClass(nextName).newInstance();
				} catch (Exception e) {
					System.err.println("Could not find plugin " + nextName);
					continue;
				}
				
				settings.add(next.settings(props));
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
		jaddplus.setMargin(new Insets(0, 3, 0, 3));
		ActionListener add = new Adder(jaddfield);
		jaddplus.addActionListener(add);
		jaddfield.addActionListener(add);

		JPanel jadd = new JPanel(new BorderLayout(0, 2));
		jadd.add(jaddfield, BorderLayout.CENTER);
		jadd.add(jaddplus, BorderLayout.EAST);
		jadd.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		peers = new HashMap<Connection, Peer>();
		pendingPeers = new HashMap<Connection, PeerPanel>();

		pendingPeersPanel = new JPanel();
		pendingPeersPanel.setLayout(new BoxLayout(pendingPeersPanel, BoxLayout.Y_AXIS));
		peersPanel = new JPanel();
		peersPanel.setLayout(new BoxLayout(peersPanel, BoxLayout.Y_AXIS));

		JPanel peersHolder = new JPanel();
		peersHolder.setLayout(new BoxLayout(peersHolder, BoxLayout.Y_AXIS));
		peersHolder.add(pendingPeersPanel);
		peersHolder.add(peersPanel);

		JScrollPane jscp = new JScrollPane(peersHolder,
				JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		jscp.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0, 4, 0, 4), jscp.getBorder()));
		jscp.setPreferredSize(new Dimension(130, 200));

		JPanel userHolder = new JPanel();
		userHolder.setLayout(new BoxLayout(userHolder, BoxLayout.X_AXIS));
		userHolder.add(settings.user);
		userHolder.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(4, 4, 4, 4),
				BorderFactory.createLineBorder(Color.GRAY)));
		
		JPanel jleft = new JPanel(new BorderLayout());
		jleft.add(jadd, BorderLayout.NORTH);
		jleft.add(jscp, BorderLayout.CENTER);
		jleft.add(userHolder, BorderLayout.SOUTH);

		add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, jleft, jtp));
		pack();
		setVisible(true);

		try {
			comm = new Comm(this, plugins, props);
		} catch (SocketException e) {
			System.err.println("Failed to create Sockets");
		}
		comm.start();

		for (Plugin p : plugins.values()) {
			p.setComm(comm);
		}
	}

	

	@Override
	public Contact makeContact(Connection c, ByteBuffer b) {
		synchronized (peers) {
			Contact ret = peers.get(c);
			if (ret != null) {
				ret.setData(b);
				return ret;
			}

			if (!pendingPeers.containsKey(c)) {
				byte[] temp = new byte[b.get()];
				b.get(temp);
				String name = new String(temp);
				
				final PeerPanel pending = new PeerPanel(this);
				pending.makePendingPanel(name,c);
				pendingPeers.put(c, pending);

				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						pendingPeersPanel.add(pending);
						pendingPeersPanel.revalidate();
						pendingPeersPanel.repaint();
					}
				});
			}
			return null;
		}
	}
	
	@Override
	public void getData(ByteBuffer b) {
		String name = settings.getUserName();
		b.put((byte)name.length());
		b.put(name.getBytes());
	}

	@Override
	public void status(String s) {
		settings.user.message(s, Color.BLACK);
	}

	@Override
	public void error(String s) {
		settings.user.message(s, Color.RED);
	}

	@Override
	public void setOwnerConnection(Connection c,boolean checked) {
		settings.setConnection(c,checked);
	}

	public class Remover implements ActionListener {
		Connection owner;

		public Remover(Connection c) {
			owner = c;
		}

		public void actionPerformed(ActionEvent arg0) {
			Peer peer;
			synchronized (peers) {
				peer = peers.remove(owner);
				if (peer != null) {
					peersPanel.remove(peer.panel);
					peersPanel.revalidate();
					peersPanel.repaint();
				} else {
					PeerPanel pending = pendingPeers.remove(owner);
					if (pending != null) {
						pendingPeersPanel.remove(pending);
						pendingPeersPanel.revalidate();
						pendingPeersPanel.repaint();
					}
					return;
				}
			}

			comm.remove(peer);
		}
	}

	public class Adder implements ActionListener {
		JTextField source;

		public Adder(JTextField s) {
			source = s;
		}

		public void actionPerformed(ActionEvent arg0) {
			String ip = source.getText();
			source.setText("");

			Peer p;
			try {
				Connection to = new Connection(ip, comm.DEFAULT_PORT);
				p = new Peer(to, Epyks.this);
			} catch (UnknownHostException e) {
				System.err.println("Bad Address");
				return;
			}

			synchronized (peers) {
				if (peers.containsKey(p.connection)) {
					System.err.println("Duplicate Contact");
					return;
				} else {
					peers.put(p.connection, p);
					peersPanel.add(p.panel);
					peersPanel.revalidate();
					peersPanel.repaint();
				}
			}

			comm.add(p);
		}
	}

	public class Retry implements ActionListener {
		Connection owner;

		public Retry(Connection c) {
			owner = c;
		}

		public void actionPerformed(ActionEvent arg0) {
			Peer p;
			synchronized (peers) {
				p = peers.get(owner);
			}

			if (p != null)
				comm.join(p);
		}
	}
	
	public class Resynch implements ActionListener {
		public void actionPerformed(ActionEvent arg0) {
			comm.synch();
		}
	}

	public class Accept implements ActionListener {
		Connection owner;

		public Accept(Connection c) {
			owner = c;
		}

		public void actionPerformed(ActionEvent arg0) {
			Peer p = new Peer(owner, Epyks.this);

			synchronized (peers) {
				PeerPanel temp = pendingPeers.remove(owner);
				pendingPeersPanel.remove(temp);
				pendingPeersPanel.revalidate();
				pendingPeersPanel.repaint();

				peers.put(owner, p);
				peersPanel.add(p.panel);
				peersPanel.revalidate();
				peersPanel.repaint();
			}

			if (p != null) {
				comm.add(p);
			}
		}
	}
	
	private class Settings extends Plugin {
		Properties config;

		PeerPanel user;
		
		String name;
		ImageIcon pic;
		Connection connection;
		
		JPanel mainPanel;

		public Settings(Properties p) {
			setName("Settings");
			setLayout(new BorderLayout());

			config = p;

			name = config.getProperty("name");
			try {
				String picname = config.getProperty("pic");
				if (picname != null)
					pic = new ImageIcon(
							ImageIO.read(new File("data/" + picname)));
			} catch (IOException e) {
				pic = null;
			}

			user = new PeerPanel(Epyks.this);
			user.makeUserPanel(pic, name);
			
			mainPanel = new JPanel();
			mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.Y_AXIS));
			mainPanel.setOpaque(false);
			
			JPanel layer1 = new JPanel();
			layer1.setLayout(new BoxLayout(layer1,BoxLayout.X_AXIS));
			layer1.setOpaque(false);
			
			JPanel jpic = new JPanel();
			jpic.setLayout(new BoxLayout(jpic,BoxLayout.Y_AXIS));
			jpic.setOpaque(false);
			
			JPanel jpiclabelpanel = new JPanel();
			jpiclabelpanel.setLayout(new BoxLayout(jpiclabelpanel,BoxLayout.X_AXIS));
			JLabel jpiclabel = new JLabel(" Picture");
			jpiclabelpanel.add(jpiclabel);
			jpiclabelpanel.add(Box.createHorizontalGlue());
			jpiclabelpanel.setOpaque(false);
			jpiclabelpanel.setAlignmentX(CENTER_ALIGNMENT);
			
			jpic.add(jpiclabelpanel);
			JLabel jpicpic = new JLabel(pic);
			jpicpic.setAlignmentX(CENTER_ALIGNMENT);
			jpic.add(jpicpic);
			JLabel jpichint = new JLabel("(click to edit)");
			jpichint.setOpaque(false);
			jpichint.setAlignmentX(CENTER_ALIGNMENT);
			jpic.add(jpichint);
			
			JPanel jname = new JPanel();
			jname.setLayout(new BoxLayout(jname,BoxLayout.Y_AXIS));
			jname.setOpaque(false);
			
			JLabel jnamelabel = new JLabel(" Name");
			jnamelabel.setAlignmentX(LEFT_ALIGNMENT);
			jname.add(jnamelabel);
			JTextField jnamefield = new JTextField(name);
			jnamefield.setAlignmentX(LEFT_ALIGNMENT);
			jnamefield.setMaximumSize(new Dimension(Integer.MAX_VALUE,jnamefield.getPreferredSize().height));
			jname.add(jnamefield);
			
			jpic.setAlignmentY(TOP_ALIGNMENT);
			layer1.add(jpic);
			layer1.add(Box.createHorizontalStrut(4));
			jname.setAlignmentY(TOP_ALIGNMENT);
			layer1.add(jname);
			layer1.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
			mainPanel.add(layer1);
			
			JScrollPane pane = new JScrollPane(mainPanel,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			pane.setOpaque(false);
			pane.getViewport().setOpaque(false);
			pane.setBorder(null);
			super.add(pane,BorderLayout.CENTER);
			
			JPanel layer2 = new JPanel();
			layer2.setLayout(new BoxLayout(layer2,BoxLayout.X_AXIS));
			layer2.setOpaque(false);
			
			layer2.add(Box.createHorizontalGlue());
			JButton save = new JButton("Save");
			save.addActionListener(new Save(jnamefield));
			save.setOpaque(false);
			layer2.add(save);
			layer2.add(Box.createHorizontalStrut(2));
			JButton cancel = new JButton("Cancel");
			cancel.setOpaque(false);
			layer2.add(cancel);
			layer2.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
			layer2.setMaximumSize(new Dimension(Integer.MAX_VALUE,layer2.getPreferredSize().height));
			
			super.add(layer2,BorderLayout.SOUTH);
		}

		@Override
		public synchronized void doEvent(Contact s, Event e) {
			// TODO Auto-generated method stub

		}

		@Override
		public byte usage() {
			return (byte) 0xff;
		}
		
		public synchronized String getUserName() {
			return name;
		}

		public void setPic(ImageIcon i) {
			synchronized (this) {
				pic = i;
			}

			user.setPic(i);
		}
		
		public synchronized ImageIcon getPic() {
			return pic;
		}

		public void setConnection(Connection c,boolean checked) {
			synchronized (this) {
				connection = c;
			}

			user.message((checked?"":"~") + connection.toString(comm.DEFAULT_PORT), Color.BLACK);
		}
		
		public Component add(Component jc) {
			if (jc != null)
				mainPanel.add(jc);
			return jc;
		}

		public synchronized void save() {
			try {
				FileWriter out = new FileWriter("data/config");
				config.store(out, null);
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		private class Save implements ActionListener {
		
			private JTextField jf;
			
			public Save(JTextField jf) {
				this.jf = jf;
			}
			
			public void actionPerformed(ActionEvent arg0) {
				String n = jf.getText();
				if (n.length() > Byte.MAX_VALUE)
					n = n.substring(0,Byte.MAX_VALUE);

				user.setName(n);
				
				
				Event e = new Event(usage(),n.length()+1);
				e.buffer.put((byte) n.length());
				e.buffer.put(n.getBytes());
				e.buffer.flip();
				comm.sendEvent(e);
				
				synchronized (this) {
					name = n;
					config.setProperty("name", n);
					save();
				}
			}
		}
	}
}
