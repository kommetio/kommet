/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

/**
 * Defines a type of proxy generated for a record. Standard proxies are those created at compile time for
 * standard types such as User, Comment, Class. Custom proxies can be generated for both standard and custom types.
 * @author Radek Krawiec
 * @created 29-01-2014
 */
public enum RecordProxyType
{
	STANDARD,
	CUSTOM
}