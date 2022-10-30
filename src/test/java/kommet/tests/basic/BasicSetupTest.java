/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.basic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.auth.PermissionService;
import kommet.auth.ProfileService;
import kommet.auth.UserService;
import kommet.basic.BasicSetupService;
import kommet.basic.Layout;
import kommet.basic.Profile;
import kommet.basic.StandardAction;
import kommet.basic.User;
import kommet.basic.actions.ActionService;
import kommet.basic.keetle.LayoutService;
import kommet.basic.keetle.ViewService;
import kommet.basic.types.SystemPermissionSets;
import kommet.basic.types.SystemTypes;
import kommet.config.Constants;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.services.SystemSettingService;
import kommet.systemsettings.SystemSettingKey;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.uch.UserCascadeHierarchyService;

public class BasicSetupTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	SystemSettingService systemSettingService;
	
	@Inject
	ActionService actionService;
	
	@Inject
	LayoutService layoutService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	UserService userService;
	
	@Inject
	EnvService envService;
	
	@Inject
	ViewService viewService;
	
	@Inject
	PermissionService permissionService;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	@Test
	public void testCreateTypesSets() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		
		basicSetupService.runBasicSetup(env);
		
		String blankLayoutId = systemSettingService.getSettingValue(SystemSettingKey.BLANK_LAYOUT_ID, env);
		assertNotNull(blankLayoutId);
		
		try
		{
			KID.get(blankLayoutId);
		}
		catch (KIDException e)
		{
			fail("Blank layout ID has invalid value " + blankLayoutId);
		}
		
		// test layout settings - make sure basic layout has been set as default
		assertNotNull(layoutService.getById(KID.get(blankLayoutId), env));
		Layout basicLayout = layoutService.getByName(Constants.BASIC_LAYOUT_NAME, env);
		assertNotNull(basicLayout);
		assertEquals(basicLayout.getId(), layoutService.getDefaultLayoutId(AuthData.getRootAuthData(env), env));
		
		String errorViewId = systemSettingService.getSettingValue(SystemSettingKey.DEFAULT_ERROR_VIEW_ID, env);
		assertNotNull(errorViewId);
		assertNotNull(viewService.getView(KID.get(errorViewId), env));
		
		// find root profile
		Profile rootProfile = profileService.getProfileByName(Profile.ROOT_NAME, env);
		assertNotNull(rootProfile);
		assertEquals(KID.get(Profile.ROOT_ID), rootProfile.getId());
		
		// find sys admin profile
		Profile saProfile = profileService.getProfileByName(Profile.SYSTEM_ADMINISTRATOR_NAME, env);
		assertNotNull(saProfile);
		assertEquals(KID.get(Profile.SYSTEM_ADMINISTRATOR_ID), saProfile.getId());
		
		// find unauthenticated profile
		Profile unauthenticatedProfile = profileService.getProfileByName(Profile.UNAUTHENTICATED_NAME, env);
		assertNotNull(unauthenticatedProfile);
		assertEquals(KID.get(Profile.UNAUTHENTICATED_ID), unauthenticatedProfile.getId());
		
		Profile unauthenticatedProfile2 = profileService.getUnauthenticatedProfile(env);
		assertNotNull(unauthenticatedProfile2);
		assertEquals(KID.get(Profile.UNAUTHENTICATED_ID), unauthenticatedProfile2.getId());
		
		// make sure User type has been created
		assertNotNull("User type not created by basic setup", env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.USER_API_NAME)));
		
		// make sure Group type has been created
		assertNotNull("Group type not created by basic setup", env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.PERMISSION_SET_API_NAME)));
		
		// make sure a group called superadmins exists
		List<Record> groups = env.getSelectCriteriaFromDAL("select id from " + SystemTypes.PERMISSION_SET_API_NAME + " where name = '" + SystemPermissionSets.SUPERADMIN + "'").list();
		assertEquals(1, groups.size());
		
		// make sure a group called support exists
		groups = env.getSelectCriteriaFromDAL("select id from " + SystemTypes.PERMISSION_SET_API_NAME + " where name = '" + SystemPermissionSets.SUPPORT + "'").list();
		assertEquals(1, groups.size());
		
		// make sure KollFile object has been created
		env.getSelectCriteriaFromDAL("select id, accessLevel, isDraft from " + SystemTypes.CLASS_API_NAME).list();
		
		env.getSelectCriteriaFromDAL("select id, accessLevel from " + SystemTypes.VIEW_API_NAME).list();
		
		// make sure action object is created
		env.getSelectCriteriaFromDAL("select id, createdDate, isSystem, isPublic, url, name, controller.id, controller.name, controller.packageName, controllerMethod, view.name, view.path, view.id, view.layout.id, typeId from " + SystemTypes.ACTION_API_NAME).list();
		
		// make sure unique check object has been created
		env.getSelectCriteriaFromDAL("select id, isSystem, name from " + SystemTypes.UNIQUE_CHECK_API_NAME).list();
		
		// make sure layout type is created
		env.getSelectCriteriaFromDAL("select id, name from " + SystemTypes.LAYOUT_API_NAME).list();
		
		// make sure system setting type is created
		env.getSelectCriteriaFromDAL("select id from " + SystemTypes.SYSTEM_SETTING_API_NAME).list();
		
		// make sure file type is created
		env.getSelectCriteriaFromDAL("select id, revisions.id, access, sealed from " + SystemTypes.FILE_API_NAME).list();
		
		// make sure file revision type is created
		env.getSelectCriteriaFromDAL("select id, file.id from " + SystemTypes.FILE_REVISION_API_NAME).list();
		
		// make sure file object assignment type is created
		env.getSelectCriteriaFromDAL("select id, file.id, comment, recordId from " + SystemTypes.FILE_RECORD_ASSIGNMENT_API_NAME).list();
		
		// make sure type info type is created
		env.getSelectCriteriaFromDAL("select id, standardController.id, defaultDetailsAction.id, defaultEditAction.id, defaultCreateAction.id, defaultListAction.id from " + SystemTypes.TYPE_INFO_API_NAME).list();
		
		// make sure type comment type is created
		env.getSelectCriteriaFromDAL("select id, parent.id, content, recordId from " + SystemTypes.COMMENT_API_NAME).list();
		
		// make sure type field history type is created
		env.getSelectCriteriaFromDAL("select id, oldValue, newValue, fieldId, recordId, operation from " + SystemTypes.FIELD_HISTORY_API_NAME).list();
		
		// make sure type TypeTrigger is created
		env.getSelectCriteriaFromDAL("select typeId, triggerFile.id, isActive, isSystem, isBeforeInsert, isBeforeUpdate, isBeforeDelete, isAfterInsert, isAfterUpdate, isAfterDelete from " + SystemTypes.TYPE_TRIGGER_API_NAME).list();
		
		assertEquals("true", systemSettingService.getSettingValue(SystemSettingKey.REASSIGN_DEFAULT_ACTION_ON_ACTION_DELETE, env));
		//assertEquals((Integer)0, systemSettingService.getSettingIntValue(SystemSettingService.ASSOCIATION_COUNT, env));
		
		// make sure user-record sharings have been created
		env.getSelectCriteriaFromDAL("select recordId, user.id, isGeneric, reason, read, edit, delete, sharingRule.id from " + SystemTypes.USER_RECORD_SHARING_API_NAME).list();
		
		// make sure group-record sharings have been created
		env.getSelectCriteriaFromDAL("select recordId, group.id, isGeneric, reason, read, edit, delete, sharingRule.id from " + SystemTypes.GROUP_RECORD_SHARING_API_NAME).list();
		
		// make sure scheduled task have been created
		env.getSelectCriteriaFromDAL("select name, cronExpression, file.id, method from " + SystemTypes.SCHEDULED_TASK_API_NAME).list();
		
		// make sure profile type has been created
		env.getSelectCriteriaFromDAL("select name, label, systemProfile from " + SystemTypes.PROFILE_API_NAME).list();
		
		// make sure user settings type has been created
		env.getSelectCriteriaFromDAL("select id, landingURL, user.id, profile.id, layout.id from " + SystemTypes.USER_SETTINGS_API_NAME).list();
		
		// make sure doc template type has been created
		env.getSelectCriteriaFromDAL("select id, name, content from " + SystemTypes.DOC_TEMPLATE_API_NAME).list();
		
		// make sure label type has been created
		env.getSelectCriteriaFromDAL("select id, key, value, locale from " + SystemTypes.TEXT_LABEL_API_NAME).list();
		
		// make sure label type has been created
		env.getSelectCriteriaFromDAL("select id, name, active, code, errorMessage, errorMessageLabel, isSystem, referencedFields from " + SystemTypes.VALIDATION_RULE_API_NAME).list();
		
		// make sure email type has been created
		env.getSelectCriteriaFromDAL("select id, messageId, subject, plainTextBody, htmlBody, sender, recipients, ccRecipients, bccRecipients, status, sendDate from " + SystemTypes.EMAIL_API_NAME).list();
		
		// make sure notification type has been created
		env.getSelectCriteriaFromDAL("select id, title, viewedDate, text, assignee.id from " + SystemTypes.NOTIFICATION_API_NAME).list();
		
		// make sure error log type has been created
		env.getSelectCriteriaFromDAL("select id, message, details, severity, codeClass, codeLine, affectedUser.id from " + SystemTypes.ERROR_LOG_API_NAME).list();
		
		// make sure login history type has been created
		env.getSelectCriteriaFromDAL("select id, loginUser.id, method, ip4Address, ip6Address, result from " + SystemTypes.LOGIN_HISTORY_API_NAME).list();
		
		// make sure report type type has been created
		env.getSelectCriteriaFromDAL("select id, name, serializedQuery, baseTypeId, description from " + SystemTypes.REPORT_TYPE_API_NAME).list();
		
		// make sure user cascade hierarchy has been created
		env.getSelectCriteriaFromDAL("select id, activeContextName, activeContextRank, localeName, env, profile.id, contextUser.id, userGroup.id from " + SystemTypes.USER_CASCADE_HIERARCHY_API_NAME).list();
		
		// make sure user group type has been created
		env.getSelectCriteriaFromDAL("select id, name, description, users.id, subgroups.id from " + SystemTypes.USER_GROUP_API_NAME).list();
		
		// make sure user group assignment type has been created
		env.getSelectCriteriaFromDAL("select id, parentGroup.id, childGroup.id, childUser.id from " + SystemTypes.USER_GROUP_ASSIGNMENT_API_NAME).list();
		
		// make sure setting value type has been created
		env.getSelectCriteriaFromDAL("select id, key, value, hierarchy.id from " + SystemTypes.SETTING_VALUE_API_NAME).list();
		
		// make sure web resource type has been created
		env.getSelectCriteriaFromDAL("select id, file.id, file.name, mimeType, name from " + SystemTypes.WEB_RESOURCE_API_NAME).list();
		
		// make sure view resource type has been created
		env.getSelectCriteriaFromDAL("select id, path, content, mimeType, name from " + SystemTypes.VIEW_RESOURCE_API_NAME).list();
		
		// make sure app type has been created
		env.getSelectCriteriaFromDAL("select id, name, label, type, urls.id, urls.url from " + SystemTypes.APP_API_NAME).list();
		
		// make sure app url type has been created
		env.getSelectCriteriaFromDAL("select id, url, app.id, app.name from " + SystemTypes.APP_URL_API_NAME).list();
		
		// make sure task type has been created
		env.getSelectCriteriaFromDAL("select id, title, content, priority, status, dueDate, progress, assignedUser.id, assignedGroup.id, recordId from " + SystemTypes.TASK_API_NAME).list();
		
		// make sure library type has been created
		env.getSelectCriteriaFromDAL("select id, name, isEnabled, status, accessLevel, source, provider, version, description, items.id, items.apiName from " + SystemTypes.LIBRARY_API_NAME).list();
		
		// make sure library item type has been created
		env.getSelectCriteriaFromDAL("select id, apiName, componentType, accessLevel, definition, library.id, library.name, recordId from " + SystemTypes.LIBRARY_ITEM_API_NAME).list();
		
		// make sure event type has been created
		env.getSelectCriteriaFromDAL("select id, name, startDate, endDate, description, owner.userName, guests.response, guests.guest.userName, guests.responseComment from " + SystemTypes.EVENT_API_NAME).list();
		
		// make sure event guest type has been created
		env.getSelectCriteriaFromDAL("select id, guest.userName, event.name, response, responseComment from " + SystemTypes.EVENT_GUEST_API_NAME).list();
		
		// make sure any record type has been created
		env.getSelectCriteriaFromDAL("select id, recordId from " + SystemTypes.ANY_RECORD_API_NAME).list();
		
		// make sure label type has been created
		env.getSelectCriteriaFromDAL("select id, text from " + SystemTypes.LABEL_API_NAME).list();
		
		// make sure label type has been created
		env.getSelectCriteriaFromDAL("select id, label.id, recordId from " + SystemTypes.LABEL_ASSIGNMENT_API_NAME).list();
		
		// make sure sharing rule type has been created
		env.getSelectCriteriaFromDAL("select id, name, description, file.id, method, type, referencedType, isEdit, isDelete, dependentTypes, sharedWith from " + SystemTypes.SHARING_RULE_API_NAME).list();
		
		env.getSelectCriteriaFromDAL("select id, name, label, invocationOrder, compiledClass.id, isCallable, isTriggerable, isDraft, isActive, description, paramAssignments.id, invocations.id, invocations.name, invocations.invokedAction.id, transitions.id, transitions.previousAction.id, transitions.nextAction.id from " + SystemTypes.BUSINESS_PROCESS_API_NAME).list();
		
		env.getSelectCriteriaFromDAL("select id, type, name, description, file.id, isEntryPoint, inputs.id, outputs.id from " + SystemTypes.BUSINESS_ACTION_API_NAME).list();
		
		env.getSelectCriteriaFromDAL("select id, name, dataTypeId, dataTypeName, description, businessProcess.id, businessAction.id from " + SystemTypes.BUSINESS_PROCESS_INPUT_API_NAME).list();
		
		env.getSelectCriteriaFromDAL("select id, name, dataTypeId, dataTypeName, description, businessProcess.id, businessAction.id from " + SystemTypes.BUSINESS_PROCESS_OUTPUT_API_NAME).list();
		
		env.getSelectCriteriaFromDAL("select id, name, parentProcess.id, invokedAction.id, invokedProcess.id from " + SystemTypes.BUSINESS_ACTION_INVOCATION_API_NAME).list();
		
		env.getSelectCriteriaFromDAL("select id, businessProcess.id, nextAction.id, previousAction.id from " + SystemTypes.BUSINESS_ACTION_TRANSITION_API_NAME).list();
		
		env.getSelectCriteriaFromDAL("select id, processInput.id, processOutput.id, businessProcess.id, sourceInvocation.id, targetInvocation.id, sourceParam.id, targetParam.id from " + SystemTypes.BUSINESS_PROCESS_PARAM_ASSIGNMENT_API_NAME).list();
		
		env.getSelectCriteriaFromDAL("select id, name, value, invocation.id from " + SystemTypes.BUSINESS_ACTION_INVOCATION_ATTRIBUTE_API_NAME).list();
		
		env.getSelectCriteriaFromDAL("select id, name, url, action.id, typeId, onClick from " + SystemTypes.BUTTON_API_NAME).list();
		
		env.getSelectCriteriaFromDAL("select id, title, content, recordId, referencedField, media, intervalUnit, intervalValue, assignedUser.id, assignedGroup.id from " + SystemTypes.REMINDER_API_NAME).list();
		
		env.getSelectCriteriaFromDAL("select id, name, items.name, items.key from " + SystemTypes.DICTIONARY_API_NAME).list();
		
		env.getSelectCriteriaFromDAL("select id, name, key, index from " + SystemTypes.DICTIONARY_ITEM_API_NAME).list();
		
		// make sure standard actions have been created for all standard types accessible to users
		for (Type type : env.getAllTypes())
		{
			if (!SystemTypes.isInaccessibleSystemType(type))
			{
				List<StandardAction> stdActions = actionService.getStandardActionsForType(type.getKID(), env);
				assertEquals(4, stdActions.size());
			}
		}
		
		// make sure default env locale is set
		assertEquals("EN_US", systemSettingService.getSettingValue(SystemSettingKey.DEFAULT_ENV_LOCALE, env));
		
		for (Type type : env.getAllTypes())
		{
			assertTrue(type.getKID().getId().startsWith(KID.TYPE_PREFIX));
			
			// make sure unique checks are initialized on types
			assertNotNull("No unique checks on type " + type.getQualifiedName(), type.getUniqueChecks());
			assertTrue("No unique checks on type " + type.getQualifiedName(), !type.getUniqueChecks().isEmpty());
		}
		
		// make sure both system administrator can create records of certain types
		testSystemAdministratorPermissions(env);
		testGuestPermissions(env);
		
		User guestUser = userService.get(Constants.UNAUTHENTICATED_USER_NAME, env);
		assertNotNull(guestUser);
		assertEquals(Constants.UNAUTHENTICATED_USER_NAME, guestUser.getUserName());
		assertEquals(Profile.UNAUTHENTICATED_NAME, guestUser.getProfile().getName());
		assertNotNull(env.getGuestUser());
		assertEquals(env.getGuestUser().getId(), guestUser.getId());
	}

	private void testGuestPermissions(EnvData env) throws KommetException
	{
		Type fileType = env.getType(KeyPrefix.get(KID.FILE_PREFIX));
		assertTrue(AuthData.getGuestAuthData(permissionService, uchService, env).canReadType(fileType.getKID(), true, env));
		assertFalse(AuthData.getGuestAuthData(permissionService, uchService, env).canReadAllType(fileType.getKID(), true, env));
		assertFalse(AuthData.getGuestAuthData(permissionService, uchService, env).canEditAllType(fileType.getKID(), true, env));
		assertFalse(AuthData.getGuestAuthData(permissionService, uchService, env).canCreateType(fileType.getKID(), true, env));
		assertFalse(AuthData.getGuestAuthData(permissionService, uchService, env).canEditType(fileType.getKID(), true, env));
		assertFalse(AuthData.getGuestAuthData(permissionService, uchService, env).canDeleteType(fileType.getKID(), true, env));
		
		Type userType = env.getType(KeyPrefix.get(KID.USER_PREFIX));
		assertTrue(AuthData.getGuestAuthData(permissionService, uchService, env).canReadType(userType.getKID(), true, env));
		assertFalse(AuthData.getGuestAuthData(permissionService, uchService, env).canReadAllType(userType.getKID(), true, env));
		assertFalse(AuthData.getGuestAuthData(permissionService, uchService, env).canEditAllType(userType.getKID(), true, env));
		assertFalse(AuthData.getGuestAuthData(permissionService, uchService, env).canCreateType(userType.getKID(), true, env));
		assertFalse(AuthData.getGuestAuthData(permissionService, uchService, env).canEditType(userType.getKID(), true, env));
		assertFalse(AuthData.getGuestAuthData(permissionService, uchService, env).canDeleteType(userType.getKID(), true, env));
	}

	private void testSystemAdministratorPermissions (EnvData env) throws KommetException
	{
		// create sa user
		Profile saProfile = profileService.getProfileByName(Profile.SYSTEM_ADMINISTRATOR_NAME, env);
		User saUser = userService.save(dataHelper.getTestUser("testsa@kommet.io", "testsa@kommet.io", saProfile, env), dataHelper.getRootAuthData(env), env);
		AuthData saAuthData = dataHelper.getAuthData(saUser, env);
		
		// create root user
		Profile rootProfile = profileService.getProfileByName(Profile.ROOT_NAME, env);
		User rootUser = userService.save(dataHelper.getTestUser("testroot@kommet.io", "testrott@kommet.io", rootProfile, env), dataHelper.getRootAuthData(env), env);
		AuthData rootAuthData = dataHelper.getAuthData(rootUser, env);
		
		for (Type type : env.getAllTypes())
		{
			assertTrue("System administrator cannot create records of type " + type.getQualifiedName(), saAuthData.canCreateType(type.getKID(), false, env));
			assertTrue("System administrator cannot edit records of type " + type.getQualifiedName(), saAuthData.canEditType(type.getKID(), false, env));
			assertTrue("System administrator cannot delete records of type " + type.getQualifiedName(), saAuthData.canDeleteType(type.getKID(), false, env));
			assertTrue("System administrator cannot edit records of type " + type.getQualifiedName(), saAuthData.canEditAllType(type.getKID(), false, env));
			assertTrue("System administrator cannot delete records of type " + type.getQualifiedName(), saAuthData.canDeleteAllType(type.getKID(), false, env));
			assertTrue(rootAuthData.canCreateType(type.getKID(), false, env));
			assertTrue(rootAuthData.canEditType(type.getKID(), false, env));
			assertTrue(rootAuthData.canDeleteType(type.getKID(), false, env));
		}
	}
	}