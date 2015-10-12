package bittorrent;

/**
 * @author  Harshil Shah
 */

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class Parser {
	
	static final ByteBuffer KEY_FAILURE_REASON = ByteBuffer.wrap( new byte[] { 
		'f', 'a', 'i', 'l', 'u', 'r', 'e', ' ', 'r', 'e', 'a', 's', 'o', 'n'
	});
	static final ByteBuffer KEY_WARNING_MESSAGE = ByteBuffer.wrap(new byte[] { 
		'w', 'a', 'r', 'n', 'i', 'n', 'g', ' ', 'm','e', 's', 's', 'a', 'g', 'e'
	});
	static final ByteBuffer KEY_COMPLETE = ByteBuffer.wrap(new byte[] {
			'c', 'o', 'm', 'p', 'l', 'e', 't', 'e' 
	});
	static final ByteBuffer KEY_INCOMPLETE = ByteBuffer.wrap(new byte[] {
			'i', 'n', 'c', 'o', 'm', 'p', 'l', 'e', 't', 'e'
	});
	static final ByteBuffer KEY_MIN_INTERVAL = ByteBuffer.wrap(new byte[] { 
			'm', 'i', 'n', ' ', 'i', 'n', 't', 'e', 'r', 'v', 'a', 'l' 
	});
	static final ByteBuffer KEY_DOWNLOADED = ByteBuffer.wrap(new byte[] {
			'd', 'o', 'w', 'n', 'l', 'o', 'a', 'd', 'e', 'd'
	});
	static final ByteBuffer KEY_INTERVAL = ByteBuffer.wrap(new byte[] {
			'i', 'n', 't', 'e', 'r', 'v', 'a', 'l' 
	});
	static final ByteBuffer KEY_PEERS = ByteBuffer.wrap(new byte[] {
			'p', 'e', 'e', 'r', 's' 
	});
	static final ByteBuffer KEY_PORT = ByteBuffer.wrap(new byte[] {
			'p', 'o', 'r', 't' 
	});
	static final ByteBuffer KEY_IP = ByteBuffer.wrap(new byte[] { 'i', 'p' });
	static final ByteBuffer KEY_PEER_ID = ByteBuffer.wrap(new byte[] {
			'p', 'e', 'e', 'r', ' ', 'i', 'd' 
	});
	
	
	public final String my_peer_id = generateMyId(20);
	private int downloaded = 0;
	private int uploaded = 0;
	private int left;
	private String info_hash;
	private String ip_addr;
	private final int port;
	private final TorrentInfo ti;
	
	public static int interval = 0;
	
	public Parser(TorrentInfo tInfo){
		this.ti = tInfo;
		this.ip_addr = ti.announce_url.toString();
		this.info_hash = Converter.bytesToURL(ti.info_hash.array());
		this.left = ti.file_length;
		this.port = getPort(ti.announce_url.toString());
	}
	
	
	/*
	 * This method parses the port from the announce url and returns the port number
	 * @param String: url
	 * @return int
	 */
	private int getPort(String url) {
		return Integer.parseInt(url.substring(url.indexOf((int)':', 6)+1, url.lastIndexOf((int)'/')));
	}

	
	/*
	 * This method creates a formatted url
	 * @return String
	 */
	public URL getUrl() throws MalformedURLException{
		return new URL(ip_addr + "?info_hash=" + info_hash + "&peer_id=" + my_peer_id 
				+ "&port=" + port + "&uploaded=" + uploaded + "&downloaded="
				+ downloaded + "&left=" + left);
	}
	
	
	/*
	 * This method makes a connection to the host.
	 * @return byte[] 
	 */
	public byte[] makeGetRequest() throws MalformedURLException, IOException {
		
		HttpURLConnection http_conn = (HttpURLConnection) getUrl().openConnection();
		http_conn.setRequestMethod("GET");
		
		InputStream is = null;
		DataInputStream baos = null;
		byte[] byteArray = null;
		
		try{
			is = http_conn.getInputStream();
			baos = new DataInputStream(is);
			
			byteArray = new byte[http_conn.getContentLength()];
			baos.readFully(byteArray);
			is.close();
			baos.close();
			http_conn.disconnect();
		} catch(IOException ioe){
			if(is != null)
				is.close();
			if(baos != null)
				baos.close();
			System.out.println(ioe.getMessage());
			throw ioe;
		} 
		return byteArray;
	}
	
	
	
	/*
	 * This method parses the list of peers from tracker response
	 * @param byte[]: tracker response
	 * @return List<Peer>
	 */
	public List<Peer> parseResponse(byte[] resp) throws BencodingException, UnknownHostException, IOException{
		
		List<Peer> peers_list = new ArrayList<Peer>();
		
		@SuppressWarnings("unchecked")
		Map<ByteBuffer, Object> tracker = (Map<ByteBuffer, Object>) Bencoder2.decode(resp);
		
		if(tracker.containsKey(KEY_FAILURE_REASON))
			throw new BencodingException(tracker.get(KEY_FAILURE_REASON).toString());
		
		if(!tracker.containsKey(KEY_PEERS))
			return null;
		
		if(tracker.containsKey(KEY_INTERVAL)){
			Parser.interval = ((Integer)tracker.get(KEY_INTERVAL)).intValue();
		}else{
			System.out.println("Warning: No interval found in the response.");
		}
		
		for( Object elem: (ArrayList<?>)tracker.get(KEY_PEERS) ) {
			
			@SuppressWarnings("unchecked")
			Map<ByteBuffer, Object> pair = (Map<ByteBuffer,Object>)elem;
			
			if(!pair.containsKey(KEY_PORT) || !pair.containsKey(KEY_IP) || !pair.containsKey(KEY_PEER_ID)){
				System.out.println("Warning: Peer found but missing information about the peer.");
				continue;
			}
			
			int port = ((Integer) pair.get(KEY_PORT)).intValue();
			String ip = Converter.objectToStr(pair.get(KEY_IP));
			byte[] peer_id = ((ByteBuffer) pair.get(KEY_PEER_ID)).array();
			
			if(Converter.objectToStr(pair.get(KEY_PEER_ID)).contains("RU"))
				peers_list.add(new Peer(this.my_peer_id.getBytes(),port,ip,peer_id,this.ti));
		}
		
		return peers_list;
		
	}
	
	
	/*
	 * This method generates a random string
	 * @param int: length of the string to be generated
	 * @return String
	 */
	private String generateMyId(int len){
		final String alphaStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		Random rnd = new Random();
		StringBuilder sb = new StringBuilder( len );
		for( int i = 0; i < len; i++ ) 
			sb.append(alphaStr.charAt(rnd.nextInt(alphaStr.length())));
		return sb.toString();
	}
	
	
	

}
