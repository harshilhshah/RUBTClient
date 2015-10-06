package bittorrent;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class Tracker {
	
	private final int port = 6886;
	
	// change this later because it's hard-coded
	private final String peer_id = Converter.bytesToHexStr("harshil".getBytes());
	
	private TorrentInfo torrent_info;
	
	private int downloaded = 0;
	private int left;
	
	protected byte[] tracker_response;
	
	public Tracker(TorrentInfo ti){
		torrent_info = ti;
	}
	
	
	// Modify this method later
	public void makeGETRequest() throws MalformedURLException, IOException{
		
		String ip_addr = torrent_info.announce_url.toString();
		String info_hash = Converter.bytesToURL(torrent_info.info_hash.array());

		String url = ip_addr + "?info_hash=" + info_hash + "&peer_id=" + peer_id 
				+ "&port=" + port + "&uploaded=0&downloaded="
				+ downloaded + "&left=" + left;

		HttpURLConnection huc = (HttpURLConnection) new URL(url)
				.openConnection();
		huc.setRequestMethod("GET");
		InputStream is = huc.getInputStream();
		//DataInputStream dis = new DataInputStream(is);
		
		
		StringBuilder result = new StringBuilder();
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));
	      String line;
	      while ((line = rd.readLine()) != null) {
	         result.append(line);
	      }
	      rd.close();
	      System.out.println(result.toString());
		
	

		/*int dataSize = huc.getContentLength();
		byte[] retArray = new byte[dataSize];

		dis.readFully(retArray);
		//dis.close();
		this.tracker_response = retArray;*/
		
	}

}
