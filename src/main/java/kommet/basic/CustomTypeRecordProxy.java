/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;

/**
 * Represents an record proxy of a custom type.
 * 
 * This class is used to represent proxies of both standard and custom types in a KOLL context. When used in
 * system context (i.e. by built-in features), {@link StandardTypeRecordProxy} is used for standard types instead.
 * 
 * @author Radek Krawiec
 * @created 29-01-2014
 */
public abstract class CustomTypeRecordProxy extends RecordProxy
{
	public CustomTypeRecordProxy() throws RecordProxyException
	{
		super();
	}
	
	public CustomTypeRecordProxy (Record record, boolean useBasicTypes, EnvData env) throws KommetException 
	{
		super(record, RecordProxyType.CUSTOM, env);
	}
}