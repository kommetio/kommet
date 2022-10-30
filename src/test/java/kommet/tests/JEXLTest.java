/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.JexlScript;
import org.apache.commons.jexl3.MapContext;
import org.junit.Test;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class JEXLTest
{
	@Test
	public void testPureExpressions()
	{
		JexlEngine jexl = new JexlBuilder().cache(512).strict(true).silent(false).create();
		
		JexlContext jc = new MapContext();
		
		TrendProxy trend = new TrendProxy(new Trend());
		trend.setFactor(2);
		jc.set("trend", trend);
		
		JexlScript condition = jexl.createScript("trend.getCount(10) == 12");
		
		Set<String> classList = new HashSet<String>();
		classList.add(TrendProxy.class.getName());
		Object result = condition.execute(jc);
		assertEquals((boolean)result, true);
	}
	
	@Test
	public void testExpressions()
	{
		kommet.vendorapis.jexl.JexlScript script = new kommet.vendorapis.jexl.JexlScript("trend.getCount(10) == 12");
		TrendProxy trend = new TrendProxy(new Trend());
		trend.setFactor(2);
		script.set("trend", trend);
		
		Set<String> classList = new HashSet<String>();
		classList.add(TrendProxy.class.getName());
		Object result = script.execute();
		assertEquals((boolean)result, true);
	}
	
	public class TrendProxy extends Trend implements MethodInterceptor
	{
		public TrendProxy (Trend t)
		{
			
		}
		
		@Override
	    public Object intercept(Object proxy, Method method, Object[] args, MethodProxy mProxy) throws Throwable
		{
			String name = method.getName();
	        return name;
	    }
	}
	
	public class Trend
	{
		private int factor;
		
		public void setFactor(int f)
		{
			this.factor = f;
		}
		
		public int getCount (int c)
		{
			return c + this.factor;
		}
	}
}
