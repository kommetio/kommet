/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.comments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.Comment;
import kommet.basic.RecordProxyType;
import kommet.basic.User;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.dao.queries.SortDirection;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.filters.QueryResultOrder;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;
import kommet.utils.MiscUtils;

@Repository
public class CommentDao extends GenericDaoImpl<Comment>
{
	@Inject
	DataService dataService;
	
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	public CommentDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public List<Comment> get (CommentFilter filter, EnvData env) throws KommetException
	{
		return this.get(filter, null, env);
	}
	
	public List<Comment> get (CommentFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new CommentFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.COMMENT_API_NAME)).getKID(), authData);
		c.addProperty("id, parent.id, content, recordId, createdBy.userName");
		
		if (StringUtils.hasText(filter.getAdditionalFields()))
		{
			// add additional fields to query
			c.addProperty(filter.getAdditionalFields());
		}
		
		c.addStandardSelectProperties();
		c.createAlias("parent", "parent");
		
		if (filter.getCommentIds() != null && !filter.getCommentIds().isEmpty())
		{
			c.add(Restriction.in("id", filter.getCommentIds()));
		}
		
		if (filter.getRecordIds() != null && !filter.getRecordIds().isEmpty())
		{
			c.add(Restriction.in("recordId", filter.getRecordIds()));
		}
		
		if (filter.isOnlyCommentsWithoutParents())
		{
			c.add(Restriction.isNull("parent"));
		}
		
		if (filter.getOrder() != null && filter.getOrderBy() != null)
		{
			c.addOrderBy(filter.getOrder().equals(QueryResultOrder.ASC) ? SortDirection.ASC : SortDirection.DESC, filter.getOrderBy());
		}
		
		List<Record> records = c.list();
		List<Comment> comments = new ArrayList<Comment>();
		for (Record r : records)
		{
			comments.add(new Comment(r, env));
		}
		
		if (filter.getSubcommentLevels() != 0)
		{
			// query subcomments
			initSubcomments(comments, filter.getSubcommentLevels(), env);
		}
		
		return comments;
	}

	/**
	 * Finds all subcomments for the given comments reaching <tt>subcommentLevels</tt> levels down.
	 * @param comments
	 * @param subcommentLevels
	 * @param env
	 * @throws KommetException
	 */
	private void initSubcomments(List<Comment> comments, int subcommentLevels, EnvData env) throws KommetException
	{
		if (comments.isEmpty())
		{
			return;
		}
		
		Set<KID> commentIds = new HashSet<KID>();
		for (Comment c : comments)
		{
			commentIds.add(c.getId());
		}
		
		Type commentType = env.getType(KeyPrefix.get(KID.COMMENT_PREFIX));
		
		// build a recursive query to traverse comment tree
		StringBuilder sql = new StringBuilder();
		sql.append("WITH RECURSIVE subitems (kid, content, recordid, parent, ").append(Field.CREATEDDATE_FIELD_DB_COLUMN).append(", ").append(Field.CREATEDBY_FIELD_DB_COLUMN).append(", path, depth) AS (");
		sql.append("SELECT kid, content, recordid, parent, ").append(Field.CREATEDDATE_FIELD_DB_COLUMN).append(", ").append(Field.CREATEDBY_FIELD_DB_COLUMN).append(", ARRAY[parent], 0 FROM ").append(commentType.getDbTable()).append(" WHERE parent is null ");
		sql.append("UNION ALL ");
		sql.append("SELECT i.kid, i.content, i.recordid, i.parent, i.").append(Field.CREATEDDATE_FIELD_DB_COLUMN).append(", i.").append(Field.CREATEDBY_FIELD_DB_COLUMN).append(", array_append(path, i.parent)::character varying(13)[], si.depth + 1 ");
		sql.append("FROM subitems si, ").append(commentType.getDbTable()).append(" i ");
		sql.append("WHERE si.kid = i.parent");
		sql.append(")\n");

		sql.append("SELECT * FROM subitems where path[2] IN (" + MiscUtils.implode(commentIds, ", ", "'") + ") AND depth > 0");
		
		if (subcommentLevels != -1)
		{
			sql.append(" AND depth > 0 AND depth <= ").append(subcommentLevels);
		}
		
		SqlRowSet rowSet = env.getJdbcTemplate().queryForRowSet(sql.toString());
	
		// map parent comment ID to list if its direct subcomments
		Map<KID, List<Comment>> commentsByParentId = new HashMap<KID, List<Comment>>();
		List<Comment> retrievedComments = new ArrayList<Comment>();
		
		while (rowSet.next())
		{
			Comment c = getCommentFromRowSet(rowSet);
			retrievedComments.add(c);
			
			if (c.getParent() != null)
			{
				List<Comment> subcomments = commentsByParentId.get(c.getParent().getId());
				if (subcomments == null)
				{
					subcomments = new ArrayList<Comment>();
				}
				
				subcomments.add(c);
				commentsByParentId.put(c.getParent().getId(), subcomments);
			}
		}
		
		// set subcomments on retrieved comments
		for (Comment c : retrievedComments)
		{
			if (commentsByParentId.containsKey(c.getId()))
			{
				c.setComments(commentsByParentId.get(c.getId()));
			}
		}
		
		// set subcomments on original comments
		for (Comment c : comments)
		{
			if (commentsByParentId.containsKey(c.getId()))
			{
				c.setComments(commentsByParentId.get(c.getId()));
			}
		}
	}

	private Comment getCommentFromRowSet(SqlRowSet rowSet) throws KommetException
	{
		Comment c = new Comment();
		c.setId(KID.get(rowSet.getString(Field.ID_FIELD_DB_COLUMN)));
		c.setContent(rowSet.getString("content"));
		c.setCreatedDate(rowSet.getDate(Field.CREATEDDATE_FIELD_DB_COLUMN));
		
		User owner = new User();
		owner.setId(KID.get(rowSet.getString(Field.CREATEDBY_FIELD_DB_COLUMN)));
		
		c.setCreatedBy(owner);
		c.setRecordId(KID.get(rowSet.getString("recordid")));
		
		if (rowSet.getString("parent") != null)
		{
			Comment parent = new Comment();
			parent.setId(KID.get(rowSet.getString("parent")));
			c.setParent(parent);
		}
		
		return c;
	}
}