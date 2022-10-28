/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.sysctx;

import kommet.auth.AuthData;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.sharing.SharingService;
import kommet.env.EnvData;
import kommet.koll.CurrentAuthDataAware;

public class SharingServiceProxy extends ServiceProxy
{
	private SharingService sharingService;
	
	public SharingServiceProxy(SharingService sharingService, CurrentAuthDataAware authDataProvider, EnvData env)
	{
		super(authDataProvider, env);
		this.sharingService = sharingService;
	}
	
	public void shareRecordWithUser (KID recordId, KID userId, boolean edit, boolean delete, String reason) throws KommetException
	{
		sharingService.shareRecord(recordId, userId, edit, delete, authDataProvider.currentAuthData(), reason, true, env);
	}
	
	public void shareRecordWithUser (KID recordId, KID userId, boolean edit, boolean delete, String reason, boolean asRoot) throws KommetException
	{
		AuthData authData = asRoot ? AuthData.getRootAuthData(env) : authDataProvider.currentAuthData();
		sharingService.shareRecord(recordId, userId, edit, delete, authData, reason, true, env);
	}
	
	public void shareRecordWithGroup (KID recordId, KID groupId, boolean edit, boolean delete, String reason) throws KommetException
	{
		sharingService.shareRecordWithGroup(recordId, groupId, edit, delete, reason, true, authDataProvider.currentAuthData(), env);
	}
	
	public void shareRecordWithGroup (KID recordId, KID groupId, boolean edit, boolean delete, String reason, boolean asRoot) throws KommetException
	{
		AuthData authData = asRoot ? AuthData.getRootAuthData(env) : authDataProvider.currentAuthData();
		sharingService.shareRecordWithGroup(recordId, groupId, edit, delete, reason, true, authData, env);
	}
	
	public void unshareRecordWithGroup (KID recordId, KID groupId) throws KommetException
	{
		sharingService.unshareRecordWithGroup(recordId, groupId, null, null, authDataProvider.currentAuthData(), env);
	}
	
	public void unshareRecordWithAllGroups (KID recordId) throws KommetException
	{
		sharingService.unshareRecordWithAllGroups(recordId, authDataProvider.currentAuthData(), env);
	}
	
	public void unshareRecordWithAllUsers (KID recordId) throws KommetException
	{
		sharingService.unshareRecordWithAllUsers(recordId, authDataProvider.currentAuthData(), env);
	}
	
	public boolean canGroupViewRecord (KID recordId, KID groupId)
	{
		return this.sharingService.canGroupViewRecord(recordId, groupId, env);
	}
	
	public boolean canGroupEditRecord (KID recordId, KID groupId)
	{
		return this.sharingService.canGroupEditRecord(recordId, groupId, env);
	}
	
	public boolean canUserViewRecord (KID recordId, KID userId) throws KommetException
	{
		return this.sharingService.canViewRecord(recordId, userId, env);
	}
	
	public boolean canUserEditRecord (KID recordId, KID userId) throws KommetException
	{
		return this.sharingService.canEditRecord(recordId, userId, env);
	}
}