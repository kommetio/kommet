/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

/**
 * Record that has no type assigned. It is used as a mock record for displaying views in view editor.
 * @author Radek Krawiec
 * @since 11/08/2016
 */
public class UntypedRecord extends Record
{
	public UntypedRecord() throws KommetException
	{
		super(true);
	}
	
	@Override
	public Object getField (String fieldName, boolean errorIfNotInitialized) throws KommetException
	{
		return null;
	}
	
	@Override
	public Object getField (String fieldName) throws KommetException
	{
		return null;
	}
}