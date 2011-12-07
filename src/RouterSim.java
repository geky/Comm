import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import comm.Connection;


public class RouterSim {
	public static final int FAST_DELAY = 10;
	public static final int SLOW_DELAY = 200;
	
	public static final int BUFFER_SIZE = 512;
	
	public static Connection from;
	public static Connection to;
	
	public static DatagramSocket socket;
	
	public volatile static int buflen = 64;
	public volatile static int delay = FAST_DELAY;
	public volatile static BlockingQueue<byte[]> fromQueue = new ArrayBlockingQueue<byte[]>(buflen);
	public volatile static BlockingQueue<byte[]> toQueue = new ArrayBlockingQueue<byte[]>(buflen);
	
	private volatile static boolean go = true;
	
	public static void main(String[] args) throws UnknownHostException, SocketException {
		
		from = new Connection(args[0]);
		to = new Connection(args[1]);
		
		socket = new DatagramSocket(11111);
		
		System.out.println("port : " + socket.getLocalPort());
		
		new Receiver().start();
		new Sender().start();
		
		Scanner s = new Scanner(System.in);
		while (go) {
			try {
				switch (s.next().charAt(0)) {
					case 's' : buflen = s.nextInt(); break;
					case 'q' : int i = s.nextInt();
							   BlockingQueue<byte[]> temp = new ArrayBlockingQueue<byte[]>(i);
							   fromQueue.drainTo(temp,i);
							   fromQueue = temp;
							   temp = new ArrayBlockingQueue<byte[]>(i);
							   toQueue.drainTo(temp,i);
							   toQueue = temp;
							   break;
					case 'd' : delay = s.nextInt(); break;
				}
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
	}

	public static class Receiver extends Thread {
		byte[] buffer = new byte[BUFFER_SIZE];
		
		
		public void run() {
			while (go) {
				DatagramPacket dp = new DatagramPacket(buffer,BUFFER_SIZE);
				try {
					socket.receive(dp);
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				if (new Connection(dp).equals(to)) {
					if (fromQueue.size() > buflen) {
						try {
							Thread.sleep(SLOW_DELAY);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
					try {
						fromQueue.put(Arrays.copyOf(buffer, dp.getLength()));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else { 
					if (toQueue.size() > buflen) {
						try {
							Thread.sleep(SLOW_DELAY);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					
					try {
						toQueue.put(Arrays.copyOf(buffer, dp.getLength()));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public static class Sender extends Thread {
		public void run() {
			while (go) {
				try {
					byte[] holder = toQueue.poll();
					if (holder != null) {
						DatagramPacket dp = new DatagramPacket(holder,holder.length,to.address,to.port);
						socket.send(dp);
					}

					holder = fromQueue.poll();
					if (holder != null) {
						DatagramPacket dp = new DatagramPacket(holder,holder.length,from.address,from.port);
						socket.send(dp);
					}
				

					Thread.sleep(delay);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
