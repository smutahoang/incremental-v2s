package incrementalV2S.data.impl;

import com.google.common.collect.ImmutableSet;

import incrementalV2S.data.BlacklistedUsersAccess;

public class HardcodedBlacklistedUsers implements BlacklistedUsersAccess {

	private static final ImmutableSet<Long> blacklistedUsers = 
			ImmutableSet.of(
					Long.valueOf(113614082L) //@EurekaMag
					);
	
	
	@Override
	public ImmutableSet<Long> getBlacklistedUsers() {
		return blacklistedUsers;
	}

}
