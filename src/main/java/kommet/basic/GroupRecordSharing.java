/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import kommet.basic.types.SystemTypes;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.GROUP_RECORD_SHARING_API_NAME)
public class GroupRecordSharing extends StandardTypeRecordProxy
{
	private KID recordId;
	private UserGroup group;
	private Boolean isGeneric;
	private String reason;
	private Boolean read;
	private Boolean edit;
	private Boolean delete;
	private SharingRule sharingRule;
	
	public GroupRecordSharing() throws KommetException
	{
		this(null, null);
	}
	
	public GroupRecordSharing (Record sharing, EnvData env) throws KommetException
	{
		super(sharing, true, env);
	}

	public void setRecordId(KID recordId)
	{
		this.recordId = recordId;
		setInitialized();
	}

	@Property(field = "recordId")
	public KID getRecordId()
	{
		return recordId;
	}

	public void setGroup(UserGroup group)
	{
		this.group = group;
		setInitialized();
	}

	@Property(field = "group")
	public UserGroup getGroup()
	{
		return group;
	}

	public void setReason(String reason)
	{
		this.reason = reason;
		setInitialized();
	}

	@Property(field = "reason")
	public String getReason()
	{
		return reason;
	}

	public void setIsGeneric(Boolean isGeneric)
	{
		this.isGeneric = isGeneric;
		setInitialized();
	}

	@Property(field = "isGeneric")
	public Boolean getIsGeneric()
	{
		return isGeneric;
	}

	public void setRead(Boolean read)
	{
		this.read = read;
		setInitialized();
	}

	@Property(field = "read")
	public Boolean getRead()
	{
		return read;
	}

	public void setEdit(Boolean edit)
	{
		this.edit = edit;
		setInitialized();
	}

	@Property(field = "edit")
	public Boolean getEdit()
	{
		return edit;
	}

	public void setDelete(Boolean delete)
	{
		this.delete = delete;
		setInitialized();
	}

	@Property(field = "delete")
	public Boolean getDelete()
	{
		return delete;
	}

	@Property(field = "sharingRule")
	public SharingRule getSharingRule()
	{
		return sharingRule;
	}

	public void setSharingRule(SharingRule sharingRule)
	{
		this.sharingRule = sharingRule;
		setInitialized();
	}
}