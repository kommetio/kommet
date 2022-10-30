/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.util.Arrays;

import javax.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.security.RestrictedAccessException;

@ControllerAdvice
public class ErrorHandlerController
{
	@ExceptionHandler(RestrictedAccessException.class)
	public ModelAndView handleRestrictedAccessException(Exception ex, HttpSession session)
	{
		String errorMsg = "Access denied";
		
		if (session != null)
		{
			AuthData authData = AuthUtil.getAuthData(session);
			if (authData != null)
			{
				errorMsg = authData.getI18n().get("err.restrictedaccess.msg");
			}
		}
		
		ModelAndView model = new ModelAndView("common/msg");
		model.addObject("errorMsgs", Arrays.asList(errorMsg));
		return model;
 
	}
	
	@ExceptionHandler(Throwable.class)
	public ModelAndView handleCustomException(Exception ex, HttpSession session)
	{	
		ex.printStackTrace();
		
		ModelAndView model = new ModelAndView("common/msg");
		String errorMsg = "An error has occurred";
		
		if (ex instanceof NoHandlerFoundException)
		{
			errorMsg = "Page not found";
			
			if (session != null)
			{
				AuthData authData = AuthUtil.getAuthData(session);
				if (authData != null)
				{
					errorMsg = authData.getI18n().get("err.pagenotfound");
				}
			}
		}
		else
		{	
			if (session != null)
			{
				AuthData authData = AuthUtil.getAuthData(session);
				if (authData != null)
				{
					errorMsg = authData.getI18n().get("err.exception.msg");
				}
			}
			
			model.addObject("exception", ex);
		}
		
		model.addObject("errorMsgs", Arrays.asList(errorMsg));
		return model;
 
	}
}