/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.actions.StandardActionType;
import kommet.basic.types.SystemTypes;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.STANDARD_ACTION_API_NAME)
public class StandardAction extends StandardTypeRecordProxy
{
	private KID typeId;
	private Profile profile;
	private StandardActionType type;
	private Action action;
	
	public StandardAction() throws KommetException
	{
		this(null, null);
	}
	
	public StandardAction (Record action, EnvData env) throws KommetException
	{
		super(action, true, env);
	}

	public StandardAction(Record record, boolean initTypes, EnvData env) throws KommetException
	{
		this(record, env);
		
		if (initTypes)
		{
			if (this.action.getTypeId() != null)
			{
				this.action.setType(env.getType(this.action.getTypeId()));
			}
		
			if (action.getView() != null && action.getView().getTypeId() != null)
			{
				action.getView().setType(env.getType(action.getView().getTypeId()));
			}
		}
	}

	public void setTypeId(KID typeId)
	{
		this.typeId = typeId;
		setInitialized();
	}

	@Property(field = "typeId")
	public KID getTypeId()
	{
		return typeId;
	}

	public void setProfile(Profile profile)
	{
		this.profile = profile;
		setInitialized();
	}

	@Property(field = "profile")
	public Profile getProfile()
	{
		return profile;
	}
	
	public void setType (String type) throws KommetException
	{
		this.type = StandardActionType.fromString(type);
		setInitialized();
	}

	@Property(field = "type")
	public String getType()
	{
		return this.type.getStringValue();
	}

	public void setStandardPageType(StandardActionType type)
	{
		this.type = type;
		setInitialized();
	}

	@Transient
	public StandardActionType getStandardPageType()
	{
		return type;
	}

	public void setAction(Action action)
	{
		this.action = action;
		setInitialized();
	}

	@Property(field = "action")
	public Action getAction()
	{
		return action;
	}

}