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

    public byte[] getMessage(){
        return this.message;
    }
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

    public static byte[] convertToByte(int value){
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(value);

        return bb.array();
    }

    public void setPayload(){

    }







}
