package com.swing.imageManager.lib.lucene;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.swing.imageManager.globals.Constants;

public class LuceneHelper {

	final static Logger LOGGER = LogManager.getLogger(LuceneHelper.class);
	
	private static String field = "contents";

	/**
	 * Function initializes the required variables before indexing
	 */
	public static void indexFiles() {
		String docsPath = Constants.LOCAL_KEYWORDS_PATH;
		boolean create;
		final File indDir = new File(Constants.LOCAL_INDEX_PATH);
		if (!indDir.exists())
			create = true;
		else
			create = false;

		final File docDir = new File(docsPath);
		/*
		 * mostly this will not occur unless there are no access permissions if
		 * (!docDir.exists() || !docDir.canRead()) {
		 * System.out.println("Document directory '" +docDir.getAbsolutePath()+
		 * "' does not exist or is not readable, please check the path");
		 * System.exit(1); }
		 */

		try {
			Directory dir = FSDirectory.open(new File(Constants.LOCAL_INDEX_PATH));
			Analyzer analyzer = new StandardAnalyzer(
					Version.parseLeniently("4.0"));
			IndexWriterConfig iwc = new IndexWriterConfig(
					Version.parseLeniently("4.0"), analyzer);

			if (create) {
				// Create a new index in the directory, removing any
				// previously indexed documents:
				iwc.setOpenMode(OpenMode.CREATE);
			} else {
				// Add new documents to an existing index:
				iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
			}

			// Optional: for better indexing performance, if you
			// are indexing many documents, increase the RAM
			// buffer. But if you do this, increase the max heap
			// size to the JVM (eg add -Xmx512m or -Xmx1g):
			//
			// iwc.setRAMBufferSizeMB(256.0);

			IndexWriter writer = new IndexWriter(dir, iwc);
			indexDocs(writer, docDir);

			// NOTE: if you want to maximize search performance,
			// you can optionally call forceMerge here. This can be
			// a terribly costly operation, so generally it's only
			// worth it when your index is relatively static (ie
			// you're done adding documents to it):
			//
			// writer.forceMerge(1);

			writer.close();

		} catch (IOException e) {
			LOGGER.info(e.getMessage()); // log
		}
	}

	/**
	 * Function to index the files
	 * 
	 * @param writer
	 * @param file
	 */
	private static void indexDocs(IndexWriter writer, File file) {
		// do not try to index files that cannot be read
		if (file.canRead()) {
			if (file.isDirectory()) {
				String[] files = file.list();
				// an IO error could occur
				if (files != null) {
					for (int i = 0; i < files.length; i++) {
						indexDocs(writer, new File(file, files[i]));
					}
				}
			} else {
				FileInputStream fis;
				try {
					fis = new FileInputStream(file);
				} catch (FileNotFoundException fnfe) {
					LOGGER.info(fnfe.getMessage());
					// at least on windows, some temporary files raise this
					// exception with an "access denied" message
					// checking if the file can be read doesn't help
					return;
				}

				try {
					// make a new, empty document
					Document doc = new Document();

					// Add the path of the file as a field named "path". Use a
					// field that is indexed (i.e. searchable), but don't
					// tokenize
					// the field into separate words and don't index term
					// frequency
					// or positional information:
					Field pathField = new StringField("path", file.getPath(),
							Field.Store.YES);
					doc.add(pathField);

					// Add the last modified date of the file a field named
					// "modified".
					// Use a LongField that is indexed (i.e. efficiently
					// filterable with
					// NumericRangeFilter). This indexes to milli-second
					// resolution, which
					// is often too fine. You could instead create a number
					// based on
					// year/month/day/hour/minutes/seconds, down the resolution
					// you require.
					// For example the long value 2011021714 would mean
					// February 17, 2011, 2-3 PM.
					doc.add(new LongField("modified", file.lastModified(),
							Field.Store.NO));

					// Add the contents of the file to a field named "contents".
					// Specify a Reader,
					// so that the text of the file is tokenized and indexed,
					// but not stored.
					// Note that FileReader expects the file to be in UTF-8
					// encoding.
					// If that's not the case searching for special characters
					// will fail.
					doc.add(new TextField("contents", new BufferedReader(
							new InputStreamReader(fis, "UTF-8"))));

					if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
						// New index, so we just add the document (no old
						// document can be there):
						// System.out.println("adding " + file); // log
						writer.addDocument(doc);
					} else {
						// Existing index (an old copy of this document may have
						// been indexed) so
						// we use updateDocument instead to replace the old one
						// matching the exact
						// path, if present:
						// System.out.println("updating " + file); // log
						writer.updateDocument(new Term("path", file.getPath()),
								doc);
					}

				} catch (IOException e) {
					LOGGER.info(e.getMessage()); // log
				} finally {
					try {
						fis.close();
					} catch (IOException e) {
						LOGGER.info(e.getMessage()); // log
					}
				}
			}
		}

	}
	
	public static List<String> doSearch(String searchText) throws IOException, ParseException {
		IndexReader reader = DirectoryReader.open(FSDirectory
				.open(new File(Constants.LOCAL_INDEX_PATH)));
		IndexSearcher searcher = new IndexSearcher(reader);
		Analyzer analyzer = new StandardAnalyzer(
				Version.parseLeniently("4.0"));

		QueryParser parser = new QueryParser(
				Version.parseLeniently("4.0"), field, analyzer);
		String line = searchText;

		Query query = parser.parse(line);
		// System.out.println("Searching for: " +
		// query.toString(field)); // log
		
		List<String> fileNames = doPagingSearch(new BufferedReader(new InputStreamReader(
				System.in, "UTF-8")), searcher, query, 10, false, true);

		reader.close();
		
		return fileNames;
	}

	private static List<String> doPagingSearch(BufferedReader in,
			IndexSearcher searcher, Query query, int hitsPerPage,
			boolean raw, boolean interactive) {
		TopDocs results;
		List<String> fileNames = new ArrayList<String>();
		try {
			results = searcher.search(query, 5 * hitsPerPage);
			ScoreDoc[] hits = results.scoreDocs;

			int numTotalHits = results.totalHits;
			// System.out.println(numTotalHits
			// + " total matching documents"); // log

			int start = 0;
			int end = Math.min(numTotalHits, hitsPerPage);

			while (start < end) {
				end = Math.min(hits.length, start + hitsPerPage);

				for (int i = start; i < end; i++) {
					Document doc = searcher.doc(hits[i].doc);
					String path = doc.get("path");
					if (path != null) {
						// System.out.println((i+1) + ". " + path); //
						// log
						String file = path.substring(
								path.lastIndexOf('\\') + 1,
								path.lastIndexOf('.'));// System.out.println(new
														// File(ThumbsPath+'\\'+file).getAbsolutePath());
														// // log
						fileNames.add(file);
					} else {
						throw new IOException("No defined path");
					}

				}

				if (end == 0) {
					break;
				}

				if (numTotalHits > end) {
					if (start + hitsPerPage < numTotalHits)
						start += hitsPerPage;
					end = Math.min(numTotalHits, start + hitsPerPage);
				} else
					break;
			}
		} catch (Exception e) {
			LOGGER.info(e.getMessage()); // log
		}
		return fileNames;
	}

}
