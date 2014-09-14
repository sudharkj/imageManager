package com.swing.imageManager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.IIOException;
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
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.swing.imageManager.helper.DbxHelper;
import com.swing.imageManager.helper.Helper;

@SuppressWarnings("serial")
public class ImageManagerConsole extends JComponent {

	private final static String CLASS_NAME = "ImageManagerConsole";
	private final static String HEADER = "In " + CLASS_NAME + ":\n";

	private static String BasePath;
	private static String ImagePath;
	private static String ThumbsPath;
	private static String DocsPath = Helper.CACHE_PATH + "/keywords";
	private static String RectPath = Helper.CACHE_PATH + "/recDetails";
	private static String IndexPath = Helper.CACHE_PATH + "/index";

	private static ArrayList<String> FileNames;
	private String lang[] = { "eng", "hin" };
	private int FileNumber;
	private ArrayList<Rectangle> ImageRectangles;
	private ArrayList<Rectangle> Rectangles;
	private ArrayList<Boolean> Different;
	private boolean MouseDown;
	private Point StartPoint, EndPoint;
	private String SearchText;
	private int prevSelectedInd;

	private String field = "contents";

	// size variables
	private final int LabelHeight = 15;
	private int AppWidth;
	private int AppHeight;
	private final int SearchPanelHeight = 35;
	private int SplitPaneHeight;
	private int KeywordWidth;
	private int DividerLocation;
	private int ActualImageWidth;
	private int ActualImageHeight;
	private int ImageWidth;
	private int ImageHeight;
	private final int ThumbsPaneHeight = 65;
	private JFrame frame;
	private JLabel imgLbl;
	private DefaultListModel<String> keyList;
	private JList<ImageIcon> thumbsList;
	private DefaultListModel<ImageIcon> thumbscon;
	private JScrollPane scrollPane;
	private Image ActualImage;
	private Image ResizedImage;
	private JTextField searchTextField;
	private JPanel searchPanel;
	private JSplitPane imgPane;
	private JPanel imgPnl;
	private JPanel keywrdPanel;
	private JLabel keywrdsLbl;
	private JList<String> keywrdList;
	private JButton btnRefresh;
	private JLabel lblLanguage;
	private JComboBox<String> LanguageComboBox;

	/**
	 * Launch the application.
	 * 
	 * @throws IOException
	 */
	public static void main(String[] args) {
		new DbxHelper();

		try {
			BufferedReader br = new BufferedReader(new FileReader(
					Helper.DBX_DIR_LOC_FILE_NAME));
			BasePath = br.readLine();
			if (BasePath.trim().isEmpty()) {
				br.close();
				Helper.handleError(HEADER + "Dropbox location not set in "
						+ Helper.DBX_DIR_LOC_FILE_NAME);
			}
			Helper.showMessage("Folder location: " + BasePath);
			br.close();
		} catch (FileNotFoundException ex) {
			Helper.handleError(HEADER + "File not found <dbx-dir-loc>: "
					+ ex.getMessage());
		} catch (IOException e) {
			Helper.handleError(HEADER + "Error reading <dbx-dir-loc>: "
					+ e.getMessage());
		}

		try {
			UIManager
					.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (Throwable e) {
			Helper.handleError(HEADER + e.getMessage());
		}

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ImageManagerConsole window = new ImageManagerConsole();
					window.frame.setVisible(true);
				} catch (Exception e) {
					Helper.handleError(HEADER + e.getMessage());
				}
			}
		});
	}

	/**
	 * Create the application.
	 * 
	 * @throws IOException
	 */
	public ImageManagerConsole() throws IOException {
		ImagePath = BasePath + "/imageFiles";
		ThumbsPath = BasePath + "/thumbImages";

		FileNames = new ArrayList<>();
		ImageRectangles = new ArrayList<Rectangle>();
		Rectangles = new ArrayList<Rectangle>();
		Different = new ArrayList<Boolean>();
		StartPoint = new Point();
		EndPoint = StartPoint;
		ActualImage = null;
		SearchText = "";

		AppWidth = 800;
		AppHeight = 600;
		KeywordWidth = 155;

		initialize();

		loadFiles(Paths.get(ImagePath));
		loadImage(0);

		MouseDown = false;
	}

	/**
	 * Initialize the contents of the frame.
	 * 
	 * @throws IOException
	 * 
	 *             Other details: 1. use setSize() if there is no layout manager
	 *             to the parent otherwise use setPreferedSize()
	 */
	private void initialize() {
		setFrameVariables();
		frame = new JFrame();
		frame.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent arg0) {
				final int prevWidth = ImageWidth;
				final int prevHeight = ImageHeight;
				AppWidth = frame.getSize().width;
				AppHeight = frame.getSize().height;

				resizeComponents(prevWidth, prevHeight);
			}
		});
		frame.setTitle("Notice Box");
		frame.setBounds(0, 0, AppWidth, AppHeight);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true); // for getting the graphics object

		searchPanel = new JPanel();

		btnRefresh = new JButton("");
		btnRefresh.setIcon(new ImageIcon(Helper.REFRESH_ICON_PATH));
		btnRefresh.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					loadFiles(Paths.get(ImagePath));
					if (!thumbscon.isEmpty())
						loadImage(0);
					prevSelectedInd = -1;
				} catch (IOException e) {
					// System.out.println(e.toString()); //log for invalid file
					// format
				}
			}
		});
		searchPanel.add(btnRefresh);

		JLabel lblSearch = new JLabel("Search :");
		searchPanel.add(lblSearch);

		searchTextField = new JTextField();
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
						SearchText = searchTextField.getText();
						try {
							loadFiles(Paths.get(ImagePath));
						} catch (IOException e) {
							// System.out.println(e.toString()); // log
						}
					}
				});
		searchPanel.add(searchTextField);
		searchTextField.setColumns(10);

		lblLanguage = new JLabel("Language: ");
		searchPanel.add(lblLanguage);

		LanguageComboBox = new JComboBox<String>();
		LanguageComboBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				generateMouseEvent();
			}
		});
		LanguageComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
				"English", "Hindi" }));
		searchPanel.add(LanguageComboBox);

		JButton showLog = new JButton();
		showLog.setIcon(new ImageIcon(Helper.LOG_ICON_PATH));
		showLog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.out.println("Show Log button is clicked");
			}
		});
		searchPanel.add(showLog);

		frame.getContentPane().add(searchPanel, BorderLayout.NORTH);

		imgPane = new JSplitPane();
		imgPane.addPropertyChangeListener(new PropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent arg0) {
				final int prevWidth = ImageWidth;
				final int prevHeight = ImageHeight;
				int change = DividerLocation - imgPane.getDividerLocation();
				// go for changes only when change is not zero
				if (((change < 0 && -change < ImageWidth) || (change > 0 && change < KeywordWidth))) {
					KeywordWidth = KeywordWidth + change;
					resizeComponents(prevWidth, prevHeight);
				}
			}
		});
		imgPane.setDividerLocation(DividerLocation);

		imgPnl = new JPanel();
		imgPnl.setLayout(new BorderLayout(0, 0));

		imgLbl = new JLabel("");
		imgLbl.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseDragged(MouseEvent arg0) {
				if (MouseDown) {
					int pt[] = normalise();
					if (pt[2] != 0 && pt[3] != 0)
						imgLbl.getGraphics().clearRect(pt[0], pt[1], pt[2],
								pt[3]);
					EndPoint = arg0.getPoint();
					paintComponent(imgLbl.getGraphics());
				}
			}
		});
		imgLbl.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				StartPoint = EndPoint;
				paintComponent(imgLbl.getGraphics());
				MouseDown = false;
			}

			@Override
			public void mousePressed(MouseEvent arg0) {
				StartPoint = arg0.getPoint();
				EndPoint = StartPoint;
				MouseDown = true;
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
				if (MouseDown && ActualImage != null) {
					int pt[] = normalise();
					if (pt[2] != 0 && pt[3] != 0)
						imgLbl.getGraphics().clearRect(pt[0], pt[1], pt[2],
								pt[3]);
					EndPoint = arg0.getPoint();
					paintComponent(imgLbl.getGraphics());
					pt = normalise();
					if (pt[2] != 0 && pt[3] != 0) {
						Rectangles
								.add(new Rectangle(pt[0], pt[1], pt[2], pt[3]));
						Different.add(false);
						getImageText(pt);
					}
					StartPoint = EndPoint;
				}
				MouseDown = false;
			}

			private void getImageText(int[] pt) {
				final int par[] = actualPoints(pt);
				ImageRectangles.add(new Rectangle(par[0], par[1], par[2],
						par[3]));
				if (par[2] != 0 && par[3] != 0) {
					SwingWorker<Void, Void> tesseract = new SwingWorker<Void, Void>() {
						@Override
						protected Void doInBackground() throws Exception {
							BufferedImage extracted = (BufferedImage) ActualImage;
							extracted = extracted.getSubimage(par[0], par[1],
									par[2], par[3]);
							String fileName = Helper.TEMP_PATH + "/buffer"
									+ Rectangles.size();
							File fp = new File(fileName + ".png");
							ImageIO.write(extracted, "png", fp);
							Runtime.getRuntime()
									.exec("cmd /c cmd.exe /K \"tesseract "
											+ fileName
											+ ".png "
											+ fileName
											+ " -l "
											+ lang[LanguageComboBox
													.getSelectedIndex()]
											+ " && exit\"").waitFor();
							fp.delete();
							@SuppressWarnings("resource")
							BufferedReader stdIn = new BufferedReader(
									new FileReader(fileName + ".txt"));
							String s, str = "";
							while ((s = stdIn.readLine()) != null)
								str = str + s + " ";
							str = str.trim();
							try (PrintWriter out = new PrintWriter(
									new BufferedWriter(new FileWriter(RectPath
											+ "/" + FileNames.get(FileNumber)
											+ ".txt", true)));
									PrintWriter out1 = new PrintWriter(
											new BufferedWriter(
													new FileWriter(
															DocsPath
																	+ "/"
																	+ FileNames
																			.get(FileNumber)
																	+ ".txt",
															true)))) {
								out.println(par[0] + ":" + par[1] + ":"
										+ par[2] + ":" + par[3] + ":" + str);
								out1.println(str + " ");
								// System.out.println("on line 491: " + par[0]
								// + ":" + par[1] + ":" + par[2] + ":"
								// + par[3] + ":" + str); // log
								out.close();
								out1.close();
								indexFiles();
							} catch (IOException e) {
								System.out.println(e.toString());// check the
																	// exceptions
							}
							keyList.addElement(str);
							fp = new File(fileName + ".txt");
							fp.delete();
							return null;
						}
					};
					tesseract.execute();
				}
			}
		});
		imgPnl.add(imgLbl, BorderLayout.CENTER);
		imgPane.setLeftComponent(imgPnl);

		keywrdPanel = new JPanel();
		keywrdPanel.setBorder(BorderFactory.createTitledBorder("Keywords"));
		keywrdPanel.setBorder(BorderFactory.createBevelBorder(0));
		keywrdPanel.setLayout(new BorderLayout(0, 0));

		keywrdsLbl = new JLabel("Keywords");
		keywrdPanel.add(keywrdsLbl, BorderLayout.NORTH);

		keywrdList = new JList<String>();
		keyList = new DefaultListModel<String>();
		keywrdList.setModel(keyList);
		keywrdList.addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent keyevt) {
				if (!keyevt.getValueIsAdjusting()
						&& !keywrdList.isSelectionEmpty()) {
					int index = keywrdList.getSelectedIndex();
					if (prevSelectedInd != -1
							&& prevSelectedInd < Rectangles.size())
						Different.set(prevSelectedInd, false);
					prevSelectedInd = index;
					Different.set(index, true);
					paintComponent(imgLbl.getGraphics());
				}
			}
		});
		JScrollPane KeyWordScrollPane = new JScrollPane(keywrdList);
		keywrdPanel.add(KeyWordScrollPane, BorderLayout.CENTER);
		imgPane.setRightComponent(keywrdPanel);

		JPanel buttonPanel = new JPanel();

		JButton plus = new JButton("+");
		buttonPanel.add(plus);
		plus.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String str = JOptionPane.showInputDialog(frame,
						"Enter the keyword to be added", "Add",
						JOptionPane.INFORMATION_MESSAGE);
				keyList.addElement(str);
				ImageRectangles.add(new Rectangle(0, 0, 1, 1));
				Rectangles.add(new Rectangle(0, 0, 1, 1));
				Different.add(false);
				modifyKeyFiles();
				paintComponent(imgLbl.getGraphics());
			}
		});
		JButton edit = new JButton("");
		edit.setIcon(new ImageIcon(Helper.EDIT_ICON_PATH));
		edit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int ind = keywrdList.getSelectedIndex();
				if (ind == -1) {
					JOptionPane.showMessageDialog(frame,
							"Only a selected keyword can be modified",
							"Error Occured", JOptionPane.ERROR_MESSAGE);
				} else {
					String str = JOptionPane.showInputDialog(frame,
							"Enter the new Keyword", "Edit",
							JOptionPane.INFORMATION_MESSAGE);
					keyList.remove(ind);
					keyList.add(ind, str);
					modifyKeyFiles();
					paintComponent(imgLbl.getGraphics());
				}
			}
		});
		buttonPanel.add(edit);
		JButton minus = new JButton("-");
		minus.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int ind = keywrdList.getSelectedIndex();
				if (ind == -1) {
					JOptionPane.showMessageDialog(frame,
							"Only a selected keyword can be deleted",
							"Error Occured", JOptionPane.ERROR_MESSAGE);
				} else {
					keyList.remove(ind);
					ImageRectangles.remove(ind);
					Rectangles.remove(ind);
					Different.remove(ind);
					modifyKeyFiles();
					paintComponent(imgLbl.getGraphics());
				}
			}
		});
		buttonPanel.add(minus);

		keywrdPanel.add(buttonPanel, BorderLayout.SOUTH);
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

		frame.getContentPane().add(imgPane, BorderLayout.CENTER);

		thumbsList = new JList<ImageIcon>();
		thumbsList.addListSelectionListener(new ListSelectionListener() {
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
						&& !thumbsList.isSelectionEmpty()) {
					try {
						loadImage(thumbsList.getSelectedIndex());
					} catch (IOException e) {
						// System.out.println(e.toString()); // log - not
						// required
					}
				}
			}

		});
		thumbsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		thumbsList.setVisibleRowCount(1);
		thumbsList.setLayoutOrientation(JList.VERTICAL_WRAP);
		thumbscon = new DefaultListModel<ImageIcon>();
		thumbsList.setModel(thumbscon);
		thumbsList.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null,
				null, null));

		scrollPane = new JScrollPane(thumbsList);
		frame.getContentPane().add(scrollPane, BorderLayout.SOUTH);

		setComponentSizes();

	}

	protected void modifyKeyFiles() {
		SwingWorker<Void, Void> editFiles = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				try {
					PrintWriter out = new PrintWriter(RectPath + "/"
							+ FileNames.get(FileNumber) + ".txt");
					PrintWriter out1 = new PrintWriter(DocsPath + "/"
							+ FileNames.get(FileNumber) + ".txt");
					for (int i = 0; i < ImageRectangles.size(); ++i) {
						Rectangle rect = ImageRectangles.get(i);
						out.println(rect.x + ":" + rect.y + ":" + rect.width
								+ ":" + rect.height + ":"
								+ keyList.elementAt(i));
						out1.println(keyList.elementAt(i) + " ");
					}
					out.close();
					out1.close();
					indexFiles();
				} catch (IOException e) {
					System.out.println(e.toString());// check the
														// exceptions
				}
				return null;
			}
		};
		editFiles.execute();
	}

	/**
	 * Function initializes the required variables before indexing
	 */
	private void indexFiles() {
		String docsPath = DocsPath;
		boolean create;
		final File indDir = new File(IndexPath);
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
			Directory dir = FSDirectory.open(new File(IndexPath));
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
			// System.out.println(" caught a " + e.getClass() +
			// "\n with message: " + e.getMessage()); // log
		}
	}

	/**
	 * Function to index the files
	 * 
	 * @param writer
	 * @param file
	 */
	private void indexDocs(IndexWriter writer, File file) {
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
					// System.out.println(e.toString()); // log
				} finally {
					try {
						fis.close();
					} catch (IOException e) {
						// System.out.println(e.toString()); // log
					}
				}
			}
		}

	}

	/**
	 * setting the variable sizes
	 */
	private void setFrameVariables() {
		DividerLocation = AppWidth - KeywordWidth;
		SplitPaneHeight = AppHeight - ThumbsPaneHeight - SearchPanelHeight;
		ImageWidth = DividerLocation - 5;
		ImageHeight = SplitPaneHeight - LabelHeight - 45;
	}

	/**
	 * Resize the components
	 * 
	 * @param prevWidth
	 * @param prevHeight
	 */
	protected void resizeComponents(final int prevWidth, final int prevHeight) {

		setFrameVariables();
		setComponentSizes();
		setImageLabel();// check this //
		// scaling the points;
		for (int i = 0; i < Rectangles.size(); ++i) {
			Rectangle rect = ImageRectangles.get(i);
			int pt[] = new int[4];
			pt[0] = (int) rect.getX() * ImageWidth / ActualImageWidth;
			pt[1] = (int) rect.getY() * ImageHeight / ActualImageHeight;
			pt[2] = (int) rect.getWidth() * ImageWidth / ActualImageWidth;
			pt[3] = (int) rect.getHeight() * ImageHeight / ActualImageHeight;
			rect = new Rectangle(pt[0], pt[1], pt[2], pt[3]);
			Rectangles.set(i, rect);
		}
		if (StartPoint != null && EndPoint != null) {
			StartPoint.x = StartPoint.x * ImageWidth / prevWidth;
			StartPoint.y = StartPoint.y * ImageHeight / prevHeight;
			EndPoint.x = EndPoint.x * ImageWidth / prevWidth;
			EndPoint.y = EndPoint.y * ImageHeight / prevHeight;
		}
		// paintComponent(imgLbl.getGraphics()); // this doesn't work till there
		// is event occured
		// generate mouse event for drawing
		generateMouseEvent();
	}

	/**
	 * Generates mouseClickEvent
	 */
	private void generateMouseEvent() {
		MouseEvent evt = new MouseEvent(imgLbl, // which
				MouseEvent.MOUSE_CLICKED, // what
				System.currentTimeMillis(), // when
				0, // no modifiers
				ImageWidth / 2, ImageHeight / 2, // where: at (0,0) on imgLbl
				1, // only 1 click
				true); // not a popup trigger
		imgLbl.dispatchEvent(evt); // this is not trigerring the event, better
									// to register two events
		Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(evt);
	}

	/**
	 * Returns normalized points
	 * 
	 * @return
	 */
	private int[] normalise() {
		int pt[] = new int[4];
		pt[0] = Math.min(StartPoint.x, EndPoint.x);
		pt[1] = Math.min(StartPoint.y, EndPoint.y);
		pt[2] = Math.max(StartPoint.x, EndPoint.x);
		pt[3] = Math.max(StartPoint.y, EndPoint.y);
		pt[0] = Math.max(pt[0], 0);
		pt[1] = Math.max(pt[1], 0);
		pt[2] = Math.min(pt[2], ImageWidth);
		pt[3] = Math.min(pt[3], ImageHeight);
		pt[2] = pt[2] - pt[0];
		pt[3] = pt[3] - pt[1];
		return pt;
	}

	/**
	 * Load the files in the application
	 * 
	 * @param path
	 * @throws IOException
	 */
	private void loadFiles(Path path) throws IOException {
		// clear the lists
		thumbscon.clear();
		keyList.clear();
		FileNames.clear();
		imgLbl.getGraphics().clearRect(0, 0, ImageWidth, ImageHeight);
		imgPane.setBorder(BorderFactory.createTitledBorder("Image Pane"));
		ActualImage = null;
		ResizedImage = null;

		/*
		 * trim the text if the search text is empty load all files else load
		 * only the files containing the keyword
		 */
		SearchText = SearchText.trim();
		if (SearchText.isEmpty()) {
			loadAllFiles(path);
		} else {
			loadSelectedFiles(path);
		}
	}

	/**
	 * Load the all file in the application
	 * 
	 * @param start
	 * @throws IOException
	 */
	private void loadAllFiles(final Path start) throws IOException {
		// walking through the whole file tree
		Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
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
					curImage = ImageIO.read(new File(ThumbsPath + "\\"
							+ fileName));
					thumbscon.addElement(new ImageIcon(curImage));
					FileNames.add(fileName);
					// scrollPane.setViewportView(thumbsList); // uncomment it
					// only if the scrollpane doesn't show all the images
				} catch (IIOException e) {
					addThumbImage(fileName);
				} catch (Exception e) {
					System.out.println(e.toString()); // log for invalid
														// file
					// format
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
		});
	}

	/**
	 * Load selected files in the application
	 * 
	 * @param path
	 */
	private void loadSelectedFiles(Path path) {
		SwingWorker<Void, Void> searching = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				IndexReader reader = DirectoryReader.open(FSDirectory
						.open(new File(IndexPath)));
				IndexSearcher searcher = new IndexSearcher(reader);
				Analyzer analyzer = new StandardAnalyzer(
						Version.parseLeniently("4.0"));

				QueryParser parser = new QueryParser(
						Version.parseLeniently("4.0"), field, analyzer);
				String line = SearchText;

				Query query = parser.parse(line);
				// System.out.println("Searching for: " +
				// query.toString(field)); // log

				doPagingSearch(new BufferedReader(new InputStreamReader(
						System.in, "UTF-8")), searcher, query, 10, false, true);

				reader.close();
				return null;
			}

			private void doPagingSearch(BufferedReader in,
					IndexSearcher searcher, Query query, int hitsPerPage,
					boolean raw, boolean interactive) {
				TopDocs results;
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
								thumbscon.addElement(new ImageIcon(ThumbsPath
										+ '\\' + file));
								FileNames.add(file);
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
					// System.out.println(e); // log
				}
			}

		};
		searching.execute();
	}

	private void loadImage(int index) throws IOException {
		prevSelectedInd = -1;
		FileNumber = index;
		if (FileNames.isEmpty()) {
			imgPane.setBorder(BorderFactory.createTitledBorder("Image Pane"));
			imgLbl.setText("Directory is empty");
			// System.out.println("Directory is empty"); // log
		} else {
			thumbsList.setSelectedIndex(index);
			imgLbl.setText("");
			TitledBorder title = BorderFactory.createTitledBorder(FileNames
					.get(index)
					+ " ("
					+ (index + 1)
					+ "/"
					+ FileNames.size()
					+ ")");
			title.setTitleJustification(TitledBorder.CENTER);
			imgPane.setBorder(title);
			ActualImage = ImageIO.read(new File(ImagePath + "/"
					+ FileNames.get(index)));
			ActualImageWidth = ActualImage.getWidth(null);
			ActualImageHeight = ActualImage.getHeight(null);
			setImageLabel();
			keyList.clear();
			ImageRectangles.clear();
			Rectangles.clear();
			Different.clear();
			try {
				@SuppressWarnings("resource")
				// remove it if needed
				BufferedReader br = new BufferedReader(new FileReader(RectPath
						+ "/" + FileNames.get(index) + ".txt"));
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
					ImageRectangles.add(new Rectangle(pt[0], pt[1], pt[2],
							pt[3]));
					pt = imagePoints(pt);
					Rectangles.add(new Rectangle(pt[0], pt[1], pt[2], pt[3]));
					Different.add(false);
					String key = m.group(5);
					keyList.addElement(key);
					// System.out.println(pr+": "+key); // log
				}
			} catch (FileNotFoundException e) {
				// System.out.println("No keywords extracted till now"+e.toString());
				// // log
			} catch (IOException e) {
				// System.out.println(e.toString()); // log for corrupted file
			}
			// paintComponent(imgLbl.getGraphics()); // this doesn't work till
			// there is event occured
			// generate mouse event for drawing
			generateMouseEvent();
		}
	}

	private void setImageLabel() {
		if (ActualImage != null) {
			ResizedImage = ActualImage.getScaledInstance(ImageWidth,
					ImageHeight, 0);
			imgLbl.getGraphics().drawImage(ResizedImage, 0, 0, null);
			// imgLbl.setIcon(new ImageIcon(ResizedImage)); // for now only use
			// drawImage(); this doesn't allow dividor location change
		}
	}

	/*
	 * This method has no effect unless an event occurs (non-Javadoc)
	 * 
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		if (ResizedImage != null) {
			Graphics2D g2d = (Graphics2D) graphics;
			g2d.drawImage(ResizedImage, 0, 0, null); // always required because
														// there is a chance of
														// clearing the
														// rectangle
			if (!Rectangles.isEmpty()) {
				int i = 0;
				for (Rectangle rect : Rectangles) {
					if (Different.get(i))
						g2d.setColor(Color.GREEN);
					else
						g2d.setColor(Color.BLACK);
					g2d.drawRect((int) rect.getX(), (int) rect.getY(),
							(int) rect.getWidth(), (int) rect.getHeight());
					++i;
				}
			}
			if (StartPoint != EndPoint) {
				int pt[] = normalise();
				g2d.setColor(Color.BLACK);
				if (pt[2] != 0 && pt[3] != 0)
					g2d.drawRect(pt[0], pt[1], pt[2], pt[3]);
			}
		}
	}

	private int[] imagePoints(int[] pt) {
		int par[] = new int[4];
		par[0] = pt[0] * ImageWidth / ActualImage.getWidth(null);
		par[1] = pt[1] * ImageHeight / ActualImage.getHeight(null);
		par[2] = pt[2] * ImageWidth / ActualImage.getWidth(null);
		par[3] = pt[3] * ImageHeight / ActualImage.getHeight(null);
		return par;
	}

	private int[] actualPoints(int[] pt) {
		int par[] = new int[4];
		par[0] = pt[0] * ActualImage.getWidth(null) / ImageWidth;
		par[1] = pt[1] * ActualImage.getHeight(null) / ImageHeight;
		par[2] = pt[2] * ActualImage.getWidth(null) / ImageWidth;
		par[3] = pt[3] * ActualImage.getHeight(null) / ImageHeight;
		return par;
	}

	protected void setComponentSizes() {
		// frame.setBounds(0, 0, AppWidth, AppHeight); // Uncommenting it will
		// make it unable to use the minimize button
		if (searchPanel != null)
			searchPanel.setPreferredSize(new Dimension(AppWidth,
					SearchPanelHeight));
		if (imgPane != null) {
			imgPane.setPreferredSize(new Dimension(AppWidth, SplitPaneHeight));
			imgPane.setDividerLocation(DividerLocation);
		}
		if (imgPnl != null)
			imgPnl.setSize(DividerLocation, SplitPaneHeight);
		// if(thumbsList!=null) thumbsList.setPreferredSize(new
		// Dimension(AppWidth-5, ThumbsHeight));
		if (scrollPane != null)
			scrollPane.setPreferredSize(new Dimension(AppWidth,
					ThumbsPaneHeight));

	}

	private void addThumbImage(String fileName) throws IOException {
		int index = fileName.lastIndexOf('.');
		if (index == 0)
			throw new IOException();
		Image image = ImageIO.read(new File(ImagePath + "\\" + fileName))
				.getScaledInstance(40, 40, Image.SCALE_SMOOTH);
		BufferedImage resizedImage = new BufferedImage(40, 40,
				BufferedImage.TYPE_INT_ARGB);
		resizedImage.getGraphics().drawImage(image, 0, 0, null);
		ImageIO.write(resizedImage, fileName.substring(index + 1), new File(
				ThumbsPath + "/" + fileName));

		// this will add the items
		if (SearchText.trim().isEmpty()) {
			thumbscon.addElement(new ImageIcon(resizedImage));
			FileNames.add(fileName);
		}
	}

}
