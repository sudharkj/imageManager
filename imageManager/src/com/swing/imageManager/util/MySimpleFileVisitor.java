package com.swing.imageManager.util;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;

public class MySimpleFileVisitor extends SimpleFileVisitor<Path> {
	
	private DefaultListModel<ImageIcon> thumbIconListModel;
	private List<String> fileNameList;
	private String searchText;
	
	public MySimpleFileVisitor(DefaultListModel<ImageIcon> thumbIconListModel, List<String> fileNameList, String searchText) {
		this.thumbIconListModel = thumbIconListModel;
		this.fileNameList = fileNameList;
		this.searchText = searchText;
	}
	
	@Override
	public FileVisitResult visitFile(Path file,
			BasicFileAttributes attrs) throws IOException {
		String fileName = file.getFileName().toString();
		BufferedImage curImage;
		try {
			// add to the required variables only if the file is of
			// proper format
			// System.out.format("%s (%s) added to the ArrayList\n",fileName,file.toString());
			// // log
			curImage = ImageIO.read(new File(
					Constants.LOCAL_THUMBS_PATH + "\\" + fileName));
			thumbIconListModel.addElement(new ImageIcon(curImage));
			fileNameList.add(fileName);
			// thumbsScrollPane.setViewportView(thumbIconList); //
			// uncomment it
			// only if the scrollpane doesn't show all the images
		} catch (IIOException e) {
			addThumbImage(fileName);
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir,
			BasicFileAttributes attrs) throws IOException {
		// System.out.format("Scanning %s ...\n",dir); // log
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException e)
			throws IOException {
		if (e == null) {
			// System.out.format("Scanning complete\n"); // log
			return FileVisitResult.CONTINUE;
		} else {
			throw e;
		}
	}

	private void addThumbImage(String fileName) throws IOException {
		int index = fileName.lastIndexOf('.');
		if (index == 0)
			throw new IOException();
		Image image = ImageIO.read(
				new File(Constants.LOCAL_IMAGES_PATH + "\\" + fileName))
				.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
		BufferedImage resizedImage = new BufferedImage(40, 40,
				BufferedImage.TYPE_INT_ARGB);
		resizedImage.getGraphics().drawImage(image, 0, 0, null);
		ImageIO.write(resizedImage, fileName.substring(index + 1), new File(
				Constants.LOCAL_THUMBS_PATH + "/" + fileName));

		// this will add the items
		if (searchText.trim().isEmpty()) {
			thumbIconListModel.addElement(new ImageIcon(resizedImage));
			fileNameList.add(fileName);
		}
	}

}
