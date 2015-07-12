package com.swing.imageManager.util.swingWorker;

import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.SwingWorker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.swing.imageManager.lib.lucene.LuceneHelper;
import com.swing.imageManager.util.Constants;

public class SearchingSwingWorker extends SwingWorker<Void, List<String>> {

	private final static Logger LOGGER;

	private DefaultListModel<ImageIcon> thumbIconListModel;
	private List<String> fileNameList;
	private String searchText;

	static {
		LOGGER = LogManager.getLogger(SearchingSwingWorker.class);
	}

	public SearchingSwingWorker(DefaultListModel<ImageIcon> thumbIconListModel,
			List<String> fileNameList, String searchText) {
		this.thumbIconListModel = thumbIconListModel;
		this.fileNameList = fileNameList;
		this.searchText = searchText;
	}

	@Override
	protected Void doInBackground() throws Exception {

		publish(LuceneHelper.doSearch(searchText));

		return null;
	}

	@Override
	protected void process(List<List<String>> arg0) {
		fileNameList.clear();
		thumbIconListModel.clear();
		for (String file : arg0.get(0)) {
			fileNameList.add(file);
			thumbIconListModel.addElement(new ImageIcon(
					Constants.LOCAL_THUMBS_PATH + '\\' + file));
		}
	}

	@Override
	protected void done() {
		LOGGER.debug("Searching Complete");
	}

}
