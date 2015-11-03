package bittorrent;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by krupal on 11/2/2015.
 */
public class Shared {
    public boolean[] have;
    public BlockingQueue<Piece> pieces;

    public Shared(int size){
        this.have = new boolean[size];
        this.pieces = new LinkedBlockingQueue<Piece>();
    }

    public void put(byte[] b, int o, int loc){
        this.pieces.add(new Piece(b,o,loc));
    }
    
    private class Piece{
    	byte[] data;
    	int offset;
    	int loc;
    	
    	public Piece(byte[] b, int o, int loc){
    		this.data = b;
    		this.offset = o;
    		this.loc =loc;
    	}
    	
		/**
		 * @return the data
		 */
		byte[] getData() {
			return data;
		}
		/**
		 * @return the offset
		 */
		int getOffset() {
			return offset;
		}
		/**
		 * @return the loc
		 */
		int getLoc() {
			return loc;
		}
    	
    }
}