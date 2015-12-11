package gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.*; 
import javax.swing.filechooser.FileNameExtensionFilter;

import bittorrent.RUBTClient;

public class MainView extends JFrame{

	private static final long serialVersionUID = 1L;
	
	private TopPanel tPanel;
	private ErrorPanel ePanel;
	private FilePanel fPanel;
	
	public MainView(){
		initUI();
	}
	
	public void initUI(){
		this.setVisible(true);
		this.setTitle("RU Bittorrent Client");
		this.setSize(700, 375);
		this.setResizable(false);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		setLayout();
	}
	
	private void setLayout(){
		
		this.setLayout(new BorderLayout());
		
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar(menuBar);	
		
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		JMenuItem openOption = new JMenuItem("Open");	
		fileMenu.add(openOption);
		JMenuItem closeOption = new JMenuItem("Close");
		fileMenu.add(closeOption);
		
		JMenu optionsMenu = new JMenu("Help");
		menuBar.add(optionsMenu);
	
		JMenuItem helpItem = new JMenuItem("Usage");
		optionsMenu.add(helpItem);
		
		JLabel saveLabel = new JLabel("Path: ");
		menuBar.add(saveLabel);
		
		tPanel = new TopPanel();
		tPanel.setPreferredSize(new Dimension(this.getWidth(),50));
		this.add(tPanel, BorderLayout.NORTH);
		
		fPanel = new FilePanel();
		fPanel.setPreferredSize(new Dimension(this.getWidth(),250));
		this.add(fPanel,BorderLayout.CENTER);
		
		ePanel = new ErrorPanel();
		ePanel.setPreferredSize(new Dimension(this.getWidth(),100));
		this.add(ePanel,BorderLayout.SOUTH);
		
		openOption.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				
				JFileChooser chooser= new JFileChooser(System.getProperty("user.dir"));
				chooser.setFileFilter(new FileNameExtensionFilter("Torrent","torrent"));
				chooser.setAcceptAllFileFilterUsed(false);

				int choice = chooser.showOpenDialog(fileMenu);
				if (choice != JFileChooser.APPROVE_OPTION) return;

				File torrent_file = chooser.getSelectedFile();
				
				if(RUBTClient.tInfo != null){
					JOptionPane.showMessageDialog(null, "Torrent file already being downloaded!");
					return;
				}
				
				if(!RUBTClient.createTrackerInfo(torrent_file))
					return;
				
				String result = JOptionPane.showInputDialog(null, "Enter file name:", RUBTClient.tInfo.file_name);
			   	if(result == null) {
			   		RUBTClient.tInfo = null;
			   		return;		    
			   	}
				RUBTClient.output_file = new File(result);
				saveLabel.setText(saveLabel.getText() + RUBTClient.output_file.getAbsolutePath());
				
				RUBTClient.setPeerThreads(torrent_file);
				tPanel.setFile(torrent_file);
				fPanel.setFileName(RUBTClient.tInfo.file_name);
				fPanel.addProgressBar();
				if(FilePanel.getProgVal() != 100){
					FilePanel.setStatus("Downloading");
					TopPanel.stopBtn.setEnabled(true);
				}
			}
		});
		
		closeOption.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				RUBTClient.quit();
			}
		});
		
		helpItem.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				String helpTxt = "<html><h2>Help</h2><p>Usage:</p>"
						+ "<p>File->Open->Select .torrent file to start downloading.</p>"
						+ "<p>File->Close to quit.</p>"
						+ "<p>A prompt will appear, file with given name will be saved in the project directory.</p>";
				JOptionPane.showMessageDialog(new JFrame(), helpTxt);
			}
			
		});
		
		addWindowListener(new WindowAdapter(){
			 public void windowClosing(WindowEvent e) {
				 	RUBTClient.quit();
	            }
		});
		
	}
	
	public void updateProgress(int progress){
		fPanel.updateProgress(progress);
	}
	
	public void display(String msg){
		ePanel.console.setText(ePanel.console.getText() + "\n " + msg);
		ePanel.console.setForeground(Color.WHITE);
	}
}
