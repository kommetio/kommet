/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import org.springframework.util.StringUtils;

import kommet.basic.keetle.LayoutService;
import kommet.basic.keetle.ViewUtil;
import kommet.basic.types.SystemTypes;
import kommet.data.ComponentType;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.deployment.Deployable;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.LAYOUT_API_NAME)
public class Layout extends StandardTypeRecordProxy implements Deployable
{
	private String name;
	private String code;
	private String beforeContent;
	private String afterContent;

	public Layout() throws KommetException
	{
		this(null, null);
	}
	
	public Layout(Record view, EnvData env) throws KommetException
	{
		super(view, true, env);
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

	@Transient
	public String getBeforeContent()
	{
		return beforeContent;
	}

	@Transient
	public String getAfterContent()
	{
		return afterContent;
	}

	public void setCode(String code) throws KommetException
	{
		this.code = code;
		
		if (StringUtils.hasText(code))
		{
			String[] contentParts = LayoutService.getPreAndPostContent(ViewUtil.wrapLayout(this.code));
			this.beforeContent = contentParts[0];
			this.afterContent = contentParts[1];
		}
		else
		{
			this.beforeContent = null;
			this.afterContent = null;
		}
		
		setInitialized();
	}

	@Property(field = "code")
	public String getCode()
	{
		return code;
	}
	
	@Transient
	public ComponentType getComponentType()
	{
		return ComponentType.LAYOUT;
	}
}