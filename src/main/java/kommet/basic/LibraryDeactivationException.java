/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import java.util.List;

import kommet.services.LibraryService.LibraryItemDeleteStatus;

public class LibraryDeactivationException extends LibraryException
{
	private static final long serialVersionUID = 73782199377855138L;
	private List<LibraryItemDeleteStatus> statuses;
	
	public LibraryDeactivationException(List<LibraryItemDeleteStatus> statuses)
	{
		super("Deactivation failed");
		this.statuses = statuses;
	}

	public List<LibraryItemDeleteStatus> getStatuses()
	{
		return statuses;
	}
}