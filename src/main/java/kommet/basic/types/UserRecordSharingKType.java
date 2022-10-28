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
import kommet.data.datatypes.KIDDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class UserRecordSharingKType extends Type
{
	private static final long serialVersionUID = -2921834943704858566L;
	private static final String LABEL = "User Record Sharing";
	private static final String PLURAL_LABEL = "User Record Sharings";

	public UserRecordSharingKType()
	{
		super();
	}
	
	public UserRecordSharingKType (Type userType) throws KommetException
	{
		this.setApiName(SystemTypes.USER_RECORD_SHARING_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.USER_RECORD_SHARING_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.USER_RECORD_SHARING_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add record ID field
		Field recordIdField = new Field();
		recordIdField.setApiName("recordId");
		recordIdField.setLabel("Record ID");
		recordIdField.setDataType(new KIDDataType());
		recordIdField.setDbColumn("recordid");
		recordIdField.setRequired(true);
		this.addField(recordIdField);
		
		// add user field
		Field userField = new Field();
		userField.setApiName("user");
		userField.setLabel("User");
		userField.setDataType(new TypeReference(userType));
		// "user" is a reserved keyword in postgresql, so we need to use a different column name
		userField.setDbColumn("assignedUser");
		userField.setRequired(true);
		this.addField(userField);
		
		// add isGeneric field
		Field genericField = new Field();
		genericField.setApiName("isGeneric");
		genericField.setLabel("Is Generic");
		genericField.setDataType(new BooleanDataType());
		genericField.setDbColumn("isgeneric");
		genericField.setRequired(true);
		this.addField(genericField);
		
		// add isGeneric field
		Field reasonField = new Field();
		reasonField.setApiName("reason");
		reasonField.setLabel("Reason");
		reasonField.setDataType(new TextDataType(100));
		reasonField.setDbColumn("reason");
		reasonField.setRequired(false);
		this.addField(reasonField);
		
		Field readField = new Field();
		readField.setApiName("read");
		readField.setLabel("Read");
		readField.setDataType(new BooleanDataType());
		readField.setDbColumn("read");
		readField.setRequired(true);
		this.addField(readField);
		
		Field editField = new Field();
		editField.setApiName("edit");
		editField.setLabel("Edit");
		editField.setDataType(new BooleanDataType());
		editField.setDbColumn("edit");
		editField.setRequired(true);
		this.addField(editField);
		
		Field deleteField = new Field();
		deleteField.setApiName("delete");
		deleteField.setLabel("Delete");
		deleteField.setDataType(new BooleanDataType());
		deleteField.setDbColumn("delete");
		deleteField.setRequired(true);
		this.addField(deleteField);
		
		Field ugaField = new Field();
		ugaField.setApiName("userGroupAssignmentId");
		ugaField.setLabel("User Group Assignment ID");
		ugaField.setDataType(new KIDDataType());
		ugaField.setDbColumn("usergroupassignmentid");
		ugaField.setRequired(false);
		this.addField(ugaField);
		
		Field grsField = new Field();
		grsField.setApiName("groupRecordSharingId");
		grsField.setLabel("Group Record Sharing ID");
		grsField.setDataType(new KIDDataType());
		grsField.setDbColumn("grouprecordsharingid");
		grsField.setRequired(false);
		this.addField(grsField);
		
		Field sharingHierarchyField = new Field();
		sharingHierarchyField.setApiName("groupSharingHierarchy");
		sharingHierarchyField.setLabel("Group Sharing Hierarchy");
		sharingHierarchyField.setDataType(new TextDataType(1000));
		sharingHierarchyField.setDbColumn("groupsharinghierarchy");
		sharingHierarchyField.setRequired(false);
		this.addField(sharingHierarchyField);
	}
}
