package mandelbrotmenge;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class ExactComplexNumber {
	
	/**
	 * Genauigkeit: 100 Dezimalstellen, Rundung: Half Even
	 */
	public static final MathContext mc = new MathContext(100, RoundingMode.HALF_EVEN);
	
	/**
	 * real part
	 */
	private BigDecimal re;
	
	/**
	 * imaginary part
	 */
	private BigDecimal im;
	
	public ExactComplexNumber() {
		re = BigDecimal.ZERO;
		im = BigDecimal.ZERO;
	}
	
	public ExactComplexNumber(double re, double im) {
		this.re = new BigDecimal(re);
		this.im = new BigDecimal(im);
	}
	
	public ExactComplexNumber(BigDecimal re, BigDecimal im) {
		this.re = re;
		this.im = im;
	}
	
	public BigDecimal getRe() {
		return re;
	}
	
	public void setRe(BigDecimal re) {
		this.re = re;
	}
	
	public BigDecimal getIm() {
		return im;
	}
	
	public void setIm(BigDecimal im) {
		this.im = im;
	}
	
	public ComplexNumber toNormalComplexNumber() {
		return new ComplexNumber(re.doubleValue(), im.doubleValue());
	}
	
	/**
	 * @param a = x + yi
	 * @return |a|^2 = x*x + y*y
	 */
	public static BigDecimal absSquare(ExactComplexNumber a) {
		BigDecimal x = a.getRe();
		BigDecimal y = a.getIm();
		return x.pow(2, mc).add(y.pow(2, mc), mc);
	}
	
	/**
	 * @param a = x + yi
	 * @param b = u + vi
	 * @return a + b = (x + u) + (y + v)i
	 */
	public static ExactComplexNumber sum(ExactComplexNumber a, ExactComplexNumber b) {
		return new ExactComplexNumber(a.getRe().add(b.getRe(), mc), a.getIm().add(b.getIm(), mc));
	}
	
	/**
	 * @param a = x + yi
	 * @param b = u + vi
	 * @return a - b = (x - u) + (y - v)i
	 */
	public static ExactComplexNumber subtract(ExactComplexNumber a, ExactComplexNumber b) {
		return new ExactComplexNumber(a.getRe().subtract(b.getRe(), mc), a.getIm().subtract(b.getIm(), mc));
	}
	
	/**
	 * @param a = x + yi
	 * @return a*a = x*x - y*y + 2xyi
	 */
	public static ExactComplexNumber square(ExactComplexNumber a) {
		BigDecimal x = a.getRe();
		BigDecimal y = a.getIm();
		return new ExactComplexNumber(x.pow(2, mc).subtract(y.pow(2, mc), mc), x.add(x, mc).multiply(y, mc));
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((im == null) ? 0 : im.hashCode());
		result = prime * result + ((re == null) ? 0 : re.hashCode());
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
		ExactComplexNumber other = (ExactComplexNumber) obj;
		if (im == null) {
			if (other.im != null)
				return false;
		} else if (!im.equals(other.im))
			return false;
		if (re == null) {
			if (other.re != null)
				return false;
		} else if (!re.equals(other.re))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return "ExactComplexNumber [re=" + re + ", im=" + im + "]";
	}
	
}
