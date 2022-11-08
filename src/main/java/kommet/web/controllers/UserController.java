/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.auth.ProfileService;
import kommet.auth.UserService;
import kommet.basic.App;
import kommet.basic.AppUrl;
import kommet.basic.BasicSetupService;
import kommet.basic.DocTemplate;
import kommet.basic.Layout;
import kommet.basic.Profile;
import kommet.basic.RecordProxyUtil;
import kommet.basic.User;
import kommet.basic.UserSettings;
import kommet.basic.keetle.LayoutService;
import kommet.basic.types.SystemTypes;
import kommet.config.UserSettingKeys;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.FieldValidationException;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.ValidationMessage;
import kommet.data.datatypes.SpecialValue;
import kommet.docs.DocTemplateService;
import kommet.docs.DocTemplateUtil;
import kommet.emailing.EmailService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.filters.UserFilter;
import kommet.i18n.I18nDictionary;
import kommet.i18n.InternationalizationService;
import kommet.i18n.Locale;
import kommet.rest.RestUtil;
import kommet.security.RestrictedAccess;
import kommet.services.AppService;
import kommet.services.SystemSettingService;
import kommet.systemsettings.SystemSettingKey;
import kommet.uch.UserCascadeHierarchyContext;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;



@Controller
public class UserController extends BasicRestController
{
	@Inject
	EnvService envService;
	
	@Inject
	DataService dataService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	UserService userService;
	
	@Inject
	LayoutService layoutService;
	
	@Inject
	InternationalizationService i18nService;
	
	@Inject
	EmailService emailService;
	
	@Inject
	DocTemplateService templateService;
	
	@Inject
	SystemSettingService settingService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	SystemSettingService sysSettingService;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	@Inject
	AppService appService;
	
	private static final Logger log = LoggerFactory.getLogger(UserController.class);
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/changepassword", method = RequestMethod.GET)
	public ModelAndView changePassword(HttpSession session) throws KommetException
	{
		AuthData authData = AuthUtil.getAuthData(session);
		
		EnvData env = envService.getCurrentEnv(session);
		
		ModelAndView mv = new ModelAndView("users/changepassword");
		mv.addObject("i18n", authData.getI18n());
		addLayoutPath(uchService, mv, authData, env);
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/forgottenpassword", method = RequestMethod.GET)
	public ModelAndView forgottenPassword(@RequestParam(value = "envId", required = false) String envId, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("users/forgottenpassword");
		
		EnvData env = null;
		if (StringUtils.hasText(envId))
		{
			env = envService.get(KID.get(envId));
		}
		
		mv.addObject("i18n", i18nService.getDictionary(getDefaultLocale(env, settingService)));
		mv.addObject("envId", envId);
		return mv;
	}
	
	/**
	 * This method sets up access to the environment for the user identified by the email.
	 * @param email
	 * @param appName
	 * @param domain
	 * @param session
	 * @param response
	 * @throws KommetException
	 * @throws IOException
	 */
	// we cannot use @RestrictedAccess with this method, because it is called through REST API and when it's called, the user credentials may not be
	// known - they are determined inside the method by the access_token
	// TODO implement determining profile by access token in the RestrictedAccessAspect
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/env/setupaccess", method = RequestMethod.POST)
	@ResponseBody
	public void setupAccess(@RequestParam("email") String email, @RequestParam("appName") String appName, @RequestParam("domain") String domain,
								@RequestParam(value = "env", required = false) String envId,
								@RequestParam(value = "access_token", required = false) String accessToken,
								HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{	
		RestInitInfo restInfo = prepareRest(envId, accessToken, session, resp);
		
		if (restInfo.getAuthData() == null || !AuthUtil.isRoot(restInfo.getAuthData()))
		{
			restInfo.getOut().write(RestUtil.getRestErrorResponse("Access denied"));
			return;
		}
		
		try
		{	
			User newUser = new User();
			newUser.setUserName(email);
			newUser.setEmail(email);
			newUser.setIsActive(false);
			newUser.setTimezone("GMT");
			newUser.setLocale("EN_US");
			newUser.setProfile(profileService.getProfileByName(Profile.SYSTEM_ADMINISTRATOR_NAME, restInfo.getEnv()));
			
			String hash = MiscUtils.getHash(30);
			newUser.setActivationHash(hash);
			
			// save new user
			newUser = userService.save(newUser, restInfo.getAuthData(), restInfo.getEnv());
			
			// create app with the given name if it doesn' exist
			App app = appService.getAppByName(appName, restInfo.getAuthData(), restInfo.getEnv());
			
			if (app == null)
			{
				app = new App();
				app.setName(appName);
				app.setLabel(appName);
				app.setType("Internal app");
				app = appService.save(app, restInfo.getAuthData(), restInfo.getEnv());
			}
			
			// create app domain
			AppUrl url = new AppUrl();
			url.setApp(app);
			url.setUrl(domain);
			appService.save(url, restInfo.getAuthData(), restInfo.getEnv(), envService.getMasterEnv());
			
			// allow users to log in
			uchService.saveUserSetting(UserSettingKeys.KM_SYS_CAN_LOGIN, "true", UserCascadeHierarchyContext.ENVIRONMENT, true, true, restInfo.getAuthData(), restInfo.getEnv());
			
			// display collections on record details by default
			uchService.saveUserSetting(UserSettingKeys.KM_SYS_DISPLAY_COLLECTIONS_IN_RECORD_DETAILS, "true", UserCascadeHierarchyContext.ENVIRONMENT, true, true, restInfo.getAuthData(), restInfo.getEnv());
			
			String activationLink = "https://app.kommet.io/km/users/activate?envId=" + restInfo.getEnv().getId() + "&hash=" + hash;
			restInfo.getOut().write(RestUtil.getRestSuccessDataResponse("{ \"link\": \"" + activationLink + "\" }"));
			return;
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			restInfo.getOut().write(RestUtil.getRestSuccessResponse("Generating link failed: " + e.getMessage()));
			return;
		}
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/user/getactivationlink", method = RequestMethod.GET)
	@ResponseBody
	public void generateActivationLink(@RequestParam("userId") String sUserId, HttpSession session, HttpServletResponse response) throws KommetException, IOException
	{
		PrintWriter out = response.getWriter();
		
		try
		{
			EnvData env = envService.getCurrentEnv(session);
			KID userId = KID.get(sUserId);
			User user = userService.getUser(userId, env);
			
			String hash = MiscUtils.getHash(30);
			String link = "https://kommet.io/km/users/activate?envId=" + env.getId() + "&hash=" + hash;
			
			// update user fields
			user.setActivationHash(hash);
			userService.save(user, AuthUtil.getAuthData(session), env);
			out.write(RestUtil.getRestSuccessDataResponse("{ \"link\": \"" + link + "\" }"));
			return;
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			out.write(RestUtil.getRestSuccessResponse("Generating link failed: " + e.getMessage()));
			return;
		}
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/forceactivate", method = RequestMethod.POST)
	@ResponseBody
	public void forceActivate(@RequestParam("userId") String sUserId, HttpSession session, HttpServletResponse response) throws KommetException, IOException
	{
		PrintWriter out = response.getWriter();
		
		try
		{
			EnvData env = envService.getCurrentEnv(session);
			KID userId = KID.get(sUserId);
			User user = userService.getUser(userId, env);
			
			// update user fields
			user.setActivationHash(null);
			user.setIsActive(true);
			userService.save(user, AuthUtil.getAuthData(session), env);
			out.write(getSuccessJSON("User activated"));
			return;
		}
		catch (KommetException e)
		{
			out.write(getErrorJSON("User activation failed"));
			return;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/activate", method = RequestMethod.GET)
	public ModelAndView activateUser(@RequestParam("hash") String hash, @RequestParam(name = "envId", required = false) String envId, HttpSession session) throws KommetException
	{	
		if (StringUtils.isEmpty(envId))
		{
			envId = appConfig.getDefaultEnvId().getId();
		}
		
		EnvData env = envService.get(KID.get(envId));
		I18nDictionary i18n = i18nService.getDictionary(getDefaultLocale(env, settingService));
		
		// find user by activation hash
		UserFilter filter = new UserFilter();
		filter.setActivationHash(hash);
		List<User> users = userService.get(filter, env);
		
		ModelAndView mv = new ModelAndView("users/activate");
		mv.addObject("i18n", i18n);
		mv.addObject("envId", envId);
		mv.addObject("hash", hash);
		
		if (users.isEmpty())
		{
			mv.addObject("errorMsgs", getMessage(i18n.get("auth.incorrect.activation.link")));
			mv.addObject("showForm", false);
			return mv;
		}
		else if (users.size() > 1)
		{
			mv.addObject("errorMsgs", getMessage("More than one user found with the given activation code"));
			mv.addObject("showForm", true);
			return mv;
		}
		
		log.debug("Activating user " + users.get(0).getUserName());
		
		mv.addObject("user", users.get(0));
		mv.addObject("showForm", true);
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/restorepassword", method = RequestMethod.GET)
	public ModelAndView restorePassword(@RequestParam("hash") String hash, @RequestParam("envId") String envId, HttpSession session) throws KommetException
	{	
		EnvData env = envService.get(KID.get(envId));
		I18nDictionary i18n = i18nService.getDictionary(getDefaultLocale(env, settingService));
		
		// find user by hash
		UserFilter filter = new UserFilter();
		filter.setForgottenPasswordHash(hash);
		List<User> users = userService.get(filter, env);
		
		ModelAndView mv = new ModelAndView("users/restorepassword");
		mv.addObject("i18n", i18n);
		mv.addObject("envId", envId);
		mv.addObject("hash", hash);
		
		if (users.isEmpty())
		{
			mv.addObject("errorMsgs", getMessage(i18n.get("auth.incorrect.pwd.restore.link")));
			mv.addObject("showForm", false);
			return mv;
		}
		else if (users.size() > 1)
		{
			mv.addObject("errorMsgs", getMessage("More than one user found with the given activation code"));
			mv.addObject("showForm", true);
			return mv;
		}
		
		mv.addObject("user", users.get(0));
		mv.addObject("showForm", true);
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/dochangepassword", method = RequestMethod.POST)
	public ModelAndView doChangePassword(@RequestParam(value = "oldPassword", required = false) String oldPassword,
									@RequestParam(value = "newPassword", required = false) String newPassword,
									@RequestParam(value = "newPasswordRepeated", required = false) String newPasswordRepeated,
									HttpSession session) throws KommetException
	{
		clearMessages();
		AuthData authData = AuthUtil.getAuthData(session);
		EnvData env = envService.getCurrentEnv(session);
		
		ModelAndView mv = new ModelAndView("users/changepassword");
		mv.addObject("layoutPath", layoutService.getDefaultLayoutId(authData, env) != null ? (env.getId() + "/" + layoutService.getDefaultLayoutId(authData, env)) : null);
		
		List<Record> users = env.getSelectCriteriaFromDAL("SELECT id, userName, timezone, locale, email, profile.id, profile.name FROM " + SystemTypes.USER_API_NAME + " WHERE userName = '" + authData.getUser().getUserName() + "' AND password = '" + MiscUtils.getSHA1Password(oldPassword) + "'").list();
		if (users.isEmpty())
		{
			addError(authData.getI18n().get("user.incorrectoldpwd"));
		}
		
		if (!StringUtils.hasText(newPassword))
		{
			addError(authData.getI18n().get("user.newpasswordempty"));
		}
		else
		{
			int minPwdLength = sysSettingService.getSettingIntValue(SystemSettingKey.MIN_PASSWORD_LENGTH, env);
			
			if (!StringUtils.hasText(newPasswordRepeated))
			{
				addError(authData.getI18n().get("user.repeatedpasswordempty"));
			}
			else if (!newPassword.equals(newPasswordRepeated))
			{
				addError(authData.getI18n().get("user.passwordsdontmatch"));
			}
			else if (newPassword.length() < minPwdLength)
			{
				addError(authData.getI18n().get("user.pwdtooshort.pre") + " " + minPwdLength + " " + authData.getI18n().get("user.pwdtooshort.post"));
			}
		}
		
		if (hasErrorMessages())
		{
			mv.addObject("errorMsgs", getErrorMsgs());
			mv.addObject("i18n", authData.getI18n());
			return mv;
		}
		
		// change password
		User user = userService.getUser(authData.getUserId(), env);
		
		// changing password for root is performed differently because it is a system-immutable record
		if (BasicSetupService.ROOT_USERNAME.equals(user.getUserName()))
		{
			userService.updateRootPassword(MiscUtils.getSHA1Password(newPassword), env);
		}
		else
		{
		user.setPassword(MiscUtils.getSHA1Password(newPassword));
		
		// user root auth data to update the password
		userService.save(user, AuthData.getRootAuthData(env), env);
		}
		
		mv.addObject("actionMsgs", getMessage(authData.getI18n().get("user.pwdchangedsuccess")));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/sendpasswordlink", method = RequestMethod.POST)
	public ModelAndView sendPasswordLink(@RequestParam(value = "email", required = false) String email,
										@RequestParam(value = "envId", required = false) String envId, HttpSession session) throws KommetException
	{
		clearMessages();
		I18nDictionary i18n = i18nService.getDictionary(Locale.EN_US);
		EnvData env = null;
		List<User> users = null;
		
		if (!StringUtils.hasText(envId))
		{
			addError(i18n.get("auth.envid.empty"));
			
			// go back to forgotten password form
			ModelAndView mv = new ModelAndView("users/forgottenpassword");
			mv.addObject("errorMsgs", getErrorMsgs());
			mv.addObject("envId", envId);
			mv.addObject("i18n", i18n);
			return mv;
		}
		else
		{
			env = envService.get(KID.get(envId));
			i18n = i18nService.getDictionary(getDefaultLocale(env, settingService));
		}
		
		if (!StringUtils.hasText(email))
		{
			addError(i18n.get("auth.email.empty"));
		}
		else
		{
			// check if user exists with this email
			UserFilter filter = new UserFilter();
			filter.setEmail(email);
			users = userService.get(filter, env);
			if (users.isEmpty())
			{
				addError(i18n.get("auth.email.user.not.found"));
			}
			// make sure the user is active - password reminders can only be sent to active and activated users
			else if (!Boolean.TRUE.equals(users.get(0).getIsActive()))
			{
				addError(i18n.get("auth.cannot.remind.pwd.inactive.user"));
			}
		}
		
		if (hasErrorMessages())
		{
			// go back to forgotten password form
			ModelAndView mv = new ModelAndView("users/forgottenpassword");
			mv.addObject("errorMsgs", getErrorMsgs());
			mv.addObject("envId", envId);
			mv.addObject("i18n", i18n);
			return mv;
		}
		
		String hash = MiscUtils.getHash(100);
		
		// update user with hash
		Record user = RecordProxyUtil.generateRecord(users.get(0), env.getTypeByRecordId(users.get(0).getId()), 2, env);
		user.setField("forgottenPasswordHash", hash);
		dataService.save(user, env);
		
		String link = "http://" + appConfig.getDefaultDomain() + "/km/users/restorepassword?hash=" + hash + "&envId=" + envId;
		
		DocTemplate emailTemplate = templateService.getByName(appConfig.getRestorePasswordEmailTemplate(), env);
		
		if (emailTemplate == null)
		{
			// TODO log fatal error here
			throw new KommetException("RestorePasswordEmail template not configured on server");
		}
		
		Map<String, Object> values = new HashMap<String, Object>();
		values.put("link", link);
		
		// send email
		emailService.sendEmail(i18n.get("auth.forgotten.pwd.email.subject"), email, DocTemplateUtil.interprete(emailTemplate.getContent(), values), null);
		
		return getMessagePage(i18n.get("auth.pwd.restore.email.sent"));
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/dorestorepassword", method = RequestMethod.POST)
	public ModelAndView doRestorePassword(@RequestParam(value = "hash", required = false) String hash,
									@RequestParam(value = "newPassword", required = false) String newPassword,
									@RequestParam(value = "newPasswordRepeated", required = false) String newPasswordRepeated,
									@RequestParam(value = "envId", required = false) String envId,
									HttpSession session) throws KommetException
	{
		clearMessages();
		EnvData env = envService.get(KID.get(envId));
		I18nDictionary i18n = i18nService.getDictionary(getDefaultLocale(env, settingService));
		
		ModelAndView mv = new ModelAndView("users/restorepassword");
		mv.addObject("envId", envId);
		mv.addObject("i18n", i18n);
		mv.addObject("hash", hash);
		mv.addObject("showForm", true);
		
		// find user by hash
		UserFilter filter = new UserFilter();
		filter.setForgottenPasswordHash(hash);
		List<User> users = userService.get(filter, env);
		
		if (users.isEmpty())
		{
			mv.addObject("errorMsgs", getMessage(i18n.get("auth.incorrect.pwd.restore.link")));
			return mv;
		}
		else if (users.size() > 1)
		{
			mv.addObject("errorMsgs", getMessage("More than one user found with the given activation code"));
			return mv;
		}
		
		if (!StringUtils.hasText(newPassword))
		{
			addError(i18n.get("user.newpasswordempty"));
		}
		else
		{
			if (!StringUtils.hasText(newPasswordRepeated))
			{
				addError(i18n.get("user.repeatedpasswordempty"));
			}
			else if (!newPassword.equals(newPasswordRepeated))
			{
				addError(i18n.get("user.passwordsdontmatch"));
			}
		}
		
		if (hasErrorMessages())
		{
			mv.addObject("errorMsgs", getErrorMsgs());
			return mv;
		}
		
		// change password
		Record user = RecordProxyUtil.generateRecord(users.get(0), env.getTypeByRecordId(users.get(0).getId()), 2, env);
		user.setField("password", MiscUtils.getSHA1Password(newPassword));
		// clear hash
		user.setField("forgottenPasswordHash", SpecialValue.NULL);
		dataService.save(user, env);
		
		// redirect to login page with appropriate message
		mv = new ModelAndView("auth/login");
		mv.addObject("actionMsgs", getMessage(i18n.get("user.pwdchangedsuccess")));
		mv.addObject("envId", envId);
		mv.addObject("i18n", i18n);
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/doactivate", method = RequestMethod.POST)
	public ModelAndView doActivateUser(@RequestParam(value = "hash", required = false) String hash,
									@RequestParam(value = "newPassword", required = false) String newPassword,
									@RequestParam(value = "newPasswordRepeated", required = false) String newPasswordRepeated,
									@RequestParam(value = "envId", required = false) String envId,
									HttpSession session) throws KommetException
	{
		clearMessages();
		EnvData env = envService.get(KID.get(envId));
		I18nDictionary i18n = i18nService.getDictionary(getDefaultLocale(env, settingService));
		
		ModelAndView mv = new ModelAndView("users/activate");
		mv.addObject("envId", envId);
		mv.addObject("i18n", i18n);
		mv.addObject("hash", hash);
		mv.addObject("showForm", true);
		
		// find user by hash
		UserFilter filter = new UserFilter();
		filter.setActivationHash(hash);
		List<User> users = userService.get(filter, env);
		
		if (users.isEmpty())
		{
			mv.addObject("errorMsgs", getMessage(i18n.get("auth.incorrect.activation.link")));
			return mv;
		}
		else if (users.size() > 1)
		{
			mv.addObject("errorMsgs", getMessage("More than one user found with the given activation code"));
			return mv;
		}
		
		if (!StringUtils.hasText(newPassword))
		{
			addError(i18n.get("user.newpasswordempty"));
		}
		else
		{
			if (!StringUtils.hasText(newPasswordRepeated))
			{
				addError(i18n.get("user.repeatedpasswordempty"));
			}
			else if (!newPassword.equals(newPasswordRepeated))
			{
				addError(i18n.get("user.passwordsdontmatch"));
			}
		}
		
		if (hasErrorMessages())
		{
			mv.addObject("errorMsgs", getErrorMsgs());
			return mv;
		}
		
		if (BasicSetupService.ROOT_USERNAME.equals(users.get(0).getUserName()))
		{
			userService.activateRoot(MiscUtils.getSHA1Password(newPassword), env);
		}
		else
		{
		// change password
		Record user = RecordProxyUtil.generateRecord(users.get(0), env.getTypeByRecordId(users.get(0).getId()), 2, env);
		user.setField("password", MiscUtils.getSHA1Password(newPassword));
			
		// clear hash
		user.setField("activationHash", SpecialValue.NULL);
		user.setField("isActive", true);
		dataService.save(user, env);
		}
		
		// redirect to login page with appropriate message
		mv = new ModelAndView("auth/login");
		mv.addObject("actionMsgs", getMessage(i18n.get("user.activation.success")));
		mv.addObject("envId", envId);
		mv.addObject("i18n", i18n);
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/me", method = RequestMethod.GET)
	public ModelAndView myProfile(HttpSession session) throws KommetException
	{
		AuthData authData = AuthUtil.getAuthData(session);
		EnvData env = envService.getCurrentEnv(session);
		User user = userService.getUser(authData.getUserId(), env);
		
		ModelAndView mv = new ModelAndView("users/myprofile");
		mv.addObject("user", user);
		mv.addObject("canChangePwd", true);
		mv.addObject("canEdit", false);
		// show notification is user has access to notification type, which should always be the case
		mv.addObject("showNotifications", authData.canReadType(env.getType(KeyPrefix.get(KID.NOTIFICATION_PREFIX)).getKID(), false, env));
		mv.addObject("i18n", authData.getI18n());
		addLayoutPath(uchService, mv, authData, env);
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/new", method = RequestMethod.GET)
	public ModelAndView newUser(HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("users/edit");
		EnvData env = envService.getCurrentEnv(session);
		mv.addObject("profiles", profileService.getProfiles(env));
		mv.addObject("locales", Locale.values());
		mv.addObject("layouts", layoutService.find(null, env));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/user/save", method = RequestMethod.POST)
	public ModelAndView saveUser (@RequestParam(value = "userId", required = false) String id,
									@RequestParam(value = "userName", required = false) String userName,
									@RequestParam(value = "email", required = false) String email,
									@RequestParam(value = "profileId", required = false) String profileId,
									@RequestParam(value = "password", required = false) String password,
									@RequestParam(value = "timezone", required = false) String timezone,
									@RequestParam(value = "locale", required = false) String sLocale,
									@RequestParam(value = "repeatedPassword", required = false) String repeatedPassword,
									@RequestParam(value = "layoutId", required = false) String sLayoutId,
									HttpSession session)
									throws KommetException
	{
		clearMessages();
		KID userId = null;
		
		Record user = null;
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		if (!authData.canEditType(env.getType(KeyPrefix.get(KID.USER_PREFIX)).getKID(), false, env))
		{
			return getErrorPage("Insufficient privileges to edit user");
		}
		
		if (StringUtils.hasText(id))
		{
			try
			{
				userId = KID.get(id);
			}
			catch (KIDException e)
			{
				e.printStackTrace();
				return getErrorPage("Invalid profile id " + id);
			}
			
			List<Record> users = env.getSelectCriteriaFromDAL("select id, userName, isActive, password, email, createdDate, profile.id, profile.name from " + SystemTypes.USER_API_NAME + " where id = '" + userId.getId() + "'").list();
			if (!users.isEmpty())
			{
				user = users.get(0);
			}
			else
			{
				getErrorPage("No user found with ID " + id);
			}
		}
		else
		{
			user = new Record(env.getType(KeyPrefix.get(KID.USER_PREFIX)));
			user.setField(Field.CREATEDDATE_FIELD_NAME, new Date());
		}
		
		// Password is not updated every time a user is saved.
		// Password is updated only if any of the password fields has been filled.
		boolean isUpdatingPwd = StringUtils.hasText(password) || StringUtils.hasText(repeatedPassword);
		
		if (isUpdatingPwd)
		{
			if (!StringUtils.hasText(password))
			{
				addError("Please type your password");
			}
			else
			{
				if (!StringUtils.hasText(repeatedPassword))
				{
					addError("Please repeat your password");
				}
				else if (!password.equals(repeatedPassword))
				{
					addError("Passwords don't match");
				}
				else
				{
					user.setField("password", MiscUtils.getSHA1Password(password));
				}
			}
		}
		
		if (StringUtils.hasText(timezone))
		{
			user.setField("timezone", timezone);
		}
		else
		{
			addError("Please select time zone");
		}
		
		if (StringUtils.hasText(profileId))
		{
			user.setField("profile.id", KID.get(profileId), env);
		}
		else
		{
			addError("Please select profile fo the user");
		}
		
		if (StringUtils.hasText(sLocale))
		{
			if (Locale.valueOf(sLocale) == null)
			{
				addError("Unsupported locale " + sLocale);
			}
			else
			{
				user.setField("locale", sLocale);
			}
		}
		else 
		{
			addError("Please select user's language");
		}
		
		ModelAndView mv = new ModelAndView("users/edit");
		
		validateNotEmpty(userName, "User name");
		validateNotEmpty(email, "E-mail");
		
		if (userName != null)
		{
			userName = userName.trim();
		}
		if (email != null)
		{
			email = email.trim();
		}
		
		user.setField("userName", userName);
		user.setField("email", email);
		
		if (user.attemptGetKID() == null && !user.isSet("isActive"))
		{
			// by default the user will be inactive
			user.setField("isActive", false);
		}
		
		if (hasErrorMessages())
		{
			mv.addObject("errorMsgs", getErrorMsgs());
			mv.addObject("user", RecordProxyUtil.generateStandardTypeProxy(User.class, user, true, env));
			mv.addObject("profiles", profileService.getProfiles(env));
			mv.addObject("layouts", layoutService.find(null, env));
			mv.addObject("locales", Locale.values());
			return mv;
		}
		
		try
		{
			user = dataService.save(user, authData, env);
		}
		catch (FieldValidationException e)
		{
			for (ValidationMessage msg : e.getMessages())
      		{
      			addError(msg.getText());
      		}
			
			mv.addObject("errorMsgs", getErrorMsgs());
			mv.addObject("user", new User(user, env));
			mv.addObject("profiles", profileService.getProfiles(env));
			mv.addObject("layouts", layoutService.find(null, env));
			return mv;
		}
		
		updateUserSettings(user, sLayoutId, authData, env);
		
		return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/user/" + user.getKID());	
	}
	
	private void updateUserSettings(Record user, String sLayoutId, AuthData authData, EnvData env) throws KommetException
	{
		if (user.attemptGetKID() == null)
		{
			throw new KommetException("Cannot update user settings for an unsaved user");
		}
		
		UserSettings userSettings = userService.getUserSettings(user.getKID(), env);
		
		// User settings are updated in two cases: when they already exist, or when any of them has
		// been set
		if (userSettings != null || sLayoutId != null)
		{	
			if (userSettings == null)
			{
				// if user settings does not exist, create it
				userSettings = new UserSettings();	
				User u = new User();
				u.setId(user.getKID());
				userSettings.setUser(u);
			}
			
			// if layout is set, update it, if not, clear this setting
			if (StringUtils.hasText(sLayoutId))
			{
				Layout defaultLayout = new Layout();
				defaultLayout.setId(KID.get(sLayoutId));
				userSettings.setLayout(defaultLayout);
			}
			else
			{
				userSettings.setLayout(null);
				userSettings.nullify("layout");
			}
			
			userService.save(userSettings, authData, env);
		}
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/user/{id}", method = RequestMethod.GET)
	public ModelAndView details(@PathVariable("id") String id, HttpSession session) throws KommetException
	{
		KID objId = null;
		try
		{
			objId = KID.get(id);
		}
		catch (KIDException e)
		{
			e.printStackTrace();
			return getErrorPage("Invalid object id " + id);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		
		List<Record> users = env.getSelectCriteriaFromDAL("select id, userName, email, createdDate, profile.id, profile.name, locale, timezone, activationHash, isActive from " + SystemTypes.USER_API_NAME + " where id = '" + id + "'").list();
		
		if (users.isEmpty())
		{
			return getErrorPage("No user found with ID " + objId);
		}
		else if (users.size() > 1)
		{
			return getErrorPage("More than one user found with ID " + objId);
		}
		
		ModelAndView mv = new ModelAndView("users/details");
		User user = new User(users.get(0), env);
		mv.addObject("user", user);
		mv.addObject("userSettings", userService.getUserSettings(objId, env));
		
		AuthData authData = AuthUtil.getAuthData(session);
		mv.addObject("canEdit", authData.canEditType(env.getType(KeyPrefix.get(KID.USER_PREFIX)).getKID(), false, env));
		mv.addObject("canLogin", AuthUtil.canLogin(authData, user.getId()));
		mv.addObject("canSendNotification", AuthUtil.isRoot(authData));
		mv.addObject("canForceActivate", AuthUtil.isRoot(authData));
		mv.addObject("canAddComments", authData.canCreateType(env.getType(KeyPrefix.get(KID.COMMENT_PREFIX)).getKID(), true, env));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/edit/{id}", method = RequestMethod.GET)
	public ModelAndView userEdit(@PathVariable("id") String id, HttpSession session) throws KommetException
	{
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		if (!authData.canEditType(env.getType(KeyPrefix.get(KID.USER_PREFIX)).getKID(), false, env))
		{
			return getErrorPage("Insufficient privileges to edit user");
		}
		
		KID objId = null;
		try
		{
			objId = KID.get(id);
		}
		catch (KIDException e)
		{
			e.printStackTrace();
			return getErrorPage("Invalid object id " + id);
		}
		
		List<Record> users = env.getSelectCriteriaFromDAL("select id, locale, userName, createdDate, email, profile.id, profile.name, timezone from " + SystemTypes.USER_API_NAME + " where id = '" + id + "'").list();
		
		if (users.isEmpty())
		{
			return getErrorPage("No user found with ID " + objId);
		}
		else if (users.size() > 1)
		{
			return getErrorPage("More than one user found with ID " + objId);
		}
		
		ModelAndView mv = new ModelAndView("users/edit");
		mv.addObject("user", new User(users.get(0), env));
		mv.addObject("userSettings", userService.getUserSettings(objId, env));
		mv.addObject("profiles", profileService.getProfiles(env));
		mv.addObject("layouts", layoutService.find(null, env));
		mv.addObject("locales", Locale.values());
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/users/list", method = RequestMethod.GET)
	public ModelAndView list(HttpSession session) throws KommetException
	{ 	
		/*EnvData env = envService.getCurrentEnv(session);
		Type type = new Type();
		type.setApiName("Pigeon");
		type.setLabel("Pigeon");
		type.setPluralLabel("Pigeons");
		type.setPackage(MiscUtils.userToEnvPackage("com.rm", env));*/
		
		ModelAndView mv = new ModelAndView("users/list");
		return mv;
	}
}