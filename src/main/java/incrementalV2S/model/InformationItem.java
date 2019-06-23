package incrementalV2S.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import incrementalV2S.model.TemporalWeight;
import util.MyMath;

public class InformationItem {
	public String name;

	public HashMap<Integer, TemporalWeight> senders;
	public HashMap<Integer, TemporalWeight> infectors;
	public HashMap<Integer, TemporalWeight> diffusers;
	public HashMap<String, TemporalWeight> retweets;

	public HashMap<Integer, HashMap<Integer, TemporalWeight>> diffusions;// sender-(infected
	// receivers
	public HashMap<Integer, HashMap<Integer, TemporalWeight>> infections;// receiver
																			// -
	// diffusing senders
	public int totalNAdoptions;
	public double totalNExponentialAdoptions;
	public int totalNDiffusions;
	public double totalNExponentialDiffusions;
	public int totalNExposures;
	public double totalNExponentialExposures;

	public HashMap<Integer, AliasSampler> senderAliases;
	public HashMap<Integer, AliasSampler> receiverAliases;
	public boolean isDiffusedFlag;
	public double virality;
	public double diffuserEntropy;
	public double diffuserExponentialEntropy;
	public double infectorEntropy;
	public double infectorExponentialEntropy;

	public int lastUpdateTime;
	public boolean isRemoved;
	public int nNewDiffusions;
	public boolean isRecycledIndex;

	/***
	 * constructor
	 */
	public InformationItem(String itemName, int timeStep, boolean indexRecycle) {
		name = itemName;
		isDiffusedFlag = false;
		totalNAdoptions = 0;
		totalNExponentialAdoptions = 0;
		totalNDiffusions = 0;
		totalNExponentialDiffusions = 0;
		virality = 0;
		lastUpdateTime = timeStep;
		isRemoved = false;
		isRecycledIndex = indexRecycle;
	}

	public void getEntropies() {
		int[] counts = new int[diffusers.size()];
		double[] weights = new double[diffusers.size()];
		for (int i = 0; i < counts.length; i++) {
			counts[i] = 0;
			weights[i] = 0;
		}
		// distribution of #diffusions over the diffusers
		Iterator<Map.Entry<Integer, TemporalWeight>> iter = diffusers.entrySet().iterator();
		Map.Entry<Integer, TemporalWeight> pair = null;
		int j = 0;
		while (iter.hasNext()) {
			pair = iter.next();
			counts[j] += pair.getValue().count;
			weights[j] += pair.getValue().weight;
			j++;
		}
		diffuserEntropy = MyMath.normalizeEntropy(MyMath.getEntropy(counts, 0, counts.length - 1), counts.length);
		diffuserExponentialEntropy = MyMath.normalizeEntropy(MyMath.getEntropy(weights, 0, weights.length - 1),
				weights.length);

		// distribution of #diffusions over the infectors
		counts = new int[infectors.size()];
		weights = new double[infectors.size()];
		for (int i = 0; i < counts.length; i++) {
			counts[i] = 0;
			weights[i] = 0;
		}

		j = 0;
		iter = infectors.entrySet().iterator();
		while (iter.hasNext()) {
			pair = iter.next();
			counts[j] += pair.getValue().count;
			weights[j] += pair.getValue().weight;
			j++;
		}
		infectorEntropy = MyMath.normalizeEntropy(MyMath.getEntropy(counts, 0, counts.length - 1), counts.length);
		infectorExponentialEntropy = MyMath.normalizeEntropy(MyMath.getEntropy(weights, 0, weights.length - 1),
				weights.length);
	}

	public void updateSenders(int u, double weight, int timeStep) {
		// update number of adoptions
		totalNAdoptions++;
		totalNExponentialAdoptions += weight;
		// update the sender's adoptions
		if (senders == null) {
			senders = new HashMap<Integer, TemporalWeight>();
			senders.put(u, new TemporalWeight(1, weight, timeStep));
		} else {
			if (senders.containsKey(u)) {
				TemporalWeight tc = senders.get(u);
				tc.update(1, weight, timeStep);
				senders.put(u, tc);
			} else {
				senders.put(u, new TemporalWeight(1, weight, timeStep));
			}
		}
	}

	/***
	 * update diffusions count and weight of a sender
	 * 
	 * @param u
	 * @param fanOut
	 */
	public void updateDiffusers(int u, double weight, int timeStep) {
		if (diffusers == null) {
			diffusers = new HashMap<Integer, TemporalWeight>();
			diffusers.put(u, new TemporalWeight(1, weight, timeStep));
		} else {
			if (diffusers.containsKey(u)) {
				TemporalWeight tc = diffusers.get(u);
				tc.update(1, weight, timeStep);
				diffusers.put(u, tc);
			} else {
				diffusers.put(u, new TemporalWeight(1, weight, timeStep));
			}
		}
	}

	/***
	 * update diffusions count and weight of a infector
	 * 
	 * @param v
	 * @param weight
	 */
	public void updateInfectors(int v, double weight, int timeStep) {
		if (infectors == null) {
			infectors = new HashMap<Integer, TemporalWeight>();
			infectors.put(v, new TemporalWeight(1, weight, timeStep));
		} else {
			if (infectors.containsKey(v)) {
				TemporalWeight tc = infectors.get(v);
				tc.update(1, weight, timeStep);
				infectors.put(v, tc);
			} else {
				infectors.put(v, new TemporalWeight(1, weight, timeStep));
			}
		}
	}

	/****
	 * update a diffusion instance: u diffuses the item to v
	 * 
	 * @param u
	 * @param v
	 */
	public void updateDiffusion(int u, int v, double weight, int timeStep) {
		// update number of diffusions
		totalNDiffusions++;
		totalNExponentialDiffusions += weight;

		// update the diffusion logs
		isDiffusedFlag = true;
		if (diffusions == null)
			diffusions = new HashMap<Integer, HashMap<Integer, TemporalWeight>>();
		if (diffusions.containsKey(u)) {
			HashMap<Integer, TemporalWeight> infectedReceivers = diffusions.get(u);
			if (!infectedReceivers.containsKey(v)) {
				infectedReceivers.put(v, new TemporalWeight(1, weight, timeStep));
				diffusions.put(u, infectedReceivers);
			} else {
				TemporalWeight tc = infectedReceivers.get(v);
				tc.update(1, weight, timeStep);
				infectedReceivers.put(v, tc);
				diffusions.put(u, infectedReceivers);
			}
		} else {
			HashMap<Integer, TemporalWeight> infectedReceivers = new HashMap<Integer, TemporalWeight>();
			infectedReceivers.put(v, new TemporalWeight(1, weight, timeStep));
			diffusions.put(u, infectedReceivers);
		}
		if (infections == null)
			infections = new HashMap<Integer, HashMap<Integer, TemporalWeight>>();
		if (infections.containsKey(v)) {
			HashMap<Integer, TemporalWeight> diffusingSenders = infections.get(v);
			if (!diffusingSenders.containsKey(u)) {
				diffusingSenders.put(u, new TemporalWeight(1, weight, timeStep));
				infections.put(v, diffusingSenders);
			} else {
				TemporalWeight tc = diffusingSenders.get(u);
				tc.update(1, weight, timeStep);
				diffusingSenders.put(u, tc);
				infections.put(v, diffusingSenders);
			}
		} else {
			HashMap<Integer, TemporalWeight> diffusingSenders = new HashMap<Integer, TemporalWeight>();
			diffusingSenders.put(u, new TemporalWeight(1, weight, timeStep));
			infections.put(v, diffusingSenders);
		}
	}

	/***
	 * To remove this item
	 */
	public void remove() {
		isRemoved = true;

		retweets = null;
		senders = null;
		diffusers = null;
		diffusions = null;
		infections = null;
		infectors = null;

		totalNAdoptions = 0;
		totalNExponentialAdoptions = 0;
		totalNDiffusions = 0;
		totalNExponentialDiffusions = 0;
		totalNExposures = 0;
		totalNExponentialExposures = 0;

		isDiffusedFlag = false;
		virality = 0;
	}

	/***
	 * update alias sampler of a sender
	 * 
	 * @param senderIndex
	 */
	private void updateSenderAlias(int senderIndex) {
		if (senderAliases == null)
			senderAliases = new HashMap<Integer, AliasSampler>();
		AliasSampler alias = new AliasSampler(diffusions.get(senderIndex));
		senderAliases.put(senderIndex, alias);
	}

	/***
	 * udate alias sample of a receiver
	 * 
	 * @param v
	 */
	private void updatedReceiverAlias(int v) {
		if (receiverAliases == null)
			receiverAliases = new HashMap<Integer, AliasSampler>();
		AliasSampler alias = new AliasSampler(infections.get(v));
		receiverAliases.put(v, alias);
	}

	/***
	 * update the aliases
	 */
	public void updateAliases() {
		if (!isDiffusedFlag)
			return;
		Iterator<Map.Entry<Integer, TemporalWeight>> iter = diffusers.entrySet().iterator();
		int u = -1;
		while (iter.hasNext()) {
			u = iter.next().getKey();
			updateSenderAlias(u);
		}

		iter = infectors.entrySet().iterator();
		int v = -1;
		while (iter.hasNext()) {
			v = iter.next().getKey();
			updatedReceiverAlias(v);
		}

	}

	/***
	 * sample a sender who diffused to a given receiver
	 * 
	 * @param v
	 * @return
	 */
	public int sampleSender(int v) {
		return receiverAliases.get(v).sample();
	}

	/***
	 * sample a receiver who infected by a given sender
	 * 
	 * @param u
	 * @return
	 */
	public int sampleReceiver(int u) {
		return senderAliases.get(u).sample();
	}

	/***
	 * randomly step to a receiver infected by a given sender
	 * 
	 * @param u
	 * @return
	 */
	public int stepToReceiver(int u) {
		return senderAliases.get(u).sample();
	}

	/***
	 * randomly step to a sender diffused to given receiver
	 * 
	 * @param v
	 * @return
	 */
	public int stepToSender(int v) {
		return receiverAliases.get(v).sample();
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
	 * /**** get top retweeted tweets and remove old retweeted tweets
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
	 * get top diffusers
	 * 
	 * @return
	 */
	public TemporalWeight[] getTopDiffuser() {
		if (isRemoved)
			return null;
		if (!isDiffusedFlag)
			return null;
		else {
			TemporalWeight[] retweetedUsers = new TemporalWeight[diffusers.size()];
			Iterator<Map.Entry<Integer, TemporalWeight>> iter = diffusers.entrySet().iterator();
			int j = 0;
			while (iter.hasNext()) {
				Map.Entry<Integer, TemporalWeight> pair = iter.next();
				retweetedUsers[j] = pair.getValue();
				retweetedUsers[j].index = pair.getKey();
				j++;
			}
			return retweetedUsers;
		}
	}

	/****
	 * get top infectors
	 * 
	 * @return
	 */
	public TemporalWeight[] getTopInfectors() {
		if (isRemoved)
			return null;
		if (!isDiffusedFlag)
			return null;
		else {
			TemporalWeight[] retweetingUsers = new TemporalWeight[infectors.size()];
			Iterator<Map.Entry<Integer, TemporalWeight>> iter = infectors.entrySet().iterator();
			int j = 0;
			while (iter.hasNext()) {
				Map.Entry<Integer, TemporalWeight> pair = iter.next();
				retweetingUsers[j] = pair.getValue();
				retweetingUsers[j].index = pair.getKey();
				j++;
			}
			return retweetingUsers;
		}
	}

	/***
	 * print all information about this item
	 */
	public void printProfile() {
		System.out.println("name = " + name);
		System.out.println("nSenders = " + senders.size());
		System.out.println("isDiffusedFlag = " + isDiffusedFlag);
		System.out.println("lastUpdateTime = " + lastUpdateTime);
		System.out.println("isRemoved = " + isDiffusedFlag);
		System.out.println("nNewDiffusions = " + nNewDiffusions);
		System.out.println("isRecycledIndex = " + isRecycledIndex);
		System.out.println("nDiffusers = " + diffusers.size());
		System.out.println("nInfectors = " + infectors.size());
		System.out.println("senders:");
		Iterator<Map.Entry<Integer, TemporalWeight>> sIter = senders.entrySet().iterator();
		while (sIter.hasNext()) {
			Map.Entry<Integer, TemporalWeight> pair = sIter.next();
			System.out.println("----- " + pair.getKey() + " (" + pair.getValue().count + " times)");
		}
		System.out.println("diffusers:");
		Iterator<Map.Entry<Integer, HashMap<Integer, TemporalWeight>>> iter = diffusions.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<Integer, HashMap<Integer, TemporalWeight>> pair = iter.next();
			System.out.println("----- " + pair.getKey() + " (" + pair.getValue().size() + " infectors)");
		}
		System.out.println("infectors:");
		iter = infections.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<Integer, HashMap<Integer, TemporalWeight>> pair = iter.next();
			System.out.println("----- " + pair.getKey() + " (" + pair.getValue().size() + " diffusers)");
		}

	}
}
