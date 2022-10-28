/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.lang.reflect.Method;
import java.util.HashMap;

import kommet.basic.RecordProxy;
import kommet.data.KommetException;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.MiscUtils;

/**
 * Represents a mapping between an object proxy and a Kommet type. Object proxy properties
 * are mapped to Kommet type fields.
 * 
 * @author Radek Krawiec
 *
 */
public class RecordProxyMapping extends PersistenceMapping
{
	protected String typeQualifiedName;
	
	public RecordProxyMapping (Class<? extends RecordProxy> cls) throws KommetPersistenceException
	{	
		this.clazz = cls;
		this.columnMappings = new HashMap<String, ColumnMapping>();
		this.propertyMappings = new HashMap<String, ColumnMapping>();
		
		initFromAnnotations();
	}
	
	private void initFromAnnotations() throws KommetPersistenceException
	{
		// get all getter methods from the type
		Method[] methods = this.clazz.getMethods();
		
		if (this.clazz.isAnnotationPresent(Entity.class))
		{
			this.typeQualifiedName = this.clazz.getAnnotation(Entity.class).type();
		}
		else
		{
			throw new KommetPersistenceException("Annotation @Entity is required on persistent proxy class " + this.clazz.getName());
		}
		
		this.columnMappings = new HashMap<String, ColumnMapping>();
		this.propertyMappings = new HashMap<String, ColumnMapping>();
		
		for (int i = 0; i < methods.length; i++)
		{
			Method method = methods[i];
			
			// check if it is a getter method
			if (method.getName().startsWith("get"))
			{
				if (method.isAnnotationPresent(Property.class))
				{
					Property columnAnnotation = method.getAnnotation(Property.class);
					ColumnMapping colMapping = new ColumnMapping();
					colMapping.setColumn(columnAnnotation.field());
					try
					{
						colMapping.setProperty(MiscUtils.getPropertyFromGetter(method.getName()));
					}
					catch (KommetException e)
					{
						throw new KommetPersistenceException("Error initializing object mapping", e);
					}
					colMapping.setRequired(columnAnnotation.required());
					
					this.columnMappings.put(colMapping.getColumn(), colMapping);
					this.propertyMappings.put(colMapping.getProperty(), colMapping);
				}
			}
		}
	}
	
	public Class<? extends RecordProxy> getProxyClass()
	{
		return this.clazz;
	}
	
	public String getTypeQualifiedName()
	{
		return typeQualifiedName;
	}
}