/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.services;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Service providing a list of system (not user defined) URLs currently registered with the environment.
 * @author Radek Krawiec
 * @since 10/12/2014
 */
public class WebSystemActionService implements SystemActionService
{
	@Inject
	RequestMappingHandlerMapping handlerMapping;
	
	public Set<String> getSystemActionURLs()
	{
		Map<RequestMappingInfo, HandlerMethod> registeredActions = this.handlerMapping.getHandlerMethods();
		Set<String> allURLs = new HashSet<String>();
		
		for (RequestMappingInfo rm : registeredActions.keySet())
		{
			allURLs.addAll(rm.getPatternsCondition().getPatterns());
		}
		
		return allURLs;
	}
}