/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import javax.inject.Inject;

import org.junit.Test;
import org.springframework.mock.web.MockHttpSession;

import kommet.auth.AuthData;
import kommet.auth.AuthException;
import kommet.auth.AuthUtil;
import kommet.basic.User;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.tests.BaseUnitTest;
import kommet.utils.AppConfig;

public class AuthUtilTest extends BaseUnitTest
{	
	@Inject
	AppConfig config;
	
	@Test
	public void testLoginAsFeature() throws KommetException
	{
		MockHttpSession session = new MockHttpSession();
		
		User primaryUser = new User();
		primaryUser.setUserName("PrimaryUser");
		primaryUser.setId(KID.get("0040000000001"));
		
		AuthData primaryAuthData = new AuthData();
		primaryAuthData.setUser(primaryUser);
		
		AuthUtil.storePrimaryAuthData(primaryAuthData, session);
		
		AuthData retrievedAuthData = AuthUtil.getAuthData(session);
		assertNotNull(retrievedAuthData);
		assertEquals(primaryUser.getUserName(), retrievedAuthData.getUser().getUserName());
		
		// now create some other user with auth data
		User secondaryUser = new User();
		secondaryUser.setUserName("SecondaryUser");
		secondaryUser.setId(KID.get("0040000000002"));
		
		AuthData secondaryAuthData = new AuthData();
		secondaryAuthData.setUser(secondaryUser);
		
		AuthUtil.storeSecondaryAuthData(secondaryAuthData, config, session);
		
		retrievedAuthData = AuthUtil.getAuthData(session);
		assertNotNull(retrievedAuthData);
		assertEquals(secondaryUser.getUserName(), retrievedAuthData.getUser().getUserName());
		
		AuthUtil.clearAuthData(session);
		retrievedAuthData = AuthUtil.getAuthData(session);
		assertNotNull(retrievedAuthData);
		assertEquals(primaryUser.getUserName(), retrievedAuthData.getUser().getUserName());
		
		// call remove auth data once again to log out the primary user
		AuthUtil.clearAuthData(session);
		assertNull(AuthUtil.getAuthData(session));
		
		// now try to log in as secondary user and make sure this is not possible
		// because we are not authenticated as primary user in the first place
		try
		{
			AuthUtil.storeSecondaryAuthData(secondaryAuthData, config, session);
			fail("Logging in as secondary user without being authenticated should fail");
		}
		catch (AuthException e)
		{
			assertEquals("Login as feature cannot be used by unauthenticated users", e.getMessage());
		}
		
		// now log out again and make sure you can log out as many times as you want
		for (int i = 0; i < 10; i++)
		{
			AuthUtil.clearAuthData(session);
		}
		
		AuthUtil.storePrimaryAuthData(primaryAuthData, session);
		try
		{
			AuthUtil.storeSecondaryAuthData(primaryAuthData, config, session);
			fail("Using the same user for primary and secondary logging should fail");
		}
		catch (AuthException e)
		{
			assertEquals("Trying to log in as the same user as the primary authenticated one", e.getMessage());
		}
		
		// now create some other user with auth data
		User tertiaryUser = new User();
		tertiaryUser.setUserName("TertiaryUser");
		tertiaryUser.setId(KID.get("0040000000003"));
		
		AuthData tertiaryAuthData = new AuthData();
		tertiaryAuthData.setUser(tertiaryUser);
		
		AuthUtil.storeSecondaryAuthData(secondaryAuthData, config, session);
		AuthUtil.storeSecondaryAuthData(tertiaryAuthData, config, session);
		AuthUtil.storeSecondaryAuthData(secondaryAuthData, config, session);
		
		AuthUtil.clearAuthData(session);
		assertEquals("TertiaryUser", AuthUtil.getAuthData(session).getUser().getUserName());
		
		AuthUtil.clearAuthData(session);
		assertEquals("PrimaryUser", AuthUtil.getAuthData(session).getUser().getUserName());
	}
}
