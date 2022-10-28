/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries.jcr;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import kommet.dao.queries.Criteria;
import kommet.dao.queries.RestrictionOperator;
import kommet.data.Field;
import kommet.data.PIR;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.DataType;
import kommet.env.EnvData;
import kommet.utils.MiscUtils;

public class Restriction
{
	private RestrictionOperator operator;
	private PIR propertyId;
	private String propertyName;
	private List<Object> args;

	public void setOperator(RestrictionOperator operator)
	{
		this.operator = operator;
	}

	public RestrictionOperator getOperator()
	{
		return operator;
	}

	public void setPropertyId(PIR propertyId)
	{
		this.propertyId = propertyId;
	}

	@JsonProperty("property_id")
	public PIR getPropertyId()
	{
		return propertyId;
	}

	public void setArgs(List<Object> args)
	{
		this.args = args;
	}

	@JsonDeserialize(using = RestrictionArgDeserializer.class)
	public List<Object> getArgs()
	{
		return args;
	}

	public void addArg (Object arg)
	{
		if (this.args == null)
		{
			this.args = new ArrayList<Object>();
		}
		this.args.add(arg);
	}

	public void setPropertyName(String propertyName)
	{
		this.propertyName = propertyName;
	}

	@JsonProperty("property_name")
	public String getPropertyName()
	{
		return propertyName;
	}

	public String getDAL(Type baseType, EnvData env) throws KommetException
	{
		if (this.operator.equals(RestrictionOperator.AND) || this.operator.equals(RestrictionOperator.OR))
		{
			List<String> dalArgs = new ArrayList<String>();
			for (Object arg : this.args)
			{
				dalArgs.add(((Restriction)arg).getDAL(baseType, env));
			}
			
			return MiscUtils.implode(dalArgs, this.operator.getDAL(), " ");
		}
		else if (this.operator.equals(RestrictionOperator.ISNULL))
		{
			String propertyName = null;
			
			if (this.propertyId != null)
			{
				PirParseResult npd = JCRUtil.pirToNestedPropertyData(this.propertyId, baseType, env);
				propertyName = npd.getQualifiedName();
			}
			else if (StringUtils.hasText(this.propertyName))
			{
				propertyName = this.propertyName;
			}
			else
			{
				throw new KommetException("Neither property ID nor property name specified on restriction");
			}
			
			// handle isnull restriction separately because it does not take any arguments
			return propertyName + " " + this.operator.getDAL();
		}
		else
		{
			String propertyName = null;
			DataType dataType = null;
			
			if (this.propertyId != null)
			{
				PirParseResult npd = JCRUtil.pirToNestedPropertyData(this.propertyId, baseType, env);
				propertyName = npd.getQualifiedName();
				dataType = npd.getMostNestedField().getDataType();
			}
			else if (StringUtils.hasText(this.propertyName))
			{
				propertyName = this.propertyName;
				
				Field field = baseType.getField(propertyName, env);
				
				if (field == null)
				{
					throw new KommetException("Field " + propertyName + " not found on type " + baseType.getQualifiedName());
				}
				
				dataType = field.getDataType();
			}
			else
			{
				throw new KommetException("Neither property ID nor property name specified on restriction");
			}
			
			if (this.args == null || this.args.isEmpty() || this.args.get(0) == null)
			{
				throw new KommetException("No arguments or null argument for operator " + this.operator + " and property " + propertyName);
			}
			
			String arg = this.args.get(0).toString();
			
			// some operators require their arguments to have special representation
			// e.g. LIKE/ILIKE or = when applied to strings required strings in single quotes
			if (this.operator.equals(RestrictionOperator.ILIKE) || this.operator.equals(RestrictionOperator.LIKE))
			{
				arg = dataType.getPostgresValue(arg);
			}
			else if (this.operator.equals(RestrictionOperator.IN))
			{
				if (this.args.get(0) instanceof JCR)
				{
					arg = "(" + ((JCR)this.args.get(0)).getQuery(env).trim() + ")";
				}
				else
				{
					// each arg contains one value for the IN condition list
					List<String> formattedValues = new ArrayList<String>();
					for (Object a : this.args)
					{
						formattedValues.add(dataType.getPostgresValue(a));
					}
					arg = "(" + MiscUtils.implode(formattedValues, ", ") + ")";
				}
			}
			else
			{
				arg = dataType.getPostgresValue(arg);
			}
			
			return propertyName + " " + this.operator.getDAL() + " " + arg;
		}
	}

	/**
	 * Translate a DAL restriction into a JCR restriction.
	 * @param dalRestriction
	 * @param type
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	public static Restriction fromDALRestriction(kommet.dao.queries.Restriction dalRestriction, Type type, EnvData env) throws KommetException
	{
		Restriction jcrRestriction = new Restriction();
		jcrRestriction.setOperator(dalRestriction.getOperator());
		
		// if this restriction operates on properties, not on other restrictions
		if (!RestrictionOperator.AND.equals(dalRestriction.getOperator()) && !RestrictionOperator.OR.equals(dalRestriction.getOperator()) && !RestrictionOperator.NOT.equals(dalRestriction.getOperator()))
		{
			jcrRestriction.setPropertyId(PIR.get(dalRestriction.getProperty(), type, env));
			jcrRestriction.setPropertyName(dalRestriction.getProperty());
		}
		
		if (dalRestriction.getSubrestrictions() != null)
		{
			for (kommet.dao.queries.Restriction subrestriction : dalRestriction.getSubrestrictions())
			{
				jcrRestriction.addArg(Restriction.fromDALRestriction(subrestriction, type, env));
			}
		}
		
		if (dalRestriction.getValue() != null)
		{
			if (dalRestriction.getValue() instanceof Criteria)
			{
				// serialize subquery criteria as JCR
				jcrRestriction.addArg(JCRUtil.getJCRFromDALCriteria((Criteria)dalRestriction.getValue(), env));
			}
			else
			{
				jcrRestriction.addArg(dalRestriction.getValue());
			}
		}
		
		if (dalRestriction.getValues() != null)
		{
			for (Object arg : dalRestriction.getValues())
			{
				jcrRestriction.addArg(arg);
			}
		}
		
		return jcrRestriction;
	}
}