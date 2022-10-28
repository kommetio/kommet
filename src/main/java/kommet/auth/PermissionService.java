/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.auth;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.basic.ActionPermission;
import kommet.basic.FieldPermission;
import kommet.basic.RecordAccessType;
import kommet.basic.TypePermission;
import kommet.basic.types.SystemTypes;
import kommet.dao.ActionPermissionDao;
import kommet.dao.FieldPermissionDao;
import kommet.dao.TypePermissionDao;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;

@Service
public class PermissionService
{
	@Inject
	DataService dataService;
	
	@Inject
	TypePermissionDao typePermissionDao;
	
	@Inject
	FieldPermissionDao fieldPermissionDao;
	
	@Inject
	ActionPermissionDao actionPermissionDao;
	
	@Transactional
	public TypePermission setTypePermissionForProfile(KID profileId, KID typeId, boolean read, boolean edit, boolean delete, boolean create, boolean readAll, boolean editAll, boolean deleteAll, AuthData authData, EnvData env) throws KommetException
	{
		return setTypePermissionForProfile(profileId, typeId, read, edit, delete, create, readAll, editAll, deleteAll, RecordAccessType.PUBLIC, authData, env);
	}
	
	@Transactional
	public TypePermission setTypePermissionForProfile(KID profileId, KID typeId, boolean read, boolean edit, boolean delete, boolean create, boolean readAll, boolean editAll, boolean deleteAll, RecordAccessType accessType, AuthData authData, EnvData env) throws KommetException
	{
		TypePermission typePermission = getTypePermissionForProfile(profileId, typeId, env);
		
		Record permission = null;
		
		if (typePermission == null)
		{
			permission = new Record(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.TYPE_PERMISSION_API_NAME)));
			permission.setField("profile.id", profileId, env);
			permission.setField("typeId", typeId, env);
		}
		else
		{
			permission = typePermission.getRecord();
		}
		
		permission.setField("read", read);
		permission.setField("edit", edit);
		permission.setField("delete", delete);
		permission.setField("create", create);
		permission.setField("readAll", readAll);
		permission.setField("editAll", editAll);
		permission.setField("deleteAll", deleteAll);
		permission.setField(Field.ACCESS_TYPE_FIELD_NAME, accessType.getId());
		
		// reset the last permission update date on env so that users are aware their
		// permissions might be out of date and need to be refreshed
		env.setLastTypePermissionsUpdate((new Date()).getTime());
		
		return new TypePermission(dataService.save(permission, true, true, authData, env), env);
	}
	
	@Transactional
	public List<TypePermission> getTypePermissionForProfile(KID profileId, EnvData env) throws KommetException
	{
		List<Record> permissions = env.getSelectCriteriaFromDAL("select id, typeId, read, edit, delete, create, readAll, editAll, deleteAll, profile.id, profile.name, accessType from " + SystemTypes.TYPE_PERMISSION_API_NAME + " where profile.id = '" + profileId + "'").list();
		List<TypePermission> typePermissions = new ArrayList<TypePermission>();
		for (Record permission : permissions)
		{
			typePermissions.add(new TypePermission(permission, env));
		}
		return typePermissions;
	}
	
	@Transactional
	public List<TypePermission> getTypePermissionForType(KID typeId, EnvData env) throws KommetException
	{
		List<Record> permissions = env.getSelectCriteriaFromDAL("select id, typeId, read, edit, delete, profile.id, profile.name from " + SystemTypes.TYPE_PERMISSION_API_NAME + " where typeId = '" + typeId + "'").list();
		List<TypePermission> objPermissions = new ArrayList<TypePermission>();
		for (Record permission : permissions)
		{
			objPermissions.add(new TypePermission(permission, env));
		}
		return objPermissions;
	}
	
	@Transactional
	public List<FieldPermission> getFieldPermissionForProfile(KID profileId, EnvData env) throws KommetException
	{
		List<Record> permissions = env.getSelectCriteriaFromDAL("select id, fieldId, read, edit, profile.id, profile.name from " + SystemTypes.FIELD_PERMISSION_API_NAME + " where profile.id = '" + profileId + "'").list();
		List<FieldPermission> fieldPermissions = new ArrayList<FieldPermission>();
		for (Record permission : permissions)
		{
			fieldPermissions.add(new FieldPermission(permission, env));
		}
		return fieldPermissions;
	}
	
	@Transactional
	public List<ActionPermission> getActionPermissionForProfile(KID profileId, EnvData env) throws KommetException
	{
		List<Record> permissions = env.getSelectCriteriaFromDAL("select id, action.id, action.url, read, profile.id, profile.name from " + SystemTypes.ACTION_PERMISSION_API_NAME + " where profile.id = '" + profileId + "'").list();
		List<ActionPermission> actionPermissions = new ArrayList<ActionPermission>();
		for (Record permission : permissions)
		{
			actionPermissions.add(new ActionPermission(permission, env));
		}
		return actionPermissions;
	}

	@Transactional
	public TypePermission getTypePermissionForProfile(KID profileId, KID typeId, EnvData env) throws KommetException
	{
		List<Record> permissions = env.getSelectCriteriaFromDAL("select id, typeId, read, edit, delete, create, readAll, editAll, profile.id, profile.name from " + SystemTypes.TYPE_PERMISSION_API_NAME + " where profile.id = '" + profileId + "' and typeId = '" + typeId + "'").list();
		if (permissions.size() > 1)
		{
			throw new KommetException("More than one permission found for type " + typeId + " and profile " + profileId);	
		}
		return permissions.isEmpty() ? null : new TypePermission(permissions.get(0), env);
	}
	
	@Transactional
	public FieldPermission getFieldPermissionForProfile(KID profileId, KID fieldId, EnvData env) throws KommetException
	{
		List<Record> permissions = env.getSelectCriteriaFromDAL("select id, fieldId, read, edit, profile.id, profile.name from " + SystemTypes.FIELD_PERMISSION_API_NAME + " where profile.id = '" + profileId + "' and fieldId = '" + fieldId + "'").list();
		if (permissions.size() > 1)
		{
			throw new KommetException("More than one permission found for field " + fieldId + " and profile " + profileId);	
		}
		return permissions.isEmpty() ? null : new FieldPermission(permissions.get(0), env);
	}
	
	@Transactional
	public ActionPermission getActionPermissionForProfile(KID profileId, KID actionId, EnvData env) throws KommetException
	{
		// TODO - escape URL in query?
		List<Record> permissions = env.getSelectCriteriaFromDAL("select id, read, profile.id, profile.name, action.id from " + SystemTypes.ACTION_PERMISSION_API_NAME + " where profile.id = '" + profileId + "' and action.id = '" + actionId + "'").list();
		if (permissions.size() > 1)
		{
			throw new KommetException("More than one permission found for action '" + actionId + "' and profile " + profileId);	
		}
		return permissions.isEmpty() ? null : new ActionPermission(permissions.get(0), env);
	}

	@Transactional
	public FieldPermission setFieldPermissionForProfile(KID profileId, KID fieldId, boolean read, boolean edit, AuthData authData, EnvData env) throws KommetException
	{
		FieldPermission objPermission = getFieldPermissionForProfile(profileId, fieldId, env);
		
		Record permission = null;
		
		if (objPermission == null)
		{
			permission = new Record(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.FIELD_PERMISSION_API_NAME)));
			permission.setField("profile.id", profileId, env);
			permission.setField("fieldId", fieldId, env);
		}
		else
		{
			permission = objPermission.getRecord();
		}
		
		permission.setField("read", read);
		permission.setField("edit", edit);
		
		// reset the last permission update date on env so that users are aware their
		// permissions might be out of date and need to be refreshed
		env.setLastFieldPermissionsUpdate((new Date()).getTime());
		
		return new FieldPermission(dataService.save(permission, true, true, authData, env), env);
	}

	public ActionPermission setActionPermissionForProfile(KID profileId, KID actionId, boolean read, AuthData authData, EnvData env) throws KommetException
	{
		ActionPermission actionPermission = getActionPermissionForProfile(profileId, actionId, env);
		
		Record permission = null;
		
		if (actionPermission == null)
		{
			permission = new Record(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.ACTION_PERMISSION_API_NAME)));
			permission.setField("profile.id", profileId, env);
			permission.setField("action.id", actionId, env);
		}
		else
		{
			permission = actionPermission.getRecord();
		}
		
		permission.setField("read", read);
		
		// reset the last permission update date on env so that users are aware their
		// permissions might be out of date and need to be refreshed
		env.setLastActionPermissionsUpdate((new Date()).getTime());
		
		return new ActionPermission(dataService.save(permission, true, true, authData, env), env);
	}

	@Transactional
	public TypePermission save(TypePermission permission, AuthData authData, EnvData env) throws KommetException
	{
		return typePermissionDao.save(permission, authData, env);
	}
	
	@Transactional
	public FieldPermission save(FieldPermission permission, AuthData authData, EnvData env) throws KommetException
	{
		return fieldPermissionDao.save(permission, authData, env);
	}
	
	@Transactional
	public ActionPermission save(ActionPermission permission, AuthData authData, EnvData env) throws KommetException
	{
		return actionPermissionDao.save(permission, authData, env);
	}
}
