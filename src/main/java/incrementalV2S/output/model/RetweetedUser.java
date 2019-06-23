package larc.incrementalV2S.output.model;

import com.google.gson.annotations.SerializedName;

/**
 * RetweetedUser class, part of Hashtag's class
 * @author arinto
 *
 */
public final class RetweetedUser extends User {
    
    @SerializedName("viralityScore")
    private final double viralityScore;
    
    /***
     * 
     * @param inId Twitter ID
     * @param inScreenName Twitter screen name
     * @param inViralityScore virality score
     * @param recentRetweet weighted number of recent retweet
     * @param cumulativeRetweet number of cumulative retweet
     */
    private RetweetedUser(String inId, String inScreenName, double inViralityScore, double recentRetweet, int cumulativeRetweet){
        super(inId, inScreenName, recentRetweet, cumulativeRetweet);
        this.viralityScore = inViralityScore;
    }

    public double getViralityScore() {
        return viralityScore;
    }
    
    public static class Builder{
        
        private final String id;
        private final String screenName;
        private double viralityScore = 0.0d;
        private double recentRetweet = 0.0d;
        private int cumulativeRetweet = 0;
        
        public Builder(String inId, String screenName){
            this.id = inId;
            this.screenName = screenName;
        }
        
        public Builder viralityScore(double input){
            this.viralityScore = input;
            return this;
        }
        
        public Builder recentRetweet(double input){
            this.recentRetweet = input;
            return this;
        }
        
        public Builder cumulativeRetweet(int input){
            this.cumulativeRetweet = input;
            return this;
        }
        
        public RetweetedUser build(){
            return new RetweetedUser(this.id, this.screenName, this.viralityScore, this.recentRetweet, this.cumulativeRetweet);
        }
    }

}
