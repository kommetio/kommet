/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.Tag;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.keetle.PageData;
import kommet.config.UserSettingKeys;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.utils.MiscUtils;
import kommet.web.RequestAttributes;

/**
 * Basic tag class for all KTL tags.
 * @created 08-2013
 *
 */
public abstract class KommetTag extends BodyTagSupport
{
	private static final long serialVersionUID = 8148441025161055281L;
	
	/**
	 * The view tag in which this tag is embedded.
	 */
	private ViewTag parentView;
	private ViewWrapperTag viewWrapper;
	
	private List<String> errorMsgs;
	
	/**
	 * Child tags of this tag mapped by their class
	 */
	private Map<Class<? extends Tag>, List<Tag>> children;
	
	public KommetTag() throws KommetException
	{
		super();
		
		this.errorMsgs = new ArrayList<String>();
		// parentView property cannot be initialized in tag constructor because the method
		// findAncestorWithClass does not seem to work in constructors
	}
	
	protected PageData getPageData()
	{
		return (PageData)this.pageContext.getRequest().getAttribute(RequestAttributes.PAGE_DATA_ATTR_NAME);
	}
	
	protected KommetTag checkParentTag (Class<? extends KommetTag> ... tagClasses) throws JspException
	{
		boolean validParentFound = false;
		for (Class<? extends KommetTag> tagClass : tagClasses)
		{
			KommetTag parentTag = (KommetTag)findAncestorWithClass(this, tagClass);
			if (parentTag != null)
			{
				return parentTag; 
			}
		}
		
		if (!validParentFound)
		{
			List<String> validParents = new ArrayList<String>();
			for (Class<? extends KommetTag> tagClass : tagClasses)
			{
				validParents.add(tagClass.getSimpleName());
			}
			throw new JspException("Tag " + this.getClass().getSimpleName() + " should be placed inside tag " + MiscUtils.implode(validParents, ", "));
		}
		else
		{
			// this code will never be reached because if tag is found, it will be returned earlier
			throw new JspException("Dead code reached - if tag was found, it should be returned earlier");
		}
	}
	
	protected int exitWithTagError(String msg) throws JspException
	{
		List<String> msgs = new ArrayList<String>();
		msgs.add(msg);
		return exitWithTagError(msgs);
	}
	
	protected int exitWithTagError(List<String> msgs) throws JspException
	{
		return exitWithTagError(msgs.toArray(new String[0]));
	}
	
	protected int exitWithTagError(String ... msgs) throws JspException
	{
		try
		{
			cleanUp();
			
			JspWriter out = this.pageContext.getOut();
			out.write("<div class=\"msg-tag tag-error\"><ul>");
			for (String msg : msgs)
			{
				out.write("<li>" + msg + "</li>");
			}
			out.write("</ul></div>");
		}
		catch (IOException e)
		{
			throw new JspException("Error writing to JSP page: " + e.getMessage());
		}
		
		return EVAL_PAGE;
	}
	
	/**
	 * Returns the current environment data.
	 * @return
	 * @throws KommetException
	 */
	public EnvData getEnv() throws KommetException
	{
		// if the tag is the km:view tag, which is the topmost tag in the hierarchy,
		// environment is ready and returned
		if (this instanceof ViewWrapperTag)
		{
			// check if env is not already set on the view tag
			if (((ViewWrapperTag)this).env == null)
			{
				((ViewWrapperTag)this).env = ((ViewWrapperTag)this).envService.getCurrentEnv(this.pageContext.getSession());
			}
			
			return ((ViewWrapperTag)this).env;
		}
		else
		{
			// return the env data from the parent view tag
			return getViewWrapper().getEnv();
		}
	}
	
	protected void writeToPage(String code) throws JspException
	{
		try
		{
			this.pageContext.getOut().write(code);
		}
		catch (IOException e)
		{
			throw new JspException("Error writing to JSP page: " + e.getMessage());
		}
	}
	
	protected void cleanUp()
	{
		clearErrorMessages();
	}

	public ViewTag getParentView() throws MisplacedTagException
	{
		if (this.parentView == null && !(this instanceof ViewTag))
		{
			this.parentView = (ViewTag)findAncestorWithClass(this, ViewTag.class);
			if (this.parentView == null)
			{
				throw new MisplacedTagException("Tag " + this.getClass().getSimpleName() + " is not placed within a view tag");
			}
		}
		return parentView;
	}
	
	public ViewWrapperTag getViewWrapper() throws MisplacedTagException
	{
		if (this.viewWrapper == null && !(this instanceof ViewWrapperTag))
		{
			this.viewWrapper = (ViewWrapperTag)findAncestorWithClass(this, ViewWrapperTag.class);
			if (this.viewWrapper == null)
			{
				throw new MisplacedTagException("Tag " + this.getClass().getSimpleName() + " is not placed within a view wrapper tag");
			}
		}
		return this.viewWrapper;
	}
	
	public void addErrorMsgs(List<String> msgs)
	{
		this.errorMsgs.addAll(msgs);
	}

	public void addErrorMsgs(String msg)
	{
		this.errorMsgs.add(msg);
	}

	protected List<String> getErrorMsgs()
	{
		return errorMsgs;
	}
	
	protected void clearErrorMessages()
	{
		if (this.errorMsgs != null)
		{
			this.errorMsgs = new ArrayList<String>();
		}
	}

	public void addChild (Tag tag)
	{
		if (this.children == null)
		{
			this.children = new HashMap<Class<? extends Tag>, List<Tag>>();
		}
		
		List<Tag> childrenWithType = this.children.get(tag.getClass());
		if (childrenWithType == null)
		{
			childrenWithType = new ArrayList<Tag>();
		}
		childrenWithType.add(tag);
		this.children.put(tag.getClass(), childrenWithType);
	}
	
	public boolean hasChildWithType (Class<? extends Tag> cls)
	{
		return this.children != null && this.children.containsKey(cls) && !this.children.get(cls).isEmpty();
	}

	public Map<Class<? extends Tag>, List<Tag>> getChildren()
	{
		return children;
	}
	
	public boolean hasErrorMsgs()
	{
		return this.errorMsgs != null && !this.errorMsgs.isEmpty();
	}
	
	/**
	 * Returns the host at which resources are served.
	 * @return
	 * @throws KommetException
	 */
	protected String getHost() throws KommetException
	{
		if (this.viewWrapper != null)
		{
			return this.viewWrapper.getHost();
		}
		
		AuthData authData = AuthUtil.getAuthData(pageContext.getSession());
		String host = authData.getUserCascadeSettings().get(UserSettingKeys.KM_SYS_HOST);
		return StringUtils.hasText(host) ? host : this.pageContext.getServletContext().getContextPath();
	}
}
