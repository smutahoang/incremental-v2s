package larc.incrementalV2S.output.model;

import com.google.gson.annotations.SerializedName;

/***
 * Abstract user class for Hashtag output data model
 * @author arinto
 *
 */
public abstract class User {
    
    @SerializedName("id")
    protected final String id;
    
    @SerializedName("screenName")
    protected final String screenName;
    
    @SerializedName("numberOfRecentRetweet")
    protected final double numberOfRecentRetweet;
    
    @SerializedName("numberOfCumulativeRetweet")
    protected final int numberOfCumulativeRetweet;
    
    /***
     * 
     * @param inId Twitter ID
     * @param inScreenName Twitter screen name
     * @param recentRetweet weighted number of recent retweet
     * @param cumulativeRetweet number of cumulative retweet
     */
    protected User(String inId, String inScreenName, double recentRetweet, int cumulativeRetweet){
        this.id = inId;
        this.screenName = inScreenName;
        this.numberOfRecentRetweet = recentRetweet;
        this.numberOfCumulativeRetweet = cumulativeRetweet;
    }

    public String getId() {
        return id;
    }

    public String getScreenName() {
        return screenName;
    }

    public double getNumberOfRecentRetweet() {
        return numberOfRecentRetweet;
    }

    public int getNumberOfCumulativeRetweet() {
        return numberOfCumulativeRetweet;
    }  
    
}
