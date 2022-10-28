/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.dal;

public class InsufficientPrivilegesException extends DALException
{
	private static final long serialVersionUID = -7686072437050727166L;
	public static final String INSUFFICIENT_PRIVILEGES_TO_EDIT_RECORD_MSG = "Insufficient privileges to edit record";
	public static final String INSUFFICIENT_PRIVILEGES_TO_DELETE_RECORD_MSG = "Insufficient privileges to delete record";
	public static final String INSUFFICIENT_PRIVILEGES_TO_EDIT_SYSTEM_IMMUTABLE_RECORD_MSG = "Insufficient privileges to edit system immutable record";
	public static final String INSUFFICIENT_PRIVILEGES_TO_DELETE_SYSTEM_IMMUTABLE_RECORD_MSG = "Insufficient privileges to delete system immutable record";
	public static final String INSUFFICIENT_PRIVILEGES_TO_INSERT_TYPE_MSG = "Insufficient privileges to create records of type";
	
	// message thrown by the DB check_edit_permissions and check_delete_permissions triggers
	// it is also hardcoded in the DB code, so if changed, the triggers to be modified there as well
	public static final String INSUFFICIENT_PRIVILEGES_TO_EDIT_DB_TRIGGER_MSG = "Insufficient privileges to edit object";
	public static final String INSUFFICIENT_PRIVILEGES_TO_EDIT_ERRCODE = "RM001";
	public static final String INSUFFICIENT_PRIVILEGES_TO_DELETE_ERRCODE = "RM002";
	public static final String INSUFFICIENT_PRIVILEGES_TO_EDIT_SYSTEM_IMMUTABLE_ERRCODE = "RM003";
	public static final String INSUFFICIENT_PRIVILEGES_TO_DELETE_SYSTEM_IMMUTABLE_ERRCODE = "RM004";
	public static final String CANNOT_MODIFY_ACCESS_TYPE = "RM005";

	public InsufficientPrivilegesException(String msg)
	{
		super(msg);
	}
}