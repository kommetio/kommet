/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.auth.oauth2;

import java.util.Date;

import kommet.data.KID;

public class AccessToken
{
	private String token;
	private String refreshToken;
	private Date expirationDate;
	private KID userId;
	
	public void setToken(String token)
	{
		this.token = token;
	}
	public String getToken()
	{
		return token;
	}
	public void setExpirationDate(Date expirationDate)
	{
		this.expirationDate = expirationDate;
	}
	public Date getExpirationDate()
	{
		return expirationDate;
	}
	public void setUserId(KID userId)
	{
		this.userId = userId;
	}
	public KID getUserId()
	{
		return userId;
	}
	public void setRefreshToken(String refreshToken)
	{
		this.refreshToken = refreshToken;
	}
	public String getRefreshToken()
	{
		return refreshToken;
	}
}
