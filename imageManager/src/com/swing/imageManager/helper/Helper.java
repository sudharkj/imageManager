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
	public final static String DIFF_KEYWORDS_PATH = TEMP_PATH + "/keywords";

	/**
	 * File must of the form: { "key": "YOUR_KEY", "secret": "YOUR_SECRET" }
	 */
	public final static String APP_AUTH_DETAILS_FILE_NAME = DETAILS_PATH
			+ "/appAuthDetails.txt";

	public final static String USER_AUTH_TOKEN_FILE_NAME = DETAILS_PATH
			+ "/userAuthToken.txt";
	public final static String DBX_DIR_LOC_FILE_NAME = DETAILS_PATH
			+ "/dbxDirLoc.txt";
	public final static String DBX_DELTA_CURSOR_FILE_NAME = DETAILS_PATH
			+ "/dbxDeltaCursor.txt";

	public final static String EDIT_ICON_PATH = ICONS_PATH + "/icon_edit.jpg";
	public final static String LOG_ICON_PATH = ICONS_PATH + "/icon_log.gif";
	public final static String REFRESH_ICON_PATH = ICONS_PATH
			+ "/icon_refresh.png";

	public final static String DBX_BASE_PATH = "/NoticeBox";
	public final static String DBX_IMAGES_PATH = DBX_BASE_PATH + "/imageFiles";
	public final static String DBX_INDEX_PATH = DBX_BASE_PATH + "/index";
	public final static String DBX_KEYWORDS_PATH = DBX_BASE_PATH + "/keywords";
	public final static String DBX_RECTANGLES_PATH = DBX_BASE_PATH
			+ "/rectDetails";
	public final static String DBX_THUMBS_PATH = DBX_BASE_PATH + "/thumbImages";
	public final static String DBX_TIME_STAMP_PATH = DBX_BASE_PATH
			+ "/timestamp";
	public final static String DBX_INDEX_FILE_NAME = DBX_TIME_STAMP_PATH
			+ "/index.txt";

	public static String LOCAL_BASE_PATH;
	public static String LOCAL_IMAGES_PATH;
	public static String LOCAL_INDEX_PATH = CACHE_PATH + "/index";
	public static String LOCAL_KEYWORDS_PATH = CACHE_PATH + "/keywords";
	public static String LOCAL_RECTANGLES_PATH = CACHE_PATH + "/rectDetails";
	public static String LOCAL_THUMBS_PATH;
	public static String LOCAL_TIME_STAMP_PATH = CACHE_PATH + "/timestamp";
	public static String LAST_SYNC_TIME_FILE_PATH = LOCAL_TIME_STAMP_PATH + "/syncTime.txt";
	public static String LOCAL_INDEX_FILE_NAME = LOCAL_TIME_STAMP_PATH
			+ "/index.txt";

}
