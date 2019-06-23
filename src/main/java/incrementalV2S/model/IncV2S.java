/***
 * Incremental V2S model
 */
package incrementalV2S.model;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.twitter.Extractor;

import incrementalV2S.data.OutputStream;
import incrementalV2S.data.TweetStream;
import incrementalV2S.data.impl.FileTweetStream;
import incrementalV2S.data.impl.HardcodedBlacklistedUsers;
import incrementalV2S.data.impl.LoggerOutputStream;
import incrementalV2S.model.Parameters;
import incrementalV2S.model.Sender;
import incrementalV2S.model.TemporalWeight;
import incrementalV2S.model.Tweet;
import util.TimeLib;

public class IncV2S {

	private static Logger logger = LoggerFactory.getLogger(IncV2S.class);

	public String outputPath;

	private double exponentialRate;
	public long referenceTime;
	public ZonedDateTime zdt;
	public SimpleDateFormat tweetTimeFormat;
	
	private static TweetStream tweetStream;
	private static OutputStream outputStream;
	public boolean outChannel;

	private HashMap<String, InMemoryUser> userId2Index;
	private HashMap<Integer, Integer> userIndex2SenderIndex;
	private HashMap<Integer, Integer> userIndex2ReceiverIndex;
	private HashMap<String, Integer> itemId2Index;

	private Sender[] senders;
	private int senderCount;

	private InformationItem[] items;
	private int itemCount;
	private LinkedList<Integer> itemQue;

	private Receiver[] receivers;
	private int receiverCount;

	private ImmutableSet<Long> blackListedUsers;

	private HashMap<Integer, RandomWalk> randomWalks;// random walks
														// on
														// diffusion
														// graph
	private int totalNVisits;// total number of visits by random walks,
								// used to normalize diffusion weight for
								// each node
	private HashSet<Integer> affectedSenders;
	private HashSet<Integer> affectedReceivers;
	private HashSet<Integer> affectedItems;
	private Extractor tweetTextProcesser = new Extractor();

	private int currentWalkUpdateTimeStep = 0;
	private int currentScoreUpdateTimeStep = 0;
	private int nWalks = 0;
	private int globalWalkIndex = 0;

	private Tweet currentTweet;

	public double entropyWeight;

	private JsonParser jsonParser;

	private HashMap<Integer, Integer> relatedSenders;
	private HashMap<Integer, Integer> relatedReceivers;

	// utility variables

	private List<String> allHashtags;
	private HashSet<Integer> selectedHashtags;

	private Iterator<Integer> intIter;
	private Iterator<String> stringIter;
	private Iterator<Map.Entry<Integer, Integer>> intIntIter;

	private int senderUpdateCount;
	private int receiverUpdateCount;
	private int itemUpdateCount;
	private int nToUpdateWalkCall;
	private int nToUpdateScoreCall;
	private int nGetTweet;
	private int nUpdate;
	private int nHashtagExtraction;

	/***
	 * initialize tweet stream
	 * 
	 * @throws ConfigurationException
	 */
	private void initStreams(String dataPath, String dataset_filename) {
		tweetStream = new FileTweetStream(dataPath, dataset_filename);
		outputStream = new LoggerOutputStream();
	}

	/***
	 * check if it is necessary to update random walks
	 * 
	 * @return
	 */
	private boolean toUpdateWalk(long time) {
		// int timeStep = TimeLib.getElapsedTime(time, referenceTime);
		// to be changed once the tweets are timely ordered
		int timeStep = Math.max(currentWalkUpdateTimeStep,
				TimeLib.getElapsedTimeSteps(time, referenceTime, Parameters.REWALK_TIME_INTERVAL));

		nToUpdateWalkCall++;
		if (nToUpdateWalkCall % 1000 == 0) {
			logger.info(" called toUpdateWalk() {} times", nToUpdateWalkCall);
			logger.info("timeStep = {} currentWalkUpdateTimeStep = {}", timeStep, currentWalkUpdateTimeStep);
		}

		if (timeStep > currentWalkUpdateTimeStep) {
			currentWalkUpdateTimeStep = timeStep;
			return true;
		} else
			return false;
	}

	/****
	 * Check if it is the time to update scores
	 * 
	 * @param time
	 * @return
	 */

	private boolean toUpdateScores(long time) {

		// int timeStep = TimeLib.getElapsedTime(time, referenceTime);
		// to be changed once the tweets are timely ordered
		int timeStep = Math.max(currentScoreUpdateTimeStep,
				TimeLib.getElapsedTimeSteps(time, referenceTime, Parameters.UPDATE_SCORE_TIME_INTERVAL));

		nToUpdateScoreCall++;
		if (nToUpdateScoreCall % 1000 == 0) {
			logger.info(" called toUpdateScore() {} times", nToUpdateScoreCall);
			logger.info("timeStep = {} currentScoreUpdateTimeStep = {}", timeStep, currentScoreUpdateTimeStep);
		}

		if (timeStep > currentScoreUpdateTimeStep) {
			currentScoreUpdateTimeStep = timeStep;
			return true;
		} else
			return false;
	}

	/***
	 * 
	 * @return get a new tweet, store in currentTweet
	 */
	private void getNewTweet() {
		currentTweet = new Tweet();
		JsonObject currentTweetJson = (JsonObject) jsonParser.parse(tweetStream.getTweet());
		JsonObject user = (JsonObject) currentTweetJson.get("user");
		currentTweet.authorId = String.valueOf(user.get("id").getAsLong());

		nGetTweet++;
		if (nGetTweet % 1000 == 0) {
			logger.info("call tweetStream.getTweet() {} times", nGetTweet);
		}

		while (blackListedUsers.contains(Long.parseLong(currentTweet.authorId))) {
			currentTweetJson = (JsonObject) jsonParser.parse(tweetStream.getTweet());
			user = (JsonObject) currentTweetJson.get("user");
			currentTweet.authorId = String.valueOf(user.get("id").getAsLong());

			nGetTweet++;
			if (nGetTweet % 1000 == 0) {
				logger.info("call tweetStream.getTweet() {} times", nGetTweet);
			}
		}

		// put into the userid-index hashmap if this is a new user
		if (!userId2Index.containsKey(currentTweet.authorId)) {
			currentTweet.authorScreenName = user.get("screenName").toString().replace("\"", "");
			userId2Index.put(currentTweet.authorId,
					new InMemoryUser(currentTweet.authorScreenName, userId2Index.size()));
		}

		currentTweet.tweetId = String.valueOf(currentTweetJson.get("id").getAsLong());
		currentTweet.content = currentTweetJson.get("text").toString();
		currentTweet.publishedTime = TimeLib.dateStringToLong(currentTweetJson.get("createdAt").toString(),
				tweetTimeFormat);

		if (currentTweetJson.has("retweetedStatus")) {
			JsonObject originalTweet = (JsonObject) currentTweetJson.get("retweetedStatus");
			JsonObject originalAuthor = (JsonObject) originalTweet.get("user");

			currentTweet.originalTweetId = String.valueOf(originalTweet.get("id").getAsLong());
			currentTweet.originalAuthorId = String.valueOf(originalAuthor.get("id").getAsLong());
			// put into the userid-index hashmap if this is a new user
			if (!userId2Index.containsKey(currentTweet.originalAuthorId)) {
				currentTweet.originalAuthorScreenName = originalAuthor.get("screenName").toString().replace("\"", "");
				userId2Index.put(currentTweet.originalAuthorId,
						new InMemoryUser(currentTweet.originalAuthorScreenName, userId2Index.size()));
			}

		}

	}

	/***
	 * check if the item is valid
	 * 
	 * @param item
	 * @return
	 */
	private boolean isValidItem(String item) {
		int nChar = 0;
		int nDigit = 0;
		int l = item.length();
		if (l < Parameters.MIN_HASHTAG_LENGTH)
			return false;
		if (l > Parameters.MAX_HASHTAG_LENGTH)
			return false;
		for (int i = 0; i < l; i++) {
			char c = item.charAt(i);
			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
				nChar++;
			else if (Character.isDigit(c))
				nDigit++;
			else {
			}
		}
		if (nDigit + nChar < l)
			return false;
		return true;
	}

	/***
	 * 
	 * @param tweet
	 * @return set of information items mentioned in the tweet
	 */
	private void extractInformationItems(Tweet tweet, boolean isRetweet) {
		nHashtagExtraction++;
		if (nHashtagExtraction % 1000 == 0) {
			logger.info("call extractInformationItems() {} times", nHashtagExtraction);
		}
		allHashtags = tweetTextProcesser.extractHashtags(tweet.content);
		selectedHashtags = new HashSet<Integer>();
		stringIter = allHashtags.iterator();
		String h = null;
		while (stringIter.hasNext()) {
			h = stringIter.next().toLowerCase();
			if (!isValidItem(h))
				continue;
			if (!itemId2Index.containsKey(h)) {
				if (isRetweet) {
					// the case when the hashtag in retweet is truncated and is
					// not the same with the hashtag in the original tweet
					continue;
				}
				if (itemQue.size() > 0) {
					int index = itemQue.remove();
					itemId2Index.put(h, index);
					items[index] = new InformationItem(h, TimeLib.getElapsedTimeSteps(tweet.publishedTime,
							referenceTime, Parameters.UPDATE_SCORE_TIME_INTERVAL), true);
				} else {
					itemId2Index.put(h, itemCount);
					items[itemCount] = new InformationItem(h, TimeLib.getElapsedTimeSteps(tweet.publishedTime,
							referenceTime, Parameters.UPDATE_SCORE_TIME_INTERVAL), false);
					itemCount++;
				}
			}
			int hIndex = itemId2Index.get(h);
			selectedHashtags.add(hIndex);
		}
		if (selectedHashtags.size() == 0)
			selectedHashtags = null;
	}

	/***
	 * 
	 * @param tweet
	 * @return true if the tweet induces adoption(s) due to viral diffusion (i.e.
	 *         retweet)
	 * @return false otherwise
	 */
	private boolean isADiffusion(Tweet tweet) {
		return (tweet.originalAuthorId != null);
	}

	/***
	 * To update adopting list of a sender
	 * 
	 * @param senderIndex
	 * @param adoptedItems
	 * @param time
	 */
	private void updateSenderAdoption(int senderIndex, HashSet<Integer> adoptedItems, double weight) {
		intIter = adoptedItems.iterator();
		int x = -1;
		while (intIter.hasNext()) {
			x = intIter.next();
			senders[senderIndex].addAdoptedItem(x, weight);
		}
	}

	/***
	 * To update diffusing list of a sender
	 * 
	 * @param senderIndex
	 * @param diffusedItems
	 * @param receiverId
	 * @param time
	 */
	private boolean updateSenderDiffusion(int senderIndex, int receiverIndex, HashSet<Integer> diffusedItems,
			double weight) {
		senderUpdateCount++;
		if (senderUpdateCount % 1000 == 0) {
			logger.info("call updateSenderDiffusion() {} times", senderUpdateCount);
		}

		boolean flag = false;
		intIter = diffusedItems.iterator();
		int n = 0;
		int x = -1;
		while (intIter.hasNext()) {
			x = intIter.next();
			if (senders[senderIndex].addDiffusedItem(x, weight)) {
				n++;
				flag = true;
				if (x == 1304) {
					logger.info("-----1304: u = {} x = {} x[name] = {}", senderIndex, x, items[x].name);
				}
			}
		}
		if (n > 0) {
			senders[senderIndex].nNewDiffusions += n;
			senders[senderIndex].addInfector(receiverIndex, n, n * weight);
		}
		return flag;

	}

	/****
	 * To update adopting list of a receiver
	 * 
	 * @param receiverIndex
	 * @param adoptedItems
	 * @param time
	 * @param senderIndex
	 */
	private boolean updateReceiverAdoption(int receiverIndex, HashSet<Integer> adoptedItems, double weight,
			int senderIndex) {
		receiverUpdateCount++;
		if (receiverUpdateCount % 1000 == 0) {
			logger.info("call updateReceiverAdoption() {} times", receiverUpdateCount);
		}

		boolean flag = false;
		intIter = adoptedItems.iterator();
		int n = 0;
		int x = -1;
		while (intIter.hasNext()) {
			x = intIter.next();
			// the case when the hashtag in retweet is truncated and is not
			// the same with the hashtag in the original tweet
			if (!senders[senderIndex].isDiffused(x))
				continue;
			receivers[receiverIndex].addAdoptedItem(x, weight);
			n++;
			flag = true;
		}
		if (n > 0) {
			receivers[receiverIndex].nNewDiffusions += n;
			receivers[receiverIndex].addDiffuser(senderIndex, n, n * weight);
		}
		return flag;
	}

	/***
	 * To update adopter list of an item
	 * 
	 * @param userIndex
	 * @param srFlag    = true if the user is the sender, = false otherwise
	 * @param itemIndex
	 */
	private void updateItems(int userIndex, int itemIndex, boolean srFlag, double weight, int timeStep) {
		itemUpdateCount++;
		if (itemUpdateCount % 1000 == 0) {
			logger.info("call updateItems() {} times", itemUpdateCount);
		}
		if (srFlag)
			items[itemIndex].updateDiffusers(userIndex, weight, timeStep);
		else
			items[itemIndex].updateInfectors(userIndex, weight, timeStep);
	}

	/***
	 * remove all the item that is last updated before itemMaxTimeWithoutUpdate time
	 * steps
	 */
	private void removeOldItems() {
		Iterator<Map.Entry<Integer, TemporalWeight>> inIter = null;
		Map.Entry<Integer, TemporalWeight> inPair = null;
		Iterator<Map.Entry<Integer, HashMap<Integer, TemporalWeight>>> outIter = null;
		Map.Entry<Integer, HashMap<Integer, TemporalWeight>> outPair = null;
		int u = -1;
		int v = -1;
		for (int x = 0; x < itemCount; x++) {
			if (items[x].isRemoved)
				continue;
			if (items[x].lastUpdateTime < currentScoreUpdateTimeStep - Parameters.MAX_ITEM_AGE) {
				// remove from senders
				// System.out.println("remove from senders x = " + x);
				if (items[x].senders != null) {
					inIter = items[x].senders.entrySet().iterator();
					while (inIter.hasNext()) {
						u = inIter.next().getKey();
						// System.out.println("u = " + u);
						senders[u].removeAnItem(x);
					}
				}
				// remove from infectors
				// System.out.println("remove from infectors x = " + x);
				if (items[x].infections != null) {
					outIter = items[x].infections.entrySet().iterator();
					while (outIter.hasNext()) {
						v = outIter.next().getKey();
						// System.out.println("v = " + v);
						receivers[v].removeAnItem(x);
					}
				}
				// remove who diffuses to whom
				// System.out.println("remove who diffuses to whom x = " + x);
				if (items[x].diffusions != null) {
					outIter = items[x].diffusions.entrySet().iterator();
					while (outIter.hasNext()) {
						outPair = outIter.next();
						u = outPair.getKey();
						affectedSenders.add(u);
						inIter = outPair.getValue().entrySet().iterator();
						while (inIter.hasNext()) {
							inPair = inIter.next();
							v = inPair.getKey();
							senders[u].reduceInfectorAdoptions(v, inPair.getValue().weight);
							receivers[v].reduceADiffuser(u, inPair.getValue().weight);
							affectedReceivers.add(v);
						}
					}
				}
				// remove from item
				itemId2Index.remove(items[x].name);
				items[x].remove();
				itemQue.add(x);
			}
		}
		// update diffusion and infection flags
		for (u = 0; u < senderCount; u++) {
			senders[u].updateDiffusionFlag();
		}
		for (v = 0; v < receiverCount; v++) {
			receivers[v].updateInfectionFlag();
		}
	}

	/***
	 * make a random walk from a user
	 * 
	 * @param userIndex
	 * @param srFlag
	 * @return
	 */
	private RandomWalk makeAWalk(int userIndex, boolean srFlag) {
		RandomWalk randomWalk = new RandomWalk();
		randomWalk.walks = new Walk[Parameters.MAX_WALK_LENGTH];
		randomWalk.startUserIndex = userIndex;
		randomWalk.flag = srFlag;
		int u = -1;
		int v = -1;
		int x = -1;
		int l = 0;
		if (srFlag) {// start from a sender
			u = userIndex;
			randomWalk.startUserIndex = u;
			while (l < Parameters.MAX_WALK_LENGTH) {
				// System.out.printf("walk from sender number %d", u);
				// keep walking
				randomWalk.walks[l] = new Walk();
				// sender
				randomWalk.walks[l].senderIndex = u;
				// item
				x = senders[u].step();
				randomWalk.walks[l].itemIndex = x;
				// System.out.printf(" to item %d ", x);
				// receiver
				v = items[x].stepToReceiver(u);
				randomWalk.walks[l].receiverIndex = v;
				// System.out.printf(" to receiver %d \n", v);
				// direction
				randomWalk.walks[l].direction = true;

				// check stop condition
				if (l >= Parameters.MAX_WALK_LENGTH - 1)
					break;
				if (Parameters.rand.nextDouble() < Parameters.WALK_JUMPING_PROB)
					break;
				// keep walking
				l++;
				// System.out.printf("walk from receiver %d ", v);
				randomWalk.walks[l] = new Walk();
				// receiver
				randomWalk.walks[l].receiverIndex = v;
				// item
				x = receivers[v].step();
				randomWalk.walks[l].itemIndex = x;
				// System.out.printf(" to item %d ", x);
				// sender
				u = items[x].stepToSender(v);
				randomWalk.walks[l].senderIndex = u;
				// System.out.printf(" to sender %d\n", u);
				// direction
				randomWalk.walks[l].direction = false;
				// check stop condition
				if (l >= Parameters.MAX_WALK_LENGTH - 1)
					break;
				if (Parameters.rand.nextDouble() < Parameters.WALK_JUMPING_PROB)
					break;
				// keep walking
				l++;
			}
		} else {// start from a receiver
			v = userIndex;
			randomWalk.startUserIndex = v;
			while (l < Parameters.MAX_WALK_LENGTH) {
				// keep walking
				randomWalk.walks[l] = new Walk();
				// receiver
				randomWalk.walks[l].receiverIndex = v;
				// System.out.printf("walk from receiver %d ", v);
				// item
				x = receivers[v].step();
				randomWalk.walks[l].itemIndex = x;
				// System.out.printf("to item %d ", x);
				// sender
				u = items[x].stepToSender(v);
				randomWalk.walks[l].senderIndex = u;
				// System.out.printf(" to sender %d\n", u);
				// direction
				randomWalk.walks[l].direction = false;

				// check stop condition
				if (l >= Parameters.MAX_WALK_LENGTH - 1)
					break;
				if (Parameters.rand.nextDouble() < Parameters.WALK_JUMPING_PROB)
					break;
				// keep walking
				l++;
				// System.out.println("walk from sender " + senderId);
				randomWalk.walks[l] = new Walk();
				// sender
				randomWalk.walks[l].senderIndex = u;
				// System.out.printf(" walk from sender %d ", u);
				// item
				x = senders[u].step();
				randomWalk.walks[l].itemIndex = x;
				// System.out.printf(" to item %d ", x);
				// receiver
				v = items[x].stepToReceiver(u);
				randomWalk.walks[l].receiverIndex = v;
				// System.out.printf(" to receiver %d\n", v);
				// direction
				randomWalk.walks[l].direction = true;

				// check stop condition
				if (l >= Parameters.MAX_WALK_LENGTH - 1)
					break;
				if (Parameters.rand.nextDouble() < Parameters.WALK_JUMPING_PROB)
					break;
				// keep walking
				l++;
			}
		}
		randomWalk.length = l + 1;
		return randomWalk;
	}

	/***
	 * remove a previously generated walk
	 * 
	 * @param walkIndex
	 */
	private void removeAWalk(RandomWalk randomWalk, int walkIndex) {
		// remove from sender side
		HashMap<Integer, Integer> relatedSenders = randomWalk.getRelatedSenderIndexes();
		intIntIter = relatedSenders.entrySet().iterator();
		int u = -1;
		while (intIntIter.hasNext()) {
			Map.Entry<Integer, Integer> sPair = intIntIter.next();
			u = sPair.getKey();
			senders[u].removeRelatedRandomWalk(walkIndex);
		}
		// remove from receiver side
		HashMap<Integer, Integer> relatedReceivers = randomWalk.getRelatedReceiverIndexes();
		intIntIter = relatedReceivers.entrySet().iterator();
		int v = 0;
		while (intIntIter.hasNext()) {
			Map.Entry<Integer, Integer> rPair = intIntIter.next();
			v = rPair.getKey();
			receivers[v].removeRelatedRandomWalk(walkIndex);
		}
		// remove the random walk
		totalNVisits -= (randomWalk.length + 1);
		randomWalks.remove(walkIndex);
	}

	/***
	 * Add a newly (re -) generated random walk
	 * 
	 * @param randomWalk
	 * @param walkIndex
	 */
	private void addARandomWalk(RandomWalk randomWalk, int walkIndex) {
		// add from sender side
		relatedSenders = randomWalk.getRelatedSenderIndexes();
		Iterator<Map.Entry<Integer, Integer>> iter = relatedSenders.entrySet().iterator();
		int u = 0;
		while (iter.hasNext()) {
			Map.Entry<Integer, Integer> sPair = iter.next();
			u = sPair.getKey();
			int times = sPair.getValue();
			senders[u].addRelatedRandomWalk(walkIndex, times);
		}
		// add from receiver side
		relatedReceivers = randomWalk.getRelatedReceiverIndexes();
		iter = relatedReceivers.entrySet().iterator();
		int v = 0;
		while (iter.hasNext()) {
			Map.Entry<Integer, Integer> rPair = iter.next();
			v = rPair.getKey();
			int times = rPair.getValue();
			receivers[v].addRelatedRandomWalk(walkIndex, times);
		}
		// add the random walk
		totalNVisits += (randomWalk.length + 1);
		randomWalks.put(walkIndex, randomWalk);
	}

	/***
	 * incrementally recompute the random walks
	 */
	private void updateWalks(Tweet tweet) {
		if (!toUpdateWalk(tweet.publishedTime)) {
			// System.out.println("----no need to update random walks");
			return;
		}
		// remove old items
		removeOldItems();
		updateEntropies();
		// do update
		logger.info("----updating random walks");
		long start = System.currentTimeMillis();
		// get walks need to be updated due to changes in senders
		// and get new walks for new diffusing senders and infected receivers
		// System.out.println("------getting senders need to be updated");
		int nRemovedWalks = 0;
		int nReWalks = 0;
		int nNewWalks = 0;
		HashMap<Integer, HashSet<Integer>> walksToUpdate = new HashMap<Integer, HashSet<Integer>>();
		HashSet<Integer> newDiffusingSenders = new HashSet<Integer>();
		Iterator<Integer> iter = affectedSenders.iterator();
		int u = -1;
		int walkIndex = -1;
		Iterator<Map.Entry<Integer, Integer>> wIter = null;
		while (iter.hasNext()) {
			u = iter.next();
			if (!senders[u].diffusedFlag)
				continue;
			senders[u].updateAlias();
			if (senders[u].nVisits == 0) {
				newDiffusingSenders.add(u);
				continue;
			}
			wIter = senders[u].relatedRandomWalks.entrySet().iterator();
			while (wIter.hasNext()) {
				Map.Entry<Integer, Integer> wPair = wIter.next();
				walkIndex = wPair.getKey();
				if (walksToUpdate.containsKey(walkIndex)) {
					HashSet<Integer> relatedUsers = walksToUpdate.get(walkIndex);
					relatedUsers.add(u);
					walksToUpdate.put(walkIndex, relatedUsers);
				} else {
					HashSet<Integer> relatedUsers = new HashSet<Integer>();
					relatedUsers.add(u);
					walksToUpdate.put(walkIndex, relatedUsers);
				}
			}
		}
		// get walks need to be updated due to changes in receivers
		// System.out.println("------getting receivers need to be updated");
		HashSet<Integer> newInfectedReceivers = new HashSet<Integer>();
		iter = affectedReceivers.iterator();
		int v = -1;
		while (iter.hasNext()) {
			v = iter.next();
			if (!receivers[v].infectedFlag)
				continue;
			receivers[v].updateAlias();
			if (receivers[v].nVisits == 0) {
				newInfectedReceivers.add(v);
				continue;
			}
			wIter = receivers[v].relatedRandomWalks.entrySet().iterator();
			while (wIter.hasNext()) {
				Map.Entry<Integer, Integer> wPair = wIter.next();
				walkIndex = wPair.getKey();
				if (walksToUpdate.containsKey(walkIndex)) {
					HashSet<Integer> relatedUsers = walksToUpdate.get(walkIndex);
					relatedUsers.add(-v);
					walksToUpdate.put(walkIndex, relatedUsers);
				} else {
					HashSet<Integer> relatedUsers = new HashSet<Integer>();
					relatedUsers.add(-v);
					walksToUpdate.put(walkIndex, relatedUsers);
				}
			}
		}

		// update aliases of items
		iter = affectedItems.iterator();
		int x = -1;
		while (iter.hasNext()) {
			x = iter.next();
			items[x].updateAliases();
		}

		// update walks
		// System.out.println("------updating walks");
		Iterator<Map.Entry<Integer, HashSet<Integer>>> updateWalkIter = walksToUpdate.entrySet().iterator();
		boolean srFlag = false;
		RandomWalk oldWalk = null;
		RandomWalk newWalk = null;
		Map.Entry<Integer, HashSet<Integer>> walkPair = null;
		// Iterator<Integer> ruIter = null;
		while (updateWalkIter.hasNext()) {
			walkPair = updateWalkIter.next();
			walkIndex = walkPair.getKey();

			// System.out.printf("removing walk %d: ", walkIndex);
			oldWalk = randomWalks.get(walkIndex);
			removeAWalk(oldWalk, walkIndex);
			nWalks--;
			// System.out.println(" done");
			u = oldWalk.startUserIndex;
			srFlag = oldWalk.flag;
			nRemovedWalks++;
			// the case when the users' diffusion logs are all deleted due to
			// deleting old items
			if (srFlag) {
				if (!senders[u].diffusedFlag)
					continue;
			} else {
				if (!receivers[u].infectedFlag)
					continue;
			}
			newWalk = makeAWalk(u, srFlag);
			// System.out.printf("rewalking walk %d: ", walkIndex);
			nWalks++;
			nReWalks++;
			addARandomWalk(newWalk, walkIndex);
			// System.out.println(" done");
		}

		// new walks from new diffusing senders
		iter = newDiffusingSenders.iterator();
		while (iter.hasNext()) {
			u = iter.next();
			newWalk = makeAWalk(u, true);
			nNewWalks++;
			// System.out.printf("adding walk %d... ", globalWalkIndex);
			globalWalkIndex++;
			nWalks++;
			addARandomWalk(newWalk, globalWalkIndex);
			// System.out.println(" done");
		}
		// new walks from new infected receivers
		iter = newInfectedReceivers.iterator();
		while (iter.hasNext()) {
			v = iter.next();
			newWalk = makeAWalk(v, false);
			nNewWalks++;
			// System.out.printf("adding walk %d... ", globalWalkIndex);
			globalWalkIndex++;
			nWalks++;
			addARandomWalk(newWalk, globalWalkIndex);
			// System.out.println(" done");
		}
		// System.out.printf("%d removed %d rewalked %d new total = %d\n",
		// nRemovedWalks, nReWalks, nNewWalks, nUserWalks);
		// System.out.println("computing new scores");
		long end = System.currentTimeMillis();
		// System.out.println("outputting new scores");

		affectedSenders = new HashSet<Integer>();
		affectedReceivers = new HashSet<Integer>();
		affectedItems = new HashSet<Integer>();
		// System.out.println("walks and scores updating done");

		// System.out
		// // .printf("inc-%s,%d,%d removed,%d rewalked,%d new,total =
		// // %d,time = %d\n",
		// .printf("inc-model,%d,%d,%d,%d,%d,%d\n", currentWalkUpdateTimeStep,
		// nRemovedWalks, nReWalks, nNewWalks,
		// nWalks, (end - start));
		logger.info("{}", String.format(
				// .printf("inc-%s,%d,%d removed,%d rewalked,%d
				// new,total =
				// %d,time = %d\n",
				"inc-model,%d,%d,%d,%d,%d,%d%n", currentWalkUpdateTimeStep, nRemovedWalks, nReWalks, nNewWalks, nWalks,
				(end - start)));
	}

	private void updateEntropies() {
		for (int x = 0; x < itemCount; x++) {
			if (items[x].isDiffusedFlag) {
				items[x].getEntropies();

			}
		}
		for (int u = 0; u < senderCount; u++) {
			if (senders[u].diffusedFlag) {
				senders[u].getDiffusingEntropies();
				senders[u].getInfectorEntropies();
			}
		}
		for (int v = 0; v < receiverCount; v++) {
			if (receivers[v].infectedFlag) {
				receivers[v].getEntropies();
			}
		}
	}

	private void updateScore(Tweet tweet) {
		if (!toUpdateScores(tweet.publishedTime)) {
			return;
		}
		logger.info("updating scores");
		computeScores(entropyWeight);
		if (outChannel) {
			logger.info("writing scores to files ... ");
			outputScoresToFiles("", true);
			logger.info("done!");
		} else {
			logger.info("inserting scores to database ... ");
			// TO-DO: to insert scores to databases by calling outputStream's functions
			logger.info("done!");
		}
	}

	/***
	 * To update the diffusion graph whenever a new tweet comes
	 * 
	 * @param tweet
	 * 
	 */
	private void update() {
		nUpdate++;
		if (nUpdate % 1000 == 0) {
			logger.info("call update() {} times", nUpdate);
		}
		boolean isDiffusion = isADiffusion(currentTweet);
		extractInformationItems(currentTweet, isDiffusion);
		HashSet<Integer> mentionedItems = selectedHashtags;
		if (mentionedItems == null) {
			// System.out.println("--no information item found in the tweet");
			return;
		}
		// System.out.printf("\t#items = %d", mentionedItems.size());
		double weight = Math.pow(exponentialRate, currentScoreUpdateTimeStep);
		// System.out.println("--updating changes due to the tweet");
		if (isDiffusion) {
			// System.out.println("\tdiffusion\t");
			// update id-2-index maps if needed
			int nu = userId2Index.get(currentTweet.originalAuthorId).index;
			String uScreenName = userId2Index.get(currentTweet.originalAuthorId).screenName;
			int nv = userId2Index.get(currentTweet.authorId).index;
			String vScreenName = userId2Index.get(currentTweet.authorId).screenName;
			if (!userIndex2SenderIndex.containsKey(nu)) {
				userIndex2SenderIndex.put(nu, senderCount);
				senders[senderCount] = new Sender(currentTweet.originalAuthorId, uScreenName);
				senderCount++;
			}
			if (!userIndex2ReceiverIndex.containsKey(nv)) {
				userIndex2ReceiverIndex.put(nv, receiverCount);
				receivers[receiverCount] = new Receiver(currentTweet.authorId, vScreenName);
				receiverCount++;
			}
			// get sender and receiver index
			int u = userIndex2SenderIndex.get(nu);
			int v = userIndex2ReceiverIndex.get(nv);

			// diffusion logs

			// String diffLogs = "";
			// update at sender side
			// System.out.println("----updating at the sender side");
			boolean updatedFlag = updateSenderDiffusion(u, v, mentionedItems, weight);
			if (updatedFlag) {
				affectedSenders.add(u);
				// diffLogs += currentTweet.originalAuthorId;
			}
			// update at receiver side
			// System.out.println("----updating at the receiver side");
			updatedFlag = updateReceiverAdoption(v, mentionedItems, weight, u);
			if (updatedFlag) {
				affectedReceivers.add(v);
				// diffLogs += "\t" + currentTweet.authorId;
			}
			// update at item side
			// System.out.println("----updating at the item side");
			Iterator<Integer> iter = mentionedItems.iterator();
			int x = -1;
			while (iter.hasNext()) {
				x = iter.next();
				// the case when the hashtag in retweet is truncated and is not
				// the same with the hashtag in the original tweet
				if (!senders[u].isDiffused(x))
					continue;
				items[x].updateDiffusion(u, v, weight, currentScoreUpdateTimeStep);
				// update at item-receiver side
				// System.out.println("----updating at the item-receiver side");
				updateItems(v, x, false, weight, currentScoreUpdateTimeStep);
				// update at item-author side
				// System.out.println("----updating at the item-author side");
				updateItems(u, x, true, weight, currentScoreUpdateTimeStep);
				if (!affectedItems.contains(x)) {
					items[x].lastUpdateTime = currentScoreUpdateTimeStep;
					items[x].isRemoved = false;
					affectedItems.add(x);
				}
				items[x].nNewDiffusions++;
				// diffLogs += "\t" + items[x].name;
			}
			/*
			 * if (!diffLogs.equals("")) { try { dlBW.write(diffLogs + "\n"); } catch
			 * (Exception e) { e.printStackTrace(); System.exit(-1); } }
			 */
			// keep track of retweets
			senders[u].addRetweet(currentTweet.originalTweetId, weight, currentScoreUpdateTimeStep);
			receivers[v].addRetweet(currentTweet.originalTweetId, weight, currentScoreUpdateTimeStep);
			iter = mentionedItems.iterator();
			while (iter.hasNext()) {
				x = iter.next();
				// the case when the hashtag in retweet is truncated and is not
				// the same with the hashtag in the original tweet
				if (!senders[u].isDiffused(x))
					continue;
				items[x].addRetweet(currentTweet.originalTweetId, weight, currentScoreUpdateTimeStep);
			}

		} else {
			int nu = userId2Index.get(currentTweet.authorId).index;
			String uScreenName = userId2Index.get(currentTweet.authorId).screenName;
			// update id-2-index map if needed
			if (!userIndex2SenderIndex.containsKey(nu)) {
				userIndex2SenderIndex.put(nu, senderCount);
				senders[senderCount] = new Sender(currentTweet.authorId, uScreenName);
				senderCount++;
			}
			// get sender index
			int u = userIndex2SenderIndex.get(nu);
			// update at sender side
			updateSenderAdoption(u, mentionedItems, weight);
			Iterator<Integer> iter = mentionedItems.iterator();
			int x = -1;
			while (iter.hasNext()) {
				x = iter.next();
				items[x].updateSenders(u, weight, currentScoreUpdateTimeStep);
				items[x].lastUpdateTime = currentScoreUpdateTimeStep;
				items[x].isRemoved = false;
			}
		}
		updateWalks(currentTweet);
		updateScore(currentTweet);
	}

	/***
	 * process the tweet stream
	 */
	public void processTweetStream() {
		logger.info("Starting process tweet stream");
		getNewTweet();
		while (currentTweet != null) {
			// System.out.print("processing the tweet: ");
			// currentTweet.printToScreen();
			// System.out.printf("\t%d\t%d", referenceTime,
			// currentRewalkTimeStep);
			update();
			// System.out.println("");
			getNewTweet();
		}
		System.out.println("NO MORE TWEET");
	}

	/***
	 * compute and the scores
	 */
	public void computeScores(double entropyWeight) {
		int u = 0;
		int v = 0;
		int x = 0;
		int j = 0;
		double d = 0;
		double s = 0;
		double norm = Math.pow(exponentialRate, currentScoreUpdateTimeStep - 1);
		// compute diffusion rank for users
		for (u = 0; u < senderCount; u++) {
			if (!senders[u].diffusedFlag)
				continue;
			senders[u].diffusionRank = (double) senders[u].nVisits / totalNVisits;
		}
		for (v = 0; v < receiverCount; v++) {
			if (!receivers[v].infectedFlag)
				continue;
			receivers[v].diffusionRank = (double) receivers[v].nVisits / totalNVisits;
		}
		// compute virality for items
		Iterator<Map.Entry<Integer, TemporalWeight>> inIter = null;
		Map.Entry<Integer, TemporalWeight> inPair = null;
		Iterator<Map.Entry<Integer, HashMap<Integer, TemporalWeight>>> outIter = null;
		Map.Entry<Integer, HashMap<Integer, TemporalWeight>> outPair = null;
		// item virality
		logger.info("----------- computing virality scores for items");
		for (x = 0; x < itemCount; x++) {
			items[x].virality = 0;
			if (!items[x].isDiffusedFlag)
				continue;
			outIter = items[x].diffusions.entrySet().iterator();
			while (outIter.hasNext()) {
				outPair = outIter.next();
				u = outPair.getKey();
				inIter = outPair.getValue().entrySet().iterator();
				s = 0;
				while (inIter.hasNext()) {
					inPair = inIter.next();
					v = inPair.getKey();
					d = inPair.getValue().weight / norm;
					s += d * (1 - receivers[v].diffusionRank - senders[u].diffusionRank);
				}
				items[x].virality += s;
			}
			items[x].virality *= Math.pow(items[x].diffuserExponentialEntropy * items[x].infectorExponentialEntropy,
					entropyWeight);
		}

		// user virality
		logger.info("----------- computing virality scores for senders");

		for (u = 0; u < senderCount; u++) {
			senders[u].virality = 0;
			if (!senders[u].diffusedFlag)
				continue;
			for (j = 0; j < senders[u].nItems; j++) {
				if (senders[u].nDiffusions[j] == 0)
					continue;
				x = senders[u].itemIndexes[j];
				try {
					inIter = items[x].diffusions.get(u).entrySet().iterator();
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("--------------------------------------------");
					System.out.println("j = " + j);
					System.out.println("x = " + x);
					System.out.println("u = " + u);
					System.out.println("nDiffusions = " + senders[u].nDiffusions[j]);
					System.out.println("--------------------------------------------");
					senders[u].printProfile();
					System.out.println("--------------------------------------------");
					items[x].printProfile();
					System.exit(-1);
				}
				s = 0;
				while (inIter.hasNext()) {
					inPair = inIter.next();
					v = inPair.getKey();
					d = inPair.getValue().weight / norm;
					s += d * (1 - receivers[v].diffusionRank
							- 0.5 * items[x].virality / (items[x].totalNExponentialDiffusions / norm));
				}
				senders[u].virality += s;
			}
			senders[u].virality *= Math.pow(
					senders[u].itemExponentialDiffusingEntropy * senders[u].infectorExponentialEntropy, entropyWeight);
		}

		// user susceptibility
		logger.info("----------- computing susceptibility scores for receivers");
		for (v = 0; v < receiverCount; v++) {
			receivers[v].susceptibility = 0;
			if (!receivers[v].infectedFlag)
				continue;
			for (j = 0; j < receivers[v].nItems; j++) {
				if (receivers[v].nAdoptions[j] == 0)
					continue;
				x = receivers[v].itemIndexes[j];
				inIter = items[x].infections.get(v).entrySet().iterator();
				s = 0;
				while (inIter.hasNext()) {
					inPair = inIter.next();
					u = inPair.getKey();
					d = inPair.getValue().weight / norm;
					s += d * (1 - 0.5 * senders[u].virality / (senders[u].totalNExponentialDiffusions / norm)
							- 0.5 * items[x].virality / (items[x].totalNExponentialDiffusions / norm));

				}
				receivers[v].susceptibility += s;
			}
			receivers[v].susceptibility *= Math
					.pow(receivers[v].itemExponentialEntropy * receivers[v].diffuserExponentialEntropy, entropyWeight);
		}
	}

	private void outputScoresToFiles(String directory, boolean restartDiffusionCount) {
		try {

			TemporalWeight[] topTweets;
			TemporalWeight[] topItems;
			TemporalWeight[] topRetweetedUsers;
			TemporalWeight[] topRetweetingUsers;
			BufferedWriter bw = new BufferedWriter(
					new FileWriter(outputPath + "/senders_" + currentScoreUpdateTimeStep + ".csv"));
			int uItems = 0;
			for (int u = 0; u < senderCount; u++) {
				uItems = 0;
				if (senders[u].itemIndexMap != null) {
					uItems = senders[u].itemIndexMap.size();
				}
				bw.write(senders[u].userId + "," + senders[u].screenName + "," + senders[u].virality + ","
						+ senders[u].diffusionRank + "," + senders[u].itemDiffusingEntropy + ","
						+ senders[u].itemExponentialDiffusingEntropy + "," + senders[u].infectorEntropy + ","
						+ senders[u].infectorExponentialEntropy + "," + senders[u].totalNAdoptions + ","
						+ senders[u].totalNExponentialAdoptions + "," + senders[u].totalNDiffusions + ","
						+ senders[u].totalNExponentialDiffusions + "," + uItems + "," + senders[u].nNewDiffusions);
				topTweets = senders[u].getAndFilterRetweets(currentScoreUpdateTimeStep - Parameters.MAX_ITEM_AGE);
				topItems = senders[u].getTopItems();
				if (topTweets != null) {
					bw.write("," + topTweets.length);
				} else {
					bw.write(",0");
				}
				if (topItems != null) {
					bw.write("," + topItems.length);
				} else {
					bw.write(",0");
				}
				if (topTweets != null) {
					for (int j = 0; j < topTweets.length; j++) {
						bw.write("," + topTweets[j].id + "," + topTweets[j].count + "," + topTweets[j].weight);
					}
				}
				if (topItems != null) {
					for (int j = 0; j < topItems.length; j++) {
						bw.write("," + items[topItems[j].index].name + "," + topItems[j].count + ","
								+ topItems[j].weight);
					}
				}
				bw.write("\n");
				if (restartDiffusionCount)
					senders[u].nNewDiffusions = 0;
			}
			bw.close();

			bw = new BufferedWriter(new FileWriter(outputPath + "/receivers_" + currentScoreUpdateTimeStep + ".csv"));
			int vItems = 0;
			for (int v = 0; v < receiverCount; v++) {
				vItems = 0;
				if (receivers[v].itemIndexMap != null)
					vItems = receivers[v].itemIndexMap.size();
				bw.write(receivers[v].userId + "," + receivers[v].screenName + "," + receivers[v].susceptibility + ","
						+ receivers[v].diffusionRank + "," + receivers[v].itemEntropy + ","
						+ receivers[v].itemExponentialEntropy + "," + receivers[v].diffuserEntropy + ","
						+ receivers[v].diffuserExponentialEntropy + "," + receivers[v].totalNAdoptions + ","
						+ receivers[v].totalNExponentialAdoptions + "," + vItems + "," + receivers[v].nNewDiffusions);
				topTweets = receivers[v].getAndFilterRetweets(currentScoreUpdateTimeStep - Parameters.MAX_ITEM_AGE);
				topItems = receivers[v].getTopItems();
				if (topTweets != null) {
					bw.write("," + topTweets.length);
				} else {
					bw.write(",0");
				}
				if (topItems != null) {
					bw.write("," + topItems.length);
				} else {
					bw.write(",0");
				}
				if (topTweets != null) {
					for (int j = 0; j < topTweets.length; j++) {
						bw.write("," + topTweets[j].id + "," + topTweets[j].count + "," + topTweets[j].weight);
					}
				}
				if (topItems != null) {
					for (int j = 0; j < topItems.length; j++) {
						bw.write("," + items[topItems[j].index].name + "," + topItems[j].count + ","
								+ topItems[j].weight);
					}
				}
				bw.write("\n");
				if (restartDiffusionCount)
					receivers[v].nNewDiffusions = 0;
			}
			bw.close();

			int nSenders = 0;
			int nDiffusers = 0;
			int nInfectors = 0;
			bw = new BufferedWriter(new FileWriter(outputPath + "/items_" + currentScoreUpdateTimeStep + ".csv"));
			for (int x = 0; x < itemCount; x++) {
				nSenders = 0;
				if (items[x].diffusers != null)
					nSenders = items[x].senders.size();
				nDiffusers = 0;
				if (items[x].diffusions != null)
					nDiffusers = items[x].diffusions.size();
				nInfectors = 0;
				if (items[x].infections != null)
					nInfectors = items[x].infections.size();

				bw.write(items[x].name + "," + items[x].virality + "," + items[x].diffuserEntropy + ","
						+ items[x].diffuserExponentialEntropy + "," + items[x].infectorEntropy + ","
						+ items[x].infectorExponentialEntropy + "," + items[x].totalNAdoptions + ","
						+ items[x].totalNExponentialAdoptions + "," + items[x].totalNDiffusions + ","
						+ items[x].totalNExponentialDiffusions + "," + nSenders + "," + nDiffusers + "," + nInfectors
						+ "," + items[x].isRemoved + "," + items[x].nNewDiffusions);
				topTweets = items[x].getAndFilterRetweets(currentScoreUpdateTimeStep - Parameters.MAX_ITEM_AGE);
				topRetweetedUsers = items[x].getTopDiffuser();
				topRetweetingUsers = items[x].getTopInfectors();
				if (topTweets != null) {
					bw.write("," + topTweets.length);
				} else {
					bw.write(",0");
				}
				if (topRetweetedUsers != null) {
					bw.write("," + topRetweetedUsers.length);
				} else {
					bw.write(",0");
				}
				if (topRetweetingUsers != null) {
					bw.write("," + topRetweetingUsers.length);
				} else {
					bw.write(",0");
				}
				if (topTweets != null) {
					for (int j = 0; j < topTweets.length; j++) {
						bw.write("," + topTweets[j].id + "," + topTweets[j].count + "," + topTweets[j].weight);
					}
				}
				if (topTweets != null) {
					for (int j = 0; j < topRetweetedUsers.length; j++) {
						bw.write("," + senders[topRetweetedUsers[j].index].screenName + "," + topRetweetedUsers[j].count
								+ "," + topRetweetedUsers[j].weight);
					}
				}
				if (topTweets != null) {
					for (int j = 0; j < topRetweetingUsers.length; j++) {
						bw.write("," + receivers[topRetweetingUsers[j].index].screenName + ","
								+ topRetweetingUsers[j].count + "," + topRetweetingUsers[j].weight);
					}
				}
				bw.write("\n");
				if (restartDiffusionCount)
					items[x].nNewDiffusions = 0;
			}
			bw.close();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public IncV2S() {

		exponentialRate = Parameters.TEMPORAL_WEIGHT_APMPLIFY_RATE;

		logger.info("initializing containers ...");
		userId2Index = new HashMap<String, InMemoryUser>();
		userIndex2SenderIndex = new HashMap<Integer, Integer>();
		userIndex2ReceiverIndex = new HashMap<Integer, Integer>();
		itemId2Index = new HashMap<String, Integer>(Parameters.ITEM_ARRAY_INITIAL_SIZE, (float) 0.75);
		affectedSenders = new HashSet<Integer>();
		affectedReceivers = new HashSet<Integer>();
		affectedItems = new HashSet<Integer>();
		randomWalks = new HashMap<Integer, RandomWalk>();
		totalNVisits = 0;

		senders = new Sender[Parameters.MAX_NUMBER_OF_USERS];
		senderCount = 0;
		receivers = new Receiver[Parameters.MAX_NUMBER_OF_USERS];
		receiverCount = 0;
		items = new InformationItem[Parameters.MAX_NUMBER_OF_ITEMS];
		itemCount = 0;
		logger.info("getting blacklisted users");
		blackListedUsers = (new HardcodedBlacklistedUsers()).getBlacklistedUsers();
		logger.info("done!");

	}

	public void initialize(String inputConfig, String outputConfig) {
		logger.info("initializing tweet stream ... ");

		initStreams(inputConfig, outputConfig);
		jsonParser = new JsonParser();
		tweetTimeFormat = new SimpleDateFormat(Parameters.TIME_FORMAT);
		logger.info("done!");

		senderUpdateCount = 0;
		receiverUpdateCount = 0;

		itemUpdateCount = 0;
		itemQue = new LinkedList<Integer>();

		nToUpdateWalkCall = 0;
		nToUpdateScoreCall = 0;
		nGetTweet = 0;

	}
}
