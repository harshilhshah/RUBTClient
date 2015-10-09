package bittorrent;

public class Peer {
	
	int port;
	String ip;
	byte[] peer_id;
	
	public Peer(int port, String ip_address, byte[] peer_id){
		this.port = port;
		this.ip = ip_address;
		this.peer_id = peer_id;
	}

}
