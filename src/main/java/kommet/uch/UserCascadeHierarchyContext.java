/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.uch;

import org.springframework.util.StringUtils;

/**
 * Represents an active context of a UCH record.
 * @author Radek Krawiec
 * @since 2014
 */
public enum UserCascadeHierarchyContext
{
	ENVIRONMENT,
	APPLICATION,
	PROFILE,
	LOCALE,
	USER_GROUP,
	USER;

	public static UserCascadeHierarchyContext fromString(String name) throws UserCascadeHierarchyException
	{
		if (!StringUtils.hasText(name))
		{
			throw new UserCascadeHierarchyException("Cannot create User Cascade Hierarchy context from empty string");
		}
		
		String lcName = name.toLowerCase();
		
		if ("application".equals(lcName))
		{
			return APPLICATION;
		}
		else if ("environment".equals(lcName))
		{
			return ENVIRONMENT;
		}
		else if ("profile".equals(lcName))
		{
			return PROFILE;
		}
		else if ("user group".equals(lcName))
		{
			return USER_GROUP;
		}
		else if ("user".equals(lcName))
		{
			return USER;
		}
		else if ("locale".equals(lcName))
		{
			return LOCALE;
		}
		else
		{
			throw new UserCascadeHierarchyException("Unsupported UCH context name " + name);
		}
	}
	
	/**
	 * Get the rank number for the current context.
	 * @return
	 * @throws UserCascadeHierarchyException
	 */
	public Integer getRank() throws UserCascadeHierarchyException
	{	
		// rank numbers are full hundred numbers, in order to give us the possibility to
		// add new ranks between them in the future if necessary
		
		if (this.equals(USER))
		{
			// user has highest (most specific) rank
			return 100;
		}
		else if (this.equals(USER_GROUP))
		{
			return 200;
		}
		else if (this.equals(LOCALE))
		{
			return 300;
		}
		else if (this.equals(PROFILE))
		{
			return 400;
		}
		else if (this.equals(APPLICATION))
		{
			return 500;
		}
		else if (this.equals(ENVIRONMENT))
		{
			return 600;
		}
		else
		{
			throw new UserCascadeHierarchyException("Rank not defined for UCH context " + this.name());
		}
		
	}
	
	@Override
	public String toString()
	{
		if (USER_GROUP.equals(this))
		{
			return "user group";
		}
		else
		{
			return this.name().toLowerCase();
		}
	}
}