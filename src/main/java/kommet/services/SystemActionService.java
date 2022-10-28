/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.services;

import java.util.Set;

/**
 * Services implementing this interface will provide a list of URLs registered with the environment.
 * 
 * The reason why we will have two services implementing it is that one of them will be used during normal
 * system operation and it will make use of injected RequestMappingHandlerMapping. The other one will be a
 * mock service used in tests. We need to have such separation because RequestMappingHandlerMapping is not
 * available in tests and cannot be injected there.
 * 
 * @author Radek Krawiec
 * @since 10/12/2014
 */
public interface SystemActionService
{
	public Set<String> getSystemActionURLs();
}