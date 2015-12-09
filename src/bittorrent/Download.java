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

public class Download extends Thread implements Constants, Peer {
	
	private static boolean completed = false;
	private static volatile int counter = 0;
	private static long time = System.nanoTime();
	
	private final Object GUI_INITIALIZATION_MONITOR = new Object();
    private boolean pauseThreadFlag = false;
    private boolean[] availablePieces;
    private boolean choked = false;
    private long updateTime;
    private int lastPieceSize = 0;
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
		this.lastPieceSize = (ti.file_length - (ti.piece_length * (this.numPieces - 1)));
		this.availablePieces = new boolean[numPieces];
	}
	
	public void connect() throws UnknownHostException, IOException{
		this.socket = new Socket(this.ip, this.port);
		this.socket.setSoTimeout(ti.getInterval()*10000);
		this.in = new DataInputStream(this.socket.getInputStream());
		this.out = new DataOutputStream(this.socket.getOutputStream());
		RUBTClient.print("Setting up connection with peer at " + ip);	
		RUBTClient.getPeers().add(Converter.objToStr(this.peer_id));
		counter++;
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
	
		try {
			
			// write bitfield message
			if(this.ti.getPercentDownloaded() > 0 && this.ti.getPercentDownloaded() < 100){
				byte[] data = (numPieces % 8 == 0) ? new byte[numPieces/8] : new byte[numPieces/8 + 1];	
		    	for(int i = 0; i < RUBTClient.getMemory().have.length; i++)
		    		data[i/8] = (byte) ((RUBTClient.getMemory().have[i]) ? 0x80 >> (i % 8) : 0);
		    	writeMessage(new PeerMsg.BitfieldMessage(data));
			}
			
			while(!completed && !this.isInterrupted() && this.ti.getPercentDownloaded() != 100){				
				checkForPaused();		
				interpretRcvdMsg(PeerMsg.readMessage(in,this.numPieces));
			}
			
			completed = true;
			RUBTClient.tInfo.announce(Event.Completed);
			
		} catch (IOException e) {
			RUBTClient.print("Miscommunication occurred with peer at " + this.ip);
		}
		
	}
	
	
	private void interpretRcvdMsg(PeerMsg rcvdMsg) throws IOException{
		setLastUpdated(System.nanoTime());
		if(rcvdMsg.mtype == MessageType.BitField){
			evaluateBitfield(rcvdMsg);
			writeMessage(new PeerMsg(MessageType.Interested));
		} else if(rcvdMsg.mtype == MessageType.Un_Choke){
			this.choked = false;
			requestPiece();
		} else if(rcvdMsg.mtype == MessageType.Choke){
			this.choked = true;
		} else if(rcvdMsg.mtype == MessageType.Piece){
			evaluatePiece(rcvdMsg);
			requestPiece();
		} else if(rcvdMsg.mtype == MessageType.Have){
			this.availablePieces[rcvdMsg.pieceIndex] = true;
			RUBTClient.incrRare(rcvdMsg.pieceIndex);
		} else if(rcvdMsg.mtype == MessageType.Choke){
			this.choked = true;
		} else{
			writeMessage(new PeerMsg(MessageType.Keep_Alive));
		}
	}
	
	private void evaluateBitfield(PeerMsg rcvdMsg){
		boolean[] bits = Converter.bytesToBoolean(Arrays.copyOfRange(rcvdMsg.msg, 5, rcvdMsg.msg.length));
	    int min = (int) Math.min(availablePieces.length, bits.length);
	    System.arraycopy(bits, 0, availablePieces, 0, min);
	    for(int k = 0; k < availablePieces.length; k++)
	    	if(availablePieces[k])
	    		RUBTClient.incrRare(k);
	}
	
	private void evaluatePiece(PeerMsg ret){
		Shared shm = RUBTClient.getMemory();
		int counter = ret.pieceIndex;
		int o = ret.begin;
		int passLen = (counter != this.numPieces-1) ? 16384*2 : lastPieceSize;
		if(shm.put(Arrays.copyOfRange(ret.msg, 13, ret.msg.length), o, counter, passLen) && shm.have[counter])
			if(!isValidPiece(counter)){
				RUBTClient.print("Invalid piece sent. The SHA-1 hash did not match!");
				shm.remove(counter);
			}else{
				this.ti.setDownloaded(this.ti.getDownloaded()+passLen);
			}
	}
	
	private void requestPiece() throws IOException{
		
		Shared shm = RUBTClient.getMemory();
		int rLen = 16384;
		int limit = ti.piece_hashes.length * (ti.piece_length/rLen);
		
		for(int counter = 0; counter < limit; counter++){
			if (shm.have[counter/2]) 
				continue;
			if(counter == limit-1) // if we are requesting the last piece
				rLen = lastPieceSize - (( (ti.piece_length / rLen) - 1 ) * 16384);
			int start = (counter%2) * 16384;
			writeMessage(new PeerMsg.RequestMessage(counter/2, start, rLen));
		}
	}
	
	
	/*private void requestPiece() throws IOException{
		int index = RUBTClient.getRarestPieceIndex(this.availablePieces);
		if (index == -1) return;
		int rLen = (index != numPieces - 1) ? 16384 : lastPieceSize - 16384;
		writeMessage(new PeerMsg.RequestMessage(index, 0, rLen));
		writeMessage(new PeerMsg.RequestMessage(index, 16384, rLen));
	}*/

	
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
	
	public boolean isChoked(){
		return choked;
	}
	
	@Override
	public void choke(){
		try {
			writeMessage(new PeerMsg(MessageType.Choke));
		} catch (IOException e) {}
	}
	
	@Override
	public void unchoke(){
		try {
			writeMessage(new PeerMsg(MessageType.Un_Choke));
		} catch (IOException e) {}
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
		
		try {
			
			connect();
			
			// validate the info hash and then start messaging 
			if(RUBTClient.tInfo.getPercentDownloaded() != 100)
				if(isValid(handshake()))
					startMessaging();
				else
					RUBTClient.print("Handshake verification failed.");
			
		}catch (IOException e) {
			RUBTClient.print("Connection with peer at " + ip + " timed out.");
		}finally {
			this.disconnect();
		}
	}
	
	private void checkForPaused() {
        synchronized (GUI_INITIALIZATION_MONITOR) {
            while (pauseThreadFlag) {
                try {
                    GUI_INITIALIZATION_MONITOR.wait();
                } catch (Exception e) {}
            }
        }
    }
	
	public void pause() throws InterruptedException {
        pauseThreadFlag = true;
    }

    public void resumeDownload() {
        synchronized(GUI_INITIALIZATION_MONITOR) {
            pauseThreadFlag = false;
            GUI_INITIALIZATION_MONITOR.notify();
        }
    }
	
	public void disconnect(){
		RUBTClient.print("Disconnecting from peer with ip: " + this.ip);
		try {
			this.in.close();
			counter--;
		} catch (Exception e) {
		}
		try {
			this.out.close();
		} catch (Exception e) {
		}
		try {
			this.socket.close();
		} catch (Exception e) {
		}
		RUBTClient.getPeers().remove(Converter.objToStr(peer_id));
		interrupt();
	}

	/**
	 * @return the completed
	 */
	boolean isCompleted() {
		return completed;
	}

	/**
	 * @return the port
	 */
	int getPort() {
		return port;
	}

	/**
	 * @return the ip
	 */
	public String getIp() {
		return ip;
	}

	/**
	 * @return the peer_id
	 */
	public byte[] getPeer_id() {
		return peer_id;
	}

	/**
	 * @return the numPieces
	 */
	int getNumPieces() {
		return numPieces;
	}

	public static double countTime(){
		return (System.nanoTime() - time)/1000000000;
	}
	
	public static long getTime(){
		return time;
	}
	
	public static void setTime(long t){
		time = t;
	}
	
	public static int getNum(){
		return counter;
	}

	@Override
	public void setLastUpdated(long l) {
		updateTime = l;
	}

	@Override
	public long getLastUpdated() {
		return updateTime;
	}
}
