/**
 * 
 */
package com.swing.imageManager.helper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Locale;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuthNoRedirect;
import com.dropbox.core.json.JsonReader;

/**
 * @author Ravi
 * 
 */
public class DbxHelper {

	public final static String CLASS_NAME = "DbxHelper";
	public final static String HEADER = "In " + CLASS_NAME + ":\n";

	public DbxHelper() {
		File authInfoFile = new File(Helper.USER_AUTH_TOKEN_FILE_NAME);
		if (!authInfoFile.exists() || authInfoFile.isDirectory())
			getAuthInfo();
	}

	public void getAuthInfo() {
		DbxAppInfo appInfo = null;
		try {
			appInfo = DbxAppInfo.Reader
					.readFromFile(Helper.APP_AUTH_DETAILS_FILE_NAME);
		} catch (JsonReader.FileLoadException e) {
			Helper.handleError(HEADER + "Error reading <app-info-file>: "
					+ e.getMessage());
		}

		// Run through Dropbox API authorization process
		String userLocale = Locale.getDefault().toString();
		DbxRequestConfig requestConfig = new DbxRequestConfig(CLASS_NAME,
				userLocale);
		DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(requestConfig,
				appInfo);

		String authorizeUrl = webAuth.start();
		Helper.showMessage("1. Go to " + authorizeUrl
				+ "\n2. Click \"Allow\" (you might have to log in first).\n"
				+ "3. Copy the authorization code.\n"
				+ "Enter the authorization code here: ");

		String code = null;
		try {
			code = new BufferedReader(new InputStreamReader(System.in))
					.readLine();
		} catch (IOException e) {
			Helper.handleError(HEADER + "Error reading <code>: "
					+ e.getMessage());
		}
		if (code == null) {
			Helper.handleError(HEADER + "Obtained <code>: null");
		}
		code = code.trim();

		DbxAuthFinish authFinish = null;
		try {
			authFinish = webAuth.finish(code);
		} catch (DbxException ex) {
			Helper.handleError(HEADER + "Error in DbxWebAuth.start: "
					+ ex.getMessage());
		}

		Helper.showMessage("Authorization complete.\n" + "- User ID: "
				+ authFinish.userId + "\n- Access Token: "
				+ authFinish.accessToken);

		new DbxClient(requestConfig, authFinish.accessToken);

		// Save auth information to output file.
		DbxAuthInfo authInfo = new DbxAuthInfo(authFinish.accessToken,
				appInfo.host);

		try {
			DbxAuthInfo.Writer.writeToFile(authInfo,
					Helper.USER_AUTH_TOKEN_FILE_NAME);
			Helper.showMessage("Saved authorization information to \""
					+ Helper.USER_AUTH_TOKEN_FILE_NAME + "\".");
		} catch (IOException ex) {
			Helper.handleError(HEADER + "Error saving to <auth-file-out>: "
					+ ex.getMessage() + "\nDumping to stderr instead: "
					+ authInfo.toString());
		}
	}

}
