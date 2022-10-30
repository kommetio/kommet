/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.basic;

import java.util.ArrayList;
import java.util.List;

import kommet.basic.types.SystemTypes;
import kommet.businessprocess.BusinessProcessDeclarationException;
import kommet.businessprocess.BusinessProcessException;
import kommet.businessprocess.ProcessBlock;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.persistence.Entity;
import kommet.persistence.Property;
import kommet.persistence.Transient;
import kommet.utils.AppConfig;

@Entity(type = AppConfig.BASE_TYPE_PACKAGE + "." + SystemTypes.BUSINESS_PROCESS_API_NAME)
public class BusinessProcess extends StandardTypeRecordProxy implements ProcessBlock
{
	private String name;
	private String label;
	private String description;
	private ArrayList<BusinessActionInvocation> invocations;
	private ArrayList<BusinessActionTransition> transitions;
	private ArrayList<BusinessProcessParamAssignment> paramAssignments;
	private ArrayList<BusinessProcessInput> inputs;
	private ArrayList<BusinessProcessOutput> outputs;
	private Boolean isDraft;
	private Boolean isActive;
	private Boolean isCallable;
	private Boolean isTriggerable;
	private Class compiledClass;
	private String invocationOrder;
	private String displaySettings;
	
	public BusinessProcess() throws KommetException
	{
		this(null, null);
	}
	
	public BusinessProcess(Record r, EnvData env) throws KommetException
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

	@Property(field = "invocations")
	public ArrayList<BusinessActionInvocation> getInvocations()
	{
		return invocations;
	}

	public void setInvocations(ArrayList<BusinessActionInvocation> invocations)
	{
		this.invocations = invocations;
		setInitialized();
	}

	@Property(field = "transitions")
	public ArrayList<BusinessActionTransition> getTransitions()
	{
		return transitions;
	}

	public void setTransitions(ArrayList<BusinessActionTransition> transitions)
	{
		this.transitions = transitions;
	}

	@Property(field = "isDraft")
	public Boolean getIsDraft()
	{
		return isDraft;
	}

	public void setIsDraft(Boolean isDraft)
	{
		this.isDraft = isDraft;
		setInitialized();
	}

	@Property(field = "isActive")
	public Boolean getIsActive()
	{
		return isActive;
	}

	public void setIsActive(Boolean isActive)
	{
		this.isActive = isActive;
		setInitialized();
	}

	@Property(field = "label")
	public String getLabel()
	{
		return label;
	}

	public void setLabel(String label)
	{
		this.label = label;
		setInitialized();
	}

	@Property(field = "paramAssignments")
	public ArrayList<BusinessProcessParamAssignment> getParamAssignments()
	{
		return paramAssignments;
	}

	public void setParamAssignments(ArrayList<BusinessProcessParamAssignment> paramAssignments)
	{
		this.paramAssignments = paramAssignments;
		setInitialized();
	}

	/**
	 * Adds an action invocation to the business process.
	 * @param action
	 * @param actionName Name to be given to the invocation
	 * @return
	 * @throws KommetException
	 */
	public BusinessActionInvocation addAction(BusinessAction action, String actionName) throws KommetException
	{
		if (this.invocations == null)
		{
			this.invocations = new ArrayList<BusinessActionInvocation>();
		}
		
		BusinessActionInvocation invocation = new BusinessActionInvocation();
		invocation.setInvokedAction(action);
		invocation.setName(actionName);
		invocation.setParentProcess(this);
		
		this.invocations.add(invocation);
		
		return invocation;
	}
	
	public BusinessActionInvocation addAction(BusinessProcess process, String actionName) throws KommetException
	{
		if (this.invocations == null)
		{
			this.invocations = new ArrayList<BusinessActionInvocation>();
		}
		
		BusinessActionInvocation invocation = new BusinessActionInvocation();
		invocation.setInvokedProcess(process);
		invocation.setName(actionName);
		invocation.setParentProcess(this);
		
		this.invocations.add(invocation);
		
		return invocation;
	}

	/**
	 * Creates an assignment of one action's output to another action's input
	 * @param src
	 * @param output
	 * @param target
	 * @param input
	 * @throws KommetException
	 */
	public void assignParam(BusinessActionInvocation src, BusinessProcessOutput output, BusinessActionInvocation target, BusinessProcessInput input) throws KommetException
	{
		BusinessProcessParamAssignment assignment = new BusinessProcessParamAssignment();
		assignment.setBusinessProcess(this);
		assignment.setSourceInvocation(src);
		assignment.setTargetInvocation(target);
		assignment.setSourceParam(output);
		assignment.setTargetParam(input);
		
		if (input == null)
		{
			throw new BusinessProcessDeclarationException("Null input passed to assignment");
		}
		
		if (output == null)
		{
			throw new BusinessProcessDeclarationException("Null output passed to assignment");
		}
		
		if (src == null)
		{
			throw new BusinessProcessDeclarationException("Null source invocation passed to assignment");
		}
		
		if (target == null)
		{
			throw new BusinessProcessDeclarationException("Null target invocation passed to assignment");
		}
		
		addParamAssignment(assignment);
	}

	@Property(field = "compiledClass")
	public Class getCompiledClass()
	{
		return compiledClass;
	}

	public void setCompiledClass(Class compiledClass)
	{
		this.compiledClass = compiledClass;
		setInitialized();
	}
	
	public void addInput(BusinessProcessInput input)
	{
		if (this.inputs == null)
		{
			this.inputs = new ArrayList<BusinessProcessInput>();
		}
		this.inputs.add(input);
		setInitialized("inputs");
	}
	
	public void addOutput(BusinessProcessOutput output)
	{
		if (this.outputs == null)
		{
			this.outputs = new ArrayList<BusinessProcessOutput>();
		}
		this.outputs.add(output);
		setInitialized("outputs");
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

	@Property(field = "invocationOrder")
	public String getInvocationOrder()
	{
		return invocationOrder;
	}

	public void setInvocationOrder(String invocationOrder)
	{
		this.invocationOrder = invocationOrder;
		setInitialized();
	}

	public void addTransition(BusinessActionInvocation call1, BusinessActionInvocation call2) throws KommetException
	{
		if (this.transitions == null)
		{
			this.transitions = new ArrayList<BusinessActionTransition>();
		}
		
		if (call1.getCallable() == null)
		{
			throw new BusinessProcessDeclarationException("Cannot add transition. Source invocation has no action/process assigned");
		}
		else if (call1.getCallable().getId() == null)
		{
			throw new BusinessProcessDeclarationException("Cannot add transition. Source invocation has unsaved action/process assigned");
		}
		
		if (call2.getCallable() == null)
		{
			throw new BusinessProcessDeclarationException("Cannot add transition. Target invocation has no action/process assigned");
		}
		else if (call2.getCallable().getId() == null)
		{
			throw new BusinessProcessDeclarationException("Cannot add transition. Target invocation has unsaved action/process assigned");
		}
		
		BusinessActionTransition transition = new BusinessActionTransition();
		transition.setPreviousAction(call1);
		transition.setNextAction(call2);
		transition.setBusinessProcess(this);
		this.transitions.add(transition);
	}

	public void addInput(String name, String desc, String dataTypeName, String targetInputName, BusinessActionInvocation targetInvocation) throws KommetException
	{
		BusinessProcessInput input = new BusinessProcessInput();
		input.setName(name);
		input.setDescription(desc);
		input.setDataTypeName(dataTypeName);
		
		addInput(input);
		
		// assign input to the input of the action
		BusinessProcessParamAssignment assignment = new BusinessProcessParamAssignment();
		assignment.setBusinessProcess(this);
		assignment.setProcessInput(input);
		assignment.setTargetInvocation(targetInvocation);
		assignment.setTargetParam(targetInvocation.getCallable().getInput(targetInputName));
		
		this.addParamAssignment(assignment);
	}
	
	public void addInput(String name, String desc, KID dataTypeId, String targetInputName, BusinessActionInvocation targetInvocation) throws KommetException
	{
		BusinessProcessInput input = new BusinessProcessInput();
		input.setName(name);
		input.setDescription(desc);
		input.setDataTypeId(dataTypeId);
		
		addInput(input);
		
		// assign input to the input of the action
		BusinessProcessParamAssignment assignment = new BusinessProcessParamAssignment();
		assignment.setBusinessProcess(this);
		assignment.setProcessInput(input);
		assignment.setTargetInvocation(targetInvocation);
		assignment.setTargetParam(targetInvocation.getCallable().getInput(targetInputName));
		
		this.addParamAssignment(assignment);
	}
	
	public void addOutput(String name, String desc, String typeName, String targetOutputName, BusinessActionInvocation sourceInvocation) throws KommetException
	{
		BusinessProcessOutput output = new BusinessProcessOutput();
		output.setName(name);
		output.setDescription(desc);
		output.setDataTypeName(typeName);
		
		addOutput(output);
		
		// assign input to the input of the action
		BusinessProcessParamAssignment assignment = new BusinessProcessParamAssignment();
		assignment.setBusinessProcess(this);
		assignment.setProcessOutput(output);
		assignment.setSourceInvocation(sourceInvocation);
		assignment.setSourceParam(sourceInvocation.getCallable().getOutput(targetOutputName));
		
		this.paramAssignments.add(assignment);
	}
	
	public void addOutput(String name, String desc, KID targetTypeId, String targetOutputName, BusinessActionInvocation sourceInvocation) throws KommetException
	{
		BusinessProcessOutput output = new BusinessProcessOutput();
		output.setName(name);
		output.setDescription(desc);
		output.setDataTypeId(targetTypeId);
		
		addOutput(output);
		
		// assign input to the input of the action
		BusinessProcessParamAssignment assignment = new BusinessProcessParamAssignment();
		assignment.setBusinessProcess(this);
		assignment.setProcessOutput(output);
		assignment.setSourceInvocation(sourceInvocation);
		assignment.setSourceParam(sourceInvocation.getCallable().getOutput(targetOutputName));
		
		this.paramAssignments.add(assignment);
	}

	public void addParamAssignment(BusinessProcessParamAssignment a)
	{
		if (this.paramAssignments == null)
		{
			this.paramAssignments = new ArrayList<BusinessProcessParamAssignment>();
		}
		this.paramAssignments.add(a);
		setInitialized("paramAssignments");
	}

	public void addInvocation(BusinessActionInvocation inv)
	{
		if (this.invocations == null)
		{
			this.invocations = new ArrayList<BusinessActionInvocation>();
		}
		
		this.invocations.add(inv);
		setInitialized("invocations");
	}

	public void addTransition(BusinessActionTransition t)
	{
		if (this.transitions == null)
		{
			this.transitions = new ArrayList<BusinessActionTransition>();
		}
		this.transitions.add(t);
		setInitialized("transitions");
	}

	public void removeTransition(BusinessActionInvocation call1, BusinessActionInvocation call2)
	{
		if (this.transitions == null)
		{
			return;
		}
		
		ArrayList<BusinessActionTransition> newTransitions = new ArrayList<BusinessActionTransition>();
		
		for (BusinessActionTransition t : this.transitions)
		{
			if (!(t.getPreviousAction().getId().equals(call1.getId()) && t.getNextAction().getId().equals(call2.getId())))
			{
				newTransitions.add(t);
			}
		}
		
		this.transitions.clear();
		this.transitions.addAll(newTransitions);
	}

	@Transient
	public BusinessActionInvocation getInvocation(String name)
	{
		if (this.invocations == null)
		{
			return null;
		}
		
		for (BusinessActionInvocation inv : this.invocations)
		{
			if (inv.getName().equals(name))
			{
				return inv;
			}
		}
		
		return null;
	}

	public void removeInput(String name) throws BusinessProcessException
	{
		if (this.inputs == null)
		{
			return;
		}
		
		BusinessProcessInput removedInput = null;
		
		ArrayList<BusinessProcessInput> newInputs = new ArrayList<BusinessProcessInput>();
		for (BusinessProcessInput input : this.inputs)
		{
			if (!input.getName().equals(name))
			{
				newInputs.add(input);
			}
			else
			{
				removedInput = input;
			}
		}
		
		if (removedInput == null)
		{
			throw new BusinessProcessException("Cannot remove process input " + name + " because it does not exist");
		}
		
		this.inputs = newInputs;
		
		// remove param assignments for this input
		if (this.paramAssignments != null)
		{
			for (BusinessProcessParamAssignment a : this.paramAssignments)
			{
				if (a.getProcessInput() != null && a.getProcessInput().getId().equals(removedInput.getId()))
				{
					removeAssignment(a);
				}
			}
		}
	}
	
	public void removeOutput(String name)
	{
		if (this.outputs == null)
		{
			return;
		}
		
		BusinessProcessOutput removedOutput = null;
		
		ArrayList<BusinessProcessOutput> newOutputs = new ArrayList<BusinessProcessOutput>();
		for (BusinessProcessOutput output : this.outputs)
		{
			if (!output.getName().equals(name))
			{
				newOutputs.add(output);
			}
			else
			{
				removedOutput = output;
			}
		}
		
		this.outputs = newOutputs;
		
		// remove param assignments for this output
		if (this.paramAssignments != null)
		{
			for (BusinessProcessParamAssignment a : this.paramAssignments)
			{
				if (a.getProcessOutput() != null && a.getProcessOutput().getId().equals(removedOutput.getId()))
				{
					removeAssignment(a);
				}
			}
		}
	}

	private void removeAssignment(BusinessProcessParamAssignment removedAssignment)
	{
		if (this.paramAssignments == null)
		{
			return;
		}
		
		ArrayList<BusinessProcessParamAssignment> newAssignments = new ArrayList<BusinessProcessParamAssignment>();
		for (BusinessProcessParamAssignment a : this.paramAssignments)
		{
			if (!a.getId().equals(removedAssignment.getId()))
			{
				newAssignments.add(a);
			}
		}
		
		this.paramAssignments = newAssignments;
	}

	public void addInvocationAttribute(String invocationName, String attrName, String attrValue) throws KommetException
	{
		BusinessActionInvocation inv = getInvocation(invocationName);
		if (inv == null)
		{
			throw new BusinessProcessDeclarationException("Invocation with name " + invocationName + " not found on process");
		}
		inv.setAttribute(attrName, attrValue);
	}

	public void removeInvocation(String name)
	{
		if (this.invocations == null)
		{
			return;
		}
		
		List<BusinessActionInvocation> newInvocations = new ArrayList<BusinessActionInvocation>();
		
		for (BusinessActionInvocation inv : this.invocations)
		{
			if (!inv.getName().equals(name))
			{
				newInvocations.add(inv);
			}
			else
			{
				// remove transitions for this invocation
				removeTransitionsForInvocation(inv);
				removeParamAssignmentsForInvocation(inv);
			}
		}
		
		this.invocations.clear();
		this.invocations.addAll(newInvocations);
	}

	private void removeParamAssignmentsForInvocation(BusinessActionInvocation inv)
	{
		if (this.paramAssignments == null)
		{
			return;
		}
		
		List<BusinessProcessParamAssignment> newAssignments = new ArrayList<BusinessProcessParamAssignment>();
		
		for (BusinessProcessParamAssignment a : this.paramAssignments)
		{
			if (!((a.getSourceInvocation() != null && a.getSourceInvocation().getId().equals(inv.getId()) || (a.getTargetInvocation() != null && a.getTargetInvocation().getId().equals(inv.getId())))))
			{
				newAssignments.add(a);
			}
		}
		
		this.paramAssignments.clear();
		this.paramAssignments.addAll(newAssignments);
	}

	private void removeTransitionsForInvocation(BusinessActionInvocation inv)
	{
		if (this.transitions == null)
		{
			return;
		}
		
		List<BusinessActionTransition> newTransitions = new ArrayList<BusinessActionTransition>();
		
		for (BusinessActionTransition t : this.transitions)
		{
			if (!t.getNextAction().getId().equals(inv.getId()) && !t.getPreviousAction().getId().equals(inv.getId()))
			{
				newTransitions.add(t);
			}
		}
		
		this.transitions.clear();
		this.transitions.addAll(newTransitions);
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

	@Property(field = "isCallable")
	public Boolean getIsCallable()
	{
		return isCallable;
	}

	public void setIsCallable(Boolean isCallable)
	{
		this.isCallable = isCallable;
		setInitialized();
	}

	@Property(field = "isTriggerable")
	public Boolean getIsTriggerable()
	{
		return isTriggerable;
	}

	public void setIsTriggerable(Boolean isTriggerable)
	{
		this.isTriggerable = isTriggerable;
		setInitialized();
	}

	@Property(field = "displaySettings")
	public String getDisplaySettings()
	{
		return displaySettings;
	}

	public void setDisplaySettings(String displaySettings)
	{
		this.displaySettings = displaySettings;
		setInitialized();
	}

	@Transient
	public BusinessProcessInput getSingleInput() throws BusinessProcessException
	{
		if (this.inputs == null && this.inputs.isEmpty())
		{
			throw new BusinessProcessException("Could not get single input from process, because it has no inputs");
		}
		
		if (this.inputs.size() != 1)
		{
			throw new BusinessProcessException("Could not get single input from process, because it " + inputs.size() + " inputs");
		}
		
		return this.inputs.get(0);
	}
}