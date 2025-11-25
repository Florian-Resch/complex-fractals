package mandelbrotmenge;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import helper.WrapLayout;

public class Window {
	
	private JFrame frame;
	
	private DrawingPanel drawingPanel;
	
	private JButton btnBeenden;
	private JComboBox<FractalType> comboBoxFractalType;

	private JLabel lblSkalierung;
	private JLabel lblFixpunkt;
	
	private JButton btnSpeichern;
	private JButton btnNeuladen;
	private JLabel lblIterationen;
	private JTextField textFieldIterationen;
	private JLabel lblParameter;
	private JTextField textFieldParameter;
	private JCheckBox chckbxNewCheckBox;
	private JButton btnSelectPosition;
	private JButton btnTest;
	
	/**
	 * für Bewegen mit MouseMotionListener
	 */
	public static Point startpunkt = null;

	/**
	 * true, if there is a selection ongoing
	 */
	public static boolean selecting = false;
	
	/**
	 * a lock which is set, if some method does any calculations or changes in drawingPanel
	 */
	public static boolean lock = false;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Window window = new Window();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public Window() {
		initialize();
	}
	
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		drawingPanel = new DrawingPanel();
		drawingPanel.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				if (selecting) {
					ComplexNumber newParameter = drawingPanel.pointToComplex(e.getPoint());
					textFieldParameter.setText(newParameter + "");
					drawingPanel.reset();

					drawingPanel.setParameter(newParameter);
					drawingPanel.setFractalType(FractalType.JuliaSet);
					drawingPanel.drawFractal();
					unsetSelecting();
					return;
				}
			}

			public void mouseReleased(MouseEvent e) {
				drawingPanel.requestFocusInWindow();
				while (lock);
				lock = true;
				startpunkt = null;
				lock = false;
			}
		});
		drawingPanel.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				while (lock);
				lock = true;
				if (startpunkt == null) {
					startpunkt = e.getPoint();
				}
				else {
					int diffX = (int) (e.getX() - startpunkt.getX());
					int diffY = (int) (e.getY() - startpunkt.getY());
					drawingPanel.move(new Point(-diffX, -diffY));
					lblFixpunkt.setText(drawingPanel.getFixpoint() + "   ");
					startpunkt = e.getPoint();
				}
				lock = false;
			}
		});
		drawingPanel.addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				while (lock);
				lock = true;
				if (e.getWheelRotation() < 0) {
					drawingPanel.zoomIn(e.getPoint());
				}
				else {
					drawingPanel.zoomOut(e.getPoint());
				}
				lblSkalierung.setText(drawingPanel.getScaling() + "   ");
				lblFixpunkt.setText(drawingPanel.getFixpoint() + "   ");
				lock = false;
			}
		});
		frame.getContentPane().add(drawingPanel, BorderLayout.CENTER);

		// Get the InputMap and ActionMap
		InputMap inputMap = drawingPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = drawingPanel.getActionMap();

		// Bind a key to an action name
		inputMap.put(KeyStroke.getKeyStroke("UP"), "moveUp");
		inputMap.put(KeyStroke.getKeyStroke("DOWN"), "moveDown");
		inputMap.put(KeyStroke.getKeyStroke("LEFT"), "moveLeft");
		inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "moveRight");
		inputMap.put(KeyStroke.getKeyStroke("PLUS"), "zoomIn");
		inputMap.put(KeyStroke.getKeyStroke("MINUS"), "zoomOut");
		inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "cancel");

		// Map the action name to actual behavior
		actionMap.put("moveUp", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				while (lock);
				lock = true;
				
				drawingPanel.move(new Point(0, -20));
				lblFixpunkt.setText(drawingPanel.getFixpoint() + "   ");
				
				lock = false;
			}
		});
		actionMap.put("moveDown", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				while (lock);
				lock = true;

				drawingPanel.move(new Point(0, 20));
				lblFixpunkt.setText(drawingPanel.getFixpoint() + "   ");
				
				lock = false;
			}
		});
		actionMap.put("moveLeft", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				while (lock);
				lock = true;
				
				drawingPanel.move(new Point(-20, 0));
				lblFixpunkt.setText(drawingPanel.getFixpoint() + "   ");
				
				lock = false;
			}
		});
		actionMap.put("moveRight", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				while (lock);
				lock = true;
				
				drawingPanel.move(new Point(20, 0));
				lblFixpunkt.setText(drawingPanel.getFixpoint() + "   ");

				lock = false;
			}
		});
		actionMap.put("zoomIn", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				while (lock);
				lock = true;

				drawingPanel.zoomIn(new Point(drawingPanel.getWidth()/2, drawingPanel.getHeight()/2));

				lblSkalierung.setText(drawingPanel.getScaling() + "   ");
				lblFixpunkt.setText(drawingPanel.getFixpoint() + "   ");
				lock = false;
			}
		});
		actionMap.put("zoomOut", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				while (lock);
				lock = true;
				
				drawingPanel.zoomOut(new Point(drawingPanel.getWidth()/2, drawingPanel.getHeight()/2), 2);

				lblSkalierung.setText(drawingPanel.getScaling() + "   ");
				lblFixpunkt.setText(drawingPanel.getFixpoint() + "   ");
				lock = false;
			}
		});
		actionMap.put("cancel", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				unsetSelecting();
			}
		});

		JMenuBar menuBar = new JMenuBar();
		menuBar.setLayout(new WrapLayout(FlowLayout.LEFT));
		frame.setJMenuBar(menuBar);
						
		btnBeenden = new JButton("Beenden");
		btnBeenden.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		menuBar.add(btnBeenden);

		comboBoxFractalType = new JComboBox<>(FractalType.values());
		comboBoxFractalType.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				while (lock);
				lock = true;
				drawingPanel.reset();
				drawingPanel.setFractalType((FractalType) comboBoxFractalType.getSelectedItem());
				drawingPanel.drawFractal();
				lock = false;
			}
		});
		menuBar.add(comboBoxFractalType);
		
		lblSkalierung = new JLabel(drawingPanel.getScaling() + "   ");
		menuBar.add(lblSkalierung);
		
		lblFixpunkt = new JLabel(drawingPanel.getFixpoint() + "   ");
		menuBar.add(lblFixpunkt);

		btnSpeichern = new JButton("Speichern");
		btnSpeichern.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread t = new Thread(new Runnable() {
					public void run() {
						BufferedImage bild = bildBerechnen();
						if (speichern(bild, "Bild")) {
							System.out.println("Speichern erfolgreich");
						}
						else {
							System.out.println("Speichern fehlgeschlagen");
						}
					}
				});
				t.run();
			}
		});
		menuBar.add(btnSpeichern);

		btnNeuladen = new JButton("Neuladen");
		btnNeuladen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				while (lock);
				lock = true;
				drawingPanel.drawFractal();
				lock = false;
			}
		});
		menuBar.add(btnNeuladen);
		
		lblIterationen = new JLabel(" Iterationen ");
		menuBar.add(lblIterationen);

		textFieldIterationen = new JTextField(drawingPanel.getMaxInterations() + "");
		textFieldIterationen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				while (lock);
				lock = true;
				try {
					drawingPanel.setMaxInterations(Integer.parseInt(textFieldIterationen.getText()));
					drawingPanel.drawFractal();
				}
				catch (NumberFormatException e1) {
					textFieldIterationen.setText(drawingPanel.getMaxInterations() + "");
				}
				lock = false;
			}
		});
		textFieldIterationen.setColumns(10);
		menuBar.add(textFieldIterationen);
		
		lblParameter = new JLabel(" Parameter ");
		menuBar.add(lblParameter);

		textFieldParameter = new JTextField(drawingPanel.getParameter() + "");
		textFieldParameter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				while (lock);
				lock = true;
				try {
					String[] numbers = textFieldParameter.getText().split("\\+");// Format: +-1234 + +-5678i
					ComplexNumber parameter = new ComplexNumber(Double.parseDouble(numbers[0]), Double.parseDouble(numbers[1].substring(0, numbers[1].length()-1)));
					drawingPanel.setParameter(parameter);
					drawingPanel.drawFractal();
				}
				catch (NumberFormatException | NullPointerException e1) {
					textFieldIterationen.setText(drawingPanel.getMaxInterations() + "");
				}
				lock = false;
			}
		});
		textFieldParameter.setColumns(10);
		menuBar.add(textFieldParameter);
		
		chckbxNewCheckBox = new JCheckBox("Border tracing");
		chckbxNewCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				drawingPanel.setBorderTracing(chckbxNewCheckBox.getSelectedObjects() != null);
			}
		});
		chckbxNewCheckBox.setSelected(true);
		menuBar.add(chckbxNewCheckBox);

		btnSelectPosition = new JButton("Select");
		btnSelectPosition.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!selecting)
					setSelecting();
				else
					unsetSelecting();
			}
		});
		menuBar.add(btnSelectPosition);

		btnTest = new JButton("Select");
		btnTest.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				videoZoom(new ComplexNumber(-0.831367145043347, 0.22917945161416745), 1, 8.871766638236154e-15, 600);
				createVideo("video.mp4", 20);
			}
		});
		menuBar.add(btnTest);

		drawingPanel.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				if (drawingPanel.getWidth() > 0 && drawingPanel.getHeight() > 0 /* don't trigger on first invokation */ && !lock /* don't trigger to often */) {
					btnNeuladen.doClick();
				}
			}
		});

	}

	private void setSelecting() {
		selecting = true;
		drawingPanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
	}

	private void unsetSelecting() {
		selecting = false;
		drawingPanel.setCursor(Cursor.getDefaultCursor());
	}

	private BufferedImage bildBerechnen() {
		BufferedImage image = drawingPanel.toImage();
		BufferedImage signature = signaturEinlesen();
		
		Graphics gr = image.getGraphics();
		for (int i=image.getWidth()-signature.getWidth(), x=0; i<image.getWidth(); i++, x++) {
			for (int j=image.getHeight()-signature.getHeight(), y=0; j<image.getHeight(); j++, y++) {
				Color c = new Color(signature.getRGB(x, y));
				if (!c.equals(Color.WHITE)) {
					gr.setColor(c);
					gr.drawLine(i, j, i, j);
				}
			}
		}
		
		return image;
	}

	public void videoZoom(ComplexNumber centerpoint, double startZoom, double endZoom, int frameCount) {
		drawingPanel.setScaling(startZoom);
		drawingPanel.setFixpoint(centerpoint);
		drawingPanel.drawFractal();
		double zoomDifference = Math.pow(endZoom/startZoom, 1.0/frameCount);
		drawingPanel.move(new Point(-drawingPanel.getWidth()/2, -drawingPanel.getHeight()/2));
		for (int i=0; i<frameCount; i++) {
			drawingPanel.setScaling(startZoom*Math.pow(zoomDifference, i));
			drawingPanel.setFixpoint(centerpoint);
			drawingPanel.setFixpoint(drawingPanel.pointToComplex(new Point(-drawingPanel.getWidth()/2, -drawingPanel.getHeight()/2)));
			//drawingPanel.move(new Point(-drawingPanel.getWidth()/2, -drawingPanel.getHeight()/2));
			drawingPanel.drawFractal();
			//drawingPanel.zoomIn(new Point(drawingPanel.getWidth()/2, drawingPanel.getHeight()/2), 1/zoomDifference);
			drawingPanel.drawFractal();
			BufferedImage frame = bildBerechnen();
			speichern(frame, "Video/");
		}
	}
	
	public boolean speichern(BufferedImage image, String naming) {
		if (image == null)
			return false;
		int imageNumber = 1;
		File file = new File("Bilder/" + naming + imageNumber + ".png");
		while (file.exists()) {
			imageNumber++;
			file = new File("Bilder/" + naming + imageNumber + ".png");
		}
		
		try {
			ImageIO.write(image, "png", file);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	public BufferedImage signaturEinlesen() {
		File file = new File("Bilder/Signatur.png");
		try {
			return ImageIO.read(file);
		} catch (IOException e) {
			return null;
		}
	}

	private static void createVideo(String outputFile, int fps) {
		ProcessBuilder pb = new ProcessBuilder(
				"ffmpeg",
				"-framerate", fps + "",                // Input framerate
				"-i", "Bilder/Video/%d.png",           // Input pattern
				"-c:v", "libx264",                     // Video codec
				"-preset", "medium",                   // Encoding speed
				"-crf", "23",                          // Quality
				"-y",                                  // Overwrite output
				"Bilder/" + outputFile
		);
		pb.redirectError(ProcessBuilder.Redirect.INHERIT);
		Process process = null;
		try {
			process = pb.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
		int exitCode = -1;
		try {
			exitCode = process.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (exitCode == 0) {
				System.out.println("Video created successfully: " + outputFile);
		} else {
				System.err.println("FFmpeg failed with exit code: " + exitCode);
		}
	}

}
