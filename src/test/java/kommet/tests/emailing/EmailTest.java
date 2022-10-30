/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.emailing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Date;

import javax.inject.Inject;

import org.junit.Test;

import kommet.auth.AuthData;
import kommet.basic.BasicSetupService;
import kommet.basic.Email;
import kommet.basic.RecordProxyUtil;
import kommet.config.UserSettingKeys;
import kommet.data.FieldValidationException;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.validationrules.ValidationRuleUninitializedFieldsMode;
import kommet.emailing.EmailFilter;
import kommet.emailing.EmailService;
import kommet.env.EnvData;
import kommet.koll.compiler.KommetCompiler;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.uch.UserCascadeHierarchyContext;
import kommet.uch.UserCascadeHierarchyService;

public class EmailTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	EmailService emailService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	@Test
	public void testEmailObject() throws KommetException
	{
		Email email = new Email();
		email.addRecipient("test@kommet.io");
		assertEquals("test@kommet.io", email.getRecipients());
		email.addRecipient("test2@kommet.io");
		assertEquals("test@kommet.io; test2@kommet.io", email.getRecipients());
		email.setRecipients(null);
		assertNull(email.getRecipients());
	}
	
	@Test
	public void testEmailCRUD() throws KommetException
	{
		EnvData env = dataHelper.getTestEnvData(false);
		basicSetupService.runBasicSetup(env);
		
		// create email
		Email email = new Email();
		email.setSubject("Hello people");
		email.addRecipient("test@kommet.io");
		email.setStatus("Draft");
		
		assertEquals("test@kommet.io", email.getRecipients());
		AuthData authData = dataHelper.getRootAuthData(env);
		
		Record emailRec = RecordProxyUtil.generateRecord(email, env.getType(KeyPrefix.get(KID.EMAIL_PREFIX)), 2, env);
		assertEquals("test@kommet.io", emailRec.getField("recipients"));
		
		Email emailProxy = (Email)RecordProxyUtil.generateStandardTypeProxy(emailRec, env, compiler);
		assertEquals("test@kommet.io", emailProxy.getRecipients());
		
		// save email
		try
		{
			emailService.save(email, authData, env);
			fail("Saving email with empty sender should fail");
		}
		catch (FieldValidationException e)
		{
			assertEquals(1, e.getMessages().size());
			assertEquals("Sender", e.getMessages().get(0).getFieldLabel());
		}
		
		uchService.saveUserSetting(UserSettingKeys.KM_ROOT_SYS_VALIDATION_RULE_UNINITIALIZED_FIELDS_MODE, ValidationRuleUninitializedFieldsMode.EVALUATE.getMode(), UserCascadeHierarchyContext.ENVIRONMENT, true, authData, env);
		
		email.setSender("mark@kommet.io");
		email = emailService.save(email, authData, env);
		assertNotNull(email.getId());
		
		// now try to set status to "Sent"
		email.setStatus("Sent");
		
		try
		{
			emailService.save(email, authData, env);
			fail("Saving email with status 'Sent' and send date/message ID not sent should fail");
		}
		catch (FieldValidationException e)
		{
			assertEquals(1, e.getMessages().size());
		}
		
		// set all required fields
		email.setSendDate(new Date());
		email.setMessageId("sjdcilw4@u4n3n2493c43n049394.vnmr3vvin59495vn4395");
		emailService.save(email, authData, env);
		
		// query by subject
		EmailFilter filter = new EmailFilter();
		filter.setSubjectLike("peo");
		assertEquals(1, emailService.get(filter, env).size());
		
		filter.setSubjectLike("sd");
		assertEquals(0, emailService.get(filter, env).size());
		
		assertNotNull(emailService.get(email.getId(), env));
	}
}
