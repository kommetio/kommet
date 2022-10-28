/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import java.util.ArrayList;
import java.util.List;

import kommet.basic.types.SystemTypes;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.COMMENT_API_NAME)
public class Comment extends StandardTypeRecordProxy
{
	private String content;
	private KID recordId;
	private Comment parent;
	private List<Comment> comments;
	
	public Comment() throws KommetException
	{
		this(null, null);
	}
	
	public Comment(Record comment, EnvData env) throws KommetException
	{
		super(comment, true, env);
	}

	public void setContent(String name)
	{
		this.content = name;
		setInitialized();
	}

	@Property(field = "content")
	public String getContent()
	{
		return content;
	}

	public void setRecordId(KID recordId)
	{
		this.recordId = recordId;
		setInitialized();
	}

	@Property(field = "recordId")
	public KID getRecordId()
	{
		return recordId;
	}

	public void setParent(Comment parent)
	{
		this.parent = parent;
		
		if (parent != null && parent.getRecordId() != null)
		{
			// the parent of the comment is always the one of the comment's parent
			setRecordId(parent.getRecordId());
		}
		
		setInitialized();
	}

	@Property(field = "parent")
	public Comment getParent()
	{
		return parent;
	}
	
	public void addComment(Comment c)
	{
		if (this.comments == null)
		{
			this.comments = new ArrayList<Comment>();
		}
		this.comments.add(c);
	}
	
	@Transient
	public List<Comment> getComments()
	{
		return this.comments;
	}

	public void setComments(List<Comment> comments)
	{
		this.comments = comments;
	}
}