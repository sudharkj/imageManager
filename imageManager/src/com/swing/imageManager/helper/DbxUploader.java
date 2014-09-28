package com.swing.imageManager.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxWriteMode;

public class DbxUploader implements Runnable {

	private String[] toUpload = { "index", "keywords", "recDetails" };
	public final static String CLASS_NAME = "DbxUploader";
	public final static String HEADER = "In " + CLASS_NAME + ":\n";

	private DbxClient _dbxClient;

	DbxUploader() {
		_dbxClient = DbxHelper.getDbxClient();
	}

	@Override
	public void run() {
		DbxEntry entry = null;
		try {
			entry = _dbxClient.getMetadata(Helper.DBX_INDEX_FILE_NAME);
		} catch (DbxException e) {
			Helper.showMessage("Unknown exception occured: " + e.getMessage());
		}

		if (entry == null)
			createLocalIndexFile();

		boolean repeat = true;
		DbxEntry.WithChildren listing = null;
		if (entry == null) {
			try {
				uploadFile(Helper.LOCAL_INDEX_FILE_NAME,
						Helper.DBX_INDEX_FILE_NAME);
				listing = _dbxClient
						.getMetadataWithChildren(Helper.DBX_TIME_STAMP_PATH);
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
							.getMetadataWithChildren(Helper.DBX_TIME_STAMP_PATH);
					for (DbxEntry child : listing.children) {
						if (!child.path
								.equalsIgnoreCase(Helper.DBX_INDEX_FILE_NAME)) {
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
						.getMetadata(Helper.DBX_INDEX_FILE_NAME);
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
				uploadFile(Helper.LOCAL_INDEX_FILE_NAME,
						Helper.DBX_INDEX_FILE_NAME);
				listing = _dbxClient
						.getMetadataWithChildren(Helper.DBX_TIME_STAMP_PATH);
				if (listing.children.size() == 1)
					repeat = false;
			} catch (DbxException e) {
				repeat = true;
			}
		}

		for (String file : toUpload) {
			uploadFolder(Helper.CACHE_PATH + "/" + file, Helper.DBX_BASE_PATH
					+ "/" + file);
		}
	}

	private void createLocalIndexFile() {
		File indexFile = new File(Helper.LOCAL_INDEX_FILE_NAME);
		if (!indexFile.exists()) {
			try {
				if (indexFile.createNewFile())
					Helper.showMessage(Helper.LOCAL_INDEX_FILE_NAME
							+ " created");
				else
					Helper.showMessage("Error creating "
							+ Helper.LOCAL_INDEX_FILE_NAME);
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
