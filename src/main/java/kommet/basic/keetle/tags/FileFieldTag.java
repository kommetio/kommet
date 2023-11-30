/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;

import org.springframework.util.StringUtils;

import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.utils.UrlUtil;

public class FileFieldTag extends KommetTag
{
	private static final long serialVersionUID = -5210468529934192791L;
	private String fileId;
	private String fileName;

	public FileFieldTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		ViewTag parentView = null;
		try
		{
			parentView = getParentView();
		}
		catch (KommetException e1)
		{
			return exitWithTagError("Error getting parent view: " + e1.getMessage());
		}
		
		KID fileKID = null;
		
		if (StringUtils.hasText(fileId))
		{
			try
			{
				fileKID = KID.get(fileId);
			}
			catch (KIDException e)
			{
				parentView.addErrorMsgs("Invalid file ID " + fileId);
				return EVAL_PAGE;
			}
		}
		
		try
		{
			this.pageContext.getOut().write(getCode(fileKID, fileName, parentView.getEnv(), parentView.nextComponentId(), getHost()));
		}
		catch (IOException e)
		{
			parentView.addErrorMsgs("Cannot render user link tag: " + e.getMessage());
		}
		catch (KommetException e)
		{
			parentView.addErrorMsgs("Cannot render user link tag: " + e.getMessage());
		}
		return EVAL_PAGE;
    }

	public static String getCode(KID fileId, String fileName, EnvData env, String formId, String contextPath) throws KommetException
	{
		StringBuilder code = new StringBuilder();
		
		code.append("<a href=\"").append(contextPath).append("/").append(UrlUtil.SYSTEM_ACTION_URL_PREFIX).append("/download/").append(fileId).append("\">");
		code.append(fileName).append("</a>");
		
		return code.toString();
	}

	public void setFileId(String fileId)
	{
		this.fileId = fileId;
	}

	public String getFileId()
	{
		return fileId;
	}

	public void setFileName(String fileName)
	{
		this.fileName = fileName;
	}

	public String getFileName()
	{
		return fileName;
	}
}
