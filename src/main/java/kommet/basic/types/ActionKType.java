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

public class ActionKType extends Type
{	
	private static final long serialVersionUID = 5495882192417830423L;
	private static final String LABEL = "Action";
	private static final String PLURAL_LABEL = "Actions";
	
	public ActionKType()
	{
		super();
	}
	
	public ActionKType(Type viewType, Type classType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.ACTION_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.ACTION_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.ACTION_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add URL field
		Field urlField = new Field();
		urlField.setApiName("url");
		urlField.setLabel("URL");
		urlField.setDataType(new TextDataType(255));
		urlField.setDbColumn("url");
		urlField.setRequired(true);
		this.addField(urlField);
		
		// add name field
		Field nameField = new Field();
		nameField.setApiName("name");
		nameField.setLabel("Name");
		nameField.setDataType(new TextDataType(255));
		nameField.setDbColumn("name");
		nameField.setRequired(true);
		this.addField(nameField);
		
		Field systemField = new Field();
		systemField.setApiName("isSystem");
		systemField.setLabel("Is System");
		systemField.setDataType(new BooleanDataType());
		systemField.setDbColumn("issystem");
		systemField.setRequired(true);
		this.addField(systemField);
		
		Field viewField = new Field();
		viewField.setApiName("view");
		viewField.setLabel("View");
		viewField.setDataType(new TypeReference(viewType));
		viewField.setDbColumn("view");
		viewField.setRequired(true);
		this.addField(viewField);
		
		Field controllerField = new Field();
		controllerField.setApiName("controller");
		controllerField.setLabel("Controller");
		controllerField.setDataType(new TypeReference(classType));
		controllerField.setDbColumn("controller");
		controllerField.setRequired(true);
		this.addField(controllerField);
		
		Field methodField = new Field();
		methodField.setApiName("controllerMethod");
		methodField.setLabel("Controller Method");
		methodField.setDataType(new TextDataType(100));
		methodField.setDbColumn("controllermethod");
		methodField.setRequired(true);
		this.addField(methodField);
		
		// Add type field that is a KID of a type for which this page is a standard page, if any
		Field typeField = new Field();
		typeField.setApiName("typeId");
		typeField.setLabel("Type ID");
		typeField.setDataType(new KIDDataType());
		typeField.setDbColumn("typeid");
		typeField.setRequired(false);
		this.addField(typeField);
		
		Field isPublicField = new Field();
		isPublicField.setApiName("isPublic");
		isPublicField.setLabel("Is Public");
		isPublicField.setDataType(new BooleanDataType());
		isPublicField.setDbColumn("ispublic");
		isPublicField.setRequired(true);
		this.addField(isPublicField);
	}
}
