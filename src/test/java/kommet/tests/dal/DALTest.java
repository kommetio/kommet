/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.dal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import kommet.dao.dal.DALSyntaxException;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.CriteriaException;
import kommet.dao.queries.InvalidResultSetAccess;
import kommet.dao.queries.QueryResult;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.FieldValueException;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.env.EnvData;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;

public class DALTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	AppConfig appConfig;
	
	private EnvData env;
	private Type pigeonType;
	
	//private static final Logger log = LoggerFactory.getLogger(DALTest.class);
	private static final String DAL_QUERY_FOR_PIGEONS = "SELECT id, name, age, father.name, father.age FROM " + TestDataCreator.PIGEON_TYPE_PACKAGE + "." + TestDataCreator.PIGEON_TYPE_API_NAME + " WHERE age >= 8 AND ((name = 'Bronek' OR name = 'Some \\' Name') OR id ISNULL)";
	
	@Test
	public void buildQueryFromDAL() throws KommetException
	{
		env.getSelectCriteriaFromDAL(DAL_QUERY_FOR_PIGEONS);
		
		try
		{
			env.getSelectCriteriaFromDAL("select userName from User where name = 'a'");
			fail("Getting criteria from invalid query should fail");
		}
		catch (DALSyntaxException e)
		{
			// expected
			assertTrue(e.getMessage().startsWith("Property name not found on type "));
		}
	}
	
	@Before
	public void prepareTestData() throws KommetException
	{
		this.env = dataHelper.configureFullTestEnv();
		this.pigeonType = dataHelper.getFullPigeonType(env);
		this.pigeonType = dataService.createType(this.pigeonType, env);
	}
	
	@Test
	public void testDALWithoutWhereClause() throws KommetException
	{
		Criteria c = env.getSelectCriteriaFromDAL("SELECT id, name, age FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME);
		assertEquals(this.pigeonType.getKID(), c.getType().getKID());
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testSelectDAL() throws KommetException
	{
		// create instance of pigeon object
		Record oldPigeon = dataService.instantiate(this.pigeonType.getKID(), env);
		oldPigeon.setField("name", "Bronek");
		oldPigeon.setField("age", 8);
		oldPigeon.setField("birthdate", new Date (105, 3, 4));
		oldPigeon.setField("colour", "green");
		
		dataService.save(oldPigeon, env);
		
		Record youngPigeon = dataService.instantiate(this.pigeonType.getKID(), env);
		youngPigeon.setField("name", "Zenek");
		youngPigeon.setField("age", 2);
		youngPigeon.setField("father", oldPigeon);
		youngPigeon.setField("birthdate", new Date (112, 3, 4));
		
		// make sure assigning a enumeration value containing a new line character fails
		try
		{
			youngPigeon.setField("colour", "green\nbrown");
			fail("Assigning an enumeration value containing a new line character should throw an exception");
		}
		catch (FieldValueException e)
		{
			// expected
		}
		
		youngPigeon.setField("colour", "brown");
		dataService.save(youngPigeon, env);
		
		// test using {defaultField} token
		List<Record> pigeonsWithParent = env.getSelectCriteriaFromDAL("SELECT id, {defaultField}, father.id FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE age > 1").list();
		assertNotNull(pigeonsWithParent);
		assertEquals(2, pigeonsWithParent.size());
		for (Record pigeon : pigeonsWithParent)
		{
			assertTrue(pigeon.isSet(pigeonType.getDefaultFieldApiName()));
		}
		
		// select pigeons with father property, but without naming that property in the criteria
		pigeonsWithParent = env.getSelectCriteriaFromDAL("SELECT id, father.id FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE age > 1").list();
		assertNotNull(pigeonsWithParent);
		assertEquals(2, pigeonsWithParent.size());
		
		// test WHERE clause without spaces between operators
		pigeonsWithParent = env.getSelectCriteriaFromDAL("SELECT id, father.id FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE age>1").list();
		assertNotNull(pigeonsWithParent);
		assertEquals(2, pigeonsWithParent.size());
		
		// test WHERE clause without spaces between operators
		pigeonsWithParent = env.getSelectCriteriaFromDAL("SELECT id, father.id FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE colour ='green'").list();
		assertNotNull(pigeonsWithParent);
		assertEquals(1, pigeonsWithParent.size());
		
		// make sure you cannot specify a bare relationship name
		try
		{
			env.getSelectCriteriaFromDAL("SELECT id, father FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME).list();
			fail("Selecting a bare relationship 'father' without a subfield (e.g. 'father.id') should fail");
		}
		catch (DALSyntaxException e)
		{
			assertTrue("Incorrect expection message text: " + e.getMessage(), e.getMessage().startsWith("Cannot reference whole relationship"));
		}
		
		// select pigeons with father whose age is greater than 7
		pigeonsWithParent = env.getSelectCriteriaFromDAL("SELECT id, father.id FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE father.age > 7").list();
		assertNotNull(pigeonsWithParent);
		assertEquals(1, pigeonsWithParent.size());
		assertEquals(youngPigeon.getKID(), pigeonsWithParent.get(0).getKID());
		
		// select pigeons with empty father property
		pigeonsWithParent = env.getSelectCriteriaFromDAL("SELECT id FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME +" WHERE father.age ISNULL").list();
		assertNotNull(pigeonsWithParent);
		assertEquals(1, pigeonsWithParent.size());
		assertEquals(oldPigeon.getKID(), pigeonsWithParent.get(0).getKID());
		
		// since only the id is listed in the select field, make sure referencing a different field
		// throws an exception
		try
		{
			pigeonsWithParent.get(0).getField("age");
			fail("Field 'age' was not listed in the SELECT clause of the query, so referencing it should throw an exception");
		}
		catch (KommetException e)
		{
			// expected
		}
		
		// select pigeons with non-empty father property
		pigeonsWithParent = env.getSelectCriteriaFromDAL("SELECT id FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE NOT (father.age ISNULL)").list();
		assertNotNull(pigeonsWithParent);
		assertEquals(1, pigeonsWithParent.size());
		assertEquals(youngPigeon.getKID(), pigeonsWithParent.get(0).getKID());
		
		// test handling superfluous brackets
		pigeonsWithParent = env.getSelectCriteriaFromDAL("SELECT id FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE (((father.age ISNULL)))").list();
		assertNotNull(pigeonsWithParent);
		assertEquals(1, pigeonsWithParent.size());
		assertEquals(oldPigeon.getKID(), pigeonsWithParent.get(0).getKID());
		
		// test simple string comparison
		pigeonsWithParent = env.getSelectCriteriaFromDAL("SELECT id FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE name = 'Bronek'").list();
		assertNotNull(pigeonsWithParent);
		assertEquals(1, pigeonsWithParent.size());
		assertEquals(oldPigeon.getKID(), pigeonsWithParent.get(0).getKID());
		
		// test selecting by date
		pigeonsWithParent = env.getSelectCriteriaFromDAL("SELECT id, age FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE birthdate > '2008-10-10'").list();
		assertNotNull(pigeonsWithParent);
		assertEquals(1, pigeonsWithParent.size());
		assertEquals(youngPigeon.getKID(), pigeonsWithParent.get(0).getKID());
		assertEquals(youngPigeon.getField("age"), pigeonsWithParent.get(0).getField("age"));
		
		// select by colour
		pigeonsWithParent = env.getSelectCriteriaFromDAL("SELECT id, age FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE colour = 'brown'").list();
		assertNotNull(pigeonsWithParent);
		assertEquals(1, pigeonsWithParent.size());
		assertEquals(youngPigeon.getKID(), pigeonsWithParent.get(0).getKID());
		assertEquals(youngPigeon.getField("age"), pigeonsWithParent.get(0).getField("age"));
		
		// select by colour using the IN operator
		pigeonsWithParent = env.getSelectCriteriaFromDAL("SELECT id, age FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE colour IN ('brown', 'yellow')").list();
		assertNotNull(pigeonsWithParent);
		assertEquals(1, pigeonsWithParent.size());
		assertEquals(youngPigeon.getKID(), pigeonsWithParent.get(0).getKID());
		assertEquals(youngPigeon.getField("age"), pigeonsWithParent.get(0).getField("age"));
		
		// select by colour using the IN operator, or by name
		pigeonsWithParent = env.getSelectCriteriaFromDAL("SELECT id, age FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE colour IN ('brown', 'yellow') OR name = 'Bronek'").list();
		assertEquals(2, pigeonsWithParent.size());
		
		// test selecting by three AND conditions not enclosed in brackets
		pigeonsWithParent = env.getSelectCriteriaFromDAL("SELECT id, age FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE colour IN ('brown', 'yellow') AND name = 'Zenek' AND age > 0").list();
		assertEquals(1, pigeonsWithParent.size());
		
		// make sure that when AND and OR conditions are put without brackets, the query fails
		try
		{
			pigeonsWithParent = env.getSelectCriteriaFromDAL("SELECT id, age FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE colour IN ('brown', 'yellow') OR name = 'Zenek' AND age > 0").list();
			fail("Query should fail with AND and OR conditions at the same level, not enclosed in brackets");
		}
		catch (CriteriaException e)
		{
			// expected
		}
		
		// test LIMIT keyword
		List<Record> recordsWithLimit = env.getSelectCriteriaFromDAL("SELECT id, age FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE colour IN ('brown', 'yellow') OR name = 'Bronek' LIMIT 1").list();
		assertEquals("The number of returned rows should be limited to 1", 1, recordsWithLimit.size());
		
		// test LIMIT keyword without a WHERE condition
		String query = "SELECT id, age FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " LIMIT 1";
		assertFalse(query.toLowerCase().contains("where"));
		recordsWithLimit = env.getSelectCriteriaFromDAL(query).list();
		assertEquals("The number of returned rows should be limited to 1", 1, recordsWithLimit.size());
		
		// test OFFSET keyword - together there are two records, so with offset 1 there should be one record on the list
		List<Record> recordsByOffset = env.getSelectCriteriaFromDAL("SELECT id, age FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " ORDER BY " + Field.CREATEDDATE_FIELD_NAME + " ASC OFFSET 1").list();
		assertEquals(1, recordsByOffset.size());
		assertEquals(youngPigeon.getKID(), recordsByOffset.get(0).getKID());
		
		// test COUNT keyword
		List<Record> aggregateResults = env.getSelectCriteriaFromDAL("SELECT COUNT(id) FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME).list();
		assertNotNull(aggregateResults);
		assertEquals(1, aggregateResults.size());
		
		Record aggregateResult = aggregateResults.get(0);
		if (!(aggregateResult instanceof QueryResult))
		{
			fail("DAL query with aggregate function COUNT() should return an object of type " + QueryResult.class.getName() + ", but it returned object of type " + aggregateResult.getClass().getName());
		}
		
		QueryResult result = (QueryResult)aggregateResult;
		// extract value both by lower and upper case
		
		// first try accessing value that is not in the result set
		try
		{
			result.getAggregateValue("invalid-value");
			fail("Accessing an invalid result set field should throw an exception");
		}
		catch (InvalidResultSetAccess e)
		{
			// expected exception
		}
		
		assertEquals(Long.valueOf(2), result.getAggregateValue("count(id)"));
		assertEquals(Long.valueOf(2), result.getAggregateValue("COUNT(id)"));
		assertEquals(Long.valueOf(2), env.getSelectCriteriaFromDAL("SELECT COUNT(id) FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME).count());
		
		try
		{
			// make sure COUNT keyword is not mixed with regular select fields
			env.getSelectCriteriaFromDAL("SELECT COUNT(id), age FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME).list();
			fail("DAL query with both aggregate and regular fields should fail");
		}
		catch (DALSyntaxException e)
		{
			// expected exception
			assertTrue("Incorrect exception message verbiage: " + e.getMessage(), e.getMessage().startsWith("The following properties should be used either in an aggregate function or a group by clause:"));
		}
		
		// test DAL syntax error
		try
		{
			env.getSelectCriteriaFromDAL("SELECT id FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " OFFSET 1 ORDER BY id ASC").list();
			fail("Query has incorrect keyword order and should throw an exception");
		}
		catch (DALSyntaxException e)
		{
			// expected
			assertTrue(e.getMessage().startsWith("Misplaced token OFFSET. Clauses in query should appear in the following order"));
		}
		
		// test a query with WHERE, ORDER BY and LIMIT clause because it caused an error at some point
		env.getSelectCriteriaFromDAL("SELECT id FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " ORDER BY id DESC LIMIT 2").list();
		
		// test aggregate functions
		testAggregateFunction("sum", "age", BigDecimal.valueOf(10), env, pigeonType);
		testAggregateFunction("avg", "age", BigDecimal.valueOf(5), env, pigeonType);
		testAggregateFunction("max", "age", BigDecimal.valueOf(8), env, pigeonType);
		testAggregateFunction("min", "age", BigDecimal.valueOf(2), env, pigeonType);
		
		Record fatherPigeon = dataService.instantiate(pigeonType.getKID(), env);
		fatherPigeon.setField("name", "Bolek");
		fatherPigeon.setField("age", 4);
		dataService.save(fatherPigeon, env);
		
		// assign same pigeon as father to all pigeons
		List<Record> allPigeons = env.getSelectCriteriaFromDAL("select id from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where id <> '" + fatherPigeon.getKID() + "'").list();
		assertEquals(2, allPigeons.size());
		for (Record pigeon : allPigeons)
		{
			pigeon.setField("father.id", fatherPigeon.getKID(), env);
			dataService.save(pigeon, env);
		}
		
		// test aggregate functions for nested field
		testAggregateFunction("sum", "father.age", BigDecimal.valueOf(8), env, pigeonType);
		testAggregateFunction("avg", "father.age", BigDecimal.valueOf(4), env, pigeonType);
		testAggregateFunction("max", "father.age", BigDecimal.valueOf(4), env, pigeonType);
		testAggregateFunction("min", "father.age", BigDecimal.valueOf(4), env, pigeonType);
		testAggregateFunctionOnCollection(pigeonType, env);
		testGroupBy(pigeonType, env);
	}
	
	private void testAggregateFunctionOnCollection(Type pigeonType, EnvData env) throws KommetException
	{
		// create children field
		Field childrenField = new Field();
		childrenField.setApiName("children");
		childrenField.setLabel("Children");
		childrenField.setDataType(new InverseCollectionDataType(pigeonType, "father"));
		pigeonType.addField(childrenField);
		
		dataService.createField(childrenField, env);
		
		Record father1 = createPigeon("father03", 6, null, pigeonType, env);
		Record father2 = createPigeon("father04", 7, null, pigeonType, env);
		
		createPigeon("son1", 6, father1, pigeonType, env);
		createPigeon("son2", 7, father1, pigeonType, env);
		Record son3 = createPigeon("son3", 3, father2, pigeonType, env);
		Record son4 = createPigeon("son4", 3, father2, pigeonType, env);
		
		String query = "select min(children.age), max(children.age), avg(children.age), sum(children.age) from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where name ilike 'father0%'";
		List<Record> pigeons = env.getSelectCriteriaFromDAL(query).list();
		assertEquals(1, pigeons.size());
		
		QueryResult pigeonRes = (QueryResult)pigeons.get(0);
		
		assertTrue(((BigDecimal)pigeonRes.getAggregateValue("avg(children.age)")).compareTo(BigDecimal.valueOf(4.75)) == 0);
		assertTrue(((BigDecimal)pigeonRes.getAggregateValue("sum(children.age)")).compareTo(BigDecimal.valueOf(19)) == 0);
		assertTrue(((BigDecimal)pigeonRes.getAggregateValue("min(children.age)")).compareTo(BigDecimal.valueOf(3)) == 0);
		assertTrue(((BigDecimal)pigeonRes.getAggregateValue("max(children.age)")).compareTo(BigDecimal.valueOf(7)) == 0);
		
		// add a group by clause
		Criteria c = env.getSelectCriteriaFromDAL("select id, min(children.age), max(children.age), avg(children.age), sum(children.age) from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where name ilike 'father0%' GROUP BY id");
		pigeons = c.list();
		assertEquals(2, pigeons.size());
		
		for (Record pigeon : pigeons)
		{
			pigeonRes = (QueryResult)pigeon;
			assertNotNull(pigeon.getField("id"));
			
			KID fatherId = (KID)pigeon.getField("id");
			if (fatherId.equals(father1.getKID()))
			{
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("avg(children.age)")).compareTo(BigDecimal.valueOf(6.5)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("sum(children.age)")).compareTo(BigDecimal.valueOf(13)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("min(children.age)")).compareTo(BigDecimal.valueOf(6)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("max(children.age)")).compareTo(BigDecimal.valueOf(7)) == 0);
			}
			else if (fatherId.equals(father2.getKID()))
			{
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("avg(children.age)")).compareTo(BigDecimal.valueOf(3)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("sum(children.age)")).compareTo(BigDecimal.valueOf(6)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("min(children.age)")).compareTo(BigDecimal.valueOf(3)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("max(children.age)")).compareTo(BigDecimal.valueOf(3)) == 0);
			}
			else
			{
				fail("Unexpected ID value " + fatherId);
			}
		}
		
		// now include in the query pigeons that have no children and see how aggregate
		// functions work on empty collections
		pigeons = env.getSelectCriteriaFromDAL("select count(children.id), count(children.age), min(children.age), max(children.age), avg(children.age), sum(children.age) from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where name ilike 'father0%' OR name = 'son1'").list();
		assertEquals(1, pigeons.size());
		pigeonRes = (QueryResult)pigeons.get(0);
		assertTrue(((BigDecimal)pigeonRes.getAggregateValue("avg(children.age)")).compareTo(BigDecimal.valueOf(4.75)) == 0);
		assertTrue(((BigDecimal)pigeonRes.getAggregateValue("sum(children.age)")).compareTo(BigDecimal.valueOf(19)) == 0);
		assertTrue(((BigDecimal)pigeonRes.getAggregateValue("min(children.age)")).compareTo(BigDecimal.valueOf(3)) == 0);
		assertTrue(((BigDecimal)pigeonRes.getAggregateValue("max(children.age)")).compareTo(BigDecimal.valueOf(7)) == 0);
		assertEquals(Long.valueOf(4), (Long)pigeonRes.getAggregateValue("count(children.age)"));
		assertEquals(Long.valueOf(4), (Long)pigeonRes.getAggregateValue("count(children.id)"));
		
		// test group by on a nested field
		//c = env.getSelectCriteriaFromDAL("select father.name, count(id), min(age), max(age), avg(age), sum(age), min(children.age), max(children.age), avg(children.age), sum(children.age) from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " GROUP BY father.name");
		c = env.getSelectCriteriaFromDAL("select father.name, min(children.age), max(children.age), avg(children.age), sum(children.age) from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " GROUP BY father.name");
		
		// add grand children
		createPigeon("son5", 1, son3, pigeonType, env);
		createPigeon("son6", 3, son4, pigeonType, env);
		createPigeon("son7", 8, son4, pigeonType, env);
		
		assertEquals(2, env.getSelectCriteriaFromDAL("select id from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where father.name = '" + father2.getField("name") + "'").list().size());
		assertEquals(Long.valueOf(2), ((QueryResult)env.getSelectCriteriaFromDAL("select count(id) from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where father.name = '" + father2.getField("name") + "' group by father.name").list().get(0)).getAggregateValue("count(id)"));
		
		env.getSelectCriteriaFromDAL("select age, father.name from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME).list(); 
		pigeons = c.list();
		
		// there are six different father names in the collection, including null value
		assertEquals(6, pigeons.size());
		
		for (Record pigeon : pigeons)
		{
			pigeonRes = (QueryResult)pigeon;
			
			String fatherName = (String)pigeonRes.getGroupByValue("father.name");
			
			if (father1.getField("name").equals(fatherName))
			{
				//assertEquals(Long.valueOf(2), pigeonRes.getAggregateValue("count(id)"));
				
				assertNull(pigeonRes.getAggregateValue("min(children.age)"));
				assertNull(pigeonRes.getAggregateValue("max(children.age)"));
				assertNull(pigeonRes.getAggregateValue("sum(children.age)"));
				assertNull(pigeonRes.getAggregateValue("avg(children.age)"));
				
				/*assertTrue(((BigDecimal)pigeonRes.getAggregateValue("avg(age)")).compareTo(BigDecimal.valueOf(6.5)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("sum(age)")).compareTo(BigDecimal.valueOf(13)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("min(age)")).compareTo(BigDecimal.valueOf(6)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("max(age)")).compareTo(BigDecimal.valueOf(7)) == 0);*/
			}
			else if (father2.getField("name").equals(fatherName))
			{
				/*assertEquals(Long.valueOf(2), pigeonRes.getAggregateValue("count(id)"));
				
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("avg(age)")).compareTo(BigDecimal.valueOf(3)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("sum(age)")).compareTo(BigDecimal.valueOf(6)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("min(age)")).compareTo(BigDecimal.valueOf(3)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("max(age)")).compareTo(BigDecimal.valueOf(3)) == 0);*/
				
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("avg(children.age)")).compareTo(BigDecimal.valueOf(4)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("sum(children.age)")).compareTo(BigDecimal.valueOf(12)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("min(children.age)")).compareTo(BigDecimal.valueOf(1)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("max(children.age)")).compareTo(BigDecimal.valueOf(8)) == 0);
			}
		}
		
		try
		{
			env.getSelectCriteriaFromDAL("select father.name, count(id), min(age), max(age), avg(age), sum(age), min(children.age), max(children.age), avg(children.age), sum(children.age) from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " GROUP BY father.name").list();
			fail("Error should be thrown because aggregate query is run on both collection and non-collection field");
		}
		catch (DALSyntaxException e)
		{
			assertEquals("Cannot call aggregate function on both collections and non-collections", e.getMessage());
		}
	}

	private void testGroupBy(Type pigeonType, EnvData env) throws KommetException
	{
		Record father1 = createPigeon("father1", 6, null, pigeonType, env);
		Record father2 = createPigeon("father2", 7, null, pigeonType, env);
		
		createPigeon("son.1.4", 6, father1, pigeonType, env);
		createPigeon("son.1.5", 7, father1, pigeonType, env);
		createPigeon("son.1.6", 6, father2, pigeonType, env);
		
		String query = "select age, min(father.age), max(father.age), avg(father.age), sum(father.age) from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + "  where name ilike 'son.1%' group by age"; 
		
		List<Record> pigeons = env.getSelectCriteriaFromDAL(query).list();
		assertEquals(2, pigeons.size());
		
		for (Record pigeon : pigeons)
		{
			QueryResult pigeonRes = (QueryResult)pigeon;
			assertNotNull(pigeon.getField("age"));
			
			Integer age = (Integer)pigeon.getField("age");
			if (age == 7)
			{
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("avg(father.age)")).compareTo(BigDecimal.valueOf(6)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("sum(father.age)")).compareTo(BigDecimal.valueOf(6)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("min(father.age)")).compareTo(BigDecimal.valueOf(6)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("max(father.age)")).compareTo(BigDecimal.valueOf(6)) == 0);
			}
			else if (age == 6)
			{
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("avg(father.age)")).compareTo(BigDecimal.valueOf(6.5)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("sum(father.age)")).compareTo(BigDecimal.valueOf(13)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("min(father.age)")).compareTo(BigDecimal.valueOf(6)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("max(father.age)")).compareTo(BigDecimal.valueOf(7)) == 0);
			}
			else
			{
				fail("Unexpected age value " + age);
			}
		}
		
		// create another son pigeon
		createPigeon("son4", 4, father1, pigeonType, env);
		
		// test group by with limit, offset and order by
		pigeons = env.getSelectCriteriaFromDAL(query + " order by age DESC limit 1 offset 1").list();
		assertEquals(1, pigeons.size());
		
		for (Record pigeon : pigeons)
		{
			QueryResult pigeonRes = (QueryResult)pigeon;
			assertNotNull(pigeon.getField("age"));
			
			Integer age = (Integer)pigeon.getField("age");
			if (age == 6)
			{
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("avg(father.age)")).compareTo(BigDecimal.valueOf(6.5)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("sum(father.age)")).compareTo(BigDecimal.valueOf(13)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("min(father.age)")).compareTo(BigDecimal.valueOf(6)) == 0);
				assertTrue(((BigDecimal)pigeonRes.getAggregateValue("max(father.age)")).compareTo(BigDecimal.valueOf(7)) == 0);
			}
			else
			{
				fail("Unexpected age value " + age);
			}
		}
		
	}

	private Record createPigeon(String name, int age, Record father, Type pigeonType, EnvData env) throws KommetException
	{
		Record pigeon = dataService.instantiate(pigeonType.getKID(), env);
		pigeon.setField("name", name);
		pigeon.setField("age", age);
		if (father != null)
		{
			pigeon.setField("father.id", father.getKID(), env);
		}
		return dataService.save(pigeon, env);
	}

	private void testAggregateFunction(String function, String property, BigDecimal expectedResult, EnvData env, Type pigeonType) throws KommetException
	{
		String functionCall = function + "(" + property + ")";
		
		List<Record> aggregateResults = env.getSelectCriteriaFromDAL("SELECT " + functionCall + " FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME).list();
		
		assertNotNull(aggregateResults);
		assertEquals(1, aggregateResults.size());
		
		Record aggregateResult = aggregateResults.get(0);
		if (!(aggregateResult instanceof QueryResult))
		{
			fail("DAL query with aggregate function " + function + "() should return an object of type " + QueryResult.class.getName() + ", but it returned object of type " + aggregateResult.getClass().getName());
		}
		
		QueryResult result = (QueryResult)aggregateResult;
		// extract value both by lower and upper case
		
		// first try accessing value that is not in the result set
		try
		{
			result.getAggregateValue("invalid-value");
			fail("Accessing an invalid result set field should throw an exception");
		}
		catch (InvalidResultSetAccess e)
		{
			// expected exception
		}
		
		assertTrue("Aggregate function " + function + ": expected " + expectedResult + " but got " + (BigDecimal)result.getAggregateValue(functionCall), expectedResult.compareTo((BigDecimal)result.getAggregateValue(functionCall)) == 0);
		assertTrue(expectedResult.compareTo((BigDecimal)result.getAggregateValue(function.toUpperCase() + "(" + property + ")")) == 0);
		
		try
		{
			// make sure COUNT keyword is not mixed with regular select fields
			env.getSelectCriteriaFromDAL("SELECT " + functionCall + ", age FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME).list();
			fail("DAL query with both aggregate and regular fields should fail");
		}
		catch (DALSyntaxException e)
		{
			// expected exception
			assertTrue("Incorrect exception message verbiage: " + e.getMessage(), e.getMessage().startsWith("The following properties should be used either in an aggregate function or a group by clause:"));
		}
	}

	@SuppressWarnings("deprecation")
	@Test
	public void testOrderBy() throws KommetException
	{
		// insert 5 pigeons with different age and names
		for (int i = 0; i < 6; i++)
		{
			Record testPigeon = dataService.instantiate(this.pigeonType.getKID(), env);
			testPigeon.setField("name", "Zenek" + i);
			testPigeon.setField("age", 5 + i);
			testPigeon.setField("birthdate", new Date (112, 3, 4));
			dataService.save(testPigeon, this.env);
		}
		
		// find all pigeons with age greater than 6 and sort them desc by age
		List<Record> sortedPigeons = this.env.getSelectCriteriaFromDAL("select id, name, age FROM " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " WHERE age > 6 order by age desc").list();
		assertEquals(4, sortedPigeons.size());
		
		for (int i = 0; i < sortedPigeons.size(); i++)
		{
			assertEquals("Zenek" + (5 - i), sortedPigeons.get(i).getField("name"));
			assertEquals(((Integer)(10 - i)), ((Integer)sortedPigeons.get(i).getField("age")));
		}
	}
}
