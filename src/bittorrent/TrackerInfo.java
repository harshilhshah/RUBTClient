package bittorrent;

/**
 *  @author Harshil Shah, Krupal Suthar, Aishwariya Gondhi
 */

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import utility.Constants;
import utility.Converter;

public class TrackerInfo extends TorrentInfo implements Constants {	
	
	static byte[] my_peer_id;
	
	private int interval = 180;
	private int min_interval = interval / 2;
	private int downloaded = 0;
	private int uploaded = 0;
	private int left;
	private String info_hash_str;
	protected static int port = 6881;
	
	public TrackerInfo(byte[] byteArray) throws BencodingException{
		super(byteArray);
		this.info_hash_str = Converter.bytesToURL(info_hash.array());
		this.left = file_length;
		my_peer_id = generateMyId(20);
	}

	/**
	 * This method generates a random string
	 * @param int: length of the string to be generated
	 * @return String
	 */
	public static byte[] generateMyId(int len){
		final String alphaStr = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		Random rnd = new Random();
		StringBuilder sb = new StringBuilder( len );
		for( int i = 0; i < len; i++ ) 
			sb.append(alphaStr.charAt(rnd.nextInt(alphaStr.length())));
		return sb.toString().getBytes();
	}
	
	/**
	 * This method creates a formatted url
	 * @return String
	 */
	public URL getUrl(Event event_type) throws MalformedURLException{
		return new URL(announce_url.toString() + "?info_hash=" + info_hash_str + "&peer_id=" 
				+ my_peer_id + "&port=" + port + "&uploaded=" + uploaded + "&downloaded="
				+ downloaded + "&left=" + left + event_type.toString());
	}

	/**
	 * This method makes a connection to the host.
	 * @return byte[] 
	 */
	public byte[] announce(Event e) throws MalformedURLException, IOException {		

		System.out.println("Connecting to the tracker at " 
				+ new URL(this.announce_url.toString()).getHost() + " " + e);
		
		HttpURLConnection http_conn = (HttpURLConnection) getUrl(e).openConnection();
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
	
	/**
	 * This method parses the list of peers from tracker response
	 * @param byte[]: tracker response
	 * @return List<Peer>
	 */
	List<Download> getPeers(byte[] resp) throws BencodingException, UnknownHostException, IOException{
		
		List<Download> peers_list = new ArrayList<Download>();
		
		@SuppressWarnings("unchecked")
		Map<ByteBuffer, Object> tracker = (Map<ByteBuffer, Object>) Bencoder2.decode(resp);
		
		if(tracker.containsKey(KEY_FAILURE_REASON))
			throw new BencodingException(tracker.get(KEY_FAILURE_REASON).toString());
		
		if(!tracker.containsKey(KEY_PEERS))
			return null;
		
		if(tracker.containsKey(KEY_INTERVAL)){
			setInterval(((Integer)tracker.get(KEY_INTERVAL)).intValue());
		}else{
			System.out.println("Warning: No interval found in the response.");
		}
		
		if(tracker.containsKey(KEY_MIN_INTERVAL)){
			setMin_interval((((Integer)tracker.get(KEY_MIN_INTERVAL)).intValue()));
		}else{
			setMin_interval(getInterval()/2);
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
			
			if(ip.contains("128.6.171.130") || ip.contains("128.6.171.131"))
				peers_list.add(new Download(port,ip,peer_id,this));
		}
		
		return peers_list;
		
	}
	
	public void updateIntervals(byte[] resp) throws BencodingException{
		@SuppressWarnings("unchecked")
		Map<ByteBuffer, Object> tracker = (Map<ByteBuffer, Object>) Bencoder2.decode(resp);
		
		if(tracker.containsKey(KEY_FAILURE_REASON))
			throw new BencodingException(tracker.get(KEY_FAILURE_REASON).toString());
		
		if(tracker.containsKey(KEY_INTERVAL)){
			setInterval(((Integer)tracker.get(KEY_INTERVAL)).intValue());
		}else{
			System.out.println("Warning: No interval found in the response.");
		}
		
		if(tracker.containsKey(KEY_MIN_INTERVAL)){
			setMin_interval((((Integer)tracker.get(KEY_MIN_INTERVAL)).intValue()));
		}else{
			setMin_interval(getInterval()/2);
		}
	}

	public int getPercentDownloaded(){
		return (int)(getDownloaded()*100f/file_length);
	}
	
	/**
	 * @return the interval
	 */
	public int getInterval() {
		return interval;
	}


	/**
	 * @param interval the interval to set
	 */
	public void setInterval(int interval) {
		this.interval = (interval > 180) ? 180 : interval;
	}


	/**
	 * @return the min_interval
	 */
	public int getMin_interval() {
		return min_interval;
	}


	/**
	 * @param min_interval the min_interval to set
	 */
	public void setMin_interval(int min_interval) {
		this.min_interval = min_interval;
	}
	
	public int getDownloaded(){
		return this.downloaded;
	}


	public void setDownloaded(int num){
		this.downloaded = num;
		this.left = file_length - num;
	}
	
	/**
	 * @return the uploaded
	 */
	int getUploaded() {
		return uploaded;
	}


	/**
	 * @param uploaded the uploaded to set
	 */
	void setUploaded(int uploaded) {
		this.uploaded += uploaded;
	}
	
	/**
	 * @return the left
	 */
	int getLeft() {
		return left;
	}


	/**
	 * @param left the left to set
	 */
	void setLeft(int left) {
		this.left = left;
	}


	public TorrentInfo getTorrentInfo(){
		return (TorrentInfo)this;
	}

}
