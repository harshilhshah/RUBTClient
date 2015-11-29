package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.*;

import bittorrent.RUBTClient;

public class FilePanel extends JPanel{

	private static final long serialVersionUID = 1L;
	
	JButton startBtn;
	JButton stopBtn;
	JButton pauseBtn;
	JLabel fileName;
	JProgressBar progress;
	File f;
	
	
	public FilePanel(){
		
		ImageIcon playImg = new ImageIcon(getClass().getResource("/images/Play.png"));
		playImg = new ImageIcon(playImg.getImage().getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH));
		startBtn = new JButton(playImg);
		startBtn.setEnabled(false);
	    add(startBtn);
	    
	    ImageIcon stopImg = new ImageIcon(getClass().getResource("/images/Stop.png"));
	    stopImg = new ImageIcon(stopImg.getImage().getScaledInstance(20, 20, java.awt.Image.SCALE_SMOOTH));
	    stopBtn = new JButton(stopImg);
	    stopBtn.setEnabled(false);
	    add(stopBtn);
	    
	    fileName = new JLabel("");
	    add(fileName);
	    
	    progress = new JProgressBar(0);
	    progress.setStringPainted(true);
	    
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
			}
	    	
	    });
	    
	    stopBtn.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				startBtn.setEnabled(true);
				stopBtn.setEnabled(false);
				RUBTClient.stopDownloaders(false);
			}
	    	
	    });
	}

}
