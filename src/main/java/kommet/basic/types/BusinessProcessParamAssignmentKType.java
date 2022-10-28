/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.types;

import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class BusinessProcessParamAssignmentKType extends Type
{
	private static final long serialVersionUID = 1451515042842995757L;
	private static final String LABEL = "Business Process Param Assignment";
	private static final String PLURAL_LABEL = "Business Process Param Assignments";
	
	public BusinessProcessParamAssignmentKType() throws KommetException
	{
		super();
	}
	
	public BusinessProcessParamAssignmentKType(BusinessProcessKType bpType, BusinessActionInvocationKType bpiType, BusinessProcessInputKType bpInputType, BusinessProcessOutputKType bpOutputType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.BUSINESS_PROCESS_PARAM_ASSIGNMENT_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.BUSINESS_PROCESS_PARAM_ASSIGNMENT_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.BUSINESS_PROCESS_PARAM_ASSIGNMENT_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add business process field
		Field bpField = new Field();
		bpField.setApiName("businessProcess");
		bpField.setLabel("Business Process");
		
		TypeReference bpRef = new TypeReference(bpType);
		bpRef.setCascadeDelete(true);
		bpField.setDataType(bpRef);
		bpField.setDbColumn("businessprocess");
		bpField.setRequired(true);
		this.addField(bpField);
		
		// add source invocation field
		Field sourceActionField = new Field();
		sourceActionField.setApiName("sourceInvocation");
		sourceActionField.setLabel("Source Invocation");
		sourceActionField.setDbColumn("sourceinvocation");
		sourceActionField.setRequired(false);
		
		TypeReference sourceInvocationRef = new TypeReference(bpiType);
		sourceInvocationRef.setCascadeDelete(true);
		sourceActionField.setDataType(sourceInvocationRef);
		this.addField(sourceActionField);
		
		// add target invocation field
		Field targetActionField = new Field();
		targetActionField.setApiName("targetInvocation");
		targetActionField.setLabel("Target Invocation");
		targetActionField.setDbColumn("targetinvocation");
		targetActionField.setRequired(false);
		
		TypeReference targetInvocationRef = new TypeReference(bpiType);
		targetInvocationRef.setCascadeDelete(true);
		targetActionField.setDataType(targetInvocationRef);
		this.addField(targetActionField);
		
		// add source param field
		Field srcParamField = new Field();
		srcParamField.setApiName("sourceParam");
		srcParamField.setLabel("Source Param");
		srcParamField.setDbColumn("sourceparam");
		srcParamField.setRequired(false);
		TypeReference srcParamRef = new TypeReference(bpOutputType);
		srcParamRef.setCascadeDelete(true);
		srcParamField.setDataType(srcParamRef);
		this.addField(srcParamField);
		
		// add source param field
		Field targetParamField = new Field();
		targetParamField.setApiName("targetParam");
		targetParamField.setLabel("Target Param");
		targetParamField.setDbColumn("targetparam");
		targetParamField.setRequired(false);
		TypeReference targetParamRef = new TypeReference(bpInputType);
		targetParamRef.setCascadeDelete(true);
		targetParamField.setDataType(targetParamRef);
		this.addField(targetParamField);
		
		// add process input field
		Field processInputField = new Field();
		processInputField.setApiName("processInput");
		processInputField.setLabel("Process Input");
		processInputField.setDbColumn("processinput");
		processInputField.setRequired(false);
		TypeReference processInputRef = new TypeReference(bpInputType);
		processInputRef.setCascadeDelete(true);
		processInputField.setDataType(processInputRef);
		this.addField(processInputField);
		
		// add process output field
		Field processOutputField = new Field();
		processOutputField.setApiName("processOutput");
		processOutputField.setLabel("Process Output");
		processOutputField.setDbColumn("processoutput");
		processOutputField.setRequired(false);
		TypeReference processOutputRef = new TypeReference(bpOutputType);
		processOutputRef.setCascadeDelete(true);
		processOutputField.setDataType(processOutputRef);
		this.addField(processOutputField);
	}

}
