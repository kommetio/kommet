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

public class BusinessActionTransitionKType extends Type
{
	private static final long serialVersionUID = 8451494397772665292L;
	private static final String LABEL = "Business Action Transition";
	private static final String PLURAL_LABEL = "Business Action Transitions";
	
	public BusinessActionTransitionKType() throws KommetException
	{
		super();
	}
	
	public BusinessActionTransitionKType(BusinessProcessKType bpType, BusinessActionInvocationKType baiType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.BUSINESS_ACTION_TRANSITION_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.BUSINESS_ACTION_TRANSITION_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.BUSINESS_ACTION_TRANSITION_ID_SEQ));
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
		
		// add business process action field
		Field nextField = new Field();
		nextField.setApiName("nextAction");
		nextField.setLabel("Next Action");
		
		TypeReference bpaNextRef = new TypeReference(baiType);
		bpaNextRef.setCascadeDelete(true);
		nextField.setDataType(bpaNextRef);
		nextField.setDbColumn("nextaction");
		nextField.setRequired(true);
		this.addField(nextField);
		
		// add business process action field
		Field prevField = new Field();
		prevField.setApiName("previousAction");
		prevField.setLabel("Previous Action");
		
		TypeReference bpaPrevRef = new TypeReference(baiType);
		bpaPrevRef.setCascadeDelete(true);
		prevField.setDataType(bpaPrevRef);
		prevField.setDbColumn("previousaction");
		prevField.setRequired(true);
		this.addField(prevField);
	}

}
