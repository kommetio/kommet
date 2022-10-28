/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.scheduler;

import javax.inject.Inject;

import org.quartz.SchedulerContext;
import org.quartz.spi.TriggerFiredBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import kommet.data.sharing.UgaApplierJob;
import kommet.data.sharing.UgaRemoverJob;
import kommet.reminders.ReminderCheckerJob;
import kommet.services.ReminderService;
import kommet.services.UserGroupService;

public class QuartzJobFactory extends org.springframework.scheduling.quartz.SpringBeanJobFactory
{
	private String[] ignoredUnknownProperties;

	private SchedulerContext schedulerContext;

	@Inject
	private ApplicationContext applicationContext;
	
	private static final Logger log = LoggerFactory.getLogger(QuartzJobFactory.class);


	@Override
	public void setIgnoredUnknownProperties(String ... ignoredUnknownProperties)
	{
		super.setIgnoredUnknownProperties(ignoredUnknownProperties);
		this.ignoredUnknownProperties = ignoredUnknownProperties;
	}

	@Override
	public void setSchedulerContext(SchedulerContext schedulerContext)
	{
		super.setSchedulerContext(schedulerContext);
		this.schedulerContext = schedulerContext;
	}

	/**
	 * An implementation of SpringBeanJobFactory that retrieves the bean from
	 * the Spring context so that autowiring and transactions work
	 * 
	 * This method is overridden.
	 * 
	 * @see org.springframework.scheduling.quartz.SpringBeanJobFactory#createJobInstance(org.quartz.spi.TriggerFiredBundle)
	 */
	@Override
	protected Object createJobInstance(TriggerFiredBundle bundle) throws Exception
	{	
		Object job = null;
		
		try
		{
			// first try to get the bean from the normal app context
			job = applicationContext.getBean(bundle.getJobDetail().getKey().getName());
		}
		catch (Exception e)
		{
			job = createBean(bundle.getJobDetail().getKey().getName());
		}
		
		if (job == null)
		{
			throw new ScheduledTaskException("Bean with name " + bundle.getJobDetail().getKey().getName() + " not found in application context");
		}
	
		BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(job);
		if (isEligibleForPropertyPopulation(bw.getWrappedInstance()))
		{
			MutablePropertyValues pvs = new MutablePropertyValues();
			if (this.schedulerContext != null)
			{
				pvs.addPropertyValues(this.schedulerContext);
			}
			pvs.addPropertyValues(bundle.getJobDetail().getJobDataMap());
			pvs.addPropertyValues(bundle.getTrigger().getJobDataMap());
			if (this.ignoredUnknownProperties != null)
			{
				for (String propName : this.ignoredUnknownProperties)
				{
					if (pvs.contains(propName) && !bw.isWritableProperty(propName))
					{
						pvs.removePropertyValue(propName);
					}
				}
				bw.setPropertyValues(pvs);
			}
			else
			{
				bw.setPropertyValues(pvs, true);
			}
		}
		return job;
	}
	
	private Object createBean (String jobName) throws ScheduledTaskException
	{
		log.debug("Creating bean for job " + jobName);
		GenericApplicationContext ctx = new GenericApplicationContext();
		
		BeanDefinitionBuilder bDBuilder = null;
		
		if (jobName.startsWith(ReminderService.REMINDER_CHECKER_JOB_PREFIX))
		{
			bDBuilder = BeanDefinitionBuilder.rootBeanDefinition(ReminderCheckerJob.class);
		}
		else if (jobName.startsWith(UserGroupService.UGA_APPLIER_JOB_PREFIX))
		{
			bDBuilder = BeanDefinitionBuilder.rootBeanDefinition(UgaApplierJob.class);
		}
		else if (jobName.startsWith(UserGroupService.UGA_REMOVER_JOB_PREFIX))
		{
			bDBuilder = BeanDefinitionBuilder.rootBeanDefinition(UgaRemoverJob.class);
		}
		else
		{
			bDBuilder = BeanDefinitionBuilder.rootBeanDefinition(ScheduledQuartzJob.class);
		}
		
		ctx.registerBeanDefinition(jobName, bDBuilder.getBeanDefinition());
		ctx.refresh();
		//ScheduledQuartzJob jobBean = (ScheduledQuartzJob)ctx.getBean(jobName);
		Object jobBean = ctx.getBean(jobName);
		
		ctx.close();
		
		if (jobBean == null)
		{
			throw new ScheduledTaskException("Error creating bean for job " + jobName);
		}
		else
		{
			log.debug("Job bean created " + jobBean);
			return jobBean;
		}
	}
}