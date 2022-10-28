/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.util.Date;

import javax.inject.Inject;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.Email;
import kommet.basic.keetle.LayoutService;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.emailing.EmailFilter;
import kommet.emailing.EmailService;
import kommet.emailing.EmailUtil;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.UrlUtil;

@Controller
public class MailboxController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@Inject
	EmailService emailService;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	@Inject
	LayoutService layoutService;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/mail/message/{id}", method = RequestMethod.GET)
	public ModelAndView messageDetails(@PathVariable("id") String sMessageId,
									HttpSession session) throws KommetException
	{	
		EnvData env = envService.getCurrentEnv(session);
		
		KID messageId = null;
		try
		{
			messageId = KID.get(sMessageId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Incorrect email ID");
		}
		
		Email email = emailService.get(messageId, env);
		if (email == null)
		{
			return getErrorPage("Email with ID " + messageId.getId() + " not found");
		}
		
		return getMessageDetailsView(email, AuthUtil.getAuthData(session), env);
	}
	
	private ModelAndView getMessageDetailsView (Email email, AuthData authData, EnvData env) throws KommetException
	{
		ModelAndView mv = new ModelAndView("mail/message");
		mv.addObject("email", email);
		mv.addObject("subject", StringUtils.hasText(email.getSubject()) ? email.getSubject() : authData.getI18n().get("mail.nosubject"));
		mv.addObject("layoutPath", layoutService.getDefaultLayoutId(authData, env) != null ? (env.getId() + "/" + layoutService.getDefaultLayoutId(authData, env)) : null);
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/mail/create", method = RequestMethod.GET)
	public ModelAndView createEmail(@RequestParam(value = "recipients", required = false) String recipients,
									@RequestParam(value = "subject", required = false) String subject, HttpSession session) throws KommetException
	{	
		EnvData env = envService.getCurrentEnv(session);
		
		ModelAndView mv = new ModelAndView("mail/create");
		mv.addObject("recipients", recipients);
		mv.addObject("subject", subject);
		addLayoutPath(uchService, mv, AuthUtil.getAuthData(session), env);
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/mail/send", method = RequestMethod.POST)
	public ModelAndView sendEmail(@RequestParam(value = "recipients", required = false) String recipients,
									@RequestParam(value = "subject", required = false) String subject,
									@RequestParam(value = "content", required = false) String content,
									HttpSession session) throws KommetException
	{	
		clearMessages();
		
		AuthData authData = AuthUtil.getAuthData(session);
		
		if (!StringUtils.hasText(recipients))
		{
			addError(authData.getI18n().get("mail.err.emptyrecipients"));
		}
		
		if (!StringUtils.hasText(subject))
		{
			addError(authData.getI18n().get("mail.err.emptysubject"));
		}
		
		if (!StringUtils.hasText(content))
		{
			addError(authData.getI18n().get("mail.err.emptycontent"));
		}
		
		EnvData env = envService.getCurrentEnv(session);
		
		if (hasErrorMessages())
		{
			ModelAndView mv = new ModelAndView("mail/create");
			mv.addObject("subject", subject);
			mv.addObject("recipients", recipients);
			mv.addObject("content", content);
			mv.addObject("errorMsgs", getErrorMsgs());
			mv.addObject("layoutPath", layoutService.getDefaultLayoutId(authData, env) != null ? (env.getId() + "/" + layoutService.getDefaultLayoutId(authData, env)) : null);
			return mv;
		}
		
		// send email
		emailService.sendEmail(subject, EmailUtil.parseRecipients(recipients), content, null, null, authData, null);
		
		Email email = new Email();
		
		// save sent email to DB
		try
		{
			email.setSubject(subject);
			email.setSendDate(new Date());
			email.setStatus("Sent");
			email.setRecipients(recipients);
			email.setPlainTextBody(content);
			email.setSender(authData.getUser().getEmail());
			emailService.save(email, authData, env);
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			ModelAndView mv = new ModelAndView("mail/create");
			mv.addObject("subject", subject);
			mv.addObject("recipients", recipients);
			mv.addObject("content", content);
			mv.addObject("errorMsgs", getMessage(authData.getI18n().get("mail.error.storing.mail")));
			mv.addObject("layoutPath", layoutService.getDefaultLayoutId(authData, env) != null ? (env.getId() + "/" + layoutService.getDefaultLayoutId(authData, env)) : null);
			return mv;
		}
		
		ModelAndView mv = getMessageDetailsView(email, AuthUtil.getAuthData(session), env);
		mv.addObject("actionMsgs", getMessage(authData.getI18n().get("mail.messagesent")));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/mail/sent", method = RequestMethod.GET)
	public ModelAndView sentMessageList(HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		// get emails sent from the current user's email
		EmailFilter filter = new EmailFilter();
		filter.addSender(authData.getUser().getEmail());
		
		ModelAndView mv = new ModelAndView("mail/sent");
		mv.addObject("emails", emailService.get(filter, env));
		return mv;
	}
}