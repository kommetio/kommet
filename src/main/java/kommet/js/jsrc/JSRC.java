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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import kommet.auth.AuthData;
import kommet.dao.queries.jcr.PIRDeserializer;
import kommet.dao.queries.jcr.PIRSerializer;
import kommet.dao.queries.jcr.KIDSerializer;
import kommet.dao.queries.jcr.KidDeserializer;
import kommet.data.PIR;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.js.jsti.JSTI;
import kommet.json.JSON;

public class JSRC
{
	private static ObjectMapper mapper;
	
	// JSTI information needed to interpret record data
	private JSTI jsti;
	
	// record list in which each record is represented as a linked hash map
	private List<LinkedHashMap<String, Object>> records;

	public void setJsti(JSTI jsti)
	{
		this.jsti = jsti;
	}

	public JSTI getJsti()
	{
		return jsti;
	}

	public void setRecords(List<LinkedHashMap<String, Object>> records)
	{
		this.records = records;
	}

	public List<LinkedHashMap<String, Object>> getRecords()
	{
		return records;
	}

	/**
	 * Build a JSRC object from a list of records.
	 * @param records
	 * @param type
	 * @param level
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	public static JSRC build (List<Record> records, Type type, int level, EnvData env, AuthData authData) throws KommetException
	{
		JSRC jsrc = new JSRC();
		
		List<LinkedHashMap<String, Object>> jsrRecords = new ArrayList<LinkedHashMap<String, Object>>();
		for (Record r : records)
		{
			jsrRecords.add(JSRUtil.recordToMap(r, type, level, env));
		}
		
		jsrc.setRecords(jsrRecords);
		
		// build JSTI
		JSTI jsti = new JSTI();
		jsti.addType(type, env, true, true, authData);
		jsrc.setJsti(jsti);
		
		return jsrc;
	}

	public static String serialize(JSRC jsrc, AuthData authData) throws JSRCSerializationException
	{
		try
		{
			StringBuilder json = new StringBuilder();
			json.append("{ \"jsti\": ").append(JSTI.serialize(jsrc.getJsti())).append(", ");
			json.append("\"records\": ").append(JSON.serialize(jsrc.getRecords(), authData)).append(" }");
			return json.toString();
			//return (getJSRCMapper()).writeValueAsString(jsrc);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new JSRCSerializationException("Error serializing JSRC: " + e.getMessage());
		}
	}
	
	/**
	 * Get JSON mapper object that has all the knowledge necessary for properly translating a JSTI object
	 * into JSON, and the other way around.
	 * @return
	 */
	private static ObjectMapper getJSRCMapper()
	{
		if (mapper == null)
		{
			mapper = new ObjectMapper();
			
			// add custom PIR deserializer
			SimpleModule pirDeserializerModule = new SimpleModule();
			pirDeserializerModule.addDeserializer(PIR.class, new PIRDeserializer());
			mapper.registerModule(pirDeserializerModule);
			
			// add custom PIR serializer
			SimpleModule pirSerializerModule = new SimpleModule();
			pirSerializerModule.addSerializer(PIR.class, new PIRSerializer());
			mapper.registerModule(pirSerializerModule);
			
			// add custom KID serializer
			SimpleModule ridSerializerModule = new SimpleModule();
			ridSerializerModule.addSerializer(KID.class, new KIDSerializer());
			mapper.registerModule(ridSerializerModule);
			
			// add custom KID deserializer
			SimpleModule ridDeserializerModule = new SimpleModule();
			ridDeserializerModule.addDeserializer(KID.class, new KidDeserializer());
			mapper.registerModule(ridDeserializerModule);
		}
		
		return mapper;
	}

	/**
	 * Deserializes JSON into a JSRC object.
	 * @param json
	 * @return
	 * @throws KommetException
	 */
	public static JSRC deserialize(String json) throws KommetException
	{
		ObjectMapper jsrcMapper = getJSRCMapper();
		
		try
		{
			return (jsrcMapper).readValue(json, JSRC.class);
		}
		catch (Exception e)
		{
			throw new KommetException("Error deserializing JSRC: " + e.getMessage(), e);
		}
	}

	
	
}