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
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import utility.Constants;

public class RUBTClient implements Constants {
	
	static TrackerInfo tInfo;
	static Timer announceTimer = new Timer();
	static String output_file; 
	private static byte[] byteArray = null;

	public static void main(String[] args) {			
		
		/* Checking error cases */
		
		if(args.length != 2)
			printError(INVALID_ARGS);
		
		if(args[0] == null)
			printError(NULL_FILENAME);
		
		
		output_file = args[1];
		
		
		/* Opening and reading the data inside the file */
		
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
		
		try {
			if(tInfo.getPeers(tInfo.announce(Event.Empty)) == null)
				printError(NO_PEERS_FOUND);
		} catch (UnknownHostException uhe){
			printError(uhe.getMessage());
		} catch (MalformedURLException e) {
			printError(String.format(INVALID_URL,args[0]));
		} catch (IOException e) {
			printError(GET_FAILED);
		} catch (BencodingException be) {
			printError(be.getMessage());
		}
		
		new Thread(new InputListener()).start();
		announceTimer.schedule(new Announcer(), tInfo.getInterval() * 1000);
		
	}
	
	private static class InputListener implements Runnable{

		@Override
		public void run() {
			Scanner sc = new Scanner(System.in);
			
			do{
				System.out.println("Enter \"exit\" to quit downloading.");
			}while(!sc.nextLine().equals("quit"));
			
			System.out.println("Quiting the program...");
			
			sc.close();
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
			System.out.println(ti.getInterval());
			try {
				ti.updateIntervals(ti.announce(Event.Empty));
			} catch (IOException | BencodingException e) {
				e.printStackTrace();
			} 
			announceTimer.schedule(new Announcer(), ti.getMin_interval() * 1000);
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
