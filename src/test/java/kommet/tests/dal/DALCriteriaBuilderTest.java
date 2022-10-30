/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.dal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.dao.dal.DALCriteriaBuilder;
import kommet.dao.queries.Restriction;
import kommet.dao.queries.RestrictionOperator;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.utils.AppConfig;

public class DALCriteriaBuilderTest extends BaseUnitTest
{
	@Inject
	AppConfig appConfig;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Test
	public void testCriteria() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		AuthData authData = AuthData.getRootAuthData(env);
		
		testParseSimpleQuery(authData, env);
		testParseSimpleAndQuery(authData, env);
		testParseSimpleOrQuery(authData, env);
		testBracketQuery(authData, env);
	}
	
	private void testParseSimpleQuery(AuthData authData, EnvData env) throws KommetException
	{	
		String query = "age > 2";
		List<String> tokens = DALCriteriaBuilder.tokenize(query);
		assertEquals(3, tokens.size());
		assertEquals("age", tokens.get(0));
		assertEquals(">", tokens.get(1));
		assertEquals("2", tokens.get(2));
		
		Restriction r = DALCriteriaBuilder.getRestrictionFromDAL(tokens, 0, query, authData, env).getRestriction();
		assertNotNull(r);
		assertNull(r.getSubrestrictions());
		assertEquals(RestrictionOperator.GT, r.getOperator());
		assertEquals("age", r.getProperty());
		assertEquals("2", r.getValue());
	}
	
	private void testParseSimpleAndQuery(AuthData authData, EnvData env) throws KommetException
	{
		String query = "(age > 2 AND name = 'mark\\'s house')";
		List<String> tokens = DALCriteriaBuilder.tokenize(query);
		assertEquals(9, tokens.size());
		
		Restriction r = DALCriteriaBuilder.getRestrictionFromDAL(tokens, 0, query, authData, env).getRestriction();
		assertNotNull(r);
		assertNotNull(r.getSubrestrictions());
		assertEquals(2, r.getSubrestrictions().size());
		assertEquals(RestrictionOperator.AND, r.getOperator());
		
		Restriction ageRestriction = r.getSubrestrictions().get(0);
		assertEquals(RestrictionOperator.GT, ageRestriction.getOperator());
		assertEquals("age", ageRestriction.getProperty());
		assertEquals("2", ageRestriction.getValue());
		
		Restriction nameRestriction = r.getSubrestrictions().get(1);
		assertEquals(RestrictionOperator.EQ, nameRestriction.getOperator());
		assertEquals("name", nameRestriction.getProperty());
		assertEquals("mark\\'s house", nameRestriction.getValue());
	}
	
	private void testParseSimpleOrQuery(AuthData authData, EnvData env) throws KommetException
	{
		String query = "(age <> 2 OR name = 'abc')";
		List<String> tokens = DALCriteriaBuilder.tokenize(query);
		assertEquals(9, tokens.size());
		
		Restriction r = DALCriteriaBuilder.getRestrictionFromDAL(tokens, 0, query, authData, env).getRestriction();
		assertNotNull(r);
		assertNotNull(r.getSubrestrictions());
		assertEquals(2, r.getSubrestrictions().size());
		assertEquals(RestrictionOperator.OR, r.getOperator());
		
		Restriction ageRestriction = r.getSubrestrictions().get(0);
		assertEquals(RestrictionOperator.NE, ageRestriction.getOperator());
		assertEquals("age", ageRestriction.getProperty());
		assertEquals("2", ageRestriction.getValue());
		
		Restriction nameRestriction = r.getSubrestrictions().get(1);
		assertEquals(RestrictionOperator.EQ, nameRestriction.getOperator());
		assertEquals("name", nameRestriction.getProperty());
		assertEquals("abc", nameRestriction.getValue());
	}
	
	private void testBracketQuery(AuthData authData, EnvData env) throws KommetException
	{
		String query = "(age <> 2 OR (name = 'abc' AND age < 1))";
		
		Restriction r = DALCriteriaBuilder.getRestrictionFromDAL(DALCriteriaBuilder.tokenize(query), 0, query, authData, env).getRestriction();
		assertNotNull(r);
		assertNotNull(r.getSubrestrictions());
		assertEquals(2, r.getSubrestrictions().size());
		assertEquals(RestrictionOperator.OR, r.getOperator());
		
		Restriction ageRestriction = r.getSubrestrictions().get(0);
		assertEquals(RestrictionOperator.NE, ageRestriction.getOperator());
		assertEquals("age", ageRestriction.getProperty());
		assertEquals("2", ageRestriction.getValue());
		
		Restriction nestedAndRestriction = r.getSubrestrictions().get(1);
		assertEquals(RestrictionOperator.AND, nestedAndRestriction.getOperator());
		
		Restriction nestedNameRestriction = nestedAndRestriction.getSubrestrictions().get(0);
		assertEquals(RestrictionOperator.EQ, nestedNameRestriction.getOperator());
		assertEquals("name", nestedNameRestriction.getProperty());
		assertEquals("abc", nestedNameRestriction.getValue());
		
		Restriction nestedAgeRestriction = nestedAndRestriction.getSubrestrictions().get(1);
		assertEquals(RestrictionOperator.LT, nestedAgeRestriction.getOperator());
		assertEquals("age", nestedAgeRestriction.getProperty());
		assertEquals("1", nestedAgeRestriction.getValue());
	}
}
