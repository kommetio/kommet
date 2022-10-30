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

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.ERROR_LOG_API_NAME)
public class ErrorLog extends StandardTypeRecordProxy
{
	private String message;
	private String details;
	private User affectedUser;
	private Integer codeLine;
	private String codeClass;
	private String severity;
	
	
	public ErrorLog() throws KommetException
	{
		this(null, null);
	}
	
	public ErrorLog(Record record, EnvData env) throws RecordProxyException
	{
		super(record, false, env);
	}

	public void setMessage(String message)
	{
		this.message = message;
		setInitialized();
	}

	@Property(field = "message")
	public String getMessage()
	{
		return message;
	}

	public void setDetails(String details)
	{
		this.details = details;
		setInitialized();
	}

	@Property(field = "details")
	public String getDetails()
	{
		return details;
	}

	public void setAffectedUser(User affectedUser)
	{
		this.affectedUser = affectedUser;
		setInitialized();
	}

	@Property(field = "affectedUser")
	public User getAffectedUser()
	{
		return affectedUser;
	}

	public void setCodeLine(Integer line)
	{
		this.codeLine = line;
		setInitialized();
	}

	@Property(field = "codeLine")
	public Integer getCodeLine()
	{
		return codeLine;
	}

	public void setCodeClass(String codeClass)
	{
		this.codeClass = codeClass;
		setInitialized();
	}

	@Property(field = "codeClass")
	public String getCodeClass()
	{
		return codeClass;
	}

	public void setSeverity(String severity)
	{
		this.severity = severity;
		setInitialized();
	}

	@Property(field = "severity")
	public String getSeverity()
	{
		return severity;
	}
}