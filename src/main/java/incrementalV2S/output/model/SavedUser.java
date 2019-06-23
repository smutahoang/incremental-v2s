package larc.incrementalV2S.output.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

/***
 * User data model for persisting into storage.
 * 
 * @author arinto
 *
 */
public class SavedUser {

	@SerializedName("id")
	private final String id;

	@SerializedName("screenName")
	private final String screenName;

	@SerializedName("scoreAt")
	private final String scoreAt;

	@SerializedName("viralityScore")
	private final double viralityScore;

	@SerializedName("susceptibilityScore")
	private final double susceptibilityScore;

	@SerializedName("topRetweetedHashtags")
	private final ImmutableList<RetweetedHashtag> topRetweetedHashtags;

	@SerializedName("topRetweetingHashtags")
	private final ImmutableList<RetweetingHashtag> topRetweetingHashtags;

	@SerializedName("topRetweetedTweets")
	private final ImmutableList<SavedTweet> topRetweetedTweets;

	@SerializedName("topRetweetingTweets")
	private final ImmutableList<SavedTweet> topRetweetingTweets;

	/***
	 * 
	 * @param inId
	 *            Twitter ID
	 * @param inScreenName
	 *            Twitter user name
	 * @param inScoreAt
	 *            When the score is saved
	 * @param inViralityScore
	 *            Virality score
	 * @param inSusceptibilityScore
	 *            Susceptibility score
	 * @param inTopRetweetedHashtags
	 *            List of top retweeted hashtags
	 * @param inTopRetweetingHashtags
	 *            List of top retweeting hashtags
	 * @param inTopRetweetedTweets
	 *            List of top tweets
	 */
	private SavedUser(String inId, String inScreenName, String inScoreAt, double inViralityScore,
			double inSusceptibilityScore, ImmutableList<RetweetedHashtag> inTopRetweetedHashtags,
			ImmutableList<RetweetingHashtag> inTopRetweetingHashtags, ImmutableList<SavedTweet> inTopRetweetedTweets,
			ImmutableList<SavedTweet> inTopRetweetingTweets) {
		this.id = inId;
		this.screenName = inScreenName;
		this.scoreAt = inScoreAt;
		this.viralityScore = inViralityScore;
		this.susceptibilityScore = inSusceptibilityScore;
		this.topRetweetedHashtags = inTopRetweetedHashtags;
		this.topRetweetingHashtags = inTopRetweetingHashtags;
		this.topRetweetedTweets = inTopRetweetedTweets;
		this.topRetweetingTweets = inTopRetweetingTweets;
	}

	public String getId() {
		return id;
	}

	public String getScreenName() {
		return screenName;
	}

	public String getScoreAt() {
		return scoreAt;
	}

	public double getViralityScore() {
		return viralityScore;
	}

	public double getSusceptibilityScore() {
		return susceptibilityScore;
	}

	public ImmutableList<RetweetedHashtag> getTopRetweetedHashtags() {
		return topRetweetedHashtags;
	}

	public ImmutableList<RetweetingHashtag> getTopRetweetingHashtags() {
		return topRetweetingHashtags;
	}

	public ImmutableList<SavedTweet> getTopRetweetedTweets() {
		return topRetweetedTweets;
	}

	public ImmutableList<SavedTweet> getTopRetweetingTweets() {
		return topRetweetingTweets;
	}

	public static class Builder {

		private final String id;
		private final String screenName;
		private String scoreAt;
		private double viralityScore;
		private double susceptibilityScore;
		private ImmutableList<RetweetedHashtag> topRetweetedHashtags;
		private ImmutableList<RetweetingHashtag> topRetweetingHashtags;
		private ImmutableList<SavedTweet> topRetweetedTweets;
		private ImmutableList<SavedTweet> topRetweetingTweets;

		public Builder(String inId, String inScreenName) {
			this.id = inId;
			this.screenName = inScreenName;
			ZonedDateTime zdt = ZonedDateTime.now();
			this.scoreAt = zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
			this.viralityScore = 0.0;
			this.susceptibilityScore = 0.0;
			this.topRetweetedHashtags = null;
			this.topRetweetingHashtags = null;
			this.topRetweetedTweets = null;
			this.topRetweetingTweets = null;
		}

		public Builder scoreAt(String input) {
			this.scoreAt = input;
			return this;
		}

		public Builder viralityScore(double score) {
			this.viralityScore = score;
			return this;
		}

		public Builder susceptibilityScore(double score) {
			this.susceptibilityScore = score;
			return this;
		}

		public Builder topRetweetedHashtags(ImmutableList<RetweetedHashtag> input) {
			this.topRetweetedHashtags = input;
			return this;
		}

		public Builder topRetweetingHashtags(ImmutableList<RetweetingHashtag> input) {
			this.topRetweetingHashtags = input;
			return this;
		}

		public Builder topRetweetedTweets(ImmutableList<SavedTweet> input) {
			this.topRetweetedTweets = input;
			return this;
		}

		public Builder topRetweetingTweets(ImmutableList<SavedTweet> input) {
			this.topRetweetingTweets = input;
			return this;
		}

		public SavedUser build() {
			return new SavedUser(this.id, this.screenName, this.scoreAt, this.viralityScore, this.susceptibilityScore,
					this.topRetweetedHashtags, this.topRetweetingHashtags, this.topRetweetedTweets,
					this.topRetweetingTweets);
		}

	}

	public static void main(String[] args) {
		ZonedDateTime zdt = ZonedDateTime.now();
		String dateStr = zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

		SavedTweet t1 = new SavedTweet("123", 0.1d, 2);
		SavedTweet t2 = new SavedTweet("123456", 0.2d, 4);
		SavedTweet t3 = new SavedTweet("987", 0.1d, 2);
		SavedTweet t4 = new SavedTweet("987654", 0.2d, 4);

		RetweetedHashtag h1 = new RetweetedHashtag.Builder("hashtag_1").viralityScore(1.1d).recentRetweet(1.2d)
				.cumulativeRetweet(1).build();
		RetweetedHashtag h2 = new RetweetedHashtag.Builder("hashtag_2").viralityScore(2.1d).recentRetweet(2.2d)
				.cumulativeRetweet(2).build();
		RetweetingHashtag h3 = new RetweetingHashtag.Builder("hashtag_3").viralityScore(3.1d).recentRetweet(3.2d)
				.cumulativeRetweet(3).build();
		RetweetingHashtag h4 = new RetweetingHashtag.Builder("hashtag_4").viralityScore(4.1d).recentRetweet(4.2d)
				.cumulativeRetweet(4).build();

		SavedUser u = new SavedUser.Builder("123", "arinto").scoreAt(dateStr).viralityScore(1.1)
				.susceptibilityScore(2.3).topRetweetedHashtags(ImmutableList.of(h1, h2))
				.topRetweetingHashtags(ImmutableList.of(h3, h4)).topRetweetedTweets(ImmutableList.of(t1, t2))
				.topRetweetingTweets(ImmutableList.of(t3, t4)).build();

		Gson g = new GsonBuilder().create();
		System.out.printf("%s%n", g.toJson(u));
	}

}
