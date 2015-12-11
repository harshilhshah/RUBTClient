package gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.*;

import utility.Converter;
import bittorrent.Download;
import bittorrent.RUBTClient;
import bittorrent.Upload;

public class FilePanel extends JPanel{

	private static final long serialVersionUID = 1L;
	private GridBagConstraints c = new GridBagConstraints();
	private static JProgressBar progress = new JProgressBar(0);
	private static JLabel status = new JLabel("");
	private static JLabel fileName = new JLabel("");
	private static JLabel size = new JLabel("");
	private static JLabel downloaded = new JLabel("");
	private static JLabel uploaded = new JLabel("");
	private static JLabel eta = new JLabel("");
	private static JLabel peers = new JLabel("");
	private static JLabel seeders = new JLabel("");
	
	public FilePanel(){
		
		setLayout(new GridBagLayout());
	 
	    c.anchor = GridBagConstraints.PAGE_START;
	    c.ipady = 15;
	    c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
	    add(fileName,c);
	    
	    progress.setStringPainted(true);
	    progress.setVisible(false);
		c.anchor = GridBagConstraints.CENTER;
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 2;
		add(progress,c);
	    
	    c.anchor = GridBagConstraints.LINE_START;
		c.ipady = 5;
		c.gridwidth = 1;
		c.ipadx = 25;	    
	    c.gridx = 0;
		c.gridy = 2;
	    add(size,c);
	    
	    c.gridx = 1;
		c.gridy = 2;
	    add(status,c);
	    
		c.gridx = 0;
		c.gridy = 3;
	    add(downloaded,c);
	    
	    c.gridx = 1;
		c.gridy = 3;
	    add(uploaded,c);
	    
	    c.gridx = 0;
		c.gridy = 4;
	    add(peers,c);
	    
	    c.gridx = 1;
		c.gridy = 4;
	    add(seeders,c);
	    
	    c.gridx = 0;
	    c.gridy = 5;
	    c.gridwidth = 2;
	    add(eta,c);

	}
	
	public void addProgressBar(){
		progress.setVisible(true);
	}
	
	public void updateProgress(int prog){
		String download = Converter.readableByteCount(RUBTClient.tInfo.getDownloaded());
		String upload = Converter.readableByteCount(RUBTClient.tInfo.getUploaded());
		String fileSize = Converter.readableByteCount(RUBTClient.tInfo.file_length);
		String time = String.valueOf(Download.countTime() + " s");
		if(Download.countTime() > 9999)
			time = String.valueOf(Download.getTime()/1000000000 + ".0 s");
		progress.setValue(prog);
		downloaded.setText("<html><b>Downloaded:</b> " + download + "</html>");
		uploaded.setText("<html><b>Uploaded:</b> " + upload + "</html>");
		size.setText("<html><b>File size:</b> " + fileSize + "</html>");
		eta.setText("<html><b>Time Elapsed:</b> " + time + "</html>");
		seeders.setText("<html><b>Seeders:</b> " + Upload.getNum() + "</html>");
		peers.setText("<html><b>Peers:</b> " + Download.getNum() + "</html>");
		if(prog != 100) {
			setStatus("Downloading");
			return;
		}
		TopPanel.stopBtn.setEnabled(false);
		TopPanel.startBtn.setEnabled(false);
		status.setText("<html><b>Status:</b> Uploading</html>");
	}
	
	public void setFileName(String s){
		fileName.setText("<html><b>Tracker File:</b> " + s + "</html>");
	}

	public static void setStatus(String s){
		status.setText("<html><b>Status:</b> " + s + "</html>");
	}
	
	public static void updateNumSeeders(){
		String siz = String.valueOf(Upload.getNum());
		seeders.setText("<html><b>Seeders:</b> " + siz + "</html>");
		peers.setText("<html><b>Peers:</b> " + Download.getNum() + "</html>");
	}
	
	public static void updateUpload(){
		String upload = Converter.readableByteCount(RUBTClient.tInfo.getUploaded());
		uploaded.setText("<html><b>Uploaded:</b> " + upload + "</html>");
	}
	
	public static int getProgVal(){
		return progress.getValue();
	}
}
