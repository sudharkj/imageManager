package com.swing.imageManager.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DbxUploadQueue {

	private final static Logger LOGGER = LogManager
			.getLogger(DbxUploadQueue.class);

	private List<Pair> UploadQueue;

	public DbxUploadQueue() {
		UploadQueue = new ArrayList<Pair>();
		Helper.scheduler.scheduleAtFixedRate(new DbxUploader(), 0, 5,
				TimeUnit.MINUTES);
	}

	public void add(Pair pair) {
		if (!UploadQueue.contains(pair)) {
			UploadQueue.add(pair);
			LOGGER.debug(pair + " is added to uploading queue");
		}
	}

	public void remove(int index) {
		Pair current = get(index);
		UploadQueue.remove(index);
		LOGGER.debug(current + " is removed from uploading queue");
	}

	public Pair get(int index) {
		return UploadQueue.get(index);
	}

	public int size() {
		return UploadQueue.size();
	}

}
