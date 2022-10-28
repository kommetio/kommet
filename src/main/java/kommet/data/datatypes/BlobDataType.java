/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.datatypes;

import kommet.data.KommetException;
import kommet.exceptions.NotImplementedException;
import kommet.i18n.Locale;

public class BlobDataType extends DataType
{
	public BlobDataType()
	{
		super(BLOB);
	}

	@Override
	public Object getJavaValue (Object value) throws KommetException
	{
		throw new NotImplementedException("Blog data type not implemented");
	}
	
	@Override
	public Object getJavaValue (String value) throws KommetException
	{
		return getJavaValue((Object)value);
	}

	@Override
	public String getPostgresValue (Object value) throws KommetException
	{
		throw new NotImplementedException("Blog data type not implemented");
	}
	
	@Override
	public String getStringValue (Object value, Locale locale) throws KommetException
	{
		throw new NotImplementedException("Blog data type not implemented");
	}

	@Override
	public boolean isTransient()
	{
		return false;
	}
	
	public String getName()
	{
		return "Blog";
	}

	/**
	 * Returns the qualified name of the Java type that can store properties of this data type.
	 * 
	 * This property can be used e.g. while generating object stubs for fields of this data type.
	 */
	@Override
	public String getJavaType()
	{
		return "byte[]";
	}
	
	@Override
	public boolean isPrimitive()
	{
		return true;
	}
	
	@Override
	public boolean isCollection()
	{
		return false;
	}
}