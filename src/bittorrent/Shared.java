package bittorrent;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by krupal on 11/2/2015.
 */
public class Shared {
	
    public boolean[] have;
    public ConcurrentHashMap<Integer,Piece> pieces;

    public Shared(int size){
        this.have = new boolean[size]; // # of pieces = 436
        Arrays.fill(this.have, false);
        this.pieces = new ConcurrentHashMap<Integer,Piece>();
        if(RUBTClient.output_file != null && RUBTClient.output_file.exists())
        	readFile(RUBTClient.output_file);
    }

    public void readFile(File f){
    	RandomAccessFile fileRead = null;
		try {
			System.out.println("Reading data from the video file.");
			fileRead = new RandomAccessFile(f,"r");
			byte[] byteArray = new byte[(int) fileRead.length()];
			fileRead.read(byteArray);
			int slider = 0;
			int iter = 0;
			int downloaded = 0;
			for(int i = 0; i < f.length() && iter < have.length; i+=slider,iter++){
				int numPieces = RUBTClient.tInfo.piece_hashes.length;
				int lastPieceSize = (RUBTClient.tInfo.file_length - (RUBTClient.tInfo.piece_length * (numPieces - 1)));
				slider = (iter != numPieces-1) ? 16384*2 : lastPieceSize;
				for(byte b: Arrays.copyOfRange(byteArray, i,i+slider))
		    		if(b != 0){
		    			if(put(Arrays.copyOfRange(byteArray, i, i + slider),iter))
		    				downloaded += slider;
		    			break;
		    		}
			}
			RUBTClient.tInfo.setDownloaded(downloaded);
			System.out.println(RUBTClient.tInfo.getPercentDownloaded()+ "% completed.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			fileRead.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public boolean put(byte[] b, int o, int loc, int len){
    	Integer integer = new Integer(loc);
    	if(this.have[loc])
    		return false;
    	if(this.pieces.containsKey(integer)){
    		if(this.pieces.get(integer).addData(b, o)){
    			this.have[loc] = true;
    			return true;
    		}
    	}else{
    		this.pieces.put(integer,new Piece(b,o,loc,len));
    		return true;
    	}
    	return false;
    }
    
    public boolean put(byte[] bArr, int loc){
    	Integer integer = new Integer(loc);
    	if(this.have[loc])
    		return false;
    	if(this.pieces.containsKey(integer))
    		return false;
    	this.pieces.put(integer,new Piece(bArr,loc));
    	this.have[loc] = true;
    	return true;
    }
    
    public boolean remove( int loc){
    	Integer integer = new Integer(loc);
    	if(!this.have[loc])
    		return false;
    	if(pieces.containsKey(integer)){
    		this.pieces.get(integer).nullifyData();
    		this.have[loc] = false;
    		return true;
    	}
    	return false;
    }
    
    public byte[] get(int index){
    	Piece p = this.pieces.get(index);
    	return (p != null && this.have[p.loc]) ? p.data : null;
    }
    
    public byte[] getAllData(int fileSize, int pieceLen){
    	byte[] all = new byte[fileSize];
    	for(Piece p: this.pieces.values()){
    		if(!have[p.loc])
    			continue;
    		System.arraycopy(p.data, 0, all, p.loc*pieceLen, p.data.length);
    	}
    	return all;
    }
    
    private class Piece{
    	
    	private byte[] data;
    	private int loc;
    	
    	public Piece(byte[] b, int o, int loc, int len){
    		this.data = new byte[len];
    		System.arraycopy(b, 0, data, o, b.length);
    		this.loc = loc;
    	}
    	
    	public Piece(byte[] b, int loc){
    		this.data = b;
    		this.loc = loc;
    	}
    	
    	public boolean addData(byte[] b, int offset){
    		if(offset < b.length && Arrays.copyOfRange(data, offset, b.length) == b)
    			return false;
    		System.arraycopy(b, 0, data, offset, b.length);
    		return true;
    	}
    	
    	public void nullifyData(){
    		Arrays.fill(data, (byte)0);;
    	}
    	
    }
}