/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.js.jsti;

import java.util.LinkedHashMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import kommet.auth.AuthData;
import kommet.dao.queries.jcr.KeyPrefixDeserializer;
import kommet.dao.queries.jcr.KeyPrefixSerializer;
import kommet.dao.queries.jcr.KIDSerializer;
import kommet.dao.queries.jcr.KidDeserializer;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.PIR;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;

/**
 * Represents a collection of records wrapped in objects that allow for their easy
 * conversion to JSON.
 * @author Radek Krawiec
 * @created 10-09-2014
 */
public class JSTI
{
	private static ObjectMapper mapper;
	
	private LinkedHashMap<PIR, JSTIField> fields;
	private LinkedHashMap<KID, JSTIType> types;
	private LinkedHashMap<String, String> pirs;

	public void setFields(LinkedHashMap<PIR, JSTIField> fields)
	{
		this.fields = fields;
	}

	public LinkedHashMap<PIR, JSTIField> getFields()
	{
		return fields;
	}

	public void setTypes(LinkedHashMap<KID, JSTIType> types)
	{
		this.types = types;
	}

	public LinkedHashMap<KID, JSTIType> getTypes()
	{
		return types;
	}
	
	public void addType(Type type, EnvData env, boolean initFields, boolean initNestedTypes, AuthData authData) throws KommetException
	{
		addType(type, env, initFields, initNestedTypes, false, authData);
	}

	public void addType(Type type, EnvData env, boolean initFields, boolean initNestedTypes, boolean initPermissions, AuthData authData) throws KommetException
	{
		if (this.types == null)
		{
			this.types = new LinkedHashMap<KID, JSTIType>();
		}
		
		// skip type if already added
		if (this.types.containsKey(type.getKID()))
		{
			return;
		}
		
		JSTIType jstiType = new JSTIType(type, env);
		
		if (initPermissions)
		{
			TypePermission permission = new TypePermission();
			permission.setRead(authData.canReadType(type.getKID(), false, env));
			permission.setEdit(authData.canEditType(type.getKID(), false, env));
			permission.setDelete(authData.canDeleteType(type.getKID(), false, env));
			permission.setCreate(authData.canCreateType(type.getKID(), false, env));
			permission.setReadAll(authData.canReadAllType(type.getKID(), false, env));
			permission.setEditAll(authData.canEditAllType(type.getKID(), false, env));
			permission.setDeleteAll(authData.canDeleteAllType(type.getKID(), false, env));
			jstiType.setPermission(permission);
		}
		
		this.types.put(type.getKID(), jstiType);
		
		if (!initFields)
		{
			return;
		}
		
		for (Field field : type.getFields())
		{
			addField(field, type, initNestedTypes, initPermissions, authData, env);
		}
	}
	
	public void addField(Field field, Type type, boolean initNestedTypes, AuthData authData, EnvData env) throws KommetException
	{
		addField(field, type, initNestedTypes, false, authData, env);
	}

	// TODO unit test for calling this method with initNestedTypes = true
	public void addField(Field field, Type type, boolean initNestedTypes, boolean initPermissions, AuthData authData, EnvData env) throws KommetException
	{
		if (this.fields == null)
		{
			this.fields = new LinkedHashMap<PIR, JSTIField>();
		}
		
		PIR pir = PIR.get(field.getApiName(), type, env);
		
		// skip field if already added
		if (this.fields.containsKey(pir))
		{
			return;
		}
		
		JSTIField jstiField = new JSTIField(field, env, authData);
		
		if (initPermissions)
		{
			FieldPermission permission = new FieldPermission();
			permission.setRead(authData.canReadField(field, false, env));
			permission.setEdit(authData.canEditField(field, false, env));
			jstiField.setPermission(permission);
		}
		
		this.fields.put(pir, jstiField);
		
		if (initNestedTypes)
		{
			if (field.isDataType(DataType.TYPE_REFERENCE))
			{
				Type nestedType = env.getType(((TypeReference)field.getDataType()).getType().getKID());
				this.addType(nestedType, env, true, initNestedTypes, initPermissions, authData);
			}
			else if (field.isDataType(DataType.INVERSE_COLLECTION))
			{
				Type nestedType = env.getType(((InverseCollectionDataType)field.getDataType()).getInverseType().getKID());
				this.addType(nestedType, env, true, initNestedTypes, initPermissions, authData);
			}
			else if (field.isDataType(DataType.ASSOCIATION))
			{
				Type nestedType = env.getType(((AssociationDataType)field.getDataType()).getAssociatedType().getKID());
				this.addType(nestedType, env, true, initNestedTypes, initPermissions, authData);
			}
		}
		
		// add mapping from property name to PIR
		if (this.pirs == null)
		{
			this.pirs = new LinkedHashMap<String, String>();
		}
		this.pirs.put(field.getType().getQualifiedName() + "." + field.getApiName(), field.getKID().getId());  
	}

	/**
	 * Serialize JSTI object to JSON string.
	 * @param jsti
	 * @return
	 * @throws JSTISerializationException
	 */
	public static String serialize(JSTI jsti) throws JSTISerializationException
	{
		try
		{
			return (getJSTIMapper()).writeValueAsString(jsti);
		}
		catch (Exception e)
		{
			throw new JSTISerializationException("Error serializing criteria: " + e.getMessage());
		}
	}
	
	/**
	 * Get JSON mapper object that has all the knowledge necessary for properly translating a JSTI object
	 * into JSON, and the other way around.
	 * @return
	 */
	private static ObjectMapper getJSTIMapper()
	{
		if (mapper == null)
		{
			mapper = new ObjectMapper();
			
			// add custom KID serializer
			SimpleModule ridSerializerModule = new SimpleModule();
			ridSerializerModule.addSerializer(KID.class, new KIDSerializer());
			mapper.registerModule(ridSerializerModule);
			
			// add custom KID deserializer
			SimpleModule ridDeserializerModule = new SimpleModule();
			ridDeserializerModule.addDeserializer(KID.class, new KidDeserializer());
			mapper.registerModule(ridDeserializerModule);
			
			// add custom KID serializer
			SimpleModule keyPrefixSerializerModule = new SimpleModule();
			keyPrefixSerializerModule.addSerializer(KeyPrefix.class, new KeyPrefixSerializer());
			mapper.registerModule(keyPrefixSerializerModule);
			
			// add custom KID deserializer
			SimpleModule keyPrefixDeserializerModule = new SimpleModule();
			keyPrefixDeserializerModule.addDeserializer(KeyPrefix.class, new KeyPrefixDeserializer());
			mapper.registerModule(keyPrefixDeserializerModule);
		}
		
		return mapper;
	}
	
	public LinkedHashMap<String, String> getPirs()
	{
		return pirs;
	}

	public void setPirs(LinkedHashMap<String, String> pirs)
	{
		this.pirs = pirs;
	}

	public static JSTI deserialize(String json) throws KommetException
	{
		ObjectMapper jstiMapper = getJSTIMapper();
		
		try
		{
			return (jstiMapper).readValue(json, JSTI.class);
		}
		catch (Exception e)
		{
			throw new KommetException("Error deserializing JSTI: " + e.getMessage(), e);
		}
	}
}