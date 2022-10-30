/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.LABEL_ASSIGNMENT_API_NAME)
public class LabelAssignment extends StandardTypeRecordProxy
{
	private KID recordId;
	private Label label;
	
	public LabelAssignment() throws KommetException
	{
		this(null, null);
	}
	
	public LabelAssignment(Record r, EnvData env) throws KommetException
	{
		super(r, true, env);
	}

	@Property(field = "recordId")
	public KID getRecordId()
	{
		return recordId;
	}

	public void setRecordId(KID recordId)
	{
		this.recordId = recordId;
		setInitialized();
	}

	@Property(field = "label")
	public Label getLabel()
	{
		return label;
	}

	public void setLabel(Label label)
	{
		this.label = label;
		setInitialized();
	}
}