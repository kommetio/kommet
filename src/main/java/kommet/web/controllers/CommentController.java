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
import java.util.LinkedHashMap;
import java.util.List;

import javax.inject.Inject;
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
import kommet.auth.UserService;
import kommet.basic.Comment;
import kommet.basic.User;
import kommet.basic.keetle.tags.CommentsTag;
import kommet.comments.CommentFilter;
import kommet.comments.CommentService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KeyPrefixException;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.TextDataType;
import kommet.data.sharing.SharingService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.QueryResultOrder;
import kommet.json.JSON;
import kommet.rest.RestUtil;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

@Controller
public class CommentController extends BasicRestController
{
	@Inject
	CommentService commentService;
	
	@Inject
	EnvService envService;
	
	@Inject
	UserService userService;
	
	@Inject
	SharingService sharingService;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/comments/save", method = RequestMethod.POST)
	@ResponseBody
	public void save (@RequestParam(value = "recordId", required = false) String sRecordId,
			@RequestParam(value = "content", required = false) String content,
			@RequestParam(value = "parentId", required = false) String sParentId,
            HttpServletResponse response, HttpSession session) throws KommetException, IOException
	{
		PrintWriter out = response.getWriter();
		AuthData authData = AuthUtil.getAuthData(session);
		
		if (!StringUtils.hasText(sRecordId))
		{
			out.write(RestUtil.getRestErrorResponse("Record ID not specified"));
			return;
		}
		
		if (!StringUtils.hasText(content))
		{
			out.write(RestUtil.getRestErrorResponse(authData.getI18n().get("comments.empty.err")));
			return;
		}
		
		EnvData env = envService.getCurrentEnv(session);
		
		Type commentType = env.getType(KeyPrefix.get(KID.COMMENT_PREFIX));
		Integer maxCommentLength = ((TextDataType)commentType.getField("content").getDataType()).getLength();
		
		// make sure the comment text does not exceed maximum length
		if (content.length() > maxCommentLength)
		{
			out.write(RestUtil.getRestErrorResponse(authData.getI18n().get("comments.length.err.pre") + " " + maxCommentLength + " " + authData.getI18n().get("comments.length.err.post")));
			return;
		}
		
		KID recordId = null;
		try
		{
			recordId = KID.get(sRecordId);
		}
		catch (KIDException e)
		{
			out.write(RestUtil.getRestErrorResponse("Invalid record ID: '" + sRecordId + "'"));
			return;
		}
		
		Comment comment = new Comment();
		comment.setContent(content);
		comment.setRecordId(recordId);
		
		if (StringUtils.hasText(sParentId))
		{
			Comment parentComment = new Comment();
			parentComment.setId(KID.get(sParentId));
			comment.setParent(parentComment);
		}
		
		try
		{
			comment = commentService.save(comment, AuthUtil.getAuthData(session), env);
			out.write("{ \"success\": true, \"id\": \"" + comment.getId() + "\" }");
			return;
		}
		catch (KommetException e)
		{
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			out.write(RestUtil.getRestErrorResponse(e.getMessage()));
			return;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/commentbox/{sCommentId}", method = RequestMethod.GET)
	@ResponseBody
	public void getCommentBox (@PathVariable("sCommentId") String sCommentId,
								HttpSession session, HttpServletResponse response) throws KommetException, IOException
	{
		PrintWriter out = response.getWriter();
		KID commentId = null;
		try
		{
			commentId = KID.get(sCommentId);
		}
		catch (KIDException e)
		{
			out.write(getErrorJSON("Invalid comment ID " + sCommentId));
			return;
		}
		
		AuthData authData = AuthUtil.getAuthData(session);
		out.write(CommentsTag.getCommentBox(commentService.get(commentId, envService.getCurrentEnv(session)), authData, authData.getI18n(), envService.getCurrentEnv(session), session.getServletContext().getContextPath(), userService));
		return;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/comments/{commentId}", method = RequestMethod.POST)
	public ModelAndView details (@RequestParam(value = "commentId", required = false) String sCommentId, HttpSession session) throws KommetException, IOException
	{
		KID commentId = null;
		try
		{
			commentId = KID.get(sCommentId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Invalid comment ID " + sCommentId);
		}
		
		ModelAndView mv = new ModelAndView("comments/details");
		mv.addObject("comment", commentService.get(commentId, envService.getCurrentEnv(session)));
		return mv;
	}
	
	/**
	 * Returns comments related to the given record.
	 * @param sRecordId
	 * @param envId
	 * @param accessToken
	 * @param session
	 * @param resp
	 * @throws KommetException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_RECORD_COMMENTS_URL + "/{recordId}", method = RequestMethod.GET)
	@ResponseBody
	public void recordComments (@PathVariable("recordId") String sRecordId,
								@RequestParam(value = "env", required = false) String envId,
								@RequestParam(value = "access_token", required = false) String accessToken,
								@RequestParam(value = "sort", required = false) String sort,
								@RequestParam(value = "additionalFields", required = false) String additionalFields,
								@RequestParam(value = "subcommentLevels", required = false) Integer subcommentLevels,
								HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		RestInitInfo restInfo = prepareRest(envId, accessToken, session, resp);
		if (!restInfo.isSuccess())
		{
			if (restInfo.getOut() != null)
			{
				returnRestError(restInfo.getError(), restInfo.getOut());
				resp.setStatus(restInfo.getRespCode());
				return;
			}
			else
			{
				throw new KommetException(restInfo.getError());
			}
		}
		
		KID recordId = null;
		try
		{
			recordId = KID.get(sRecordId);
		}
		catch (KIDException e)
		{
			returnRestError("Invalid record ID " + sRecordId, restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		// get comments for record
		CommentFilter filter = new CommentFilter();
		filter.addRecordId(recordId);
		
		if (StringUtils.hasText(additionalFields))
		{
			filter.setAdditionalFields(additionalFields);
		}
		
		filter.setOnlyCommentsWithoutParents(true);
		
		if (!StringUtils.hasText(sort) || "DESC".equals(sort.toUpperCase()))
		{
			filter.setOrderBy(Field.CREATEDDATE_FIELD_NAME);
			filter.setOrder(QueryResultOrder.DESC);
		}
		else if ("ASC".equals(sort.toUpperCase()))
		{
			filter.setOrderBy(Field.CREATEDDATE_FIELD_NAME);
			filter.setOrder(QueryResultOrder.ASC);
		}
		
		if (subcommentLevels != null)
		{
			filter.setSubcommentLevels(subcommentLevels);
		}
		
		List<Comment> comments = commentService.get(filter, restInfo.getAuthData(), restInfo.getEnv());
		
		// wrap comments into a hash map so that we can pass information about the comment author
		// because normally only the author's ID (field createdBy) would be passed, not their user name
		
		try
		{
			List<LinkedHashMap<String, Object>> wrappedComments = new ArrayList<LinkedHashMap<String, Object>>();
			for (Comment c : comments)
			{
				wrappedComments.add(wrapComment(c, filter.getAdditionalFields(), restInfo.getAuthData(), restInfo.getEnv()));
			}
			restInfo.getOut().write(getSuccessDataJSON(JSON.serialize(wrappedComments, restInfo.getAuthData())));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			restInfo.getOut().write(getErrorJSON("Error serializing comments to JSON: " + e.getMessage()));
		}
	}
	
	private LinkedHashMap<String, Object> wrapComment(Comment c, String additionalFields, AuthData authData, EnvData env) throws KeyPrefixException, KommetException
	{
		LinkedHashMap<String, Object> wrapper = new LinkedHashMap<String, Object>();
		wrapper.put("id", c.getId().getId());
		wrapper.put("content", JSON.escape(MiscUtils.newLinesToBr(c.getContent())));
		
		LinkedHashMap<String, Object> createdByUser = new LinkedHashMap<String, Object>();
		
		if (c.getCreatedBy() != null)
		{
			createdByUser.put("id", c.getCreatedBy().getId());
			createdByUser.put("userName", ((User)c.getCreatedBy()).getUserName());
			wrapper.put("createdBy", createdByUser);
		}
		else
		{
			wrapper.put("createdBy", null);
		}
		
		wrapper.put("createdDate", c.getCreatedDate());
		wrapper.put("parent", c.getParent());
		
		// add to JSON additional fields added by user
		if (StringUtils.hasText(additionalFields))
		{
			List<String> fieldNames = MiscUtils.splitAndTrim(additionalFields, ",");
			for (String fieldName : fieldNames)
			{
				wrapper.put(fieldName, c.getRecord().attemptGetField(fieldName));
			}
		}
		
		if (c.getComments() != null && !c.getComments().isEmpty())
		{
			List<LinkedHashMap<String, Object>> wrappedSubcomments = new ArrayList<LinkedHashMap<String,Object>>();
			for (Comment subcomment : c.getComments())
			{
				wrappedSubcomments.add(wrapComment(subcomment, additionalFields, authData, env));
			}
			
			wrapper.put("comments", wrappedSubcomments);
		}
		
		wrapper.put("canDelete", authData.canDeleteType(env.getType(KeyPrefix.get(KID.COMMENT_PREFIX)).getKID(), false, env) && sharingService.canDeleteRecord(c.getId(), authData.getUserId(), env) );
		
		return wrapper;
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/comments/delete", method = RequestMethod.POST)
	@ResponseBody
	public void delete (@RequestParam(value = "commentId", required = false) String sCommentId,
            HttpServletResponse response, HttpSession session) throws KommetException, IOException
	{
		PrintWriter out = response.getWriter();
		KID commentId = null;
		try
		{
			commentId = KID.get(sCommentId);
		}
		catch (KIDException e)
		{
			out.write(getErrorJSON("Invalid comment ID: '" + sCommentId + "'"));
			return;
		}
		
		try
		{
			commentService.deleteComment(commentId, true, null, envService.getCurrentEnv(session));
			out.write(getSuccessJSON("Comment deleted"));
			return;
		}
		catch (KommetException e)
		{
			out.write(getErrorJSON("Error: '" + e.getMessage() + "'"));
			return;
		}
	}
}