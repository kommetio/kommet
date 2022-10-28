/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.koll.compiler;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;

import javax.inject.Inject;

import org.junit.Test;

import kommet.basic.BasicSetupService;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.koll.annotations.Controller;
import kommet.koll.compiler.KommetCompiler;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;

public class KommetCompilerTest extends BaseUnitTest
{
	@Inject
	KommetCompiler compiler;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	TestDataCreator dataHelper;
	
	@Test
	public void testDifferentInstance() throws KommetException, MalformedURLException, ClassNotFoundException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		
		Class<?> classFromEnvClassPath = compiler.getClass(Controller.class.getName(), false, env);
		
		// make sure instances of class Controller are different when obtained using the 
		// current compiler and the env compiler
		// Note: the "==" comparison operator is intended - we are actually comparing instances
		assertTrue(classFromEnvClassPath != Compiler.class); 
		
		// make sure the same class obtained twice from the same env class loader is the same instance
		assertTrue (compiler.getClass(Controller.class.getName(), false, env) == classFromEnvClassPath);
	}
}
