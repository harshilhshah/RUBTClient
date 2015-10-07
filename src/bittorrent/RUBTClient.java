package bittorrent;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RUBTClient implements Errors{

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		
		
		/* Checking error cases */
		
		if(args.length != 2)
			printError(INVALID_ARGS);
		
		if(args[0] == null)
			printError(NULL_FILENAME);
		
		
		
		byte[] byteArray = null;
		TorrentInfo torrent_info = null;
		
		
		
		/* Opening and reading the data inside the file */
		
		try{
			
			File torrent_file = new File(args[0]);
			RandomAccessFile fileRead = new RandomAccessFile(torrent_file,"r");
			byteArray = new byte[(int) fileRead.length()];
			fileRead.read(byteArray);
			fileRead.close();
			torrent_info = new TorrentInfo(byteArray);
			
		}catch(IOException ne){
			printError(String.format(FILE_DOESNT_EXIST,args[0]));
		}catch (BencodingException e) {
			printError(e.getMessage() + String.format(CORRUPT_FILE,args[0]));
		}
		
		
		
		/* Parsing the data in the file */
		
		Parser parser = new Parser(torrent_info);
		
		
		
		
		/* Making a get request */
		
		try {
			
			URL url = new URL(parser.getUrl());
			HttpURLConnection http_conn = (HttpURLConnection) url.openConnection();
			http_conn.setRequestMethod("GET");
			InputStream is = http_conn.getInputStream();
			DataInputStream baos = new DataInputStream(is);
			int dataSize = http_conn.getContentLength();
			byteArray = new byte[dataSize];
			baos.readFully(byteArray);
			is.close();
			baos.close();
			
		} catch (MalformedURLException e) {
			printError(String.format(INVALID_URL,args[0]));
		} catch (IOException e) {
			printError(GET_FAILED);
		}

		
		System.out.println(torrent_info.announce_url.toString());
		System.out.println(torrent_info.file_name);
		
		Map<ByteBuffer, Object> decode;
		try {
			decode = (Map<ByteBuffer, Object>) Bencoder2.decode(byteArray);
			decode = ((ArrayList<HashMap<ByteBuffer, Object>>) decode.values().toArray()[5]).get(0);
			//printError(decode.keySet().toArray()[0].toString());
			System.out.println(new String(((ByteBuffer)decode.keySet().toArray()[2]).array(),"ASCII"));
		} catch (BencodingException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void printError(String error_message){
		System.out.println(error_message);
		System.exit(1);
	}
	
	/*public void makeGETRequest() throws MalformedURLException, IOException{
		
		String ip_addr = torrent_info.announce_url.toString();
		info_hash = Converter.bytesToURL(torrent_info.info_hash.array());

		String url = ip_addr + "?info_hash=" + info_hash + "&peer_id=" + peer_id 
				+ "&port=" + port + "&uploaded=0&downloaded="
				+ downloaded + "&left=" + left;

		HttpURLConnection huc = (HttpURLConnection) new URL(url)
				.openConnection();
		huc.setRequestMethod("GET");
		InputStream is = huc.getInputStream();
		DataInputStream dis = new DataInputStream(is);
		
		
		/*StringBuilder result = new StringBuilder();
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));
	      String line;
	      while ((line = rd.readLine()) != null) {
	         result.append(line);
	      }
	      rd.close();
	      System.out.println(result.toString());
	
	

		int dataSize = huc.getContentLength();
		byte[] retArray = new byte[dataSize];

		dis.readFully(retArray);
		//dis.close();
		this.tracker_response = retArray;
		
	}*/

}
