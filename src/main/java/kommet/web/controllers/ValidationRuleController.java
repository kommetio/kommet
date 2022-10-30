/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
import kommet.basic.Class;
import kommet.basic.Profile;
import kommet.basic.ValidationRule;
import kommet.basic.keetle.tags.breadcrumbs.Breadcrumbs;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.validationrules.ValidationRuleFilter;
import kommet.data.validationrules.ValidationRuleService;
import kommet.data.validationrules.ValidationRuleUtil;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.errorlog.ErrorLogService;
import kommet.errorlog.ErrorLogSeverity;
import kommet.koll.compiler.CompilationError;
import kommet.koll.compiler.CompilationResult;
import kommet.koll.compiler.KommetCompiler;
import kommet.labels.TextLabelFilter;
import kommet.labels.TextLabelService;
import kommet.rel.RELSyntaxException;
import kommet.rest.RestUtil;
import kommet.security.RestrictedAccess;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;
import kommet.utils.ValidationUtil;

@Controller
public class ValidationRuleController extends CommonKommetController
{
	@Inject
	ValidationRuleService vrService;
	
	@Inject
	EnvService envService;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	TextLabelService labelService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	ErrorLogService logService;
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/validationrules/delete", method = RequestMethod.POST)
	@ResponseBody
	public void delete(@RequestParam(value = "ruleId", required = false) String sRuleId, HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{	
		PrintWriter out = resp.getWriter();
		EnvData env = envService.getCurrentEnv(session);
		ValidationRule rule = vrService.get(KID.get(sRuleId), env);
		
		try
		{
			vrService.delete(rule.getId(), AuthUtil.getAuthData(session), env);
			out.write(RestUtil.getRestSuccessDataResponse("{ \"keyPrefix\": \"" + env.getType(rule.getTypeId()).getKeyPrefix() + "\" }"));
		}
		catch (Exception e)
		{
			AuthData authData = AuthUtil.getAuthData(session);
			logService.logException(e, ErrorLogSeverity.ERROR, this.getClass().getName(), 0, authData.getUserId(), authData, env);
			out.write(RestUtil.getRestErrorResponse("Deleting validation rule failed"));
		}
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/validationrules/list", method = RequestMethod.GET)
	public ModelAndView list(HttpSession session, HttpServletRequest req) throws KommetException
	{	
		EnvData env = envService.getCurrentEnv(session);
		ModelAndView mv = new ModelAndView("vrs/list");
		
		AuthData authData = AuthUtil.getAuthData(session);
		
		List<ValidationRuleWrapper> rules = new ArrayList<ValidationRuleWrapper>();
		List<ValidationRule> vrs = vrService.get(new ValidationRuleFilter(), env);
		for (ValidationRule vr : vrs)
		{
			// only root can see system rules
			if (Boolean.FALSE.equals(vr.getIsSystem()) || AuthUtil.isRoot(authData))
			{
				rules.add(new ValidationRuleWrapper(vr, env));
			}
		}
		
		mv.addObject("vrs", rules);
		
		// add breadcrumbs
		Breadcrumbs.add(req.getRequestURL().toString(), "Validation rules", appConfig.getBreadcrumbMax(), session);
		
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/typevalidationrules", method = RequestMethod.POST)
	public ModelAndView listForType(@RequestParam(value = "typeId", required = false) String sTypeId, HttpSession session) throws KommetException
	{
		ValidationRuleFilter filter = new ValidationRuleFilter();
		if (StringUtils.hasText(sTypeId))
		{
			filter.addTypeId(KID.get(sTypeId));
		}
		
		AuthData authData = AuthUtil.getAuthData(session);
		EnvData env = envService.getCurrentEnv(session);
		
		List<ValidationRuleWrapper> rules = new ArrayList<ValidationRuleWrapper>();
		List<ValidationRule> vrs = vrService.get(filter, env);
		for (ValidationRule vr : vrs)
		{
			// only root can see system rules
			if (Boolean.FALSE.equals(vr.getIsSystem()) || AuthUtil.isRoot(authData))
			{
				rules.add(new ValidationRuleWrapper(vr, env));
			}
		}
		
		ModelAndView mv = new ModelAndView("vrs/listfortype");
		mv.addObject("vrs", rules);
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/validationrules/save", method = RequestMethod.POST)
	public ModelAndView save(@RequestParam(value = "typeId", required = false) String sTypeId,
							@RequestParam(value = "ruleId", required = false) String sRuleId,
							@RequestParam(value = "name", required = false) String name,
							@RequestParam(value = "code", required = false) String code,
							@RequestParam(value = "errorMsg", required = false) String errorMessage,
							@RequestParam(value = "errorMsgLabel", required = false) String errorMessageLabel,
							@RequestParam(value = "isActive", required = false) String sIsActive,
							HttpSession session) throws KommetException
	{
		clearMessages();
		ValidationRule vr = null;
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		if (StringUtils.hasText(sRuleId))
		{
			vr = vrService.get(KID.get(sRuleId), env);
		}
		else
		{
			vr = new ValidationRule();
		}
		
		vr.setActive("true".equals(sIsActive));
		vr.setName(name);
		vr.setCode(code);
		vr.setTypeId(KID.get(sTypeId));
		vr.setIsSystem(false);
		
		if (StringUtils.hasText(errorMessage))
		{
			vr.setErrorMessage(errorMessage);
		}
		else
		{
			vr.setErrorMessage(null);
			vr.nullify("errorMessage");
		}
		
		if (StringUtils.hasText(errorMessageLabel))
		{
			// make sure the text label exists
			TextLabelFilter filter = new TextLabelFilter();
			filter.addKey(errorMessageLabel);
			if (labelService.get(filter, env).isEmpty())
			{
				addError("Text label with key " + errorMessageLabel + " does not exist");
			}
			
			vr.setErrorMessageLabel(errorMessageLabel);
		}
		else
		{
			vr.setErrorMessageLabel(null);
			vr.nullify("errorMessageLabel");
		}
		
		if (!StringUtils.hasText(errorMessageLabel) && !StringUtils.hasText(errorMessage))
		{
			addError("Either error message or error message text label needs to be specified");
		}
		
		if (!StringUtils.hasText(name))
		{
			addError("Name is empty");
		}
		else if (!ValidationUtil.isValidOptionallyQualifiedResourceName(name))
		{
			addError("Invalid validation rule name");
		}
		
		if (!StringUtils.hasText(code))
		{
			addError("REL evaluation is empty");
		}
		else
		{
			// check if REL is correct
			try
			{
				// in order to create an evaluator, we first need to extract the fields used in the condition
				// and set them on the validation rule
				ValidationRuleService.initReferencedFields(vr, env);
				
				Class vrFile = ValidationRuleUtil.getValidationRuleExecutor(env.getType(KID.get(sTypeId)), "TestExecutor_" + (new Random()).nextInt(10000), MiscUtils.toSet(vr), compiler, env);
				CompilationResult res = compiler.compile(vrFile, env);
				
				if (!res.isSuccess())
				{
					for (CompilationError err : res.getErrors())
					{
						addError("Incorrect REL expression: " + translateVRError(err.getMessage()));
					}
				}
			}
			catch (RELSyntaxException e)
			{
				addError("REL syntax error: " + e.getMessage());
			}
			catch (Exception e)
			{
				e.printStackTrace();
				logService.logException(e, ErrorLogSeverity.FATAL, this.getClass().getName(), -1, authData.getUserId(), env);
				addError("Error creating validation rule: " + e.getMessage());
			}
		}
		
		if (hasErrorMessages())
		{
			ModelAndView mv = new ModelAndView("vrs/edit");
			mv.addObject("errorMsgs", getErrorMsgs());
			mv.addObject("vr", vr);
			mv.addObject("typeId", sTypeId);
			mv.addObject("keyPrefix", env.getType(KID.get(sTypeId)).getKeyPrefix());
			return mv;
		}
		
		// save the validation rule
		vr = vrService.save(vr, authData, env);
		
		// redirect to validation rule details
		return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/validationrules/" + vr.getId());
	}
	
	/**
	 * Translates a compilation error message returned by Kommet compiler while compiling
	 * a VRE into a message appropriate to end-user, i.e. hiding the details of the actual
	 * Java code and instead informing about errors in REL syntax.
	 * @param msg The original Kommet compiler message text
	 * @return
	 */
	private static String translateVRError(String msg)
	{
		if (msg.contains("incompatible types found :") && msg.endsWith("required: boolean"))
		{
			return "expression does not return a logical value";
		}
		else
		{
			return msg;
		}
	}

	private ModelAndView prepareRuleDetails(ModelAndView mv, String sRuleId, EnvData env) throws KommetException
	{
		KID ruleId = null;
		
		try
		{
			ruleId = KID.get(sRuleId);
		}
		catch (KIDException e)
		{
			mv.addObject("errorMsgs", getMessage("Invalid rule ID " + sRuleId));
			return mv;
		}
		
		ValidationRule vr = vrService.get(ruleId, env);
		if (vr == null)
		{
			mv.addObject("errorMsgs", getMessage("Validation rule not found"));
			return mv;
		}
	
		mv.addObject("vr", vr);
		mv.addObject("typeName", env.getType(vr.getTypeId()).getLabel());
		mv.addObject("typePrefix", env.getType(vr.getTypeId()).getKeyPrefix());
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/validationrules/{sRuleId}", method = RequestMethod.GET)
	public ModelAndView details(@PathVariable("sRuleId") String sRuleId, HttpSession session, HttpServletRequest req) throws KommetException
	{
		ModelAndView mv = new ModelAndView("vrs/details");
		mv = prepareRuleDetails(mv, sRuleId, envService.getCurrentEnv(session));
		
		// add breadcrumbs
		Breadcrumbs.add(req.getRequestURL().toString(), ((ValidationRule)mv.getModel().get("vr")).getName(), appConfig.getBreadcrumbMax(), session);
		
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/validationrules/edit/{sRuleId}", method = RequestMethod.GET)
	public ModelAndView edit(@PathVariable("sRuleId") String sRuleId, HttpSession session) throws KommetException
	{	
		ModelAndView mv = new ModelAndView("vrs/edit");
		EnvData env = envService.getCurrentEnv(session);
		KID ruleId = null;
		
		try
		{
			ruleId = KID.get(sRuleId);
		}
		catch (KIDException e)
		{
			mv.addObject("errorMsgs", getMessage("Invalid rule ID " + sRuleId));
			return mv;
		}
		
		ValidationRule vr = vrService.get(ruleId, env);
		if (vr == null)
		{
			mv.addObject("errorMsgs", getMessage("Validation rule not found"));
			return mv;
		}
		
		if (vr.getIsSystem())
		{
			mv.addObject("errorMsgs", getMessage("Permission denied to edit system validation rule"));
			return mv;
		}
	
		Type type = env.getType(vr.getTypeId());
		
		mv.addObject("vr", vr);
		mv.addObject("typeId", vr.getTypeId());
		mv.addObject("keyPrefix", type.getKeyPrefix());
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/validationrules/new", method = RequestMethod.GET)
	public ModelAndView newValidationRule(@RequestParam(value = "typeId", required = false) String sTypeId, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("vrs/edit");
		
		if (!StringUtils.hasText(sTypeId))
		{
			mv.addObject("errorMsgs", getMessage("Type not defined"));
			return mv;
		}
		
		KID typeId = null;
		
		try
		{
			typeId = KID.get(sTypeId);
		}
		catch (KIDException e)
		{
			mv.addObject("errorMsgs", getMessage("Invalid type ID " + sTypeId));
			return mv;
		}
		
		ValidationRule templateRule = new ValidationRule();
		templateRule.setActive(true);
		
		EnvData env = envService.getCurrentEnv(session);
		mv.addObject("typeId", typeId);
		mv.addObject("vr", templateRule);
		mv.addObject("keyPrefix", env.getType(typeId).getKeyPrefix());
		return mv;
	}
	
	/**
	 * Validation rule wrapper used on the validation rule list
	 */
	public class ValidationRuleWrapper
	{
		private KID id;
		private String typeName;
		private String name;
		private Date createdDate;
		private Boolean active;
		
		public ValidationRuleWrapper (ValidationRule vr, EnvData env) throws KommetException
		{
			this.id = vr.getId();
			this.name = vr.getName();
			this.createdDate = vr.getCreatedDate();
			this.typeName = env.getType(vr.getTypeId()).getLabel();
			this.active = vr.getActive();
		}
		
		public KID getId()
		{
			return id;
		}
		
		public String getTypeName()
		{
			return typeName;
		}
		
		public Date getCreatedDate()
		{
			return createdDate;
		}

		public String getName()
		{
			return name;
		}

		public Boolean getActive()
		{
			return active;
		}
	}
}