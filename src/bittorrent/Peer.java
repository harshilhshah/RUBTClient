package bittorrent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import bittorrent.PeerMsg.MessageType;

public class Peer {
	
	static final byte[] BT_PROTOCOL = { 'B', 'i', 't', 'T', 'o',
		'r', 'r', 'e', 'n', 't', ' ', 'p', 'r', 'o', 't', 'o', 'c', 'o', 'l' 
	};
	
	int port;
	String ip;
	private byte[] info_hash;
	private byte[] peer_id;
	private DataInputStream in;
	private DataOutputStream out;
	private byte[] my_id;
	private TorrentInfo ti;
	
	
	private Socket socket = null;
	
	public Peer(byte[] my_id, int port, String ip_address, byte[] client_id, TorrentInfo ti) throws UnknownHostException, IOException{
		this.my_id = my_id;
		this.port = port;
		this.ip = ip_address;
		this.peer_id = client_id;
		this.ti = ti;
		this.info_hash = ti.info_hash.array();
		
		this.socket = new Socket(this.ip, this.port);
		this.in = new DataInputStream(this.socket.getInputStream());
		this.out = new DataOutputStream(this.socket.getOutputStream());
		
		if(isValid(handshake())) startMessaging();
		
		this.in.close();
		this.out.close();
		this.socket.close();
	}
	
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

	private void startMessaging() {
		// Optional: send bit message, but I didn't
		try {
			int length = this.in.readInt();
			this.in.readByte();
			readMessage(length-1);
			
			writeMessage(new PeerMsg(MessageType.Interested));
			
			readMessage(5);
			
			System.out.println(ti.file_length);
			System.out.println(ti.piece_length);
			System.out.println(ti.piece_hashes.length);
			
			int req_len = 16384;
			byte[] thefile = new byte[ti.file_length];
			final int limit = ti.piece_hashes.length * (ti.piece_length/16384);
			
			System.out.println(ti.file_length - (ti.piece_length * (ti.piece_hashes.length - 1)) - 16384);
			
			for(int counter = 0; counter < limit+1; counter++){
				
				if(counter == limit)
					req_len = ti.file_length - (ti.piece_length * (ti.piece_hashes.length - 1)) - 16384;
				int start = (counter%2) * req_len;
				PeerMsg m = new PeerMsg(MessageType.Request);
				m.setPayload(0, null, 0, 0, req_len, start, counter/2);
				writeMessage(m);
				readMessage(5); // don't care about <length-prefix><7>
				readMessage(8); // don't care about <index><begin>
				byte[] bytes = readMessage(req_len);
				System.arraycopy(bytes, 0, thefile, req_len*counter, bytes.length);
				System.out.println(counter + ":" + Arrays.toString(bytes));
				
			}
			RUBTClient.writeToFile(thefile);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public byte[] handshake() throws IOException{
		
		byte[] send_message = new byte[68];
		byte[] recieve_message = new byte[68];
		
		send_message[0] = 19;
		System.arraycopy(BT_PROTOCOL, 0, send_message, 1, BT_PROTOCOL.length);
		System.arraycopy(this.info_hash, 0, send_message, 28, 20);
		System.arraycopy(my_id, 0, send_message, 48, 20);
		
		this.out.write(send_message);
		this.out.flush();
		this.socket.setSoTimeout(130000);
		this.in.read(recieve_message);
		
		System.out.println("Send:" + new String(send_message, "UTF-8"));
		System.out.println("Recv:" + new String(recieve_message, "UTF-8"));
		
		return recieve_message;
		
	}
	
	private void writeMessage(PeerMsg pm) throws IOException{
		this.out.write(pm.getMessage());
		this.out.flush();
	}
	
	private byte[] readMessage(int len) throws IOException{
		byte[] bArr = new byte[len];
		for(int i=0; i < len; i++)
			bArr[i] = this.in.readByte();
		return bArr;
	}

}
