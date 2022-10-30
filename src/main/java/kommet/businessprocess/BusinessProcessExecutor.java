/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.businessprocess;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.BusinessAction;
import kommet.basic.BusinessActionInvocation;
import kommet.basic.BusinessActionInvocationAttribute;
import kommet.basic.BusinessActionTransition;
import kommet.basic.BusinessProcess;
import kommet.basic.BusinessProcessInput;
import kommet.basic.BusinessProcessOutput;
import kommet.basic.BusinessProcessParamAssignment;
import kommet.basic.RecordProxy;
import kommet.basic.RecordProxyUtil;
import kommet.businessprocess.annotations.Execute;
import kommet.businessprocess.annotations.Input;
import kommet.businessprocess.annotations.Output;
import kommet.data.DataAccessUtil;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.errorlog.ErrorLogService;
import kommet.koll.ClassService;
import kommet.koll.compiler.KommetCompiler;
import kommet.utils.MiscUtils;

/**
 * Utility class that can be used for executing a business process.
 * @author Radek Krawiec
 * @since 21/07/2016 (my birthday!)
 */
public class BusinessProcessExecutor
{
	private List<BusinessActionInvocation> startingPoints = new ArrayList<BusinessActionInvocation>();
	private BusinessActionInvocation entryPoint;
	private Map<KID, List<BusinessActionTransition>> transitionsByNextAction = new HashMap<KID, List<BusinessActionTransition>>();
	private Map<KID, List<BusinessActionTransition>> transitionsByPrevAction = new HashMap<KID, List<BusinessActionTransition>>();
	private Map<KID, Map<String, Object>> actionExecutionResults = new HashMap<KID, Map<String,Object>>();
	private Map<KID, Class<?>> actionClasses = new HashMap<KID, Class<?>>();
	private Map<KID, Map<String, Method>> actionInputSetters = new HashMap<KID, Map<String,Method>>();
	private Map<KID, Map<String, Method>> actionOutputGetters = new HashMap<KID, Map<String,Method>>();
	private Map<KID, Method> executeMethodsByActionId = new HashMap<KID, Method>();
	private Map<KID, BusinessActionInvocation> invocationsById = new HashMap<KID, BusinessActionInvocation>();
	
	// map of blocked invocations - the key is the blocked invocation, the value - the missing (blocking) one
	private Map<KID, MissingInvocations> blockedInvocations = new HashMap<KID, MissingInvocations>();
	private Map<KID, BusinessProcessExecutor> processExecutors = new HashMap<KID, BusinessProcessExecutor>();
	
	private Map<String, Object> processInputValues;
	
	private KommetCompiler compiler;
	private ErrorLogService logService;
	private ClassService classService;
	private DataService dataService;
	private EnvData env;
	private BusinessProcess process;
	private boolean isPrepared;
	private Long processLastModifiedTimestamp;
	
	private static final Logger log = LoggerFactory.getLogger(BusinessProcessExecutor.class);
	
	public BusinessProcessExecutor (KommetCompiler compiler, ErrorLogService logService, ClassService classService, DataService dataService, EnvData env)
	{
		this.compiler = compiler;
		this.logService = logService;
		this.classService = classService;
		this.dataService = dataService;
		this.env = env;
	}
	
	/**
	 * Executes a business process and returns a map of its output values.
	 * @param process The process to execute
	 * @param inputs Map of input values
	 * @return
	 * @throws BusinessProcessException 
	 * @throws KommetException
	 */
	public ProcessResult execute (BusinessProcess process, Map<String, Object> inputs, AuthData authData) throws BusinessProcessException
	{
		if (isPrepared)
		{
			throw new BusinessProcessExecutionException("Cannot prepare the process twice on the same executor instance. Use method execute(inputs) instead to invoke a prepared executor");
		}
		
		// make sure that the time stamp of the process for which the executor was prepared is the same as the timestamp of the current process
		if (this.processLastModifiedTimestamp != null && this.processLastModifiedTimestamp != process.getLastModifiedDate().getTime())
		{
			throw new BusinessProcessExecutionException("Process executor was prepared for an older version of the process, but is called with a newer one");
		}
		
		this.processInputValues = inputs;
		
		prepare(process);
		
		return execute(inputs, authData);
	}

	/**
	 * Clean up the state of the executor related to one specific execution. Leave just the state related to the
	 * business process itself.
	 */
	private void cleanExecutionData()
	{
		this.actionExecutionResults = new HashMap<KID, Map<String,Object>>();
	}
	
	public ProcessResult execute(Map<String, Object> inputs, AuthData authData) throws BusinessProcessException
	{
		return execute(inputs, authData, false);
	}

	/**
	 * Execute a process on a prepared executor.
	 * @param inputs
	 * @return
	 * @throws KommetException
	 */
	public ProcessResult execute(Map<String, Object> inputs, AuthData authData, boolean cleanClassCache) throws BusinessProcessException
	{
		log.debug("Running process " + this.process.getName());
		
		if (!isPrepared)
		{
			throw new BusinessProcessDeclarationException("Executor not prepared");
		}
		
		cleanExecutionData();
		
		if (cleanClassCache)
		{
			// when executors are cached on the environment, the cached instances of action classes may have been created with a different class loader
			// then the one with which the process is called (since classloaders are destroyed and recreated over and over again)
			// this is why we don't want to keep the cached action classes in some cases
			actionClasses = new HashMap<KID, Class<?>>();
			actionInputSetters = new HashMap<KID, Map<String,Method>>();
			actionOutputGetters = new HashMap<KID, Map<String,Method>>();
		}
		
		// assign input parameters to the entry point and call it
		boolean entryPointResult = executeEntryPoint(inputs);
		
		if (!entryPointResult)
		{
			// exit the process, returning empty values
			return getProcessOutput(process, true, false);
		}
		
		for (BusinessActionTransition t : this.process.getTransitions())
		{
			log.debug("Transition from {" + t.getPreviousAction().getName() + "} to {" + t.getNextAction().getName() + "}");
		}
		
		// execute all branches starting at the starting points
		for (BusinessActionInvocation startingPoint : this.startingPoints)
		{
			// execute the starting point, but not if it is an entry point, because entry points have custom logic
			// and are called earlier using the executeEntryPoint() method
			if (!startingPoint.getId().equals(this.entryPoint.getId()))
			{
				// execute just the starting action
				executeInvocation(startingPoint, authData);
			}
			
			log.debug("Executing invocations starting from starting point {" + startingPoint.getName() + "}");
			
			// execute all subsequent actions in this branch, until the branch is finished or blocked
			executeUntilBlocked(process, startingPoint, null, authData);
			
			log.debug("Invocations from starting point {" + startingPoint.getName() + "} completed");
		}
		
		while (true)
		{
			// check if any of the blocked branches have been released
			if (this.blockedInvocations.isEmpty())
			{
				return getProcessOutput(process, true, true);
			}
			
			// branches to resume
			Set<KID> invocationsToResume = new HashSet<KID>();
			
			for (KID blockedInvocation : this.blockedInvocations.keySet())
			{
				MissingInvocations missingInvocations = this.blockedInvocations.get(blockedInvocation);
				Set<String> unblockedInputs = new HashSet<String>();
				
				for (String inputName : missingInvocations.getInvocationsByParamName().keySet())
				{
					Set<KID> missingInvocationIds = missingInvocations.getInvocationsByParamName().get(inputName);
					Set<KID> newMissingInvocationIds = new HashSet<KID>();
					
					// check if in the meantime any of the missing invocations have been executed
					for (KID missingInvocation : missingInvocationIds)
					{
						// check if this invocation is still missing
						if (!this.actionExecutionResults.containsKey(missingInvocation))
						{
							newMissingInvocationIds.add(missingInvocation);
						}
					}
					
					if (newMissingInvocationIds.isEmpty())
					{
						// this parameter is unblocked
						unblockedInputs.add(inputName);
					}
					else
					{
						missingInvocations.getInvocationsByParamName().get(inputName).clear();
						missingInvocations.getInvocationsByParamName().get(inputName).addAll(newMissingInvocationIds);
					}
				}
				
				boolean allParamsUnblocked = true;
				
				// check if all inputs have been unblocked for this invocation
				for (String blockedInput : missingInvocations.getInvocationsByParamName().keySet())
				{
					if (!unblockedInputs.contains(blockedInput))
					{
						allParamsUnblocked = false;
						break;
					}
				}
				
				if (allParamsUnblocked)
				{
					// the branch blocked at "blockedInvocation" is released, we can continue
					invocationsToResume.add(blockedInvocation);
				}
			}
			
			// there are blocked actions, but there aren't any branches that can be resumed
			if (invocationsToResume.isEmpty())
			{
				List<String> blockedActions = new ArrayList<String>();
				for (KID invId : this.blockedInvocations.keySet())
				{
					BusinessActionInvocation blockedInv = this.invocationsById.get(invId);
					
					if (blockedInv == null)
					{
						throw new BusinessProcessException("Did not find invocation with ID " + invId);
					}
					
					List<String> missingInvocations = new ArrayList<String>();
					for (Set<KID> missingInvIds : this.blockedInvocations.get(invId).invocationsByParamName.values())
					{
						for (KID missingInvId : missingInvIds)
						{
							missingInvocations.add(this.invocationsById.get(missingInvId).getName());
						}
					}
					
					blockedActions.add(blockedInv.getName() + " (waiting for action {" + MiscUtils.implode(missingInvocations, ", ") + "})");
				}
				
				throw new BusinessProcessException("Business process is blocked at the following actions: " + MiscUtils.implode(blockedActions, ", "));
			}
			
			// execute the branches that can be resumed
			for (KID blockedInvocationId : invocationsToResume)
			{
				log.debug("Resuming unblocked invocation {" + this.invocationsById.get(blockedInvocationId).getName() + "} (of " + invocationsToResume.size() + ")");
				executeInvocation(this.invocationsById.get(blockedInvocationId), authData);
				//executeUntilBlocked(process, this.invocationsById.get(blockedInvocationId), null, authData);
				log.debug("Finished resumed invocation {" + this.invocationsById.get(blockedInvocationId).getName() + "}");
			}
		}
	}

	/**
	 * Create a map of process output values from invocation results, basing on parameter mapping.
	 * @param process
	 * @return
	 * @throws KommetException
	 */
	private ProcessResult getProcessOutput(BusinessProcess process, boolean isSuccess, boolean isPassedEntryPoint) throws BusinessProcessException
	{
		// output values mapped by process output name
		Map<String, Object> outputs = new HashMap<String, Object>();
		
		if (!isPassedEntryPoint)
		{
			ProcessResult result = new ProcessResult();
			result.setSuccess(isSuccess);
			result.setPassedEntryPoint(isPassedEntryPoint);
			result.setOutputValues(outputs);
			
			return result;
		}
		
		// for each process output value, look for assignments that set its value
		for (BusinessProcessOutput processOutput : process.getOutputs())
		{
			boolean outputFound = false;
			
			// find assignment for this output value
			for (BusinessProcessParamAssignment a : process.getParamAssignments())
			{
				if (a.getProcessOutput() != null && a.getProcessOutput().getName().equals(processOutput.getName()))
				{
					if (a.getSourceInvocation() == null)
					{
						if (a.getProcessInput() != null)
						{
							// TODO allow for doing this
							throw new BusinessProcessExecutionException("Cannot simply pass input value to output without processing");
						}
						else
						{
							throw new BusinessProcessExecutionException("Source for output param " + a.getProcessOutput().getName() + " not set");
						}
					}
					
					outputFound = true;
					
					Object outputValue = this.actionExecutionResults.get(a.getSourceInvocation().getId()).get(a.getSourceParam().getName());
					outputs.put(processOutput.getName(), outputValue);
				}
			}
			
			if (!outputFound)
			{
				throw new BusinessProcessException("Output value for process output parameter " + processOutput.getName() + " not found");
			}
		}
		
		ProcessResult result = new ProcessResult();
		result.setSuccess(isSuccess);
		result.setPassedEntryPoint(isPassedEntryPoint);
		result.setOutputValues(outputs);
		
		return result;
	}

	/**
	 * Execute subsequent invocation in the current branch until the branch reaches its end, or until it is blocked by some invocation that is
	 * awaiting input from another branch.
	 * @param process
	 * @param lastExecutedInvocation
	 * @throws KommetException
	 */
	private void executeUntilBlocked(BusinessProcess process, BusinessActionInvocation lastExecutedInvocation, Set<KID> filteredNextInvocations, AuthData authData) throws BusinessProcessException
	{
		// find next invocation
		List<BusinessActionTransition> transitions = this.transitionsByPrevAction.get(lastExecutedInvocation.getId());
		
		log.debug("Invocation {" + lastExecutedInvocation.getName() + "} has " + (transitions != null ? transitions.size() : 0) + " outgoing transitions");
		
		if (transitions == null || transitions.isEmpty())
		{
			// this is the last action
			return;
		}
		
		// follow every transition that goes out of this invocation
		for (BusinessActionTransition transition : transitions)
		{	
			// get next action
			BusinessActionInvocation nextInvocation = transition.getNextAction();
			
			log.debug("Transitioning to {" + nextInvocation.getName() + "}" + (nextInvocation.getInvokedAction() != null ? (" (type " + nextInvocation.getInvokedAction().getType() + ")") : ""));
			
			// if we only want to execute some transitions, skip those that are not selected
			// this is used if the previous action was an if-condition
			if (filteredNextInvocations != null && !filteredNextInvocations.isEmpty() && !filteredNextInvocations.contains(nextInvocation.getId()))
			{
				log.debug("Skipping filtered invocation {" + nextInvocation.getName() + "}");
				continue;
			}
		
			MissingInvocations missingInvocations = null;
			
			Set<KID> selectedNextInvocations = new HashSet<KID>();
			
			// handle if-condition
			if (nextInvocation.getInvokedAction() != null && nextInvocation.getInvokedAction().getType().equals("If"))
			{
				IfActionResult ifResult = executeIfAction(nextInvocation, authData);
				
				if (ifResult.getMissingInvocation() != null)
				{
					missingInvocations = ifResult.getMissingInvocation();
				}
				else
				{	
					if (ifResult.getWinningInvocations() != null && !ifResult.getWinningInvocations().isEmpty())
					{
						List<String> winningInvNames = new ArrayList<String>();
						
						// the winning invocation is one of the two transitions that come out of the if-condition that should be followed
						// the other one is ignored because it was bound to the unfulfilled condition
						for (BusinessActionInvocation inv : ifResult.getWinningInvocations())
						{
							selectedNextInvocations.add(inv.getId());
							winningInvNames.add("{" + inv.getName() + "}");
						}
						
						log.debug("IF-condition {" + nextInvocation.getName() + "} passes control to: " + MiscUtils.implode(winningInvNames, ", "));
					}
					else
					{
						// the if-condition executed successfully, but the winning invocation can be null, if there was no transition specified
						// for this case - in this case we just stop the execution and skip to the next invocation
						log.debug("IF-condition {" + nextInvocation.getName() + "} has no winning invocations");
						continue;
					}
				}
			}
			else
			{
				// execute the next action
				missingInvocations = executeInvocation(nextInvocation, authData);
			}
			
			// if the action could not be executed because it is waiting for some invocation that has not been called yet, stop this branch
			if (missingInvocations != null && !missingInvocations.getInvocationsByParamName().isEmpty())
			{
				// this is a superfluous check, but we want to have it
				for (Set<KID> missingInvIds : missingInvocations.getInvocationsByParamName().values())
				{
					for (KID missingInvId : missingInvIds)
					{
						if (missingInvId.equals(nextInvocation.getId()))
						{
							throw new BusinessProcessExecutionException("Invocation {" + nextInvocation.getName() + "} is blocked by itself");
						}
					}
				}
				
				this.blockedInvocations.put(nextInvocation.getId(), missingInvocations);
			}
			else
			{
				log.debug("Passing to {" + nextInvocation.getName() + "}");
				executeUntilBlocked(process, nextInvocation, selectedNextInvocations, authData);
			}
		}
	}

	private IfActionResult executeIfAction(BusinessActionInvocation ifInvocation, AuthData authData) throws BusinessProcessExecutionException, BusinessProcessDeclarationException
	{	
		MissingInvocations missingInvocations = new MissingInvocations();
		
		boolean conditionTrue;
		try
		{
			conditionTrue = evaluateREL(ifInvocation, missingInvocations, authData, env);
		}
		catch (KommetException e)
		{
			throw new BusinessProcessExecutionException("Error evaluating condition: " + e.getMessage());
		}
		
		log.debug("If-condition {" + ifInvocation.getName() + "} evaluated to " + conditionTrue);
		
		if (!missingInvocations.getInvocationsByParamName().isEmpty())
		{
			IfActionResult result = new IfActionResult();
			
			// get the first missing invocation
			result.setMissingInvocations(missingInvocations);
		}
		
		List<BusinessActionInvocation> winningInvocations = new ArrayList<BusinessActionInvocation>();
		
		if (conditionTrue)
		{
			// the "true" transition may not be set
			if (ifInvocation.isAttributeSet("ifTrueInvocationName"))
			{
				List<BusinessActionInvocationAttribute> winningInvocationNames = ifInvocation.getAttribute("ifTrueInvocationName");
				if (winningInvocationNames.isEmpty())
				{
					throw new BusinessProcessExecutionException("On true transition not set on if condition " + ifInvocation.getName());
				}
				
				for (BusinessActionInvocationAttribute inv : winningInvocationNames)
				{
					winningInvocations.add(process.getInvocation(inv.getValue()));
				}
			}
		}
		else
		{
			if (ifInvocation.isAttributeSet("ifFalseInvocationName"))
			{
				List<BusinessActionInvocationAttribute> winningInvocationNames = ifInvocation.getAttribute("ifFalseInvocationName");
				if (winningInvocationNames == null)
				{
					throw new BusinessProcessExecutionException("On false transition not set on if condition " + ifInvocation.getName());
				}
				
				for (BusinessActionInvocationAttribute inv : winningInvocationNames)
				{
					winningInvocations.add(process.getInvocation(inv.getValue()));
				}
			}
		}
		
		IfActionResult result = new IfActionResult();
		result.setWinningInvocation(winningInvocations);
		
		return result;
	}

	/*private Map<String, Map<String, Object>> getRecordsByInvocationName() throws BusinessProcessException
	{
		Map<String, Map<String, Object>> recordsByInvocationName = new HashMap<String, Map<String, Object>>();
		
		for (KID invId : this.actionExecutionResults.keySet())
		{
			Map<String, Object> actionOutputs = this.actionExecutionResults.get(invId);
			Map<String, Object> outputValues = new HashMap<String, Object>();
			
			if (actionOutputs != null)
			{
				for (String outputName : actionOutputs.keySet())
				{
					Object outputVal = actionOutputs.get(outputName);
					if (outputVal instanceof RecordProxy)
					{
						// translate record proxy to record
						ProcessBlock callable = invocationsById.get(invId).getCallable();
						try
						{
							outputValues.put(outputName, RecordProxyUtil.generateRecord((RecordProxy)outputVal, env.getType(callable.getOutput(outputName).getDataTypeId()), 10));
						}
						catch (KommetException e)
						{
							throw new BusinessProcessExecutionException("Could not generate record from proxy of type " + outputVal.getClass().getName());
						}
					}
					else
					{
						outputValues.put(outputName, outputVal);
					}
				}
			}
			
			recordsByInvocationName.put(invocationsById.get(invId).getName(), outputValues);
		}
		
		return recordsByInvocationName;
	}*/

	private boolean evaluateREL(BusinessActionInvocation ifInvocation, MissingInvocations missingInvocations, AuthData authData, EnvData env) throws KIDException, KommetException
	{	
		List<BusinessActionInvocationAttribute> relConditionAttrs = ifInvocation.getAttribute("evaluatorClassId");
		if (relConditionAttrs.isEmpty())
		{
			throw new BusinessProcessExecutionException("Evaluator class not created for if condition " + ifInvocation.getName());
		}
		
		kommet.basic.Class evaluatorClass = classService.getClass(KID.get(relConditionAttrs.get(0).getValue()), env);
		Class<?> evaluatorCompiledClass = null;
		
		try
		{
			evaluatorCompiledClass = compiler.getClass(evaluatorClass, false, env);
		}
		catch (ClassNotFoundException e)
		{
			throw new BusinessProcessExecutionException("Evaluator class not found in class loader for if condition " + ifInvocation.getName());
		}
		catch (MalformedURLException e)
		{
			e.printStackTrace();
			throw new BusinessProcessExecutionException("Could not get class for if condition from class loader");
		}
		
		Object evaluatorInstance = null;
		
		try
		{
			evaluatorInstance = evaluatorCompiledClass.newInstance();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new BusinessProcessExecutionException("Could not instantiate evaluator class for if condition from class loader");
		}
		
		Map<String, Object> injectedValues = new HashMap<String, Object>();
		
		// create a map of injected values
		// each of them has a name "invocation_" + invId + "_" + outputId
		
		List<BusinessActionInvocationAttribute> requiredInvsAttr = ifInvocation.getAttribute("requiredInvocationIds");
		if (requiredInvsAttr.isEmpty())
		{
			throw new BusinessProcessExecutionException("Required invocations not listed for if condition " + ifInvocation.getName());
		}
		
		if (StringUtils.hasText(requiredInvsAttr.get(0).getValue()))
		{
			List<String> sRequiredInvocationIds = MiscUtils.splitAndTrim(requiredInvsAttr.get(0).getValue(), ";");
			for (String invocationWithOutputId : sRequiredInvocationIds)
			{
				List<String> invWithOutputId = MiscUtils.splitAndTrim(invocationWithOutputId, ",");
				
				KID invocationId = KID.get(invWithOutputId.get(0));
				String outputName = invWithOutputId.get(1);
				
				if (!this.actionExecutionResults.containsKey(invocationId))
				{
					// if it's an if-condition, all missing invocations are for the same input parameter - "condition"
					missingInvocations.addMissingInvocation("condition", invocationId);
					continue;
				}
				
				injectedValues.put("invocation_" + invocationId + "_" + outputName, this.actionExecutionResults.get(invocationId).get(outputName));
			}
		}
		
		// make sure there are no missing invocations for this execution
		if (!missingInvocations.getInvocationsByParamName().isEmpty())
		{
			// stop execution
			return false;
		}
		
		// execute the evaluator class
		Method evaluateMethod = MiscUtils.getMethodByName(evaluatorCompiledClass, "evaluate");
		
		try
		{
			return (Boolean)evaluateMethod.invoke(evaluatorInstance, injectedValues);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new BusinessProcessExecutionException("Error calling evaluator for condition " + ifInvocation.getName() + ": " + e.getMessage());
		}
	}

	/**
	 * Executes an entry point. Entry point actions are usually stub actions that just rewrite input parameters to output. Sometimes they perform some custom
	 * logic, but this logic is hardcoded here, not declared in any code class.
	 * @param inputs
	 * @throws KommetException
	 */
	private boolean executeEntryPoint(Map<String, Object> inputs) throws BusinessProcessException
	{
		if (this.entryPoint.getInvokedAction() == null)
		{
			throw new BusinessProcessException("Invalid entry point - is an execution of a process, not an action");
		}
		
		// value of the single parameter that the entry point takes (the record with which the process is called)
		Object inputVal = null;
		String processInputName = null;
		
		if (this.entryPoint.getInvokedAction().getType().equals("RecordCreate") || this.entryPoint.getInvokedAction().getType().equals("RecordUpdate") || this.entryPoint.getInvokedAction().getType().equals("RecordSave"))
		{	
			for (BusinessProcessParamAssignment a : process.getParamAssignments())
			{
				if (a.getTargetInvocation() != null && a.getTargetInvocation().getName().equals(this.entryPoint.getName()))
				{
					if ("record".equals(a.getTargetParam().getName()))
					{
						// entry point can only have assignments from process inputs, not from other invocations
						// TODO add this validation during process save
						if (a.getProcessInput() != null)
						{
							processInputName = a.getProcessInput().getName();
							if (!inputs.containsKey(processInputName))
							{
								throw new BusinessProcessException("Input parameter '" + processInputName + "' not passed to process");
										
							}
							inputVal = inputs.get(processInputName);
						}
						else
						{
							String assignmentSource = a.getSourceInvocation() != null ? "{" + a.getSourceInvocation().getName() + "}." + (a.getSourceParam() != null ? a.getSourceParam().getName() : "null") : "unknown";
							throw new BusinessProcessDeclarationException("Invalid parameter assignment to process entry point {" + this.entryPoint.getName() + "}. The source of such assignment should be a process input param, but is " + assignmentSource);
						}
					}
					else
					{
						throw new BusinessProcessExecutionException("Parameter assignment to entry point {" + this.entryPoint.getName() + "} has invalid target parameter. Expected 'record', but was '" + a.getTargetParam().getName() + "'");
					}
				}
			}
			
			if (this.actionExecutionResults.containsKey(this.entryPoint.getId()))
			{
				throw new BusinessProcessException("Entry point " + this.entryPoint.getName() + " called more than once");
			}
			
			if (this.entryPoint.isAttributeSet("acceptedTypes"))
			{
				List<String> acceptedTypesIds = MiscUtils.splitAndTrim(this.entryPoint.getSingleAttribute("acceptedTypes").getValue(), ",");
				Set<String> typeIdSet = new HashSet<String>();
				typeIdSet.addAll(acceptedTypesIds);
				
				if (!(inputVal instanceof RecordProxy))
				{
					throw new BusinessProcessException("Entry point of process " + this.process.getLabel() + " should be provided with a record proxy as parameter, but was " + inputVal.getClass().getName());
				}
				else if (inputVal != null)
				{
					Type type = null;
					try
					{
						type = env.getType(MiscUtils.envToUserPackage(inputVal.getClass().getName(), env));
					}
					catch (KommetException e)
					{
						e.printStackTrace();
						throw new BusinessProcessException("Could not get type for object of class " + inputVal.getClass().getName());
					}
					
					if (!typeIdSet.contains(type.getKID().getId()))
					{
						// the entry point should not be fired for objects of this type because it is not among the accepted types
						// so we just exit the process
						return false;
					}
				}
			}
			
			// for standard entry points, we just rewrite the input record to the output parameter with the same name ("record")
			this.actionExecutionResults.put(this.entryPoint.getId(), new HashMap<String, Object>());
			this.actionExecutionResults.get(this.entryPoint.getId()).put("record", inputVal);
			return true;
		}
		else
		{
			throw new BusinessProcessException("Unsupported entry point type " + this.entryPoint.getInvokedAction().getType());
		}
	}
	
	/**
	 * Execute an action or a subprocess. Returns null if execution was successful, or BusinessActionInvocation if it was blocked. The returned invocation is such case it the one that should
	 * have been called and from which results are needed.
	 * @param inv
	 * @return
	 * @throws KommetException
	 */
	private MissingInvocations executeInvocation(BusinessActionInvocation inv, AuthData authData) throws BusinessProcessException
	{	
		log.debug("Starting invocation {" + inv.getName() + "}");
		
		MissingInvocations missingInvocations = new MissingInvocations();
		
		// find inputs for this action
		
		Map<String, Object> inputs = new HashMap<String, Object>();
		for (BusinessProcessParamAssignment a : process.getParamAssignments())
		{
			// target invocation may be null if param assignment assigns the value to the process output, not to any invocation
			if (a.getTargetInvocation() == null || !a.getTargetInvocation().getId().equals(inv.getId()))
			{
				continue;
			}
			
			Object inputValue = null;
			
			// the source param for this invocation can come either from another invocation, or from the process input param
			if (a.getSourceInvocation() != null)
			{
				Map<String, Object> result = this.actionExecutionResults.get(a.getSourceInvocation().getId());
				if (result == null)
				{
					// mark that an invocation is missing for this input param
					missingInvocations.addMissingInvocation(a.getTargetParam().getName(), a.getSourceInvocation().getId());
					
					// skip this parameter and process the next one
					continue;
				}
				
				if (!result.containsKey(a.getSourceParam().getName()))
				{
					throw new BusinessProcessExecutionException("Output value not recorded for invoked action " + a.getSourceInvocation().getCallable().getName());
				}
				
				inputValue = result.get(a.getSourceParam().getName());
			}
			else if (a.getProcessInput() != null)
			{
				if (!this.processInputValues.containsKey(a.getProcessInput().getName()))
				{
					throw new BusinessProcessException("Process input values are missing input value for parameter " + a.getProcessInput().getName());
				}
				
				if (!this.processInputValues.containsKey(a.getProcessInput().getName()))
				{
					throw new BusinessProcessException("Missing input value for parameter " + a.getProcessInput().getName());
				}
				
				inputValue = this.processInputValues.get(a.getProcessInput().getName());
			}
			else
			{
				throw new BusinessProcessException("Param assigment has no source defined");
			}
			
			inputs.put(a.getTargetParam().getName(), inputValue);
		}
		
		// at this point the inputs map contains all the parameters that have been found
		// but we need to check if these are all the parameters that the action requires, and if not, take their values from the invocation attributes
		for (BusinessProcessInput input : inv.getInputs())
		{
			if (inputs.containsKey(input.getName()))
			{
				// there is an input value for this parameter, but we might have marked it as awaiting multiple inputs (from conditional branches)
				// so we unmark this (one conditional branch provided input for this parameter, so we don't need to wait for others)
				missingInvocations.getInvocationsByParamName().remove(input.getName());
			}
			else
			{
				// input parameter has no value assigned, either because there is no param assignment for this param, or because this invocation is missing
				// if it's missing, we need to wait for it, but if it is not defined, we can use the default value
				if (!missingInvocations.getInvocationsByParamName().containsKey(input.getName()))
				{
					// there is no missing invocation for this input, so it means that no param assignment was defined for it and we can use the default value
					if (inv.isAttributeSet(input.getName()))
					{
						// use the attribute value as the input param - this allows for using attribute values as default values for inputs
						// and overriding them with assigned parameters
						inputs.put(input.getName(), inv.getSingleAttributeValue(input.getName()));
					}
					else
					{
						throw new BusinessProcessExecutionException("Input parameter " + input.getName() + " of action " + inv.getName() + " is not assigned");
					}
				}
			}
		}
		
		// if there are still missing invocations for this invocation, mark it as blocked
		if (!missingInvocations.getInvocationsByParamName().isEmpty())
		{
			return missingInvocations;
		}
		
		// invoke the associated action or process
		if (inv.getInvokedAction() != null)
		{
			// handle built-in FieldUpdate action
			if ("FieldUpdate".equals(inv.getInvokedAction().getType()))
			{
				callFieldUpdateAction(inv, inputs, authData, env);
			}
			// handle built-in FieldUpdate action
			else if ("FieldValue".equals(inv.getInvokedAction().getType()))
			{
				callFieldValueAction(inv, inputs, authData, env);
			}
			else
			{
				// call action
				Object actionInstance = getActionInstance(inv.getInvokedAction());
				
				// assign input values to the action that is about to be called
				setActionInputs(inputs, actionInstance, inv.getInvokedAction());
				
				// call the actual Java executor method for this action
				callActionMethod(actionInstance, inv, authData);
			}
		}
		else if (inv.getInvokedProcess() != null)
		{
			// call process
			BusinessProcessExecutor executor = getProcessExecutor(inv.getInvokedProcess().getId());
			ProcessResult subprocessResult = executor.execute(inv.getInvokedProcess(), inputs, authData);
			
			this.actionExecutionResults.put(inv.getId(), subprocessResult.getOutputValues());
		}
		else
		{
			// neither action nor process set on invocation - this should never happen due to earlier validation
			throw new BusinessProcessException("Neither process nor action set on invocation " + inv.getName());
		}
		
		// if this action was successfully called, we want to remove it from the list of blocked calls
		// this call may have been blocked by another branch, waiting for the current branch, and the current branch unblocked it
		// when it provided the missing invocations
		this.blockedInvocations.remove(inv.getId());
		
		return missingInvocations;
	}

	private void callFieldUpdateAction(BusinessActionInvocation inv, Map<String, Object> inputs, AuthData authData, EnvData env) throws BusinessProcessExecutionException
	{
		Object recordProxy = inputs.get("record");
		if (recordProxy == null)
		{
			throw new BusinessProcessExecutionException("Null record passed to parameter {" + inv.getName() + "}.record");
		}
		
		if (!(recordProxy instanceof RecordProxy))
		{
			throw new BusinessProcessExecutionException("Record passed to parameter {" + inv.getName() + "}.record is not a subclass of RecordProxy (it is " + recordProxy.getClass().getName() + ")");
		}
		
		// all attributes of this action are treated as property assignments
		// i.e. the attribute names are treated as property names, and attribute values as string property values
		if (inv.getAttributes() != null && !inv.getAttributes().isEmpty())
		{
			Type type = null;
			try
			{
				type = env.getType(MiscUtils.envToUserPackage(recordProxy.getClass().getName(), env));
			}
			catch (KommetException e)
			{
				throw new BusinessProcessExecutionException("Error getting type for proxy class " + recordProxy.getClass().getName());
			}
			
			Record record = null;
			try
			{
				record = RecordProxyUtil.generateRecord((RecordProxy)recordProxy, type, 1, env);
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				throw new BusinessProcessExecutionException("Error generating record from proxy: " + e.getMessage());
			}
			
			for (BusinessActionInvocationAttribute attr : inv.getAttributes())
			{
				try
				{
					record.setField(attr.getName(), attr.getValue());
				}
				catch (KommetException e)
				{
					e.printStackTrace();
					throw new BusinessProcessExecutionException("Error assigning value \"" + attr.getValue() + "\" to field \"" + attr.getName() + "\": " + e.getMessage());
				}
			}
			
			// save the record
			try
			{
				dataService.save(record, authData, env);
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				throw new BusinessProcessExecutionException("Error updating record in field update action: " + e.getMessage());
			}
		}
	}
	
	private void callFieldValueAction(BusinessActionInvocation inv, Map<String, Object> inputs, AuthData authData, EnvData env) throws BusinessProcessExecutionException, BusinessProcessDeclarationException
	{
		log.debug("Running field value action {" + inv.getName() + "}");
		
		Object recordProxy = inputs.get("record");
		if (recordProxy == null)
		{
			throw new BusinessProcessExecutionException("Null record passed to parameter {" + inv.getName() + "}.record");
		}
		
		if (!(recordProxy instanceof RecordProxy))
		{
			throw new BusinessProcessExecutionException("Record passed to parameter {" + inv.getName() + "}.record is not a subclass of RecordProxy (it is " + recordProxy.getClass().getName() + ")");
		}
		
		Type type = null;
		try
		{
			type = env.getType(MiscUtils.envToUserPackage(recordProxy.getClass().getName(), env));
		}
		catch (KommetException e)
		{
			throw new BusinessProcessExecutionException("Error getting type for proxy class " + recordProxy.getClass().getName());
		}
		
		Record record = null;
		try
		{
			record = RecordProxyUtil.generateRecord((RecordProxy)recordProxy, type, 1, env);
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			throw new BusinessProcessExecutionException("Error generating record from proxy: " + e.getMessage());
		}
		
		Field field = BusinessProcessService.getFieldValueActionSingleOutput(inv, type);
		Map<String, Object> actionOutput = new HashMap<String, Object>();
		
		boolean isFieldSet = false;
		
		try
		{
			isFieldSet = record.isSet(field.getApiName());
		}
		catch (KommetException e)
		{
			throw new BusinessProcessExecutionException("Could not check if property " + field.getApiName() + " is set. Nested: " + e.getMessage());
		}
		
		boolean isNestedRecord = field.getDataTypeId().equals(DataType.TYPE_REFERENCE);
		
		// TODO add a unit test that runs a process which updates a field not set in the 
		if (!isFieldSet)
		{
			// if the field is not set on the record, we just fetch it from database
			List<String> fieldsToQuery = new ArrayList<String>();
			
			if (isNestedRecord)
			{
				Type referencedType = null;
				try
				{
					referencedType = env.getType(((TypeReference)field.getDataType()).getTypeId());
					
					// if we are going to query a nested record, we want to query all of its fields
					List<String> nestedFields = DataAccessUtil.getReadableFieldApiNamesForQuery(referencedType, authData, env, true);
					
					for (String nestedField : nestedFields)
					{
						fieldsToQuery.add(field.getApiName() + "." + nestedField);
					}
				}
				catch (KommetException e)
				{
					e.printStackTrace();
					throw new BusinessProcessExecutionException("Error reading fields for querying type " + ((TypeReference)field.getDataType()).getTypeId());
				}
			}
			else
			{
				fieldsToQuery.add(field.getApiName());
			}
			
			try
			{
				List<Record> records = dataService.getRecords(Arrays.asList(record.getKID()), type, fieldsToQuery, authData, env);
				record = records.get(0);
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				throw new BusinessProcessExecutionException("Error fetching record of type " + type.getQualifiedName() + ". Nested: " + e.getMessage());
			}
		}
		
		try
		{
			Object fieldValue = record.getField(field.getApiName());
			
			if (isNestedRecord)
			{
				// convert to proxy because KOLL code expects record proxies, not records
				fieldValue = fieldValue != null ? RecordProxyUtil.generateCustomTypeProxy((Record)fieldValue, env, compiler) : null;
			}
			
			actionOutput.put(BusinessAction.FIELD_VALUE_ACTION_OUTPUT, fieldValue);
			this.actionExecutionResults.put(inv.getId(), actionOutput);
			log.debug("Completed field value action {" + inv.getName() + "}");
		}
		catch (KommetException e)
		{
			e.printStackTrace();
			throw new BusinessProcessExecutionException("Error updating record in field update action: " + e.getMessage());
		}
	}

	private BusinessProcessExecutor getProcessExecutor(KID processId)
	{
		if (!this.processExecutors.containsKey(process.getId()))
		{
			this.processExecutors.put(processId, new BusinessProcessExecutor(compiler, logService, classService, dataService, env));
		}
		return this.processExecutors.get(processId);
	}

	/**
	 * Calls the action method in the action's source class.
	 * @param actionInstance
	 * @param inv
	 * @return
	 * @throws KommetException
	 */
	private Map<String, Object> callActionMethod(Object actionInstance, BusinessActionInvocation inv, AuthData authData) throws BusinessProcessException
	{
		BusinessAction action = inv.getInvokedAction();
		
		if (action == null)
		{
			throw new BusinessProcessException("Cannot call action method because the invocation should call a process");
		}
		
		// add auth data to the thread so that classes are invoked in this context
		this.env.addAuthData(authData);
		
		Method method = getExecuteMethod(action.getId());
		try
		{
			method.invoke(actionInstance);
		}
		catch (Exception e)
		{
			//logService.logException(e, ErrorLogSeverity.ERROR, this.getClass().getName(), -1, authData.getUserId(), env);
			e.printStackTrace();
			throw new BusinessProcessExecutionException("Error calling action " + action.getName() + ": " + e.getMessage());
		}
		finally
		{
			// remove the auth data from the thread
			this.env.clearAuthData();
		}
		
		// read output values
		Map<String, Object> actionOutput = readActionOutput(actionInstance, action);
		
		// remember the output of this call
		this.actionExecutionResults.put(inv.getId(), actionOutput);
		
		return actionOutput;
	}

	private Map<String, Object> readActionOutput(Object actionInstance, BusinessAction action) throws BusinessProcessException
	{
		Map<String, Object> outputValues = new HashMap<String, Object>();
		
		for (BusinessProcessOutput outputParam : action.getOutputs())
		{
			Method getter = getOutputGetter(outputParam.getName(), action);
			
			// call the getter
			try
			{
				outputValues.put(outputParam.getName(), getter.invoke(actionInstance));
			}
			catch (Exception e)
			{
				throw new BusinessProcessExecutionException("Error reading output value for action " + action.getName() + ": " + e.getMessage());
			}
		}
		
		return outputValues;
	}

	private Method getExecuteMethod(KID actionId)
	{
		if (!this.executeMethodsByActionId.containsKey(actionId))
		{
			for (Method m : actionClasses.get(actionId).getDeclaredMethods())
			{
				if (m.isAnnotationPresent(Execute.class))
				{
					this.executeMethodsByActionId.put(actionId, m);
					break;
				}
			}
		}
		
		return this.executeMethodsByActionId.get(actionId);
	}

	private void setActionInputs(Map<String, Object> inputs, Object actionInstance, BusinessAction action) throws BusinessProcessException
	{
		for (BusinessProcessInput input : action.getInputs())
		{
			Method setter = getInputSetter(input.getName(), action);
			
			try
			{
				// call the setter
				setter.invoke(actionInstance, inputs.get(input.getName()));
			}
			catch (Exception e)
			{
				throw new BusinessProcessExecutionException("Error calling setter for input " + input.getName() + ": " + e.getMessage() + ". Expected type " + setter.getParameterTypes()[0].getName() + ", actual argument is of type " + (inputs.get(input.getName()) != null ? inputs.get(input.getName()).getClass().getName() : "null"));
			}
		}
	}
	
	/**
	 * Gets a method that is a getter of the given output property of the action's compiled class.
	 * @param outputName The name of the output property of the action
	 * @param actionId The ID of the action
	 * @return
	 * @throws BusinessProcessExecutionException 
	 */
	private Method getOutputGetter(String outputName, BusinessAction action) throws BusinessProcessExecutionException
	{
		KID actionId = action.getId();
		
		if (!this.actionOutputGetters.containsKey(actionId))
		{
			this.actionOutputGetters.put(actionId, new HashMap<String, Method>());
		}
		
		if (!this.actionOutputGetters.get(actionId).containsKey(outputName))
		{	
			for (Method m : getActionClass(action).getDeclaredMethods())
			{
				if (m.isAnnotationPresent(Output.class))
				{
					Output outputAnnot = m.getAnnotation(Output.class);
					
					if (outputAnnot.name().equals(outputName))
					{
						this.actionOutputGetters.get(actionId).put(outputName, m);
					}
				}
			}
		}
		
		return this.actionOutputGetters.get(actionId).get(outputName);
	}

	/**
	 * Gets a method that is a setter of the given input property of the action's compiled class.
	 * @param inputName The name of the input property of the action
	 * @param action The action for which input setter is searched
	 * @return
	 */
	private Method getInputSetter(String inputName, BusinessAction action) throws BusinessProcessException
	{
		if (!this.actionInputSetters.containsKey(action.getId()))
		{
			this.actionInputSetters.put(action.getId(), new HashMap<String, Method>());
		}
		
		if (!this.actionInputSetters.get(action.getId()).containsKey(inputName))
		{
			for (Method m : getActionClass(action).getDeclaredMethods())
			{
				if (m.isAnnotationPresent(Input.class))
				{
					Input inputAnnot = m.getAnnotation(Input.class);
					
					if (inputAnnot.name().equals(inputName))
					{
						this.actionInputSetters.get(action.getId()).put(inputName, m);
					}
				}
			}
		}
		
		return this.actionInputSetters.get(action.getId()).get(inputName);
	}

	/**
	 * Returns an instance of the action's executor class.
	 * @param action
	 * @return
	 * @throws KommetException
	 */
	private Object getActionInstance(BusinessAction action) throws BusinessProcessException
	{	
		try
		{
			return getActionClass(action).newInstance();
		}
		catch (Exception e)
		{
			throw new BusinessProcessExecutionException("Could not instantiate action class for action " + action.getName() + ": " + e.getMessage());
		}
	}
	
	private Class<?> getActionClass(BusinessAction action) throws BusinessProcessExecutionException
	{
		if (!actionClasses.containsKey(action.getId()))
		{
			try
			{
				actionClasses.put(action.getId(), compiler.getClass(action.getFile(), false, env));
			}
			catch (ClassNotFoundException e1)
			{
				throw new BusinessProcessExecutionException("Class for action " + action.getName() + " not found");
			}
			catch (Exception e)
			{
				throw new BusinessProcessExecutionException("Could not get action class for action " + action.getName() + ": " + e.getMessage());
			}
		}
		
		return actionClasses.get(action.getId());
	}

	/**
	 * Prepares this executor for executing the given process.
	 * @param process
	 * @throws BusinessProcessDeclarationException
	 */
	public void prepare(BusinessProcess process) throws BusinessProcessException
	{
		log.debug("Preparing process " + process.getName());
		
		if (process.getInvocations() == null || process.getInvocations().isEmpty())
		{
			throw new BusinessProcessException("Business process contains no invocations");
		}
		
		for (BusinessActionInvocation inv : process.getInvocations())
		{
			invocationsById.put(inv.getId(), inv);
		}
		
		if (process.getTransitions() != null && !process.getTransitions().isEmpty())
		{	
			for (BusinessActionTransition t : process.getTransitions())
			{
				t.setNextAction(invocationsById.get(t.getNextAction().getId()));
				t.setPreviousAction(invocationsById.get(t.getPreviousAction().getId()));
				
				if (!this.transitionsByNextAction.containsKey(t.getNextAction().getId()))
				{
					this.transitionsByNextAction.put(t.getNextAction().getId(), new ArrayList<BusinessActionTransition>());
				}
				transitionsByNextAction.get(t.getNextAction().getId()).add(t);
				
				if (!this.transitionsByPrevAction.containsKey(t.getPreviousAction().getId()))
				{
					this.transitionsByPrevAction.put(t.getPreviousAction().getId(), new ArrayList<BusinessActionTransition>());
				}
				transitionsByPrevAction.get(t.getPreviousAction().getId()).add(t);
			}
			
			// find entry points - action invocations, that have no incoming transition
			for (BusinessActionInvocation inv : process.getInvocations())
			{	
				if (!transitionsByNextAction.containsKey(inv.getId()))
				{
					startingPoints.add(inv);
					
					if (inv.getInvokedAction() != null && Boolean.TRUE.equals(inv.getInvokedAction().getIsEntryPoint()))
					{
						if (this.entryPoint == null)
						{
							this.entryPoint = inv;
						}
						else
						{
							throw new BusinessProcessDeclarationException("Process contains more than one entry point");
						}
					}
				}
			}
		}
		else
		{
			// this is a border case in which the process contains no transitions
			
			for (BusinessActionInvocation inv : process.getInvocations())
			{
				invocationsById.put(inv.getId(), inv);
				
				startingPoints.add(inv);
					
				if (inv.getInvokedAction() != null && Boolean.TRUE.equals(inv.getInvokedAction().getIsEntryPoint()))
				{
					if (this.entryPoint == null)
					{
						this.entryPoint = inv;
					}
					else
					{
						throw new BusinessProcessDeclarationException("Process contains more than one entry point");
					}
				}
			}
		}
		
		if (this.entryPoint == null)
		{
			throw new BusinessProcessDeclarationException("Process contains no entry point");
		}
		
		this.isPrepared = true;
		this.process = process;
		this.processLastModifiedTimestamp = this.process.getLastModifiedDate() != null ? this.process.getLastModifiedDate().getTime() : null;
	}
	
	class MissingInvocations
	{
		private Map<String, Set<KID>> invocationsByParamName = new HashMap<String, Set<KID>>();

		public Map<String, Set<KID>> getInvocationsByParamName()
		{
			return invocationsByParamName;
		}

		public void addMissingInvocation(String targetParam, KID invocationId)
		{
			if (!this.invocationsByParamName.containsKey(targetParam))
			{
				this.invocationsByParamName.put(targetParam, new HashSet<KID>());
			}
			this.invocationsByParamName.get(targetParam).add(invocationId);
		}
	}
	
	class IfActionResult
	{
		private MissingInvocations missingInvocations;
		private List<BusinessActionInvocation> winningInvocations;
		
		public MissingInvocations getMissingInvocation()
		{
			return missingInvocations;
		}
		
		public void setMissingInvocations(MissingInvocations missingInvocations)
		{
			this.missingInvocations = missingInvocations;
		}

		public void addMissingInvocation(String param, BusinessActionInvocation missingInvocation)
		{
			if (this.missingInvocations == null)
			{
				this.missingInvocations = new MissingInvocations();
			}
			this.missingInvocations.addMissingInvocation(param, missingInvocation.getId());
		}
		
		public List<BusinessActionInvocation> getWinningInvocations()
		{
			return winningInvocations;
		}
		public void setWinningInvocation(List<BusinessActionInvocation> winningInvocation)
		{
			this.winningInvocations = winningInvocation;
		}
	}
	
	public BusinessActionInvocation getEntryPoint()
	{
		return this.entryPoint;
	}
}