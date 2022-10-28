/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.webmock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.dao.dal.AggregateFunction;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.RestrictionOperator;
import kommet.dao.queries.jcr.Grouping;
import kommet.dao.queries.jcr.JCR;
import kommet.dao.queries.jcr.JCRUtil;
import kommet.dao.queries.jcr.JcrSerializationException;
import kommet.dao.queries.jcr.Ordering;
import kommet.dao.queries.jcr.Property;
import kommet.dao.queries.jcr.Restriction;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.NoSuchFieldException;
import kommet.data.PIR;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.js.jsrc.JSRC;
import kommet.tests.harness.CompanyAppDataSet;
import kommet.utils.TestUtil;
import kommet.utils.UrlUtil;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/test-app-context.xml")
@Transactional
@WebAppConfiguration
@Rollback
public class JCRTest extends BasicWebMockTest
{	
	@Inject
	DataService dataService;

	@Test
	public void testJCR() throws Exception
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type pigeonType = dataService.createType(dataHelper.getFullPigeonType(env), env);
		
		// create another type to test the same scenarios again
		// but not interfere with records already created for the pigeon type
		Type birdType = dataHelper.getFullPigeonType(env);
		birdType.setApiName("Bird");
		birdType.setLabel("Bird");
		birdType.setPluralLabel("Birds");
		birdType = dataService.createType(birdType, env);
		assertNotNull(birdType.getKID());
		assertNotNull(env.getType(birdType.getKID()));
		
		testPIR(pigeonType, env);
		testSerialization(pigeonType, false, env);
		testSerializationOfIsNullRestriction(pigeonType, env);
		testValidation(pigeonType, false, env);
		testTranslateDALCriteriaToJCR(pigeonType, env);
		testJCRAjaxCalls(pigeonType, false, env);
		
		testJCRAjaxCalls(birdType, true, env);
		testValidation(birdType, true, env);
		testSerialization(birdType, true, env);
		
		testJCRForSubqueries(env);
		testJCRForInRestriction(pigeonType, env);
	}
	
	private void testJCRForInRestriction(Type pigeonType, EnvData env) throws KommetException, JcrSerializationException
	{
		String pigeonName = "joaquin";
		
		// create a pigeon
		Record pigeon1 = new Record(pigeonType);
		pigeon1.setField("name", pigeonName);
		pigeon1.setField("age", BigDecimal.valueOf(2));
		dataService.save(pigeon1, env);
		assertNotNull(pigeon1.getKID());
		
		Criteria c = env.getSelectCriteriaFromDAL("select id, name from " + pigeonType.getQualifiedName() + " where name in ('john', '" + pigeonName + "')");
		JCR jcr = JCRUtil.getJCRFromDALCriteria(c, env);
		assertNotNull(jcr);
		
		List<Record> pigeons = c.list();
		assertEquals(1, pigeons.size());
		assertEquals(pigeon1.getKID(), pigeons.get(0).getKID());
		
		String jcrJSON = JCRUtil.serialize(jcr);
		assertTrue("Invalid json: " + jcrJSON, jcrJSON.contains("{\"baseTypeId\":\"" + pigeonType.getKID() + "\""));
		
		// parse back to criteria
		JCR restoredJCR = JCRUtil.deserialize(jcrJSON);
		String restoredQuery = restoredJCR.getQuery(env);
		
		// find the pigeon using the restored query
		pigeons = env.getSelectCriteriaFromDAL(restoredQuery).list();
		assertEquals(1, pigeons.size());
		assertEquals(pigeon1.getKID(), pigeons.get(0).getKID());
	}

	private void testJCRForSubqueries(EnvData env) throws KommetException, JcrSerializationException
	{
		CompanyAppDataSet dataSet = CompanyAppDataSet.getInstance(dataService, env);
		Record company1 = dataService.save(dataSet.getTestCompany("company-1", null), env);
		Record employee1 = dataService.save(dataSet.getTestEmployee("john", "bee", null, null, null), env);
		dataService.save(dataSet.getTestEmployee("john", "beek", null, null, null), env);
		
		createEmployeeAssociation(dataSet, env);
		
		Field employeesField = dataSet.getCompanyType().getField("employees");
		assertNotNull(employeesField);
		
		dataService.associate(employeesField.getKID(), company1.getKID(), employee1.getKID(), AuthData.getRootAuthData(env), env);
		
		Criteria c = env.getSelectCriteriaFromDAL("select id, firstName, lastName from " + CompanyAppDataSet.COMPANY_PACKAGE + "." + CompanyAppDataSet.EMPLOYEE_TYPE_API_NAME + " where id in (select employees.id from " + dataSet.getCompanyType().getQualifiedName() + " where id = '" + company1.getKID() + "') and firstName <> 'arthur' ORDER by firstName ASC" );
		
		kommet.dao.queries.Restriction topAndRestriction = c.getBaseRestriction().getSubrestrictions().get(0);
		assertEquals(RestrictionOperator.AND, topAndRestriction.getOperator());
		
		kommet.dao.queries.Restriction inRestriction = null;
		for (kommet.dao.queries.Restriction r : topAndRestriction.getSubrestrictions())
		{
			if (r.getOperator().equals(RestrictionOperator.IN))
			{
				inRestriction = r;
				break;
			}
		}
		
		assertNotNull(inRestriction);
		
		// make sure the argument of the IN restriction is a criteria object
		assertTrue(inRestriction.getValue() instanceof Criteria);
		assertTrue(((Criteria)inRestriction.getValue()).isSubquery());
		
		JCR jcr = JCRUtil.getJCRFromDALCriteria(c, env);
		assertNotNull(jcr);
		assertEquals(dataSet.getEmployeeType().getKID(), jcr.getBaseTypeId());
		assertEquals(3, jcr.getProperties().size());
		assertNotNull(jcr.getRestrictions());
		assertEquals(1, jcr.getRestrictions().size());
		assertEquals(RestrictionOperator.AND, jcr.getRestrictions().get(0).getOperator());
		
		Restriction topRestriction = ((Restriction)jcr.getRestrictions().get(0).getArgs().get(0));
		
		// check the subquery restriction
		Restriction subqueryRestriction = null;
		for (Object subrestrictionObj : topRestriction.getArgs())
		{
			assertTrue(subrestrictionObj instanceof Restriction);
			Restriction r = (Restriction)subrestrictionObj;
			
			if (r.getOperator().equals(RestrictionOperator.IN))
			{
				subqueryRestriction = r;
				break;
			}
		}
		
		assertNotNull(subqueryRestriction);
		assertEquals(1, subqueryRestriction.getArgs().size());
		assertTrue(subqueryRestriction.getArgs().get(0) instanceof JCR);
		
		// make sure the serialized JCR for the subquery contains only one property in the select clause (as did the original subquery, and that the ID property was not added automatically during serialization)
		assertEquals(1, ((JCR)subqueryRestriction.getArgs().get(0)).getProperties().size());
		assertEquals("employees.id", ((JCR)subqueryRestriction.getArgs().get(0)).getProperties().get(0).getName());
		
		String jcrJSON = JCRUtil.serialize(jcr);
		assertTrue("Invalid json: " + jcrJSON, jcrJSON.contains("{\"baseTypeId\":\"" + dataSet.getCompanyType().getKID() + "\""));
		
		// parse back to criteria
		JCR restoredJCR = JCRUtil.deserialize(jcrJSON);
		String restoredQuery = restoredJCR.getQuery(env);
		assertTrue(restoredQuery.toLowerCase().startsWith("select "));
		String normalizedRestoredQuery = restoredQuery.toLowerCase().replaceAll("[\\s]+", " ");
		assertTrue("Invalid restored query: " + normalizedRestoredQuery, normalizedRestoredQuery.contains("where id in (select employees.id from " + dataSet.getCompanyType().getQualifiedName().toLowerCase() + " where id = '" + company1.getKID() + "')"));
		
		// now run the restored query
		List<Record> employees = env.getSelectCriteriaFromDAL(restoredQuery).list();
		assertEquals(1, employees.size());
	}

	private void createEmployeeAssociation(CompanyAppDataSet dataSet, EnvData env) throws KommetException
	{
		// create a linking type
		Type linkingType = new Type();
		linkingType.setApiName("Employment");
		linkingType.setLabel("Employment");
		linkingType.setPluralLabel("Employments");
		linkingType.setPackage(CompanyAppDataSet.COMPANY_PACKAGE);
		
		// add reference to company type
		Field companyRef = new Field();
		companyRef.setApiName("company");
		companyRef.setLabel("Company");
		companyRef.setDataType(new TypeReference(dataSet.getCompanyType()));
		((TypeReference)companyRef.getDataType()).setCascadeDelete(true);
		companyRef.setRequired(true);
		linkingType.addField(companyRef);
		
		// add reference to employee type
		Field employeeRef = new Field();
		employeeRef.setApiName("employee");
		employeeRef.setLabel("Employee");
		employeeRef.setDataType(new TypeReference(dataSet.getEmployeeType()));
		((TypeReference)employeeRef.getDataType()).setCascadeDelete(true);
		employeeRef.setRequired(true);
		linkingType.addField(employeeRef);
		
		linkingType = dataService.createType(linkingType, dataHelper.getRootAuthData(env), env);
		
		// now create an association on company to employees
		Field association = new Field();
		association.setApiName("employees");
		association.setLabel("employees");
		association.setDataType(new AssociationDataType(linkingType, dataSet.getEmployeeType(), "company", "employee"));
		
		association.setType(dataSet.getCompanyType());
		association = dataService.createField(association, dataHelper.getRootAuthData(env), env);
		
		dataSet.setCompanyType(env.getType(dataSet.getCompanyType().getKID()));
	}

	private void testTranslateSimpleQueryToJCR (Type pigeonType, EnvData env) throws KommetException
	{
		// test translating simple query with one condition
		String originalQuery = "SELECT id, name, age, father.name FROM " + pigeonType.getQualifiedName() + " WHERE age = 3";
		Criteria dalCriteria = env.getSelectCriteriaFromDAL(originalQuery);
		assertEquals(3, dalCriteria.getProperties().size());
		assertEquals(1, dalCriteria.getNestedProperties().size());
		assertEquals(1, dalCriteria.getBaseRestriction().getSubrestrictions().size());
		kommet.dao.queries.Restriction firstSubrestriction = dalCriteria.getBaseRestriction().getSubrestrictions().get(0);
		assertNotNull(firstSubrestriction.getValue());
		assertTrue("Incorrect type of value: " + firstSubrestriction.getValue().getClass().getName(), firstSubrestriction.getValue() instanceof String);
		
		JCR jcr = JCRUtil.getJCRFromDALCriteria(dalCriteria, env);
		assertNotNull(jcr);
		assertEquals(pigeonType.getKID(), jcr.getBaseTypeId());
		assertEquals(4, jcr.getProperties().size());
		assertNotNull(jcr.getRestrictions());
		assertEquals(1, jcr.getRestrictions().size());
		
		// make sure for each property, name and PIR are set
		for (Property prop : jcr.getProperties())
		{
			assertNotNull("When creating JCR from criteria, property name needs to be set", prop.getName());
			assertNotNull("When creating JCR from criteria, property ID needs to be set", prop.getId());
			assertEquals(prop.getName(), JCRUtil.getPropertyName(prop, env.getType(jcr.getBaseTypeId()), env));
		}
		
		Restriction jcrBaseRestriction = jcr.getRestrictions().get(0);
		assertEquals(RestrictionOperator.AND, jcrBaseRestriction.getOperator());
		assertNotNull(jcrBaseRestriction.getArgs());
		assertEquals(1, jcrBaseRestriction.getArgs().size());
		assertTrue(jcrBaseRestriction.getArgs().get(0) instanceof Restriction);
		
		Restriction ageRestriction = (Restriction)jcrBaseRestriction.getArgs().get(0);
		assertEquals(RestrictionOperator.EQ, ageRestriction.getOperator());
		assertNotNull(ageRestriction.getArgs());
		assertEquals(1, ageRestriction.getArgs().size());
		assertEquals("3", ageRestriction.getArgs().get(0));
		assertEquals("age", ageRestriction.getPropertyName());
		assertEquals(PIR.get("age", pigeonType, env), ageRestriction.getPropertyId());
		
		// now get query from JCR
		TestUtil.assertQueriesEquals(originalQuery.trim().replaceAll("\\s+", " "), jcr.getQuery(env).trim().replaceAll("\\s+", " "));
	}
	
	private void testTranslateDALCriteriaToJCR(Type pigeonType, EnvData env) throws KommetException
	{
		testTranslateSimpleQueryToJCR(pigeonType, env);
		
		// test more complicated query
		Criteria dalCriteria = env.getSelectCriteriaFromDAL("SELECT id, name, age, father.name FROM " + pigeonType.getQualifiedName() + " WHERE age > 3 and name <> 'Alan' ORDER BY age ASC LIMIT 5 OFFSET 2");
		assertEquals(3, dalCriteria.getProperties().size());
		assertEquals(1, dalCriteria.getNestedProperties().size());
		JCR jcr = JCRUtil.getJCRFromDALCriteria(dalCriteria, env);
		assertNotNull(jcr);
		assertEquals(pigeonType.getKID(), jcr.getBaseTypeId());
		assertEquals(4, jcr.getProperties().size());
		assertNotNull(jcr.getOrderings());
		assertEquals(1, jcr.getOrderings().size());
		
		Ordering ageOrdering = jcr.getOrderings().get(0);
		assertEquals("ASC", ageOrdering.getSortDirection());
		assertEquals(PIR.get("age", pigeonType, env), ageOrdering.getPropertyId());
		assertEquals("age", ageOrdering.getPropertyName());
		
		Map<String, Boolean> foundProperties = new HashMap<String, Boolean>();
		foundProperties.put("id", false);
		foundProperties.put("name", false);
		foundProperties.put("age", false);
		foundProperties.put("father.name", false);
		
		for (String prop : foundProperties.keySet())
		{
			for (Property jcrProp : jcr.getProperties())
			{
				if (jcrProp.getName().equals(prop))
				{
					foundProperties.put(prop, true);
				}
			}
		}
		
		for (String prop : foundProperties.keySet())
		{
			assertTrue("Property " + prop + " not included in JCR", foundProperties.get(prop));
		}
		
		// make sure limit and offset are translated
		assertEquals(dalCriteria.getLimit(), jcr.getLimit());
		assertEquals(dalCriteria.getOffset(), jcr.getOffset());
		
		// check restrictions
		assertEquals(1, jcr.getRestrictions().size());
		
		Restriction mainJCRRestriction = jcr.getRestrictions().get(0);
		assertEquals(RestrictionOperator.AND, mainJCRRestriction.getOperator());
		assertEquals(1, mainJCRRestriction.getArgs().size());
		assertTrue(mainJCRRestriction.getArgs().get(0) instanceof Restriction);
		mainJCRRestriction = (Restriction)mainJCRRestriction.getArgs().get(0);
		
		boolean nameRestrictionChecked = false;
		boolean ageRestrictionChecked = false;
		
		assertEquals(RestrictionOperator.AND, mainJCRRestriction.getOperator());
		
		for (Object arg : mainJCRRestriction.getArgs())
		{
			assertTrue(arg instanceof Restriction);
			Restriction r = (Restriction)arg;
			
			if (r.getPropertyName().equals("name"))
			{
				nameRestrictionChecked = true;
				assertEquals(RestrictionOperator.NE, r.getOperator());
			}
			else if (r.getPropertyName().equals("age"))
			{
				ageRestrictionChecked = true;
				assertEquals(RestrictionOperator.GT, r.getOperator());
			}
			else
			{
				fail("Unexpected restriction on property " + r.getPropertyName());
			}
		}
		
		assertTrue(nameRestrictionChecked);
		assertTrue(ageRestrictionChecked);
		
		// check another criteria, this time with aggregate functions
		dalCriteria = env.getSelectCriteriaFromDAL("SELECT COUNT(id), name FROM " + pigeonType.getQualifiedName() + " WHERE age = 3 OR name = 'Alan' GROUP BY name OFFSET 2");
		assertEquals(1, dalCriteria.getAggregateFunctions().size());
		assertEquals(1, dalCriteria.getProperties().size());
		jcr = JCRUtil.getJCRFromDALCriteria(dalCriteria, env);
		assertNotNull(jcr);
		
		assertEquals(2, jcr.getProperties().size());
		
		for (Property prop : jcr.getProperties())
		{
			assertNotNull(prop.getId());
			assertNotNull(prop.getName());
			
			if (prop.getName().equals("name"))
			{
				assertNull(prop.getAggregateFunction());
			}
			else if (prop.getName().equals("id"))
			{
				assertEquals(AggregateFunction.COUNT, prop.getAggregateFunction());
			}
			else
			{
				fail("Unexpected property in JCR " + prop.getName());
			}
		}
		
		// check restrictions
		assertEquals(1, jcr.getRestrictions().size());
		
		mainJCRRestriction = jcr.getRestrictions().get(0);
		assertEquals(RestrictionOperator.AND, mainJCRRestriction.getOperator());
		assertEquals(1, mainJCRRestriction.getArgs().size());
		assertTrue(mainJCRRestriction.getArgs().get(0) instanceof Restriction);
		mainJCRRestriction = (Restriction)mainJCRRestriction.getArgs().get(0);
		assertEquals(RestrictionOperator.OR, mainJCRRestriction.getOperator());
		
		for (Object arg : mainJCRRestriction.getArgs())
		{
			assertTrue(arg instanceof Restriction);
			Restriction subrestriction = (Restriction)arg;
			assertNotNull("PIR of subrestriction property should be set", subrestriction.getPropertyId());
			assertNotNull("Name of subrestriction property should be set", subrestriction.getPropertyName());
		}
		
		assertNotNull(jcr.getGroupings());
		assertEquals(1, jcr.getGroupings().size());
		Grouping grouping = jcr.getGroupings().get(0);
		assertEquals(PIR.get("name", pigeonType, env), grouping.getPropertyId());
		assertEquals("name", grouping.getPropertyName());
	}

	private void testSerializationOfIsNullRestriction(Type pigeonType, EnvData env) throws JcrSerializationException, KommetException
	{
		Field motherField = pigeonType.getField("mother");
		Field ageField = pigeonType.getField("age");
		JCR c = new JCR();
		
		Property prop1 = new Property();
		prop1.setId(new PIR(motherField.getKID() + "." + ageField.getKID()));
		prop1.setName("mother.age");
		c.addProperty(prop1);
		
		c.setBaseTypeId(pigeonType.getKID());
		
		Restriction r1 = new Restriction();
		r1.setArgs(null);
		r1.setOperator(RestrictionOperator.ISNULL);
		r1.setPropertyId(prop1.getId());
		
		String json = JCRUtil.serialize(c);
		JCR deserializedJCR = JCRUtil.deserialize(json);
		
		// deserialize JCR that contains an isnull restriction with empty args array
		// testing this because it returned an error at some point due to incorrect interpretation of an empty args array
		json = "{ \"baseTypeId\": \"" + pigeonType.getKID() + "\", \"properties\": [ { \"id\": \"" + motherField.getKID() + "\" }], \"restrictions\": [ { \"property_id\": \"" + motherField.getKID() + "\", \"operator\": \"isnull\", \"args\": [] }] }";
		deserializedJCR = JCRUtil.deserialize(json);
		String builtQuery = deserializedJCR.getQuery(env);
		assertNotNull(builtQuery);
		
		json = "{ \"baseTypeName\": \"" + pigeonType.getQualifiedName() + "\", \"properties\": [ { \"id\": \"" + motherField.getKID() + "\" }], \"restrictions\": [ { \"property_id\": \"" + motherField.getKID() + "\", \"operator\": \"isnull\", \"args\": [] }] }";
		deserializedJCR = JCRUtil.deserialize(json);
		String builtQuery2 = deserializedJCR.getQuery(env);
		assertNotNull(builtQuery2);
		assertEquals(builtQuery, builtQuery2);
	}

	/**
	 * Test Ajax calls that use JCR to retrieve records.
	 * @param type
	 * @param propertiesOnlyByName Whether properties should be specified only by their name, or also by their PIR.
	 * @param env
	 * @throws Exception
	 */
	private void testJCRAjaxCalls(Type type, boolean propertiesOnlyByName, EnvData env) throws Exception
	{
		Field motherField = type.getField("mother");
		Field ageField = type.getField("age");
		
		// create a pigeon
		Record pigeon1 = new Record(type);
		pigeon1.setField("name", "Bo");
		pigeon1.setField("age", BigDecimal.valueOf(2));
		dataService.save(pigeon1, env);
		assertNotNull(pigeon1.getKID());
		
		// create another pigeon
		Record pigeon2 = new Record(type);
		pigeon2.setField("name", "Li");
		pigeon2.setField("age", BigDecimal.valueOf(55));
		dataService.save(pigeon2, env);
		assertNotNull(pigeon2.getKID());
		
		JCR c = new JCR();
		
		Property prop1 = new Property();
		
		if (!propertiesOnlyByName)
		{
			prop1.setId(new PIR(motherField.getKID() + "." + ageField.getKID()));
		}
		prop1.setName("mother.age");
		c.addProperty(prop1);
		
		Property prop2 = new Property();
		
		if (!propertiesOnlyByName)
		{
			prop2.setId(new PIR(ageField.getKID().getId()));
		}
		prop2.setName("age");
		c.addProperty(prop2);
		
		if (propertiesOnlyByName)
		{
			c.setBaseTypeName(type.getQualifiedName());
		}
		else
		{
			c.setBaseTypeId(type.getKID());
		}
		
		Ordering ordering = new Ordering();
		
		if (propertiesOnlyByName)
		{
			ordering.setPropertyName(prop2.getName());
		}
		else
		{
			ordering.setPropertyId(prop2.getId());
		}
		ordering.setSortDirection("ASC");
		c.addOrdering(ordering);
		
		// test serialization
		String serializedJCR = JCRUtil.serialize(c);
		
		String accessToken = obtainAccessToken(true, env);
		
		MvcResult result = this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_QUERY_DS_URL)
				.param("access_token", accessToken)
				.param("env", env.getId().getId())
				.param("jcr", serializedJCR)
				.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
	        	.andReturn();
		
		String respJSON = result.getResponse().getContentAsString();
		assertTrue("Empty response returned", StringUtils.hasText(respJSON));
		
		assertEquals("Response code " + result.getResponse().getStatus() + ". The response JSON is " + respJSON, result.getResponse().getStatus(), HttpServletResponse.SC_OK);
		
		// convert response JSON to JSRC
		JSRC jsrc = JSRC.deserialize(respJSON);
		assertNotNull(jsrc);
		assertNotNull(jsrc.getJsti());
		assertNotNull(jsrc.getJsti().getTypes().get(type.getKID()));
		assertNotNull(jsrc.getRecords());
		assertFalse(jsrc.getRecords().isEmpty());
		assertEquals(2, jsrc.getRecords().size());
		
		LinkedHashMap<String, Object> fetchedPigeon = jsrc.getRecords().get(0);
		
		// make sure only fields specified in the JCR passed to the query have been retrieved
		assertTrue(fetchedPigeon.containsKey(PIR.get("mother", type, env).getValue()));
		assertTrue(fetchedPigeon.containsKey(PIR.get("age", type, env).getValue()));
		assertFalse(fetchedPigeon.containsKey(PIR.get("name", type, env).getValue()));
		assertFalse(fetchedPigeon.containsKey(PIR.get("father", type, env).getValue()));
		
		// now issue a count() query
		JCR countJCR = new JCR();
		prop2.setAggregateFunction(AggregateFunction.COUNT);
		countJCR.addProperty(prop2);
		
		if (propertiesOnlyByName)
		{
			countJCR.setBaseTypeName(type.getQualifiedName());
		}
		else
		{
			countJCR.setBaseTypeId(type.getKID());
		}
		
		serializedJCR = JCRUtil.serialize(countJCR);
		result = this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_QUERY_DS_URL)
				.param("access_token", accessToken)
				.param("env", env.getId().getId())
				.param("jcr", serializedJCR)
				.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
	        	.andExpect(status().isOk())
	        	.andReturn();
		
		respJSON = result.getResponse().getContentAsString();
		assertTrue("Empty response returned", StringUtils.hasText(respJSON));
		
		// convert response JSON to JSRC
		jsrc = JSRC.deserialize(respJSON);
		assertNotNull(jsrc);
		assertNotNull(jsrc.getJsti());
		assertNotNull(jsrc.getJsti().getTypes().get(type.getKID()));
		assertNotNull(jsrc.getRecords());
		assertFalse(jsrc.getRecords().isEmpty());
		assertEquals(1, jsrc.getRecords().size());
		
		LinkedHashMap<String, Object> countResult = jsrc.getRecords().get(0);
		assertTrue(countResult.containsKey("count(" + PIR.get("age", type, env) + ")"));
		assertEquals(2, countResult.get("count(" + PIR.get("age", type, env) + ")"));
		
		// test a group by query
		JCR groupByJCR = new JCR();
		Grouping grouping = new Grouping();
		
		if (propertiesOnlyByName)
		{
			grouping.setPropertyName(prop2.getName());
			groupByJCR.setBaseTypeName(type.getQualifiedName());
		}
		else
		{
			grouping.setPropertyId(prop2.getId());
			groupByJCR.setBaseTypeId(type.getKID());
		}
		
		groupByJCR.addGrouping(grouping);
		prop2.setAggregateFunction(null);
		groupByJCR.addProperty(prop2);
		
		serializedJCR = JCRUtil.serialize(groupByJCR);
		
		result = this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_QUERY_DS_URL)
				.param("access_token", accessToken)
				.param("env", env.getId().getId())
				.param("jcr", serializedJCR)
				.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
	        	.andExpect(status().isOk())
	        	.andReturn();
		
		respJSON = result.getResponse().getContentAsString();
		assertTrue("Empty response returned", StringUtils.hasText(respJSON));
		
		// convert response JSON to JSRC
		jsrc = JSRC.deserialize(respJSON);
		assertNotNull(jsrc);
		assertNotNull(jsrc.getJsti());
		assertNotNull(jsrc.getJsti().getTypes().get(type.getKID()));
		assertNotNull(jsrc.getRecords());
		assertFalse(jsrc.getRecords().isEmpty());
		assertEquals(2, jsrc.getRecords().size());
		
		LinkedHashMap<String, Object> groupByResult = jsrc.getRecords().get(0);
		assertTrue(groupByResult.containsKey(PIR.get("age", type, env).getValue()));
		Integer val = (Integer)groupByResult.get(PIR.get("age", type, env).getValue());
		assertTrue(val == 55 || val == 2);
		
		// test executing query in "datasource" mode
		result = this.mockMvc.perform(post("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_QUERY_DS_URL)
				.param("access_token", accessToken)
				.param("env", env.getId().getId())
				.param("jcr", serializedJCR)
				.param("mode", "datasource")
				.accept(MediaType.parseMediaType("application/html;charset=UTF-8")))
	        	.andExpect(status().isOk())
	        	.andReturn();
		
		respJSON = result.getResponse().getContentAsString();
		assertTrue("Empty response returned", StringUtils.hasText(respJSON));
		assertTrue(respJSON.startsWith("{ \"jsrc\": {"));
		
		// convert response JSON to JSRC
		jsrc = JSRC.deserialize(respJSON.substring(respJSON.indexOf("\"jsrc\": ") + "\"jsrc\": ".length(), respJSON.indexOf("\"recordCount\"")));
		assertNotNull(jsrc);
		assertNotNull(jsrc.getJsti());
		assertNotNull(jsrc.getJsti().getTypes().get(type.getKID()));
		assertNotNull(jsrc.getRecords());
		assertFalse(jsrc.getRecords().isEmpty());
		assertEquals(2, jsrc.getRecords().size());
	}

	private void testValidation(Type pigeonType, boolean propertyNameOnly, EnvData env) throws JcrSerializationException, KommetException
	{
		// get father field
		Field fatherField = pigeonType.getField("father");
		Field motherField = pigeonType.getField("mother");
		Field ageField = pigeonType.getField("age");
		
		PIR pir = new PIR(fatherField.getKID() + "." + motherField.getKID() + "." + ageField.getKID());
		assertEquals("father.mother.age", JCRUtil.pirToNestedProperty(pir, pigeonType, env));
		
		pir = new PIR(ageField.getKID() + ". " + fatherField.getKID() + "." + motherField.getKID() + "." + ageField.getKID());
		
		try
		{
			JCRUtil.pirToNestedProperty(pir, pigeonType, env);
			fail("Translating property with invalid reference on a non-type reference field should fail");
		}
		catch (KommetException e)
		{
			assertTrue(e.getMessage().startsWith("Property age on type "));
		}
		
		JCR c = new JCR();
		
		Property prop1 = new Property();
		
		if (propertyNameOnly)
		{
			prop1.setName("mother.age");
			c.setBaseTypeName(pigeonType.getQualifiedName());
		}
		else
		{
			prop1.setId(new PIR(motherField.getKID() + "." + ageField.getKID()));
			c.setBaseTypeId(pigeonType.getKID());
		}
		c.addProperty(prop1);
		
		String dal = c.getQuery(env);
		assertEquals("select mother.age from " + pigeonType.getQualifiedName().toLowerCase(), dal.toLowerCase());
		
		// test serialization
		String json = JCRUtil.serialize(c);
		assertNotNull(json);
		
		// age property JSON will be different depending on whether JCR contained property ID or property name
		String agePropertyJSON = propertyNameOnly ? "\"id\":null,\"name\":\"mother.age\"" : "\"id\":\"" + motherField.getKID() + "." + ageField.getKID() + "\",\"name\":null";
		String baseTypeJSON = null;
		
		if (propertyNameOnly)
		{
			baseTypeJSON = "\"baseTypeId\":null,\"baseTypeName\":\"" + pigeonType.getQualifiedName() + "\"";
		}
		else
		{
			baseTypeJSON = "\"baseTypeId\":\"" + pigeonType.getKID() + "\",\"baseTypeName\":null";
		}
		
		String expectedJSON = "{" + baseTypeJSON + ",\"properties\":[{" + agePropertyJSON + ",\"alias\":null,\"aggr\":null}],\"groupings\":null,\"restrictions\":null,\"orderings\":null,\"limit\":null,\"offset\":null}";
		assertEquals(expectedJSON, json);
		
		// add aggregated field to criteria
		Property prop2 = new Property();
		
		if (propertyNameOnly)
		{
			prop2.setName("mother.father.age");
		}
		else
		{
			prop2.setId(new PIR(motherField.getKID() + "." + fatherField.getKID() + "." + ageField.getKID()));
		}
		// add aggregate function for one field but leave the other field outside an aggregate function, which will cause a
		// validation error
		prop2.setAggregateFunction(AggregateFunction.SUM);
		c.addProperty(prop2);
		
		assertEquals(1, JCRUtil.validate(c, env).size());
		
		prop1.setAggregateFunction(AggregateFunction.AVG);
		assertTrue(JCRUtil.validate(c, env).isEmpty());
		
		// clear properties
		c.setProperties(null);
		c.setGroupings(null);
		
		// add property one once again, but without the aggregate function
		prop1.setAggregateFunction(null);
		c.addProperty(prop1);
		assertTrue(JCRUtil.validate(c, env).isEmpty());
		
		// now add aggregate function
		Grouping group = new Grouping();
		
		if (propertyNameOnly)
		{
			group.setPropertyName(prop2.getName());
		}
		else
		{
			group.setPropertyId(prop2.getId());
		}
		c.addGrouping(group);
		assertEquals(1, JCRUtil.validate(c, env).size());
		
		// now aggregate the first property
		prop1.setAggregateFunction(AggregateFunction.MAX);
		assertTrue(JCRUtil.validate(c, env).isEmpty());
		
		// add another grouping
		Field nameField = pigeonType.getField("name");
		Grouping group2 = new Grouping();
		
		if (propertyNameOnly)
		{
			group2.setPropertyName(nameField.getApiName());
		}
		else
		{
			group2.setPropertyId(new PIR(nameField.getKID().getId()));
		}
		c.addGrouping(group2);
		assertTrue(JCRUtil.validate(c, env).isEmpty());
	}

	private void testSerialization(Type pigeonType, boolean usePropertyNames, EnvData env) throws KommetException, JcrSerializationException
	{
		Field fatherField = pigeonType.getField("father");
		Field motherField = pigeonType.getField("mother");
		Field ageField = pigeonType.getField("age");
		
		PIR pir = new PIR(fatherField.getKID() + "." + motherField.getKID() + "." + ageField.getKID());
		assertEquals("father.mother.age", JCRUtil.pirToNestedProperty(pir, pigeonType, env));
		
		Field reparsedField = PIR.getField(pir, pigeonType, env);
		assertNotNull(reparsedField);
		assertEquals(ageField.getKID(), reparsedField.getKID());
		
		pir = new PIR(ageField.getKID() + ". " + fatherField.getKID() + "." + motherField.getKID() + "." + ageField.getKID());
		
		try
		{
			JCRUtil.pirToNestedProperty(pir, pigeonType, env);
			fail("Translating property with invalid reference on a non-type reference field should fail");
		}
		catch (KommetException e)
		{
			assertTrue(e.getMessage().startsWith("Property age on type "));
		}
		
		JCR c = new JCR();
		
		Property prop1 = new Property();
		prop1.setId(new PIR(motherField.getKID() + "." + ageField.getKID()));
		prop1.setName("mother.age");
		c.addProperty(prop1);
		
		Ordering idOrdering = new Ordering();
		
		if (usePropertyNames)
		{
			idOrdering.setPropertyName(Field.ID_FIELD_NAME);
		}
		else
		{
			idOrdering.setPropertyId(PIR.get(Field.ID_FIELD_NAME, pigeonType, env));
		}
		idOrdering.setSortDirection("ASC");
		c.addOrdering(idOrdering);
		
		c.setBaseTypeId(pigeonType.getKID());
		
		String dal = c.getQuery(env);
		assertEquals("select mother.age from " + pigeonType.getQualifiedName().toLowerCase() + " order by id asc", dal.toLowerCase());
		
		c.setOrderings(null);
		
		// test serialization
		String json = JCRUtil.serialize(c);
		assertNotNull(json);
		
		String expectedJSON = "{\"baseTypeId\":\"" + pigeonType.getKID() + "\",\"baseTypeName\":null,\"properties\":[{\"id\":\"" + motherField.getKID() + "." + ageField.getKID() + "\",\"name\":\"mother.age\",\"alias\":null,\"aggr\":null}],\"groupings\":null,\"restrictions\":null,\"orderings\":null,\"limit\":null,\"offset\":null}";
		assertEquals(expectedJSON, json);
		
		// add aggregated field to criteria
		Property prop2 = new Property();
		prop2.setId(new PIR(motherField.getKID() + "." + fatherField.getKID() + "." + ageField.getKID()));
		prop2.setName("mother.father.age");
		prop2.setAggregateFunction(AggregateFunction.SUM);
		c.addProperty(prop2);
		
		json = JCRUtil.serialize(c);
		expectedJSON = "{\"baseTypeId\":\"" + pigeonType.getKID() + "\",\"baseTypeName\":null,\"properties\":[{\"id\":\"" + motherField.getKID() + "." + ageField.getKID() + "\",\"name\":\"mother.age\",\"alias\":null,\"aggr\":null},{\"id\":\"" + motherField.getKID() + "." + fatherField.getKID() + "." + ageField.getKID() + "\",\"name\":\"mother.father.age\",\"alias\":null,\"aggr\":\"sum\"}],\"groupings\":null,\"restrictions\":null,\"orderings\":null,\"limit\":null,\"offset\":null}";
		assertEquals(expectedJSON, json);
		
		// add grouping to criteria
		Grouping group = new Grouping();
		
		if (usePropertyNames)
		{
			group.setPropertyName(fatherField.getApiName() + "." + ageField.getApiName());
		}
		else
		{
			group.setPropertyId(new PIR(fatherField.getKID() + "." + ageField.getKID()));
		}
		group.setAlias("sth");
		c.addGrouping(group);
		
		json = JCRUtil.serialize(c);
		
		if (usePropertyNames)
		{
			expectedJSON = expectedJSON.replaceAll("\"groupings\":null", "\"groupings\":[{\"alias\":\"sth\",\"property_id\":null,\"property_name\":\"" + group.getPropertyName() + "\"}]");
		}
		else
		{
			expectedJSON = expectedJSON.replaceAll("\"groupings\":null", "\"groupings\":[{\"alias\":\"sth\",\"property_id\":\"" + group.getPropertyId().getValue() + "\",\"property_name\":null}]");
		}
		assertEquals(expectedJSON, json);
		
		// parse back to JCR
		JCR deserializedJCR = JCRUtil.deserialize(json);
		assertNotNull(deserializedJCR);
		assertEquals(2, deserializedJCR.getProperties().size());
		assertEquals(1, deserializedJCR.getGroupings().size());
		assertEquals(pigeonType.getKID(), deserializedJCR.getBaseTypeId());
		assertEquals(prop1.getId().getValue(), deserializedJCR.getProperties().get(0).getId().getValue());
		assertEquals(prop2.getId().getValue(), deserializedJCR.getProperties().get(1).getId().getValue());
		
		// add restriction to criteria
		Restriction andCondition = new Restriction();
		andCondition.setOperator(RestrictionOperator.AND);
		
		Field nameField = pigeonType.getField("name");
		Restriction nameCondition = new Restriction();
		nameCondition.setOperator(RestrictionOperator.EQ);
		
		if (usePropertyNames)
		{
			nameCondition.setPropertyName(nameField.getApiName());
		}
		else
		{
			nameCondition.setPropertyId(new PIR(nameField.getKID().getId()));
		}
		nameCondition.addArg("'Kamila'");
		
		Restriction ageCondition = new Restriction();
		ageCondition.setOperator(RestrictionOperator.GT);
		
		if (usePropertyNames)
		{
			ageCondition.setPropertyName(ageField.getApiName());
		}
		else
		{
			ageCondition.setPropertyId(new PIR(ageField.getKID().getId()));
		}
		ageCondition.addArg(6);
		
		andCondition.addArg(ageCondition);
		andCondition.addArg(nameCondition);
		
		c.addRestriction(andCondition);
		
		assertNotNull(c.getQuery(env));
		
		json = JCRUtil.serialize(c);
		
		String ageRestrictionJSON = null;
		String nameRestrictionJSON = null;
		if (usePropertyNames)
		{
			ageRestrictionJSON = "\"property_id\":null,\"property_name\":\"" + ageField.getApiName() + "\"";
			nameRestrictionJSON = "\"property_id\":null,\"property_name\":\"" + nameField.getApiName() + "\"";
		}
		else
		{
			ageRestrictionJSON = "\"property_id\":\"" + ageField.getKID() + "\",\"property_name\":null";
			nameRestrictionJSON = "\"property_id\":\"" + nameField.getKID() + "\",\"property_name\":null";
		}
		
		String restrictionJSON = "\"restrictions\":[{\"operator\":\"and\",\"args\":[{\"operator\":\"gt\",\"args\":[6]," + ageRestrictionJSON + "},{\"operator\":\"eq\",\"args\":[\"'Kamila'\"]," + nameRestrictionJSON + "}],\"property_id\":null,\"property_name\":null}]";
		assertTrue("Invalid serialized criteria: " + json, json.contains(restrictionJSON));
		
		// deserialize criteria
		deserializedJCR = JCRUtil.deserialize(json);
		assertNotNull(deserializedJCR);
		assertNotNull(deserializedJCR.getQuery(env));
		assertEquals(1, deserializedJCR.getRestrictions().size());
		assertEquals(RestrictionOperator.AND, deserializedJCR.getRestrictions().get(0).getOperator());
		Restriction deserializedAgeRestriction = (Restriction)deserializedJCR.getRestrictions().get(0).getArgs().get(0);
		assertEquals(RestrictionOperator.GT, deserializedAgeRestriction.getOperator());
		
		if (usePropertyNames)
		{
			assertNull(deserializedAgeRestriction.getPropertyId());
			assertEquals(ageCondition.getPropertyName(), deserializedAgeRestriction.getPropertyName());
		}
		else
		{
			assertNull(deserializedAgeRestriction.getPropertyName());
			assertEquals(ageCondition.getPropertyId().getValue(), deserializedAgeRestriction.getPropertyId().getValue());
		}
		assertNotNull(deserializedAgeRestriction.getArgs());
		assertEquals(Integer.valueOf(6), deserializedAgeRestriction.getArgs().get(0));
		assertEquals(RestrictionOperator.GT, deserializedAgeRestriction.getOperator());
		
		Restriction deserializedNameRestriction = (Restriction)deserializedJCR.getRestrictions().get(0).getArgs().get(1);
		
		if (usePropertyNames)
		{
			assertEquals(nameCondition.getPropertyName(), deserializedNameRestriction.getPropertyName());
		}
		else
		{
			assertEquals(nameCondition.getPropertyId().getValue(), deserializedNameRestriction.getPropertyId().getValue());
		}
		assertNotNull(deserializedNameRestriction.getArgs());
		assertEquals("'Kamila'", deserializedNameRestriction.getArgs().get(0));
		
		// test deserializing criteria with null values
		ageCondition.setArgs(null);
		nameCondition.setPropertyId(null);
		deserializedJCR = JCRUtil.deserialize(JCRUtil.serialize(c));
		assertEquals(1, deserializedJCR.getRestrictions().size());
		assertEquals(RestrictionOperator.AND, deserializedJCR.getRestrictions().get(0).getOperator());
		
		deserializedAgeRestriction = (Restriction)deserializedJCR.getRestrictions().get(0).getArgs().get(0);
		
		if (usePropertyNames)
		{
			assertEquals(ageCondition.getPropertyName(), deserializedAgeRestriction.getPropertyName());
		}
		else
		{
			assertEquals(ageCondition.getPropertyId().getValue(), deserializedAgeRestriction.getPropertyId().getValue());
		}
		assertNull(deserializedAgeRestriction.getArgs());
		
		deserializedNameRestriction = (Restriction)deserializedJCR.getRestrictions().get(0).getArgs().get(1);
		assertNull("Expected null, but got " + deserializedNameRestriction.getPropertyId(), deserializedNameRestriction.getPropertyId());
		assertNotNull(deserializedNameRestriction.getArgs());
		
		// add ordering
		Ordering ordering = new Ordering();
		ordering.setPropertyId(prop1.getId());
		ordering.setSortDirection("asc");
		c.addOrdering(ordering);
		
		json = JCRUtil.serialize(c);
		assertTrue("Incorrect JSON " + json,json.contains("\"orderings\":[{\"property_id\":\"" + prop1.getId().getValue() + "\""));
		deserializedJCR = JCRUtil.deserialize(json);
		assertEquals(1, deserializedJCR.getOrderings().size());
		assertEquals(prop1.getId(), deserializedJCR.getOrderings().get(0).getPropertyId());
		assertEquals("asc", deserializedJCR.getOrderings().get(0).getSortDirection());
		
		// test limit and offset
		c = new JCR();
		Property nameProp = new Property();
		nameProp.setId(new PIR(motherField.getKID() + "." + ageField.getKID()));
		nameProp.setName("mother.age");
		c.addProperty(nameProp);
		c.setBaseTypeId(pigeonType.getKID());
		c.setLimit(10);
		c.setOffset(2);
		
		String limitDAL = c.getQuery(env);
		assertTrue(limitDAL.endsWith("LIMIT 10 OFFSET 2"));
	}

	private void testPIR (Type pigeonType, EnvData env) throws KommetException
	{
		Field fatherField = pigeonType.getField("father");
		Field motherField = pigeonType.getField("mother");
		Field ageField = pigeonType.getField("age");
		
		String nestedProp = "father.mother.age";
		PIR pir = PIR.get(nestedProp, pigeonType, env);
		assertEquals(fatherField.getKID() + "." + motherField.getKID() + "." + ageField.getKID(), pir.getValue());
		
		try
		{
			PIR.get("father.something", pigeonType, env);
			fail("Getting PIR from a non-existing property should fail");
		}
		catch (NoSuchFieldException e)
		{
			// expected
		}
		
		try
		{
			PIR.get("father.something.age", pigeonType, env);
			fail("Getting PIR from a non-existing property should fail");
		}
		catch (NoSuchFieldException e)
		{
			// expected
		}
		
		PIR profileNamePir = PIR.get("profile.name", env.getType(KeyPrefix.get(KID.USER_PREFIX)), env);
		assertNotNull(profileNamePir);
	}
}
