/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.StringUtils;

import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.ValidationError;
import kommet.data.ValidationErrorType;
import kommet.env.EnvData;
import kommet.persistence.Property;
import kommet.persistence.Transient;

/**
 * Proxy representing a kommet.data record translated into a POJO.
 * @author Radek Krawiec
 * @since 2013
 */
public abstract class RecordProxy
{
	private Record record;
	private boolean isNull = false;
	
	protected KID id;
	
	private Date lastModifiedDate;
	private Date createdDate;
	//private RecordProxy lastModifiedBy;
	//private RecordProxy createdBy;
	private Integer accessType;
	private Set<String> initializedProperties = new HashSet<String>();
	private Set<String> nullifiedProperties = new HashSet<String>();
	private List<ValidationError> errors;
	
	public boolean isInitialized()
	{
		return !this.initializedProperties.isEmpty();
	}
	
	public RecordProxy() throws RecordProxyException
	{
		// empty
	}
	
	public RecordProxy (Record record, RecordProxyType proxyType, EnvData env) throws RecordProxyException 
	{	
		this.record = record;
		
		if (this.record != null)
		{
			// initialize the proxy from the record
			RecordProxyUtil.initProxy(this, record, true, proxyType, new HashMap<Integer, RecordProxy>(), env);
		}
	}
	
	@Transient
	public Set<String> getInitializedProperties()
	{
		// return a copy of the original collection so that it is not modified directly
		Set<String> props = new HashSet<String>();
		props.addAll(initializedProperties);
		return props;
	}
	
	protected void setInitialized (String property)
	{
		this.initializedProperties.add(property);
	}
	
	protected void setInitialized ()
	{
		String method = Thread.currentThread().getStackTrace()[2].getMethodName();
		if (method.startsWith("set"))
		{
			setInitialized(StringUtils.uncapitalize(method.substring(3)));
		}
	}
	
	public boolean isSet (String property)
	{
		return this.initializedProperties.contains(property);
	}
	
	@Deprecated
	public void nullify (String property)
	{
		this.nullifiedProperties.add(property);
	}
	
	/**
	 * This method is useful when we want to save the record as a new record. In this case we want to keep all its properties,
	 * but erase the value of the ID property as if it had never been set. Thanks to this the record will be treated as new.
	 */
	public void uninitializeId()
	{
		this.nullifiedProperties.remove(Field.ID_FIELD_NAME);
		this.initializedProperties.remove(Field.ID_FIELD_NAME);
		this.id = null;
	}
	
	public void uninitializeProperty (String name)
	{
		this.nullifiedProperties.remove(name);
		this.initializedProperties.remove(name);
	}
	
	/**
	 * Returns a null object of a given ObjectStub subtype.
	 * @param <T>
	 * @param cls
	 * @return
	 * @throws RecordProxyException
	 */
	@Transient
	public static <T extends RecordProxy> T getNullObject (java.lang.Class<T> cls) throws RecordProxyException
	{
		try
		{
			T instance = cls.newInstance();
			instance.setNull(true);
			return instance;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RecordProxyException("Error creating null stub: " + e.getMessage());
		}
	}

	public void setRecord(Record record)
	{
		this.record = record;
	}

	@Transient
	public Record getRecord()
	{
		return record;
	}

	public void setNull(boolean isNull)
	{
		this.isNull = isNull;
	}

	public boolean isNull()
	{
		return isNull;
	}

	public final void setLastModifiedDate(Date lastModifiedDate)
	{
		this.lastModifiedDate = lastModifiedDate;
		setInitialized();
	}

	@Property(field = Field.LAST_MODIFIED_DATE_FIELD_NAME)
	public final Date getLastModifiedDate()
	{
		return lastModifiedDate;
	}

	public final void setCreatedDate(Date createdDate)
	{
		this.createdDate = createdDate;
		setInitialized();
	}

	@Property(field = Field.CREATEDDATE_FIELD_NAME)
	public final Date getCreatedDate()
	{
		return createdDate;
	}

	public final void setId(KID id)
	{
		this.id = id;
		setInitialized();
	}

	@Property(field = Field.ID_FIELD_NAME, required = true)
	public final KID getId()
	{
		return id;
	}

	@Deprecated
	public boolean isNull(String property)
	{
		return this.nullifiedProperties.contains(property);
	}

	public void addError (String msg)
	{
		addError(new ValidationError(null, msg, ValidationErrorType.GENERAL));
	}

	private void addError (ValidationError error)
	{
		if (this.errors == null)
		{
			this.errors = new ArrayList<ValidationError>();
		}
		this.errors.add(error);
	}

	@Transient
	public List<ValidationError> getErrors()
	{
		return errors;
	}

	@Transient
	public Object getField(String fieldApiName) throws KommetException
	{
		Method getter = null;
		try
		{
			getter = this.getClass().getMethod("get" + StringUtils.capitalize(fieldApiName));
		}
		catch (NoSuchMethodException e)
		{
			throw new KommetException("Property " + fieldApiName + " does not exist on type");
		}
		
		try
		{
			return getter.invoke(this);
		}
		catch (Exception e)
		{
			throw new KommetException("Error reading value of property " + fieldApiName + ". Nested exception: " + e.getMessage());
		}
	}

	@Property(field = Field.ACCESS_TYPE_FIELD_NAME)
	public Integer getAccessType()
	{
		return accessType;
	}

	public void setAccessType(Integer accessType)
	{
		this.accessType = accessType;
		setInitialized();
	}
}