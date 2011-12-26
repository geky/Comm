package comm.filesender;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import comm.Comm;
import comm.Contact;
import comm.Event;
import comm.StatusListener;

public class FileSender{

	public static final byte PUT_FILE = 0x10;
	public static final byte PUT_COMPRESSED_FILE = 0x11;
	public static final byte PULL_REQUEST = 0x20;
	public static final byte PUSH_REQUEST = 0x30;
	public static final byte RM_FILE = 0x70; 
	
	private List<File> files = new LinkedList<File>();
	
	private Comm comm;
	private File data;
	
	public FileSender(File holder) {
		if (!holder.isDirectory())
			throw new RuntimeException("Must be a directory");
		data = holder;
	}
	
	public void pullFile(File f, StatusListener o) {
		
	}
	
	public void pushFile(File f, StatusListener o) {
		
	}
	
	public void putFile(File f, boolean comp, StatusListener o) throws FileNotFoundException {
		try {
			File out = new File(data.getAbsolutePath()+File.separator+f.getName());
			
			for (int n=0; out.exists(); n++) {
				out = new File(data.getAbsolutePath()+File.separator+f.getName() + "(" + n + ")");
			}
			
			try {
				out.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			new Mover(new FileInputStream(f),new FileOutputStream(out),o).start();
		} catch (FileNotFoundException e) {
			if (o != null) o.error("File Not Found");
			throw e;
		}
		
		Event e = comm.makeEvent();
		e.buffer.put(usage());
		e.buffer.put(comp?PUT_COMPRESSED_FILE:PUT_FILE);
		String n = f.getName();
		e.buffer.put((byte) n.length());
		e.buffer.put(n.getBytes());
		e.buffer.flip();
		comm.sendEvent(e);
	}
	
	//@Override
	public void doEvent(Contact s, Event e) {
		// TODO Auto-generated method stub
		
	}
	
	//@Override
	public void pollData(Contact s, ByteBuffer b) {}

	//@Override
	public void doData(Contact s, ByteBuffer b) {}


	//@Override
	public byte usage() {
		return 0xf;
	}

	//@Override
	public void setComm(Comm c) {
		comm = c;
	}
	
	private class Mover extends Thread {
		private InputStream i;
		private OutputStream o;
		private StatusListener so;
		
		Mover(InputStream i, OutputStream o, StatusListener so) {
			this.i = i;
			this.o = o;
			this.so = so;
		}
		
		public void run() {
			
		}
	}

	//@Override
	public int getMinimumRateDelay() {
		return -1;
	}

	//@Override
	public int getMaximumRateDelay() {
		return -1;
	}
}
