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
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class AppUrlKType extends Type
{	
	private static final long serialVersionUID = 8101738780200610226L;
	private static final String LABEL = "AppUrl";
	private static final String PLURAL_LABEL = "AppUrls";

	public AppUrlKType()
	{
		super();
	}
	
	public AppUrlKType(Type appType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.APP_URL_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.APP_URL_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.APP_URL_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add user name field
		Field urlField = new Field();
		urlField.setApiName("url");
		urlField.setLabel("URL");
		urlField.setDataType(new TextDataType(100));
		urlField.setDbColumn("url");
		urlField.setRequired(true);
		this.addField(urlField);
		
		Field appField = new Field();
		appField.setApiName("app");
		appField.setLabel("App");
		
		TypeReference appRef = new TypeReference(appType);
		// delete app URLs when apps are removed
		appRef.setCascadeDelete(true);
		appField.setDataType(appRef);
		
		appField.setDbColumn("app");
		appField.setRequired(true);
		this.addField(appField);
	}
}
