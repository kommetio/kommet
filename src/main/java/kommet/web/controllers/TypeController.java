/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.auth.PermissionService;
import kommet.auth.ProfileService;
import kommet.auth.UserService;
import kommet.basic.Action;
import kommet.basic.Profile;
import kommet.basic.SettingValue;
import kommet.basic.StandardAction;
import kommet.basic.TypeInfo;
import kommet.basic.TypePermission;
import kommet.basic.actions.ActionFilter;
import kommet.basic.actions.ActionService;
import kommet.basic.keetle.ViewService;
import kommet.basic.keetle.tags.breadcrumbs.Breadcrumbs;
import kommet.basic.types.SystemTypes;
import kommet.config.UserSettingKeys;
import kommet.dao.SettingValueFilter;
import kommet.dao.dal.DALSyntaxException;
import kommet.dao.queries.Criteria;
import kommet.data.DataAccessUtil;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KeyPrefixException;
import kommet.data.LimitExceededException;
import kommet.data.OperationResult;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.TypeDefinitionException;
import kommet.data.TypeInfoService;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.errorlog.ErrorLogService;
import kommet.errorlog.ErrorLogSeverity;
import kommet.i18n.InternationalizationService;
import kommet.json.JSON;
import kommet.json.TypeJSONUtil;
import kommet.rest.RestUtil;
import kommet.security.RestrictedAccess;
import kommet.uch.UserCascadeHierarchyContext;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.NestedContextField;
import kommet.utils.UrlUtil;
import kommet.utils.ValidationUtil;

@Controller
public class TypeController extends CommonKommetController
{	
	@Inject
	EnvService envService;
	
	@Inject
	DataService dataService;
	
	@Inject
	ViewService viewService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	ActionService actionService;
	
	@Inject
	TypeInfoService typeInfoService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	ErrorLogService logService;
	
	@Inject
	UserService userService;
	
	@Inject
	InternationalizationService i18n;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	@Inject
	PermissionService permissionService;
	
	private Type type;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/types/edit/{keyPrefix}", method = RequestMethod.GET)
	public ModelAndView edit(@PathVariable("keyPrefix") String keyPrefixStr, HttpSession session) throws KommetException
	{
		KeyPrefix keyPrefix = null;
		try
		{
			keyPrefix = KeyPrefix.get(keyPrefixStr);
		}
		catch (KeyPrefixException e)
		{
			e.printStackTrace();
			return getErrorPage("Invalid object prefix " + keyPrefixStr);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		this.type = env.getType(keyPrefix);
		if (this.type == null)
		{
			return getErrorPage("No object found with key prefix " + keyPrefixStr);
		}
		
		ModelAndView mv = new ModelAndView("types/edit");
		mv.addObject("type", this.type);
		mv.addObject("title", "Edit " + type.getLabel());
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_CREATE_FIELD_URL, method = RequestMethod.POST)
	@ResponseBody
	public void createField(@RequestParam("type") String typeName, @RequestParam("apiName") String fieldApiName, @RequestParam("label") String fieldLabel, HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		Type type = env.getType(typeName);
		
		Field newField = new Field();
		newField.setApiName(fieldApiName);
		newField.setLabel(fieldLabel);
		newField.setRequired(false);
		type.addField(newField);
		newField.setDataType(new TextDataType(1000000));
		
		newField = dataService.createField(newField, authData, env);
		
		out.write(RestUtil.getRestSuccessDataResponse("{ \"fieldId\": \"" + newField.getKID() + "\" }"));
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/grid/{keyPrefix}", method = RequestMethod.GET)
	public ModelAndView grid(@PathVariable("keyPrefix") String keyPrefix, HttpSession session, HttpServletRequest req) throws KommetException
	{
		ModelAndView mv = new ModelAndView("types/grid");
		
		KeyPrefix typePrefix = null;
		try
		{
			typePrefix = KeyPrefix.get(keyPrefix);
		}
		catch (KeyPrefixException e)
		{
			return getErrorPage("Invalid type prefix " + keyPrefix);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		
		this.type = env.getType(typePrefix);
		if (this.type == null)
		{
			return getErrorPage("No type found with prefix " + typePrefix);
		}
		
		mv.addObject("type", type);
		
		List<NestedContextField> fields = DataAccessUtil.getReadableFields(type, AuthUtil.getAuthData(session), env);
		List<String> fieldNames = new ArrayList<String>();
		for (NestedContextField field : fields)
		{
			fieldNames.add(field.getNestedName());
		}
		
		mv.addObject("fieldList", MiscUtils.implode(fieldNames, ", "));
		
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/type/{keyPrefixOrId}", method = RequestMethod.GET)
	public ModelAndView details(@PathVariable("keyPrefixOrId") String keyPrefixOrId, HttpSession session, HttpServletRequest req) throws KommetException
	{	
		KeyPrefix typePrefix = null;
		EnvData env = envService.getCurrentEnv(session);
		
		if (keyPrefixOrId.length() == 13)
		{
			KID typeId = KID.get(keyPrefixOrId);
			typePrefix = env.getType(typeId).getKeyPrefix();
		}
		else
		{
			try
			{
				typePrefix = KeyPrefix.get(keyPrefixOrId);
			}
			catch (KeyPrefixException e)
			{
				return getErrorPage("Invalid type prefix " + keyPrefixOrId);
			}
		}
		
		this.type = env.getType(typePrefix);
		if (this.type == null)
		{
			return getErrorPage("No type found with prefix " + typePrefix);
		}
		
		ModelAndView mv = new ModelAndView("types/details");
		mv.addObject("obj", this.type);
		
		// separate custom and built-in fields
		List<Field> customFields = new ArrayList<Field>();
		List<Field> systemFields = new ArrayList<Field>();
		
		for (Field field : this.type.getFields())
		{
			if (Field.isSystemField(field.getApiName()))
			{
				// do not show the access type field, because users should not use it directly at all
				if (!Field.ACCESS_TYPE_FIELD_NAME.equals(field.getApiName()))
				{
					systemFields.add(field);
				}
			}
			else
			{
				customFields.add(field);
			}
		}
		
		AuthData authData = AuthUtil.getAuthData(session);
		
		// get standard pages
		List<StandardAction> stdPages = actionService.getStandardActionsForType(this.type.getKID(), env);
		mv.addObject("stdPages", stdPages);
		mv.addObject("customFields", customFields);
		mv.addObject("systemFields", systemFields);
		mv.addObject("defaultField", type.getDefaultFieldId() != null ? type.getField(type.getDefaultFieldId()).getLabel() : Field.ID_FIELD_LABEL);
		mv.addObject("sharingControlledByField", type.getSharingControlledByFieldId() != null ? type.getField(type.getSharingControlledByFieldId()).getLabel() : null);
		mv.addObject("typePackage", this.type.getPackage());
		mv.addObject("canEdit", !this.type.isDeclaredInCode() && !this.type.isBasic() && (AuthUtil.isSysAdmin(authData) || authData.canEditType(this.type.getKID(), false, env)));
		mv.addObject("canAddFields", !this.type.isDeclaredInCode() && (!this.type.isBasic() || !SystemTypes.isInaccessibleSystemType(this.type)) && (AuthUtil.isSysAdmin(authData) || authData.canEditType(this.type.getKID(), false, env)));
		mv.addObject("canEditPermissions", AuthUtil.isSysAdmin(authData) || authData.canEditType(this.type.getKID(), false, env));
		mv.addObject("canDelete", !this.type.isDeclaredInCode() && !this.type.isBasic() && (AuthUtil.isSysAdmin(authData) || authData.canDeleteType(this.type.getKID(), false, env)));
		mv.addObject("canForceDelete", AuthUtil.isRoot(authData) && !this.type.isBasic() && authData.canDeleteType(this.type.getKID(), false, env));
		mv.addObject("defaultListViewId", uchService.getUserSettingAsKID(UserSettingKeys.KM_SYS_DEFAULT_TYPE_LIST_VIEW + "." + this.type.getKID(), authData, AuthData.getRootAuthData(env), env));
		mv.addObject("defaultEditViewId", uchService.getUserSettingAsKID(UserSettingKeys.KM_SYS_DEFAULT_TYPE_EDIT_VIEW + "." + this.type.getKID(), authData, AuthData.getRootAuthData(env), env));
		mv.addObject("defaultCreateViewId", uchService.getUserSettingAsKID(UserSettingKeys.KM_SYS_DEFAULT_TYPE_CREATE_VIEW + "." + this.type.getKID(), authData, AuthData.getRootAuthData(env), env));
		mv.addObject("defaultDetailsViewId", uchService.getUserSettingAsKID(UserSettingKeys.KM_SYS_DEFAULT_TYPE_DETAILS_VIEW + "." + this.type.getKID(), authData, AuthData.getRootAuthData(env), env));
		
		/*
		 * mv.addObject("canEdit", !this.type.isBasic() && (authData.canEditType(this.type.getKID(), false, env) || AuthUtil.isSysAdminOrRoot(authData)));
		mv.addObject("canEditPermissions", (authData.canEditType(this.type.getKID(), false, env) || AuthUtil.isSysAdminOrRoot(authData)));
		mv.addObject("canDelete", !this.type.isBasic() && (authData.canDeleteType(this.type.getKID(), false, env) || AuthUtil.isSysAdminOrRoot(authData)));
		 */
		
		// add breadcrumbs
		Breadcrumbs.add(req.getRequestURL().toString(), type.getLabel(), appConfig.getBreadcrumbMax(), session, getContextPath(session));
		
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/datamodeldiagram", method = RequestMethod.GET)
	public ModelAndView dataDiagram(HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("types/datadiagram");
		
		EnvData env = envService.getCurrentEnv(session);
		
		List<Type> typesForDiagram = new ArrayList<Type>();
		for (Type type : dataService.getTypes(null, true, true, env))
		{
			if (type.isAccessible())
			{
				typesForDiagram.add(type);
			}
		}
		
		mv.addObject("typeBindings", getTypeBindings(env));
		mv.addObject("types", JSON.serialize(typesForDiagram, AuthUtil.getAuthData(session)));
		return mv;
	}
	
	private String getTypeBindings(EnvData env) throws KommetException
	{
		List<String> bindings = new ArrayList<String>();
		
		for (Field field : dataService.getFields(null, env))
		{
			// note that we only check for type reference and association bindings, because other inverse collection is the same as type reference, only the other way around
			
			if (field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
			{
				bindings.add("{ field: \"" + field.getApiName() + "\", firstTypeId: \"" + field.getType().getKID() + "\", secondTypeId: \"" + ((TypeReference)field.getDataType()).getTypeId() + "\", type: \"type_reference\", \"desc\": \"Reference field:" + field.getApiName() + "\" }");
			}
			else if (field.getDataTypeId().equals(DataType.ASSOCIATION))
			{
				bindings.add("{ field: \"" + field.getApiName() + "\", firstTypeId: \"" + field.getType().getKID() + "\", secondTypeId: \"" + ((AssociationDataType)field.getDataType()).getAssociatedTypeId() + "\", type: \"association\", \"desc\": \"Association:" + field.getApiName() + "\" }");
			}
		}
		
		return "[ " + MiscUtils.implode(bindings, ", ") + " ]";
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/typefromquery", method = RequestMethod.GET)
	@ResponseBody
	public void getTypeFromQuery(@RequestParam(value = "query", required = false) String query, HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		try
		{
			Criteria c = env.getSelectCriteriaFromDAL(query);
			out.write(RestUtil.getRestSuccessDataResponse("{ \"type\": " + JSON.serialize(c.getType(), authData) + ", \"isValidQuery\": true }"));
		}
		catch (DALSyntaxException e)
		{
			// in case of any error (e.g. incorrect query) just return a null type
			out.write(RestUtil.getRestSuccessDataResponse("{ \"type\": null, \"isValidQuery\": false }"));
		}
		catch (Exception e)
		{
			// in case of any error (e.g. incorrect query) just return a null type
			out.write(RestUtil.getRestSuccessDataResponse("{ \"type\": null }"));
		}
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/types/new", method = RequestMethod.GET)
	public ModelAndView newType() throws KommetException
	{
		ModelAndView mv = new ModelAndView("types/edit");
		mv.addObject("title", "New object");
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/customtypes", method = RequestMethod.GET)
	@ResponseBody
	public void getTypeInfo (HttpSession session, HttpServletResponse response) throws IOException, KommetException
	{
		PrintWriter out = response.getWriter();
		EnvData env = envService.getCurrentEnv(session);
		
		List<String> serializedTypes = new ArrayList<String>();
		
		for (Type type : env.getCustomTypes())
		{
			serializedTypes.add(TypeJSONUtil.serializeType(type));
		}
		
		out.write("[ " + MiscUtils.implode(serializedTypes, ", ") + " ]");
	}

	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/type/delete", method = RequestMethod.POST)
	@ResponseBody
	public void deleteType (@RequestParam("typeId") String id, @RequestParam("forceCleanup") Boolean forceCleanup, HttpSession session, HttpServletResponse response) throws KIDException, IOException
	{
		KID typeId = KID.get(id);
		EnvData env;
		PrintWriter out = response.getWriter();
		try
		{
			env = envService.getCurrentEnv(session);
			OperationResult result = dataService.deleteType(env.getType(typeId), true, Boolean.TRUE.equals(forceCleanup), AuthUtil.getAuthData(session), env);
			if (!result.isResult())
			{
				out.write(getErrorJSON(result.getMessage()));
			}
			else
			{
				out.write(getSuccessJSON("Type deleted"));
			}
			return;
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
			out.write(getErrorJSON("Error deleting type"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			out.write(getErrorJSON("Error deleting type: " + e.getMessage()));
		}
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/savedefaulttypeview", method = RequestMethod.POST)
	@ResponseBody
	public void saveDefaultTypeView (@RequestParam(value = "viewId", required = false) String sViewid, @RequestParam("settingKey") String settingKey, @RequestParam("typeId") String sTypeId, HttpSession session, HttpServletResponse response) throws IOException, KommetException
	{
		KID typeId = KID.get(sTypeId);
		EnvData env;
		PrintWriter out = response.getWriter();
		KID viewId = StringUtils.hasText(sViewid) ? KID.get(sViewid) : null;
		
		try
		{
			env = envService.getCurrentEnv(session);
			
			SettingValueFilter filter = new SettingValueFilter();
			filter.addKey(settingKey + "." + typeId);
			List<SettingValue> settings = uchService.getSettings(filter, AuthData.getRootAuthData(env), AuthData.getRootAuthData(env), env);
			
			if (!settings.isEmpty())
			{
				if (viewId != null)
				{
					SettingValue setting = settings.get(0);
					setting.setValue(viewId.getId());
					uchService.saveSetting(setting, setting.getHierarchy().getActiveContext(), setting.getHierarchy().getActiveContextValue(), AuthData.getRootAuthData(env), env);
				}
				else
				{
					uchService.deleteSetting(settings.get(0), AuthData.getRootAuthData(env), env);
				}
			}
			else
			{
				if (viewId != null)
				{
					uchService.saveUserSetting(settingKey + "." + typeId, viewId.getId(), UserCascadeHierarchyContext.ENVIRONMENT, true, AuthData.getRootAuthData(env), env);
				}
			}
			
			out.write(RestUtil.getRestSuccessResponse("Default view saved"));
			
			return;
		}
		catch (NullPointerException e)
		{
			//logService.logException(e, ErrorLogSeverity.FATAL, this.getClass().getName(), -1, AuthUtil.getAuthData(session).getUserId(), env);
			out.write(RestUtil.getRestErrorResponse("Error saving default view for type"));
		}
		catch (Exception e)
		{
			out.write(RestUtil.getRestErrorResponse("Error saving default view for type: " + e.getMessage()));
		}
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/type/save", method = RequestMethod.POST)
	public ModelAndView save (@RequestParam(value = "kObjectId", required = false) String id,
									@RequestParam(value = "apiName", required = false) String apiName,
									@RequestParam(value = "packageName", required = false) String packageName,
									@RequestParam(value = "label", required = false) String label,
									@RequestParam(value = "description", required = false) String description,
									@RequestParam(value = "defaultField", required = false) String sDefaultField,
									@RequestParam(value = "sharingControlledByField", required = false) String sSharingControlledByField,
									@RequestParam(value = "combineRecordAndCascadeSharing", required = false) String sCombineRecordAndCascadeSharing,
									@RequestParam(value = "pluralLabel", required = false) String pluralLabel,
									@RequestParam(value = "labelKey", required = false) String labelKey,
									@RequestParam(value = "pluralLabelKey", required = false) String pluralLabelKey,
									HttpSession session)
									throws KommetException
	{
		clearMessages();
		
		KID typeId = StringUtils.hasText(id) ? KID.get(id) : null;
		
		ModelAndView mv = new ModelAndView("types/edit");
		
		if (!StringUtils.hasText(label))
		{
			addError("Label cannot be empty");
		}
		if (!StringUtils.hasText(pluralLabel))
		{
			addError("Plural label cannot be empty");
		}
		if (!StringUtils.hasText(packageName))
		{
			addError("Please fill in package name");
		}
		else if (!ValidationUtil.isValidPackageName(packageName))
		{
			addError("Invalid package name. Package name must contain only lower case letters and dots.");
		}
		
		if (!StringUtils.hasText(apiName))
		{
			addError("API name cannot be empty");
		}
		else
		{
			if (!ValidationUtil.isValidTypeApiName(apiName))
			{
				addError("API name is not valid. Valid type name can contain only letters, digits and an underscore, and must start with a capital letter.");
			}
		}
		
		KID defaultFieldId = null;
		KID sharingControlledByFieldId = null;
		
		if (typeId != null)
		{
			defaultFieldId = type.getField(Field.ID_FIELD_NAME).getKID();
			
			if (!StringUtils.hasText(sDefaultField) && typeId != null)
			{
				addError("Default field cannot be empty");
			}
			else
			{
				try
				{
					defaultFieldId = KID.get(sDefaultField);
				}
				catch (KIDException e)
				{
					addError("Incorrect value for default field: " + e.getMessage());
				}
				
				Field defaultField = type.getField(defaultFieldId);
				
				if (!DataService.isValidDefaultField(defaultField, type))
				{
					if (!defaultField.getDataTypeId().equals(DataType.FORMULA))
					{
						addError("Field " + type.getField(defaultFieldId).getApiName() + " cannot be set as default because it is not a required field");
					}
					else
					{
						addError("Formula field " + type.getField(defaultFieldId).getApiName() + " cannot be set as default because not all fields used in it are required");
					}
				}
				
				if (StringUtils.hasText(sSharingControlledByField))
				{
					// set sharingControlledByField
					try
					{
						sharingControlledByFieldId = KID.get(sSharingControlledByField);
						
						try
						{
							DataService.validateSharingControlledByField(sharingControlledByFieldId, type);
						}
						catch (TypeDefinitionException e)
						{
							addError(e.getMessage());
						}
					}
					catch (KIDException e)
					{
						addError("Incorrect value for sharing controlled by field: " + e.getMessage());
					}
				}
			}
		}
		
		EnvData env = envService.getCurrentEnv(session);
		
		if (hasErrorMessages())
		{
			if (typeId == null)
			{
				type = new Type();
			}
			else
			{
				type = env.getType(typeId);
			}
			
			type.setApiName(apiName);
			type.setLabel(label);
			type.setPluralLabel(pluralLabel);
			type.setPackage(packageName);
			type.setDefaultFieldId(defaultFieldId);
			type.setDescription(description);
			type.setSharingControlledByFieldId(sharingControlledByFieldId);
			type.setUchPluralLabel(pluralLabelKey);
			type.setUchLabel(labelKey);
			
			mv.addObject("errorMsgs", getErrorMsgs());
			mv.addObject("type", this.type);
			mv.addObject("title", StringUtils.hasText(type.getApiName()) ? type.getApiName() : "Editing type");
			return mv;
		}
		
		// save the type
		if (typeId == null)
		{
			Type type = new Type();
			type.setApiName(apiName.trim());
			type.setLabel(label.trim());
			type.setPluralLabel(pluralLabel.trim());
			type.setCreated(new Date());
			type.setDefaultFieldId(defaultFieldId);
			type.setSharingControlledByFieldId(sharingControlledByFieldId);
			type.setCombineRecordAndCascadeSharing("true".equals(sCombineRecordAndCascadeSharing));
			type.setPackage(packageName.trim());
			type.setUchPluralLabel(StringUtils.hasText(pluralLabelKey) ? pluralLabelKey : null);
			type.setUchLabel(StringUtils.hasText(labelKey) ? labelKey : null);
			
			try
			{
				AuthData authData = AuthUtil.getAuthData(session);
				
				type = dataService.createType(type, authData, env);
				
				// mark type permissions for update, so that user reinitialize their permissions and they can be applied immediately
				env.setLastTypePermissionsUpdate((new Date()).getTime());
				
				AuthUtil.prepareAuthData(authData.getUser(), session, false, userService, i18n, uchService, appConfig, env);
				
				return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/type/" + type.getKeyPrefix());
			}
			catch (LimitExceededException e)
			{
				addError(e.getMessage());
			}
			catch (KommetException e)
			{
				addError(e.getMessage());
			}
		}
		else
		{
			Type existingType = env.getType(typeId);
			existingType.setApiName(apiName.trim());
			existingType.setLabel(label.trim());
			existingType.setPluralLabel(pluralLabel.trim());
			existingType.setDefaultFieldId(defaultFieldId);
			existingType.setSharingControlledByFieldId(sharingControlledByFieldId);
			existingType.setCombineRecordAndCascadeSharing("true".equals(sCombineRecordAndCascadeSharing));	
			existingType.setPackage(packageName);
			existingType.setUchPluralLabel(StringUtils.hasText(pluralLabelKey) ? pluralLabelKey : null);
			existingType.setUchLabel(StringUtils.hasText(labelKey) ? labelKey : null);
			
			try
			{
				existingType = dataService.updateType(existingType, AuthUtil.getAuthData(session), env);
				return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/type/" + existingType.getKeyPrefix());
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				addError(e.getMessage());
			}
		}
		
		// if this point has been reached, there must have been some error messages
		if (typeId == null)
		{
			type = new Type();
		}
		else
		{
			type = env.getType(typeId);
		}
		
		type.setApiName(apiName);
		type.setLabel(label);
		type.setPluralLabel(pluralLabel);
		type.setPackage(packageName);
		type.setDefaultFieldId(defaultFieldId);
		type.setDescription(description);
		type.setSharingControlledByFieldId(sharingControlledByFieldId);
		
		mv.addObject("errorMsgs", getErrorMsgs());
		mv.addObject("type", this.type);
		mv.addObject("title", "Edit " + type.getLabel());
		
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/types/list", method = RequestMethod.GET)
	public ModelAndView typeList(HttpSession session, HttpServletRequest req) throws KommetException
	{ 	
		ModelAndView mv = new ModelAndView("types/list");
		
		// TODO consider reading in object from the DB anew here, to keep it synced
		EnvData env = envService.getCurrentEnv(session);
		
		List<Type> types = new ArrayList<Type>();
		types.addAll(env.getAccessibleTypes());
		
		// sort by name
		Collections.sort(types, new TypeComparator());
		
		List<Type> basicTypes = new ArrayList<Type>();
		List<Type> customTypes = new ArrayList<Type>();
		
		for (Type type : types)
		{
			if (type.isAutoLinkingType())
			{
				continue;
			}
			
			if (type.isBasic())
			{
				basicTypes.add(type);
			}
			else
			{
				customTypes.add(type);
			}
		}
		
		mv.addObject("basicTypes", basicTypes);
		mv.addObject("customTypes", customTypes);
		
		// add breadcrumbs
		Breadcrumbs.add(req.getRequestURL().toString(), "Objects", appConfig.getBreadcrumbMax(), session, getContextPath(session));
		
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/types/views/{keyPrefix}", method = RequestMethod.GET)
	public ModelAndView typeViews(@PathVariable("keyPrefix") String keyPrefix, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("types/views");
		
		KeyPrefix prefix = KeyPrefix.get(keyPrefix);
		
		EnvData env = envService.getCurrentEnv(session);
		Type type = env.getType(prefix);
		
		mv.addObject("type", type);
		
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/typepermissions/{keyPrefix}", method = RequestMethod.GET)
	public ModelAndView typePermissions(@PathVariable("keyPrefix") String keyPrefix, @RequestParam(name = "saved", required = false) String isAfterSave, HttpSession session) throws KommetException
	{
		KeyPrefix prefix = KeyPrefix.get(keyPrefix);
		
		EnvData env = envService.getCurrentEnv(session);
		Type type = env.getType(prefix);
		
		// find permissions for the current type and all profiles except root and system administrator
		List<Record> permissions = env.getSelectCriteriaFromDAL("select id, read, edit, create, delete, readAll, editAll, deleteAll, profile.id, profile.name, profile.label, profile.systemProfile, permissionSet.id, typeId from " + SystemTypes.TYPE_PERMISSION_API_NAME + " where typeId = '" + type.getKID().getId() + "' AND profile.id <> '" + Profile.SYSTEM_ADMINISTRATOR_ID + "' AND profile.id <> '" + Profile.ROOT_ID + "'").list();
		Map<KID, TypePermission> typePermissionsByProfile = new HashMap<KID, TypePermission>();
		for (Record permission : permissions)
		{
			KID profileId = (KID)permission.getField("profile.id"); 
			if (profileId.equals(KID.get(Profile.SYSTEM_ADMINISTRATOR_ID)) || profileId.equals(KID.get(Profile.ROOT_ID)))
			{
				continue;
			}
						
			TypePermission typePerm = new TypePermission(permission, env);
			Profile profile = new Profile(null, env);
			profile.setId(profileId);
			profile.setName((String)permission.getField("profile.name"));
			profile.setLabel((String)permission.getField("profile.label"));
			typePerm.setProfile(profile);
			typePermissionsByProfile.put((KID)permission.getField("profile.id"), typePerm);
		}
		
		// for profiles for which we have no access, add empty access
		List<Profile> allProfiles = profileService.getProfiles(env);
		for (Profile profile : allProfiles)
		{
			// do not display permissions for system administrator and root
			// but do display for unauthenticated
			if (profile.getId().equals(KID.get(Profile.SYSTEM_ADMINISTRATOR_ID)) || profile.getId().equals(KID.get(Profile.ROOT_ID)))
			{
				continue;
			}
			
			if (!typePermissionsByProfile.containsKey(profile.getId()))
			{
				TypePermission permission = new TypePermission(null, env);
				permission.setDelete(false);
				permission.setRead(false);
				permission.setEdit(false);
				permission.setCreate(false);
				permission.setReadAll(false);
				permission.setEditAll(false);
				permission.setDeleteAll(false);
				permission.setTypeId(type.getKID());
				permission.setProfile(profile);
				typePermissionsByProfile.put(profile.getId(), permission);
			}
		}
		
		ModelAndView mv = new ModelAndView("types/typepermissions");
		mv.addObject("permissions", typePermissionsByProfile.values());
		mv.addObject("object", type);
		mv.addObject("afterSave", "1".equals(isAfterSave));
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/typepermissions/save", method = RequestMethod.POST)
	public ModelAndView saveTypePermissions(@RequestParam("objectId") String typeId, @RequestParam(name = "applyForFields", required = false) String applyForFields, HttpSession session, HttpServletRequest req) throws KommetException
	{
		KID objId = null;
		try
		{
			objId = KID.get(typeId);
		}
		catch (KIDException e)
		{
			e.printStackTrace();
			return getErrorPage("Invalid type id " + typeId);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		Type type = env.getType(objId);
		
		// find permissions for this type and all profiles except root and system administrator, because permission for these two profiles cannot be modified
		List<Record> permissions = env.getSelectCriteriaFromDAL("select id, read, edit, delete, create, readAll, editAll, profile.id, profile.name, permissionSet.id, typeId from " + SystemTypes.TYPE_PERMISSION_API_NAME + " where typeId = '" + type.getKID().getId() + "' AND profile.id <> '" + Profile.SYSTEM_ADMINISTRATOR_ID + "' AND profile.id <> '" + Profile.ROOT_ID + "'").list();
		Map<KID, TypePermission> typePermissionsByProfile = new HashMap<KID, TypePermission>();
		for (Record permission : permissions)
		{
			// Since checkbox values for granted permission access are only passed when the checkbox is
			// selected, unselected checkboxes are not passed at all and we would not know
			// that the given permission should be revoked.
			// This is why at the beginning we set all permissions to false.
			permission.setField("read", false);
			permission.setField("edit", false);
			permission.setField("delete", false);
			permission.setField("create", false);
			permission.setField("readAll", false);
			permission.setField("editAll", false);
			permission.setField("deleteAll", false);
			typePermissionsByProfile.put((KID)permission.getField("profile.id"), new TypePermission(permission, env));
		}
		
		Type typePermissionType = env.getType(KeyPrefix.get(KID.TYPE_PERMISSION_PREFIX));
		
		Enumeration<String> paramNames = req.getParameterNames();
		while (paramNames.hasMoreElements())
		{
			String param = paramNames.nextElement();
			if (param.startsWith("perm_"))
			{
				String[] val = req.getParameterValues(param);
				
				if (val != null && val.length > 0)
				{
					String fieldName = param.replaceFirst("perm_", ""); 
					if (val.length > 1)
					{
						throw new KommetException("Field " + fieldName + " appears more than one on the page");
					}
					
					String[] typePermissionIndex = fieldName.split("_");
					KID profileId = KID.get(typePermissionIndex[0]);
					
					// check if the type permission for this type and profile already exists
					TypePermission permission = typePermissionsByProfile.get(profileId);
					Record permissionRec = permission != null ? permission.getRecord() : null;
					if (permission == null || permissionRec == null)
					{
						permissionRec = new Record(typePermissionType);
						permissionRec.setField("profile.id", profileId, env);
						permissionRec.setField("typeId", objId);
						permissionRec.setField("read", false);
						permissionRec.setField("edit", false);
						permissionRec.setField("delete", false);
						permissionRec.setField("create", false);
						permissionRec.setField("readAll", false);
						permissionRec.setField("editAll", false);
						permissionRec.setField("deleteAll", false);
					}
					
					String accessType = typePermissionIndex[1];
					if ("read".equals(accessType))
					{
						permissionRec.setField("read", Boolean.valueOf(val[0]));
					}
					else if ("edit".equals(accessType))
					{
						permissionRec.setField("edit", Boolean.valueOf(val[0]));
					}
					else if ("delete".equals(accessType))
					{
						permissionRec.setField("delete", Boolean.valueOf(val[0]));
					}
					else if ("create".equals(accessType))
					{
						permissionRec.setField("create", Boolean.valueOf(val[0]));
					}
					else if ("readAll".equals(accessType))
					{
						permissionRec.setField("readAll", Boolean.valueOf(val[0]));
					}
					else if ("editAll".equals(accessType))
					{
						permissionRec.setField("editAll", Boolean.valueOf(val[0]));
					}
					else if ("deleteAll".equals(accessType))
					{
						permissionRec.setField("deleteAll", Boolean.valueOf(val[0]));
					}
					else
					{
						throw new KommetException("Unknown access type '" + accessType + "'");
					}
					
					typePermissionsByProfile.put(profileId, new TypePermission(permissionRec, env));
				}
			}
		}
		
		AuthData authData = AuthUtil.getAuthData(session);
		
		// save updated type permissions
		for (TypePermission permission : typePermissionsByProfile.values())
		{
			dataService.save(permission.getRecord(), authData, env);
		}
		
		if ("true".equals(applyForFields))
		{
			// apply the same permissions for all fields of this type
			for (KID profileID : typePermissionsByProfile.keySet())
			{
				TypePermission profilePermission = typePermissionsByProfile.get(profileID);
				
				for (Field field : type.getFields())
				{
					if (!Field.isSystemField(field.getApiName()))
					{
						permissionService.setFieldPermissionForProfile(profileID, field.getKID(), profilePermission.getRead(), profilePermission.getEdit(), authData, env);
					}
				}
			}
		}
		
		env.setLastTypePermissionsUpdate((new Date()).getTime());
		
		return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/typepermissions/" + type.getKeyPrefix() + "?saved=1");
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/types/relatedpages/{keyPrefix}", method = RequestMethod.GET)
	public ModelAndView listStandardActions (@PathVariable("keyPrefix") String keyPrefix, HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		
		TypeInfo typeInfo = typeInfoService.getForType(env.getType(KeyPrefix.get(keyPrefix)).getKID(), env);
		
		// type info should always be created with the type, but there have been some situations when as a result of an error it was not
		if (typeInfo == null)
		{
			AuthData authData = AuthUtil.getAuthData(session);
			logService.log("No type info exists for type " + keyPrefix, ErrorLogSeverity.FATAL, this.getClass().getName(), -1, authData.getUserId(), AuthData.getRootAuthData(env), env);
			
			// return error page
			// TODO return an error page with blank layout
			return getErrorPage("No type info exists for the type. This is a fatal error that makes it impossible to use the type. System administrator has been notified.", true);
		}
		
		ActionFilter filter = new ActionFilter();
		filter.setControllerId(typeInfo.getStandardController().getId());
		filter.setIsSystem(false);
		List<Action> pagesUsingStandardController = actionService.getActions(filter, env);
		
		ModelAndView mv = new ModelAndView("types/relatedpages");
		mv.addObject("pages", pagesUsingStandardController);
		return mv;
	}
	
	public class TypeComparator implements Comparator<Type>
	{
	    @Override
	    public int compare(Type o1, Type o2)
	    {
	        return o1.getLabel().compareTo(o2.getLabel());
	    }
	}
}