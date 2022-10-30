/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.ProfileService;
import kommet.auth.UserService;
import kommet.basic.BasicSetupService;
import kommet.basic.Layout;
import kommet.basic.Profile;
import kommet.basic.UniqueCheckViolationException;
import kommet.basic.User;
import kommet.basic.UserSettings;
import kommet.basic.keetle.LayoutService;
import kommet.data.DataService;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.filters.UserFilter;
import kommet.i18n.Locale;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;

public class UserTest extends BaseUnitTest
{
	@Inject
	DataService typeService;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	UserService userService;
	
	@Inject
	LayoutService layoutService;
	
	@Test
	public void createUserType() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		
		// create layout
		Layout layout = new Layout();
		String code = "<km:layout name=\"Test\"><km:beforeContent></km:beforeContent></km:layout>";
		layout.setCode(code);
		layout.setName("Test");
		layout = layoutService.save(layout, dataHelper.getRootAuthData(env), env);
		
		// create profile
		Profile profile = new Profile();
		profile.setName("TestProfile");
		profile.setLabel("TestProfile");
		profile.setSystemProfile(false);
		profile = profileService.save(profile, dataHelper.getRootAuthData(env), env);
		assertNotNull(profile.getId());
		
		// create user
		User user = new User();
		user.setProfile(profile);
		user.setUserName("test");
		user.setEmail("test@kolmu.com");
		user.setPassword(MiscUtils.getSHA1Password("test"));
		user.setTimezone("GMT");
		user.setLocale("EN_US");
		user.setIsActive(true);
		
		user = userService.save(user, dataHelper.getRootAuthData(env), env);
		assertNotNull(user.getId());
		
		assertNotNull(userService.authenticate(user.getUserName(), "test", env));
		
		// find user by username
		UserFilter filter = new UserFilter();
		filter.setUsername("test");
		List<User> users = userService.get(filter, env);
		assertEquals(1, users.size());
		assertEquals("test", users.get(0).getUserName());
		assertEquals(profile.getName(), users.get(0).getProfile().getName());
		assertEquals(Locale.EN_US, users.get(0).getLocaleSetting());
		
		testFindInactiveUsers(profile, env);
		testUserSettings(user, env);
		testInitStandardUsers(user, env);
		
		User duplicateUser = new User();
		duplicateUser.setProfile(profile);
		duplicateUser.setUserName("test");
		duplicateUser.setEmail("test2@kolmu.com");
		duplicateUser.setPassword(MiscUtils.getSHA1Password("test"));
		duplicateUser.setTimezone("GMT");
		duplicateUser.setLocale("EN_US");
		duplicateUser.setIsActive(false);
		
		try
		{
			duplicateUser = userService.save(duplicateUser, dataHelper.getRootAuthData(env), env);
			fail("Creating two users with the same user name should fail");
		}
		catch (UniqueCheckViolationException e)
		{
			// expected
		}
	}

	private void testInitStandardUsers(User user, EnvData env) throws KommetException
	{
		// create some object, e.g. profile
		Profile profile = new Profile();
		profile.setName("Test Profile");
		profile.setCreatedBy(user);
		profile.setLastModifiedBy(user);
		
		Map<KID, User> usersById = userService.getUsersForStandardFields(Arrays.asList(profile), env);
		assertEquals(1, usersById.size());
		assertEquals(profile.getCreatedBy().getId(), usersById.get(user.getId()).getId());
	}

	private void testFindInactiveUsers(Profile profile, EnvData env) throws KommetException
	{
		// create inactive user
		User inactiveUser = new User();
		inactiveUser.setProfile(profile);
		inactiveUser.setUserName("test2");
		inactiveUser.setEmail("test2@kolmu.com");
		inactiveUser.setPassword("test2");
		inactiveUser.setTimezone("GMT");
		inactiveUser.setLocale("EN_US");
		inactiveUser.setIsActive(false);
		
		inactiveUser = userService.save(inactiveUser, dataHelper.getRootAuthData(env), env);
		assertNotNull(inactiveUser.getId());
		
		// now find inactive users
		UserFilter filter = new UserFilter();
		filter.setIsActive(false);
		List<User> users = userService.get(filter, env);
		assertEquals(1, users.size());
		assertEquals(inactiveUser.getId(), users.get(0).getId());
	}

	private void testUserSettings(User user, EnvData env) throws KommetException
	{
		UserSettings settings = new UserSettings();
		settings.setUser(user);
		settings.setLandingURL("some/url/test");
		
		settings = userService.save(settings, dataHelper.getRootAuthData(env), env);
		assertNotNull(settings.getId());
	}
}
