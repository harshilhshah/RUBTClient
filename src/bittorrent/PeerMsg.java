package bittorrent;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * This file is simply used to make messages the takes place between peers.
 * Created by krupal on 10/10/2015.
 */
public class PeerMsg {
	
	
	/*
	 * Made an enum to make life easier. 
	 */
	public enum MessageType{
		
		Choke(0),
		Un_Choke(1), 
		Interested(2), 
		Not_Interested(3), 
		Have(4), 
		BitField(5), 
		Request(6), 
		Piece(7), 
		Cancel(8);
		
		private final byte id;
		private byte lengthPrefix;
		
		MessageType(int id){
			this.id = (byte) id;
			if(id == 0 || id == 1 || id == 2 || id == 3)
				this.lengthPrefix = 1;
			else if(id == 4)
				this.lengthPrefix = 5;
			else if(id == 6)
				this.lengthPrefix = 13;
			else
				this.lengthPrefix = 0;
		}
	}


    private byte[] message;
    private final MessageType mtype;
    
    
    /**
     * Constructor for peer to send messages
     * @param MessageType
     */
    
    public PeerMsg(MessageType type){
    	this.mtype = type;
    	this.message = new byte[mtype.lengthPrefix + 4];
    	System.arraycopy(convertToByte(mtype.lengthPrefix),0,this.message,0,4);
        this.message[4] = mtype.id;
    }

    /**
     * returns the message array
     * @return byte[]
     */
    public byte[] getMessage(){
        return this.message;
    }

    /**
     * Converts int to 4 byte big-endian
     * @param value
     * @return byte[]
     */
    public static byte[] convertToByte(int value){
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(value);
        return bb.array();
    }

    /**
     * Set payload of this message.
     * @param reqLen
     * @param reqStart
     * @param reqIndex
     */
    public void setPayload(int reqLen, int reqStart, int reqIndex){
        if(this.mtype == MessageType.Request){
        	System.arraycopy(convertToByte(reqIndex),0,this.message,5,4);
            System.arraycopy(convertToByte(reqStart),0,this.message,9,4);
            System.arraycopy(convertToByte(reqLen),0,this.message,13,4);
        }
    }

    /**
     * gets payLoad of the message
     * @return byte[]
     */
    public byte[] getPayLoad(){
    	
        byte[] ans = null;

        switch (mtype){
            case Have:
            case Piece:
            case Request:
                ans = new byte[mtype.lengthPrefix -1];
                System.arraycopy(this.message,5,ans,0,mtype.lengthPrefix - 1);
                break;
            default:
                System.out.println("No payload required for this messsage");
        }

        return ans;
    }
    
    @Override
    public String toString(){
		return Arrays.toString(this.getMessage());
    }

}
