/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.keetle.PageData;
import kommet.basic.keetle.StandardObjectController;
import kommet.basic.keetle.tags.breadcrumbs.Breadcrumbs;
import kommet.basic.keetle.tags.buttons.Button;
import kommet.basic.keetle.tags.buttons.ButtonFactory;
import kommet.basic.keetle.tags.buttons.ButtonPanel;
import kommet.basic.keetle.tags.buttons.ButtonPrototype;
import kommet.basic.keetle.tags.objectlist.ListColumn;
import kommet.config.UserSettingKeys;
import kommet.data.DataAccessUtil;
import kommet.data.Field;
import kommet.data.KeyPrefixException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.UntypedRecord;
import kommet.data.datatypes.CollectionDataType;
import kommet.data.datatypes.DataType;
import kommet.env.EnvData;
import kommet.i18n.I18nDictionary;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.NestedContextField;
import kommet.web.kmparams.KmParamNode;
import kommet.web.kmparams.actions.Action;
import kommet.web.kmparams.actions.ShowLookup;

public class ObjectDetailsTag extends KommetTag implements RecordContext
{
	private static final long serialVersionUID = -9208229271738042190L;
	private Record record;
	private Type type;
	private AuthData authData;
	private List<Tab> tabs;
	private List<FieldTag> fields;
	private PropertyTableTag innerPropertyTable;
	private Boolean failOnUninitializedFields;
	private Boolean idFieldRendered = false;
	private Boolean displayMessages = true;
	private FormTag parentForm;
	private TagMode mode;
	private String fieldNamePrefix;
	private String innerPropertyTableCode;
	private ButtonPanel buttonPanel;
	private Boolean renderTitle = true;
	
	// title displayed on top of the object details tag
	private String title;
	
	// Whether the object details should be rendered within an ibox.
	private Boolean renderContainerBox = true;
	
	// Style in which tabs are rendered for this object details.
	// Defaults to TabStyle.TOP.
	private TabStyle tabStyle;
	private ViewTag parentView;
	
	private TabConfigTag tabsConfig;
	private boolean renderingStoppedDueToErrors;
	private boolean isMock;
	
	// mock form wrapping this object details tag, if no form has been explicitly put into the view
	private MockWrapperForm mockWrapperForm;
	
	private boolean jsRender = false;

	public ObjectDetailsTag() throws KommetException
	{
		super();
	}
	
	@Override
    public int doStartTag() throws JspException
    {	
		this.isMock = record instanceof UntypedRecord;
		
		if (this.mode == null)
		{
			// get form mode from invoking controller, if set
			try
			{
				getInheritedMode();
			}
			catch (KommetException e)
			{
				return exitWithTagError("Error assigning form mode: " + e.getMessage());
			}
		}
		
		if (this.mode == null)
		{
			// assume view mode
			this.mode = TagMode.VIEW;
		}
		
		if (this.failOnUninitializedFields == null)
		{
			// for create/edit views, do not require fields to be set on the record because it's a new record
			this.failOnUninitializedFields = !TagMode.EDIT.equals(this.mode); 
		}
		
		try
		{
			this.parentView = getParentView();
		}
		catch (KommetException e)
		{
			this.renderingStoppedDueToErrors = true;
			return exitWithTagError("The page is not rendered within a view tag: " + e.getMessage());
		}
		
		if (tabStyle == null)
		{
			tabStyle = TabStyle.TOP;
		}
		
		if (this.id == null)
		{
			this.id = parentView.nextComponentId();
		}
		
		this.fields = new ArrayList<FieldTag>();
		this.buttonPanel = new ButtonPanel(false);
		
		if (record == null)
		{
			this.renderingStoppedDueToErrors = true;
			return exitWithTagError("Record passed to object details tag is null");
		}
		
		try
		{
			if (!this.isMock)
			{
				this.type = getEnv().getType(record.getType().getKID());
			}
			
			this.authData = AuthUtil.getAuthData(this.pageContext.getSession());
			this.authData.updateTypePermissions(true, getEnv());
		}
		catch (KommetException e)
		{
			this.renderingStoppedDueToErrors = true;
			return exitWithTagError("Error initializing objectDetails tag: " + e.getMessage());
		}
		
		if (TagMode.EDIT.equals(this.mode) && !StringUtils.hasText(this.fieldNamePrefix))
		{
			this.fieldNamePrefix = StandardObjectController.FORM_FIELD_PREFIX + ".";
		}
		
		if (TagMode.EDIT.equals(this.mode))
		{
			this.parentForm = (FormTag)ObjectDetailsTag.findAncestorWithClass(this, FormTag.class);
			
			if (this.parentForm == null)
			{
				// add a mock form
				generateMockWrapperForm();
			}
		}
		
		return EVAL_BODY_INCLUDE;
    }
	
	private void generateMockWrapperForm()
	{
		this.mockWrapperForm = new MockWrapperForm();
		this.mockWrapperForm.setId("form" + MiscUtils.getHash(10));
		this.mockWrapperForm.setFormStartCode("<form id=\"" + this.mockWrapperForm.getId() + "\" method=\"POST\">");
		this.mockWrapperForm.setFormEndCode("</form>");
	}

	private void getInheritedMode() throws KommetException
	{
		String sMode = (String)getPageData().getValue(StandardObjectController.MODE_PARAM);
		if (sMode != null)
		{
			setMode(sMode);
		}
	}

	private static String getBtnPanel(ButtonPanel buttonPanel, KID recordId, I18nDictionary i18n, String contextPath, ViewWrapperTag viewWrapper, AuthData authData, EnvData env) throws KeyPrefixException, KommetException
	{
		// start buttons panel
		StringBuilder code = new StringBuilder("<div style=\"margin: 20px 0 20px 0; text-align: left\"");
		
		String buttonsClass = "km-object-details-btns";
		
		String additionalClass = viewWrapper.getUserCascadeHierarchyService().getUserSettingAsString(UserSettingKeys.KM_BUTTONS_SECTION_CLASS, authData, AuthData.getRootAuthData(env), env);
		if (StringUtils.hasText(additionalClass))
		{
			buttonsClass += " " + additionalClass;
		}
		
		code.append(" class=\"").append(buttonsClass).append("\">");
		
		for (ButtonPrototype btn : buttonPanel.getButtons())
		{
			if (btn instanceof Button)
			{
				code.append(((Button)btn).getCode());
			}
			else
			{
				code.append(ButtonFactory.getButton(btn.getType(), recordId, null, null, i18n, contextPath).getCode());
			}
		}
		// end buttons panel
		code.append("</div>");
		return code.toString();
	}

	@Override
    public int doEndTag() throws JspException
    {	
		if (this.renderingStoppedDueToErrors)
		{
			this.renderingStoppedDueToErrors = false;
			return EVAL_PAGE;
		}
		
		// main tab code is the code of the first tab (if object details tag is rendered with multiple tabs)
		StringBuilder mainTabCode = new StringBuilder();
		
		if (this.mockWrapperForm != null)
		{
			// wrap in a form tag
			mainTabCode.append(this.mockWrapperForm.getFormStartCode());
		}
		
		mainTabCode.append("<div class=\"object-detail-main-panel\">");
		
		// include javascript utils for object details tag
		mainTabCode.append("<script type=\"text/javascript\" src=\"").append(pageContext.getServletContext().getContextPath()).append("/resources/js/object-details.js\"></script>");
		//mainTabCode.append("<script type=\"text/javascript\" src=\"").append(pageContext.getServletContext().getContextPath()).append("/resources/km/js/km.filelookup.js\"></script>");
		
		String fieldLayout = null;
		
		try
		{
			if (displayMessages)
			{
				// Display action and error messages, if there are any. The reason why messages are displayed here, not
				// by a separate km:errors/messages tag, is that we want to put them within the object detail ibox.
				mainTabCode.append(getMessages());
			}
			
			// render hidden fields storing information about association to be created
			if (StringUtils.hasText((String)getPageData().getValue(StandardObjectController.ASSOC_FIELD_PARAM)) && StringUtils.hasText((String)getPageData().getValue(StandardObjectController.ASSOC_PARENT_PARAM)))
			{
				// add field for association field
				mainTabCode.append("<input type=\"hidden\" name=\"").append(StandardObjectController.ASSOC_FIELD_PARAM).append("\" ");
				mainTabCode.append(" value=\"").append(getPageData().getValue(StandardObjectController.ASSOC_FIELD_PARAM)).append("\" />");
				
				// add field for association parent record
				mainTabCode.append("<input type=\"hidden\" name=\"").append(StandardObjectController.ASSOC_PARENT_PARAM).append("\" ");
				mainTabCode.append(" value=\"").append(getPageData().getValue(StandardObjectController.ASSOC_PARENT_PARAM)).append("\" />");
			}
			
			// delete button is shown only in view mode because it includes a form, and edit mode also includes
			// a form, and forms cannot be nested, so if we wanted to enable 'delete' for edit mode, we'd 
			// have to use the same form
			boolean canDelete = !isMock && !TagMode.EDIT.equals(this.mode) && authData.canDeleteType(type.getKID(), false, getEnv());
			
			I18nDictionary i18n = parentView.getI18n().getDictionary(authData.getUser().getLocaleSetting());
			KmParamNode rmParams = getPageData().getRmParams();
			
			// add back to list link
			//String backUrl = this.pageContext.getServletContext().getContextPath() + "/" + type.getKeyPrefix();
			//mainTabCode.append("<div class=\"upper-links\">");
			//mainTabCode.append("<span class=\"km-back-icon-span\"><img src=\"").append(this.pageContext.getServletContext().getContextPath()).append("/resources/images/list.png\"></img></span>");
			//mainTabCode.append("<a href=\"").append(backUrl).append("\">").append(type.getLabel()).append(" - ").append(i18n.get("back.to.record.list.post")).append("</a></div>");
			
			if (Boolean.TRUE.equals(this.renderTitle))
			{
				// add record title
				if (!StringUtils.hasText(this.title) && !isMock)
				{
					this.title = this.record.attemptGetKID() != null ? this.record.getDefaultFieldValue(authData.getLocale()) : i18n.get("msg.newrecord") + " " + type.getInterpretedLabel(authData); 
				}
				mainTabCode.append("<div id=\"rec-title\" class=\"km-title\">").append(this.title).append("</div>");
			}
			
			String lookupId = null;
			
			// if the tag is rendered in lookup mode, create a field to store this information
			// so that it is passed to the save method
			if (rmParams != null)
			{
				if (rmParams.getSingleActionNode("lookup") != null)
				{
					// notify parent window about changed dialog content
					//parentView.appendScript("if (window.parent && window.parent.km.js.scope.resizeEvent) { window.parent.km.js.scope.resizeEvent.notify(); }");
					
					lookupId = ((ShowLookup)rmParams.getSingleActionNode("lookup")).getId();
					mainTabCode.append("<input type=\"hidden\" name=\"rm.lookup\" value=\"").append(lookupId).append("\" />");
					
					// do not render a container box when in lookup mode
					this.renderContainerBox = false;
					
					// if the page is displayed after successful save action in lookup mode
					// close the lookup and assign the new object to the lookup field
					Action afterInsertAction = rmParams.getSingleActionNode("afterinsert");
					if (afterInsertAction != null)
					{
						mainTabCode.append("<script language=\"Javascript\">");
						mainTabCode.append("$(document).ready(function(){ console.log(\"Scope\" + JSON.stringify(window.parent.km.js.scope));");
						
						mainTabCode.append("var normalizedId = km.js.utils.normalizeId('").append(lookupId).append("');");
						mainTabCode.append("var ref = window.parent.km.js.scope.refs[normalizedId];");
						mainTabCode.append("var rel = window.parent.km.js.scope.rels[normalizedId];");
						mainTabCode.append("if (ref) {");
						mainTabCode.append("console.log(\"Closing lookup for type reference\");\n");
						mainTabCode.append("ref.setDisplayValue('").append(record.getDefaultFieldValue(authData.getLocale())).append("');");
						mainTabCode.append("ref.select('").append(record.getKID()).append("');");
						
						// end if (ref)
						mainTabCode.append("}");
						mainTabCode.append("else if (rel) {");
						mainTabCode.append("rel.onItemSelect('").append(record.getKID()).append("');");
						// end if (rel)
						mainTabCode.append("}");
						//mainTabCode.append("window.parent.km.js.ref.selectRefItem('").append(lookupId).append("','").append(record.getKID()).append("','").append(record.getDefaultFieldValue(authData.getLocale())).append("');");
						
						mainTabCode.append("});");
						mainTabCode.append("</script>");
					}
				}
			}
			
			// add standard buttons by default only if no custom button buttons have been specified by the user
			if (!this.buttonPanel.isCustomButtons())
			{
				// render edit button if user can edit this record
				if (this.type != null && !TagMode.EDIT.equals(this.mode) && (authData.canEditAllType(type.getKID(), false, getEnv()) || (authData.canEditType(type.getKID(), false, getEnv()) && getViewWrapper().getSharingService().canEditRecord(record.getKID(), authData.getUserId(), getEnv()))))
				{
					this.buttonPanel.addButton(ButtonFactory.getEditButton(record.getKID(), i18n, this.pageContext.getServletContext().getContextPath()));
				}
				
				// render delete button if user can delete object
				if (canDelete)
				{
					this.buttonPanel.addButton(ButtonFactory.getDeleteButton(record.getKID(), i18n, this.pageContext.getServletContext().getContextPath()));
				}
				
				this.buttonPanel.addButton(ButtonFactory.getListButton(type, i18n, this.pageContext.getServletContext().getContextPath()));
			
				if (TagMode.EDIT.equals(this.mode))
				{
					// add save button
					this.buttonPanel.addButton(ButtonFactory.getSaveButton(record.getType().getKeyPrefix(), parentForm != null ? parentForm.getId() : this.mockWrapperForm.getId(), i18n.get("btn.save"), i18n, this.pageContext.getServletContext().getContextPath()));
					
					Button cancelBtn = null;
					
					// add cancel button
					if (!StringUtils.hasText(lookupId))
					{
						cancelBtn = ButtonFactory.getCancelButton(record.attemptGetKID(), record.getType().getKeyPrefix(), i18n.get("btn.cancel"), i18n, this.pageContext.getServletContext().getContextPath());
					}
					else
					{
						cancelBtn = new Button("<a href=\"javascript:;\" class=\"sbtn\" onclick=\"parent.window.$.closeRialog(); if (parent.window.km.js.ui.dialog) { parent.window.km.js.ui.dialog.closeAllDialogs(); }\">" + parentView.getI18n().get(authData.getUser().getLocaleSetting(), "btn.cancel") + "</a>");
					}
					
					buttonPanel.addButton(cancelBtn);
				}
				else
				{
					// add user defined buttons to view mode
					List<kommet.basic.Button> customButtons = getEnv().getTypeCustomButtons(type.getKID());
					for (kommet.basic.Button btn : customButtons)
					{	
						boolean displayButton = true;
						
						if (StringUtils.hasText(btn.getDisplayCondition()))
						{
							// TODO evaluate the display condition and set displayButton flag based on the result
							// the condition below is a mock condition now and should be changed to handle more complex expressions
							Field field = type.getField(btn.getDisplayCondition());
							if (field != null)
							{
								Object val = record.getField(btn.getDisplayCondition());
								displayButton = Boolean.TRUE.equals(val);
							}
						}
						
						if (displayButton)
						{
							buttonPanel.addButton(Button.fromCustomButton(btn, record, getPageContext().getServletContext().getContextPath(), authData, getEnv()));
						}
					}
				}
			}
			
			if (this.tabs != null && TabStyle.INSIDE.equals(tabStyle))
			{
				// add a tab link to every related list
				for (Tab relatedList : this.tabs)
				{
					this.buttonPanel.addButton(getTabLink(relatedList.getTitle(), relatedList.getId(), this.pageContext));
				}
			}
			
			fieldLayout = getViewWrapper().getUserCascadeHierarchyService().getUserSettingAsString(UserSettingKeys.KM_SYS_FIELD_LAYOUT + "." + type.getKID(), authData, AuthData.getRootAuthData(getEnv()), getEnv());
			
			// add button for editing the layout for admins
			if (AuthUtil.isSysAdminOrRoot(authData))
			{	
				if (fieldLayout == null)
				{	
					fieldLayout = getDefaultLayout(type);
				}
				
				Button customizeBtn = new Button("<a href=\"javascript:;\" class=\"sbtn\" onclick='km.js.utils.customizableObjectDetails({ target: $(\"div.object-detail-main-panel\"), layout: " + fieldLayout + ", typeName: \"" + type.getQualifiedName() + "\" })'>" + parentView.getI18n().get(authData.getUser().getLocaleSetting(), "btn.customizeObjectDetails") + "</a>");
				buttonPanel.addButton(customizeBtn);
			}
			
			// render button panel
			mainTabCode.append(getBtnPanel(buttonPanel, this.record.attemptGetKID(), authData.getI18n(), this.pageContext.getServletContext().getContextPath(), getViewWrapper(), authData, getEnv()));
		}
		catch (KommetException e)
		{
			cleanUp();
			return exitWithTagError("Error initializing objectDetails tag: " + e.getMessage());
		}
		
		mainTabCode.append("<div id=\"warnPrompt\" style=\"margin:1em 0\"></div>");
		
		try
		{	
			// Render output fields only if they have not rendered themselves or have been rendered
			// by a property table inside this object details tag. To find this out, just check if this tag contains
			// a propertyTable tag
			if (this.innerPropertyTable == null)
			{
				// if no fields were contained within the objectDetails tag, get all readable fields from type
				if (this.fields.isEmpty() && !isMock)
				{
					List<NestedContextField> fieldsForDisplay = DataAccessUtil.getReadableFields(type, authData, null, getEnv());
					AppConfig config = getViewWrapper().getAppConfig();
					
					if (jsRender)
					{
						List<String> fieldNames = new ArrayList<String>();
						
						for (NestedContextField contextField : fieldsForDisplay)
						{
							fieldNames.add(contextField.getNestedName());
						}
						
						// render the object form using javascript rm.objectdetails library	
						String query = "select " + MiscUtils.implode(fieldNames, ", ") + " FROM " + type.getQualifiedName() + " where " + Field.ID_FIELD_NAME + " = '" + this.record.getKID() + "'";
						
						parentView.appendScript(getObjectDetailsRenderScript(fieldLayout, "$(\".object-detail-main-panel\")", query, this.record, type, this.mode.name().toLowerCase(), getAuthData()));
					}
					else
					{	
						// check setting for displaying collections
						boolean isDisplayCollections = getViewWrapper().getUserCascadeHierarchyService().getUserSettingAsBoolean(UserSettingKeys.KM_SYS_DISPLAY_COLLECTIONS_IN_RECORD_DETAILS, authData, AuthData.getRootAuthData(getEnv()), getEnv());
						
						for (NestedContextField contextNameField : fieldsForDisplay)
						{
							Field field = contextNameField.getField();
							
							if (config.isRenderOnlyRequiredFieldsOnObjectDetails() && !field.isRequired())
							{
								// if only required fields should be rendered and this field is not required so skip it
								continue;
							}
							
							// hide system fields
							if (Field.isSystemField(field.getApiName()))
							{
								continue;
							}
							
							// Do not display the following fields in edit mode:
							// - formula fields, because their value cannot be set explicitly
							// - collection fields
							if (TagMode.EDIT.equals(this.mode) && (field.getDataTypeId().equals(DataType.FORMULA) || field.getDataType().isCollection()))
							{
								continue;
							}
							
							// check if collections should be rendered as inline fields
							if (field.getDataType().isCollection())
							{
								// do not display collections for unsaved records
								if  (!isDisplayCollections || record.attemptGetKID() == null)
								{
									continue;
								}
								
								String collectionDisplay = getViewWrapper().getUserCascadeHierarchyService().getUserSettingAsString(UserSettingKeys.KM_SYS_COLLECTION_DISPLAY_MODE + "." + field.getKID(), getAuthData(), AuthData.getRootAuthData(getEnv()), getEnv());
								
								if (collectionDisplay == null)
								{
									collectionDisplay = config.getDefaultCollectionDisplay();
								}
								
								if (collectionDisplay.equals("tab"))
								{
									// prepare configuration for the related list
									
									// by default display only the default field
									Field defaultField = ((CollectionDataType)field.getDataType()).getCollectionType().getDefaultField();
									Set<String> queriedFields = MiscUtils.toSet(defaultField.getApiName());
									
									List<ListColumn> columns = new ArrayList<ListColumn>();
									ListColumn defaultFieldColumn = new ListColumn();
									defaultFieldColumn.setField(defaultField.getApiName());
									defaultFieldColumn.setLabel(defaultField.getInterpretedLabel(authData));
									defaultFieldColumn.setLink(true);
									columns.add(defaultFieldColumn);
									
									// add tab to object details tag
									addTab(RelatedListTag.getRelatedListTab(field, "tab-" + MiscUtils.getHash(5), columns, queriedFields, record, field.getInterpretedLabel(authData), 25, this, getEnv()));
									
									// do not process further because it would result in the collection being displayed as an inline field as well
									continue;
								}
								else
								{
									// do nothing - this will display the field as normal inline list
								}
							}
							
							if (TagMode.EDIT.equals(this.mode))
							{
								if (Field.isSystemField(field.getApiName()) ||  !authData.canEditField(field, false, getEnv()) || field.getDataTypeId().equals(DataType.AUTO_NUMBER))
								{
									continue;
								}
								
								InputFieldTag inputField = new InputFieldTag();
								inputField.setName(field.getApiName());
								inputField.setId(!field.getDataTypeId().equals(DataType.TYPE_REFERENCE) ? field.getApiName() : field.getApiName() + "_" + Field.ID_FIELD_NAME);
								this.fields.add(inputField);
							}
							else
							{
								OutputFieldTag outputField = new OutputFieldTag();
								outputField.setName(field.getApiName());	
								this.fields.add(outputField);
							}
						}
					}
				}
				
				if (!jsRender)
				{
					// start property table
					mainTabCode.append(PropertyTableTag.getStartTagCode(null, this.mode.equals(TagMode.VIEW) ? "km-rd-table-mobile-inline-labels-view" : null, null));
					// get field list code
					mainTabCode.append(PropertyTableTag.getInnerCodeForFields(fields, this, 2, this.failOnUninitializedFields, parentView, pageContext));
					// end property table
					mainTabCode.append(PropertyTableTag.getEndTagCode());
				}
			}
			else
			{
				if (StringUtils.hasText(this.innerPropertyTableCode))
				{
					mainTabCode.append(this.innerPropertyTableCode);
				}
				else
				{
					cleanUp();
					return exitWithTagError("Property table tag is used inside object detail tag, but the code it rendered is empty");
				}
			}
			
			if (TagMode.EDIT.equals(this.mode))
			{
				// if ID field has not been rendered, but the ID is not null
				// we want to have it on the form as hidden field so that edit action knows
				// what record is being updated
				if (!idFieldRendered && this.record.attemptGetKID() != null)
				{
					mainTabCode.append(HiddenFieldTag.getCode(this.record.attemptGetKID(), Field.ID_FIELD_NAME, this.fieldNamePrefix, parentView.nextComponentId()));
				}
				
				// also add information about the source view
				mainTabCode.append("<input type=\"hidden\" name=\"sourceViewId\" value=\"").append(getPageData().getViewId()).append("\" />");
			}
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			return exitWithTagError("Error displaying record details: " + e.getMessage());
		}
		
		mainTabCode.append("</div>");
		
		if (this.mockWrapperForm != null)
		{
			mainTabCode.append(this.mockWrapperForm.getFormEndCode());
		}
		
		List<String> tabDefinitions = new ArrayList<String>();
		StringBuilder tabCode = new StringBuilder();
		List<String> tabInitScripts = new ArrayList<String>();
		
		// render other tabs if they are defined
		if (this.tabs != null)
		{	
			int tabIndex = 1;
			for (Tab tab : this.tabs)
			{
				if (TabStyle.TOP.equals(tab.getTabStyle()))
				{
					String tabId = "tab_" + tabIndex + "_" + this.id;
					
					// wrap tab code in a wrapper
					tabCode.append("<div id=\"" + tabId + "\">" +tab.getCode() + "</div>");
					
					// create javascript tab definition
					tabDefinitions.add("{ label: \"" + tab.getTitle() + "\", content: $(\"#" + tabId + "\") }");
					
					if (StringUtils.hasText(tab.getAfterRenderCallback()))
					{
						tabInitScripts.add(tab.getAfterRenderCallback());
					}
				}
				else
				{
					tabCode.append(tab.getCode());
				}
				tabIndex++;
			}
		}
		
		if (!tabInitScripts.isEmpty())
		{
			if (this.tabsConfig == null)
			{
				try
				{
					this.tabsConfig = new TabConfigTag();
				}
				catch (KommetException e)
				{
					throw new JspException("Error displaying record details: " + e.getMessage());
				}
			}
			
			// append after render script
			tabsConfig.setAfterRender("function() { " + (StringUtils.hasText(tabsConfig.getAfterRender()) ? tabsConfig.getAfterRender() + ";" : "") + MiscUtils.implode(tabInitScripts, "\n") + "}");
		}
		
		if (tabDefinitions.isEmpty())
		{
			if (this.renderContainerBox)
			{
				mainTabCode.insert(0, "<div class=\"ibox\">");
				mainTabCode.append("</div>");
			}
			else
			{
				mainTabCode.insert(0, "<div class=\"km-font-scale\">");
				mainTabCode.append("</div>");
			}
			
			writeToPage(mainTabCode.toString());
			
			// add function for showing related lists
			parentView.appendScript("function showTab(id) { $(\".related-list\").hide(); $(\"#\" + id).show(); }");
		}
		else
		{
			mainTabCode.insert(0, "<div id=\"tab_0_" + this.id + "\">");
			mainTabCode.append("</div>");
			tabCode.insert(0, mainTabCode);
			
			Integer tabsComponentId = (new Random()).nextInt(1000);
			String containerId = "tab_container_" + tabsComponentId;
			
			// add container that will hold the currently displayed tab
			tabCode.insert(0, "<div id=\"" + containerId + "\"></div>");
			
			// wrap main panel in tab
			String mainTabDef = "{ label: \"" + this.title + "\", content: $(\"#tab_0_" + this.id + "\") }";
			tabDefinitions.add(0, mainTabDef);
			
			StringBuilder tabInit = new StringBuilder();
			tabInit.append("$(document).ready(function() {");
			
			String tabVar = null;
			
			if (this.tabsConfig != null && StringUtils.hasText(this.tabsConfig.getVar()))
			{
				// if km.js.tabs is stored in a user-defined var, we will make this var global because we assume
				// that if user specified this var name, they will want to access it outside of the function
				tabVar = this.tabsConfig.getVar();
			}
			else
			{
				tabVar = "tabs_" + tabsComponentId;
				
				// declare as non-global
				tabInit.append("var ").append(tabVar).append(" = null;");
			}
			
			tabInit.append(tabVar).append(" = km.js.tabs.create({ tabs: [");
			tabInit.append(MiscUtils.implode(tabDefinitions, ", ")).append("], originalContentHandling: \"remove\"");
			
			if (this.tabsConfig != null && StringUtils.hasText(this.tabsConfig.getAfterRender()))
			{
				tabInit.append(", afterRender: ").append(this.tabsConfig.getAfterRender());
			}
			
			tabInit.append("});\n");
			
			// when tabs are rendered, append them to the container
			tabInit.append(tabVar).append(".render(function(code) { $(\"#").append(containerId).append("\").append(code); });\n");
			
			// open first tab by default
			tabInit.append(tabVar).append(".open(0);");
			
			// end the document.ready function
			tabInit.append("});");
			
			parentView.appendScript(tabInit.toString());
			
			// render all tab content
			writeToPage(tabCode.toString());
		}
		
		// pass any errors to the parent tag
		// TODO prevent the content of the page from being rendered when errors appear (do this in View Tag)
		// and do not process this details tag at all when there are errors
		parentView.addErrorMsgs(getErrorMsgs());
		
		// add breadcrumb to session
		try
		{
			if (this.mode.equals(TagMode.VIEW))
			{
				Breadcrumbs.add(this.getPageData().getRequestURL(), getRecord().getDefaultFieldValue(authData.getLocale()), getViewWrapper().getAppConfig().getBreadcrumbMax(), this.pageContext.getSession());
			}
		}
		catch (KommetException e1)
		{
			return exitWithTagError("Could not render breadcrumbs due to an error in configuration");
		}
		
		cleanUp();
		
		return EVAL_PAGE;
    }
	
	/*private void applyUserLayout(String fieldLayout) throws KommetException
	{
		if (this.innerPropertyTable != null || !this.fields.isEmpty())
		{
			// if field layout is defined explicitly in km:tags, use that layout, not the one from user settings
			return;
		}
		
		HashMap<String, Object> layout = null;
		
		// generate property table from field layout
		try
		{
			layout = JSON.parseToMap(fieldLayout);
		}
		catch (JsonDeserializationException e)
		{
			e.printStackTrace();
			throw new KommetException("Error deserializing field layout: " + e.getMessage());
		}
		
		this.innerPropertyTable = new PropertyTableTag();
		
		ArrayList<HashMap<String, Object>> sections = (ArrayList<HashMap<String, Object>>)layout.get("sections");
		
		// iterate over sections
		for (HashMap<String, Object> section : sections)
		{
			SectionTag sectionTag = new SectionTag();
			
			ArrayList<HashMap<String, Object>> rows = (ArrayList<HashMap<String, Object>>)section.get("rows");
			
			// iterate over rows
			for (HashMap<String, Object> row : rows)
			{
				PropertyRowTag rowTag = new PropertyRowTag();
				
				sectionTag.addChild(rowTag);
				
				ArrayList<HashMap<String, Object>> fields = (ArrayList<HashMap<String, Object>>)row.get("fields");
				
				// iterate over fields
				for (HashMap<String, Object> field : fields)
				{
					PropertyTag propertyTag = new PropertyTag();
					
					try
					{
						PropertyLabelTag labelTag = new PropertyLabelTag();
						//labelTag.set
						propertyTag.addChild(labelTag);
					}
					catch (Exception e)
					{
						throw new KommetException("Error generating propertyLabel tag");
					}
					
					FieldTag fieldTag = new FieldTag();
					fieldTag.setName((String)field.get("name"));
					
					PropertyValueTag valueTag = new PropertyValueTag();
					valueTag.addChild(fieldTag);
					
					propertyTag.addChild(valueTag);
					
					rowTag.addChild(propertyTag);
				}
			}
			
			this.innerPropertyTable.addChild(sectionTag);
		}
	}*/

	private String getObjectDetailsRenderScript(String fieldLayout, String target, String query, Record record, Type type, String mode, AuthData authData) throws KommetException
	{
		StringBuilder code = new StringBuilder();
		
		// add record title
		String title = record.getDefaultFieldValue(authData.getLocale());
		
		StringBuilder btnPanel = new StringBuilder();
		btnPanel.append("var btnPanel = km.js.buttonpanel.create({});");
		
		if (TagMode.VIEW.equals(this.mode))
		{
			btnPanel.append("btnPanel.addButton({ label: km.js.config.i18n[\"btn.edit\"] })");
		}
		else
		{
			btnPanel.append("btnPanel.addButton({ label: km.js.config.i18n[\"btn.save\"] })");
			btnPanel.append("btnPanel.addButton({ label: km.js.config.i18n[\"btn.cancel\"] })");
		}
		
		code.append(btnPanel).append(";\n");
		code.append("km.js.objectdetails.renderRecord({ query: \"" + query + "\", typeName: \"" + type.getQualifiedName() + "\", layout: " + fieldLayout + ", target: " + target + ", mode: \"" + mode + "\", buttonPanel: btnPanel, title: \"" + title + "\" });");
		
		return code.toString();
	}

	private String getDefaultLayout(Type type)
	{
		// no layout is defined, so we use standard field layout
		List<String> rows = new ArrayList<String>();
		
		List<Field> allFields = new ArrayList<Field>();
		allFields.addAll(type.getFields());
		
		for (int i = 0; i < allFields.size(); i++)
		{
			String row = "{ fields: [ ";
			
			Field field = allFields.get(i++);
			row += "{ \"name\": \"" + field.getApiName() + "\", \"label\": \"" + field.getLabel() + "\" }";
			
			if (i < allFields.size())
			{
				field = allFields.get(i);
				row += ", { \"name\": \"" + field.getApiName() + "\", \"label\": \"" + field.getLabel() + "\" }";
			}
			
			row += " ] }";
			
			rows.add(row);
		}
		
		return "{ sections: [ { rows: [ " + MiscUtils.implode(rows, ", ") + " ] } ] }";
	}

	@Override
	protected void cleanUp()
	{
		super.cleanUp();
		this.type = null;
		this.authData = null;
		this.tabs = null;
		this.fields = null;
		this.innerPropertyTable = null;
		this.fieldNamePrefix = null;
		this.idFieldRendered = false;
		this.displayMessages = true;
		this.buttonPanel = null;
		this.title = null;
		this.renderContainerBox = true;
		this.tabStyle = null;
		this.parentView = null;
		this.renderTitle = true;
		this.isMock = false;
		this.tabsConfig = null;
		this.mode = null;
		this.mockWrapperForm = null;
		this.failOnUninitializedFields = null;
		this.jsRender = false;
		
		// make sure not to clean this var here
		// this.renderingStoppedDueToErrors = false;
		clearErrorMessages();
	}

	/**
	 * Checks if there are any action or error messages to display, and if there are, returns their HTML code.
	 * @return
	 * @throws KommetException
	 */
	@SuppressWarnings("unchecked")
	private String getMessages() throws KommetException
	{
		StringBuilder code = new StringBuilder();
		PageData pageData = getPageData();
		if (pageData == null)
		{
			throw new KommetException("Page data not available in object details tag");
		}
		
		// get action messages
		List<String> msgs = (ArrayList<String>)pageData.getValue(PageData.ACTION_MSGS_KEY);
		code.append(ActionMessagesTag.getCode(msgs, this.pageContext.getServletContext().getContextPath(), getViewWrapper().getAppConfig().getMaxMessagesDisplayed(), getAuthData().getI18n()));
		
		// get error messages
		msgs = (ArrayList<String>)pageData.getValue(PageData.ERROR_MSGS_KEY);
		code.append(ErrorMessagesTag.getCode(msgs, this.pageContext.getServletContext().getContextPath(), getViewWrapper().getAppConfig().getMaxMessagesDisplayed(), getAuthData().getI18n()));
		
		return code.toString();
	}

	private Button getTabLink (String title, String id, PageContext pageContext)
	{
		StringBuilder code = new StringBuilder("<div class=\"rel-list-tab\" onclick=\"showTab('").append(id).append("');document.location = (document.location.href.split('#')[0]) + '#anchor-").append(id).append("';return false;\">");
		code.append("<span>").append(title);
		code.append("</span><img src=\"").append(pageContext.getServletContext().getContextPath()).append("/resources/images/list.png\">");
		code.append("</div>");
		return new Button(code.toString());
	}

	public void setRecord(Record record)
	{
		this.record = record;
	}
	
	public Record getRecord()
	{
		return record;
	}
	
	public Type getType()
	{
		return this.type;
	}

	public AuthData getAuthData()
	{
		return authData;
	}
	
	public void addTab (String title, String code, String target, String afterRenderCallback, String id, TabStyle style)
	{
		if (this.tabs == null)
		{
			this.tabs = new ArrayList<Tab>();
		}
		this.tabs.add(new Tab(title, code, target, afterRenderCallback, id, style != null ? style : this.tabStyle));
	}
	
	public void addTab (Tab tab)
	{
		if (this.tabs == null)
		{
			this.tabs = new ArrayList<Tab>();
		}
		this.tabs.add(tab);
	}
	
	public void addTab (kommet.basic.keetle.tags.tabs.Tab tab)
	{
		this.addTab(new Tab(tab.getName(), tab.getContent(), null, TabStyle.TOP));
	}
	
	public void addTab (String title, String code, String id, TabStyle style)
	{
		if (this.tabs == null)
		{
			this.tabs = new ArrayList<Tab>();
		}
		this.tabs.add(new Tab(title, code, id, style != null ? style : this.tabStyle));
	}
	
	public void addField (FieldTag tag)
	{
		this.fields.add(tag);
	}
	
	public PropertyTableTag getInnerPropertyTable()
	{
		return innerPropertyTable;
	}
	
	public void addButton (ButtonPrototype button)
	{
		this.buttonPanel.addButton(button);
	}

	public void setInnerPropertyTable(PropertyTableTag innerPropertyTable)
	{
		this.innerPropertyTable = innerPropertyTable;
	}
	
	public Boolean getIdFieldRendered()
	{
		return idFieldRendered;
	}

	public void setIdFieldRendered(Boolean idFieldRendered)
	{
		this.idFieldRendered = idFieldRendered;
	}

	public void setFailOnUninitializedFields(Boolean failOnUninitializedFields)
	{
		this.failOnUninitializedFields = failOnUninitializedFields;
	}

	public Boolean getFailOnUninitializedFields()
	{
		return failOnUninitializedFields;
	}

	public void setParentForm(FormTag parentForm)
	{
		this.parentForm = parentForm;
	}

	public FormTag getParentForm()
	{
		return parentForm;
	}

	public void setMode(String mode) throws KommetException
	{
		this.mode = TagMode.fromString(mode);
	}

	public String getMode()
	{
		return this.mode.stringValue();
	}

	public void setFieldNamePrefix(String fieldNamePrefix)
	{
		this.fieldNamePrefix = fieldNamePrefix;
	}

	public String getFieldNamePrefix()
	{
		return fieldNamePrefix;
	}
	
	public void setDisplayMessages(Boolean displayMessages)
	{
		this.displayMessages = displayMessages;
	}

	public Boolean getDisplayMessages()
	{
		return displayMessages;
	}

	public void setInnerPropertyTableCode(String innerPropertyTableCode)
	{
		this.innerPropertyTableCode = innerPropertyTableCode;
	}

	public String getInnerPropertyTableCode()
	{
		return innerPropertyTableCode;
	}

	class Tab
	{
		private String title;
		private String code;
		private String id;
		private TabStyle tabStyle;
		
		private String target;
		private String afterRenderCallback;
		
		public Tab (String title, String code, String id, TabStyle tabStyle)
		{
			this.title = title;
			this.code = code;
			this.id = id;
			this.tabStyle = tabStyle;
		}
		
		public Tab (String title, String code, String target, String afterRenderCallback, String id, TabStyle tabStyle)
		{
			this.title = title;
			this.target = target;
			this.code = code;
			this.afterRenderCallback = afterRenderCallback;
			this.id = id;
			this.tabStyle = tabStyle;
		}
		
		public String getTitle()
		{
			return title;
		}
		
		public String getCode()
		{
			return code;
		}

		public String getId()
		{
			return id;
		}

		public TabStyle getTabStyle()
		{
			return tabStyle;
		}

		public void setTabStyle(TabStyle tabStyle)
		{
			this.tabStyle = tabStyle;
		}

		public String getAfterRenderCallback()
		{
			return afterRenderCallback;
		}

		public void setAfterRenderCallback(String afterRenderCallback)
		{
			this.afterRenderCallback = afterRenderCallback;
		}

		public String getTarget()
		{
			return target;
		}

		public void setTarget(String target)
		{
			this.target = target;
		}
	}

	public void setButtonPanel(ButtonPanel panel)
	{
		this.buttonPanel = panel;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getTitle()
	{
		return title;
	}

	public Boolean getRenderContainerBox()
	{
		return renderContainerBox;
	}

	public void setRenderContainerBox(Boolean renderContainerBox)
	{
		this.renderContainerBox = renderContainerBox;
	}
	
	public void setTabStyle (String style)
	{
		this.tabStyle = style != null ? TabStyle.valueOf(style.toUpperCase()) : null;
	}
	
	public String getRelatedListCssClass()
	{
		return TabStyle.INSIDE.equals(this.tabStyle) ? "related-list" : "km-tab-content";
	}

	public TabConfigTag getTabsConfig()
	{
		return tabsConfig;
	}

	public void setTabsConfig(TabConfigTag tabsConfig)
	{
		this.tabsConfig = tabsConfig;
	}

	@Override
	public KID getRecordId() throws KommetException
	{
		return this.record != null ? this.record.attemptGetKID() : null;
	}

	public Boolean getRenderTitle()
	{
		return renderTitle;
	}

	public void setRenderTitle(Boolean renderTitle)
	{
		this.renderTitle = renderTitle;
	}
	
	public PageContext getPageContext()
	{
		return this.pageContext;
	}
	
	class MockWrapperForm
	{
		private String formStartCode;
		private String formEndCode;
		private String id;
		
		public String getFormStartCode()
		{
			return formStartCode;
		}
		
		public void setFormStartCode(String formStartCode)
		{
			this.formStartCode = formStartCode;
		}
		
		public String getFormEndCode()
		{
			return formEndCode;
		}
		
		public void setFormEndCode(String formEndCode)
		{
			this.formEndCode = formEndCode;
		}

		public String getId()
		{
			return id;
		}

		public void setId(String id)
		{
			this.id = id;
		}
	}
}
