/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.LABEL_API_NAME)
public class Label extends StandardTypeRecordProxy
{
	private String text;
	
	public Label() throws KommetException
	{
		this(null, null);
	}
	
	public Label(Record r, EnvData env) throws KommetException
	{
		super(r, true, env);
	}

	@Property(field = "text")
	public String getText()
	{
		return text;
	}

	public void setText(String text)
	{
		this.text = text;
		setInitialized();
	}
}