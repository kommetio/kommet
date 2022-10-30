/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.IOException;
import java.io.PrintWriter;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import kommet.auth.AuthUtil;
import kommet.auth.UserService;
import kommet.basic.User;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.sharing.SharingService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.utils.UrlUtil;

@Controller
public class RecordSharingController extends CommonKommetController
{
	@Inject
	SharingService sharingService;
	
	@Inject
	UserService userService;
	
	@Inject
	EnvService envService;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/deleteuserrecordsharing", method = RequestMethod.POST)
	@ResponseBody
	public void searchRecord (@RequestParam("sharingId") String sSharingId, HttpServletResponse resp, HttpSession session) throws KommetException, IOException
	{
		EnvData env = envService.getCurrentEnv(session);
		sharingService.deleteSharing(KID.get(sSharingId), env);

		PrintWriter out = resp.getWriter();
		out.write(getSuccessJSON("Sharing removed"));
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/sharerecord", method = RequestMethod.POST)
	@ResponseBody
	public void shareRecord (@RequestParam("recordId") String sRecordId, @RequestParam("userId") String sUserId,
							@RequestParam(name = "permissions", required = false) String permissions,
							HttpServletResponse resp, HttpSession session) throws KommetException, IOException
	{
		EnvData env = envService.getCurrentEnv(session);
		
		User user = userService.getUser(KID.get(sUserId), env);
		
		PrintWriter out = resp.getWriter();
		
		if (user == null)
		{
			out.write(getErrorJSON("User with ID " + sUserId + " not found"));
			return;
		}
		
		if (!StringUtils.hasText(permissions))
		{
			permissions = "read";
		}
		
		boolean canEdit = false;
		boolean canDelete = false;
		
		if ("read-edit".equals(permissions))
		{
			canEdit = true;
		}
		else if ("read-edit-delete".equals(permissions))
		{
			canEdit = true;
			canDelete = true;
		}
		
		sharingService.shareRecord(KID.get(sRecordId), user.getId(), canEdit, canDelete, AuthUtil.getAuthData(session), null, true, env);
		
		out.write(getSuccessJSON("Sharing added"));
	}
}