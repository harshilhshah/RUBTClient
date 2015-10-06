package bittorrent;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class RUBTClient {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		if(args.length != 2){	
			System.out.println(Errors.INVALID_ARGS);
			return;
		}
		
		if(args[0] == null){
			System.out.println(Errors.NULL_FILENAME);
			return;
		}
		
		byte[] byteArray;
		TorrentInfo torrent_info;
		
		try{
			
			File torrent_file = new File(args[0]);
			RandomAccessFile fileRead = new RandomAccessFile(torrent_file,"r");
			byteArray = new byte[(int) fileRead.length()];
			fileRead.read(byteArray);
			fileRead.close();
			torrent_info = new TorrentInfo(byteArray);
			
		}catch(IOException ne){
			System.out.println(String.format(Errors.FILE_DOESNT_EXIST,args[0]));
			return;
		} catch (BencodingException e) {
			System.out.println(e.getMessage() + String.format(Errors.CORRUPT_FILE,args[0]));
			return;
		}
		
		Tracker tracker = new Tracker(torrent_info);
		try {
			tracker.makeGETRequest();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println(torrent_info.announce_url.toString());
		System.out.println(torrent_info.file_name);
		
	}

}
