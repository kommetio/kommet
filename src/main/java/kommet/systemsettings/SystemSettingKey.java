/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.systemsettings;

/**
 * List of names of system settings.
 * @author Radek Krawiec
 * @created 17-05-2014
 *
 */
public enum SystemSettingKey
{
	// The default locale of the environment in form of a string, e.g. "EN_GB"
	DEFAULT_ENV_LOCALE,
	// If this setting is set to true, when a page is deleted that is defined as a standard action for an object,
	// the default action for this action will be assigned instead.
	// If the setting is false, the system will not allow for deleting such action.
	REASSIGN_DEFAULT_ACTION_ON_ACTION_DELETE,
	BLANK_LAYOUT_ID,
	MIN_PASSWORD_LENGTH,
	IGNORE_NON_EXISTING_FIELD_LABELS,
	DEFAULT_ERROR_VIEW_ID;
	
	@Override
	public String toString()
	{
		return name();
	}
}