/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.sharing;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.GroupRecordSharing;
import kommet.basic.RecordAccessType;
import kommet.basic.RecordProxyUtil;
import kommet.basic.UserGroupAssignment;
import kommet.basic.UserRecordSharing;
import kommet.dao.UserGroupAssignmentDao;
import kommet.dao.UserGroupAssignmentFilter;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.koll.compiler.KommetCompiler;
import kommet.utils.MiscUtils;

@Service
public class SharingService
{
	@Inject
	UserRecordSharingDao userRecordSharingDao;
	
	@Inject
	GroupRecordSharingDao groupRecordSharingDao;
	
	@Inject
	UserGroupAssignmentDao userGroupAssignmentDao;
	
	@Inject
	DataService dataService;
	
	@Inject
	KommetCompiler compiler;
	
	@Transactional(readOnly = true)
	public List<UserRecordSharing> find (UserRecordSharingFilter filter, EnvData env) throws KommetException
	{
		return userRecordSharingDao.find(filter, env);
	}
	
	@Transactional
	public UserRecordSharing shareRecord (KID recordId, KID userId, AuthData authData, String reason, boolean isGeneric, EnvData env) throws KommetException
	{
		return shareRecord(recordId, userId, true, true, reason, isGeneric, null, null, compiler, userRecordSharingDao, dataService, authData, true, env);
	}
	
	@Transactional
	public GroupRecordSharing shareRecordWithGroup (KID recordId, KID groupId, AuthData authData, String reason, boolean isGeneric, EnvData env) throws KommetException
	{
		return shareRecordWithGroup(recordId, groupId, false, false, reason, isGeneric, null, null, compiler, userRecordSharingDao, dataService, userGroupAssignmentDao, authData, true, env);
	}
	
	@Transactional
	public UserRecordSharing shareRecord (KID recordId, KID userId, boolean edit, boolean delete, AuthData authData, String reason, boolean isGeneric, EnvData env) throws KommetException
	{
		return shareRecord(recordId, userId, edit, delete, reason, isGeneric, null, null, compiler, userRecordSharingDao, dataService, authData, true, env);
	}
	
	@Transactional
	public UserRecordSharing shareRecord (KID recordId, KID userId, boolean edit, boolean delete, String reason, boolean isGeneric, KID sharingRuleId, PropagatedSharingData psd, AuthData authData, EnvData env) throws KommetException
	{
		return shareRecord(recordId, userId, edit, delete, reason, isGeneric, sharingRuleId, psd, compiler, userRecordSharingDao, dataService, authData, true, env);
	}
	
	@Transactional
	public UserRecordSharing shareRecord (KID recordId, KID userId, boolean edit, boolean delete, String reason, boolean isGeneric, PropagatedSharingData psd, AuthData authData, EnvData env) throws KommetException
	{
		return shareRecord(recordId, userId, edit, delete, reason, isGeneric, null, psd, compiler, userRecordSharingDao, dataService, authData, true, env);
	}

	@Transactional
	public void deleteSharing(KID sharingId, EnvData env) throws KommetException
	{
		userRecordSharingDao.delete(MiscUtils.toList(getSharing(sharingId, env)), true, null, env);
	}
	
	/**
	 * Removes all generic sharings for this record. Sharings resulting from sharing this record with a
	 * group are not removed.
	 * @param recordId
	 * @param authData
	 * @param env
	 * @throws KommetException
	 */
	// TODO unit tests for this method
	@Transactional
	public void unshareRecordWithAllUsers(KID recordId, AuthData authData, EnvData env) throws KommetException
	{
		unshareRecord(recordId, null, authData, env);
	}
	
	@Transactional
	public void unshareRecord(KID recordId, KID userId, AuthData authData, EnvData env) throws KommetException
	{
		UserRecordSharingFilter filter = new UserRecordSharingFilter();
		filter.addRecordId(recordId);
		
		if (userId != null)
		{
			filter.addUserId(userId);
		}
		
		// unshare removes only generic sharings, because non-generic ones are a result of e.g.
		// group sharing propagation and can only be removed when this group sharing is removed
		filter.setIsGeneric(true);
		
		// do not use users auth data while looking for sharings
		userRecordSharingDao.delete(userRecordSharingDao.find(filter, env), null, env);
	}
	
	@Transactional
	public void unshareRecordWithAllGroups(KID recordId, AuthData authData, EnvData env) throws KommetException
	{
		unshareRecordWithGroup(recordId, null, null, null, authData, env);
	}
	
	@Transactional
	public void unshareRecordWithGroup(KID recordId, KID groupId, Boolean isGeneric, String reason, AuthData authData, EnvData env) throws KommetException
	{
		GroupRecordSharingFilter filter = new GroupRecordSharingFilter();
		filter.addRecordId(recordId);
		
		if (StringUtils.hasText(reason))
		{
			filter.addReason(reason);
		}
		
		if (isGeneric != null)
		{
			filter.setIsGeneric(isGeneric);
		}
		
		if (groupId != null)
		{
			filter.addGroupId(groupId);
		}
		
		deleteGroupSharings(groupRecordSharingDao.find(filter, env), env);
	}

	@Transactional(readOnly = true)
	public UserRecordSharing getSharing(KID sharingId, EnvData env) throws KommetException
	{
		return userRecordSharingDao.get(sharingId, env);
	}

	@Transactional
	public static UserRecordSharing shareRecord(KID recordId, KID userId, boolean edit, boolean delete, String reason, boolean isGeneric, KID sharingRuleId, PropagatedSharingData propagatedSharing, KommetCompiler compiler, UserRecordSharingDao userRecordSharingDao, DataService dataService, AuthData authData, boolean checkForExistingGenericSharing, EnvData env) throws KommetException
	{
		//System.out.println("Share record " + recordId + " with user " + userId);
		
		//if (propagatedSharing != null && propagatedSharing.getGroupHierarchyPath() != null)
		//{
		//	System.out.println("PSD: " + MiscUtils.implode(propagatedSharing.getGroupHierarchyPath(), ", "));
		//}
		
		if (recordId == null)
		{
			throw new KommetException("Trying to create a sharing for null record ID");
		}
		
		if (userId == null)
		{
			throw new KommetException("Trying to create a sharing for null user ID");
		}
		
		Type ursType = env.getType(KeyPrefix.get(KID.USER_RECORD_SHARING_PREFIX));
		
		// Check if sharing for this user-record combination does not exist.
		// Note: while querying user-record sharings, do not use current users privileges, but operate in root mode instead so that
		// all sharings (even those to which the current user doesn't have access) are returned.
		List<Record> existingGenericSharings = new ArrayList<Record>();
		
		// only generic sharings can be merged into one
		if (isGeneric && checkForExistingGenericSharing)
		{
			// if the sharing operation times out, this query is usually the reason
			existingGenericSharings = env.getSelectCriteriaFromDAL("select recordId, user.id, reason, edit, delete, isGeneric from " + ursType.getQualifiedName() + " where isGeneric = true and user.id = '" + userId + "' and recordId = '" + recordId + "'").list();
		}
		else if (propagatedSharing != null)
		{
			// with propagated sharings, we always want to check if a user record sharing
			// for this propagation does not already exist
			// if we didn't, this would cause new propagated URS records to be created every time a sharing
			// is modified on a given group and propagated to its members
			String query = "select recordId, user.id, reason, edit, delete, isGeneric from " + ursType.getQualifiedName() + " where isGeneric = false ";
			query += " AND groupRecordSharingId = '" + propagatedSharing.getGroupRecordSharingId() + "' AND userGroupAssignmentId = '" + propagatedSharing.getUserGroupAssignmentId() + "' AND "; 
			
			if (propagatedSharing.getGroupHierarchyPath().isEmpty())
			{
				query += " groupSharingHierarchy isnull";
			}
			else
			{
				query += " groupSharingHierarchy = '" + MiscUtils.implode(propagatedSharing.getGroupHierarchyPath(), ";") + "'";
			}
			
			// find propagated sharings
			existingGenericSharings = env.getSelectCriteriaFromDAL(query).list();
		}
		
		Record sharing = null;
		
		// if sharing does not exist, create it, otherwise update the existing one
		if (existingGenericSharings.isEmpty())
		{
			sharing = new Record(ursType);
			sharing.setField("recordId", recordId);
			sharing.setField("user.id", userId, env);
		}
		else
		{
			sharing = existingGenericSharings.get(0);
		}
		
		sharing.setField("reason", reason);
		sharing.setField("isGeneric", isGeneric);
		sharing.setField(Field.ACCESS_TYPE_FIELD_NAME, isGeneric ? RecordAccessType.PUBLIC.getId() : RecordAccessType.SYSTEM.getId());
		sharing.setField("read", true);
		sharing.setField("edit", edit);
		sharing.setField("delete", delete);
		sharing.setField("sharingRule.id", sharingRuleId, env);
		
		// if this sharing is a propagation of a group sharing, set additional properties
		if (propagatedSharing != null)
		{
			sharing.setField("groupRecordSharingId", propagatedSharing.getGroupRecordSharingId());
			sharing.setField("userGroupAssignmentId", propagatedSharing.getUserGroupAssignmentId());
			
			if (!propagatedSharing.getGroupHierarchyPath().isEmpty())
			{
				sharing.setField("groupSharingHierarchy", MiscUtils.implode(propagatedSharing.getGroupHierarchyPath(), ";"));
			}
		}
		
		// create sharing record - of course do not create any sharings on sharings, this is why skipSharing is passed
		// also skipping create permission is set because all users should be able to insert URS records
		return (UserRecordSharing)RecordProxyUtil.generateStandardTypeProxy(dataService.save(sharing, true, true, true, false, authData, env), env, compiler);
	}
	
	@Transactional
	public GroupRecordSharing shareRecordWithGroup(KID recordId, KID groupId, boolean edit, boolean delete, String reason, boolean isGeneric, KID sharingRuleId, AuthData authData, EnvData env) throws KommetException
	{
		return shareRecordWithGroup(recordId, groupId, edit, delete, reason, isGeneric, sharingRuleId, null, compiler, userRecordSharingDao, dataService, userGroupAssignmentDao, authData, isGeneric /*sic*/, env);
	}
	
	@Transactional
	public GroupRecordSharing shareRecordWithGroup(KID recordId, KID groupId, boolean edit, boolean delete, String reason, boolean isGeneric, AuthData authData, EnvData env) throws KommetException
	{
		return shareRecordWithGroup(recordId, groupId, edit, delete, reason, isGeneric, null, null, compiler, userRecordSharingDao, dataService, userGroupAssignmentDao, authData, isGeneric /*sic*/, env);
	}
	
	@Transactional
	public GroupRecordSharing shareRecordWithGroup(KID recordId, KID groupId, boolean edit, boolean delete, String reason, boolean isGeneric, KID sharingRuleId, PropagatedSharingData psd, AuthData authData, EnvData env) throws KommetException
	{
		return shareRecordWithGroup(recordId, groupId, edit, delete, reason, isGeneric, sharingRuleId, psd, compiler, userRecordSharingDao, dataService, userGroupAssignmentDao, authData, isGeneric /*sic*/, env);
	}
	
	/*@Transactional
	public void cleanOrphanSharings(EnvData env)
	{
		long startTime = System.currentTimeMillis();
		String query = "select clean_sharing(recordid) from obj_00o";
		SqlRowSet rowSet = env.getJdbcTemplate().queryForRowSet(query);
		
		long duration = System.currentTimeMillis() - startTime;
	}*/
	
	@Transactional
	public static GroupRecordSharing shareRecordWithGroup(KID recordId, KID groupId, boolean edit, boolean delete, String reason, boolean isGeneric, KID sharingRuleId, PropagatedSharingData psd, KommetCompiler compiler, UserRecordSharingDao userRecordSharingDao, DataService dataService, UserGroupAssignmentDao userGroupAssignmentDao, AuthData authData, boolean checkForExistingGenericSharing, EnvData env) throws KommetException
	{
		if (recordId == null)
		{
			throw new KommetException("Trying to create a sharing for null record ID");
		}
		
		if (groupId == null)
		{
			throw new KommetException("Trying to create a sharing for null group ID");
		}
		
		Type grsType = env.getType(KeyPrefix.get(KID.GROUP_RECORD_SHARING_PREFIX));
		
		// Check if sharing for this user-record combination does not exist.
		// Note: while querying user-record sharings, do not use current users privileges, but operate in root mode instead so that
		// all sharings (even those to which the current user doesn't have access) are returned.
		List<Record> existingGenericSharings = new ArrayList<Record>();
		
		// only generic sharings can be merged into one
		if (isGeneric && checkForExistingGenericSharing)
		{
			existingGenericSharings = env.getSelectCriteriaFromDAL("select recordId, group.id, reason, edit, delete, isGeneric from " + grsType.getQualifiedName() + " where isGeneric = true and group.id = '" + groupId + "' and recordId = '" + recordId + "'").list();
		}
		
		Record sharing = null;
		
		// if sharing does not exist, create it, otherwise update the existing one
		if (existingGenericSharings.isEmpty())
		{
			sharing = new Record(grsType);
			sharing.setField("recordId", recordId);
			sharing.setField("group.id", groupId, env);
		}
		else
		{
			sharing = existingGenericSharings.get(0);
		}
		
		sharing.setField("reason", reason);
		sharing.setField("isGeneric", isGeneric);
		sharing.setField(Field.ACCESS_TYPE_FIELD_NAME, isGeneric ? RecordAccessType.PUBLIC.getId() : RecordAccessType.SYSTEM.getId());
		sharing.setField("read", true);
		sharing.setField("edit", edit);
		sharing.setField("delete", delete);
		sharing.setField("sharingRule.id", sharingRuleId, env);
		
		// create sharing record - of course do not create any sharings on sharings, this is why skipSharing is passed
		GroupRecordSharing grs = (GroupRecordSharing)RecordProxyUtil.generateStandardTypeProxy(dataService.save(sharing, true, true, AuthData.getRootAuthData(env), env), env, compiler);
		
		if (psd == null)
		{
			psd = new PropagatedSharingData(grs.getId(), null, new ArrayList<String>());
		}
		
		// share record with all group and subgroup members
		propagateGroupSharing(groupId, sharingRuleId, grs, psd, compiler, userRecordSharingDao, dataService, userGroupAssignmentDao, AuthData.getRootAuthData(env), env);
		
		return grs;
	}

	/**
	 * Propagates a group record sharing to all the group's members and members of all subgroups in the hierarchy.
	 * @param groupId
	 * @param grs
	 * @param propagatedSharing
	 * @param compiler
	 * @param userRecordSharingDao
	 * @param dataService
	 * @param userGroupAssignmentDao
	 * @param authData
	 * @param env
	 * @throws KommetException
	 */
	private static void propagateGroupSharing(KID groupId, KID sharingRuleId, GroupRecordSharing grs, PropagatedSharingData propagatedSharing, KommetCompiler compiler, UserRecordSharingDao userRecordSharingDao, DataService dataService, UserGroupAssignmentDao userGroupAssignmentDao, AuthData authData, EnvData env) throws KommetException
	{
		UserGroupAssignmentFilter filter = new UserGroupAssignmentFilter();
		filter.addParentGroupId(groupId);
		
		// find all users in group - but sure not to use current user's auth data because they may not
		// have access to this record
		
		KID recordId = grs != null ? grs.getRecordId() : null;
		
		System.out.println("[RecordSharing][" + recordId + "] Looking for UGAs for parent " + groupId);
		List<UserGroupAssignment> ugas = userGroupAssignmentDao.get(filter, null, env);
		System.out.println("[RecordSharing][" + recordId + "] Found UGAs: " + ugas.size());
		
		for (UserGroupAssignment uga : ugas)
		{	
			if (uga.getChildGroup() != null)
			{
				System.out.println("[RecordSharing][" + recordId + "] UGA child group = " + uga.getChildGroup().getId());
				propagatedSharing.getGroupHierarchyPath().add(uga.getId().getId());
				
				// propagate sharing to subgroups
				propagateGroupSharing(uga.getChildGroup().getId(), sharingRuleId, grs, propagatedSharing, compiler, userRecordSharingDao, dataService, userGroupAssignmentDao, authData, env);
				
				propagatedSharing.getGroupHierarchyPath().remove(propagatedSharing.getGroupHierarchyPath().size() - 1);
				System.out.println("[RecordSharing][" + recordId + "] Propagation done");
			}
			else
			{
				System.out.println("[RecordSharing][" + recordId + "] UGA child user = " + uga.getChildUser().getId());
				propagatedSharing.setUserGroupAssignmentId(uga.getId());
				
				// share record with user
				shareRecord(grs.getRecordId(), uga.getChildUser().getId(), grs.getEdit(), grs.getDelete(), grs.getReason(), false, sharingRuleId, propagatedSharing, compiler, userRecordSharingDao, dataService, authData, false, env);
			
				propagatedSharing.setUserGroupAssignmentId(null);
				System.out.println("[RecordSharing][" + recordId + "] Sharing done");
			}
		}
	}

	/**
	 * Tells whether the given user can edit the given record basing on the user record sharings.
	 * This method does not check type permissions.
	 * @param recordId
	 * @param userId
	 * @return
	 * @throws KommetException 
	 */
	@Transactional(readOnly = true)
	public boolean canEditRecord(KID recordId, KID userId, EnvData env) throws KommetException
	{
		return userRecordSharingDao.canEditRecord(recordId, userId, env);
	}
	
	@Transactional(readOnly = true)
	public boolean canDeleteRecord(KID recordId, KID userId, EnvData env) throws KommetException
	{
		return userRecordSharingDao.canDeleteRecord(recordId, userId, env);
	}
	
	@Transactional(readOnly = true)
	public boolean canViewRecord(KID recordId, KID userId, EnvData env) throws KommetException
	{
		return userRecordSharingDao.canViewRecord(recordId, userId, env);
	}
	
	@Transactional(readOnly = true)
	public boolean canGroupViewRecord(KID recordId, KID groupId, EnvData env)
	{
		return groupRecordSharingDao.canViewRecord(recordId, groupId, env);
	}
	
	@Transactional(readOnly = true)
	public boolean canGroupEditRecord(KID recordId, KID groupId, EnvData env)
	{
		return groupRecordSharingDao.canEditRecord(recordId, groupId, env);
	}

	@Transactional(readOnly = true)
	public List<GroupRecordSharing> get(GroupRecordSharingFilter filter, EnvData env) throws KommetException
	{
		return groupRecordSharingDao.find(filter, env);
	}

	@Transactional
	public void deleteGroupSharings(List<GroupRecordSharing> sharings, EnvData env) throws KommetException
	{
		if (!sharings.isEmpty())
		{
			UserRecordSharingFilter ursFilter = new UserRecordSharingFilter();
			
			System.out.println("[sharing] Removing " + sharings.size() + " sharings");
			
			// remove all user record sharings being a propagation of this group record sharing
			for (GroupRecordSharing sharing : sharings)
			{
				ursFilter.addGroupRecordSharingId(sharing.getId());
			}
			
			
			System.out.println("[sharing] Querying URS for deletion");
			List<UserRecordSharing> userRecordSharings = userRecordSharingDao.find(ursFilter, env);
			System.out.println("[sharing] Found URS for deletion");
			
			// remove propagated URS records
			userRecordSharingDao.delete(userRecordSharings, null, env);
			
			System.out.println("[sharing] All URS deleted");
		}
		
		groupRecordSharingDao.delete(sharings, null, env);
	}

	/*public void shareRecordsByGRS(Map<KID, PropagatedSharingData> psdByRecord, KID userId, Map<KID, GroupRecordSharing> grsByRecord, boolean isGeneric, boolean checkForExistingGenericSharing, AuthData authData, EnvData env)
	{	
		if (userId == null)
		{
			throw new KommetException("Trying to create a sharing for null user ID");
		}
		
		Type ursType = env.getType(KeyPrefix.get(KID.USER_RECORD_SHARING_PREFIX));
		
		// Check if sharing for this user-record combination does not exist.
		// Note: while querying user-record sharings, do not use current users privileges, but operate in root mode instead so that
		// all sharings (even those to which the current user doesn't have access) are returned.
		List<Record> existingGenericSharings = new ArrayList<Record>();
		
		// only generic sharings can be merged into one
		if (isGeneric && checkForExistingGenericSharing)
		{
			existingGenericSharings = env.getSelectCriteriaFromDAL("select recordId, user.id, reason, edit, delete, isGeneric from " + ursType.getQualifiedName() + " where isGeneric = true and user.id = '" + userId + "' and recordId = '" + recordId + "'").list();
		}
		else if (propagatedSharing != null)
		{
			// with propagated sharings, we always want to check if a user record sharing
			// for this propagation does not already exist
			// if we didn't, this would cause new propagated URS records to be created every time a sharing
			// is modified on a given group and propagated to its members
			String query = "select recordId, user.id, reason, edit, delete, isGeneric from " + ursType.getQualifiedName() + " where isGeneric = false ";
			query += " AND groupRecordSharingId = '" + propagatedSharing.getGroupRecordSharingId() + "' AND userGroupAssignmentId = '" + propagatedSharing.getUserGroupAssignmentId() + "' AND "; 
			
			if (propagatedSharing.getGroupHierarchyPath().isEmpty())
			{
				query += " groupSharingHierarchy isnull";
			}
			else
			{
				query += " groupSharingHierarchy = '" + MiscUtils.implode(propagatedSharing.getGroupHierarchyPath(), ";") + "'";
			}
			
			// find propagated sharings
			existingGenericSharings = env.getSelectCriteriaFromDAL(query).list();
		}
		
		Record sharing = null;
		
		// if sharing does not exist, create it, otherwise update the existing one
		if (existingGenericSharings.isEmpty())
		{
			sharing = new Record(ursType);
			sharing.setField("recordId", recordId);
			sharing.setField("user.id", userId, env);
		}
		else
		{
			sharing = existingGenericSharings.get(0);
		}
		
		sharing.setField("reason", reason);
		sharing.setField("isGeneric", isGeneric);
		sharing.setField(Field.ACCESS_TYPE_FIELD_NAME, isGeneric ? RecordAccessType.PUBLIC.getId() : RecordAccessType.SYSTEM.getId());
		sharing.setField("read", true);
		sharing.setField("edit", edit);
		sharing.setField("delete", delete);
		sharing.setField("sharingRule.id", sharingRuleId, env);
		
		// if this sharing is a propagation of a group sharing, set additional properties
		if (propagatedSharing != null)
		{
			sharing.setField("groupRecordSharingId", propagatedSharing.getGroupRecordSharingId());
			sharing.setField("userGroupAssignmentId", propagatedSharing.getUserGroupAssignmentId());
			
			if (!propagatedSharing.getGroupHierarchyPath().isEmpty())
			{
				sharing.setField("groupSharingHierarchy", MiscUtils.implode(propagatedSharing.getGroupHierarchyPath(), ";"));
			}
		}
		
		// create sharing record - of course do not create any sharings on sharings, this is why skipSharing is passed
		// also skipping create permission is set because all users should be able to insert URS records
		return (UserRecordSharing)RecordProxyUtil.generateStandardTypeProxy(dataService.save(sharing, true, true, true, false, authData, env), env, compiler);
	}*/
}