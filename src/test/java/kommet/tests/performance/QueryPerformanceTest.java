/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.performance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.dao.queries.SelectQuery;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.env.EnvData;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;

public class QueryPerformanceTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	private static final int INSERTED_ROWS = 1; //1000
	private static final int SELECTS_TO_RUN = 1; //100
	private static final int OBJECTS_PER_COLLECTION = 1; //100
	private static final int OBJECTS_WITH_COLLECTIONS = 1; //100
	private static final int NUM_OF_QUERIES_FOR_COLLECTIONS = 1; //10
	
	private static final Logger log = LoggerFactory.getLogger(QueryPerformanceTest.class);
	
	@Test
	public void testInsertsAndSelects() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type obj = dataHelper.getFullPigeonType(env);
		
		obj = dataService.createType(obj, env);
		
		// first create rows without inserting
		List<Record> records = new ArrayList<Record>();
		
		for (int i = 0; i < INSERTED_ROWS; i++)
		{
			Record r = new Record(obj);
			r.setField("name", "marek");
			r.setField("age", 3);
			records.add(r);
		}
		
		// not start time
		Date beforeTime = new Date();
		
		for (int i = 0; i < INSERTED_ROWS; i++)
		{
			dataService.save(records.get(i), env);
		}
		
		Date afterTime = new Date();
		long interval = afterTime.getTime() - beforeTime.getTime();
		log.debug("Insert of " + INSERTED_ROWS + " records lasted " + interval + "ms, " + (interval / INSERTED_ROWS) + "ms per record");
		
		// now test selects
		Criteria criteria = new Criteria(obj, env, true);
		criteria.add(Restriction.gt("age", 1));
		criteria.createAlias("father", "father");
		criteria.addProperty("father.age");
		criteria.addProperty("father.name");
		
		// test criteria query
		beforeTime = new Date();
		
		for (int i = 0; i < SELECTS_TO_RUN; i++)
		{
			criteria.list();
		}
		
		afterTime = new Date();
		interval = afterTime.getTime() - beforeTime.getTime();
		log.debug("Execution of " + SELECTS_TO_RUN + " selects lasted " + interval + "ms, " + (interval / SELECTS_TO_RUN) + "ms per query");
		
		// test criteria building
		beforeTime = new Date();
		
		for (int i = 0; i < 100 * SELECTS_TO_RUN; i++)
		{
			SelectQuery.buildFromCriteria(criteria, null, env);
		}
		
		afterTime = new Date();
		interval = afterTime.getTime() - beforeTime.getTime();
		log.debug("The preparation of " + (100 * SELECTS_TO_RUN) + " selects lasted " + interval + "ms, " + (interval / (100 * SELECTS_TO_RUN)) + "ms per query");
		
		// test plain SQL queries for comparison
		beforeTime = new Date();
		
		for (int i = 0; i < SELECTS_TO_RUN; i++)
		{
			SqlRowSet rowSet = env.getJdbcTemplate().queryForRowSet("select * from " + obj.getDbTable() + " where ((age > 3))");
			List<Record> stubRecords = new ArrayList<Record>();
			while (rowSet.next())
			{
				Record r = new Record(obj);
				r.setField("name", rowSet.getObject("name"));
				r.setField("age", rowSet.getObject("age"));
				r.setField("createddate", rowSet.getObject("createddate"));
				r.setField("createdby", rowSet.getObject("createdby"));
				r.setField("lastmodifieddate", rowSet.getObject("lastmodifieddate"));
				r.setField("lastmodifiedby", rowSet.getObject("lastmodifiedby"));
				r.setField("kid", rowSet.getObject("kid"));
				stubRecords.add(r);
			}
		}
		
		afterTime = new Date();
		interval = afterTime.getTime() - beforeTime.getTime();
		log.debug("For comparison, execution of " + SELECTS_TO_RUN + " plain SQL selects lasted " + interval + "ms, " + (interval / SELECTS_TO_RUN) + "ms per query");
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testQueryingInverseCollections() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type obj = dataHelper.getFullPigeonType(env);
		
		// add children collection field
		Field field = new Field();
		field.setApiName("children");
		field.setLabel("Children");
		field.setDataType(new InverseCollectionDataType(obj, "father"));
		field.setRequired(false);
		obj.addField(field);
		
		obj = dataService.createType(obj, env);
		
		String parentName = "marek";
		
		insertPigeonsWithChildren(obj, parentName, env);
		
		Date beforeTime = new Date();
		
		// now query all records that exist, both children and parents
		Integer pigeonCount = null;  
		int percentage = 0;
		int tenPerCent = NUM_OF_QUERIES_FOR_COLLECTIONS/10;
		if (tenPerCent == 0)
		{
			tenPerCent = 1;
		}
		for (int i = 0; i < NUM_OF_QUERIES_FOR_COLLECTIONS; i++)
		{
			List<Record> pigeons = env.getSelectCriteriaFromDAL("select id, name, age, children.id, children.name, children.age from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where name = '" + parentName + "'").list();
			pigeonCount = pigeons.size();
			if ((i+1) % tenPerCent == 0)
			{
				percentage += 10;
				log.debug(percentage + "% of queries done");
				
				// every now and then make sure the collection is really initialized
				assertEquals(OBJECTS_PER_COLLECTION, ((List<Record>)pigeons.get(0).getField("children")).size());
				assertNotNull(((List<Record>)pigeons.get(0).getField("children")).get(0).attemptGetKID());
			}
		}
		
		Date afterTime = new Date();
		Long interval = afterTime.getTime() - beforeTime.getTime();
		log.info("Querying " + NUM_OF_QUERIES_FOR_COLLECTIONS + " times for " + pigeonCount + " pigeons with " + OBJECTS_PER_COLLECTION + " items in collection lasted " + interval + "ms, " + (interval/NUM_OF_QUERIES_FOR_COLLECTIONS) + "ms per query");
	}

	private void insertPigeonsWithChildren(Type pigeonType, String parentName, EnvData env) throws KommetException
	{
		// insert some parent records
		List<Record> parents = new ArrayList<Record>();
		
		log.debug("Inserting objects with collections.");
		for (int i = 0; i < OBJECTS_WITH_COLLECTIONS; i++)
		{
			Record r = new Record(pigeonType);
			r.setField("name", parentName);
			r.setField("age", 3);
			parents.add(r);
		}
		
		// save records
		for (Record r : parents)
		{
			dataService.save(r, env);
		}
		
		// now insert children records
		List<Record> children = new ArrayList<Record>();
		
		for (int i = 0; i < OBJECTS_WITH_COLLECTIONS; i++)
		{
			for (int k = 0; k < OBJECTS_PER_COLLECTION; k++)
			{
				Record r = new Record(pigeonType);
				r.setField("name", "child-pigeon-" + k);
				r.setField("age", 3);
				r.setField("father", parents.get(i));
				children.add(r);
			}
		}
		
		for (Record child : children)
		{
			dataService.save(child, env);
		}
		
		log.debug("Objects with collections inserted.");
		
	}
}
