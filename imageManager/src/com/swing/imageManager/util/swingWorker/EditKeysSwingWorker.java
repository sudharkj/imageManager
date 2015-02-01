package com.swing.imageManager.util.swingWorker;

import java.awt.Rectangle;
import java.io.PrintWriter;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.SwingWorker;

import com.swing.imageManager.lib.lucene.LuceneIndexer;
import com.swing.imageManager.lib.model.Pair;
import com.swing.imageManager.util.Constants;
import com.swing.imageManager.util.Helper;

public class EditKeysSwingWorker extends SwingWorker<Void, Void> {

	private final String imageNameText;
	private final String diff;

	private List<Rectangle> loadedRectangleList;
	private DefaultListModel<String> keywordListModel;

	public EditKeysSwingWorker(List<Rectangle> loadedRectangleList,
			DefaultListModel<String> keywordListModel, String imageName,
			String diff) {
		this.loadedRectangleList = loadedRectangleList;
		this.keywordListModel = keywordListModel;
		this.imageNameText = imageName + ".txt";
		this.diff = diff;
	}

	@Override
	protected Void doInBackground() throws Exception {
		try (PrintWriter out = new PrintWriter(Constants.LOCAL_KEY_DETAILS_PATH
				+ "/" + imageNameText);
				PrintWriter out1 = new PrintWriter(
						Constants.LOCAL_KEYWORDS_PATH + "/" + imageNameText);
				PrintWriter out2 = new PrintWriter(
						Constants.TEMP_DIFF_KEY_DETAILS_PATH + "/"
								+ imageNameText)) {
			for (int i = 0; i < loadedRectangleList.size(); ++i) {
				Rectangle rect = loadedRectangleList.get(i);
				out.println(rect.x + ":" + rect.y + ":" + rect.width + ":"
						+ rect.height + ":" + keywordListModel.elementAt(i));
				out1.println(keywordListModel.elementAt(i) + " ");
			}
			out2.println(diff);

			Helper.UploadQueue.enque(new Pair(Constants.LOCAL_KEY_DETAILS_PATH
					+ "/" + imageNameText, Constants.DBX_KEY_DETAILS_PATH + "/"
					+ imageNameText));
			LuceneIndexer.indexHelper();
		}
		return null;
	}

}
