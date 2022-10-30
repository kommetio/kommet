/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.mock;

import java.util.HashSet;
import java.util.Set;

import kommet.services.SystemActionService;

/**
 * System action service mock used in unit tests.
 * @author Radek Krawiec
 * @since 10/12/2014
 */
public class MockSystemActionService implements SystemActionService
{
	public Set<String> getSystemActionURLs()
	{
		return new HashSet<String>();
	}
}
