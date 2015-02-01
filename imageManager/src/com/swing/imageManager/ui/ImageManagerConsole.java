package com.swing.imageManager.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.swing.imageManager.util.Constants;
import com.swing.imageManager.util.MySimpleFileVisitor;
import com.swing.imageManager.util.swingWorker.EditKeysSwingWorker;
import com.swing.imageManager.util.swingWorker.SearchingSwingWorker;
import com.swing.imageManager.util.swingWorker.TesseractSwingWorker;

@SuppressWarnings("serial")
public class ImageManagerConsole extends JComponent {

	/**
	 * Constants
	 */
	private final static Logger LOGGER;
	private final static String[] LANG;
	private final static int FILE_NAME_LABEL_HEIGHT;
	private final static int OPTIONS_PANEL_HEIGHT;
	private final static int THUMBS_PANEL_HEIGHT;

	/**
	 * imageio variables
	 */
	private Image loadedImage;
	private Image image;

	/**
	 * UI (javax.swing) variables
	 */
	private JPanel optionsPanel;
	private JSplitPane imageSplitPane;
	private JPanel imagePanel;
	private JLabel imageLabel;
	private DefaultListModel<String> keywordListModel;
	private JScrollPane thumbsScrollPane;
	private JList<ImageIcon> thumbIconList;
	private DefaultListModel<ImageIcon> thumbIconListModel;

	/**
	 * size variables
	 */
	private int consoleWidth;
	private int consoleHeight;
	private int imageSplitPaneHeight;
	private int loadedImageWidth;
	private int loadedImageHeight;
	private int imageWidth;
	private int imageHeight;
	private int dividerLocation;
	private int keywordWidth;

	/**
	 * Other global variables used by the class
	 */
	private Path LOCAL_IMAGES_PATH_PATH;
	private String searchText;
	private int selectedFileNumber;
	private List<String> fileNameList;
	private List<Rectangle> loadedRectangleList;
	private List<Rectangle> rectangleList;
	private int previousSelectedRectangleIndex;
	private List<Boolean> isSelectedRectangleList;
	private boolean isMouseDown;
	private Point topLeftPoint, bottomRightPoint;

	static {
		LOGGER = LogManager.getLogger(ImageManagerConsole.class);

		LANG = new String[2];
		LANG[0] = "eng";
		LANG[1] = "hin";

		FILE_NAME_LABEL_HEIGHT = 15;
		OPTIONS_PANEL_HEIGHT = 35;
		THUMBS_PANEL_HEIGHT = 65;
	}

	/**
	 * Create the application.
	 * 
	 * @throws IOException
	 */
	public ImageManagerConsole() throws IOException {

		initializeVariables();

		initialize();

		loadFiles();
		loadImage(0);

		isMouseDown = false;
	}

	private void initializeVariables() {

		LOCAL_IMAGES_PATH_PATH = Paths.get(Constants.LOCAL_IMAGES_PATH);
		
		fileNameList = new ArrayList<>();

		loadedRectangleList = new ArrayList<Rectangle>();
		rectangleList = new ArrayList<Rectangle>();
		isSelectedRectangleList = new ArrayList<Boolean>();

		topLeftPoint = new Point();
		bottomRightPoint = topLeftPoint;

		loadedImage = null;
		searchText = "";

		consoleWidth = 800;
		consoleHeight = 600;
		keywordWidth = 155;
		setFrameVariables();
	}

	/**
	 * setting the variable sizes
	 */
	private void setFrameVariables() {

		imageSplitPaneHeight = consoleHeight - THUMBS_PANEL_HEIGHT
				- OPTIONS_PANEL_HEIGHT;
		imageHeight = imageSplitPaneHeight - FILE_NAME_LABEL_HEIGHT - 45;

		dividerLocation = consoleWidth - keywordWidth;
		imageWidth = dividerLocation - 5;
	}

	/**
	 * Initialize the contents of the consoleFrame.
	 * 
	 * @throws IOException
	 * 
	 *             Other details: 1. use setSize() if there is no layout manager
	 *             to the parent otherwise use setPreferedSize()
	 */
	private void initialize() {
		final JFrame consoleFrame = new JFrame();
		consoleFrame.setTitle("Notice Box");
		consoleFrame.setBounds(0, 0, consoleWidth, consoleHeight);
		consoleFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		consoleFrame.setVisible(true); // for getting the graphics object
		consoleFrame.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent arg0) {
				consoleWidth = consoleFrame.getSize().width;
				consoleHeight = consoleFrame.getSize().height;

				resizeComponents(imageWidth, imageHeight);
			}
		});

		optionsPanel = new JPanel();

		JButton refreshButton = new JButton("");
		refreshButton.setIcon(new ImageIcon(Constants.REFRESH_ICON_PATH));
		refreshButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					loadFiles();

					if (!thumbIconListModel.isEmpty()) {
						loadImage(0);
						LOGGER.info(fileNameList.get(0) + " is loaded");
					}

					previousSelectedRectangleIndex = -1;

				} catch (IOException e) {

					// log for invalid file format
					LOGGER.info(e.getMessage());
				}
			}
		});
		optionsPanel.add(refreshButton);

		JLabel lblSearch = new JLabel("Search :");
		optionsPanel.add(lblSearch);

		final JTextField searchTextField = new JTextField();
		searchTextField.setColumns(10);
		searchTextField.getDocument().addDocumentListener(
				new DocumentListener() {
					public void changedUpdate(DocumentEvent e) {
						modify();
					}

					public void removeUpdate(DocumentEvent e) {
						modify();
					}

					public void insertUpdate(DocumentEvent e) {
						modify();
					}

					private void modify() {
						searchText = searchTextField.getText();
						try {
							loadFiles();
						} catch (IOException e) {
							LOGGER.info(e.getMessage()); // log
						}
					}
				});
		optionsPanel.add(searchTextField);

		JLabel languageLabel = new JLabel("Language: ");
		optionsPanel.add(languageLabel);

		final JComboBox<String> languageComboBox = new JComboBox<String>();
		languageComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
				"English", "Hindi" }));
		languageComboBox.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				
				generateMouseClickEvent();
			}
		});
		optionsPanel.add(languageComboBox);

		JButton showLog = new JButton();
		showLog.setIcon(new ImageIcon(Constants.LOG_ICON_PATH));
		showLog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				LOGGER.info("Feature yet to be implemented");
			}
		});
		optionsPanel.add(showLog);

		consoleFrame.getContentPane().add(optionsPanel, BorderLayout.NORTH);

		imageSplitPane = new JSplitPane();
		imageSplitPane.setDividerLocation(dividerLocation);
		imageSplitPane.addPropertyChangeListener(new PropertyChangeListener() {
			
			public void propertyChange(PropertyChangeEvent arg0) {
				
				final int previousImageWidth = imageWidth;
				final int previousImageHeight = imageHeight;
				int change = dividerLocation
						- imageSplitPane.getDividerLocation();
				
				// go for changes only when change is not zero
				if (((change < 0 && -change < imageWidth) || (change > 0 && change < keywordWidth))) {
					keywordWidth = keywordWidth + change;
					resizeComponents(previousImageWidth, previousImageHeight);
				}
			}
		});

		imagePanel = new JPanel();
		imagePanel.setLayout(new BorderLayout(0, 0));

		imageLabel = new JLabel("");
		imageLabel.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent arg0) {
				if (isMouseDown) {
					int pt[] = normalise();
					if (pt[2] != 0 && pt[3] != 0)
						imageLabel.getGraphics().clearRect(pt[0], pt[1], pt[2],
								pt[3]);
					bottomRightPoint = arg0.getPoint();
					paintComponent(imageLabel.getGraphics());
				}
			}
		});
		imageLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				topLeftPoint = bottomRightPoint;
				paintComponent(imageLabel.getGraphics());
				isMouseDown = false;
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				topLeftPoint = arg0.getPoint();
				bottomRightPoint = topLeftPoint;
				isMouseDown = true;
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				rectangleComplete(arg0);
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				rectangleComplete(arg0);
			}

			private void rectangleComplete(MouseEvent arg0) {
				
				if (isMouseDown && loadedImage != null) {
					
					// clear previous rectangle
					int pt[] = normalise();
					if (pt[2] != 0 && pt[3] != 0)
						imageLabel.getGraphics().clearRect(pt[0], pt[1], pt[2],
								pt[3]);
					bottomRightPoint = arg0.getPoint();
					paintComponent(imageLabel.getGraphics());
					
					// draw the new rectangle
					pt = normalise();
					if (pt[2] != 0 && pt[3] != 0) {
						rectangleList.add(new Rectangle(pt[0], pt[1], pt[2],
								pt[3]));
						isSelectedRectangleList.add(false);
						LOGGER.info("Rectangle drawn: [" + pt[0] + "," + pt[1]
								+ "," + pt[2] + "," + pt[3] + "]");
						getImageText(pt);
					}
					topLeftPoint = bottomRightPoint;
				}
				isMouseDown = false;
			}

			private void getImageText(int[] pt) {
				final int par[] = actualPoints(pt);
				loadedRectangleList.add(new Rectangle(par[0], par[1], par[2],
						par[3]));
				if (par[2] != 0 && par[3] != 0) {
					new TesseractSwingWorker(keywordListModel, (BufferedImage) loadedImage,
							fileNameList.get(selectedFileNumber),
							LANG[languageComboBox.getSelectedIndex()], par,
							rectangleList.size()).execute();
				}
			}
		});
		imagePanel.add(imageLabel, BorderLayout.CENTER);
		imageSplitPane.setLeftComponent(imagePanel);

		JPanel keywordsPanel = new JPanel();
		keywordsPanel.setBorder(BorderFactory.createTitledBorder("Keywords"));
		keywordsPanel.setBorder(BorderFactory.createBevelBorder(0));
		keywordsPanel.setLayout(new BorderLayout(0, 0));

		JLabel keywordLabel = new JLabel("Keywords");
		keywordsPanel.add(keywordLabel, BorderLayout.NORTH);

		final JList<String> keywordList = new JList<String>();
		keywordListModel = new DefaultListModel<String>();
		keywordList.setModel(keywordListModel);
		keywordList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent keyevt) {
				if (!keyevt.getValueIsAdjusting()
						&& !keywordList.isSelectionEmpty()) {
					int index = keywordList.getSelectedIndex();
					if (previousSelectedRectangleIndex != -1
							&& previousSelectedRectangleIndex < rectangleList
									.size())
						isSelectedRectangleList.set(
								previousSelectedRectangleIndex, false);
					previousSelectedRectangleIndex = index;
					isSelectedRectangleList.set(index, true);
					paintComponent(imageLabel.getGraphics());
				}
			}
		});
		JScrollPane KeyWordScrollPane = new JScrollPane(keywordList);
		keywordsPanel.add(KeyWordScrollPane, BorderLayout.CENTER);
		imageSplitPane.setRightComponent(keywordsPanel);

		JPanel buttonPanel = new JPanel();

		JButton plus = new JButton("+");
		buttonPanel.add(plus);
		plus.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String str = JOptionPane.showInputDialog(consoleFrame,
						"Enter the keyword to be added", "Add",
						JOptionPane.INFORMATION_MESSAGE);
				keywordListModel.addElement(str);
				LOGGER.info("<" + str + "> added to keywords");
				loadedRectangleList.add(new Rectangle(0, 0, 1, 1));
				rectangleList.add(new Rectangle(0, 0, 1, 1));
				isSelectedRectangleList.add(false);
				modifyKeyFiles("+:0:0:1:1:" + str);
				paintComponent(imageLabel.getGraphics());
			}
		});
		JButton edit = new JButton("");
		edit.setIcon(new ImageIcon(Constants.EDIT_ICON_PATH));
		edit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int ind = keywordList.getSelectedIndex();
				if (ind == -1) {
					JOptionPane.showMessageDialog(consoleFrame,
							"Only a selected keyword can be modified",
							"Error Occured", JOptionPane.ERROR_MESSAGE);
				} else {
					String str = JOptionPane.showInputDialog(consoleFrame,
							"Enter the new Keyword", "Edit",
							JOptionPane.INFORMATION_MESSAGE);
					String keyword = keywordListModel.get(ind);
					Rectangle rectangle = loadedRectangleList.get(ind);
					keywordListModel.remove(ind);
					keywordListModel.add(ind, str);
					LOGGER.info("<" + str + "> added to keywords");
					modifyKeyFiles("-:" + rectangle.x + ":" + rectangle.y + ":"
							+ rectangle.height + ":" + rectangle.width + ":"
							+ keyword + "+:" + rectangle.x + ":" + rectangle.y
							+ ":" + rectangle.height + ":" + rectangle.width
							+ ":" + str);
					paintComponent(imageLabel.getGraphics());
				}
			}
		});
		buttonPanel.add(edit);
		JButton minus = new JButton("-");
		minus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int ind = keywordList.getSelectedIndex();
				if (ind == -1) {
					JOptionPane.showMessageDialog(consoleFrame,
							"Only a selected keyword can be deleted",
							"Error Occured", JOptionPane.ERROR_MESSAGE);
				} else {
					String keyword = keywordListModel.remove(ind);
					Rectangle rectangle = loadedRectangleList.remove(ind);
					rectangleList.remove(ind);
					isSelectedRectangleList.remove(ind);
					LOGGER.info("<" + keyword
							+ "> removed from keywords");
					modifyKeyFiles("-:" + rectangle.x + ":" + rectangle.y + ":"
							+ rectangle.height + ":" + rectangle.width + ":"
							+ keyword);
					paintComponent(imageLabel.getGraphics());
				}
			}
		});
		buttonPanel.add(minus);

		keywordsPanel.add(buttonPanel, BorderLayout.SOUTH);
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

		consoleFrame.getContentPane().add(imageSplitPane, BorderLayout.CENTER);

		thumbIconList = new JList<ImageIcon>();
		thumbIconList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent lsevt) {
				/*
				 * 2 ListSelectionEvents are dispatched when the JList is
				 * selected—one during and another after the selection event.
				 * The isAdjusting flag is true if the user is still
				 * manipulating the selection, and false if the user has
				 * finished changing the selection. Therefore, ensure that the
				 * ListSelection value is not adjusting. This is done by the two
				 * conditions
				 */
				if (!lsevt.getValueIsAdjusting()
						&& !thumbIconList.isSelectionEmpty()) {
					try {
						loadImage(thumbIconList.getSelectedIndex());
					} catch (IOException e) {
						LOGGER.info(e.getMessage()); // log - not
						// required
					}
				}
			}

		});
		thumbIconList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		thumbIconList.setVisibleRowCount(1);
		thumbIconList.setLayoutOrientation(JList.VERTICAL_WRAP);
		thumbIconListModel = new DefaultListModel<ImageIcon>();
		thumbIconList.setModel(thumbIconListModel);
		thumbIconList.setBorder(new BevelBorder(BevelBorder.LOWERED, null,
				null, null, null));

		thumbsScrollPane = new JScrollPane(thumbIconList);
		consoleFrame.getContentPane().add(thumbsScrollPane, BorderLayout.SOUTH);

		setComponentSizes();

	}

	/**
	 * Resize the components
	 * 
	 * @param prevWidth
	 * @param prevHeight
	 */
	private void resizeComponents(final int prevWidth, final int prevHeight) {

		setFrameVariables();
		setComponentSizes();
		setImageLabel();// check this //
		// scaling the points;
		for (int i = 0; i < rectangleList.size(); ++i) {
			Rectangle rect = loadedRectangleList.get(i);
			int pt[] = new int[4];
			pt[0] = (int) rect.getX() * imageWidth / loadedImageWidth;
			pt[1] = (int) rect.getY() * imageHeight / loadedImageHeight;
			pt[2] = (int) rect.getWidth() * imageWidth / loadedImageWidth;
			pt[3] = (int) rect.getHeight() * imageHeight / loadedImageHeight;
			rect = new Rectangle(pt[0], pt[1], pt[2], pt[3]);
			rectangleList.set(i, rect);
		}
		if (topLeftPoint != null && bottomRightPoint != null) {
			topLeftPoint.x = topLeftPoint.x * imageWidth / prevWidth;
			topLeftPoint.y = topLeftPoint.y * imageHeight / prevHeight;
			bottomRightPoint.x = bottomRightPoint.x * imageWidth / prevWidth;
			bottomRightPoint.y = bottomRightPoint.y * imageHeight / prevHeight;
		}
		// paintComponent(imageLabel.getGraphics()); // this doesn't work till
		// there
		// is event occured
		// generate mouse event for drawing
		generateMouseClickEvent();
	}

	private void setComponentSizes() {
		// consoleFrame.setBounds(0, 0, consoleWidth, consoleHeight); //
		// Uncommenting it will
		// make it unable to use the minimize button
		if (optionsPanel != null)
			optionsPanel.setPreferredSize(new Dimension(consoleWidth,
					OPTIONS_PANEL_HEIGHT));
		if (imageSplitPane != null) {
			imageSplitPane.setPreferredSize(new Dimension(consoleWidth,
					imageSplitPaneHeight));
			imageSplitPane.setDividerLocation(dividerLocation);
		}
		if (imagePanel != null)
			imagePanel.setSize(dividerLocation, imageSplitPaneHeight);
		// if(thumbIconList!=null) thumbIconList.setPreferredSize(new
		// Dimension(consoleWidth-5, ThumbsHeight));
		if (thumbsScrollPane != null)
			thumbsScrollPane.setPreferredSize(new Dimension(consoleWidth,
					THUMBS_PANEL_HEIGHT));

	}

	private void setImageLabel() {
		if (loadedImage != null) {
			image = loadedImage.getScaledInstance(imageWidth, imageHeight, 0);
			imageLabel.getGraphics().drawImage(image, 0, 0, null);
			// imageLabel.setIcon(new ImageIcon(image)); // for now only use
			// drawImage(); this doesn't allow dividor location change
		}
	}

	/**
	 * Generates mouseClickEvent
	 */
	private void generateMouseClickEvent() {
		MouseEvent evt = new MouseEvent(imageLabel, // which
				MouseEvent.MOUSE_CLICKED, // what
				System.currentTimeMillis(), // when
				0, // no modifiers
				imageWidth / 2, imageHeight / 2, // where: at (0,0) on
													// imageLabel
				1, // only 1 click
				true); // not a popup trigger
		imageLabel.dispatchEvent(evt); // this is not trigerring the event,
										// better
										// to register two events
		Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(evt);
	}

	/**
	 * Load the files in the application
	 * 
	 * @throws IOException
	 */
	private void loadFiles() throws IOException {
		// clear the lists
		thumbIconListModel.clear();
		keywordListModel.clear();
		fileNameList.clear();
		imageLabel.getGraphics().clearRect(0, 0, imageWidth, imageHeight);
		imageSplitPane
				.setBorder(BorderFactory.createTitledBorder("Image Pane"));
		loadedImage = null;
		image = null;

		/*
		 * trim the text if the search text is empty load all files else load
		 * only the files containing the keyword
		 */
		searchText = searchText.trim();
		if (searchText.isEmpty()) {
			// Load the all file in the application
			Files.walkFileTree(LOCAL_IMAGES_PATH_PATH,
					new MySimpleFileVisitor(thumbIconListModel, fileNameList,
							searchText));
		} else {
			// Load selected files in the application
			new SearchingSwingWorker(thumbIconListModel, fileNameList,
					searchText).execute();
		}
	}

	private void loadImage(int index) throws IOException {
		previousSelectedRectangleIndex = -1;
		selectedFileNumber = index;
		if (fileNameList.isEmpty()) {
			imageSplitPane.setBorder(BorderFactory
					.createTitledBorder("Image Pane"));
			imageLabel.setText("Directory is empty");
		} else {
			thumbIconList.setSelectedIndex(index);
			imageLabel.setText("");
			TitledBorder title = BorderFactory.createTitledBorder(fileNameList
					.get(index)
					+ " ("
					+ (index + 1)
					+ "/"
					+ fileNameList.size() + ")");
			title.setTitleJustification(TitledBorder.CENTER);
			imageSplitPane.setBorder(title);
			
			loadedImage = ImageIO.read(new File(Constants.LOCAL_IMAGES_PATH
					+ "/" + fileNameList.get(index)));
			loadedImageWidth = loadedImage.getWidth(null);
			loadedImageHeight = loadedImage.getHeight(null);
			setImageLabel();
			keywordListModel.clear();
			loadedRectangleList.clear();
			rectangleList.clear();
			isSelectedRectangleList.clear();

			try (BufferedReader br = new BufferedReader(new FileReader(
					Constants.LOCAL_KEY_DETAILS_PATH + "/"
							+ fileNameList.get(index) + ".txt"))) {

				String curLine;
				while ((curLine = br.readLine()) != null) {
					String pattern = "(\\d*):(\\d*):(\\d*):(\\d*):(.*)";
					Pattern p = Pattern.compile(pattern);
					Matcher m = p.matcher(curLine);

					// for debugging purpose
					if (!m.find()) {/*
									 * log
									 * System.out.println("Wanted: "+pattern);
									 * System.out.println("Got: "+curLine);
									 */
						throw new IOException("Illegal format");
					}

					int pt[] = new int[4];
					pt[0] = Integer.parseInt(m.group(1));
					pt[1] = Integer.parseInt(m.group(2));
					pt[2] = Integer.parseInt(m.group(3));
					pt[3] = Integer.parseInt(m.group(4));
					loadedRectangleList.add(new Rectangle(pt[0], pt[1], pt[2],
							pt[3]));
					pt = imagePoints(pt);
					rectangleList
							.add(new Rectangle(pt[0], pt[1], pt[2], pt[3]));
					isSelectedRectangleList.add(false);
					String key = m.group(5);
					keywordListModel.addElement(key);
					// System.out.println(pr+": "+key); // log
				}
			}
			// paintComponent(imageLabel.getGraphics()); // this doesn't work
			// till
			// there is event occured
			// generate mouse event for drawing
			generateMouseClickEvent();
		}
	}

	private int[] imagePoints(int[] pt) {
		int par[] = new int[4];
		par[0] = pt[0] * imageWidth / loadedImage.getWidth(null);
		par[1] = pt[1] * imageHeight / loadedImage.getHeight(null);
		par[2] = pt[2] * imageWidth / loadedImage.getWidth(null);
		par[3] = pt[3] * imageHeight / loadedImage.getHeight(null);
		return par;
	}

	/**
	 * Returns normalized points
	 * 
	 * @return
	 */
	private int[] normalise() {
		int pt[] = new int[4];
		pt[0] = Math.min(topLeftPoint.x, bottomRightPoint.x);
		pt[1] = Math.min(topLeftPoint.y, bottomRightPoint.y);
		pt[2] = Math.max(topLeftPoint.x, bottomRightPoint.x);
		pt[3] = Math.max(topLeftPoint.y, bottomRightPoint.y);
		pt[0] = Math.max(pt[0], 0);
		pt[1] = Math.max(pt[1], 0);
		pt[2] = Math.min(pt[2], imageWidth);
		pt[3] = Math.min(pt[3], imageHeight);
		pt[2] = pt[2] - pt[0];
		pt[3] = pt[3] - pt[1];
		return pt;
	}

	private int[] actualPoints(int[] pt) {
		int par[] = new int[4];
		par[0] = pt[0] * loadedImage.getWidth(null) / imageWidth;
		par[1] = pt[1] * loadedImage.getHeight(null) / imageHeight;
		par[2] = pt[2] * loadedImage.getWidth(null) / imageWidth;
		par[3] = pt[3] * loadedImage.getHeight(null) / imageHeight;
		return par;
	}

	/*
	 * This method has no effect unless an event occurs (non-Javadoc)
	 * 
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		if (image != null) {
			Graphics2D g2d = (Graphics2D) graphics;
			g2d.drawImage(image, 0, 0, null); // always required because
												// there is a chance of
												// clearing the
												// rectangle
			if (!rectangleList.isEmpty()) {
				int i = 0;
				for (Rectangle rect : rectangleList) {
					if (isSelectedRectangleList.get(i))
						g2d.setColor(Color.GREEN);
					else
						g2d.setColor(Color.BLACK);
					g2d.drawRect((int) rect.getX(), (int) rect.getY(),
							(int) rect.getWidth(), (int) rect.getHeight());
					++i;
				}
			}
			if (topLeftPoint != bottomRightPoint) {
				int pt[] = normalise();
				g2d.setColor(Color.BLACK);
				if (pt[2] != 0 && pt[3] != 0)
					g2d.drawRect(pt[0], pt[1], pt[2], pt[3]);
			}
		}
	}

	protected void modifyKeyFiles(final String diff) {
		new EditKeysSwingWorker(loadedRectangleList, keywordListModel,
				fileNameList.get(selectedFileNumber), diff).execute();
	}

}
