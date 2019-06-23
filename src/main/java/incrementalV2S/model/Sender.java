package incrementalV2S.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import incrementalV2S.model.Parameters;
import util.MyMath;

public class Sender {

	public String userId;
	public String screenName;

	public HashMap<String, TemporalWeight> retweets;

	public int nItems;
	public int nInfectors;

	public HashMap<Integer, Integer> itemIndexMap;
	public int[] itemIndexes;
	public HashMap<Integer, Integer> infectorIndexMap;
	public int[] infectorIndexes;

	public int[] nAdoptions;
	public int totalNAdoptions;
	public double[] nExponentialAdoptions;
	public double totalNExponentialAdoptions;
	public int[] nDiffusions;
	public int totalNDiffusions;
	public double[] nExponentialDiffusions;
	public double totalNExponentialDiffusions;
	public int[] infectorAdoptions;
	public double[] infectorExponentialAdoptions;
	public double itemAdoptingEntropy;
	public double itemExponentialAdoptingEntropy;
	public double itemDiffusingEntropy;
	public double itemExponentialDiffusingEntropy;
	public double infectorEntropy;
	public double infectorExponentialEntropy;
	public boolean diffusedFlag;

	public AliasSampler alias;
	public HashMap<Integer, Integer> relatedRandomWalks;
	public int nVisits;
	public double diffusionRank;
	public double virality;

	private boolean isAdoptingArrayInitialized;
	private boolean isDiffusingArrayInitialized;
	public int nNewDiffusions;

	private void initializeAdoptingArrays() {
		itemIndexMap = new HashMap<Integer, Integer>((int) (Parameters.SENDER_DIFFUSED_ITEM_ARRAY_INITIAL_SIZE * 1.34),
				(float) 0.75);
		itemIndexes = new int[Parameters.SENDER_DIFFUSED_ITEM_ARRAY_INITIAL_SIZE];
		nAdoptions = new int[Parameters.SENDER_DIFFUSED_ITEM_ARRAY_INITIAL_SIZE];
		nExponentialAdoptions = new double[Parameters.SENDER_DIFFUSED_ITEM_ARRAY_INITIAL_SIZE];
	}

	private void initializeDiffusingArrays() {
		nDiffusions = new int[nAdoptions.length];
		nExponentialDiffusions = new double[nExponentialAdoptions.length];
		infectorAdoptions = new int[Parameters.SENDER_DIFFUSED_ITEM_ARRAY_INITIAL_SIZE];
		infectorExponentialAdoptions = new double[Parameters.SENDER_DIFFUSED_ITEM_ARRAY_INITIAL_SIZE];
		infectorIndexMap = new HashMap<Integer, Integer>(
				(int) (Parameters.SENDER_DIFFUSED_ITEM_ARRAY_INITIAL_SIZE * 1.34), (float) 0.75);
		infectorIndexes = new int[Parameters.SENDER_DIFFUSED_ITEM_ARRAY_INITIAL_SIZE];
	}

	public Sender(String _userId, String _screenName) {
		userId = _userId;
		screenName = _screenName;
		nItems = 0;
		diffusedFlag = false;
		totalNAdoptions = 0;
		totalNExponentialAdoptions = 0;
		totalNDiffusions = 0;
		totalNExponentialDiffusions = 0;
		isAdoptingArrayInitialized = false;
		isDiffusingArrayInitialized = false;
	}

	/***
	 * to extend arrays and hashmaps
	 */
	private void extendItemCollectionsSize() {
		int currentSize = nItems;
		int newSize = (int) (currentSize * Parameters.SENDER_DIFFUSED_ITEM_ARRAY_GROWING_RATE);
		// hashmap
		HashMap<Integer, Integer> currentIndexMap = itemIndexMap;
		itemIndexMap = new HashMap<Integer, Integer>((int) (newSize * 1.34), (float) 0.75);
		itemIndexMap.putAll(currentIndexMap);
		currentIndexMap = null;
		// arrays
		itemIndexes = Arrays.copyOf(itemIndexes, newSize);
		nAdoptions = Arrays.copyOf(nAdoptions, newSize);
		nExponentialAdoptions = Arrays.copyOf(nExponentialAdoptions, newSize);
		if (nDiffusions != null) {
			nDiffusions = Arrays.copyOf(nDiffusions, newSize);
			nExponentialDiffusions = Arrays.copyOf(nExponentialDiffusions, newSize);
		}
	}

	private void extendInfectorCollectionsSize() {
		int currentSize = nInfectors;
		int newSize = (int) (currentSize * Parameters.SENDER_DIFFUSED_ITEM_ARRAY_GROWING_RATE);
		// hashmap
		HashMap<Integer, Integer> currentInfectorMap = infectorIndexMap;
		infectorIndexMap = new HashMap<Integer, Integer>((int) (newSize * 1.34), (float) 0.75);
		infectorIndexMap.putAll(currentInfectorMap);
		currentInfectorMap = null;
		// arrays
		infectorIndexes = Arrays.copyOf(infectorIndexes, newSize);
		infectorAdoptions = Arrays.copyOf(infectorAdoptions, newSize);
		infectorExponentialAdoptions = Arrays.copyOf(infectorExponentialAdoptions, newSize);
	}

	public void getAdoptingEntropies() {
		itemAdoptingEntropy = MyMath.normalizeEntropy(MyMath.getEntropy(nAdoptions, 0, nItems - 1), nItems);
		itemExponentialAdoptingEntropy = MyMath
				.normalizeEntropy(MyMath.getEntropy(nExponentialAdoptions, 0, nItems - 1), nItems);
	}

	public void getDiffusingEntropies() {
		itemDiffusingEntropy = MyMath.normalizeEntropy(MyMath.getEntropy(nDiffusions, 0, nItems - 1), nItems);
		itemExponentialDiffusingEntropy = MyMath
				.normalizeEntropy(MyMath.getEntropy(nExponentialDiffusions, 0, nItems - 1), nItems);
	}

	public void getInfectorEntropies() {
		infectorEntropy = MyMath.normalizeEntropy(MyMath.getEntropy(infectorAdoptions, 0, nInfectors - 1), nInfectors);
		infectorExponentialEntropy = MyMath
				.normalizeEntropy(MyMath.getEntropy(infectorExponentialAdoptions, 0, nInfectors - 1), nInfectors);
	}

	/***
	 * Add an item to the adopting list
	 * 
	 * @param x
	 * @param adoptingTime
	 */
	public void addAdoptedItem(int x, double weight) {
		// increase the total number of adoptions
		totalNAdoptions++;
		totalNExponentialAdoptions += weight;
		if (!isAdoptingArrayInitialized) {
			initializeAdoptingArrays();
			isAdoptingArrayInitialized = true;
		}
		if (nItems == 0) {// fist adopted item
			// increase the number of items adopted
			nItems++;
			// add item to the list
			itemIndexMap.put(x, 0);
			// record item
			itemIndexes[0] = x;
			// store the number of adoptions
			nAdoptions[0] = 1;
			// store number of time-exponential adoptions
			nExponentialAdoptions[0] = weight;

		} else if (!itemIndexMap.containsKey(x)) {// first time adopted
													// item
			// increase the number of items adopted
			nItems++;
			// add item to the list
			int j = nItems - 1;
			if (j >= itemIndexes.length) {
				// time to increase size of arrays and hashmaps
				extendItemCollectionsSize();
			}
			itemIndexMap.put(x, j);
			itemIndexes[j] = x;

			// store the number of adoptions
			nAdoptions[j] = 1;
			// exponential adoptions
			nExponentialAdoptions[j] = weight;

		} else {// already adopted before
			// get item index
			int j = itemIndexMap.get(x);
			// increase the number of adoptions
			nAdoptions[j]++;
			nExponentialAdoptions[j] += weight;
		}
	}

	/***
	 * Add an item to the diffusing list
	 * 
	 * @param x
	 * @param weight
	 */
	public boolean addDiffusedItem(int x, double weight) {
		// the case when the original tweet is published before all other
		// original tweets from the same author in the dataset
		if (itemIndexMap == null)
			return false;
		// the case when the hashtag in retweet is truncated and is not the same
		// with the hashtag in the original tweet
		if (!itemIndexMap.containsKey(x))
			return false;
		// increase the total number of diffusions
		totalNDiffusions++;
		totalNExponentialDiffusions += weight;
		if (!isDiffusingArrayInitialized) {
			initializeDiffusingArrays();
			isDiffusingArrayInitialized = true;
		}
		// mark that this user already diffused an item
		diffusedFlag = true;
		// get item index
		int j = itemIndexMap.get(x);
		// increase the number of diffusions
		nDiffusions[j]++;
		nExponentialDiffusions[j] += weight;
		return true;
	}

	/***
	 * add an infector to the list
	 * 
	 * @param v
	 * @param weight
	 */
	public void addInfector(int v, int count, double weight) {
		if (!isDiffusingArrayInitialized) {
			initializeDiffusingArrays();
			isDiffusingArrayInitialized = true;
		}
		if (nInfectors == 0) {// first infector
			nInfectors++;
			// add item to the list
			infectorIndexMap.put(v, 0);
			// record item
			infectorIndexes[0] = v;
			// store the number of adoptions
			infectorAdoptions[0] = count;
			// store number of time-exponential adoptions
			infectorExponentialAdoptions[0] = weight;
		} else if (!infectorIndexMap.containsKey(v)) {// first time
														// adopted
														// infector
			// increase the number of infectors
			nInfectors++;
			// add infector to the list
			int j = nInfectors - 1;
			if (j >= infectorIndexes.length) {
				// time to increase size of arrays and hashmaps
				extendInfectorCollectionsSize();
			}
			infectorIndexMap.put(v, j);
			infectorIndexes[j] = v;
			// store the number of adoptions
			infectorAdoptions[j] = count;
			// exponential adoptions
			infectorExponentialAdoptions[j] = weight;
		} else {// already adopted before
			// get infector index
			int j = infectorIndexMap.get(v);
			// increase the number of adoptions
			infectorAdoptions[j] += count;
			infectorExponentialAdoptions[j] += weight;
		}
	}

	/***
	 * check if this user adopted the item
	 * 
	 * @param x
	 * @return
	 */
	public boolean isAdopted(int x) {
		return itemIndexMap.containsKey(x);
	}

	/***
	 * check if this user diffused the item
	 * 
	 * @param x
	 * @return
	 */
	public boolean isDiffused(int x) {
		if (!diffusedFlag)
			return false;
		if (!itemIndexMap.containsKey(x))
			return false;
		int j = itemIndexMap.get(x);
		if (nDiffusions[j] == 0)
			return false;
		return true;
	}

	/***
	 * remove the item x by replacing it with the item at the last position
	 * 
	 * @param x
	 */
	public void removeAnItem(int x) {
		int index = itemIndexMap.remove(x);
		if (index != (nItems - 1)) {
			// replace in itemIndexMap
			int lastX = itemIndexes[nItems - 1];
			itemIndexMap.put(lastX, index);
			// replace in itemIndexes array
			itemIndexes[index] = lastX;
			// reduce totalNAdoptions
			totalNAdoptions -= nAdoptions[index];
			// replacing in nAdoptions array
			nAdoptions[index] = nAdoptions[nItems - 1];
			// reduce nExponentialAdoptions
			totalNExponentialAdoptions -= nExponentialAdoptions[index];
			// replace in nExponentialAdoptions array
			nExponentialAdoptions[index] = nExponentialAdoptions[nItems - 1];
			if (isDiffusingArrayInitialized) {
				// reduce totalNDiffusions
				totalNDiffusions -= nDiffusions[index];
				// replace in nDiffusions array
				nDiffusions[index] = nDiffusions[nItems - 1];
				// reduce totalNExponentialDiffusions
				totalNExponentialDiffusions -= nExponentialDiffusions[index];
				// replace in nExponentialDiffusions array
				nExponentialDiffusions[index] = nExponentialDiffusions[nItems - 1];
			}
			// decrease the number of items adopted
			nItems--;
			// delete the last item
			itemIndexes[nItems] = -1;
			// delete the number of adoptions
			nAdoptions[nItems] = 0;
			// delete exponential adoptions
			nExponentialAdoptions[nItems] = 0;
			if (isDiffusingArrayInitialized) {
				// delete the number of diffusions
				nDiffusions[nItems] = 0;
				nExponentialDiffusions[nItems] = 0;
			}
		} else {
			// decrease the number of items adopted
			nItems--;
			// reduce totalNAdoptions
			totalNAdoptions -= nAdoptions[nItems];
			// reduce totalNExponentialAdoptions
			totalNExponentialAdoptions -= nExponentialAdoptions[nItems];
			if (isDiffusingArrayInitialized) {
				// reduce totalNDiffusions
				totalNDiffusions -= nDiffusions[nItems];
				// reduce totalNExponentialDiffusions
				totalNExponentialDiffusions -= nExponentialDiffusions[nItems];
			}
			// delete the last item
			itemIndexes[nItems] = -1;
			// delete the number of adoptions
			nAdoptions[nItems] = 0;
			// delete exponential adoptions
			nExponentialAdoptions[nItems] = 0;
			if (isDiffusingArrayInitialized) {
				// delete the number of diffusions
				nDiffusions[nItems] = 0;
				nExponentialDiffusions[nItems] = 0;
			}
		}
	}

	/***
	 * reduce adoptions by infector v
	 * 
	 * @param v
	 * @param weight
	 */
	public void reduceInfectorAdoptions(int v, double weight) {
		int j = infectorIndexMap.get(v);
		infectorAdoptions[j]--;
		infectorExponentialAdoptions[j] -= weight;
		if (infectorAdoptions[j] == 0)
			removeAnInfector(v);
	}

	private void removeAnInfector(int v) {
		int index = infectorIndexMap.get(v);
		if (index != (nInfectors - 1)) {
			// replace in itemIndexMap
			int lastV = infectorIndexes[nInfectors - 1];
			infectorIndexMap.remove(v);
			infectorIndexMap.put(lastV, index);
			// replace in infectorIndexes array
			infectorIndexes[index] = lastV;
			// replacing in infectorAdoptions array
			infectorAdoptions[index] = infectorAdoptions[nInfectors - 1];
			// replace in infectorExponentialAdoptions array
			infectorExponentialAdoptions[index] = infectorExponentialAdoptions[nInfectors - 1];
			// decrease the number of infectors
			nInfectors--;
			// delete the last item
			infectorIndexes[nInfectors] = -1;
			// delete the number of adoptions
			infectorAdoptions[nInfectors] = 0;
			// delete exponential adoptions
			infectorExponentialAdoptions[nInfectors] = 0;
		} else {
			infectorIndexMap.remove(v);
			// decrease the number of infectors
			nInfectors--;
			// delete the last item
			infectorIndexes[nInfectors] = -1;
			// delete the number of adoptions
			infectorAdoptions[nInfectors] = 0;
			// delete exponential adoptions
			infectorExponentialAdoptions[nInfectors] = 0;
		}
	}

	/***
	 * update diffusedFlag: will be called after removed all old items
	 */
	public void updateDiffusionFlag() {
		if (totalNDiffusions > 0)
			diffusedFlag = true;
		else
			diffusedFlag = false;
	}

	/***
	 * update the alias sampler
	 */
	public void updateAlias() {
		if (totalNDiffusions == 0) {
			diffusedFlag = false;
			alias = null;
			return;
		}
		alias = new AliasSampler();
		alias.flag = true;
		alias.nOutcomes = 0;
		for (int j = 0; j < nItems; j++) {
			if (nDiffusions[j] > 0)
				alias.nOutcomes++;
		}
		alias.outcomes = new DiscreteVariable[alias.nOutcomes];
		alias.nOutcomes = 0;
		for (int j = 0; j < nItems; j++) {
			if (nDiffusions[j] > 0) {
				alias.outcomes[alias.nOutcomes] = new DiscreteVariable();
				alias.outcomes[alias.nOutcomes].index = itemIndexes[j];
				alias.outcomes[alias.nOutcomes].prob = nExponentialDiffusions[j];
				alias.nOutcomes++;
			}
		}
		alias.buildAlias();
	}

	/***
	 * 
	 * @return
	 */
	public int step() {
		return alias.sample();
	}

	/***
	 * remove a related random walk
	 * 
	 * @param walkIndex
	 */
	public void removeRelatedRandomWalk(int walkIndex) {
		if (relatedRandomWalks.containsKey(walkIndex)) {
			int n = relatedRandomWalks.get(walkIndex);
			relatedRandomWalks.remove(walkIndex);
			nVisits -= n;
		}
	}

	/***
	 * add a related random walk
	 * 
	 * @param walkIndex
	 * @param times
	 */
	public void addRelatedRandomWalk(int walkIndex, int times) {
		if (relatedRandomWalks == null)
			relatedRandomWalks = new HashMap<Integer, Integer>();
		if (relatedRandomWalks.containsKey(walkIndex)) {
			int newTimes = times + relatedRandomWalks.get(walkIndex);
			relatedRandomWalks.put(walkIndex, newTimes);
		} else {
			relatedRandomWalks.put(walkIndex, times);
		}
		nVisits += times;
	}

	/***
	 * Add a new retweet
	 * 
	 * @param tweetId
	 * @param weight
	 * @param timeStep
	 */
	public void addRetweet(String tweetId, double weight, int timeStep) {
		if (retweets == null)
			retweets = new HashMap<>();
		if (retweets.containsKey(tweetId)) {
			TemporalWeight retweet = retweets.get(tweetId);
			retweet.count++;
			retweet.weight += weight;
			retweet.lastUpdateTime = timeStep;
			retweets.put(tweetId, retweet);
		} else {
			retweets.put(tweetId, new TemporalWeight(1, weight, timeStep));
		}

	}

	/***
	 * remove a retweeted tweet
	 * 
	 * @param tweetId
	 */
	public void removeRetweet(String tweetId) {
		retweets.remove(tweetId);
	}

	/****
	 * get top retweeted tweets and remove old retweeted tweets
	 * 
	 * @param lastUpdate
	 * @return
	 */
	public TemporalWeight[] getAndFilterRetweets(int lastUpdate) {
		if (retweets == null) {
			return null;
		}
		HashSet<String> oldRetweets = new HashSet<>();
		Iterator<Map.Entry<String, TemporalWeight>> iter = retweets.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, TemporalWeight> pair = iter.next();
			String tweetId = pair.getKey();
			TemporalWeight retweet = pair.getValue();
			if (retweet.lastUpdateTime < lastUpdate)
				oldRetweets.add(tweetId);
		}
		Iterator<String> tIter = oldRetweets.iterator();
		while (tIter.hasNext()) {
			removeRetweet(tIter.next());
		}
		int n = retweets.size();
		if (n == 0)
			return null;
		TemporalWeight[] topTweets;
		topTweets = new TemporalWeight[retweets.size()];
		int j = 0;
		iter = retweets.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, TemporalWeight> pair = iter.next();
			String tweetId = pair.getKey();
			TemporalWeight retweet = pair.getValue();
			topTweets[j] = new TemporalWeight(retweet.count, retweet.weight, tweetId);
			j++;
		}
		return topTweets;

	}

	/***
	 * get top diffused items
	 * 
	 * @return
	 */
	public TemporalWeight[] getTopItems() {
		if (diffusedFlag) {
			TemporalWeight[] topItems = new TemporalWeight[nItems];
			for (int j = 0; j < nItems; j++) {
				topItems[j] = new TemporalWeight(nDiffusions[j], nExponentialDiffusions[j], itemIndexes[j], true);
			}
			return topItems;
		}
		return null;
	}

	/***
	 * print all information about this user
	 */
	public void printProfile() {
		System.out.println("userId = " + userId);
		System.out.println("screenname = " + screenName);
		System.out.println("nItems = " + nItems);
		System.out.println("nInfectors = " + nInfectors);
		System.out.println("diffusedFlag = " + diffusedFlag);
		System.out.println("nNewDiffusions = " + nNewDiffusions);
		System.out.println("items:");
		for (int i = 0; i < nItems; i++) {
			System.out.printf("----- index = " + i);
			System.out.printf("\t itemIndex = " + itemIndexes[i]);
			System.out.printf(" (index in map: " + itemIndexMap.get(itemIndexes[i]) + ")");
			System.out.printf("\t #adoptions = " + nAdoptions[i]);
			System.out.println("\t #diffusions = " + nDiffusions[i]);
		}

		System.out.println("infectors:");
		for (int i = 0; i < nInfectors; i++) {
			System.out.printf("----- index = " + i);
			System.out.printf("\t infectorIndex = " + infectorIndexes[i]);
			System.out.print(" (index in map: " + infectorIndexMap.get(infectorIndexes[i]) + ")");
			System.out.println("\t #infections = " + infectorAdoptions[i]);
		}
	}
}
