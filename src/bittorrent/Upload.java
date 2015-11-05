package bittorrent;

import utility.Constants;
import utility.Converter;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created by krupal on 10/27/2015.
 */
public class Upload implements Constants, Runnable{

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
        din.readFully(recieve_msg);   
        String peer_id = Converter.objToStr(Arrays.copyOfRange(recieve_msg, 48,recieve_msg.length));
        if(RUBTClient.connectedPeers.contains(peer_id) ){
        	System.out.println("Already have a TCP connection on the download side");
        	return false;
        }
        if(!this.sckt.getRemoteSocketAddress().toString().contains("128.6.171.")){
        	System.out.println("This program only accepts connection from Rutgers ip-addresses");
        	return false;
        }
        System.arraycopy(TrackerInfo.my_peer_id, 0, recieve_msg, 48, 20);
        dout.write(recieve_msg);
        return recieve_msg[0] == 19;
    }

    public void startUpload() throws Exception {
    	
    	/* Stuff for bitfield (optional) 
        int numPieces = RUBTClient.tInfo.piece_hashes.length;
    	byte[] data = (numPieces % 8 == 0) ? new byte[numPieces/8] : new byte[numPieces/8 + 1];	
    	for(int i = 0; i < RUBTClient.getMemory().have.length; i++)
    		data[i/8] = (byte) ((RUBTClient.getMemory().have[i]) ? 0x80 >> (i % 8) : 0); */
    	
        boolean choked = false;
        int i;
        
        for(i = 0; i < RUBTClient.getMemory().have.length; i++)
        	if(RUBTClient.getMemory().have[i])
        		writeMessage(new PeerMsg.HaveMessage(i));
        
        while (!RUBTClient.terminate) {
        	PeerMsg req = PeerMsg.readMessage(din,i-1);
        	if(req == null) continue;
        	System.out.println(req);
        	switch(req.mtype){
        		case Keep_Alive:
        			break;
        		case Choke:
        			choked = true;
        			break;
        		case Un_Choke:
        			choked = false;
        			break;
        		case Interested:
        			writeMessage(new PeerMsg(MessageType.Un_Choke));
        			break;
        		case Not_Interested:
        			writeMessage(new PeerMsg(MessageType.Choke));
        			choked = true;
        			break;
        		case Request:
        			if (!choked && RUBTClient.getMemory().have[req.pieceIndex]) {
        				byte[] block = new byte[req.reqLen];
        				System.arraycopy(RUBTClient.getMemory().get(req.pieceIndex), req.begin, block, 0, req.reqLen);
        				dout.write(new PeerMsg.PieceMessage(req.pieceIndex, req.begin, block).msg);
        				RUBTClient.tInfo.setUploaded(RUBTClient.tInfo.getUploaded() + req.reqLen);
        				System.out.println("Wrote piece message");
        			}
        			break;
        		default:
        			continue;
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
