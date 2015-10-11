package bittorrent;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

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
	
	// change this later because it's hard-coded
	public static final String my_peer_id = Converter.bytesToURL("ihatethisprojectbruh".getBytes());
	private int downloaded = 0;
	private int uploaded = 0;
	private int left;
	private String info_hash;
	private String ip_addr;
	private final int port = 6885;
	private final TorrentInfo ti;
	
	private int interval = 0;
	private int min_interval = 0;
	
	public Parser(TorrentInfo tInfo){
		this.ti = tInfo;
		this.ip_addr = ti.announce_url.toString();
		this.info_hash = Converter.bytesToURL(ti.info_hash.array());
		this.left = ti.file_length;
	}
	
	public String getUrl(){
		return ip_addr + "?info_hash=" + info_hash + "&peer_id=" + my_peer_id 
				+ "&port=" + port + "&uploaded=" + uploaded + "&downloaded="
				+ downloaded + "&left=" + left;
	}
	
	public byte[] makeGetRequest() throws MalformedURLException, IOException {
		URL url = new URL(getUrl());
		HttpURLConnection http_conn = (HttpURLConnection) url.openConnection();
		http_conn.setRequestMethod("GET");
		
		InputStream is = null;
		DataInputStream baos = null;
		byte[] byteArray;
		
		try{
			is = http_conn.getInputStream();
			baos = new DataInputStream(is);
		
			byteArray = new byte[http_conn.getContentLength()];
			baos.readFully(byteArray);
		} finally{
			if(is != null)
				is.close();
			if(baos != null)
				baos.close();
		}
		return byteArray;
	}
	
	public List<Peer> parseResponse(byte[] resp) throws BencodingException, UnknownHostException, IOException{
		
		List<Peer> peers_list = new ArrayList<Peer>();
		
		@SuppressWarnings("unchecked")
		Map<ByteBuffer, Object> tracker = (Map<ByteBuffer, Object>) Bencoder2.decode(resp);
		
		if(tracker.containsKey(KEY_FAILURE_REASON)){
			throw new BencodingException(tracker.get(KEY_FAILURE_REASON).toString());
		}
		
		if(tracker.containsKey(KEY_INTERVAL)){
			this.interval = ((Integer)tracker.get(KEY_INTERVAL)).intValue();
		}else{
			System.out.println("Warning: No interval found in the response.");
		}
		
		if(tracker.containsKey(KEY_MIN_INTERVAL)){
			this.min_interval = ((Integer)tracker.get(KEY_INTERVAL)).intValue();
		}else{
			System.out.println("Warning: No min interval found in the response.");
		}
		
		if(!tracker.containsKey(KEY_PEERS))
			return null;
		
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
				peers_list.add(new Peer(Parser.my_peer_id.getBytes(),port,ip,peer_id,this.ti));
		}
		
		//ToolKit.print(Bencoder2.decode(resp));
		
		return peers_list;
		
	}
	
	
	

}
