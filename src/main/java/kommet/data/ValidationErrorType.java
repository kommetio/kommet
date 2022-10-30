/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

public enum ValidationErrorType
{
	FIELD_REQUIRED,
	FIELD_INVALID_VALUE,
	STRING_VALUE_TOO_LONG,
	INVALID_EMAIL,
	INVALID_ENUM_VALUE,
	VALIDATION_RULE_VIOLATION,
	GENERAL;
}