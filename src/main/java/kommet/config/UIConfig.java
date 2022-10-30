/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.config;

import kommet.auth.AuthData;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.uch.UserCascadeHierarchyService;

public class UIConfig
{
	private String userMenuFontSize = "1rem";
	private String systemMenuFontSize = "1rem";
	private String buttonFontSize = "1rem";

	public String getUserMenuFontSize()
	{
		return userMenuFontSize;
	}

	public void setUserMenuFontSize(String userMenuFontSize)
	{
		this.userMenuFontSize = userMenuFontSize;
	}

	public String getSystemMenuFontSize()
	{
		return systemMenuFontSize;
	}

	public void setSystemMenuFontSize(String systemMenuFontSize)
	{
		this.systemMenuFontSize = systemMenuFontSize;
	}

	public String getButtonFontSize()
	{
		return buttonFontSize;
	}

	public void setButtonFontSize(String buttonFontSize)
	{
		this.buttonFontSize = buttonFontSize;
	}

	public static UIConfig get(UserCascadeHierarchyService uchService, AuthData authData, EnvData env) throws KommetException
	{
		UIConfig config = new UIConfig();
		
		Type settingValueType = env.getType(KeyPrefix.get(KID.SETTING_VALUE_PREFIX));
		AuthData rootAuthData = AuthData.getRootAuthData(env);
		
		String btnFontSize = (String)uchService.getSettingValue(settingValueType, "value", "key", Constants.SYSTEM_SETTING_UI_BTN_FONT_SIZE, authData, rootAuthData, env);
		if (btnFontSize != null)
		{
			config.setButtonFontSize(btnFontSize);
		}
		
		String newSystemMenuFontSize = (String)uchService.getSettingValue(settingValueType, "value", "key", Constants.SYSTEM_SETTING_UI_SYSTEM_MENU_FONT_SIZE, authData, rootAuthData, env);
		if (newSystemMenuFontSize != null)
		{
			config.setSystemMenuFontSize(newSystemMenuFontSize);
		}
		
		String newUserMenuFontSize = (String)uchService.getSettingValue(settingValueType, "value", "key", Constants.SYSTEM_SETTING_UI_USER_MENU_FONT_SIZE, authData, rootAuthData, env);
		if (newUserMenuFontSize != null)
		{
			config.setUserMenuFontSize(newUserMenuFontSize);
		}
		
		// TODO overwrite other settings
		
		return config;
	}
}