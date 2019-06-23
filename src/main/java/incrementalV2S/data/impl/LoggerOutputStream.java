package incrementalV2S.data.impl;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import incrementalV2S.data.OutputStream;
import incrementalV2S.output.model.Hashtag;
import incrementalV2S.output.model.RetweetedUser;
import incrementalV2S.output.model.RetweetingUser;
import incrementalV2S.output.model.SavedTweet;
import incrementalV2S.output.model.SavedUser;

public class LoggerOutputStream implements OutputStream {

	private static final Logger logger = LoggerFactory.getLogger(LoggerOutputStream.class);

	private final Gson gson = new GsonBuilder().create();

	@Override
	public void init() {
		logger.info("init stream");
	}

	@Override
	public void saveUser(SavedUser anUser, Date date) {
		logger.info("anUser: {}", gson.toJson(anUser));
	}

	@Override
	public void saveHashtag(Hashtag aHashtag, Date date) {
		logger.info("aHashtag: {}", gson.toJson(aHashtag));
	}

	@Override
	public void close() {
		logger.info("close stream");
	}

	public static void main(String[] args) {
		OutputStream os = new LoggerOutputStream();

		ZonedDateTime zdt = ZonedDateTime.now();
		String dateStr = zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

		SavedTweet t1 = new SavedTweet("123", 0.1d, 2);
		SavedTweet t2 = new SavedTweet("123456", 0.2d, 4);

		/*SavedUser u = new SavedUser.Builder("123", "arinto").scoreAt(dateStr).viralityScore(1.1)
				.susceptibilityScore(2.3).topRetweetedHashtags(ImmutableList.of("a", "b", "c"))
				.topRetweetingHashtags(ImmutableList.of("d", "e", "f")).topRetweetedTweets(ImmutableList.of(t1, t2))
				.build();*/

		RetweetedUser a = new RetweetedUser.Builder("a", "userA").viralityScore(1.0d).recentRetweet(2.0d)
				.cumulativeRetweet(3).build();
		RetweetedUser b = new RetweetedUser.Builder("b", "userB").viralityScore(4.0d).recentRetweet(5.0d)
				.cumulativeRetweet(6).build();
		RetweetingUser c = new RetweetingUser.Builder("c", "userC").susceptibilityScore(7.0d).recentRetweet(8.0d)
				.cumulativeRetweet(9).build();
		RetweetingUser d = new RetweetingUser.Builder("d", "userD").susceptibilityScore(10.0d).recentRetweet(11.0d)
				.cumulativeRetweet(12).build();
		Hashtag h = new Hashtag("hashtag", dateStr, 1.5, ImmutableList.of(a, b), ImmutableList.of(c, d),
				ImmutableList.of(t1, t2));

		//os.saveUser(u);
		os.saveHashtag(h, Date.from(zdt.toInstant()));
	}

}
