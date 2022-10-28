/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.APP_URL_API_NAME)
public class AppUrl extends StandardTypeRecordProxy
{	
	private String url;
	private App app;
	
	public AppUrl(Record record, EnvData env) throws RecordProxyException
	{
		super(record, true, env);
	}
	
	public AppUrl() throws RecordProxyException
	{
		super(null, true, null);
	}

	public void setUrl(String url)
	{
		this.url = url;
		setInitialized();
	}

	@Property(field = "url")
	public String getUrl()
	{
		return url;
	}
	
	public void setApp(App app)
	{
		this.app = app;
		setInitialized();
	}

	@Property(field = "app")
	public App getApp()
	{
		return app;
	}
}
