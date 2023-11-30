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
import javax.servlet.jsp.PageContext;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.FileRecordAssignment;
import kommet.basic.keetle.tags.buttons.Button;
import kommet.basic.keetle.tags.datatable.DataSourceType;
import kommet.basic.keetle.tags.datatable.DataTable;
import kommet.basic.keetle.tags.datatable.DataTableColumn;
import kommet.basic.keetle.tags.objectlist.ObjectListConfig;
import kommet.basic.keetle.tags.objectlist.ObjectListTag;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.files.FileRecordAssignmentFilter;
import kommet.i18n.I18nDictionary;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

public class FilesTag extends KommetTag
{
	private static final long serialVersionUID = -6647102275388752606L;
	private String title;
	
	// one of "view" or "download"
	private String onClick;
	
	private Integer pageSize;
	
	private boolean isDisplayWithJavascript = true;
	
	private static final String JS_PREFIX = "javascript:";

	public FilesTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {
		ObjectDetailsTag parent = (ObjectDetailsTag)findAncestorWithClass(this, ObjectDetailsTag.class);
		if (parent == null)
		{
			throw new JspException("Files tag needs to be places inside an ObjectDetails tag");
		}
		
		// if parent object details tag is rendered in edit mode, we do not display the files tab
		if (!TagMode.VIEW.stringValue().equals(parent.getMode()))
		{
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
		
		String listId = null;
		ViewTag parentView = null;
		String actualTitle = StringUtils.hasText(this.title) ? this.title : i18n.get("files.list.title");
		
		try
		{
			parentView = getParentView();
			listId = parentView.nextComponentId();
		}
		catch (KommetException e)
		{
			return exitWithTagError("Error getting files associated with the record: " + e.getMessage());
		}
		
		if (this.pageSize == null)
		{
			this.pageSize = 15;
		}
		
		String actualOnClick = this.onClick;
		
		if (!StringUtils.hasText(actualOnClick))
		{
			actualOnClick = "download";
		}
		
		if (!this.isDisplayWithJavascript)
		{
			FileRecordAssignmentFilter filter = new FileRecordAssignmentFilter();
			filter.addRecordId(recordId);
			filter.setInitFiles(true);
			List<FileRecordAssignment> assignments = null;
			StringBuilder code = new StringBuilder();
			
			try
			{
			code.append("\n\n<script language=\"Javascript\">");
				code.append("function deleteFile(assignmentId) { $.post(\"" + getHost() + "/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/unassignfile\", { assignmentId: assignmentId }, ");
			code.append("function(data) {");
			code.append("if (data.result == \"success\") { $(\"#file-row-\" + assignmentId).remove(); km.js.ui.statusbar.show(km.js.config.i18n[\"files.deleted.msg\"]); } ");
			code.append("}, \"json\"); }");
			code.append("</script>\n\n");
			code.append("<div class=\"").append(parent.getRelatedListCssClass()).append("\" id=\"").append(listId).append("\">");
			
				assignments = parentView.getFileService().findAssignments(filter, parent.getAuthData(), getEnv());
				code.append(ObjectListConfig.detachedBtnPanel(null, actualTitle, null, getHost(), i18n, getEnv(), uploadBtn(recordId, getViewWrapper(), this.pageContext, i18n.get("files.upload"))));
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				return exitWithTagError("Error rendering buttons for files tag: " + e.getMessage());
			}
			// warning container
			code.append("<div id=\"warn-").append(listId).append("\"></div>");
			// start record table
			code.append("<table class=\"std-table\">");
			List<String> columnNames = MiscUtils.toList(i18n.get("files.name"), i18n.get("label.createdDate"), i18n.get("label.createdBy"), i18n.get("files.comment"), "");
			code.append(ObjectListTag.getTableHeader(columnNames, null, null));
			code.append("<tbody>");
			
			try
			{
				if (!assignments.isEmpty())
				{
					for (FileRecordAssignment assignment : assignments)
					{
						code.append("<tr id=\"file-row-").append(assignment.getId()).append("\">");
						
						if (actualOnClick.equals("view"))
						{
							code.append("<td><a href=\"").append(getHost()).append("/").append(UrlUtil.SYSTEM_ACTION_URL_PREFIX).append("/files/").append(assignment.getFile().getId()).append("?recordId=").append(recordId).append("\">").append(assignment.getFile().getName()).append("</a></td>");
						}
						else if (actualOnClick.equals("download"))
						{
							code.append("<td><a href=\"").append(getHost()).append("/").append(UrlUtil.SYSTEM_ACTION_URL_PREFIX).append("/download/").append(assignment.getFile().getId()).append("\">").append(assignment.getFile().getName()).append("</a></td>");
						}
						else if (actualOnClick.startsWith(JS_PREFIX))
						{
							String jsCall = actualOnClick.substring(JS_PREFIX.length());
							code.append("<td><a href=\"javascript:;\" onClick=\"").append(jsCall).append("\">").append(assignment.getFile().getName()).append("</a></td>");
						}
						
						// render date according to user's locale
						code.append("<td>").append(MiscUtils.formatDateTimeByUserLocale(assignment.getFile().getCreatedDate(), parentView.getAuthData())).append("</td>");
						code.append("<td>").append(UserLinkTag.getCode(assignment.getFile().getCreatedBy().getId(), getEnv(), getHost(), parentView.getUserService())).append("</td>");
						// TODO truncate comment
						code.append("<td>").append(assignment.getComment() != null ? truncComment(assignment.getComment()) : "").append("</td>");
						code.append("<td><a href=\"javascript:;\" onclick=\"ask('").append(i18n.get("files.delete.warning")).append("', 'warn-").append(listId).append("', function() { deleteFile('" + assignment.getId() + "'); }, 'rel-list-del-ask');\">").append(i18n.get("btn.delete")).append("</a></td>");
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
		}
		else
		{
			try
			{
				parent.addTab(getFilesTab(listId, actualTitle, actualOnClick, this.pageSize, parent, parent.getRecord(), getEnv()));
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				parent.addErrorMsgs("Could not render related list");
				cleanUp();
				return EVAL_PAGE;
			}
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
	private static ObjectDetailsTag.Tab getFilesTab(String tabId, String title, String onClickAction, Integer pageSize, ObjectDetailsTag parent, Record record, EnvData env) throws KommetException
	{
		String targetId = "tab-" + MiscUtils.getHash(20);
		String targetElement = "<div class=\"km-relatedlist-container\"><a name=\"anchor-" + tabId + "\"></a><div id=\"" + targetId + "\"></div></div>";
		String jqueryTarget = "$(\"#" + targetId + "\")";
		
		String tabRenderCallback = getTabJavascriptRenderFunction(jqueryTarget, parent, title, onClickAction, record, pageSize, parent.getPageContext(), parent.getAuthData(), env);
		ObjectDetailsTag.Tab tab = parent.new Tab(title, targetElement, jqueryTarget, tabRenderCallback, tabId, null);
		tab.setTabStyle(TabStyle.TOP);
		
		return tab;
	}
	
	private static String getTabJavascriptRenderFunction(String jqueryTarget, ObjectDetailsTag parent, String actualTitle, String onClickAction, Record record, Integer pageSize, PageContext pageContext, AuthData authData, EnvData env) throws KommetException
	{	
		StringBuilder query = new StringBuilder("SELECT file.id, file.name, file.createdBy.userName, file.createdBy.id, file.createdDate, comment from FileRecordAssignment where recordId = '" + record.getKID() + "'");
		
		String varName = "rel_" + MiscUtils.getHash(20);
		
		DataTable dt = new DataTable(query.toString(), DataSourceType.DATABASE, env);
		dt.setPageSize(pageSize);
		dt.setVar(varName);
		dt.setPaginationActive(true);
		dt.setTitle(actualTitle);
		dt.setJqueryTarget(jqueryTarget);
		dt.setProperties(getFileColumns(parent.getHost(), onClickAction));
		
		StringBuilder btnPanelCode = new StringBuilder();
		
		Type fileType = env.getType(KeyPrefix.get(KID.FILE_PREFIX));
		
		String renderFunctionName = "renderFileList" + MiscUtils.getHash(10);
		
		// add new button
		if (authData.canCreateType(fileType.getKID(), true, env))
		{
			String btnPanelVar = "btnPanel_" + MiscUtils.getHash(10);
			
			// create button panel
			btnPanelCode.append("var ").append(btnPanelVar).append(" = km.js.buttonpanel.create({ id: \"btn-panel-").append(MiscUtils.getHash(5)).append("\"");
			btnPanelCode.append(", cssClass: \"km-record-list-button-panel\"").append(" });\n");
			
			btnPanelCode.append(btnPanelVar).append(".addButton({");
			btnPanelCode.append("label: km.js.config.i18n['files.upload']");
			btnPanelCode.append(", onClick: function() { " + getFileUploadFunction(parent.getViewWrapper(), record.getKID(), renderFunctionName) + "(); }");
			
			btnPanelCode.append("});\n");
			
			btnPanelCode.append(varName).append(".setButtonPanel(").append(btnPanelVar).append(");");
			
			dt.setTitleHandledByButtonPanel(true);
		}
		
		StringBuilder renderFunction = new StringBuilder();
		renderFunction.append("function ").append(renderFunctionName).append("() {");
		renderFunction.append(dt.getCode().getInitCode() + "; " + btnPanelCode.toString()).append("}\n");
		
		parent.getParentView().appendScript(renderFunction.toString());
		parent.getParentView().appendScript(renderFunctionName + "();");
		parent.getParentView().appendScript(getDeleteFileFunction(renderFunctionName));
		
		return renderFunctionName + "()";
	}
	
	private static String getDeleteFileFunction(String renderFunctionName)
	{
		StringBuilder code = new StringBuilder();
		code.append("function deleteFile(assignmentId) {");
		code.append("$.post(km.js.config.contextPath + \"/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/unassignfile\", { assignmentId: assignmentId }, ");
		code.append("function(data) {");
		code.append("if (data.result == \"success\") { " + renderFunctionName + "(); km.js.ui.statusbar.show(km.js.config.i18n[\"files.deleted.msg\"]); } ");
		code.append("}, \"json\"); };");
		return code.toString();
	}

	private static String getFileUploadFunction(ViewWrapperTag viewWrapper, KID recordId, String rerenderFileListFunction)
	{
		String rand = MiscUtils.getHash(10);
		String varName = "file_upload_" + rand;
		String initCode = "<script>var " + varName + " = km.js.fileupload.create({";
		initCode += "id:\"" + varName + "\", \"recordId\": \"" + recordId + "\", ";
		initCode += "afterClose: function() { " + rerenderFileListFunction + "() }, ";
		initCode += "\"parentDialog\": \"" + varName + "\"});";
		initCode += "function showUpload" + rand + "() { " + varName + ".show();";
		initCode += "}</script>";
		viewWrapper.addPostViewCode(initCode);
		
		return "showUpload" + rand;
	}

	private static List<DataTableColumn> getFileColumns (String contextPath, String onClickAction)
	{
		List<DataTableColumn> dtColumns = new ArrayList<DataTableColumn>();
		
		DataTableColumn nameColumn = new DataTableColumn();
		nameColumn.setFieldApiName("file.name");
		nameColumn.setSortable(true);
		nameColumn.setLinkStyle(true);
		
		if ("view".equals(onClickAction))
		{
			nameColumn.setUrl(contextPath + "/km/files/{file.id}");
		}
		else if ("download".equals(onClickAction))
		{
			nameColumn.setUrl(contextPath + "/km/download/{file.id}");
		}
		else if (onClickAction.startsWith(JS_PREFIX))
		{
			String jsCall = onClickAction.substring(JS_PREFIX.length());
			nameColumn.setOnClick(jsCall);
		}
		
		dtColumns.add(nameColumn);
		
		DataTableColumn createdDateColumn = new DataTableColumn();
		createdDateColumn.setFieldApiName("file.createdDate");
		createdDateColumn.setSortable(true);
		dtColumns.add(createdDateColumn);
		
		DataTableColumn createdByColumn = new DataTableColumn();
		createdByColumn.setFieldApiName("file.createdBy.userName");
		createdByColumn.setSortable(true);
		dtColumns.add(createdByColumn);
		
		StringBuilder deleteBtnFunction = new StringBuilder();
		deleteBtnFunction.append("function(val) {");
		deleteBtnFunction.append("var link = $(\"<a></a>\").attr(\"href\", \"javascript:;\").text(km.js.config.i18n[\"btn.delete\"]);");
		deleteBtnFunction.append("var doDelete = function() { deleteFile(val, $(this)); };");
		deleteBtnFunction.append("km.js.ui.confirm({ callback: doDelete, question: km.js.config.i18n[\"files.delete.warning\"], target: link });");
		deleteBtnFunction.append("return link;");
		deleteBtnFunction.append("}");
		
		DataTableColumn deleteBtnColumn = new DataTableColumn();
		deleteBtnColumn.setFieldApiName("id");
		deleteBtnColumn.setFormatFunction(deleteBtnFunction.toString());
		dtColumns.add(deleteBtnColumn);
		
		return dtColumns;
	}

	private String truncComment(String comment)
	{
		return comment != null && comment.length() > 35 ? comment.substring(0, 34) + "..." : comment;
	}

	private Button uploadBtn(KID recordId, ViewWrapperTag viewWrapper, PageContext pageContext, String label)
	{
		String rand = MiscUtils.getHash(10);
		String varName = "file_upload_" + rand;
		String initCode = "<script>var " + varName + " = km.js.fileupload.create({";
		initCode += "id:\"" + varName + "\", \"recordId\": \"" + recordId + "\", ";
		initCode += "\"parentDialog\": \"" + varName + "\"});";
		initCode += "function showUpload" + rand + "() { " + varName + ".show();";
		initCode += "}</script>";
		viewWrapper.addPostViewCode(initCode);
		
		StringBuilder code = new StringBuilder();
		code.append("<a href=\"javascript:;\" onclick=\"showUpload" + rand + "()\"");
		code.append(" class=\"sbtn\"");
		code.append(">").append(label).append("</a>");
		return new Button(code.toString());
	}
	
	@Override
	protected void cleanUp()
	{
		super.cleanUp();
		this.onClick = null;
		this.title = null;
		this.pageSize = null;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getTitle()
	{
		return title;
	}

	public String getOnClick()
	{
		return onClick;
	}

	public void setOnClick(String onClick)
	{
		this.onClick = onClick;
	}

	public Integer getPageSize()
	{
		return pageSize;
	}

	public void setPageSize(Integer pageSize)
	{
		this.pageSize = pageSize;
	}
}
