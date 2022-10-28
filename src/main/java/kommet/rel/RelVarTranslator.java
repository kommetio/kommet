/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.rel;

import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;

public interface RelVarTranslator
{
	public TranslatedVar translateVar(String variable, String recordVar, boolean isCheckFields, Type type, EnvData env) throws KommetException;
}