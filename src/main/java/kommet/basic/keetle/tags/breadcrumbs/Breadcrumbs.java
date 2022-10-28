/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.breadcrumbs;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.util.StringUtils;

import kommet.i18n.I18nDictionary;

public class Breadcrumbs
{
	private static final String SESSION_KEY = "breadcrumbs";
	private int max;
	private List<Breadcrumb> items = new ArrayList<Breadcrumb>();
	private String listLinkLabel;
	private String listLinkURL;
	private String contextPath;
	
	public Breadcrumbs (int max, String contextPath)
	{
		this.max = max;
		this.contextPath = contextPath;
	}
	
	public void add (String url, String title)
	{
		if (url == null)
		{
			return;
		}
		
		// check if URL is not already on the list
		
		Integer removeAtIndex = null;
		for (int i = 0; i < this.items.size(); i++)
		{
			if (this.items.get(i).getUrl().equals(url))
			{
				removeAtIndex = i;
				break;
			}
		}
		
		if (removeAtIndex == null && this.items.size() >= this.max)
		{
			// remove the oldest item
			removeAtIndex = this.items.size() - 1;
		}
		
		if (removeAtIndex != null)
		{
			this.items.remove((int)removeAtIndex);
		}
		
		this.items.add(new Breadcrumb(url, title));
	}
	
	public String getCode(I18nDictionary i18n, boolean isAlwaysVisible)
	{
		StringBuilder code = new StringBuilder();
		
		code.append("<div id=\"breadcrumbs\">");
		
		if (StringUtils.hasText(this.listLinkLabel) && StringUtils.hasText(this.listLinkURL))
		{
			// render list link
			code.append("<a href=\"").append(this.listLinkURL).append("\" class=\"bc km-breadcrumbs-rec-list\">");
			
			code.append("<span class=\"km-back-icon-span\"><img src=\"").append(this.contextPath).append("/resources/images/list.png\"></img></span>");
			
			code.append(this.listLinkLabel).append("</a>");
		}
		
		// add main button
		code.append("<a class=\"bclnk\" href=\"").append(!this.items.isEmpty() ? this.items.get(0).getUrl() : "javascript:;").append("\">&lt;&lt;</a>");
		
		String visibilityStyle = isAlwaysVisible ? "" : " style=\"display:none\"";
		
		code.append("<a").append(visibilityStyle);
		code.append(" href=\"javascript:;\" class=\"info\">").append(i18n.get("back.to.record.list")).append("</a>");
		
		for (Breadcrumb item : this.items)
		{
			code.append("<a").append(visibilityStyle).append(" href=\"").append(item.getUrl()).append("\" class=\"bc\">").append(item.getTitle()).append("</a>");
		}
		
		code.append("</div>");
		
		if (!isAlwaysVisible)
		{
			code.append("<script language=\"Javascript\">");
			code.append("var bctimeout;");
			code.append("$(\"#breadcrumbs\").mouseleave(function() { bctimeout = setTimeout(function() { $(\"#breadcrumbs > a.bc\").fadeOut(200); $(\"#breadcrumbs > a.info\").fadeOut(200); }, 100); });");
			code.append("$(\"#breadcrumbs\").mouseenter(function() { clearTimeout(bctimeout); });");
			code.append("$(\"#breadcrumbs > a.bclnk\").mouseenter(function() { $(\"#breadcrumbs > a.bc\").show(); $(\"#breadcrumbs > a.info\").show(); });");
			code.append("</script>");
		}
		
		return code.toString();
	}
	
	public static void setListLink (String url, String label, int max, HttpSession session)
	{
		Breadcrumbs crumbs = (Breadcrumbs)session.getAttribute(SESSION_KEY);
		if (crumbs == null)
		{
			crumbs = new Breadcrumbs(max, session.getServletContext().getContextPath());
		}
		
		crumbs.setListLinkLabel(label);
		crumbs.setListLinkURL(url);
		session.setAttribute(SESSION_KEY, crumbs);
	}
	
	public static void add (String url, String title, int max, HttpSession session)
	{
		Breadcrumbs crumbs = (Breadcrumbs)session.getAttribute(SESSION_KEY);
		if (crumbs == null)
		{
			crumbs = new Breadcrumbs(max, session.getServletContext().getContextPath());
		}
		
		crumbs.add(url, title);
		session.setAttribute(SESSION_KEY, crumbs);
	}
	
	public static String getCode(I18nDictionary i18n, boolean isAlwaysVisible, HttpSession session)
	{
		Breadcrumbs crumbs = (Breadcrumbs)session.getAttribute(SESSION_KEY);
		if (crumbs == null)
		{
			return "";
		}
		else
		{
			return crumbs.getCode(i18n, isAlwaysVisible);
		}
	}

	public String getListLinkLabel()
	{
		return listLinkLabel;
	}

	public void setListLinkLabel(String listLinkLabel)
	{
		this.listLinkLabel = listLinkLabel;
	}

	public String getListLinkURL()
	{
		return listLinkURL;
	}

	public void setListLinkURL(String listLinkURL)
	{
		this.listLinkURL = listLinkURL;
	}
}
