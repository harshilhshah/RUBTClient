package bittorrent;

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
     * returns the message array
     * @return byte[]
     */
    public byte[] getMessage(){
        return this.msg;
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
    
    @Override
    public String toString(){
		return Converter.byteArrToStr(this.msg);
    }

}
