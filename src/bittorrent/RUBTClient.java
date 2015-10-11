package bittorrent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

public class RUBTClient {
	
	static final String INVALID_ARGS = "Error: Invalid number of arguments\n"
			+ "\nUsage: java cp . RUBTClient <load_file> <storage_file>";
	static final String NULL_FILENAME = "Error: Please provide a valid file path";
	static final String FILE_NOT_FOUND  = "Error: The file %s doesn't exist. "
			+ "\nPlease provide a valid file path name";
	static final String CORRUPT_FILE = "Error: the file %s is corrupted.";
	static final String INVALID_URL = "Error: An invalid url was formed. "
			+ "\nCheck the contents of the file %s";
	static final String GET_FAILED = "Error: The program failed to properly "
			+ "execute HTTP GET request";
	static final String NO_PEERS_FOUND = "Warning: No peers found in the response. Terminating ..";
	
	static String output_file;
	static byte[] byteArray = null;

	public static void main(String[] args) {			
		
		/* Checking error cases */
		
		if(args.length != 2)
			printError(INVALID_ARGS);
		
		if(args[0] == null)
			printError(NULL_FILENAME);
		
		output_file = args[1];
	
		
		
		/* Opening and reading the data inside the file */
		
		try{
			File torrent_file = new File(args[0]);
			RandomAccessFile fileRead = new RandomAccessFile(torrent_file,"r");
			byteArray = new byte[(int) fileRead.length()];
			fileRead.read(byteArray);
			fileRead.close();
		} catch(FileNotFoundException fe){
			printError(String.format(FILE_NOT_FOUND,args[0]));
		} catch (IOException e) {
			printError(e.getMessage());
		} 
		
		
		
		/* Creating parser which will parse info from torrent_info */
		
		Parser parser = null;
		try {
			parser = new Parser(new TorrentInfo(byteArray));
		} catch (BencodingException e1) {
			printError(e1.getMessage() + String.format(CORRUPT_FILE,args[0]));
		}
		
		
		
		/* Making a get request and decoding the request */
		
		try {
			if(parser.parseResponse(parser.makeGetRequest()) == null)
				printError(NO_PEERS_FOUND);
		} catch (UnknownHostException uhe){
			printError(uhe.getMessage());
		} catch (MalformedURLException e) {
			printError(String.format(INVALID_URL,args[0]));
		} catch (IOException e) {
			printError(GET_FAILED);
		} catch (BencodingException e) {
			e.printStackTrace();
		}
		
	}
	
	private static void printError(String error_message){
		System.out.println(error_message);
		System.exit(1);
	}
		
	public static void writeToFile(byte[] bytes){
		
		File file = new File(RUBTClient.output_file);
		
		RandomAccessFile stream = null;
		try {
			stream = new RandomAccessFile(file,"rw");
			stream.write(bytes, 0, bytes.length);
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
            try {
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

	}

}
