package bittorrent;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by krupal on 11/2/2015.
 */
public class Shared {
	
    public boolean[] have;
    public ConcurrentHashMap<Integer,Piece> pieces;

    public Shared(int size){
        this.have = new boolean[size];
        this.pieces = new ConcurrentHashMap<Integer,Piece>();
    }

    public void put(byte[] b, int o, int loc, int len){
        this.pieces.put(new Integer(o),new Piece(b,o,loc,len));
    }
    public byte[] get(int index){
    	Piece p = this.pieces.get(index);
    	return (p != null && p.complete) ? p.data : null;
    }
    
    private class Piece{
    	
    	private byte[] data;
    	private int loc;
    	public boolean complete;
    	
    	public Piece(byte[] b, int o, int loc, int len){
    		complete = (data != null);
    		this.data = new byte[len];
    		System.arraycopy(b, 0, data, o, b.length);
    		this.loc = loc;
    	}
    	
    }
}