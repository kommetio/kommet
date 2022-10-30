/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.services;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.basic.Reminder;
import kommet.basic.Task;
import kommet.basic.UserRecordSharing;
import kommet.config.UserSettingKeys;
import kommet.dao.TaskDao;
import kommet.data.FieldValidationException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.sharing.SharingService;
import kommet.data.sharing.UserRecordSharingDao;
import kommet.data.sharing.UserRecordSharingFilter;
import kommet.emailing.EmailException;
import kommet.emailing.EmailService;
import kommet.env.EnvData;
import kommet.filters.ReminderFilter;
import kommet.filters.TaskFilter;
import kommet.uch.UserCascadeHierarchyService;

@Service
public class TaskService
{
	@Inject
	TaskDao taskDao;
	
	@Inject
	UserRecordSharingDao ursDao;
	
	@Inject
	SharingService sharingService;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	@Inject
	EmailService emailService;
	
	@Inject
	ReminderService reminderService;
	
	private static final String TASK_ASSIGNEE_SHARING_REASON = "Is assigned to the task";
	
	private static final Logger log = LoggerFactory.getLogger(TaskService.class);
	
	@Transactional
	public Task save (Task task, AuthData authData, EnvData env) throws KommetException
	{
		if (task.getAssignedGroup() != null && task.getAssignedUser() != null)
		{
			throw new FieldValidationException("Both assigned user and assigned group cannot be set on a task");
		}
		
		boolean isNewTask = (task.getId() == null);
		boolean isAssigneeChanged = isNewTask;
		
		Task oldTask = null;
		
		if (!isNewTask)
		{
			// get the old version of the task
			oldTask = get(task.getId(), authData, env);
		}
		
		task = taskDao.save(task, authData, env);
		
		if (!isNewTask)
		{	
			isAssigneeChanged = (oldTask.getAssigneeId() == null && task.getAssigneeId() != null) || (oldTask.getAssigneeId() != null && task.getAssigneeId() == null) || (oldTask.getAssigneeId() != null && task.getAssigneeId() != null && !oldTask.getAssigneeId().equals(task.getAssigneeId()));
			
			if (isAssigneeChanged)
			{
				// if assigned user changed
				if (oldTask.getAssignedUser() != null)
				{	
					// unshare task with previous assignee
					UserRecordSharingFilter filter = new UserRecordSharingFilter();
					filter.addUserId(oldTask.getAssignedUser().getId());
					filter.addReason(TASK_ASSIGNEE_SHARING_REASON);
					filter.addRecordId(oldTask.getId());
					filter.setIsGeneric(false);
					
					// find previous sharings
					List<UserRecordSharing> sharings = ursDao.find(filter, env);
					if (!sharings.isEmpty())
					{
						// delete the sharing
						ursDao.delete(sharings, authData, env);
					}
				}
				else if (oldTask.getAssignedGroup() != null)
				{
					log.debug("Unsharing with group " + oldTask.getAssignedGroup().getName() + "/" + oldTask.getAssignedGroup().getId());
					sharingService.unshareRecordWithGroup(oldTask.getId(), oldTask.getAssignedGroup().getId(), false, TASK_ASSIGNEE_SHARING_REASON, authData, env);
				}
			}
		}
		
		if (isAssigneeChanged)
		{
			// share the task with the new assigned user or group
			if (task.getAssignedUser() != null)
			{
				// give read and edit permission, but not delete
				sharingService.shareRecord(task.getId(), task.getAssignedUser().getId(), true, false, authData, TASK_ASSIGNEE_SHARING_REASON, false, env);
			}
			else if (task.getAssignedGroup() != null)
			{
				// give read and edit permission, but not delete
				sharingService.shareRecordWithGroup(task.getId(), task.getAssignedGroup().getId(), true, false, TASK_ASSIGNEE_SHARING_REASON, false, authData, env);
			}
			
			if (uchService.getUserSettingAsBoolean(UserSettingKeys.KM_SYS_NEW_TASK_EMAIL_NOTIFICATION, authData, AuthData.getRootAuthData(env), env))
			{
				sendNewTaskNotification(task, authData, env);
			}
			
			reinitTaskReminders(task, authData, env);
		}
		
		return task;
	}
	
	private void reinitTaskReminders(Task task, AuthData authData, EnvData env) throws KommetException
	{
		ReminderFilter filter = new ReminderFilter();
		filter.addRecordId(task.getId());
		
		for (Reminder reminder : reminderService.get(filter, authData, env))
		{
			reminder.setAssignedUser(task.getAssignedUser());
			reminder.setAssignedGroup(task.getAssignedGroup());
			reminderService.save(reminder, authData, env);
		}
	}

	private void sendNewTaskNotification(Task task, AuthData authData, EnvData env) throws EmailException
	{
		String subject = authData.getI18n().get("tasks.newtask.email.subject");
		String contentPre = authData.getI18n().get("tasks.newtask.email.content.pre");
		
		if (task.getAssignedUser() != null)
		{
			emailService.sendEmail(subject, task.getAssignedUser().getEmail(), contentPre, null);
		}
	}

	@Transactional(readOnly = true)
	public List<Task> get (TaskFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		return taskDao.find(filter, authData, env);
	}

	@Transactional
	public void delete(KID id, AuthData authData, EnvData env) throws KommetException
	{
		// find reminders for this task and remove them
		ReminderFilter filter = new ReminderFilter();
		filter.addRecordId(id);
		reminderService.delete(reminderService.get(filter, authData, env), authData, env);
		
		taskDao.delete(id, authData, env);
	}
	
	@Transactional(readOnly = true)
	public Task get(KID id, AuthData authData, EnvData env) throws KommetException
	{
		return get(id, false, authData, env);
	}

	@Transactional(readOnly = true)
	public Task get(KID id, boolean fetchAssigneeData, AuthData authData, EnvData env) throws KommetException
	{
		TaskFilter filter = new TaskFilter();
		filter.addTaskId(id);
		filter.setFetchAssigneeData(fetchAssigneeData);
		List<Task> tasks = taskDao.find(filter, authData, env);
		return tasks.isEmpty() ? null : tasks.get(0);
	}
}