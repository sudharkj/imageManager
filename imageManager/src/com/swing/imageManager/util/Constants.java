package com.swing.imageManager.util;

import com.swing.imageManager.ImageManager;

public class Constants {
	
	public final static String APPLICATION_NAME;

	public final static String CACHE_PATH;
	public final static String DETAILS_PATH;
	public final static String ICONS_PATH;
	public final static String TEMP_PATH;
	public final static String DIFF_KEYWORDS_PATH;
	public final static String TEMP_DIFF_KEY_DETAILS_PATH;

	/**
	 * File must of the form: { "key": "YOUR_KEY", "secret": "YOUR_SECRET" }
	 */
	public final static String APP_AUTH_DETAILS_FILE_NAME;

	public final static String USER_AUTH_TOKEN_FILE_NAME;
	public final static String DBX_DIR_LOC_FILE_NAME;
	public final static String DBX_DELTA_CURSOR_FILE_NAME;

	public final static String EDIT_ICON_PATH;
	public final static String LOG_ICON_PATH;
	public final static String REFRESH_ICON_PATH;

	public final static String DBX_BASE_PATH;
	public final static String DBX_IMAGES_PATH;
	public final static String DBX_INDEX_PATH;
	public final static String DBX_KEY_DETAILS_PATH;
	public final static String DBX_KEYWORDS_PATH;
	public final static String DBX_RECTANGLES_PATH;
	public final static String DBX_THUMBS_PATH;
	public final static String DBX_TIME_STAMP_PATH;
	public final static String DBX_INDEX_FILE_NAME;

	public static String LOCAL_BASE_PATH;
	public static String LOCAL_IMAGES_PATH;
	public static String LOCAL_INDEX_PATH;
	public static String LOCAL_KEY_DETAILS_PATH;
	public static String LOCAL_KEYWORDS_PATH;
	public static String LOCAL_RECTANGLES_PATH;
	public static String LOCAL_THUMBS_PATH;
	public static String LOCAL_TIME_STAMP_PATH;
	public static String LAST_SYNC_TIME_FILE_PATH;
	public static String LOCAL_INDEX_FILE_NAME;

	public static String KEYWORD_DETAIL_PATTERN;

	static {
		APPLICATION_NAME = ImageManager.class.getName();
		
		CACHE_PATH = "cache";
		DETAILS_PATH = CACHE_PATH + "/details";
		ICONS_PATH = CACHE_PATH + "/icons";
		TEMP_PATH = CACHE_PATH + "/tmp";
		DIFF_KEYWORDS_PATH = TEMP_PATH + "/keywords";
		TEMP_DIFF_KEY_DETAILS_PATH = TEMP_PATH + "/keyDetails";

		APP_AUTH_DETAILS_FILE_NAME = DETAILS_PATH + "/appAuthDetails.txt";
		USER_AUTH_TOKEN_FILE_NAME = DETAILS_PATH + "/userAuthToken.txt";
		DBX_DIR_LOC_FILE_NAME = DETAILS_PATH + "/dbxDirLoc.txt";
		DBX_DELTA_CURSOR_FILE_NAME = DETAILS_PATH + "/dbxDeltaCursor.txt";

		EDIT_ICON_PATH = ICONS_PATH + "/icon_edit.jpg";
		LOG_ICON_PATH = ICONS_PATH + "/icon_log.gif";
		REFRESH_ICON_PATH = ICONS_PATH + "/icon_refresh.png";

		DBX_BASE_PATH = "/NoticeBox";
		DBX_IMAGES_PATH = DBX_BASE_PATH + "/imageFiles";
		DBX_INDEX_PATH = DBX_BASE_PATH + "/index";
		DBX_KEY_DETAILS_PATH = DBX_BASE_PATH + "/keyDetails";
		DBX_KEYWORDS_PATH = DBX_BASE_PATH + "/keywords";
		DBX_RECTANGLES_PATH = DBX_BASE_PATH + "/rectDetails";
		DBX_THUMBS_PATH = DBX_BASE_PATH + "/thumbImages";
		DBX_TIME_STAMP_PATH = DBX_BASE_PATH + "/timestamp";
		DBX_INDEX_FILE_NAME = DBX_TIME_STAMP_PATH + "/index.txt";

		LOCAL_INDEX_PATH = CACHE_PATH + "/index";
		LOCAL_KEY_DETAILS_PATH = CACHE_PATH + "/keyDetails";
		LOCAL_KEYWORDS_PATH = CACHE_PATH + "/keywords";
		LOCAL_RECTANGLES_PATH = CACHE_PATH + "/rectDetails";
		LOCAL_TIME_STAMP_PATH = CACHE_PATH + "/timestamp";
		LAST_SYNC_TIME_FILE_PATH = LOCAL_TIME_STAMP_PATH + "/syncTime.txt";
		LOCAL_INDEX_FILE_NAME = LOCAL_TIME_STAMP_PATH + "/index.txt";

		KEYWORD_DETAIL_PATTERN = "(\\d*):(\\d*):(\\d*):(\\d*):(.*)";
	}

}
