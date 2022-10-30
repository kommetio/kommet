/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.validationrules.executors;

import java.util.Set;

import kommet.basic.RecordProxy;
import kommet.data.KommetException;
import kommet.data.validationrules.ValidationRuleError;

public interface ValidationRuleExecutor<T extends RecordProxy>
{
	public Set<ValidationRuleError> execute(T obj, String uninitializedFieldsMode) throws KommetException;
}