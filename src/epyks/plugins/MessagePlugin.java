package epyks.plugins;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.ByteBuffer;

import javax.swing.JTextArea;
import javax.swing.JTextField;

import comm.Comm;
import comm.Contact;
import comm.Event;

import epyks.PeerPanel;
import epyks.Plugin;


public class MessagePlugin extends Plugin implements ActionListener {
	
	private Comm comm;
	
	private JTextArea area;
	private JTextField field;
	
	public MessagePlugin() {
		setName("Messages");
		setLayout(new BorderLayout());
		
		area = new JTextArea();
		field = new JTextField();
		
		add(area,BorderLayout.CENTER);
		add(field,BorderLayout.SOUTH);
		
		field.addActionListener(this);
	}
	
	@Override
	public void setComm(Comm c) {
		comm = c;
	}
	
	@Override
	public byte usage() {
		return 0x22;
	}

	@Override
	public void doEvent(Contact s, Event e) {
		area.append(s.connection.toString() + " : " + new String(e.buffer.array(),e.buffer.position(),e.buffer.limit()-e.buffer.position()) + "\n");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Event to = comm.makeEvent(usage());
		to.buffer.put(field.getText().getBytes());
		to.buffer.flip();
		comm.sendEvent(to);
		area.append("me : " + field.getText() + "\n");
		field.setText("");
	}
}
