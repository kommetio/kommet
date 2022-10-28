/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.objectlist;

public class SerializedObjectListConfig
{
	private String title;
	private String pageNo;
	private String pageSize;
	private String recordListId;
	private String dalFilter;
	private String contextPath;
	private String sortBy;
	private SerializedListColumn[] columns;

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getTitle()
	{
		return title;
	}

	public void setPageNo(String pageNo)
	{
		this.pageNo = pageNo;
	}

	public String getPageNo()
	{
		return pageNo;
	}

	public void setPageSize(String pageSize)
	{
		this.pageSize = pageSize;
	}

	public String getPageSize()
	{
		return pageSize;
	}

	public void setDalFilter(String dalFilter)
	{
		this.dalFilter = dalFilter;
	}

	public String getDalFilter()
	{
		return dalFilter;
	}

	public void setRecordListId(String recordListId)
	{
		this.recordListId = recordListId;
	}

	public String getRecordListId()
	{
		return recordListId;
	}

	public void setContextPath(String contextPath)
	{
		this.contextPath = contextPath;
	}

	public String getContextPath()
	{
		return contextPath;
	}

	public void setSortBy(String sortBy)
	{
		this.sortBy = sortBy;
	}

	public String getSortBy()
	{
		return sortBy;
	}

	public void setColumns(SerializedListColumn[] columns)
	{
		this.columns = columns;
	}

	public SerializedListColumn[] getColumns()
	{
		return columns;
	}
}
