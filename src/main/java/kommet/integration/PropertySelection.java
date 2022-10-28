/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.integration;

public enum PropertySelection
{
	// all simple (i.e. non-nested properties) will be included in the select clause
	ALL_SIMPLE_PROPERTIES,
	// only specified properties will be selected 
	SPECIFIED,
	// only specified properties plus the 5 basic ones: id, createdDate, createdBy, lastModifiedDate, lastModifiedBy
	SPECIFIED_AND_BASIC;
}