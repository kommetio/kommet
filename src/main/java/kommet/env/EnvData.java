/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.env;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.auth.AuthHandlerConfig;
import kommet.auth.UserService;
import kommet.basic.Action;
import kommet.basic.BusinessProcess;
import kommet.basic.Button;
import kommet.basic.Class;
import kommet.basic.Dictionary;
import kommet.basic.RecordProxy;
import kommet.basic.SharingRule;
import kommet.basic.SystemSetting;
import kommet.basic.TypeTrigger;
import kommet.basic.User;
import kommet.basic.View;
import kommet.basic.ViewResource;
import kommet.basic.WebResource;
import kommet.basic.keetle.FieldLabelNotFoundException;
import kommet.basic.keetle.ViewDao;
import kommet.basic.keetle.ViewFilter;
import kommet.basic.types.SystemTypes;
import kommet.businessprocess.BusinessProcessException;
import kommet.businessprocess.BusinessProcessExecutor;
import kommet.dao.GlobalTypeStore;
import kommet.dao.KommetPersistenceException;
import kommet.dao.RecordProxyMapping;
import kommet.dao.TypeForProxyNotFoundException;
import kommet.dao.TypePersistenceConfig;
import kommet.dao.TypePersistenceMapping;
import kommet.dao.dal.DALCriteriaBuilder;
import kommet.dao.queries.Criteria;
import kommet.data.DataService;
import kommet.data.Env;
import kommet.data.EnvSpecificTypeException;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.errorlog.ErrorLogService;
import kommet.errorlog.ErrorLogSeverity;
import kommet.filters.ButtonFilter;
import kommet.filters.DictionaryFilter;
import kommet.filters.WebResourceFilter;
import kommet.i18n.I18nDictionary;
import kommet.integration.EnvPersistenceInterface;
import kommet.koll.ClassCompilationException;
import kommet.koll.ClassService;
import kommet.koll.annotations.Auth;
import kommet.koll.annotations.Public;
import kommet.koll.annotations.ResponseBody;
import kommet.koll.annotations.Rest;
import kommet.koll.annotations.triggers.AfterInsert;
import kommet.koll.annotations.triggers.AfterUpdate;
import kommet.koll.annotations.triggers.BeforeInsert;
import kommet.koll.annotations.triggers.BeforeUpdate;
import kommet.koll.annotations.triggers.OldValues;
import kommet.koll.compiler.KommetCompiler;
import kommet.labels.TextLabelDictionary;
import kommet.persistence.CustomTypeRecordProxyDao;
import kommet.persistence.Entity;
import kommet.rest.RestServiceException;
import kommet.services.ButtonService;
import kommet.services.DictionaryService;
import kommet.services.ViewResourceService;
import kommet.services.WebResourceService;
import kommet.systemsettings.SystemSettingKey;
import kommet.triggers.TriggerException;
import kommet.triggers.TypeTriggerDao;
import kommet.triggers.TypeTriggerFilter;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.PropertyUtilException;
import kommet.web.actions.ParsedURL;

public class EnvData
{
	private Env env;
	private JdbcTemplate jdbcTemplate;
	
	/**
	 * The super admin of the whole environment
	 */
	private Record rootUser;
	
	/**
	 * Guest user used for unauthenticated operations
	 */
	private User guestUser;
	
	/**
	 * Note: although PersistenceMappingService is declared as a bean, we don't want it to be injected.
	 * We want it to work as a plain POJO here.
	 */
	private TypePersistenceConfig typePersistenceConfig;
	
	/**
	 * Stores information about record-to-object-proxy translation of custom 
	 * (i.e. user defined) types on the given environment.
	 */
	private EnvPersistenceConfig customTypeProxyConfig;
	
	/**
	 * Stores information about record-to-object-proxy translation of built-in 
	 * (i.e. basic) types on the given environment.
	 */
	private EnvPersistenceConfig basicTypeProxyConfig;
	
	/**
	 * Stores all KObject definitions for the given environment.
	 */
	private GlobalTypeStore globalTypeStore;
	
	private Map<String, Action> actionsByUrl = new HashMap<String, Action>();
	
	private static final Logger log = LoggerFactory.getLogger(EnvData.class);
	
	/**
	 * Time when type permissions where last updated on the environment;
	 */
	private long lastTypePermissionsUpdate;
	
	/**
	 * Time when field permissions where last updated on the environment;
	 */
	private long lastFieldPermissionsUpdate;
	
	/**
	 * Time when page permissions where last updated on the environment;
	 */
	private long lastActionPermissionsUpdate;
	
	/**
	 * The time when the environment was initialized in the cache
	 */
	private long timeInitialized;
	
	/**
	 * Views mapped by their ID.
	 * When we have pages mapped by their URL, information about a view related to the given page
	 * is also stored there. Multiple pages can reference the same view. When a view is updated by a user,
	 * we would need to update its references in all pages in the "pagesByUrl" collection, which would be a pain.
	 * 
	 * The solution is to have all actions in the actionsByUrl collection reference views from the views map, and
	 * update only this map.
	 */
	private Map<KID, View> views;
	
	/**
	 * TypeTrigger assignments by type ID 
	 */
	private HashMap<KID, Map<KID, TypeTrigger>> triggersByTypeAndFile;
	
	/**
	 * Validation rule assignments by type ID 
	 */
	private Set<KID> typesWithValidationRules;
	
	private Map<String, String> settings;
	
	/**
	 * Map where all text labels are stored. Each entry is a locale mapped to the text label dictionary.
	 */
	private TextLabelDictionary textLabelDictionary;
	
	/**
	 * Generic actions are actions not stored in the database, they are initialized on system start-up
	 * from actions annotated with the @Rest annotation.
	 */
	private Map<String, GenericAction> genericActions;
	
	/**
	 * Maps web resources by name.
	 */
	private Map<String, WebResource> webResourcesByName;
	
	/**
	 * View resources mapped by their name.
	 * Note: view resources in this collection will have content property not initialized
	 * to spare memory.
	 */
	private Map<String, ViewResource> viewResources;
	
	private Map<Long, AuthData> authDataByThread = new HashMap<Long, AuthData>();
	
	private String compileClassPath;
	
	private Map<KID, List<SharingRule>> sharingRulesByType = new HashMap<KID, List<SharingRule>>();
	
	private Map<KID, List<SharingRule>> dependentSharingRulesByType = new HashMap<KID, List<SharingRule>>();
	
	/**
	 * Processes mapped by either the KID of the type for which they can be triggered, or by the type name (if the process should be triggered for all types, i.e. for type name kommet.basic.RecordProxy)
	 */
	private Map<String, List<BusinessProcess>> triggerableBusinessProcesses = new HashMap<String, List<BusinessProcess>>();
	
	private Map<KID, BusinessProcessExecutor> businessProcessExecutors = new HashMap<KID, BusinessProcessExecutor>();
	
	private Map<KID, Boolean> triggersWithBeforeOldProxiesByTypeId = new HashMap<KID, Boolean>();
	private Map<KID, Boolean> triggersWithAfterOldProxiesByTypeId = new HashMap<KID, Boolean>();
	
	private Map<KID, Dictionary> dictionaries = new HashMap<KID, Dictionary>();
	
	private Map<KID, List<Button>> customTypeButtons = new HashMap<KID, List<Button>>();
 	
	public EnvData (Env env, DataSource dataSource) throws KommetException
	{
		this.env = env;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.typePersistenceConfig = new TypePersistenceConfig();
		this.globalTypeStore = new GlobalTypeStore();
		this.customTypeProxyConfig = new EnvPersistenceConfig(this.globalTypeStore);
		this.basicTypeProxyConfig = new EnvPersistenceConfig(this.globalTypeStore);
		
		// date when buffered permissions on this env object were last fetched from the actual database values
		long lastPermissionUpdate = (new Date()).getTime();
		this.lastFieldPermissionsUpdate = lastPermissionUpdate;
		this.lastTypePermissionsUpdate = lastPermissionUpdate;
		this.lastActionPermissionsUpdate = lastPermissionUpdate;
		this.views = new HashMap<KID, View>();
		this.triggersByTypeAndFile = new HashMap<KID, Map<KID, TypeTrigger>>();
		this.typesWithValidationRules = new HashSet<KID>();
		this.genericActions = new HashMap<String, GenericAction>();
		this.webResourcesByName = new HashMap<String, WebResource>();
		this.viewResources = new HashMap<String, ViewResource>();
		this.textLabelDictionary = new TextLabelDictionary();
		
		// set default time zone for the system
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	public Env getEnv()
	{
		return env;
	}

	public JdbcTemplate getJdbcTemplate()
	{
		return jdbcTemplate;
	}
	
	public void setJdbcTemplate(JdbcTemplate t)
	{
		this.jdbcTemplate = t;
	}
	
	public TypePersistenceMapping getTypeMapping (KID rid)
	{
		return this.typePersistenceConfig.getMapping(rid);
	}
	
	public TypePersistenceMapping getTypeMappingByApiName (String apiName)
	{
		return this.typePersistenceConfig.getMappingByApiName(apiName);
	}

	public void addTypeMapping (Type type) throws KommetException
	{
		if (type.getId() == null)
		{
			throw new KommetException("Cannot add type mapping for type with null ID");
		}
		
		if (type.getKID() == null)
		{
			throw new KommetException("Cannot add type mapping for type with null KID");
		}
		
		this.typePersistenceConfig.addMapping(type.getKID(), type.getQualifiedName(), TypePersistenceMapping.get(type, this));
	}
	
	public void addCustomTypeProxyMapping (java.lang.Class<? extends RecordProxy> proxyClass) throws KommetException
	{
		this.customTypeProxyConfig.addMapping(proxyClass, this);
	}
	
	public void deleteCustomTypeProxyMapping (java.lang.Class<? extends RecordProxy> proxyClass, boolean isDeclaredInCode) throws KommetException
	{
		this.customTypeProxyConfig.deleteMapping(proxyClass, isDeclaredInCode, this);
	}
	
	public void addBasicTypeProxyMapping (java.lang.Class<? extends RecordProxy> proxyClass) throws KommetException
	{
		this.basicTypeProxyConfig.addMapping(proxyClass, this);
	}
	
	public void deleteBasicTypeProxyMapping (java.lang.Class<? extends RecordProxy> proxyClass) throws KommetException
	{
		this.basicTypeProxyConfig.deleteMapping(proxyClass, false, this);
	}

	public void setRootUser(Record rootUser) throws KommetException
	{
		if (!(rootUser.getType().getApiName().equals(SystemTypes.USER_API_NAME)))
		{
			throw new KommetException("Root user has to be an instance of the User type");
		}
		this.rootUser = rootUser;
	}

	public Record getRootUser()
	{
		return rootUser;
	}
	
	public void registerType (Type type) throws KommetException
	{
		if (isEnvSpecificPackage(type.getQualifiedName()))
		{
			throw new KommetException("Type qualified name " + type.getQualifiedName() + " is env-specific");
		}
		this.globalTypeStore.registerType(type);
	}
	
	public void removeType (Type type) throws KommetException
	{
		this.globalTypeStore.unregisterType(type);
		this.typePersistenceConfig.removeMapping(type.getKID(), type.getQualifiedName());
		// remove trigger info
		this.triggersByTypeAndFile.remove(type.getKID());
		// remove validation rule flag
		this.typesWithValidationRules.remove(type.getKID());
	}
	
	public Type getType (KeyPrefix keyPrefix) throws KommetException
	{
		if (keyPrefix == null)
		{
			throw new KommetException("Attempt to get type by null key prefix");
		}
		Type type = this.globalTypeStore.getType(keyPrefix); 
		return type != null ? MiscUtils.cloneType(type) : null;
	}
	
	/**
	 * Returns a detached copy of the type from the environment. Since it is a copy, any changes made to it (the type itself and its fields) will not be
	 * reflected on the type/field instances stored on the env. Type/field would need to be re-registered for any changes to take effect.
	 * @param typeId
	 * @return
	 * @throws KommetException
	 */
	public Type getType (KID typeId) throws KommetException
	{
		Type type = this.globalTypeStore.getType(typeId); 
		return type != null ? MiscUtils.cloneType(type) : null;
	}
	
	public Collection<Type> getAllTypes () throws KommetException
	{
		List<Type> types = new ArrayList<Type>();
		
		for (Type type : this.globalTypeStore.getAllTypes())
		{
			types.add(MiscUtils.cloneType(type));
		}
		
		return types;
	}
	
	public List<Type> getAllTypesInCreationOrder () throws KommetException
	{
		List<Type> types = new ArrayList<Type>();
		
		for (Type type : this.globalTypeStore.getAllTypes())
		{
			types.add(MiscUtils.cloneType(type));
		}
		
		// sort by ID, ascending
		Collections.sort(types, new TypeComparator());
		return types;
	}
	
	public Collection<Type> getCustomTypes() throws KommetException
	{	
		List<Type> types = new ArrayList<Type>();
		
		for (Type type : this.globalTypeStore.getAllNonStandardTypes())
		{
			types.add(MiscUtils.cloneType(type));
		}
		
		return types;
	}
	
	public List<Type> getUserAccessibleTypes() throws KommetException
	{
		Collection<Type> allTypes = getAllTypes();
		List<Type> filteredTypes = new ArrayList<Type>();
		for (Type type : allTypes)
		{
			if (type.isAccessible())
			{
				filteredTypes.add(type);
			}
		}
		
		return filteredTypes;
	}
	
	/**
	 * Returns all custom types plus accessible standard types.
	 * @return
	 * @throws KommetException
	 */
	public Collection<Type> getAccessibleTypes() throws KommetException
	{	
		List<Type> types = new ArrayList<Type>();
		
		for (Type type : this.globalTypeStore.getAllTypes())
		{
			if (type.isAccessible())
			{
				types.add(MiscUtils.cloneType(type));
			}
		}
		
		return types;
	}
	
	/**
	 * Returns the type by its qualified name.
	 * @param qualifiedName
	 * @return
	 * @throws KommetException 
	 */
	public Type getType (String qualifiedName) throws KommetException
	{
		if (isEnvSpecificPackage(qualifiedName))
		{
			throw new EnvSpecificTypeException("Qualified type name " + qualifiedName + " is env-specific", qualifiedName);
		}
		Type type = this.globalTypeStore.getType(qualifiedName); 
		return type != null ? MiscUtils.cloneType(type) : null;
	}
	
	public Criteria getSelectCriteria (KID typeId) throws KommetException
	{
		return getSelectCriteria(typeId, null);
	}
	
	public Criteria getSelectCriteria (KID typeId, AuthData authData) throws KommetException
	{
		return getCriteria(typeId, authData, true);
	}
	
	public Criteria getUpdateCriteria (KID typeId) throws KommetException
	{
		return getCriteria(typeId, null, false);
	}
	
	private Criteria getCriteria (KID typeId, AuthData authData, boolean useMainTableAlias) throws KommetException
	{
		return new Criteria(getType(typeId), authData, this, useMainTableAlias);
	}
	
	public Criteria getSelectCriteriaFromDAL (String dalQuery) throws KommetException
	{
		return getSelectCriteriaFromDAL(dalQuery, null);
	}

	public Criteria getSelectCriteriaFromDAL (String dalQuery, AuthData authData) throws KommetException
	{
		return DALCriteriaBuilder.getSelectCriteriaFromDAL(dalQuery, authData, this);
	}
	
	public List<Record> select(String dalQuery, AuthData authData) throws KommetException
	{
		return getSelectCriteriaFromDAL(dalQuery, authData).list();
	}
	
	public List<Record> select(String dalQuery) throws KommetException
	{
		return getSelectCriteriaFromDAL(dalQuery).list();
	}
	
	public List<Record> select(Type type, Collection<String> fields, String conditions, AuthData authData) throws KommetException
	{
		StringBuilder query = new StringBuilder("SELECT ");
		query.append(MiscUtils.implode(fields, ", ")).append(" FROM ").append(type.getQualifiedName());
		
		if (StringUtils.hasText(conditions))
		{
			query.append(" WHERE ").append(conditions);
		}
		
		return getSelectCriteriaFromDAL(query.toString(), authData).list();
	}

	public String getKeetleDir(String keetleDir)
	{
		return keetleDir + "/" + this.env.getKID();
	}
	
	/**
	 * Convert the given directory to an env-specific view resource directory,
	 * usually by appending the env's ID.
	 * @param commonViewResourceDir Relative or absolute path to the common view resource directory
	 * @return
	 */
	public String getViewResourceEnvDir(String commonViewResourceDir)
	{
		return commonViewResourceDir + "/" + this.env.getKID();
	}
	
	public String getLayoutDir(String layoutDir)
	{
		return layoutDir + "/" + this.env.getKID();
	}

	/**
	 * Return the name of the environment.
	 * @return
	 */
	public String getName()
	{
		return this.env.getName();
	}

	/**
	 * Return the ID of the environment.
	 * @return
	 */
	public KID getId()
	{
		return this.env.getKID();
	}
	
	public void addAction(String url, Action action) throws KommetException
	{
		addAction(url, null, action);
	}
	
	/**
	 * Add the action to the environment. If an action with the given URL already exists, it will be replaced
	 * @param url
	 * @param action
	 * @param oldUrl
	 * @throws KommetException
	 */
	public void addAction (String url, String oldUrl, Action action) throws KommetException
	{
		if (!StringUtils.hasText(url))
		{
			throw new KommetException("Cannot add action to environment whose URL is empty");
		}
		
		if (StringUtils.hasText(oldUrl))
		{
			this.actionsByUrl.remove(oldUrl.toLowerCase());
		}
		
		this.actionsByUrl.put(url.toLowerCase(), action);
		this.addView(action.getView());
	}

	public Action getActionForUrl(String url) throws KommetException
	{
		if (!StringUtils.hasText(url))
		{
			throw new KommetException("URL by which action is retrieved is empty");
		}
		
		// check whether each URL matches the provided one
		for (String existingURL : this.actionsByUrl.keySet())
		{
			if (new ParsedURL(existingURL).matchesParameterized(url))
			{
				return this.actionsByUrl.get(existingURL);
			}
		}
		
		return null;
	}
	
	public void removeActionForUrl(String url) throws KommetException
	{	
		if (this.actionsByUrl != null)
		{
			this.actionsByUrl.remove(url.toLowerCase());
		}
	}

	public Type getTypeByRecordId (KID recordId) throws KommetException
	{
		return getType(recordId.getKeyPrefix());
	}

	public RecordProxyMapping getCustomTypeProxyMapping (KID typeId)
	{
		return this.customTypeProxyConfig.getMapping(typeId);
	}
	
	public RecordProxyMapping getBasicTypeProxyMapping (KID typeId)
	{
		return this.basicTypeProxyConfig.getMapping(typeId);
	}

	@SuppressWarnings("unchecked")
	public void scanForPersistenceMappings (String basePackage, boolean ignoreNonExistingTypes) throws KommetException
	{
		log.debug("Scanning package for persistent mappings");
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));

		for (BeanDefinition candidate : scanner.findCandidateComponents(basePackage))
		{
			java.lang.Class<?> cls = ClassUtils.resolveClassName(candidate.getBeanClassName(), ClassUtils.getDefaultClassLoader());
			
			if (RecordProxy.class.isAssignableFrom(cls))
			{
				try
				{
					addBasicTypeProxyMapping((java.lang.Class<? extends RecordProxy>)cls);
					log.debug("Found type proxy " + cls.getName());
				}
				catch (TypeForProxyNotFoundException e)
				{
					if (!ignoreNonExistingTypes)
					{
						throw e;
					}
					else
					{
						log.warn("Ignored error: " + e.getMessage());
					}
				}
			}
			else
			{
				throw new KommetPersistenceException("Class " + cls.getName() + " is annotated with @" + Entity.class.getSimpleName() + " but does not extends ObjectProxy");
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public CustomTypeRecordProxyDao<? extends RecordProxy> getCustomProxyDao(java.lang.Class<? extends RecordProxy> cls, EnvPersistenceInterface envPersistence)
	{
		return (CustomTypeRecordProxyDao<? extends RecordProxy>)(new CustomTypeRecordProxyDao(cls, envPersistence));
	}
	
	public boolean isEnvSpecificPackage (String packageName)
	{
		return packageName.startsWith(getEnv().getBasePackage());
	}

	public void registerField (Field field) throws KommetException
	{
		Type type = field.getType();
		if (type == null)
		{
			throw new KommetException("Type is not set on field " + field.getApiName());
		}
		if (type.getKID() == null)
		{
			throw new KommetException("Trying to register field on an unsaved type");
		}
		
		// get the original type from env
		Type typeOnEnv = getOriginalType(type.getKID());
		if (typeOnEnv == null)
		{
			throw new KommetException("Cannot update field because type " + type.getApiName() + " is not registered on the environment");
		}
	
		// update the type with new field on env
		typeOnEnv.addField(MiscUtils.cloneField(field));
	}

	/**
	 * Returns the original type object from the env, not its protective copy.
	 * @param rid
	 * @return
	 */
	private Type getOriginalType(KID rid)
	{
		return this.globalTypeStore.getType(rid);
	}

	public void setLastTypePermissionsUpdate(long lastTypePermissionsUpdate)
	{
		this.lastTypePermissionsUpdate = lastTypePermissionsUpdate;
	}

	public long getLastTypePermissionsUpdate()
	{
		return lastTypePermissionsUpdate;
	}

	public void setLastFieldPermissionsUpdate(long lastFieldPermissionsUpdate)
	{
		this.lastFieldPermissionsUpdate = lastFieldPermissionsUpdate;
	}

	public long getLastFieldPermissionsUpdate()
	{
		return lastFieldPermissionsUpdate;
	}

	public void setLastActionPermissionsUpdate(long lastActionPermissionsUpdate)
	{
		this.lastActionPermissionsUpdate = lastActionPermissionsUpdate;
	}

	public long getLastActionPermissionsUpdate()
	{
		return lastActionPermissionsUpdate;
	}

	public void renameType(String oldQualifiedName, Type type) throws KommetException
	{
		globalTypeStore.renameType(oldQualifiedName, type);
		typePersistenceConfig.renameType(oldQualifiedName, type, this);
	}

	public void removeField(Type type, String apiName) throws KommetException
	{
		getOriginalType(type.getKID()).removeField(apiName);
	}

	public void renameField(Type type, Field field, String oldFieldName) throws KommetException
	{
		getOriginalType(type.getKID()).renameField(field, oldFieldName);
	}

	public void updateType(Type type) throws KommetException
	{
		globalTypeStore.updateType(type);
		typePersistenceConfig.updateType(type, this);
	}
	
	public void addSetting (SystemSetting setting)
	{
		if (this.settings == null)
		{
			this.settings = new HashMap<String, String>();
		}
		this.settings.put(setting.getKey(), setting.getValue());
	}

	public String getSetting (SystemSettingKey key) throws KommetException
	{
		return getSetting(key.toString());
	}
	
	public String getSetting (String key) throws KommetException
	{
		if (settings == null)
		{
			return null;
		}
		return settings.get(key);
	}

	/**
	 * This method returns a copy of the field object identified by the parameters.
	 * It is necessary to have a copy of the field while updating it so that it can be compared
	 * to the original on the environment in search of changes.
	 * @param typePrefix
	 * @param fieldApiName
	 * @return
	 * @throws KommetException
	 */
	public Field getFieldForUpdate(KeyPrefix typePrefix, String fieldApiName) throws KommetException
	{
		Type type = this.globalTypeStore.getType(typePrefix);
		Field field = type.getField(fieldApiName);
		return field != null ? MiscUtils.cloneField(field) : null;
	}

	public User getUser(KID userId, UserService userService) throws KommetException
	{
		// TODO buffer users somehow, e.g. 100 last used users
		return userService.getUser(userId, this);
	}
	
	public View getView(KID id)
	{
		return this.views.get(id);
	}
	
	public void addView (View view) throws KommetException
	{
		if (view == null)
		{
			throw new KommetException("Trying to add a null view");
		}
		else if (view.getId() == null)
		{
			throw new KommetException("Trying to add view with null ID");
		}
		
		this.views.put(view.getId(), view);
	}

	public Map<KID, TypeTrigger> getTriggers(KID typeId)
	{
		return this.triggersByTypeAndFile.containsKey(typeId) ? this.triggersByTypeAndFile.get(typeId) : new HashMap<KID, TypeTrigger>();
	}
	
	public KID getBlankLayoutId() throws KommetException
	{
		String id = getSetting(SystemSettingKey.BLANK_LAYOUT_ID);
		if (id == null)
		{
			throw new KommetException("Blank layout ID for env " + this.getName() + " is not set");
		}
		return KID.get(id);
	}

	public void registerTrigger(TypeTrigger typeTrigger) throws KommetException
	{
		if (typeTrigger.getTypeId() == null)
		{
			throw new KommetException("Type ID not set on type-trigger assignment");
		}
		
		if (getType(typeTrigger.getTypeId()) == null)
		{
			throw new KommetException("Trying to register trigger for type with ID " + typeTrigger.getTypeId() + " - no such type ID exists");
		}
		
		if (typeTrigger.getTriggerFile() == null)
		{
			throw new KommetException("Trigger file not set on trigger-type assignment");
		}
		else
		{
			if (typeTrigger.getTriggerFile().getId() == null)
			{
				throw new KommetException("Trying to registered an unsaved trigger file to type");
			}
		}
		
		// Add the new file to the type triggers.
		// If this trigger file is already registered for this type, it will be replaced.
		Map<KID, TypeTrigger> triggersByFileId = this.triggersByTypeAndFile.get(typeTrigger.getTypeId());
		if (triggersByFileId == null)
		{
			triggersByFileId = new HashMap<KID, TypeTrigger>();
		}
		triggersByFileId.put(typeTrigger.getTriggerFile().getId(), typeTrigger);
		this.triggersByTypeAndFile.put(typeTrigger.getTypeId(), triggersByFileId);
	}
	
	public void unregisterTrigger(TypeTrigger typeTrigger) throws TriggerException
	{
		Map<KID, TypeTrigger> triggersByFileId = this.triggersByTypeAndFile.get(typeTrigger.getTypeId());
		if (triggersByFileId == null)
		{
			throw new TriggerException("No triggers exist for type " + typeTrigger.getTypeId());
		}
		
		if (triggersByFileId.containsKey(typeTrigger.getTriggerFile().getId()))
		{
			triggersByFileId.remove(typeTrigger.getTriggerFile().getId());
		}
		else
		{
			throw new TriggerException("File with ID " + typeTrigger.getTriggerFile().getId() + " is not registered as trigger for type " + typeTrigger.getTypeId());
		}
	}
	
	public void setTimeInitialized(long timeInitialized)
	{
		this.timeInitialized = timeInitialized;
	}

	public long getTimeInitialized()
	{
		return timeInitialized;
	}

	public void setTextLabelDictionary(TextLabelDictionary textLabelDictionary)
	{
		this.textLabelDictionary = textLabelDictionary;
	}
	
	public TextLabelDictionary getTextLabelDictionary()
	{
		return this.textLabelDictionary;
	}
	
	/**
	 * Returns the label of the field specified as argument. The first is specified in the package-type-field
	 * notation, e.g. "kommet.some.package.TypeName.fieldName".
	 * @param typeAndField
	 * @return If type and field exist, the label of the field is returned. If type or field do not exist,
	 * either null is returned or FieldLabelNotFoundException is thrown, depending on the system setting
	 * {@link SystemSettingKey.IGNORE_NON_EXISTING_FIELD_LABELS}
	 * @throws KommetException
	 */
	public String getEnvSpecificFieldLabel(String typeAndField, AuthData authData) throws KommetException
	{
		String typeName = typeAndField.substring(0, typeAndField.lastIndexOf('.'));
		
		Type type = getType(typeName);
		
		if (type == null)
		{
			if ("true".equals(getSetting(SystemSettingKey.IGNORE_NON_EXISTING_FIELD_LABELS)))
			{
				return null;
			}
			else
			{
				throw new FieldLabelNotFoundException("Type " + typeName + " not found");
			}
		}
		else
		{
			Field field = type.getField(typeAndField.substring(typeAndField.lastIndexOf('.') + 1));
			if (field == null)
			{
				if ("true".equals(getSetting(SystemSettingKey.IGNORE_NON_EXISTING_FIELD_LABELS)))
				{
					return null;
				}
				else
				{
					throw new FieldLabelNotFoundException("Field " + typeAndField.substring(typeAndField.lastIndexOf('.') + 1) + " not found");
				}
			}
			
			return field.getInterpretedLabel(authData);
		}
	}

	class TypeComparator implements Comparator<Type>
	{
	    @Override
	    public int compare(Type o1, Type o2)
	    {
	        return o1.getId().compareTo(o2.getId());
	    }
	}

	public void clearSystemSettingCache()
	{
		this.settings = null;
	}

	public void clearValidationRuleFlags()
	{
		this.typesWithValidationRules = new HashSet<KID>();
	}

	/**
	 * Tells whether active validation rules exist for the given type.
	 * @param typeId
	 * @return
	 */
	public boolean hasValidationRules(KID typeId)
	{
		return this.typesWithValidationRules != null && this.typesWithValidationRules.contains(typeId);
	}

	public void setHasValidationRules(KID typeId, boolean flag)
	{
		if (flag)
		{
			this.typesWithValidationRules.add(typeId);
		}
		else
		{
			this.typesWithValidationRules.remove(typeId);
		}
	}

	public Map<String, GenericAction> getGenericActions()
	{
		return genericActions;
	}
	
	public GenericAction getGenericAction (String url)
	{
		// check whether each URL matches the provided one
		for (String existingURL : this.genericActions.keySet())
		{
			if (new ParsedURL(existingURL).matches(url))
			{
				return this.genericActions.get(existingURL);
			}
		}
		
		return null;
		//return this.genericActions.get(url);
	}
	
	public WebResource getWebResource (String name)
	{
		return this.webResourcesByName.get(name);
	}
	
	public Map<String, WebResource> getWebResources()
	{
		return this.webResourcesByName;
	}
	
	/**
	 * Initializes cached web resources on the env.
	 * @param webResourceService
	 * @throws KommetException
	 */
	public void initWebResources(WebResourceService webResourceService) throws KommetException
	{
		this.webResourcesByName = new HashMap<String, WebResource>();
		
		// fetch web resources from database
		List<WebResource> resources = webResourceService.initFilePath(webResourceService.find(new WebResourceFilter(), this), AuthData.getRootAuthData(this), this);
		
		for (WebResource resource : resources)
		{
			this.webResourcesByName.put(resource.getName(), resource);
		}
	}
	
	/**
	 * Initializes cached view resources on the env.
	 * @param webResourceService
	 * @throws KommetException
	 */
	public void initViewResources(ViewResourceService viewResourceService) throws KommetException
	{
		this.viewResources = new HashMap<String, ViewResource>();
		
		// fetch web resources from database
		List<ViewResource> resources = viewResourceService.find(null, this);
		
		for (ViewResource resource : resources)
		{
			// store resources in map without content
			resource.setContent(null);
			this.viewResources.put(resource.getName(), resource);
		}
	}
	
	public ViewResource getViewResource (String name)
	{
		return this.viewResources.get(name);
	}
	
	public String getViewResourcePath (String name, AppConfig config) throws PropertyUtilException
	{
		ViewResource res = this.viewResources.get(name);
		return res != null ? getViewResourceEnvDir(config.getViewResourceRelativeDir()) + "/" + res.getPath() : null;
	}
	
	/**
	 * This method should be called every time a class file is updated, to update added/removed generic
	 * actions declared in this class file.
	 * @param file
	 * @throws KommetException 
	 */
	public void updateGenericActionsForClass (Class file, KommetCompiler compiler, Set<String> systemURLs, I18nDictionary i18n, ViewDao viewDao, boolean isDeleteClass) throws KommetException
	{
		if (file.getId() == null)
		{
			// we need the file ID to set its value to the GenericAction.controllerClassId property
			throw new KommetException("Cannot call generic action update for unsaved class file");
		}
		
		java.lang.Class<?> cls = null;
		
		// load class into class loader
		try
		{
			cls = compiler.getClass(file.getQualifiedName(), true, this);
		}
		catch (ClassNotFoundException e)
		{
			throw new KommetException("Generic actions cannot be updated for class " + file.getQualifiedName() + " because it is not available in the class loader");
		}
		
		Map<String, GenericAction> updatedActions = new HashMap<String, GenericAction>();
		
		// scan generic actions for actions declared in this class file
		// even if the file is not annotated with @Controller, because it might have been
		// annotated as such before being saved
		for (GenericAction action : this.genericActions.values())
		{
			// if an action is found that was declared in this controller
			// remove it, because we will add it anew below
			if (!action.getControllerClassId().equals(file.getId()))
			{
				updatedActions.put(action.getUrl(), action);
			}
		}
		
		this.genericActions = updatedActions;
		
		// if class is being deleted, we don't add its actions anew
		if (!isDeleteClass)
		{
			// add new actions from class
			getActionsFromClass(cls, file.getId(), systemURLs, i18n, viewDao, compiler);
		}
	}
	
	private void getActionsFromClass (java.lang.Class<?> cls, KID classFileId, Set<String> systemURLs, I18nDictionary i18n, ViewDao viewDao, KommetCompiler compiler) throws KommetException
	{
		Map<String, GenericAction> currentClassRestActions = new HashMap<String, GenericAction>();
		
		// read in generic actions - this has to be done after KOLL files have been initialized
		// get methods from the controller annotated with @Rest
		for (Method method : cls.getMethods())
		{
			kommet.koll.annotations.Action actionAnnotation = method.getAnnotation(kommet.koll.annotations.Action.class);
			boolean isGenericActionAnnotation = (actionAnnotation != null && StringUtils.hasText(actionAnnotation.url()));
			boolean isRestAnnotation = method.isAnnotationPresent(Rest.class); 
			
			if (isRestAnnotation || isGenericActionAnnotation)
			{
				GenericAction action = new GenericAction();
				action.setControllerName(MiscUtils.envToUserPackage(cls.getName(), this));
				action.setActionMethod(method.getName());
				
				if (isRestAnnotation)
				{
					// @Rest annotation cannot be mixed with @Action or @View
					if (isGenericActionAnnotation)
					{
						// TODO write unit test for this error
						throw new KommetException("@" + Rest.class.getSimpleName() + " annotation cannot be mixed with @" + kommet.koll.annotations.Action.class.getSimpleName());
					}
					
					if (method.isAnnotationPresent(kommet.koll.annotations.View.class))
					{
						// TODO write unit test for this error
						throw new KommetException("@" + Rest.class.getSimpleName() + " annotation cannot be mixed with @" + kommet.koll.annotations.View.class.getSimpleName());
					}
				}
				
				if (isGenericActionAnnotation)
				{
					action.setUrl(actionAnnotation.url());
					
					// check for @View annotation
					if (method.isAnnotationPresent(kommet.koll.annotations.View.class))
					{
						kommet.koll.annotations.View viewAnnotation = method.getAnnotation(kommet.koll.annotations.View.class);
						if (!StringUtils.hasText(viewAnnotation.name()))
						{
							throw new KommetException("View name not defined in @" + kommet.koll.annotations.View.class.getSimpleName() + " annotation");
						}
						
						// find view by name
						View actionView = null;
						ViewFilter filter = new ViewFilter();
						filter.setSystemView(false);
						
						// find non-system views
						for (View v : viewDao.find(filter, this))
						{
							if (v.getQualifiedName().equals(viewAnnotation.name()))
							{
								actionView = v;
								break;
							}
						}
						
						if (actionView == null)
						{
							throw new KommetException("View " + viewAnnotation.name() + " in @" + kommet.koll.annotations.View.class.getSimpleName() + " annotation not found");
						}
						
						action.setViewId(actionView.getId());
					}
				}
				else
				{
					action.setUrl(((Rest)method.getAnnotation(Rest.class)).url());
				}
				
				action.setControllerClassId(classFileId);
				
				// public actions are those annotated with @Public
				action.setPublic(method.isAnnotationPresent(Public.class));
				action.setRest(method.isAnnotationPresent(Rest.class));
				
				if (method.isAnnotationPresent(Auth.class))
				{
					kommet.koll.annotations.Auth authAnnotation = method.getAnnotation(kommet.koll.annotations.Auth.class);
					
					String handlerClassName = authAnnotation.handler();
					try
					{
						// check if class exists
						compiler.getClass(handlerClassName, true, this);
						action.setAuthHandler(handlerClassName);
					}
					catch (ClassNotFoundException e)
					{
						throw new KommetException("Class " + handlerClassName + " used for @Auth annotation handler not found");
					}
					
					AuthHandlerConfig conf = new AuthHandlerConfig();
					conf.setTokenHeader(authAnnotation.header());
					conf.setOverrideOtherAuth(authAnnotation.isOverride());
					action.setAuthHandlerConfig(conf);
				}
				
				if (action.getUrl().startsWith("/"))
				{
					throw new ClassCompilationException(i18n != null ? i18n.get("kolmu.rest.urlmustnotstartwithslash") : "REST service URL must not start with a slash");
				}
				
				if (method.isAnnotationPresent(ResponseBody.class))
				{
					// make sure the method returns type string
					if (method.getReturnType().equals(String.class))
					{
						action.setReturnsResponseBody(true);
					}
					else
					{
						throw new RestServiceException("Method " + method.getName() + " is annotated with @" + ResponseBody.class.getSimpleName() + " but does not return string");
					}
				}
				
				// check if a REST action with this URL has not been already defined
				if (currentClassRestActions.containsKey(action.getUrl()))
				{
					throw new ClassCompilationException("REST service with URL " + action.getUrl() + " is defined twice");
				}
				
				// check if a REST action with this URL is not already defined in a different class
				GenericAction genericAction = getGenericAction(action.getUrl()); 
				if (genericAction != null)
				{
					throw new ClassCompilationException("REST service with URL " + action.getUrl() + " is already defined in controller class " + genericAction.getControllerName());
				}
				
				// check if there isn't a regular action with this URL registered on the env
				if (getActionForUrl(action.getUrl()) != null)
				{
					throw new ClassCompilationException("An action with URL " + action.getUrl() + " is already defined on the environment");
				}
				
				// check system URLs - they start with a slash, and our action URL don't,
				// so we need to add a slash for this check
				if (systemURLs != null && systemURLs.contains("/" + action.getUrl()))
				{
					throw new ClassCompilationException("URL " + action.getUrl() + " is used by a system action");
				}
				
				currentClassRestActions.put(action.getUrl(), action);
				this.genericActions.put(action.getUrl(), action);
			}
		}
	}

	public void initGenericActions(List<Class> classes, Set<KID> classesWithCompileErrors, KommetCompiler compiler, Set<String> systemURLs, I18nDictionary i18n, ViewDao viewDao, ErrorLogService logService) throws KommetException
	{		
		for (Class file : classes)
		{	
			if (classesWithCompileErrors.contains(file.getId()))
			{
				// if we know that this class contains compile errors, we don't even try to get actions from it because it would cause an exception (class not found in class loader)
				continue;
			}
			
			try
			{
				// load class into class loader
				java.lang.Class<?> cls = compiler.getClass(file.getQualifiedName(), true, this);
				getActionsFromClass(cls, file.getId(), systemURLs, i18n, viewDao, compiler);
			}
			catch (ClassNotFoundException e)
			{
				throw new KommetException("Class " + file.getQualifiedName() + " not found by the environment\'s class loader");
			}
			catch (Exception e)
			{
				// even if the class had no formal compile problems (and was not included in "classesWithCompileErrors")
				// it might still cause problems with reading generic actions. This might happen e.g. if two controllers declare a @Rest action
				// with the same URL. From the compilation point of view they are OK, but the @Rest definitions are duplicate and an error will be thrown
				String msg = "Error while reading generci action from class " + file.getName() + ": " + e.getMessage();
				log.error(msg);
				logService.log(msg, ErrorLogSeverity.FATAL, this.getClass().getName(), -1, AuthData.getRootAuthData(this).getUserId(), AuthData.getRootAuthData(this), this);
			}
		}
	}

	public User getGuestUser()
	{
		return guestUser;
	}

	public void setGuestUser(User guestUser)
	{
		this.guestUser = guestUser;
	}

	public String getCompileClassPath()
	{
		return compileClassPath;
	}

	public void setCompileClassPath(String compileClassPath)
	{
		this.compileClassPath = compileClassPath;
	}
	
	public void addAuthData (AuthData authData)
	{
		this.authDataByThread.put(Thread.currentThread().getId(), authData);
	}
	
	public AuthData currentAuthData()
	{
		return this.authDataByThread.get(Thread.currentThread().getId());
	}
	
	public void clearAuthData()
	{
		this.authDataByThread.remove(Thread.currentThread().getId());
	}

	public Map<KID, List<SharingRule>> getSharingRulesByType()
	{
		return sharingRulesByType;
	}

	public Map<KID, List<SharingRule>> getDependentSharingRulesByType()
	{
		return dependentSharingRulesByType;
	}

	public void unregisterSharingRule(SharingRule rule, KommetCompiler compiler) throws KommetException
	{
		if (sharingRulesByType.containsKey(rule.getReferencedType()))
		{
			List<SharingRule> newTypeRules = new ArrayList<SharingRule>();
			for (SharingRule existingRule : sharingRulesByType.get(rule.getReferencedType()))
			{
				if (!existingRule.getId().equals(rule.getId()))
				{
					newTypeRules.add(existingRule);
				}
			}
			
			sharingRulesByType.put(rule.getReferencedType(), newTypeRules);
		}
		
		if (!StringUtils.hasText(rule.getDependentTypes()))
		{
			return;
		}
		
		List<String> typeIdsQueriedByRule = MiscUtils.splitAndTrim(rule.getDependentTypes(), ";");
		
		for (String typeId : typeIdsQueriedByRule)
		{
			Type queriedType = getType(KID.get(typeId));
			
			if (!dependentSharingRulesByType.containsKey(queriedType.getKID()))
			{
				throw new KommetException("Invalid state: rule was not registered for queried type " + queriedType.getKID());
			}
			
			List<SharingRule> newTypeRules = new ArrayList<SharingRule>();
			
			for (SharingRule existingRule : dependentSharingRulesByType.get(queriedType.getKID()))
			{
				if (!existingRule.getId().equals(rule.getId()))
				{
					newTypeRules.add(existingRule);
				}
			}
			
			dependentSharingRulesByType.put(queriedType.getKID(), newTypeRules);
		}
	}

	/**
	 * Registers the sharing rule on the environment.
	 * @param rule
	 * @throws KIDException
	 */
	public void registerSharingRule(SharingRule rule) throws KIDException
	{
		if (!this.sharingRulesByType.containsKey(rule.getReferencedType()))
		{
			this.sharingRulesByType.put(rule.getReferencedType(), new ArrayList<SharingRule>());
		}
		this.sharingRulesByType.get(rule.getReferencedType()).add(rule);
		
		if (StringUtils.hasText(rule.getDependentTypes()))
		{
			List<String> typeIds = MiscUtils.splitAndTrim(rule.getDependentTypes(), ";");
			
			for (String sTypeId : typeIds)
			{
				KID typeId = KID.get(sTypeId);
				if (!this.dependentSharingRulesByType.containsKey(typeId))
				{
					this.dependentSharingRulesByType.put(typeId, new ArrayList<SharingRule>());
				}
				this.dependentSharingRulesByType.get(typeId).add(rule);
			}
		}
				
	}

	public Map<String, List<BusinessProcess>> getTriggerableBusinessProcesses()
	{
		return triggerableBusinessProcesses;
	}

	public void setTriggerableBusinessProcesses(Map<String, List<BusinessProcess>> triggerableBusinessProcesses)
	{
		this.triggerableBusinessProcesses = triggerableBusinessProcesses;
	}

	/**
	 * Gets a cached process executor for the given process. If the executor does not exist, it is created first.
	 * @param bp
	 * @param compiler
	 * @param logService
	 * @param classService
	 * @return
	 * @throws BusinessProcessException
	 */
	public BusinessProcessExecutor getProcessExecutor(BusinessProcess bp, KommetCompiler compiler, ErrorLogService logService, ClassService classService, DataService dataService) throws BusinessProcessException
	{
		if (!this.businessProcessExecutors.containsKey(bp.getId()))
		{
			BusinessProcessExecutor executor = new BusinessProcessExecutor(compiler, logService, classService, dataService, this);
			executor.prepare(bp);
			this.businessProcessExecutors.put(bp.getId(), executor);
		}
		
		return this.businessProcessExecutors.get(bp.getId());
	}

	/**
	 * Removes a cached process executor for the process.
	 * @param processId
	 */
	public void removeProcessExecutor(KID processId)
	{
		this.businessProcessExecutors.remove(processId);
	}
	
	public void removeProcessExecutors()
	{
		this.businessProcessExecutors.clear();
	}

	/**
	 * Tells if there is a cached process executor for this process.
	 * @param processId
	 * @return
	 */
	public boolean isCachedProcessExecutor(KID processId)
	{
		return this.businessProcessExecutors.containsKey(processId);
	}

	/**
	 * Tells if the type with the given ID has @Before triggers that require old proxies 
	 * @param typeId
	 * @return
	 */
	public boolean hasTypeBeforeTriggersWithOldProxies(KID typeId)
	{
		return Boolean.TRUE.equals(this.triggersWithBeforeOldProxiesByTypeId.get(typeId));
	}
	
	/**
	 * Tells if the type with the given ID has @After triggers that require old proxies 
	 * @param typeId
	 * @return
	 */
	public boolean hasTypeAfterTriggersWithOldProxies(KID typeId)
	{
		return Boolean.TRUE.equals(this.triggersWithAfterOldProxiesByTypeId.get(typeId));
	}

	public void setHasTypeAfterTriggersWithOldProxies(KID typeId, boolean value)
	{
		this.triggersWithAfterOldProxiesByTypeId.put(typeId, value);
	}
	
	public void setHasTypeBeforeTriggersWithOldProxies(KID typeId, boolean value)
	{
		this.triggersWithBeforeOldProxiesByTypeId.put(typeId, value);
	}
	
	/**
	 * Initialize flags that tell whether old proxies should be injected to triggers on the given type.
	 * @param typeId
	 * @param ttDao
	 * @param compiler
	 * @throws KommetException
	 */
	public void initOldProxiesOnTypeFlags(KID typeId, TypeTriggerDao ttDao, KommetCompiler compiler) throws KommetException
	{
		TypeTriggerFilter filter = new TypeTriggerFilter();
		filter.addTypeId(typeId);
		
		// clear flags
		setHasTypeAfterTriggersWithOldProxies(typeId, false);
		setHasTypeBeforeTriggersWithOldProxies(typeId, false);
		
		// find all triggers for this type
		for (TypeTrigger tt : ttDao.find(filter, this))
		{
			String triggerName = tt.getTriggerFile().getPackageName() + "." + tt.getTriggerFile().getName();
			java.lang.Class<?> triggerClass = null;
			try
			{
				triggerClass = compiler.getClass(triggerName, true, this);
			}
			catch (ClassNotFoundException e)
			{
				throw new KommetException("Trigger class " + triggerName + " not found");
			}
			
			setOldProxiesOnTypeFlag(typeId, triggerClass, tt, this);
		}
	}
	
	public void setOldProxiesOnTypeFlag(KID typeId, java.lang.Class<?> triggerClass, TypeTrigger typeTrigger, EnvData env)
	{
		if (triggerClass.isAnnotationPresent(OldValues.class) && Boolean.TRUE.equals(typeTrigger.getIsActive()))
		{
			if (triggerClass.isAnnotationPresent(AfterInsert.class) || triggerClass.isAnnotationPresent(AfterUpdate.class))
			{
				env.setHasTypeAfterTriggersWithOldProxies(typeTrigger.getTypeId(), true);
			}
			if (triggerClass.isAnnotationPresent(BeforeInsert.class) || triggerClass.isAnnotationPresent(BeforeUpdate.class))
			{
				env.setHasTypeBeforeTriggersWithOldProxies(typeTrigger.getTypeId(), true);
			}
		}
	}
	
	public void initCustomTypeButtons(ButtonService btnService) throws KommetException
	{
		this.customTypeButtons.clear();
		
		// find all buttons
		for (Button btn : btnService.get(new ButtonFilter(), AuthData.getRootAuthData(this), this))
		{
			if (!this.customTypeButtons.containsKey(btn.getTypeId()))
			{
				this.customTypeButtons.put(btn.getTypeId(), new ArrayList<Button>());
			}
			this.customTypeButtons.get(btn.getTypeId()).add(btn);
		}
	}

	public List<Button> getTypeCustomButtons(KID typeId)
	{
		List<Button> buttons = this.customTypeButtons.get(typeId);
		return buttons != null ? buttons : new ArrayList<Button>();
	}

	public Map<KID, Dictionary> getDictionaries()
	{
		return dictionaries;
	}

	public void initDictionaries(DictionaryService dictService) throws KommetException
	{
		this.dictionaries.clear();
		
		for (Dictionary d : dictService.get(new DictionaryFilter(), AuthData.getRootAuthData(this), this))
		{
			this.dictionaries.put(d.getId(), d);
		}
	}
}