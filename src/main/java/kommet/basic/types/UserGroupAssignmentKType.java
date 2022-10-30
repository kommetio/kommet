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
import kommet.data.datatypes.BooleanDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class UserGroupAssignmentKType extends Type
{
	private static final long serialVersionUID = 4990837953065665136L;
	private static final String LABEL = "User Group Assignment";
	private static final String PLURAL_LABEL = "User Group Assignments";
	
	public UserGroupAssignmentKType() throws KommetException
	{
		super();
	}
	
	public UserGroupAssignmentKType(UserKType userType, UserGroupKType userGroupType) throws KommetException
	{
		super();
		
		this.setApiName(SystemTypes.USER_GROUP_ASSIGNMENT_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.USER_GROUP_ASSIGNMENT_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.USER_GROUP_ASSIGNMENT_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		Field parentField = new Field();
		parentField.setApiName("parentGroup");
		parentField.setLabel("Parent Group");
		TypeReference parentGroupDT = new TypeReference(userGroupType);
		parentGroupDT.setCascadeDelete(true);
		parentField.setDataType(parentGroupDT);
		parentField.setDbColumn("parentgroup");
		parentField.setRequired(true);
		this.addField(parentField);
		
		Field childGroupField = new Field();
		childGroupField.setApiName("childGroup");
		childGroupField.setLabel("Child Group");
		TypeReference childGroupDT = new TypeReference(userGroupType);
		childGroupDT.setCascadeDelete(true);
		childGroupField.setDataType(childGroupDT);
		childGroupField.setDbColumn("childgroup");
		childGroupField.setRequired(false);
		this.addField(childGroupField);
		
		Field childUserField = new Field();
		childUserField.setApiName("childUser");
		childUserField.setLabel("Child User");
		TypeReference childUserDT = new TypeReference(userType);
		childUserDT.setCascadeDelete(true);
		childUserField.setDataType(childUserDT);
		childUserField.setDbColumn("childuser");
		childUserField.setRequired(false);
		this.addField(childUserField);
		
		Field rmField = new Field();
		rmField.setApiName("isApplyPending");
		rmField.setLabel("Is Apply Pending");
		rmField.setDataType(new BooleanDataType());
		rmField.setDbColumn("isapplypending");
		rmField.setRequired(false);
		this.addField(rmField);
		
		Field removePendingField = new Field();
		removePendingField.setApiName("isRemovePending");
		removePendingField.setLabel("Is Remove Pending");
		removePendingField.setDataType(new BooleanDataType());
		removePendingField.setDbColumn("isremovepending");
		removePendingField.setRequired(false);
		this.addField(removePendingField);
		
	}

}

