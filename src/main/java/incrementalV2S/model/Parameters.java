package incrementalV2S.model;

import java.time.ZonedDateTime;
import java.util.Random;

public class Parameters {
	// startTime

	public static ZonedDateTime ZERO_TIME;

	// elastic search parameters
	// public static String ETS_CONFIG_FILE =
	// "src/main/resources/test-es-tweet-stream.properties";
	public static final long FIRST_CHUNK_OF_TWEETS = 1;// 6hours
	public static final long QUERY_PERIOD = 1;// every 1 min
	public static final String TIME_FORMAT = "\"MMM dd, yyyy hh:mm:ss aaa\"";
	// model parameters

	public static final double TEMPORAL_WEIGHT_APMPLIFY_RATE = Math.pow(2, 1.0 / (24 * 60));

	public static final long UPDATE_SCORE_TIME_INTERVAL = 5 * 60 * 1000;// 5mins
	public static final long REWALK_TIME_INTERVAL = 60 * 1000;// 1min

	public static final Random rand = new Random(0);
	public static final double WALK_JUMPING_PROB = 0.15;
	public static final int MAX_WALK_LENGTH = 20;

	public static final double UNBIASNESS_EXPONENTIAL_WEIGHT = 2.0;

	// data limitation parameters
	public static final int MAX_HASHTAG_LENGTH = 30;
	public static final int MIN_HASHTAG_LENGTH = 2;
	public static final int MAX_ITEM_AGE = 24 * 12;

	// in-memory limitation parameters
	public static final int MAX_NUMBER_OF_USERS = 500000;
	public static final int MAX_NUMBER_OF_ITEMS = 2000000;

	public static final int SENDER_DIFFUSED_ITEM_ARRAY_INITIAL_SIZE = 20;
	public static final double SENDER_DIFFUSED_ITEM_ARRAY_GROWING_RATE = 1.5;

	public static final int RECEIVER_INFECTED_ITEM_ARRAY_INITIAL_SIZE = 50;
	public static final double RECEIVER_INFECTED_ITEM_ARRAY_GROWING_RATE = 1.5;

	public static final int ITEM_ARRAY_INITIAL_SIZE = 10000;
	public static final double ITEM_ARRAY_GROWING_RATE = 1.5;

	// output parameters
	public static final double MIN_USER_VIRALITY = 5d;
	public static final double MIN_USER_SUSCEPTIBILITY = 2d;
	public static final double MIN_ITEM_VIRALITY = 5d;
	public static final int NUMBER_TOP_TWEETS = 10;
	public static final int NUMBER_TOP_HASHTAGS = 10;
	public static final int NUMBER_TOP_USERS = 10;

}
