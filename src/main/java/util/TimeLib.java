package util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import incrementalV2S.model.Parameters;

public class TimeLib {

	/***
	 * to work with time labeled items, e.g. tweets
	 * 
	 * @author Tuan-Anh Hoang
	 *
	 */
	public static class TimeLabeledItem {
		String name;
		Date label;

		public TimeLabeledItem(String n, Date l) {
			this.name = n;
			this.label = l;
		}

		public String getName() {
			return name;
		}

		public Date getTimeLabel() {
			return label;
		}
	}

	static private Comparator<TimeLabeledItem> ascName;
	static private Comparator<TimeLabeledItem> descTimeLabel;

	static {
		ascName = new Comparator<TimeLabeledItem>() {
			@Override
			public int compare(TimeLabeledItem e1, TimeLabeledItem e2) {
				return e1.getName().compareTo(e2.getName());
			}
		};

		descTimeLabel = new Comparator<TimeLabeledItem>() {
			@Override
			public int compare(TimeLabeledItem e1, TimeLabeledItem e2) {
				int c = e2.getTimeLabel().compareTo(e1.getTimeLabel());
				if (c > 0)
					return 1;
				else if (c < 0)
					return -1;
				else
					return 0;
			}
		};
	}

	/***
	 * write the items out to file outputPath in time order
	 * 
	 * @param items
	 * @param labels
	 *            : Time labels of the items
	 * @param outputPath
	 */
	public static void outputInTimeOrder(String[] items, Date[] labels, String outputPath) {
		try {
			TimeLabeledItem[] t_items = new TimeLabeledItem[items.length];
			for (int i = 0; i < t_items.length; i++)
				t_items[i] = new TimeLabeledItem(items[i], labels[i]);
			Arrays.sort(t_items, descTimeLabel);
			BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath));
			for (int i = t_items.length - 1; i >= 0; i--)
				bw.write(t_items[i].getTimeLabel().toString() + "\t" + t_items[i].getName() + "\n");
			bw.close();

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/***
	 * get time difference between two time point
	 * 
	 * @param timeOne
	 * @param timeTwo
	 * @return
	 */
	public static long getTimeDifference(long timeOne, long timeTwo) {
		return Math.abs(timeTwo - timeOne);
	}

	/***
	 * get elapsed time step since the reference time point
	 * 
	 * @param time
	 * @return
	 */
	public static int getElapsedTimeSteps(long time, long refTime, long stepLength) {
		return (int) ((time - refTime) / stepLength);
	}

	/***
	 * get current time of the program
	 * 
	 * @return
	 */
	public static long getCurrentTimeStep(long refTime, long stepLength) {
		return getElapsedTimeSteps(System.currentTimeMillis(), refTime, Parameters.UPDATE_SCORE_TIME_INTERVAL);
	}

	/***
	 * convert time string to long
	 * 
	 * @param dateString
	 * @param format
	 * @return
	 */
	public static long dateStringToLong(String dateString, SimpleDateFormat format) {
		try {
			Date d = format.parse(dateString);
			return d.getTime();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return -1;
	}

}
