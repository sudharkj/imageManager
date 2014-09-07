/**
 * 
 */
package com.swing.imageManager.helper;

/**
 * @author Ravi
 * 
 */
public class Helper {

	public final static String CACHE_PATH = "cache";
	public final static String DETAILS_PATH = CACHE_PATH + "/details";
	public final static String ICONS_PATH = CACHE_PATH + "/icons";
	public final static String TEMP_PATH = CACHE_PATH + "/tmp";

	/**
	 * File must of the form: { "key": "YOUR_KEY", "secret": "YOUR_SECRET" }
	 */
	public final static String APP_AUTH_DETAILS_FILE_NAME = DETAILS_PATH
			+ "/appAuthDetails.txt";

	public final static String USER_AUTH_TOKEN_FILE_NAME = DETAILS_PATH
			+ "/userAuthToken.txt";
	public final static String DBX_DIR_LOC_FILE_NAME = DETAILS_PATH
			+ "/dbxDirLoc.txt";

	public final static String EDIT_ICON_PATH = ICONS_PATH + "/icon_edit.jpg";
	public final static String LOG_ICON_PATH = ICONS_PATH + "/icon_log.gif";
	public final static String REFRESH_ICON_PATH = ICONS_PATH + "/icon_refresh.png";

	public static void showMessage(String message) {
		System.out.print("\n" + message);
	}

	public static void handleError(String message) {
		System.err.println(message);
		System.exit(1);
	}

}
