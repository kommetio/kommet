/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.scheduler;

/**
 * Error that occurs during execution of a scheduled task.
 * @author Radek Krawiec
 * @date 24/05/2014
 */
public class ScheduledTaskExecutionException extends ScheduledTaskException
{
	private static final long serialVersionUID = 1137106568649765393L;

	public ScheduledTaskExecutionException(String msg)
	{
		super(msg);
	}
}