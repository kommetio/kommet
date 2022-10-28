/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.uch;

public class AmbiguousUserCascadeHierarchySettingException extends UserCascadeHierarchyException
{
	private static final long serialVersionUID = -261278529658706502L;
	public static final String ERROR_MESSAGE = "More than one application user cascade hierarchy exists for the given user";

	public AmbiguousUserCascadeHierarchySettingException()
	{
		super(ERROR_MESSAGE);
	}
}