/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.filters;

import java.util.Collection;

import kommet.dao.queries.Criteria;
import kommet.dao.queries.SortDirection;

public class BasicFilter<T>
{
	private QueryResultOrder order;
	private String orderBy;
	private Long id;
	private Integer limit;
	private Collection<Long> ids;
	
	public Criteria applySortAndLimit(Criteria c)
	{
		if (getOrder() != null && getOrderBy() != null)
		{
			c.addOrderBy(getOrder().equals(QueryResultOrder.ASC) ? SortDirection.ASC : SortDirection.DESC, getOrderBy());
		}
		
		if (getLimit() != null)
		{
			c.setLimit(getLimit());
		}
		
		return c;
	}

	public void setOrder(QueryResultOrder order)
	{
		this.order = order;
	}

	public QueryResultOrder getOrder()
	{
		return order;
	}

	public void setOrderBy(String orderBy)
	{
		this.orderBy = orderBy;
	}

	public String getOrderBy()
	{
		return orderBy;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public Long getId()
	{
		return id;
	}

	public void setIds(Collection<Long> ids)
	{
		this.ids = ids;
	}

	public Collection<Long> getIds()
	{
		return ids;
	}

	public void setLimit(Integer limit)
	{
		this.limit = limit;
	}

	public Integer getLimit()
	{
		return limit;
	}
}