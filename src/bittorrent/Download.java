package bittorrent;

/**
 * @author Harshil Shah, Krupal Suthar, Aishwarya Gondhi
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import utility.Constants;
import utility.Converter;

public class Download implements Constants, Runnable {
	
	private static boolean completed = false;
	private static int complete = 0;
	private static long time = System.nanoTime();
	
	private int port;
	private String ip;
	private byte[] info_hash;
	private byte[] peer_id;
	private DataInputStream in;
	private DataOutputStream out;
	private TrackerInfo ti;
	private Socket socket = null;
	private int numPieces = 0;
	
	public Download(int port, String ip_address, byte[] id, TrackerInfo p) {
		this.port = port;
		this.ip = ip_address;
		this.peer_id = id;
		this.ti = p;
		this.info_hash = p.info_hash.array();
		this.numPieces = this.ti.piece_hashes.length;
	}
	
	public void connect() throws UnknownHostException, IOException{
		this.socket = new Socket(this.ip, this.port);
		this.socket.setSoTimeout(ti.getInterval()*1000);
		this.in = new DataInputStream(this.socket.getInputStream());
		this.out = new DataOutputStream(this.socket.getOutputStream());
		System.out.println("Setting up connection with peer at " + ip);	
		RUBTClient.connectedPeers.add(Converter.objToStr(this.peer_id));
	}
	
	
	/**
	 * This method validates the handshake response
	 * @param byte[]
	 * @return boolean
	 */
	private boolean isValid(byte[] handshake) {
		
		if(handshake == null || handshake.length != 68 || handshake[0] != 19)
			return false;
		
		byte[] spliced_arr = new byte[19];
		System.arraycopy(handshake, 1, spliced_arr, 0, 19);
		if(!Arrays.equals(spliced_arr,BT_PROTOCOL))
			return false;
		
		spliced_arr = new byte[20];
		System.arraycopy(handshake, 28, spliced_arr, 0, 20);
		if(!Arrays.equals(spliced_arr,this.info_hash))
			return false;
		
		spliced_arr = new byte[20];
		System.arraycopy(handshake, 28, spliced_arr, 0, 20);
		return !Arrays.equals(spliced_arr,this.peer_id);
		
	}
	
	private boolean isValidPiece(int index){
		try {
			if(index == numPieces-1) return true;
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			byte[] hashed = md.digest(RUBTClient.getMemory().get(index));
			return Arrays.equals(this.ti.piece_hashes[index].array(), hashed);
		} catch (NoSuchAlgorithmException e) {
			return false;
		}
	}

	
	/**
	 * This method communicates with the peer
	 * @return byte[]
	 */
	private void startMessaging() {
		
		Shared shm = RUBTClient.getMemory();
	
		try {
			
			if(this.ti.getPercentDownloaded() > 0 && this.ti.getPercentDownloaded() < 100){
				byte[] data = (numPieces % 8 == 0) ? new byte[numPieces/8] : new byte[numPieces/8 + 1];	
		    	for(int i = 0; i < RUBTClient.getMemory().have.length; i++)
		    		data[i/8] = (byte) ((RUBTClient.getMemory().have[i]) ? 0x80 >> (i % 8) : 0);
		    	writeMessage(new PeerMsg.BitfieldMessage(data));
			}
			
			PeerMsg.readMessage(in,this.numPieces).mtype.name();
			
			writeMessage(new PeerMsg(MessageType.Interested));
			
			while(PeerMsg.readMessage(in,this.numPieces).mtype != MessageType.Un_Choke)
				writeMessage(new PeerMsg(MessageType.Interested));
			
			if(!completed)
				System.out.println("Connection with peer:" + ip + " is now unchoked and interested. Starting to request.");
			
			int rLen = 16384;
			int limit = ti.piece_hashes.length * (ti.piece_length/rLen);
			int lastPieceSize = (ti.file_length - (ti.piece_length * (this.numPieces - 1)));
			
			for(int counter = 0; counter < limit && !completed; counter++){
				
				if(shm.have[counter/2])
					continue;
				
				if(counter == limit-1)
					rLen = lastPieceSize - (( (ti.piece_length / rLen) - 1 ) * 16384);
				
				int start = (counter%2) * rLen;
				int passLen = (counter/2 != this.numPieces-1) ? rLen*2 : lastPieceSize;
				int o = (counter%2)*16384;
				
				writeMessage(new PeerMsg.RequestMessage(counter/2, start, rLen));
				PeerMsg ret = PeerMsg.readMessage(in,this.numPieces);
				
				if(shm.put(Arrays.copyOfRange(ret.msg, 13, ret.msg.length), o, counter/2, passLen) && shm.have[counter/2]){
					if(!isValidPiece(counter/2)){
						System.out.println("Invalid piece sent. The SHA-1 hash did not match!");
						shm.remove(counter/2);
					}else{
						writeMessage(new PeerMsg.HaveMessage(counter/2));
						this.ti.setDownloaded(this.ti.getDownloaded()+passLen);
						if(this.ti.getPercentDownloaded() != complete){
							complete = this.ti.getPercentDownloaded();
							System.out.println(complete + "% completed.");
						}
					}
				}
				
				if(!completed && this.ti.getPercentDownloaded() == 100){							
					System.out.println("Time taken to download: " + 
							(System.nanoTime() - time)/1000000000 + " seconds.");
					completed = true;
					RUBTClient.announceTimer.cancel();
					RUBTClient.tInfo.announce(Event.Completed);
					return;
				}
			}
			
		} catch (IOException e) {
			System.out.println("Not communicating properly with the peer.");
		}
		
	}

	
	/**
	 * This method makes a handshake with the peer
	 * @return byte[]
	 */
	public byte[] handshake() throws IOException{
		
		byte[] send_message = new byte[68];
		byte[] recieve_message = new byte[68];
		
		send_message[0] = 19;
		System.arraycopy(BT_PROTOCOL, 0, send_message, 1, BT_PROTOCOL.length);
		System.arraycopy(this.info_hash, 0, send_message, 28, 20);
		System.arraycopy(TrackerInfo.my_peer_id, 0, send_message, 48, 20);
		
		this.out.write(send_message);
		this.out.flush();
		this.in.read(recieve_message);
		
		return recieve_message;
		
	}
	
	/**
	 * This method sends the given message
	 * @param PeerMsg
	 */
	private void writeMessage(PeerMsg pm) throws IOException{
		if (this.socket.isClosed())
			return;
		this.out.write(pm.getMessage());
		this.out.flush();
	}

	@Override
	public void run() {
		// create a connection
		try {
			connect();
			
			// validate the info hash and then start messaging 
			if(RUBTClient.tInfo.getPercentDownloaded() != 100)
				if(isValid(handshake()))
					startMessaging();
				else
					System.out.println("Handshake verification failed.");
			
			if(RUBTClient.tInfo.getPercentDownloaded() != 100)
				System.out.println("There were invalid pieces sent.");
			
			RUBTClient.connectedPeers.remove(Converter.objToStr(this.peer_id));
			this.disconnect();
			
		}catch (IOException e) {
			e.printStackTrace();
		}finally {
			this.disconnect();
		}
	}
	
	public void disconnect(){
		try {
			this.in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			this.out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			this.socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
