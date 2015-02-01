/**
 * 
 */
package com.swing.imageManager.lib.dropbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dropbox.core.DbxAppInfo;
import com.dropbox.core.DbxAuthFinish;
import com.dropbox.core.DbxAuthInfo;
import com.dropbox.core.DbxClient;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.DbxWebAuthNoRedirect;
import com.dropbox.core.json.JsonReader.FileLoadException;
import com.swing.imageManager.util.Constants;
import com.swing.imageManager.util.Helper;

/**
 * @author Ravi
 * 
 */
public class DbxHelper {

	private final static Logger LOGGER;
	
	public final static String CLASS_NAME;

	static {
		CLASS_NAME = "DbxHelper";
		LOGGER = LogManager.getLogger(DbxHelper.class);
	}

	public DbxHelper() {
		try {
			File authInfoFile = new File(Constants.USER_AUTH_TOKEN_FILE_NAME);
			if (!authInfoFile.exists() || authInfoFile.isDirectory())
				getAuthInfo();

			DbxDownloader dbxDownloader = new DbxDownloader();
			DbxUploader dbxUploader = new DbxUploader();
			Helper.scheduler.scheduleAtFixedRate(dbxDownloader, 0, 5,
					TimeUnit.MINUTES);
			Helper.scheduler.scheduleAtFixedRate(dbxUploader, 0, 5,
					TimeUnit.MINUTES);
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("Error running " + CLASS_NAME);
		}
	}

	public void getAuthInfo() throws Exception {
		DbxAppInfo appInfo = DbxAppInfo.Reader
				.readFromFile(Constants.APP_AUTH_DETAILS_FILE_NAME);

		// Run through Dropbox API authorization process
		String userLocale = Locale.getDefault().toString();
		DbxRequestConfig requestConfig = new DbxRequestConfig(CLASS_NAME,
				userLocale);
		DbxWebAuthNoRedirect webAuth = new DbxWebAuthNoRedirect(requestConfig,
				appInfo);

		String authorizeUrl = webAuth.start();
		LOGGER.info("1. Go to " + authorizeUrl
				+ "\n2. Click \"Allow\" (you might have to log in first).\n"
				+ "3. Copy the authorization code.\n"
				+ "Enter the authorization code here: ");

		String code = null;
		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				System.in))) {
			code = br.readLine();
		}
		if (code == null) {
			LOGGER.info("Obtained <code>: null");
		}
		code = code.trim();

		DbxAuthFinish authFinish = webAuth.finish(code);
		LOGGER.info("Authorization complete.\n" + "- User ID: "
				+ authFinish.userId + "\n- Access Token: "
				+ authFinish.accessToken);

		// Save auth information to output file.
		DbxAuthInfo authInfo = new DbxAuthInfo(authFinish.accessToken,
				appInfo.host);

		DbxAuthInfo.Writer.writeToFile(authInfo,
				Constants.USER_AUTH_TOKEN_FILE_NAME);
		LOGGER.info("Saved authorization information to \""
				+ Constants.USER_AUTH_TOKEN_FILE_NAME + "\".");
	}

	public static DbxClient getDbxClient() throws FileLoadException {
		// Read auth info file.
		DbxAuthInfo authInfo = DbxAuthInfo.Reader
				.readFromFile(Constants.USER_AUTH_TOKEN_FILE_NAME);

		// Create a DbxClient, which is what you use to make API calls.
		String userLocale = Locale.getDefault().toString();
		DbxRequestConfig requestConfig = new DbxRequestConfig(CLASS_NAME,
				userLocale);
		return new DbxClient(requestConfig, authInfo.accessToken, authInfo.host);
	}

}
