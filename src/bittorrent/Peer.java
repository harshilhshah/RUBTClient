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
import java.util.Arrays;
import java.util.concurrent.ArrayBlockingQueue;

import bittorrent.PeerMsg.RequestMessage;
import utility.Constants;

public class Peer implements Constants, Runnable {
	
	private int port;
	private String ip;
	private byte[] info_hash;
	private byte[] peer_id;
	private DataInputStream in;
	private DataOutputStream out;
	private TrackerInfo ti;
	private Socket socket = null;
	private int numPieces = 0;
	private Shared mem;
	private ArrayBlockingQueue<PeerMsg> requestQueue;
	
	public Peer(int port, String ip_address, byte[] id, TrackerInfo p) {
		this.port = port;
		this.ip = ip_address;
		this.peer_id = id;
		this.ti = p;
		this.info_hash = p.info_hash.array();
		this.numPieces = this.ti.piece_hashes.length;
		this.mem = new Shared(this.numPieces);
		this.requestQueue = new ArrayBlockingQueue<PeerMsg>(this.numPieces);
	}
	
	public void connect() throws UnknownHostException, IOException{
			this.socket = new Socket(this.ip, this.port);
			try{
				this.socket.setSoTimeout(ti.getInterval()*1000);
			}catch(SocketException se){
				System.out.println("Connection timed with peer " + this.peer_id);
			}
			this.in = new DataInputStream(this.socket.getInputStream());
			this.out = new DataOutputStream(this.socket.getOutputStream());
			System.out.println("Connection established with peer " + this.peer_id);
			
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
		if(!Arrays.equals(spliced_arr,Peer.BT_PROTOCOL))
			return false;
		
		spliced_arr = new byte[20];
		System.arraycopy(handshake, 28, spliced_arr, 0, 20);
		if(!Arrays.equals(spliced_arr,this.info_hash))
			return false;
		
		spliced_arr = new byte[20];
		System.arraycopy(handshake, 28, spliced_arr, 0, 20);
		return !Arrays.equals(spliced_arr,this.peer_id);
		
	}

	
	/**
	 * This method communicates with the peer
	 * @return byte[]
	 */
	private byte[] startMessaging() {
		
		byte[] thefile = new byte[ti.file_length];
		
		try {
			
			System.out.println(PeerMsg.decodeMessageType(in,this.numPieces));
			
			writeMessage(new PeerMsg(MessageType.Interested));
			
			while(PeerMsg.decodeMessageType(in,this.numPieces).mtype != MessageType.Un_Choke)
				writeMessage(new PeerMsg(MessageType.Interested));
			
			int rLen = 16384;
			int limit = ti.piece_hashes.length * (ti.piece_length/rLen);
			int bytesWritten = 0;
			
			for(int counter = 0; counter < limit; counter++){
				
				if(this.mem.have[counter/2])
					continue;
				
				if(counter == limit-1)
					rLen = ti.file_length 
					- (ti.piece_length * (ti.piece_hashes.length - 1)) 
					- ( (ti.piece_length / rLen) - 1 ) * 16384;
				
				int start = (counter%2) * rLen;
				
				writeMessage(new RequestMessage(rLen, start, counter/2));
				PeerMsg ret = PeerMsg.decodeMessageType(in,this.numPieces);
				this.mem.put(Arrays.copyOfRange(ret.msg, 13, ret.msg.length), start, counter/2, ti.piece_length);
				this.ti.setDownloaded(bytesWritten);
				bytesWritten += rLen;
				
			}
			
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
		
		return thefile;
		
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
			if(isValid(handshake()))
				RUBTClient.writeToFile(startMessaging());
			else
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
