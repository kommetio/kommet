/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.emailing;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.Email;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.utils.AppConfig;
import kommet.utils.PropertyUtilException;

/**
 * Service for sending emails.
 * @author Radek Krawiec
 * @created 2014
 */
@Service
public class EmailService
{
	@Inject
	JavaMailSender mailSender;
	
	@Inject
	EmailDao emailDao;
	
	@Inject
	AppConfig appConfig;
	
	private static final Logger log = LoggerFactory.getLogger(EmailService.class);
	
	public EmailMessage sendEmail(String subject, String recipient, String content, String htmlContent) throws EmailException
	{
		List<Recipient> recipients = new ArrayList<Recipient>();
		recipients.add(new Recipient(recipient));
		
		return sendEmail(subject, recipients, content, htmlContent);
	}
	
	public EmailMessage sendEmail (String subject, List<Recipient> recipients, String content) throws EmailException
	{
		return sendEmail(subject, recipients, content, null);
	}
	
	public EmailMessage sendEmail (String subject, List<Recipient> recipients, String content, String htmlContent) throws EmailException
	{
		return sendEmail(subject, recipients, content, htmlContent, null, null, null);
	}
	
	private JavaMailSender getMailSenderForAccount(EmailAccount account)
	{
	    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
	    mailSender.setHost(account.getSmtpHost());
	    mailSender.setPort(account.getSmtpPort());
	    mailSender.setProtocol("smtp");

	    Properties properties = new Properties();
	    properties.setProperty("mail.smtp.starttls.enable", "false");
	    properties.setProperty("mail.debug", "true");
	    
	    boolean isAuth = true;
	    
	    for (String key : account.getProperties().keySet())
	    {
	    	if ("noauth".equals(key))
	    	{
	    		isAuth = false;
	    	}
	    	else
	    	{
	    		properties.put(key, account.getProperties().get(key));
	    	}
	    }
	    
	    if (isAuth)
	    {
	    	mailSender.setUsername(account.getUserName());
		    mailSender.setPassword(account.getPassword());
		    properties.setProperty("mail.smtp.auth", "true");
	    }
	    else
	    {
	    	properties.setProperty("mail.smtp.auth", "false");
	    }
	    
	    // use different settings for gmail
	    if (account.getSmtpHost().endsWith("gmail.com"))
	    {
	    	properties.setProperty("mail.smtp.starttls.enable", "true");
	    	properties.setProperty("mail.smtp.auth", "true");
	    	properties.setProperty("mail.smtp.quitwait", "false");
	    	mailSender.setProtocol("smtp");
	    }
	    
	    /*properties.setProperty("mail.smtp.auth", "true");
	    properties.setProperty("mail.smtp.ssl.trust", "*");
	    properties.setProperty("mail.smtp.starttls.enable", "false");*/
	    mailSender.setJavaMailProperties(properties);

	    return mailSender;
	}
	
	/**
	 * Receive email from inbox for the given account.
	 * @param acc
	 * @param startDate
	 * @return
	 * @throws KommetException
	 */
	public List<EmailMessage> readEmails (EmailAccount acc, Date startDate) throws KommetException
	{
		if (startDate == null)
		{
			throw new KommetException("Start date not defined when receiving email");
		}
		
		Properties props = new Properties();
		List<EmailMessage> readEmails = new ArrayList<EmailMessage>();
		 
		props.setProperty("mail.store.protocol", "imaps");
		 
		try
		{
			Session session = Session.getInstance(props, null);
			
			boolean isGmail = acc.getImapHost().endsWith("gmail.com");
			Store store = isGmail ? session.getStore("imaps") : session.getStore();
			
			store.connect(acc.getImapHost(), acc.getUserName(), acc.getPassword());
			Folder inbox = store.getFolder("INBOX");
			inbox.open(Folder.READ_ONLY);
			
			SearchTerm newerThan = new ReceivedDateTerm(ComparisonTerm.GT, startDate);
			
			for (Message msg : inbox.search(newerThan))
			{
				EmailMessage email = new EmailMessage();
				email.setSubject(msg.getSubject());
				
				// get full content of the message
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				msg.writeTo(baos);
				email.setSource(baos.toString("UTF-8"));
				
				email.setSender(msg.getFrom() != null ? ((InternetAddress)msg.getFrom()[0]).getAddress() : null);
				email.setSendDate(msg.getReceivedDate());
				
				String[] replies = msg.getHeader("In-Reply-To");
				if (replies != null && replies.length > 0)
				{
					email.setInReplyTo(replies[0]);
				}
					
				readEmails.add(email);
		    }
			
			return readEmails;
		 }
		 catch (Exception e)
		 {
			 e.printStackTrace();
			 throw new KommetException("Error fetching messages");
		 }
	}

	/*public EmailMessage sendEmail (String subject, List<Recipient> recipients, String content, String htmlContent, Collection<File> attachments, AuthData authData, EmailAccount account) throws EmailException
	{
		List<Attachment> newAttachments = new ArrayList<Attachment>();
		
		if (attachments != null && !attachments.isEmpty())
		{
			java.io.File systemFile = null;
			
			for (File attachment : attachments)
			{
				try
				{
					systemFile = new java.io.File(appConfig.getFileDir() + "/" + attachment.getLatestRevision().getPath());
				}
				catch (KommetException e)
				{
					throw new EmailException("Error reading disk file for attachment: " + e.getMessage());
				}
				
				if (!systemFile.exists())
				{
					throw new EmailException("Attachment file " + attachment.getName() + " not found on server");
				}
				
				try
				{
					newAttachments.add(new Attachment(attachment.getName(), new FileInputStream(systemFile)));
				}
				catch (FileNotFoundException e)
				{
					throw new EmailException("Attachment file " + systemFile.getName() + " not found");
				}
			}
		}
		
		return sendEmail(subject, recipients, content, htmlContent, newAttachments, authData, account);	
	}*/
	
	public EmailMessage sendEmail (String subject, List<Recipient> recipients, String content, String htmlContent, Collection<Attachment> attachments, AuthData authData, EmailAccount account) throws EmailException
	{
		JavaMailSender javaMailSender = this.mailSender;
		
		String senderAddress = null;
		String senderName = null;
		String replyTo = null;
		
		try
		{
			if (account == null && !appConfig.isEmailFeatureActive())
			{
				// If email feature is inactive, the send action is skipped without errors.
				// This allows to test this feature on a machine where mail feature is not configured.
				log.debug("Emails deactivated in config. Not sending email [subject = \"" + subject + "\", recipients: " + EmailUtil.toRecipientList(recipients) + "]");
				return null;
			}
			
			if (account == null)
			{
				senderAddress = replyTo = "no-reply-mailer-daemon@" + appConfig.getDefaultDomain();
				senderName = "Kommet";
			}
			else
			{
				javaMailSender = getMailSenderForAccount(account);
				
				senderAddress = replyTo = account.getSenderAddress();
				senderName = account.getSenderName();
			}
		}
		catch (PropertyUtilException e)
		{
			throw new EmailException("Could not read email setting property: " + e.getMessage());
		}
		
		log.info("[Email] Sending mail to " + EmailUtil.toRecipientList(recipients));
		MimeMessage message = javaMailSender.createMimeMessage();
	 
		MimeMessageHelper helper;
		try
		{
			helper = new MimeMessageHelper(message, true, "UTF-8");
 
			for (Recipient recipient : recipients)
			{
				helper.addTo(recipient.getAddress());
			}
			
			try
			{
				if (authData == null || account != null)
				{
					helper.setFrom(senderAddress, senderName);
					helper.setReplyTo(replyTo);
				}
				else
				{
					helper.setFrom(authData.getUser().getEmail(), authData.getUser().getUserName());
					helper.setReplyTo(authData.getUser().getEmail());
				}
			}
			catch (UnsupportedEncodingException e)
			{
				// this exception will actually never occur
				throw new EmailException("Encoding error adding from to email");
			}
	
			helper.setSubject(subject);
			
			if (StringUtils.hasText(content))
			{
				if (StringUtils.hasText(htmlContent))
				{
					// set both plain and HTML text
					helper.setText(content, htmlContent);
				}
				else
				{
					// set only plain text
					helper.setText(content);
				}
			}
			else
			{
				if (StringUtils.hasText(htmlContent))
				{
					// set only HTML text
					helper.setText(htmlContent, true);
				}
				else
				{
					// both plain and HTML text is empty
					helper.setText("");
				}
			}
			
			if (attachments != null && !attachments.isEmpty())
			{	
				for (Attachment att : attachments)
				{
					java.io.File systemFile = null;
					
					try
					{	
						systemFile = new java.io.File(appConfig.getFileDir() + "/" + att.getFilePath());
					}
					catch (KommetException e)
					{
						throw new EmailException("Error reading disk file for attachment: " + e.getMessage());
					}
					
					if (!systemFile.exists())
					{
						throw new EmailException("Attachment file " + att.getName() + " not found on server");
					}
					
					helper.addAttachment(att.getName(), systemFile);
				}
			}
			
			javaMailSender.send(message);
			
			log.info("[Email] Sent mail [subject = \"" + subject + "\", to = \"" + EmailUtil.toRecipientList(recipients) + "\"]");
			
			return EmailMessage.fromMimeMessage(message);
		}
		catch (javax.mail.MessagingException e)
		{
			throw new EmailException("Error sending email to " + EmailUtil.toRecipientList(recipients) + ":\n" + e.getMessage());
		}
	}
	
	@Transactional(readOnly = true)
	public List<Email> get (EmailFilter filter, EnvData env) throws KommetException
	{
		return emailDao.get(filter, env);
	}
	
	@Transactional
	public Email save (Email email, AuthData authData, EnvData env) throws KommetException
	{
		return emailDao.save(email, authData, env);
	}

	@Transactional(readOnly = true)
	public Email get(KID id, EnvData env) throws KommetException
	{
		return emailDao.get(id, env);
	}
}