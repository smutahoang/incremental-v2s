package larc.incrementalV2S.output.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

public class Hashtag {

	@SerializedName("hashtag")
	private final String hashtag;

	@SerializedName("scoreAt")
	private final String scoreAt;

	@SerializedName("viralityScore")
	private final double viralityScore;

	@SerializedName("topRetweetedUsers")
	private final ImmutableList<RetweetedUser> topRetweetedUsers;

	@SerializedName("topRetweetingUsers")
	private final ImmutableList<RetweetingUser> topRetweetingUsers;

	@SerializedName("topTweets")
	private final ImmutableList<SavedTweet> topTweets;

	public Hashtag(String inHashtag, String inScoreAt, double inViralityScore,
			ImmutableList<RetweetedUser> inTopRetweetedUsers, ImmutableList<RetweetingUser> inTopRetweetingUsers,
			ImmutableList<SavedTweet> inTopTweets) {
		this.hashtag = inHashtag;
		this.scoreAt = inScoreAt;
		this.viralityScore = inViralityScore;
		this.topRetweetedUsers = inTopRetweetedUsers;
		this.topRetweetingUsers = inTopRetweetingUsers;
		this.topTweets = inTopTweets;
	}

	protected Hashtag(String inHashtag, double inViralityScore) {
		this.hashtag = inHashtag;
		this.scoreAt = null;
		this.viralityScore = inViralityScore;
		this.topRetweetedUsers = null;
		this.topRetweetingUsers = null;
		this.topTweets = null;
	}

	public String getHashtag() {
		return hashtag;
	}

	public String getScoreAt() {
		return scoreAt;
	}

	public double getViralityScore() {
		return viralityScore;
	}

	public ImmutableList<RetweetedUser> getTopRetweetedUsers() {
		return topRetweetedUsers;
	}

	public ImmutableList<RetweetingUser> getTopRetweetingUsers() {
		return topRetweetingUsers;
	}

	public ImmutableList<SavedTweet> getTopTweets() {
		return topTweets;
	}

	public static class Builder {

		private final String hashtag;
		private String scoreAt;
		private double viralityScore;
		private ImmutableList<RetweetedUser> topRetweetedUsers;
		private ImmutableList<RetweetingUser> topRetweetingUsers;
		private ImmutableList<SavedTweet> topTweets;

		public Builder(String inHashtag) {
			this.hashtag = inHashtag;
		}

		public Builder scoreAt(String input) {
			this.scoreAt = input;
			return this;
		}

		public Builder viralityScore(double score) {
			this.viralityScore = score;
			return this;
		}

		public Builder topRetweetedUsers(ImmutableList<RetweetedUser> input) {
			this.topRetweetedUsers = input;
			return this;
		}

		public Builder topRetweetingUsers(ImmutableList<RetweetingUser> input) {
			this.topRetweetingUsers = input;
			return this;
		}

		public Builder topTweets(ImmutableList<SavedTweet> input) {
			this.topTweets = input;
			return this;
		}

		public Hashtag build() {
			return new Hashtag(this.hashtag, this.scoreAt, this.viralityScore, this.topRetweetedUsers,
					this.topRetweetingUsers, this.topTweets);
		}

	}

	public static void main(String[] args) {
		RetweetedUser a = new RetweetedUser.Builder("a", "userA").viralityScore(1.0d).recentRetweet(2.0d)
				.cumulativeRetweet(3).build();
		RetweetedUser b = new RetweetedUser.Builder("b", "userB").viralityScore(4.0d).recentRetweet(5.0d)
				.cumulativeRetweet(6).build();
		RetweetingUser c = new RetweetingUser.Builder("c", "userC").susceptibilityScore(7.0d).recentRetweet(8.0d)
				.cumulativeRetweet(9).build();
		RetweetingUser d = new RetweetingUser.Builder("d", "userD").susceptibilityScore(10.0d).recentRetweet(11.0d)
				.cumulativeRetweet(12).build();

		SavedTweet t1 = new SavedTweet("123", 0.1d, 2);
		SavedTweet t2 = new SavedTweet("123456", 0.2d, 4);

		ZonedDateTime zdt = ZonedDateTime.now();
		String dateStr = zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

		Hashtag h = new Hashtag("hashtag", dateStr, 1.5, ImmutableList.of(a, b), ImmutableList.of(c, d),
				ImmutableList.of(t1, t2));
		Gson g = new GsonBuilder().create();
		System.out.printf("%s%n", g.toJson(h));
	}

}
