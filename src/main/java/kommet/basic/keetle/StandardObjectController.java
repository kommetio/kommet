/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.ActionErrorException;
import kommet.basic.SystemContextAware;
import kommet.basic.UniqueCheck;
import kommet.basic.UniqueCheckViolationException;
import kommet.basic.keetle.tags.TagMode;
import kommet.basic.keetle.tags.breadcrumbs.Breadcrumbs;
import kommet.data.DataAccessUtil;
import kommet.data.Field;
import kommet.data.FieldValidationException;
import kommet.data.InvalidFieldValueException;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.ValidationErrorType;
import kommet.data.ValidationMessage;
import kommet.data.datatypes.SpecialValue;
import kommet.i18n.I18nDictionary;
import kommet.koll.annotations.Action;
import kommet.koll.annotations.Controller;
import kommet.koll.annotations.Param;
import kommet.systemsettings.SystemSettingKey;
import kommet.transactions.Savepoint;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;
import kommet.web.kmparams.actions.ShowLookup;

/**
 * Standard object controller that will handle all basic operations on type records regardless of
 * their type.
 * @author Radek Krawiec
 */
@Controller
public class StandardObjectController extends BaseController implements SystemContextAware
{
	private KeyPrefix typePrefix;
	
	public static final String FORM_FIELD_PREFIX = "new";
	public static final String FORM_SET_FIELD_PREFIX = "setfield";
	public static final String ASSOC_FIELD_PARAM = "assocfield";
	public static final String ASSOC_PARENT_PARAM = "assocparent";
	public static final String RECORD_VAR_PARAM = "record";
	public static final String MODE_PARAM = "mode";
	
	private KID recordId;
	private Record record;
	
	/**
	 * Map of fields whose value has been changed by user in an edit action.
	 * This map is set in the save() method from request parameters.
	 */
	private Map<String, Object> editedFields;
	
	/**
	 * Map of fields whose value has been preset for the form.
	 * This map is set in the save() method from request parameters.
	 */
  	private Map<String, Object> presetFields;
  	
  	/**
  	 * Fields that will be queried in the details/edit action.
  	 */
  	private Set<String> queriedFields;
  	
  	private Operation operation;
  	
	
	/**
	 * Constructor of the standard controller
	 * @param typePrefix Key prefix of the type for which the constructor is created
	 */
	public StandardObjectController (KeyPrefix typePrefix)
	{
		this.typePrefix = typePrefix;
	}
	
	@Action
	public PageData list() throws KommetException
	{
		clearMessages();
		PageData pd = this.getPageData();
		
		Type type = getEnv().getType(typePrefix);
		
		// inject the type for which the list is displayed
		pd.setValue("type", type);
		pd.setValue("pluralLabel", type.getInterpretedPluralLabel(getAuthData()));
		
		// nullify list links
		Breadcrumbs.setListLink(null, null, getAppConfig().getBreadcrumbMax(), getRequest().getSession());
		
		// add breadcrumbs
		Breadcrumbs.add(getRequest().getRequestURL().toString(), type.getPluralLabel(), getAppConfig().getBreadcrumbMax(), getRequest().getSession());
		
		return pd;
	}
  
  	@Action
	public PageData details(@Param("id") String id) throws KommetException
	{	
  		getPageData().setValue(MODE_PARAM, TagMode.VIEW.stringValue());
  		
		if (!StringUtils.hasText(id))
		{
			addError(getPageData(), "Empty record ID");
			return getPageData();
		}
		
		try
		{
			this.recordId = KID.get(id);
		}
		catch (KIDException e)
		{
			addError(getPageData(), "Invalid ID " + id);
			return getPageData();
		}
		
      	try
        {
          	List<Record> records = getDataService().select(getType(), getAllQueriedFields(), "id = '" + id + "' LIMIT 1", getAuthData(), getEnv());
      		this.record = records.isEmpty() ? null : records.get(0);
      		getPageData().setValue(RECORD_VAR_PARAM, this.record);
        }
      	catch (Exception e)
        {
      		e.printStackTrace();
      		
      		addError(getPageData(), "Error selecting records: " + e.getMessage());
      		
      		// record is not set yet, so we don't want to continue to the object details tag
      		// because it can't handle null records
      		this.getPageData().setViewId(getDefaultErrorViewId());
      		return this.getPageData();
        }
      	
      	getPageData().setValue("recordId", id);
		
      	if (this.record == null)
      	{
      		addError(this.getPageData(), getAuthData().getI18n().get("auth.insufficient.privileges.to.view.record"));
      		getPageData().setViewId(getDefaultErrorViewId());
      	}
      	
      	// always add record list to breadcrumbs
      	//Breadcrumbs.add(getRequest().getRequestURL().toString(), getType().getPluralLabel(), getAppConfig().getBreadcrumbMax(), getRequest().getSession());
      	Breadcrumbs.setListLink(getRequest().getContextPath() + "/" + getType().getKeyPrefix(), getType().getPluralLabel(), getAppConfig().getBreadcrumbMax(), getRequest().getSession());
      	
      	// add breadcrumbs
     	Breadcrumbs.add(getRequest().getRequestURL().toString(), getType().getLabel() + ": " + record.getField(getType().getDefaultFieldApiName()), getAppConfig().getBreadcrumbMax(), getRequest().getSession());
      	
		return getPageData();
	}
  	
  	/**
  	 * Returns a list of API names of fields that will be queried in a given details or edit action.
  	 * The list consists of all readable non-nested properties of the given type, plus fields added by users
  	 * by calling addQueriedField, as long as they are readable to the current user.
  	 * @return
  	 * @throws KommetException
  	 */
  	private Set<String> getAllQueriedFields() throws KommetException
	{
		Set<String> allQueriedFields = new HashSet<String>();
		
		// add fields defined by user
		if (this.queriedFields != null)
		{
			allQueriedFields.addAll(this.queriedFields);
			
			// TODO check if fields are readable
			// The code below does this fine, except that for nested fields method getFieldNamesForDisplay
			// converts their name to non-nested (father.name becomes name)
			
			// convert to list
			/*List<String> queriedFieldList = new ArrayList<String>();
			queriedFieldList.addAll(this.queriedFields);
			
			// filter out non-readable fields
			allQueriedFields.addAll(DataAccessUtil.getFieldsNamesForDisplay(getType(), getAuthData(), queriedFieldList, getEnv()));
			*/
		}
		
		// add all non-nested fields of the type
		allQueriedFields.addAll(DataAccessUtil.getReadableFieldApiNamesForQuery(getType(), getAuthData(), getEnv(), true));
		
		return allQueriedFields;
	}

	@Action
	public PageData delete(@Param("id") String id) throws KommetException
	{
		KID recordId = null;
		
		try
		{
			recordId = KID.get(id);
		}
		catch (KIDException e)
		{
			addActionMessage(getPageData(), "Invalid ID " + id);
			return getPageData();
		}
		
      	try
        {
          	getDataService().deleteRecord(KID.get(id), getAuthData(), getEnv());
        }
      	catch (KommetException e)
        {
      		addActionMessage(getPageData(), "Error deleting records: " + e.getMessage());
        }
      	
      	// TODO add some kind of message after object is deleted
      	// redirect to list of records of this type
      	getPageData().setRedirectURL("/" + UrlUtil.getRecordListUrl(getEnv().getTypeByRecordId(recordId)));
		
		return getPageData();
	}
  
  	@Action
	public PageData create() throws KommetException
	{ 	
  		getPageData().setValue(MODE_PARAM, TagMode.EDIT.stringValue());
  		
      	Record record = new Record(getType());
      	getPageData().setValue(RECORD_VAR_PARAM, record);
  		
      	try
      	{
      		initPassedParams(record);
      		initAssocParams(getPageData());
      	}
	  	catch (KommetException e)
	    {
	  		addActionMessage(getPageData(), "Error creating object: " + e.getMessage());
	  		return getPageData();
	    }
      	
		return getPageData();
	}
  	
  	/**
  	 * Initialized parameters used for presetting association binding on a new record.
  	 * @param data
  	 * @throws KommetException 
  	 */
  	protected void initAssocParams(PageData data) throws KommetException
	{
  		data.setValue(ASSOC_FIELD_PARAM, getParameter(ASSOC_FIELD_PARAM));
  		data.setValue(ASSOC_PARENT_PARAM, getParameter(ASSOC_PARENT_PARAM));
	}

	/**
  	 * Initialized passed parameters on the record.
  	 * <p>Passed parameters are preset on a record, e.g. when a record is created from a related list by clicking
  	 * the "New" button, the parent object will be passed as a parameter to the new child object.
  	 * @param record
  	 * @return
  	 * @throws KommetException
  	 */
  	private Record initPassedParams (Record record) throws KommetException
  	{
  		if (record != null)
  		{
      		for (String param : getParameters().keySet())
          	{
          		if (!param.startsWith("passed."))
          		{
          			continue;
          		}
          		
          		String fieldApiName = param.substring("passed.".length());
          		String value = getParameter(param);
          		record.setField(fieldApiName, value, getEnv());
          	}
  		}
  		
  		return record;
  	}
  	
  	protected PageData edit (Record record) throws KommetException
  	{
  		return edit(record, true);
  	}
  	
  	/**
  	 * Prepares an edit page for the given record and user.
  	 * 
  	 * This is not an action method, so it is not annotated with @Action. However, it is public so that
  	 * users can make use of it in custom controller implementations.
  	 * 
  	 * @param record The record to be edited.
  	 * @param applySharings If set to true, user record sharings will be checked to make sure the current
  	 * user has edit access to the given record.
  	 * @return
  	 * @throws KommetException
  	 */
  	protected PageData edit (Record record, boolean applySharings) throws KommetException
	{
  		getPageData().setValue(MODE_PARAM, TagMode.EDIT.stringValue());
  		
  		if (record == null)
  		{
  			throw new KommetException("Record passed to edit action is null");
  		}
  		
  		this.record = record;
  		this.recordId = record.attemptGetKID();
  		
  		// make sure the user has permission to edit the record
  		if (applySharings && !AuthUtil.canEditRecord(this.recordId, getAuthData(), getSharingService(), getEnv()))
  		{
  			addError(this.getPageData(), getAuthData().getI18n().get("auth.insufficient.privileges.to.edit.object"));
  			this.getPageData().setViewId(getDefaultErrorViewId());
	  		return this.getPageData();
  		}
  		
  		getPageData().setValue("recordId", this.recordId);
		
  		try
      	{
      		initPassedParams(this.record);
      		initAssocParams(this.getPageData());
      	}
	  	catch (KommetException e)
	    {
	  		addActionMessage(this.getPageData(), "Error editing record: " + e.getMessage());
	  		return this.getPageData();
	    }
	  	
  		this.getPageData().setValue(RECORD_VAR_PARAM, this.record);    		
		return this.getPageData();
	}
  	
  	private KID getDefaultErrorViewId() throws KommetException
	{
  		String defaultErrViewId = getSys().getSystemSettingService().getSettingValue(SystemSettingKey.DEFAULT_ERROR_VIEW_ID, getEnv());
		if (defaultErrViewId == null)
		{
			throw new KommetException("Default error view not defined");
		}
		return KID.get(defaultErrViewId);
	}

	@Action
	public PageData edit(@Param("id") String id) throws KommetException
	{
  		return edit(id, true);
	}
  	
	protected PageData edit(String id, boolean applySharings) throws KommetException
	{
  		try
  		{
  			getPageData().setValue("recordId", KID.get(id));
  		}
  		catch (KIDException e)
  		{
  			addError(this.getPageData(), "Invalid record ID " + id);
  			return this.getPageData();
  		}
  		
      	try
        {
      		// get the record by ID
      		List<Record> records = getDataService().select(getType(), getAllQueriedFields(), "id = '" + id + "' LIMIT 1", getAuthData(), getEnv());
      		
      		if (!records.isEmpty())
      		{
      			return edit(records.get(0), applySharings);
      		}
      		else
      		{
      			addError(getPageData(), "Record with ID " + id + " not found");
      		}		
        }
      	catch (KommetException e)
        {
      		e.printStackTrace();
      		addError(getPageData(), "Error selecting records: " + e.getMessage());
        }
		
		return getPageData();
	}
  	
  	/**
  	 * This method initializes a record from the incoming request.
  	 * @throws KommetException 
  	 */
  	protected void initRecordFromRequest() throws KommetException
  	{
  		// Regardless of whether a new record is created, or an existing one edited,
  		// we can operate on an empty record.
  		// This is faster and ensures that only fields explicitly modified in the request
  		// will be updated.
      	this.record = getNewRecord();
      	boolean setUninitializedFieldsToNull;
      
      	String id = getParameter(FORM_FIELD_PREFIX + "." + Field.ID_FIELD_NAME);
      	
      	if (id != null)
        {
      		this.operation = Operation.UPDATE;
      		this.recordId = KID.get(id);
        	this.record.setKID(KID.get(id));
        	
        	// if it's not a new record and the user does not fill in some field, we assume they want to set its value to empty
        	// so we explicitly set it to empty
        	setUninitializedFieldsToNull = true;
        }
      	else
      	{
      		this.operation = Operation.INSERT;
      		
      		// if it's a new record and the user did not fill in some field, we treat it as uninitialized so that default value can be applied to it
      		setUninitializedFieldsToNull = false;
      	}
      	
      	this.editedFields = new HashMap<String, Object>();
      	this.presetFields = new HashMap<String, Object>();
      
      	I18nDictionary i18n = getAuthData().getI18n();
      	
      	// get field values from request parameters and assign them to the record
      	for (String param : getParameters().keySet())
      	{	
      		if (param.startsWith(FORM_FIELD_PREFIX + "."))
      		{
	      		String fieldName = param.substring((FORM_FIELD_PREFIX + ".").length());
	      		Field field = getType().getField(fieldName, getEnv());
	          
	      		// do not direct fields that are auto set, e.g. ID, createdDate etc.
	      		if (!fieldName.contains(".") && field.isAutoSet())
	      		{
	      			if (fieldName.equals(Field.ID_FIELD_NAME))
	      			{
	      				this.recordId = KID.get(((String[])getParameters().get(param))[0]);
	      			}
	      			continue;
	      		}
	  
	      		// read parameter string value
	      		String value = ((String[])getParameters().get(param))[0];
	      		
	      		try
	      		{
	      			Object objValue = null;
	      			
	      			if (value != "")
	      			{
	      				// translate string value into Java object
	      				
	      				try
	      				{
	      					objValue = field.getDataType().getJavaValue(value);
	      				}
	      				catch (Exception e)
	      				{
	      					throw new InvalidFieldValueException(e.getMessage());
	      				}
	      				
	      				editedFields.put(fieldName, objValue);
	      				this.record.setField(fieldName, objValue, getEnv());
	      			}
	      			else
	      			{
	      				if (setUninitializedFieldsToNull)
	      				{
	      					objValue = SpecialValue.NULL;
	      					editedFields.put(fieldName, objValue);
		      				this.record.setField(fieldName, objValue, getEnv());
	      				}
	      				else
	      				{
	      					editedFields.put(fieldName, null);
	      				}
	      			}
	      		}
	      		catch (InvalidFieldValueException e)
	      		{
	      			// TODO use labels rendered by propertyLabel tag in the message below
	      			addError(getPageData(), i18n.get("fielderr.invalidvalue") + " <span style=\"font-weight:bold\">" + field.getInterpretedLabel(getAuthData()) + "</span>");
	      		}
      		}
      		else if (param.startsWith(FORM_SET_FIELD_PREFIX + "."))
      		{
      			// retrieve parameters that are not supposed to be set to the edited object,
      			// but are supposed to be displayed in case anything goes wrong
      			String fieldName = param.substring((FORM_SET_FIELD_PREFIX + ".").length());
	      		Field field = getType().getField(fieldName);
	          
	      		if (!fieldName.contains(".") && field.isAutoSet())
	      		{
	      			continue;
	      		}
	  
	      		String[] paramValues = (String[])getParameters().get(param);
	      		String value = paramValues[0];
	      		Object objValue = value != "" ? field.getDataType().getJavaValue(value) : null;
	      		presetFields.put(fieldName, objValue);
      		}
      	}
      	
      	// set the record on the page data
      	this.pageData.setValue(RECORD_VAR_PARAM, this.record);
  	}
  	
  	@Action
    public PageData save(@Param("sourceViewId") String sourceViewId) throws KommetException
    {
  		return this.save(StringUtils.hasText(sourceViewId) ? KID.get(sourceViewId) : null, true);
    }
  
  	/**
  	 * Saves the current record.
  	 * @param sourceViewId
  	 * @param useTransactions Tells whether transactions should be used in this operation. It is useful to pass false value
  	 * to this parameter and thus skip transactions, if we are calling this method from an overridden controller that calls some
  	 * transactional methods after calling save(). If save() was called with useTransactions = true, then any transactional method
  	 * called afterwards would result in Atomikos error:<p>
  	 * <tt> org.postgresql.xa.PGXAException: suspend/resume not implemented</tt>
  	 * </p>
  	 * @return
  	 * @throws KommetException
  	 */
    protected PageData save(KID sourceViewId, boolean useTransactions) throws KommetException
	{
    	try
    	{
    		initRecordFromRequest();
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    		addError(getPageData(), "Error saving record: " + e.getMessage());
    	}
  		
  		I18nDictionary i18n = getAuthData().getI18n();
  		
      	// create a save point so that save can be rolled back
      	Savepoint savepoint = null;
      	
      	if (useTransactions)
  		{
      		savepoint = getSys().getTransactionManager().getSavepoint();
  		}
      	
      	// call custom before save action
      	this.record = beforeSave();
      
      	try
        {
      		// although there may have been errors above, let the save operation execute because it will provide
      		// information about other errors in other fields that are discovered by validation methods during save
      		this.record = getDataService().save(this.record, getAuthData(), getEnv());
      		
      		// call custom after save action
      		this.record = afterSave();
        }
      	catch (FieldValidationException e)
      	{	
      		// parse validation messages discovered during save
      		for (ValidationMessage msg : e.getMessages())
      		{
      			// add error message in user's language
      			if (msg.getErrorType().equals(ValidationErrorType.FIELD_REQUIRED))
      			{
      				addError(getPageData(), i18n.get("fielderr.required.pre") + " <span style=\"font-weight:bold\">" + msg.getFieldLabel() + "</span> " + i18n.get("fielderr.required.post"));
      			}
      			else if (msg.getErrorType().equals(ValidationErrorType.FIELD_INVALID_VALUE))
      			{
      				addError(getPageData(), i18n.get("fielderr.invalid.pre") + " <span style=\"font-weight:bold\">" + msg.getFieldLabel() + "</span> " + i18n.get("fielderr.invalid.post"));
      			}
      			else if (msg.getErrorType().equals(ValidationErrorType.STRING_VALUE_TOO_LONG))
      			{
      				addError(getPageData(), i18n.get("fielderr.stringvaluetoolong.pre") + " <span style=\"font-weight:bold\">" + msg.getFieldLabel() + "</span> " + i18n.get("fielderr.stringvaluetoolong.post"));
      			}
      			else if (msg.getErrorType().equals(ValidationErrorType.INVALID_EMAIL))
      			{
      				addError(getPageData(), i18n.get("fielderr.invalidemail.pre") + " <span style=\"font-weight:bold\">" + msg.getFieldLabel() + "</span> " + i18n.get("fielderr.invalidemail.post"));
      			}
      			else if (msg.getErrorType().equals(ValidationErrorType.INVALID_ENUM_VALUE))
      			{
      				addError(getPageData(), i18n.get("fielderr.invalidenum.pre") + " <span style=\"font-weight:bold\">" + msg.getFieldLabel() + "</span>");
      			}
      			else
      			{
      				addError(getPageData(), msg.getText());
      			}
      		}
      	}
      	catch (UniqueCheckViolationException e)
      	{
      		UniqueCheck uc = e.getUniqueCheck();
      		String numberSuffix = uc.getParsedFieldIds().size() > 1 ? "pl" : "sing";
  			
      		List<String> fieldNames = new ArrayList<String>();
      		Type type = getEnv().getType(uc.getTypeId());
      		for (KID fieldId : uc.getParsedFieldIds())
      		{
      			fieldNames.add(type.getField(fieldId).getInterpretedLabel(getAuthData()));
      		}
      		
  			addError(getPageData(), i18n.get("fielderr.unique.check.violated.pre." + numberSuffix) + " " + MiscUtils.implode(fieldNames, ", ") + " " + i18n.get("fielderr.unique.check.violated.post." + numberSuffix));
      	}
      	catch (ActionErrorException e)
      	{
      		for (String err : e.getErrors())
      		{
      			addError(getPageData(), err);
      		}
      	}
      	catch (KommetException e)
        {
      		e.printStackTrace();
      		addError(getPageData(), "Error saving record: " + e.getMessage());
        }
      	
      	if (!hasErrorMessages())
      	{
      		try
      		{
      			createAssociation(this.record, getAuthData());
      		}
      		catch (KommetException e)
      		{
      			addError(getPageData(), "Error creating association: " + e.getMessage());
      		}
      	}
      	
      	// if this page was run in look-up mode, this variable will contain the look up ID
      	String lookupId = null;
      	if (getPageData().getRmParams() != null && getPageData().getRmParams().getSingleActionNode("lookup") != null)
      	{
      		lookupId = ((ShowLookup)getPageData().getRmParams().getSingleActionNode("lookup")).getId();
      	}
      	
      	if (hasErrorMessages())
      	{
      		// roll back save
      		if (useTransactions)
      		{
      			savepoint.rollback();
      		}
      		
      		reinitRecord();
      		
      		//refetchedRecord = getDataService().initDefaultFieldsOnNestedProperties(refetchedRecord, getEnv());
      		
      		// if we are going back to edit view, we need to reinitialize association parameters
      		initAssocParams(getPageData());
      		
      		// if this page was run in lookup mode, pass lookup parameters to the result page
      		if (lookupId != null)
      		{
      			//getPageData().setValue("rm.layout", getEnv().getBlankLayoutId());
      			getPageData().setLayoutId(getEnv().getBlankLayoutId());
      			getPageData().setValue("rm.lookup", lookupId);
      		}
      		
      		if (sourceViewId != null)
      		{
      			// override default view
      			getPageData().setViewId(sourceViewId);
      		}
      		
      		// there are errors, so user will be redirected back to the edit form
      		getPageData().setValue(MODE_PARAM, TagMode.EDIT.stringValue());
      		
      		return getPageData();
      	}
      	else
      	{
      		if (useTransactions)
      		{
      			savepoint.release();
      		}
      		
      		// there are no errors, so the user will be taken to view form, but it will happen via redirection, so setting TagMode on the page data is not necessary
      		// getPageData().setValue(MODE_PARAM, TagMode.VIEW.stringValue());
      	}
      	
      	String redirectUrl = "/" + record.getKID().getId();
      	
      	// if save was submitted in lookup mode, pass rm parameters to the result view
      	// that tells it to close
      	if (lookupId != null)
      	{
      		redirectUrl += "?rm.layout=" + getEnv().getBlankLayoutId().getId() + "&rm.lookup=" + lookupId;
      		redirectUrl += this.operation.equals(Operation.UPDATE) ? "&rm.afterupdate" : "&rm.afterinsert";
      	}
      
      	// redirect to object details
      	getPageData().setRedirectURL(redirectUrl);
		
		return getPageData();
	}
  	
  	/**
  	 * This method is just a stub, but it can be overridden by controllers implementing standard controller.
  	 * It can be used to define actions that will be executed in the save() method, after the actual call to the
  	 * database save() method takes place.
  	 * @return The record processed by this method.
  	 */
  	protected Record afterSave() throws KommetException
  	{
  		return this.record;
  	}
  	
  	/**
  	 * This method is just a stub, but it can be overridden by controllers implementing standard controller.
  	 * It can be used to define actions that will be executed in the save() method, just before the actual call to the
  	 * database save() method takes place.
  	 * @return The record processed by this method.
  	 */
  	protected Record beforeSave() throws KommetException
  	{
  		return this.record;
  	}
  	
  	protected void reinitRecord() throws KommetException
	{
  		Record refetchedRecord = null;
  		// If save failed, we need to refetch the original record and rewrite the properties that were changed
  		// on it. We cannot just pass the newly-created record to the details view because it may be missing some
  		// fields.
  		if (this.recordId != null)
  		{
  			List<Record> records = getDataService().select(getType(), DataAccessUtil.getReadableFieldApiNamesForQuery(getType(), getAuthData(), getEnv(), true), "id = '" + this.recordId.getId() + "'", getAuthData(), getEnv());
  			refetchedRecord = records.isEmpty() ? null : records.get(0);
  		
      		if (refetchedRecord == null)
      		{
      			throw new KommetException("Record with ID " + this.recordId.getId() + " not found");
      		}
  		}
  		else
  		{
  			// If the save action was called for a new record, it cannot be fetched from DB because it obviously
  			// does not exist yet. We need to create a new one
  			refetchedRecord = new Record(getType());
  		}
  		
  		// reassigned field values edited by users
  		for (String field : editedFields.keySet())
  		{
  			refetchedRecord.setField(field, editedFields.get(field), getEnv());
  		}
  		
  		// reassigned preset fields
  		for (String field : presetFields.keySet())
  		{
  			refetchedRecord.setField(field, presetFields.get(field), getEnv());
  		}
  		
  		this.record = refetchedRecord;
  		pageData.setValue(RECORD_VAR_PARAM, this.record);
	}
	
	protected void createAssociation(Record record, AuthData authData) throws KommetException
	{ 	
		if (record.attemptGetKID() == null)
		{
			throw new KommetException("Cannot create association link for uninserted record");
		}
		
      	if (!StringUtils.hasText((String)getParameter(StandardObjectController.ASSOC_FIELD_PARAM)) || !StringUtils.hasText((String)getParameter(StandardObjectController.ASSOC_PARENT_PARAM)))
		{
      		return;
		}
      	
      	KID associationParentRecordId = KID.get((String)getParameter(StandardObjectController.ASSOC_PARENT_PARAM));
      	KID assocFieldId = KID.get((String)getParameter(StandardObjectController.ASSOC_FIELD_PARAM));
      	
      	getDataService().associate(assocFieldId, associationParentRecordId, record.getKID(), authData, getEnv());
	}

	/**
	 * Returns the type that this controller represents.
	 * @return type of the controller
	 */
	public Type getType() throws KommetException
	{
		return getEnv().getType(this.typePrefix);
	}
	
	/**
	 * Returns a new instance of the type
	 * @return
	 * @throws KommetException 
	 */
	protected Record getNewRecord() throws KommetException
	{
		return new Record(getType());
	}

	protected KID getRecordId()
	{
		return recordId;
	}

	protected Record getRecord()
	{
		return record;
	}
	
	public void setQueriedFields(Set<String> queriedFields)
	{
		this.queriedFields = queriedFields;
	}

	public Set<String> getQueriedFields()
	{
		return queriedFields;
	}
	
	protected void addQueriedFields (String ... fields)
	{
		if (this.queriedFields == null)
		{
			this.queriedFields = new HashSet<String>();
		}
		
		for (String field : fields)
		{
			this.queriedFields.add(field);
		}
	}

	enum Operation
	{
		INSERT,
		UPDATE
	}
}
