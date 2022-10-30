/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.comments;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.Comment;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class CommentFilter extends BasicFilter<Comment>
{
	private Set<KID> recordIds;
	private Set<KID> commentIds;
	
	// number of levels for which subcomments should be retrieved
	private int subcommentLevels = 0;
	
	private boolean onlyCommentsWithoutParents = true;
	
	// additional fields to query
	private String additionalFields;
	
	public void addRecordId(KID id)
	{
		if (this.recordIds == null)
		{
			this.recordIds = new HashSet<KID>();
		}
		this.recordIds.add(id);
	}
	
	public void addCommentId(KID id)
	{
		if (this.commentIds == null)
		{
			this.commentIds = new HashSet<KID>();
		}
		this.commentIds.add(id);
	}

	public int getSubcommentLevels()
	{
		return subcommentLevels;
	}

	public void setSubcommentLevels(int subcommentLevels)
	{
		this.subcommentLevels = subcommentLevels;
	}

	public Set<KID> getRecordIds()
	{
		return recordIds;
	}

	public void setRecordIds(Set<KID> recordIds)
	{
		this.recordIds = recordIds;
	}

	public boolean isOnlyCommentsWithoutParents()
	{
		return onlyCommentsWithoutParents;
	}

	public void setOnlyCommentsWithoutParents(boolean onlyCommentsWithoutParents)
	{
		this.onlyCommentsWithoutParents = onlyCommentsWithoutParents;
	}

	public Set<KID> getCommentIds()
	{
		return commentIds;
	}

	public void setCommentIds(Set<KID> commentIds)
	{
		this.commentIds = commentIds;
	}

	public String getAdditionalFields()
	{
		return additionalFields;
	}

	public void setAdditionalFields(String additionalFields)
	{
		this.additionalFields = additionalFields;
	}
}