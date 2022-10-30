/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.triggers;

import java.util.List;

import kommet.basic.RecordProxy;
import kommet.basic.SystemContextAware;
import kommet.koll.SystemContext;

/**
 * Basic class that must be extended by all trigger classes.
 * 
 * The class is not extended explicitly by users in KOLL. Instead, the extends keyword is added during conversion
 * to Java. The existance of this class is transparent to KOLL users.
 *  
 * @author Radek Krawiec
 * @created 18-01-2014
 *
 * @param <T>
 */
public abstract class DatabaseTrigger<T extends RecordProxy> implements SystemContextAware
{
	private List<T> oldValues;
	private List<T> newValues;
	private boolean isInsert;
	private boolean isUpdate;
	private boolean isDelete;
	private boolean isBefore;
	private boolean isAfter;
	
	private SystemContext systemContext;
	
	public void setOldValues(List<T> oldValues)
	{
		this.oldValues = oldValues;
	}
	
	public List<T> getOldValues()
	{
		return oldValues;
	}
	
	public void setNewValues(List<T> newValues)
	{
		this.newValues = newValues;
	}
	
	public List<T> getNewValues()
	{
		return newValues;
	}
	
	/**
	 * Just an alias for deprecate getSystemContext().
	 * @return
	 */
	public SystemContext getSys()
	{
		return systemContext;
	}

	public void setSystemContext(SystemContext systemContext)
	{
		this.systemContext = systemContext;
	}

	public void setInsert(boolean isInsert)
	{
		this.isInsert = isInsert;
	}

	public boolean isInsert()
	{
		return isInsert;
	}

	public void setUpdate(boolean isUpdate)
	{
		this.isUpdate = isUpdate;
	}

	public boolean isUpdate()
	{
		return isUpdate;
	}

	public void setDelete(boolean isDelete)
	{
		this.isDelete = isDelete;
	}

	public boolean isDelete()
	{
		return isDelete;
	}

	public void setBefore(boolean isBefore)
	{
		this.isBefore = isBefore;
	}

	public boolean isBefore()
	{
		return isBefore;
	}

	public void setAfter(boolean isAfter)
	{
		this.isAfter = isAfter;
	}

	public boolean isAfter()
	{
		return isAfter;
	}
}