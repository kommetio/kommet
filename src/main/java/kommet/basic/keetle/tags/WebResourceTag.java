/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import java.util.List;

import javax.servlet.jsp.JspException;

import kommet.basic.WebResource;
import kommet.data.KommetException;
import kommet.filters.WebResourceFilter;

public class WebResourceTag extends KommetTag
{
	private static final long serialVersionUID = -4703784511748152402L;
	private String name;

	public WebResourceTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {	
		ViewWrapperTag viewWrapper;
		try
		{
			viewWrapper = getViewWrapper();
		}
		catch (MisplacedTagException e1)
		{
			return exitWithTagError("Tag km:resource must be placed within a view wrapper tag");
		}
		
		// find resource by name
		WebResourceFilter filter = new WebResourceFilter();
		filter.setName(this.name);
		List<WebResource> resources = null;
		try
		{
			resources = viewWrapper.getWebResourceService().find(filter, viewWrapper.getEnv());
		}
		catch (KommetException e)
		{
			return exitWithTagError("Error getting resources by name. Nested: " + e.getMessage());
		}
		
		if (resources.isEmpty())
		{
			return exitWithTagError("Web resource with name '" + this.name + "' not found");
		}
		
		WebResource resource = resources.get(0);
		
		if (resource.getMimeType().equals("text/css"))
		{
			writeToPage("<link href=\"" + this.pageContext.getServletContext().getContextPath() + "/downloadresource?name=" + resource.getFile().getId() + "\" rel=\"stylesheet\" type=\"text/css\" />");
		}
		else if (resource.getMimeType().equals("application/javascript"))
		{
			writeToPage("<script type=\"text/javascript\" src=\"" + this.pageContext.getServletContext().getContextPath() + "/downloadresource?name=" + resource.getFile().getId() + "\" />");
		}
		else if (resource.getMimeType().equals("image/jpeg") || resource.getMimeType().equals("image/jpg") || resource.getMimeType().equals("image/png"))
		{
			writeToPage("<img src=\"" + this.pageContext.getServletContext().getContextPath() + "/downloadresource?name=" + resource.getFile().getId() + "\" />");
		}
		else
		{
			cleanUp();
			return exitWithTagError("Unsupported resource MIME type " + resource.getMimeType());
		}
		
		cleanUp();
		return EVAL_PAGE;
    }
	
	@Override
    protected void cleanUp()
	{
		super.cleanUp();
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getName()
	{
		return name;
	}
}
