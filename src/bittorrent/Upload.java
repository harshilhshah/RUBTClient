package bittorrent;

import jdk.nashorn.internal.runtime.ECMAException;
import sun.misc.resources.Messages_es;
import utility.Constants;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Created by krupal on 10/27/2015.
 */
public class Upload extends Shared implements Runnable, Constants {

    public DataInputStream din = null;
    public DataOutputStream dout = null;
    public Socket sckt;
    public InputStream in = null;
    public OutputStream out = null;

    private TorrentInfo ti;
    //Shared sh;

    public Upload(Socket s, int size, TorrentInfo ti) throws Exception {
        //this.sh = new Shared(size);
        super(size);
        in = s.getInputStream();
        out = s.getOutputStream();
        din = new DataInputStream(in);
        dout = new DataOutputStream(out);
        this.ti = ti;

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
            System.arraycopy(this.ti.info_hash, 0, recieve_msg, 28, 20);
            System.arraycopy(TrackerInfo.my_peer_id, 0, recieve_msg, 48, 20);

            dout.write(recieve_msg);
            return true;
        }

    }

    public void startUpload() throws Exception {
        //
        PeerMsg uc = new PeerMsg(MessageType.Un_Choke);
        dout.write(uc.msg);

        //int len = din.read();
        //byte id = din.readByte();
        PeerMsg pm = new PeerMsg(MessageType.Piece);
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
                if (this.have[req.pieceIndex]) {
                    byte[] block = new byte[req.reqLen];
                    System.arraycopy(this.get(req.pieceIndex), req.begin, block, 0, req.reqLen);

                    //PeerMsg pi = new PeerMsg.PieceMessage(req.pieceIndex,req.begin,block);
                    dout.write(new PeerMsg.PieceMessage(req.pieceIndex, req.begin, block).msg);
                    // update upload
                    RUBTClient.tInfo.setUploaded(req.reqLen);

                }
            }
        }
    }

    @Override
    public void run() {
        try {
            if (handShake()) this.startUpload();

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            sckt.close();
            din.close();
            in.close();
            dout.close();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
