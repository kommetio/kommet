/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.i18n.Locale;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.USER_API_NAME)
public class User extends StandardTypeRecordProxy
{
	private String userName;
	private String email;
	private String password;
	private Profile profile;
	private String timezone;
	private String locale;
	private String activationHash;
	private String forgottenPasswordHash;
	private Boolean isActive;
	private String firstName;
	private String lastName;
	private String middleName;
	private String title;
	private String rememberMeToken;

	public User(Record record, EnvData env) throws RecordProxyException
	{
		super(record, true, env);
	}
	
	public User() throws RecordProxyException
	{
		this(null, null);
	}

	public void setUserName(String userName)
	{
		this.userName = userName;
		setInitialized();
	}

	@Property(field = "userName")
	public String getUserName()
	{
		return userName;
	}

	public void setEmail(String email)
	{
		this.email = email;
		setInitialized();
	}

	@Property(field = "email")
	public String getEmail()
	{
		return email;
	}

	public void setPassword (String password)
	{
		this.password = password;
		setInitialized();
	}

	@Property(field = "password")
	public String getPassword()
	{
		return password;
	}

	public void setProfile(Profile profile)
	{
		this.profile = profile;
		setInitialized();
	}

	@Property(field = "profile")
	public Profile getProfile()
	{
		return profile;
	}

	public void setTimezone(String timezone)
	{
		this.timezone = timezone;
		setInitialized();
	}

	@Property(field = "timezone")
	public String getTimezone()
	{
		return timezone;
	}

	public void setLocale(String locale) throws KommetException
	{
		if (Locale.valueOf(locale) == null)
		{
			throw new KommetException("Unsupported locale " + locale);
		}
		this.locale = locale;
		setInitialized();
	}

	@Property(field = "locale")
	public String getLocale()
	{
		return locale;
	}
	
	@Transient
	public Locale getLocaleSetting()
	{
		return locale != null ? Locale.valueOf(locale.toUpperCase()) : null;
	}

	public void setActivationHash(String activationHash)
	{
		this.activationHash = activationHash;
		setInitialized();
	}

	@Property(field = "activationHash")
	public String getActivationHash()
	{
		return activationHash;
	}

	public void setForgottenPasswordHash(String forgottenPasswordHash)
	{
		this.forgottenPasswordHash = forgottenPasswordHash;
		setInitialized();
	}

	@Property(field = "forgottenPasswordHash")
	public String getForgottenPasswordHash()
	{
		return forgottenPasswordHash;
	}

	public void setIsActive(Boolean isActive)
	{
		this.isActive = isActive;
		setInitialized();
	}

	@Property(field = "isActive")
	public Boolean getIsActive()
	{
		return isActive;
	}

	@Property(field = "firstName")
	public String getFirstName()
	{
		return firstName;
	}

	public void setFirstName(String firstName)
	{
		this.firstName = firstName;
		setInitialized();
	}

	@Property(field = "lastName")
	public String getLastName()
	{
		return lastName;
	}

	public void setLastName(String lastName)
	{
		this.lastName = lastName;
		setInitialized();
	}

	@Property(field = "middleName")
	public String getMiddleName()
	{
		return middleName;
	}

	public void setMiddleName(String middleName)
	{
		this.middleName = middleName;
		setInitialized();
	}

	@Property(field = "title")
	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
		setInitialized();
	}

	@Property(field = "rememberMeToken")
	public String getRememberMeToken()
	{
		return rememberMeToken;
	}

	public void setRememberMeToken(String rememberMeToken)
	{
		this.rememberMeToken = rememberMeToken;
		setInitialized();
	}

}