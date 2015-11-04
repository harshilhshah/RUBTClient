package bittorrent;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by krupal on 11/2/2015.
 */
public class Shared {
	
    public boolean[] have;
    public ConcurrentHashMap<Integer,Piece> pieces;

    public Shared(int size){
        this.have = new boolean[size];
        Arrays.fill(this.have, false);
        this.pieces = new ConcurrentHashMap<Integer,Piece>();
    }

    public void put(byte[] b, int o, int loc, int len){
    	Integer integer = new Integer(loc);
    	if(this.pieces.containsKey(integer)){
    		if(this.pieces.get(integer).addData(b, o))
    			this.have[loc] = true;
    	}else{
    		this.pieces.put(integer,new Piece(b,o,loc,len));
    	}
    }
    
    public byte[] get(int index){
    	Piece p = this.pieces.get(index);
    	return (p != null && this.have[p.loc]) ? p.data : null;
    }
    
    public byte[] getAllData(int fileSize){
    	byte[] all = new byte[fileSize];
    	for(Piece p: this.pieces.values()){
    		System.arraycopy(p.data, 0, all, p.loc*have.length, p.data.length);
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
    	
    	public boolean addData(byte[] b, int offset){
    		if(Arrays.copyOfRange(data, offset, b.length) == b)
    			return false;
    		System.arraycopy(b, 0, data, offset, b.length);
    		return false;
    	}
    	
    }
}