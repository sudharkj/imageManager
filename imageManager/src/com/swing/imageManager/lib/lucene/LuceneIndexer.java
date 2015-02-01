package com.swing.imageManager.lib.lucene;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



public class LuceneIndexer implements Runnable {

	private final static Logger LOGGER;
	
	private static long count = 0;

	static {
		LOGGER = LogManager.getLogger(LuceneIndexer.class);
	}

	public LuceneIndexer() {
	}

	public static void indexHelper() {
		++count;
		if (count == 1)
			new Thread(new LuceneIndexer()).start();
	}

	@Override
	public void run() {
		while (count > 0) {
			--count;
			try {
				LuceneHelper.indexFiles();
			} catch (IOException e) {
				e.printStackTrace();
				LOGGER.error("Error occured while indexing: " + e.getMessage());
			}
		}
	}
}
