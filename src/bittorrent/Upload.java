package bittorrent;

import utility.Constants;

import java.io.*;
import java.net.Socket;

/**
 * Created by krupal on 10/27/2015.
 */
public class Upload implements Runnable, Constants {

    private DataInputStream din = null;
    private DataOutputStream dout = null;
    private Socket sckt;

    public Upload(Socket soc) throws IOException {
    	this.sckt = soc;
    	this.din = new DataInputStream(soc.getInputStream());
    	this.dout = new DataOutputStream(soc.getOutputStream());
    	System.out.println("Recieved a connection from " +  this.sckt.getRemoteSocketAddress().toString());
    }

    public boolean handShake() throws Exception {
        byte[] recieve_msg = new byte[68];
        byte handshake = 19;
        // Recieving msg
        din.readFully(recieve_msg);
        byte msgType = recieve_msg[0];

        if (msgType != handshake) {
            return false;
        } else {
            System.arraycopy(BT_PROTOCOL, 0, recieve_msg, 1, BT_PROTOCOL.length);
            System.arraycopy(RUBTClient.tInfo.info_hash.array(), 0, recieve_msg, 28, 20);
            System.arraycopy(TrackerInfo.my_peer_id, 0, recieve_msg, 48, 20);

            dout.write(recieve_msg);
            return true;
        }

    }

    public void startUpload() throws Exception {
        int numPieces = RUBTClient.tInfo.piece_hashes.length;
    	byte[] data = (numPieces % 8 == 0) ? new byte[numPieces/8] : new byte[numPieces/8 + 1];	
    	for(int i = 0; i < RUBTClient.getMemory().have.length; i++)
    		data[i/8] = (byte) ((RUBTClient.getMemory().have[i]) ? 0x80 >> (i % 8) : 0);
        writeMessage(new PeerMsg.BitfieldMessage(data));


        PeerMsg req = PeerMsg.decodeMessageType(din, 0);
        if (req.mtype == MessageType.BitField) {
            PeerMsg bit = new PeerMsg(MessageType.BitField);
            dout.write(bit.msg);
            req = PeerMsg.decodeMessageType(din,0);
        }

        //PeerMsg.RequestMessage pr = new PeerMsg.RequestMessage()
        while (true) {
            if (req.mtype == MessageType.Request) {
                req = PeerMsg.decodeMessageType(din,0);
                if (RUBTClient.getMemory().have[req.pieceIndex]) {
                    byte[] block = new byte[req.reqLen];
                    System.arraycopy(RUBTClient.getMemory().get(req.pieceIndex), req.begin, block, 0, req.reqLen);

                    //PeerMsg pi = new PeerMsg.PieceMessage(req.pieceIndex,req.begin,block);
                    dout.write(new PeerMsg.PieceMessage(req.pieceIndex, req.begin, block).msg);
                    // update upload
                    RUBTClient.tInfo.setUploaded(req.reqLen);

                }
            }
        }
    }
    
    public void disconnect(){
    	try {
			this.din.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			this.dout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			this.sckt.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    /**
	 * This method sends the given message
	 * @param PeerMsg
	 */
	private void writeMessage(PeerMsg pm) throws IOException{
		if (this.sckt.isClosed())
			return;
		this.dout.write(pm.getMessage());
		this.dout.flush();
	}

    @Override
    public void run() {
    		
    		try {
    			if (handShake()) this.startUpload();
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
	        disconnect();
    	
    }
}
