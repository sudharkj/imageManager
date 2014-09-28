/**
 * 
 */
package com.swing.imageManager.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxDelta;
import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;

/**
 * @author Ravi
 * 
 */
public class DbxDownloader implements Runnable {

	private DbxClient _dbxClient;
	private String _cursor;

	DbxDownloader() {
		_dbxClient = DbxHelper.getDbxClient();

		File deltaCursorFile = new File(Helper.DBX_DELTA_CURSOR_FILE_NAME);
		_cursor = null;
		if (deltaCursorFile.exists()) {
			try {
				BufferedReader br = new BufferedReader(new FileReader(
						deltaCursorFile));
				_cursor = br.readLine();
				br.close();
			} catch (FileNotFoundException e) {
				// e.printStackTrace(); // ignore the error
			} catch (IOException e) {
				// e.printStackTrace();
				Helper.showMessage("Unknown Exception: " + e.getMessage());
			}
		} else {
			try {
				if (deltaCursorFile.createNewFile())
					Helper.showMessage("Created <cursor-file>");
				else
					Helper.showMessage("Unable to create <cursor-file>");
			} catch (IOException e) {
				// e.printStackTrace();
				Helper.showMessage("Unable to create <cursor-file>: "
						+ e.getMessage());
			}
		}

		if (_cursor == null)
			_cursor = "";
	}

	@Override
	public void run() {
		try {
			DbxDelta<DbxEntry> deltaEntry = null;
			do {
				deltaEntry = _dbxClient.getDeltaWithPathPrefix(_cursor,
						Helper.DBX_BASE_PATH);
				for (DbxDelta.Entry<DbxEntry> entry : deltaEntry.entries) {
					manageEntry(entry.metadata.path);
				}
			} while (deltaEntry.hasMore);
		} catch (DbxException e) {
			// e.printStackTrace();
			Helper.showMessage("Unknown exception occured: " + e.getMessage());
		}
	}

	private void manageEntry(String path) {
		String parentPath = path.substring(0, path.lastIndexOf('/'));
		String fileName = path.substring(path.lastIndexOf('/') + 1);
		if (parentPath.contains(Helper.DBX_IMAGES_PATH)) {
			downloadFile(path, Helper.LOCAL_IMAGES_PATH + "/" + fileName, false);
		} else if (parentPath.contains(Helper.DBX_THUMBS_PATH)) {
			downloadFile(path, Helper.LOCAL_THUMBS_PATH + "/" + fileName, false);
		} else if (parentPath.contains(Helper.DBX_INDEX_PATH)) {
			downloadFile(path, Helper.LOCAL_INDEX_PATH + "/" + fileName, true);
		} else if (parentPath.contains(Helper.DBX_KEYWORDS_PATH)) {
			downloadFile(path, Helper.LOCAL_KEYWORDS_PATH + "/" + fileName,
					true);
		} else if (parentPath.contains(Helper.DBX_RECTANGLES_PATH)) {
			downloadFile(path, Helper.LOCAL_RECTANGLES_PATH + "/" + fileName,
					true);
		}
	}

	private void downloadFile(String from, String to, boolean fromTimeStamp) {
		long dbxTime = 0, localTime = 0;
		if (fromTimeStamp) {
			FileOutputStream outputStream = null;
			try {
				String localIndexFilePath = Helper.TEMP_PATH + "/index.txt";
				outputStream = new FileOutputStream(localIndexFilePath);
				_dbxClient.getFile(Helper.DBX_INDEX_FILE_NAME, null,
						outputStream);
				BufferedReader br = new BufferedReader(new FileReader(
						localIndexFilePath));
				dbxTime = (Long) Long.parseLong(br.readLine());
				br.close();
				br = new BufferedReader(new FileReader(
						Helper.LOCAL_INDEX_FILE_NAME));
				localTime = (Long) Long.parseLong(br.readLine());
				br.close();
			} catch (FileNotFoundException e) {
				// e.printStackTrace();
				Helper.showMessage("Unknown exception occured: "
						+ e.getMessage());
				dbxTime = localTime = 0;
			} catch (DbxException e) {
				// e.printStackTrace();
				Helper.showMessage("Internet Connection Error");
				dbxTime = localTime = 0;
			} catch (IOException e) {
				// e.printStackTrace();
				Helper.showMessage("Unknown exception occured: "
						+ e.getMessage());
				dbxTime = localTime = 0;
			} finally {
				if (outputStream != null)
					try {
						outputStream.close();
					} catch (IOException e) {
						// e.printStackTrace();
						Helper.showMessage("Unknown exception occured: "
								+ e.getMessage());
						dbxTime = localTime = 0;
					}
			}
		} else {
			try {
				dbxTime = _dbxClient.getMetadata(from).asFile().lastModified
						.getTime();
				localTime = new File(to).getAbsoluteFile().lastModified();
			} catch (DbxException e) {
				// e.printStackTrace();
				Helper.showMessage("Internet Connection Error");
				dbxTime = localTime = 0;
			}
		}

		if (localTime < dbxTime) {
			FileOutputStream outputStream = null;
			try {
				outputStream = new FileOutputStream(to);
				_dbxClient.getFile(from, null, outputStream);
			} catch (FileNotFoundException e) {
				// e.printStackTrace();
				Helper.showMessage("Unknown exception occured: "
						+ e.getMessage());
			} catch (DbxException e) {
				// e.printStackTrace();
				Helper.showMessage("Internet Connection Error");
			} catch (IOException e) {
				// e.printStackTrace();
				Helper.showMessage("Unknown exception occured: "
						+ e.getMessage());
			} finally {
				if (outputStream != null)
					try {
						outputStream.close();
					} catch (IOException e) {
						// e.printStackTrace();
						Helper.showMessage("Unknown exception occured: "
								+ e.getMessage());
					}
			}
		}
	}

}
