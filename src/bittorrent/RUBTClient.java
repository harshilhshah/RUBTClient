package bittorrent;

/**
 * The RUBTClient program implements a bit torrent client that
 * downloads the file from peer and puts it in your directory.
 *
 * @author Harshil Shah, Krupal Suthar, Aishwarya Gondhi
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import utility.Constants;

public class RUBTClient implements Constants {
	
	static TrackerInfo tInfo;
	static boolean terminate = false;
	static Timer announceTimer = new Timer();
	static File output_file; 
	static List<String> connectedPeers = new ArrayList<>();
	
	private static Thread[] peer_threads = null;
	private static Shared memory;
	private static Stack<Thread> uploaders = new Stack<>();

	public static void main(String[] args) {			
		
		/* Checking error cases */
		
		if(args.length != 2)
			printError(INVALID_ARGS);
		
		if(args[0] == null)
			printError(NULL_FILENAME);
		
		
		output_file = new File(args[1]);
		
		
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
		
		
		/* Starting a thread that listens if user wants to quit.*/
		new Thread(new InputListener()).start();
		
		/* Scheduling tracker announcement*/
		announceTimer.schedule(new Announcer(), tInfo.getMin_interval() * 1000);
		
		
		/* Making a get request and decoding the request */
		
		List<Download> peer_list = null;
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
		
		
		/* Setting up threads for each peer */
		
		if(peer_list == null){
			System.out.println(NO_PEERS_FOUND);
		}else{
			memory = new Shared(tInfo.piece_hashes.length);
			peer_threads = new Thread[peer_list.size()];
			for(int i = 0; i < peer_list.size(); i++){
				peer_threads[i] = new Thread(peer_list.get(i));
				peer_threads[i].start();
			}
		}

		ServerSocket ss;
		try {
			ss = new ServerSocket(TrackerInfo.port);
			ss.setSoTimeout(100000);
			System.out.println("Listening for connections on " 
					+ ss.getInetAddress().getHostAddress() + ":" + TrackerInfo.port);
		} catch (IOException e) {
			System.out.println("Unable to create a server socket");
			return;
		}
		
		while(!terminate){
			try{
    			Socket sckt = ss.accept();
    			uploaders.push(new Thread(new Upload(sckt)));
    			uploaders.peek().start();
    		}catch(IOException e){
    			System.out.println("Server socket timed out.");
    		}
		}
		while(!uploaders.isEmpty())
			uploaders.pop().interrupt();
		try {
			ss.close();
		} catch (IOException e) {
			return;
		}
	}
	
	private static class InputListener implements Runnable{

		@Override
		public void run() {
			Scanner sc = new Scanner(System.in);
			
			do{
				System.out.println("Enter \"quit\" to stop the program.");
			}while(!sc.nextLine().equalsIgnoreCase("quit"));
			
			System.out.println("Quiting the program...");
			
			terminate = true;
			sc.close();
			
			if(peer_threads != null)
				for(Thread t: peer_threads)
					t.interrupt();
			
			writeFile();	
			
			try {
				RUBTClient.tInfo.announce(Event.Stopped);
			} catch (IOException e) {}
			announceTimer.cancel();
			System.exit(0);
		}
		
	}
	
	private static class Announcer extends TimerTask {

		@Override
		public void run() {
			
			TrackerInfo ti = tInfo;
			
			try {
				ti.updateIntervals(ti.announce(Event.Empty));
				System.out.println("Downloaded: " + ti.getDownloaded() + " Uploaded: " + ti.getUploaded());
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
	
	public static Shared getMemory(){
		return memory;
	}
	
		
	/**
	 * Makes a file specified by User and writes the bytes downloaded from peer.
	 * @param bytes
	 */
	public static void writeFile(){
		
		byte[] bytes = memory.getAllData(tInfo.file_length, tInfo.piece_length);
		
		RandomAccessFile stream = null;
		try {
			stream = new RandomAccessFile(output_file,"rw");
			stream.write(bytes, 0, bytes.length);	
			System.out.println("File " + output_file + " has been saved.");
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
