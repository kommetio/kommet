/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.rel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kommet.basic.keetle.StandardObjectController;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.utils.UrlUtil;
import kommet.web.RequestAttributes;

public class VRELParser
{
	private static final Pattern TEXT_LABEL_PATTERN = Pattern.compile("\\{\\{\\$[lL]abel\\.([A-z0-9\\.]+)\\}\\}");
	private static final Pattern USER_SETTING_PATTERN = Pattern.compile("\\{\\{\\$[uU]serSetting\\.([A-z0-9\\.]+)\\}\\}");
	private static final Pattern FIELD_LABEL_PATTERN = Pattern.compile("\\{\\{\\$[fF]ieldLabel\\.([A-z0-9\\.]+)\\}\\}");
	private static final Pattern RECORD_FIELD_PATTERN = Pattern.compile("\\{\\{\\$[rR]ecord\\.([A-z0-9\\.]+)\\}\\}");
	private static final Pattern WEB_RESOURCE_PATH_PATTERN = Pattern.compile("\\{\\{\\$[rR]esource\\.path\\.([A-z0-9\\.]+)\\}\\}");
	private static final Pattern VIEW_RESOURCE_PATH_PATTERN = Pattern.compile("\\{\\{\\$[vV]iewresource\\.path\\.([A-z0-9\\.]+)\\}\\}");
	private static final Pattern PAGE_DATA_VAR_PATTERN = Pattern.compile("\\{\\{([A-z][A-z0-9\\.\\s\\-\\+\\=\\>\\<]+)\\}\\}");
	//private static final Pattern EL_EXPRESSION_PATTERN = Pattern.compile("\\{\\{(.*?)\\}\\}");
	
	/**
	 * Translates VREL expressions in KTL code into Java expressions.
	 * @param viewCode Source KTL code
	 * @return parsed JSP code
	 * @throws KommetException 
	 */
	public static String interpreteVREL (String viewCode, EnvData env) throws KommetException
	{
		viewCode = interpreteTextLabels(viewCode);
		viewCode = interpreteUserSettings(viewCode);
		viewCode = interpreteFieldLabels(viewCode);
		viewCode = interpretePageDataVars(viewCode, env);
		viewCode = interpretRecordFields(viewCode);
		viewCode = interpretWebResourcePaths(viewCode);
		viewCode = interpretViewResourcePaths(viewCode);
		return viewCode;
	}

	private static String interpretePageDataVars(String ktlCode, EnvData env) throws KommetException
	{
		Matcher m = PAGE_DATA_VAR_PATTERN.matcher(ktlCode);
		
		StringBuffer sb = new StringBuffer();
		while (m.find())
		{
			String relExpression = m.group(1);
			
			// treat each group as a REL expression
			m.appendReplacement(sb, "\\${" + RELParser.relToJava(relExpression, null, null, true, false, false, new PageDataVarTranslator(), env) + "}");
		}
		m.appendTail(sb);
		return sb.toString();
	}

	private static String interpreteTextLabels(String code)
	{
		Matcher m = TEXT_LABEL_PATTERN.matcher(code);
		
		StringBuffer sb = new StringBuffer();
		while (m.find())
		{
			m.appendReplacement(sb, "\\${" + RequestAttributes.TEXT_LABELS_ATTR_NAME + ".get('" + m.group(1) + "', " + RequestAttributes.AUTH_DATA_ATTR_NAME + ".getLocale())}");
		}
		m.appendTail(sb);
		return sb.toString();
	}
	
	private static String interpreteUserSettings(String code)
	{
		Matcher m = USER_SETTING_PATTERN.matcher(code);
		
		StringBuffer sb = new StringBuffer();
		while (m.find())
		{
			m.appendReplacement(sb, "\\${" + RequestAttributes.AUTH_DATA_ATTR_NAME + ".getUserCascadeSettings().get('" + m.group(1) + "')}");
		}
		m.appendTail(sb);
		return sb.toString();
	}
	
	private static String interpretRecordFields(String code)
	{
		Matcher m = RECORD_FIELD_PATTERN.matcher(code);
		
		StringBuffer sb = new StringBuffer();
		while (m.find())
		{
			m.appendReplacement(sb, "\\${pageData.getValue('" + StandardObjectController.RECORD_VAR_PARAM + "').getField('" + m.group(1) + "')}");
		}
		m.appendTail(sb);
		return sb.toString();
	}
	
	private static String interpretWebResourcePaths(String code)
	{
		Matcher m = WEB_RESOURCE_PATH_PATTERN.matcher(code);
		
		StringBuffer sb = new StringBuffer();
		while (m.find())
		{
			//m.appendReplacement(sb, "\\${" + RequestAttributes.APP_CONFIG_ATTR_NAME + ".getFileDir()}/\\${" + RequestAttributes.ENV_ATTR_NAME + ".getWebResource('" + m.group(1) + "').getDiskFilePath()}");
			m.appendReplacement(sb, "\\${pageContext.request.contextPath}/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/downloadresource?name=\\${" + RequestAttributes.ENV_ATTR_NAME + ".getWebResource('" + m.group(1) + "').getName()}");
		}
		m.appendTail(sb);
		return sb.toString();
	}
	
	private static String interpretViewResourcePaths(String code)
	{
		Matcher m = VIEW_RESOURCE_PATH_PATTERN.matcher(code);
		
		StringBuffer sb = new StringBuffer();
		while (m.find())
		{
			m.appendReplacement(sb, "\\${pageContext.request.contextPath}/\\${" + RequestAttributes.ENV_ATTR_NAME + ".getViewResourcePath('" + m.group(1) + "', " + RequestAttributes.APP_CONFIG_ATTR_NAME + ")}");
		}
		m.appendTail(sb);
		return sb.toString();
	}
	
	private static String interpreteFieldLabels(String code)
	{
		Matcher m = FIELD_LABEL_PATTERN.matcher(code);
		
		StringBuffer sb = new StringBuffer();
		while (m.find())
		{
			m.appendReplacement(sb, "\\${" + RequestAttributes.ENV_ATTR_NAME + ".getEnvSpecificFieldLabel('" + m.group(1) + "', " + RequestAttributes.AUTH_DATA_ATTR_NAME + ")}");
		}
		m.appendTail(sb);
		return sb.toString();
	}
}