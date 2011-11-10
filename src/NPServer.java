import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.HashMap;

public class NPServer extends Thread {	
	public final static int DEFAULT_PORT = 11110;
	
	public final static byte REQUEST_BYTE = 0x70;
	public final static byte REPLY_BYTE = 0x60;
	
	public final int port;
	public HashMap<String,InetAddress> connections;
	public DatagramSocket sock;
	
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
			byte[] buffer = new byte[1];
			DatagramPacket hit = new DatagramPacket(buffer,buffer.length);
			try {
				sock.receive(hit);
			} catch (IOException e) {
				System.out.println("IOException thrown when receiving packet!");
				e.printStackTrace();
			}
			if (buffer[0] == REQUEST_BYTE) {
				ByteBuffer rBuffer = ByteBuffer.allocate(15);
				rBuffer.put(REPLY_BYTE);
				long time = System.currentTimeMillis();
				rBuffer.putLong(time);
				rBuffer.put(hit.getAddress().getAddress());
				rBuffer.putShort((short)hit.getPort());
				rBuffer.flip();
				
				DatagramPacket reply = new DatagramPacket(rBuffer.array(),rBuffer.limit(),hit.getAddress(),hit.getPort());
				try {
					sock.send(reply);
				} catch (IOException e) {
					System.out.println("IOException thrown when sending packet!");
					e.printStackTrace();
				}
				
				System.out.println("sent packet to " + hit.getAddress().toString() + " at time " + time);
			}
		}
	}
}
