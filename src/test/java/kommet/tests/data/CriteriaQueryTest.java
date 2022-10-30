/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.dao.MappedObjectQueryBuilder;
import kommet.dao.TypePersistenceMapping;
import kommet.dao.dal.AggregateFunction;
import kommet.dao.dal.AggregateFunctionCall;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.QueryResult;
import kommet.dao.queries.Restriction;
import kommet.dao.queries.SelectQuery;
import kommet.dao.queries.SortDirection;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.NumberDataType;
import kommet.env.EnvData;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.MiscUtils;

public class CriteriaQueryTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Test
	public void testCriteriaSQL() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type pigeonType = dataHelper.getFullPigeonType(env);
		
		Field bodyLengthField = new Field();
		bodyLengthField.setApiName("length");
		bodyLengthField.setDataType(new NumberDataType(0, Integer.class));
		bodyLengthField.setLabel("Length");
		bodyLengthField.setRequired(true);
		pigeonType.addField(bodyLengthField);
		
		pigeonType = dataService.createType(pigeonType, env);
		
		// create instance of pigeon object
		Record oldPigeon = dataService.instantiate(pigeonType.getKID(), env);
		oldPigeon.setField("name", "Bronek");
		oldPigeon.setField("age", 8);
		oldPigeon.setField("length", 20);
		dataService.save(oldPigeon, env);
		
		Criteria criteria = new Criteria(pigeonType, null, env, false);
		criteria.add(Restriction.eq("id", oldPigeon.getKID()));
		
		TypePersistenceMapping mapping = env.getTypeMapping(oldPigeon.getType().getKID());
		
		String updateSQL = MappedObjectQueryBuilder.getUpdateQuery(mapping, oldPigeon, criteria, dataHelper.getRootAuthData(env), false);
		assertTrue(updateSQL.startsWith("SELECT execute_update('UPDATE obj_" + pigeonType.getKeyPrefix() + " SET "));
		assertTrue(updateSQL.contains("name = ''Bronek''"));
		assertTrue(updateSQL.contains("age = 8"));
		assertTrue(updateSQL.contains("length = 20"));
		assertTrue(updateSQL.contains("accesstype = 0"));
		assertTrue(updateSQL.contains("lastmodifiedby = ''" + oldPigeon.getLastModifiedBy().getKID().getId() + "''"));
		assertTrue(updateSQL.contains("createddate = ''" + MiscUtils.formatPostgresDateTime(oldPigeon.getCreatedDate()) + "''"));
		assertTrue(updateSQL.contains("createdby = ''" + oldPigeon.getCreatedBy().getKID().getId() + "''"));
		assertTrue(updateSQL.contains("lastmodifieddate = ''" + MiscUtils.formatPostgresDateTime(oldPigeon.getLastModifiedDate()) + "''"));
		
		assertTrue(updateSQL.contains("kid = ''" + oldPigeon.getKID() + "''"));
		assertTrue(updateSQL.contains("age = 8"));
		assertTrue(updateSQL.contains("age = 8"));
		assertTrue(updateSQL.contains(Field.TRIGGER_FLAG_DB_COLUMN + " = ''EDITALL''"));
		assertTrue(updateSQL.endsWith("WHERE ((\"kid\" = ''" + oldPigeon.getKID() + "''))')"));
		
		testGroupByQuery(pigeonType, env);
	}

	private void testGroupByQuery(Type pigeonType, EnvData env) throws KommetException
	{
		Record pigeon1 = dataService.instantiate(pigeonType.getKID(), env);
		pigeon1.setField("name", "Zenek");
		pigeon1.setField("age", 8);
		pigeon1.setField("length", 10);
		dataService.save(pigeon1, env);
		
		Record pigeon2 = dataService.instantiate(pigeonType.getKID(), env);
		pigeon2.setField("name", "Felek");
		pigeon2.setField("age", 9);
		pigeon2.setField("length", 30);
		dataService.save(pigeon2, env);
		
		Record pigeon3 = dataService.instantiate(pigeonType.getKID(), env);
		pigeon3.setField("name", "Arek");
		pigeon3.setField("age", 9);
		pigeon3.setField("length", 40);
		dataService.save(pigeon3, env);
		
		Criteria criteria = new Criteria(pigeonType, null, env, false);
		criteria.addGroupByProperty("age");
		
		// add aggregate function call
		AggregateFunctionCall avgCall = new AggregateFunctionCall();
		avgCall.setFunction(AggregateFunction.AVG);
		avgCall.setProperty("length");
		criteria.addAggregateFunction(avgCall);
		
		SelectQuery query = SelectQuery.buildFromCriteria(criteria, null, env);
		List<Record> records = query.execute();
		assertNotNull(records);
		assertEquals(2, records.size());
		
		for (Record rec : records)
		{
			assertTrue(rec instanceof QueryResult);
			assertTrue(((BigDecimal)((QueryResult)rec).getAggregateValue("avg(length)")).compareTo(BigDecimal.valueOf(15)) == 0 || ((BigDecimal)((QueryResult)rec).getAggregateValue("avg(length)")).compareTo(BigDecimal.valueOf(35)) == 0);  
		}
		
		// add another field to the select clause
		criteria.addProperty("age");
		query = SelectQuery.buildFromCriteria(criteria, null, env);
		records = query.execute();
		assertNotNull(records);
		assertEquals(2, records.size());
		
		for (Record rec : records)
		{
			assertTrue(rec instanceof QueryResult);
			if (((BigDecimal)((QueryResult)rec).getGroupByValue("age")).compareTo(BigDecimal.valueOf(8)) == 0)
			{
				assertTrue(((BigDecimal)((QueryResult)rec).getAggregateValue("avg(length)")).compareTo(BigDecimal.valueOf(15)) == 0);
			}
			else if (((BigDecimal)((QueryResult)rec).getGroupByValue("age")).compareTo(BigDecimal.valueOf(9)) == 0)
			{
				assertTrue(((BigDecimal)((QueryResult)rec).getAggregateValue("avg(length)")).compareTo(BigDecimal.valueOf(35)) == 0);
			}
			else
			{
				fail("Unexpected age " + (BigDecimal)((QueryResult)rec).getGroupByValue("age"));
			}
		}
		
		// now try to add ordering to grouped query
		criteria.addOrderBy(SortDirection.ASC, "age");
		query = SelectQuery.buildFromCriteria(criteria, null, env);
		records = query.execute();
		assertTrue(((BigDecimal)((QueryResult)records.get(0)).getGroupByValue("age")).compareTo(BigDecimal.valueOf(8)) == 0);
		
		// add limit and offset
		criteria.setLimit(1);
		criteria.setOffset(1);
		query = SelectQuery.buildFromCriteria(criteria, null, env);
		records = query.execute();
		assertEquals(1, records.size());
		assertTrue(((BigDecimal)((QueryResult)records.get(0)).getGroupByValue("age")).compareTo(BigDecimal.valueOf(9)) == 0);
		
		Record pigeon5 = dataService.instantiate(pigeonType.getKID(), env);
		pigeon5.setField("name", "Mirek");
		pigeon5.setField("age", 30);
		pigeon5.setField("length", 150);
		dataService.save(pigeon5, env);
		
		Record pigeon6 = dataService.instantiate(pigeonType.getKID(), env);
		pigeon6.setField("name", "Bolek");
		pigeon6.setField("age", 114);
		pigeon6.setField("length", 150);
		dataService.save(pigeon6, env);
		
		// assign pigeon5 as father to all pigeons
		List<Record> allPigeons = env.getSelectCriteriaFromDAL("select id from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where id <> '" + pigeon5.getKID() + "'").list();
		assertEquals(5, allPigeons.size());
		for (Record pigeon : allPigeons)
		{
			pigeon.setField("father.id", pigeon5.getKID(), env);
			dataService.save(pigeon, env);
		}
		
		allPigeons.get(0).setField("father.id", pigeon6.getKID(), env);
		dataService.save(allPigeons.get(0), env);
		
		// add average father age to queried fields
		AggregateFunctionCall avgFatherAge = new AggregateFunctionCall();
		avgFatherAge.setFunction(AggregateFunction.AVG);
		avgFatherAge.setProperty("father.age");
		criteria.addAggregateFunction(avgFatherAge);
		
		records = env.getSelectCriteriaFromDAL("select avg(father.length) from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " group by age").list();
		assertEquals(4, records.size());
	}
}
