/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import javax.servlet.jsp.JspException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.auth.UserService;
import kommet.basic.Comment;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.i18n.I18nDictionary;
import kommet.utils.MiscUtils;

public class CommentsTag extends KommetTag
{
	private static final long serialVersionUID = 7548611422460265526L;
	private String title;
	
	/**
	 * ID of the record for which the association is displayed.
	 * This ID has priority over the ID of the record retrieved from the parent object details tag.
	 */
	private String sRecordId;
	private String buttonsPostprocessor;
	private String queriedFields;
	
	private static final Logger log = LoggerFactory.getLogger(CommentsTag.class);

	public CommentsTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		log.debug("Started comment tag");
		
		ObjectDetailsTag parent = (ObjectDetailsTag)findAncestorWithClass(this, ObjectDetailsTag.class);
		KID recordId = null;
		
		if (parent == null)
		{	
			// parent details is not set, but perhaps record ID is specified
			if (!StringUtils.hasText(this.sRecordId))
			{
				return exitWithTagError("Field history tag is not placed within objectDetails tag and record ID is not defined.");
			}
		}
		else
		{
			// if parent object details tag is rendered in edit mode, we do not display the comments tab
			if (!TagMode.VIEW.stringValue().equals(parent.getMode()))
			{
				return EVAL_PAGE;
			}
			
			try
			{
				recordId = parent.getRecord().getKID();
			}
			catch (KommetException e)
			{
				return exitWithTagError("Error retrieving parent record ID. Nested: " + e.getMessage());
			}
		}
		
		if (StringUtils.hasText(this.sRecordId))
		{
			log.debug("Record ID is not null");
			try
			{
				recordId = KID.get(this.sRecordId);
			}
			catch (KIDException e)
			{
				return exitWithTagError("Invalid record ID value '" + this.sRecordId + "'");
			}
		}
		else
		{
			log.debug("Explicitly set record ID is null");
		}
		
		String listId;
		I18nDictionary i18n;
		ViewTag parentView = null;
		try
		{
			parentView = getParentView();
			listId = getParentView().nextComponentId();
			i18n = getParentView().getI18n().getDictionary(parentView.getAuthData().getUser().getLocaleSetting());
		}
		catch (KommetException e)
		{
			log.debug("KommetException thrown in comment tag");
			e.printStackTrace();
			cleanUp();
			return exitWithTagError("Error reading record ID from object details tag: " + e.getMessage());
		}
		catch (Exception e)
		{
			log.debug("Unknown exception thrown in comment tag");
			e.printStackTrace();
			cleanUp();
			return exitWithTagError("Error reading record ID from object details tag: " + e.getMessage());
		}
		
		String commentContainerId = parentView.nextComponentId();
		String actualTitle = StringUtils.hasText(this.title) ? this.title : i18n.get("comments.list.title");
		
		boolean canAddComments = false;
		
		try
		{
			canAddComments = parentView.getAuthData().canCreateType(getEnv().getType(KeyPrefix.get(KID.COMMENT_PREFIX)).getKID(), true, getEnv());;
		}
		catch (KommetException e)
		{
			return exitWithTagError("Error rendering comments tag");
		}
		
		log.debug("Creating tag code");
		
		// initialization code
		StringBuilder initCode = new StringBuilder();
		initCode.append("$(document).ready(function() {"); 
		initCode.append("var cmt = km.js.comments.create({");
		initCode.append("id: \"comment-list-").append(commentContainerId).append("\",\n");
		initCode.append("recordId: \"").append(recordId.getId()).append("\",\n");
		initCode.append("addComments: ").append(canAddComments).append(",\n");
		
		if (StringUtils.hasText(this.buttonsPostprocessor))
		{
			initCode.append("buttonsPostprocessor: ").append(this.buttonsPostprocessor).append(",\n");
		}
		
		if (StringUtils.hasText(queriedFields))
		{
			initCode.append("queriedFields: \"").append(this.queriedFields).append("\",\n");
		}
		
		initCode.append("title: \"").append(actualTitle).append("\"\n");
		initCode.append("});\n");
		initCode.append("cmt.render(function(code) { $(\"#").append(commentContainerId).append("\").empty().append(code); });");
		initCode.append("});");
		
		parentView.appendScript(initCode.toString());
		
		log.debug("Added init code \"" + initCode.toString().substring(0, 50) + "\" to parent view, length = " + initCode.length());
		
		StringBuilder code = new StringBuilder();
		code.append("<div id=\"").append(commentContainerId).append("\"></div>");
		
		if (parent != null)
		{
			// if tab is embedded in object details, add it to the parent details tag
			parent.addTab(actualTitle, code.toString(), listId, null);
		}
		else
		{
			// write tag code to the page
			writeToPage(code.toString());
		}
		
		log.debug("Finished rendering tag");
		
		return EVAL_PAGE;
    }
	
	public static String getCommentBox(Comment comment, AuthData authData, I18nDictionary i18n, EnvData env, String contextPath, UserService userService) throws KommetException
	{
		String userLink;
		userLink = UserLinkTag.getCode(comment.getCreatedBy().getId(), env, contextPath, userService);
		String date = MiscUtils.formatDateTimeByUserLocale(comment.getCreatedDate(), authData);
		String deleteLink = "<a href=\"javascript:;\" id=\"delete-comment-" + comment.getId() + "\" onclick=\"ask('" + i18n.get("comments.delete.warning") + "', 'comment-delete-warn-" + comment.getId() + "', function() { deleteComment('" + comment.getId() + "'); }, null, '" + i18n.get("btn.yes") + "', '" + i18n.get("btn.no") + "');\">" + i18n.get("btn.delete") + "</a>";
		return getCommentBox(comment.getId(), MiscUtils.newLinesToBr(comment.getContent()), date, userLink, deleteLink);
	}
	
	private static String getCommentBox(KID commentId, String content, String date, String userLink, String deleteLink)
	{
		StringBuilder code = new StringBuilder();
		// add warn prompt container for this comment
		code.append("<div id=\"comment-delete-warn-").append(commentId).append("\"></div>");
		code.append("<div class=\"item\"");
		code.append(" id=\"").append("comment-").append(commentId.getId()).append("\">");
		code.append("<div class=\"header\">");
		code.append("<span class=\"user\">").append(userLink).append("</span>");
		code.append("<span class=\"date\">").append(date).append("</span>");
		code.append("<span class=\"btns\">").append(deleteLink).append("</span>");
		code.append("</div><div class=\"text\">");
		code.append(content);
		code.append("</div></div>");
		return code.toString();
	}

	@Override
	protected void cleanUp()
	{
		super.cleanUp();
		this.title = null;
		this.buttonsPostprocessor = null;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getTitle()
	{
		return title;
	}
	
	public String getRecordId()
	{
		return sRecordId;
	}

	public void setRecordId(String sRecordId)
	{
		this.sRecordId = sRecordId;
	}

	public String getButtonsPostprocessor()
	{
		return buttonsPostprocessor;
	}

	public void setButtonsPostprocessor(String buttonsPostProcessor)
	{
		this.buttonsPostprocessor = buttonsPostProcessor;
	}

	public String getQueriedFields()
	{
		return queriedFields;
	}

	public void setQueriedFields(String queriedFields)
	{
		this.queriedFields = queriedFields;
	}
}
