import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import epyks.Epyks;



public class Test2 {
	
	public static volatile DatagramSocket sock;
	public static volatile DatagramPacket pack;
	
	public static void main(String[] args) throws IOException {
		short mask1 = (short)0xffff;
		short mask2 = (short)0x00ff;
		
		ArrayList<Integer> list = new ArrayList<Integer>(Short.SIZE);
		int[] events = new int[Short.SIZE];
		
		go(mask1,mask2,list,events);
	}
	
	public static void go(int mask, short eventMaskS, List<Integer> l, int[] events) {
		mask &= 0xffff;
		mask ^= eventMaskS;
		for (int t=0; mask != 0; mask >>>= 0x1,t++) {
			System.out.println(mask + " ["+Integer.toHexString(mask)+"]");
    		if ((mask & 0x1) != 0)
    			l.add(events[t]);
    	}
	}
}
