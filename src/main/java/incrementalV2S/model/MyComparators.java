package incrementalV2S.model;

import java.util.Comparator;

public class MyComparators {
	static public Comparator<TemporalWeight> temporalWeightDescCount;
	static public Comparator<TemporalWeight> temporalWeightDescWeight;

	static {
		temporalWeightDescCount = new Comparator<TemporalWeight>() {
			@Override
			public int compare(TemporalWeight e1, TemporalWeight e2) {
				if (e2.count - e1.count > 0)
					return 1;
				else if (e2.count - e1.count < 0)
					return -1;
				else
					return 0;
			}
		};

		temporalWeightDescWeight = new Comparator<TemporalWeight>() {
			@Override
			public int compare(TemporalWeight e1, TemporalWeight e2) {
				if (e2.weight - e1.weight > 0)
					return 1;
				else if (e2.weight - e1.weight < 0)
					return -1;
				else
					return 0;
			}
		};
	}
}
