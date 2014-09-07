/**
 * 
 */
package com.swing.imageManager.helper;

/**
 * @author Ravi
 * 
 */
public class Helper {

	public final static String CACHE_DIR_NAME = "cache";
	public final static String DETAILS_DIR_NAME = CACHE_DIR_NAME + "/details";

	/**
	 * File must of the form: { "key": "YOUR_KEY", "secret": "YOUR_SECRET" }
	 */
	public final static String APP_AUTH_DETAILS_FILE_NAME = DETAILS_DIR_NAME
			+ "/appAuthDetails.txt";

	public final static String USER_AUTH_TOKEN_FILE_NAME = DETAILS_DIR_NAME
			+ "/userAuthToken.txt";

	public static void showMessage(String message) {
		System.out.print("\n" + message);
	}

	public static void handleError(String message) {
		System.err.println(message);
		System.exit(1);
	}

}
