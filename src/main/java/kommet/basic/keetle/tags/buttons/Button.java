/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle.tags.buttons;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.utils.XMLUtil;

public class Button extends ButtonPrototype
{
	private static final Pattern RECORD_FIELD_PATTERN = Pattern.compile("\\{([a-z][A-z0-9\\.]*[A-z0-9])\\}");
	
	private String label;
	private String url;
	private String onClick;
	private String code;
	
	public Button (ButtonType type)
	{
		super(type);
	}
	
	public Button (String label, String url)
	{
		super(ButtonType.CUSTOM);
		this.label = label;
		this.url = url;
	}

	public Button (String code)
	{
		super(ButtonType.CUSTOM);
		this.code = code;
	}
	
	public Button (String code, ButtonType type, String label)
	{
		super(type);
		this.code = code;
		this.label = label;
	}

	public String getLabel()
	{
		return label;
	}
	
	public void setLabel (String label)
	{
		this.label = label;
	}

	public String getUrl()
	{
		return url;
	}
	
	public void setUrl (String url)
	{
		this.url = url;
	}
	
	public void setCode(String code)
	{
		this.code = code;
	}

	public String getCode()
	{
		return code;
	}

	public void setOnClick(String onClick)
	{
		this.onClick = onClick;
	}

	public String getOnClick()
	{
		return onClick;
	}

	public static Button fromPrototype(ButtonPrototype button)
	{
		return new Button(button.getType());
	}

	public static ButtonPrototype fromCustomButton(kommet.basic.Button btn, Record record, String contextPath, AuthData authData, EnvData env) throws KommetException
	{
		Button customBtn = new Button(ButtonType.CUSTOM);
		
		String label = btn.getLabel();
		if (StringUtils.hasText(btn.getLabelKey()))
		{
			label = authData.getUserCascadeSettings().get(btn.getLabelKey());
			if (!StringUtils.hasText(label))
			{
				label = btn.getLabel();
			}
		}
		customBtn.setLabel(label);
		customBtn.setOnClick(btn.getOnClick());
		
		if (btn.getAction() != null)
		{
			customBtn.setUrl(btn.getAction().getUrl());
		}
		else
		{
			customBtn.setUrl(interpreteUrl(btn.getUrl(), record));
		}
		
		customBtn.setCode(getBtnCode(customBtn, contextPath));
		
		return customBtn;
	}

	private static String interpreteUrl(String url, Record record) throws KommetException
	{
		if (url == null)
		{
			return null;
		}
		
		Matcher m = RECORD_FIELD_PATTERN.matcher(url);
		
		StringBuffer sb = new StringBuffer();
		while (m.find())
		{
			Object val = record.getField(m.group(1));
			m.appendReplacement(sb, val != null ? val.toString() : "");
		}
		m.appendTail(sb);
		return sb.toString();
	}

	private static String getBtnCode(Button button, String contextPath)
	{	
		StringBuilder code = new StringBuilder("<a class=\"sbtn\"");
		
		if (StringUtils.hasText(button.getUrl()))
		{
			String actualUrl = contextPath;
			if (!button.getUrl().startsWith("/"))
			{
				actualUrl += "/";
			}
			
			actualUrl += button.getUrl();
		
			//((Button)button).setUrl(actualUrl);
			XMLUtil.attr("href", actualUrl, code);
		}
		
		XMLUtil.attr("onclick", button.getOnClick(), code);
		code.append(">").append(button.getLabel()).append("</a>");
		
		return code.toString();
	}
}
