/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.js.jsrc;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import kommet.dao.queries.QueryResult;
import kommet.data.Field;
import kommet.data.PIR;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;

public class JSRUtil
{	
	/**
	 * Returns a record serialized to a hash map. The key of the hash map is a string with the following interpretation:
	 * <ul>
	 * <li>if a property is queried without an alias or an aggregate function, the key of the map is a PIR of this property</li>
	 * <li>if a property is queried with an alias, the key will be this alias</li>
	 * <li>if a property is queried with an aggregate function (but without an alias), the key of the map will be the aggregate function expression together with the PIR, e.g. "count(003000000021a)"</li>
	 * </ul>
	 * @param record
	 * @param type
	 * @param levels
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@SuppressWarnings("unchecked")
	public static LinkedHashMap<String, Object> recordToMap(Record record, Type type, int levels, EnvData env) throws KommetException
	{
		// get the type from the env to make sure we have the most up-to-date version of it
		type = env.getType(type.getKeyPrefix());
		
		if (levels < 0)
		{
			return null;
		}
		
		LinkedHashMap<String, Object> jsr = new LinkedHashMap<String, Object>();
		
		for (String fieldName : record.getFieldValues().keySet())
		{
			Object val = record.getField(fieldName);
			Field field = type.getField(fieldName);
			
			if (field == null)
			{
				throw new KommetException("Field " + fieldName + " not found on type " + type.getQualifiedName());
			}
			
			if (field.getDataType().isPrimitive())
			{
				jsr.put(PIR.get(fieldName, type, env).getValue(), val);
			}
			else if (field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
			{
				TypeReference objRef = (TypeReference)field.getDataType();
				jsr.put(PIR.get(fieldName, type, env).getValue(), val != null ? recordToMap((Record)val, objRef.getType(), levels - 1, env) : null);
			}
			else if (field.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
			{
				InverseCollectionDataType dt = (InverseCollectionDataType)field.getDataType();
				List<Record> items = (List<Record>)val;
				List<LinkedHashMap<String, Object>> serializedItems = new ArrayList<LinkedHashMap<String,Object>>();
				
				for (Record item : items)
				{
					serializedItems.add(recordToMap(item, dt.getInverseType(), levels - 1, env));
				}
				jsr.put(PIR.get(fieldName, type, env).getValue(), serializedItems);
			}
			else if (field.getDataTypeId().equals(DataType.ASSOCIATION))
			{
				AssociationDataType dt = (AssociationDataType)field.getDataType();
				List<Record> items = (List<Record>)val;
				List<LinkedHashMap<String, Object>> serializedItems = new ArrayList<LinkedHashMap<String,Object>>();
				
				for (Record item : items)
				{
					serializedItems.add(recordToMap(item, dt.getAssociatedType(), levels - 1, env));
				}
				jsr.put(PIR.get(fieldName, type, env).getValue(), serializedItems);
			}
		}
		
		if (record instanceof QueryResult)
		{
			QueryResult qr = (QueryResult)record;
			
			for (String key : qr.getAggregateValues().keySet())
			{
				// extract property name from aggregate function call
				String aggrFunction = key.substring(0, key.indexOf("("));
				String propertyName = key.substring(key.indexOf("(") + 1, key.indexOf(")"));
				
				// translate key (which is the property name) to PIR
				jsr.put(aggrFunction + "(" + PIR.get(propertyName, type, env).getValue() + ")", qr.getAggregateValue(key));
			}
			
			for (String key : qr.getGroupByValues().keySet())
			{
				// translate key (which is the property name) to PIR
				jsr.put(PIR.get(key, type, env).getValue(), qr.getGroupByValue(key));
			}
		}
		
		return jsr;
	}
	
	/**
	 * Return the value of a field identified by the given PIR.
	 * @param record
	 * @param pir
	 * @return
	 */
	// TODO unit test for this method
	@SuppressWarnings("unchecked")
	public static Object getFieldValue (LinkedHashMap<String, Object> record, PIR pir)
	{
		String sPIR = pir.getValue();
		if (!sPIR.contains("."))
		{
			return record.get(sPIR);
		}
		else
		{
			String[] subProps = sPIR.split("\\.");
			return getFieldValue((LinkedHashMap<String, Object>)record.get(subProps[0]), new PIR(sPIR.substring(sPIR.indexOf(".") + 1)));
		}
	}
}