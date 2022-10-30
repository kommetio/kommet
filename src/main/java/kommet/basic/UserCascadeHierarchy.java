/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import org.springframework.util.StringUtils;

import kommet.basic.types.SystemTypes;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.i18n.Locale;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.uch.UserCascadeHierarchyContext;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.USER_CASCADE_HIERARCHY_API_NAME)
public class UserCascadeHierarchy extends StandardTypeRecordProxy
{
	private String activeContextName;
	private Integer activeContextRank;
	private UserCascadeHierarchyContext activeContext;
	private Boolean env;
	private Profile profile;
	private User user;
	private UserGroup userGroup;
	private String localeName;
	private Locale locale;
	
	public UserCascadeHierarchy() throws KommetException
	{
		this(null, null);
	}
	
	public UserCascadeHierarchy(Record rt, EnvData env) throws KommetException
	{
		super(rt, true, env);
	}

	public void setEnv(Boolean env) throws KommetException
	{
		this.env = env;
		setInitialized();
		
		setActiveContextName(Boolean.TRUE.equals(env) ? UserCascadeHierarchyContext.ENVIRONMENT.toString() : null);
	}

	@Property(field = "env")
	public Boolean getEnv()
	{
		return this.env;
	}

	public void setProfile(Profile profile) throws KommetException
	{
		this.profile = profile;
		setInitialized();
		setActiveContextName(profile != null ? UserCascadeHierarchyContext.PROFILE.toString() : null);
	}

	@Property(field = "profile")
	public Profile getProfile()
	{
		return profile;
	}

	public void setUser(User user) throws KommetException
	{
		this.user = user;
		setInitialized("contextUser");
		
		setActiveContextName(user != null ? UserCascadeHierarchyContext.USER.toString() : null);
	}

	@Property(field = "contextUser")
	public User getUser()
	{
		return user;
	}

	public void setActiveContextName(String activeContextName) throws KommetException
	{
		this.activeContextName = activeContextName;
		setInitialized();
		
		this.activeContext = StringUtils.hasText(activeContextName) ? UserCascadeHierarchyContext.fromString(activeContextName) : null;
		
		// set context rank
		setActiveContextRank(this.activeContext != null ? this.activeContext.getRank() : null);
	}

	@Property(field = "activeContextName")
	public String getActiveContextName()
	{
		return activeContextName;
	}

	public void setLocaleName(String localeName) throws KommetException
	{
		this.localeName = localeName;
		setInitialized();
		
		if (StringUtils.hasText(localeName))
		{
			this.locale = Locale.valueOf(localeName.toUpperCase());
			setActiveContextName(UserCascadeHierarchyContext.LOCALE.toString());
		}
		else
		{
			this.locale = null;
			setActiveContextName(null);
		}
	}

	@Property(field = "localeName")
	public String getLocaleName()
	{
		return localeName;
	}

	@Transient
	public Locale getLocale()
	{
		return locale;
	}

	@Transient
	public UserCascadeHierarchyContext getActiveContext()
	{
		return activeContext;
	}

	@Property(field = "userGroup")
	public UserGroup getUserGroup()
	{
		return userGroup;
	}

	public void setUserGroup(UserGroup userGroup) throws KommetException
	{
		this.userGroup = userGroup;
		setInitialized();
		
		setActiveContextName(userGroup != null ? UserCascadeHierarchyContext.USER_GROUP.toString() : null);
	}
	
	/**
	 * Set a value of the current active context.
	 * @param ctx The active context to set and assign a value to
	 * @param value Value of the active context
	 * @throws KommetException
	 */
	public void setActiveContext (UserCascadeHierarchyContext ctx, Object value) throws KommetException
	{
		if (ctx.equals(UserCascadeHierarchyContext.ENVIRONMENT))
		{
			setEnv((Boolean)value);
		}
		else if (ctx.equals(UserCascadeHierarchyContext.PROFILE))
		{
			Profile p = new Profile();
			p.setId((KID)value);
			setProfile(p);
		}
		else if (ctx.equals(UserCascadeHierarchyContext.LOCALE))
		{
			setLocaleName(((Locale)value).name());
		}
		else if (ctx.equals(UserCascadeHierarchyContext.USER_GROUP))
		{
			UserGroup ug = new UserGroup();
			ug.setId((KID)value);
			setUserGroup(ug);
		}
		else if (ctx.equals(UserCascadeHierarchyContext.USER))
		{
			User u = new User();
			u.setId((KID)value);
			setUser(u);
		}
		else
		{
			throw new KommetException("Unsupported context: " + ctx);
		}
	}

	@Property(field = "activeContextRank")
	public Integer getActiveContextRank()
	{
		return activeContextRank;
	}

	public void setActiveContextRank(Integer activeContextRank)
	{
		this.activeContextRank = activeContextRank;
		setInitialized();
	}

	@Transient
	public Object getActiveContextValue() throws KommetException
	{
		if (this.activeContext == null)
		{
			return null;
		}
		
		if (this.activeContext.equals(UserCascadeHierarchyContext.ENVIRONMENT))
		{
			return this.env;
		}
		else if (this.activeContext.equals(UserCascadeHierarchyContext.PROFILE))
		{
			return this.profile != null ? this.profile.getId() : null;
		}
		else if (this.activeContext.equals(UserCascadeHierarchyContext.LOCALE))
		{
			return this.locale;
		}
		else if (this.activeContext.equals(UserCascadeHierarchyContext.USER_GROUP))
		{
			return this.userGroup != null ? this.userGroup.getId() : null;
		}
		else if (this.activeContext.equals(UserCascadeHierarchyContext.USER))
		{
			return this.user != null ? this.user.getId() : null;
		}
		else
		{
			throw new KommetException("Unsupported context: " + this.activeContext);
		}
	}
}