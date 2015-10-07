package bittorrent;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class Parser {
	
	private final int port = 6881;
	
	// change this later because it's hard-coded
	private final String peer_id = Converter.bytesToHexStr("harshil".getBytes());
	
	private TorrentInfo torrent_info;
	
	private int downloaded = 0;
	private int left;
	private String info_hash;
	
	protected byte[] tracker_response;
	
	public Parser(TorrentInfo ti){
		torrent_info = ti;
	}
	
	public String getUrl(){
		String ip_addr = torrent_info.announce_url.toString();
		info_hash = Converter.bytesToURL(torrent_info.info_hash.array());

		return ip_addr + "?info_hash=" + info_hash + "&peer_id=" + peer_id 
				+ "&port=" + port + "&uploaded=0&downloaded="
				+ downloaded + "&left=" + left;
	}
	// Modify this method later
	
	
	/*public void decodeTrackerResponse(byte[] trackerResponse)
			throws BencodingException {

		Object o = Bencoder2.decode(trackerResponse);

		HashMap<ByteBuffer, Object> response = (HashMap<ByteBuffer, Object>) o;

		ArrayList<HashMap<ByteBuffer, Object>> encodedPeerList = null;
		if (response.containsKey(Parser.KEY_PEERS)) {
			encodedPeerList = (ArrayList<HashMap<ByteBuffer, Object>>) response
					.get(Parser.KEY_PEERS);
		} else {
			System.out.println("No peer list given by tracker response.");
			return;
		}

		// Iterate through the peers and build peer list
		final LinkedList<Peer> peerList = new LinkedList<Peer>();
		for (final HashMap<ByteBuffer, Object> peerMap : encodedPeerList) {

			// Print the map
			// ToolKit.print(peerMap);

			// Get peer IP
			String peerIP = null;
			if (peerMap.containsKey(Parser.KEY_IP)) {
				final ByteBuffer peerIPBB = (ByteBuffer) peerMap
						.get(Parser.KEY_IP);
				peerIP = new String(peerIPBB.array(), "UTF-8");
			}

			// Get peer ID
			ByteBuffer peerIdBB;
			byte[] peerId = null;
			if (peerMap.containsKey(Parser.KEY_PEER_ID)) {
				peerIdBB = (ByteBuffer) peerMap.get(Parser.KEY_PEER_ID);
				peerId = peerIdBB.array();
			}

			// Get peer port
			Integer peerPort = -1;
			if (peerMap.containsKey(Parser.KEY_PORT)) {
				peerPort = (Integer) peerMap.get(Parser.KEY_PORT);
			}

			// Add new peer
			 Peer peer = new Peer(peerId, peerIP, peerPort, this.info_hash,
					this.clientId);
			peerList.add(peer);

			System.out.println("Peer in torrent: " + peer);
		}

	}*/

}
