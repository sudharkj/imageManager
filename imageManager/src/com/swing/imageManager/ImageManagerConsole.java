package com.swing.imageManager;

import java.awt.EventQueue;
import java.io.IOException;

import javax.swing.JFrame;

import com.swing.imageManager.helper.DbxHelper;

public class ImageManagerConsole {
	
	private JFrame frame;

	/**
	 * Launch the application.
	 * @throws IOException 
	 */
	public static void main(String[] args) {
		new DbxHelper();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ImageManagerConsole window = new ImageManagerConsole();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ImageManagerConsole() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
