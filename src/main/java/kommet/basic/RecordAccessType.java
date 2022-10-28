/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

/**
 * Values of the accessType field on the record.
 * 
 * @author Radek Krawiec
 * @since 02/05/2016
 */
public enum RecordAccessType
{
	/**
	 * Public access type means that this is a regular record that can be edited or deleted according to normal sharing settings for profiles and users.
	 * System types associated with custom types (such as ID unique check, type info records, standard actions) have access type SYSTEM, and if they are associated
	 * with basic types, the access type is SYSTEM_IMMUTABLE.
	 */
	PUBLIC(0),
	
	/**
	 * System immutable access type on a record means that this record is created during env setup and cannot be edit or deleted, even by root user.
	 */
	SYSTEM_IMMUTABLE(1),
	
	
	/**
	 * System access type means a record was created automatically by the system (e.g. sharing records for records owners), but can be modified or deleted
	 */
	SYSTEM(2);
	
	private int id;
	
	private RecordAccessType(int id)
	{
		this.id = id;
	}
	
	public int getId()
	{
		return this.id;
	}
}