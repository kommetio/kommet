/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.LOGIN_HISTORY_API_NAME)
public class LoginHistory extends StandardTypeRecordProxy
{
	private User loginUser;
	private String method;
	private String ip4Address;
	private String ip6Address;
	private String result;
	
	public LoginHistory() throws KommetException
	{
		this(null, null);
	}
	
	public LoginHistory(Record record, EnvData env) throws RecordProxyException
	{
		super(record, false, env);
	}

	public void setLoginUser(User loginUser)
	{
		this.loginUser = loginUser;
		setInitialized();
	}

	@Property(field = "loginUser")
	public User getLoginUser()
	{
		return loginUser;
	}

	public void setMethod(String method)
	{
		this.method = method;
		setInitialized();
	}

	@Property(field = "method")
	public String getMethod()
	{
		return method;
	}

	public void setIp4Address(String ip4Address)
	{
		this.ip4Address = ip4Address;
		setInitialized();
	}

	@Property(field = "ip4Address")
	public String getIp4Address()
	{
		return ip4Address;
	}

	public void setIp6Address(String ip6Address)
	{
		this.ip6Address = ip6Address;
		setInitialized();
	}

	@Property(field = "ip6Address")
	public String getIp6Address()
	{
		return ip6Address;
	}

	public void setResult(String result)
	{
		this.result = result;
		setInitialized();
	}

	@Property(field = "result")
	public String getResult()
	{
		return result;
	}
}