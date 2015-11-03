package bittorrent;

import java.io.DataInputStream;
import java.io.IOException;

import utility.Constants;
import utility.Converter;

/**
 *  @author Harshil Shah, Krupal Suthar, Aishwariya Gondhi
 */
public class PeerMsg implements Constants{


    private byte[] msg;
    private final MessageType mtype;
    
    
    /**
     * Constructor for peer to send messages
     * @param MessageType
     */
    
    public PeerMsg(MessageType type){
    	this.mtype = type;
    	this.msg = new byte[mtype.lenPref + 4];
    	System.arraycopy(Converter.intToByteArr(mtype.lenPref),0,this.msg,0,4);
        this.msg[4] = mtype.id;
    }
    
    /**
     * Constructor mainly for bitfield messages 
     */
    public PeerMsg(MessageType type, int numPieces){
    	this.mtype = type;
    	this.mtype.lenPref += numPieces / 8;
    	this.msg = new byte[mtype.lenPref + 4];
    	System.arraycopy(Converter.intToByteArr(mtype.lenPref),0,this.msg,0,4);
        this.msg[4] = mtype.id;
    }

    /**
     * returns the message array
     * @return byte[]
     */
    public byte[] getMessage(){
        return this.msg;
    }
    
    public static MessageType decodeMessageType(DataInputStream in, int numPieces) throws IOException{
    	
    	int lenPrefix = in.readInt();
    	
    	if (lenPrefix == 0)
    		return MessageType.Keep_Alive;
    	
    	int messageID = in.readByte();
    	
    	if(messageID == MessageType.Choke.id)
    		return MessageType.Choke;
    	else if (messageID == MessageType.Un_Choke.id)
    		return MessageType.Un_Choke;
    	else if (messageID == MessageType.Interested.id)
    		return MessageType.Interested;
    	else if (messageID == MessageType.Not_Interested.id)
    		return MessageType.Not_Interested;
    	else if (messageID == MessageType.Have.id){
    		MessageType temp = MessageType.Have;
    		temp.setPieceIndex(in.readInt());
    		return temp;
    	}
    	else if (messageID == MessageType.BitField.id){
    		MessageType temp = MessageType.BitField;
    		byte[] data = (numPieces % 8 == 0) ? new byte[numPieces/8] : new byte[numPieces/8 + 1];	
    		in.readFully(data);
    		temp.setData(data);
    		return temp;
    	}
    	else if (messageID == MessageType.Request.id){
    		MessageType temp = MessageType.Request;
    		temp.setPieceIndex(in.readInt());
    		temp.setBegin(in.readInt());
    		temp.setLength(in.readInt());
    		return temp;
    	}
    	else if (messageID == MessageType.Piece.id){
    		MessageType temp = MessageType.Piece;
    		temp.setPieceIndex(in.readInt());
    		temp.setBegin(in.readInt());
    		temp.lenPref = (byte) (9 + (numPieces / 8));
    		byte[] data = new byte[lenPrefix - 9];
			in.readFully(data);
			temp.setBlock(data);
    		return temp;
    	}
		return null;
    }

    /**
     * Set payload of this message.
     * @param reqLen
     * @param reqStart
     * @param reqIndex
     */
    public void setPayload(int reqLen, int reqStart, int reqIndex){
        if(this.mtype == MessageType.Request){
        	System.arraycopy(Converter.intToByteArr(reqIndex),0,this.msg,5,4);
            System.arraycopy(Converter.intToByteArr(reqStart),0,this.msg,9,4);
            System.arraycopy(Converter.intToByteArr(reqLen),0,this.msg,13,4);
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
                ans = new byte[mtype.lenPref -1];
                System.arraycopy(this.msg,5,ans,0,mtype.lenPref - 1);
                break;
            default:
                System.out.println("No payload required for this messsage");
        }

        return ans;
    }
    
    public byte[] createBitfield(int numPieces, boolean[] haveArr){
    	byte[] bitfield = new byte[numPieces/8.0 == numPieces/8.0 ? numPieces/8 : numPieces/8 + 1];
    	return null;
    }
    
    @Override
    public String toString(){
		return Converter.byteArrToStr(this.msg);
    }

}
