/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.data.Field;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Property;

/**
 * A proxy used for standard types in system context, i.e. by built-in features.
 * 
 * When standard types are used in KOLL context (i.e. in users' code), {@link CustomTypeRecordProxy} class is used
 * instead.
 * 
 * @author Radek Krawiec
 * @created 29/01/2014
 *
 */
public abstract class StandardTypeRecordProxy extends RecordProxy
{
	private RecordProxy lastModifiedBy;
	private RecordProxy createdBy;
	
	public StandardTypeRecordProxy() throws RecordProxyException
	{
		super();
	}
	
	public StandardTypeRecordProxy (Record record, boolean useBasicTypes, EnvData env) throws RecordProxyException 
	{
		super(record, RecordProxyType.STANDARD, env);
	}
	
	public final void setLastModifiedBy(RecordProxy lastModifiedBy)
	{
		this.lastModifiedBy = lastModifiedBy;
		setInitialized();
	}

	@Property(field = Field.LAST_MODIFIED_BY_FIELD_NAME)
	public final RecordProxy getLastModifiedBy()
	{
		return lastModifiedBy;
	}

	public final void setCreatedBy(RecordProxy createdBy)
	{
		this.createdBy = createdBy;
		setInitialized();
	}

	@Property(field = Field.CREATEDBY_FIELD_NAME)
	public final RecordProxy getCreatedBy()
	{
		return createdBy;
	}
}