import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;


public class ServerTest {
	public final static int DEFAULT_PORT = 11111;
	
	public final static byte REQUEST_BYTE = 0x70;
	public final static byte REPLY_BYTE = 0x60;
	
	public static void main(String[] args) throws IOException {
		InetAddress dest = args.length > 0 ? InetAddress.getByName(args[0]) : InetAddress.getLocalHost();
		DatagramSocket ds = new DatagramSocket(11113);
		DatagramPacket request = new DatagramPacket(new byte[] {REQUEST_BYTE},1,dest,args.length>1?Integer.parseInt(args[1]):DEFAULT_PORT);
		ds.send(request);
		
		System.out.println("Sent request to " + (args.length>0?args[0]:"localhost") + " on port " + (args.length>1?args[1]:DEFAULT_PORT + "\ncurrent time is " + System.currentTimeMillis() + "\n"));
		
		ByteBuffer temp = ByteBuffer.allocate(15);
		DatagramPacket reply = new DatagramPacket(temp.array(),temp.limit());
		ds.receive(reply);
		byte code = temp.get();
		while (code != REPLY_BYTE) {
			System.out.println("Recieved " + code + " instead of " + REPLY_BYTE + "\nstill waiting...\n");
		}
		
		System.out.println("Recieved response!\nserver says time is " + temp.getLong() + "\nand we are " + (0xff&temp.get())+"."+(0xff&temp.get())+"."+(0xff&temp.get())+"."+(0xff&temp.get()) + " on port " + temp.getShort());
	}
}
