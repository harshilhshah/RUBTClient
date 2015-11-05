package bittorrent;

/**
 * @author Harshil Shah, Krupal Suthar, Aishwariya Gondhi
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import utility.Constants;

public class Download implements Constants, Runnable {
	
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
			try{
				this.socket.setSoTimeout(ti.getInterval()*1000);
			}catch(SocketException se){
				System.out.println("Connection timed out with peer at " +ip);
			}
			this.in = new DataInputStream(this.socket.getInputStream());
			this.out = new DataOutputStream(this.socket.getOutputStream());
			System.out.println("Connection established with peer at " + ip);			
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
		if(!Arrays.equals(spliced_arr,Download.BT_PROTOCOL))
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
			
			System.out.println("Recieved " + PeerMsg.decodeMessageType(in,this.numPieces).mtype.name());
			
			System.out.println("Sending Interested to the peer at " + ip);
			writeMessage(new PeerMsg(MessageType.Interested));
			
			while(PeerMsg.decodeMessageType(in,this.numPieces).mtype != MessageType.Un_Choke){
				System.out.println("Peer at " + ip + " unchoked the connection. Starting requesting.");
				writeMessage(new PeerMsg(MessageType.Interested));
			}
			
			int rLen = 16384;
			int limit = ti.piece_hashes.length * (ti.piece_length/rLen);
			int lastPieceSize = (ti.file_length - (ti.piece_length * (this.numPieces - 1)));
			
			for(int counter = 0; counter < limit && ti.getPercentDownloaded() != 100; counter++){
				
				if(shm.have[counter/2])
					continue;
				
				if(counter == limit-1)
					rLen = lastPieceSize - (( (ti.piece_length / rLen) - 1 ) * 16384);
				
				int start = (counter%2) * rLen;
				int passLen = (counter/2 != this.numPieces-1) ? rLen*2 : lastPieceSize;
				int o = (counter%2)*16384;
				
				writeMessage(new PeerMsg.RequestMessage(rLen, start, counter/2));
				PeerMsg ret = PeerMsg.decodeMessageType(in,this.numPieces);
				
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
				
				if(this.ti.getPercentDownloaded() == 100){							
					System.out.println("Time taken to download: " + 
							(System.nanoTime() - time)/1000000000 + " seconds.");
					RUBTClient.announceTimer.cancel();
					RUBTClient.tInfo.announce(Event.Completed);
					return;
				}
			}
			
		} catch (IOException e) {
			System.out.println(e.getMessage());
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
