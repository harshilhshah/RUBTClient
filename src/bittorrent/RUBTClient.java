package bittorrent;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

public class RUBTClient {


	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		if(args.length != 2){	
			System.out.println(Errors.INVALID_ARGS);
			return;
		}
		
		try{
			File torrent_file = new File(args[0]);
			DataInputStream metaIn = new DataInputStream(
					new FileInputStream(torrent_file));
		}catch(Exception e){
			
		}
		
		
	}

}
