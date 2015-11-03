package bittorrent;

import java.io.DataInputStream;
import java.io.IOException;

import utility.Constants;
import utility.Converter;

/**
 *  @author Harshil Shah, Krupal Suthar, Aishwariya Gondhi
 */
public class PeerMsg implements Constants{
	
    protected byte[] msg;
    protected final MessageType mtype;
	
	public static class RequestMessage extends PeerMsg{

		public RequestMessage(int reqLen, int reqStart, int reqIndex) {
			super(MessageType.Request);
			System.arraycopy(Converter.intToByteArr(reqIndex),0,this.msg,5,4);
            System.arraycopy(Converter.intToByteArr(reqStart),0,this.msg,9,4);
            System.arraycopy(Converter.intToByteArr(reqLen),0,this.msg,13,4);
		}
		
	}
	
	public static class HaveMessage extends PeerMsg{

		public HaveMessage(int pieceIndex) {
			super(MessageType.Have);
			System.arraycopy(Converter.intToByteArr(pieceIndex),0,this.msg,5,4);
		}
		
	}
	
	public static class PieceMessage extends PeerMsg{

		public PieceMessage(int pieceIndex, int begin, byte[] block, int lenPref) {
			super(MessageType.Piece, lenPref);
			System.arraycopy(Converter.intToByteArr(pieceIndex),0,this.msg,5,4);
			System.arraycopy(Converter.intToByteArr(begin),0,this.msg,9,4);
			System.arraycopy(block,0,this.msg,13,block.length);
		}
		
	}
	
	public static class BitfieldMessage extends PeerMsg{

		public BitfieldMessage(byte[] data) {
			super(MessageType.BitField);
			System.arraycopy(data,0,this.msg,5,data.length);
		}
		
	}
	
	
    
    
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
    public PeerMsg(MessageType type, int lenPref){
    	this.mtype = type;
    	this.mtype.lenPref = (byte) lenPref;
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
    
    public static PeerMsg decodeMessageType(DataInputStream in, int numPieces) throws IOException{
    	
    	int lenPrefix = in.readInt();
    	
    	if (lenPrefix == 0)
    		return new PeerMsg(MessageType.Keep_Alive);
    	
    	int messageID = in.readByte();
    	
    	if(messageID == MessageType.Choke.id)
    		return new PeerMsg(MessageType.Choke);
    	else if (messageID == MessageType.Un_Choke.id)
    		return new PeerMsg(MessageType.Un_Choke);
    	else if (messageID == MessageType.Interested.id)
    		return new PeerMsg(MessageType.Interested);
    	else if (messageID == MessageType.Not_Interested.id)
    		return new PeerMsg(MessageType.Not_Interested);
    	else if (messageID == MessageType.Have.id)
    		return new HaveMessage(in.readInt());
    	else if (messageID == MessageType.Request.id)
    		return new RequestMessage(in.readInt(), in.readInt(), in.readInt());
    	else if (messageID == MessageType.BitField.id){
    		byte[] data = (numPieces % 8 == 0) ? new byte[numPieces/8] : new byte[numPieces/8 + 1];	
    		in.readFully(data);
    		return new BitfieldMessage(data);
    	}
    	else if (messageID == MessageType.Piece.id){
    		int lenPref = (byte) (9 + (numPieces / 8));
    		byte[] data = new byte[lenPrefix - 9];
			in.readFully(data);
    		return new PieceMessage(in.readInt(), in.readInt(), data, lenPref);
    	}
		return null;
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
