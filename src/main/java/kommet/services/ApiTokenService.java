/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.services;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.ApiToken;
import kommet.basic.User;
import kommet.dao.ApiTokenDao;
import kommet.data.FieldValidationException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;

@Service
public class ApiTokenService
{
	private static final Logger log = LoggerFactory.getLogger(ApiTokenService.class);

	public static final String CSRF_SESSION_ATTRIBUTE = "kommet.apiToken.csrf";

	private static final int TOKEN_BYTES = 32;
	private static final int MAX_NAME_LENGTH = 100;
	private static final SecureRandom secureRandom = new SecureRandom();

	@Inject
	ApiTokenDao apiTokenDao;

	@Transactional
	public CreatedApiToken add(String name, Date expirationDate, boolean readPermission, boolean writePermission, User systemUser, EnvData env) throws KommetException
	{
		log.debug("[APITOKEN] Creating token for user " + (systemUser != null ? systemUser.getId() : null) + " in environment " + (env != null ? env.getId() : null));
		if (systemUser == null || systemUser.getId() == null)
		{
			throw new FieldValidationException("A system user is required for an API token");
		}
		if (!StringUtils.hasText(name) || name.trim().length() > MAX_NAME_LENGTH)
		{
			throw new FieldValidationException("Token name is required and must be at most " + MAX_NAME_LENGTH + " characters");
		}
		if (!readPermission && !writePermission)
		{
			throw new FieldValidationException("Select at least one API token permission");
		}
		if (expirationDate != null && !expirationDate.after(new Date()))
		{
			throw new FieldValidationException("Expiration date must be in the future");
		}

		String secret = generateSecret();
		ApiToken token = new ApiToken();
		token.setName(name.trim());
		token.setTokenHash(hashToken(secret));
		token.setTokenPrefix(secret.substring(0, 11));
		token.setSystemUser(systemUser);
		token.setExpirationDate(expirationDate);
		token.setIsReadPermission(readPermission);
		token.setIsWritePermission(writePermission);

		token = apiTokenDao.save(token, AuthData.getRootAuthData(env), env);
		log.debug("[APITOKEN] Created token record " + token.getId() + " in environment " + env.getId());
		return new CreatedApiToken(token, secret);
	}

	@Transactional(readOnly = true)
	public List<ApiToken> search(KID systemUserId, EnvData env) throws KommetException
	{
		log.debug("[APITOKEN] Searching tokens for user " + systemUserId + " in environment " + (env != null ? env.getId() : null));
		List<ApiToken> tokens = apiTokenDao.findForUser(systemUserId, env);
		log.debug("[APITOKEN] Found " + tokens.size() + " tokens for user " + systemUserId);
		return tokens;
	}

	@Transactional
	public void remove(KID tokenId, KID systemUserId, EnvData env) throws KommetException
	{
		log.debug("[APITOKEN] Removing token " + tokenId + " for user " + systemUserId + " in environment " + (env != null ? env.getId() : null));
		ApiToken token = apiTokenDao.get(tokenId, AuthData.getRootAuthData(env), env);
		if (token == null || token.getSystemUser() == null || !systemUserId.equals(token.getSystemUser().getId()))
		{
			log.debug("[APITOKEN] Token " + tokenId + " was not found for the requested user");
			throw new KommetException("API token not found");
		}
		apiTokenDao.delete(tokenId, AuthData.getRootAuthData(env), env);
		log.debug("[APITOKEN] Removed token " + tokenId);
	}

	@Transactional(readOnly = true)
	public ApiToken findValidToken(String secret, EnvData env) throws KommetException
	{
		log.debug("[APITOKEN] Looking up token in environment " + (env != null ? env.getId() : null));
		if (!StringUtils.hasText(secret))
		{
			log.debug("[APITOKEN] Token lookup rejected an empty secret");
			return null;
		}
		ApiToken token = apiTokenDao.findByTokenHash(hashToken(secret), env);
		if (token == null)
		{
			log.debug("[APITOKEN] No token record matched the supplied secret");
			return null;
		}
		if (token.getExpirationDate() != null && !token.getExpirationDate().after(new Date()))
		{
			log.debug("[APITOKEN] Token " + token.getId() + " has expired");
			return null;
		}
		log.debug("[APITOKEN] Found valid token record " + token.getId());
		return token;
	}

	private String generateSecret()
	{
		log.debug("[APITOKEN] Generating a new token secret");
		byte[] tokenBytes = new byte[TOKEN_BYTES];
		secureRandom.nextBytes(tokenBytes);
		String secret = "km_" + Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
		log.debug("[APITOKEN] Generated a new token secret");
		return secret;
	}

	private String hashToken(String secret) throws KommetException
	{
		log.debug("[APITOKEN] Hashing token secret");
		try
		{
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			String hash = HexFormat.of().formatHex(digest);
			log.debug("[APITOKEN] Token secret hashed successfully");
			return hash;
		}
		catch (NoSuchAlgorithmException e)
		{
			log.debug("[APITOKEN] SHA-256 is unavailable while hashing a token secret");
			throw new KommetException("SHA-256 is not available", e);
		}
	}

	public static class CreatedApiToken
	{
		private final ApiToken apiToken;
		private final String secret;

		public CreatedApiToken(ApiToken apiToken, String secret)
		{
			this.apiToken = apiToken;
			this.secret = secret;
		}

		public ApiToken getApiToken() { return apiToken; }
		public String getSecret() { return secret; }
	}
}