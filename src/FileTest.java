import java.io.File;

import comm.StatusObserver;
import epyks.plugins.FilePlugin;


public class FileTest implements StatusObserver {
	public static void main(String[] args) {
		File f = new File(args[0]);
		System.out.println("Using:" + f.toString());
		System.out.println("Does it exist: " + f.exists());
		FilePlugin fp = new FilePlugin();
		fp.sendFile(f, new FileTest());
	}

	@Override
	public void status(String s) {
		System.out.println(s);
	}

	@Override
	public void error(String s) {
		System.err.println(s);
	}
}
