import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import epyks.Epyks;



public class Test2 {
	
	public static volatile DatagramSocket sock;
	public static volatile DatagramPacket pack;
	
	public static void main(String[] args) throws IOException {
		String s = "hello";
		System.out.println(s);
		byte[] arr = s.getBytes();
		System.out.println(arr);
		ByteBuffer b = ByteBuffer.allocate(512);
		b.putInt(20);
		b.put(arr);
		b.flip();
		System.out.println(b);
		arr = b.array();
		System.out.println(arr);
		s = new String(arr,4,512-4);
		System.out.println(s);
		
	}
}
