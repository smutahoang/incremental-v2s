package larc.incrementalV2S.output.model;

import com.google.gson.annotations.SerializedName;

public class RetweetedHashtag extends Hashtag {

	@SerializedName("numberOfRecentRetweet")
	private final double recentTweet;

	@SerializedName("numberOfCumulativeRetweet")
	private final int cumulativeTweet;

	/***
	 * 
	 * @param inId
	 *            Twitter ID
	 * @param inScreenName
	 *            Twitter screen name
	 * @param inViralityScore
	 *            virality score
	 * @param recentRetweet
	 *            weighted number of recent retweet
	 * @param cumulativeRetweet
	 *            number of cumulative retweet
	 */
	private RetweetedHashtag(String inHashtag, double inViralityScore, double inRecentRetweet,
			int inCumulativeRetweet) {
		
		super(inHashtag, inViralityScore);
		
		this.recentTweet = inRecentRetweet;
		this.cumulativeTweet = inCumulativeRetweet;
	}

	public double getViralityScore() {
		return super.getViralityScore();
	}

	public static class Builder {

		private final String hashtag;
		private double viralityScore = 0.0d;
		private double recentRetweet = 0.0d;
		private int cumulativeRetweet = 0;

		public Builder(String inHashtag) {
			this.hashtag = inHashtag;
		}

		public Builder viralityScore(double input) {
			this.viralityScore = input;
			return this;
		}

		public Builder recentRetweet(double input) {
			this.recentRetweet = input;
			return this;
		}

		public Builder cumulativeRetweet(int input) {
			this.cumulativeRetweet = input;
			return this;
		}

		public RetweetedHashtag build() {
			return new RetweetedHashtag(this.hashtag, this.viralityScore, this.recentRetweet, this.cumulativeRetweet);
		}
	}
}
