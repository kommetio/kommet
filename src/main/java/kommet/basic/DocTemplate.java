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

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.DOC_TEMPLATE_API_NAME)
public class DocTemplate extends StandardTypeRecordProxy
{	
	private String name;
	private String content;
	
	public DocTemplate() throws RecordProxyException
	{
		super(null, true, null);
	}
	
	public DocTemplate(Record template, EnvData env) throws KommetException
	{
		super(template, true, env);
	}

	public void setName(String name)
	{
		this.name = name;
		setInitialized();
	}

	@Property(field = "name")
	public String getName()
	{
		return name;
	}

	public void setContent(String content)
	{
		this.content = content;
		setInitialized();
	}

	@Property(field = "content")
	public String getContent()
	{
		return content;
	}

}