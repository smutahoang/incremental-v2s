package incrementalV2S.model;

public class InMemoryUser {
	public String userId;
	public String screenName;
	public int index;

	public InMemoryUser(String _userId, String _screenName, int _index) {
		this.userId = _userId;
		this.screenName = _screenName;
		this.index = _index;
	}

	public InMemoryUser(String _screenName, int _index) {
		this.screenName = _screenName;
		this.index = _index;
	}
}
