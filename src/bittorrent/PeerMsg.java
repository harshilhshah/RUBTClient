package bittorrent;

import java.nio.ByteBuffer;

/**
 * Created by krupal on 10/10/2015.
 */
public class PeerMsg {
    /*public static void main (String args[]){
        byte[] msg = new byte[13+4];
        for(int i = 0; i< msg.length;i++) {
            System.out.print(msg[i]);
        }
    }*/

    public static final byte Choke = 0;
    public static final byte Un_Choke = 1;
    public static final byte Interested = 2;
    public static final byte Not_Interested = 3;
    public static final byte Have = 4;
    public static final byte BitField = 5;
    public static final byte Request = 6;
    public static final byte Piece = 7;
    public static final byte Cancel = 8;


    private byte[] message;
    public final byte msgId;
    public final int lengthPrefix;
    byte[] block;
    /**
     * Constructor for peer to send messages
     * @param lenPre
     * @param msgID
     */
    public PeerMsg(int lenPre, byte msgID){
        this.lengthPrefix = lenPre;
        this.msgId = msgID;
        this.message = new byte[this.lengthPrefix + 4];
        setMessage();

    }

    /**
     * returns the message array
     * @return byte[]
     */
    public byte[] getMessage(){
        return this.message;
    }

    /**
     * sets up the byte array for messages
     */
    public void setMessage(){
        switch (msgId){
            case Choke:
                System.arraycopy(convertToByte(1),0,this.message,0,4);
                this.message[4] = (byte)0;
                break;
            case Un_Choke:
                System.arraycopy(convertToByte(1),0,this.message,0,4);
                this.message[4] = (byte)1;
                break;
            case Interested:
                System.arraycopy(convertToByte(1),0,this.message,0,4);
                this.message[4] = (byte)2;
                break;
            case Not_Interested:
                System.arraycopy(convertToByte(1),0,this.message,0,4);
                this.message[4] = (byte)3;
                break;
            case Have:
                System.arraycopy(convertToByte(5),0,this.message,0,4);
                this.message[4] = (byte)4;
                break;
            case Request:
                System.arraycopy(convertToByte(13),0,this.message,0,4);
                this.message[4] = (byte)6;
                break;
            case Piece:
                System.arraycopy(convertToByte(this.lengthPrefix),0,this.message,0,4);
                this.message[4] = (byte)7;
                break;

        }
    }

    /**
     * Converts int to 4 byte big-endian
     * @param value
     * @return
     */
    public static byte[] convertToByte(int value){
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(value);

        return bb.array();
    }

    /**
     * Set payload of this message.
     * @param payLoad
     * @param block
     * @param pieceStart
     * @param pieceIndex
     * @param reqLen
     * @param reqStart
     * @param reqIndex
     */
    public void setPayload(int payLoad, byte[]block, int pieceStart, int pieceIndex,
                           int reqLen, int reqStart, int reqIndex){
        switch (msgId){
            case Have:
                System.arraycopy(convertToByte(payLoad),0,this.message,5,4);
                break;
            case Piece:
                this.block = block;
                System.arraycopy(convertToByte(pieceIndex),0,this.message,5,4);
                System.arraycopy(convertToByte(pieceStart),0,this.message,9,4);
                System.arraycopy(block,0,this.message,13,this.lengthPrefix - 9);
                break;
            case Request:
                System.arraycopy(convertToByte(reqIndex),0,this.message,5,4);
                System.arraycopy(convertToByte(reqStart),0,this.message,9,4);
                System.arraycopy(convertToByte(reqLen),0,this.message,13,4);
                break;
            default:
                // do nothing
        }

        }

    /**
     * gets payLoad of the message
     * @return byte[]
     */
    public byte[] getPayLoad(){
        byte[] ans = null;

        switch (msgId){
            case Have:
                ans = new byte[4];
                System.arraycopy(this.message,5,ans,0,4);
                break;
            case Piece:
                ans = new byte[this.lengthPrefix -1];
                System.arraycopy(this.message,5,ans,0,this.lengthPrefix - 1);
                break;
            case Request:
                ans =  new byte[12];
                System.arraycopy(this.message,5,ans,0,12);
                break;
            default:
                System.out.println("No payload required for this messsage");
        }

        return ans;
    }







}
