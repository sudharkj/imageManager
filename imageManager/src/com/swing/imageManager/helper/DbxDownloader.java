/**
 * 
 */
package com.swing.imageManager.helper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxDelta;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;

/**
 * @author Ravi
 * 
 */
public class DbxDownloader implements Runnable {

	private final static Logger LOGGER = LogManager
			.getLogger(DbxDownloader.class);

	private DbxClient _dbxClient;
	private String _cursor;

	private Date LastSyncedTime = new Date();
	private Date LatestSyncingTime;

	DbxDownloader() {
		_dbxClient = DbxHelper.getDbxClient();
	}

	@Override
	public void run() {
		File deltaCursorFile = new File(Helper.DBX_DELTA_CURSOR_FILE_NAME);
		File syncTimeFile = new File(Helper.LAST_SYNC_TIME_FILE_PATH);
		String lastSyncedStrTime = null;

		// load cursor
		try {
			_cursor = getContent(deltaCursorFile);
		} catch (IOException e) {
			LOGGER.error("Error reading <" + deltaCursorFile.getAbsolutePath()
					+ ">: " + e.getMessage());
			return;
		}
		if (_cursor == null)
			_cursor = "";
		LOGGER.info("Cursor value from <" + deltaCursorFile.getAbsolutePath()
				+ ">: " + _cursor);

		// load lastSyncedTime
		try {
			lastSyncedStrTime = getContent(syncTimeFile);
		} catch (IOException e) {
			LOGGER.error("Error reading <" + syncTimeFile.getAbsolutePath()
					+ ">: " + e.getMessage());
			return;
		}
		if (lastSyncedStrTime == null || lastSyncedStrTime.isEmpty())
			LastSyncedTime.setTime(0);
		else
			LastSyncedTime.setTime(((Long) Long.parseLong(lastSyncedStrTime))
					.longValue());
		LOGGER.info("Last sync time from <" + syncTimeFile.getAbsolutePath()
				+ ">: " + LastSyncedTime);

		LatestSyncingTime = new Date();

		// sync local files
		try {
			DbxDelta<DbxEntry> deltaEntry = null;
			do {
				deltaEntry = _dbxClient.getDeltaWithPathPrefix(_cursor,
						Helper.DBX_BASE_PATH);
				for (DbxDelta.Entry<DbxEntry> entry : deltaEntry.entries) {
					manageEntry(entry.metadata.asFile());
				}
			} while (deltaEntry.hasMore);
			_cursor = deltaEntry.cursor;
		} catch (DbxException e) {
			LOGGER.info("Unknown exception occured: " + e.getMessage());
			return;
		} catch (Exception e) {
			return;
		}

		// save the latest cursor
		try {
			updateFile(deltaCursorFile, _cursor);
		} catch (IOException e) {
			LOGGER.error("Error updating <" + deltaCursorFile.getAbsolutePath()
					+ ">: " + e.getMessage());
			return;
		}

		// save the LatestSyncingTime
		try {
			updateFile(syncTimeFile,
					((Long) LatestSyncingTime.getTime()).toString());
		} catch (IOException e) {
			LOGGER.error("Error updating <" + syncTimeFile.getAbsolutePath()
					+ ">: " + e.getMessage());
			return;
		}
		
		// TODO index files after download and upload to main server
	}

	private String getContent(File file) throws IOException {
		String content = null;
		BufferedReader br = new BufferedReader(new FileReader(file));
		content = br.readLine();
		br.close();
		return content;
	}

	private void manageEntry(DbxEntry.File dbxFile) throws Exception {
		File localFile = null;
		boolean useLastSyncedTime = true;

		String path = dbxFile.path;
		String parentPath = path.substring(0, path.lastIndexOf('/'));
		String fileName = path.substring(path.lastIndexOf('/') + 1);

		if (parentPath.contains(Helper.DBX_IMAGES_PATH)) {
			localFile = new File(Helper.LOCAL_IMAGES_PATH + "/" + fileName);
			useLastSyncedTime = false;
		} else if (parentPath.contains(Helper.DBX_THUMBS_PATH)) {
			localFile = new File(Helper.LOCAL_THUMBS_PATH + "/" + fileName);
			useLastSyncedTime = false;
		} else if (parentPath.contains(Helper.DBX_INDEX_PATH)) {
			localFile = new File(Helper.LOCAL_INDEX_PATH + "/" + fileName);
		} else if (parentPath.contains(Helper.DBX_KEYWORDS_PATH)) {
			localFile = new File(Helper.LOCAL_KEYWORDS_PATH + "/" + fileName);
		} else if (parentPath.contains(Helper.DBX_RECTANGLES_PATH)) {
			localFile = new File(Helper.LOCAL_RECTANGLES_PATH + "/" + fileName);
		} else {
			return;
		}

		Date dbxFileModifiedTime = dbxFile.lastModified;
		Date localFileModifiedTime;

		if (LatestSyncingTime.after(dbxFileModifiedTime)) {
			LatestSyncingTime.setTime(dbxFileModifiedTime.getTime());
		}

		if (useLastSyncedTime) {
			localFileModifiedTime = LastSyncedTime;
		} else {
			if (localFile.exists()) {
				localFileModifiedTime = new Date(localFile.lastModified());
			} else {
				localFileModifiedTime = new Date(0);
			}
		}

		if (localFile != null
				&& dbxFileModifiedTime.after(localFileModifiedTime)) {
			try {
				downloadFile(dbxFile, localFile);
			} catch (DbxException | IOException e) {
				LOGGER.error("Error downloading <" + dbxFile.path + ">: "
						+ e.getMessage());
				throw new Exception(e.getMessage());
			}
			// TODO synchronize data with buffers (or history)
		}
	}

	private void downloadFile(DbxEntry.File dbxFile, File localFile)
			throws DbxException, IOException {
		FileOutputStream outputStream = new FileOutputStream(localFile);
		_dbxClient.getFile(dbxFile.path, null, outputStream);
		outputStream.close();
		LOGGER.info("Downloaded <" + dbxFile.path + ">");
	}

	private void updateFile(File file, String content) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		writer.write(content);
		writer.close();
		LOGGER.info("Updated <" + file.getAbsolutePath() + "> to have <"
				+ content + ">");
	}
}
