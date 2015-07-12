package com.swing.imageManager.util.swingWorker;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.SwingWorker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.swing.imageManager.lib.lucene.LuceneIndexer;
import com.swing.imageManager.lib.model.Pair;
import com.swing.imageManager.util.Constants;
import com.swing.imageManager.util.Helper;

public class TesseractSwingWorker extends SwingWorker<Void, Void> {

	private final static Logger LOGGER;

	private final BufferedImage loadedImage;
	private final String imageNameText;
	private final String language;
	private final int[] actualRectangle;
	private final int bufferIndex;

	private DefaultListModel<String> keywordListModel;

	static {
		LOGGER = LogManager.getLogger(TesseractSwingWorker.class);
	}

	public TesseractSwingWorker(DefaultListModel<String> keywordListModel,
			BufferedImage loadedImage, String imageName, String language,
			int[] actualRectangle, int bufferIndex) {
		this.keywordListModel = keywordListModel;
		this.loadedImage = loadedImage;
		this.imageNameText = imageName + ".txt";
		this.language = language;
		this.actualRectangle = actualRectangle;
		this.bufferIndex = bufferIndex;
	}

	@Override
	protected Void doInBackground() throws Exception {
		BufferedImage extracted = loadedImage;
		extracted = extracted.getSubimage(actualRectangle[0],
				actualRectangle[1], actualRectangle[2], actualRectangle[3]);
		String fileName = Constants.TEMP_PATH + "/buffer" + bufferIndex;
		File fp = new File(fileName + ".png");
		ImageIO.write(extracted, "png", fp);
		Runtime.getRuntime()
				.exec("cmd /c cmd.exe /K \"tesseract " + fileName + ".png "
						+ fileName + " -l " + language + " && exit\"")
				.waitFor();
		fp.delete();

		String s, str = "";
		try (BufferedReader stdIn = new BufferedReader(new FileReader(fileName
				+ ".txt"))) {
			while ((s = stdIn.readLine()) != null)
				str = str + s + " ";
			str = str.trim();
		}
		LOGGER.info("<" + str + "> added to keywords");

		try (PrintWriter out = new PrintWriter(new BufferedWriter(
				new FileWriter(Constants.LOCAL_KEY_DETAILS_PATH + "/"
						+ imageNameText, true)));
				PrintWriter out1 = new PrintWriter(new BufferedWriter(
						new FileWriter(Constants.LOCAL_KEYWORDS_PATH + "/"
								+ imageNameText, true)));
				PrintWriter out2 = new PrintWriter(new BufferedWriter(
						new FileWriter(Constants.TEMP_DIFF_KEY_DETAILS_PATH
								+ "/" + imageNameText, true)))) {
			out.println(actualRectangle[0] + ":" + actualRectangle[1] + ":"
					+ actualRectangle[2] + ":" + actualRectangle[3] + ":" + str);
			out1.println(str + " ");
			out2.println("+:" + actualRectangle[0] + ":" + actualRectangle[1]
					+ ":" + actualRectangle[2] + ":" + actualRectangle[3] + ":"
					+ str);
			Helper.UploadQueue.enque(new Pair(Constants.LOCAL_KEY_DETAILS_PATH
					+ "/" + imageNameText, Constants.DBX_KEY_DETAILS_PATH + "/"
					+ imageNameText));
			LuceneIndexer.indexHelper();
		}
		keywordListModel.addElement(str);
		new File(fileName + ".txt").delete();
		return null;
	}

}
