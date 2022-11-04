/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.kmparams;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import kommet.web.kmparams.actions.Action;
import kommet.web.kmparams.actions.ActionMessage;
import kommet.web.kmparams.actions.ExecuteCode;
import kommet.web.kmparams.actions.KeepParameters;
import kommet.web.kmparams.actions.OverrideLayout;
import kommet.web.kmparams.actions.SetField;
import kommet.web.kmparams.actions.SetParentField;
import kommet.web.kmparams.actions.ShowLookup;

public class KmParamNode
{
	private String name;
	private String value;
	private KmParamNodeType type;
	private Map<String, Event> eventNodes;
	private Map<String, Set<Action>> actionNodes;
	private Integer keep;
	
	public static KmParamNode get (String name) throws KmParamException
	{
		KmParamNodeType type = getNodeType(name);
		switch (type)
		{
			case SET: return new SetField();
			case SET_PARENT_WINDOW_FIELD: return new SetParentField();
			case KEEP: return new KeepParameters();
			case MESSAGE: return new ActionMessage();
			case JAVASCRIPT: return new ExecuteCode();
			case LAYOUT: return new OverrideLayout();
			case LOOKUP: return new ShowLookup();
			case CLOSE_WINDOW: return new Action(name);
			case CLOSE_DIALOG: return new Action(name);
			case AFTER_INSERT: return new Action(name);
			case AFTER_UPDATE: return new Action(name);
			case SELECT_LIST_ITEM: return new Event(name);
			case SAVE: return new Event(name);
			case CANCEL: return new Event(name);
			default: throw new KmParamException("Cannot instantiate RM parameter node for type " + type); 
		}
	}
	
	public KmParamNode (String name) throws KmParamException
	{
		this.name = name;
		this.type = getNodeType(name);
	}
	
	private static KmParamNodeType getNodeType(String name) throws KmParamException
	{
		if ("km".equals(name))
		{
			return KmParamNodeType.BASE;
		}
		else if ("set".equals(name))
		{
			return KmParamNodeType.SET;
		}
		else if ("setpwf".equals(name))
		{
			return KmParamNodeType.SET_PARENT_WINDOW_FIELD;
		}
		else if ("save".equals(name))
		{
			return KmParamNodeType.SAVE;
		}
		else if ("listselect".equals(name))
		{
			return KmParamNodeType.SELECT_LIST_ITEM;
		}
		else if ("success".equals(name))
		{
			return KmParamNodeType.RESULT;
		}
		else if ("fail".equals(name))
		{
			return KmParamNodeType.RESULT;
		}
		else if ("cancel".equals(name))
		{
			return KmParamNodeType.CANCEL;
		}
		else if ("keep".equals(name))
		{
			return KmParamNodeType.KEEP;
		}
		else if ("msg".equals(name))
		{
			return KmParamNodeType.MESSAGE;
		}
		else if ("listselect".equals(name))
		{
			return KmParamNodeType.SELECT_LIST_ITEM;
		}
		else if ("layout".equals(name))
		{
			return KmParamNodeType.LAYOUT;
		}
		else if ("close".equals(name))
		{
			return KmParamNodeType.CLOSE_WINDOW;
		}
		else if ("closedialog".equals(name))
		{
			return KmParamNodeType.CLOSE_DIALOG;
		}
		else if ("js".equals(name))
		{
			return KmParamNodeType.JAVASCRIPT;
		}
		else if ("lookup".equals(name))
		{
			return KmParamNodeType.LOOKUP;
		}
		else if ("afterinsert".equals(name))
		{
			return KmParamNodeType.AFTER_INSERT;
		}
		else if ("afterupdate".equals(name))
		{
			return KmParamNodeType.AFTER_UPDATE;
		}
		else
		{
			throw new KmParamException("Unsupported node name " + name);
		}
		
	}

	public void addEventNode(Event node)
	{
		if (this.eventNodes == null)
		{
			this.eventNodes = new HashMap<String, Event>();
		}
		this.eventNodes.put(node.getName(), node);
	}
	
	public void addActionNode(Action node)
	{
		if (this.actionNodes == null)
		{
			this.actionNodes = new HashMap<String, Set<Action>>();
		}
		
		Set<Action> actionNodesForType = this.actionNodes.get(node.getName());
		if (actionNodesForType == null)
		{
			actionNodesForType = new HashSet<Action>();
		}
		actionNodesForType.add(node);
		
		this.actionNodes.put(node.getName(), actionNodesForType);
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}

	public Map<String, Event> getEventNodes()
	{
		return eventNodes;
	}

	public Event getEventNode(String name)
	{
		return this.eventNodes != null ? this.eventNodes.get(name) : null;
	}
	
	public Set<Action> getActionNodes(String name)
	{
		return this.actionNodes != null ? this.actionNodes.get(name) : null;
	}
	
	public Action getSingleActionNode(String name) throws KmParamException
	{
		Set<Action> nodes = getActionNodes(name);
		if (nodes != null)
		{
			if (nodes.size() == 1)
			{
				return nodes.iterator().next();
			}
			else
			{
				throw new KmParamException("Tried to get action node " + name + " as single node, but found " + nodes.size() + " such nodes");
			}
		}
		else
		{
			return null;
		}
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public String getValue()
	{
		return value;
	}

	public KmParamNodeType getType()
	{
		return type;
	}

	public void setKeep(Integer keep)
	{
		this.keep = keep;
	}

	public Integer getKeep()
	{
		return keep;
	}

	public Map<String, Set<Action>> getActionNodes()
	{
		return actionNodes;
	}

	public void addNode (KmParamNode node) throws KmParamException
	{
		if (node instanceof Action)
		{
			this.addActionNode((Action)node);
		}
		else if (node instanceof Event)
		{
			this.addEventNode((Event)node);
		}
		else
		{
			throw new KmParamException("Unsupported node type " + node.getClass().getName());
		}
	}
}