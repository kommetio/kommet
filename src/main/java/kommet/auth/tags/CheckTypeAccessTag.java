/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.auth.tags;

import java.util.List;

import javax.servlet.jsp.JspException;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.keetle.tags.KommetTag;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.utils.MiscUtils;

public class CheckTypeAccessTag extends KommetTag
{
	private static final long serialVersionUID = 8133802588903476040L;
	private String access;
	private String typeId;
	
	public CheckTypeAccessTag() throws KommetException
	{
		super();
	}

	@Override
	public int doStartTag() throws JspException
	{
		if (true)
		{
			throw new JspException("This tag is not yet fully implemented - it requires to be put within a view tag");
		}
		KID id = null;
		try
		{
			id = KID.get(typeId);
		}
		catch (KIDException e)
		{
			return exitWithTagError("Incorrect type ID '" + typeId + "'");
		}
		
		if (!StringUtils.hasText(this.access))
		{	
			return exitWithTagError("Attribute access is required on tag checkTypeAccess");
		}
		
		try
		{
			List<String> accessTypes = MiscUtils.splitAndTrim(this.access, ",");
			AuthData authData = AuthUtil.getAuthData(pageContext.getSession());
			
			for (String accessType : accessTypes)
			{
				if ("read".equals(accessType))
				{
					if (!authData.canReadType(id, false, getEnv()))
					{
						return SKIP_BODY;
					}
				}
				else if ("edit".equals(accessType))
				{
					if (!authData.canEditType(id, false, getEnv()))
					{
						return SKIP_BODY;
					}
				}
				else if ("delete".equals(accessType))
				{
					if (!authData.canDeleteType(id, false, getEnv()))
					{
						return SKIP_BODY;
					}
				}
			}
		}
		catch (KommetException e)
		{
			return exitWithTagError("Error executing checkTypeAccess tag: " + e.getMessage());
		}
		
		return EVAL_BODY_INCLUDE;
	}

	public void setAccess(String access)
	{
		this.access = access;
	}

	public String getAccess()
	{
		return access;
	}
	
	public String getTypeId()
	{
		return typeId;
	}

	public void setTypeId(String typeId)
	{
		this.typeId = typeId;
	}
}
