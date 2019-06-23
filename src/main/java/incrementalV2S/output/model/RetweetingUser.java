package larc.incrementalV2S.output.model;

import com.google.gson.annotations.SerializedName;

/***
 * RetweetingUser class, part of Hashtag class' field
 * @author arinto
 *
 */
public final class RetweetingUser extends User {
    
    @SerializedName("susceptibilityScore")
    private final double susceptibilityScore;
    
    /***
     * 
     * @param inId Twitter ID
     * @param inScreenName Twitter screen name
     * @param inSusceptibilityScore susceptibility score
     * @param recentRetweet weighted number of recent retweet
     * @param cumulativeRetweet number of cumulative retweet
     */
    private RetweetingUser(String inId, String inScreenName, double inSusceptibilityScore, double recentRetweet, int cumulativeRetweet){
        super(inId, inScreenName, recentRetweet, cumulativeRetweet);
        this.susceptibilityScore = inSusceptibilityScore;
    }

    public double getSusceptibilityScore() {
        return susceptibilityScore;
    }
    
    public static class Builder{
        
        private final String id;
        private final String screenName;
        private double susceptibilityScore = 0.0d;
        private double recentRetweet = 0.0d;
        private int cumulativeRetweet = 0;
        
        public Builder(String inId, String screenName){
            this.id = inId;
            this.screenName = screenName;
        }
        
        public Builder susceptibilityScore(double input){
            this.susceptibilityScore = input;
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
        
        public RetweetingUser build(){
            return new RetweetingUser(this.id, this.screenName, this.susceptibilityScore, this.recentRetweet, this.cumulativeRetweet);
        }
    }

}
