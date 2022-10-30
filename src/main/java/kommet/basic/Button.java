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

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.BUTTON_API_NAME)
public class Button extends StandardTypeRecordProxy
{
	private String label;
	private String name;
	private String labelKey;
	private KID typeId;
	private String url;
	private String onClick;
	private Action action;
	private String displayCondition;

	public Button() throws KommetException
	{
		this(null, null);
	}

	public Button(Record task, EnvData env) throws KommetException
	{
		super(task, true, env);
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

	@Property(field = "name")
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
		setInitialized();
	}

	@Property(field = "labelKey")
	public String getLabelKey()
	{
		return labelKey;
	}

	public void setLabelKey(String labekKey)
	{
		this.labelKey = labekKey;
		setInitialized();
	}

	@Property(field = "typeId")
	public KID getTypeId()
	{
		return typeId;
	}

	public void setTypeId(KID typeId)
	{
		this.typeId = typeId;
		setInitialized();
	}

	@Property(field = "url")
	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
		setInitialized();
	}

	@Property(field = "onClick")
	public String getOnClick()
	{
		return onClick;
	}

	public void setOnClick(String onClick)
	{
		this.onClick = onClick;
		setInitialized();
	}

	@Property(field = "action")
	public Action getAction()
	{
		return action;
	}

	public void setAction(Action action)
	{
		this.action = action;
		setInitialized();
	}

	@Property(field = "displayCondition")
	public String getDisplayCondition()
	{
		return displayCondition;
	}

	public void setDisplayCondition(String displayCondition)
	{
		this.displayCondition = displayCondition;
		setInitialized();
	}
}