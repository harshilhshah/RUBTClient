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
import java.util.List;

public class RUBTClient {
	
	static final String INVALID_ARGS = "Error: Invalid number of arguments\n"
			+ "\nUsage: java cp . RUBTClient <load_file> <storage_file>";
	static final String NULL_FILENAME = "Error: Please provide a valid file path";
	static final String FILE_DOESNT_EXIST  = "Error: The file %s doesn't exist. "
			+ "\nPlease provide a valid file path name";
	static final String CORRUPT_FILE = "Error: the file %s is corrupted.";
	static final String INVALID_URL = "Error: An invalid url was formed. "
			+ "\nCheck the contents of the file %s";
	static final String GET_FAILED = "Error: The program failed to properly "
			+ "execute HTTP GET request";
	static final String NO_PEERS_FOUND = "Warning: No peers found in the response. Terminating ..";

	public static void main(String[] args) {			
		
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
		
		
		
		/* Creating parser which will parse info from torrent_info */
		
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
		
		
		
		/* Decoding the tracker response in order to get list of peers */
		List<Peer> peers = null;
		try {
			peers = parser.parseResponse(byteArray);
		} catch (BencodingException | UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		if(peers == null)
			printError(NO_PEERS_FOUND);
		
		
	}
	
	public static void printError(String error_message){
		System.out.println(error_message);
		System.exit(1);
	}
		

}
