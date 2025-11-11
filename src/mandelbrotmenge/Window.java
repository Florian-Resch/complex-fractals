package mandelbrotmenge;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

public class Window {
	
	private JFrame frame;
	
	private DrawingPanel drawingPanel;
	
	private JLabel lblSkalierung;
	private JLabel lblFixpunkt;
	
	private JButton btnSpeichern;
	private JButton btnNeuladen;
	private JLabel lblIterationen;
	private JTextField textFieldIterationen;
	private JLabel lblParameter;
	private JTextField textFieldParameter;
	private JCheckBox chckbxNewCheckBox;
	
	boolean gestartet = true;
	
	/**
	 * für Bewegen mit MouseMotionListener
	 */
	public static Point startpunkt = null;
	
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
			public void mouseReleased(MouseEvent e) {
				drawingPanel.requestFocusInWindow();
				if (!gestartet) return;
				while (lock);
				lock = true;
				startpunkt = null;
				lock = false;
			}
		});
		drawingPanel.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent e) {
				if (!gestartet) return;
				while (lock);
				lock = true;
				if (startpunkt == null) {
					startpunkt = e.getPoint();
				}
				else {
					int diffX = (int) (e.getX() - startpunkt.getX());
					int diffY = (int) (e.getY() - startpunkt.getY());
					drawingPanel.bewegen(new Point(-diffX, -diffY));
					lblFixpunkt.setText(drawingPanel.getFixpunkt() + "   ");
					startpunkt = e.getPoint();
				}
				lock = false;
			}
		});
		drawingPanel.addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent e) {
				if (!gestartet) return;
				while (lock);
				lock = true;
				if (e.getWheelRotation() < 0) {
					drawingPanel.reinzoomen(e.getPoint());
				}
				else {
					drawingPanel.rauszoomen(e.getPoint());
				}
				lblSkalierung.setText(drawingPanel.getSkalierung() + "   ");
				lblFixpunkt.setText(drawingPanel.getFixpunkt() + "   ");
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

		// Map the action name to actual behavior
		actionMap.put("moveUp", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!gestartet) return;
				while (lock);
				lock = true;
				
				drawingPanel.bewegen(new Point(0, -20));
				lblFixpunkt.setText(drawingPanel.getFixpunkt() + "   ");
				
				lock = false;
			}
		});
		actionMap.put("moveDown", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!gestartet) return;
				while (lock);
				lock = true;

				drawingPanel.bewegen(new Point(0, 20));
				lblFixpunkt.setText(drawingPanel.getFixpunkt() + "   ");
				
				lock = false;
			}
		});
		actionMap.put("moveLeft", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!gestartet) return;
				while (lock);
				lock = true;
				
				drawingPanel.bewegen(new Point(-20, 0));
				lblFixpunkt.setText(drawingPanel.getFixpunkt() + "   ");
				
				lock = false;
			}
		});
		actionMap.put("moveRight", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!gestartet) return;
				while (lock);
				lock = true;
				
				drawingPanel.bewegen(new Point(20, 0));
				lblFixpunkt.setText(drawingPanel.getFixpunkt() + "   ");

				lock = false;
			}
		});
		actionMap.put("zoomIn", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!gestartet) return;
				while (lock);
				lock = true;

				drawingPanel.reinzoomen(new Point(drawingPanel.getWidth()/2, drawingPanel.getHeight()/2));

				lblSkalierung.setText(drawingPanel.getSkalierung() + "   ");
				lblFixpunkt.setText(drawingPanel.getFixpunkt() + "   ");
				lock = false;
			}
		});
		actionMap.put("zoomOut", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!gestartet) return;
				while (lock);
				lock = true;
				
				drawingPanel.rauszoomen(new Point(drawingPanel.getWidth()/2, drawingPanel.getHeight()/2));

				lblSkalierung.setText(drawingPanel.getSkalierung() + "   ");
				lblFixpunkt.setText(drawingPanel.getFixpunkt() + "   ");
				lock = false;
			}
		});

		JMenuBar menuBar = new JMenuBar();
		menuBar.setLayout(new FlowLayout());
		frame.setJMenuBar(menuBar);
		
		JMenu mnSystem = new JMenu("System");
		menuBar.add(mnSystem);
		
		JMenuItem mntmStartenMandelbrot = new JMenuItem("Starten Mandelbrot");
		mntmStartenMandelbrot.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				while (lock);
				lock = true;
				drawingPanel.reset();
				drawingPanel.setMandelbrotmenge(true);
				drawingPanel.drawMandelbrotJuliamenge();
				gestartet = true;
				lock = false;
			}
		});
		mnSystem.add(mntmStartenMandelbrot);
		
		JMenuItem mntmStartenJulia = new JMenuItem("Starten Julia");
		mntmStartenJulia.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				while (lock);
				lock = true;
				drawingPanel.reset();
				drawingPanel.setMandelbrotmenge(false);
				drawingPanel.drawMandelbrotJuliamenge();
				gestartet = true;
				lock = false;
			}
		});
		mnSystem.add(mntmStartenJulia);
		
		JMenuItem mntmBeenden = new JMenuItem("Beenden");
		mntmBeenden.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		mnSystem.add(mntmBeenden);
		
		lblSkalierung = new JLabel(drawingPanel.getSkalierung() + "   ");
		menuBar.add(lblSkalierung);
		
		lblFixpunkt = new JLabel(drawingPanel.getFixpunkt() + "   ");
		menuBar.add(lblFixpunkt);

		btnSpeichern = new JButton("Speichern");
		btnSpeichern.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Thread t = new Thread(new Runnable() {
					public void run() {
						BufferedImage bild = bildBerechnen();
						if (speichern(bild, false)) {
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
				if (!gestartet) return;
				drawingPanel.drawMandelbrotJuliamenge();
				lock = false;
			}
		});
		menuBar.add(btnNeuladen);
		
		lblIterationen = new JLabel(" Iterationen ");
		menuBar.add(lblIterationen);

		textFieldIterationen = new JTextField(drawingPanel.getMaxIterationen() + "");
		textFieldIterationen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!gestartet) return;
				while (lock);
				lock = true;
				try {
					drawingPanel.setMaxIterationen(Integer.parseInt(textFieldIterationen.getText()));
					drawingPanel.drawMandelbrotJuliamenge();
				}
				catch (NumberFormatException e1) {
					textFieldIterationen.setText(drawingPanel.getMaxIterationen() + "");
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
				if (!gestartet) return;
				while (lock);
				lock = true;
				try {
					String[] zahlen = textFieldParameter.getText().split("\\+");// Format: +-1234 + +-5678i
					ComplexNumber parameter = new ComplexNumber(Double.parseDouble(zahlen[0]), Double.parseDouble(zahlen[1].substring(0, zahlen[1].length()-1)));
					drawingPanel.setParameter(parameter);
					drawingPanel.drawMandelbrotJuliamenge();
				}
				catch (NumberFormatException | NullPointerException e1) {
					textFieldIterationen.setText(drawingPanel.getMaxIterationen() + "");
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
		
	}
	
	private BufferedImage bildBerechnen() {
		BufferedImage image = drawingPanel.toImage();
		BufferedImage signatur = signaturEinlesen();
		
		Graphics gr = image.getGraphics();
		for (int i=image.getWidth()-signatur.getWidth(), x=0; i<image.getWidth(); i++, x++) {
			for (int j=image.getHeight()-signatur.getHeight(), y=0; j<image.getHeight(); j++, y++) {
				Color c = new Color(signatur.getRGB(x, y));
				if (!c.equals(Color.WHITE)) {
					gr.setColor(c);
					gr.drawLine(i, j, i, j);
				}
			}
		}
		
		return image;
	}
	
	public boolean speichern(BufferedImage bild, boolean debug) {
		if (bild == null)
			return false;
		int bildNummer = 1;
		File file;
		if (debug) {
			file = new File("Bilder/Debug" + bildNummer + ".png");
			while (file.exists()) {
				bildNummer++;
				file = new File("Bilder/Debug" + bildNummer + ".png");
			}
		}
		else {
			file = new File("Bilder/Bild" + bildNummer + ".png");
			while (file.exists()) {
				bildNummer++;
				file = new File("Bilder/Bild" + bildNummer + ".png");
			}
		}
		
		try {
			ImageIO.write(bild, "png", file);
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
	
}

