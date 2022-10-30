/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.emailing;

import java.util.HashMap;
import java.util.Map;

public class EmailAccount
{
	private String smtpHost;
	private String imapHost;
	private String userName;
	private String password;
	private Integer smtpPort;
	private Integer imapPort;
	private String senderAddress;
	private String senderName;
	private Map<String, String> properties;
	
	public EmailAccount()
	{
		this.properties = new HashMap<String, String>();
	}
	
	public void setProp (String key, String value)
	{
		this.properties.put(key, value);
	}
	
	public Map<String, String> getProperties()
	{
		return this.properties;
	}
	
	public String getSmtpHost()
	{
		return smtpHost;
	}

	public void setSmtpHost(String host)
	{
		this.smtpHost = host;
	}

	public String getUserName()
	{
		return userName;
	}

	public void setUserName(String userName)
	{
		this.userName = userName;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public Integer getSmtpPort()
	{
		return smtpPort;
	}

	public void setSmtpPort(Integer smtpPort)
	{
		this.smtpPort = smtpPort;
	}

	public Integer getImapPort()
	{
		return imapPort;
	}

	public void setImapPort(Integer imapPort)
	{
		this.imapPort = imapPort;
	}

	public String getSenderAddress()
	{
		return senderAddress;
	}

	public void setSenderAddress(String senderAddress)
	{
		this.senderAddress = senderAddress;
	}

	public String getSenderName()
	{
		return senderName;
	}

	public void setSenderName(String senderName)
	{
		this.senderName = senderName;
	}

	public String getImapHost()
	{
		return imapHost;
	}

	public void setImapHost(String imapHost)
	{
		this.imapHost = imapHost;
	}
}