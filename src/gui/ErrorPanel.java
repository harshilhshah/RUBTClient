package gui;

import java.awt.Color;

import javax.swing.*;

public class ErrorPanel extends JPanel{
	
	private static final long serialVersionUID = 1L;
	
	public JLabel console;
	
	public ErrorPanel(){
		this.setBackground(Color.RED);
		console = new JLabel("");
		add(console);
	}
	
}
