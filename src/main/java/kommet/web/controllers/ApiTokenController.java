/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.data.FieldValidationException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.services.ApiTokenService;
import kommet.utils.UrlUtil;

@Controller
public class ApiTokenController extends CommonKommetController
{
	@Inject
	EnvService envService;

	@Inject
	ApiTokenService apiTokenService;

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/api-tokens/create", method = RequestMethod.POST)
	public String create(@RequestParam("name") String name,
			@RequestParam(value = "expirationDays", required = false) String expirationDays,
			@RequestParam(value = "readPermission", required = false) String readPermission,
			@RequestParam(value = "writePermission", required = false) String writePermission,
			@RequestParam("csrfToken") String csrfToken,
			HttpSession session,
			RedirectAttributes redirectAttributes) throws KommetException
	{
		AuthData authData = requireAuthData(session);
		verifyCsrfToken(csrfToken, session);

		try
		{
			Date expirationDate = parseExpirationDate(expirationDays);
			ApiTokenService.CreatedApiToken created = apiTokenService.add(name, expirationDate,
					"true".equals(readPermission), "true".equals(writePermission), authData.getUser(), envService.getCurrentEnv(session));
			redirectAttributes.addFlashAttribute("newApiTokenSecret", created.getSecret());
			redirectAttributes.addFlashAttribute("newApiTokenName", created.getApiToken().getName());
		}
		catch (FieldValidationException e)
		{
			redirectAttributes.addFlashAttribute("apiTokenError", e.getMessage());
		}

		return profileRedirect();
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/api-tokens/remove", method = RequestMethod.POST)
	public String remove(@RequestParam("tokenId") String tokenId,
			@RequestParam("csrfToken") String csrfToken,
			HttpSession session,
			RedirectAttributes redirectAttributes) throws KommetException
	{
		AuthData authData = requireAuthData(session);
		verifyCsrfToken(csrfToken, session);

		try
		{
			apiTokenService.remove(KID.get(tokenId), authData.getUserId(), envService.getCurrentEnv(session));
			redirectAttributes.addFlashAttribute("apiTokenMessage", "API token revoked");
		}
		catch (KommetException e)
		{
			redirectAttributes.addFlashAttribute("apiTokenError", "Could not revoke API token");
		}

		return profileRedirect();
	}

	private AuthData requireAuthData(HttpSession session) throws KommetException
	{
		AuthData authData = AuthUtil.getAuthData(session);
		if (authData == null || authData.getUserId() == null || authData.getUser() == null)
		{
			throw new KommetException("Authentication is required to manage API tokens");
		}
		return authData;
	}

	private void verifyCsrfToken(String suppliedToken, HttpSession session) throws KommetException
	{
		String sessionToken = (String)session.getAttribute(ApiTokenService.CSRF_SESSION_ATTRIBUTE);
		if (!StringUtils.hasText(sessionToken) || !StringUtils.hasText(suppliedToken)
				|| !MessageDigest.isEqual(sessionToken.getBytes(StandardCharsets.UTF_8), suppliedToken.getBytes(StandardCharsets.UTF_8)))
		{
			throw new KommetException("Invalid request token");
		}
	}

	private Date parseExpirationDate(String expirationDays) throws FieldValidationException
	{
		if (!StringUtils.hasText(expirationDays))
		{
			return null;
		}

		try
		{
			int days = Integer.parseInt(expirationDays);
			if (days < 1 || days > 3650)
			{
				throw new FieldValidationException("Expiration must be between 1 and 3650 days");
			}
			Calendar expiration = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			expiration.add(Calendar.DAY_OF_YEAR, days);
			return expiration.getTime();
		}
		catch (NumberFormatException e)
		{
			throw new FieldValidationException("Expiration days must be a whole number");
		}
	}

	private String profileRedirect()
	{
		return "redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/me";
	}
}