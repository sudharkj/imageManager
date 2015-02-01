package com.swing.imageManager.lib.dropbox;

import java.io.File;
import java.io.FileInputStream;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxWriteMode;
import com.dropbox.core.json.JsonReader.FileLoadException;
import com.swing.imageManager.lib.model.Pair;
import com.swing.imageManager.util.Constants;
import com.swing.imageManager.util.Helper;

public class DbxUploader implements Runnable {

	private final static Logger LOGGER;
	public final static String CLASS_NAME;

	private final long timeLimit = 5 * 60 * 1000;
	private final long uploadLimit = 4 * 60 * 1000;

	private DbxClient _dbxClient;

	static {
		CLASS_NAME = "DbxUploader";

		LOGGER = LogManager.getLogger(DbxUploader.class);
	}

	DbxUploader() throws FileLoadException {
		_dbxClient = DbxHelper.getDbxClient();
	}

	@Override
	public void run() {
		try {
			upload();
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("Error uploading recent changes: " + e.getMessage());
		}
	}

	private synchronized void upload() throws Exception {
		DbxEntry entry;
		long startTime = 0, curTime = 0;

		entry = _dbxClient.getMetadata(Constants.DBX_INDEX_FILE_NAME);

		while (Helper.UploadQueue.size() > 0) {
			getLockToUpload(entry, startTime, curTime);

			startTime = new Date().getTime();
			Pair current = Helper.UploadQueue.top();
			String from = current.getKey();
			String to = current.getValue();
			Helper.UploadQueue.deque();
			uploadFile(from, to);
			curTime = new Date().getTime();

			// delete buffer file associated with the uploaded file
			if (from.contains(Constants.LOCAL_KEY_DETAILS_PATH)) {
				File histFile = new File(Constants.TEMP_DIFF_KEY_DETAILS_PATH
						+ "/" + from.substring(from.lastIndexOf("/")));
				if (histFile.exists()) {
					if (histFile.delete()) {
						LOGGER.info("Deleted <" + histFile.getAbsolutePath()
								+ "> on successful upload of <" + from + ">");
					} else {
						LOGGER.info("Couldn't deleted <"
								+ histFile.getAbsolutePath()
								+ "> on successful upload of <" + from + ">");
					}
				} else {
					LOGGER.debug("<" + histFile.getAbsolutePath()
							+ "> do not exist");
				}
			}
		}
	}

	private void getLockToUpload(DbxEntry entry, long startTime, long curTime)
			throws Exception {
		DbxEntry indexMetadata;
		DbxEntry.WithChildren listing = null;

		long time, diff;

		while (entry == null
				|| (listing != null && listing.children.size() > 1)
				|| (curTime - startTime) > uploadLimit) {
			startTime = curTime = 0;

			uploadFile(Constants.LOCAL_INDEX_FILE_NAME,
					Constants.DBX_INDEX_FILE_NAME);
			entry = _dbxClient.getMetadata(Constants.DBX_INDEX_FILE_NAME);
			listing = _dbxClient
					.getMetadataWithChildren(Constants.DBX_TIME_STAMP_PATH);

			if (listing != null && listing.children.size() > 1) {
				// delete the conflicted files created during upload
				for (DbxEntry child : listing.children) {
					if (!child.path
							.equalsIgnoreCase(Constants.DBX_INDEX_FILE_NAME)) {
						_dbxClient.delete(child.path);
					}
				}

				// get the modified time of the lock (index file)
				time = 0;
				indexMetadata = _dbxClient
						.getMetadata(Constants.DBX_INDEX_FILE_NAME);
				if (indexMetadata != null)
					time = indexMetadata.asFile().lastModified.getTime();

				// wait till the lock is released (time has expired)
				diff = new Date().getTime() - time;
				if (diff < timeLimit) {
					synchronized (this) {
						LOGGER.info("Waiting " + (timeLimit - diff)
								+ "ms for the lock to be released");
						this.wait(timeLimit - diff);
						LOGGER.info("Assuming that lock is released");
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

	private void uploadFile(String from, String to) throws Exception {
		LOGGER.info("Uploading file [" + from + "] to [" + to + "]");
		DbxEntry metadata = _dbxClient.getMetadata(to);
		File file = new File(from);
		try (FileInputStream in = new FileInputStream(file)) {
			if (metadata != null)
				_dbxClient.delete(to);
			_dbxClient.uploadFile(to, DbxWriteMode.add(), -1, in);
		}
		LOGGER.info("Uploaded file [" + from + "] to [" + to + "]");
	}
}
