/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.i18n.I18nDictionary;
import kommet.utils.MiscUtils;

public class TasksTag extends KommetTag
{
	private static final long serialVersionUID = -1676749667251502551L;

	private String title;
	
	public TasksTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		ObjectDetailsTag parent = (ObjectDetailsTag)findAncestorWithClass(this, ObjectDetailsTag.class);
		if (parent == null)
		{
			throw new JspException("Tasks tag needs to be places inside an ObjectDetails tag");
		}
		
		// if parent object details tag is rendered in edit mode, we do not display the tasks tab
		if (!TagMode.VIEW.stringValue().equals(parent.getMode()))
		{
			return EVAL_PAGE;
		}
		
		I18nDictionary i18n = null;
		try
		{
			i18n = getParentView().getI18n().getDictionary(parent.getAuthData().getUser().getLocaleSetting());
		}
		catch (KommetException e)
		{
			return exitWithTagError("Error reading record ID from object details tag: " + e.getMessage());
		}
		
		String listId = null;
		ViewTag parentView = null;
		String actualTitle = StringUtils.hasText(this.title) ? this.title : i18n.get("tasks.list.title");
		
		try
		{
			parentView = getParentView();
			listId = parentView.nextComponentId();
		}
		catch (KommetException e)
		{
			return exitWithTagError("Error getting tasks associated with the record: " + e.getMessage());
		}
		
		try
		{
			parent.addTab(getTasksTab(listId, actualTitle, parent, parent.getRecord(), getEnv()));
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			parent.addErrorMsgs("Could not render related list");
			cleanUp();
			return EVAL_PAGE;
		}
		
		return EVAL_PAGE;
    }
	
	/**
	 * Returns a definitions of a related list tab.
	 * @param tabId
	 * @param queriedFields
	 * @param record
	 * @param title
	 * @return
	 * @throws KommetException
	 */
	public static ObjectDetailsTag.Tab getTasksTab(String tabId, String title, ObjectDetailsTag parent, Record record, EnvData env) throws KommetException
	{
		String targetId = "tab-" + MiscUtils.getHash(20);
		String targetElement = "<div class=\"km-relatedlist-container\"><a name=\"anchor-" + tabId + "\"></a><div id=\"" + targetId + "\"></div></div>";
		String jqueryTarget = "$(\"#" + targetId + "\")";
		
		String tabRenderCallback = getTabJavascriptRenderFunction(jqueryTarget, parent, title, record, 25, parent.getPageContext(), parent.getAuthData(), env);
		ObjectDetailsTag.Tab tab = parent.new Tab(title, targetElement, jqueryTarget, tabRenderCallback, tabId, null);
		tab.setTabStyle(TabStyle.TOP);
		
		return tab;
	}
	
	private static String getTabJavascriptRenderFunction(String jqueryTarget, ObjectDetailsTag parent, String actualTitle, Record record, Integer pageSize, PageContext pageContext, AuthData authData, EnvData env) throws KommetException
	{	
		String renderFunctionName = "renderTaskList" + MiscUtils.getHash(10);
		
		StringBuilder renderFunction = new StringBuilder();
		renderFunction.append("function ").append(renderFunctionName).append("() {");
		
		renderFunction.append("km.js.tasks.show({ target: ").append(jqueryTarget).append(", recordId: \"").append(record.getKID()).append("\", isEmbedded: true });");
		
		renderFunction.append("\n}");
		
		parent.getParentView().appendScript(renderFunction.toString());
		parent.getParentView().appendScript(renderFunctionName + "();");
		
		return renderFunctionName + "()";
	}
	
	@Override
	protected void cleanUp()
	{
		super.cleanUp();
		this.title = null;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getTitle()
	{
		return title;
	}
}
