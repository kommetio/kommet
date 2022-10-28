/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic;

import java.util.ArrayList;

import kommet.basic.types.SystemTypes;
import kommet.data.ComponentType;
import kommet.data.Record;
import kommet.deployment.Deployable;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.APP_API_NAME)
public class App extends StandardTypeRecordProxy implements Deployable
{	
	private String name;
	private String type;
	private String landingUrl;
	private String label;
	private ArrayList<AppUrl> urls;
	
	public App() throws RecordProxyException
	{
		super(null, true, null);
	}
	
	public App(Record app, EnvData env) throws RecordProxyException
	{
		super(app, true, env);
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
	
	public void setType(String type)
	{
		this.type = type;
		setInitialized();
	}

	@Property(field = "type")
	public String getType()
	{
		return type;
	}

	@Property(field = "landingUrl")
	public String getLandingUrl()
	{
		return landingUrl;
	}

	public void setLandingUrl(String landingUrl)
	{
		this.landingUrl = landingUrl;
		setInitialized();
	}
	
	@Transient
	public ComponentType getComponentType()
	{
		return ComponentType.APP;
	}

	@Property(field = "label")
	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
		setInitialized();
	}

	@Property(field = "urls")
	public ArrayList<AppUrl> getUrls()
	{
		return urls;
	}

	public void setUrls(ArrayList<AppUrl> urls)
	{
		this.urls = urls;
		setInitialized();
	}
}
