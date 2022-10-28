/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.USER_SETTINGS_API_NAME)
public class UserSettings extends StandardTypeRecordProxy
{
	private Profile profile;
	private User user;
	private Layout layout;
	private String landingURL;
	
	public UserSettings() throws RecordProxyException
	{
		super(null, true, null);
	}
	
	public UserSettings(Record record, EnvData env) throws RecordProxyException
	{
		super(record, true, env);
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

	public void setUser(User user)
	{
		this.user = user;
		setInitialized();
	}

	@Property(field = "user")
	public User getUser()
	{
		return user;
	}

	public void setLayout(Layout layout)
	{
		this.layout = layout;
		setInitialized();
	}

	@Property(field = "layout")
	public Layout getLayout()
	{
		return layout;
	}

	public void setLandingURL(String landingURL)
	{
		this.landingURL = landingURL;
		setInitialized();
	}

	@Property(field = "landingURL")
	public String getLandingURL()
	{
		return landingURL;
	}
}