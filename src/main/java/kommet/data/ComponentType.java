/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

public enum ComponentType
{
	CLASS(0),
	VIEW(1),
	TYPE(2),
	FIELD(3),
	LAYOUT(4),
	VALIDATION_RULE(5),
	UNIQUE_CHECK(6),
	APP(7),
	SCHEDULED_TASK(8),
	PROFILE(9),
	USER_GROUP(10),
	WEB_RESOURCE(11),
	VIEW_RESOURCE(12),
	ACTION(13),
	
	/**
	 * This component type represents a collection of records. It is not really a separate component type, but it is used
	 * as such in the deployment process where a file containing serialized records is a separate library item/file.
	 */
	RECORD_COLLECTION(14);
	
	private int id;
	
	private ComponentType (int id)
	{
		this.id = id;
	}

	public static ComponentType fromExt(String ext) throws KommetException
	{
		if (FileExtension.CLASS_EXT.equals(ext))
		{
			return CLASS;
		}
		else if (FileExtension.VIEW_EXT.equals(ext))
		{
			return VIEW;
		}
		else if (FileExtension.TYPE_EXT.equals(ext))
		{
			return TYPE;
		}
		else if (FileExtension.FIELD_EXT.equals(ext))
		{
			return FIELD;
		}
		else if (FileExtension.LAYOUT_EXT.equals(ext))
		{
			return LAYOUT;
		}
		else if (FileExtension.SCHEDULED_TASK_EXT.equals(ext))
		{
			return ComponentType.SCHEDULED_TASK;
		}
		else if (FileExtension.VALIDATION_RULE_EXT.equals(ext))
		{
			return VALIDATION_RULE;
		}
		else if (FileExtension.UNIQUE_CHECK_EXT.equals(ext))
		{
			return UNIQUE_CHECK;
		}
		else if (FileExtension.APP_EXT.equals(ext))
		{
			return APP;
		}
		else if (FileExtension.PROFILE_EXT.equals(ext))
		{
			return ComponentType.PROFILE;
		}
		else if (FileExtension.USER_GROUP_EXT.equals(ext))
		{
			return USER_GROUP;
		}
		else if (FileExtension.VIEW_RESOURCE_EXT.equals(ext))
		{
			return ComponentType.VIEW_RESOURCE;
		}
		else if (FileExtension.WEB_RESOURCE_EXT.equals(ext))
		{
			return WEB_RESOURCE;
		}
		else if (FileExtension.ACTION_EXT.equals(ext))
		{
			return ACTION;
		}
		else if (FileExtension.RECORD_COLLECTION_EXT.equals(ext))
		{
			return RECORD_COLLECTION;
		}
		else
		{
			throw new KommetException("Cannot deduce component type from extension '" + ext + "'");
		}
	}

	public int getId()
	{
		return id;
	}
}