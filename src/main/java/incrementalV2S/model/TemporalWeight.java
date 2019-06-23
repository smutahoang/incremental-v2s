package incrementalV2S.model;

public class TemporalWeight {
	public int lastUpdateTime;
	public int count;
	public double weight;

	// To identify, not always used
	public int index;
	public String id;

	public TemporalWeight(int c, double w, int t) {
		lastUpdateTime = t;
		count = c;
		weight = w;
	}

	public TemporalWeight(int c, double w, int i, boolean flag) {
		if (flag)
			index = i;
		else
			lastUpdateTime = i;
		count = c;
		weight = w;
	}

	public TemporalWeight(int c, double w, String ojectId) {
		id = ojectId;
		count = c;
		weight = w;
	}

	public void update(int c, double w, int t) {
		lastUpdateTime = t;
		count += c;
		weight += w;
	}
	
}
