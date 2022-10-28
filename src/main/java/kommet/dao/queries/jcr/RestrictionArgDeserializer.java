/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao.queries.jcr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.JsonParser.NumberType;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import kommet.dao.queries.RestrictionOperator;
import kommet.data.PIR;
import kommet.data.KommetException;
import kommet.utils.MiscUtils;

/**
 * Custom deserializer that deserializes collection Restriction.args into objects.
 * A custom deserializer is needed for this collection, because it can contain objects of
 * type String, Double, Integer or Restriction, and standard deserializer does not know which
 * type to use.
 * 
 * @author Radek Krawiec
 * @created 3-09-2014
 */
public class RestrictionArgDeserializer extends JsonDeserializer<List<Object>>
{
	@Override
	public List<Object> deserialize(JsonParser parser, DeserializationContext ctx) throws IOException, JsonProcessingException
	{
		TreeNode values = parser.readValueAsTree();
		List<Object> deserializedValues = new ArrayList<Object>();
		
		if (!values.isArray())
		{
			throw new JcrSerializationException("Error deserializing restriction arguments - JSON values is not an array");
		}
		
		for (int i = 0; i < values.size(); i++)
		{
			TreeNode arrayElem = values.get(i);
			
			if (arrayElem.isValueNode())
			{
				Object val = getScalarNodeValue(arrayElem);
				if (val != null)
				{
					deserializedValues.add(val);
				}
			}
			else if (arrayElem.isObject())
			{
				deserializedValues.add(getRestriction(arrayElem));
			}
			else
			{
				throw new JcrSerializationException("Unsupported node type while deserializing restriction arguments");
			}
		}
		
		return deserializedValues;
	}

	/**
	 * Transform a tree node into a restriction.
	 * @param node
	 * @return
	 * @throws JcrSerializationException
	 */
	private Restriction getRestriction(TreeNode node) throws JcrSerializationException
	{
		Restriction r = new Restriction();
		
		String operator = node.path("operator").toString();
		r.setOperator(operator != null ? RestrictionOperator.valueOf(MiscUtils.trim(operator, '"').toUpperCase()) : null);
		Object pir = node.path("property_id");
		if (pir != null && !"null".equals(pir.toString()))
		{
			r.setPropertyId(new PIR(MiscUtils.trim(pir.toString(), '"')));
		}
		
		Object propertyName = getScalarNodeValue(node.path("property_name"));
		if (propertyName != null && !"null".equals(propertyName.toString()))
		{
			r.setPropertyName((String)propertyName.toString());
		}
		
		TreeNode args = node.path("args");
		
		if (args != null)
		{
			for (int i = 0; i < args.size(); i++)
			{
				TreeNode arg = args.get(i);
				if (arg.isValueNode())
				{
					Object val = getScalarNodeValue(arg);
					if (val != null)
					{
						r.addArg(val);
					}
				}
				else if (arg.isObject())
				{
					// if argument of a restriction is an object, it can be either a subrestriction (if the operator
					// of this restriction is AND or OR), or a subquery JCR (if operator is IN)
					if (r.getOperator().equals(RestrictionOperator.AND) || r.getOperator().equals(RestrictionOperator.OR))
					{
						r.addArg(getRestriction(arg));
					}
					else if (r.getOperator().equals(RestrictionOperator.IN))
					{
						try
						{
							r.addArg(JCRUtil.deserialize(arg.toString()));
						}
						catch (KommetException e)
						{
							e.printStackTrace();
							throw new JcrSerializationException("Could not deserialize subquery JCR " + arg.toString());
						}
					}
					else
					{
						throw new JcrSerializationException("Did not expected JSON object as restriction argument for operator " + r.getOperator());
					}
				}
				else
				{
					throw new JcrSerializationException("Unsupported node type while deserializing restriction arguments");
				}
			}
		}
		
		return r;
	}
	
	private static Object getScalarNodeValue (TreeNode node) throws JcrSerializationException
	{
		if (node.asToken().isBoolean())
		{
			return Boolean.valueOf(node.toString());
		}
		else if (node.asToken().isNumeric())
		{
			if (node.numberType().equals(NumberType.INT))
			{
				return Integer.valueOf(node.toString());
			}
			else if (node.numberType().equals(NumberType.DOUBLE))
			{
				return Double.valueOf(node.toString());
			}
			else if (node.numberType().equals(NumberType.LONG))
			{
				return Long.valueOf(node.toString());
			}
			else 
			{
				throw new JcrSerializationException("Unsupported number type " + node.numberType() + " for value " + node.toString());
			}
		}
		else if (node.asToken().isBoolean())
		{
			return MiscUtils.trim(node.toString(), '"');
		}
		else
		{
			return MiscUtils.trim(node.toString(), '"');
		}
	}
	
}