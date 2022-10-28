/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.koll;

import java.lang.reflect.Method;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;

import kommet.auth.AuthData;
import kommet.auth.LoginHistoryService;
import kommet.auth.UserService;
import kommet.basic.keetle.LayoutService;
import kommet.basic.keetle.ViewService;
import kommet.comments.CommentService;
import kommet.data.DataService;
import kommet.data.sharing.SharingService;
import kommet.dataexport.DataExportService;
import kommet.docs.DocTemplateService;
import kommet.emailing.EmailService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.files.FileService;
import kommet.http.HttpService;
import kommet.i18n.InternationalizationService;
import kommet.koll.compiler.KommetCompiler;
import kommet.notifications.NotificationService;
import kommet.services.FieldHistoryService;
import kommet.services.SystemSettingService;
import kommet.services.UserGroupService;
import kommet.services.ViewResourceService;
import kommet.testing.TestService;
import kommet.transactions.TransactionManager;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.AppConfig;

@Service
public class SystemContextFactory
{
	@Inject
	KommetCompiler compiler;
	
	@Inject
	EmailService emailService;
	
	@Inject
	SharingService sharingService;
	
	@Inject
	SystemSettingService systemSettingService;
	
	@Inject
	DataService dataService;
	
	@Inject
	DocTemplateService docTemplateService;
	
	@Inject
	FieldHistoryService fieldHistoryService;
	
	@Inject
	NotificationService notificationService;
	
	@Inject
	UserGroupService userGroupService;
	
	@Inject
	CommentService commentService;
	
	@Inject
	PlatformTransactionManager txManager;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	@Inject
	HttpService httpService;
	
	@Inject
	LoginHistoryService lhService;
	
	@Inject
	UserService userService;
	
	@Inject
	InternationalizationService i18n;
	
	@Inject
	ViewService viewService;
	
	@Inject
	LayoutService layoutService;
	
	@Inject
	FileService fileService;
	
	@Inject
	ViewResourceService viewResourceService;
	
	@Inject
	EnvService envService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	TestService testService;
	
	@Inject
	DataExportService dataExportService;
	
	public SystemContext get (AuthData authData, EnvData env)
	{
		return new SystemContext(compiler, appConfig, uchService, httpService, emailService, systemSettingService, sharingService, dataService,
								docTemplateService, fieldHistoryService, notificationService, userGroupService, commentService, i18n,
								lhService, userService, viewResourceService, viewService, layoutService, envService, testService, fileService, dataExportService, authData, env, new TransactionManager(txManager));
	}

	public void injectSystemContext(Object handlerClassInstance, AuthData authData, EnvData env) throws SystemContextException
	{
		// find methods annotated with @SystemContext
		Method[] methods = handlerClassInstance.getClass().getDeclaredMethods();
		
		SystemContext sys = get(authData, env);
		
		for (Method m : methods)
		{
			if (m.isAnnotationPresent(kommet.koll.annotations.InjectSystemContext.class))
			{
				Class<?>[] paramTypes = m.getParameterTypes();
				if (paramTypes.length == 1 && paramTypes[0].getName().equals(SystemContext.class.getName()))
				{
					// invoke setter
					try
					{
						m.invoke(handlerClassInstance, sys);
					}
					catch (Exception e)
					{
						e.printStackTrace();
						throw new SystemContextException("System context could not be injected. Error calling method " + handlerClassInstance.getClass().getName() + "." + m.getName());
					}
				}
				else
				{
					throw new SystemContextException("Method " + handlerClassInstance.getClass().getName() + "." + m.getName() + " is annotated with @" + kommet.koll.annotations.InjectSystemContext.class.getSimpleName() + " but does not take one parameter of type " + SystemContext.class.getName());
				}
			}
		}
	}
}