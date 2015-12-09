package bittorrent;

/**
 * The RUBTClient program implements a bit torrent client that
 * downloads the file from peer and puts it in your directory.
 *
 * @author Harshil Shah, Krupal Suthar, Aishwarya Gondhi
 */

import gui.FilePanel;
import gui.MainView;

import java.awt.EventQueue;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import utility.Constants;

public class RUBTClient implements Constants {
	
	public static TrackerInfo tInfo;
	public static boolean terminate = false;
	public static Timer announceTimer = new Timer();
	public static File output_file; 
	
	private static Timer optimizer = new Timer();
	private static List<String> connectedPeers = new ArrayList<>();
	private static Download[] peer_threads = null;
	private static int[] rare;
	private static Shared memory;
	private static ArrayList<Upload> uploaders = new ArrayList<>();
	private static ServerSocket ss;
	private static MainView gui;
	
	public static void main(String[] args) {
		
		/* Checking error cases */
		if(args.length != 2 && args.length != 0)
			printError(INVALID_ARGS);
		
		if(!(args.length == 2 && args[0] != null)){
			EventQueue.invokeLater(new Runnable(){

				@Override
				public void run() {
					gui = new MainView();
				}
			
			});
		}else{		
			output_file = new File(args[1]);		
			if(RUBTClient.createTrackerInfo(new File(args[0])))
				RUBTClient.setPeerThreads(new File(args[0]));
			new Thread(new InputListener()).start();
			announceTimer.schedule(new Announcer(), tInfo.getMin_interval() * 1000);			
		}
		
		
		/* Upload */
		
		try {
			ss = new ServerSocket(TrackerInfo.port);
			ss.setSoTimeout(3000000);
			print("Listening for connections on " + ss.getInetAddress().getHostAddress() 
					+ ":" + TrackerInfo.port);
		} catch (IOException e) {
			print("Unable to create a server socket");
			return;
		}
		
		while(!terminate){
			try{
    			Socket sckt = ss.accept();
    			uploaders.add(new Upload(sckt));
    			FilePanel.updateNumSeeders();
    		}catch(IOException e){
    			print("Uploader socket timed out.");
    		}
		}

	}
	
	private static class OptimizedChoke extends TimerTask {

		@Override
		public void run() {
			
			ArrayList<Peer> peers = new ArrayList<>(uploaders);
			List<Peer> dpeers = Arrays.asList(peer_threads);
			peers.addAll(dpeers);
			Peer peer_to_add = null;
			
			if(peers.size() > 2){
				
				int mi = -1;
				int numUnChokedPeers = 0;
				
				// make sure we have at most 5 unchoked connections.
				for(Peer p: peers){
					if(p.isChoked() && numUnChokedPeers < 6){
						p.unchoke();
						numUnChokedPeers++;
					} else if(!p.isChoked() && numUnChokedPeers > 5){
						p.choke();
						numUnChokedPeers--;
					}
				}
				
				// Find the worst unchoked peer
				for(int j = 0; j < peers.size(); j++)
					if(!peers.get(j).isChoked()){
						if(mi == -1) mi = j;
						mi = (peers.get(mi).getLastUpdated() < peers.get(j).getLastUpdated()) ? j: mi;
						peers.get(j).setLastUpdated(0);
						numUnChokedPeers++;
					}
					
				for(Peer p: peers)
					if(p.isChoked())
						peer_to_add = p;
				
				if(mi != -1 && peer_to_add != null){
					peers.get(mi).choke();
					peer_to_add.unchoke();
				}
			}
			
			optimizer.schedule(new OptimizedChoke(), 30 * 1000);
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
			sc.close();	
			quit();
		}
		
	}
	
	private static class Announcer extends TimerTask {

		@Override
		public void run() {
			
			TrackerInfo ti = tInfo;
			
			try {
				ti.updateIntervals(ti.announce(Event.Empty));
				print("Downloaded: " + ti.getDownloaded() + " Uploaded: " + ti.getUploaded());
			} catch (IOException | BencodingException e) {
				e.printStackTrace();
			} 
			
			announceTimer.schedule(new Announcer(), ti.getInterval() * 1000);
		}

	}
	
	private static byte[] readTorrentFile(File torrent_file){
		
		try{
			RandomAccessFile fileRead = new RandomAccessFile(torrent_file,"r");
			byte[] byteArray = new byte[(int) fileRead.length()];
			fileRead.read(byteArray);
			fileRead.close();
			return byteArray;
		} catch(FileNotFoundException fe){
			printError(String.format(FILE_NOT_FOUND,torrent_file.getName()));
		} catch (IOException e) {
			printError(e.getMessage());
		} catch (NullPointerException npe){
			printError(NULL_FILENAME);
		}
		return null;
	}
	
	public static boolean createTrackerInfo(File f){
		try {
			tInfo = new TrackerInfo(readTorrentFile(f));
			rare = new int[tInfo.piece_hashes.length];
			return true;
		} catch (BencodingException e1) {
			printError(e1.getMessage() + String.format(CORRUPT_FILE,f.getName()));
		} catch (IllegalArgumentException iae){
		} catch (NullPointerException nil){
			printError(INVALID_TYPE);
		}
		return false;
	}
	
	public static void setPeerThreads(File f){
		
		memory = new Shared(tInfo.piece_hashes.length);
		
		if(output_file != null && output_file.exists())
        	memory.readFile(output_file);
		
		List<Download> peer_list = null;
		
		try {
			peer_list = tInfo.getPeers(tInfo.announce(Event.Started));
		} catch (UnknownHostException uhe){
			printError(uhe.getMessage());
		} catch (MalformedURLException e) {
			printError(String.format(INVALID_URL,f.getName()));
		} catch (IOException e) {
			printError(GET_FAILED);
		} catch (BencodingException be) {
			printError(be.getMessage());
		}
		
		/* Setting up threads for each peer */
		if(peer_list != null){
			peer_threads = new Download[peer_list.size()];
			for(int i = 0; i < peer_list.size(); i++){
				peer_threads[i] = peer_list.get(i);
				peer_threads[i].start();
			}
			optimizer.schedule(new OptimizedChoke(), 30000);
		}else{
			printError(NO_PEERS_FOUND);
		}
	}
	
	public static void incrRare(int index){
		rare[index]++;
	}
	
	public synchronized static int getRarestPieceIndex(boolean[] peer_has){
		List<Integer> intList = new ArrayList<>();
		for(int i = 0; i < rare.length; i++)
			intList.add(i);
		Collections.shuffle(intList);
		int rarestIndex = -1;
		for (Integer i: intList) 
			if(peer_has[i.intValue()] && !RUBTClient.getMemory().have[i.intValue()])
				if (rarestIndex == -1)
					rarestIndex = i.intValue();
				else if (rare[rarestIndex] > rare[i.intValue()])
					rarestIndex = i.intValue();
		return rarestIndex;
	}
	
	/**
	 * prints the given error message and exits.
	 * @param error_message
	 */
	static void printError(String error_message){
		if(gui == null){
			System.out.println(error_message);
			System.exit(1);
		}else{
			gui.display(error_message);
		}
	}
	
	static void print(String msg){
		if(gui == null)
			System.out.println(msg);
		else
			gui.display(msg);
	}
	
	static void updateProgress(int prog){
		if(gui != null){
			gui.updateProgress(prog);
			return;
		}
		System.out.println(prog + "% completed.");
		if(prog == 100) 				
			System.out.println("Time taken to download: " + Download.countTime() + " seconds.");		
	}
	
	public static MainView getGUI(){
		return gui;
	}
	
	public static Shared getMemory(){
		return memory;
	}
	
	public static List<String> getPeers(){
		return connectedPeers;
	}
	
	private static void stopUploaders(){
		for(Upload u: uploaders)
			u.disconnect();
		try{ ss.close(); } catch(IOException e){};
	}

	public static void stopDownloaders(boolean end) {
		if(peer_threads != null)
			for(Download t: peer_threads)
				if(end)
					t.interrupt();
				else
					try {
						t.pause();
					} catch (InterruptedException e) {
						printError("Don't interrupt me.");
					}
		Download.setTime(System.nanoTime() - Download.getTime());
	}
	
	public static synchronized boolean resumeDownloaders(){
		Download.setTime(System.nanoTime() - Download.getTime());
		if(peer_threads == null) return false;
		for(Download t: peer_threads)
			t.resumeDownload();
		return true;
 	}
	
	/**
	 * Makes a file specified by User and writes the bytes downloaded from peer.
	 * @param bytes
	 */
	private static void writeFile(){
		
		byte[] bytes = memory.getAllData(tInfo.file_length, tInfo.piece_length);
		
		RandomAccessFile stream = null;
		try {
			stream = new RandomAccessFile(output_file,"rw");
			stream.write(bytes, 0, bytes.length);	
			print("File " + output_file + " has been saved.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
            try {
                stream.close();
            } catch (IOException e) {
                printError("Already Closed.");
            }
        }

	}
	
	public static void quit(){
		terminate = true;	
		if(tInfo != null){
			stopDownloaders(true);
			stopUploaders();
			writeFile();	
			try {
				tInfo.announce(Event.Stopped);
			} catch (IOException e) {}
			announceTimer.cancel();
			optimizer.cancel();
		}
		System.exit(0);
	}

}
