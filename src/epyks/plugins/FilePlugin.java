package epyks.plugins;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import comm.Contact;
import comm.Event;
import comm.StatusListener;

import epyks.Plugin;

public class FilePlugin extends Plugin {

	//This is the default buffer size for the DeflatorOutputStream and should be what ZipOutputStream uses
	public static final int BUFFER_SIZE = 512;
	
	@Override
	public void doEvent(Contact s, Event e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public byte usage() {
		return 0x0F;
	}
	
	public void sendFile(File f,StatusListener o)  {
		
		//TODO
		Compactor c = new Compactor(f,null,o);
		c.start();
	}
	
	private class Compactor extends Thread {
		StatusListener o;
		File f;
		
		Compactor(File f, Runnable r, StatusListener o) {
			super(r);
			this.o = o;
			this.f = f;
		}
		
		public void run() {
			if (!f.exists()) {
				o.error("File Not Found");
				return;
			}
				
			try {
				FileInputStream fin = new FileInputStream(f);
				ZipOutputStream zipout = new ZipOutputStream(new FileOutputStream("data" + File.separator + "files" + File.separator + f.getName()+".zip"));
				zipout.putNextEntry(new ZipEntry(f.getPath()));
				
				byte[] buffer = new byte[BUFFER_SIZE]; 
				long size = (f.length()/BUFFER_SIZE)+1;
				long count = 0;
				
				while (true) {
					if (o != null)
						o.status("Compacting - " + (100*count++/size) + "%");
					
					int through = fin.read(buffer);
					if (through < 0)
						break;
					zipout.write(buffer, 0, through);
				}
				
				zipout.closeEntry();
				zipout.close();
				
			} catch (FileNotFoundException e) {
				if (o != null) o.error("Lost File Stream");
				return;
			} catch (IOException e) {
				if (o != null) o.error("Failed");
				return;
			}
			
			super.run();
		}
	}
	
	private class FileRep {
		
		final File path;
		volatile boolean zipped;
		
		public FileRep(File f) {
			path = f;
		}
	}
}
