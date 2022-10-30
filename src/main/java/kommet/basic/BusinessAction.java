/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import java.util.ArrayList;

import kommet.basic.types.SystemTypes;
import kommet.businessprocess.ProcessBlock;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.BUSINESS_ACTION_API_NAME)
public class BusinessAction extends StandardTypeRecordProxy implements ProcessBlock
{
	private String name;
	private String description;
	private Class file;
	private String type;
	private ArrayList<BusinessProcessInput> inputs;
	private ArrayList<BusinessProcessOutput> outputs;
	
	/**
	 * Tells if this action can act as an entry point of a triggerable process
	 */
	private Boolean isEntryPoint;
	
	public static final String FIELD_VALUE_ACTION_FIELD_NAME_INPUT = "field";
	public static final String FIELD_VALUE_ACTION_RECORD_INPUT = "record";
	public static final String FIELD_VALUE_ACTION_OUTPUT = "value";
	
	public BusinessAction() throws KommetException
	{
		this(null, null);
	}
	
	public BusinessAction(Record r, EnvData env) throws KommetException
	{
		super(r, true, env);
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

	@Property(field = "description")
	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
		setInitialized();
	}

	@Property(field = "file")
	public Class getFile()
	{
		return file;
	}

	public void setFile(Class file)
	{
		this.file = file;
		setInitialized();
	}

	@Property(field = "type")
	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
		setInitialized();
	}

	public void addInput(BusinessProcessInput input)
	{
		if (this.inputs == null)
		{
			this.inputs = new ArrayList<BusinessProcessInput>();
		}
		this.inputs.add(input);
	}
	
	public void addOutput(BusinessProcessOutput output)
	{
		if (this.outputs == null)
		{
			this.outputs = new ArrayList<BusinessProcessOutput>();
		}
		this.outputs.add(output);
	}
	
	@Property(field = "inputs")
	public ArrayList<BusinessProcessInput> getInputs()
	{
		return inputs;
	}

	public void setInputs(ArrayList<BusinessProcessInput> inputs)
	{
		this.inputs = inputs;
		setInitialized();
	}

	@Property(field = "outputs")
	public ArrayList<BusinessProcessOutput> getOutputs()
	{
		return outputs;
	}

	public void setOutputs(ArrayList<BusinessProcessOutput> outputs)
	{
		this.outputs = outputs;
		setInitialized();
	}

	@Property(field = "isEntryPoint")
	public Boolean getIsEntryPoint()
	{
		return isEntryPoint;
	}

	public void setIsEntryPoint(Boolean isEntryPoint)
	{
		this.isEntryPoint = isEntryPoint;
		setInitialized();
	}

	@Transient
	public BusinessProcessOutput getOutput(String param)
	{
		if (this.outputs == null)
		{
			return null;
		}
		
		for (BusinessProcessOutput output : this.outputs)
		{
			if (output.getName().equals(param))
			{
				return output;
			}		
		}
		
		// param with name not found
		return null;
	}
	
	@Transient
	public BusinessProcessInput getInput(String param)
	{
		if (this.inputs == null)
		{
			return null;
		}
		
		for (BusinessProcessInput input : this.inputs)
		{
			if (input.getName().equals(param))
			{
				return input;
			}		
		}
		
		// param with name not found
		return null;
	}
	
	public static boolean isInitial (BusinessAction action)
	{
		return action != null && (action.getType().equals("RecordCreate") || action.getType().equals("RecordUpdate") || action.getType().equals("RecordSave"));
	}
}