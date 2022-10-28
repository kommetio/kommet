/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.auth.oauth2;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.stereotype.Service;

import kommet.data.KID;

@Service
public class TokenStore
{
	private Map<String, AccessToken> tokens = new HashMap<String, AccessToken>();
	
	public void store (String tokenString, String refreshToken, int expiresIn, KID userId)
	{
		AccessToken token = new AccessToken();
		token.setToken(tokenString);
		token.setUserId(userId);
		token.setRefreshToken(refreshToken);
		token.setExpirationDate(DateUtils.addMilliseconds(new Date(), expiresIn * 1000));
		tokens.put(tokenString, token);
	}
	
	public AccessToken getToken (String tokenString)
	{
		AccessToken token = this.tokens.get(tokenString);
		
		if (token == null)
		{
			return null;
		}
		// check if token is not expired
		else if (token.getExpirationDate().after(new Date()))
		{
			return token;
		}
		else
		{
			// remove expired token
			this.tokens.remove(tokenString);
			return null;
		}
	}
}
