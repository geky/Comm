import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.HashMap;

//Written by Geky

public class NPServer extends Thread {	
	public final static int DEFAULT_PORT = 11110;
	public final static int BUFFER_SIZE = 512;
	
	public final static byte REQUEST_BYTE = 0x70;
	public final static byte REPLY_BYTE = 0x60;
	
	public static final byte NAT_WORKAROUND_REQUEST_BYTE = 0x71;
	public static final byte NAT_WORKAROUND_FORWARD_BYTE = 0x61;
	
	public final int port;
	public HashMap<String,InetAddress> connections;
	public DatagramSocket sock;
	
	public ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
	
	public static void main(String[] args) throws IOException {
		System.out.println("NAT Punchthrough Server");
		NPServer p;
		if (args.length > 0)
			p = new NPServer(Integer.parseInt(args[0]));
		else
			p = new NPServer(DEFAULT_PORT);
		
		p.start();
	}
	
	public NPServer(int p) throws SocketException {
		port = p;
		sock = new DatagramSocket(port);
	}
	
	@Override
	public void run() {
		System.out.println("initialized\n");
		while (true) {
			
			buffer.clear();
			DatagramPacket hit = new DatagramPacket(buffer.array(),buffer.capacity());
			try {
				sock.receive(hit);
			} catch (IOException e) {
				System.err.println("IOException thrown when receiving packet!");
				e.printStackTrace();
			}
			
			buffer.limit(hit.getLength());
			byte head = buffer.get();
			
			if (head == REQUEST_BYTE) {
				
				ByteBuffer rBuffer = ByteBuffer.allocate(15);
				rBuffer.put(REPLY_BYTE);
				rBuffer.put(hit.getAddress().getAddress());
				rBuffer.putShort((short)hit.getPort());
				rBuffer.flip();
				
				DatagramPacket reply = new DatagramPacket(rBuffer.array(),rBuffer.limit(),hit.getAddress(),hit.getPort());
				try {
					sock.send(reply);
				} catch (IOException e) {
					System.err.println("IOException thrown when sending packet!");
					e.printStackTrace();
				}
				
				System.out.println("sent packet to " + hit.getAddress().toString());
				
			} else if(head == NAT_WORKAROUND_REQUEST_BYTE) {

				byte[] address = new byte[4];
				buffer.get(address);
				InetAddress target;
				try {
					target = InetAddress.getByAddress(address);
				} catch (UnknownHostException e1) {
					return;
				}
				int port = buffer.getShort();
				
				ByteBuffer rBuffer = ByteBuffer.allocate(buffer.limit());
				rBuffer.put(NAT_WORKAROUND_FORWARD_BYTE);
				rBuffer.put(buffer);
				rBuffer.flip();
				
				DatagramPacket reply = new DatagramPacket(rBuffer.array(),rBuffer.limit(),target,port);
				try {
					sock.send(reply);
				} catch (IOException e) {
					System.err.println("IOException thrown when sending packet!");
					e.printStackTrace();
				}
				
				System.out.println("forwarded packet to " + hit.getAddress().toString());
				System.out.println("\tdata: " + rBuffer.array());
			}
		}
	}
}
