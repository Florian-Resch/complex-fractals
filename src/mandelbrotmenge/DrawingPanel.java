package mandelbrotmenge;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.stream.Collectors;

import javax.swing.JPanel;

/**
 * zeichenbarer Bereich = (0, 0) bis (width-1, height-1)
 */
public class DrawingPanel extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4523863750308369087L;
	
	/**
	 * a buffer which holds every image before it is drawn
	 */
	private BufferedImage graphicsBuffer = new BufferedImage(Math.max(1, getWidth()), Math.max(1, getHeight()), BufferedImage.TYPE_INT_RGB);

	/**
	 * die Zahl, die das Pixel (0,0) bekommt
	 */
	private ComplexNumber fixpoint = new ComplexNumber(-2, 1);//-1.424455409425403, 0.12712914901631397);
	
	/**
	 * die Zahl, der 450 Pixel entsprechen
	 */
	private double scaling = 1;//1.4210854715202004e-14;
	
	private int maxInterations = 1000;
	
	/**
	 * true, wenn die Mandelbrotmenge gezeichnet wird; false, wenn eine Juliamenge gezeichnet wird
	 */
	private FractalType fractalType = FractalType.MandelbrotSet;
	
	/**
	 * der Parameter für das Zeichnen von Juliamengen
	 */
	private ComplexNumber parameter = new ComplexNumber();
	
	/**
	 * true, wenn border tracing verwendet werden soll
	 */
	private boolean borderTracing = true;

	public DrawingPanel(/*int breite, int hoehe*/) {
		// setBounds(0, 0, breite, hoehe);
		addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				BufferedImage oldGraphicsBuffer = graphicsBuffer;
				graphicsBuffer = new BufferedImage(Math.max(1, getWidth()), Math.max(1, getHeight()), BufferedImage.TYPE_INT_RGB);
				Graphics buffer = graphicsBuffer.getGraphics();
				buffer.drawImage(oldGraphicsBuffer, 0, 0, null);
				if (getWidth() > oldGraphicsBuffer.getWidth()) {
					drawFractalPart(true, -(getWidth()-oldGraphicsBuffer.getWidth()), buffer);
				}
				if (getHeight() > oldGraphicsBuffer.getHeight()) {
					drawFractalPart(false, -(getHeight()-oldGraphicsBuffer.getHeight()), buffer);
				}
			}
		});
	}
	
	public void reset() {
		fixpoint = new ComplexNumber(-2, 1);
		scaling = 1;
		
		/*Graphics gr = getGraphics();
		for (int i=0; i<getWidth(); i++) {
			for (int j=0; j<getHeight(); j++) {
				Color c = intToColor((i+j)%256);
				gr.setColor(c);
				gr.drawLine(i, j, i, j);
			}
		}*/
	}

	/**
	 * zooms into the fractal
	 * @param cursorPos the position of the cursor on the panel
	 * @param zoomFactor the zoomfactor (must be greater than 1)
	 */
	public void zoomIn(Point cursorPos, double zoomFactor) {
		if (zoomFactor < 1) {
			throw new IllegalArgumentException();
		}
		if (scaling/zoomFactor != 0.0) {
			Point newFixpoint = new Point((int) (cursorPos.x*(1.0-1.0/zoomFactor)), (int) (cursorPos.y*(1.0-1.0/zoomFactor)));
			fixpoint = pointToComplex(newFixpoint);
			scaling /= zoomFactor;
			
			drawFractalPart(true, 0, graphicsBuffer.getGraphics());
			getGraphics().drawImage(graphicsBuffer, 0, 0, null);
		}
	}
	
	/**
	 * zoomt näher heran
	 * @param cursorPos die Position der Maus beim Zoomen
	 */
	public void zoomIn(Point cursorPos) {
		zoomIn(cursorPos, 2);
	}
	
	/**
	 * zooms out from the fractal
	 * @param cursorPos the position of the cursor on the panel
	 * @param zoomFactor the zoomfactor (must be greater than 1)
	 */
	public void zoomOut(Point cursorPos, double zoomFactor) {
		if (zoomFactor < 1) {
			throw new IllegalArgumentException();
		}
		if (scaling*zoomFactor != Double.POSITIVE_INFINITY) {
			Point newFixpoint = new Point((int) (-cursorPos.x*(zoomFactor-1.0)), (int) (-cursorPos.y*(zoomFactor-1.0)));
			fixpoint = pointToComplex(newFixpoint);
			scaling *= zoomFactor;

			BufferedImage tmp = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
			tmp.getGraphics().drawImage(graphicsBuffer, 0, 0, null);
			graphicsBuffer.getGraphics().drawImage(tmp, -newFixpoint.x/2, -newFixpoint.y/2, (int) (getWidth()/zoomFactor), (int) (getHeight()/zoomFactor), null);

			drawFractalPart(true, -newFixpoint.x/2+2, graphicsBuffer.getGraphics());
			drawFractalPart(true, newFixpoint.x/2-2, graphicsBuffer.getGraphics());
			drawFractalPart(false, -newFixpoint.y/2+2, graphicsBuffer.getGraphics());
			drawFractalPart(false, newFixpoint.y/2-2, graphicsBuffer.getGraphics());

			getGraphics().drawImage(graphicsBuffer, 0, 0, null);
		}
	}
	
	/**
	 * zoomt weiter heraus
	 * @param cursorPos die Position der Maus beim Zoomen
	 */
	public void zoomOut(Point cursorPos) {
		zoomOut(cursorPos, 2);
	}
	
	/**
	 * bewegt den sichtbaren Bereich
	 * @param offset wenn x/y > 0, dann bildet sich rechts/unten ein Rand
	 */
	public void move(Point offset) {
		fixpoint = pointToComplex(offset);
		
		Graphics gr = graphicsBuffer.getGraphics();
		gr.copyArea(0, 0, getWidth(), getHeight(), -offset.x, -offset.y);
		
		if (offset.getX() != 0) {
			drawFractalPart(true, -offset.x, gr);
		}
		if (offset.getY() != 0) {
			drawFractalPart(false, -offset.y, gr);
		}

		getGraphics().drawImage(graphicsBuffer, 0, 0, null);
	}
	
	/**
	 * zeichnet die Menge mit dem gewählten Parameter
	 * <br>Mandelbrot: x² + c mit x0 = konst. und c variabel
	 * <br>Julia:      x² + c mit c = konst. und x0 variabel
	 * @param parameter der Wert von x0; standardmäßig 0
	 */
	public void drawFractal() {
		drawFractalPart(true, 0, getGraphics());
	}
	
	/**
	 * zeichnet einen Teil der Mandelbrotmenge (bei wert = 0 wird alles gezeichnet)
	 * @param xDirection true, wenn der zu zeichnende Streifen links oder rechts liegt; false, wenn er oben oder unten liegt
	 * @param value positiv, wenn der Streifen oben oder links liegt; kann auch 0 sein, dann wir alles gezeichnet
	 */
	private void drawFractalPart(boolean xDirection, int value, Graphics gr) {
		long time = System.nanoTime();
		/*if (wert == 0) {
			deepZoomMandelbrotmenge(new Point(0, 0), getWidth()-1, getHeight()-1, getGraphics());
		}
		else */
		if (value == 0) {
			if (scaling < 0.51 && borderTracing) {
				int width = getWidth();
				int height = getHeight();
				bordertracingFractal(new Point(0, 0), new Point(width, height), gr);
			}
			else {
				partBordertracingFractal(gr);
			}
		}
		else {
			int startI = 0;
			int startJ = 0;
			int width = getWidth();
			int height = getHeight();
			
			if (xDirection) {
				if (value > 0) {
					width = value;
				}
				else if (value < 0) {
					startI = width+value;
				}
			}
			else {
				if (value > 0) {
					height = value;
				}
				else if (value < 0) {
					startJ = height+value;
				}
			}
			bordertracingFractal(new Point(startI, startJ), new Point(width, height), gr);
		}
		System.out.println(iter + " iterations: " + (System.nanoTime()-time)/1000000 + " ms");
		iter = 0;
		
		
	}
	
	/**
	 * enthält Offsets für die Richtungen N, NO, O, SO, S, SW, W, NW
	 */
	private static int[][] directions = {{0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};
	
	/**
	 * zeichnet die gesamte Mandelbrotmenge unter Ausnutzen des Randes der Gebiete
	 * @param start der Punkt, um den das Pixel (0,0) verschoben werden soll
	 * @param end das Pixel nach dem letzten Pixel, das gezeichnet werden soll
	 * @param gr das Graphics-Objekt, in das gezeichnet werden soll
	 */
	private void bordertracingFractal(Point start, Point end, Graphics gr) {
//		gr.setColor(Color.WHITE);
//		gr.fillRect(offset.x, offset.y, breite, hoehe);
		
		// statt immer wieder boolean-Arrays zu initialisieren, wird in dieses Array mit fortlaufender Nummer geschrieben
		int[][] alreadyColored = new int[end.x][end.y];// oft zu groß, aber effizienter abzufragen
		int coloredCnt = 0;
		
		// enthält 0 für nicht untersuchte Pixel, sonst die Anzahl der Iterationen für diesen Punkt
		int[][] results = new int[end.x][end.y];// oft zu groß, aber effizienter abzufragen
		
		for (int i=start.x; i<end.x; i++) {
			for (int j=start.y; j<end.y; j++) {
				if (results[i][j] == 0) {
					ComplexNumber c = pointToComplex(new Point(i, j));
					int result = isInSet(c);
					results[i][j] = result;
					gr.setColor(intToColor(result));
					gr.drawLine(i, j, i, j);
					
					// Punkte in der todoList liegen im untersuchten Bereich
					LinkedList<Point> todoList = new LinkedList<>();
					todoList.add(new Point(i, j));
					
					// Punkte die schon in todoList waren
					HashSet<Point> done = new HashSet<>();
					
					// enthält Punkte außerhalb des untersuchten Bereichs, die nahe am Rand liegen
					HashSet<Point> borderOutside = new HashSet<>();
					
					// wenn der Startpunkt in der Mitte des untersuchten Bereichs liegt muss erst ein Rand gefunden werden
					boolean borderFound = false;
					
					// Ziel: alle Punkte finden, die den gleichen Wert wie ergebnis liefern und zusammenhängen
					
					Point currentPoint;
					while ((currentPoint = todoList.poll()) != null) {
						if (done.contains(currentPoint)) {
							continue;
						}
						done.add(currentPoint);
						
						// zählt die Anzahl der Nachbarn im N, O, S oder W des aktellen Punktes, die nicht im untersuchten Bereich sind
						int notInSetNeighbors = 0;
						
						ArrayList<Point> newPointsOutside = new ArrayList<>();
						ArrayList<Point> newPointsInside = new ArrayList<>();
						
						for (int k=0; k<8; k++) {// in alle Richtungen nach weiteren Punkten suchen
							int xNew = currentPoint.x + directions[k][0];
							int yNew = currentPoint.y + directions[k][1];
							if (xNew >= end.x || yNew >= end.y || xNew < start.x || yNew < start.y) {// Punkt kann nicht berechnet werden
								newPointsOutside.add(new Point(xNew, yNew));
								notInSetNeighbors++;
								continue;
							}
							
							if (results[xNew][yNew] == 0) {
								ComplexNumber cNew = pointToComplex(new Point(xNew, yNew));
								int resultNew = isInSet(cNew);
								results[xNew][yNew] = resultNew;
								gr.setColor(intToColor(resultNew));
								gr.drawLine(xNew, yNew, xNew, yNew);
								
								if (results[xNew][yNew] != results[i][j]) {
									notInSetNeighbors++;
									newPointsOutside.add(new Point(xNew, yNew));
								}
								else {
									newPointsInside.add(new Point(xNew, yNew));
								}
							}
							else if (results[xNew][yNew] != results[i][j]) {
								notInSetNeighbors++;
								newPointsOutside.add(new Point(xNew, yNew));
							}
							else {
								newPointsInside.add(new Point(xNew, yNew));
							}
						}
						
						boolean borderPoint = (notInSetNeighbors == 0);
						
						if (!borderPoint) {// sonst liegt der Punkt im Zentrum des untersuchten Bereichs
							borderFound = true;
							for (Point p: newPointsOutside) {
								borderOutside.add(p);
							}
							for (Point p: newPointsInside) {
								todoList.add(p);
							}
						}
						else if (!borderFound) {
							// alle Nachbarn gehören zum untersuchten Bereich -> wähle aus Effizienzgründen nur einen aus
							// außerdem ist der aktuelle Punkt nicht am Rand -> wähle den Punkt links des aktuellen Punktes
							todoList.add(new Point(currentPoint.x-1, currentPoint.y));
						}
					}
					
					//Punkte im Inneren färben
					
					coloredCnt++;
					gr.setColor(intToColor(result));
					
					// enthält Punkte im untersuchten Bereich, die noch gefärbt werden müssen
					LinkedList<Point> toColor = new LinkedList<>();
					toColor.add(new Point(i, j));
					
					Point currentPointNew;
					while ((currentPointNew = toColor.poll()) != null) {
						if (results[currentPointNew.x][currentPointNew.y] == 0) {// nur färben, wenn noch nicht gefärbt wurde
							gr.drawLine(currentPointNew.x, currentPointNew.y, currentPointNew.x, currentPointNew.y);
						}
						results[currentPointNew.x][currentPointNew.y] = result;
						for (int k=0; k<8; k+=2) {// nur N, O, S, W durchgehen
							int xNew = currentPointNew.x + directions[k][0];
							int yNew = currentPointNew.y + directions[k][1];
							if (xNew >= end.x || yNew >= end.y || xNew < start.x || yNew < start.y) {
								continue;
							}
							Point newPoint = new Point(xNew, yNew);
							if (alreadyColored[xNew][yNew] != coloredCnt && !borderOutside.contains(newPoint)) {
								toColor.add(newPoint);
								alreadyColored[xNew][yNew] = coloredCnt;
							}
						}
					}
					
				}// end of "if ergebnisse[i][j] == 0"
			}
		}
	}
	
	/**
	 * zeichnet die gesamte Mandelbrotmenge unter Ausnutzen des Randes der Mandelbrotmenge
	 * <br>zeichnet immer den gesamten Bildschirm
	 * @param gr das Graphics-Objekt, in das gezeichnet werden soll
	 */
	private void partBordertracingFractal(Graphics gr) {
		int width = getWidth();
		int height = getHeight();
		
//		gr.setColor(Color.WHITE);
//		gr.fillRect(0, 0, getWidth(), getHeight());
		
		// enthält 0 für nicht untersuchte Pixel, 1 für Pixel in der Menge und 2 für Pixel außerhalb der Menge
		int[][] results = new int[width][height];
		for (int i=0; i<width; i++) {
			for (int j=0; j<height; j++) {
				if (results[i][j] == 0) {
					ComplexNumber c = pointToComplex(new Point(i, j));
					int result = isInSet(c);
					boolean inSet = (result == Integer.MAX_VALUE);
					results[i][j] = (inSet ? 1 : 2);
					gr.setColor(intToColor(result));
					gr.drawLine(i, j, i, j);
					
					if (inSet) {
						// enthält Punkte außerhalb der Mandelbrotmenge, deren Nachbarn untersucht werden sollen
						LinkedList<Point> todoList = new LinkedList<>();
						if (j != 0) {
							todoList.add(new Point(i, j-1));
						}
						else if (i != 0) {
							todoList.add(new Point(i, -1));
						}
						else {
							todoList.add(new Point(0, -1));
						}
						
						// enthält true, wenn der Punkt entweder in todoList steht oder schon untersucht wurde -> betrifft effektiv nur Pixel außerhalb der Menge
						// es wird auch ein Randpixel in jede Richtung gespeichert -> schonUntersucht[0][0] = (-1, -1)
						boolean[][] alreadyExamined = new boolean[width+2][height+2];
						
						// enthält Punkte außerhalb der Mandelbrotmenge, die nahe am Rand liegen
						HashSet<Point> borderOutside = new HashSet<>();
						
						// enthält Punkte in der Mandelbrotmenge, die nahe am Rand liegen
						HashSet<Point> borderInside = new HashSet<>();
						
						while (!todoList.isEmpty()) {
							Point currentPoint = todoList.poll();
							
							ArrayList<Point> newPointsOutside = new ArrayList<>();
							ArrayList<Point> newPointsInside = new ArrayList<>();
							// zählt die Anzahl der Nachbarn im N, O, S oder W des aktellen Punktes, die nicht in der Menge sind
							int notInSetNeighbors = 0;
							// der maximale Wert von inMengeNachbarn (4 in der Mitte, 3 am Rand, 2 in der Ecke)
							int potentialNeighbors = 4;
							for (int k=0; k<8; k++) {
								int xNew = currentPoint.x + directions[k][0];
								int yNew = currentPoint.y + directions[k][1];
								if (xNew >= width || yNew >= height || xNew < 0 || yNew < 0) {// Punkt kann nicht berechnet werden
									if (((xNew == width || xNew == -1) && yNew < height && yNew >= 0) // linker oder rechter Rand
											|| (yNew == height || yNew == -1) && xNew < width && xNew >= 0) {// oberer oder unterer Rand
										if (!alreadyExamined[xNew+1][yNew+1]) {
											newPointsOutside.add(new Point(xNew, yNew));
											alreadyExamined[xNew+1][yNew+1] = true;
										}
									}
									if (k%2 == 0) {
										potentialNeighbors--;
									}
									continue;
								}
								
								if (results[xNew][yNew] == 0) {
									ComplexNumber cNew = pointToComplex(new Point(xNew, yNew));
									int resultNew = isInSet(cNew);
									boolean inSetNew = (resultNew == Integer.MAX_VALUE);
									results[xNew][yNew] = (inSetNew ? 1 : 2);
									gr.setColor(intToColor(resultNew));
									gr.drawLine(xNew, yNew, xNew, yNew);
									
									if (!inSetNew) {
										if (k%2 == 0) {
											notInSetNeighbors++;
										}
										newPointsOutside.add(new Point(xNew, yNew));
									}
									else {
										newPointsInside.add(new Point(xNew, yNew));
									}
								}
								else if (results[xNew][yNew] == 2) {
									if (!alreadyExamined[xNew+1][yNew+1]) {
										newPointsOutside.add(new Point(xNew, yNew));
									}
									if (k%2 == 0) {
										notInSetNeighbors++;
									}
								}
							}
							if (notInSetNeighbors != potentialNeighbors) {
								// bei 4 (3 am Rand, 2 in der Ecke) Nachbarn den Punkt ignorieren, denn er liegt nicht am Rand der Menge
								borderOutside.add(currentPoint);
								for (Point p: newPointsOutside) {
									if (p.x < width && p.y < height && p.x >= 0 && p.y >= 0) {
										alreadyExamined[p.x+1][p.y+1] = true;
									}
									todoList.add(p);
								}
								for (Point p: newPointsInside) {
									borderInside.add(p);
								}
							}
							else {
								for (Point p: newPointsInside) {
									// Punkte in der Mandelbrotmenge sind nicht verbunden mit dem untersuchten Teil
									// kommt nur selten vor (0,01% - 0,1%) der Punkte in der Menge
									results[p.x][p.y] = 0;
								}
							}
						}
						
						//Punkte im Inneren färben
						
						gr.setColor(Color.BLACK);
						
						// enthält Punkte in der Mandelbrotmenge, die noch gefärbt werden müssen
						LinkedList<Point> toColor = new LinkedList<>();
						
						// enthält true, wenn der Punkt entweder in zuFaerben steht oder schon gefärbt wurde
						boolean[][] alreadyColored = new boolean[width][height];
						
						for (Point p: borderInside) {
							toColor.add(p);
							alreadyColored[p.x][p.y] = true;
						}
						
						while (!toColor.isEmpty()) {
							Point currentPoint = toColor.poll();
							results[currentPoint.x][currentPoint.y] = 1;
							gr.drawLine(currentPoint.x, currentPoint.y, currentPoint.x, currentPoint.y);
							for (int k=0; k<8; k+=2) {// nur N, O, S, W durchgehen
								int xNew = currentPoint.x + directions[k][0];
								int yNew = currentPoint.y + directions[k][1];
								if (xNew >= width || yNew >= height || xNew < 0 || yNew < 0) {
									continue;
								}
								Point newPoint = new Point(xNew, yNew);
								if (!alreadyColored[xNew][yNew] && !borderOutside.contains(newPoint)) {
									toColor.add(newPoint);
									alreadyColored[xNew][yNew] = true;
								}
							}
						}
					}// end of "if (inMenge)"
				}
			}
		}
	}
	
	/**
	 * in der Mitte jedes Block wird ein Punkt exakt berechnet, alle anderen werden durch Fehlerrechnung bestimmt
	 */
	public static final int blockSize = 11;
	
	public void deepZoomMandelbrotmenge(Point offset, int width, int height, Graphics gr) {
//		gr.setColor(Color.WHITE);
//		gr.fillRect(offset.x, offset.y, breite, hoehe);
		
		for (int i=offset.x+5; i<width; i+=blockSize) {
			for (int j=offset.y+5; j<height; j+=blockSize) {
				ExactComplexNumber c = pointToExactComplex(new Point(i, j));
				ArrayList<ExactComplexNumber> comparisonValueExact = isInSetExact(c);
				ArrayList<ComplexNumber> comparisonValues = (ArrayList<ComplexNumber>) comparisonValueExact.stream().map(e ->  e.toNormalComplexNumber()).collect(Collectors.toList());
				int result = comparisonValues.size() == maxInterations ? Integer.MAX_VALUE : comparisonValues.size();
				gr.setColor(intToColor(result));
				gr.drawLine(i, j, i, j);
				
				for (int k=i-5; k<=i+5; k++) {
					for (int l=j-5; l<=j+5; l++) {
						if (k == i && l == j) {
							continue;
						}
						ExactComplexNumber cNew = pointToExactComplex(new Point(k, l));
						ComplexNumber delta = ExactComplexNumber.subtract(cNew, c).toNormalComplexNumber();
						double delta_x = delta.getRe();
						double delta_y = delta.getIm();
						
						double x = delta_x;
						double y = delta_y;
						int resultNew = Integer.MAX_VALUE;
						
						ComplexNumber z_n = comparisonValues.get(0);
						M:
						for (int m=1; m<=maxInterations; m++) {
							if (comparisonValues.size() <= m) {
//								if (Integer.valueOf(1) == 1) {
//									ergebnisNeu = ergebnis;
//									break;
//								}
								BigDecimal four = new BigDecimal(4);
								MathContext mc = ExactComplexNumber.mc;
								
								BigDecimal x0 = cNew.getRe();
								BigDecimal y0 = cNew.getIm();
								
								BigDecimal bx = new BigDecimal(x);
								BigDecimal by = new BigDecimal(y);
								BigDecimal x2 = bx.pow(2, mc);
								BigDecimal y2 = by.pow(2, mc);
								for (; m<=maxInterations; m++) {
									iter++;
									by = bx.add(bx, mc).multiply(by, mc).add(y0, mc);
									bx = x2.subtract(y2, mc).add(x0, mc);
									x2 = bx.pow(2, mc);
									y2 = by.pow(2, mc);
									
									if (x2.add(y2, mc).compareTo(four) > 0) {
										resultNew = m;
										break M;
									}
								}
								break M;
							}
							iter++;
							double xz = z_n.getRe();
							double yz = z_n.getIm();
							
							double lastX = x;
							x = x * (x+xz+xz) - y * (y+yz+yz) + delta_x;
							y = 2 * (xz*y + lastX * (yz+y))+ delta_y;
							
							z_n = comparisonValues.get(m);
							double sumX = x+z_n.getRe();
							double sumY = y+z_n.getIm();
							if (sumX * sumX + sumY * sumY > 4) {
								resultNew = m;
								break;
							}
						}
						
						gr.setColor(intToColor(resultNew));
						gr.drawLine(k, l, k, l);
					}
				}
			}
		}
	}
	
	private static long iter = 0;
	
	/**
	 * prüft ob eine komplexe Zahl c in der Mandelbrotmenge/Juliamenge liegt
	 * @param c eine komplexe Zahl
	 * @return Integer.MAX_VALUE, wenn c in der MB/J-Menge liegt, sonst die Zahl der Iterationen >= 1 bis c divergiert
	 */
	private int isInSet(ComplexNumber c) {
		switch (fractalType) {
		case MandelbrotSet: {
			double x0 = c.getRe();
			double y0 = c.getIm();
			
			double x = parameter.getRe();
			double y = parameter.getIm();
			double x2 = x*x;
			double y2 = y*y;
			for (int i=1; i<=maxInterations; i++) {
				iter++;
				y = (x+x)*y + y0;
				x = x2 - y2 + x0;
				x2 = x*x;
				y2 = y*y;
				
				if (x2 + y2 > 4) {
					return i;// - Math.log(Math.log(x2 + y2) / Math.log(4)) / Math.log(2);
				}
			}
			break;
		}
		case JuliaSet: {
			double x0 = parameter.getRe();
			double y0 = parameter.getIm();
			
			double x = c.getRe();
			double y = c.getIm();
			double x2 = x*x;
			double y2 = y*y;
			for (int i=1; i<=maxInterations; i++) {
				iter++;
				y = (x+x)*y + y0;
				x = x2 - y2 + x0;
				x2 = x*x;
				y2 = y*y;
				
				if (x2 + y2 > 4) {
					return i;// - Math.log(Math.log(x2 + y2) / Math.log(4)) / Math.log(2);
				}
			}
			break;
		}
		case BurningShipFractal: {
			double x0 = c.getRe();
			double y0 = c.getIm();
			
			double x = parameter.getRe();
			double y = parameter.getIm();
			double x2 = x*x;
			double y2 = y*y;
			for (int i=1; i<=maxInterations; i++) {
				iter++;
				y = Math.abs((x+x)*y) + y0;
				x = Math.abs(x2 - y2) + x0;
				x2 = x*x;
				y2 = y*y;
				
				if (x2 + y2 > 4) {
					return i;// - Math.log(Math.log(x2 + y2) / Math.log(4)) / Math.log(2);
				}
			}
			break;
		}
		case Tricorn: {
			double x0 = c.getRe();
			double y0 = c.getIm();
			
			double x = parameter.getRe();
			double y = parameter.getIm();
			double x2 = x*x;
			double y2 = y*y;
			for (int i=1; i<=maxInterations; i++) {
				iter++;
				y = -(x+x)*y + y0;
				x = x2 - y2 + x0;
				x2 = x*x;
				y2 = y*y;
				
				if (x2 + y2 > 4) {
					return i;// - Math.log(Math.log(x2 + y2) / Math.log(4)) / Math.log(2);
				}
			}
			break;
		}
		}

		return Integer.MAX_VALUE;
	}
	
	private ArrayList<ExactComplexNumber> isInSetExact(ExactComplexNumber c) {
		ArrayList<ExactComplexNumber> gradiant = new ArrayList<>();
		BigDecimal four = new BigDecimal(4);
		
		switch (fractalType) {
		case MandelbrotSet: {
			ExactComplexNumber z0 = parameter.toExactComplexNumber();
			
			MathContext mc = ExactComplexNumber.mc;
			
			BigDecimal x0 = c.getRe();
			BigDecimal y0 = c.getIm();
			
			BigDecimal x = z0.getRe();
			BigDecimal y = z0.getIm();
			BigDecimal x2 = x.pow(2, mc);
			BigDecimal y2 = y.pow(2, mc);
			for (int i=1; i<=maxInterations; i++) {
				iter++;
				y = x.add(x, mc).multiply(y, mc).add(y0, mc);
				x = x2.subtract(y2, mc).add(x0, mc);
				x2 = x.pow(2, mc);
				y2 = y.pow(2, mc);
				
				gradiant.add(new ExactComplexNumber(x, y));
				if (x2.add(y2, mc).compareTo(four) > 0) {
					break;
				}
			}
		}
		case JuliaSet: {
			ExactComplexNumber z0 = parameter.toExactComplexNumber();
			
			MathContext mc = ExactComplexNumber.mc;
			
			BigDecimal x0 = z0.getRe();
			BigDecimal y0 = z0.getIm();
			
			BigDecimal x = c.getRe();
			BigDecimal y = c.getIm();
			BigDecimal x2 = x.pow(2, mc);
			BigDecimal y2 = y.pow(2, mc);
			for (int i=1; i<=maxInterations; i++) {
				iter++;
				y = x.add(x, mc).multiply(y, mc).add(y0, mc);
				x = x2.subtract(y2, mc).add(x0, mc);
				x2 = x.pow(2, mc);
				y2 = y.pow(2, mc);
				
				gradiant.add(new ExactComplexNumber(x, y));
				if (x2.add(y2, mc).compareTo(four) > 0) {
					break;
				}
			}
		}
		default:
			break;
		}
		return gradiant;
	}
	
	/**
	 * die letztberechnete Skalierung
	 */
	private double lastScaling = 0;
	
	/**
	 * die letztberechnete Skalierung / 450
	 */
	private double lastScalingOver450 = 0;
	
	/**
	 * errechnet aus der Position eines Pixels seinen komplexen Wert
	 * @param p ein Pixel
	 * @return den Wert, dem das Pixel entspricht
	 */
	public ComplexNumber pointToComplex(Point p) {
		if (lastScaling != scaling) {
			lastScaling = scaling;
			lastScalingOver450 = scaling/450;
		}
		return new ComplexNumber(lastScalingOver450 * p.x + fixpoint.getRe(), lastScalingOver450 * (-p.y) + fixpoint.getIm());
	}
	
	public ExactComplexNumber pointToExactComplex(Point p) {
		if (lastScaling != scaling) {
			lastScaling = scaling;
			lastScalingOver450 = scaling/450;
		}
		// Wichtig: Genauigkeitsverlust tritt nicht bei skalierung * p.x auf
		//			sondern bei der Addition des Fixpunkts (dieser hat eine hohe Genauigkeit)
		return new ExactComplexNumber(new BigDecimal(lastScalingOver450 * p.x).add(new BigDecimal(fixpoint.getRe())), new BigDecimal(lastScalingOver450 * -p.y).add(new BigDecimal(fixpoint.getIm())));
	}
	
	private static int colorPalette = 1;

	private static boolean interpolateColor = false;
	
	private static Color[] colors = new Color[colorPalette == 0 ? 768 : 256];
	
	static {
		for (int i=0; i<(colorPalette == 0 ? 768 : 256); i++) {
			if (colorPalette == 0) {
				int t = i;//256 * 3
				if (t < 256) {
					colors[i] = new Color(t, t, 255);
				}
				else if (t < 255+256) {
					t %= 256;
					colors[i] = new Color(255, 255-t/2, 255-t);
				}
				else {
					t %= 256;
					colors[i] = new Color(255-t, 127-t/2, t);
				}
			}
			else if (colorPalette == 1) {
				double t = (double)i/255;
				// Bernstein-Polynom 4.Grades
				colors[i] = new Color((int) (9 * (1-t) * t * t * t * 255), 
									  (int) (15 * (1-t) * (1-t) * t * t * 255), 
									  (int) (8.5 * (1-t) * (1-t) * (1-t) * t * 255));
			}
		}
	}
	
	public Color intToColor(int i) {
		if (i == Integer.MAX_VALUE)
			return Color.BLACK;
		
		i %= (colorPalette == 0 ? 767 : 255);
		
		if (interpolateColor) {
			return interpolateColor(colors[(int) Math.floor(i)], colors[(int) Math.floor(i) + 1], i%1);
		}
		else {
			return colors[i];
		}
	}
	
	private Color interpolateColor(Color c1, Color c2, double fraction) {
		int[] diff = new int[] {c2.getRed() - c1.getRed(), c2.getGreen() - c1.getGreen(), c2.getBlue() - c1.getBlue()};
		int[] diffWeighted = new int[] {(int) Math.round(diff[0] * fraction), (int) Math.round(diff[1] * fraction), (int) Math.round(diff[2] * fraction)};
		return new Color(c1.getRed() + diffWeighted[0], c1.getGreen() + diffWeighted[1], c1.getBlue() + diffWeighted[2]);
	}
	
	public BufferedImage toImage() {
		int width = getWidth();
		int height = getHeight();
		
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics gr = image.getGraphics();
		
//		deepZoomMandelbrotmenge(new Point(0, 0), breite-1, hoehe-1, gr);
		if (scaling < 0.51) {
			bordertracingFractal(new Point(0, 0), new Point(width, height), gr);
		}
		else {
			partBordertracingFractal(gr);
		}
		
		return image;
	}
	
	public ComplexNumber getFixpoint() {
		return fixpoint;
	}
	
	public void setFixpoint(ComplexNumber fixpunkt) {
		this.fixpoint = fixpunkt;
	}
	
	public double getScaling() {
		return scaling;
	}
	
	public void setScaling(double skalierung) {
		this.scaling = skalierung;
	}
	
	public int getMaxInterations() {
		return maxInterations;
	}
	
	public void setMaxInterations(int iterationen) {
		this.maxInterations = iterationen;
	}
	
	public FractalType getFractalType() {
		return fractalType;
	}
	
	public void setFractalType(FractalType mandelbrotmenge) {
		this.fractalType = mandelbrotmenge;
	}
	
	public ComplexNumber getParameter() {
		return parameter;
	}
	
	public void setParameter(ComplexNumber parameter) {
		this.parameter = parameter;
	}
	
	public boolean isBorderTracing() {
		return borderTracing;
	}
	
	public void setBorderTracing(boolean borderTracing) {
		this.borderTracing = borderTracing;
	}
	
}
