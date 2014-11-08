package com.swing.imageManager.lib.lucene;



public class LuceneIndexer implements Runnable {

	private static long count = 0;

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
			LuceneHelper.indexFiles();
		}
	}
}
