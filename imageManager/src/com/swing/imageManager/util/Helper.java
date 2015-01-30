/**
 * 
 */
package com.swing.imageManager.util;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.swing.imageManager.lib.model.UploadQueue;

/**
 * @author Ravi
 * 
 *         TODO rectDetails folder can be deleted
 */
public class Helper {

	public final static ScheduledExecutorService scheduler;
	public static UploadQueue UploadQueue;

	static {
		scheduler = Executors.newScheduledThreadPool(1);
		UploadQueue = new UploadQueue();
	}

	public static File getFile(String fileName) throws IOException {
		File file = new File(fileName);
		if (!file.exists()) {
			file.createNewFile();
		}
		return file;
	}
}
