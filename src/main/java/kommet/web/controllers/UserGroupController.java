/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.UserGroup;
import kommet.basic.UserGroupAssignment;
import kommet.basic.keetle.tags.breadcrumbs.Breadcrumbs;
import kommet.dao.UserGroupFilter;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.errorlog.ErrorLogService;
import kommet.errorlog.ErrorLogSeverity;
import kommet.json.JSON;
import kommet.json.JsonSerializationException;
import kommet.rest.RestUtil;
import kommet.services.UserGroupService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;
import kommet.utils.ValidationUtil;

@Controller
public class UserGroupController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	ErrorLogService errorLog;
	
	@Inject
	UserGroupService userGroupService;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/usergroups/list", method = RequestMethod.GET)
	public ModelAndView list(HttpSession session, HttpServletRequest req) throws KommetException
	{
		// add breadcrumbs
		Breadcrumbs.add(req.getRequestURL().toString(), "User groups", appConfig.getBreadcrumbMax(), session);
				
		ModelAndView mv = new ModelAndView("usergroups/list");
		mv.addObject("i18n", AuthUtil.getAuthData(session).getI18n());
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/usergroups/list/data", method = RequestMethod.GET)
	@ResponseBody
	public void listData(HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		AuthData authData = AuthUtil.getAuthData(session);
		List<UserGroup> groups = userGroupService.get(new UserGroupFilter(), authData, envService.getCurrentEnv(session));
		resp.getWriter().write(getSuccessDataJSON(JSON.serializeObjectProxies(groups, MiscUtils.toSet("id", "name", "createdDate"), authData)));
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/usergroups/new", method = RequestMethod.GET)
	public ModelAndView create(HttpSession session) throws KommetException
	{
		AuthData authData = AuthUtil.getAuthData(session);
		ModelAndView mv = new ModelAndView("usergroups/edit");
		mv.addObject("i18n", authData.getI18n());
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/usergroups/addmember", method = RequestMethod.POST)
	@ResponseBody
	public void addGroupMember(@RequestParam(required = false, value = "userGroupId") String sMemberGroupId,
								@RequestParam(required = false, value = "userId") String sUserId,
								@RequestParam(required = false, value = "parentGroupId") String sParentGroupId,
								HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		
		if (!StringUtils.hasText(sParentGroupId))
		{
			out.write(RestUtil.getRestErrorResponse("Parent group ID not specified"));
			return;
		}
		
		if (!StringUtils.hasText(sMemberGroupId) && !StringUtils.hasText(sUserId))
		{
			out.write(RestUtil.getRestErrorResponse("Neither member user not member group specified"));
			return;
		}
		
		AuthData authData = AuthUtil.getAuthData(session);
		EnvData env = envService.getCurrentEnv(session);
		
		if (StringUtils.hasText(sMemberGroupId))
		{
			try
			{
				userGroupService.assignGroupToGroup(KID.get(sMemberGroupId), KID.get(sParentGroupId), authData, env);
			}
			catch (Exception e)
			{
				errorLog.logException(e, ErrorLogSeverity.ERROR, UserGroupController.class.getName(), 118, authData.getUserId(), authData, env);
				out.write(RestUtil.getRestErrorResponse("Error while adding subgroup"));
				return;
			}
		}
		if (StringUtils.hasText(sUserId))
		{
			try
			{
				userGroupService.assignUserToGroup(KID.get(sUserId), KID.get(sParentGroupId), authData, env);
			}
			catch (Exception e)
			{
				errorLog.logException(e, ErrorLogSeverity.ERROR, UserGroupController.class.getName(), 118, authData.getUserId(), authData, env);
				out.write(RestUtil.getRestErrorResponse("Error while adding group member"));
				return;
			}
		}
		
		out.write(RestUtil.getRestSuccessResponse("Group member added successully"));
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/usergroups/members/{id}", method = RequestMethod.GET)
	@ResponseBody
	public void userGroupMembers(@PathVariable("id") String sGroupId, HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		AuthData authData = AuthUtil.getAuthData(session);
		EnvData env = envService.getCurrentEnv(session);
		KID groupId = null;
		
		try
		{
			groupId = KID.get(sGroupId);
		}
		catch (KIDException e)
		{
			out.write(RestUtil.getRestErrorResponse("Invalid group ID '" + sGroupId + "'"));
			return;
		}
		
		// find group members
		List<UserGroupAssignment> assignments = userGroupService.getGroupMembers(groupId, authData, env);
		
		List<String> groupMemberJsonItems = new ArrayList<String>();
		for (UserGroupAssignment assignment : assignments)
		{
			groupMemberJsonItems.add(getUserGroupMemberJsonItem(assignment, authData));
		}
		
		String returnData = "[ " + MiscUtils.implode(groupMemberJsonItems, ", ") + " ]";
		out.write(RestUtil.getRestSuccessDataResponse(returnData));
	}
	
	/**
	 * Returns a JSON representation of the user group assignment
	 * @param assignment
	 * @param authData
	 * @return
	 * @throws JsonSerializationException
	 */
	private String getUserGroupMemberJsonItem(UserGroupAssignment assignment, AuthData authData) throws JsonSerializationException
	{
		Set<String> fields = new HashSet<String>();
		fields.add("id");
		fields.add("createdDate");
		fields.add("childUser.id");
		fields.add("childUser.userName");
		fields.add("childUser.email");
		fields.add("childGroup.id");
		fields.add("childGroup.name");
		return JSON.serializeObjectProxy(assignment, fields, authData);
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/usergroups/{id}", method = RequestMethod.GET)
	public ModelAndView details(@PathVariable("id") String sGroupId, HttpSession session, HttpServletRequest req) throws KommetException
	{			
		ModelAndView mv = new ModelAndView("usergroups/details");
		AuthData authData = AuthUtil.getAuthData(session);
		EnvData env = envService.getCurrentEnv(session);
		UserGroup group = userGroupService.get(KID.get(sGroupId), authData, env);
		
		// add breadcrumbs
		Breadcrumbs.add(req.getRequestURL().toString(), group.getName(), appConfig.getBreadcrumbMax(), session);
		
		mv.addObject("group", group);
		mv.addObject("i18n", authData.getI18n());
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/usergroups/edit/{id}", method = RequestMethod.GET)
	public ModelAndView edit(@PathVariable("id") String sGroupId, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("usergroups/edit");
		AuthData authData = AuthUtil.getAuthData(session);
		UserGroup group = userGroupService.get(KID.get(sGroupId), authData, envService.getCurrentEnv(session));
		mv.addObject("group", group);
		mv.addObject("i18n", authData.getI18n());
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/usergroups/save", method = RequestMethod.POST)
	public ModelAndView save (@RequestParam(value = "groupId", required = false) String sGroupId,
								@RequestParam(value = "name", required = false) String name,
								HttpSession session) throws KommetException
	{
		clearMessages();
		
		UserGroup group = new UserGroup();
		AuthData authData = AuthUtil.getAuthData(session);
		EnvData env = envService.getCurrentEnv(session);
		
		if (StringUtils.hasText(sGroupId))
		{
			group = userGroupService.get(KID.get(sGroupId), authData, env);
			
			if (group == null)
			{
				addError("User group with ID " + sGroupId + " does not exist");
			}
		}
		
		group.setName(name);
		if (!StringUtils.hasLength(name))
		{
			addError(authData.getI18n().get("usergroups.err.groupnamereq"));
		}
		else if (!ValidationUtil.isValidOptionallyQualifiedResourceName(name))
		{
			addError("Invalid user group name");
		}
		
		if (hasErrorMessages())
		{
			ModelAndView mv = new ModelAndView("usergroups/edit");
			mv.addObject("errorMsgs", getErrorMsgs());
			mv.addObject("group", group);
			mv.addObject("i18n", authData.getI18n());
			return mv;
		}
		
		userGroupService.save(group, authData, env);
		return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/usergroups/" + group.getId());
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/usergroups/removemember", method = RequestMethod.POST)
	@ResponseBody
	public void unassignGroupMember (@RequestParam(value = "assignmentId", required = false) String sAssignmentId, HttpServletResponse response, HttpSession session) throws KommetException, IOException
	{
		PrintWriter out = response.getWriter();
		AuthData authData = AuthUtil.getAuthData(session);
		EnvData env = envService.getCurrentEnv(session);
		
		if (StringUtils.hasText(sAssignmentId))
		{
			userGroupService.deleteUserGroupAssignment(KID.get(sAssignmentId), authData, env);
			out.write(RestUtil.getRestSuccessResponse("Group member removed"));
			return;
		}
		else
		{
			out.write(RestUtil.getRestErrorResponse("Group member assignment ID not specified"));
			return;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/usergroups/delete", method = RequestMethod.POST)
	@ResponseBody
	public void delete (@RequestParam(value = "id", required = false) String sGroupId, HttpServletResponse response, HttpSession session) throws KommetException, IOException
	{
		KID groupId = null;
		PrintWriter out = response.getWriter();
		
		try
		{
			groupId = KID.get(sGroupId);
		}
		catch (KIDException e)
		{
			out.write(getErrorJSON("Invalid template ID " + sGroupId));
			return;
		}
		
		AuthData authData = AuthUtil.getAuthData(session);
		
		try
		{
			userGroupService.delete(groupId, authData, envService.getCurrentEnv(session));
			out.write(getSuccessJSON(authData.getI18n().get("usergroups.deleted.msg")));
			return;
		}
		catch (KommetException e)
		{
			out.write(getErrorJSON(authData.getI18n().get("usergroups.err.deleting") + ": " + e.getMessage()));
			return;
		}
	}
}