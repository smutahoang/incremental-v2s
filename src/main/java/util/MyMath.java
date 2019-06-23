package util;

public class MyMath {
	/***
	 * scaling down a non-negative number d to [0,1)
	 * 
	 * @param d
	 * @return
	 */
	public static double scaledown(double d) {
		return d / (d + Math.exp(-d));
	}

	/***
	 * get entropy of a subarray of values
	 * 
	 * @param values
	 * @param startIndex
	 * @param endIndex
	 * @return
	 */
	public static double getEntropy(double[] values, int startIndex,
			int endIndex) {
		if (startIndex == endIndex)
			return 0;
		double sum = 0;
		for (int i = startIndex; i <= endIndex; i++)
			sum += values[i];
		double e = 0;
		double p = 0;
		for (int i = startIndex; i <= endIndex; i++) {
			p = values[i] / sum;
			if (p > 0)
				e += p * Math.log(p);

		}
		return -e;
	}

	public static double getEntropy(int[] values, int startIndex, int endIndex) {
		if (startIndex == endIndex)
			return 0;
		double[] temp = new double[endIndex - startIndex + 1];
		for (int i = 0; i <= endIndex - startIndex; i++) {
			temp[i] = values[i + startIndex];
		}
		return getEntropy(temp, 0, endIndex - startIndex);
	}

	/***
	 * get new entropy of a subarray of values when values[pos] is changed to
	 * newValue
	 * 
	 * @param values
	 * @param startIndex
	 * @param endIndex
	 * @param pos
	 * @param newValue
	 * @param oldEntropy
	 * @return
	 */
	public static double updateEntropy(double[] values, int startIndex,
			int endIndex, double subsum, int pos, int newValue,
			double oldEntropy) {
		if (pos < startIndex)
			return oldEntropy;
		if (pos > endIndex)
			return oldEntropy;
		double oldValue = values[pos];
		double delta = newValue - oldValue;
		double a = oldEntropy * subsum;
		double b = oldValue * Math.log(oldValue) - newValue
				* Math.log(newValue);
		double c = Math.log(delta + subsum) * (delta + subsum) - subsum
				* Math.log(subsum);
		double newEntropy = (a + b + c) / (delta + subsum);
		return newEntropy;
	}

	/***
	 * linearly map [0, ln(n)] to [0.5, 1]
	 * 
	 * @param e
	 * @param n
	 * @return
	 */
	public static double normalizeEntropy(double e, int n) {
		if (n == 1)
			return 0.5;
		e = 0.5 * e / Math.log(n) + 0.5;
		return e;
	}

}
