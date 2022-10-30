/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.kmparams;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;

import kommet.basic.keetle.PageData;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.utils.MiscUtils;
import kommet.web.kmparams.actions.Action;
import kommet.web.kmparams.actions.ActionMessage;
import kommet.web.kmparams.actions.ExecuteCode;
import kommet.web.kmparams.actions.KeepParameters;
import kommet.web.kmparams.actions.OverrideLayout;
import kommet.web.kmparams.actions.SetField;
import kommet.web.kmparams.actions.SetParentField;
import kommet.web.kmparams.actions.ShowLookup;


public class KmParamUtils
{
	public static KmParamNode parseParam (KmParamNode baseNode, String paramName, String paramValue) throws KmParamException
	{	
		if (baseNode == null)
		{
			throw new KmParamException("KM param node to parse value into is null");
		}
		
		if (!baseNode.getName().equals("km"))
		{
			throw new KmParamException("Base node passed to parseParam method must have name 'rm'");
		}
		
		if (!StringUtils.hasText(paramName))
		{
			throw new KmParamException("KM parameter name is empty"); 
		}

		if (!paramName.startsWith("km."))
		{
			throw new KmParamException("RM parameter name must start with 'km' prefix");
		}
		
		String[] parts = paramName.split("\\.");

		setKmParam(baseNode, parts, paramValue, 1);
		return baseNode;
	}
	
	private static KmParamNode setKmParam (KmParamNode node, String[] splitParamName, String paramValue, int i) throws KmParamException
	{
		// check if the current node is an action node, if yes, parse it manually
		if (node.getType().equals(KmParamNodeType.SET) || node.getType().equals(KmParamNodeType.SET_PARENT_WINDOW_FIELD))
		{
			// implode the rest of the parameters after "set", e.g. for "km.set.one.two=three", treat
			// "one.two" as variable name
			
			List<String> varNameParts = new ArrayList<String>();
			for (int k = i; k < splitParamName.length; k++)
			{
				varNameParts.add(splitParamName[k]);
			}
			
			if (node instanceof SetField)
			{
				((SetField)node).setField(MiscUtils.implode(varNameParts, "."));
			}
			else
			{
				((SetParentField)node).setField(MiscUtils.implode(varNameParts, "."));
			}
			node.setValue(paramValue);
			return node;
		}
		else if (node.getType().equals(KmParamNodeType.KEEP))
		{
			if (i < splitParamName.length)
			{
				throw new KmParamException("Error in syntax of keep action " + MiscUtils.implode(splitParamName, "."));
			}
			
			((KeepParameters)node).setKeepFor(Integer.valueOf(paramValue));
			return node;
		}
		else if (node.getType().equals(KmParamNodeType.MESSAGE))
		{
			if (i < splitParamName.length)
			{
				throw new KmParamException("Error in syntax of message action " + MiscUtils.implode(splitParamName, "."));
			}
			
			((ActionMessage)node).setMessage(paramValue);
			return node;
		}
		else if (node.getType().equals(KmParamNodeType.LOOKUP))
		{
			if (i < splitParamName.length)
			{
				throw new KmParamException("Error in syntax of lookup action " + MiscUtils.implode(splitParamName, "."));
			}
			
			((ShowLookup)node).setId(paramValue);
			return node;
		}
		else if (node.getType().equals(KmParamNodeType.JAVASCRIPT))
		{
			if (i < splitParamName.length)
			{
				throw new KmParamException("Error in syntax of Javascript action " + MiscUtils.implode(splitParamName, "."));
			}
			
			((ExecuteCode)node).setCode(paramValue);
			return node;
		}
		else if (node.getType().equals(KmParamNodeType.CLOSE_WINDOW) || node.getType().equals(KmParamNodeType.CLOSE_DIALOG)
				|| node.getType().equals(KmParamNodeType.AFTER_INSERT) || node.getType().equals(KmParamNodeType.AFTER_UPDATE))
		{
			if (i < splitParamName.length)
			{
				throw new KmParamException("Error in syntax of action " + node.getType() + " action " + MiscUtils.implode(splitParamName, "."));
			}
			
			((Action)node).setName(splitParamName[i-1]);
			return node;
		}
		else if (node.getType().equals(KmParamNodeType.LAYOUT))
		{
			if (i < splitParamName.length)
			{
				// if the next value after "layout" is not empty, it must be a "keep" parameter
				if ("keep".equals(splitParamName[i]))
				{
					((OverrideLayout)node).setKeep(Integer.parseInt(paramValue));
					return node;
				}
				else
				{
					throw new KmParamException("Error in syntax of layout action " + MiscUtils.implode(splitParamName, "."));
				}
			}
			
			try
			{
				((OverrideLayout)node).setLayoutId(KID.get(paramValue));
			}
			catch (KIDException e)
			{
				throw new KmParamException("Invalid layout ID value '" + paramValue + "' in km parameter " + MiscUtils.implode(splitParamName, "."));
			}
			return node;
		}
		
		// no action node has been discovered, so we make sure that this is not the last node
		if (i >= splitParamName.length)
		{
			throw new KmParamException("No action node at the end of the km parameter " + MiscUtils.implode(splitParamName, ".") + "(i = " + i + ")");
		}
		
		// remember previous node
		KmParamNode prevNode = node;
		
		// get new node from the existing event nodes
		node = node.getEventNode(splitParamName[i]);
		
		if (node == null)
		{
			node = KmParamNode.get(splitParamName[i]);
			prevNode.addNode(node);
		}
			
		return setKmParam(node, splitParamName, paramValue, i + 1);
	}
	
	public static KmParamNode getKmParamsFromRequest (HttpServletRequest req) throws KmParamException
	{
		KmParamNode node = new KmParamNode("km");
		
		// initialize all rm parameters
		for (Object param : req.getParameterMap().keySet())
		{
			if (param instanceof String && ((String)param).startsWith("km."))
			{
				KmParamUtils.parseParam(node, (String)param, ((String[])req.getParameterMap().get(param))[0]);
			}
		}
		
		return node;
	}
	
	public static PageData initRmParams(HttpServletRequest req, PageData pageData) throws KmParamException
	{
		pageData.setRmParams(getKmParamsFromRequest(req));
		
		// init overridden layout
		// TODO note that layout will be reserved for this use as a parameter
		//pageData.setLayout(req.getParameter("layout"));
		
		return pageData;
	}
}