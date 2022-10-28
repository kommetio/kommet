/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.Comment;
import kommet.comments.CommentFilter;
import kommet.comments.CommentService;
import kommet.data.DataService;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.utils.MiscUtils;

public class CommentTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	DataService dataService;
	
	@Inject
	EnvService envService;
	
	@Inject
	CommentService commentService;
	
	@Test
	public void testCommentCRUD() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		
		Type commentType = env.getType(KeyPrefix.get(KID.COMMENT_PREFIX));
		assertNotNull(commentType.getSharingControlledByFieldId());
		assertNotNull(commentType.getSharingControlledByField());
		assertEquals(commentType.getField("recordId").getKID(), commentType.getSharingControlledByFieldId());
	
		// add comment to admin user
		Comment comment = new Comment();
		comment.setContent("Sample text");
		comment.setRecordId(env.getRootUser().getKID());
		comment = commentService.save(comment, dataHelper.getRootAuthData(env), env);
		assertNotNull(comment.getId());
		
		CommentFilter filter = new CommentFilter();
		filter.addRecordId(env.getRootUser().getKID());
		List<Comment> comments = commentService.get(filter, env);
		assertNotNull(comments);
		assertEquals(1, comments.size());
		assertEquals(env.getRootUser().getKID(), comments.get(0).getRecordId());
		
		// delete comment
		commentService.deleteComment(comment.getId(), true, null, env);
		assertTrue(commentService.get(filter, env).isEmpty());
		
		testRetrieveAndDeleteSubcomments(env);
	}

	private void testRetrieveAndDeleteSubcomments(EnvData env) throws KommetException
	{
		AuthData authData = dataHelper.getRootAuthData(env);
		
		Comment c1 = new Comment();
		c1.setContent("c1");
		c1.setRecordId(env.getRootUser().getKID());
		c1 = commentService.save(c1, authData, env);
		
		Comment c2 = new Comment();
		c2.setContent("c2");
		c2.setRecordId(env.getRootUser().getKID());
		c2 = commentService.save(c2, authData, env);
		
		Comment c3 = new Comment();
		c3.setContent("c3");
		c3.setRecordId(env.getRootUser().getKID());
		c3 = commentService.save(c3, authData, env);
		
		Comment c12 = new Comment();
		c12.setContent("c12");
		c12.setParent(c1);
		c12.setRecordId(env.getRootUser().getKID());
		assertNotNull(c12.getRecordId());
		assertEquals(c1.getRecordId(), c12.getRecordId());
		c12 = commentService.save(c12, authData, env);
		
		Comment c13 = new Comment();
		c13.setContent("c13");
		c13.setParent(c1);
		c13.setRecordId(env.getRootUser().getKID());
		c13 = commentService.save(c13, authData, env);
		
		Comment c121 = new Comment();
		c121.setContent("c121");
		c121.setParent(c12);
		c121.setRecordId(env.getRootUser().getKID());
		c121 = commentService.save(c121, authData, env);
		
		Comment c1212 = new Comment();
		c1212.setContent("c1212");
		c1212.setParent(c121);
		c1212.setRecordId(env.getRootUser().getKID());
		c1212 = commentService.save(c1212, authData, env);
		
		CommentFilter filter = new CommentFilter();
		filter.setRecordIds(MiscUtils.toSet(env.getRootUser().getKID()));
		filter.setOnlyCommentsWithoutParents(false);
		assertEquals(7, commentService.get(filter, env).size());
		
		// find all subcomments for comments c1 and c2
		filter = new CommentFilter();
		filter.addCommentId(c1.getId());
		filter.addCommentId(c2.getId());
		filter.addCommentId(c3.getId());
		filter.setSubcommentLevels(2);
		
		List<Comment> comments = commentService.get(filter, env);
		assertEquals(3, comments.size());
		
		Comment retrievedC1 = null;
		for (Comment c : comments)
		{
			if (c.getId().equals(c1.getId()))
			{
				retrievedC1 = c;
				break;
			}
		}
		
		assertNotNull(retrievedC1.getComments());
		assertEquals(2, retrievedC1.getComments().size());
		assertEquals(env.getRootUser().getKID(), retrievedC1.getComments().get(0).getRecordId());
		
		Comment retrievedC12 = null;
		for (Comment c : retrievedC1.getComments())
		{
			if (c.getId().equals(c12.getId()))
			{
				retrievedC12 = c;
				break;
			}
		}
		
		assertNotNull(retrievedC12.getComments());
		assertEquals(1, retrievedC12.getComments().size());
		
		Comment retrievedC121 = retrievedC12.getComments().get(0);
		
		assertEquals(c121.getId(), retrievedC121.getId());
		assertEquals(env.getRootUser().getKID(), retrievedC121.getRecordId());
		assertNull("Comments should be retrieved only two levels down", retrievedC121.getComments());
		
		// now modify filter to retrieve comments all the way down
		filter.setSubcommentLevels(-1);
		comments = commentService.get(filter, env);
		assertEquals(3, comments.size());
		
		retrievedC1 = null;
		for (Comment c : comments)
		{
			if (c.getId().equals(c1.getId()))
			{
				retrievedC1 = c;
				break;
			}
		}
		
		retrievedC12 = null;
		for (Comment c : retrievedC1.getComments())
		{
			if (c.getId().equals(c12.getId()))
			{
				retrievedC12 = c;
				break;
			}
		}
		
		retrievedC121 = retrievedC12.getComments().get(0);
		assertNotNull("Comments should be retrieved all levels down", retrievedC121.getComments());
		assertEquals(1, retrievedC121.getComments().size());
		assertEquals(c1212.getId(), retrievedC121.getComments().get(0).getId());
		
		filter = new CommentFilter();
		filter.setOnlyCommentsWithoutParents(false);
		
		// now delete comment c1 and make sure all of its subcomments have been deleted
		assertEquals(7, commentService.get(filter, env).size());
		commentService.deleteComment(c1.getId(), true, authData, env);
		assertEquals(2, commentService.get(filter, env).size());
	}
}
