package com.swing.imageManager.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxWriteMode;

public class DbxUploader implements Runnable {

	final static Logger LOGGER = LogManager.getLogger(DbxUploader.class);

	public final static String CLASS_NAME = "DbxUploader";
	private final long timeLimit = 5 * 60 * 1000;
	private final long uploadLimit = 4 * 60 * 1000;

	private DbxClient _dbxClient;

	DbxUploader() {
		_dbxClient = DbxHelper.getDbxClient();
	}

	@Override
	public void run() {
		upload();
	}

	private synchronized void upload() {
		DbxEntry entry = null;
		long startTime = 0, curTime = 0;

		try {
			entry = _dbxClient.getMetadata(Helper.DBX_INDEX_FILE_NAME);
		} catch (DbxException e) {
			LOGGER.error("Unknown exception occured: " + e.getMessage());
			return;
		}

		while (Helper.UploadQueue.size() > 0) {
			try {
				getLockToUpload(entry, startTime, curTime);
			} catch (DbxException | IOException e) {
				LOGGER.info("Error acquiring lock: " + e.getMessage());
				return;
			}

			startTime = new Date().getTime();
			Pair current = Helper.UploadQueue.get(0);
			Helper.UploadQueue.remove(0);
			try {
				uploadFile(current.getKey(), current.getValue());
			} catch (DbxException | IOException e) {
				LOGGER.info("Error uploading file [" + current.getKey()
						+ "] to [" + current.getValue() + "]");
				Helper.UploadQueue.add(current);
			}
			curTime = new Date().getTime();
		}
	}

	private void getLockToUpload(DbxEntry entry, long startTime, long curTime)
			throws DbxException, IOException {
		DbxEntry indexMetadata;
		DbxEntry.WithChildren listing = null;

		long time, diff;

		while (entry == null
				|| (listing != null && listing.children.size() > 1)
				|| (curTime - startTime) > uploadLimit) {
			startTime = curTime = 0;

			uploadFile(Helper.LOCAL_INDEX_FILE_NAME, Helper.DBX_INDEX_FILE_NAME);
			entry = _dbxClient.getMetadata(Helper.DBX_INDEX_FILE_NAME);
			listing = _dbxClient
					.getMetadataWithChildren(Helper.DBX_TIME_STAMP_PATH);

			if (listing != null && listing.children.size() > 1) {
				// delete the conflicted files created during upload
				for (DbxEntry child : listing.children) {
					if (!child.path
							.equalsIgnoreCase(Helper.DBX_INDEX_FILE_NAME)) {
						_dbxClient.delete(child.path);
					}
				}

				// get the modified time of the lock (index file)
				time = 0;
				indexMetadata = _dbxClient
						.getMetadata(Helper.DBX_INDEX_FILE_NAME);
				if (indexMetadata != null)
					time = indexMetadata.asFile().lastModified.getTime();

				// wait till the lock is released (time has expired)
				diff = new Date().getTime() - time;
				if (diff < timeLimit) {
					synchronized (this) {
						try {
							LOGGER.info("Waiting " + (timeLimit - diff)
									+ "ms for the lock to be released");
							this.wait(timeLimit - diff);
							LOGGER.info("Assuming that lock is released");
						} catch (InterruptedException e) {
							LOGGER.error("Wait interrupted: " + e.getMessage());
						}
					}
				} else {
					break;
				}
			} else {
				break;
			}
		}
		LOGGER.info("Lock is released");
	}

	private void uploadFile(String from, String to) throws DbxException,
			IOException {
		LOGGER.info("Uploading file [" + from + "] to [" + to + "]");
		DbxEntry metadata = _dbxClient.getMetadata(to);
		File file = new File(from);
		FileInputStream in = new FileInputStream(file);
		if (metadata != null)
			_dbxClient.delete(to);
		_dbxClient.uploadFile(to, DbxWriteMode.add(), -1, in);
		LOGGER.info("Uploaded file [" + from + "] to [" + to + "]");
	}
}
