/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import java.util.ArrayList;
import java.util.List;

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

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.TYPE_INFO_API_NAME)
public class TypeInfo extends StandardTypeRecordProxy
{
	private KID typeId;
	private Action defaultDetailsAction;
	private Action defaultEditAction;
	private Action defaultListAction;
	private Action defaultCreateAction;
	private Action defaultSaveAction;
	private Class standardController;
	
	public TypeInfo() throws RecordProxyException
	{
		super(null, true, null);
	}

	public TypeInfo(Record record, EnvData env) throws KommetException
	{
		super(record, true, env);
		
		for (Action defaultAction : getDefaultActions())
		{
			if (defaultAction == null)
			{
				continue;
			}
			
			if (defaultAction.getTypeId() != null)
			{
				defaultAction.setType(env.getType(defaultAction.getTypeId()));
			}
			
			if (defaultAction.getView() != null && defaultAction.getView().getTypeId() != null)
			{
				defaultAction.getView().setType(env.getType(defaultAction.getView().getTypeId()));
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

	public void setDefaultDetailsAction(Action defaultDetailsAction)
	{
		this.defaultDetailsAction = defaultDetailsAction;
		setInitialized();
	}

	@Property(field = "defaultDetailsAction")
	public Action getDefaultDetailsAction()
	{
		return defaultDetailsAction;
	}

	public void setDefaultEditAction(Action defaultEditAction)
	{
		this.defaultEditAction = defaultEditAction;
		setInitialized();
	}

	@Property(field = "defaultEditAction")
	public Action getDefaultEditAction()
	{
		return defaultEditAction;
	}

	public void setDefaultListAction(Action defaultListAction)
	{
		this.defaultListAction = defaultListAction;
		setInitialized();
	}

	@Property(field = "defaultListAction")
	public Action getDefaultListAction()
	{
		return defaultListAction;
	}

	public void setDefaultCreateAction(Action defaultCreateAction)
	{
		this.defaultCreateAction = defaultCreateAction;
		setInitialized();
	}

	@Property(field = "defaultCreateAction")
	public Action getDefaultCreateAction()
	{
		return defaultCreateAction;
	}

	public void setStandardController(Class standardController)
	{
		this.standardController = standardController;
		setInitialized();
	}

	@Property(field = "standardController")
	public Class getStandardController()
	{
		return standardController;
	}

	public void setDefaultSaveAction(Action defaultSaveAction)
	{
		this.defaultSaveAction = defaultSaveAction;
		setInitialized();
	}

	@Property(field = "defaultSaveAction")
	public Action getDefaultSaveAction()
	{
		return defaultSaveAction;
	}
	
	@Transient
	public List<Action> getDefaultActions()
	{
		List<Action> defaultActions = new ArrayList<Action>();
		defaultActions.add(this.defaultCreateAction);
		defaultActions.add(this.defaultEditAction);
		defaultActions.add(this.defaultListAction);
		defaultActions.add(this.defaultDetailsAction);
		defaultActions.add(this.defaultSaveAction);
		return defaultActions;
	}

	@Transient
	public Action getDefaultAction(StandardActionType type) throws KommetException
	{
		switch (type)
		{
			case LIST: return this.defaultListAction;
			case EDIT: return this.defaultEditAction;
			case VIEW: return this.defaultDetailsAction;
			case CREATE: return this.defaultCreateAction;
			default: throw new KommetException("Unsupported standard action type " + type);
		}
	}

}