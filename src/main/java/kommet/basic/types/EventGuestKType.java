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
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class EventGuestKType extends Type
{
	private static final long serialVersionUID = 348313440506368126L;
	private static final String LABEL = "Event Guest";
	private static final String PLURAL_LABEL = "Event Guests";
	
	public EventGuestKType()
	{
		super();
	}
	
	public EventGuestKType(EventKType eventType, UserKType userType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.EVENT_GUEST_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.EVENT_GUEST_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.EVENT_GUEST_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add name field
		Field responseField = new Field();
		responseField.setApiName("response");
		responseField.setLabel("Response");
		
		EnumerationDataType respEnum = new EnumerationDataType("Yes\nNo\nMaybe");
		respEnum.setValidateValues(true);
		
		responseField.setDataType(respEnum);
		responseField.setDbColumn("response");
		responseField.setRequired(false);
		this.addField(responseField);
		
		// add start date field
		Field responseCommentField = new Field();
		responseCommentField.setApiName("responseComment");
		responseCommentField.setLabel("Response Comment");
		responseCommentField.setDataType(new TextDataType(255));
		responseCommentField.setDbColumn("responsecomment");
		responseCommentField.setRequired(false);
		this.addField(responseCommentField);
		
		Field guestField = new Field();
		guestField.setApiName("guest");
		guestField.setLabel("Guest");
		guestField.setDataType(new TypeReference(userType));
		guestField.setDbColumn("guest");
		guestField.setRequired(true);
		this.addField(guestField);
		
		Field eventField = new Field();
		eventField.setApiName("event");
		eventField.setLabel("Event");
		eventField.setDataType(new TypeReference(eventType));
		eventField.setDbColumn("event");
		eventField.setRequired(true);
		this.addField(eventField);
	}
}

