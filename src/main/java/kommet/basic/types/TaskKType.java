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
import kommet.data.datatypes.DateTimeDataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.KIDDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class TaskKType extends Type
{	
	private static final long serialVersionUID = 3378246427070802573L;
	private static final String LABEL = "Task";
	private static final String PLURAL_LABEL = "Tasks";
	
	public TaskKType()
	{
		super();
	}
	
	public TaskKType(Type userType, Type userGroupType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.TASK_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.TASK_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.TASK_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add title field
		Field titleField = new Field();
		titleField.setApiName("title");
		titleField.setLabel("Title");
		titleField.setDataType(new TextDataType(1000));
		titleField.setDbColumn("title");
		titleField.setRequired(true);
		this.addField(titleField);
		
		Field contentField = new Field();
		contentField.setApiName("content");
		contentField.setLabel("Content");
		contentField.setDataType(new TextDataType(10000));
		contentField.setDbColumn("content");
		contentField.setRequired(false);
		this.addField(contentField);
		
		Field dueDateField = new Field();
		dueDateField.setApiName("dueDate");
		dueDateField.setLabel("Due Date");
		dueDateField.setDataType(new DateTimeDataType());
		dueDateField.setDbColumn("duedate");
		dueDateField.setRequired(false);
		this.addField(dueDateField);
		
		Field recordIdField = new Field();
		recordIdField.setApiName("recordId");
		recordIdField.setLabel("Record ID");
		recordIdField.setDataType(new KIDDataType());
		recordIdField.setDbColumn("recordid");
		recordIdField.setRequired(false);
		this.addField(recordIdField);
		
		Field statusField = new Field();
		statusField.setApiName("status");
		statusField.setLabel("Status");
		statusField.setDataType(new EnumerationDataType("Open\nIn progress\nWaiting for information\nRejected\nResolved"));
		statusField.setDbColumn("status");
		statusField.setRequired(true);
		
		// always track history of status changes
		statusField.setTrackHistory(true);
		
		this.addField(statusField);
		
		Field priorityField = new Field();
		priorityField.setApiName("priority");
		priorityField.setLabel("Priority");
		priorityField.setDataType(new NumberDataType(0, Integer.class));
		priorityField.setDbColumn("priority");
		priorityField.setRequired(true);
		this.addField(priorityField);
		
		Field assignedUserField = new Field();
		assignedUserField.setApiName("assignedUser");
		assignedUserField.setLabel("Assigned User");
		assignedUserField.setDataType(new TypeReference(userType));
		assignedUserField.setDbColumn("assigneduser");
		assignedUserField.setRequired(false);
		this.addField(assignedUserField);
		
		Field assignedGroupField = new Field();
		assignedGroupField.setApiName("assignedGroup");
		assignedGroupField.setLabel("Assigned Group");
		assignedGroupField.setDataType(new TypeReference(userGroupType));
		assignedGroupField.setDbColumn("assignedgroup");
		assignedGroupField.setRequired(false);
		this.addField(assignedGroupField);
		
		Field progressField = new Field();
		progressField.setApiName("progress");
		progressField.setLabel("Progress");
		progressField.setDataType(new NumberDataType(0, Integer.class));
		progressField.setDbColumn("progress");
		progressField.setRequired(false);
		this.addField(progressField);
	}
}
