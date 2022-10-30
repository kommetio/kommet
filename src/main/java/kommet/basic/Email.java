/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.util.StringUtils;

import kommet.basic.types.SystemTypes;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.emailing.EmailException;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.EMAIL_API_NAME)
public class Email extends StandardTypeRecordProxy
{
	private String subject;
	private String plainTextBody;
	private String htmlBody;
	private String sender;
	private String status;
	private Date sendDate;
	private List<String> recipients;
	private List<String> ccRecipients;
	private List<String> bccRecipients;
	private String messageId;
	
	public Email() throws KommetException
	{
		this(null, null);
	}
	
	public Email(Record record, EnvData env) throws RecordProxyException
	{
		super(record, false, env);
	}
	
	public void addRecipient (String recipientEmail)
	{
		if (this.recipients == null)
		{
			this.recipients = new ArrayList<String>();
		}
		
		this.recipients.add(recipientEmail);
		setInitialized("recipients");
	}
	
	public void addCcRecipient (String recipientEmail)
	{
		if (this.ccRecipients == null)
		{
			this.ccRecipients = new ArrayList<String>();
		}
		
		this.ccRecipients.add(recipientEmail);
		setInitialized("ccrecipients");
	}
	
	public void addBccRecipient (String recipientEmail)
	{
		if (this.bccRecipients == null)
		{
			this.bccRecipients = new ArrayList<String>();
		}
		
		this.bccRecipients.add(recipientEmail);
		setInitialized("bccRecipients");
	}

	public void setSubject(String subject)
	{
		this.subject = subject;
		setInitialized();
	}

	@Property(field = "subject")
	public String getSubject()
	{
		return subject;
	}

	public void setPlainTextBody(String plainTextBody) throws EmailException
	{
		if (this.htmlBody != null && plainTextBody != null)
		{
			throw new EmailException("Cannot set plain text body because HTML body is already different from null");
		}
		this.plainTextBody = plainTextBody;
		setInitialized();
	}

	@Property(field = "plainTextBody")
	public String getPlainTextBody()
	{
		return plainTextBody;
	}

	public void setHtmlBody(String htmlBody) throws EmailException
	{
		if (this.plainTextBody != null && htmlBody != null)
		{
			throw new EmailException("Cannot set HTML text body because plain text body is already different from null");
		}
		this.htmlBody = htmlBody;
		setInitialized();
	}

	@Property(field = "htmlBody")
	public String getHtmlBody()
	{
		return htmlBody;
	}
	
	public void setSender(String sender)
	{
		this.sender = sender;
		setInitialized();
	}

	@Property(field = "sender")
	public String getSender()
	{
		return sender;
	}

	public void setRecipients(String recipients)
	{
		this.recipients = StringUtils.hasText(recipients) ? MiscUtils.splitAndTrim(recipients, ";") : null;
		setInitialized();
	}

	@Property(field = "recipients")
	public String getRecipients()
	{
		return this.recipients != null ? MiscUtils.implode(this.recipients, "; ") : null;
	}
	
	public void setCcRecipients(String recipients)
	{
		this.ccRecipients = StringUtils.hasText(recipients) ? MiscUtils.splitAndTrim(recipients, ";") : null;
		setInitialized();
	}

	@Property(field = "ccRecipients")
	public String getCcRecipients()
	{
		return this.ccRecipients != null ? MiscUtils.implode(this.ccRecipients, "; ") : null;
	}
	
	public void setBccRecipients(String recipients)
	{
		this.bccRecipients = StringUtils.hasText(recipients) ? MiscUtils.splitAndTrim(recipients, ";") : null;
		setInitialized();
	}

	@Property(field = "bccRecipients")
	public String getBccRecipients()
	{
		return this.bccRecipients != null ? MiscUtils.implode(this.bccRecipients, "; ") : null;
	}

	public void setStatus(String status)
	{
		this.status = status;
		setInitialized();
	}

	@Property(field = "status")
	public String getStatus()
	{
		return status;
	}

	public void setSendDate(Date sendDate)
	{
		this.sendDate = sendDate;
		setInitialized();
	}

	@Property(field = "sendDate")
	public Date getSendDate()
	{
		return sendDate;
	}

	public void setMessageId(String messageId)
	{
		this.messageId = messageId;
		setInitialized();
	}

	@Property(field = "messageId")
	public String getMessageId()
	{
		return messageId;
	}
}