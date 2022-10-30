/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.filters;

import java.util.HashSet;
import java.util.Set;

import kommet.data.KID;


public class UserFilter
{
	private String username;
	private String email;
	private Integer status;
	private String activationHash;
	private String forgottenPasswordHash;
	private String encryptedPassword;
	private Boolean isActive;
	private Set<KID> userIds;
	private Set<KID> profileIds;
	private String rememberMeToken;

	public void setUsername(String username)
	{
		this.username = username;
	}

	public String getUsername()
	{
		return username;
	}

	public void setStatus(Integer status)
	{
		this.status = status;
	}

	public Integer getStatus()
	{
		return status;
	}

	public void setActivationHash(String activationHash)
	{
		this.activationHash = activationHash;
	}

	public String getActivationHash()
	{
		return activationHash;
	}

	public void setForgottenPasswordHash(String forgottenPasswordHash)
	{
		this.forgottenPasswordHash = forgottenPasswordHash;
	}

	public String getForgottenPasswordHash()
	{
		return forgottenPasswordHash;
	}

	public void setEmail(String email)
	{
		this.email = email;
	}

	public String getEmail()
	{
		return email;
	}

	public void setIsActive(Boolean isActive)
	{
		this.isActive = isActive;
	}

	public Boolean getIsActive()
	{
		return isActive;
	}

	public void setEncryptedPassword(String password)
	{
		this.encryptedPassword = password;
	}

	public String getEncryptedPassword()
	{
		return encryptedPassword;
	}

	public Set<KID> getUserIds()
	{
		return userIds;
	}

	public void setUserIds(Set<KID> userIds)
	{
		this.userIds = userIds;
	}
	
	public void addUserId (KID userId)
	{
		if (this.userIds == null)
		{
			this.userIds = new HashSet<KID>();
		}
		this.userIds.add(userId);
	}
	
	public void addProfileId (KID id)
	{
		if (this.profileIds == null)
		{
			this.profileIds = new HashSet<KID>();
		}
		this.profileIds.add(id);
	}

	public Set<KID> getProfileIds()
	{
		return profileIds;
	}

	public void setProfileIds(Set<KID> profileIds)
	{
		this.profileIds = profileIds;
	}

	public String getRememberMeToken()
	{
		return rememberMeToken;
	}

	public void setRememberMeToken(String rememberMeToken)
	{
		this.rememberMeToken = rememberMeToken;
	}
}