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
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class TaskDependencyKType extends Type
{	
	private static final long serialVersionUID = 8895676586725136327L;
	private static final String LABEL = "Task Dependency";
	private static final String PLURAL_LABEL = "Task Dependencies";
	
	public TaskDependencyKType()
	{
		super();
	}
	
	public TaskDependencyKType(Type taskType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.TASK_DEPENDENCY_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.TASK_DEPENDENCY_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.TASK_DEPENDENCY_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add parent task field
		Field parentField = new Field();
		parentField.setApiName("parentTask");
		parentField.setLabel("Parent Task");
		parentField.setDataType(new TypeReference(taskType));
		parentField.setDbColumn("parenttask");
		parentField.setRequired(true);
		this.addField(parentField);
		
		// add child task field
		Field childField = new Field();
		childField.setApiName("childTask");
		childField.setLabel("Child Task");
		childField.setDataType(new TypeReference(taskType));
		childField.setDbColumn("childtask");
		childField.setRequired(true);
		this.addField(childField);
		
		// add child task field
		Field typeField = new Field();
		typeField.setApiName("dependencyType");
		typeField.setLabel("Dependency Type");
		
		EnumerationDataType typeDT = new EnumerationDataType("isPart\nRequires");
		typeDT.setValidateValues(true);
		
		typeField.setDataType(typeDT);
		typeField.setDbColumn("dependencytype");
		typeField.setRequired(true);
		this.addField(typeField);
	}
}
