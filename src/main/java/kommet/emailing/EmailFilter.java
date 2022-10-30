/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.emailing;

import java.util.HashSet;
import java.util.Set;

import kommet.basic.Email;
import kommet.data.KID;
import kommet.filters.BasicFilter;

public class EmailFilter extends BasicFilter<Email>
{
	private Set<String> messageIds;
	private Set<KID> emailIds;
	private String subjectLike;
	private String plainTextBodyLike;
	private Set<String> senders;
	
	public void addSender(String sender)
	{
		if (this.senders == null)
		{
			this.senders = new HashSet<String>();
		}
		this.senders.add(sender);
	}
	
	public void addMessageId(String id)
	{
		if (this.messageIds == null)
		{
			this.messageIds = new HashSet<String>();
		}
		this.messageIds.add(id);
	}
	
	public void addEmailId(KID id)
	{
		if (this.emailIds == null)
		{
			this.emailIds = new HashSet<KID>();
		}
		this.emailIds.add(id);
	}

	public void setMessageIds(Set<String> messageIds)
	{
		this.messageIds = messageIds;
	}

	public Set<String> getMessageIds()
	{
		return messageIds;
	}

	public void setEmailIds(Set<KID> emailIds)
	{
		this.emailIds = emailIds;
	}

	public Set<KID> getEmailIds()
	{
		return emailIds;
	}

	public void setSubjectLike(String subjectLike)
	{
		this.subjectLike = subjectLike;
	}

	public String getSubjectLike()
	{
		return subjectLike;
	}

	public void setPlainTextBodyLike(String bodyLike)
	{
		this.plainTextBodyLike = bodyLike;
	}

	public String getPlainTextBodyLike()
	{
		return plainTextBodyLike;
	}

	public void setSenders(Set<String> senders)
	{
		this.senders = senders;
	}

	public Set<String> getSenders()
	{
		return senders;
	}
}