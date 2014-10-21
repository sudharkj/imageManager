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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
			LOGGER.debug("Exception occured: " + e.getMessage());
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
		boolean changedKeyDetails = false;

		if (parentPath.contains(Helper.DBX_IMAGES_PATH)) {
			localFile = new File(Helper.LOCAL_IMAGES_PATH + "/" + fileName);
			useLastSyncedTime = false;
		} else if (parentPath.contains(Helper.DBX_THUMBS_PATH)) {
			localFile = new File(Helper.LOCAL_THUMBS_PATH + "/" + fileName);
			useLastSyncedTime = false;
		} else if (parentPath.contains(Helper.DBX_INDEX_PATH)) {
			localFile = new File(Helper.LOCAL_INDEX_PATH + "/" + fileName);
		} else if (parentPath.contains(Helper.DBX_KEY_DETAILS_PATH)) {
			localFile = new File(Helper.LOCAL_KEY_DETAILS_PATH + "/" + fileName);
			changedKeyDetails = true;
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
			if (changedKeyDetails) {
				try {
					syncKeyDetails(fileName);
				} catch (IOException e) {
					LOGGER.error("Error synching <" + fileName + ">: "
							+ e.getMessage());
					throw new Exception(e.getMessage());
				}
			}
		}
	}

	private void syncKeyDetails(String fileName) throws IOException {
		List<String> downloaded = new ArrayList<String>();
		List<String> additions = new ArrayList<String>();
		List<String> deletions = new ArrayList<String>();

		BufferedReader br = null;
		String str;

		br = new BufferedReader(new FileReader(Helper.LOCAL_KEY_DETAILS_PATH
				+ "/" + fileName));
		while ((str = br.readLine()) != null)
			downloaded.add(str.trim());
		br.close();

		File histFile = new File(Helper.TEMP_DIFF_KEY_DETAILS_PATH + "/"
				+ fileName);
		if (histFile.exists()) {
			br = new BufferedReader(new FileReader(histFile));
			while ((str = br.readLine()) != null) {
				str = str.trim();
				if (str.charAt(0) == '+')
					additions.add(str.substring(1));
				else
					deletions.add(str.substring(1));
			}
			if (histFile.delete())
				LOGGER.info("Couldn't delete <" + histFile.getAbsolutePath()
						+ ">");
			br.close();

			for (String addition : additions) {
				if (!downloaded.contains(addition))
					downloaded.add(addition);
			}
			for (String deletion : deletions) {
				if (downloaded.contains(deletion))
					downloaded.remove(deletion);
			}
		}

		BufferedWriter writer = new BufferedWriter(new FileWriter(
				Helper.LOCAL_KEY_DETAILS_PATH + "/" + fileName));
		BufferedWriter writer1 = new BufferedWriter(new FileWriter(
				Helper.LOCAL_KEYWORDS_PATH + "/" + fileName));
		Pattern p = Pattern.compile(Helper.KEYWORD_DETAIL_PATTERN);
		Matcher m;
		for (String keyDetail : downloaded) {
			m = p.matcher(keyDetail);
			String key = m.group(5);
			writer.append(keyDetail);
			writer1.append(key);
		}
		writer.close();
		writer1.close();
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
