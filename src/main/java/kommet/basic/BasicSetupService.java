/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.auth.PermissionService;
import kommet.auth.ProfileService;
import kommet.auth.RootAuthData;
import kommet.auth.UserService;
import kommet.basic.keetle.LayoutService;
import kommet.basic.keetle.ViewService;
import kommet.basic.keetle.ViewSyntaxException;
import kommet.basic.keetle.ViewUtil;
import kommet.basic.types.ActionKType;
import kommet.basic.types.ActionPermissionKType;
import kommet.basic.types.AnyRecordKType;
import kommet.basic.types.AppKType;
import kommet.basic.types.AppUrlKType;
import kommet.basic.types.BusinessActionInvocationAttributeKType;
import kommet.basic.types.BusinessActionInvocationKType;
import kommet.basic.types.BusinessActionKType;
import kommet.basic.types.BusinessActionTransitionKType;
import kommet.basic.types.BusinessProcessInputKType;
import kommet.basic.types.BusinessProcessKType;
import kommet.basic.types.BusinessProcessOutputKType;
import kommet.basic.types.BusinessProcessParamAssignmentKType;
import kommet.basic.types.ButtonKType;
import kommet.basic.types.ClassKType;
import kommet.basic.types.CommentKType;
import kommet.basic.types.DictionaryItemKType;
import kommet.basic.types.DictionaryKType;
import kommet.basic.types.DocTemplateKType;
import kommet.basic.types.EmailKType;
import kommet.basic.types.ErrorLogKType;
import kommet.basic.types.EventGuestKType;
import kommet.basic.types.EventKType;
import kommet.basic.types.FieldHistoryKType;
import kommet.basic.types.FieldPermissionKType;
import kommet.basic.types.FileKType;
import kommet.basic.types.FileRecordAssignmentKType;
import kommet.basic.types.FileRevisionKType;
import kommet.basic.types.GroupRecordSharingKType;
import kommet.basic.types.LabelAssignmentKType;
import kommet.basic.types.LabelKType;
import kommet.basic.types.LayoutKType;
import kommet.basic.types.LibraryKType;
import kommet.basic.types.LoginHistoryKType;
import kommet.basic.types.NotificationKType;
import kommet.basic.types.PermissionSetKType;
import kommet.basic.types.ProfileKType;
import kommet.basic.types.ReminderKType;
import kommet.basic.types.ReportTypeKType;
import kommet.basic.types.ScheduledTaskKType;
import kommet.basic.types.SettingValueKType;
import kommet.basic.types.SharingRuleKType;
import kommet.basic.types.StandardActionKType;
import kommet.basic.types.SystemPermissionSets;
import kommet.basic.types.SystemSettingKType;
import kommet.basic.types.SystemTypes;
import kommet.basic.types.TaskDependencyKType;
import kommet.basic.types.TaskKType;
import kommet.basic.types.TextLabelKType;
import kommet.basic.types.TypeInfoKType;
import kommet.basic.types.TypePermissionKType;
import kommet.basic.types.TypeTriggerKType;
import kommet.basic.types.UniqueCheckKType;
import kommet.basic.types.UserCascadeHierarchyKType;
import kommet.basic.types.UserGroupAssignmentKType;
import kommet.basic.types.UserGroupKType;
import kommet.basic.types.UserKType;
import kommet.basic.types.UserRecordSharingKType;
import kommet.basic.types.UserSettingsKType;
import kommet.basic.types.ValidationRuleKType;
import kommet.basic.types.ViewKType;
import kommet.basic.types.ViewResourceKType;
import kommet.basic.types.WebResourceKType;
import kommet.businessprocess.BusinessProcessService;
import kommet.config.Constants;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.GlobalSettings;
import kommet.data.KeyPrefix;
import kommet.data.KeyPrefixException;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.UniqueCheckService;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.TypeReference;
import kommet.data.validationrules.ValidationRuleService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.i18n.Locale;
import kommet.koll.ClassService;
import kommet.services.GlobalSettingsService;
import kommet.services.SystemSettingService;
import kommet.services.ViewResourceService;
import kommet.systemsettings.SystemSettingKey;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.PropertyUtilException;

@Service
public class BasicSetupService
{
	@Inject
	DataService dataService;
	
	@Inject
	GlobalSettingsService settingService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	ViewService viewService;
	
	@Inject
	LayoutService layoutService;
	
	@Inject
	ValidationRuleService vrService;
	
	@Inject
	SystemSettingService systemSettingService;
	
	@Inject
	ViewResourceService viewResourceService;
	
	@Inject
	UniqueCheckService uniqueCheckService;
	
	@Inject
	PermissionService permissionService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	EnvService envService;
	
	@Inject
	UserService userService;
	
	@Inject
	ClassService classService;
	
	@Inject
	BusinessProcessService bpService;
	
	public final static String ROOT_USERNAME = "root";
	public final static String ROOT_USER_EMAIL = "root@kommet.io";
	
	private static final Logger log = LoggerFactory.getLogger(BasicSetupService.class);
	
	private RootAuthData rootAuthData;
	
	/**
	 * Tells if a type is a basic type.
	 * 
	 * Basic types are types necessary for the functioning of every environment. They are created on
	 * every environment during its creation.
	 * 
	 * @param type
	 * @return
	 */
	public static boolean isBasicType (Type type)
	{
		return type instanceof UserKType || type instanceof PermissionSetKType || type instanceof ProfileKType ||
				type instanceof TypePermissionKType || type instanceof FieldPermissionKType || type instanceof ActionPermissionKType ||
				type instanceof ViewKType || type instanceof ClassKType || type instanceof ActionKType || type instanceof StandardActionKType ||
				type instanceof UniqueCheckKType || type instanceof LayoutKType || type instanceof SystemSettingKType ||
				type instanceof FileKType || type instanceof FileRevisionKType || type instanceof FileRecordAssignmentKType ||
				type instanceof TypeInfoKType || type instanceof CommentKType || type instanceof FieldHistoryKType ||
				type instanceof TypeTriggerKType || type instanceof UserRecordSharingKType || type instanceof ScheduledTaskKType ||
				type instanceof UserSettingsKType || type instanceof DocTemplateKType || type instanceof TextLabelKType || type instanceof ValidationRuleKType ||
				type instanceof EmailKType || type instanceof NotificationKType || type instanceof LoginHistoryKType || type instanceof ErrorLogKType ||
				type instanceof ReportTypeKType || type instanceof UserCascadeHierarchyKType || type instanceof UserGroupKType ||
				type instanceof UserGroupAssignmentKType || type instanceof GroupRecordSharingKType || type instanceof SettingValueKType ||
				type instanceof WebResourceKType || type instanceof ViewResourceKType || type instanceof AppKType || type instanceof AppUrlKType ||
				type instanceof TaskKType || type instanceof TaskDependencyKType || type instanceof LibraryKType || type instanceof LibraryItemKType ||
				type instanceof EventKType || type instanceof AnyRecordKType || type instanceof EventGuestKType || type instanceof LabelKType || type instanceof LabelAssignmentKType ||
				type instanceof SharingRuleKType || type instanceof BusinessProcessKType || type instanceof BusinessActionKType || type instanceof BusinessProcessInputKType ||
				type instanceof BusinessProcessOutputKType || type instanceof BusinessActionInvocationKType || type instanceof BusinessActionTransitionKType || type instanceof BusinessProcessParamAssignmentKType ||
				type instanceof BusinessActionInvocationAttributeKType || type instanceof ButtonKType || type instanceof ReminderKType || type instanceof DictionaryKType ||
				type instanceof DictionaryItemKType;
 				
	}

	public static Record getRoot (UserKType userType, Record rootProfile, String username, String email) throws KommetException
	{
		Record rootUser = new Record(userType);
		rootUser.setField("userName", username);
		rootUser.setField("email", email);
		rootUser.setField("password", MiscUtils.getSHA1Password("test"));
		rootUser.setField("profile", rootProfile);
		rootUser.setField("timezone", "GMT");
		rootUser.setField("locale", "EN_US");
		rootUser.setField("isActive", true);
		
		return rootUser;
	}
	
	public CoreSetup runCoreSetup(EnvData env) throws PropertyUtilException, KommetException
	{
		// set initial values for sequences
		settingService.setSetting(GlobalSettings.TYPE_SEQ, appConfig.getTypeSeqStart(), env);
		settingService.setSetting(GlobalSettings.FIELD_SEQ, appConfig.getFieldSeqStart(), env);
		
		// create profile type
		ProfileKType profileType = createProfileType(env);
		
		// create root profile
		Record rootProfile = dataService.saveSystemProfile(getRootProfile(env), false, env);
		
		// create system administrator profile
		dataService.saveSystemProfile(getSystemAdministratorProfile(env), false, env);
		
		// create unauthenticated profile
		Record guestProfile = dataService.saveSystemProfile(getUnauthenticatedProfile(env), false, env);
		
		// create root authdata
		AuthData rootAuthData = new AuthData();
		User root = new User();
		root.setId(AppConfig.getRootUserId());
		
		Profile rootProfileObj = new Profile();
		rootProfileObj.setId(rootProfile.getKID());
		
		rootAuthData.setProfile(rootProfileObj);
		rootAuthData.setUser(root);
		
		// create user type
		UserKType userType = createUserType(env, profileType, rootAuthData);
		
		// create an instance of the user object that will be a super admin of the environment
		Record rootUser = createRootUser(env, rootProfile);
		
		// now that user type is created, and the root user exists, we can add createdby and lastmodifiedby fields on the profile type
		dataService.addUserReferencesOnProfileAndUser(userType, profileType, false, rootAuthData, env);
		
		// create guest (unauthenticated) user
		createGuestUser(env, rootAuthData, guestProfile);
		
		// create UniqueCheck type
		UniqueCheckKType ucType = createUniqueCheckType(env);
		
		// create a unique check for user's user name
		UniqueCheck uniqueUserNameCheck = new UniqueCheck();
		uniqueUserNameCheck.setName("UniqueUserName");
		uniqueUserNameCheck.setTypeId(userType.getKID());
		uniqueUserNameCheck.addField(userType.getField("userName"));
		uniqueUserNameCheck.setIsSystem(true);
		uniqueUserNameCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(uniqueUserNameCheck, true, rootAuthData, env);
		
		// create a unique check for profile name
		UniqueCheck uniqueProfileNameCheck = new UniqueCheck();
		uniqueProfileNameCheck.setName("UniqueProfileName");
		uniqueProfileNameCheck.setTypeId(profileType.getKID());
		uniqueProfileNameCheck.addField(profileType.getField("name"));
		uniqueProfileNameCheck.setIsSystem(true);
		uniqueProfileNameCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(uniqueProfileNameCheck, true, rootAuthData, env);
		
		// refetch root user and root profile
		// because they contain reference to profile and user types, but these references does not know
		// that some fields have been added to these types (fields: createdby and lastmodifiedby)
		rootUser = env.getSelectCriteriaFromDAL("select id, userName, profile.id, profile.name, accessType from User where id = '" + rootUser.getKID() + "' limit 1").singleRecord();
		rootProfile = env.getSelectCriteriaFromDAL("select id, name from Profile where id = '" + rootProfile.getKID() + "' limit 1").singleRecord();
		//ucType = (UniqueCheckKType)env.getType(ucType.getKID());
		profileType = (ProfileKType)env.getType(profileType.getKID());
		userType = (UserKType)env.getType(userType.getKID());
		
		return new CoreSetup(profileType, userType, ucType, rootUser, rootProfile);
	}

	public void runBasicSetup (EnvData env) throws KommetException
	{
		CoreSetup coreSetup = runCoreSetup(env);
		
		// create PermissionSet object
		PermissionSetKType permissionSetType = createPermissionSetType(env);
		
		// create system permission sets
		Map<String, Record> systemPermissionSets = getSystemPermissionSets(env);
		Map<String, Record> savedPermissionSets = new HashMap<String, Record>();
		for (Record permissionSet : systemPermissionSets.values())
		{
			savedPermissionSets.put((String)permissionSet.getField("name"), dataService.save(permissionSet, true, true, env));
		}
		
		// set the profile for system administrator
		coreSetup.getRootUser().setField("profile", coreSetup.getSysAdminProfile());
		dataService.save(coreSetup.getRootUser(), true, true, env);
		
		ValidationRuleKType vrType = createValidationRuleType(env);
		
		// create user-record sharings - this has to be done before other types are created
		// because for other types a check edit permissions trigger is created that uses the
		// sharing table
		UserRecordSharingKType ursType = createUserRecordSharings(coreSetup.getUserType(), env);
		
		UserGroupKType userGroupType = createUserGroupType(coreSetup.getUserType(), env);
		
		// we need to create the UCH type as early as possible, because it is used in text field validation 
		// when other types are created
		UserCascadeHierarchyKType uchType = createUserCascadeHierarchyType(env, coreSetup.getUserType(), userGroupType, coreSetup.getProfileType());
		
		executePostInitDbScripts(env);
		
		createSettingValueType(uchType, env);
		
		// create check edit permissions trigger for user and profile type
		// we could not create these permissions earlier because we wanted to update some users and profiles a few times
		dataService.createCheckPermissionsTriggers(coreSetup.getUserType(), env);
		dataService.createCheckPermissionsTriggers(coreSetup.getProfileType(), env);
		dataService.createCheckPermissionsTriggers(coreSetup.getUniqueCheckType(), env);
		
		// layout type must be created before the view type, because the latter references it
		Type layoutType = createLayoutType(env);
		
		// some fields on User type already exist that reference the stale profile type (not the one on the env)
		// so we will update those references
		// this part is only needed for tests, because normally the env would have been initialized much earlier and then read in anew,
		// so all references would be up-to-date
		((TypeReference)env.getType(coreSetup.getUserType().getKID()).getField("profile").getDataType()).setType(coreSetup.getProfileType());
		
		Type viewKType = createViewType(layoutType, env, coreSetup.getRootUser().getKID());
		ClassKType clsKType = createClassType(env, coreSetup.getRootUser().getKID());
		ActionKType actionType = createActionType(env, viewKType, clsKType, coreSetup.getRootUser().getKID());
		
		createTypeTriggerType(clsKType, coreSetup.getRootUser().getKID(), env);
		
		AuthData rootAuthData = getRootAuthData(env);
		
		// now add field references to views and koll files to the TypeInfo type
		createTypeInfoType(actionType, clsKType, coreSetup.getRootUser().getKID(), env);
		
		createStandardActionType(env, coreSetup.getProfileType(), actionType, coreSetup.getRootUser().getKID());
		
		createPermissionTypes(coreSetup.getProfileType(), permissionSetType, actionType, env);
		
		// create system setting type
		createSystemSettingType(env);
		
		// create file and file revision types
		FileKType fileType = createFileAndFileRevisionType(env, coreSetup.getRootUserAuthData());
		createCommentType(coreSetup.getUserType(), env, coreSetup.getRootUser().getKID());
		createFieldHistoryType(env, coreSetup.getRootUser().getKID());
		createScheduledTaskType(env, clsKType, coreSetup.getRootUserAuthData());
		createUserSettingsType(env, coreSetup.getUserType(), coreSetup.getProfileType(), layoutType);
		createDocTemplateType(env);
		createTextLabelType(env);
		createEmailType(vrType, env);
		createNotificationType(coreSetup.getUserType(), env, coreSetup.getRootUser().getKID());
		createErrorLogType(coreSetup.getUserType(), env, coreSetup.getRootUser().getKID());
		createLoginHistoryType(coreSetup.getUserType(), env, coreSetup.getRootUser().getKID());
		createReportTypeType(env);
		GroupRecordSharingKType grsType = createGroupRecordSharings(userGroupType, env);
		createWebResourceType(fileType, env);
		createViewResourceType(env);
		createAppType(env);
		createTaskType(coreSetup.getUserType(), userGroupType, env);
		createLibraryType(fileType, coreSetup.getRootUserAuthData(), env);
		createEventType(coreSetup.getUserType(), coreSetup.getRootUserAuthData(), env);
		createLabelType(env);
		createAnyRecordType(coreSetup.getRootUserAuthData(), env);
		createSharingRule(clsKType, ursType, grsType, coreSetup.getRootUserAuthData(), env);
		createBusinessProcess(clsKType, env);
		createButtonType(actionType, env);
		createReminderType(coreSetup.getUserType(), userGroupType, env);
		createDictionaryType(env);
		
		env.scanForPersistenceMappings("kommet", false);
		
		// initialize view dir
		viewService.initKeetleDir(env, true);
		// initialize layout dir
		layoutService.initLayoutDir(env, true);
		//layoutService.setEmptyDefaultLayout(appConfig, env);
		
		// initialize view resource dir
		viewResourceService.initViewResourcesOnDisk(env, true);
		
		// When types were created, standard pages were not generated for standard types.
		// However, since users do have access to some standard types (e.g. user or file),
		// we want to generate standard pages for these types as well so that they can be customized by users.
		generateStandardActionsForAccessibleStandardTypes(rootAuthData, env);
		
		// create blank layout
		Layout blankLayout = layoutService.createBlankLayout(rootAuthData, env);
		Layout basicLayout = createBasicLayout(rootAuthData, env);
		
		layoutService.setDefaultLayout(basicLayout, rootAuthData, env);
		systemSettingService.setSetting(SystemSettingKey.BLANK_LAYOUT_ID, blankLayout.getId().getId(), RecordAccessType.SYSTEM, rootAuthData, env);
		
		// define default locale for this environment
		systemSettingService.setSetting(SystemSettingKey.DEFAULT_ENV_LOCALE, Locale.EN_US.toString(), RecordAccessType.SYSTEM, rootAuthData, env);
		
		systemSettingService.setSetting(SystemSettingKey.IGNORE_NON_EXISTING_FIELD_LABELS, "true", RecordAccessType.SYSTEM, rootAuthData, env);
		
		systemSettingService.setSetting(SystemSettingKey.MIN_PASSWORD_LENGTH, appConfig.getMinPasswordLength().toString(), RecordAccessType.SYSTEM, rootAuthData, env);
		
		systemSettingService.setSetting(SystemSettingKey.DEFAULT_ERROR_VIEW_ID, createDefaultErrorView(rootAuthData, env), RecordAccessType.SYSTEM, rootAuthData, env);
				
		setDefaultSettings(env, dataService.getRootAuthData(env));
		
		Profile saProfile = profileService.getProfileByName(Profile.SYSTEM_ADMINISTRATOR_NAME, env);
		
		// give system administrator permissions to create records of all types
		for (Type type : env.getAllTypes())
		{
			permissionService.setTypePermissionForProfile(saProfile.getId(), type.getKID(), true, true, true, true, true, true, true, RecordAccessType.SYSTEM_IMMUTABLE, rootAuthData, env);
		}
		
		env.setGuestUser(userService.get(Constants.UNAUTHENTICATED_USER_NAME, env));
		profileService.setUnauthenticatedProfileDefaultPermissions(rootAuthData, env);
		
		// create basic instructions/actions for the business process
		createBusinessProcessBasicActions(dataService.getRootAuthData(env), env);
	}
	
	private void createBusinessProcess(ClassKType clsKType, EnvData env) throws KommetException
	{
		BusinessProcessKType bpType = (BusinessProcessKType)dataService.createType(new BusinessProcessKType(clsKType), dataService.getRootAuthData(env), env);
		
		// add unique check for the name
		UniqueCheck uniqueNameCheck = new UniqueCheck();
		uniqueNameCheck.setName("UniqueBusinessProcessName");
		uniqueNameCheck.setTypeId(bpType.getKID());
		uniqueNameCheck.setIsSystem(true);
		uniqueNameCheck.addField(bpType.getField("name"));
		uniqueNameCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(uniqueNameCheck, true, dataService.getRootAuthData(env), env);
		
		BusinessActionKType bpaType = (BusinessActionKType)dataService.createType(new BusinessActionKType(clsKType), dataService.getRootAuthData(env), env);
		
		ValidationRule draftProcessNotActiveVR = new ValidationRule();
		draftProcessNotActiveVR.setActive(true);
		draftProcessNotActiveVR.setIsSystem(true);
		draftProcessNotActiveVR.setName("BusinessProcessDraftNotActive");
		draftProcessNotActiveVR.setTypeId(bpType.getKID());
		draftProcessNotActiveVR.setCode("(isNotNull(isDraft) and isDraft == false) or (isActive == false)");
		draftProcessNotActiveVR.setErrorMessage("Business processes in draft state cannot be set as active");
		draftProcessNotActiveVR.setActive(true);
		draftProcessNotActiveVR.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		vrService.saveSystemValidationRule(draftProcessNotActiveVR, dataService.getRootAuthData(env), env);
		
		/*ValidationRule compiledClassVR = new ValidationRule();
		compiledClassVR.setActive(true);
		compiledClassVR.setIsSystem(true);
		compiledClassVR.setName("CompiledClassOnBusinessProcess");
		compiledClassVR.setTypeId(bpType.getKID());
		compiledClassVR.setCode("(isNotNull(isActive) and isActive == false) or isNotNull(compiledClass)");
		compiledClassVR.setErrorMessage("Compiled class must be set on a business process that is active");
		compiledClassVR.setActive(true);
		compiledClassVR.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		vrService.saveSystemValidationRule(compiledClassVR, dataService.getRootAuthData(env), env);*/
		
		// add unique check for the name
		UniqueCheck uniqueBPANameCheck = new UniqueCheck();
		uniqueBPANameCheck.setName("UniqueBusinessProcessActionName");
		uniqueBPANameCheck.setTypeId(bpaType.getKID());
		uniqueBPANameCheck.setIsSystem(true);
		uniqueBPANameCheck.addField(bpaType.getField("name"));
		uniqueBPANameCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(uniqueBPANameCheck, true, dataService.getRootAuthData(env), env);
		
		ValidationRule notNullFileRefVR = new ValidationRule();
		notNullFileRefVR.setActive(true);
		notNullFileRefVR.setIsSystem(true);
		notNullFileRefVR.setName("BusinessActionFileNotNull");
		notNullFileRefVR.setTypeId(bpaType.getKID());
		notNullFileRefVR.setCode("(isNotNull(file) and type == 'Action')  or (isNull(file) and type <> 'Action')");
		notNullFileRefVR.setErrorMessage("File can be set only for user-defined actions");
		notNullFileRefVR.setActive(true);
		notNullFileRefVR.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		vrService.saveSystemValidationRule(notNullFileRefVR, dataService.getRootAuthData(env), env);
		
		UniqueCheck uniqueBPAFileCheck = new UniqueCheck();
		uniqueBPAFileCheck.setName("UniqueBusinessProcessActionFile");
		uniqueBPAFileCheck.setTypeId(bpaType.getKID());
		uniqueBPAFileCheck.setIsSystem(true);
		uniqueBPAFileCheck.addField(bpaType.getField("file"));
		uniqueBPAFileCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(uniqueBPAFileCheck, true, dataService.getRootAuthData(env), env);
		
		BusinessProcessInputKType bpiType = (BusinessProcessInputKType)dataService.createType(new BusinessProcessInputKType(bpaType, bpType), dataService.getRootAuthData(env), env);
		
		// add unique check for the name
		UniqueCheck uniqueBPINameCheck = new UniqueCheck();
		uniqueBPINameCheck.setName("UniqueBusinessProcessInputNameForAction");
		uniqueBPINameCheck.setTypeId(bpiType.getKID());
		uniqueBPINameCheck.setIsSystem(true);
		uniqueBPINameCheck.addField(bpiType.getField("name"));
		uniqueBPINameCheck.addField(bpiType.getField("businessAction"));
		uniqueBPINameCheck.addField(bpiType.getField("businessProcess"));
		uniqueBPINameCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(uniqueBPINameCheck, true, dataService.getRootAuthData(env), env);
		
		ValidationRule bpiVR = new ValidationRule();
		bpiVR.setActive(true);
		bpiVR.setIsSystem(true);
		bpiVR.setName("BusinessProcessInputParent");
		bpiVR.setTypeId(bpiType.getKID());
		bpiVR.setCode("(isNotNull(businessProcess) and isNull(businessAction))  or (isNotNull(businessAction) and isNull(businessProcess))");
		bpiVR.setErrorMessage("Both business process and business action cannot be set on an input parameter");
		bpiVR.setActive(true);
		bpiVR.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		vrService.saveSystemValidationRule(bpiVR, dataService.getRootAuthData(env), env);
		
		ValidationRule inputDataTypeVR = new ValidationRule();
		inputDataTypeVR.setActive(true);
		inputDataTypeVR.setIsSystem(true);
		inputDataTypeVR.setName("InputDataTypeSet");
		inputDataTypeVR.setTypeId(bpiType.getKID());
		inputDataTypeVR.setCode("isNotNull(dataTypeName) or isNotNull(dataTypeId)");
		inputDataTypeVR.setErrorMessage("Either data type name or data type ID must be set on input param");
		inputDataTypeVR.setActive(true);
		inputDataTypeVR.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		vrService.saveSystemValidationRule(inputDataTypeVR, dataService.getRootAuthData(env), env);
		
		BusinessProcessOutputKType bpoType = (BusinessProcessOutputKType)dataService.createType(new BusinessProcessOutputKType(bpaType, bpType), dataService.getRootAuthData(env), env);
		
		// add unique check for the name
		UniqueCheck uniqueBPONameCheck = new UniqueCheck();
		uniqueBPONameCheck.setName("UniqueBusinessProcessOutputNameForAction");
		uniqueBPONameCheck.setTypeId(bpoType.getKID());
		uniqueBPONameCheck.setIsSystem(true);
		uniqueBPONameCheck.addField(bpoType.getField("name"));
		uniqueBPONameCheck.addField(bpoType.getField("businessAction"));
		uniqueBPONameCheck.addField(bpoType.getField("businessProcess"));
		uniqueBPONameCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(uniqueBPONameCheck, true, dataService.getRootAuthData(env), env);
		
		// an inverse collection of BusinessActionInputs on the Business Action type
		Field inputsField = new Field();
		inputsField.setApiName("inputs");
		inputsField.setLabel("Inputs");
		inputsField.setRequired(false);
		InverseCollectionDataType inputsType = new InverseCollectionDataType();
		inputsType.setInverseProperty("businessAction");
		inputsType.setInverseType(bpiType);
		inputsField.setDataType(inputsType);
		bpaType.addField(inputsField);
		dataService.createField(inputsField, env);
		
		// an inverse collection of BusinessActionInputs on the Business Action type
		Field outputsField = new Field();
		outputsField.setApiName("outputs");
		outputsField.setLabel("Outputs");
		outputsField.setRequired(false);
		InverseCollectionDataType outputsType = new InverseCollectionDataType();
		outputsType.setInverseProperty("businessAction");
		outputsType.setInverseType(bpoType);
		outputsField.setDataType(outputsType);
		bpaType.addField(outputsField);
		dataService.createField(outputsField, env);
		
		// an inverse collection of BusinessProcessInputs on the Business Process type
		Field processInputsField = new Field();
		processInputsField.setApiName("inputs");
		processInputsField.setLabel("Inputs");
		processInputsField.setRequired(false);
		InverseCollectionDataType processInputsType = new InverseCollectionDataType();
		processInputsType.setInverseProperty("businessProcess");
		processInputsType.setInverseType(bpiType);
		processInputsField.setDataType(processInputsType);
		bpType.addField(processInputsField);
		dataService.createField(processInputsField, env);
		
		// an inverse collection of BusinessActionInputs on the Business Process type
		Field processOutputsField = new Field();
		processOutputsField.setApiName("outputs");
		processOutputsField.setLabel("Outputs");
		processOutputsField.setRequired(false);
		InverseCollectionDataType processOutputsType = new InverseCollectionDataType();
		processOutputsType.setInverseProperty("businessProcess");
		processOutputsType.setInverseType(bpoType);
		processOutputsField.setDataType(processOutputsType);
		bpType.addField(processOutputsField);
		dataService.createField(processOutputsField, env);
		
		ValidationRule bpoVR = new ValidationRule();
		bpoVR.setActive(true);
		bpoVR.setIsSystem(true);
		bpoVR.setName("UniqueBusinessProcessInputNameForAction");
		bpoVR.setTypeId(bpiType.getKID());
		bpoVR.setCode("(isNotNull(businessProcess) and isNull(businessAction))  or (isNotNull(businessAction) and isNull(businessProcess))");
		bpoVR.setErrorMessage("Both business process and business action cannot be set on an output parameter");
		bpoVR.setActive(true);
		bpoVR.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		vrService.saveSystemValidationRule(bpoVR, dataService.getRootAuthData(env), env);
		
		ValidationRule outputDataTypeVR = new ValidationRule();
		outputDataTypeVR.setActive(true);
		outputDataTypeVR.setIsSystem(true);
		outputDataTypeVR.setName("OutputDataTypeSet");
		outputDataTypeVR.setTypeId(bpoType.getKID());
		outputDataTypeVR.setCode("isNotNull(dataTypeName) or isNotNull(dataTypeId)");
		outputDataTypeVR.setErrorMessage("Either data type name or data type ID must be set on output param");
		outputDataTypeVR.setActive(true);
		outputDataTypeVR.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		vrService.saveSystemValidationRule(outputDataTypeVR, dataService.getRootAuthData(env), env);
		
		BusinessActionInvocationKType baiType = (BusinessActionInvocationKType)dataService.createType(new BusinessActionInvocationKType(bpType, bpaType), dataService.getRootAuthData(env), env);
		BusinessActionTransitionKType batType = (BusinessActionTransitionKType)dataService.createType(new BusinessActionTransitionKType(bpType, baiType), dataService.getRootAuthData(env), env);
		BusinessProcessParamAssignmentKType bppaType = (BusinessProcessParamAssignmentKType)dataService.createType(new BusinessProcessParamAssignmentKType(bpType, baiType, bpiType, bpoType), dataService.getRootAuthData(env), env);
		
		ValidationRule invokedActionVR = new ValidationRule();
		invokedActionVR.setActive(true);
		invokedActionVR.setIsSystem(true);
		invokedActionVR.setName("InvokedActionSetRule");
		invokedActionVR.setTypeId(baiType.getKID());
		invokedActionVR.setCode("(isNotNull(invokedProcess) and isNull(invokedAction))  or (isNotNull(invokedAction) and isNull(invokedProcess))");
		invokedActionVR.setErrorMessage("Either invoked process or invoked action must be set on action invocation, but not both");
		invokedActionVR.setActive(true);
		invokedActionVR.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		vrService.saveSystemValidationRule(invokedActionVR, dataService.getRootAuthData(env), env);
		
		// add a unique check for unique param assignment
		UniqueCheck uniqueParamAssignmentCheck = new UniqueCheck();
		uniqueParamAssignmentCheck.setName("UniqueParamAssignmentInProcess");
		uniqueParamAssignmentCheck.setTypeId(bppaType.getKID());
		uniqueParamAssignmentCheck.setIsSystem(true);
		uniqueParamAssignmentCheck.addField(bppaType.getField("targetInvocation"));
		uniqueParamAssignmentCheck.addField(bppaType.getField("sourceInvocation"));
		uniqueParamAssignmentCheck.addField(bppaType.getField("targetParam"));
		uniqueParamAssignmentCheck.addField(bppaType.getField("sourceParam"));
		uniqueParamAssignmentCheck.addField(bppaType.getField("processInput"));
		uniqueParamAssignmentCheck.addField(bppaType.getField("processOutput"));
		uniqueParamAssignmentCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(uniqueParamAssignmentCheck, true, dataService.getRootAuthData(env), env);
		
		ValidationRule paramAssignmentPropertiesVR = new ValidationRule();
		paramAssignmentPropertiesVR.setActive(true);
		paramAssignmentPropertiesVR.setIsSystem(true);
		paramAssignmentPropertiesVR.setName("ParamAssignmentProperties");
		paramAssignmentPropertiesVR.setTypeId(bppaType.getKID());
		
		String ruleCondition1 = "(isNotNull(targetInvocation) and isNotNull(targetParam) and isNull(processOutput)) or (isNull(targetInvocation) and isNull(targetParam) and isNotNull(processOutput))";
		String ruleCondition2 = "(isNotNull(sourceInvocation) and isNotNull(sourceParam) and isNull(processInput)) or (isNull(sourceInvocation) and isNull(sourceParam) and isNotNull(processInput))";
		
		paramAssignmentPropertiesVR.setCode("(" + ruleCondition1 + ") or (" + ruleCondition2 + ")");
		paramAssignmentPropertiesVR.setErrorMessage("Either data type name or data type ID must be set on output param");
		paramAssignmentPropertiesVR.setActive(true);
		paramAssignmentPropertiesVR.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		vrService.saveSystemValidationRule(paramAssignmentPropertiesVR, dataService.getRootAuthData(env), env);
		
		// an inverse collection of BusinessActionInvocations on the BusinessProcess type
		Field actionInvocationsField = new Field();
		actionInvocationsField.setApiName("invocations");
		actionInvocationsField.setLabel("Invocations");
		actionInvocationsField.setRequired(false);
		
		InverseCollectionDataType aiType = new InverseCollectionDataType();
		aiType.setInverseProperty("parentProcess");
		aiType.setInverseType(baiType);
		actionInvocationsField.setDataType(aiType);
		bpType.addField(actionInvocationsField);
		dataService.createField(actionInvocationsField, env);
		
		// an inverse collection of BusinessProcessParamAssignments on the BusinessProcess type
		Field assignmentsField = new Field();
		assignmentsField.setApiName("paramAssignments");
		assignmentsField.setLabel("Param Assignments");
		assignmentsField.setRequired(false);
		InverseCollectionDataType paramAssignmentsType = new InverseCollectionDataType();
		paramAssignmentsType.setInverseProperty("businessProcess");
		paramAssignmentsType.setInverseType(bppaType);
		assignmentsField.setDataType(paramAssignmentsType);
		bpType.addField(assignmentsField);
		dataService.createField(assignmentsField, env);
		
		// an inverse collection of BusinessActionTransitions on the BusinessProcess type
		Field transitionsField = new Field();
		transitionsField.setApiName("transitions");
		transitionsField.setLabel("Transitions");
		transitionsField.setRequired(false);
		
		InverseCollectionDataType transitionDataType = new InverseCollectionDataType();
		transitionDataType.setInverseProperty("businessProcess");
		transitionDataType.setInverseType(batType);
		transitionsField.setDataType(transitionDataType);
		bpType.addField(transitionsField);
		dataService.createField(transitionsField, env);
		
		// create invocation attribute type
		BusinessActionInvocationAttributeKType attrType = (BusinessActionInvocationAttributeKType)dataService.createType(new BusinessActionInvocationAttributeKType(baiType), env);
		
		// an inverse collection of BusinessActionInvocationAttributes on the Business Action Invocation type
		Field invAttributesField = new Field();
		invAttributesField.setApiName("attributes");
		invAttributesField.setLabel("Attributes");
		invAttributesField.setRequired(false);
		InverseCollectionDataType attrsType = new InverseCollectionDataType();
		attrsType.setInverseProperty("invocation");
		attrsType.setInverseType(attrType);
		invAttributesField.setDataType(attrsType);
		baiType.addField(invAttributesField);
		dataService.createField(invAttributesField, env);
		
		/*UniqueCheck uniqueInvocationAttrCheck = new UniqueCheck();
		uniqueInvocationAttrCheck.setName("UniqueInvocationAttribute");
		uniqueInvocationAttrCheck.setTypeId(attrType.getKID());
		uniqueInvocationAttrCheck.setIsSystem(true);
		uniqueInvocationAttrCheck.addField(attrType.getField("invocation"));
		uniqueInvocationAttrCheck.addField(attrType.getField("name"));
		uniqueInvocationAttrCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(uniqueInvocationAttrCheck, true, dataService.getRootAuthData(env), env);*/
	}

	private void createBusinessProcessBasicActions(AuthData authData, EnvData env) throws KommetException
	{
		// create if-action
		BusinessAction ifAction = new BusinessAction();
		ifAction.setName("IF-Clause");
		ifAction.setDescription("Evaluates a condition and follows one of the process branches");
		ifAction.setType("If");
		ifAction.setIsEntryPoint(false);
		ifAction.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		bpService.save(ifAction, authData, env);
		
		// create foreach-action
		BusinessAction forEachAction = new BusinessAction();
		forEachAction.setName("For Each");
		forEachAction.setDescription("Iterates over a collection of items");
		forEachAction.setType("ForEach");
		forEachAction.setIsEntryPoint(false);
		forEachAction.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		bpService.save(forEachAction, authData, env);
		
		// create entry points
		BusinessAction recordSaveAction = new BusinessAction();
		recordSaveAction.setName("Record Save");
		recordSaveAction.setDescription("Entry point fired when a record is created or updated");
		recordSaveAction.setType("RecordSave");
		recordSaveAction.setIsEntryPoint(true);
		recordSaveAction.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		bpService.save(recordSaveAction, authData, env);
		
		addRecordUpsertFields(recordSaveAction, authData, env);
		
		BusinessAction recordCreateAction = new BusinessAction();
		recordCreateAction.setName("Record Create");
		recordCreateAction.setDescription("Entry point fired when a record is created");
		recordCreateAction.setType("RecordCreate");
		recordCreateAction.setIsEntryPoint(true);
		recordCreateAction.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		bpService.save(recordCreateAction, authData, env);
		
		addRecordUpsertFields(recordCreateAction, authData, env);
		
		BusinessAction recordUpdateAction = new BusinessAction();
		recordUpdateAction.setName("Record Update");
		recordUpdateAction.setDescription("Entry point fired when a record is updated");
		recordUpdateAction.setType("RecordUpdate");
		recordUpdateAction.setIsEntryPoint(true);
		recordUpdateAction.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		bpService.save(recordUpdateAction, authData, env);
		
		addRecordUpsertFields(recordUpdateAction, authData, env);
		
		// create stop action
		BusinessAction stopAction = new BusinessAction();
		stopAction.setName("Stop");
		stopAction.setDescription("Stops the business process");
		stopAction.setType("Stop");
		stopAction.setIsEntryPoint(false);
		stopAction.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		bpService.save(stopAction, authData, env);
		
		// create stop action
		BusinessAction typeCastAction = new BusinessAction();
		typeCastAction.setName("Type Cast");
		typeCastAction.setDescription("Casts one type onto another");
		typeCastAction.setType("TypeCast");
		typeCastAction.setIsEntryPoint(false);
		typeCastAction.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		bpService.save(typeCastAction, authData, env);
		
		addRecordUpsertFields(typeCastAction, authData, env);
		
		BusinessAction fieldUpdateAction = new BusinessAction();
		fieldUpdateAction.setName("Field Update");
		fieldUpdateAction.setDescription("Updates a field and saves the record");
		fieldUpdateAction.setType("FieldUpdate");
		fieldUpdateAction.setIsEntryPoint(false);
		fieldUpdateAction.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		bpService.save(fieldUpdateAction, authData, env);
		
		addRecordUpsertFields(fieldUpdateAction, authData, env);
		
		createFieldValueAction(authData, env);
		
		// create query unique action
		bpService.createStandardBusinessActions(classService, dataService, env);
	}

	public void createFieldValueAction(AuthData authData, EnvData env) throws KommetException
	{
		BusinessAction fieldValueAction = new BusinessAction();
		fieldValueAction.setName("Field Value");
		fieldValueAction.setDescription("Retrieves a value of a record field");
		fieldValueAction.setType("FieldValue");
		fieldValueAction.setIsEntryPoint(false);
		fieldValueAction.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		bpService.save(fieldValueAction, authData, env);
		
		BusinessProcessInput fieldValueInput = new BusinessProcessInput();
		fieldValueInput.setBusinessAction(fieldValueAction);
		fieldValueInput.setDescription("Field name to read");
		fieldValueInput.setName(BusinessAction.FIELD_VALUE_ACTION_FIELD_NAME_INPUT);
		fieldValueInput.setDataTypeName(String.class.getName());
		fieldValueInput.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		bpService.save(fieldValueInput, authData, env);
		
		BusinessProcessInput recordInput = new BusinessProcessInput();
		recordInput.setBusinessAction(fieldValueAction);
		recordInput.setDescription("Record from which property is read");
		recordInput.setName(BusinessAction.FIELD_VALUE_ACTION_RECORD_INPUT);
		recordInput.setDataTypeName(RecordProxy.class.getName());
		recordInput.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		bpService.save(recordInput, authData, env);
		
		BusinessProcessOutput output = new BusinessProcessOutput();
		output.setBusinessAction(fieldValueAction);
		output.setDescription("Field value");
		output.setName(BusinessAction.FIELD_VALUE_ACTION_OUTPUT);
		output.setDataTypeName(Object.class.getName());
		output.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		bpService.save(output, authData, env);
	}

	private void addRecordUpsertFields(BusinessAction action, AuthData authData, EnvData env) throws KommetException
	{
		BusinessProcessInput input = new BusinessProcessInput();
		input.setBusinessAction(action);
		input.setDescription("Record that is being saved");
		input.setName("record");
		input.setDataTypeName(RecordProxy.class.getName());
		input.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		bpService.save(input, authData, env);
		
		BusinessProcessOutput output = new BusinessProcessOutput();
		output.setBusinessAction(action);
		output.setDescription("Record that has been saved");
		output.setName("record");
		output.setDataTypeName(RecordProxy.class.getName());
		output.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		bpService.save(output, authData, env);
	}

	private void createSharingRule(ClassKType clsType, UserRecordSharingKType ursType, GroupRecordSharingKType grsType, AuthData rootUserAuthData, EnvData env) throws KommetException
	{
		SharingRuleKType sharingRuleType = new SharingRuleKType(clsType);
		sharingRuleType = (SharingRuleKType)dataService.createType(sharingRuleType, dataService.getRootAuthData(env), env);
		
		UniqueCheck uniqueNameCheck = new UniqueCheck();
		uniqueNameCheck.setName("UniqueSharingRuleName");
		uniqueNameCheck.setTypeId(sharingRuleType.getKID());
		uniqueNameCheck.setIsSystem(true);
		uniqueNameCheck.addField(sharingRuleType.getField("name"));
		uniqueNameCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(uniqueNameCheck, true, dataService.getRootAuthData(env), env);
		
		UniqueCheck uniqueFileMethodCheck = new UniqueCheck();
		uniqueFileMethodCheck.setName("UniqueSharingRuleClassMethod");
		uniqueFileMethodCheck.setTypeId(sharingRuleType.getKID());
		uniqueFileMethodCheck.setIsSystem(true);
		uniqueFileMethodCheck.addField(sharingRuleType.getField("file"));
		uniqueFileMethodCheck.addField(sharingRuleType.getField("method"));
		uniqueFileMethodCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(uniqueFileMethodCheck, true, dataService.getRootAuthData(env), env);
		
		// add reference to sharing rule to the URS type
		Field sharingRuleField = new Field();
		sharingRuleField.setApiName("sharingRule");
		sharingRuleField.setLabel("Sharing Rule");
		
		TypeReference ruleRef = new TypeReference(sharingRuleType);
		
		// when a rule is deleted, delete the sharing based on it as well
		ruleRef.setCascadeDelete(true);
		
		sharingRuleField.setDataType(ruleRef);
		sharingRuleField.setDbColumn("sharingrule");
		sharingRuleField.setRequired(false);
		ursType.addField(sharingRuleField);
		
		dataService.createField(sharingRuleField, env);
		
		// add reference to sharing rule to the URS type
		Field groupSharingRuleField = new Field();
		groupSharingRuleField.setApiName("sharingRule");
		groupSharingRuleField.setLabel("Sharing Rule");
		
		TypeReference groupRuleRef = new TypeReference(sharingRuleType);
		
		// when a rule is deleted, delete the sharing based on it as well
		groupRuleRef.setCascadeDelete(true);
		
		groupSharingRuleField.setDataType(groupRuleRef);
		groupSharingRuleField.setDbColumn("sharingrule");
		groupSharingRuleField.setRequired(false);
		grsType.addField(groupSharingRuleField);
		
		dataService.createField(groupSharingRuleField, env);
	}

	private String createDefaultErrorView(AuthData rootAuthData2, EnvData env) throws ViewSyntaxException, KommetException
	{
		View view = new View();
		view.setIsSystem(true);
		view.setPackageName("kommet.standardviews");
		String viewName = "StandardErrorView";
		view.initKeetleCode(ViewUtil.getStandardErrorViewCode(viewName, view.getPackageName()), appConfig, env);
		view.setPath(viewName);
		view.setName(viewName);
		
		// access type has to be SYSTEM, even for system types, because the view is recompiled when the type changes
		view.setAccessType(RecordAccessType.SYSTEM.getId());
		view.setAccessLevel("Closed");
		view = viewService.save(view, appConfig, getRootAuthData(env), env);
		return view.getId().getId();
	}

	private Layout createBasicLayout(AuthData authData, EnvData env) throws KommetException
	{
		// now create basic layout as well
		java.io.File basicLayoutFile = new java.io.File(getClass().getClassLoader().getResource("basiclayout.layout").getFile());
		String layoutContent = null;
		
		Scanner scanner = null;
		try
		{
			scanner = new Scanner(basicLayoutFile);
			layoutContent = scanner.useDelimiter("\\Z").next();
		}
		catch (FileNotFoundException e)
		{
			throw new KommetException("Basic layout definition file not found in app resources");
		}
		finally
		{
			scanner.close();
		}
		
		Layout layout = new Layout();
		layout.setName(Constants.BASIC_LAYOUT_NAME);
		layout.setCode(layoutContent);
		
		// layout has access system, not system_immutable, because we sometimes allow root users to modify this
		// however, layout service checks if the modifying user is root, and only with such permission is the modification allowed
		layout.setAccessType(RecordAccessType.SYSTEM.getId());
		
		return layoutService.save(layout, authData, env);
	}

	private void createLibraryType(FileKType fileType, AuthData authData, EnvData env) throws KommetException
	{
		LibraryKType libType = new LibraryKType(fileType);
		libType = (LibraryKType)dataService.createType(libType, dataService.getRootAuthData(env), env);
		
		UniqueCheck uniqueNameCheck = new UniqueCheck();
		uniqueNameCheck.setName("UniqueLibraryName");
		uniqueNameCheck.setTypeId(libType.getKID());
		uniqueNameCheck.setIsSystem(true);
		uniqueNameCheck.addField(libType.getField("name"));
		uniqueNameCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(uniqueNameCheck, true, dataService.getRootAuthData(env), env);
		
		dataService.setDefaultField(libType.getKID(), libType.getField("name").getKID(), authData, env);
		
		LibraryItemKType liType = new LibraryItemKType(libType);
		liType = (LibraryItemKType)dataService.createType(liType, dataService.getRootAuthData(env), env);
		
		UniqueCheck uniqueLibItemCheck = new UniqueCheck();
		uniqueLibItemCheck.setName("UniqueLibraryItem");
		uniqueLibItemCheck.setTypeId(liType.getKID());
		uniqueLibItemCheck.setIsSystem(true);
		uniqueLibItemCheck.addField(liType.getField("apiName"));
		uniqueLibItemCheck.addField(liType.getField("componentType"));
		uniqueLibItemCheck.addField(liType.getField("library"));
		uniqueLibItemCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(uniqueLibItemCheck, true, dataService.getRootAuthData(env), env);
		
		// create inverse collection to items
		Field itemsField = new Field();
		itemsField.setApiName("items");
		itemsField.setLabel("Items");
		itemsField.setDataType(new InverseCollectionDataType(liType, "library"));
		itemsField.setDbColumn("items");
		itemsField.setRequired(false);
		libType.addField(itemsField);
		
		dataService.createField(itemsField, env);
		
		ValidationRule externalSourceVR = new ValidationRule();
		externalSourceVR.setActive(true);
		externalSourceVR.setIsSystem(true);
		externalSourceVR.setName("ExternalLibFieldsFilled");
		externalSourceVR.setTypeId(libType.getKID());
		externalSourceVR.setCode("source == 'Local' or (isNotNull(accessLevel) and isNotNull(isEnabled))");
		externalSourceVR.setErrorMessage("Both assigned user and assigned group cannot be set on a task");
		externalSourceVR.setActive(true);
		externalSourceVR.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		vrService.saveSystemValidationRule(externalSourceVR, dataService.getRootAuthData(env), env);
	}

	public void createMissingTypes(EnvData env) throws KeyPrefixException, KommetException
	{
		Type userType = env.getType(KeyPrefix.get(KID.USER_PREFIX));
		Type userGroupType = env.getType(KeyPrefix.get(KID.USER_GROUP_PREFIX));
		
		createTaskType(userType, userGroupType, env);
	}

	private void createTaskType(Type userType, Type userGroupType, EnvData env) throws KommetException
	{
		TaskKType taskType = new TaskKType(userType, userGroupType);
		taskType = (TaskKType)dataService.createType(taskType, dataService.getRootAuthData(env), env);
		
		ValidationRule assignedVR = new ValidationRule();
		assignedVR.setActive(true);
		assignedVR.setIsSystem(true);
		assignedVR.setName("TaskAssigneeNotNull");
		assignedVR.setTypeId(taskType.getKID());
		assignedVR.setCode("!(isNotNull(assignedGroup) and isNotNull(assignedUser))");
		assignedVR.setErrorMessage("Both assigned user and assigned group cannot be set on a task");
		assignedVR.setActive(true);
		assignedVR.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		vrService.saveSystemValidationRule(assignedVR, dataService.getRootAuthData(env), env);
		
		ValidationRule progressVR = new ValidationRule();
		progressVR.setActive(true);
		progressVR.setIsSystem(true);
		progressVR.setName("ProgressPercentageCheck");
		progressVR.setTypeId(taskType.getKID());
		progressVR.setCode("isNull(progress) or (progress >= 0 and progress <= 100)");
		progressVR.setErrorMessage("Progress must be expressed in percents and must be a number between 0 and 100");
		progressVR.setActive(true);
		progressVR.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		vrService.saveSystemValidationRule(progressVR, dataService.getRootAuthData(env), env);
		
		// now create task dependency
		TaskDependencyKType taskDepType = new TaskDependencyKType(taskType);
		taskDepType = (TaskDependencyKType)dataService.createType(taskDepType, dataService.getRootAuthData(env), env);
		
		ValidationRule diffParentAndChildVR = new ValidationRule();
		diffParentAndChildVR.setActive(true);
		diffParentAndChildVR.setIsSystem(true);
		diffParentAndChildVR.setName("TaskDependencyParentAndChildDifferent");
		diffParentAndChildVR.setTypeId(taskDepType.getKID());
		diffParentAndChildVR.setCode("parentTask.id <> childTask.id");
		diffParentAndChildVR.setErrorMessage("Parent and child tasks must be different");
		diffParentAndChildVR.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		vrService.saveSystemValidationRule(diffParentAndChildVR, dataService.getRootAuthData(env), env);
	}
	
	private void createReminderType(Type userType, Type userGroupType, EnvData env) throws KommetException
	{
		ReminderKType reminderType = new ReminderKType(userType, userGroupType);
		reminderType = (ReminderKType)dataService.createType(reminderType, dataService.getRootAuthData(env), env);
		
		ValidationRule assignedVR = new ValidationRule();
		assignedVR.setActive(true);
		assignedVR.setIsSystem(true);
		assignedVR.setName("ReminderAssigneeNotNull");
		assignedVR.setTypeId(reminderType.getKID());
		assignedVR.setCode("(isNotNull(assignedGroup) or isNotNull(assignedUser)) and !(isNotNull(assignedGroup) and isNotNull(assignedUser))");
		assignedVR.setErrorMessage("Either assigned user or assigned group can be set on a reminder, but not both");
		assignedVR.setActive(true);
		assignedVR.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		vrService.saveSystemValidationRule(assignedVR, dataService.getRootAuthData(env), env);
		
		UniqueCheck uniqueReminderAssignedUserCheck = new UniqueCheck();
		uniqueReminderAssignedUserCheck.setName("UniqueReminderAssignedUser");
		uniqueReminderAssignedUserCheck.setTypeId(reminderType.getKID());
		uniqueReminderAssignedUserCheck.addField(reminderType.getField("recordId"));
		uniqueReminderAssignedUserCheck.addField(reminderType.getField("media"));
		uniqueReminderAssignedUserCheck.addField(reminderType.getField("assignedUser"));
		uniqueReminderAssignedUserCheck.setIsSystem(true);
		uniqueReminderAssignedUserCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		
		uniqueCheckService.save(uniqueReminderAssignedUserCheck, true, dataService.getRootAuthData(env), env);
		
		UniqueCheck uniqueReminderAssignedUserGroupCheck = new UniqueCheck();
		uniqueReminderAssignedUserGroupCheck.setName("UniqueReminderAssignedUserGroup");
		uniqueReminderAssignedUserGroupCheck.setTypeId(reminderType.getKID());
		uniqueReminderAssignedUserGroupCheck.addField(reminderType.getField("recordId"));
		uniqueReminderAssignedUserGroupCheck.addField(reminderType.getField("media"));
		uniqueReminderAssignedUserGroupCheck.addField(reminderType.getField("assignedGroup"));
		uniqueReminderAssignedUserGroupCheck.setIsSystem(true);
		uniqueReminderAssignedUserGroupCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		
		uniqueCheckService.save(uniqueReminderAssignedUserGroupCheck, true, dataService.getRootAuthData(env), env);
	}

	private void createWebResourceType(Type fileType, EnvData env) throws KommetException
	{
		WebResourceKType webResourceType = new WebResourceKType(fileType);
		webResourceType = (WebResourceKType)dataService.createType(webResourceType, dataService.getRootAuthData(env), env);
		
		UniqueCheck uniqueNameCheck = new UniqueCheck();
		uniqueNameCheck.setName("UniqueWebResourceName");
		uniqueNameCheck.setTypeId(webResourceType.getKID());
		uniqueNameCheck.addField(webResourceType.getField("name"));
		uniqueNameCheck.setIsSystem(true);
		uniqueNameCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		
		uniqueCheckService.save(uniqueNameCheck, true, dataService.getRootAuthData(env), env);
	}
	
	private void createViewResourceType(EnvData env) throws KommetException
	{
		ViewResourceKType viewResourceType = new ViewResourceKType();
		viewResourceType = (ViewResourceKType)dataService.createType(viewResourceType, dataService.getRootAuthData(env), env);
		
		UniqueCheck uniqueNameCheck = new UniqueCheck();
		uniqueNameCheck.setName("UniqueViewResourceName");
		uniqueNameCheck.setTypeId(viewResourceType.getKID());
		uniqueNameCheck.addField(viewResourceType.getField("name"));
		uniqueNameCheck.setIsSystem(true);
		uniqueNameCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		
		uniqueCheckService.save(uniqueNameCheck, true, dataService.getRootAuthData(env), env);
		
		UniqueCheck uniquePathCheck = new UniqueCheck();
		uniquePathCheck.setName("UniqueViewResourcePath");
		uniquePathCheck.setTypeId(viewResourceType.getKID());
		uniquePathCheck.addField(viewResourceType.getField("path"));
		uniquePathCheck.setIsSystem(true);
		uniquePathCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		
		uniqueCheckService.save(uniquePathCheck, true, dataService.getRootAuthData(env), env);
	}
	
	private void createEventType(UserKType userType, AuthData rootAuthData, EnvData env) throws KommetException
	{
		EventKType eventType = new EventKType(userType);
		eventType = (EventKType)dataService.createType(eventType, dataService.getRootAuthData(env), env);
		
		dataService.setDefaultField(eventType.getKID(), eventType.getField("name").getKID(), dataService.getRootAuthData(env), env);
		
		ValidationRule dateVR = new ValidationRule();
		dateVR.setActive(true);
		dateVR.setIsSystem(true);
		dateVR.setName("EventDateCheck");
		dateVR.setTypeId(eventType.getKID());
		dateVR.setCode("startDate <= endDate");
		dateVR.setErrorMessage("Event start date must not be greater than end date");
		dateVR.setActive(true);
		dateVR.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		vrService.saveSystemValidationRule(dateVR, dataService.getRootAuthData(env), env);
		
		// create event guest type
		EventGuestKType eventGuestType = new EventGuestKType(eventType, userType);
		eventGuestType = (EventGuestKType)dataService.createType(eventGuestType, dataService.getRootAuthData(env), env);
		
		UniqueCheck uniqueGuestCheck = new UniqueCheck();
		uniqueGuestCheck.setName("UniqueEventGuest");
		uniqueGuestCheck.setTypeId(eventGuestType.getKID());
		uniqueGuestCheck.addField(eventGuestType.getField("guest"));
		uniqueGuestCheck.addField(eventGuestType.getField("event"));
		uniqueGuestCheck.setIsSystem(true);
		uniqueGuestCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		
		uniqueCheckService.save(uniqueGuestCheck, true, dataService.getRootAuthData(env), env);
		
		// add guests field on the event type
		Field guestsField = new Field();
		guestsField.setApiName("guests");
		guestsField.setLabel("Guests");
		
		InverseCollectionDataType guestsDT = new InverseCollectionDataType();
		guestsDT.setInverseType(eventGuestType);
		guestsDT.setInverseProperty("event");
		
		guestsField.setDataType(guestsDT);
		guestsField.setDbColumn("guests");
		guestsField.setRequired(false);
		eventType.addField(guestsField);
		
		dataService.createField(guestsField, env);
	}
	
	private void createLabelType(EnvData env) throws KommetException
	{
		LabelKType labelType = new LabelKType();
		labelType = (LabelKType)dataService.createType(labelType, dataService.getRootAuthData(env), env);
		
		dataService.setDefaultField(labelType.getKID(), labelType.getField("text").getKID(), dataService.getRootAuthData(env), env);
		
		UniqueCheck uniqueLabelTextCheck = new UniqueCheck();
		uniqueLabelTextCheck.setName("UniqueLabelText");
		uniqueLabelTextCheck.setTypeId(labelType.getKID());
		uniqueLabelTextCheck.addField(labelType.getField("text"));
		uniqueLabelTextCheck.setIsSystem(true);
		uniqueLabelTextCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		
		uniqueCheckService.save(uniqueLabelTextCheck, true, dataService.getRootAuthData(env), env);
		
		// create label assignment type
		LabelAssignmentKType laType = new LabelAssignmentKType(labelType);
		laType = (LabelAssignmentKType)dataService.createType(laType, dataService.getRootAuthData(env), env);
		
		UniqueCheck uniqueLabelAssignmentCheck = new UniqueCheck();
		uniqueLabelAssignmentCheck.setName("UniqueLabelRecordAssignment");
		uniqueLabelAssignmentCheck.setTypeId(laType.getKID());
		uniqueLabelAssignmentCheck.addField(laType.getField("recordId"));
		uniqueLabelAssignmentCheck.addField(laType.getField("label"));
		uniqueLabelAssignmentCheck.setIsSystem(true);
		uniqueLabelAssignmentCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		
		uniqueCheckService.save(uniqueLabelAssignmentCheck, true, dataService.getRootAuthData(env), env);
	}
	
	private void createButtonType(Type actionType, EnvData env) throws KommetException
	{
		ButtonKType buttonType = new ButtonKType(actionType);
		buttonType = (ButtonKType)dataService.createType(buttonType, dataService.getRootAuthData(env), env);
		
		dataService.setDefaultField(buttonType.getKID(), buttonType.getField("name").getKID(), dataService.getRootAuthData(env), env);
		
		UniqueCheck uniqueButtonNameCheck = new UniqueCheck();
		uniqueButtonNameCheck.setName("UniqueButtoName");
		uniqueButtonNameCheck.setTypeId(buttonType.getKID());
		uniqueButtonNameCheck.addField(buttonType.getField("name"));
		uniqueButtonNameCheck.setIsSystem(true);
		uniqueButtonNameCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		
		uniqueCheckService.save(uniqueButtonNameCheck, true, dataService.getRootAuthData(env), env);
		
		ValidationRule urlActionVR = new ValidationRule();
		urlActionVR.setActive(true);
		urlActionVR.setIsSystem(true);
		urlActionVR.setName("ButtonUrlOrActionSet");
		urlActionVR.setTypeId(buttonType.getKID());
		urlActionVR.setCode("isNull(url) or isNull(action)");
		urlActionVR.setErrorMessage("Both URL and action cannot be set for a button");
		urlActionVR.setActive(true);
		urlActionVR.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		vrService.saveSystemValidationRule(urlActionVR, dataService.getRootAuthData(env), env);
		
		ValidationRule btnActionDefinedVR = new ValidationRule();
		btnActionDefinedVR.setActive(true);
		btnActionDefinedVR.setIsSystem(true);
		btnActionDefinedVR.setName("ButtonActionDefinedVR");
		btnActionDefinedVR.setTypeId(buttonType.getKID());
		btnActionDefinedVR.setCode("isNotNull(url) or isNotNull(action) or isNotNull(onClick)");
		btnActionDefinedVR.setErrorMessage("Button URL, action or onClick event must be defined");
		btnActionDefinedVR.setActive(true);
		btnActionDefinedVR.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		vrService.saveSystemValidationRule(btnActionDefinedVR, dataService.getRootAuthData(env), env);
	}
	
	public void createDictionaryType(EnvData env) throws KommetException
	{
		DictionaryKType dictionaryType = new DictionaryKType();
		dictionaryType = (DictionaryKType)dataService.createType(dictionaryType, dataService.getRootAuthData(env), env);
		
		dataService.setDefaultField(dictionaryType.getKID(), dictionaryType.getField("name").getKID(), dataService.getRootAuthData(env), env);
		
		UniqueCheck uniqueDictionaryNameCheck = new UniqueCheck();
		uniqueDictionaryNameCheck.setName("UniqueDictionaryName");
		uniqueDictionaryNameCheck.setTypeId(dictionaryType.getKID());
		uniqueDictionaryNameCheck.addField(dictionaryType.getField("name"));
		uniqueDictionaryNameCheck.setIsSystem(true);
		uniqueDictionaryNameCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		
		uniqueCheckService.save(uniqueDictionaryNameCheck, true, dataService.getRootAuthData(env), env);
		
		DictionaryItemKType dictionaryItemType = new DictionaryItemKType(dictionaryType);
		dictionaryItemType = (DictionaryItemKType)dataService.createType(dictionaryItemType, dataService.getRootAuthData(env), env);
		
		dataService.setDefaultField(dictionaryItemType.getKID(), dictionaryType.getField("name").getKID(), dataService.getRootAuthData(env), env);
		
		UniqueCheck uniqueItemCheck = new UniqueCheck();
		uniqueItemCheck.setName("UniqueDictionaryItemName");
		uniqueItemCheck.setTypeId(dictionaryItemType.getKID());
		uniqueItemCheck.addField(dictionaryItemType.getField("name"));
		uniqueItemCheck.addField(dictionaryItemType.getField("dictionary"));
		uniqueItemCheck.setIsSystem(true);
		uniqueItemCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		
		uniqueCheckService.save(uniqueItemCheck, true, dataService.getRootAuthData(env), env);
		
		UniqueCheck uniqueItemKeyCheck = new UniqueCheck();
		uniqueItemKeyCheck.setName("UniqueDictionaryItemKey");
		uniqueItemKeyCheck.setTypeId(dictionaryItemType.getKID());
		uniqueItemKeyCheck.addField(dictionaryItemType.getField("key"));
		uniqueItemKeyCheck.addField(dictionaryItemType.getField("dictionary"));
		uniqueItemKeyCheck.setIsSystem(true);
		uniqueItemKeyCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		
		uniqueCheckService.save(uniqueItemKeyCheck, true, dataService.getRootAuthData(env), env);
		
		// create dictionary items field
		Field itemsField = new Field();
		itemsField.setApiName("items");
		itemsField.setLabel("Items");
		itemsField.setDataType(new InverseCollectionDataType(dictionaryItemType, "dictionary"));
		itemsField.setDbColumn("items");
		itemsField.setRequired(false);
		dictionaryType.addField(itemsField);
		
		dataService.createField(itemsField, env);
	}
	
	private void createAnyRecordType(AuthData rootAuthData, EnvData env) throws KommetException
	{
		AnyRecordKType anyRecordType = new AnyRecordKType();
		anyRecordType = (AnyRecordKType)dataService.createType(anyRecordType, dataService.getRootAuthData(env), env);
	}
	
	private void createAppType(EnvData env) throws KommetException
	{
		AppKType appType = new AppKType();
		appType = (AppKType)dataService.createType(appType, dataService.getRootAuthData(env), env);
		
		UniqueCheck uniqueNameCheck = new UniqueCheck();
		uniqueNameCheck.setName("UniqueAppResourceName");
		uniqueNameCheck.setTypeId(appType.getKID());
		uniqueNameCheck.addField(appType.getField("name"));
		uniqueNameCheck.setIsSystem(true);
		uniqueNameCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		
		uniqueCheckService.save(uniqueNameCheck, true, dataService.getRootAuthData(env), env);
		
		UniqueCheck uniqueLabelCheck = new UniqueCheck();
		uniqueLabelCheck.setName("UniqueAppLabel");
		uniqueLabelCheck.setTypeId(appType.getKID());
		uniqueLabelCheck.addField(appType.getField("label"));
		uniqueLabelCheck.setIsSystem(true);
		uniqueLabelCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		
		uniqueCheckService.save(uniqueLabelCheck, true, dataService.getRootAuthData(env), env);
		
		AppUrlKType appUrlType = new AppUrlKType(appType);
		appUrlType = (AppUrlKType)dataService.createType(appUrlType, dataService.getRootAuthData(env), env);
		
		UniqueCheck uniqueUrlCheck = new UniqueCheck();
		uniqueUrlCheck.setName("UniqueAppURLName");
		uniqueUrlCheck.setTypeId(appUrlType.getKID());
		uniqueUrlCheck.addField(appUrlType.getField("url"));
		uniqueUrlCheck.setIsSystem(true);
		uniqueUrlCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		
		uniqueCheckService.save(uniqueUrlCheck, true, dataService.getRootAuthData(env), env);
		
		// create app URLs field
		Field urlsField = new Field();
		urlsField.setApiName("urls");
		urlsField.setLabel("URLs");
		urlsField.setDataType(new InverseCollectionDataType(appUrlType, "app"));
		urlsField.setDbColumn("urls");
		urlsField.setRequired(false);
		appType.addField(urlsField);
		
		dataService.createField(urlsField, env);
	}

	// TODO make this method private once this type is added
	public void createSettingValueType(UserCascadeHierarchyKType uchType, EnvData env) throws KommetException
	{
		SettingValueKType svType = new SettingValueKType(uchType);
		dataService.createType(svType, true, true, true, true, true, false, dataService.getRootAuthData(env), env);
		
		// add references from type and field UCH labels to the setting value keys
		/*String constraint = "ALTER TABLE fields ADD CONSTRAINT field_uchlabel_fkey FOREIGN KEY (uchlabel) REFERENCES obj_" + KID.SETTING_VALUE_PREFIX + " (key) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;";
		env.getJdbcTemplate().execute(constraint);
		
		constraint = "ALTER TABLE types ADD CONSTRAINT types_uchlabel_fkey FOREIGN KEY (uchlabel) REFERENCES obj_" + KID.SETTING_VALUE_PREFIX + " (key) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;";
		env.getJdbcTemplate().execute(constraint);
		
		constraint = "ALTER TABLE types ADD CONSTRAINT types_uchplurallabel_fkey FOREIGN KEY (uchplurallabel) REFERENCES obj_" + KID.SETTING_VALUE_PREFIX + " (key) MATCH SIMPLE ON UPDATE NO ACTION ON DELETE NO ACTION;";
		env.getJdbcTemplate().execute(constraint);*/
	}

	private void executePostInitDbScripts(EnvData env) throws KommetException
	{
		createUserRecordSharing(env);
		createGetUserGroupsFunction(env);
		createGetParentGroupsFunction(env);
	}
	
	private void createGetUserGroupsFunction (EnvData env) throws KommetException
	{
		// create user record sharing
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("get-user-groups.sql");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		
		StringBuilder query = new StringBuilder();
		String line = null;
		
		try
		{
			while( (line = reader.readLine()) != null)
			{
				query.append(line).append("\n");
			}
		}
		catch (IOException e)
		{
			throw new KommetException("Error reading SQL script config: " + e.getMessage());
		}
			
		env.getJdbcTemplate().execute(query.toString());
	}
	
	private void createGetParentGroupsFunction (EnvData env) throws KommetException
	{
		// create user record sharing
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("get-parent-groups.sql");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		
		StringBuilder query = new StringBuilder();
		String line = null;
		
		try
		{
			while( (line = reader.readLine()) != null)
			{
				query.append(line).append("\n");
			}
		}
		catch (IOException e)
		{
			throw new KommetException("Error reading SQL script config: " + e.getMessage());
		}
			
		env.getJdbcTemplate().execute(query.toString());
	}

	private void createUserRecordSharing(EnvData env) throws KommetException
	{
		// create user record sharing
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("sharing-config-scripts.sql");
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		
		StringBuilder query = new StringBuilder();
		String line = null;
		
		try
		{
			while( (line = reader.readLine()) != null)
			{
				query.append(line).append("\n");
			}
		}
		catch (IOException e)
		{
			throw new KommetException("Error reading SQL script config: " + e.getMessage());
		}
			
		env.getJdbcTemplate().execute(query.toString());
	}

	private UserGroupAssignmentKType createUserGroupAssignmentType(UserKType userType, UserGroupKType userGroupType, EnvData env) throws KommetException
	{
		UserGroupAssignmentKType userGroupAssignmentType = new UserGroupAssignmentKType(userType, userGroupType);
		userGroupAssignmentType = (UserGroupAssignmentKType) dataService.createType(userGroupAssignmentType, dataService.getRootAuthData(env), env);
		
		ValidationRule childItemVR = new ValidationRule();
		childItemVR.setActive(true);
		childItemVR.setIsSystem(true);
		childItemVR.setName("ChildItemForUserGroupAssignmentVR");
		childItemVR.setTypeId(userGroupAssignmentType.getKID());
		childItemVR.setCode("(isNull(childUser) and isNotNull(childGroup)) or (isNotNull(childUser) and isNull(childGroup))");
		childItemVR.setErrorMessage("Exactly one of child user and child group can be set");
		childItemVR.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		vrService.saveSystemValidationRule(childItemVR, dataService.getRootAuthData(env), env);
		
		ValidationRule childGroupVR = new ValidationRule();
		childGroupVR.setActive(true);
		childGroupVR.setIsSystem(true);
		childGroupVR.setName("ChildGroupDiffFromParentGroupVR");
		childGroupVR.setTypeId(userGroupAssignmentType.getKID());
		childGroupVR.setCode("isNull(childGroup) or childGroup.id <> parentGroup.id");
		childGroupVR.setErrorMessage("Child group must be different from parent group");
		childGroupVR.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		vrService.saveSystemValidationRule(childGroupVR, dataService.getRootAuthData(env), env);
		
		// add unique checks
		UniqueCheck uc1 = new UniqueCheck();
		uc1.setIsSystem(true);
		uc1.setTypeId(userGroupAssignmentType.getKID());
		uc1.setName("UniqueUserGroupUserCombination");
		uc1.addField(userGroupAssignmentType.getField("childUser"));
		uc1.addField(userGroupAssignmentType.getField("parentGroup"));
		uc1.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(uc1, true, dataService.getRootAuthData(env), env);
		
		UniqueCheck uc2 = new UniqueCheck();
		uc2.setIsSystem(true);
		uc2.setTypeId(userGroupAssignmentType.getKID());
		uc2.setName("UniqueUserGroupSubgroupCombination");
		uc2.addField(userGroupAssignmentType.getField("childGroup"));
		uc2.addField(userGroupAssignmentType.getField("parentGroup"));
		uc2.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(uc2, true, dataService.getRootAuthData(env), env);
		
		return userGroupAssignmentType;
	}

	private UserGroupKType createUserGroupType(UserKType userType, EnvData env) throws KommetException
	{
		UserGroupKType userGroupType = new UserGroupKType();
		userGroupType = (UserGroupKType)dataService.createType(userGroupType, true, true, true, true, true, false, dataService.getRootAuthData(env), env);
		
		// create a unique constraint on file id, method and cron expression
		UniqueCheck uniqueNameCheck = new UniqueCheck();
		uniqueNameCheck.setName("UniqueUserGroupName");
		uniqueNameCheck.setTypeId(userGroupType.getKID());
		uniqueNameCheck.addField(userGroupType.getField("name"));
		uniqueNameCheck.setIsSystem(true);
		uniqueNameCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(uniqueNameCheck, true, dataService.getRootAuthData(env), env);
		
		UserGroupAssignmentKType ugaType = createUserGroupAssignmentType(userType, userGroupType, env);
		
		// now that UserGroupAssignment type is created, add an association field "users" to the UserGroup type
		Field usersField = new Field();
		usersField.setApiName("users");
		usersField.setLabel("Users");
		usersField.setDataType(new AssociationDataType(ugaType, userType, "parentGroup", "childUser"));
		userGroupType.addField(usersField);
		dataService.createField(usersField, env);
		
		userGroupType = (UserGroupKType) env.getType(userGroupType.getKID());
		
		// add a "subgroups" field
		Field subgroupsField = new Field();
		subgroupsField.setApiName("subgroups");
		subgroupsField.setLabel("Subgroups");
		subgroupsField.setDataType(new AssociationDataType(ugaType, userGroupType, "parentGroup", "childGroup"));
		userGroupType.addField(subgroupsField);
		dataService.createField(subgroupsField, env);
		
		return userGroupType;
	}

	private UserCascadeHierarchyKType createUserCascadeHierarchyType(EnvData env, UserKType userType, UserGroupKType userGroupType, ProfileKType profileType) throws KommetException
	{
		UserCascadeHierarchyKType uchType = new UserCascadeHierarchyKType(profileType, userType, userGroupType);
		return (UserCascadeHierarchyKType)dataService.createType(uchType, true, true, true, true, true, false, dataService.getRootAuthData(env), env);
	}

	private void createNotificationType(UserKType userType, EnvData env, KID rid) throws KommetException
	{
		NotificationKType notificationType = new NotificationKType(userType);
		dataService.createType(notificationType, dataService.getRootAuthData(env), env);
	}
	
	private void createLoginHistoryType(UserKType userType, EnvData env, KID rid) throws KommetException
	{
		LoginHistoryKType lhType = new LoginHistoryKType(userType);
		dataService.createType(lhType, dataService.getRootAuthData(env), env);
	}
	
	private void createErrorLogType(UserKType userType, EnvData env, KID rid) throws KommetException
	{
		ErrorLogKType logType = new ErrorLogKType(userType);
		dataService.createType(logType, dataService.getRootAuthData(env), env);
	}

	private void createDocTemplateType(EnvData env) throws KommetException
	{
		DocTemplateKType docTemplateType = new DocTemplateKType();
		docTemplateType = (DocTemplateKType)dataService.createType(docTemplateType, env);
		
		// create a unique constraint on file id, method and cron expression
		UniqueCheck uniqueNameCheck = new UniqueCheck();
		uniqueNameCheck.setName("UniqueDocTemplateName");
		uniqueNameCheck.setTypeId(docTemplateType.getKID());
		uniqueNameCheck.addField(docTemplateType.getField("name"));
		uniqueNameCheck.setIsSystem(true);
		uniqueNameCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(uniqueNameCheck, true, dataService.getRootAuthData(env), env);
	}
	
	private void createTextLabelType(EnvData env) throws KommetException
	{
		TextLabelKType labelType = new TextLabelKType();
		labelType = (TextLabelKType)dataService.createType(labelType, env);
		
		// create a unique constraint on file id, method and cron expression
		UniqueCheck uniqueNameCheck = new UniqueCheck();
		uniqueNameCheck.setName("UniqueLabelKeyForLocaleName");
		uniqueNameCheck.setTypeId(labelType.getKID());
		uniqueNameCheck.addField(labelType.getField("key"));
		uniqueNameCheck.addField(labelType.getField("locale"));
		uniqueNameCheck.setIsSystem(true);
		uniqueNameCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(uniqueNameCheck, true, dataService.getRootAuthData(env), env);
	}
	
	private ValidationRuleKType createValidationRuleType(EnvData env) throws KommetException
	{
		ValidationRuleKType vrType = new ValidationRuleKType();
		vrType = (ValidationRuleKType)dataService.createType(vrType, true, true, true, true, true, false, AuthData.getRootAuthData(env), env);
		
		// create a unique constraint on file id, method and cron expression
		UniqueCheck uniqueNameCheck = new UniqueCheck();
		uniqueNameCheck.setName("UniqueValidationRuleName");
		uniqueNameCheck.setTypeId(vrType.getKID());
		uniqueNameCheck.addField(vrType.getField("name"));
		uniqueNameCheck.setIsSystem(true);
		uniqueNameCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(uniqueNameCheck, true, dataService.getRootAuthData(env), env);
		
		return vrType;
	}
	
	private void createEmailType(ValidationRuleKType vrType, EnvData env) throws KommetException
	{
		EmailKType emailType = new EmailKType();
		emailType = (EmailKType)dataService.createType(emailType, env);
		
		ValidationRule sendDateVR = new ValidationRule();
		sendDateVR.setActive(true);
		sendDateVR.setIsSystem(true);
		sendDateVR.setName("SendDateValidationRuleForEmail");
		sendDateVR.setTypeId(emailType.getKID());
		sendDateVR.setCode("status == 'Draft' or (isNotNull(sendDate) and isNotNull(messageId))");
		sendDateVR.setErrorMessage("Send date and message ID must not be null when email status is different than 'Draft'");
		sendDateVR.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		vrService.saveSystemValidationRule(sendDateVR, dataService.getRootAuthData(env), env);
		
		// create a unique constraint on message ID
		UniqueCheck messageIdCheck = new UniqueCheck();
		messageIdCheck.setName("UniquEmailMessageID");
		messageIdCheck.setTypeId(emailType.getKID());
		messageIdCheck.addField(emailType.getField("messageId"));
		messageIdCheck.setIsSystem(true);
		messageIdCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(messageIdCheck, true, dataService.getRootAuthData(env), env);
	}
	
	private void createReportTypeType(EnvData env) throws KommetException
	{
		ReportTypeKType reportType = new ReportTypeKType();
		reportType = (ReportTypeKType)dataService.createType(reportType, env);
		
		// create a unique constraint on name field
		UniqueCheck nameCheck = new UniqueCheck();
		nameCheck.setName("UniqueReportTypeName");
		nameCheck.setTypeId(reportType.getKID());
		nameCheck.addField(reportType.getField("name"));
		nameCheck.setIsSystem(true);
		nameCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(nameCheck, true, dataService.getRootAuthData(env), env);
	}

	private void createUserSettingsType(EnvData env, UserKType userType, ProfileKType profileType, Type layoutType) throws KommetException
	{
		UserSettingsKType userSettingsType = new UserSettingsKType(userType, profileType, layoutType);
		userSettingsType = (UserSettingsKType)dataService.createType(userSettingsType, env);
	}

	private void createScheduledTaskType(EnvData env, Type classType, AuthData authData) throws KommetException
	{
		ScheduledTaskKType scheduledTaskType = new ScheduledTaskKType(classType);
		scheduledTaskType = (ScheduledTaskKType)dataService.createType(scheduledTaskType, env);
		
		// create a unique constraint on file id, method and cron expression
		UniqueCheck checkOne = new UniqueCheck();
		checkOne.setName("UniqueScheduledFileMethodCron");
		checkOne.setTypeId(scheduledTaskType.getKID());
		checkOne.addField(scheduledTaskType.getField("file"));
		checkOne.addField(scheduledTaskType.getField("method"));
		checkOne.addField(scheduledTaskType.getField("cronExpression"));
		checkOne.setIsSystem(true);
		checkOne.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(checkOne, true, dataService.getRootAuthData(env), env);
		
		// create a unique constraint on file id, method and cron expression
		UniqueCheck checkTwo = new UniqueCheck();
		checkTwo.setName("UniqueScheduledTaskName");
		checkTwo.setTypeId(scheduledTaskType.getKID());
		checkTwo.addField(scheduledTaskType.getField("name"));
		checkTwo.setIsSystem(true);
		checkTwo.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(checkTwo, true, dataService.getRootAuthData(env), env);
		
		// set name as the default field for file
		dataService.setDefaultField(scheduledTaskType.getKID(), scheduledTaskType.getField("name").getKID(), authData, env);
	}

	private void generateStandardActionsForAccessibleStandardTypes(AuthData authData, EnvData env) throws KommetException
	{
		for (Type type : env.getAllTypes())
		{
			// generate standard actions for standard types accessible to users
			if (!type.isBasic() || SystemTypes.isInaccessibleSystemType(type))
			{
				continue;
			}
			
			log.debug("Generating standard action for type " + type.getQualifiedName());
			Class controller = dataService.createStandardTypeController(type, authData, env);
			dataService.createStandardActionsForType(type, controller, env);
		}
	}

	public UserRecordSharingKType createUserRecordSharings(UserKType userType, EnvData env) throws KommetException
	{
		UserRecordSharingKType urSharingType = new UserRecordSharingKType(userType);
		return (UserRecordSharingKType)dataService.createType(urSharingType, true, true, false, false, false, false, dataService.getRootAuthData(env), env);
	}
	
	public GroupRecordSharingKType createGroupRecordSharings(UserGroupKType userGroupType, EnvData env) throws KommetException
	{
		GroupRecordSharingKType grSharingType = new GroupRecordSharingKType(userGroupType);
		return (GroupRecordSharingKType)dataService.createType(grSharingType, true, true, false, false, false, true, dataService.getRootAuthData(env), env);
	}

	private void createFieldHistoryType(EnvData env, KID userId) throws KommetException
	{
		FieldHistoryKType fhType = new FieldHistoryKType();
		dataService.createType(fhType, dataService.getRootAuthData(env), env);
	}

	private void createCommentType(Type userType, EnvData env, KID userId) throws KommetException
	{
		CommentKType commentType = new CommentKType(userType);
		commentType = (CommentKType)dataService.createType(commentType, dataService.getRootAuthData(env), env);
		
		// add parent field
		Field parentField = new Field();
		parentField.setApiName("parent");
		parentField.setLabel("Parent");
		
		TypeReference parentRef = new TypeReference(commentType);
		parentRef.setCascadeDelete(true);
		
		parentField.setDataType(parentRef);
		parentField.setDbColumn("parent");
		parentField.setRequired(false);
		
		commentType.addField(parentField);
		
		parentField = dataService.createField(parentField, dataService.getRootAuthData(env), env);
		
		commentType.setSharingControlledByFieldId(commentType.getField("recordId").getKID());
		dataService.updateType(commentType, dataService.getRootAuthData(env), env);
	}

	private void setDefaultSettings(EnvData env, AuthData authData) throws KommetException
	{
		systemSettingService.setSetting(SystemSettingKey.REASSIGN_DEFAULT_ACTION_ON_ACTION_DELETE, "true", RecordAccessType.SYSTEM, authData, env);
		//systemSettingService.setSetting(SystemSettingService.ASSOCIATION_COUNT, "0", userId, env);
	}

	private void createFileObjectAssignmentType(FileKType fileType, EnvData env, AuthData authData) throws KommetException
	{
		FileRecordAssignmentKType foaType = new FileRecordAssignmentKType(fileType);
		foaType.setSharingControlledByFieldId(foaType.getField("file").getKID());
		foaType = (FileRecordAssignmentKType)dataService.createType(foaType, dataService.getRootAuthData(env), env);
	}

	private UniqueCheckKType createUniqueCheckType(EnvData env) throws KIDException, KommetException
	{
		UniqueCheckKType uc = new UniqueCheckKType();
		uc = (UniqueCheckKType)dataService.createUniqueCheckType(uc, dataService.getRootAuthData(env), env);
		
		// create a unique constraint on unique check name
		UniqueCheck check = new UniqueCheck();
		check.setName("UniqueUniqueCheckName");
		check.setTypeId(uc.getKID());
		check.addField(uc.getField("name"));
		check.setIsSystem(true);
		check.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(check, true, dataService.getRootAuthData(env), env);
		
		return uc;
	}
	
	private TypeInfoKType createTypeInfoType(Type pageType, Type kollType, KID userId, EnvData env) throws KIDException, KommetException
	{
		TypeInfoKType ti = new TypeInfoKType(kollType, pageType);
		ti = (TypeInfoKType)dataService.createType(ti, dataService.getRootAuthData(env), env);
		
		// create a unique constraint on type ID
		UniqueCheck check = new UniqueCheck();
		check.setName("UniqueTypeInfoForType");
		check.setTypeId(ti.getKID());
		check.addField(ti.getField("typeId"));
		check.setIsSystem(true);
		check.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(check, true, dataService.getRootAuthData(env), env);
		
		return ti;
	}

	private Type createStandardActionType(EnvData env, Type profileObj, Type pageObj, KID userId) throws KommetException
	{
		StandardActionKType stdPageType = new StandardActionKType(profileObj, pageObj);
		stdPageType = (StandardActionKType)dataService.createType(stdPageType, env);
		
		// create a unique constraint field name and package
		UniqueCheck check = new UniqueCheck();
		check.setName("UniqueStandardActionForProfile");
		check.setTypeId(stdPageType.getKID());
		check.addField(stdPageType.getField("profile"));
		check.addField(stdPageType.getField("type"));
		check.addField(stdPageType.getField("typeId"));
		check.setIsSystem(true);
		check.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(check, true, dataService.getRootAuthData(env), env);
		
		return stdPageType;
	}

	private Type createViewType(Type layoutType, EnvData env, KID userId) throws KIDException, KommetException
	{
		ViewKType viewType = new ViewKType(layoutType);
		viewType = (ViewKType)dataService.createType(viewType, env);
		
		// create a unique constraint on view name
		UniqueCheck check = new UniqueCheck();
		check.setName("UniqueViewNameAndPackage");
		check.setTypeId(viewType.getKID());
		check.addField(viewType.getField("name"));
		check.addField(viewType.getField("packageName"));
		check.setIsSystem(true);
		check.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(check, true, dataService.getRootAuthData(env), env);
		
		return viewType;
	}
	
	private Type createLayoutType(EnvData env) throws KIDException, KommetException
	{
		LayoutKType layoutType = new LayoutKType();
		return dataService.createType(layoutType, env);
	}
	
	private FileKType createFileAndFileRevisionType (EnvData env, AuthData authData) throws KIDException, KommetException
	{
		// first create file type - for now without reference to revisions
		FileKType fileType = new FileKType();
		fileType = (FileKType)dataService.createType(fileType, env);
		
		// set name as the default field for file
		dataService.setDefaultField(fileType.getKID(), fileType.getField("name").getKID(), authData, env);
		
		// now create file revision type
		FileRevisionKType fileRevisionType = new FileRevisionKType(fileType);
		fileRevisionType.setSharingControlledByFieldId(fileRevisionType.getField("file").getKID());
		fileRevisionType = (FileRevisionKType)dataService.createType(fileRevisionType, env);
		
		// create a unique constraint on file revision
		UniqueCheck check = new UniqueCheck();
		check.setName("UniqueRevisionNumberForFile");
		check.setTypeId(fileRevisionType.getKID());
		check.addField(fileRevisionType.getField("file"));
		check.addField(fileRevisionType.getField("revisionNumber"));
		check.setIsSystem(true);
		check.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(check, true, dataService.getRootAuthData(env), env);
		
		// create a unique check for the path field
		UniqueCheck pathCheck = new UniqueCheck();
		pathCheck.setName("UniquePathForFileRevision");
		pathCheck.setTypeId(fileRevisionType.getKID());
		pathCheck.addField(fileRevisionType.getField("path"));
		pathCheck.setIsSystem(true);
		pathCheck.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(pathCheck, true, dataService.getRootAuthData(env), env);
		
		// now add field "revisions" to type file
		Field revisionsField = new Field();
		revisionsField.setApiName("revisions");
		revisionsField.setLabel("Revisions");
		revisionsField.setDataType(new InverseCollectionDataType(fileRevisionType, "file"));
		revisionsField.setDbColumn("revisions");
		revisionsField.setRequired(false);
		fileType.addField(revisionsField);
		
		dataService.createField(revisionsField, env);
		
		fileType = (FileKType)env.getType(fileType.getKID());
		
		createFileObjectAssignmentType(fileType, env, authData);
		
		return fileType;
	}
	
	private ClassKType createClassType (EnvData env, KID userId) throws KIDException, KommetException
	{
		ClassKType kollFileType = new ClassKType();
		ClassKType file = (ClassKType)dataService.createType(kollFileType, env);
		
		// create a unique constraint field name and package
		UniqueCheck check = new UniqueCheck();
		check.setName("UniqueFullFileName");
		check.setTypeId(file.getKID());
		check.addField(file.getField("name"));
		check.addField(file.getField("packageName"));
		check.setIsSystem(true);
		check.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(check, true, dataService.getRootAuthData(env), env);
		
		return file;
	}
	
	private ActionKType createActionType(EnvData env, Type keetleViewType, Type kollFileType, KID userId) throws KIDException, KommetException
	{
		ActionKType actionType = new ActionKType(keetleViewType, kollFileType);
		actionType = (ActionKType)dataService.createType(actionType, env);
		
		// create a unique constraint on field URL
		UniqueCheck check = new UniqueCheck();
		check.setName("UniqueActionURL");
		check.setTypeId(actionType.getKID());
		check.addField(actionType.getField("url"));
		check.setIsSystem(true);
		check.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(check, true, dataService.getRootAuthData(env), env);
		
		return actionType;
	}
	
	private TypeTriggerKType createTypeTriggerType(Type classType, KID userId, EnvData env) throws KIDException, KommetException
	{
		TypeTriggerKType ttType = new TypeTriggerKType(classType);
		ttType = (TypeTriggerKType)dataService.createType(ttType, env);
		
		// create a unique constraint field name and package
		UniqueCheck check = new UniqueCheck();
		check.setName("UniqueTriggerFile");
		check.setTypeId(ttType.getKID());
		check.addField(ttType.getField("triggerFile"));
		check.setIsSystem(true);
		check.setAccessType(RecordAccessType.SYSTEM_IMMUTABLE.getId());
		uniqueCheckService.save(check, true, dataService.getRootAuthData(env), env);
		
		return ttType;
	}
	
	private SystemSettingKType createSystemSettingType(EnvData env) throws KIDException, KommetException
	{
		SystemSettingKType type = new SystemSettingKType();
		return (SystemSettingKType)dataService.createType(type, env);
	}

	private void createPermissionTypes(ProfileKType profileType, PermissionSetKType permissionSetType, ActionKType actionType, EnvData env) throws KommetException
	{
		TypePermissionKType tp = new TypePermissionKType(profileType, permissionSetType); 
		tp = (TypePermissionKType)dataService.createType(tp, env);
		
		FieldPermissionKType fp = new FieldPermissionKType(profileType, permissionSetType);
		fp = (FieldPermissionKType)dataService.createType(fp, env);
		
		ActionPermissionKType pp = new ActionPermissionKType(profileType, permissionSetType, actionType);
		dataService.createType(pp, env);
	}

	public static Record getRootProfile(EnvData env) throws KommetException
	{
		Record sysAdmin = new Record((ProfileKType)env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.PROFILE_API_NAME)));
		sysAdmin.setField("name", Profile.ROOT_NAME);
		sysAdmin.setField("systemProfile", true);
		sysAdmin.setField("label", Profile.ROOT_NAME);
		sysAdmin.setField(Field.ACCESS_TYPE_FIELD_NAME, RecordAccessType.SYSTEM_IMMUTABLE.getId());
		return sysAdmin;
	}
	
	private static Record getSystemAdministratorProfile(EnvData env) throws KommetException
	{
		Record sysAdmin = new Record((ProfileKType)env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.PROFILE_API_NAME)));
		sysAdmin.setField("name", Profile.SYSTEM_ADMINISTRATOR_NAME);
		sysAdmin.setField("label", Profile.SYSTEM_ADMINISTRATOR_LABEL);
		sysAdmin.setField("systemProfile", true);
		sysAdmin.setField(Field.ACCESS_TYPE_FIELD_NAME, RecordAccessType.SYSTEM_IMMUTABLE.getId());
		return sysAdmin;
	}
	
	private static Record getUnauthenticatedProfile(EnvData env) throws KommetException
	{
		Record sysAdmin = new Record((ProfileKType)env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.PROFILE_API_NAME)));
		sysAdmin.setField("name", Profile.UNAUTHENTICATED_NAME);
		sysAdmin.setField("label", Profile.UNAUTHENTICATED_NAME);
		sysAdmin.setField("systemProfile", true);
		return sysAdmin;
	}

	/**
	 * Creates and returns (but does not insert) system groups that every environment needs to have configured.
	 * @return list of system groups (unsaved)
	 * @throws KommetException 
	 */
	private Map<String, Record> getSystemPermissionSets(EnvData env) throws KommetException
	{
		Map<String, Record> groups = new HashMap<String, Record>();
		PermissionSetKType permissionSetType = (PermissionSetKType)env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.PERMISSION_SET_API_NAME));
		
		// create super admin permission set
		Record superAdminPS = new Record(permissionSetType);
		superAdminPS.setField("name", SystemPermissionSets.SUPERADMIN);
		superAdminPS.setField("systemPermissionSet", true);
		superAdminPS.setField(Field.ACCESS_TYPE_FIELD_NAME, RecordAccessType.SYSTEM_IMMUTABLE.getId());
		groups.put(SystemPermissionSets.SUPERADMIN, superAdminPS);
		
		// create support permission set
		Record supportPS = new Record(permissionSetType);
		supportPS.setField("name", SystemPermissionSets.SUPPORT);
		supportPS.setField("systemPermissionSet", true);
		supportPS.setField(Field.ACCESS_TYPE_FIELD_NAME, RecordAccessType.SYSTEM_IMMUTABLE.getId());
		groups.put(SystemPermissionSets.SUPPORT, supportPS);
		
		return groups;
	}

	@Transactional
	public Record createRootUser(EnvData env, Record rootProfile) throws KommetException
	{
		UserKType userType = (UserKType)env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.USER_API_NAME));
		if (userType == null)
		{
			throw new BasicSetupException("Cannot create superadmin because type " + SystemTypes.USER_API_NAME + " is not registered with the environment");
		}
		
		Record rootUser = BasicSetupService.getRoot(userType, rootProfile, ROOT_USERNAME, ROOT_USER_EMAIL);
		env.setRootUser(rootUser);
		dataService.saveRootUser(rootUser, env);
		return rootUser;
	}
	
	@Transactional
	public Record createGuestUser(EnvData env, AuthData rootAuthData, Record profile) throws KommetException
	{
		UserKType userType = (UserKType)env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.USER_API_NAME));
		if (userType == null)
		{
			throw new BasicSetupException("Cannot create guest user because type " + SystemTypes.USER_API_NAME + " is not registered with the environment");
		}
		
		Record guestUser = new Record(userType);
		guestUser.setField("userName", Constants.UNAUTHENTICATED_USER_NAME);
		guestUser.setField("email", Constants.UNAUTHENTICATED_USER_NAME);
		
		// password for guest user is never used
		guestUser.setField("password", MiscUtils.getSHA1Password("stub"));
		
		guestUser.setField("profile", profile);
		guestUser.setField("timezone", "GMT");
		guestUser.setField("locale", "EN_US");
		guestUser.setField("isActive", true);
		guestUser.setField(Field.ACCESS_TYPE_FIELD_NAME, RecordAccessType.SYSTEM_IMMUTABLE.getId());
		
		dataService.save(guestUser, true, true, rootAuthData, env);
		return guestUser;
	}

	@Transactional
	public UserKType createUserType(EnvData env, ProfileKType profileType, AuthData authData) throws KommetException
	{
		// create user type
		UserKType userType = new UserKType(profileType);
		userType = (UserKType)dataService.createCoreType(userType, env);
		
		// make user name the default field for the user
		dataService.setDefaultField(userType.getKID(), userType.getField("userName").getKID(), authData, env);
		
		return userType;
	}
	
	private PermissionSetKType createPermissionSetType(EnvData env) throws KommetException
	{
		// create permission set type
		PermissionSetKType psType = new PermissionSetKType();
		return (PermissionSetKType)dataService.createType(psType, true, true, true, true, true, false, AuthData.getRootAuthData(env), env);
	}
	
	@Transactional
	public ProfileKType createProfileType(EnvData env) throws KommetException
	{
		// create user object
		ProfileKType profileType = new ProfileKType();
		profileType = (ProfileKType)dataService.createCoreType(profileType, env);
		
		// set default field
		profileType.setDefaultFieldId(profileType.getField("name").getKID());
		profileType = (ProfileKType)dataService.updateCoreType(profileType, env);
		
		return profileType;
	}
	
	private class CoreSetup
	{
		private ProfileKType profileType;
		private UserKType userType;
		private UniqueCheckKType uniqueCheckType;
		private Record rootUser;
		private Record sysAdminProfile;
		
		public CoreSetup(ProfileKType profileObj, UserKType userObj, UniqueCheckKType ucType, Record rootUser, Record sysAdminProfile)
		{
			this.profileType = profileObj;
			this.userType = userObj;
			this.rootUser = rootUser;
			this.uniqueCheckType = ucType;
			this.sysAdminProfile = sysAdminProfile;
		}

		public AuthData getRootUserAuthData() throws KommetException
		{
			AuthData authData = new AuthData();
			User u = new User();
			u.setId(getRootUser().getKID());
			authData.setUser(u);
			return authData;
		}

		public ProfileKType getProfileType()
		{
			return profileType;
		}

		public UserKType getUserType()
		{
			return userType;
		}

		public Record getRootUser()
		{
			return rootUser;
		}

		public Record getSysAdminProfile()
		{
			return sysAdminProfile;
		}

		public UniqueCheckKType getUniqueCheckType()
		{
			return uniqueCheckType;
		}
	}
	
	public AuthData getRootAuthData(EnvData env) throws KommetException
	{
		if (this.rootAuthData == null)
		{	
			User root = new User();
			root.setEmail(BasicSetupService.ROOT_USER_EMAIL);
			root.setUserName(BasicSetupService.ROOT_USERNAME);
			root.setId(AppConfig.getRootUserId());
			root.setLocale(Locale.EN_US.name());
			root.setIsActive(true);
			root.setTimezone("CET");
			
			Profile rootProfile = new Profile();
			rootProfile.setName(Profile.ROOT_NAME);
			rootProfile.setId(KID.get(Profile.ROOT_ID));
			root.setProfile(rootProfile);
			
			this.rootAuthData = new RootAuthData();
			this.rootAuthData.setUser(root);
			this.rootAuthData.setProfile(root.getProfile());
			this.rootAuthData.setEnvId(env.getId());
			
			/*if (permissionService != null)
			{
				this.rootAuthData.setPermissionService(permissionService);
			}*/
		}
		
		return this.rootAuthData;
	}
}