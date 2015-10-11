package bittorrent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

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
	
	
	private Socket socket = null;
	
	public Peer(byte[] my_id, int port, String ip_address, byte[] client_id, byte[] info_hash){
		this.my_id = my_id;
		this.port = port;
		this.ip = ip_address;
		this.peer_id = client_id;
		this.info_hash = info_hash;
		
		try {
			this.socket = new Socket(this.ip, this.port);
			this.in = new DataInputStream(this.socket.getInputStream());
			this.out = new DataOutputStream(this.socket.getOutputStream());
			
			if(isValid(handshake())) startMessaging();
			
			this.in.close();
			this.out.close();
			this.socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
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
		// Optional: send bit message
		//this.out.write();
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

}
