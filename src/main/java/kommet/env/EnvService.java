/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.env;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.Action;
import kommet.basic.BasicSetupService;
import kommet.basic.Class;
import kommet.basic.Dictionary;
import kommet.basic.RecordAccessType;
import kommet.basic.ScheduledTask;
import kommet.basic.TypeTrigger;
import kommet.basic.UniqueCheck;
import kommet.basic.User;
import kommet.basic.View;
import kommet.basic.actions.ActionService;
import kommet.basic.keetle.LayoutService;
import kommet.basic.keetle.ViewDao;
import kommet.basic.keetle.ViewService;
import kommet.basic.types.SystemTypes;
import kommet.businessprocess.BusinessProcessService;
import kommet.config.Constants;
import kommet.dao.EnvDao;
import kommet.dao.UserDao;
import kommet.data.DataService;
import kommet.data.Env;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.TypeFilter;
import kommet.data.UniqueCheckService;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.validationrules.ValidationRuleService;
import kommet.errorlog.ErrorLogService;
import kommet.errorlog.ErrorLogSeverity;
import kommet.filters.EnvFilter;
import kommet.filters.QueryResultOrder;
import kommet.filters.UserFilter;
import kommet.koll.ClassCompilationException;
import kommet.koll.ClassFilter;
import kommet.koll.ClassService;
import kommet.koll.KollParserException;
import kommet.koll.compiler.CompilationResult;
import kommet.koll.compiler.KommetCompiler;
import kommet.labels.TextLabelService;
import kommet.scheduler.ScheduledTaskFilter;
import kommet.scheduler.ScheduledTaskService;
import kommet.services.ButtonService;
import kommet.services.DictionaryService;
import kommet.services.ReminderService;
import kommet.services.SharingRuleService;
import kommet.services.SystemActionService;
import kommet.services.SystemSettingService;
import kommet.services.UserGroupService;
import kommet.services.ViewResourceService;
import kommet.services.WebResourceService;
import kommet.triggers.TriggerService;
import kommet.utils.AppConfig;
import kommet.utils.PropertyUtilException;

@Service
public class EnvService
{
	private static final String MASTER_ENV = "master";
	public static final String MASTER_ENV_KID = "0010000000001";

	private AppConfig appConfig;

	@Inject
	private DataSourceFactory dataSourceFactory;

	@Inject
	BasicSetupService basicSetupService;

	@Inject
	ButtonService buttonService;

	@Inject
	ActionService actionService;

	@Inject
	KommetCompiler compiler;

	@Inject
	DataService dataService;

	@Inject
	ViewService viewService;

	@Inject
	ViewDao viewDao;

	@Inject
	LayoutService layoutService;

	@Inject
	EnvDao envDao;

	@Inject
	TriggerService triggerService;

	@Inject
	ValidationRuleService vrService;

	@Inject
	ClassService classService;

	@Inject
	SystemSettingService systemSettingService;

	@Inject
	ScheduledTaskService schedulerService;

	@Inject
	TextLabelService labelService;

	@Inject
	SystemActionService systemActionService;

	@Inject
	UniqueCheckService uniqueCheckService;

	@Inject
	ViewResourceService viewResourceService;

	@Inject
	WebResourceService webResourceService;

	@Inject
	SharingRuleService srService;

	@Inject
	BusinessProcessService bpService;

	@Inject
	ErrorLogService logService;

	@Inject
	UserDao userDao;

	@Inject
	ReminderService reminderService;

	@Inject
	UserGroupService ugService;

	@Inject
	DictionaryService dictionaryService;

	private static final Logger log = LoggerFactory.getLogger(EnvService.class);

	private Map<KID, EnvData> envs = new HashMap<KID, EnvData>();

	// maps the env ID to the name of the current datasource bean
	private Map<KID, String> dataSources = new HashMap<KID, String>();

	public EnvService()
	{
		//
	}

	@Inject
	public EnvService (AppConfig appConfig, DataSourceFactory dataSourceFactory) throws KommetException
	{
		this.appConfig = appConfig;
		this.dataSourceFactory = dataSourceFactory;
	}

	@Transactional(readOnly = true)
	public List<Env> find (EnvFilter filter, EnvData env) throws KommetException
	{
		return envDao.find(filter, env);
	}

	/**
	 * Returns master environment data.
	 * @return
	 * @throws KommetException
	 */
	public EnvData getMasterEnv() throws KommetException
	{
		EnvData masterEnv = this.envs.get(KID.get(MASTER_ENV_KID));
		if (masterEnv == null)
		{
			// add master env - we don't do this in the constructor, because this caused concurrent...
			add(setupMasterEnv());
			return this.envs.get(KID.get(MASTER_ENV_KID));
		}
		else
		{
			return masterEnv;
		}
	}

	/**
	 * Returns master environment object with connection to the database set up.
	 * @return
	 * @throws KommetException
	 */
	private EnvData setupMasterEnv() throws KommetException
	{
		// create the master env object
		Env masterEnv = new Env();
		masterEnv.setName(MASTER_ENV);
		masterEnv.setCreated(new Date());
		masterEnv.setKID(KID.get(MASTER_ENV_KID));

		// create data source for this env
		DataSource dataSource = dataSourceFactory.getDataSource(masterEnv.getKID(), this, appConfig.getMasterDBHost(), appConfig.getMasterDBPort(), appConfig.getMasterDB(), appConfig.getMasterDBUser(), appConfig.getMasterDBPassword(), false);

		return new EnvData(masterEnv, dataSource);
	}

	public void add (EnvData envData)
	{
		this.envs.put(envData.getEnv().getKID(), envData);
	}

	public void resetEnv(KID envId)
	{
		this.envs.remove(envId);
	}

	/**
	 * Return the current environment basic on user authentication data stored in the session.
	 * @param session
	 * @return
	 * @throws KommetException
	 */
	public EnvData getCurrentEnv(HttpSession session) throws KommetException
	{
		return get(AuthUtil.getAuthData(session).getEnvId());
	}

	public EnvData get (KID id) throws KommetException
	{
		return get(id, true, true, false, true, true, true, true, true, true, true, true, true, true, true);
	}

	/**
	 * Removes the initialized environment from cache. After this method is called, the
	 * environment will need to be initialized again in order to be used.
	 * @param envId Environment ID
	 */
	public void clear (KID envId)
	{
		envs.remove(envId);
	}

	/**
	 * Get environment data from cache, or read it in from database if it is not cached.
	 *
	 * Note that this method is synchronized which means only one instance of this method can fire at a time. Actually, we should allow one instance of this method
	 * to run per each environment, but since it is simpler to synchronize it for any environment, and it is not likely to have it called at the same time for two different
	 * environments, we implement the whole method as synchronized.
	 *
	 * @param envId
	 * @param initTypes
	 * @param initTypeProxies
	 * @param ignoreTypeProxyErrors
	 * @param initActions
	 * @param initTriggers
	 * @param initScheduledTasks
	 * @param initTextLabels
	 * @param initValidationRules
	 * @param initSharingRules
	 * @param initBusinessProcesses
	 * @param recompileClasses
	 * @param restore
	 * @return
	 * @throws KommetException
	 */
	public synchronized EnvData get (KID envId, boolean initTypes, boolean initTypeProxies, boolean ignoreTypeProxyErrors, boolean initActions, boolean initTriggers, boolean initScheduledTasks, boolean initTextLabels, boolean initValidationRules, boolean initSharingRules, boolean initBusinessProcesses, boolean recompileClasses, boolean restore, boolean initButtons, boolean initDictionaries) throws KommetException
	{
		log.debug("Looking for env " + envId);

		if (envs.containsKey(envId))
		{
			// if environment is already initialized, just return it
			return envs.get(envId);
		}

		log.debug("Initializing env " + envId);

		EnvReadResult envReadResult = readEnvConf(envId, initTypes, initTypeProxies, ignoreTypeProxyErrors, initActions, initTriggers, initTextLabels, initValidationRules, initSharingRules, initBusinessProcesses, recompileClasses, initButtons, initDictionaries);
		EnvData env = envReadResult.getEnv();

		if (!initTypeProxies)
		{
			// without type proxies not further initialization is possible
			log.debug("Partial env initialization finished");
			return env;
		}

		List<Class> classes = classService.getClasses(null, env);

		// Create proxies for all types anew.
		// Since basic types such as User or Profile are duplicated in the user's package,
		// they need to be compiled as well. This is why we just go on creating proxies for all types
		if (restore)
		{
			log.debug("Restoring standard controllers");
			dataService.createStandardControllers(appConfig.isGenerateControllerCodeAnew(), env.getAllTypes(), AuthData.getRootAuthData(env), env);

			log.debug("Restoring classes");

			alignJavaPackageWithEnv(classes, envReadResult.getClassesWithCompileErrors(), env);

			List<Class> classesWithoutErrors = new ArrayList<Class>();
			for (Class cls : classes)
			{
				if (!envReadResult.getClassesWithCompileErrors().contains(cls.getId()))
				{
					classesWithoutErrors.add(cls);
				}
			}

			// get all KOLL files on the env and recompile them
			CompilationResult result = compiler.compile(classesWithoutErrors, env);
			if (!result.isSuccess())
			{
				logService.log("Compilation error during env startup: " + result.getDescription(), ErrorLogSeverity.FATAL, this.getClass().getName(), -1, AuthData.getRootAuthData(env).getUserId(), AuthData.getRootAuthData(env), env);
				throw new ClassCompilationException("Error compiling classes from database", result);
			}

 			log.debug("Restoring proxies for basic types");
			for (Type type : env.getAllTypes())
			{
				dataService.updateTypeProxy(type, AuthData.getRootAuthData(env), env);
			}

			log.debug("Restoring view dir");
			viewService.initKeetleDir(env, true);
			log.debug("Restoring layout dir");
			layoutService.initLayoutDir(env, false);
			log.debug("Restoring view resources");
			viewResourceService.initViewResourcesOnDisk(env, true);
		}

		// read in all KOLL files into the env's class loader

		log.debug("Initializing generic actions");
		env.initGenericActions(classes, envReadResult.getClassesWithCompileErrors(), compiler, systemActionService.getSystemActionURLs(), null, viewDao, logService);

		if (initScheduledTasks)
		{
			log.debug("Scheduling tasks");
			List<ScheduledTask> tasks = schedulerService.get(new ScheduledTaskFilter(), env);
			for (ScheduledTask task : tasks)
			{
				// First unschedule a job for this task that may exist.
				// A job may already exist if the environment cache was cleared without the server
				// being restarted, in which case any scheduled jobs will be there.
				schedulerService.unschedule(task.getId(), false, env);

				schedulerService.createScheduler(task, basicSetupService.getRootAuthData(env), env);
			}

			try
			{
				reminderService.scheduleReminderChecker(env);
			}
			catch (SchedulerException e)
			{
				e.printStackTrace();
				throw new KommetException("Error scheduling reminder checker: " + e.getMessage());
			}

			try
			{
				ugService.scheduleUgaApplier(env);
			}
			catch (SchedulerException e)
			{
				e.printStackTrace();
				throw new KommetException("Error scheduling sharing applier: " + e.getMessage());
			}

			try
			{
				ugService.scheduleUgaRemover(env);
			}
			catch (SchedulerException e)
			{
				e.printStackTrace();
				throw new KommetException("Error scheduling sharing remover: " + e.getMessage());
			}
		}

		// cache view resources in the env object
		env.initViewResources(viewResourceService);

		env.initWebResources(webResourceService);

		env.setTimeInitialized((new Date()).getTime());
		this.envs.put(env.getEnv().getKID(), env);

		log.debug("Env initialization finished [" + envId + "]");
		return env;
	}

	/**
	 * Some classes may have environment name from a different environment hardcoded in the Java package. This happens when an env is imported
	 * to a different env ID. We need to generate the Java code anew for those classes to prevent inconsistent env IDs.
	 *
	 * This happens e.g. for the QueryUniqueAcion business action.
	 *
	 * @param classes
	 * @param classesWithCompileErrors
	 * @param env
	 * @throws KommetException
	 */
	private void alignJavaPackageWithEnv(List<Class> classes, Set<KID> classesWithCompileErrors, EnvData env) throws KommetException
	{
		for (Class cls : classes)
		{
			if (classesWithCompileErrors.contains(cls.getId()))
			{
				// skip classes which we know contain compile errors
				continue;
			}

			if (!cls.getAccessType().equals(RecordAccessType.SYSTEM_IMMUTABLE) && cls.getJavaCode().trim().startsWith("package kommet.envs."))
			{
				cls.setJavaCode(null);

				// this is a silent update that does not change the last modified- properties of the class
				try
				{
				classService.fullSave(cls, dataService, false, true, AuthData.getRootAuthData(env), env);
			}
				catch (Exception e)
				{
					
				}
			}
		}
	}

	/**
	 * This method read in environment configuration from database.
	 * @param envId the ID of the environment
	 * @param initTypes tells whether types should be initialized
	 * @param initActions tells whether pages should be initialized
	 * @param initTriggers tells whether triggers should be initialized
	 * @param initBusinessProcesses
	 * @return
	 * @throws KommetException
	 */
	private EnvReadResult readEnvConf (KID envId, boolean initTypes, boolean initTypeProxies, boolean ignoreTypeProxyErrors, boolean initActions, boolean initTriggers, boolean initTextLabels, boolean initValidationRules, boolean initSharingRules, boolean initBusinessProcesses, boolean recompileClasses, boolean initButtons, boolean initDictionaries) throws KommetException
	{
		EnvFilter filter = new EnvFilter();
		filter.setKID(envId);
		List<Env> envs = envDao.find(filter, getMasterEnv());

		if (envs.isEmpty())
		{
			throw new KommetException("Environment with ID " + envId + " not found");
		}

		if (envs.size() > 1)
		{
			throw new KommetException("More than one environment found with ID " + envId);
		}

		// create a data source to connect to this environment's database
		DataSource dataSource = dataSourceFactory.getDataSource(envId, this, appConfig.getEnvDBHost(), appConfig.getEnvDBPort(), envs.get(0).getDBName(), appConfig.getEnvDBUser(), appConfig.getEnvDBPassword(), false);

		// instantiate environment object
		EnvData envData = new EnvData(envs.get(0), dataSource);
		envData.setCompileClassPath(compiler.getCompileClassPath(envId));

		// environment may or may not be initialized when this method is run
		boolean envInitialized = false;

		alignEnvs(envData);

		if (initTypes)
		{
			// TODO fix the method below because it gets unique check type only by its API name
			// not the package, which may result in a wrong type being returned if there is another
			// type called UniqueCheck in another package

			// first read in the unique check type, because other type definitions depend on it
			Type uniqueCheckType = dataService.getTypeByName(AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.UNIQUE_CHECK_API_NAME, true, envData);

			if (uniqueCheckType == null)
			{
				throw new KommetException("UniqueCheck type does not exist. It must be created on the environment before env metadata can be read in from database.");
			}

			// insert type mapping into the env's data
			envData.addTypeMapping(uniqueCheckType);
			// register type with the global store
			envData.registerType(uniqueCheckType);

			// Read in all type metadata for the given environment.
			// Unique checks cannot be read in at this stage because the env has no information about them.
			// Types should be read in in order of creation so that all dependencies between them are satisfied.

			TypeFilter typeFilter = new TypeFilter();
			typeFilter.setOrder(QueryResultOrder.ASC);
			typeFilter.setOrderBy("id");
			List<Type> allTypesOnEnv = dataService.getTypes(typeFilter, true, true, envData);

			envInitialized = !allTypesOnEnv.isEmpty();

			// only after all types are read in (and the UniqueCheck object among them), can unique checks be read in for these types
			for (Type type : allTypesOnEnv)
			{
				// insert the type mapping into the env's data
				envData.addTypeMapping(type);

				// register the object with the global store
				envData.registerType(type);
			}

			// init all unique checks on types
			List<UniqueCheck> uniqueChecks = uniqueCheckService.find(null, envData, dataService);
			for (UniqueCheck uc : uniqueChecks)
			{
				envData.getType(uc.getTypeId()).addUniqueCheck(uc);
			}

			if (initTypeProxies)
			{
				envData.scanForPersistenceMappings("kommet", ignoreTypeProxyErrors);

				log.debug("Updating type proxies");

				// Generate type proxies, but do not generate properties that reference other types
				// because proxy classes for those types may not exist yet as the env is not yet fully initialized.
				// The reason why we need to initialize such incomplete proxies in the first place is that they will
				// be used to add full proxies (those that reference other proxies) in the next step.
				dataService.updateTypeProxy(allTypesOnEnv, false, AuthData.getRootAuthData(envData), envData);

				// now generate and compile all proxies again, but this time with all properties
				dataService.updateTypeProxy(allTypesOnEnv, AuthData.getRootAuthData(envData), envData);
			}
		}

		if (initActions)
		{
			// find all pages and add them to the environment
			List<Action> actions = actionService.getActions(null, envData);
			for (Action action : actions)
			{
				envData.addAction(action.getUrl(), action);
			}

			// also, add all views to the env cache - some have already been added when addAction was called, but some not
			for (View view : viewService.getAllViews(envData))
			{
				envData.addView(view);
			}
		}

		if (initTypes)
		{
			// find root user
			List<Record> adminUsers = envData.getSelectCriteriaFromDAL("select id, userName, email, profile.id from " + SystemTypes.USER_API_NAME + " where id = '" + envs.get(0).getAdminId().getId() + "' LIMIT 1").list();
			if (adminUsers.isEmpty())
			{
				throw new KommetException("Admin user with ID " + envs.get(0).getAdminId() + " not found on env " + envData.getName());
			}
			envData.setRootUser(adminUsers.get(0));

			// find guest user
			UserFilter userFilter = new UserFilter();
			userFilter.setUsername(Constants.UNAUTHENTICATED_USER_NAME);
			List<User> guestUsers = userDao.get(userFilter, envData);

			if (guestUsers.isEmpty())
			{
				throw new KommetException("Guest user not found on env");
			}
			envData.setGuestUser(guestUsers.get(0));
		}

		Set<KID> classesWithCompileErrors = new HashSet<KID>();

		if (initTriggers)
		{
			//boolean allClassesRecompiled = false;

			// get all trigger-type assignments
			List<TypeTrigger> typeTriggers = triggerService.find(null, envData);
			for (TypeTrigger typeTrigger : typeTriggers)
			{
				// register trigger on the environment
				envData.registerTrigger(typeTrigger);

				String triggerName = typeTrigger.getTriggerFile().getPackageName() + "." + typeTrigger.getTriggerFile().getName();

				java.lang.Class<?> triggerClass = null;
				try
				{
					triggerClass = compiler.getClass(triggerName, true, envData);
					envData.setOldProxiesOnTypeFlag(typeTrigger.getTypeId(), triggerClass, typeTrigger, envData);
				}
				catch (ClassNotFoundException e)
				{
					classesWithCompileErrors.add(typeTrigger.getTriggerFile().getId());
				}
			}
		}

		if (initValidationRules)
		{
			vrService.initValidationRulesOnEnv(envData);
		}

		if (initTextLabels)
		{
			labelService.initTextLabels(envData);
		}

		if (initSharingRules)
		{
			srService.initSharingRules(envData);
		}

		if (initBusinessProcesses)
		{
			bpService.initTriggerableProcesses(envData);

			// recompile business action files
			/*List<BusinessAction> businessActions = bpService.get(new BusinessActionFilter(), AuthData.getRootAuthData(envData), envData);

			for (BusinessAction action : businessActions)
			{
				if (action.getFile() != null)
				{
					// TODO we need to update the package in the Java code, because if the database was moved from an environment with another ID
					// this envId will be hardcoded in the Java code package name
					classService.updateJavaCode(classService.getClass(action.getFile().getId(), envData), dataService, AuthData.getRootAuthData(envData), envData);
				}
			}*/
		}

		if (initButtons)
		{
			envData.initCustomTypeButtons(buttonService);
		}

		if (initDictionaries)
		{
			// dictionaries must be initialized before types, because when we initialize enum fields, we are using the initialized dictionaries
			envData.initDictionaries(dictionaryService);

			// reinitialize dictionaries on enum fields
			for (Type type : envData.getAllTypes())
			{
				for (Field field : type.getFields())
				{
					if (field.getDataTypeId().equals(DataType.ENUMERATION) && ((EnumerationDataType)field.getDataType()).getDictionary() != null)
					{
						Dictionary dict = envData.getDictionaries().get(((EnumerationDataType)field.getDataType()).getDictionary().getId());
						if (dict == null)
						{
							throw new KommetException("Dictionary " + ((EnumerationDataType)field.getDataType()).getDictionary().getId() + " referenced by field " + field.getApiName() + " not found");
						}
						((EnumerationDataType)field.getDataType()).setDictionary(dict);
					}
				}
			}
		}

		log.debug("Compiling classes");

		if (recompileClasses)
		{
			ClassFilter classFilter = new ClassFilter();
			classFilter.setSystemFile(false);

			List<Class> clss = classService.getClasses(classFilter, envData);

			log.debug("Found " + clss.size() + " classes");

			for (Class cls : clss)
			{
				log.debug("Compiling " + cls.getName());

				// find only classes with public access type, because those with SYSTEM_IMMUTABLE cannot be modified and an error would be thrown
				if (RecordAccessType.SYSTEM_IMMUTABLE.getId() != cls.getAccessType())
				{
					try
					{
						classService.updateJavaCode(cls, dataService, AuthData.getRootAuthData(envData), envData);
					}
					catch (ClassCompilationException e)
					{
						classesWithCompileErrors.add(cls.getId());
						log.error("Compilation error during env startup (class " + cls.getQualifiedName() + "): " + e.getCompilationResult().getDescription());
						logService.log("Compilation error during env startup: " + e.getCompilationResult().getDescription(), ErrorLogSeverity.FATAL, this.getClass().getName(), -1, AuthData.getRootAuthData(envData).getUserId(), AuthData.getRootAuthData(envData), envData);
					}
					catch (KollParserException e)
					{
						classesWithCompileErrors.add(cls.getId());
						log.error("Compilation error during env startup (class " + cls.getQualifiedName() + "): " + e.getMessage());
						logService.log("Compilation error during env startup: " + e.getMessage(), ErrorLogSeverity.FATAL, this.getClass().getName(), -1, AuthData.getRootAuthData(envData).getUserId(), AuthData.getRootAuthData(envData), envData);
					}
					catch (ClassCircularityError e)
					{
						classesWithCompileErrors.add(cls.getId());
						log.error("Compilation error during env startup (class " + cls.getQualifiedName() + "): " + e.getMessage());
						logService.log("Compilation error during env startup: " + e.getMessage(), ErrorLogSeverity.FATAL, this.getClass().getName(), -1, AuthData.getRootAuthData(envData).getUserId(), AuthData.getRootAuthData(envData), envData);
					}
				}
			}
		}
		else if (initBusinessProcesses)
		{
			// we need to update the package in the Java code of action classes, because if the database was moved from an environment with another ID
			// this envId will be hardcoded in the Java code package name
			throw new KommetException("When business processes are initialized on env, classes must be recompiled");
		}

		log.debug("Compiling classes done");

		if (envInitialized)
		{
			log.debug("Reloading system settings");
			systemSettingService.reloadSystemSettings(envData);
			log.debug("Reloading system settings done");
		}

		EnvReadResult result = new EnvReadResult();
		result.setEnv(envData);
		result.setClassesWithCompileErrors(classesWithCompileErrors);

		return result;
	}

	private void alignEnvs(EnvData env) throws KommetException
	{
		log.debug("Aligning environments");

		EnvAlignmentUtil.alignRememberMe(this.dataService, log, env);
		EnvAlignmentUtil.alignUgaIsPendingApply(this.dataService, log, env);
		EnvAlignmentUtil.alignUgaIsPendingRemove(this.dataService, log, env);

		log.debug("Env aligned");
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public EnvData createEnv(String envName, KID envId, boolean isInitEnv) throws DataAccessException, KIDException, PropertyUtilException, KommetException
	{
		return createEnv(envName, envId, isInitEnv, false);
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public EnvData createEnv(String envName, KID envId, boolean isInitEnv, boolean isDisconnectTemplateDb) throws DataAccessException, KIDException, PropertyUtilException, KommetException
	{
		return createEnv(envName, envId, isInitEnv, isDisconnectTemplateDb, appConfig.getTemplateEnvDb());
	}

	/**
	 * Creates a new environment.
	 * @param newEnvName
	 * @param newEnvId
	 * @return
	 * @throws KommetException
	 * @throws PropertyUtilException
	 * @throws KIDException
	 * @throws DataAccessException
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public EnvData createEnv(String envName, KID envId, boolean isInitEnv, boolean isDisconnectTemplateDb, String template) throws DataAccessException, KIDException, PropertyUtilException, KommetException
	{
		if (!StringUtils.hasText(envName))
		{
			throw new KommetException("Cannot create env with empty name");
		}

		// make sure that an env with the given name does not exist
		if (getMasterEnv().getJdbcTemplate().queryForObject("SELECT count(id) FROM envs WHERE name = '" + envName + "'", Integer.class) > 0)
		{
			throw new EnvAlreadyExistsException("Environment with name " + envName + " already exists");
		}

		// make sure that an env with the given ID does not exist
		if (getMasterEnv().getJdbcTemplate().queryForObject("SELECT count(id) FROM envs WHERE kid = '" + envId.getId() + "'", Integer.class) > 0)
		{
			throw new KommetException("Environment with ID " + envId + " already exists");
		}

		// make sure that the env database does not exist
		if (getMasterEnv().getJdbcTemplate().queryForObject("SELECT count(*) from pg_database WHERE datname= 'env" + envId + "'", Integer.class) > 0)
		{
			throw new KommetException("Environment database env" + envId + " already exists");
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
		String createdDate = sdf.format(new Date());
		// insert the env into the master database
		getMasterEnv().getJdbcTemplate().execute("INSERT INTO envs (name, kid, adminid, created) VALUES ('" + envName + "', '" + envId.getId() + "', '" + appConfig.getDefaultRootUserIdForNewEnvs().getId() + "', '" + createdDate + "')");

		String createDbSql = "";

		if (isDisconnectTemplateDb)
		{
			createDbSql += "SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity	WHERE pg_stat_activity.datname = '" + template + "' AND pid <> pg_backend_pid();";
		}

		// now create the env database by copying a template database
		createDbSql += "CREATE DATABASE env" + envId + " WITH OWNER = " + appConfig.getNewEnvDbOwner() + " ENCODING = 'UTF8'";
		createDbSql += " TEMPLATE " + template;
		createDbSql += " TABLESPACE = pg_default CONNECTION LIMIT = -1;";

		getMasterEnv().getJdbcTemplate().execute(createDbSql);

		EnvReadResult envReadResult = readEnvConf(envId, false, false, false, false, false, false, false, false, false, false, false, false);

		if (isInitEnv)
		{
			basicSetupService.runBasicSetup(envReadResult.getEnv());
		}

		return envReadResult.getEnv();
	}

	/**
	 * Remove the env and its associated database.
	 *
	 * Do not execute transactionally, because database cannot be dropped in transacation
	 * @param newenvname
	 * @throws KommetException
	 * @throws DataAccessException
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void deleteEnv(KID envId) throws DataAccessException, KommetException
	{
		// force close open connections to this database
		String disconnectQuery = "SELECT pg_terminate_backend(pg_stat_activity.pid) FROM pg_stat_activity WHERE pg_stat_activity.datname = 'env" + envId + "' AND pid <> pg_backend_pid();";
		getMasterEnv().getJdbcTemplate().execute(disconnectQuery);

		getMasterEnv().getJdbcTemplate().execute("DELETE FROM envs WHERE kid = '" + envId + "'");
		getMasterEnv().getJdbcTemplate().execute("DROP DATABASE env" + envId);

		// delete the data source
		this.dataSourceFactory.removeDataSource(envId, this);
		this.dataSources.remove(envId);
	}

	/**
	 * Remove the env and its associated database.
	 *
	 * Do not execute transactionally, because database cannot be dropped in transacation
	 * @param newenvname
	 * @throws KommetException
	 * @throws DataAccessException
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public boolean deleteEnv (String envName) throws DataAccessException, KommetException
	{
		Integer envCount = getMasterEnv().getJdbcTemplate().queryForObject("SELECT count(id) FROM envs WHERE name = '" + envName + "'", Integer.class);

		if (envCount == 0)
		{
			return false;
		}

		String envId = getMasterEnv().getJdbcTemplate().queryForObject("SELECT kid FROM envs WHERE name = '" + envName + "'", String.class);
		deleteEnv(KID.get(envId));
		return true;
	}

	public void reconnectToDatabase (EnvData env) throws KommetException
	{
		EnvFilter filter = new EnvFilter();
		filter.setKID(env.getId());
		List<Env> envs = envDao.find(filter, getMasterEnv());

		DataSource ds = dataSourceFactory.getDataSource(env.getId(), this, appConfig.getEnvDBHost(), appConfig.getEnvDBPort(), envs.get(0).getDBName(), appConfig.getEnvDBUser(), appConfig.getEnvDBPassword(), true);
		env.setJdbcTemplate(new JdbcTemplate(ds));
	}

	public void setDataSource (KID envId, String name)
	{
		this.dataSources.put(envId, name);
	}

	public String getDataSourceName (KID envId)
	{
		return this.dataSources.get(envId);
	}

	class EnvReadResult
	{
		private EnvData env;
		private Set<KID> classesWithCompileErrors;

		public Set<KID> getClassesWithCompileErrors()
		{
			return classesWithCompileErrors;
		}

		public void setClassesWithCompileErrors(
				Set<KID> classesWithCompileErrors)
		{
			this.classesWithCompileErrors = classesWithCompileErrors;
		}

		public EnvData getEnv()
		{
			return env;
		}

		public void setEnv(EnvData env)
		{
			this.env = env;
		}
	}
}