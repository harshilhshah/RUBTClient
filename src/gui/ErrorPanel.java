package gui;

import java.awt.Color;

import javax.swing.*;

public class ErrorPanel extends JPanel{
	
	private static final long serialVersionUID = 1L;
	
	public JTextArea console;
	
	public ErrorPanel(){
		console = new JTextArea(5,54);
		console.setEditable(false);
		JScrollPane js = new JScrollPane(console);
		console.setBackground(Color.RED);
		js.setPreferredSize(console.getPreferredSize());
		js.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		js.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		add(js);
	}
	
}
