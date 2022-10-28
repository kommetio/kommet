/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.uch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.CustomTypeRecordProxy;
import kommet.basic.Layout;
import kommet.basic.RecordProxy;
import kommet.basic.RecordProxyUtil;
import kommet.basic.SettingValue;
import kommet.basic.UserCascadeHierarchy;
import kommet.basic.keetle.LayoutDao;
import kommet.config.UserSettingKeys;
import kommet.dao.SettingValueDao;
import kommet.dao.SettingValueFilter;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KeyPrefixException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.koll.compiler.KommetCompiler;
import kommet.utils.MiscUtils;

@Service
public class UserCascadeHierarchyService
{
	@Inject
	UserCascadeHierarchyDao dao;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	DataService dataService;
	
	@Inject
	SettingValueDao settingValueDao;
	
	@Inject
	LayoutDao layoutDao;
	
	public static final String SETTING_EXISTS_ERROR = "Setting with the given key and context already exists";
	
	@Transactional(readOnly = true)
	public List<UserCascadeHierarchy> get (UserCascadeHierarchyFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		return dao.get(filter, authData, env);
	}
	
	// TODO write unit tests for this method
	@Transactional(readOnly = true)
	public boolean getUserSettingAsBoolean (String key, AuthData ctxAuthData, AuthData authData, EnvData env) throws KeyPrefixException, KommetException
	{
		return "true".equals(getSettingValue(env.getType(KeyPrefix.get(KID.SETTING_VALUE_PREFIX)), "value", "key", key, ctxAuthData, authData, env));
	}
	
	@Transactional(readOnly = true)
	public Integer getUserSettingAsInt (String key, AuthData ctxAuthData, AuthData authData, EnvData env) throws KeyPrefixException, KommetException
	{
		String sValue = (String)getSettingValue(env.getType(KeyPrefix.get(KID.SETTING_VALUE_PREFIX)), "value", "key", key, ctxAuthData, authData, env);
		return StringUtils.hasText(sValue) ? Integer.parseInt(sValue) : null;
	}
	
	@Transactional(readOnly = true)
	public KID getUserSettingAsKID (String key, AuthData ctxAuthData, AuthData authData, EnvData env) throws KeyPrefixException, KommetException
	{
		String sValue = (String)getSettingValue(env.getType(KeyPrefix.get(KID.SETTING_VALUE_PREFIX)), "value", "key", key, ctxAuthData, authData, env);
		return StringUtils.hasText(sValue) ? KID.get(sValue) : null;
	}
	
	@Transactional(readOnly = true)
	public String getUserSettingAsString (String key, AuthData ctxAuthData, AuthData authData, EnvData env) throws KeyPrefixException, KommetException
	{
		return (String)getSettingValue(env.getType(KeyPrefix.get(KID.SETTING_VALUE_PREFIX)), "value", "key", key, ctxAuthData, authData, env);
	}
	
	/**
	 * Return a record associated with a UCH applicable to the current user context represented by the users <tt>AuthData</tt>.
	 * @param type
	 * @param fields
	 * @param contextAuthData <tt>AuthData</tt> identifying the user for which the setting is retrieved
	 * @param authData <tt>AuthData</tt> of a user performing the operation
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional(readOnly = true)
	public <T extends RecordProxy> T getSetting (Type type, Collection<String> fields, AuthData contextAuthData, AuthData authData, EnvData env) throws KommetException
	{
		return dao.getSetting(type, fields, null, null, compiler, contextAuthData, authData, env);
	}
	
	@Transactional(readOnly = true)
	public Object getSettingValue (Type type, String fieldName, String discriminatorFieldName, Object discriminatorFieldValue, AuthData contextAuthData, AuthData authData, EnvData env) throws KommetException
	{
		RecordProxy setting = getSetting(type, Arrays.asList(fieldName), discriminatorFieldName, discriminatorFieldValue, contextAuthData, authData, env);
		return setting != null ? setting.getField(fieldName) : null;
	}
	
	/**
	 * Return a record associated with a UCH applicable to the current user context represented by the users <tt>AuthData</tt>.
	 * @param type
	 * @param fields
	 * @param discriminatorFieldName The name of the discriminating field
	 * @param discriminatorFieldValue The value of the discriminating field
	 * @param contextAuthData <tt>AuthData</tt> identifying the user for which the setting is retrieved
	 * @param authData <tt>AuthData</tt> of a user performing the operation
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional(readOnly = true)
	public <T extends RecordProxy> T getSetting (Type type, Collection<String> fields, String discriminatorFieldName, Object discriminatorFieldValue, AuthData contextAuthData, AuthData authData, EnvData env) throws KommetException
	{
		return dao.getSetting(type, fields, discriminatorFieldName, discriminatorFieldValue, compiler, contextAuthData, authData, env);
	}
	
	@Transactional
	public UserCascadeHierarchy save (UserCascadeHierarchy uch, AuthData authData, EnvData env) throws KommetException
	{	
		// make sure exactly one active context is set
		List<String> activeContexts = new ArrayList<String>();
		if (uch.getEnv() != null)
		{
			activeContexts.add(UserCascadeHierarchyContext.ENVIRONMENT.toString());
		}
		if (uch.getProfile() != null)
		{
			activeContexts.add(UserCascadeHierarchyContext.PROFILE.toString());
		}
		if (uch.getLocale() != null)
		{
			activeContexts.add(UserCascadeHierarchyContext.LOCALE.toString());
		}
		if (uch.getUser() != null)
		{
			activeContexts.add(UserCascadeHierarchyContext.USER.toString());
		}
		
		if (activeContexts.size() > 1)
		{
			throw new UserCascadeHierarchyException("More than one active context defined for user cascade hierarchy: " + MiscUtils.implode(activeContexts, ", "));
		}
		
		// save UCH, skip triggers and sharing
		return dao.save(uch, true, true, false, false, authData, env);
	}
	
	@Transactional
	public void deleteSetting(SettingValue setting, AuthData authData, EnvData env) throws KommetException
	{
		dao.delete(setting.getHierarchy().getId(), authData, env);
		settingValueDao.delete(Arrays.asList(setting), authData, env);
	}
	
	public <T extends RecordProxy> T saveSetting(T setting, UserCascadeHierarchyContext context, Object contextValue, AuthData authData, EnvData env) throws KommetException
	{
		return saveSetting(setting, context, contextValue, false, authData, env);
	}

	/**
	 * Save a setting record and assign a UCH to it.
	 * <p>
	 * This method creates a UCH record with the given active context, assigns this UCH record to the setting
	 * and saves the setting.
	 * </p>
	 * @param setting
	 * @param context
	 * @param contextValue
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@SuppressWarnings("unchecked")
	@Transactional
	public <T extends RecordProxy> T saveSetting(T setting, UserCascadeHierarchyContext context, Object contextValue, boolean ignoreDuplicate, AuthData authData, EnvData env) throws KommetException
	{
		if (contextValue == null)
		{
			throw new UserCascadeHierarchyException("Context value is null");
		}
		
		// first create the UCH record
		UserCascadeHierarchy uch = new UserCascadeHierarchy();
		uch.setActiveContext(context, contextValue);
		uch = dao.save(uch, authData, env);

		Type settingType = null;
		
		if (setting instanceof CustomTypeRecordProxy)
		{
			settingType = env.getType(MiscUtils.envToUserPackage(setting.getClass().getName(), env));
		}
		else if (setting instanceof SettingValue)
		{
			// if the setting object is not a custom proxy, we only support SettingValue type
			// from the standard types to be used as a setting, simply because no other standard
			// (built in) type has reference to UCH
			settingType = env.getType(KeyPrefix.get(KID.SETTING_VALUE_PREFIX));
			
			SettingValueFilter filter = new SettingValueFilter();
			filter.addKey(((SettingValue)setting).getKey());
			filter.setContext(context);
			filter.setContextValue(contextValue);
			List<SettingValue> existingValues = getSettings(filter, authData, authData, env);
			
			if (!existingValues.isEmpty())
			{
				if (existingValues.size() > 1)
				{
					// this should never happen
					throw new KommetException("More than one setting exists with key " + ((SettingValue)setting).getKey() + ", context " + context + " and context value " + contextValue);
				}
				else
				{
					// if such setting already exists and it is not the same setting, this is an error
					if (setting.getId() == null || !existingValues.get(0).getId().equals(setting.getId()))
					{
						if (ignoreDuplicate)
						{
							return null;
						}
						else
						{
							throw new KommetException(SETTING_EXISTS_ERROR);
						}
					}
				}
			}
		}
		else
		{
			throw new KommetException("Unsupported type of object proxy: " + setting.getClass().getName());
		}
		
		// now save the setting object with reference to the newly added UCH
		// we need to convert proxy into record to be able to set the UCH reference field by reflection
		Record settingRecord = RecordProxyUtil.generateRecord(setting, settingType, 1, env);
		Field uchField = UserCascadeHierarchyUtil.getUCHField(settingType, env);
		
		// UCH records cannot exist without actual setting records referencing them, so we check
		// whether the type reference has cascade delete
		if (!((TypeReference)uchField.getDataType()).isCascadeDelete())
		{
			throw new UserCascadeHierarchyException("Field " + uchField.getApiName() + " on type " + settingType.getQualifiedName() + " referencing UCH object should have cascade delete set to true");
		}
		
		settingRecord.setField(uchField.getApiName() + "." + Field.ID_FIELD_NAME, uch.getId(), env);
		
		settingRecord = dataService.save(settingRecord, authData, env);
		
		// setting record will either be a custom type, or a standard type SettingValue
		// no other standard types are supported as settings
		if (setting instanceof SettingValue)
		{
			return (T)RecordProxyUtil.generateStandardTypeProxy(settingRecord, env, compiler);
		}
		else
		{
			return (T)RecordProxyUtil.generateCustomTypeProxy(settingRecord, env, compiler);
		}
	}
	
	@Transactional
	public SettingValue saveUserSetting(String key, String value, UserCascadeHierarchyContext ctx, Object contextValue, AuthData authData, EnvData env) throws KommetException
	{
		return saveUserSetting(key, value, ctx, contextValue, false, authData, env);
	}
	
	@Transactional
	public SettingValue saveUserSetting(String key, String value, UserCascadeHierarchyContext ctx, Object contextValue, boolean ignoreDuplicate, AuthData authData, EnvData env) throws KommetException
	{
		SettingValue setting = new SettingValue();
		setting.setKey(key);
		setting.setValue(value);
		
		return saveSetting(setting, ctx, contextValue, ignoreDuplicate, authData, env);
	}

	/**
	 * Deletes a UCH object.
	 * @param objects
	 * @param authData
	 * @param env
	 * @throws KommetException
	 */
	@Transactional
	public void delete(List<UserCascadeHierarchy> objects, AuthData authData, EnvData env) throws KommetException
	{
		dao.delete(objects, authData, env);
	}
	
	@Transactional(readOnly = true)
	public List<SettingValue> getSettings(SettingValueFilter filter, AuthData ctxAuthData, AuthData rootAuthData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new SettingValueFilter();
		}
		
		return settingValueDao.get(filter, rootAuthData, env);
	}

	/**
	 * Gets all settings for the given user
	 * @param ctxAuthData
	 * @param rootAuthData
	 * @param env
	 * @return
	 * @throws KommetException 
	 */
	// TODO add unit test for this method
	@Transactional(readOnly = true)
	public Map<String, String> getSettingsAsMap(SettingValueFilter filter, AuthData ctxAuthData, AuthData rootAuthData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new SettingValueFilter();
		}
		
		List<SettingValue> settingValues = settingValueDao.get(filter, rootAuthData, env);
		
		Set<String> settingKeys = new HashSet<String>();
		
		// get unique keys
		for (SettingValue val : settingValues)
		{
			settingKeys.add(val.getKey());
		}
		
		Type settingType = env.getType(KeyPrefix.get(KID.SETTING_VALUE_PREFIX));
		
		Map<String, String> finalValues = new HashMap<String, String>();
		
		// for each key, get the applicable value
		for (String key : settingKeys)
		{
			finalValues.put(key, (String)getSettingValue(settingType, "value", "key", key, ctxAuthData, rootAuthData, env));
		}
		
		return finalValues;
	}

	@Transactional(readOnly = true)
	public String getInterpretedValue(SettingValue setting, EnvData env) throws KommetException
	{
		if (UserSettingKeys.KM_SYS_DEFAULT_LAYOUT_ID.equals(setting.getKey()))
		{
			Layout layout = layoutDao.get(KID.get(setting.getValue()), env);
			if (layout == null)
			{
				throw new KommetException("Layout with ID " + setting.getValue() + " used in user setting " + setting.getKey() + " not found");
			}
			return layout.getName();
		}
		else
		{
			// return unchanged value
			return setting.getValue();
		}
	}
}