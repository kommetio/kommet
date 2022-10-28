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
import kommet.data.datatypes.KIDDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class TypeInfoKType extends Type
{
	private static final long serialVersionUID = -5130987518795696849L;
	private static final String LABEL = "Type Info";
	private static final String PLURAL_LABEL = "Type Infos";
	
	public TypeInfoKType() throws KommetException
	{
		super();
	}
	
	public TypeInfoKType(Type controllerType, Type actionType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.TYPE_INFO_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.TYPE_INFO_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.TYPE_INFO_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add type field - references the file for which info is stored in this object
		Field typeField = new Field();
		typeField.setApiName("typeId");
		typeField.setLabel("Type ID");
		typeField.setDataType(new KIDDataType());
		typeField.setDbColumn("typeid");
		typeField.setRequired(true);
		this.addField(typeField);
		
		// add controller field
		Field controllerField = new Field();
		controllerField.setApiName("standardController");
		controllerField.setLabel("Standard Controller");
		controllerField.setDataType(new TypeReference(controllerType));
		controllerField.setDbColumn("standardcontroller");
		controllerField.setRequired(true);
		this.addField(controllerField);
		
		// add list view field
		Field defaultListViewField = new Field();
		defaultListViewField.setApiName("defaultListAction");
		defaultListViewField.setLabel("Default List Action");
		defaultListViewField.setDataType(new TypeReference(actionType));
		defaultListViewField.setDbColumn("defaultlistaction");
		defaultListViewField.setRequired(true);
		this.addField(defaultListViewField);
		
		// add details view field
		Field defaultDetailsViewField = new Field();
		defaultDetailsViewField.setApiName("defaultDetailsAction");
		defaultDetailsViewField.setLabel("Default Details Action");
		defaultDetailsViewField.setDataType(new TypeReference(actionType));
		defaultDetailsViewField.setDbColumn("defaultdetailsaction");
		defaultDetailsViewField.setRequired(true);
		this.addField(defaultDetailsViewField);
		
		// add edit view field
		Field defaultEditViewField = new Field();
		defaultEditViewField.setApiName("defaultEditAction");
		defaultEditViewField.setLabel("Default Edit Action");
		defaultEditViewField.setDataType(new TypeReference(actionType));
		defaultEditViewField.setDbColumn("defaulteditaction");
		defaultEditViewField.setRequired(true);
		this.addField(defaultEditViewField);
		
		// add create view field
		Field defaultCreateViewField = new Field();
		defaultCreateViewField.setApiName("defaultCreateAction");
		defaultCreateViewField.setLabel("Default Create Action");
		defaultCreateViewField.setDataType(new TypeReference(actionType));
		defaultCreateViewField.setDbColumn("defaultcreateaction");
		defaultCreateViewField.setRequired(true);
		this.addField(defaultCreateViewField);
		
		// add save view field
		Field defaultSaveViewField = new Field();
		defaultSaveViewField.setApiName("defaultSaveAction");
		defaultSaveViewField.setLabel("Default Save Action");
		defaultSaveViewField.setDataType(new TypeReference(actionType));
		defaultSaveViewField.setDbColumn("defaultsaveaction");
		defaultSaveViewField.setRequired(true);
		this.addField(defaultSaveViewField);
	}

}
