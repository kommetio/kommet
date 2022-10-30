/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries;

import com.fasterxml.jackson.annotation.JsonValue;

import kommet.data.KommetException;

public enum RestrictionOperator
{
	EQ("eq"),
	GT("gt"),
	GE("ge"),
	LT("lt"),
	LE("le"),
	ISNULL("isnull"),
	IN("in"),
	NOT("not"),
	OR("or"),
	NE("ne"),
	LIKE("like"),
	ILIKE("ilike"),
	AND("and");
	
	private String stringValue;
	
	private RestrictionOperator (final String val)
	{
		this.stringValue = val;
	}

	public static RestrictionOperator fromDALOperator (String operator)
	{
		if ("and".equals(operator.toLowerCase()))
		{
			return RestrictionOperator.AND;
		}
		else if ("or".equals(operator.toLowerCase()))
		{
			return RestrictionOperator.OR;
		}
		else if ("=".equals(operator.toLowerCase()))
		{
			return RestrictionOperator.EQ;
		}
		else if ("<>".equals(operator.toLowerCase()))
		{
			return RestrictionOperator.NE;
		}
		else if ("not".equals(operator.toLowerCase()))
		{
			return RestrictionOperator.NOT;
		}
		else if ("isnull".equals(operator.toLowerCase()))
		{
			return RestrictionOperator.ISNULL;
		}
		else if ("in".equals(operator.toLowerCase()))
		{
			return RestrictionOperator.IN;
		}
		else if (">=".equals(operator.toLowerCase()))
		{
			return RestrictionOperator.GE;
		}
		else if ("<=".equals(operator.toLowerCase()))
		{
			return RestrictionOperator.LE;
		}
		else if (">".equals(operator.toLowerCase()))
		{
			return RestrictionOperator.GT;
		}
		else if ("<".equals(operator.toLowerCase()))
		{
			return RestrictionOperator.LT;
		}
		else if ("<>".equals(operator.toLowerCase()))
		{
			return RestrictionOperator.NE;
		}
		else if ("in".equals(operator.toLowerCase()))
		{
			return RestrictionOperator.IN;
		}
		else if ("like".equals(operator.toLowerCase()))
		{
			return RestrictionOperator.LIKE;
		}
		else if ("ilike".equals(operator.toLowerCase()))
		{
			return RestrictionOperator.ILIKE;
		}
		else
		{
			return null;
		}
	}

	/**
	 * Returns a DAL operator representing this restriction operator. For example, for the restriction operator
	 * EQ the DAL operator is "=".
	 * @return
	 * @throws KommetException
	 */
	public String getDAL() throws KommetException
	{
		if (this.equals(EQ))
		{
			return "=";
		}
		else if (this.equals(GT))
		{
			return ">";
		}
		else if (this.equals(GE))
		{
			return ">=";
		}
		else if (this.equals(LT))
		{
			return "<";
		}
		else if (this.equals(LE))
		{
			return "<=";
		}
		else if (this.equals(IN))
		{
			return "IN";
		}
		else if (this.equals(NE))
		{
			return "<>";
		}
		else if (this.equals(ISNULL))
		{
			return "ISNULL";
		}
		else if (this.equals(OR))
		{
			return "OR";
		}
		else if (this.equals(AND))
		{
			return "AND";
		}
		else if (this.equals(LIKE))
		{
			return "LIKE";
		}
		else if (this.equals(ILIKE))
		{
			return "ILIKE";
		}
		else
		{
			throw new KommetException("Cannot translate operator " + this + " to DAL");
		}
	}
	
	 @JsonValue
	 public String getStringValue()
	 {
		 return this.stringValue;
	 }
}