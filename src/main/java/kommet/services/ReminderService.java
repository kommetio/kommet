/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.services;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.auth.UserService;
import kommet.basic.Notification;
import kommet.basic.Reminder;
import kommet.basic.User;
import kommet.dao.ReminderDao;
import kommet.data.DataAccessUtil;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.FieldValidationException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.DataType;
import kommet.emailing.EmailService;
import kommet.env.EnvData;
import kommet.filters.ReminderFilter;
import kommet.notifications.NotificationService;
import kommet.reminders.ReminderCheckerJob;
import kommet.reminders.ReminderCheckerJobDetail;

@Service
public class ReminderService
{
	@Inject
	ReminderDao dao;
	
	@Inject
	DataService dataService;
	
	@Inject
	SchedulerFactoryBean schedulerFactory;
	
	@Inject
	NotificationService notificationService;
	
	@Inject
	EmailService emailService;
	
	@Inject
	UserService userService;
	
	private static final Logger log = LoggerFactory.getLogger(ReminderService.class);
	
	public static final String REMINDER_CHECKER_JOB_PREFIX = "reminder-checker";
	
	@Transactional
	public void runReminders (AuthData authData, EnvData env) throws KommetException
	{
		Date currentDate = new Date();
		
		for (Reminder reminder : get(new ReminderFilter(), authData, env))
		{	
			Record record = isReminderToSend(reminder, currentDate, authData, env);
			if (record != null)
			{
				// send reminder
				sendReminder(record, reminder, authData, env);
			}
		}
	}
	
	public void scheduleReminderChecker(EnvData env) throws SchedulerException
	{
		String jobName = REMINDER_CHECKER_JOB_PREFIX + "-" + env.getId();
		String jobGroup = "reminder-checkers-" + env.getId();
		
		// check if a job for this task does not already exist
		JobDetail jobDetail = schedulerFactory.getScheduler().getJobDetail(JobKey.jobKey(jobName, jobGroup));
		
		if (jobDetail != null)
		{
			// be sure to use method deleteJob instead of unscheduleJob
			schedulerFactory.getScheduler().deleteJob(JobKey.jobKey(jobName, jobGroup));
		}
		
		ReminderCheckerJobDetail job = new ReminderCheckerJobDetail(this, env);
		job.setName(jobName);
		job.setGroup(jobGroup);
		job.setJobClass(ReminderCheckerJob.class);

		// Trigger the job to run now, and then repeat every 2 minutes
		CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(jobName + "-trigger", jobGroup).withSchedule(CronScheduleBuilder.cronSchedule("0 */2 * * * ?")).build();
		schedulerFactory.getScheduler().scheduleJob(job, trigger);
	}
	
	@SuppressWarnings("deprecation")
	public Record isReminderToSend(Reminder reminder, Date currentDate, AuthData authData, EnvData env) throws KommetException
	{
		if (reminder.getStatus() == null)
		{
			throw new KommetException("Reminder status not set, cannot determine if reminder is to be sent");
		}
		
		// find associated record
		Type type = env.getTypeByRecordId(reminder.getRecordId());
		
		if (type == null)
		{
			throw new KommetException("Type not found by record ID " + reminder.getRecordId());
		}
		
		List<Record> records = dataService.getRecords(Arrays.asList(reminder.getRecordId()), type, DataAccessUtil.getReadableFieldApiNamesForQuery(type, authData, env, false), authData, env);
		
		if (records.isEmpty())
		{
			log.info("Record " + reminder.getRecordId() + " referenced by reminder " + reminder.getId() + " does not exist. Deleting the reminder");
			
			// the records for which the remionde was created have been removed, so we will remove the reminder as well
			delete(reminder.getId(), authData, env);
			
			log.debug("Reminder " + reminder.getId() + "deleted");
			
			return null;
			//throw new KommetException("Record " + reminder.getRecordId() + " referenced by reminder " + reminder.getId() + " not found");
		}
		
		Field field = type.getField(reminder.getReferencedField());
		
		if (!field.getDataTypeId().equals(DataType.DATE) && !field.getDataTypeId().equals(DataType.DATETIME))
		{
			throw new FieldValidationException("Field " + field.getApiName() + " referenced by a reminder is not a date/datetime field");
		}
		
		Date refFieldValue = (Date)records.get(0).getField(field.getApiName());
		
		if (refFieldValue == null)
		{
			return null;
		}
		
		Date referencedDate = getIntervalForCalendar(currentDate, reminder);
		refFieldValue.setSeconds(0);
		
		// the reminder qualified for sending if the date of the task has already passed and the task's status is not "sent"
		if (referencedDate.compareTo(refFieldValue) == 0 || (referencedDate.compareTo(refFieldValue) > 0 && !reminder.getStatus().equals("sent")))
		{
			return records.get(0);
		}
		else
		{
			return null;
		}
	}
	
	private void sendReminder(Record record, Reminder reminder, AuthData authData, EnvData env) throws KommetException
	{
		if (reminder.getMedia().equals("notification"))
		{
			Notification n = new Notification();
			n.setTitle(reminder.getTitle());
			n.setText(reminder.getContent());
			n.setAssignee(reminder.getAssignedUser());
			notificationService.save(n, authData, env);
		}
		else if (reminder.getMedia().equals("email"))
		{
			// get full user
			User assignee = userService.getUser(reminder.getAssignedUser().getId(), env);
			emailService.sendEmail(reminder.getTitle(), assignee.getEmail(), reminder.getContent(), null);
		}
		else
		{
			throw new KommetException("Reminder type " + reminder.getMedia() + " not supported");
		}
		
		reminder.setStatus("sent");
		save(reminder, authData, env);
	}

	private Date getIntervalForCalendar(Date referencedDate, Reminder reminder) throws KommetException
	{
		Calendar c = Calendar.getInstance();
		c.setTime(referencedDate);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		
		Integer value = reminder.getIntervalValue();
		Integer unit = null; 
		
		if ("year".equals(reminder.getIntervalUnit()))
		{
			unit = Calendar.YEAR;
		}
		else if ("month".equals(reminder.getIntervalUnit()))
		{
			unit = Calendar.MONTH;
		}
		else if ("week".equals(reminder.getIntervalUnit()))
		{
			unit = Calendar.DATE;
			value = 7;
		}
		else if ("day".equals(reminder.getIntervalUnit()))
		{
			unit = Calendar.DATE;
		}
		else if ("hour".equals(reminder.getIntervalUnit()))
		{
			unit = Calendar.HOUR;
		}
		else if ("minute".equals(reminder.getIntervalUnit()))
		{
			unit = Calendar.MINUTE;
		}
		else
		{
			throw new KommetException("Unsupported reminder interval unit " + reminder.getIntervalUnit());
		}
		
		c.add(unit, -1 * value);
		
		return c.getTime();
	}

	@Transactional
	public Reminder save (Reminder reminder, AuthData authData, EnvData env) throws KommetException
	{
		if (reminder.getRecordId() == null)
		{
			throw new FieldValidationException("Record ID not set on reminder");
		}
		
		if (reminder.getReferencedField() == null)
		{
			throw new FieldValidationException("Field ID not set on reminder");
		}
		
		Type type = env.getTypeByRecordId(reminder.getRecordId());
		
		if (type == null)
		{
			throw new FieldValidationException("Did not find type by record ID " + reminder.getRecordId());
		}
		
		Field field = type.getField(reminder.getReferencedField()); 
		
		if (field == null)
		{
			throw new FieldValidationException("Field with ID " + reminder.getReferencedField() + " does not exist on type " + type.getQualifiedName());
		}
		
		if (!field.getDataTypeId().equals(DataType.DATE) && !field.getDataTypeId().equals(DataType.DATETIME))
		{
			throw new FieldValidationException("Field " + field.getApiName() + " referenced by a reminder is not a date/datetime field");
		}
		
		return dao.save(reminder, authData, env);
	}
	
	@Transactional(readOnly = true)
	public List<Reminder> get (ReminderFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		return dao.find(filter, authData, env);
	}

	@Transactional
	public void delete(KID id, AuthData authData, EnvData env) throws KommetException
	{
		dao.delete(id, authData, env);
	}

	@Transactional(readOnly = true)
	public Reminder get(KID id, AuthData authData, EnvData env) throws KommetException
	{
		ReminderFilter filter = new ReminderFilter();
		filter.addReminderId(id);
		List<Reminder> reminders = dao.find(filter, authData, env);
		return reminders.isEmpty() ? null : reminders.get(0);
	}

	@Transactional
	public void delete(List<Reminder> reminders, AuthData authData, EnvData env) throws KommetException
	{
		dao.delete(reminders, authData, env);
	}
}