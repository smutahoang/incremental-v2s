package incrementalV2S.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import incrementalV2S.model.Parameters;
import util.MyMath;

public class Receiver {
	public String userId;
	public String screenName;

	public HashMap<String, TemporalWeight> retweets;

	public int nItems;
	public int nDiffusers;
	public HashMap<Integer, Integer> itemIndexMap;
	public HashMap<Integer, Integer> diffuserIndexMap;
	public int[] itemIndexes;
	public int[] nAdoptions;
	public int totalNAdoptions;
	public double[] nExponentialAdoptions;
	public double totalNExponentialAdoptions;
	public double itemEntropy;
	public double itemExponentialEntropy;
	public double diffuserEntropy;
	public double diffuserExponentialEntropy;

	public int[] diffuserIndexes;
	public int[] diffuserDiffusions;
	public double[] diffuserExponentialDiffusions;
	private boolean isArrayInitialized;
	public boolean infectedFlag;

	public double susceptibility;

	public AliasSampler alias;
	public HashMap<Integer, Integer> relatedRandomWalks;
	public int nVisits;
	public double diffusionRank;
	public int nNewDiffusions;

	/***
	 * constructor
	 * 
	 * @param _userId
	 */
	public Receiver(String _userId, String _screenName) {
		userId = _userId;
		screenName = _screenName;
		nItems = 0;
		nDiffusers = 0;
		infectedFlag = false;
		isArrayInitialized = false;
		totalNAdoptions = 0;
		totalNExponentialAdoptions = 0;
	}

	private void initializeArrays() {
		itemIndexMap = new HashMap<Integer, Integer>(
				(int) (Parameters.RECEIVER_INFECTED_ITEM_ARRAY_INITIAL_SIZE * 1.34), (float) 0.75);
		diffuserIndexMap = new HashMap<Integer, Integer>(
				(int) (Parameters.RECEIVER_INFECTED_ITEM_ARRAY_INITIAL_SIZE * 1.34), (float) 0.75);
		itemIndexes = new int[Parameters.RECEIVER_INFECTED_ITEM_ARRAY_INITIAL_SIZE];
		nAdoptions = new int[Parameters.RECEIVER_INFECTED_ITEM_ARRAY_INITIAL_SIZE];
		nExponentialAdoptions = new double[Parameters.RECEIVER_INFECTED_ITEM_ARRAY_INITIAL_SIZE];
		diffuserIndexes = new int[Parameters.RECEIVER_INFECTED_ITEM_ARRAY_INITIAL_SIZE];
		diffuserDiffusions = new int[Parameters.RECEIVER_INFECTED_ITEM_ARRAY_INITIAL_SIZE];
		diffuserExponentialDiffusions = new double[Parameters.RECEIVER_INFECTED_ITEM_ARRAY_INITIAL_SIZE];
	}

	private void extendCollectionsSize() {
		int newSize = (int) (nItems * Parameters.RECEIVER_INFECTED_ITEM_ARRAY_GROWING_RATE);
		// hashmap
		HashMap<Integer, Integer> currentItemIndexMap = itemIndexMap;
		itemIndexMap = new HashMap<Integer, Integer>((int) (newSize * 1.34), (float) 0.75);
		itemIndexMap.putAll(currentItemIndexMap);

		currentItemIndexMap = diffuserIndexMap;
		diffuserIndexMap = new HashMap<Integer, Integer>((int) (newSize * 1.34), (float) 0.75);
		diffuserIndexMap.putAll(currentItemIndexMap);
		// item arrays
		itemIndexes = Arrays.copyOf(itemIndexes, newSize);
		nAdoptions = Arrays.copyOf(nAdoptions, newSize);
		nExponentialAdoptions = Arrays.copyOf(nExponentialAdoptions, newSize);

		// diffuser arrays
		newSize = (int) (nDiffusers * Parameters.RECEIVER_INFECTED_ITEM_ARRAY_GROWING_RATE);
		diffuserIndexes = Arrays.copyOf(diffuserIndexes, newSize);
		diffuserDiffusions = Arrays.copyOf(diffuserDiffusions, newSize);
		diffuserExponentialDiffusions = Arrays.copyOf(diffuserExponentialDiffusions, newSize);
	}

	public void getEntropies() {
		itemEntropy = MyMath.normalizeEntropy(MyMath.getEntropy(nAdoptions, 0, nItems - 1), nItems);
		itemExponentialEntropy = MyMath.normalizeEntropy(MyMath.getEntropy(nExponentialAdoptions, 0, nItems - 1),
				nItems);
		diffuserEntropy = MyMath.normalizeEntropy(MyMath.getEntropy(diffuserDiffusions, 0, nDiffusers - 1), nDiffusers);
		diffuserExponentialEntropy = MyMath
				.normalizeEntropy(MyMath.getEntropy(diffuserExponentialDiffusions, 0, nDiffusers - 1), nDiffusers);
	}

	/***
	 * Add an item to the exposing list
	 * 
	 * @param x
	 * @param time
	 */
	public void addAdoptedItem(int x, double weight) {
		// increase the total number of adoptions
		totalNAdoptions++;
		totalNExponentialAdoptions += weight;
		if (!isArrayInitialized) {
			initializeArrays();
			isArrayInitialized = true;
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
				extendCollectionsSize();
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

	/****
	 * add a diffuser to the list
	 * 
	 * @param u
	 * @param weight
	 */
	public void addDiffuser(int u, int count, double weight) {
		if (!isArrayInitialized) {
			initializeArrays();
			isArrayInitialized = true;
		}
		if (nDiffusers == 0) {// fist diffuser
			// increase the number of diffuser
			nDiffusers++;
			// add item to the list
			diffuserIndexMap.put(u, 0);
			// record diffuser
			diffuserIndexes[0] = u;
			// store the number of adoptions
			diffuserDiffusions[0] = count;
			// store number of time-exponential adoptions
			diffuserExponentialDiffusions[0] = weight;

		} else if (!diffuserIndexMap.containsKey(u)) {// first time diffused
			// increase the number of diffuser
			nDiffusers++;
			// add diffuser to the list
			int j = nDiffusers - 1;
			if (j >= diffuserIndexes.length) {
				// time to increase size of arrays and hashmaps
				extendCollectionsSize();
			}
			diffuserIndexMap.put(u, j);
			diffuserIndexes[j] = u;
			// store the number of diffusions
			diffuserDiffusions[j] = count;
			// store number of time-exponential diffusions
			diffuserExponentialDiffusions[j] = weight;
		} else {// already adopted before
			// get diffuser index
			int j = diffuserIndexMap.get(u);
			// increase the number of adoptions
			diffuserDiffusions[j] += count;
			diffuserExponentialDiffusions[j] += weight;
		}
	}

	/***
	 * check if this user infected by the item
	 * 
	 * @param item
	 * @return
	 */
	public boolean isInfected(int itemIndex) {
		if (!infectedFlag)
			return false;
		if (!itemIndexMap.containsKey(itemIndex))
			return false;
		int j = itemIndexMap.get(itemIndex);
		if (nAdoptions[j] == 0)
			return false;
		return true;
	}

	/***
	 * remove the item at position index by replacing it with the item at the
	 * last position
	 * 
	 * @param x
	 */
	public void removeAnItem(int x) {
		int index = itemIndexMap.get(x);
		if (index != (nItems - 1)) {
			// replace in itemIndexMap
			int lastX = itemIndexes[nItems - 1];
			itemIndexMap.remove(x);
			itemIndexMap.put(lastX, index);
			// replace in itemIndexes
			itemIndexes[index] = lastX;
			// reduce totalNAdoptions
			totalNAdoptions -= nAdoptions[index];
			// replace in nAdoptions
			nAdoptions[index] = nAdoptions[nItems - 1];
			// reduce totalNExponentialAdoptions
			totalNExponentialAdoptions -= nExponentialAdoptions[index];
			// replace in nExponentialAdoptions
			nExponentialAdoptions[index] = nExponentialAdoptions[nItems - 1];
			nItems--;
			// delete the last item
			itemIndexes[nItems] = -1;
			// delete number of adoption
			nAdoptions[nItems] = 0;
			nExponentialAdoptions[nItems] = 0;
		} else {
			// delete the last item
			// reduce the number of item
			nItems--;
			itemIndexMap.remove(x);
			// reduce totalNAdoptions
			totalNAdoptions -= nAdoptions[nItems];
			// reduce totalNExponentialAdoptions
			totalNExponentialAdoptions -= nExponentialAdoptions[nItems];

			// delete the last item
			itemIndexes[nItems] = -1;
			// delete number of adoption
			nAdoptions[nItems] = 0;
			nExponentialAdoptions[nItems] = 0;
		}
	}

	/***
	 * reduce diffusions by a diffuser v
	 * 
	 * @param u
	 * @param weight
	 */
	public void reduceADiffuser(int u, double weight) {
		int j = diffuserIndexMap.get(u);
		diffuserDiffusions[j]--;
		diffuserExponentialDiffusions[j] -= weight;
		if (diffuserDiffusions[j] == 0)
			removeADiffuser(u);
	}

	/****
	 * remove a diffuser from the list
	 * 
	 * @param u
	 */
	private void removeADiffuser(int u) {
		int index = diffuserIndexMap.get(u);
		if (index != (nDiffusers - 1)) {
			// replace in diffuserIndexMap
			int lastU = diffuserIndexes[nDiffusers - 1];
			diffuserIndexMap.remove(u);
			diffuserIndexMap.put(lastU, index);
			// replace in diffuserIndexes
			diffuserIndexes[index] = lastU;
			// replace in diffuserDiffusions
			diffuserDiffusions[index] = diffuserDiffusions[nDiffusers - 1];
			// replace in diffuserExponentialDiffusions
			diffuserExponentialDiffusions[index] = diffuserExponentialDiffusions[nDiffusers - 1];
			// reduce the number of diffuser
			nDiffusers--;
			// delete the last diffuser
			diffuserIndexes[nDiffusers] = -1;
			// delete number of diffusions
			diffuserDiffusions[nDiffusers] = 0;
			diffuserExponentialDiffusions[nDiffusers] = 0;

		} else {
			// delete the last diffuser
			// reduce the number of diffuser
			nDiffusers--;
			diffuserIndexMap.remove(u);
			// delete the last diffuser
			diffuserIndexes[nDiffusers] = -1;
			// delete number of diffusions
			diffuserDiffusions[nDiffusers] = 0;
			diffuserExponentialDiffusions[nDiffusers] = 0;
		}
	}

	/***
	 * update infectedFlag: will be called after removed all old items
	 */
	public void updateInfectionFlag() {
		if (totalNAdoptions > 0)
			infectedFlag = true;
		else
			infectedFlag = false;
	}

	/***
	 * update the alias
	 */
	public void updateAlias() {
		alias = new AliasSampler(itemIndexes, nExponentialAdoptions, nItems);
	}

	// step to an adopted item
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
		if (infectedFlag) {
			TemporalWeight[] topItems = new TemporalWeight[nItems];
			for (int j = 0; j < nItems; j++) {
				topItems[j] = new TemporalWeight(nAdoptions[j], nExponentialAdoptions[j], itemIndexes[j], true);
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
		System.out.println("nDiffusers = " + nDiffusers);
		System.out.println("infectedFlag = " + infectedFlag);
		System.out.println("nNewDiffusions = " + nNewDiffusions);
		System.out.println("items:");
		for (int i = 0; i < nItems; i++) {
			System.out.printf("----- index = " + i);
			System.out.printf("\t itemIndex = " + itemIndexes[i]);
			System.out.println(" (index in map: " + itemIndexMap.get(itemIndexes[i]) + ")");
			System.out.printf("----- #adoptions = " + nAdoptions[i]);
		}

		System.out.println("diffusers:");
		for (int i = 0; i < nDiffusers; i++) {
			System.out.printf("----- index = " + i);
			System.out.printf("\t infectorIndex = " + diffuserIndexes[i]);
			System.out.println(" (index in map: " + diffuserIndexMap.get(diffuserIndexes[i]) + ")");
			System.out.printf("----- #diffusions = " + diffuserDiffusions[i]);
		}
	}
}
