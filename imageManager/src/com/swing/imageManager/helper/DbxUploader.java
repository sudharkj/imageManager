package com.swing.imageManager.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWriteMode;
import com.dropbox.core.json.JsonReader;

public class DbxUploader implements Runnable {

	private String[] toUpload = { "index", "keywords", "recDetails" };
	public final static String CLASS_NAME = "DbxUploader";
	public final static String HEADER = "In " + CLASS_NAME + ":\n";
	private final static String DBX_BASE_PATH = "/NoticeBox";
	private final static String DBX_TIME_STAMP_PATH = DBX_BASE_PATH
			+ "/timestamp";
	private final static String DBX_INDEX_FILE_NAME = DBX_TIME_STAMP_PATH
			+ "/index.txt";

	private String LocalIndexFilePath = Helper.TEMP_PATH + "/index.txt";

	private DbxClient _dbxClient;

	DbxUploader() {
		// Read auth info file.
		DbxAuthInfo authInfo = null;
		try {
			authInfo = DbxAuthInfo.Reader
					.readFromFile(Helper.USER_AUTH_TOKEN_FILE_NAME);
		} catch (JsonReader.FileLoadException e) {
			Helper.handleError(HEADER
					+ "Error reading <user-auth-token-file>: " + e.getMessage());
		}

		// Create a DbxClient, which is what you use to make API calls.
		String userLocale = Locale.getDefault().toString();
		DbxRequestConfig requestConfig = new DbxRequestConfig(CLASS_NAME,
				userLocale);
		_dbxClient = new DbxClient(requestConfig, authInfo.accessToken,
				authInfo.host);
	}

	@Override
	public void run() {
		DbxEntry entry = null;
		try {
			entry = _dbxClient.getMetadata(DBX_INDEX_FILE_NAME);
		} catch (DbxException e) {
			Helper.showMessage("Unknown exception occured: " + e.getMessage());
		}

		if (entry == null)
			createLocalIndexFile();

		boolean repeat = true;
		DbxEntry.WithChildren listing = null;
		if (entry == null) {
			try {
				uploadFile(LocalIndexFilePath, DBX_INDEX_FILE_NAME);
				listing = _dbxClient
						.getMetadataWithChildren(DBX_TIME_STAMP_PATH);
				if (listing.children.size() == 1)
					repeat = false;
			} catch (DbxException e) {
				repeat = true;
			}
		}
		while (repeat) {
			// conflict occured while creating the index file
			if (listing != null && listing.children.size() > 1) {
				try {
					listing = _dbxClient
							.getMetadataWithChildren(DBX_TIME_STAMP_PATH);
					for (DbxEntry child : listing.children) {
						if (!child.path.equalsIgnoreCase(DBX_INDEX_FILE_NAME)) {
							_dbxClient.delete(child.path);
						}
					}
				} catch (DbxException e) {
					Helper.showMessage("Unknown exception caught: "
							+ e.getMessage());
				}
			}

			// get the modified time
			long time = 0;
			try {
				DbxEntry indexMetadata = _dbxClient
						.getMetadata(DBX_INDEX_FILE_NAME);
				if (indexMetadata != null)
					time = indexMetadata.asFile().lastModified.getTime();
			} catch (DbxException e1) {
				// e1.printStackTrace(); // ignore the exception
			}

			long diff = new Date().getTime() - time;
			long limit = 5 * 60 * 1000;
			if (diff < limit) {
				synchronized (this) {
					try {
						Helper.showMessage("going to wait");
						this.wait(limit - diff);
						createLocalIndexFile();
						Helper.showMessage("waiting over");
					} catch (InterruptedException e) {
						// e.printStackTrace(); // ignore the error
					}
				}
			}

			try {
				uploadFile(LocalIndexFilePath, DBX_INDEX_FILE_NAME);
				listing = _dbxClient
						.getMetadataWithChildren(DBX_TIME_STAMP_PATH);
				if (listing.children.size() == 1)
					repeat = false;
			} catch (DbxException e) {
				repeat = true;
			}
		}

		for (String file : toUpload) {
			uploadFolder(Helper.CACHE_PATH + "/" + file, DBX_BASE_PATH + "/"
					+ file);
		}
	}

	private void createLocalIndexFile() {
		File indexFile = new File(LocalIndexFilePath);
		if (!indexFile.exists()) {
			try {
				if (indexFile.createNewFile())
					Helper.showMessage(LocalIndexFilePath + " created");
				else
					Helper.showMessage("Error creating " + LocalIndexFilePath);
			} catch (IOException e1) {
				// e1.printStackTrace(); // ignore the caught exception
			}
		}
	}

	private void uploadFile(String from, String to) throws DbxException {

		// Make the API call to upload the file.
		Helper.showMessage("uploading file [" + from + "] to [" + to + "]");
		// log
		try {
			DbxEntry metadata = _dbxClient.getMetadata(to);
			File file = new File(from);
			FileInputStream in = new FileInputStream(file);
			try {
				if (metadata != null)
					_dbxClient.delete(to);
				_dbxClient.uploadFile(to, DbxWriteMode.add(), -1, in);
			} catch (DbxException ex) {
				in.close();
				throw ex;
			}
		} catch (IOException ex) {
			// ex.printStackTrace(); // ignore error
		}

	}

	private void uploadFolder(String from, String to) {
		File dir = new File(from);
		if (dir.isDirectory()) {
			for (String file : dir.list()) {
				try {
					uploadFile(from + "/" + file, to + "/" + file);
				} catch (DbxException e) {
					// e.printStackTrace(); // ignore error
				}
			}
		}
	}

}
