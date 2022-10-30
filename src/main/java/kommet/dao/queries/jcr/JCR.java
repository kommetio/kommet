/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries.jcr;

import java.util.ArrayList;
import java.util.List;

import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.utils.MiscUtils;

public class JCR
{
	// KID of the base type
	private KID baseTypeId;
	
	// base type qualified name (user-defined)
	private String baseTypeName;
	
	private List<Property> properties;
	private List<Grouping> groupings;
	private List<Restriction> restrictions;
	private List<Ordering> orderings;
	private Integer limit;
	private Integer offset;

	public void setBaseTypeId(KID baseTypeId)
	{
		this.baseTypeId = baseTypeId;
	}

	public KID getBaseTypeId()
	{
		return baseTypeId;
	}

	public void setProperties(List<Property> properties)
	{
		this.properties = properties;
	}

	public List<Property> getProperties()
	{
		return properties;
	}

	public void setGroupings(List<Grouping> groupings)
	{
		this.groupings = groupings;
	}

	public List<Grouping> getGroupings()
	{
		return groupings;
	}

	public void addProperty(Property prop)
	{
		if (this.properties == null)
		{
			this.properties = new ArrayList<Property>();
		}
		this.properties.add(prop);
	}
	
	public void addGrouping (Grouping grouping)
	{
		if (this.groupings == null)
		{
			this.groupings = new ArrayList<Grouping>();
		}
		this.groupings.add(grouping);
	}

	public String getQuery (EnvData env) throws KommetException
	{
		StringBuilder sb = new StringBuilder("SELECT ");
		
		Type type = JCRUtil.getBaseType(this, env);
		
		List<String> fieldNames = new ArrayList<String>();
		
		for (Property prop : this.properties)
		{
			String propName = JCRUtil.getPropertyName(prop, type, env);
			
			if (propName == null)
			{
				throw new KommetException("Property name/ID not defined in JCR");
			}
			
			if (prop.getAggregateFunction() == null)
			{
				fieldNames.add(propName);
			}
			else
			{
				fieldNames.add(prop.getAggregateFunction().name() + "(" + propName + ")");
			}
		}
		
		sb.append(MiscUtils.implode(fieldNames, ", "));
		
		// add from clause
		sb.append(" FROM ").append(type.getQualifiedName());
		
		// TODO add unit test for restrictions
		if (this.restrictions != null && !this.restrictions.isEmpty())
		{
			sb.append(" WHERE ");
			List<String> restrictionList = new ArrayList<String>();
			for (Restriction r : this.restrictions)
			{
				restrictionList.add(r.getDAL(type, env));
			}
			
			sb.append(MiscUtils.implode(restrictionList, " AND "));
		}
		
		// add groupings
		if (this.groupings != null && !this.groupings.isEmpty())
		{
			sb.append(" GROUP BY ");
			List<String> groupedProps = new ArrayList<String>();
			for (Grouping grouping : this.groupings)
			{
				groupedProps.add(JCRUtil.getPropertyName(grouping, type, env));
			}
			
			sb.append(MiscUtils.implode(groupedProps, ", "));
		}
		
		// add orderings
		if (this.orderings != null && !this.orderings.isEmpty())
		{
			sb.append(" ORDER BY ");
			List<String> orderingDAL = new ArrayList<String>();
			for (Ordering ordering : this.orderings)
			{
				orderingDAL.add(JCRUtil.getPropertyName(ordering, type, env) + " " + ordering.getSortDirection());
			}
			
			sb.append(MiscUtils.implode(orderingDAL, ", "));
		}
		
		// add limit and offset
		if (this.limit != null)
		{
			sb.append(" LIMIT " + this.limit);
		}
		
		if (this.offset != null)
		{
			sb.append(" OFFSET " + this.offset);
		}
		
		return sb.toString();
	}

	public void addRestriction(Restriction restriction)
	{
		if (this.restrictions == null)
		{
			this.restrictions = new ArrayList<Restriction>();
		}
		this.restrictions.add(restriction);
	}

	public void setRestrictions(List<Restriction> restrictions)
	{
		this.restrictions = restrictions;
	}

	public List<Restriction> getRestrictions()
	{
		return restrictions;
	}

	public void setOrderings(List<Ordering> orderings)
	{
		this.orderings = orderings;
	}

	public List<Ordering> getOrderings()
	{
		return orderings;
	}

	public void addOrdering(Ordering ordering)
	{
		if (this.orderings == null)
		{
			this.orderings = new ArrayList<Ordering>();
		}
		this.orderings.add(ordering);
	}

	public void setLimit(Integer limit)
	{
		this.limit = limit;
	}

	public Integer getLimit()
	{
		return limit;
	}

	public void setOffset(Integer offset)
	{
		this.offset = offset;
	}

	public Integer getOffset()
	{
		return offset;
	}

	public String getBaseTypeName()
	{
		return baseTypeName;
	}

	public void setBaseTypeName(String baseTypeName)
	{
		this.baseTypeName = baseTypeName;
	}
}