package incrementalV2S.model;

import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;

public class ModelRunner {
	static void setZEROTIME() {
		//Calendar cal = Calendar.getInstance();
		//cal.setTime(new Date(System.currentTimeMillis()));
		//cal.add(Calendar.DAY_OF_MONTH, -5);
		Parameters.ZERO_TIME = ZonedDateTime.now().minusDays(5).withHour(0).withMinute(0).withSecond(0).withNano(0);
	}

	public static void main(String[] args) {

		IncV2S incV2S = new IncV2S();
		incV2S.outputPath = "E:/tempOutput";
		incV2S.tweetTimeFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss aaa");
		incV2S.entropyWeight = Parameters.UNBIASNESS_EXPONENTIAL_WEIGHT;

		setZEROTIME();
		incV2S.zdt = Parameters.ZERO_TIME;
		incV2S.referenceTime = incV2S.zdt.toEpochSecond() * 1000;
		incV2S.initialize("test-es-tweet-stream.properties", "test-es-tweet-stream.properties");

		incV2S.outChannel = false;
		incV2S.processTweetStream();

	}
}
