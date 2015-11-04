package bittorrent;

/**
 * The RUBTClient program implements a bit torrent client that
 * downloads the file from peer and puts it in your directory.
 *
 * @author Harshil Shah, Krupal Suthar, Aishwariya Gondhi
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import utility.Constants;

public class RUBTClient implements Constants {
	
	static TrackerInfo tInfo;
	private static Timer announceTimer = new Timer();
	private static String output_file; 
	private static Thread[] peer_threads = null;
	private static Shared memory;

	public static void main(String[] args) {			
		
		/* Checking error cases */
		
		if(args.length != 2)
			printError(INVALID_ARGS);
		
		if(args[0] == null)
			printError(NULL_FILENAME);
		
		
		output_file = args[1];
		
		
		/* Opening and reading the data inside the file */
		byte[] byteArray = null;
		
		try{
			File torrent_file = new File(args[0]);
			RandomAccessFile fileRead = new RandomAccessFile(torrent_file,"r");
			byteArray = new byte[(int) fileRead.length()];
			fileRead.read(byteArray);
			fileRead.close();
		} catch(FileNotFoundException fe){
			printError(String.format(FILE_NOT_FOUND,args[0]));
		} catch (IOException e) {
			printError(e.getMessage());
		} 
		
		
		
		/* Creating parser which will parse info from torrent_info */
		
		
		try {
			tInfo = new TrackerInfo(byteArray);
		} catch (BencodingException e1) {
			printError(e1.getMessage() + String.format(CORRUPT_FILE,args[0]));
		}
		
		
		/* Making a get request and decoding the request */
		List<Peer> peer_list = null;
		
		try {
			peer_list = tInfo.getPeers(tInfo.announce(Event.Started));
		} catch (UnknownHostException uhe){
			printError(uhe.getMessage());
		} catch (MalformedURLException e) {
			printError(String.format(INVALID_URL,args[0]));
		} catch (IOException e) {
			printError(GET_FAILED);
		} catch (BencodingException be) {
			printError(be.getMessage());
		}
		
		if(peer_list == null)
			printError(NO_PEERS_FOUND);
		else{
			memory = new Shared(tInfo.piece_hashes.length);
			peer_threads = new Thread[peer_list.size()];
			for(int i = 0; i < peer_list.size(); i++){
				peer_threads[i] = new Thread(peer_list.get(i));
				peer_threads[i].start();
			}
		}
		
		new Thread(new InputListener()).start();
		announceTimer.schedule(new Announcer(), tInfo.getInterval() * 100);

	}
	
	private static class InputListener implements Runnable{

		@Override
		public void run() {
			Scanner sc = new Scanner(System.in);
			
			do{
				System.out.println("Enter \"exit\" to stop downloading.");
			}while(!sc.nextLine().equalsIgnoreCase("exit"));
			
			System.out.println("Quiting the program...");
			
			sc.close();
			if(peer_threads != null)
				for(Thread t: peer_threads)
					t.interrupt();
			RUBTClient.writeToFile(RUBTClient.memory.getAllData(RUBTClient.tInfo.file_length));	
			try {
				RUBTClient.tInfo.announce(Event.Stopped);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			System.exit(0);
		}
		
	}
	
	private static class Announcer extends TimerTask {

		@Override
		public void run() {
			TrackerInfo ti = tInfo;
			int complete = (int)(ti.getDownloaded()*100f/ti.file_length);
			if(complete == 100){
				RUBTClient.writeToFile(RUBTClient.memory.getAllData(RUBTClient.tInfo.file_length));
				System.exit(0);
			}
			System.out.println(complete + "% complete.");
			try {
				ti.updateIntervals(ti.announce(Event.Empty));
			} catch (IOException | BencodingException e) {
				e.printStackTrace();
			} 
			announceTimer.schedule(new Announcer(), ti.getMin_interval() * 500);
		}

	}

	
	

	/**
	 * prints the given error message and exits.
	 * @param error_message
	 */
	private static void printError(String error_message){
		System.out.println(error_message);
		System.exit(1);
	}
	
	public static Shared getMemory(){
		return memory;
	}
	
		
	/**
	 * Makes a file specified by User and writes the bytes downloaded from peer.
	 * @param bytes
	 */
	public static void writeToFile(byte[] bytes){
		
		File file = new File(RUBTClient.output_file);
		
		RandomAccessFile stream = null;
		try {
			stream = new RandomAccessFile(file,"rw");
			stream.write(bytes, 0, bytes.length);		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

	}

}
