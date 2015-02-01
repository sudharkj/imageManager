package com.swing.imageManager.lib.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UploadQueue {

	private final static Logger LOGGER;

	private List<Pair> queue;

	static {
		LOGGER = LogManager.getLogger(UploadQueue.class);
	}

	public UploadQueue() {
		queue = new ArrayList<Pair>();
	}

	public void enque(Pair pair) {
		if (!queue.contains(pair)) {
			queue.add(pair);
			LOGGER.debug(pair + " is added to uploading queue");
		}
	}

	public void deque() {
		Pair current = queue.get(0);
		queue.remove(0);
		LOGGER.debug(current + " is removed from uploading queue");
	}

	public Pair top() {
		return queue.get(0);
	}

	public int size() {
		return queue.size();
	}

}
