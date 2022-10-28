/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.businessprocess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import kommet.auth.AuthData;
import kommet.basic.BusinessAction;
import kommet.basic.BusinessActionInvocation;
import kommet.basic.BusinessActionInvocationAttribute;
import kommet.basic.BusinessActionTransition;
import kommet.basic.BusinessProcess;
import kommet.basic.BusinessProcessInput;
import kommet.basic.BusinessProcessOutput;
import kommet.basic.BusinessProcessParamAssignment;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.utils.MiscUtils;

public class ProcessDeserializer
{
	private Map<String, BusinessActionInvocation> invocationsByHash = new HashMap<String, BusinessActionInvocation>();
	private Map<String, BusinessActionTransition> transitionsByHash = new HashMap<String, BusinessActionTransition>();
	private Map<String, BusinessProcessInput> inputsByHash = new HashMap<String, BusinessProcessInput>();
	private Map<String, BusinessProcessOutput> outputsByHash = new HashMap<String, BusinessProcessOutput>();
	private Map<String, BusinessProcessParamAssignment> paramAssignmentByHash = new HashMap<String, BusinessProcessParamAssignment>();
	private BusinessProcessService bpService;
	private EnvData env;
	private AuthData authData;
	private List<String> invocationPositions = new ArrayList<String>();
	
	public ProcessDeserializer (BusinessProcessService bpService, AuthData authData, EnvData env)
	{
		this.bpService = bpService;
		this.env = env;
		this.authData = authData;
	}
	
	/**
	 * Parses a process property map into a process
	 * @param processMap
	 * @return
	 * @throws KommetException 
	 */
	@SuppressWarnings("unchecked")
	public BusinessProcess getProcessFromMap(HashMap<String, Object> map) throws KommetException
	{
		BusinessProcess process = new BusinessProcess();
		
		if (map.get("id") != null)
		{
			process.setId(KID.get((String)map.get("id")));
		}
		
		process.setName((String)map.get("name"));
		process.setLabel((String)map.get("label"));
		process.setIsDraft((Boolean)map.get("isDraft"));
		process.setIsCallable(Boolean.TRUE.equals((Boolean)map.get("isCallable")));
		process.setIsTriggerable(Boolean.TRUE.equals((Boolean)map.get("isTriggerable")));
		process.setIsActive((Boolean)map.get("isActive"));
		
		process.setInvocations(getProcessInvocations((List<LinkedHashMap<String, Object>>)map.get("invocations")));
		process.setTransitions(getProcessTransitions((List<LinkedHashMap<String, Object>>)map.get("transitions")));
		process.setInputs(getProcessInputs((List<LinkedHashMap<String, Object>>)map.get("inputs")));
		process.setOutputs(getProcessOutputs((List<LinkedHashMap<String, Object>>)map.get("outputs")));
		
		// param assignments must be read after the invocations, inputs and outputs are initialized
		process.setParamAssignments(getProcessParamAssignments((List<LinkedHashMap<String, Object>>)map.get("paramAssignments")));
		
		process.setDisplaySettings("{ \"invocations\": [ " + MiscUtils.implode(this.invocationPositions, ", ") + "] }");
		
		return process;
	}
	
	private ArrayList<BusinessProcessOutput> getProcessOutputs(List<LinkedHashMap<String, Object>> serializedOutputs) throws KommetException
	{
		ArrayList<BusinessProcessOutput> outputs = new ArrayList<BusinessProcessOutput>();
		
		if (serializedOutputs == null)
		{
			return outputs;
		}
		
		for (LinkedHashMap<String, Object> serializedOutput : serializedOutputs)
		{
			BusinessProcessOutput output = new BusinessProcessOutput();
			output.setName((String)serializedOutput.get("name"));
			output.setDataTypeName((String)serializedOutput.get("dataTypeName"));
			
			String sDataTypeId = (String)serializedOutput.get("dataTypeId");
			output.setDataTypeId(sDataTypeId != null ? KID.get(sDataTypeId) : null);
			
			outputs.add(output);
			
			this.outputsByHash.put((String)serializedOutput.get("hash"), output);
		}
		
		return outputs;
	}

	private ArrayList<BusinessProcessInput> getProcessInputs(List<LinkedHashMap<String, Object>> serializedInputs) throws KommetException
	{
		ArrayList<BusinessProcessInput> inputs = new ArrayList<BusinessProcessInput>();
		
		if (serializedInputs == null)
		{
			return inputs;
		}
		
		for (LinkedHashMap<String, Object> serializedInput : serializedInputs)
		{
			BusinessProcessInput input = new BusinessProcessInput();
			input.setName((String)serializedInput.get("name"));
			input.setDataTypeName((String)serializedInput.get("dataTypeName"));
			
			String sDataTypeId = (String)serializedInput.get("dataTypeId");
			input.setDataTypeId(sDataTypeId != null ? KID.get(sDataTypeId) : null);
			
			inputs.add(input);
			
			this.inputsByHash.put((String)serializedInput.get("hash"), input);
		}
		
		return inputs;
	}

	private ArrayList<BusinessProcessParamAssignment> getProcessParamAssignments(List<LinkedHashMap<String, Object>> serializedAssignments) throws KommetException
	{
		if (serializedAssignments == null)
		{
			return null;
		}
		
		ArrayList<BusinessProcessParamAssignment> assignments = new ArrayList<BusinessProcessParamAssignment>();
		
		for (LinkedHashMap<String, Object> serializedAssignment : serializedAssignments)
		{
			BusinessProcessParamAssignment assignment = deserializeParamAssignment(serializedAssignment);
			this.paramAssignmentByHash.put((String)serializedAssignment.get("hash"), assignment);
			assignments.add(assignment);
		}
		
		return assignments;
	}

	@SuppressWarnings("unchecked")
	private BusinessProcessParamAssignment deserializeParamAssignment(LinkedHashMap<String, Object> serializedAssignment) throws KommetException
	{
		BusinessProcessParamAssignment a = new BusinessProcessParamAssignment();
		
		LinkedHashMap<String, Object> source = (LinkedHashMap<String, Object>)serializedAssignment.get("source");
		LinkedHashMap<String, Object> processInput = (LinkedHashMap<String, Object>)serializedAssignment.get("processInput");
		
		if (source != null)
		{
			if (source.containsKey("invocation"))
			{
				a.setSourceInvocation(this.invocationsByHash.get(((LinkedHashMap<String, Object>)source.get("invocation")).get("hash")));
				
				BusinessProcessOutput outputParam = new BusinessProcessOutput();
				LinkedHashMap<String, Object> serializedParam = (LinkedHashMap<String, Object>)((LinkedHashMap<String, Object>)source.get("param"));
				outputParam.setId(KID.get((String)serializedParam.get("id")));
				outputParam.setName((String)serializedParam.get("name"));
				
				a.setSourceInvocation(this.invocationsByHash.get(((LinkedHashMap<String, Object>)source.get("invocation")).get("hash")));
				a.setSourceParam(outputParam);
			}
			else
			{
				throw new KommetException("Process property source.invocation not defined on param assignment");
			}
		}
		else if (processInput != null)
		{
			BusinessProcessInput input = this.inputsByHash.get((String)processInput.get("hash"));
			
			if (input == null)
			{
				throw new KommetException("Input with hash " + (String)processInput.get("hash") + " not found");
			}
					
			a.setProcessInput(input);
		}
		else
		{
			throw new KommetException("Neither source invocation nor process input defined on param assignment");
		}
		
		LinkedHashMap<String, Object> target = (LinkedHashMap<String, Object>)serializedAssignment.get("target");
		LinkedHashMap<String, Object> processOutput = (LinkedHashMap<String, Object>)serializedAssignment.get("processOutput");
		
		if (target != null)
		{
			if (target.containsKey("invocation"))
			{
				a.setTargetInvocation(this.invocationsByHash.get(((LinkedHashMap<String, Object>)target.get("invocation")).get("hash")));
				
				BusinessProcessInput inputParam = new BusinessProcessInput();
				LinkedHashMap<String, Object> serializedParam = (LinkedHashMap<String, Object>)((LinkedHashMap<String, Object>)target.get("param"));
				inputParam.setId(KID.get((String)serializedParam.get("id")));
				inputParam.setName((String)serializedParam.get("name"));
				
				a.setTargetInvocation(this.invocationsByHash.get(((LinkedHashMap<String, Object>)target.get("invocation")).get("hash")));
				a.setTargetParam(inputParam);
			}
			else
			{
				throw new KommetException("Process property target.invocation not defined on param assignment");
			}
		}
		else if (processOutput != null)
		{
			BusinessProcessOutput output = this.outputsByHash.get((String)processOutput.get("hash"));
			
			if (output == null)
			{
				throw new KommetException("Output with hash " + (String)processInput.get("hash") + " not found");
			}
					
			a.setProcessOutput(output);
		}
		else
		{
			throw new KommetException("Neither source invocation nor process input defined on param assignment");
		}
		
		return a;
	}

	private ArrayList<BusinessActionTransition> getProcessTransitions(List<LinkedHashMap<String, Object>> serializedTransitions) throws KommetException
	{
		if (serializedTransitions == null)
		{
			return null;
		}
		
		ArrayList<BusinessActionTransition> transitions = new ArrayList<BusinessActionTransition>();
		
		for (LinkedHashMap<String, Object> serializedTransition : serializedTransitions)
		{
			BusinessActionTransition transition = deserializeTransition(serializedTransition);
			this.transitionsByHash.put((String)serializedTransition.get("hash"), transition);
			transitions.add(transition);
		}
		
		return transitions;
	}

	@SuppressWarnings("unchecked")
	private BusinessActionTransition deserializeTransition(LinkedHashMap<String, Object> serializedTransition) throws KommetException
	{
		BusinessActionTransition trans = new BusinessActionTransition();
		
		String prevActionHash = (String)((LinkedHashMap<String, Object>)serializedTransition.get("prevAction")).get("hash");
		trans.setPreviousAction(this.invocationsByHash.get(prevActionHash));
		
		String nextActionHash = (String)((LinkedHashMap<String, Object>)serializedTransition.get("nextAction")).get("hash");
		trans.setNextAction(this.invocationsByHash.get(nextActionHash));
		
		return trans;
	}

	@SuppressWarnings("unchecked")
	private ArrayList<BusinessActionInvocation> getProcessInvocations(List<LinkedHashMap<String, Object>> serializedInvocations) throws KommetException
	{
		if (serializedInvocations == null)
		{
			return null;
		}
		
		ArrayList<BusinessActionInvocation> invocations = new ArrayList<BusinessActionInvocation>();
		
		for (LinkedHashMap<String, Object> serializedInvocation : serializedInvocations)
		{
			BusinessActionInvocation invocation = deserializeInvocation(serializedInvocation);
			this.invocationsByHash.put((String)serializedInvocation.get("hash"), invocation);
			invocations.add(invocation);
			
			// read invocation position
			if (serializedInvocation.containsKey("position"))
			{
				LinkedHashMap<String, Object> position = (LinkedHashMap<String, Object>)serializedInvocation.get("position");
				this.invocationPositions.add("{ \"invocation\": { \"name\": \"" + invocation.getName() + "\" }, \"position\": { \"x\": " + (Integer)position.get("x") + ", \"y\": " + (Integer)position.get("y") + " } }"); 
			}
		}
		
		return invocations;
	}

	@SuppressWarnings("unchecked")
	private BusinessActionInvocation deserializeInvocation(LinkedHashMap<String, Object> serializedInvocation) throws KommetException
	{
		BusinessActionInvocation inv = new BusinessActionInvocation();
		
		inv.setName((String)serializedInvocation.get("name"));
		
		LinkedHashMap<String, Object> serializedCallable = (LinkedHashMap<String, Object>)serializedInvocation.get("invokedAction");
		if ("action".equals(serializedCallable.get("callableType")))
		{
			inv.setInvokedAction((BusinessAction)deserializeAction(serializedCallable));
		}
		else if ("process".equals(serializedCallable.get("callableType")))
		{
			inv.setInvokedProcess((BusinessProcess)deserializeProcess(serializedCallable));
		}
		else
		{
			throw new BusinessProcessException("Unsupported callable type " + serializedCallable.get("callableType"));
		}
		
		inv.setAttributes((ArrayList<BusinessActionInvocationAttribute>)deserializeAttributes((ArrayList<LinkedHashMap<String, Object>>)serializedInvocation.get("attributes"), inv));
		
		return inv;
	}

	private ArrayList<BusinessActionInvocationAttribute> deserializeAttributes(ArrayList<LinkedHashMap<String, Object>> serializedAttrs, BusinessActionInvocation inv) throws KommetException
	{
		if (serializedAttrs == null)
		{
			return null;
		}
		
		ArrayList<BusinessActionInvocationAttribute> attrs = new ArrayList<BusinessActionInvocationAttribute>();
		
		for (LinkedHashMap<String, Object> serializedAttr : serializedAttrs)
		{
			BusinessActionInvocationAttribute attr = deserializeAttribute(serializedAttr);
			attr.setInvocation(inv);
			attrs.add(attr);
		}
		
		return attrs;
	}

	private BusinessActionInvocationAttribute deserializeAttribute(LinkedHashMap<String, Object> serializedAttr) throws KommetException
	{
		BusinessActionInvocationAttribute attr = new BusinessActionInvocationAttribute();
		attr.setName((String)serializedAttr.get("name"));
		attr.setValue((String)serializedAttr.get("value"));
		
		return attr;
	}

	private BusinessAction deserializeAction(LinkedHashMap<String, Object> serializedAction) throws KommetException
	{
		return bpService.getAction(KID.get((String)serializedAction.get("id")), authData, env);
	}
	
	private BusinessProcess deserializeProcess(LinkedHashMap<String, Object> serializedProcess) throws KommetException
	{
		return bpService.getBusinessProcess(KID.get((String)serializedProcess.get("id")), authData, env);
	}
}