package incrementalV2S.data;

import com.google.common.collect.ImmutableSet;

public interface BlacklistedUsersAccess {
	
	ImmutableSet<Long> getBlacklistedUsers();

}
