package bittorrent;

/**
 * @author Harshil Shah, Krupal Suthar, Aishwarya Gondhi
 */

import utility.Constants;
import gui.FilePanel;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class Upload extends Thread implements Constants, Peer{
	
	private static volatile int counter = 0;
    private DataInputStream din = null;
    private DataOutputStream dout = null;
    private Socket sckt;
<<<<<<< HEAD
	private Timer keepAliveTimer = new Timer();
	private boolean choke = false;
	private long updateTime;
	private String peer_ip;

    public Upload(Socket soc) throws IOException {
    	this.sckt = soc;
    	this.din = new DataInputStream(soc.getInputStream());
    	this.dout = new DataOutputStream(soc.getOutputStream());
    	this.peer_ip = this.sckt.getRemoteSocketAddress().toString();
    	RUBTClient.print("Recieved a connection from " +  peer_ip);
    	if(counter < 7) {
    		counter++;
    		start();
    	}else{
    		disconnect();
    	}
=======
    private Timer keepAliveTimer = new Timer();

    public Upload(Socket soc) throws IOException {
        this.sckt = soc;
        this.din = new DataInputStream(soc.getInputStream());
        this.dout = new DataOutputStream(soc.getOutputStream());
        System.out.println("Recieved a connection from " +  this.sckt.getRemoteSocketAddress().toString());
>>>>>>> origin/master
    }

    public boolean handShake() throws Exception {
        byte[] recieve_msg = new byte[68];
<<<<<<< HEAD
        din.readFully(recieve_msg);   
        /*if(!this.peer_ip.contains("128.6.")){
        	RUBTClient.print("This program only accepts connection from Rutgers ip-addresses");
        	return false;
        }*/
=======
        din.readFully(recieve_msg);
        String peer_id = Converter.objToStr(Arrays.copyOfRange(recieve_msg, 48,recieve_msg.length));
        if(RUBTClient.getPeers().contains(peer_id) ){
            System.out.println("Already have a TCP connection on the download side");
            return false;
        }
        if(!this.sckt.getRemoteSocketAddress().toString().contains("128.6.171.")){
            System.out.println("This program only accepts connection from Rutgers ip-addresses");
            return false;
        }
>>>>>>> origin/master
        System.arraycopy(TrackerInfo.my_peer_id, 0, recieve_msg, 48, 20);
        dout.write(recieve_msg);
        return recieve_msg[0] == 19;
    }

    public void startUpload() throws Exception {
    	
    	counter++;
    	
    	/* Stuff for bitfield (optional) 
        int numPieces = RUBTClient.tInfo.piece_hashes.length;
    	byte[] data = (numPieces % 8 == 0) ? new byte[numPieces/8] : new byte[numPieces/8 + 1];	
    	for(int i = 0; i < RUBTClient.getMemory().have.length; i++)
    		data[i/8] = (byte) ((RUBTClient.getMemory().have[i]) ? 0x80 >> (i % 8) : 0); */
<<<<<<< HEAD
    	
    	keepAliveTimer.schedule(new KeepMeAlive(), 120000);
        int i;
        boolean[] initHaveArr = RUBTClient.getMemory().have;
        
        for(i = 0; i < initHaveArr.length; i++)
        	if(initHaveArr[i])
        		writeMessage(new PeerMsg.HaveMessage(i));
        
        while (!RUBTClient.terminate) {
        	if(!Arrays.equals(initHaveArr, RUBTClient.getMemory().have))
        		for(i = 0; i < initHaveArr.length; i++)
                	if(initHaveArr[i] != RUBTClient.getMemory().have[i]){
                		writeMessage(new PeerMsg.HaveMessage(i));
                		initHaveArr[i] = !initHaveArr[i];
                	}
        	
        	PeerMsg req = PeerMsg.readMessage(din,i-1);
        	if(req == null) continue;
        	switch(req.mtype){
        		case Choke:
        			choke = true;
        			break;
        		case Un_Choke:
        			choke = false;
        			break;
        		case Interested:
        			writeMessage(new PeerMsg(MessageType.Un_Choke));
        			choke = false;
        			break;
        		case Not_Interested:
        			writeMessage(new PeerMsg(MessageType.Choke));
        			choke = true;
        			break;
        		case Request:
        			if (!choke && RUBTClient.getMemory().have[req.pieceIndex]) {
        				byte[] block = new byte[req.reqLen];
        				System.arraycopy(RUBTClient.getMemory().get(req.pieceIndex), req.begin, block, 0, req.reqLen);
        				dout.write(new PeerMsg.PieceMessage(req.pieceIndex, req.begin, block).msg);
        				RUBTClient.tInfo.setUploaded(RUBTClient.tInfo.getUploaded() + req.reqLen);
        				FilePanel.updateUpload();
        			}
        			break;
        		default:
        			break;
            } 
        }
    }
    
    public static synchronized void decr(){
    	counter--;
    }
    
    public void disconnect(){
    	keepAliveTimer.cancel();
    	decr();
    	RUBTClient.print("Upload: Disconnecting from the peer at " + peer_ip);
    	FilePanel.updateNumSeeders();
    	try {
			this.din.close();
		} catch (IOException e) {
		}
		try {
			this.dout.close();
		} catch (IOException e) {
		}
		try {
			this.sckt.close();
		} catch (IOException e) {
		}
		interrupt();
=======

        keepAliveTimer.schedule(new KeepMeAlive(), 120000);
        boolean choked = false;
        int i;

        System.out.println("Sending Have messages for each piece that we have");
        for(i = 0; i < RUBTClient.getMemory().have.length; i++)
            if(RUBTClient.getMemory().have[i])
                writeMessage(new PeerMsg.HaveMessage(i));

        while (!RUBTClient.terminate) {
            PeerMsg req = PeerMsg.readMessage(din,i-1);
            if(req == null) continue;
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
                        System.out.println("Sending requested piece to peer.");
                    }
                    break;
                default:
                    continue;
            }
        }
    }

    public void disconnect(){
        keepAliveTimer.cancel();
        try {
            this.din.close();
        } catch (IOException e) {
        }
        try {
            this.dout.close();
        } catch (IOException e) {
        }
        try {
            this.sckt.close();
        } catch (IOException e) {
        }
>>>>>>> origin/master
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
<<<<<<< HEAD
    		
    		try {
    			if (handShake()) this.startUpload();
    		} catch (Exception e) {
    			if(e.getMessage() != null)
    				RUBTClient.print("Peer at " + peer_ip + "caused: " + e.getMessage());
    		}
	        disconnect();
    	
=======

        try {
            if (handShake()) this.startUpload();
        } catch (Exception e) {
            System.out.println("There was a miscommunication with the peer.");;
        }
        disconnect();

>>>>>>> origin/master
    }

    private class KeepMeAlive extends TimerTask{

        @Override
        public void run() {
            try {
                writeMessage(new PeerMsg(MessageType.Keep_Alive));
            } catch (IOException e) {
            }
            keepAliveTimer.schedule(new KeepMeAlive(), 120000);
        }

    }
    
    public static int getNum(){
    	return counter;
    }

	@Override
	public void choke() {
		try {
			writeMessage(new PeerMsg(MessageType.Choke));
		} catch (IOException e) {}
		choke = true;
	}

	@Override
	public void unchoke() {
		try {
			writeMessage(new PeerMsg(MessageType.Un_Choke));
		} catch (IOException e) {}
		choke = false;
	}

	@Override
	public boolean isChoked() {
		return choke;
	}

	@Override
	public void setLastUpdated(long l) {
		updateTime = l;
	}

	@Override
	public long getLastUpdated() {
		return updateTime;
	}
}
