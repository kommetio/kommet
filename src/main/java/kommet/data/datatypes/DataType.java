/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.datatypes;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;

import kommet.data.KommetException;
import kommet.i18n.Locale;

public abstract class DataType
{
	public static final int NUMBER = 0;
	public static final int TEXT = 1;
	public static final int BOOLEAN = 2;
	public static final int DATETIME = 3;
	public static final int KOMMET_ID = 4;
	public static final int EMAIL = 5;
	public static final int TYPE_REFERENCE = 6;
	public static final int ENUMERATION = 7;
	public static final int INVERSE_COLLECTION = 8;
	public static final int BLOB = 9;
	public static final int ASSOCIATION = 10;
	public static final int FORMULA = 11;
	public static final int DATE = 12;
	public static final int MULTI_ENUMERATION = 13;
	public static final int AUTO_NUMBER = 14;
	
	private static List<DataType> dataTypes;
	
	private Integer id;
	
	public DataType (Integer id)
	{
		this.setId(id);
	}

	public void setId(Integer id)
	{
		this.id = id;
	}

	public Integer getId()
	{
		return id;
	}

	/**
	 * Returns the name of the Postgres data type corresponding to this Kommet data type.
	 * @param dataType
	 * @return
	 * @throws KommetException
	 */
	public static String getPostgresType (DataType dataType) throws KommetException
	{
		if (dataType instanceof NumberDataType)
		{
			return "numeric(18, " + ((NumberDataType)dataType).getDecimalPlaces() + ")";
		}
		else if (dataType instanceof TextDataType)
		{
			return "character varying (" + ((TextDataType)dataType).getLength() + ")";
		}
		else if (dataType instanceof AutoNumber)
		{
			return "character varying (30)";
		}
		else if (dataType instanceof DateTimeDataType)
		{
			return "timestamp without time zone";
		}
		else if (dataType instanceof DateDataType)
		{
			return "timestamp without time zone";
		}
		else if (dataType instanceof BooleanDataType)
		{
			return "boolean";
		}
		else if (dataType instanceof KIDDataType)
		{
			return "character varying (13)";
		}
		else if (dataType instanceof EmailDataType)
		{
			return "character varying (100)";
		}
		else if (dataType instanceof TypeReference)
		{
			// type references are in fact KIDs
			return "character varying(13)";
		}
		else if (dataType instanceof MultiEnumerationDataType)
		{
			return "character varying (255)[]";
		}
		else if (dataType instanceof EnumerationDataType)
		{
			return "character varying (1024)";
		}
		else if (dataType instanceof BlobDataType)
		{
			return "bytea";
		}
		else if (dataType instanceof AssociationDataType)
		{
			throw new KommetException("Association data type has not Postgres type representation");
		}
		else if (dataType instanceof AssociationDataType)
		{
			throw new KommetException("Association data type has not Postgres type representation");
		}
		else if (dataType instanceof FormulaDataType)
		{
			return ((FormulaDataType)dataType).getReturnType().getPostgresType();
		}
		else if (dataType instanceof MultiEnumerationDataType)
		{
			return "character varying(13)[]";
		}
		else
		{
			throw new KommetException("Unknown data type " + dataType.getClass().getName());
		}
	}
	
	private static List<DataType> getDefaultDataTypes()
	{
		if (dataTypes == null)
		{
			dataTypes = new ArrayList<DataType>();
			dataTypes.add(new NumberDataType(0, Integer.class));
			dataTypes.add(new TextDataType(100, false, false));
			dataTypes.add(new BooleanDataType());
			dataTypes.add(new DateTimeDataType());
			dataTypes.add(new DateDataType());
			dataTypes.add(new KIDDataType());
			dataTypes.add(new EmailDataType());
			dataTypes.add(new TypeReference());
			dataTypes.add(new EnumerationDataType());
			dataTypes.add(new MultiEnumerationDataType());
			dataTypes.add(new InverseCollectionDataType());
			dataTypes.add(new BlobDataType());
			dataTypes.add(new FormulaDataType());
			dataTypes.add(new AssociationDataType());
			dataTypes.add(new AutoNumber());
		}
		
		return dataTypes;
	}

	/**
	 * Get data type by its ID.
	 * @param dataTypeId
	 * @return
	 * @throws KommetException
	 */
	public static DataType getById (int dataTypeId) throws KommetException
	{
		switch (dataTypeId)
		{
			case NUMBER: return new NumberDataType(0, Integer.class);
			case TEXT: return new TextDataType(100, false, false);
			case BOOLEAN: return new BooleanDataType();
			case DATETIME: return new DateTimeDataType();
			case DATE: return new DateDataType();
			case KOMMET_ID: return new KIDDataType();
			case EMAIL: return new EmailDataType();
			case TYPE_REFERENCE: return new TypeReference();
			case ENUMERATION: return new EnumerationDataType();
			case MULTI_ENUMERATION: return new MultiEnumerationDataType();
			case INVERSE_COLLECTION: return new InverseCollectionDataType();
			case BLOB: return new BlobDataType();
			case ASSOCIATION: return new AssociationDataType();
			case FORMULA: return new FormulaDataType();
			case AUTO_NUMBER: return new AutoNumber();
			default: throw new KommetException("No data type exists for Id " + dataTypeId);
		}
	}

	/**
	 * Returns the formatted value of the given data type, as it should be inserted
	 * into Postgres queries
	 * @param value
	 * @return
	 * @throws KommetException
	 */
	public abstract String getPostgresValue (Object value) throws KommetException;
	
	/**
	 * Returns the value of the given data type as string.
	 * @param value
	 * @return
	 * @throws KommetException
	 */
	public abstract String getStringValue (Object value, Locale locale) throws KommetException;

	/**
	 * Returns the value of the given data type cast to the type in which it is represented
	 * in Kommet
	 * @param value
	 * @return
	 * @throws KommetException
	 */
	public abstract Object getJavaValue (Object value) throws KommetException;
	
	/**
	 * Returns the value of the given data type cast to the type in which it is represented
	 * in Kommet.
	 * We need to have a method that converts string to the given data type for each data type, because
	 * HTTP parameters come as strings, and we need to be able to parse them.
	 * @param value
	 * @return
	 * @throws KommetException
	 */
	public abstract Object getJavaValue (String value) throws KommetException;
	
	/**
	 * Is the data type is transient (like e.g. InverseCollection),
	 * its value is not set in insert/update operations.
	 * @return
	 */
	public abstract boolean isTransient();
	
	public static boolean isSpecialValueNull (Object value)
	{
		return SpecialValue.isNull(value);
	}
	
	public abstract String getName();
	
	/**
	 * Tells if this is a primitive data type.
	 * Primitive data types are all except InverseCollection, ObjectReference and Association.
	 * @return
	 */
	public abstract boolean isPrimitive();
	
	/**
	 * Tells if this data type represents a collection of records. Collection data types are
	 * inverse collection and association.
	 * @return
	 */
	public abstract boolean isCollection();
	
	/**
	 * Returns the qualified name of the Java type that can store properties of this data type.
	 * 
	 * This property can be used e.g. while generating object stubs for fields of this data type.
	 */
	public abstract String getJavaType();

	public static DataType getByName(String name) throws KommetException
	{
		for (DataType dt : getDefaultDataTypes())
		{
			if (name.equals(dt.getName()))
			{
				try
				{
					return (DataType)BeanUtils.cloneBean(dt);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					throw new KommetException("Cannot clone data type bean for data type " + dt.getName() + ". Nested: " + e.getMessage());
				}
			}
		}
		
		return null;
	}
}