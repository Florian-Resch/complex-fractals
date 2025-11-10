package mandelbrotmenge;

public class ComplexNumber {
	
	/**
	 * real part
	 */
	private double re;
	
	/**
	 * imaginary part
	 */
	private double im;
	
	/**
	 * initializes a new complex number with real = imaginary = 0
	 */
	public ComplexNumber() {
		re = 0;
		im = 0;
	}
	
	/**
	 * initializes a new complex number with real = re and imaginary = im
	 */
	public ComplexNumber(double re, double im) {
		this.re = re;
		this.im = im;
	}
	
	public double getRe() {
		return re;
	}
	
	public void setRe(double re) {
		this.re = re;
	}
	
	public double getIm() {
		return im;
	}
	
	public void setIm(double im) {
		this.im = im;
	}
	
	public ExactComplexNumber toExactComplexNumber() {
		return new ExactComplexNumber(re, im);
	}
	
	/**
	 * @param a = x + yi
	 * @return _a = x - yi
	 */
	public static ComplexNumber conjugate(ComplexNumber a) {
		return new ComplexNumber(a.getRe(), -a.getIm());
	}
	
	/**
	 * @param a = x + yi
	 * @return -a = - x - yi
	 */
	public static ComplexNumber invert(ComplexNumber a) {
		return new ComplexNumber(-a.getRe(), -a.getIm());
	}
	
	/**
	 * @param a = x + yi
	 * @return |a| = sqrt(x*x + y*y)
	 */
	public static double abs(ComplexNumber a) {
		double x = a.getRe();
		double y = a.getIm();
		return Math.sqrt(x*x + y*y);
	}
	
	/**
	 * @param a = x + yi
	 * @return |a|^2 = x*x + y*y
	 */
	public static double absSquare(ComplexNumber a) {
		double x = a.getRe();
		double y = a.getIm();
		return x*x + y*y;
	}
	
	/**
	 * @param a = x + yi
	 * @param b = u + vi
	 * @return a + b = (x + u) + (y + v)i
	 */
	public static ComplexNumber sum(ComplexNumber a, ComplexNumber b) {
		return new ComplexNumber(a.getRe() + b.getRe(), a.getIm() + b.getIm());
	}
	
	/**
	 * @param a = x + yi
	 * @param b = u + vi
	 * @return a - b = (x - u) + (y - v)i
	 */
	public static ComplexNumber subtract(ComplexNumber a, ComplexNumber b) {
		return new ComplexNumber(a.getRe() - b.getRe(), a.getIm() - b.getIm());
	}
	
	/**
	 * @param a = x + yi
	 * @param b = u + vi
	 * @return a * b = (xu - yv) + (xv + yu)i
	 */
	public static ComplexNumber multiply(ComplexNumber a, ComplexNumber b) {
		double x = a.getRe();
		double y = a.getIm();
		double u = b.getRe();
		double v = b.getIm();
		return new ComplexNumber(x*u - y*v, x*v + y*u);
	}
	
	/**
	 * @param a = x + yi
	 * @return a*a = x*x - y*y + 2xyi
	 */
	public static ComplexNumber square(ComplexNumber a) {
		double x = a.getRe();
		double y = a.getIm();
		return new ComplexNumber(x*x - y*y, 2*x*y);
	}
	
	/**
	 * @param a = x + yi
	 * @return 1 / a = x/|a| - iy/|a|
	 */
	public static ComplexNumber reciprocal(ComplexNumber a) {
		double abs = abs(a);
		return new ComplexNumber(a.getRe()/abs, a.getIm()/abs);
	}
	
	/**
	 * @param a = x + yi
	 * @param b = u + vi
	 * @return a/b = a * (1/b)
	 */
	public static ComplexNumber divide(ComplexNumber a, ComplexNumber b) {
		return multiply(a, reciprocal(b));
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(im);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(re);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ComplexNumber other = (ComplexNumber) obj;
		if (Double.doubleToLongBits(im) != Double.doubleToLongBits(other.im))
			return false;
		if (Double.doubleToLongBits(re) != Double.doubleToLongBits(other.re))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return re + " + " + im + "i";
	}
	
}
