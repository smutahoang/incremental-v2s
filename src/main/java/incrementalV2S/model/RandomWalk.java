package incrementalV2S.model;

import java.util.HashMap;

public class RandomWalk {
	public Walk[] walks;
	public int length;
	public int startUserIndex;
	public boolean flag;// true if start from sender, false otherwise

	/***
	 * 
	 * @return
	 */
	public HashMap<Integer, Integer> getRelatedSenderIndexes() {
		HashMap<Integer, Integer> senders = new HashMap<Integer, Integer>();
		for (int i = 0; i < length; i++) {
			if (!walks[i].direction) {
				if (i < length - 1)
					continue;
			}
			int senderIndex = walks[i].senderIndex;
			if (senders.containsKey(senderIndex)) {
				int n = senders.get(senderIndex) + 1;
				senders.remove(senderIndex);
				senders.put(senderIndex, n);
			} else {
				senders.put(senderIndex, 1);
			}
		}
		return senders;
	}

	/***
	 * 
	 * @return
	 */
	public HashMap<Integer, Integer> getRelatedReceiverIndexes() {
		HashMap<Integer, Integer> receivers = new HashMap<Integer, Integer>();
		for (int i = 0; i < length; i++) {
			if (walks[i].direction) {
				if (i < length - 1)
					continue;
			}
			int receiverIndex = walks[i].receiverIndex;
			if (receivers.containsKey(receiverIndex)) {
				int n = receivers.get(receiverIndex) + 1;
				receivers.remove(receiverIndex);
				receivers.put(receiverIndex, n);
			} else {
				receivers.put(receiverIndex, 1);
			}
		}
		return receivers;
	}
}
