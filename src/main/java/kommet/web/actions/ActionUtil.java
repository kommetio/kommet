/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.actions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.RecordProxy;
import kommet.basic.RecordProxyUtil;
import kommet.basic.keetle.ActionParamCastException;
import kommet.basic.keetle.BaseController;
import kommet.basic.keetle.PageData;
import kommet.data.DataService;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.sharing.SharingService;
import kommet.env.EnvData;
import kommet.i18n.Locale;
import kommet.json.JSON;
import kommet.koll.SystemContext;
import kommet.koll.annotations.ActionConfig;
import kommet.koll.annotations.CrossOrigin;
import kommet.koll.annotations.Header;
import kommet.koll.annotations.Param;
import kommet.koll.annotations.Params;
import kommet.koll.annotations.RequestBody;
import kommet.koll.annotations.Rest;
import kommet.koll.annotations.ReturnsFile;
import kommet.koll.annotations.SystemContextVar;
import kommet.koll.compiler.KommetCompiler;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.NumberFormatUtil;

/**
 * Helper class for various features related to calling web actions.
 * @author Radek Krawiec
 * @since 09/04/2015
 */
public class ActionUtil
{
	public static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
	
	/**
	 * Casts string request parameter values to the actual type of the parameter.
	 * @param value
	 * @param cls
	 * @return
	 * @throws KIDException 
	 * @throws ActionParamCastException
	 */
	public static Object castParam (Object value, Class<?> cls, String paramName, boolean treatEmptyStringAsNull, Locale locale, KommetCompiler compiler, EnvData env) throws ActionParamCastException
	{
		if (value == null || (treatEmptyStringAsNull && "".equals(value)))
		{
			return null;
		}
		
		if (cls.getName().equals(String.class.getName()))
		{
			return value.toString();
		}
		else if (cls.getName().equals(Boolean.class.getName()))
		{
			if (value instanceof String)
			{
				return Boolean.valueOf((String)value);
			}
			else
			{
				throw new ActionParamCastException("Value of type " + value.getClass().getName() + " cannot be cast to boolean", paramName, cls, value.getClass());
			}
		}
		else if (cls.getName().equals(BigDecimal.class.getName()))
		{
			if (value instanceof String)
			{
				try
				{
					return new BigDecimal(NumberFormatUtil.parseLocaleSpecificNumber((String)value, locale));
				}
				catch (KommetException e)
				{
					throw new ActionParamCastException("Could not cast parameter value " + value + " to number", paramName, cls, value.getClass());
				}
			}
			else
			{
				throw new ActionParamCastException("Value of type " + value.getClass().getName() + " cannot be cast to BigDecimal", paramName, cls, value.getClass());
			}
		}
		else if (cls.getName().equals(Date.class.getName()))
		{
			if (value instanceof String)
			{
				try
				{
					// the date string can be in any of the three formats:
					// yyyy-MM-dd
					// yyyy-MM-dd hh:mm:ss
					// yyyy-MM-dd hh:mm:ss.SSS
					return MiscUtils.parseDateTime((String)value, true);
				}
				catch (ParseException e)
				{
					throw new ActionParamCastException("Could not cast parameter value " + value + " to date", paramName, cls, value.getClass());
				}
			}
			else
			{
				throw new ActionParamCastException("Value of type " + value.getClass().getName() + " cannot be cast to Date", paramName, cls, value.getClass());
			}
		}
		else if (cls.getName().equals(Integer.class.getName()))
		{
			if (value instanceof String)
			{
				try
				{
					return Integer.valueOf((String)value);
				}
				catch (NumberFormatException e)
				{
					throw new ActionParamCastException("Value of type " + value.getClass().getName() + " cannot be cast to integer", paramName, cls, value.getClass());
				}
			}
			else
			{
				throw new ActionParamCastException("Value of type " + value.getClass().getName() + " cannot be cast to integer", paramName, cls, value.getClass());
			}
		}
		else if (cls.getName().equals(Long.class.getName()))
		{
			if (value instanceof String)
			{
				try
				{
					return Long.valueOf((String)value);
				}
				catch (NumberFormatException e)
				{
					throw new ActionParamCastException("Value of type " + value.getClass().getName() + " cannot be cast to long", paramName, cls, value.getClass());
				}
			}
			else
			{
				throw new ActionParamCastException("Value of type " + value.getClass().getName() + " cannot be cast to long", paramName, cls, value.getClass());
			}
		}
		else if (cls.getName().equals(KID.class.getName()))
		{
			if (value instanceof String)
			{
				try
				{
					return KID.get((String)value);
				}
				catch (KIDException e)
				{
					throw new ActionParamCastException("Argument value " + value + " cannot be cast to KID", paramName, cls, value.getClass());
				}
			}
			else
			{
				throw new ActionParamCastException("Value of type " + value.getClass().getName() + " cannot be cast to integer", paramName, cls, value.getClass());
			}
		}
		else if (RecordProxy.class.isAssignableFrom(cls))
		{
			if (!(value instanceof String))
			{
				throw new ActionParamCastException("Value of the '" + paramName + "' parameter is expected to be a string representing serialized object, but has type" + value.getClass().getName(), paramName, cls, value.getClass());
			}
			
			Type type = null;
			
			try
			{
				type = env.getType(MiscUtils.envToUserPackage(cls.getName(), env));
				
				if (type == null)
				{
					throw new ActionParamCastException("Type " + MiscUtils.envToUserPackage(cls.getName(), env) + " not found", paramName, cls, value.getClass());
				}
			}
			catch (KommetException e)
			{
				throw new ActionParamCastException("Error getting type for class name " + cls.getName(), paramName, cls, value.getClass());
			}
			
			// if the parameter is a record proxy, assume that the value is a serialized record and try to deserialize it
			try
			{
				return RecordProxyUtil.generateCustomTypeProxy(JSON.toRecord((String)value, true, type, env), env, compiler);
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				throw new ActionParamCastException("Error deserializing JSON parameter: " +e.getMessage(), paramName, cls, value.getClass());
			}
		}
		else
		{
			throw new ActionParamCastException("Parameter type " + cls.getName() + " cannot be automatically cast from string", paramName, cls, value.getClass());
		}
	}
	
	/**
	 * Calls an action for the given KTL page.
	 * 
	 * Every time this method is called, it creates a new instance of the controller defined for this page, thus 
	 * controllers are stateless between requests.
	 * 
	 * @param action
	 * @param request
	 * @param compiler
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	public static Object callAction(String controllerName, String actionMethod, Map<String, String> urlParams, SystemContext systemContext, HttpServletRequest request, HttpServletResponse response, PageData pageData, KommetCompiler compiler, DataService dataService, SharingService sharingService, AuthData authData, EnvData env, AppConfig appConfig) throws KommetException
	{
		if (!StringUtils.hasText(controllerName) || !StringUtils.hasText(actionMethod))
		{
			throw new KommetException("Action controller or method is null");
		}
		
		Object controllerInstance = null;
		Class<?> controllerClass = null;
		Method controllerMethod = null;
		
		try
		{
			controllerClass = compiler.getClass(controllerName, true, env);
			controllerMethod = MiscUtils.getMethodByName(controllerClass, actionMethod);
			controllerInstance = controllerClass.newInstance();
			
			if (controllerClass.isAnnotationPresent(SystemContextVar.class))
			{
				String sysVar = ((SystemContextVar)controllerClass.getAnnotation(SystemContextVar.class)).value();
				
				// inject system context
				controllerClass.getField(sysVar).set(controllerInstance, systemContext);
			}
			
			setInvocationParams(controllerInstance, systemContext, pageData, env, request, compiler, dataService, sharingService, appConfig);
		}
		catch (Exception e)
		{
			throw new KommetException("Error getting controller class " + controllerName + ": " + e.getMessage(), e);
		}
		
		return callActionMethod(controllerInstance, controllerMethod, urlParams, request, response, compiler, authData, env);
	}
	
	/**
	 * Sets properties of a basic controller necessary for invoking an action method.
	 * @param controllerInstance
	 * @param env
	 * @throws KommetException
	 */
	private static void setInvocationParams(Object controllerInstance, SystemContext systemContext, PageData pageData, EnvData env, HttpServletRequest request, KommetCompiler compiler, DataService dataService, SharingService sharingService, AppConfig appConfig) throws KommetException
	{
		if (!BaseController.class.isAssignableFrom(controllerInstance.getClass()))
		{
			throw new KommetException("Controller class " + controllerInstance.getClass().getName() + " does not extend " + BaseController.class.getSimpleName());
		}
		
		try
		{
			PropertyUtils.setProperty(controllerInstance, "env", env);
			PropertyUtils.setProperty(controllerInstance, "systemContext", systemContext);
			PropertyUtils.setProperty(controllerInstance, "dataService", dataService);
			PropertyUtils.setProperty(controllerInstance, "sharingService", sharingService);
			PropertyUtils.setProperty(controllerInstance, "parameters", request.getParameterMap());
			PropertyUtils.setProperty(controllerInstance, "authData", AuthUtil.getAuthData(request.getSession()));
			PropertyUtils.setProperty(controllerInstance, "pageData", pageData);
			PropertyUtils.setProperty(controllerInstance, "appConfig", appConfig);
			PropertyUtils.setProperty(controllerInstance, "request", request);
		}
		catch (Exception e)
		{
			throw new KommetException("Error setting invocation parameters on controller " + controllerInstance.getClass().getName() + ": " + e.getMessage(), e);
		}
	}
	
	/**
	 * Calls a controller method and returns its result.
	 * 
	 * The controller method needs to have all parameters annotated with @Param. All such parameters will be mapped to
	 * and automatically assigned values to HTTP request parameter's.
	 * 
	 * @param controllerInstance the instance of the controller class
	 * @param actionMethod the instance of the controller method to be called
	 * @param parsedUrl 
	 * @param request HTTP request
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	private static Object callActionMethod (Object controllerInstance, Method actionMethod, Map<String, String> urlParams, HttpServletRequest request, HttpServletResponse resp, KommetCompiler compiler, AuthData authData, EnvData env) throws KommetException
	{
		if (actionMethod == null)
		{
			throw new KommetException("Action method to call is null");
		}
		
		String methodQualifiedName = controllerInstance.getClass().getName() + "." + actionMethod.getName();
		
		// check all parameters of the method annotated with @Param
		Annotation[][] parameterAnnotations = actionMethod.getParameterAnnotations();
		List<Object> paramValues = new ArrayList<Object>();
		
		boolean treatEmptyStringParamsAsNull = true;
		boolean processParamCastErrors = false;
		List<ParamCastError> paramCastExceptions = null;
		
		if (actionMethod.isAnnotationPresent(CrossOrigin.class))
		{
			String[] corsDomains = actionMethod.getAnnotation(CrossOrigin.class).allowedOrigins(); 
			if (corsDomains != null && corsDomains.length > 0)
			{
				// add CORS header
				// System.out.println("CORS header: " + MiscUtils.implode(corsDomains, ", "));
				resp.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, MiscUtils.implode(corsDomains, ", "));
			}
		}
		
		if (actionMethod.isAnnotationPresent(ActionConfig.class))
		{
			treatEmptyStringParamsAsNull = actionMethod.getAnnotation(ActionConfig.class).emptyParamAsNull();
			processParamCastErrors = actionMethod.getAnnotation(ActionConfig.class).processParamCastErrors();
			
			if (processParamCastErrors)
			{
				paramCastExceptions = new ArrayList<ParamCastError>();
			}
		}
		
		for (int i = 0; i < parameterAnnotations.length; i++)
		{
			// parameters of an action method must have exactly one annotation - @Param
			if (parameterAnnotations[i].length == 0)
			{
				throw new KommetException("Parameters of an action method must be annotated with either @" + Param.class.getSimpleName() + " or @Params. Parameter #" + (i + 1) + " of method " + methodQualifiedName + " is not");
			}
			else if (parameterAnnotations[i].length > 1)
			{
				throw new KommetException("Parameter " + (i + 1) + " of action method " + methodQualifiedName + " has more than one annotation.");
			}
			
			Annotation paramAnnotation = parameterAnnotations[i][0];
			
			if (paramAnnotation instanceof Param)
			{
				String paramName = ((Param)paramAnnotation).value();
				
				if (!StringUtils.hasText(paramName))
				{
					throw new KommetException("Annotation @Param must have attribute 'value' set");
				}

				// try to find a parameter in the request with the same name as the attribute if not found, assign null
				try
				{
					paramValues.add(castParam(getParamValue(paramName, request, urlParams), actionMethod.getParameterTypes()[i], paramName, treatEmptyStringParamsAsNull, authData.getLocale(), compiler, env));
				}
				catch (ActionParamCastException e)
				{
					if (processParamCastErrors)
					{
						// list the exception so that it can be processed by the user in the action method
						paramCastExceptions.add(new ParamCastError(e));
						
						// use null for this value
						paramValues.add(null);
					}
					else
					{
						throw e;
					}
				}
			}
			else if (paramAnnotation instanceof Params)
			{
				if (!Map.class.isAssignableFrom(actionMethod.getParameterTypes()[i]))
				{
					throw new KommetException("Parameter #" + i + " or method " + methodQualifiedName + " annotated with @Params must be an instance of " + Map.class.getSimpleName());
				}
				
				String paramNames = ((Params)paramAnnotation).value();
				
				if (!StringUtils.hasText(paramNames))
				{
					throw new KommetException("Annotation @Params must have default attribute set");
				}
				
				Map<String, Object> values = new HashMap<String, Object>();
				
				List<String> paramNameList = MiscUtils.splitAndTrim(paramNames, ",");
				for (String param : paramNameList)
				{
					try
					{
						values.put(param, castParam(getParamValue(param, request, urlParams), actionMethod.getParameterTypes()[i], param, treatEmptyStringParamsAsNull, authData.getLocale(), compiler, env));
					}
					catch (ActionParamCastException e)
					{
						if (processParamCastErrors)
						{
							// list the exception so that it can be processed by the user
							// in the action method
							paramCastExceptions.add(new ParamCastError(e));
							
							// use null for this value
							paramValues.add(null);
						}
						else
						{
							throw e;
						}
					}
				}
				
				paramValues.add(values);
			}
			else if (paramAnnotation instanceof Header)
			{
				String headerName = ((Header)paramAnnotation).value();
				
				if (!StringUtils.hasText(headerName))
				{
					throw new KommetException("Annotation @Header must have attribute 'value' set");
				}
				
				/*Enumeration headerNames = request.getHeaderNames();
		        while (request.getHeaderNames().hasMoreElements()) {
		            String key = (String) headerNames.nextElement();
		            String value = request.getHeader(key);
		        }*/

				// try to find a header in the request with the same name as the attribute if not found, assign null
				paramValues.add(request.getHeader(headerName));
			}
			else if (paramAnnotation instanceof RequestBody)
			{
				if (!String.class.isAssignableFrom(actionMethod.getParameterTypes()[i]))
				{
					throw new KommetException("Parameter #" + i + " of method " + methodQualifiedName + " annotated with @RequestBody must be an instance of " + String.class.getSimpleName());	
				}
				
				try
				{
					paramValues.add(request.getReader().lines().collect(Collectors.joining(System.lineSeparator())));
					//BufferedReader r = new BufferedReader(new InputStreamReader(request.getInputStream(), request.getCharacterEncoding()));
					//paramValues.add(IOUtils.toString(request.getReader()));
					//paramValues.add(IOUtils.toString(r));
				}
				catch (IOException e)
				{
					e.printStackTrace();
					throw new KommetException("Error reading request body: " + e.getMessage());
				}
			}
			else
			{
				throw new KommetException("All parameters of an action method must be annotated with @Param or @Params. Parameter " + (i + 1) + " of action method " + methodQualifiedName + " is not");
			}
		}
		
		if (processParamCastErrors)
		{
			// if parameter cast errors are injected for processing instead of being thrown,
			// we inject them into the controller
			// note that even if the list of errors is empty, we inject it, so that the user
			// can always access a non-null list
			((BaseController)controllerInstance).setParamCastErrors(paramCastExceptions);
		}
		
		try
		{
			// call the action method
			Object pageData = actionMethod.invoke(controllerInstance, paramValues.toArray(new Object[0]));
			
			// errors are not set for REST actions and for file downloads
			if (!actionMethod.isAnnotationPresent(Rest.class) && !actionMethod.isAnnotationPresent(ReturnsFile.class))
			{
				// get action messages from controller and put them in page data
				// so that they are available for the view
				((PageData)pageData).setValue(PageData.ACTION_MSGS_KEY, PropertyUtils.getProperty(controllerInstance, "actionMsgs"));
				((PageData)pageData).setValue(PageData.ERROR_MSGS_KEY, PropertyUtils.getProperty(controllerInstance, "errorMsgs"));
			}
			
			return pageData;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new KommetException("Error calling action method " + methodQualifiedName + ". Nested: " + e.getMessage(), e);
		}
	}
	
	public static Object getParamValue(String param, HttpServletRequest req, Map<String, String> urlParams)
	{
		Object paramValue = req.getAttribute(param);
		
		if (paramValue == null)
		{
			paramValue = req.getParameter(param);
		}
		
		if (paramValue == null && urlParams != null)
		{
			paramValue = urlParams.get(param);
		}
		
		return paramValue;
	}

	/**
	 * Writes a byte content to the response output.
	 * @param actionResult
	 * @param returnsFileAnnotation
	 * @param response
	 * @throws KommetException 
	 */
	public static void returnFile(Object actionResult, ReturnsFile returnsFileAnnotation, HttpServletResponse response) throws KommetException
	{
		response.setHeader("Content-Disposition", "attachment; filename=\"" + returnsFileAnnotation.name() + "\""); 
		
		InputStream is = null;
		
		if (actionResult instanceof byte[])
		{
			is = new ByteArrayInputStream((byte[])actionResult);
		}
		else if (actionResult instanceof String)
		{
			is = new ByteArrayInputStream(((String)actionResult).getBytes(StandardCharsets.UTF_8));
		}
		else
		{
			throw new KommetException("Type " + (actionResult != null ? actionResult.getClass().getName() : "null") + " cannot be returned as file");
		}
		
		// copy it to response's output stream
		try
		{
			IOUtils.copy(is, response.getOutputStream());
			
			if (StringUtils.hasText(returnsFileAnnotation.mimeType()))
			{
				response.setContentType(returnsFileAnnotation.mimeType());
			}
			
			response.flushBuffer();
		}
		catch (IOException e)
		{
			throw new KommetException("Error writing file to output. Nested: " + e.getMessage());
		}
	}

}