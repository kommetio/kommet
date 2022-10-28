/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class Restriction
{
	private RestrictionOperator operator;
	private String property;
	private Object value;
	private Collection<Object> values;
	private List<Restriction> subrestrictions;
	
	public Restriction (RestrictionOperator operator, String property, Object value)
	{
		this.operator = operator;
		this.property = property;
		this.value = value;
	}
	
	public Restriction()
	{
		// empty
	}
	
	public Restriction (RestrictionOperator operator)
	{
		this.operator = operator;
	}
	
	public void addSubrestriction(Restriction subrestriction)
	{
		if (this.subrestrictions == null)
		{
			this.subrestrictions = new ArrayList<Restriction>();
		}
		this.subrestrictions.add(subrestriction);
	}

	public static Restriction eq (String property, Object value)
	{
		return new Restriction(RestrictionOperator.EQ, property, value);
	}
	
	public static Restriction gt (String property, Object value)
	{
		return new Restriction(RestrictionOperator.GT, property, value);
	}
	
	public static Restriction lt (String property, Object value)
	{
		return new Restriction(RestrictionOperator.LT, property, value);
	}
	
	public static Restriction ge (String property, Object value)
	{
		return new Restriction(RestrictionOperator.GE, property, value);
	}
	
	public static Restriction le (String property, Object value)
	{
		return new Restriction(RestrictionOperator.LE, property, value);
	}
	
	public static Restriction ilike(String property, Object value)
	{
		return new Restriction(RestrictionOperator.ILIKE, property, value);
	}
	
	public static Restriction like(String property, Object value)
	{
		return new Restriction(RestrictionOperator.LIKE, property, value);
	}
	
	public static Restriction isNull (String property)
	{
		return new Restriction(RestrictionOperator.ISNULL, property, null);
	}
	
	public static Restriction not (Restriction restriction)
	{
		Restriction r = new Restriction(RestrictionOperator.NOT);
		r.addSubrestriction(restriction);
		return r;
	}
	
	public static Restriction and (Restriction ... restrictions)
	{
		Restriction r = new Restriction(RestrictionOperator.AND);
		for (int i = 0; i < restrictions.length; i++)
		{
			r.addSubrestriction(restrictions[i]);
		}
		return r;
	}
	
	/*public static <T> Restriction in (String property, Set<T> values)
	{
		Collection<Object> valueCollection = new HashSet<Object>();
		CollectionUtils.addAll(valueCollection, values.iterator());
		return in(property, valueCollection);
	}*/
	
	@SuppressWarnings("unchecked")
	public static <T> Restriction in (String property, Collection<T> values)
	{
		Restriction r = new Restriction(RestrictionOperator.IN);
		r.setProperty(property);
		r.setValues((Collection<Object>)values);
		return r;
	}
	
	public static Restriction or (Restriction ... restrictions)
	{
		Restriction r = new Restriction(RestrictionOperator.OR);
		for (int i = 0; i < restrictions.length; i++)
		{
			r.addSubrestriction(restrictions[i]);
		}
		return r;
	}

	public RestrictionOperator getOperator()
	{
		return operator;
	}
	
	public void setProperty (String property)
	{
		this.property = property;
	}

	public String getProperty()
	{
		return property;
	}
	
	public void setValue (Object value)
	{
		this.value = value;
	}

	public Object getValue()
	{
		return value;
	}
	
	public void setOperator (RestrictionOperator operator)
	{
		this.operator = operator;
	}

	public List<Restriction> getSubrestrictions()
	{
		return this.subrestrictions;
	}

	public boolean isValid() throws CriteriaException
	{
		if (this.operator == null)
		{
			return false;
		}
		
		if (this.operator.equals(RestrictionOperator.NOT) || this.operator.equals(RestrictionOperator.AND) || this.operator.equals(RestrictionOperator.OR))
		{
			if (this.value != null)
			{
				return false;
			}
			if (this.subrestrictions == null || this.subrestrictions.isEmpty())
			{
				return false;
			}
			if (this.operator.equals(RestrictionOperator.NOT) && this.subrestrictions.size() > 1)
			{
				return false;
			}
			return true;
		}
		else if (isBinaryOperator(this.operator) && !RestrictionOperator.IN.equals(this.operator))
		{
			return this.value != null && this.property != null;
		}
		else if (this.operator.equals(RestrictionOperator.IN))
		{
			return (this.values != null && !this.values.isEmpty() && this.value == null) || (this.values == null && this.value != null && this.value instanceof Criteria);
		}
		else if (this.operator.equals(RestrictionOperator.ISNULL))
		{
			return this.property != null && this.value == null;
		}
		else
		{
			throw new CriteriaException("Operator must be either binary or one of AND, OR, NOT, ISNULL");
		}
	}

	private static boolean isBinaryOperator (RestrictionOperator operator)
	{
		return operator.equals(RestrictionOperator.EQ) || operator.equals(RestrictionOperator.GT) ||
				operator.equals(RestrictionOperator.LT) || operator.equals(RestrictionOperator.LE) ||
				operator.equals(RestrictionOperator.GE) || operator.equals(RestrictionOperator.NE) ||
				operator.equals(RestrictionOperator.IN) || operator.equals(RestrictionOperator.LIKE) ||
				operator.equals(RestrictionOperator.ILIKE);
	}

	public void setValues(Collection<Object> values)
	{
		this.values = values;
	}

	public Collection<Object> getValues()
	{
		return values;
	}
	
	public void addValue (Object value)
	{
		if (this.values == null)
		{
			this.values = new ArrayList<Object>();
		}
		this.values.add(value);
	}
}