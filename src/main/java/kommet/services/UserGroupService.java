/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.basic.GroupRecordSharing;
import kommet.basic.UniqueCheckViolationException;
import kommet.basic.User;
import kommet.basic.UserGroup;
import kommet.basic.UserGroupAssignment;
import kommet.basic.UserGroupAssignmentException;
import kommet.basic.UserRecordSharing;
import kommet.dao.UserGroupAssignmentDao;
import kommet.dao.UserGroupAssignmentFilter;
import kommet.dao.UserGroupDao;
import kommet.dao.UserGroupFilter;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.sharing.GroupHierarchyPath;
import kommet.data.sharing.GroupRecordSharingDao;
import kommet.data.sharing.GroupRecordSharingFilter;
import kommet.data.sharing.PropagatedSharingData;
import kommet.data.sharing.SharingService;
import kommet.data.sharing.UgaApplierJob;
import kommet.data.sharing.UgaApplierJobDetail;
import kommet.data.sharing.UgaRemoverJob;
import kommet.data.sharing.UgaRemoverJobDetail;
import kommet.data.sharing.UserRecordSharingDao;
import kommet.data.sharing.UserRecordSharingFilter;
import kommet.env.EnvData;
import kommet.utils.MiscUtils;
import kommet.utils.ValidationUtil;

@Service
public class UserGroupService
{
	@Inject
	UserGroupDao userGroupDao;
	
	@Inject
	UserGroupAssignmentDao userGroupAssignmentDao;
	
	@Inject
	GroupRecordSharingDao groupRecordSharingDao;
	
	@Inject
	UserRecordSharingDao userRecordSharingDao;
	
	@Inject
	SharingService sharingService;
	
	@Inject
	SchedulerFactoryBean schedulerFactory;
	
	// ten seconds limit
	private static final int UGA_APPLIER_MILLIS_LIMIT = 60000;
	
	public static final String UGA_APPLIER_JOB_PREFIX = "uga-applier";
	public static final String UGA_REMOVER_JOB_PREFIX = "uga-remover";
	
	@Transactional
	public UserGroupAssignment assignUserToGroup (KID userId, KID groupId, AuthData authData, EnvData env) throws KommetException
	{
		return assignUserToGroup(userId, groupId, authData, env, false);
	}
	
	@Transactional
	public UserGroupAssignment assignUserToGroup (KID userId, KID groupId, AuthData authData, EnvData env, boolean isApplyImmediately) throws KommetException
	{
		if (userId == null)
		{
			throw new KommetException("Null user ID while assigning user to group");
		}
		else if (!userId.getId().startsWith(KID.USER_PREFIX))
		{
			throw new KommetException("Record with ID " + userId + " is not a user so it cannot be assigned to user group");
		}
		
		UserGroupAssignment uga = new UserGroupAssignment();
		
		UserGroup parentGroup = new UserGroup();
		parentGroup.setId(groupId);
		uga.setParentGroup(parentGroup);
		
		User childUser = new User();
		childUser.setId(userId);
		uga.setChildUser(childUser);
		
		// this sharing will be propagated later by a scheduled job UgaApplierJob
		uga.setIsApplyPending(true);
		
		try
		{
			uga = userGroupAssignmentDao.save(uga, authData, env);
		}
		catch (UniqueCheckViolationException e)
		{
			if (env.getType(e.getUniqueCheck().getTypeId()).getKeyPrefix().equals(KeyPrefix.get(KID.USER_GROUP_ASSIGNMENT_PREFIX)))
			{
				throw new UserGroupAssignmentException("User is already assigned to group");
			}
			else
			{
				throw e;
			}
		}
		
		// this sharing will be propagated later by a scheduled job UgaApplierJob
		// propagateGroupSharingsToUser(uga, authData, env);
		
		if (isApplyImmediately)
		{
			batchPropagatePendingUserGroupSharings(AuthData.getRootAuthData(env), env);
		}
		
		return uga;
	}
	
	public void batchPropagatePendingUserGroupSharings(AuthData authData, EnvData env) throws KommetException
	{
		UserGroupAssignmentFilter filter = new UserGroupAssignmentFilter();
		filter.setApplyPending(true);
		
		List<UserGroupAssignment> assignments = userGroupAssignmentDao.get(filter, null, env);
		
		System.out.println("[User group sharing] Found " + assignments.size() + " unapplied sharings");
		
		long startTime = System.currentTimeMillis();
		int processedUgas = 0;
		
		for (UserGroupAssignment uga : assignments)
		{
			if (uga.getChildUser() != null)
			{
				propagateGroupSharingsToUser(uga, authData, env);
			}
			
			if (uga.getChildGroup() != null)
			{
				propagateGroupSharingsToGroup(uga, authData, env);
			}
			
			// mark the UGA as applied
			uga.setIsApplyPending(false);
			userGroupAssignmentDao.save(uga, authData, env);
			processedUgas++;
			
			if ((System.currentTimeMillis() - startTime) > UGA_APPLIER_MILLIS_LIMIT)
			{
				System.out.println("[User group sharing] Max time exceeded, processed " + processedUgas + " group sharings");
				break;
			}
		}
		
		System.out.println("[User group sharing] Completed");
	}
	
	public void scheduleUgaApplier (EnvData env) throws SchedulerException
	{
		String jobName = UGA_APPLIER_JOB_PREFIX + "-" + env.getId();
		String jobGroup = "uga-appliers-" + env.getId();
		
		// check if a job for this task does not already exist
		JobDetail jobDetail = schedulerFactory.getScheduler().getJobDetail(JobKey.jobKey(jobName, jobGroup));
		
		if (jobDetail != null)
		{
			// be sure to use method deleteJob instead of unscheduleJob
			schedulerFactory.getScheduler().deleteJob(JobKey.jobKey(jobName, jobGroup));
		}
		
		UgaApplierJobDetail job = new UgaApplierJobDetail(this, env);
		job.setName(jobName);
		job.setGroup(jobGroup);
		job.setJobClass(UgaApplierJob.class);

		// Trigger the job to run now, and then repeat every 2 minutes
		CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(jobName + "-trigger", jobGroup).withSchedule(CronScheduleBuilder.cronSchedule("0 */1 * * * ?")).build();
		schedulerFactory.getScheduler().scheduleJob(job, trigger);
	}
	
	public void scheduleUgaRemover (EnvData env) throws SchedulerException
	{
		String jobName = UGA_REMOVER_JOB_PREFIX + "-" + env.getId();
		String jobGroup = "uga-removers-" + env.getId();
		
		// check if a job for this task does not already exist
		JobDetail jobDetail = schedulerFactory.getScheduler().getJobDetail(JobKey.jobKey(jobName, jobGroup));
		
		if (jobDetail != null)
		{
			// be sure to use method deleteJob instead of unscheduleJob
			schedulerFactory.getScheduler().deleteJob(JobKey.jobKey(jobName, jobGroup));
		}
		
		UgaRemoverJobDetail job = new UgaRemoverJobDetail(this, env);
		job.setName(jobName);
		job.setGroup(jobGroup);
		job.setJobClass(UgaRemoverJob.class);

		// Trigger the job to run now, and then repeat every 2 minutes
		CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(jobName + "-trigger", jobGroup).withSchedule(CronScheduleBuilder.cronSchedule("0 */1 * * * ?")).build();
		schedulerFactory.getScheduler().scheduleJob(job, trigger);
	}
	
	/**
	 * Shares every record that is shared with the given user group, with the given user.
	 * @param uga
	 * @param env
	 * @throws KommetException
	 */
	private void propagateGroupSharingsToUser(UserGroupAssignment uga, AuthData authData, EnvData env) throws KommetException
	{
		System.out.println("[Sharing] Propagating group sharing to user " + uga.getId());
		
		Map<KID, GroupHierarchyPath> supergroups = getGroupsForUserWithHierarchy(uga.getChildUser().getId(), null, env);
		
		System.out.println("[Sharing] Found supergroups " + supergroups.size());
		
		GroupRecordSharingFilter filter = new GroupRecordSharingFilter();
		filter.setGroupIds(supergroups.keySet());
		filter.setExcludeSystemObjects(true);
		
		// find all records shared with all super groups
		// TODO is it now a problem that there can be millions of them? perhaps it should be done by a DB function?
		List<GroupRecordSharing> groupRecordSharings = groupRecordSharingDao.find(filter, env);
		
		System.out.println("[Sharing] Supergroup record sharings " + groupRecordSharings.size());
		int i = 0;
		for (GroupRecordSharing grs : groupRecordSharings)
		{
			List<String> groupHierarchyIds = MiscUtils.idListToStringList(supergroups.get(grs.getGroup().getId()).getGroupToGroupAssignmentIds());
			PropagatedSharingData psd = new PropagatedSharingData(grs.getId(), uga.getId(), groupHierarchyIds);
			
			if (i % 100 == 0)
			{
				System.out.println("[Sharing] Record " + i + " / " + groupRecordSharings.size());
			}
			
			i++;
			
			sharingService.shareRecord(grs.getRecordId(), uga.getChildUser().getId(), grs.getEdit(), grs.getDelete(), grs.getReason(), false, psd, authData, env);
		}
		
		System.out.println("[Sharing] Querying group record sharings");
		
		// share with directly assigned group
		filter = new GroupRecordSharingFilter();
		Set<KID> groupIds = new HashSet<KID>();
		groupIds.add(uga.getParentGroup().getId());
		filter.setGroupIds(groupIds);
		groupRecordSharings = groupRecordSharingDao.find(filter, env);
		
		System.out.println("[Sharing] Sharing directly associated records " + groupRecordSharings.size());
		
		i = 0;
		for (GroupRecordSharing grs : groupRecordSharings)
		{
			PropagatedSharingData psd = new PropagatedSharingData(grs.getId(), uga.getId(), new ArrayList<String>());
			
			if (i % 100 == 0)
			{
				System.out.println("[Sharing] Record " + (i++));
			}
			
			// the direct user group sharing will not have any group to group assignments, and also, no PropagatedSharingData is needed (really?)
			sharingService.shareRecord(grs.getRecordId(), uga.getChildUser().getId(), grs.getEdit(), grs.getDelete(), grs.getReason(), false, psd, authData, env);
		}
		
		System.out.println("[Sharing] Propagating done");
	}
	
	/*private void propagateGroupSharingsToUser(UserGroupAssignment uga, AuthData authData, EnvData env) throws KommetException
	{
		System.out.println("[Sharing] Propagating group sharing to user");
		Map<KID, GroupHierarchyPath> supergroups = getGroupsForUserWithHierarchy(uga.getChildUser().getId(), null, env);
		
		GroupRecordSharingFilter filter = new GroupRecordSharingFilter();
		filter.setGroupIds(supergroups.keySet());
		
		// find all records shared with all super groups
		// TODO is it now a problem that there can be millions of them? perhaps it should be done by a DB function?
		List<GroupRecordSharing> groupRecordSharings = groupRecordSharingDao.find(filter, env);
		
		System.out.println("[Sharing] Group record sharings " + groupRecordSharings.size());
		
		Map<KID, PropagatedSharingData> psdByRecord = new HashMap<KID, PropagatedSharingData>();
		Map<KID, GroupRecordSharing> grsByRecord = new HashMap<KID, GroupRecordSharing>();
		
		for (GroupRecordSharing grs : groupRecordSharings)
		{
			List<String> groupHierarchyIds = MiscUtils.idListToStringList(supergroups.get(grs.getGroup().getId()).getGroupToGroupAssignmentIds());
			psdByRecord.put(grs.getRecordId(), new PropagatedSharingData(grs.getId(), uga.getId(), groupHierarchyIds));
			grsByRecord.put(grs.getRecordId(), grs);
		}
		
		sharingService.shareRecordsByGRS(psdByRecord, grsByRecord, false, authData, env);
		
		System.out.println("[Sharing] Querying group record sharings");
		
		// share with directly assigned group
		filter = new GroupRecordSharingFilter();
		Set<KID> groupIds = new HashSet<KID>();
		groupIds.add(uga.getParentGroup().getId());
		filter.setGroupIds(groupIds);
		groupRecordSharings = groupRecordSharingDao.find(filter, env);
		
		System.out.println("[Sharing] Sharing records " + groupRecordSharings.size());
		
		for (GroupRecordSharing grs : groupRecordSharings)
		{
			PropagatedSharingData psd = new PropagatedSharingData(grs.getId(), uga.getId(), new ArrayList<String>());
			
			// the direct user group sharing will not have any group to group assignments, and also, no PropagatedSharingData is needed (really?)
			sharingService.shareRecord(grs.getRecordId(), uga.getChildUser().getId(), grs.getEdit(), grs.getDelete(), grs.getReason(), false, psd, authData, env);
		}
		
		System.out.println("[Sharing] Propagating done");
	}*/
	
	/**
	 * Returns all groups to which the given user is assigned (directly or indirectly through group-to-group assignment).
	 * @param userId the ID of the user
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional(readOnly = false)
	public List<UserGroup> getGroupsForUser(KID userId, AuthData authData, EnvData env) throws KommetException
	{
		SqlRowSet rowSet = env.getJdbcTemplate().queryForRowSet("select get_user_groups('" + userId.getId() + "')");
		
		UserGroupFilter filter = new UserGroupFilter();
		
		while (rowSet.next())
		{
			filter.addUserGroupId(KID.get(rowSet.getString(0)));
		}
		
		return userGroupDao.get(filter, authData, env); 
	}

	/**
	 * Returns all groups to which the given user is assigned (directly or indirectly through group-to-group assignment).
	 * @param userId the ID of the user
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional(readOnly = false)
	public Map<KID, GroupHierarchyPath> getGroupsForUserWithHierarchy(KID userId, AuthData authData, EnvData env) throws KommetException
	{
		UserGroupAssignmentFilter filter = new UserGroupAssignmentFilter();
		filter.addChildUserId(userId);
		List<UserGroupAssignment> ugas = userGroupAssignmentDao.get(filter, authData, env);
		
		Map<KID, GroupHierarchyPath> groups = new HashMap<KID, GroupHierarchyPath>();
		
		// check all groups to which the user is assigned
		for (UserGroupAssignment uga : ugas)
		{
			GroupHierarchyPath ghp = new GroupHierarchyPath(uga.getParentGroup());
			ghp.addBottomGroupAssignmentId(uga.getId());
			
			groups.put(uga.getParentGroup().getId(), ghp);
		}
		
		groups = getParentGroups(groups, env);
		return groups;
	}

	/**
	 * Return all groups to which any of the given groups belongs.
	 * @param groups
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	private Map<KID, GroupHierarchyPath> getParentGroups(Map<KID, GroupHierarchyPath> groups, EnvData env) throws KommetException
	{
		UserGroupAssignmentFilter filter = new UserGroupAssignmentFilter();
		
		for (GroupHierarchyPath group : groups.values())
		{
			filter.addChildGroupId(group.getTopGroup().getId());
		}
		
		Map<KID, GroupHierarchyPath> parentGroups = new HashMap<KID, GroupHierarchyPath>();
		
		List<UserGroupAssignment> groupAssignments = userGroupAssignmentDao.get(filter, null, env);
		
		// for each group assignment
		for (UserGroupAssignment groupAssignment : groupAssignments)
		{
			// create a new group hierarchy path with the new parent group on top
			GroupHierarchyPath ghp = new GroupHierarchyPath(groupAssignment.getParentGroup());
			ghp.setGroupToGroupAssignmentIds(groups.get(groupAssignment.getChildGroup().getId()).getGroupToGroupAssignmentIds());
			ghp.addTopGroupId(groupAssignment.getId());
			
			parentGroups.put(ghp.getTopGroup().getId(), ghp);
		}
		
		if (!groupAssignments.isEmpty())
		{
			// retrieve recursively groups from higher levels
			//parentGroups.addAll(getParentGroups(parentGroups, env));
			return getParentGroups(parentGroups, env);
		}
		
		// add old groups to the collection as well
		parentGroups.putAll(groups);
		
		return parentGroups;
	}
	
	@Transactional
	public UserGroupAssignment assignGroupToGroup (KID childGroupId, KID parentGroupId, AuthData authData, EnvData env) throws KommetException
	{
		return assignGroupToGroup(childGroupId, parentGroupId, authData, env, false);
	}

	@Transactional
	public UserGroupAssignment assignGroupToGroup (KID childGroupId, KID parentGroupId, AuthData authData, EnvData env, boolean isApplyImmediately) throws KommetException
	{
		UserGroupAssignment uga = new UserGroupAssignment();
		
		UserGroup parentGroup = new UserGroup();
		parentGroup.setId(parentGroupId);
		uga.setParentGroup(parentGroup);
		
		UserGroup childGroup = new UserGroup();
		childGroup.setId(childGroupId);
		uga.setChildGroup(childGroup);
		
		// this sharing will be propagated later by a scheduled job UgaApplierJob
		uga.setIsApplyPending(true);
		
		try
		{
			uga = userGroupAssignmentDao.save(uga, authData, env);
		}
		catch (UniqueCheckViolationException e)
		{
			if (env.getType(e.getUniqueCheck().getTypeId()).getKeyPrefix().equals(KeyPrefix.get(KID.USER_GROUP_ASSIGNMENT_PREFIX)))
			{
				throw new UserGroupAssignmentException("Group is already assigned to group");
			}
			else
			{
				throw e;
			}
		}
		
		// this sharing will be propagated later by a scheduled job UgaApplierJob
		if (isApplyImmediately)
		{
			propagateGroupSharingsToGroup(uga, authData, env);
		}
		
		return uga;
	}
	
	private void propagateGroupSharingsToGroup(UserGroupAssignment uga, AuthData authData, EnvData env) throws KommetException
	{
		System.out.println("[Sharing] Propagating group sharing to group " + uga.getId());
		
		// find all supergroups of the parent group
		Map<KID, GroupHierarchyPath> supergroups = new HashMap<KID, GroupHierarchyPath>();
		GroupHierarchyPath ghp = new GroupHierarchyPath(uga.getParentGroup());
		supergroups.put(uga.getParentGroup().getId(), ghp);
		supergroups.putAll(getParentGroups(supergroups, env));
		
		GroupRecordSharingFilter filter = new GroupRecordSharingFilter();
		filter.setGroupIds(supergroups.keySet());
		filter.setExcludeSystemObjects(true);
		
		// find all records shared with these groups
		// TODO is it now a problem that there can be millions of them? perhaps it should be done by a DB function?
		List<GroupRecordSharing> groupRecordSharings = groupRecordSharingDao.find(filter, env);
		
		System.out.println("[Sharing] Supergroup record sharings " + groupRecordSharings.size());
		
		int i = 0;
		for (GroupRecordSharing groupRecordSharing : groupRecordSharings)
		{
			PropagatedSharingData psd = new PropagatedSharingData(groupRecordSharing.getId(), null, MiscUtils.toList(uga.getId().getId()));
			
			if (i % 100 == 0)
			{
				System.out.println("[Sharing] Record " + i + " / " + groupRecordSharings.size());
			}
			
			i++;
			
			// share each record with the child group
			sharingService.shareRecordWithGroup(groupRecordSharing.getRecordId(), uga.getChildGroup().getId(), groupRecordSharing.getEdit(), groupRecordSharing.getDelete(), groupRecordSharing.getReason(), false, groupRecordSharing.getSharingRule() != null ? groupRecordSharing.getSharingRule().getId() : null, psd, authData, env);
		}
		
		System.out.println("[Sharing] Propagating done");
	}

	@Transactional
	public void unassignUserFromGroup (KID userId, KID groupId, AuthData authData, EnvData env) throws KommetException
	{
		UserGroupAssignmentFilter filter = new UserGroupAssignmentFilter();
		filter.addChildUserId(userId);
		filter.addParentGroupId(groupId);
		
		List<UserGroupAssignment> assignments = userGroupAssignmentDao.get(filter, null, env);
		
		System.out.println("[sharing] Removing " + assignments.size() + " user group assignments");
		
		if (!assignments.isEmpty())
		{
			// propagate unassignment to user record sharings
			UserRecordSharingFilter ursFilter = new UserRecordSharingFilter();
			
			for (UserGroupAssignment a : assignments)
			{
				ursFilter.addUserGroupAssignmentId(a.getId());
			}
			
			List<UserRecordSharing> urss = userRecordSharingDao.find(ursFilter, env);
			
			System.out.println("[sharing] Deleting " + urss.size() + " URS");
			
			userRecordSharingDao.delete(urss, null, env);
			
			// remove the user group assignment
			userGroupAssignmentDao.delete(assignments.get(0).getId(), null, env);
		}
	}
	
	@Transactional
	public void unassignUserGroupFromGroup (KID childGroupId, KID parentGroupId, boolean isDeterred, AuthData authData, EnvData env) throws KommetException
	{
		UserGroupAssignmentFilter filter = new UserGroupAssignmentFilter();
		filter.addChildGroupId(childGroupId);
		filter.addParentGroupId(parentGroupId);
		
		List<UserGroupAssignment> assignments = userGroupAssignmentDao.get(filter, null, env);
		if (!assignments.isEmpty())
		{
			if (assignments.size() > 1)
			{
				throw new KommetException("Illegal state: more than one user group assignment between child group " + childGroupId + " and parent group " + parentGroupId);
			}
			
			if (isDeterred)
			{
				// schedule the assignment to be deleted at a later time
				assignments.get(0).setIsRemovePending(true);
				
				// TODO can we use user's authData here?
				userGroupAssignmentDao.save(assignments.get(0), authData, env);
			}
			else
			{
				System.out.println("[sharing] Looking for URS");
				
				userRecordSharingDao.deleteSharingsForHierarchy(assignments.get(0).getId().getId(), env);
				
				System.out.println("[sharing] Sharings deleted");
				
				// delete to group-to-group assignment
				// TODO can we use user's authData here?
				userGroupAssignmentDao.delete(assignments.get(0).getId(), authData, env);
				
				System.out.println("[sharing] UGA deleted");
			}
		}
	}
	
	@Transactional
	public int propagateDeleteUserGroupAssignment (KID ugaId, int limit, EnvData env) throws KommetException
	{
		System.out.println("[sharing] Deleting assignments for UGA " + ugaId);
		int itemsRemaining = userRecordSharingDao.deleteSharingsForHierarchy(ugaId.getId(), limit, env);
		System.out.println("[sharing] Deleted some assignments for UGA " + ugaId);
		return itemsRemaining;
	}
	
	@Transactional
	public UserGroup save (UserGroup group, AuthData authData, EnvData env) throws KommetException
	{
		if (!ValidationUtil.isValidOptionallyQualifiedResourceName(group.getName()))
		{
			throw new KommetException("Invalid user group name " + group.getName());
		}
		
		return userGroupDao.save(group, authData, env);
	}
	
	@Transactional(readOnly = true)
	public List<UserGroup> get (UserGroupFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		return userGroupDao.get(filter, authData, env);
	}
	
	@Transactional(readOnly = true)
	public List<UserGroupAssignment> getUserGroupAssignments (UserGroupAssignmentFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		return userGroupAssignmentDao.get(filter, authData, env);
	}
	
	@Transactional(readOnly = true)
	public UserGroup get (KID groupId, AuthData authData, EnvData env) throws KommetException
	{
		return userGroupDao.get(groupId, authData, env);
	}
	
	@Transactional(readOnly = true)
	public UserGroup getByName (String name, AuthData authData, EnvData env) throws KommetException
	{
		UserGroupFilter filter = new UserGroupFilter();
		filter.setName(name);
		List<UserGroup> groups = userGroupDao.get(filter, authData, env);
		return groups.isEmpty() ? null : groups.get(0);
	}
	
	@Transactional
	public void delete (KID id, AuthData authData, EnvData env) throws KommetException
	{
		// TODO this method is a bit suboptimal, could be done by database constraints if there was a constraint
		
		// on the field UserRecordSharing.groupRecordAssignmentId (it would have to be an type reference, not an ID)
		// find all user group assignments for this group
		GroupRecordSharingFilter grsFilter = new GroupRecordSharingFilter();
		grsFilter.addGroupId(id);
		List<GroupRecordSharing> groupRecordSharings = groupRecordSharingDao.find(grsFilter, env);
		
		UserRecordSharingFilter ursFilter = null;
		
		if (!groupRecordSharings.isEmpty())
		{
			ursFilter = new UserRecordSharingFilter();
			for (GroupRecordSharing grs : groupRecordSharings)
			{
				ursFilter.addGroupRecordSharingId(grs.getId());
			}
			userRecordSharingDao.delete(userRecordSharingDao.find(ursFilter, env), null, env);
		}
		
		// remove all sharings being a propagation of this groups memberships
		
		// find all group memberships for this group
		UserGroupAssignmentFilter ugaFilter = new UserGroupAssignmentFilter();
		ugaFilter.addChildGroupId(id);
		List<UserGroupAssignment> ugas = userGroupAssignmentDao.get(ugaFilter, null, env);
		if (!ugas.isEmpty())
		{
			for (UserGroupAssignment uga : ugas)
			{
				ursFilter = new UserRecordSharingFilter();
				ursFilter.setGroupSharingHierarchy(uga.getId().getId());
				
				// delete URS referencing this group-group membership
				userRecordSharingDao.delete(userRecordSharingDao.find(ursFilter, env), null, env);
			}
		}
		
		// finally delete the group
		userGroupDao.delete(id, authData, env);
	}

	@Transactional(readOnly = true)
	public boolean isUserInGroup(KID userId, KID groupId, boolean onlyDirectAssignments, EnvData env) throws KommetException
	{
		if (onlyDirectAssignments)
		{
			UserGroupAssignmentFilter filter = new UserGroupAssignmentFilter();
			filter.addParentGroupId(groupId);
			filter.addChildUserId(userId);
			
			return !userGroupAssignmentDao.get(filter, null, env).isEmpty();
		}
		else
		{
			return userGroupAssignmentDao.isUserInGroup(userId, groupId, env);
		}
	}
	
	@Transactional(readOnly = true)
	public boolean isGroupInGroup(KID childGroupId, KID parentGroupId, boolean onlyDirectAssignments, EnvData env) throws KommetException
	{
		if (onlyDirectAssignments)
		{
			UserGroupAssignmentFilter filter = new UserGroupAssignmentFilter();
			filter.addParentGroupId(parentGroupId);
			filter.addChildGroupId(childGroupId);
			
			return !userGroupAssignmentDao.get(filter, null, env).isEmpty();
		}
		else
		{
			return userGroupAssignmentDao.isGroupInGroup(childGroupId, parentGroupId, env);
		}
	}

	@Transactional(readOnly = true)
	public List<UserGroupAssignment> getGroupMembers(KID groupId, AuthData authData, EnvData env) throws KommetException
	{
		UserGroupAssignmentFilter filter = new UserGroupAssignmentFilter();
		filter.addParentGroupId(groupId);
		filter.setFetchExtendedData(true);
		return userGroupAssignmentDao.get(filter, authData, env);
	}

	public void deleteUserGroupAssignment (KID assignmentId, AuthData authData, EnvData env) throws KommetException
	{
		userGroupAssignmentDao.delete(assignmentId, authData, env);
	}
}