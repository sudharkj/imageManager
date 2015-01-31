package com.swing.imageManager;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.UIManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.swing.imageManager.util.Constants;
import com.swing.imageManager.util.Helper;
import com.swing.imageManager.lib.dropbox.DbxHelper;
import com.swing.imageManager.ui.ImageManagerConsole;

public class ImageManager {

	final static Logger LOGGER = LogManager.getLogger(ImageManager.class);

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		LOGGER.info("Use Java SE 7 or above for expected functionality");
		
		try (BufferedReader br = new BufferedReader(new FileReader(
				Constants.DBX_DIR_LOC_FILE_NAME))) {
			String temp = br.readLine();
			Constants.LOCAL_BASE_PATH = (temp == null) ? "" : temp.trim();
			if (Constants.LOCAL_BASE_PATH.isEmpty()) {
				closeApplication("Dropbox location not set in "
						+ Constants.DBX_DIR_LOC_FILE_NAME);
			}
			LOGGER.info("Dropbox folder location: " + Constants.LOCAL_BASE_PATH);
			Constants.LOCAL_IMAGES_PATH = Constants.LOCAL_BASE_PATH
					+ "/imageFiles";
			Constants.LOCAL_THUMBS_PATH = Constants.LOCAL_BASE_PATH
					+ "/thumbImages";
		} catch (IOException e) {
			closeApplication("Error reading <dbx-dir-loc>: " + e.getMessage());
		}

		try {
			UIManager
					.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Throwable e) {
			LOGGER.error(e.getMessage());
		}

		initApplication();

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					new ImageManagerConsole();
				} catch (Exception e) {
					closeApplication(e.getMessage());
				}
			}
		});
	}

	/**
	 * TODO: create folders used for application
	 */
	public static void initApplication() {
		try {
			Helper.getFile(Constants.LOCAL_INDEX_FILE_NAME);
		} catch (IOException e) {
			LOGGER.error("Error creating <" + Constants.LOCAL_INDEX_FILE_NAME
					+ ">: " + e.getMessage());
		}
		new DbxHelper();
	}

	/**
	 * closeApplication
	 * 
	 * @param message
	 */
	private static void closeApplication(String message) {
		LOGGER.error(message);
		LOGGER.warn("Aborting system");
		System.exit(-1);
	}

}
