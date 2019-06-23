package larc.incrementalV2S.output.model;

import com.google.gson.annotations.SerializedName;

/***
 * SavedTweet class to represent a tweet in Hashtag and SavedUser class
 * @author arinto
 *
 */
public class SavedTweet {
    
    @SerializedName("id")
    private final String id;
    
    @SerializedName("numberOfRecentRetweet")
    private final double recentTweet;
    
    @SerializedName("numberOfCumulativeRetweet")
    private final int cumulativeTweet;
    
    public SavedTweet(String inId, double inRecentTweet, int inCumulativeTweet){
        this.id = inId;
        this.recentTweet = inRecentTweet;
        this.cumulativeTweet = inCumulativeTweet;
    }

    public String getId() {
        return id;
    }

    public double getRecentTweet() {
        return recentTweet;
    }

    public int getCumulativeTweet() {
        return cumulativeTweet;
    }

}
