/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.koll;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class InjectSystemContextAspect
{	
	//@Before("@within(kommet.koll.annotations.SystemContextVar)")
	/*@Before("execution(* kommet.envs..*.*(..))")
	public void  aroundOne(JoinPoint point) throws Throwable
	{
		MethodSignature signature = (MethodSignature)point.getSignature();
	    Method method = signature.getMethod();
	    method.getDeclaringClass();
	    
	    point.getThis();
	}
	
	@Pointcut("execution(* *(..))")
	public void aroundThree() throws Throwable
	{
		int i = 0;
		int k = i;
	}
	
	@Before("within(kommet.envs.*)")
	public void aroundTwo() throws Throwable
	{
		int i = 0;
		int k = i;
	}*/
}