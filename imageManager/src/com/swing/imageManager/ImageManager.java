package com.swing.imageManager;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.UIManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.swing.imageManager.lib.dropbox.DbxHelper;
import com.swing.imageManager.ui.ImageManagerConsole;
import com.swing.imageManager.util.Constants;
import com.swing.imageManager.util.Helper;

public class ImageManager {

	private final static Logger LOGGER;

	static {
		LOGGER = LogManager.getLogger(ImageManager.class);
	}

	/**
	 * Launch the application.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		LOGGER.info("Use Java SE 7 or above for expected functionality");

		try {
			String temp;
			try (BufferedReader br = new BufferedReader(new FileReader(
					Constants.DBX_DIR_LOC_FILE_NAME))) {
				temp = br.readLine();
			}

			Constants.LOCAL_BASE_PATH = (temp == null) ? "" : temp.trim();
			if (Constants.LOCAL_BASE_PATH.isEmpty()) {
				throw new Exception("Dropbox location not set in "
						+ Constants.DBX_DIR_LOC_FILE_NAME);
			}
			LOGGER.info("Dropbox folder location: " + Constants.LOCAL_BASE_PATH);
			Constants.LOCAL_IMAGES_PATH = Constants.LOCAL_BASE_PATH
					+ "/imageFiles";
			Constants.LOCAL_THUMBS_PATH = Constants.LOCAL_BASE_PATH
					+ "/thumbImages";

			try {
				UIManager
						.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
			} catch (Exception e) {
				e.printStackTrace();
			}

			initApplication();

		} catch (Exception e) {
			e.printStackTrace();
			closeApplication("Error initializing " + Constants.APPLICATION_NAME
					+ ": " + e.getMessage());
		}

		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					new ImageManagerConsole();
				} catch (IOException e) {
					e.printStackTrace();
					closeApplication("Error running "
							+ Constants.APPLICATION_NAME + ": "
							+ e.getMessage());
				}
			}
		});
	}

	/**
	 * TODO: create folders used for application
	 * 
	 * @throws IOException
	 */
	public static void initApplication() throws IOException {
		Helper.getFile(Constants.LOCAL_INDEX_FILE_NAME);
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
