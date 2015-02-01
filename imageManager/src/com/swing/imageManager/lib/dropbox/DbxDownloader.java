/**
 * 
 */
package com.swing.imageManager.lib.dropbox;

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
import com.dropbox.core.json.JsonReader.FileLoadException;
import com.swing.imageManager.lib.lucene.LuceneIndexer;
import com.swing.imageManager.lib.model.Pair;
import com.swing.imageManager.util.Constants;
import com.swing.imageManager.util.Helper;

/**
 * @author Ravi
 * 
 */
public class DbxDownloader implements Runnable {

	private final static Logger LOGGER;

	private DbxClient _dbxClient;
	private String _cursor;

	private Date LastSyncedTime;
	private Date LatestSyncingTime;

	static {
		LOGGER = LogManager.getLogger(DbxDownloader.class);
	}

	DbxDownloader() throws FileLoadException {
		LastSyncedTime = new Date();

		_dbxClient = DbxHelper.getDbxClient();
	}

	@Override
	public void run() {
		try {
			download();
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("Error downloading recent updates: " + e.getMessage());
		}
	}

	private synchronized void download() throws Exception {
		File deltaCursorFile = Helper
				.getFile(Constants.DBX_DELTA_CURSOR_FILE_NAME);
		File syncTimeFile = Helper.getFile(Constants.LAST_SYNC_TIME_FILE_PATH);

		// load cursor
		_cursor = getContent(deltaCursorFile);
		if (_cursor == null)
			_cursor = "";
		LOGGER.info("Cursor value from <" + deltaCursorFile.getAbsolutePath()
				+ ">: " + _cursor);

		// load lastSyncedTime
		String lastSyncedStrTime = getContent(syncTimeFile);
		if (lastSyncedStrTime == null || lastSyncedStrTime.isEmpty())
			LastSyncedTime.setTime(0);
		else
			LastSyncedTime.setTime(((Long) Long.parseLong(lastSyncedStrTime))
					.longValue());
		LOGGER.info("Last sync time from <" + syncTimeFile.getAbsolutePath()
				+ ">: " + LastSyncedTime);

		LatestSyncingTime = new Date();

		// sync local files
		DbxDelta<DbxEntry> deltaEntry;
		do {
			deltaEntry = _dbxClient.getDeltaWithPathPrefix(_cursor,
					Constants.DBX_BASE_PATH);
			for (DbxDelta.Entry<DbxEntry> entry : deltaEntry.entries) {
				if (entry.metadata.isFile()) {
					manageEntry(entry.metadata.asFile());
				}
			}
		} while (deltaEntry.hasMore);
		_cursor = deltaEntry.cursor;

		// save the latest cursor
		updateFile(deltaCursorFile, _cursor);

		// save the LatestSyncingTime
		updateFile(syncTimeFile,
				((Long) LatestSyncingTime.getTime()).toString());

		// TODO conditional indexing after download and upload to main server
		LuceneIndexer.indexHelper();
	}

	private String getContent(File file) throws IOException {
		String content;
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			content = br.readLine();
		}
		return content;
	}

	private void manageEntry(DbxEntry.File dbxFile) throws Exception {
		File localFile;

		String path = dbxFile.path;
		String parentPath = path.substring(0, path.lastIndexOf('/'));
		String fileName = path.substring(path.lastIndexOf('/') + 1);

		boolean useLastSyncedTime = true;
		boolean changedKeyDetails = false;

		if (parentPath.contains(Constants.DBX_IMAGES_PATH)) {
			localFile = new File(Constants.LOCAL_IMAGES_PATH + "/" + fileName);
			useLastSyncedTime = false;
		} else if (parentPath.contains(Constants.DBX_KEY_DETAILS_PATH)) {
			localFile = new File(Constants.LOCAL_KEY_DETAILS_PATH + "/"
					+ fileName);
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
			downloadFile(dbxFile, localFile);
			if (changedKeyDetails) {
				syncKeyDetails(fileName);
				Helper.UploadQueue.enque(new Pair(localFile.getAbsolutePath(),
						dbxFile.path));
			}
		}
	}

	private void syncKeyDetails(String fileName) throws IOException {
		List<String> downloaded = new ArrayList<String>();
		List<String> additions = new ArrayList<String>();
		List<String> deletions = new ArrayList<String>();

		String str;

		try (BufferedReader br = new BufferedReader(new FileReader(
				Constants.LOCAL_KEY_DETAILS_PATH + "/" + fileName))) {
			while ((str = br.readLine()) != null)
				downloaded.add(str.trim());
		}

		File histFile = new File(Constants.TEMP_DIFF_KEY_DETAILS_PATH + "/"
				+ fileName);
		if (histFile.exists()) {
			try (BufferedReader br = new BufferedReader(
					new FileReader(histFile))) {
				while ((str = br.readLine()) != null) {
					str = str.trim();
					if (str.charAt(0) == '+')
						additions.add(str.substring(2));
					else
						deletions.add(str.substring(2));
				}
			}

			for (String addition : additions) {
				if (!downloaded.contains(addition))
					downloaded.add(addition);
			}
			for (String deletion : deletions) {
				if (downloaded.contains(deletion))
					downloaded.remove(deletion);
			}
		}

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(
				Constants.LOCAL_KEY_DETAILS_PATH + "/" + fileName));
				BufferedWriter writer1 = new BufferedWriter(new FileWriter(
						Constants.LOCAL_KEYWORDS_PATH + "/" + fileName))) {
			Pattern p = Pattern.compile(Constants.KEYWORD_DETAIL_PATTERN);
			Matcher m;
			for (String keyDetail : downloaded) {
				m = p.matcher(keyDetail);

				if (!m.find()) {
					// for debugging purpose
					LOGGER.error("Wanted: " + Constants.KEYWORD_DETAIL_PATTERN);
					LOGGER.error("Got: " + keyDetail);

					throw new IOException("Illegal format");
				}

				String key = m.group(5);
				writer.append(keyDetail);
				writer1.append(key);
			}
		}
	}

	private void downloadFile(DbxEntry.File dbxFile, File localFile)
			throws Exception {
		try (FileOutputStream outputStream = new FileOutputStream(localFile)) {
			_dbxClient.getFile(dbxFile.path, null, outputStream);
		}
		LOGGER.info("Downloaded <" + dbxFile.path + ">");
	}

	private void updateFile(File file, String content) throws IOException {
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			writer.write(content);
		}
		LOGGER.info("Updated <" + file.getAbsolutePath() + "> to have <"
				+ content + ">");
	}
}
