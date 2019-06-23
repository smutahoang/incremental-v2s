package incrementalV2S.model;

public class Tweet {
	public String tweetId;
	public String authorId;
	public String authorScreenName;
	public String originalTweetId;
	public String originalAuthorId;
	public String originalAuthorScreenName;
	public String content;
	public long publishedTime;

	public void printToScreen() {
		System.out.printf("%s\t%s\t%s\t%d\t%s\t%s\t%s\n", tweetId, authorId, originalAuthorId, publishedTime,
				authorScreenName, originalAuthorScreenName, originalTweetId);
	}
}
