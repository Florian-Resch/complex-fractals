package mandelbrotmenge;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
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
	 * die Zahl, die das Pixel (0,0) bekommt
	 */
	private ComplexNumber fixpunkt = new ComplexNumber(-2, 1);//-1.424455409425403, 0.12712914901631397);
	
	/**
	 * die Zahl, der 450 Pixel entsprechen
	 */
	private double skalierung = 1;//1.4210854715202004e-14;
	
	private int maxIterationen = 1000;
	
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
	}
	
	public void reset() {
		fixpunkt = new ComplexNumber(-2, 1);
		skalierung = 1;
		
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
	 * zoomt näher heran
	 * @param mausPos die Position der Maus beim Zoomen
	 */
	public void reinzoomen(Point mausPos) {
		if (skalierung/2 != 0.0) {
			Point neuerFixpunkt = new Point(mausPos.x/2, mausPos.y/2);
			fixpunkt = pointToComplex(neuerFixpunkt);
			skalierung /= 2;
			
			drawMandelbrotmengeTeil(true, 0);
		}
	}
	
	/**
	 * zoomt weiter heraus
	 * @param mausPos die Position der Maus beim Zoomen
	 */
	public void rauszoomen(Point mausPos) {
		if (skalierung*2 != Double.POSITIVE_INFINITY) {
			Point neuerFixpunkt = new Point(-mausPos.x, -mausPos.y);
			fixpunkt = pointToComplex(neuerFixpunkt);
			skalierung *= 2;
			
			drawMandelbrotmengeTeil(true, 0);
		}
	}
	
	/**
	 * bewegt den sichtbaren Bereich
	 * @param offset wenn x/y > 0, dann bildet sich rechts/unten ein Rand
	 */
	public void bewegen(Point offset) {
		fixpunkt = pointToComplex(offset);
		
		Graphics gr = getGraphics();
		gr.copyArea(0, 0, getWidth(), getHeight(), -offset.x, -offset.y);
		
		if (offset.getX() != 0) {
			drawMandelbrotmengeTeil(true, -offset.x);
		}
		if (offset.getY() != 0) {
			drawMandelbrotmengeTeil(false, -offset.y);
		}
	}
	
	/**
	 * zeichnet die Menge mit dem gewählten Parameter
	 * <br>Mandelbrot: x² + c mit x0 = konst. und c variabel
	 * <br>Julia:      x² + c mit c = konst. und x0 variabel
	 * @param parameter der Wert von x0; standardmäßig 0
	 */
	public void drawMandelbrotJuliamenge() {
		drawMandelbrotmengeTeil(true, 0);
	}
	
	/**
	 * zeichnet einen Teil der Mandelbrotmenge (bei wert = 0 wird alles gezeichnet)
	 * @param xRichtung true, wenn der zu zeichnende Streifen links oder rechts liegt; false, wenn er oben oder unten liegt
	 * @param wert positiv, wenn der Streifen oben oder links liegt; kann auch 0 sein, dann wir alles gezeichnet
	 */
	private void drawMandelbrotmengeTeil(boolean xRichtung, int wert) {
		long time = System.nanoTime();
		/*if (wert == 0) {
			deepZoomMandelbrotmenge(new Point(0, 0), getWidth()-1, getHeight()-1, getGraphics());
		}
		else */
		if (wert == 0) {
			if (skalierung < 0.51 && borderTracing) {
				int breite = getWidth();
				int hoehe = getHeight();
				kantenverfolgungMandelbrotmenge(new Point(0, 0), new Point(breite, hoehe), getGraphics());
			}
			else {
				teilkantenverfolgungMandelbrotmenge(getGraphics());
			}
		}
		else {
			int startI = 0;
			int startJ = 0;
			int breite = getWidth();
			int hoehe = getHeight();
			
			if (xRichtung) {
				if (wert > 0) {
					breite = wert;
				}
				else if (wert < 0) {
					startI = breite+wert;
				}
			}
			else {
				if (wert > 0) {
					hoehe = wert;
				}
				else if (wert < 0) {
					startJ = hoehe+wert;
				}
			}
			kantenverfolgungMandelbrotmenge(new Point(startI, startJ), new Point(breite, hoehe), getGraphics());
		}
		System.out.println((System.nanoTime()-time)/1000000 + ": " + iter);
		iter = 0;
		
		
	}
	
	/**
	 * enthält Offsets für die Richtungen N, NO, O, SO, S, SW, W, NW
	 */
	private static int[][] richtungen = {{0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};
	
	/**
	 * zeichnet die gesamte Mandelbrotmenge unter Ausnutzen des Randes der Gebiete
	 * @param anfang der Punkt, um den das Pixel (0,0) verschoben werden soll
	 * @param ende das Pixel nach dem letzten Pixel, das gezeichnet werden soll
	 * @param gr das Graphics-Objekt, in das gezeichnet werden soll
	 */
	private void kantenverfolgungMandelbrotmenge(Point anfang, Point ende, Graphics gr) {
//		gr.setColor(Color.WHITE);
//		gr.fillRect(offset.x, offset.y, breite, hoehe);
		
		// statt immer wieder boolean-Arrays zu initialisieren, wird in dieses Array mit fortlaufender Nummer geschrieben
		int[][] schonGefaerbt = new int[ende.x][ende.y];// oft zu groß, aber effizienter abzufragen
		int gefaerbtCnt = 0;
		
		// enthält 0 für nicht untersuchte Pixel, sonst die Anzahl der Iterationen für diesen Punkt
		int[][] ergebnisse = new int[ende.x][ende.y];// oft zu groß, aber effizienter abzufragen
		
		for (int i=anfang.x; i<ende.x; i++) {
			for (int j=anfang.y; j<ende.y; j++) {
				if (ergebnisse[i][j] == 0) {
					ComplexNumber c = pointToComplex(new Point(i, j));
					int ergebnis = isInSet(c);
					ergebnisse[i][j] = ergebnis;
					gr.setColor(intToColor(ergebnis));
					gr.drawLine(i, j, i, j);
					
					// Punkte in der todoList liegen im untersuchten Bereich
					LinkedList<Point> todoList = new LinkedList<>();
					todoList.add(new Point(i, j));
					
					// Punkte die schon in todoList waren
					HashSet<Point> done = new HashSet<>();
					
					// enthält Punkte außerhalb des untersuchten Bereichs, die nahe am Rand liegen
					HashSet<Point> randAussen = new HashSet<>();
					
					// wenn der Startpunkt in der Mitte des untersuchten Bereichs liegt muss erst ein Rand gefunden werden
					boolean randGefunden = false;
					
					// Ziel: alle Punkte finden, die den gleichen Wert wie ergebnis liefern und zusammenhängen
					
					Point aktuellerPunkt;
					while ((aktuellerPunkt = todoList.poll()) != null) {
						if (done.contains(aktuellerPunkt)) {
							continue;
						}
						done.add(aktuellerPunkt);
						
						// zählt die Anzahl der Nachbarn im N, O, S oder W des aktellen Punktes, die nicht im untersuchten Bereich sind
						int nichtInMengeNachbarn = 0;
						
						ArrayList<Point> neuePunkteAussen = new ArrayList<>();
						ArrayList<Point> neuePunkteInnen = new ArrayList<>();
						
						for (int k=0; k<8; k++) {// in alle Richtungen nach weiteren Punkten suchen
							int xNeu = aktuellerPunkt.x + richtungen[k][0];
							int yNeu = aktuellerPunkt.y + richtungen[k][1];
							if (xNeu >= ende.x || yNeu >= ende.y || xNeu < anfang.x || yNeu < anfang.y) {// Punkt kann nicht berechnet werden
								neuePunkteAussen.add(new Point(xNeu, yNeu));
								nichtInMengeNachbarn++;
								continue;
							}
							
							if (ergebnisse[xNeu][yNeu] == 0) {
								ComplexNumber cNeu = pointToComplex(new Point(xNeu, yNeu));
								int ergebnisNeu = isInSet(cNeu);
								ergebnisse[xNeu][yNeu] = ergebnisNeu;
								gr.setColor(intToColor(ergebnisNeu));
								gr.drawLine(xNeu, yNeu, xNeu, yNeu);
								
								if (ergebnisse[xNeu][yNeu] != ergebnisse[i][j]) {
									nichtInMengeNachbarn++;
									neuePunkteAussen.add(new Point(xNeu, yNeu));
								}
								else {
									neuePunkteInnen.add(new Point(xNeu, yNeu));
								}
							}
							else if (ergebnisse[xNeu][yNeu] != ergebnisse[i][j]) {
								nichtInMengeNachbarn++;
								neuePunkteAussen.add(new Point(xNeu, yNeu));
							}
							else {
								neuePunkteInnen.add(new Point(xNeu, yNeu));
							}
						}
						
						boolean randFeld = (nichtInMengeNachbarn == 0);
						
						if (!randFeld) {// sonst liegt der Punkt im Zentrum des untersuchten Bereichs
							randGefunden = true;
							for (Point p: neuePunkteAussen) {
								randAussen.add(p);
							}
							for (Point p: neuePunkteInnen) {
								todoList.add(p);
							}
						}
						else if (!randGefunden) {
							// alle Nachbarn gehören zum untersuchten Bereich -> wähle aus Effizienzgründen nur einen aus
							// außerdem ist der aktuelle Punkt nicht am Rand -> wähle den Punkt links des aktuellen Punktes
							todoList.add(new Point(aktuellerPunkt.x-1, aktuellerPunkt.y));
						}
					}
					
					//Punkte im Inneren färben
					
					gefaerbtCnt++;
					gr.setColor(intToColor(ergebnis));
					
					// enthält Punkte im untersuchten Bereich, die noch gefärbt werden müssen
					LinkedList<Point> zuFearben = new LinkedList<>();
					zuFearben.add(new Point(i, j));
					
					Point aktuellerPunktNeu;
					while ((aktuellerPunktNeu = zuFearben.poll()) != null) {
						if (ergebnisse[aktuellerPunktNeu.x][aktuellerPunktNeu.y] == 0) {// nur färben, wenn noch nicht gefärbt wurde
							gr.drawLine(aktuellerPunktNeu.x, aktuellerPunktNeu.y, aktuellerPunktNeu.x, aktuellerPunktNeu.y);
						}
						ergebnisse[aktuellerPunktNeu.x][aktuellerPunktNeu.y] = ergebnis;
						for (int k=0; k<8; k+=2) {// nur N, O, S, W durchgehen
							int xNeu = aktuellerPunktNeu.x + richtungen[k][0];
							int yNeu = aktuellerPunktNeu.y + richtungen[k][1];
							if (xNeu >= ende.x || yNeu >= ende.y || xNeu < anfang.x || yNeu < anfang.y) {
								continue;
							}
							Point neuerPunkt = new Point(xNeu, yNeu);
							if (schonGefaerbt[xNeu][yNeu] != gefaerbtCnt && !randAussen.contains(neuerPunkt)) {
								zuFearben.add(neuerPunkt);
								schonGefaerbt[xNeu][yNeu] = gefaerbtCnt;
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
	private void teilkantenverfolgungMandelbrotmenge(Graphics gr) {
		int breite = getWidth();
		int hoehe = getHeight();
		
//		gr.setColor(Color.WHITE);
//		gr.fillRect(0, 0, getWidth(), getHeight());
		
		// enthält 0 für nicht untersuchte Pixel, 1 für Pixel in der Menge und 2 für Pixel außerhalb der Menge
		int[][] ergebnisse = new int[breite][hoehe];
		for (int i=0; i<breite; i++) {
			for (int j=0; j<hoehe; j++) {
				if (ergebnisse[i][j] == 0) {
					ComplexNumber c = pointToComplex(new Point(i, j));
					int ergebnis = isInSet(c);
					boolean inMenge = (ergebnis == Integer.MAX_VALUE);
					ergebnisse[i][j] = (inMenge ? 1 : 2);
					gr.setColor(intToColor(ergebnis));
					gr.drawLine(i, j, i, j);
					
					if (inMenge) {
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
						boolean[][] schonUntersucht = new boolean[breite+2][hoehe+2];
						
						// enthält Punkte außerhalb der Mandelbrotmenge, die nahe am Rand liegen
						HashSet<Point> randAussen = new HashSet<>();
						
						// enthält Punkte in der Mandelbrotmenge, die nahe am Rand liegen
						HashSet<Point> randInnen = new HashSet<>();
						
						while (!todoList.isEmpty()) {
							Point aktuellerPunkt = todoList.poll();
							
							ArrayList<Point> neuePunkteAussen = new ArrayList<>();
							ArrayList<Point> neuePunkteInnen = new ArrayList<>();
							// zählt die Anzahl der Nachbarn im N, O, S oder W des aktellen Punktes, die nicht in der Menge sind
							int nichtInMengeNachbarn = 0;
							// der maximale Wert von inMengeNachbarn (4 in der Mitte, 3 am Rand, 2 in der Ecke)
							int moeglicheNachbarn = 4;
							for (int k=0; k<8; k++) {
								int xNeu = aktuellerPunkt.x + richtungen[k][0];
								int yNeu = aktuellerPunkt.y + richtungen[k][1];
								if (xNeu >= breite || yNeu >= hoehe || xNeu < 0 || yNeu < 0) {// Punkt kann nicht berechnet werden
									if (((xNeu == breite || xNeu == -1) && yNeu < hoehe && yNeu >= 0) // linker oder rechter Rand
											|| (yNeu == hoehe || yNeu == -1) && xNeu < breite && xNeu >= 0) {// oberer oder unterer Rand
										if (!schonUntersucht[xNeu+1][yNeu+1]) {
											neuePunkteAussen.add(new Point(xNeu, yNeu));
											schonUntersucht[xNeu+1][yNeu+1] = true;
										}
									}
									if (k%2 == 0) {
										moeglicheNachbarn--;
									}
									continue;
								}
								
								if (ergebnisse[xNeu][yNeu] == 0) {
									ComplexNumber cNeu = pointToComplex(new Point(xNeu, yNeu));
									int ergebnisNeu = isInSet(cNeu);
									boolean inMengeNeu = (ergebnisNeu == Integer.MAX_VALUE);
									ergebnisse[xNeu][yNeu] = (inMengeNeu ? 1 : 2);
									gr.setColor(intToColor(ergebnisNeu));
									gr.drawLine(xNeu, yNeu, xNeu, yNeu);
									
									if (!inMengeNeu) {
										if (k%2 == 0) {
											nichtInMengeNachbarn++;
										}
										neuePunkteAussen.add(new Point(xNeu, yNeu));
									}
									else {
										neuePunkteInnen.add(new Point(xNeu, yNeu));
									}
								}
								else if (ergebnisse[xNeu][yNeu] == 2) {
									if (!schonUntersucht[xNeu+1][yNeu+1]) {
										neuePunkteAussen.add(new Point(xNeu, yNeu));
									}
									if (k%2 == 0) {
										nichtInMengeNachbarn++;
									}
								}
							}
							if (nichtInMengeNachbarn != moeglicheNachbarn) {
								// bei 4 (3 am Rand, 2 in der Ecke) Nachbarn den Punkt ignorieren, denn er liegt nicht am Rand der Menge
								randAussen.add(aktuellerPunkt);
								for (Point p: neuePunkteAussen) {
									if (p.x < breite && p.y < hoehe && p.x >= 0 && p.y >= 0) {
										schonUntersucht[p.x+1][p.y+1] = true;
									}
									todoList.add(p);
								}
								for (Point p: neuePunkteInnen) {
									randInnen.add(p);
								}
							}
							else {
								for (Point p: neuePunkteInnen) {
									// Punkte in der Mandelbrotmenge sind nicht verbunden mit dem untersuchten Teil
									// kommt nur selten vor (0,01% - 0,1%) der Punkte in der Menge
									ergebnisse[p.x][p.y] = 0;
								}
							}
						}
						
						//Punkte im Inneren färben
						
						gr.setColor(Color.BLACK);
						
						// enthält Punkte in der Mandelbrotmenge, die noch gefärbt werden müssen
						LinkedList<Point> zuFearben = new LinkedList<>();
						
						// enthält true, wenn der Punkt entweder in zuFaerben steht oder schon gefärbt wurde
						boolean[][] schonGefaerbt = new boolean[breite][hoehe];
						
						for (Point p: randInnen) {
							zuFearben.add(p);
							schonGefaerbt[p.x][p.y] = true;
						}
						
						while (!zuFearben.isEmpty()) {
							Point aktuellerPunkt = zuFearben.poll();
							ergebnisse[aktuellerPunkt.x][aktuellerPunkt.y] = 1;
							gr.drawLine(aktuellerPunkt.x, aktuellerPunkt.y, aktuellerPunkt.x, aktuellerPunkt.y);
							for (int k=0; k<8; k+=2) {// nur N, O, S, W durchgehen
								int xNeu = aktuellerPunkt.x + richtungen[k][0];
								int yNeu = aktuellerPunkt.y + richtungen[k][1];
								if (xNeu >= breite || yNeu >= hoehe || xNeu < 0 || yNeu < 0) {
									continue;
								}
								Point neuerPunkt = new Point(xNeu, yNeu);
								if (!schonGefaerbt[xNeu][yNeu] && !randAussen.contains(neuerPunkt)) {
									zuFearben.add(neuerPunkt);
									schonGefaerbt[xNeu][yNeu] = true;
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
	
	public void deepZoomMandelbrotmenge(Point offset, int breite, int hoehe, Graphics gr) {
//		gr.setColor(Color.WHITE);
//		gr.fillRect(offset.x, offset.y, breite, hoehe);
		
		for (int i=offset.x+5; i<breite; i+=blockSize) {
			for (int j=offset.y+5; j<hoehe; j+=blockSize) {
				ExactComplexNumber c = pointToExactComplex(new Point(i, j));
				ArrayList<ExactComplexNumber> vergleichswerteExakt = isInSetExact(c);
				ArrayList<ComplexNumber> vergleichswerte = (ArrayList<ComplexNumber>) vergleichswerteExakt.stream().map(e ->  e.toNormalComplexNumber()).collect(Collectors.toList());
				int ergebnis = vergleichswerte.size() == maxIterationen ? Integer.MAX_VALUE : vergleichswerte.size();
				gr.setColor(intToColor(ergebnis));
				gr.drawLine(i, j, i, j);
				
				for (int k=i-5; k<=i+5; k++) {
					for (int l=j-5; l<=j+5; l++) {
						if (k == i && l == j) {
							continue;
						}
						ExactComplexNumber cNeu = pointToExactComplex(new Point(k, l));
						ComplexNumber delta = ExactComplexNumber.subtract(cNeu, c).toNormalComplexNumber();
						double delta_x = delta.getRe();
						double delta_y = delta.getIm();
						
						double x = delta_x;
						double y = delta_y;
						int ergebnisNeu = Integer.MAX_VALUE;
						
						ComplexNumber z_n = vergleichswerte.get(0);
						M:
						for (int m=1; m<=maxIterationen; m++) {
							if (vergleichswerte.size() <= m) {
//								if (Integer.valueOf(1) == 1) {
//									ergebnisNeu = ergebnis;
//									break;
//								}
								BigDecimal four = new BigDecimal(4);
								MathContext mc = ExactComplexNumber.mc;
								
								BigDecimal x0 = cNeu.getRe();
								BigDecimal y0 = cNeu.getIm();
								
								BigDecimal bx = new BigDecimal(x);
								BigDecimal by = new BigDecimal(y);
								BigDecimal x2 = bx.pow(2, mc);
								BigDecimal y2 = by.pow(2, mc);
								for (; m<=maxIterationen; m++) {
									iter++;
									by = bx.add(bx, mc).multiply(by, mc).add(y0, mc);
									bx = x2.subtract(y2, mc).add(x0, mc);
									x2 = bx.pow(2, mc);
									y2 = by.pow(2, mc);
									
									if (x2.add(y2, mc).compareTo(four) > 0) {
										ergebnisNeu = m;
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
							
							z_n = vergleichswerte.get(m);
							double summe_x = x+z_n.getRe();
							double summe_y = y+z_n.getIm();
							if (summe_x * summe_x + summe_y * summe_y > 4) {
								ergebnisNeu = m;
								break;
							}
						}
						
						gr.setColor(intToColor(ergebnisNeu));
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
			for (int i=1; i<=maxIterationen; i++) {
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
			for (int i=1; i<=maxIterationen; i++) {
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
			for (int i=1; i<=maxIterationen; i++) {
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
			for (int i=1; i<=maxIterationen; i++) {
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
		ArrayList<ExactComplexNumber> verlauf = new ArrayList<>();
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
			for (int i=1; i<=maxIterationen; i++) {
				iter++;
				y = x.add(x, mc).multiply(y, mc).add(y0, mc);
				x = x2.subtract(y2, mc).add(x0, mc);
				x2 = x.pow(2, mc);
				y2 = y.pow(2, mc);
				
				verlauf.add(new ExactComplexNumber(x, y));
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
			for (int i=1; i<=maxIterationen; i++) {
				iter++;
				y = x.add(x, mc).multiply(y, mc).add(y0, mc);
				x = x2.subtract(y2, mc).add(x0, mc);
				x2 = x.pow(2, mc);
				y2 = y.pow(2, mc);
				
				verlauf.add(new ExactComplexNumber(x, y));
				if (x2.add(y2, mc).compareTo(four) > 0) {
					break;
				}
			}
		}		
		case BurningShipFractal: {
			break;
		}
			default:
				break;
		}
		return verlauf;
	}
	
	/**
	 * die letztberechnete Skalierung
	 */
	private double lastSkalierung = 0;
	
	/**
	 * die letztberechnete Skalierung / 450
	 */
	private double lastSkalierungDurch450 = 0;
	
	/**
	 * errechnet aus der Position eines Pixels seinen komplexen Wert
	 * @param p ein Pixel
	 * @return den Wert, dem das Pixel entspricht
	 */
	public ComplexNumber pointToComplex(Point p) {
		if (lastSkalierung != skalierung) {
			lastSkalierung = skalierung;
			lastSkalierungDurch450 = skalierung/450;
		}
		return new ComplexNumber(lastSkalierungDurch450 * p.x + fixpunkt.getRe(), lastSkalierungDurch450 * (-p.y) + fixpunkt.getIm());
	}
	
	public ExactComplexNumber pointToExactComplex(Point p) {
		if (lastSkalierung != skalierung) {
			lastSkalierung = skalierung;
			lastSkalierungDurch450 = skalierung/450;
		}
		// Wichtig: Genauigkeitsverlust tritt nicht bei skalierung * p.x auf
		//			sondern bei der Addition des Fixpunkts (dieser hat eine hohe Genauigkeit)
		return new ExactComplexNumber(new BigDecimal(lastSkalierungDurch450 * p.x).add(new BigDecimal(fixpunkt.getRe())), new BigDecimal(lastSkalierungDurch450 * -p.y).add(new BigDecimal(fixpunkt.getIm())));
	}
	
	private static int farbenPalette = 1;
	
	private static Color[] farben = new Color[farbenPalette == 0 ? 768 : 256];
	
	static {
		for (int i=0; i<(farbenPalette == 0 ? 768 : 256); i++) {
			if (farbenPalette == 0) {
				int t = i;//256 * 3
				if (t < 256) {
					farben[i] = new Color(t, t, 255);
				}
				else if (t < 255+256) {
					t %= 256;
					farben[i] = new Color(255, 255-t/2, 255-t);
				}
				else {
					t %= 256;
					farben[i] = new Color(255-t, 127-t/2, t);
				}
			}
			else {
				double t = (double)i/255;
				// Bernstein-Polynom 4.Grades
				farben[i] = new Color((int) (9 * (1-t) * t * t * t * 255), 
									  (int) (15 * (1-t) * (1-t) * t * t * 255), 
									  (int) (8.5 * (1-t) * (1-t) * (1-t) * t * 255));
			}
		}
	}
	
	public Color intToColor(int i) {
		if (i == Integer.MAX_VALUE)
			return Color.BLACK;
		
		i %= (farbenPalette == 0 ? 767 : 255);
		
		return farben[i];
		//return interpolateColor(farben[(int) Math.floor(i)], farben[(int) Math.floor(i) + 1], i%1);
	}
	
	@SuppressWarnings("unused")
	private Color interpolateColor(Color c1, Color c2, double fraction) {
		int[] diff = new int[] {c2.getRed() - c1.getRed(), c2.getGreen() - c1.getGreen(), c2.getBlue() - c1.getBlue()};
		int[] diffWeighted = new int[] {(int) Math.round(diff[0] * fraction), (int) Math.round(diff[1] * fraction), (int) Math.round(diff[2] * fraction)};
		return new Color(c1.getRed() + diffWeighted[0], c1.getGreen() + diffWeighted[1], c1.getBlue() + diffWeighted[2]);
	}
	
	public BufferedImage toImage() {
		int breite = getWidth();
		int hoehe = getHeight();
		
		BufferedImage image = new BufferedImage(breite, hoehe, BufferedImage.TYPE_INT_RGB);
		Graphics gr = image.getGraphics();
		
//		deepZoomMandelbrotmenge(new Point(0, 0), breite-1, hoehe-1, gr);
		if (skalierung < 0.51) {
			kantenverfolgungMandelbrotmenge(new Point(0, 0), new Point(breite, hoehe), gr);
		}
		else {
			teilkantenverfolgungMandelbrotmenge(gr);
		}
		
		return image;
	}
	
	public ComplexNumber getFixpunkt() {
		return fixpunkt;
	}
	
	public void setFixpunkt(ComplexNumber fixpunkt) {
		this.fixpunkt = fixpunkt;
	}
	
	public double getSkalierung() {
		return skalierung;
	}
	
	public void setSkalierung(double skalierung) {
		this.skalierung = skalierung;
	}
	
	public int getMaxIterationen() {
		return maxIterationen;
	}
	
	public void setMaxIterationen(int iterationen) {
		this.maxIterationen = iterationen;
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
