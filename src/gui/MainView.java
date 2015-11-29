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
	
	private ErrorPanel ePanel;
	private FilePanel fPanel;
	
	public MainView(){
		initUI();
	}
	
	public void initUI(){
		this.setVisible(true);
		this.setTitle("RU Bittorrent Client");
		this.setSize(900, 600);
		this.setResizable(false);
		this.setLocationRelativeTo(null);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setLayout(new BorderLayout());
		setLayout();
	}
	
	private void setLayout(){
		
		JMenuBar menuBar = new JMenuBar();
		this.setJMenuBar(menuBar);	
		
		JMenu fileMenu = new JMenu("File");
		menuBar.add(fileMenu);
		JMenuItem openOption = new JMenuItem("Open");	
		fileMenu.add(openOption);
		JMenuItem mnExit = new JMenuItem("Close");
		fileMenu.add(mnExit);		
		JMenuItem saveAsItem = new JMenuItem("Save As");
		fileMenu.add(saveAsItem);
		
		JMenu optionsMenu = new JMenu("Help");
		menuBar.add(optionsMenu);
	
		JMenuItem helpItem = new JMenuItem("Usage");
		optionsMenu.add(helpItem);
		
		JTextField saveInput = new JTextField("video.mov");
		
		menuBar.add(new JLabel("Save As: "));
		menuBar.add(saveInput);
		
		fPanel = new FilePanel();
		this.add(fPanel,BorderLayout.CENTER);
		
		ePanel = new ErrorPanel();
		ePanel.setPreferredSize(new Dimension(this.getWidth(),30));
		this.add(ePanel,BorderLayout.SOUTH);
		
		openOption.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser= new JFileChooser(System.getProperty("user.dir"));
				chooser.setFileFilter(new FileNameExtensionFilter("Torrent","torrent"));

				int choice = chooser.showOpenDialog(fileMenu);
				if (choice != JFileChooser.APPROVE_OPTION) return;

				File torrent_file = chooser.getSelectedFile();
				
				RUBTClient.output_file = new File(saveInput.getText());
				
				if(!RUBTClient.createTrackerInfo(torrent_file))
					return;
				
				RUBTClient.setPeerThreads(torrent_file);
				fPanel.f = torrent_file;
				fPanel.fileName.setText(RUBTClient.tInfo.file_name);
				fPanel.add(fPanel.progress);
				fPanel.stopBtn.setEnabled(true);
			}
		});
		
		helpItem.addActionListener(new ActionListener(){

			@Override
			public void actionPerformed(ActionEvent e) {
				String helpTxt = "<html><h2>Help</h2><p>Usage:</p>"
						+ "<p>File->Open->Select .torrent file to start downloading.</p>"
						+ "<p>File->Save As to change target file name.</p>"
						+ "<p>Click start. Specified file will be saved in the project directory</p>";
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
		fPanel.progress.setValue(progress);
	}
	
	public void display(String msg){
		ePanel.console.setText(msg);
		ePanel.console.setForeground(Color.WHITE);
	}
}
