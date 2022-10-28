/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.types;

import kommet.data.Field;
import kommet.data.FieldValidationException;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.BooleanDataType;
import kommet.data.datatypes.EmailDataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.utils.AppConfig;

public class UserKType extends Type
{
	private static final long serialVersionUID = 422838472262007317L;
	
	private static final String LABEL = "User";
	private static final String PLURAL_LABEL = "Users";
	
	public UserKType()
	{
		super();
	}
	
	public UserKType(Type profileType) throws KommetException
	{
		super();
		this.setApiName(SystemTypes.USER_API_NAME);
		this.setKeyPrefix(KeyPrefix.get(KID.USER_PREFIX));
		this.setKID(KID.get(KID.TYPE_PREFIX, SystemTypes.USER_ID_SEQ));
		this.setLabel(LABEL);
		this.setPluralLabel(PLURAL_LABEL);
		this.setPackage(AppConfig.BASE_TYPE_PACKAGE);
		this.setBasic(true);
		
		// add user name field
		Field usernameField = new Field();
		usernameField.setApiName("userName");
		usernameField.setLabel("Username");
		usernameField.setDataType(new TextDataType(100));
		usernameField.setDbColumn("username");
		usernameField.setRequired(true);
		this.addField(usernameField);
		
		// add email field
		Field emailField = new Field();
		emailField.setApiName("email");
		emailField.setLabel("Email");
		emailField.setDataType(new EmailDataType());
		emailField.setDbColumn("email");
		emailField.setRequired(true);
		this.addField(emailField);
		
		// add password field
		Field passwordField = new Field();
		passwordField.setApiName("password");
		passwordField.setLabel("Password");
		passwordField.setDataType(new TextDataType(255));
		passwordField.setDbColumn("password");
		passwordField.setRequired(false);
		this.addField(passwordField);
		
		// add profile field
		Field profileField = new Field();
		profileField.setApiName("profile");
		profileField.setLabel("Profile");
		profileField.setDataType(new TypeReference(profileType));
		profileField.setDbColumn("profile");
		profileField.setRequired(true);
		this.addField(profileField);
		
		// add timezone field
		Field tzField = new Field();
		tzField.setApiName("timezone");
		tzField.setLabel("Time zone");
		tzField.setDataType(new TextDataType(3));
		tzField.setDbColumn("timezone");
		tzField.setRequired(true);
		this.addField(tzField);
		
		// add locale field
		Field localeField = new Field();
		localeField.setApiName("locale");
		localeField.setLabel("Locale");
		localeField.setDataType(new EnumerationDataType("English\nPolski"));
		localeField.setDbColumn("locale");
		localeField.setRequired(true);
		this.addField(localeField);
		
		// add remember me field to the User type
		Field rmField = new Field();
		rmField.setApiName("rememberMeToken");
		rmField.setLabel("Remember Me Token");
		rmField.setDataType(new TextDataType(1000));
		rmField.setDbColumn("remembermetoken");
		rmField.setRequired(false);
		this.addField(rmField);
		
		// activation hash
		Field activationHashField = new Field();
		activationHashField.setApiName("activationHash");
		activationHashField.setLabel("Activation Hash");
		activationHashField.setDataType(new TextDataType(255));
		activationHashField.setDbColumn("activationhash");
		activationHashField.setRequired(false);
		this.addField(activationHashField);
		
		// forgotten password hash
		Field forgottenPasswordHashField = new Field();
		forgottenPasswordHashField.setApiName("forgottenPasswordHash");
		forgottenPasswordHashField.setLabel("Forgotten Password Hash");
		forgottenPasswordHashField.setDataType(new TextDataType(255));
		forgottenPasswordHashField.setDbColumn("forgottenpasswordhash");
		forgottenPasswordHashField.setRequired(false);
		this.addField(forgottenPasswordHashField);
		
		// forgotten password hash
		Field isActiveField = new Field();
		isActiveField.setApiName("isActive");
		isActiveField.setLabel("Is Active");
		isActiveField.setDataType(new BooleanDataType());
		isActiveField.setDbColumn("isactive");
		isActiveField.setRequired(true);
		this.addField(isActiveField);
		
		Field firstNameField = new Field();
		firstNameField.setApiName("firstName");
		firstNameField.setLabel("First Name");
		firstNameField.setDataType(new TextDataType(30));
		firstNameField.setDbColumn("firstname");
		firstNameField.setRequired(false);
		this.addField(firstNameField);
		
		Field middleNameField = new Field();
		middleNameField.setApiName("middleName");
		middleNameField.setLabel("Middle Name");
		middleNameField.setDataType(new TextDataType(30));
		middleNameField.setDbColumn("middlename");
		middleNameField.setRequired(false);
		this.addField(middleNameField);
		
		Field lastNameField = new Field();
		lastNameField.setApiName("lastName");
		lastNameField.setLabel("Last Name");
		lastNameField.setDataType(new TextDataType(100));
		lastNameField.setDbColumn("lastname");
		lastNameField.setRequired(false);
		this.addField(lastNameField);
		
		Field titleField = new Field();
		titleField.setApiName("title");
		titleField.setLabel("Title");
		titleField.setDataType(new TextDataType(10));
		titleField.setDbColumn("title");
		titleField.setRequired(false);
		this.addField(titleField);
	}

	/**
	 * Checks if the given KID is a valid KUser ID.
	 * @param userId
	 * @throws FieldValidationException
	 */
	public static void validateUserId (KID userId) throws FieldValidationException
	{
		if (!userId.getId().startsWith(KID.USER_PREFIX))
		{
			throw new FieldValidationException("KID must be a user ID in the current context. It should start with " + KID.USER_PREFIX + ". The actual value is " + userId);
		}
	}
}
