/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import java.util.Date;

import kommet.basic.types.SystemTypes;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.API_TOKEN_API_NAME)
public class ApiToken extends StandardTypeRecordProxy
{
	private String name;
	private String tokenHash;
	private String tokenPrefix;
	private User systemUser;
	private Date expirationDate;
	private boolean readPermission;
	private boolean writePermission;

	public ApiToken() throws KommetException
	{
		this(null, null);
	}

	public ApiToken(Record record, EnvData env) throws KommetException
	{
		super(record, true, env);
	}

	@Property(field = "name")
	public String getName() { return name; }
	public void setName(String name) { this.name = name; setInitialized(); }

	@Property(field = "tokenHash")
	public String getTokenHash() { return tokenHash; }
	public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; setInitialized(); }

	@Property(field = "tokenPrefix")
	public String getTokenPrefix() { return tokenPrefix; }
	public void setTokenPrefix(String tokenPrefix) { this.tokenPrefix = tokenPrefix; setInitialized(); }

	@Property(field = "systemUser")
	public User getSystemUser() { return systemUser; }
	public void setSystemUser(User systemUser) { this.systemUser = systemUser; setInitialized(); }

	@Property(field = "expirationDate")
	public Date getExpirationDate() { return expirationDate; }
	public void setExpirationDate(Date expirationDate) { this.expirationDate = expirationDate; setInitialized(); }

	@Property(field = "isReadPermission")
	public boolean getIsReadPermission() { return readPermission; }
	public void setReadPermission(boolean readPermission) { this.readPermission = readPermission; setInitialized("isReadPermission"); }

	@Property(field = "isWritePermission")
	public boolean getIsWritePermission() { return writePermission; }
	public void setWritePermission(boolean writePermission) { this.writePermission = writePermission; setInitialized("isWritePermission"); }
}