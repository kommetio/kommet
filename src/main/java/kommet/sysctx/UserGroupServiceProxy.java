/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.sysctx;

import kommet.auth.AuthData;
import kommet.basic.RecordProxyUtil;
import kommet.basic.UserGroup;
import kommet.data.DataService;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.koll.CurrentAuthDataAware;
import kommet.services.UserGroupService;

public class UserGroupServiceProxy extends ServiceProxy
{
	private UserGroupService ugs;
	private DataService dataService;
	
	public UserGroupServiceProxy (UserGroupService userGroupService, DataService dataService, CurrentAuthDataAware provider, EnvData env)
	{
		super(provider, env);
		this.ugs = userGroupService;
		this.dataService = dataService;
	}
	
	public Record getUserGroup (String groupName) throws KommetException
	{
		UserGroup group = ugs.getByName(groupName, authDataProvider.currentAuthData(), env);
		return group != null ? RecordProxyUtil.generateRecord(group, env.getType(KeyPrefix.get(KID.USER_GROUP_PREFIX)), 1, env) : null;
	}
	
	public Record saveUserGroup (Record group) throws KommetException
	{
		return dataService.save(group, authDataProvider.currentAuthData(), env);
	}
	
	public void deleteUserGroup (KID id) throws KommetException
	{
		ugs.delete(id, authDataProvider.currentAuthData(), env);
	}
	
	public void addUserToGroup (KID userId, KID groupId) throws KommetException
	{
		this.addUserToGroup(userId, groupId, false);
	}
	
	public void addUserToGroup (KID userId, KID groupId, boolean asRoot) throws KommetException
	{
		AuthData authData = asRoot ? AuthData.getRootAuthData(env) : authDataProvider.currentAuthData();
		ugs.assignUserToGroup(userId, groupId, authData, env);
	}
	
	public void addGroupToGroup (KID childGroupId, KID parentGroupId) throws KommetException
	{
		ugs.assignGroupToGroup(childGroupId, parentGroupId, authDataProvider.currentAuthData(), env);
	}
	
	public void removeUserFromGroup (KID userId, KID groupId) throws KommetException
	{
		ugs.unassignUserFromGroup(userId, groupId, authDataProvider.currentAuthData(), env);
	}
	
	public void unassignGroupFromGroup (KID childGroupId, KID parentGroupId, boolean isDeterred) throws KommetException
	{
		ugs.unassignUserGroupFromGroup(childGroupId, parentGroupId, isDeterred, authDataProvider.currentAuthData(), env);
	}
	
	public void unassignGroupFromGroup (KID childGroupId, KID parentGroupId) throws KommetException
	{
		ugs.unassignUserGroupFromGroup(childGroupId, parentGroupId, true, authDataProvider.currentAuthData(), env);
	}
	
	public void unassignUserFromGroup(KID userId, KID groupId) throws KommetException
	{
		ugs.unassignUserFromGroup(userId, groupId, authDataProvider.currentAuthData(), env);
	}
}