/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import kommet.basic.types.SystemTypes;
import kommet.businessprocess.BusinessProcessDeclarationException;
import kommet.businessprocess.BusinessProcessException;
import kommet.businessprocess.ProcessBlock;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.BUSINESS_ACTION_INVOCATION_API_NAME)
public class BusinessActionInvocation extends StandardTypeRecordProxy
{
	private BusinessProcess parentProcess;
	private BusinessAction invokedAction;
	private BusinessProcess invokedProcess;
	
	// the name of the variable to which the result of this invocation will be assigned
	// it can be defined by the user on the frontend or generated automatically
	private String name;
	
	private Set<String> traversedNodes;
	
	// the actual data type can be:
	// - a java class (if the value is not a Kommet type)
	// - a KID of the type
	// - the RecordProxy class
	private Map<String, Object> actualOutputDataTypes;
	
	private ArrayList<BusinessActionInvocationAttribute> attributes;
	
	public BusinessActionInvocation() throws KommetException
	{
		this(null, null);
	}
	
	public BusinessActionInvocation(Record r, EnvData env) throws KommetException
	{
		super(r, true, env);
	}

	@Property(field = "parentProcess")
	public BusinessProcess getParentProcess()
	{
		return parentProcess;
	}

	public void setParentProcess(BusinessProcess businessProcess)
	{
		this.parentProcess = businessProcess;
		setInitialized();
	}

	@Property(field = "invokedAction")
	public BusinessAction getInvokedAction()
	{
		return invokedAction;
	}

	public void setInvokedAction(BusinessAction businessAction)
	{
		this.invokedAction = businessAction;
		setInitialized();
	}

	@Property(field = "name")
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
		setInitialized();
	}

	@Property(field = "attributes")
	public ArrayList<BusinessActionInvocationAttribute> getAttributes()
	{
		return attributes;
	}

	public void setAttributes(ArrayList<BusinessActionInvocationAttribute> attributes)
	{
		this.attributes = attributes;
		setInitialized();
	}

	/**
	 * Set a value of an attribute. If attributes with the given name already exist, they will be removed.
	 * @param name
	 * @param value
	 * @throws KommetException
	 */
	public void setAttribute(String name, String value) throws KommetException
	{
		if (this.attributes == null)
		{
			this.attributes = new ArrayList<BusinessActionInvocationAttribute>();
		}
		
		if (!getAttribute(name).isEmpty())
		{
			removeAttribute(name);
		}
		
		BusinessActionInvocationAttribute attr = new BusinessActionInvocationAttribute();
		attr.setName(name);
		attr.setValue(value);
		attr.setInvocation(this);
		this.attributes.add(attr);
	}

	public void removeAttribute(String name)
	{
		if (this.attributes == null)
		{
			return;
		}
		
		ArrayList<BusinessActionInvocationAttribute> newAttributes = new ArrayList<BusinessActionInvocationAttribute>();
		
		for (BusinessActionInvocationAttribute attr : this.attributes)
		{
			if (!attr.getName().equals(name))
			{
				newAttributes.add(attr);
			}
		}
		
		this.attributes = newAttributes;
	}

	@Transient
	public List<BusinessActionInvocationAttribute> getAttribute(String name)
	{
		if (this.attributes == null)
		{
			return null;
		}
		
		List<BusinessActionInvocationAttribute> matchingAttrs = new ArrayList<BusinessActionInvocationAttribute>();
		
		for (BusinessActionInvocationAttribute attr : this.attributes)
		{
			if (attr.getName().equals(name))
			{
				matchingAttrs.add(attr);
			}
		}
		
		return matchingAttrs;
	}

	public boolean isAttributeSet(String name)
	{
		if (this.attributes == null)
		{
			return false;
		}
		
		for (BusinessActionInvocationAttribute attr : this.attributes)
		{
			if (attr.getName().equals(name))
			{
				return true;
			}
		}
		
		return false;
	}

	public boolean hasTraversedNode(String nodeId)
	{
		if (this.traversedNodes == null)
		{
			return false;
		}
		return this.traversedNodes.contains(nodeId);
	}

	public void addTraversedNode(String nodeId)
	{
		if (this.traversedNodes == null)
		{
			this.traversedNodes = new HashSet<String>();
		}
		this.traversedNodes.add(nodeId);
	}

	@Transient
	public Set<String> getTraversedNodes()
	{
		return traversedNodes;
	}

	public void addTraversedNodes(Set<String> traversedNodes)
	{
		if (traversedNodes == null)
		{
			return;
		}
		
		if (this.traversedNodes == null)
		{
			this.traversedNodes = new HashSet<String>();
		}
		this.traversedNodes.addAll(traversedNodes);
	}

	@Property(field = "invokedProcess")
	public BusinessProcess getInvokedProcess()
	{
		return invokedProcess;
	}

	public void setInvokedProcess(BusinessProcess invokedProcess)
	{
		this.invokedProcess = invokedProcess;
		setInitialized();
	}
	
	@Transient
	public ArrayList<BusinessProcessInput> getInputs() throws BusinessProcessException
	{
		ProcessBlock callable = getCallable();
		if (callable == null)
		{
			throw new BusinessProcessException("Neither process nor action set on invocation " + this.name);
		}
		return callable.getInputs();
	}
	
	@Transient
	public ArrayList<BusinessProcessOutput> getOutputs() throws BusinessProcessException
	{
		ProcessBlock callable = getCallable();
		if (callable == null)
		{
			throw new BusinessProcessException("Neither process nor action set on invocation " + this.name);
		}
		return callable.getOutputs();
	}
	
	@Transient
	public ProcessBlock getCallable() throws BusinessProcessException
	{
		if (this.invokedAction != null)
		{
			return this.invokedAction;
		}
		else if (this.invokedProcess != null)
		{
			return this.invokedProcess;
		}
		else
		{
			return null;
		}
	}

	@Transient
	public String getSingleAttributeValue(String name) throws BusinessProcessException
	{
		BusinessActionInvocationAttribute attr = getSingleAttribute(name);
		return attr != null ? attr.getValue() : null;
	}

	@Transient
	public BusinessActionInvocationAttribute getSingleAttribute(String name) throws BusinessProcessException
	{
		List<BusinessActionInvocationAttribute> attrs = getAttribute(name);
		if (attrs == null)
		{
			return null;
		}
		else if (attrs.size() > 1)
		{
			throw new BusinessProcessException("Could not get single attribute value for " + name + ", " + attrs.size() + " values found");
		}
		else
		{
			return attrs.isEmpty() ? null : attrs.get(0);
		}
	}

	/**
	 * If possible, narrows down the data type of the output basing on the invocations attributes.
	 * @param output
	 * @return
	 * @throws KommetException 
	 */
	@Transient
	public BusinessProcessOutput getActualOutput(BusinessProcessOutput output, EnvData env) throws KommetException
	{
		if (this.invokedAction != null)
		{
			if ("RecordSave".equals(this.invokedAction.getType()) || "RecordCreate".equals(this.invokedAction.getType()) || "RecordUpdate".equals(this.invokedAction.getType()))
			{
				String acceptedTypes = getSingleAttributeValue("acceptedTypes");
				
				// if there is exactly one accepted type
				if (acceptedTypes != null && !acceptedTypes.contains(","))
				{
					BusinessProcessOutput newOutput = new BusinessProcessOutput();
					newOutput.setDataTypeId(KID.get(acceptedTypes));
					newOutput.setDataTypeName(output.getDataTypeName());
					newOutput.setDescription(output.getDescription());
					newOutput.setName(output.getName());
					newOutput.setId(output.getId());
					newOutput.setBusinessAction(output.getBusinessAction());
					newOutput.setBusinessProcess(output.getBusinessProcess());
					return newOutput;
				}
				else
				{
					return output;
				}
			}
			else if ("QueryUnique".equals(this.invokedAction.getType()))
			{
				String query = getSingleAttributeValue("query");
				if (query != null)
				{
					// infer type from the query
					BusinessProcessOutput newOutput = new BusinessProcessOutput();
					newOutput.setDataTypeId(env.getSelectCriteriaFromDAL(query).getType().getKID());
					newOutput.setDataTypeName(output.getDataTypeName());
					newOutput.setDescription(output.getDescription());
					newOutput.setName(output.getName());
					newOutput.setId(output.getId());
					newOutput.setBusinessAction(output.getBusinessAction());
					newOutput.setBusinessProcess(output.getBusinessProcess());
					return newOutput;
				}
				else
				{
					return output;
				}
			}
			else
			{
				return output;
			}
		}
		else
		{
			return output;
		}
	}
	
	public void setActualDataType(String output, String dataType) throws BusinessProcessDeclarationException
	{
		if (this.actualOutputDataTypes == null)
		{
			this.actualOutputDataTypes = new HashMap<String, Object>();
		}
		
		this.actualOutputDataTypes.put(output, dataType);
	}

	public void setActualDataType(String output, Type dataType) throws BusinessProcessDeclarationException
	{
		if (this.actualOutputDataTypes == null)
		{
			this.actualOutputDataTypes = new HashMap<String, Object>();
		}
		
		this.actualOutputDataTypes.put(output, dataType);
	}

	@Transient
	public Object getActualDataType(String output, EnvData env) throws BusinessProcessException
	{
		if (this.actualOutputDataTypes != null && this.actualOutputDataTypes.containsKey(output))
		{
			return this.actualOutputDataTypes.get(output);
		}
		else
		{
			if (this.getCallable().getOutput(output).getDataTypeId() != null)
			{
				try
				{
					return env.getType(this.getCallable().getOutput(output).getDataTypeId());
				}
				catch (KommetException e)
				{
					throw new BusinessProcessException("Type with ID " + this.getCallable().getOutput(output).getDataTypeId() + " not found. The type is mentioned as return type for output {" + getCallable().getName() + "}." + output);
				}
			}
			else
			{
				return this.getCallable().getOutput(output).getDataTypeName();
			}
		}
	}
}