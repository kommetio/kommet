/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.data.KommetException;

public enum TaskPriority
{
	IMMEDIATE(0),
	URGENT(1),
	IMPORTANT(2),
	NORMAL(3),
	LOW(4);
	
	private int priority;
	
	private TaskPriority (int priority)
	{
		this.priority = priority;
	}
	
	public int getPriority()
	{
		return this.priority;
	}
	
	public static TaskPriority valueOf (int priority) throws KommetException
	{
		switch (priority)
		{
			case 0: return TaskPriority.IMMEDIATE;
			case 1: return TaskPriority.URGENT;
			case 2: return TaskPriority.IMPORTANT;
			case 3: return TaskPriority.NORMAL;
			case 4: return TaskPriority.LOW;
			default: throw new KommetException("Unrecognized task priority value " + priority);
		}
	}
}