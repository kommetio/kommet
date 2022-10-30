/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.emailing;

import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

public class EmailMessage
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
	private String source;
	private String messageId;
	private String inReplyTo;
	public String getSubject()
	{
		return subject;
	}
	public void setSubject(String subject)
	{
		this.subject = subject;
	}
	public String getPlainTextBody()
	{
		return plainTextBody;
	}
	public void setPlainTextBody(String plainTextBody)
	{
		this.plainTextBody = plainTextBody;
	}
	public String getHtmlBody()
	{
		return htmlBody;
	}
	public void setHtmlBody(String htmlBody)
	{
		this.htmlBody = htmlBody;
	}
	public String getSender()
	{
		return sender;
	}
	public void setSender(String sender)
	{
		this.sender = sender;
	}
	public String getStatus()
	{
		return status;
	}
	public void setStatus(String status)
	{
		this.status = status;
	}
	public Date getSendDate()
	{
		return sendDate;
	}
	public void setSendDate(Date sendDate)
	{
		this.sendDate = sendDate;
	}
	public List<String> getRecipients()
	{
		return recipients;
	}
	public void setRecipients(List<String> recipients)
	{
		this.recipients = recipients;
	}
	public List<String> getCcRecipients()
	{
		return ccRecipients;
	}
	public void setCcRecipients(List<String> ccRecipients)
	{
		this.ccRecipients = ccRecipients;
	}
	public List<String> getBccRecipients()
	{
		return bccRecipients;
	}
	public void setBccRecipients(List<String> bccRecipients)
	{
		this.bccRecipients = bccRecipients;
	}
	public String getMessageId()
	{
		return messageId;
	}
	public void setMessageId(String messageId)
	{
		this.messageId = messageId;
	}
	public static EmailMessage fromMimeMessage(MimeMessage msg) throws EmailException
	{
		EmailMessage email = new EmailMessage();

		try
		{
			email.setMessageId(msg.getMessageID());
			email.setSubject(msg.getSubject());
			
			// TODO set other properties
			
			return email;
		}
		catch (MessagingException e)
		{
			e.printStackTrace();
			throw new EmailException("Error converting MIME message to Kommet message");
		}
	}
	public String getInReplyTo()
	{
		return inReplyTo;
	}
	public void setInReplyTo(String inReplyTo)
	{
		this.inReplyTo = inReplyTo;
	}
	public String getSource()
	{
		return source;
	}
	public void setSource(String source)
	{
		this.source = source;
	}
}