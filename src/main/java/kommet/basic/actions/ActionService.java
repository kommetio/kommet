/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.basic.Action;
import kommet.basic.Profile;
import kommet.basic.RecordAccessType;
import kommet.basic.RecordProxyUtil;
import kommet.basic.StandardAction;
import kommet.basic.TypeInfo;
import kommet.basic.keetle.ViewService;
import kommet.basic.types.SystemTypes;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.OperationResult;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.TypeInfoFilter;
import kommet.data.TypeInfoService;
import kommet.env.EnvData;
import kommet.env.GenericAction;
import kommet.koll.ClassService;
import kommet.koll.compiler.KommetCompiler;
import kommet.services.SystemActionService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.ValidationUtil;

@Service
public class ActionService
{
	@Inject
	ActionDao actionDao;
	
	@Inject
	DataService dataService;
	
	@Inject
	StandardActionDao stdActionDao;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	TypeInfoService typeInfoService;
	
	@Inject
	SystemActionService systemActionService;
	
	@Inject
	ClassService kollService;
	
	@Inject
	ViewService keetleService;
	
	@Inject
	AppConfig appConfig;
	
	private static final Logger log = LoggerFactory.getLogger(ActionService.class);
	
	/**
	 * Sets the action as a standard page for the given type and the given profile.
	 * Type and profile records must be saved. Page record may or may not be saved - if it is not,
	 * it will be saved by this method.
	 * @param obj
	 * @param page
	 * @param profile
	 * @param actionType
	 * @throws KommetException 
	 */
	@Transactional
	public StandardAction setStandardAction (KID typeId, KID actionId, KID profileId, StandardActionType actionType, RecordAccessType accessType, AuthData authData, EnvData env) throws KommetException
	{
		if (typeId == null)
		{
			throw new KommetException("Error setting standard action. Type ID is null.");
		}
		
		if (actionId == null)
		{
			throw new KommetException("Error setting standard action. Action ID is null.");
		}
		
		if (profileId == null)
		{
			throw new KommetException("Error setting standard action. Profile ID is null.");
		}
		
		if (actionType == null)
		{
			throw new KommetException("Error setting standard action. Action Type is null.");
		}
		
		if (authData == null)
		{
			throw new KommetException("Error setting standard action. Auth data is null.");
		}
		
		if (env == null)
		{
			throw new KommetException("Error setting standard action. EnvData is null.");
		}
		
		Type pageKObj = env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.STANDARD_ACTION_API_NAME));
		
		if (pageKObj == null)
		{
			throw new KommetException("No object found with API name " + SystemTypes.STANDARD_ACTION_API_NAME);
		}
		
		// check if standard pages for this profile, object type and page type does not exist
		Record stdAction = getStandardActionRecordForTypeAndProfile(typeId, profileId, actionType, env);
		
		if (stdAction == null)
		{
			stdAction = new Record(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.STANDARD_ACTION_API_NAME)));
			stdAction.setField("type", actionType.getStringValue());
			stdAction.setField("profile.id", profileId, env);
			stdAction.setField("typeId", typeId);
			stdAction.setField(Field.ACCESS_TYPE_FIELD_NAME, accessType.getId());
		}
		
		stdAction.setField("action.id", actionId, env);
		stdAction = dataService.save(stdAction, true, true, authData, env);
		
		return new StandardAction(stdAction, true, env);
	}
	
	/**
	 * Sets the page as a standard page for the given object and the given profile.
	 * Object and profile records must be saved. Page record may or may not be saved - if it is not,
	 * it will be saved by this method.
	 * @param obj
	 * @param page
	 * @param profile
	 * @param actionType
	 * @throws KommetException 
	 */
	@Transactional
	public StandardAction setStandardAction (KID typeId, Action page, KID profileId, StandardActionType actionType, AuthData authData, EnvData env) throws KommetException
	{
		Type pageKObj = env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.STANDARD_ACTION_API_NAME));
		
		if (pageKObj == null)
		{
			throw new KommetException("No object found with API name " + SystemTypes.STANDARD_ACTION_API_NAME);
		}
		
		Record stdAction = getStandardActionRecordForTypeAndProfile(typeId, profileId, actionType, env); 
		
		if (stdAction == null)
		{
			stdAction = new Record(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.STANDARD_ACTION_API_NAME)));
			stdAction.setField("type", actionType.getStringValue());
			stdAction.setField("profile.id", profileId, env);
			stdAction.setField("typeId", typeId);
		}
		
		stdAction.setField("action", RecordProxyUtil.generateRecord(page, env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.ACTION_API_NAME)), 2, env));
		stdAction = dataService.save(stdAction, env);
		
		StandardAction standardAction = new StandardAction(stdAction, true, env);//, env.getType(SystemObjects.getSystemObjectQualifiedName(SystemObjects.STANDARD_PAGE_API_NAME)).getKID());
		return stdActionDao.save(standardAction, authData, env);
	}
	
	@Transactional (readOnly = true)
	public Record getStandardActionRecordForTypeAndProfile (KID typeId, KID profileId, StandardActionType type, EnvData env) throws KommetException
	{
		List<Record> stdPages = env.getSelectCriteriaFromDAL("select " + StandardActionDao.STANDARD_ACTION_SELECT_FIELDS + " from " + SystemTypes.STANDARD_ACTION_API_NAME + " where profile.id = '" + profileId + "' and typeId = '" + typeId + "' and type = '" + type.getStringValue() + "'").list();
		
		if (stdPages.size() > 1)
		{
			throw new KommetException("More than one standard action assigned to profile " + profileId + ", object " + typeId + ", type " + type.getStringValue());
		}
		
		return stdPages.isEmpty() ? null : stdPages.get(0);
	}
	
	@Transactional (readOnly = true)
	public StandardAction getStandardActionForTypeAndProfile (KID typeId, KID profileId, StandardActionType type, EnvData env) throws KommetException
	{
		Record stdAction = getStandardActionRecordForTypeAndProfile(typeId, profileId, type, env);
		return stdAction == null ? null : new StandardAction(stdAction, env);
	}
	
	@Transactional
	public List<StandardAction> findStandardActions (StandardActionFilter filter, EnvData env) throws KommetException
	{
		return stdActionDao.find(filter, env);
	}

	@Transactional(readOnly = true)
	public List<StandardAction> getStandardActionsForType(KID typeId, EnvData env) throws KommetException
	{	
		List<Record> stdActions = env.getSelectCriteriaFromDAL("select " + StandardActionDao.STANDARD_ACTION_SELECT_FIELDS + " from " + SystemTypes.STANDARD_ACTION_API_NAME + " where typeId = '" + typeId + "'").list();
		List<StandardAction> actions = new ArrayList<StandardAction>();
		
		for (Record r : stdActions)
		{
			actions.add(new StandardAction(r, true, env));
		}
		
		return actions;
	}
	
	@Transactional(readOnly = true)
	public List<Action> getActions (ActionFilter filter, EnvData env) throws KommetException
	{
		return actionDao.find(filter, env);
	}

	@Transactional(readOnly = true)
	public Action getAction(KID id, EnvData env) throws KommetException
	{	
		if (id == null)
		{
			throw new KommetException("Attempt to get action by null ID");
		}
		
		return actionDao.get(id, env);
	}
	
	@Transactional
	public Action saveOnEnv (Action action, String oldUrl, AuthData authData, EnvData env) throws KommetException
	{
		return saveOnEnv(action, oldUrl, false, authData, env);
	}
	
	/**
	 * Saves the action object to the database and adds its definition to the environment.
	 * @param action page object to be saved
	 * @param oldUrl the URL at which the page was previously accessible (if changed and page is not new)
	 * @param userId 
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional
	public Action saveOnEnv (Action action, String oldUrl, boolean saveControllerAndView, AuthData authData, EnvData env) throws KommetException
	{
		String actionURL = MiscUtils.trim(action.getUrl(), '/');
		
		// make sure this URL is not used in any system action
		if (systemActionService.getSystemActionURLs().contains(actionURL))
		{
			throw new ActionCreationException("URL " + actionURL + " is reserved for a system action", ActionCreationException.ERR_CODE_RESERVED_URL);
		}
		
		// make sure an action with this URL does not already exist, or if it does,
		// that it is the same action (with the same ID) as the one being added
		Action existingAction = env.getActionForUrl(actionURL);
		if (existingAction != null && (existingAction.getId() == null || !existingAction.getId().equals(action.getId())))
		{
			throw new ActionCreationException("Another action with URL " + actionURL + " already exists", ActionCreationException.ERR_CODE_DUPLICATE_REGISTERED_ACTION_URL);
		}
		
		GenericAction genericAction = env.getGenericAction(actionURL); 
		
		// make sure this URL is not used by any generic action
		if (genericAction != null)
		{
			throw new ActionCreationException("URL " + actionURL + " is already used by a REST service declared in class " + genericAction.getControllerName(), ActionCreationException.ERR_CODE_DUPLICATE_GENERIC_ACTION_URL);
		}
		
		boolean storeViewOnDisk = false;
		
		if (saveControllerAndView)
		{
			if (action.getController().getId() == null)
			{
				// save controller file
				kollService.fullSave(action.getController(), dataService, authData, env);
			}
			if (action.getView().getId() == null)
			{
				// save controller file
				keetleService.save(action.getView(), appConfig, authData, env);
				storeViewOnDisk = true;
			}
		}
		
		Action savedAction = actionDao.save(action, authData, env);
		
		// register the action with the environment
		env.addAction(savedAction.getUrl(), oldUrl, savedAction);
		
		// store view on disk
		if (storeViewOnDisk)
		{
			keetleService.storeView(action.getView(), env);
		}
		
		return savedAction;
	}

	@Transactional
	public Action save (Action action, AuthData authData, EnvData env) throws KommetException
	{
		if (!ValidationUtil.isValidOptionallyQualifiedResourceName(action.getName()))
		{
			throw new ActionCreationException("Invalid action name " + action.getName(), ActionCreationException.ERR_CODE_OTHER);
		}
		return actionDao.save(action, authData, env);
	}

	/**
	 * Get the details page used for viewing records of a given object type by a given profile.
	 * @param type
	 * @param profile
	 * @return
	 * @throws KommetException 
	 */
	@Transactional(readOnly = true)
	public StandardAction getStandardDetailsAction(Type type, Profile profile, EnvData env) throws KommetException
	{
		List<Record> actions = env.getSelectCriteriaFromDAL("select " + StandardActionDao.STANDARD_ACTION_SELECT_FIELDS + " from " + SystemTypes.STANDARD_ACTION_API_NAME + " where typeId = '" + type.getKID() + "' AND type = '" + StandardActionType.VIEW.getStringValue() + "' AND profile.id = '" + profile.getId().getId() + "'").list();
		
		if (actions.size() > 1)
		{
			throw new KommetException("More than one standard details action found for type " + type.getApiName() + " and profile " + profile.getName());
		}
		
		return actions.isEmpty() ? null : (StandardAction)RecordProxyUtil.generateStandardTypeProxy(actions.get(0), true, env, compiler);
	}
	
	/**
	 * Get the edit page used for viewing records of a given object type by a given profile.
	 * @param type
	 * @param profile
	 * @return
	 * @throws KommetException 
	 */
	@Transactional(readOnly = true)
	public StandardAction getStandardEditAction(Type type, Profile profile, EnvData env) throws KommetException
	{
		List<Record> actions = env.getSelectCriteriaFromDAL("select " + StandardActionDao.STANDARD_ACTION_SELECT_FIELDS + " from " + SystemTypes.STANDARD_ACTION_API_NAME + " where typeId = '" + type.getKID() + "' AND type = '" + StandardActionType.EDIT.getStringValue() + "' AND profile.id = '" + profile.getId().getId() + "'").list();
		
		if (actions.size() > 1)
		{
			throw new KommetException("More than one standard edit action found for type " + type.getApiName() + " and profile " + profile.getName());
		}
		
		return actions.isEmpty() ? null : (StandardAction)RecordProxyUtil.generateStandardTypeProxy(actions.get(0), true, env, compiler);
	}
	
	@Transactional(readOnly = true)
	public StandardAction getStandardCreateAction(Type type, Profile profile, EnvData env) throws KommetException
	{
		List<Record> actions = env.getSelectCriteriaFromDAL("select " + StandardActionDao.STANDARD_ACTION_SELECT_FIELDS + " from " + SystemTypes.STANDARD_ACTION_API_NAME + " where typeId = '" + type.getKID() + "' AND type = '" + StandardActionType.CREATE.getStringValue() + "' AND profile.id = '" + profile.getId().getId() + "'").list();
		
		if (actions.size() > 1)
		{
			throw new KommetException("More than one standard edit page found for object " + type.getApiName() + " and profile " + profile.getName());
		}
		
		return actions.isEmpty() ? null : (StandardAction)RecordProxyUtil.generateStandardTypeProxy(actions.get(0), true, env, compiler);
	}
	
	/**
	 * Get the list action used for viewing records of a given object type by a given profile.
	 * @param type
	 * @param profile
	 * @return
	 * @throws KommetException 
	 */
	@Transactional(readOnly = true)
	public StandardAction getStandardListAction(Type type, Profile profile, EnvData env) throws KommetException
	{
		List<Record> actions = env.getSelectCriteriaFromDAL("select " + StandardActionDao.STANDARD_ACTION_SELECT_FIELDS + " from " + SystemTypes.STANDARD_ACTION_API_NAME + " where typeId = '" + type.getKID() + "' AND type = '" + StandardActionType.LIST.getStringValue() + "' AND profile.id = '" + profile.getId().getId() + "'").list();
		
		if (actions.size() > 1)
		{
			throw new KommetException("More than one standard list action found for type " + type.getApiName() + " and profile " + profile.getName());
		}
		
		return actions.isEmpty() ? null : (StandardAction)RecordProxyUtil.generateStandardTypeProxy(actions.get(0), true, env, compiler);
	}

	@Transactional
	public void deleteStandardActions(List<StandardAction> actions, EnvData env) throws KommetException
	{
		stdActionDao.delete(actions, null, env);
	}
	
	@Transactional
	public OperationResult delete(List<Action> actions, AuthData authData, EnvData env) throws KommetException
	{
		return delete(actions, false, authData, env);
	}
	
	@Transactional
	public OperationResult delete(Action page, boolean reassignStandardActions, AuthData authData, EnvData env) throws KommetException
	{
		List<Action> actions = new ArrayList<Action>();
		actions.add(page);
		return delete(actions, reassignStandardActions, authData, env);
	}

	@Transactional
	public OperationResult delete(List<Action> actions, boolean reassignStandardActions, AuthData authData, EnvData env) throws KommetException
	{
		// check if any standard pages use any of these pages
		StandardActionFilter filter = new StandardActionFilter();
		
		for (Action action : actions)
		{
			filter.addActionId(action.getId());
		}
		
		List<StandardAction> stdActions = findStandardActions(filter, env);
		
		if (!stdActions.isEmpty())
		{
			if (reassignStandardActions)
			{
				log.debug("[Delete action] Found " + stdActions.size() + " referencing the deleted action - reassigning to default");
				Map<KID, TypeInfo> typeInfosByTypeId = new HashMap<KID, TypeInfo>();
				TypeInfoFilter tiFilter = new TypeInfoFilter();
				
				// for every standard page using the deleted page, reassign the default page for the given action and profile
				for (StandardAction stdAction : stdActions)
				{
					tiFilter.addTypeId(stdAction.getTypeId());
				}
				
				// find type info for types
				List<TypeInfo> typeInfos = typeInfoService.find(tiFilter, env);
				for (TypeInfo ti : typeInfos)
				{
					typeInfosByTypeId.put(ti.getTypeId(), ti);
				}
				
				for (StandardAction action : stdActions)
				{
					// assign the default page
					action.setAction(typeInfosByTypeId.get(action.getTypeId()).getDefaultAction(action.getStandardPageType()));
					// TODO this is not optimal - a separate query for each page
					stdActionDao.save(action, authData, env);
				}
			}
			else
			{
				log.debug("[Delete action] Found " + stdActions.size() + " referencing the deleted page - aborting delete");
				return new OperationResult(false, "Cannot delete action because there are standard actions referencing it");
			}
		}
		
		actionDao.delete(actions, authData, env);
		return new OperationResult(true, null);
	}

	@Transactional
	public void deleteStandardAction(StandardAction stdAction, EnvData env) throws KommetException
	{
		List<StandardAction> actions = new ArrayList<StandardAction>();
		actions.add(stdAction);
		stdActionDao.delete(actions, null, env);
	}
	
	@Transactional
	public void deleteAction(Action action, EnvData env) throws KommetException
	{
		List<Action> actions = new ArrayList<Action>();
		actions.add(action);
		actionDao.delete(actions, null, env);
	}
	
	@Transactional
	public void deleteAction(KID id, AuthData authData, EnvData env) throws KommetException
	{
		actionDao.delete(id, authData, env);
	}

	public Action saveSystemActionOnEnv(Action action, AuthData authData, EnvData env) throws KommetException
	{
		Action savedAction = actionDao.save(action, true, true, authData, env);
		
		if (savedAction.getController().getName() == null)
		{
			throw new KommetException("Name not initialized on action controller when adding action to env");
		}
		
		env.addAction(savedAction.getUrl(), null, savedAction);
		return savedAction;
	}

	@Transactional(readOnly = true)
	public Action getActionByName(String name, AuthData authData, EnvData env) throws KommetException
	{
		ActionFilter filter = new ActionFilter();
		filter.setName(name);
		List<Action> actions = actionDao.find(filter, env);
		return actions.isEmpty() ? null : actions.get(0);
	}

	public StandardAction getStandardAction(Type type, StandardActionType actionType, Profile profile, EnvData env) throws KommetException
	{
		if (actionType.equals(StandardActionType.CREATE))
		{
			return getStandardCreateAction(type, profile, env);
		}
		else if (actionType.equals(StandardActionType.VIEW))
		{
			return getStandardDetailsAction(type, profile, env);
		}
		else if (actionType.equals(StandardActionType.EDIT))
		{
			return getStandardEditAction(type, profile, env);
		}
		else if (actionType.equals(StandardActionType.LIST))
		{
			return getStandardListAction(type, profile, env);
		}
		else
		{
			throw new KommetException("Cannot get standard action for action type " + actionType);
		}
	}
}
