package incrementalV2S.data;

import java.util.Date;

import incrementalV2S.output.model.Hashtag;
import incrementalV2S.output.model.SavedUser;

public interface OutputStream {
    
    void init();
	void saveUser(SavedUser anUser, Date date); 
	void saveHashtag(Hashtag aHashtag, Date date); 
	void close();
}
