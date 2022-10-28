/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.security;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpSession;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;

/**
 * Aspect that makes sure methods annotated with {@link RestrictedAccess} will only be called by users
 * with appropriate privileges.
 * @author Radek Krawiec
 * @since 24/01/2015
 */
@Aspect
@Component
public class RestrictedAccessAspect
{	
	@Around("execution(* *(..)) && @annotation(kommet.security.RestrictedAccess)")
	public Object around(ProceedingJoinPoint point) throws Throwable
	{	
		MethodSignature signature = (MethodSignature)point.getSignature();
	    Method method = signature.getMethod();

	    RestrictedAccess annotation = method.getAnnotation(RestrictedAccess.class);
	    
	    // get profiles for which access is allowed
	    String[] profileNames = annotation.profiles() != null ? annotation.profiles() : new String[0];
	    Set<String> profileSet = new HashSet<String>();
	    profileSet.addAll(Arrays.asList(profileNames));
	    
	    RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
		
		// get current auth data
		AuthData authData = AuthUtil.getAuthData((HttpSession)requestAttributes.resolveReference(RequestAttributes.REFERENCE_SESSION));
		if (!profileSet.contains(authData.getProfile().getName()))
		{
			throw new RestrictedAccessException("Access restricted for profile " + authData.getProfile().getName());
		}
		
		Object result = point.proceed();
		return result;
	}
}