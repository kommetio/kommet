/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.sysctx;

import kommet.basic.Comment;
import kommet.comments.CommentService;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.koll.CurrentAuthDataAware;

public class CommentServiceProxy extends ServiceProxy
{
	private CommentService commentService;
	
	public CommentServiceProxy(CommentService commentService, CurrentAuthDataAware authDataProvider, EnvData env)
	{
		super(authDataProvider, env);
		this.commentService = commentService;
	}
	
	public Comment save(Comment comment) throws KommetException
	{
		return this.commentService.save(comment, authDataProvider.currentAuthData(), env);
	}
	
	public Comment save(String text, KID recordId) throws KommetException
	{
		Comment comment = new Comment();
		comment.setContent(text);
		comment.setRecordId(recordId);
		return this.commentService.save(comment, authDataProvider.currentAuthData(), env);
	}
}