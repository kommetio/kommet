/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.comments;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.basic.Comment;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;

@Service
public class CommentService
{
	@Inject
	CommentDao commentDao;
	
	@Transactional(readOnly = true)
	public List<Comment> get (CommentFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		return commentDao.get(filter, authData, env);
	}
	
	@Transactional(readOnly = true)
	public List<Comment> get (CommentFilter filter, EnvData env) throws KommetException
	{
		return commentDao.get(filter, env);
	}

	@Transactional
	public Comment save(Comment comment, AuthData authData, EnvData env) throws KommetException
	{
		return commentDao.save(comment, authData, env);
	}

	@Transactional
	public void deleteComment(KID commentId, boolean skipTriggers, AuthData authData, EnvData env) throws KommetException
	{
		Comment comment = commentDao.get(commentId, env);
		if (comment == null)
		{
			throw new KommetException("Comment with ID" + commentId + " does not exist");
		}
		
		commentDao.delete(Arrays.asList(comment), skipTriggers, authData, env);
	}

	@Transactional(readOnly = true)
	public Comment get(KID commentId, EnvData env) throws KommetException
	{
		return commentDao.get(commentId, env);
	}
}