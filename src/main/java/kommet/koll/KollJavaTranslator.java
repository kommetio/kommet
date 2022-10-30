/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.koll;

import kommet.auth.AuthData;
import kommet.basic.Class;
import kommet.env.EnvData;

public interface KollJavaTranslator
{
	public Class kollToJava (Class file, boolean isInterpreteKoll, AuthData authData, EnvData env) throws KollParserException;
	public String kollToJava (String code, boolean isInterpreteKoll, AuthData authData, EnvData env) throws KollParserException;
}