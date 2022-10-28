/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.springframework.util.StringUtils;

import kommet.basic.UserRecordSharing;
import kommet.basic.keetle.tags.objectlist.ObjectListConfig;
import kommet.basic.keetle.tags.objectlist.ObjectListTag;
import kommet.data.KeyPrefixException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.sharing.UserRecordSharingFilter;
import kommet.i18n.I18nDictionary;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

public class RecordSharingTag extends KommetTag
{
	private static final long serialVersionUID = -8807672241656589836L;
	private String title;

	public RecordSharingTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		ObjectDetailsTag parent = (ObjectDetailsTag)findAncestorWithClass(this, ObjectDetailsTag.class);
		if (parent == null)
		{
			throw new JspException("Record sharing tag needs to be places inside an ObjectDetails tag");
		}
		
		if (!TagMode.VIEW.stringValue().equals(parent.getMode()))
		{
			// skip field history tag in non-view mode
			return EVAL_PAGE;
		}
		
		KID recordId;
		I18nDictionary i18n = null;
		try
		{
			recordId = parent.getRecord().getKID();
			i18n = getParentView().getI18n().getDictionary(parent.getAuthData().getUser().getLocaleSetting());
		}
		catch (KommetException e)
		{
			return exitWithTagError("Error reading record ID from object details tag: " + e.getMessage());
		}
		
		UserRecordSharingFilter filter = new UserRecordSharingFilter();
		filter.addRecordId(recordId);
		filter.setInitUser(false);
		List<UserRecordSharing> sharings = null;
		ViewTag parentView = null;
		String listId = null;
		try
		{
			parentView = getParentView();
			sharings = parentView.getSharingService().find(filter, getEnv());
			listId = parentView.nextComponentId();
		}
		catch (KommetException e)
		{
			return exitWithTagError("Error getting sharings: " + e.getMessage());
		}
		
		StringBuilder code = new StringBuilder();
		code.append("\n\n<script language=\"Javascript\">");
		code.append("function deleteSharing(sharingId) { $.post(\"" + pageContext.getServletContext().getContextPath() + "/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX  + "/deleteuserrecordsharing\", { sharingId: sharingId }, ");
		code.append("function(data) {");
		code.append("if (data.result == \"success\") { $(\"#file-row-\" + sharingId).remove(); } ");
		code.append("}, \"json\"); }");
		code.append("</script>\n\n");
		code.append("<div class=\"").append(parent.getRelatedListCssClass()).append("\" id=\"").append(listId).append("\">");
		
		String actualTitle = StringUtils.hasText(this.title) ? this.title : i18n.get("urs.title");
		
		try
		{
			code.append(ObjectListConfig.detachedBtnPanel(null, actualTitle, null, null, null, getEnv()));
			code.append(shareForm(recordId, parent, getViewWrapper(), this.pageContext, i18n.get("urs.share"), i18n));
		}
		catch (KommetException e)
		{
			return exitWithTagError("Error rendering user autocomplete: " + e.getMessage());
		}
		
		// warning container
		code.append("<div id=\"warn-").append(listId).append("\"></div>");
		// start record table
		code.append("<table class=\"std-table\">");
		List<String> columnNames = MiscUtils.toList(i18n.get("urs.user"), i18n.get("urs.date"), i18n.get("urs.permissions.list"), i18n.get("urs.reason"), i18n.get("urs.isgeneric"), i18n.get("urs.sharedBy"), "");
		code.append(ObjectListTag.getTableHeader(columnNames, null, null));
		code.append("<tbody>");
		
		try
		{
			if (!sharings.isEmpty())
			{
				for (UserRecordSharing sharing : sharings)
				{
					code.append("<tr id=\"file-row-").append(sharing.getId()).append("\">");
					code.append("<td>").append(UserLinkTag.getCode(sharing.getUser().getId(), getEnv(), this.pageContext.getServletContext().getContextPath(), parentView.getUserService())).append("</td>");
					// render date according to user's locale
					code.append("<td>").append(MiscUtils.formatDateTimeByUserLocale(sharing.getCreatedDate(), parentView.getAuthData())).append("</td>");
					
					// show permissions
					code.append("<td>").append("read");
					if (Boolean.TRUE.equals(sharing.getEdit()))
					{
						code.append(", edit");
					}
					if (Boolean.TRUE.equals(sharing.getDelete()))
					{
						code.append(", delete");
					}
					code.append("</td>");
					
					code.append("<td>").append(truncComment(sharing.getReason())).append("</td>");
					code.append("<td>").append(sharing.getIsGeneric()).append("</td>");
					code.append("<td>").append(UserLinkTag.getCode(sharing.getCreatedBy().getId(), getEnv(), this.pageContext.getServletContext().getContextPath(), parentView.getUserService())).append("</td>");
					code.append("<td><a href=\"javascript:;\" onclick=\"ask('").append(i18n.get("urs.delete.warning")).append("', 'warn-").append(listId).append("', function() { deleteSharing('" + sharing.getId() + "'); }, 'rel-list-del-ask');\">").append(i18n.get("btn.delete")).append("</a></td>");
					code.append("</tr>");
				}
			}
			else
			{
				code.append(ObjectListConfig.getNoResultsMsg(columnNames.size(), parentView.getI18n().get(parentView.getAuthData().getUser().getLocaleSetting(), "msg.noresults")));
			}
		}
		catch (KommetException e)
		{
			parentView.addErrorMsgs("Error rendering files list: " + e.getMessage());
			return EVAL_PAGE;
		}
		
		code.append("</tbody></table>");
		code.append("</div>");
		
		// create anchor so that users can be taken directly to the list
		String anchor = "<a name=\"anchor-" + listId + "\"></a>";
		
		parent.addTab(actualTitle, anchor + code.toString(), listId, null);
		
		return EVAL_PAGE;
    }
	
	private String truncComment(String str)
	{
		return str != null ? (str.length() > 35 ? str.substring(0, 34) + "..." : str) : "";
	}

	private String shareForm(KID recordId, ObjectDetailsTag objectDetailsTag, ViewWrapperTag viewWrapper, PageContext pageContext, String label, I18nDictionary i18n) throws KeyPrefixException, KommetException
	{
		StringBuilder code = new StringBuilder();
		code.append("<div style=\"margin-bottom: 2em\" class=\"km-record-sharing\">");
		
		code.append("<div style=\"display:inline-block; margin-right: 2em\"><input type=\"text\" id=\"userToShare\" class=\"std-input\" /></div>");
		
		addUserLookup("userToShare", "sharedUserId", objectDetailsTag, viewWrapper, pageContext);
		
		String permissionFieldId = "km-permissions-" + MiscUtils.getHash(5);
		
		code.append("<input type=\"button\" class=\"sbtn\" value=\"").append(label).append("\" onclick=\"shareRecord()\" />");
		
		code.append("<select id=\"" + permissionFieldId + "\" class=\"std-input km-sharing-permissions\" style=\"min-width: 5em\">");
		code.append("<option value=\"read\">can read</option>");
		code.append("<option value=\"read-edit\">can read and edit</option>");
		code.append("<option value=\"read-edit-delete\">can read, edit and delete</option>");
		code.append("</select>");
		
		//code.append("<input type=\"hidden\" id=\"userToShareId\" />");
		code.append("</div>");
		
		// add javascript to handle sharing form
		code.append("<script language=\"Javascript\">");
		/*code.append("$(document).ready(function() { $(\"#userToShare\").autocomplete ({ source: searchUsers }); });");
		
		// start searchUsers function
		code.append("function searchUsers (req, resp) {");
		code.append("$.post(\"").append(pageContext.getServletContext().getContextPath()).append("/").append(UrlUtil.SYSTEM_ACTION_URL_PREFIX).append("/searchrecords\", ");
		code.append("{ typeId: \"").append(getEnv().getType(KeyPrefix.get(KID.USER_PREFIX)).getKID().getId()).append("\", keyword: $(\"#userToShare\").val(), searchField: \"userName\" }, ");
		code.append("function(data) { resp(data); }, \"json\"");
		// end $.post call
		code.append(");");
		// end searchUsers function
		code.append("}");*/
		
		// add shareRecord function
		code.append("function shareRecord(permissionFieldId) {");
		code.append("$.post(\"").append(pageContext.getServletContext().getContextPath()).append("/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/sharerecord\", ");
		code.append("{ recordId: \"").append(recordId).append("\", userId: $(\"#sharedUserId\").val(), permissions: $(\"#" + permissionFieldId + "\").val() }, ");
		code.append("function(data) { ");
		code.append("if (data.status == \"success\") { km.js.ui.statusbar.show(km.js.config.i18n[\"urs.sharingAdded\"]); } else {");
		code.append(" km.js.ui.statusbar.show(km.js.config.i18n[\"urs.errorAddingSharing\"]); }");
		code.append("}, \"json\");");
		code.append("}");
		// end shareRecord function
		
		code.append("</script>");
		
		return code.toString();
	}

	private void addUserLookup(String inputId, String newInputId, ObjectDetailsTag objectDetailsTag, ViewWrapperTag viewWrapper, PageContext pageContext) throws KommetException
	{
		String rand = MiscUtils.getHash(10);
		String varName = "ul" + rand;
		String callback = "initUserLookup" + rand;
		String initCode = "<script>";
		initCode += "function " + callback + "() { var " + varName + " = km.js.userlookup.create({ inputName: \"sharingInput" + rand + "\", inputId: \"" + newInputId + "\", visibleInput: { cssClass: \"km-input\" } }); " + varName + ".render($('#" + inputId + "'))";
		initCode += "}</script>";
		viewWrapper.addPostViewCode(initCode);
		
		if (objectDetailsTag.getTabsConfig() == null)
		{
			objectDetailsTag.setTabsConfig(new TabConfigTag());
			objectDetailsTag.getTabsConfig().setAutoAdded(true);
		}
		objectDetailsTag.getTabsConfig().setAfterRender("return function() { " + callback + "(); }");
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
