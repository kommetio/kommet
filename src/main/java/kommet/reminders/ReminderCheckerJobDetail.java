/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.reminders;

import org.quartz.impl.JobDetailImpl;

import kommet.env.EnvData;
import kommet.services.ReminderService;

public class ReminderCheckerJobDetail extends JobDetailImpl
{
	private static final long serialVersionUID = 5166783050836904869L;
	
	private EnvData env;
	private ReminderService reminderService;
	
	public ReminderCheckerJobDetail(ReminderService reminderService, EnvData env)
	{
		super();
		this.env = env;
		this.reminderService = reminderService;
	}
	
	public ReminderService getReminderService()
	{
		return reminderService;
	}

	public EnvData getEnv()
	{
		return env;
	}
}