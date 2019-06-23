/***
 * sampling from discrete distribution using alias method
 */
package incrementalV2S.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AliasSampler {
	private static class Alias {
		public int lIndex;
		public int hIndex;
		public double prob;

		public Alias(int l, int h, double p) {
			this.lIndex = l;
			this.hIndex = h;
			this.prob = p;
		}
	}

	private Alias[] aliases;
	public int nOutcomes;
	public DiscreteVariable[] outcomes;
	public boolean flag;// true if sample from set discrete variables

	public AliasSampler() {
		aliases = null;
		nOutcomes = -1;
		outcomes = null;
		flag = false;
	}

	// utility variables

	/***
	 * To normalize a non-negative vector to a distribution
	 * 
	 * @param dVariables
	 * @return
	 */
	private void normalize() {
		double sum = 0;
		for (int i = 0; i < nOutcomes; i++)
			sum += outcomes[i].prob;
		for (int i = 0; i < nOutcomes; i++)
			outcomes[i].prob = outcomes[i].prob / sum;
	}

	/***
	 * build the alias from the distribution
	 * 
	 * @param dVariables
	 */
	public void buildAlias() {
		normalize();
		aliases = new Alias[nOutcomes];
		// identify low and high bins
		double threshold = 1.0 / nOutcomes;
		int[] lIndexes = new int[nOutcomes];
		int[] hIndexes = new int[nOutcomes];
		double[] lProbs = new double[nOutcomes];
		double[] hProbs = new double[nOutcomes];
		int lCount = 0;
		int hCount = 0;
		for (int i = 0; i < nOutcomes; i++) {
			if (outcomes[i].prob < threshold) {
				lIndexes[lCount] = i;
				lProbs[lCount] = outcomes[i].prob;
				lCount++;
			} else {
				hIndexes[hCount] = i;
				hProbs[hCount] = outcomes[i].prob;
				hCount++;
			}
		}

		int aIndex = 0;
		int currentL = 0;
		int currentH = 0;
		double remain = 0;
		while (true) {
			if (currentL < lCount) {
				if (currentH >= hCount) {// last element, no more high vase
					aliases[aIndex] = new Alias(lIndexes[currentL],
							lIndexes[currentL], lProbs[currentL]);
					aIndex++;
					currentL++;
				} else {
					aliases[aIndex] = new Alias(lIndexes[currentL],
							hIndexes[currentH], lProbs[currentL]);
					aIndex++;
					remain = hProbs[currentH] - (threshold - lProbs[currentL]);
					if (remain > threshold) {
						hProbs[currentH] = remain;
						currentL++;
					} else {
						lProbs[currentL] = remain;
						lIndexes[currentL] = hIndexes[currentH];
						currentH++;
					}
				}
			} else if (currentH < hCount) {
				aliases[aIndex] = new Alias(hIndexes[currentH],
						hIndexes[currentH], hProbs[currentH]);
				aIndex++;
				currentH++;
			} else {
				break;
			}
		}

	}

	/***
	 * constructor
	 * 
	 * @param outcomeLikelihoods
	 * @param r
	 */
	public AliasSampler(int[] outcomeIndexes, double[] outcomeLikelihoods,
			int nElements) {
		nOutcomes = nElements;
		outcomes = new DiscreteVariable[nOutcomes];
		for (int i = 0; i < nElements; i++) {
			outcomes[i] = new DiscreteVariable();
			outcomes[i].index = outcomeIndexes[i];
			outcomes[i].prob = outcomeLikelihoods[i];
		}

		buildAlias();
		flag = true;
	}

	/***
	 * constructor
	 * 
	 * @param outcomeLikelihoods
	 * @param outcomeLikelihoods
	 * @param r
	 */
	public AliasSampler(HashMap<Integer, TemporalWeight> outcomeLikelihoods) {
		nOutcomes = outcomeLikelihoods.size();
		outcomes = new DiscreteVariable[nOutcomes];
		Iterator<Map.Entry<Integer, TemporalWeight>> iter = outcomeLikelihoods
				.entrySet().iterator();
		Map.Entry<Integer, TemporalWeight> pair = null;
		int i = 0;
		while (iter.hasNext()) {
			pair = iter.next();
			outcomes[i] = new DiscreteVariable();
			outcomes[i].index = pair.getKey();
			outcomes[i].prob = pair.getValue().weight;
			i++;
		}

		buildAlias();
		flag = true;
	}

	/***
	 * Sampling from an alias
	 * 
	 * @return
	 */
	public int sample() {
		if (nOutcomes == 1) {
			if (flag)
				return outcomes[0].index;
			else
				return 0;
		} else {
			int o = -1;
			int bin = Parameters.rand.nextInt(nOutcomes);
			if (nOutcomes * aliases[bin].prob > Parameters.rand.nextDouble()) {
				o = aliases[bin].hIndex;
			} else {
				o = aliases[bin].lIndex;
			}
			if (flag)
				return outcomes[o].index;
			else
				return o;
		}
	}

}
