package incrementalV2S.data.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import incrementalV2S.data.TweetStream;

public class FileTweetStream implements TweetStream{
	public String tweetPath;
	public String dataset;
	private static String[] tweetFiles;
	private static int currentFileIndex;
	private BufferedReader br;

	public FileTweetStream(String path, String _dataset) {
		try {
			this.tweetPath = path;
			this.dataset = _dataset;
			tweetFiles = new String[new File(tweetPath).listFiles().length];
			for (int i = 1; i <= tweetFiles.length; i++) {
				if (dataset.equals("us"))
					if (i < 10)
						tweetFiles[i - 1] = tweetPath + "/tweet_2012-10-0" + i
								+ ".txt";
					else
						tweetFiles[i - 1] = tweetPath + "/tweet_2012-10-" + i
								+ ".txt";
				else
					tweetFiles[i - 1] = tweetPath + "/tweets_10_" + i;

			}
			br = new BufferedReader(new FileReader(tweetFiles[0]));
			currentFileIndex = 0;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/***
	 * get a tweet from tweet stream
	 * 
	 * @return a tweet
	 */
	@Override
	public String getTweet() {
		try {
			String line = br.readLine();
			if (line != null) {
				return line;
			}
			br.close();
			if (currentFileIndex < tweetFiles.length - 1) {
				currentFileIndex++;
				br = new BufferedReader(new FileReader(
						tweetFiles[currentFileIndex]));
				line = br.readLine();
			}
			/*
			 * System.out.printf("returning a tweet from %s",
			 * tweetFiles[currentFileIndex]);
			 */
			return line;
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

}
