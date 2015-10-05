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
		
		if(args[0] == null){
			System.out.println(Errors.NULL_FILENAME);
			return;
		}
		
		try{
			File torrent_file = new File(args[0]);
			if(!torrent_file.exists()){
				System.out.println(String.format(Errors.FILE_DOESNT_EXIST,args[0]));
				return;
			}
			DataInputStream metaIn = new DataInputStream(
					new FileInputStream(torrent_file));
		}catch(Exception ne){
			
		}
		
		
		
	}

}
