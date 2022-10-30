/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.jsp.JspException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.auth.UserService;
import kommet.basic.Layout;
import kommet.basic.keetle.LayoutService;
import kommet.comments.CommentService;
import kommet.data.KommetException;
import kommet.data.sharing.SharingService;
import kommet.files.FileService;
import kommet.i18n.InternationalizationService;
import kommet.services.FieldHistoryService;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;
import kommet.web.tags.ActionErrors;

public class ViewTag extends KommetTag
{
	private static final long serialVersionUID = -7271157789664501658L;
	
	// prefix added to generated IDs of components, short for "Kommet item"
	private static final String COMPONENT_ID_PREFIX = "ki_";
	
	// title of the page
	private String title;
	
	private String layout;
	protected Layout usedLayout;
	
	private ViewWrapperTag viewWrapper;
	
	// The ordinal number of the current component on page
	private Integer componentOrdinal;
	
	// Javascript code to be appended to the end of the page
	private List<String> appendedScripts;
	
	/**
	 * Qualified type name
	 */
	private String object;
	
	public ViewTag() throws KommetException
	{
		super();
		this.componentOrdinal = 0;
	}
	
	@Override
    public int doStartTag() throws JspException
    {	
		try
		{
			this.viewWrapper = getViewWrapper();
		}
		catch (MisplacedTagException e1)
		{
			return exitWithTagError("Tag view is not wrapper in tag view wrapper");
		}
		
		if (StringUtils.hasText(this.layout))
		{
			try
			{
				// get layout object by layout name
				this.usedLayout = getViewWrapper().getLayoutService().getByName(this.layout, getViewWrapper().getEnv());
			}
			catch (KommetException e)
			{
				return exitWithTagError("Error getting layout " + this.layout + ": " + e.getMessage());
			}
		}
			
		StringBuilder code = new StringBuilder();
		
		code.append("<!DOCTYPE html>\n");
		code.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
		code.append("<head>\n");
		code.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />");
		
		// include stylesheet
		String contextPath = this.pageContext.getServletContext().getContextPath();
		code.append("<link href=\"").append(contextPath).append("/resources/layout.css\" rel=\"stylesheet\" type=\"text/css\" />\n");
		code.append("<link href=\"").append(contextPath).append("/resources/header.css\" rel=\"stylesheet\" type=\"text/css\" />\n");
		code.append("<link href=\"").append(contextPath).append("/resources/tag-styles.css\" rel=\"stylesheet\" type=\"text/css\" />\n");
		code.append("<link rel=\"stylesheet\" href=\"").append(contextPath).append("/resources/km/css/km.all.min.css\" />");
		code.append("<link href=\"").append(contextPath).append("/resources/themes/std/styles.css\" rel=\"stylesheet\" type=\"text/css\" />\n");
		code.append("<script type=\"text/javascript\" src=\"").append(contextPath).append("/resources/js/jquery-1.9.1.js\"></script>");
		code.append("<script type=\"text/javascript\" src=\"").append(contextPath).append("/resources/js/jquery-ui-1.10.3.custom.min.js\"></script>");
		
		// include user custom CSS styles
		code.append("<link href=\"").append(contextPath).append(UrlUtil.USER_CSS_STYLES_URL).append("\" rel=\"stylesheet\" type=\"text/css\" />\n");
		
		// common.js library
		code.append("<script type=\"text/javascript\" src=\"").append(contextPath).append("/resources/js/common.js\"></script>");
		
		code.append("<link rel=\"stylesheet\" href=\"").append(contextPath).append("/resources/css/smoothness/jquery-ui-1.10.3.custom.min.css\" />");
		
		// import km.js.* libs - we wanted to make this import conditional basing on what tags are rendered
		// on the page, but the header has already been rendered when we get to processing tags
		code.append("<script type=\"text/javascript\" src=\"").append(contextPath).append("/resources/km/js/km.core.js\"></script>");
		code.append("<script type=\"text/javascript\" src=\"").append(contextPath).append("/js/km.config.js\"></script>");
		code.append("<script type=\"text/javascript\" src=\"").append(contextPath).append("/resources/km/js/km.all.min.js\"></script>");
		
		// TODO remove these two references
		code.append("<script type=\"text/javascript\" src=\"").append(contextPath).append("/resources/km/js/km.scope.js\"></script>");
		
		if (StringUtils.hasText(this.title))
		{
			code.append("<title>").append(this.title).append("</title>");
		}
		
		code.append("</head>\n");
		code.append("<body>\n");
		writeToPage(code.toString());
		
		return EVAL_BODY_INCLUDE;
    }
	
	public ViewWrapperTag getViewWrapper() throws MisplacedTagException
	{
		if (this.viewWrapper == null)
		{
			this.viewWrapper = (ViewWrapperTag)findAncestorWithClass(this, ViewWrapperTag.class);
			if (this.viewWrapper == null)
			{
				throw new MisplacedTagException("Tag " + this.getClass().getSimpleName() + " is not placed within a view wrapper tag");
			}
		}
		return this.viewWrapper;
	}
	
	@Override
    public int doEndTag() throws JspException
    {	
		StringBuilder code = new StringBuilder();
		
		// if there are any errors rendering the page, display them
		if (!getErrorMsgs().isEmpty())
		{
			code.append(ActionErrors.getCode(getErrorMsgs(), null, this.pageContext));
		}
		
		code.append("</body>\n");
		
		// append javascript code if defined
		if (this.appendedScripts != null && !this.appendedScripts.isEmpty())
		{
			code.append("<script type=\"text/javascript\">\n//<![CDATA[\n").append(MiscUtils.implode(this.appendedScripts, "\n\n"));
			code.append("\n//]]>\n</script>");
		}
		
		code.append("</html>\n");
		writeToPage(code.toString());
		
		// clean up tag properties
		cleanUp();
		
		// TODO do we need to nullify the env and other injected properties from the tag here?
		// guess not
		return EVAL_PAGE;
    }
	
	@Override
	protected void cleanUp()
	{
		super.cleanUp();
		this.layout = null;
		this.title = null;
		this.appendedScripts = null;
		
		//log.debug("Cleaning up view tag");
		
		// print stack trace to know where we are
		// TODO remove this
		//(new KommetException("Mock error - remove this")).printStackTrace();
		
		clearErrorMessages();
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getTitle()
	{
		return title;
	}

	public Integer getComponentOrdinal()
	{
		return componentOrdinal;
	}
	
	public Integer nextComponentOrdinal()
	{
		return ++this.componentOrdinal;
	}
	
	public String nextComponentId()
	{
		return COMPONENT_ID_PREFIX + (++this.componentOrdinal);
	}
	
	public String getComponentId()
	{
		return COMPONENT_ID_PREFIX + this.componentOrdinal;
	}

	public void setLayout(String layout)
	{
		this.layout = layout;
	}

	public String getLayout()
	{
		return layout;
	}

	public void setName(String name)
	{
		// do nothing
	}

	public String getName()
	{
		return null;
	}

	public void setPackage(String packageName)
	{
		// do nothing
	}

	public String getPackage()
	{
		return null;
	}
	
	public FileService getFileService() throws MisplacedTagException
	{
		return getViewWrapper().getFileService();
	}
	
	public SharingService getSharingService() throws MisplacedTagException
	{
		return getViewWrapper().getSharingService();
	}
	
	public CommentService getCommentService() throws MisplacedTagException
	{
		return getViewWrapper().getCommentService();
	}

	public AuthData getAuthData() throws MisplacedTagException
	{
		return getViewWrapper().getAuthData();
	}

	public UserService getUserService() throws MisplacedTagException
	{
		return getViewWrapper().getUserService();
	}

	public LayoutService getLayoutService() throws MisplacedTagException
	{
		return getViewWrapper().getLayoutService();
	}
	
	public InternationalizationService getI18n() throws MisplacedTagException
	{
		return getViewWrapper().getI18n();
	}

	public FieldHistoryService getFieldHistoryService() throws MisplacedTagException
	{
		return getViewWrapper().getFieldHistoryService();
	}
	
	/**
	 * Add javascript code to be appended at the end of the page.
	 * @param script javascript code, not wrapped in <script> tags
	 */
	public void appendScript(String script)
	{
		if (this.appendedScripts == null)
		{
			this.appendedScripts = new ArrayList<String>();
		}
		this.appendedScripts.add(script);
	}

	public String getObject()
	{
		return object;
	}

	public void setObject(String type)
	{
		this.object = type;
	}
}
