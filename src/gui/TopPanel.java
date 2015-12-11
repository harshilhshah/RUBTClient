package gui;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import bittorrent.RUBTClient;

public class TopPanel extends JPanel{

	private static final long serialVersionUID = 1L;
	
	private File f;
	
	static JButton startBtn;
	static JButton stopBtn;
	
	public TopPanel(){
		
		ImageIcon playImg = new ImageIcon(getClass().getResource("Play.png"));
		playImg = new ImageIcon(playImg.getImage().getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH));
		startBtn = new JButton(playImg);
		startBtn.setEnabled(false);
	    add(startBtn);
	    
	    ImageIcon stopImg = new ImageIcon(getClass().getResource("Stop.png"));
	    stopImg = new ImageIcon(stopImg.getImage().getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH));
	    stopBtn = new JButton(stopImg);
	    stopBtn.setEnabled(false);
	    add(stopBtn);
	    
	    startBtn.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				startBtn.setEnabled(false);
				stopBtn.setEnabled(true);
				if(RUBTClient.tInfo == null)
					if(RUBTClient.createTrackerInfo(f))
						RUBTClient.setPeerThreads(f);
					else
						return;
				else
					RUBTClient.resumeDownloaders();
				FilePanel.setStatus("Downloading");
			}
	    	
	    });
	    
	    stopBtn.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				startBtn.setEnabled(true);
				stopBtn.setEnabled(false);
				RUBTClient.stopDownloaders(false);
				FilePanel.setStatus("Stopped");
			}
	    	
	    });
		
	}
	
	public void setFile(File file){
		f = file;
	}

}
