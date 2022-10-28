/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.businessprocess;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import kommet.basic.RecordAccessType;
import kommet.basic.RecordProxy;
import kommet.businessprocess.annotations.Execute;
import kommet.businessprocess.annotations.Input;
import kommet.businessprocess.annotations.Output;
import kommet.config.Constants;
import kommet.dao.BusinessActionDao;
import kommet.dao.BusinessActionInvocationAttributeDao;
import kommet.dao.BusinessActionInvocationDao;
import kommet.dao.BusinessActionTransitionDao;
import kommet.dao.BusinessProcessDao;
import kommet.dao.BusinessProcessInputDao;
import kommet.dao.BusinessProcessOutputDao;
import kommet.dao.BusinessProcessParamAssignmentDao;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.koll.ClassService;
import kommet.koll.compiler.KommetCompiler;
import kommet.rel.RELParser;
import kommet.utils.MiscUtils;
import kommet.utils.ValidationUtil;

@Service
public class BusinessProcessService
{
	@Inject
	BusinessProcessDao bpDao;
	
	@Inject
	BusinessActionDao bpaDao;
	
	@Inject
	BusinessProcessInputDao bpiDao;
	
	@Inject
	BusinessProcessOutputDao bpoDao;
	
	@Inject
	BusinessActionInvocationDao invocationDao;
	
	@Inject
	BusinessActionTransitionDao transitionDao;
	
	@Inject
	BusinessProcessParamAssignmentDao assignmentDao;
	
	@Inject
	BusinessActionInvocationAttributeDao invAttrDao;
	
	@Inject
	KommetCompiler compiler;
	
	private static Set<String> allowedParamTypes;
	
	private static final Pattern INVOCATION_NAME_PATTERN = Pattern.compile("(\\{([^\\}\\{]+)\\}(\\.[a-z][a-zA-Z0-9]*)*)");
	
	/**
	 * Saves the whole business process, together with all its action invocations, transitions, inputs, outputs and parameter assignments.
	 * 
	 * Note that each time the process is saved, all its subitems are erased and saved anew, because the logic of checking which have changed and updating only the
	 * changed components would be too complicated.
	 * 
	 * @param process
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional
	public BusinessProcessSaveResult save (BusinessProcess process, ClassService classService, DataService dataService, AuthData authData, EnvData env) throws KommetException
	{
		BusinessProcessSaveResult saveResult = new BusinessProcessSaveResult();
		saveResult.setProcess(process);
		
		// before standardization is possible, we need to make sure process inputs and outputs have unique names
		validateUniqueInputAndOutputNames(process, saveResult);
		
		if (!saveResult.isValid())
		{
			return saveResult;
		}
		
		standardizeProcess(process, saveResult);
		
		// if process standardization failed, we cannot continue with validation
		if (!saveResult.isValid())
		{
			return saveResult;
		}
		
		validateProcess(process, saveResult, env);
		
		// if process validation failed, we don't want to save it, so we just return the information about the failure
		if (!saveResult.isValid())
		{
			return saveResult;
		}
		
		Map<KID, BusinessActionInvocation> oldInvocationsById = new HashMap<KID, BusinessActionInvocation>();
		BusinessProcess existingProcess = null;
		
		if (process.getId() != null)
		{	
			// get the existing process from database so that we can compare it with the saved one
			existingProcess = getBusinessProcess(process.getId(), authData, env);
			
			if (existingProcess.getInvocations() != null)
			{
				oldInvocationsById = MiscUtils.mapById(existingProcess.getInvocations());
			}
			
			validateUsedCallable(process, existingProcess, "process", saveResult, env);
			if (!saveResult.isValid())
			{
				return saveResult;
			}
			
			// delete process items that were present in the old process, but have been removed from the new version that is being saved right now
			deleteRemovedProcessItems(process, existingProcess, classService, dataService, authData, env);
		}
		
		// save the business process
		process = bpDao.save(process, authData, env);
		
		List<String> orderedInvocationIds = new ArrayList<String>();
		
		Map<String, BusinessActionInvocation> savedInvocationsByName = new HashMap<String, BusinessActionInvocation>();
		
		// save each invocation
		if (process.getInvocations() != null)
		{
			for (BusinessActionInvocation inv : process.getInvocations())
			{
				boolean isNewInvocation = inv.getId() == null;
				
				inv.setParentProcess(process);
				inv = invocationDao.save(inv, authData, env);
				orderedInvocationIds.add(inv.getId().getId());
				
				savedInvocationsByName.put(inv.getName(), inv);
		
				// compile a condition evaluator for the if condition, if the process is not a draft and if the condition hasn't changed
				if (inv.getInvokedAction() != null && inv.getInvokedAction().getType().equals("If") && Boolean.FALSE.equals(process.getIsDraft()))
				{
					String oldCondition = !isNewInvocation ? oldInvocationsById.get(inv.getId()).getSingleAttributeValue("condition") : null;
					String newCondition = inv.getSingleAttributeValue("condition");
					
					if (StringUtils.hasText(newCondition))
					{
						// generate the evaluator anew if condition has changed, or if it has not changed but the process was previously a draft, because in this
						// case the evaluator was never generated
						if (!newCondition.equals(oldCondition) || (existingProcess != null && Boolean.TRUE.equals(existingProcess.getIsDraft())))
						{
							// map of IDs of invocations used by this if-condition
							Set<String> requiredInvocations = new HashSet<String>();
							
							// create an expression evaluator for this condition
							kommet.basic.Class conditionEvaluator = getConditionEvaluator(inv, process, requiredInvocations, classService, dataService, saveResult, authData, env);
							
							if (!saveResult.isValid())
							{
								return saveResult;
							}
							
							inv.setAttribute("evaluatorClassId", conditionEvaluator.getId().getId());
							inv.setAttribute("requiredInvocationIds", MiscUtils.implode(requiredInvocations, ";"));
						}
					}
					else
					{
						// new condition is empty, but the process is not a draft, so this is an error
						throw new BusinessProcessDeclarationException("Empty condition on if-action {" + inv.getName() + "}");
					}
				}
				
				// for each invocation, save its attributes
				if (inv.getAttributes() != null && !inv.getAttributes().isEmpty())
				{
					for (BusinessActionInvocationAttribute attr : inv.getAttributes())
					{
						// update the invocation ID to the new one on the attribute, because the old invocation assigned to this attribute has been deleted
						// (it is deleted each time a process is saved)
						//attr.setInvocation(inv);
						//attr.setId(null);
						
						// save the attribute
						attr = invAttrDao.save(attr, authData, env);
					}
				}
			}
		}
		
		Map<KID, List<BusinessActionTransition>> transitionsByNextAction = new HashMap<KID, List<BusinessActionTransition>>();
		Map<KID, List<BusinessActionTransition>> transitionsByPrevAction = new HashMap<KID, List<BusinessActionTransition>>();
		
		// save each transition
		if (process.getTransitions() != null)
		{
			for (BusinessActionTransition t : process.getTransitions())
			{
				// set the ID to null - each time we save the process, we create its subitems anew
				t.uninitializeId();
				
				t.setBusinessProcess(process);
				
				t.setNextAction(savedInvocationsByName.get(t.getNextAction().getName()));
				t.setPreviousAction(savedInvocationsByName.get(t.getPreviousAction().getName()));
				
				if (!transitionsByNextAction.containsKey(t.getNextAction().getId()))
				{
					transitionsByNextAction.put(t.getNextAction().getId(), new ArrayList<BusinessActionTransition>());
				}
				transitionsByNextAction.get(t.getNextAction().getId()).add(t);
				
				if (!transitionsByPrevAction.containsKey(t.getPreviousAction().getId()))
				{
					transitionsByPrevAction.put(t.getPreviousAction().getId(), new ArrayList<BusinessActionTransition>());
				}
				transitionsByPrevAction.get(t.getPreviousAction().getId()).add(t);
				
				t.setId(transitionDao.save(t, authData, env).getId());
			}
		}
		
		// once transitions are saved, verify more features of invocations
		if (process.getInvocations() != null)
		{
			if (Boolean.FALSE.equals(process.getIsDraft()))
			{
				// path identifiers can only be set when transitions are saved, because their ids are used to create the identifiers 
				initTranversedNodes(process, transitionsByNextAction, transitionsByPrevAction);
				
				// only after all invocations have been saved can we verify input param assignments, because in order to verify this we will use invocation IDs
				// (though we could also use invocation names, and then the save would not be necessary)
				// make sure for each input parameter of this invocation there is a param assignment
				for (BusinessActionInvocation inv : process.getInvocations())
				{
					validateInputAssignments(inv, saveResult);
				}
			}
		}
		
		if (!saveResult.isValid())
		{
			return saveResult;
		}
		
		Map<String, BusinessProcessInput> savedProcessInputs = new HashMap<String, BusinessProcessInput>();
		
		// save each input
		if (process.getInputs() != null)
		{
			for (BusinessProcessInput input : process.getInputs())
			{	
				// temporarily assign a stub process with just the ID set
				// this is because the process contained a collection of null invocations which caused errors during proxy generated
				// TODO if time permits, this situation should in investigated
				BusinessProcess clonedProcess = new BusinessProcess();
				clonedProcess.setId(process.getId());
				
				input.setBusinessProcess(clonedProcess);
				
				BusinessProcessInput savedInput = bpiDao.save(input, authData, env);
				savedProcessInputs.put(input.getName(), savedInput);
				input.setId(savedInput.getId());
				
				// assign back the correct process object
				input.setBusinessProcess(process);
			}
		}
		
		Map<String, BusinessProcessOutput> savedProcessOutputs = new HashMap<String, BusinessProcessOutput>();
		
		// save each output
		if (process.getOutputs() != null)
		{
			for (BusinessProcessOutput output : process.getOutputs())
			{	
				// temporarily assign a stub process with just the ID set
				// this is because the process contained a collection of null invocations which caused errors during proxy generated
				// TODO if time permits, this situation should in investigated
				BusinessProcess clonedProcess = new BusinessProcess();
				clonedProcess.setId(process.getId());
				
				output.setBusinessProcess(clonedProcess);
				
				BusinessProcessOutput savedOutput = bpoDao.save(output, authData, env);
				savedProcessOutputs.put(output.getName(), savedOutput);
				output.setId(savedOutput.getId());
				
				// assign back the correct process object
				output.setBusinessProcess(process);
			}
		}
		
		// save each param assignment
		if (process.getParamAssignments() != null)
		{
			for (BusinessProcessParamAssignment a : process.getParamAssignments())
			{
				// set the ID to null - each time we save the process, we create its subitems anew
				a.uninitializeId();
				
				if (a.getTargetInvocation() != null)
				{
					a.setTargetInvocation(savedInvocationsByName.get(a.getTargetInvocation().getName()));
				}
				
				if (a.getSourceInvocation() != null)
				{
					a.setSourceInvocation(savedInvocationsByName.get(a.getSourceInvocation().getName()));
				}
				
				if (a.getProcessInput() != null)
				{
					a.setProcessInput(savedProcessInputs.get(a.getProcessInput().getName()));
				}
				
				if (a.getProcessOutput() != null)
				{
					a.setProcessOutput(savedProcessOutputs.get(a.getProcessOutput().getName()));
				}
				
				BusinessProcess clonedProcess = new BusinessProcess();
				clonedProcess.setId(process.getId());
				
				a.setBusinessProcess(clonedProcess);
				
				a.setId(assignmentDao.save(a, authData, env).getId());
				
				a.setBusinessProcess(process);
			}
		}
		
		// after invocations have been saved and we know their IDs, we can set the invocation order field
		if (process.getInvocations() != null)
		{
			process.setInvocationOrder(MiscUtils.implode(orderedInvocationIds, ";"));
			process = bpDao.save(process, authData, env);
		}
		
		// update business process definitions on env
		initTriggerableProcesses(env);
		
		// clear cached process executor
		env.removeProcessExecutor(process.getId());
		
		saveResult.setProcess(process);
		saveResult.setSuccess(true);
		return saveResult;
	}

	private void validateUsedCallable(ProcessBlock newCallable, ProcessBlock existingCallable, String callableType, BusinessProcessSaveResult saveResult, EnvData env) throws KommetException
	{	
		// find uses of this process in other processes
		BusinessActionInvocationFilter invocationFilter = new BusinessActionInvocationFilter();
		
		if (existingCallable instanceof BusinessProcess)
		{
			invocationFilter.addInvokedProcessId(existingCallable.getId());
		}
		else if (existingCallable instanceof BusinessAction)
		{
			invocationFilter.addInvokedActionId(existingCallable.getId());
		}
		else
		{
			throw new BusinessProcessException("Unsupported callable type " + existingCallable.getClass().getName());
		}
		
		if (invocationDao.get(invocationFilter, AuthData.getRootAuthData(env), env).isEmpty())
		{
			// callable is not embedded in other processes
			return;
		}
		
		if (MiscUtils.collectionSize(newCallable.getInputs()) != MiscUtils.collectionSize(existingCallable.getInputs()))
		{
			saveResult.addError("Number of inputs changed on used " + callableType + " from " + MiscUtils.collectionSize(existingCallable.getInputs()) + " to " + MiscUtils.collectionSize(newCallable.getInputs()));
		}
		else
		{	
			if (newCallable.getInputs() != null)
			{
				Map<String, BusinessProcessInput> oldInputsByName = new HashMap<String, BusinessProcessInput>();
				for (BusinessProcessInput input : existingCallable.getInputs())
				{
					oldInputsByName.put(input.getName(), input);
				}
						
				for (BusinessProcessInput input : newCallable.getInputs())
				{
					// in processes, existing outputs have IDs set, but in actions, they are not because they are defined anew from code each time an action is saved
					if (callableType.equals("process") && input.getId() == null)
					{
						saveResult.addError("New input " + input.getName() + " added to used " + callableType);
						continue;
					}
					else
					{
						// the param already exists, but we want to make sure its data type has not changed
						BusinessProcessInput oldInput = oldInputsByName.get(input.getName());
						
						if (oldInput == null)
						{
							// this situation should never happen for processes, so if it does, we throw an error
							if (callableType.equals("process"))
							{
								throw new BusinessProcessException("Input with name " + input.getName() + " present in new " + callableType + " and is saved, but was not found in the old version of the " + callableType);
							}
							else
							{
								saveResult.addError("New input " + input.getName() + " added to used " + callableType);
								continue;
							}
						}
						
						// if this is an action, the IDs on params will not be set because params are defined anew from a class each time an action is created
						// so we want to assign the IDs of the existing params to the new ones
						// we do this here, even though we don't know yet if the parameter data types match, because if they don't, the save will be terminated
						// and the assignment of IDs to the new params will have no effect
						input.setId(oldInput.getId());
						
						if (input.getDataTypeId() != null && input.getDataTypeName() != null)
						{
							throw new KommetException("Both data type ID and data type name set on input " + input.getName());
						}
						
						if ((oldInput.getDataTypeId() != null && input.getDataTypeId() != null && oldInput.getDataTypeId().equals(input.getDataTypeId())))
						{
							// this input has compatible data types
							continue;
						}
						
						if ((oldInput.getDataTypeName() != null && input.getDataTypeName() != null && oldInput.getDataTypeName().equals(input.getDataTypeName())))
						{
							// this input has compatible data types
							continue;
						}
						
						saveResult.addError("Data type changed on input " + input.getName() + " from " + oldInput.getDataTypeName() + " to " + input.getDataTypeName() + " in used " + callableType);
					}
				}
			}
		}
		
		if (MiscUtils.collectionSize(newCallable.getOutputs()) != MiscUtils.collectionSize(existingCallable.getOutputs()))
		{
			saveResult.addError("Number of outputs changed on used " + callableType + " from " + MiscUtils.collectionSize(existingCallable.getOutputs()) + " to " + MiscUtils.collectionSize(newCallable.getOutputs()));
		}
		else
		{	
			if (newCallable.getOutputs() != null)
			{
				Map<String, BusinessProcessOutput> oldOutputsByName = new HashMap<String, BusinessProcessOutput>();
				for (BusinessProcessOutput output : existingCallable.getOutputs())
				{
					oldOutputsByName.put(output.getName(), output);
				}
						
				for (BusinessProcessOutput output : newCallable.getOutputs())
				{
					// in processes, existing outputs have IDs set, but in actions, they are not because they are defined anew from code each time an action is saved
					if (callableType.equals("process") && output.getId() == null)
					{
						saveResult.addError("New output " + output.getName() + " added to used " + callableType);
						continue;
					}
					else
					{
						// the param already exists, but we want to make sure its data type has not changed
						BusinessProcessOutput oldOutput = oldOutputsByName.get(output.getName());
						
						if (oldOutput == null)
						{
							// this situation should never happen for processes, so if it does, we throw an error
							if (callableType.equals("process"))
							{
								throw new BusinessProcessException("Output with name " + output.getName() + " present in new " + callableType + " and is saved, but was not found in the old version of the " + callableType);
							}
							else
							{
								saveResult.addError("New output " + output.getName() + " added to used " + callableType);
								continue;
							}
						}
						
						// if this is an action, the IDs on params will not be set because params are defined anew from a class each time an action is created
						// so we want to assign the IDs of the existing params to the new ones
						// we do this here, even though we don't know yet if the parameter data types match, because if they don't, the save will be terminated
						// and the assignment of IDs to the new params will have no effect
						output.setId(oldOutput.getId());
						
						if (output.getDataTypeId() != null && output.getDataTypeName() != null)
						{
							throw new KommetException("Both data type ID and data type name set on output " + output.getName());
						}
						
						if ((oldOutput.getDataTypeId() != null && output.getDataTypeId() != null && oldOutput.getDataTypeId().equals(output.getDataTypeId())))
						{
							// this param has compatible data types
							continue;
						}
						
						if ((oldOutput.getDataTypeName() != null && output.getDataTypeName() != null && oldOutput.getDataTypeName().equals(output.getDataTypeName())))
						{
							// this input has compatible data types
							continue;
						}
						
						saveResult.addError("Data type changed on output " + output.getName() + " from " + oldOutput.getDataTypeName() + " to " + output.getDataTypeName() + " in used " + callableType);
					}
				}
			}
		}
	}

	private void deleteRemovedProcessItems(BusinessProcess process, BusinessProcess existingProcess, ClassService classService, DataService dataService, AuthData authData, EnvData env) throws KommetException
	{				
		if (existingProcess.getInvocations() != null && !existingProcess.getInvocations().isEmpty())
		{
			List<BusinessActionInvocation> invocationsToRemove = new ArrayList<BusinessActionInvocation>();
			Map<KID, BusinessActionInvocation> newInvocationsById = new HashMap<KID, BusinessActionInvocation>();
			
			if (process.getInvocations() != null)
			{
				newInvocationsById = MiscUtils.mapById(process.getInvocations());
			}
			
			for (BusinessActionInvocation existingInv : existingProcess.getInvocations())
			{
				if (!newInvocationsById.containsKey(existingInv.getId()))
				{
					invocationsToRemove.add(existingInv);
				}
				else
				{
					// the invocation should not be removed, but perhaps its attributes have been deleted and should be removed
					if (existingInv.getAttributes() != null && !existingInv.getAttributes().isEmpty())
					{
						BusinessActionInvocation newInv = newInvocationsById.get(existingInv.getId());
						if (newInv.getAttributes() == null || newInv.getAttributes().isEmpty())
						{
							// the new invocation has no attributes, so all attributes from the old invocation should be removed
							invAttrDao.delete(existingInv.getAttributes(), authData, env);
						}
						else
						{
							// find attributes that have been removed
							Map<KID, BusinessActionInvocationAttribute> newAttrsById = MiscUtils.mapById(newInv.getAttributes());
							List<BusinessActionInvocationAttribute> attributesToRemove = new ArrayList<BusinessActionInvocationAttribute>();
							for (BusinessActionInvocationAttribute attr : existingInv.getAttributes())
							{
								if (!newAttrsById.containsKey(attr.getId()))
								{
									attributesToRemove.add(attr);
								}
							}
							invAttrDao.delete(attributesToRemove, authData, env);
						}
					}
				}
			}
			
			deleteInvocations(invocationsToRemove, classService, dataService, authData, env);
		}
		
		if (existingProcess.getTransitions() != null)
		{
			deleteTransitions(existingProcess.getTransitions(), authData, env);
		}
		
		if (existingProcess.getParamAssignments() != null)
		{
			deleteParamAssignments(existingProcess.getParamAssignments(), authData, env);
		}
		
		if (existingProcess.getInputs() != null)
		{
			ArrayList<BusinessProcessInput> inputsToRemove = new ArrayList<BusinessProcessInput>();
			Map<KID, BusinessProcessInput> newInputsById = new HashMap<KID, BusinessProcessInput>();
			
			if (process.getInputs() != null)
			{
				newInputsById = MiscUtils.mapById(process.getInputs());
			}
			
			for (BusinessProcessInput input : existingProcess.getInputs())
			{
				if (!newInputsById.containsKey(input.getId()))
				{
					inputsToRemove.add(input);
				}
			}
			
			deleteInputs(inputsToRemove, authData, env);
		}
		
		if (existingProcess.getOutputs() != null)
		{
			ArrayList<BusinessProcessOutput> outputsToRemove = new ArrayList<BusinessProcessOutput>();
			Map<KID, BusinessProcessOutput> newOutputsById = new HashMap<KID, BusinessProcessOutput>();
			
			if (process.getOutputs() != null)
			{
				newOutputsById = MiscUtils.mapById(process.getOutputs());
			}
			
			for (BusinessProcessOutput input : existingProcess.getOutputs())
			{
				if (!newOutputsById.containsKey(input.getId()))
				{
					outputsToRemove.add(input);
				}
			}
			
			deleteOutputs(outputsToRemove, authData, env);
		}
	}

	private void validateUniqueInputAndOutputNames(BusinessProcess process, BusinessProcessSaveResult saveResult) throws BusinessProcessDeclarationException
	{
		if (process.getInputs() != null)
		{
			Set<String> inputNames = new HashSet<String>();
			
			for (BusinessProcessInput input : process.getInputs())
			{
				if (inputNames.contains(input.getName()))
				{
					saveResult.addError("Duplicate process input " + input.getName());
				}
				inputNames.add(input.getName());
			}
		}
		
		if (process.getOutputs() != null)
		{
			Set<String> outputNames = new HashSet<String>();
			
			for (BusinessProcessOutput output : process.getOutputs())
			{
				if (outputNames.contains(output.getName()))
				{
					saveResult.addError("Duplicate process output " + output.getName());
				}
				outputNames.add(output.getName());
			}
		}
		
	}

	/**
	 * Rewrites instances of invocations to other objects to make sure that all objects (such as transitions, param assignments) use the same invocation object instances
	 * which are fully initialized.
	 * @param process
	 * @param saveResult 
	 * @throws BusinessProcessException 
	 */
	private void standardizeProcess(BusinessProcess process, BusinessProcessSaveResult saveResult) throws BusinessProcessException
	{
		Map<KID, BusinessProcessInput> inputsById = new HashMap<KID, BusinessProcessInput>();
		Map<KID, BusinessProcessOutput> outputsById = new HashMap<KID, BusinessProcessOutput>();
		Map<String, BusinessProcessInput> processInputsByName = new HashMap<String, BusinessProcessInput>();
		Map<String, BusinessProcessOutput> processOutputsByName = new HashMap<String, BusinessProcessOutput>();
		
		// the sync jobs below are based on the assumption that invocation names are unique
		// so first we need to check if they really are
		Set<String> invNames = new HashSet<String>();
		if (process.getInvocations() != null)
		{
			for (BusinessActionInvocation inv : process.getInvocations())
			{
				if (invNames.contains(inv.getName()))
				{
					saveResult.addError("Invocation name " + inv.getName() + " is not unique across the process");
				}
				invNames.add(inv.getName());
				
				if (inv.getInputs() != null)
				{
					for (BusinessProcessInput input : inv.getInputs())
					{
						if (input.getId() == null)
						{
							throw new BusinessProcessException("ID not initialized on input {" + inv.getName() + "}." + input.getName());
						}
						
						inputsById.put(input.getId(), input);
					}
				}
				
				if (inv.getOutputs() != null)
				{
					for (BusinessProcessOutput output : inv.getOutputs())
					{
						if (output.getId() == null)
						{
							throw new BusinessProcessException("ID not initialized on output {" + inv.getName() + "}." + output.getName());
						}
						
						outputsById.put(output.getId(), output);
					}
				}
			}
		}
		
		if (process.getInputs() != null)
		{
			for (BusinessProcessInput input : process.getInputs())
			{
				processInputsByName.put(input.getName(), input);
			}
		}
		
		if (process.getOutputs() != null)
		{
			for (BusinessProcessOutput output : process.getOutputs())
			{
				processOutputsByName.put(output.getName(), output);
			}
		}
		
		// sync invocation instances across parameter assignments
		if (process.getParamAssignments() != null)
		{
			for (BusinessProcessParamAssignment a : process.getParamAssignments())
			{
				if (a.getSourceInvocation() != null)
				{
					a.setSourceInvocation(process.getInvocation(a.getSourceInvocation().getName()));
				}
				if (a.getTargetInvocation() != null)
				{
					a.setTargetInvocation(process.getInvocation(a.getTargetInvocation().getName()));
				}
				
				// if the process was a result of deserialization, the input and output definitions may not be complete
				// so we want to replace them with complete ones coming from the above invocation query
				if (a.getProcessInput() != null)
				{
					a.setProcessInput(processInputsByName.get(a.getProcessInput().getName()));
				}
				if (a.getProcessOutput() != null)
				{
					a.setProcessOutput(processOutputsByName.get(a.getProcessOutput().getName()));
				}
				if (a.getSourceParam() != null)
				{
					a.setSourceParam(outputsById.get(a.getSourceParam().getId()));
				}
				if (a.getTargetParam() != null)
				{
					a.setTargetParam(inputsById.get(a.getTargetParam().getId()));
				}
			}
		}
		
		// sync invocation instances across transitions
		if (process.getTransitions() != null)
		{
			for (BusinessActionTransition a : process.getTransitions())
			{
				if (a.getPreviousAction() != null)
				{
					a.setPreviousAction(process.getInvocation(a.getPreviousAction().getName()));
				}
				if (a.getNextAction() != null)
				{
					a.setNextAction(process.getInvocation(a.getNextAction().getName()));
				}
			}
		}
	}

	/**
	 * Reinitialize all processes on the env.
	 * @param env
	 * @throws KommetException
	 */
	@Transactional
	public void initTriggerableProcesses(EnvData env) throws KommetException
	{
		env.getTriggerableBusinessProcesses().clear();
		
		BusinessProcessFilter filter = new BusinessProcessFilter();
		filter.setInitSubprocesses(true);
		
		List<BusinessProcess> processes = get(filter, AuthData.getRootAuthData(env), env);
		
		for (BusinessProcess process : processes)
		{
			if (!Boolean.TRUE.equals(process.getIsTriggerable()) || !Boolean.TRUE.equals(process.getIsActive()))
			{
				continue;
			}
			
			BusinessProcessInput input = process.getSingleInput();
			
			String dataTypeLabel = input.getDataTypeId() != null ? input.getDataTypeId().getId() : input.getDataTypeName();
			
			if (!env.getTriggerableBusinessProcesses().containsKey(dataTypeLabel))
			{
				env.getTriggerableBusinessProcesses().put(dataTypeLabel, new ArrayList<BusinessProcess>());
			}
			env.getTriggerableBusinessProcesses().get(dataTypeLabel).add(process);
		}
	}

	/**
	 * Mark an identifier on each invocation telling on which conditional node it is. This identifier is a concatenation of transition IDs
	 * @param process
	 * @param transitionsByPrevAction 
	 * @param transitionsByNextAction 
	 */
	private void initTranversedNodes(BusinessProcess process, Map<KID, List<BusinessActionTransition>> transitionsByNextAction, Map<KID, List<BusinessActionTransition>> transitionsByPrevAction)
	{
		List<BusinessActionInvocation> startingPoints = new ArrayList<BusinessActionInvocation>();
		
		// find starting points - action invocations, that have no incoming transition
		for (BusinessActionInvocation inv : process.getInvocations())
		{	
			if (!transitionsByNextAction.containsKey(inv.getId()))
			{
				startingPoints.add(inv);
			}
		}
		
		// follow each branch
		for (BusinessActionInvocation inv : startingPoints)
		{
			List<BusinessActionTransition> transitions = transitionsByPrevAction.get(inv.getId());
			
			if (transitions == null)
			{
				continue;
			}
			
			for (BusinessActionTransition t : transitions)
			{
				initTranversedNodes(t.getNextAction(), inv, transitionsByNextAction, transitionsByPrevAction);
			}
		}
	}

	private void initTranversedNodes(BusinessActionInvocation inv, BusinessActionInvocation prevInv, Map<KID, List<BusinessActionTransition>> transitionsByNextAction, Map<KID, List<BusinessActionTransition>> transitionsByPrevAction)
	{
		inv.addTraversedNodes(prevInv.getTraversedNodes());
		
		List<BusinessActionTransition> transitions = transitionsByPrevAction.get(inv.getId());
		
		if (transitions == null)
		{
			return;
		}
		
		for (BusinessActionTransition t : transitions)
		{
			initTranversedNodes(t.getNextAction(), inv, transitionsByNextAction, transitionsByPrevAction);
		}
	}

	private void validateInputAssignments(BusinessActionInvocation inv, BusinessProcessSaveResult saveResult) throws BusinessProcessException
	{
		if (inv.getInputs() != null)
		{
			for (BusinessProcessInput input : inv.getInputs())
			{
				// collection of path identifiers for source invocations of param assignments
				Set<BusinessActionInvocation> sourceInvocations = new HashSet<BusinessActionInvocation>();
				Set<BusinessProcessInput> sourceInputs = new HashSet<BusinessProcessInput>();
				
				// look for assignments among parameters
				for (BusinessProcessParamAssignment a : inv.getParentProcess().getParamAssignments())
				{
					if (a.getTargetInvocation() != null && a.getTargetInvocation().getName().equals(inv.getName()) && a.getTargetParam() != null && a.getTargetParam().getName().equals(input.getName()))
					{
						// process input is not on any conditional path, so if there is an assignment from process input to this param, there can be no other assignment
						if (!sourceInputs.isEmpty())
						{
							saveResult.addError("More than one assignment of input parameter " + input.getName() + " of action " + inv.getName());
							continue;
						}
						
						// now we want to make sure that we only get one assignment to each input from each execution tree path
						if (a.getSourceInvocation() != null)
						{	
							// check if we already have some assignments to this input
							if (sourceInvocations.isEmpty())
							{
								sourceInvocations.add(a.getSourceInvocation());
							}
							else
							{
								// at this point we have an invocation from which parameter assignment will be made
								// and we want to make sure that if this invocation is executed, no other of the previous source invocations is executed
								// so we check the traversed nodes for each source invocation and make sure that this invocation is not on amoong these nodes
								for (BusinessActionInvocation previouslyAddedSourceInv : sourceInvocations)
								{
									if (previouslyAddedSourceInv.hasTraversedNode(a.getSourceInvocation().getId().getId()))
									{
										throw new BusinessProcessDeclarationException("Ambiguous parameter assignment for input " + input.getName() + " of action " + inv.getName());
									}
								}
								
								sourceInvocations.add(a.getSourceInvocation());
							}
						}
						else if (a.getProcessInput() != null)
						{
							sourceInputs.add(a.getProcessInput());
						}
					}
				}
				
				// if no assignments for this input have been found
				if (sourceInvocations.isEmpty() && sourceInputs.isEmpty())
				{
					// look for assignments among attributes
					if (!inv.isAttributeSet(input.getName()))
					{
						// there is no parameter assignment for this action's input
						saveResult.addError("No parameter assignment for input parameter " + input.getName() + " of action " + inv.getName());
						continue;
					}
				}
			}
		}
	}

	private void deleteOutputs(ArrayList<BusinessProcessOutput> outputs, AuthData authData, EnvData env) throws KommetException
	{
		bpoDao.delete(outputs, authData, env);
	}

	private void deleteInputs(ArrayList<BusinessProcessInput> inputs, AuthData authData, EnvData env) throws KommetException
	{
		bpiDao.delete(inputs, authData, env);
	}

	private void deleteParamAssignments(ArrayList<BusinessProcessParamAssignment> assignments, AuthData authData, EnvData env) throws KommetException
	{
		assignmentDao.delete(assignments, authData, env);
	}

	private void validateProcess (BusinessProcess process, BusinessProcessSaveResult saveResult, EnvData env) throws BusinessProcessException
	{
		if (process.getIsDraft() == null)
		{
			saveResult.addError("Business process is draft field is empty");
		}
		
		if (process.getIsActive() == null)
		{
			saveResult.addError("Business process is active field is empty");
		}
		
		if (Boolean.TRUE.equals(process.getIsActive()) && !Boolean.FALSE.equals(process.getIsDraft()))
		{
			saveResult.addError("Business processes in draft state cannot be set as active");
		}
		
		if (!StringUtils.hasText(process.getName()))
		{
			saveResult.addError("Process name is empty");
		}
		else if (!ValidationUtil.isValidOptionallyQualifiedResourceName(process.getName()))
		{
			saveResult.addError("Invalid process name " + process.getName());
		}
		
		if (!StringUtils.hasText(process.getLabel()))
		{
			saveResult.addError("Process label is empty");
		}
		
		// make sure invocation names are unique
		Set<String> invocationNames = new HashSet<String>();
		if (process.getInvocations() != null)
		{
			for (BusinessActionInvocation inv : process.getInvocations())
			{
				if (invocationNames.contains(inv.getName()))
				{
					saveResult.addError("Duplicate invocation name " + inv.getName());
				}
				
				invocationNames.add(inv.getName());
			}
		}
		
		if (!saveResult.isValid())
		{
			return;
		}
		
		if (Boolean.FALSE.equals(process.getIsDraft()))
		{
			validateNonDraftProcess(process, saveResult, env);
		}
	}

	private void validateIfAction(BusinessActionInvocation inv, BusinessProcessSaveResult saveResult) throws BusinessProcessDeclarationException
	{
		if (inv.getAttribute("condition") == null)
		{
			saveResult.addError("If action " + inv.getName() + " has no condition assigned");
		}
		if (!inv.isAttributeSet("ifTrueInvocationName") && !inv.isAttributeSet("ifFalseInvocationName"))
		{
			saveResult.addError("If action " + inv.getName() + " has to have at least one outgoing transition");
		}
	}
	
	private Field validateFieldValueAction(BusinessActionInvocation inv, BusinessProcess process, BusinessProcessSaveResult saveResult, Set<String> invocationsWithInitializedActualDataTypes, EnvData env) throws KommetException
	{
		Field returnedField = null;
		
		// find parameter assignments to this invocation
		for (BusinessProcessParamAssignment a : process.getParamAssignments())
		{
			if (a.getTargetInvocation() != null && a.getTargetInvocation().getName().equals(inv.getName()))
			{
				if (a.getTargetParam() != null && BusinessAction.FIELD_VALUE_ACTION_RECORD_INPUT.equals(a.getTargetParam().getName()))
				{	
					// if the invocation from which this assignment takes parameters does not have actual parameters set, set them first
					if (!invocationsWithInitializedActualDataTypes.contains(a.getSourceInvocation().getName()))
					{
						a.setSourceInvocation(initActualDataTypeOnInvocation(a.getSourceInvocation(), process, saveResult, invocationsWithInitializedActualDataTypes, env));
					}
					
					// if the source param is an output of another invocation
					if (a.getSourceParam() != null)
					{	
						Object sourceType = a.getSourceInvocation().getActualDataType(a.getSourceParam().getName(), env);
						
						if (sourceType instanceof Type)
						{
							try
							{
								// try to get the field and make sure no exception is thrown
								returnedField = getFieldValueActionSingleOutput(inv, (Type)sourceType);
							}
							catch (BusinessProcessDeclarationException e)
							{
								saveResult.addError(e.getMessage());
							}
						}
						else if (RecordProxy.class.getName().equals(sourceType))
						{
							// if the source param is a record proxy, we can only extract system properties from it
							String fieldName = inv.getSingleAttributeValue(BusinessAction.FIELD_VALUE_ACTION_FIELD_NAME_INPUT);
							if (!Field.isSystemField(fieldName))
							{
								saveResult.addError("Output of invocation " + a.getSourceInvocation().getName() + " is a record proxy, so field " + fieldName + " cannot be read from it");
							}
						}
						else
						{
							saveResult.addError("Invocation " + a.getSourceInvocation().getName() + " parameter '" + a.getSourceParam().getName() + "' has data type " + a.getSourceParam().getDataTypeName() + " which is an invalid input for FieldValue parameter " + BusinessAction.FIELD_VALUE_ACTION_FIELD_NAME_INPUT);
						}
					}
					// if the input of the assignment is a process input
					else if (a.getProcessInput() != null)
					{
						if (a.getProcessInput().getDataTypeId() != null)
						{
							try
							{
								// try to get the field and make sure no exception is thrown
								returnedField = getFieldValueActionSingleOutput(inv, env.getType(a.getProcessInput().getDataTypeId()));
							}
							catch (BusinessProcessDeclarationException e)
							{
								saveResult.addError(e.getMessage());
							}
						}
						else if (RecordProxy.class.getName().equals(a.getProcessInput().getDataTypeName()))
						{
							// if the source param is a record proxy, we can only extract system properties from it
							String fieldName = inv.getSingleAttributeValue(BusinessAction.FIELD_VALUE_ACTION_FIELD_NAME_INPUT);
							if (!Field.isSystemField(fieldName))
							{
								saveResult.addError("Process input " + a.getProcessInput().getName() + " is a record proxy, so field " + fieldName + " cannot be read from it");
							}
						}
						else
						{
							saveResult.addError("Invocation " + inv.getName() + " parameter '" + a.getSourceParam().getName() + "' has data type " + a.getSourceParam().getDataTypeName() + " which is an invalid input for FieldValue parameter " + BusinessAction.FIELD_VALUE_ACTION_FIELD_NAME_INPUT);
						}
					}
					else
					{
						throw new BusinessProcessDeclarationException("Invalid param assignment to action FieldValue. Assignment source is neither a process input nor another invocation's output");
					}
				}
				else if (a.getTargetParam() != null && BusinessAction.FIELD_VALUE_ACTION_FIELD_NAME_INPUT.equals(a.getTargetParam().getName()))
				{
					// do not validate this, because regular validation of all parameter assignments will take care of this
				}
				else
				{
					throw new BusinessProcessDeclarationException("Invalid param assignment to action FieldValue. Invalid target parameter " + a.getTargetParam().getName());
				}
			}
		}
		
		return returnedField;
	}

	/**
	 * If the action's output is a record proxy, but can be cast to a specific type basic on the actions properties (e.g. a query), this method
	 * returns the case type.
	 * This applies only to actions such as QueryUnique - those, which add parameter casting.
	 * @param sourceInvocation
	 * @param sourceParam
	 * @return
	 * @throws BusinessProcessException 
	 */
	/*private Type getActualOutputDataTypeId(BusinessActionInvocation inv, BusinessProcessOutput output, EnvData env) throws BusinessProcessException
	{
		if (inv.getInvokedAction().getType().equals("QueryUnique"))
		{
			String query = inv.getSingleAttributeValue("query");
			try
			{
				return env.getSelectCriteriaFromDAL(query).getType();
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				throw new BusinessProcessDeclarationException("Invalid query: " + query + ". " + e.getMessage());
			}
		}
		else
		{
			return null;
		}
	}*/

	private void validateNonDraftProcess(BusinessProcess process, BusinessProcessSaveResult saveResult, EnvData env) throws BusinessProcessException
	{
		// make sure there is exactly one staring action (one without any incoming transitions)
		Set<String> transitionsTargets = new HashSet<String>();
		
		if (process.getTransitions() != null)
		{
			for (BusinessActionTransition t : process.getTransitions())
			{
				transitionsTargets.add(t.getNextAction().getName());
			}
		}
		
		List<BusinessActionInvocation> startingActions = new ArrayList<BusinessActionInvocation>();
		Set<String> invocationsWithInitializedActualDataTypes = new HashSet<String>();
		
		for (BusinessActionInvocation inv : process.getInvocations())
		{
			if (!transitionsTargets.contains(inv.getName()))
			{
				startingActions.add(inv);
			}
			
			inv = initActualDataTypeOnInvocation(inv, process, saveResult, invocationsWithInitializedActualDataTypes, env);
		}
		
		if (startingActions.isEmpty())
		{
			saveResult.addError("Process contains no starting action");
			return;
		}
		
		for (BusinessActionInvocation startingAction : startingActions)
		{
			// make sure all input parameters of all action invocations has param assignments, either from other invocations or from process input parameters
			if (startingAction.getInputs() != null)
			{
				if (process.getInputs() == null | process.getInputs().isEmpty())
				{
					saveResult.addError("Starting action has input values, but the process does not");
					return;
				}
				
				for (BusinessProcessInput actionInput : startingAction.getInputs())
				{
					boolean assignmentFound = false;
					
					// for each input of the starting action, make sure there is an assignment of the input of the process
					for (BusinessProcessParamAssignment pa : process.getParamAssignments())
					{
						if (pa.getTargetInvocation() != null && pa.getTargetInvocation().getName().equals(startingAction.getName()) && pa.getProcessInput() != null)
						{
							// TODO make sure the input assignment is type-compatible
							assignmentFound = true;
							break;
						}
					}
					
					if (!assignmentFound)
					{
						// look for parameters in invocation attributes
						if (!startingAction.isAttributeSet(actionInput.getName()))
						{
							saveResult.addError("Input parameter " + actionInput.getName() + " of the starting action " + startingAction.getName() + " has no assigned input value from the process input, nor is the input defined in invocation attributes");
						}
					}
				}
			}
			
			// for each starting action, follow all parameter assignments from this action, deduce the actual
			// parameter types on these actions, and propagate the type casts to subsequent actions
			checkParamAssignmentsForInv(startingAction, process, saveResult, env);
		}
		
		// field value param assignments will be validated later, when we already have actual data types of their input invocations
		//List<BusinessProcessParamAssignment> assignmentsDependingOnFieldValueActions = new ArrayList<BusinessProcessParamAssignment>();
		
		// validate parameter assignments
		/*if (process.getParamAssignments() != null)
		{
			for (BusinessProcessParamAssignment a : process.getParamAssignments())
			{
				Object targetDataType = null;
				if (a.getTargetParam() != null)
				{
					targetDataType = a.getTargetParam().getDataTypeId() != null ? a.getTargetParam().getDataTypeId() : a.getTargetParam().getDataTypeName();
				}
				else if (a.getProcessOutput() != null)
				{
					targetDataType = a.getProcessOutput().getDataTypeId() != null ? a.getProcessOutput().getDataTypeId() : a.getProcessOutput().getDataTypeName();
				}
				else
				{
					saveResult.addError("Neither target param nor process output set on parameter assignment " + (a.getSourceInvocation() != null ? " from {" + a.getSourceInvocation().getName() + "}" : ""));
					continue;
				}
				
				String sourceInvocationType = null;
				String sourceActionName = null;
				if (a.getSourceInvocation() != null && a.getSourceInvocation().getInvokedAction() != null)
				{
					sourceInvocationType = a.getSourceInvocation().getInvokedAction().getType();
					sourceActionName = a.getSourceInvocation().getInvokedAction().getName();
				}
				
				if ("FieldValue".equals(sourceInvocationType))
				{
					// first we need to validate field value actions, and then actions that take their output as input, so that we know what is the actual
					// data type of the field returned by the FieldValue action
					assignmentsDependingOnFieldValueActions.add(a);
					continue;
				}
				
				if ("QueryUnique".equals(sourceActionName))
				{
					Object sourceDataType = null;
					
					// determine output type by queried type
					if (a.getSourceInvocation().isAttributeSet("query"))
					{
						try
						{
							Type type = env.getSelectCriteriaFromDAL(a.getSourceInvocation().getAttribute("query").get(0).getValue()).getType();
							sourceDataType = type.getKID();
							
							// remember the deduced actual type on the output so that subsequent actions know the actual data type
							//a.getSourceParam().setActualDataTypeName(type.getQualifiedName());
						}
						catch (KommetException e)
						{
							saveResult.addError("Could not get queried type from query: " + a.getSourceInvocation().getAttribute("query").get(0).getValue());
							continue;
						}
						
						if (!isParamCastValid(sourceDataType, targetDataType))
						{
							addInvalidParamAssignmentException(a, saveResult, env);
							continue;
						}
					}
					else
					{
						// we don't know what type will be returned by the query, because the query will be provided at runtime
						// but if the "assumeValidOutputType" attribute is set, we assume compatible types during process saving,
						// and we'll see if it is correct when the process runs
						String assumeValidTypeCast = a.getSourceInvocation().getSingleAttributeValue("assumeValidOutputType");
						
						// the query is provided as input parameter at runtime, so we don't know what type will be queried
						// so we can only assume it will be some RecordProxy
						if (!"true".equals(assumeValidTypeCast) && !RecordProxy.class.getName().equals(targetDataType))
						{
							saveResult.addError("Invalid parameter assignment from a query {" + a.getSourceInvocation().getName() + "} to " + targetDataType);
							continue;
						}
					}
				}
				else if ("TypeCast".equals(sourceInvocationType))
				{
					Object sourceDataType = null;
					
					if (a.getSourceInvocation().isAttributeSet("typeId"))
					{
						try
						{
							sourceDataType = env.getSelectCriteriaFromDAL(a.getSourceInvocation().getAttribute("typeId").get(0).getValue()).getType().getKID();
						}
						catch (KommetException e)
						{
							saveResult.addError("Could not get queried type from query: " + a.getSourceInvocation().getAttribute("query").get(0).getValue());
							continue;
						}
						
						if (!isParamCastValid(sourceDataType, targetDataType))
						{
							addInvalidParamAssignmentException(a, saveResult, env);
							continue;
						}
					}
					else
					{
						// the query is provided as input parameter at runtime, so we don't know what type will be queried
						// so we can only assume it will be some RecordProxy
						if (!RecordProxy.class.getName().equals(targetDataType))
						{
							saveResult.addError("Invalid parameter assignment from a query {" + a.getSourceInvocation().getName() + "} to " + targetDataType);
							continue;
						}
					}
				}
				// handle entry point action separately because their output data type is modified by the acceptedTypes attribute
				else if ("RecordSave".equals(sourceInvocationType) || "RecordCreate".equals(sourceInvocationType) || "RecordUpdate".equals(sourceInvocationType))
				{
					Object sourceDataType = null;
					
					if (a.getSourceInvocation().isAttributeSet("acceptedTypes"))
					{
						BusinessActionInvocationAttribute attr = a.getSourceInvocation().getAttribute("acceptedTypes").get(0);
						
						List<String> sTypeIds = MiscUtils.splitAndTrim(attr.getValue(), ",");
						
						if (sTypeIds.size() == 1)
						{
							try
							{
								sourceDataType = KID.get(sTypeIds.get(0));
							}
							catch (KIDException e)
							{
								throw new BusinessProcessException("Invalid type ID " + sTypeIds.get(0));
							}
							
							if (!isParamCastValid(sourceDataType, targetDataType))
							{
								addInvalidParamAssignmentException(a, saveResult, env);
								continue;
							}
						}
						else
						{
							// there are more than one possible types of the output param, so the target param must be an record proxy
							if (!RecordProxy.class.getName().equals(targetDataType))
							{
								saveResult.addError("Invalid parameter assignment from ambiguous type {" + a.getSourceInvocation().getInvokedAction().getName() + "} to " + targetDataType);
								continue;
							}
						}
					}
					else
					{
						sourceDataType = RecordProxy.class.getName();
						
						if (!isParamCastValid(sourceDataType, targetDataType))
						{
							addInvalidParamAssignmentException(a, saveResult, env);
							continue;
						} 
					}
				}
				else
				{
					// this is a regular parameter assignment
					Object sourceDataType = null;
					
					if (a.getSourceParam() != null)
					{
						sourceDataType = a.getSourceParam().getDataTypeId() != null ? a.getSourceParam().getDataTypeId() : a.getSourceParam().getDataTypeName();
					}
					else if (a.getProcessInput() != null)
					{
						sourceDataType = a.getProcessInput().getDataTypeId() != null ? a.getProcessInput().getDataTypeId() : a.getProcessInput().getDataTypeName();
					}
					else
					{
						saveResult.addError("Neither source param not process input set on invocation");
						continue;
					}
					
					if (a.getTargetParam() != null)
					{
						targetDataType = a.getTargetParam().getDataTypeId() != null ? a.getTargetParam().getDataTypeId() : a.getTargetParam().getDataTypeName();
					}
					else if (a.getProcessOutput() != null)
					{
						targetDataType = a.getProcessOutput().getDataTypeId() != null ? a.getProcessOutput().getDataTypeId() : a.getProcessOutput().getDataTypeName();
					}
					else
					{
						saveResult.addError("Neither target param nor process output set on invocation");
						continue;
					}
					
					if (!isParamCastValid(sourceDataType, targetDataType))
					{
						addInvalidParamAssignmentException(a, saveResult, env);
					}
				}
			}
			
			// now validate assignments from field value actions
			for (BusinessProcessParamAssignment a : assignmentsDependingOnFieldValueActions)
			{
				// this is a regular parameter assignment
				Object sourceDataType = a.getSourceInvocation().getActualDataTypeName(a.getSourceParam().getName(), env);
				
				Object targetDataType = null;
				
				if (a.getTargetParam() != null)
				{
					targetDataType = a.getTargetParam().getDataTypeId() != null ? a.getTargetParam().getDataTypeId() : a.getTargetParam().getDataTypeName();
				}
				else if (a.getProcessOutput() != null)
				{
					targetDataType = a.getProcessOutput().getDataTypeId() != null ? a.getProcessOutput().getDataTypeId() : a.getProcessOutput().getDataTypeName();
				}
				else
				{
					saveResult.addError("Neither target param nor process output set on invocation");
					continue;
				}
				
				if (!isParamCastValid(sourceDataType, targetDataType))
				{
					addInvalidParamAssignmentException(a, saveResult, env);
				}
			}
		}*/
		
		validateTriggerableProcess(process, startingActions, saveResult, env);
	}

	private void checkParamAssignmentsForInv(BusinessActionInvocation inv, BusinessProcess process, BusinessProcessSaveResult saveResult, EnvData env) throws BusinessProcessException
	{	
		// first all param assignments that start from this action
		if (process.getParamAssignments() == null)
		{
			return;
		}
		
		for (BusinessProcessParamAssignment a : process.getParamAssignments())
		{
			// follow all assignments from this action
			if (a.getSourceInvocation() != null && a.getSourceInvocation().getName().equals(inv.getName()))
			{
				// this is a regular parameter assignment
				Object sourceDataType = null;
				
				if (a.getSourceParam() != null)
				{
					sourceDataType = a.getSourceInvocation().getActualDataType(a.getSourceParam().getName(), env);
					
					if (sourceDataType == null)
					{
						throw new BusinessProcessException("Actual data type not set on output {" + a.getSourceInvocation().getName() + "}." + a.getTargetParam().getName());
					}
					//sourceDataType = a.getSourceParam().getDataTypeId() != null ? a.getSourceParam().getDataTypeId() : a.getSourceParam().getDataTypeName();
				}
				else if (a.getProcessInput() != null)
				{
					sourceDataType = a.getProcessInput().getDataTypeId() != null ? a.getProcessInput().getDataTypeId() : a.getProcessInput().getDataTypeName();
				}
				else
				{
					saveResult.addError("Neither source param not process input set on invocation");
					continue;
				}
				
				Object targetDataType = null;
				
				if (a.getTargetParam() != null)
				{
					targetDataType = a.getTargetParam().getDataTypeId() != null ? a.getTargetParam().getDataTypeId() : a.getTargetParam().getDataTypeName();
				}
				else if (a.getProcessOutput() != null)
				{
					targetDataType = a.getProcessOutput().getDataTypeId() != null ? a.getProcessOutput().getDataTypeId() : a.getProcessOutput().getDataTypeName();
				}
				else
				{
					saveResult.addError("Neither target param nor process output set on invocation");
					continue;
				}
				
				if (!isParamCastValid(sourceDataType, targetDataType, env))
				{
					boolean isValidCast = false;
					
					if (a.getSourceInvocation().getInvokedAction() != null && a.getSourceInvocation().getInvokedAction().getName().equals("QueryUnique"))
					{
						// we don't know what type will be returned by the query, because the query will be provided at runtime
						// but if the "assumeValidOutputType" attribute is set, we assume compatible types during process saving,
						// and we'll see if it is correct when the process runs
						String assumeValidTypeCast = a.getSourceInvocation().getSingleAttributeValue("assumeValidOutputType");
						
						// the query is provided as input parameter at runtime, so we don't know what type will be queried
						// so we can only assume it will be some RecordProxy
						if ("true".equals(assumeValidTypeCast) || targetDataType.equals(RecordProxy.class.getName()))
						{
							isValidCast = true;
						}
					}
					
					if (!isValidCast)
					{
						addInvalidParamAssignmentException(a, saveResult, env);
					}
				}
				
				if (a.getTargetInvocation() != null)
				{
					checkParamAssignmentsForInv(a.getTargetInvocation(), process, saveResult, env);
				}
			}
		}
	}

	/**
	 * If this invocation represents an action whose output parameters can be cast to other types due to specific action configuration,
	 * this method will deduce the actual data type on the invocation's output parameters.
	 * @param inv
	 * @param process
	 * @param saveResult
	 * @param invocationsWithInitializedActualDataTypes
	 * @param env
	 * @return
	 * @throws BusinessProcessException 
	 * @throws KommetException 
	 */
	private BusinessActionInvocation initActualDataTypeOnInvocation(BusinessActionInvocation inv, BusinessProcess process, BusinessProcessSaveResult saveResult, Set<String> invocationsWithInitializedActualDataTypes, EnvData env) throws BusinessProcessException
	{
		if (invocationsWithInitializedActualDataTypes.contains(inv.getName()))
		{
			return inv;
		}
		
		if (inv.getInvokedAction() != null)
		{
			if (inv.getInvokedAction().getType().equals("If"))
			{
				validateIfAction(inv, saveResult);
			}
			else if (inv.getInvokedAction().getType().equals("FieldValue"))
			{
				Field returnedField = null;
				
				try
				{
					returnedField = validateFieldValueAction(inv, process, saveResult, invocationsWithInitializedActualDataTypes, env);
				}
				catch (KommetException e)
				{
					e.printStackTrace();
					throw new BusinessProcessDeclarationException(e.getMessage());
				}
				
				if (returnedField == null)
				{
					saveResult.addError("Cannot obtain returned value from FieldValue invocation " + inv.getName() + ". Probably no record is assigned as input to the invocation");
					return inv;
				}
				
				if (returnedField.getDataTypeId().equals(DataType.TYPE_REFERENCE))
				{
					KID typeId = ((TypeReference)returnedField.getDataType()).getTypeId();
					
					try
					{
						// remember the deduced actual type on the output so that subsequent actions know the actual data type
						inv.setActualDataType(BusinessAction.FIELD_VALUE_ACTION_OUTPUT, env.getType(typeId));
					}
					catch (KommetException e)
					{
						e.printStackTrace();
						throw new BusinessProcessException("Error getting type with ID " + typeId + ". Nested: " + e.getMessage());
					}
				}
				else
				{
					// remember the deduced actual type on the output so that subsequent actions know the actual data type
					inv.setActualDataType(BusinessAction.FIELD_VALUE_ACTION_OUTPUT, returnedField.getDataType().getJavaType());
				}
			}
			else if ("QueryUnique".equals(inv.getInvokedAction().getName()) && "Action".equals(inv.getInvokedAction().getType()))
			{	
				// determine output type by queried type
				if (inv.isAttributeSet("query"))
				{
					try
					{
						Type type = env.getSelectCriteriaFromDAL(inv.getSingleAttributeValue("query")).getType();
						
						// remember the deduced actual type on the output so that subsequent actions know the actual data type
						inv.setActualDataType("record", type);
					}
					catch (KommetException e)
					{
						saveResult.addError("Could not get queried type for invocation {" + inv.getName() + "}");
						return inv;
					}
				}
				else
				{
					inv.setActualDataType("record", RecordProxy.class.getName());
				}
			}
			else if ("RecordSave".equals(inv.getInvokedAction().getType()) || "RecordCreate".equals(inv.getInvokedAction().getType()) || "RecordUpdate".equals(inv.getInvokedAction().getType()))
			{
				if (inv.isAttributeSet("acceptedTypes"))
				{
					BusinessActionInvocationAttribute attr = inv.getSingleAttribute("acceptedTypes");
					
					List<String> sTypeIds = MiscUtils.splitAndTrim(attr.getValue(), ",");
					
					if (sTypeIds.size() == 1)
					{
						try
						{
							KID sourceDataType = KID.get(sTypeIds.get(0));
							
							// add interpreted actual data type so that subsequent action's know the exact cast data type
							inv.setActualDataType("record", env.getType((KID)sourceDataType));
						}
						catch (KIDException e)
						{
							throw new BusinessProcessException("Invalid type ID " + sTypeIds.get(0));
						}
						catch (KommetException e) 
						{
							e.printStackTrace();
							throw new BusinessProcessException(e.getMessage());
						}
					}
					else
					{
						inv.setActualDataType("record", RecordProxy.class.getName());
					}
				}
				else
				{
					inv.setActualDataType("record", RecordProxy.class.getName());
				}
			}
		}
		
		invocationsWithInitializedActualDataTypes.add(inv.getName());
		
		return inv;
	}

	public static boolean isValidTriggerableEntryPoint(BusinessAction action)
	{
		return "RecordSave".equals(action.getType()) || "RecordCreate".equals(action.getType()) || "RecordUpdate".equals(action.getType());
	}

	private void addInvalidParamAssignmentException(BusinessProcessParamAssignment a, BusinessProcessSaveResult saveResult, EnvData env) throws BusinessProcessException
	{
		String targetDataType = null;
		String targetParamName = null;
		
		if (a.getTargetParam() != null)
		{
			targetParamName = "{" + a.getTargetInvocation().getName() + "}." + a.getTargetParam().getName();
			try
			{
				targetDataType = a.getTargetParam().getDataTypeId() != null ? env.getType(a.getTargetParam().getDataTypeId()).getQualifiedName() : a.getTargetParam().getDataTypeName();
			}
			catch (KommetException e)
			{
				saveResult.addError("Could not get data type for ID " + a.getTargetParam().getDataTypeId());
				return;
			}
		}
		else if (a.getProcessOutput() != null)
		{
			targetParamName = " process output " + a.getProcessOutput().getName();
			try
			{
				targetDataType = a.getProcessOutput().getDataTypeId() != null ? env.getType(a.getProcessOutput().getDataTypeId()).getQualifiedName() : a.getProcessOutput().getDataTypeName();
			}
			catch (KommetException e)
			{
				throw new BusinessProcessException("Could not get data type for ID " + a.getProcessOutput().getDataTypeId());
			}
		}
		else
		{
			saveResult.addError("Neither target param nor process output set on invocation");
			return;
		}
		
		String sourceDataType = null;
		String sourceParamName = null;
		
		if (a.getSourceParam() != null)
		{
			sourceParamName = "{" + a.getSourceInvocation().getName() + "}." + a.getSourceParam().getName();
			try
			{
				sourceDataType = a.getSourceParam().getDataTypeId() != null ? env.getType(a.getSourceParam().getDataTypeId()).getQualifiedName() : a.getSourceParam().getDataTypeName();
			}
			catch (KommetException e)
			{
				throw new BusinessProcessException("Could not get data type for ID " + a.getSourceParam().getDataTypeId());
			}
		}
		else if (a.getProcessInput() != null)
		{
			sourceParamName = " process input " + a.getProcessInput().getName();
			try
			{
				sourceDataType = a.getProcessInput().getDataTypeId() != null ? env.getType(a.getProcessInput().getDataTypeId()).getQualifiedName() : a.getProcessInput().getDataTypeName();
			}
			catch (KommetException e)
			{
				throw new BusinessProcessException("Could not get data type for ID " + a.getProcessInput().getDataTypeId());
			}
		}
		else
		{
			saveResult.addError("Neither target param nor process output set on invocation");
			return;
		}
		
		saveResult.addError("Invalid parameter assignment from " + sourceParamName + " (" + sourceDataType + ") to " + targetParamName + " (" + targetDataType + ")");
	}

	private boolean isParamCastValid(Object sourceDataType, Object targetDataType, EnvData env) throws BusinessProcessException
	{	
		if (sourceDataType instanceof KID)
		{
			try
			{
				sourceDataType = env.getType((KID)sourceDataType);
			}
			catch (KommetException e)
			{
				throw new BusinessProcessException("Error getting type with KID " + sourceDataType);
			}
		}
		
		if (targetDataType instanceof KID)
		{
			try
			{
				targetDataType = env.getType((KID)targetDataType);
			}
			catch (KommetException e)
			{
				throw new BusinessProcessException("Error getting type with KID " + targetDataType);
			}
		}
		
		if (sourceDataType.equals(targetDataType) || (sourceDataType instanceof Type && targetDataType instanceof Type && ((Type)sourceDataType).getKID().equals(((Type)targetDataType).getKID())))
		{
			return true;
		}
		
		if (sourceDataType instanceof Type && !(targetDataType instanceof Type) && RecordProxy.class.getName().equals(targetDataType))
		{
			return true;
		}
		
		return false;
	}

	private void validateTriggerableProcess(BusinessProcess process, List<BusinessActionInvocation> startingActions, BusinessProcessSaveResult saveResult, EnvData env) throws BusinessProcessDeclarationException
	{
		if (!Boolean.TRUE.equals(process.getIsTriggerable()))
		{
			return;
		}
		
		int entryPointCount = 0;
		for (BusinessActionInvocation startingAction : startingActions)
		{
			if (isValidTriggerableEntryPoint(startingAction.getInvokedAction()))
			{
				entryPointCount++;
			}
		}
		
		if (entryPointCount > 1)
		{
			saveResult.addError("Triggerable process must have exactly one entry point, but has " + startingActions.size());
		}
		else if (entryPointCount == 0)
		{
			saveResult.addError("Triggerable process has no valid entry point");
		}
		
		// get input values for this triggerable process - there should be exactly one input value - the record that was created
		if (process.getInputs() == null || process.getInputs().isEmpty())
		{
			saveResult.addError("Triggerable process " + process.getName() + " has no input values");
		}
		else if (process.getInputs().size() != 1)
		{
			saveResult.addError("Triggerable process " + process.getName() + " should have exactly one input value, but has " + process.getInputs().size());
		}
		else
		{
			if (!RecordProxy.class.getName().equals(process.getInputs().get(0).getDataTypeName()))
			{
				try
				{
					saveResult.addError("Triggerable process " + process.getName() + " has input value of invalid type " + process.getInputs().get(0).getDataTypeName() != null ? process.getInputs().get(0).getDataTypeName() : env.getType(process.getInputs().get(0).getDataTypeId()).getQualifiedName());
				}
				catch (KommetException e)
				{
					throw new BusinessProcessDeclarationException("Error " + e.getMessage());
				}
			}
		}
	}

	private kommet.basic.Class getConditionEvaluator(BusinessActionInvocation ifConditionInvocation, BusinessProcess process, Set<String> requiredInvocations, ClassService classService, DataService dataService, BusinessProcessSaveResult saveResult, AuthData authData, EnvData env) throws KommetException
	{
		String relCondition = ifConditionInvocation.getAttribute("condition").get(0).getValue();
		
		Matcher m = INVOCATION_NAME_PATTERN.matcher(relCondition);
		
		String actionValuesJavaVar = "actionValues";
		
		// This map contains aliases of java expressions mapped to these expressions.
		// When the condition is translated, action property references such as {Action name}.user.id are translated to Java expressions such as
		// invocations.get("Action name").getField("user").getField("id").
		// However, if we want to use the RELParse to substitute operators in such expression, the parser does not work good with such complicated Java expressions.
		// This is why before applying the parser, we change all expressions to aliases (e.g. "expr9sKSSDlss"), then apply the parser, and then replace the aliases with
		// correct Java expressions.
		Map<String, String> aliasToJavaExpression = new HashMap<String, String>();
		
		StringBuffer sb = new StringBuffer();
		while (m.find())
		{
			List<String> groupParts = MiscUtils.splitAndTrim(m.group(1), "\\.");
			String invocationName = groupParts.get(0).substring(1, groupParts.get(0).length() - 1);
			BusinessActionInvocation invocation = process.getInvocation(invocationName);
			
			if (groupParts.size() < 2)
			{
				throw new BusinessProcessDeclarationException("No properties found for invocation name " + invocationName + " in if-condition");
			}
			
			BusinessAction action = invocation.getInvokedAction();
			
			String javaExpr = null;
			
			if (groupParts.size() == 2)
			{
				// there is just one property name after the invocation name, so it must be an output of the invocation which is not a record
				BusinessProcessOutput output = action.getOutput(groupParts.get(1));
				
				// generate some key for the injected value
				String varKey = "invocation_" + invocation.getId() + "_" + output.getName();
				
				requiredInvocations.add(invocation.getId() + "," + output.getName());
				
				if (output.getDataTypeId() != null || output.getDataTypeName().equals(RecordProxy.class.getName()))
				{
					throw new BusinessProcessDeclarationException("Illegal condition " + relCondition + ": only property of action " + invocationName + " is a record");
				}
				else
				{
					// add a cast to the inferred type
					javaExpr = "((" + output.getDataTypeName() + ")" + actionValuesJavaVar + ".get(\"" + varKey + "\"))";
				}
			}
			else
			{
				// there are more than one property after the invocation name, so they have to be references to a record
				// there is just one property name after the invocation name, so it must be an output of the invocation which is not a record
				BusinessProcessOutput output = action.getOutput(groupParts.get(1));
				
				// get the actual output from the invocation - the data type of the actual output may be narrowed down compared to the action's output
				// due to attributes on the invocation that allow to deduce the data type (e.g. the "acceptedTypes" attribute on the RecordSave action)
				output = invocation.getActualOutput(output, env);
				
				// generate some key for the injected value
				String varKey = "invocation_" + invocation.getId() + "_" + output.getName();
				
				requiredInvocations.add(invocation.getId() + "," + output.getName());
				Type recordType = null;
				
				if (output.getDataTypeId() == null && !output.getDataTypeName().equals(RecordProxy.class.getName()))
				{
					throw new BusinessProcessDeclarationException("Illegal condition " + relCondition + ": the first property of action " + invocationName + " is not  oa record");
				}
				else
				{
					String typeName = null;
					if (output.getDataTypeId() != null)
					{
						recordType = env.getType(output.getDataTypeId());
						typeName = recordType.getQualifiedName();
					}
					else
					{
						typeName = output.getDataTypeName();
					}
					
					// add a cast to the inferred type
					javaExpr = "((" + typeName + ")" + actionValuesJavaVar + ".get(\"" + varKey + "\"))";
				}
				
				// remove first two elements
				groupParts.remove(0);
				groupParts.remove(0);
				
				Type currentType = recordType;
				
				// record type is precisely defined
				if (recordType != null)
				{
					// all subsequent properties must be translated into field getters
					for (int i = 0; i < groupParts.size(); i++)
					{
						String propName = groupParts.get(i);
						Field field = currentType.getField(propName);
						
						if (field == null)
						{
							saveResult.addError("Field " + propName + " not found on type " + currentType.getQualifiedName());
							return null;
						}
						
						javaExpr += ".getField(\"" + propName + "\")";
						
						if (i < (groupParts.size() - 1))
						{
							if (field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
							{
								currentType = env.getType(((TypeReference)field.getDataType()).getType().getKID());
							}
							else if (field.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
							{
								currentType = env.getType(((InverseCollectionDataType)field.getDataType()).getInverseTypeId());
							}
							else if (field.getDataTypeId().equals(DataType.ASSOCIATION))
							{
								currentType = env.getType(((AssociationDataType)field.getDataType()).getAssociatedTypeId());
							}
							else
							{
								saveResult.addError("Invalid nested field reference " + MiscUtils.implode(groupParts, ".") + " in if condition");
								return null;
							}
						}
					}
				}
				// we only know the record inherits from record proxy
				else
				{
					currentType = null;
					
					// all subsequent properties must be translated into field getters
					for (int i = 0; i < groupParts.size(); i++)
					{
						String propName = groupParts.get(i);
						
						if (i == 0)
						{
							// this is the direct subproperty of the record, and we only know it is some record, so the only available properties
							// are system fields
							if (!Field.isSystemField(propName))
							{
								throw new BusinessProcessDeclarationException("Invalid nested field reference " + MiscUtils.implode(groupParts, ".") + " in if condition");
							}
							else
							{
								// if the property is a user reference, we can get its type
								if (Field.CREATEDBY_FIELD_NAME.equals(propName) || Field.LAST_MODIFIED_BY_FIELD_NAME.equals(propName))
								{
									currentType = env.getType(KeyPrefix.get(KID.USER_PREFIX));
								}
							}
						}
						else
						{
							if (currentType == null)
							{
								saveResult.addError("Invalid nested field reference " + MiscUtils.implode(groupParts, ".") + " in if condition");
								return null;
							}
							else
							{
								Field field = currentType.getField(propName);
								
								if (field == null)
								{
									saveResult.addError("Field " + propName + " not found on type " + currentType.getQualifiedName());
									return null;
								}
								
								if (i < (groupParts.size() - 1))
								{
									if (field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
									{
										currentType = env.getType(((TypeReference)field.getDataType()).getType().getKID());
									}
									else if (field.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
									{
										currentType = env.getType(((InverseCollectionDataType)field.getDataType()).getInverseTypeId());
									}
									else if (field.getDataTypeId().equals(DataType.ASSOCIATION))
									{
										currentType = env.getType(((AssociationDataType)field.getDataType()).getAssociatedTypeId());
									}
									else
									{
										saveResult.addError("Invalid nested field reference " + MiscUtils.implode(groupParts, ".") + " in if condition");
										return null;
									}
								}
							}
						}
						
						javaExpr += ".getField(\"" + propName + "\")";
					}
				}
			}
			
			// create an alias to this expression
			String alias = "expr" + MiscUtils.getHash(15);
			aliasToJavaExpression.put(alias, javaExpr);
			
			m.appendReplacement(sb, alias);
		}
		m.appendTail(sb);
		
		relCondition = conditionToJava(sb.toString(), aliasToJavaExpression, env);
		
		String evaluatorCode = getConditionEvaluatorClass("IfConditionEvaluator" + ifConditionInvocation.getId(), "kommet.businessprocess.evaluators", relCondition, actionValuesJavaVar);
		
		kommet.basic.Class conditionEvaluator = new kommet.basic.Class();
		
		if (ifConditionInvocation.getId() != null)
		{
			String evaluatorClassId = ifConditionInvocation.getSingleAttributeValue("evaluatorClassId");
			if (StringUtils.hasText(evaluatorClassId))
			{
				// reuse existing evaluator class
				conditionEvaluator = classService.getClass(KID.get(evaluatorClassId), env);
				
				if (conditionEvaluator == null)
				{
					// the evaluator class can be not found if the process was moved e.g. together with the whole database
					// from another server instance - in this case we simply create a new evaluator
					conditionEvaluator = new kommet.basic.Class();
				}
			}
		}
		
		conditionEvaluator.setName("IfConditionEvaluator" + ifConditionInvocation.getId());
		conditionEvaluator.setPackageName("kommet.businessprocess.evaluators");
		conditionEvaluator.setIsSystem(true);
		conditionEvaluator.setAccessType(RecordAccessType.SYSTEM.getId());
		conditionEvaluator.setKollCode(evaluatorCode);
		
		return classService.fullSave(conditionEvaluator, dataService, authData, env);
	}
	
	private String getConditionEvaluatorClass(String name, String packageName, String javaCondition, String actionValuesJavaVar)
	{
		StringBuilder code = new StringBuilder();
		code.append("package ").append(packageName).append(";\n\n");
		
		code.append("import ").append(Record.class.getName()).append(";\n");
		code.append("import ").append(KommetException.class.getName()).append(";\n");
		code.append("import ").append(java.util.Map.class.getName()).append(";\n");
		
		// start class
		code.append("public class ").append(name).append(" {");
		
		code.append("public boolean evaluate(Map<String, Object> " + actionValuesJavaVar + ") throws KommetException {");
		code.append("return ").append(javaCondition).append(";\n}\n");
		
		// end class
		code.append("\n}");
		
		return code.toString();
	}

	/**
	 * 
	 * @param relCondition
	 * @param aliasToJavaExpression
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	private static String conditionToJava(String relCondition, Map<String, String> aliasToJavaExpression, EnvData env) throws KommetException
	{
		relCondition = RELParser.relToJava(relCondition, null, null, false, false, true, env);
		
		// replace single quotes with double quotes
		Pattern singleQuotePattern = Pattern.compile("'(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
		Matcher quoteMatcher = singleQuotePattern.matcher(relCondition);
		
		StringBuffer quoteString = new StringBuffer();
		while (quoteMatcher.find())
		{
			quoteMatcher.appendReplacement(quoteString, "\"");
		}
		quoteMatcher.appendTail(quoteString);
		relCondition = quoteString.toString();
		
		// replace <> operator
		Pattern nonEqualOperatorPattern = Pattern.compile("<>(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
		Matcher neOperatorMatcher = nonEqualOperatorPattern.matcher(relCondition);
		
		StringBuffer neString = new StringBuffer();
		while (neOperatorMatcher.find())
		{
			neOperatorMatcher.appendReplacement(neString, "!=");
		}
		neOperatorMatcher.appendTail(neString);
		relCondition = neString.toString();
		
		// replace = operator
		Pattern equalOperatorPattern = Pattern.compile("=(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
		Matcher eqOperatorMatcher = equalOperatorPattern.matcher(relCondition);
		
		StringBuffer eqString = new StringBuffer();
		while (eqOperatorMatcher.find())
		{
			eqOperatorMatcher.appendReplacement(eqString, "==");
		}
		eqOperatorMatcher.appendTail(eqString);
		relCondition = eqString.toString();
		
		// replace aliases with correct java expressions
		// TODO this replaces aliases everywhere, also in strings, but we assumes they are so random that they are not likely to appear in strings
		for (String alias : aliasToJavaExpression.keySet())
		{
			relCondition = relCondition.replaceAll(alias, aliasToJavaExpression.get(alias));
		}
		
		return relCondition;
	}

	@Transactional
	private void deleteInvocations(List<BusinessActionInvocation> invocations, ClassService classService, DataService dataService, AuthData authData, EnvData env) throws KommetException
	{
		// first delete evaluator classes for each invocation
		for (BusinessActionInvocation inv : invocations)
		{
			if (inv.getInvokedAction() != null && inv.getInvokedAction().getType().equals("If"))
			{
				List<BusinessActionInvocationAttribute> attr = inv.getAttribute("evaluatorClassId");
				
				// this attribute is not always set - if the process was previously saved as draft, the evaluator class was not generated
				// so there is nothing to remove
				if (!attr.isEmpty())
				{
					classService.delete(KID.get(attr.get(0).getValue()), dataService, authData, env);
				}
			}
		}
		
		invocationDao.delete(invocations, authData, env);
	}
	
	@Transactional
	private void deleteTransitions(List<BusinessActionTransition> transitions, AuthData authData, EnvData env) throws KommetException
	{
		transitionDao.delete(transitions, authData, env);
	}
	
	@Transactional(readOnly = true)
	public BusinessProcess getBusinessProcess(KID id, AuthData authData, EnvData env) throws KommetException
	{
		return getBusinessProcess(id, false, authData, env);
	}

	@Transactional(readOnly = true)
	public BusinessProcess getBusinessProcess(KID id, boolean initSubprocesses, AuthData authData, EnvData env) throws KommetException
	{
		BusinessProcessFilter filter = new BusinessProcessFilter();
		filter.addProcessId(id);
		filter.setInitSubprocesses(initSubprocesses);
		List<BusinessProcess> processes = get(filter, authData, env);
		return processes.isEmpty() ? null : processes.get(0);
	}

	@Transactional
	public BusinessAction createBusinessActionFromFile(Class<?> compiledClass, kommet.basic.Class file, BusinessAction existingAction, AuthData authData, EnvData env) throws KommetException
	{
		BusinessAction action = new BusinessAction();
		
		kommet.businessprocess.annotations.BusinessAction actionAnnot = compiledClass.getAnnotation(kommet.businessprocess.annotations.BusinessAction.class);
		if (actionAnnot == null)
		{
			throw new BusinessProcessDeclarationException("Cannot created business action from class not annotated with @" + BusinessAction.class.getSimpleName());
		}
		
		action.setName(actionAnnot.name());
		
		if (StringUtils.hasText(actionAnnot.description()))
		{
			action.setDescription(actionAnnot.description());
		}
		
		action.setFile(file);
		action.setType("Action");
		action.setIsEntryPoint(false);
		
		List<BusinessActionDeclarationError> errors = new ArrayList<BusinessActionDeclarationError>();
		
		validateExecuteMethod(compiledClass, errors, env);
		
		if (!errors.isEmpty())
		{
			throw new BusinessProcessDeclarationException(errors);
		}
		
		action = readParams(action, compiledClass, errors, env);
		
		if (!errors.isEmpty())
		{
			throw new BusinessProcessDeclarationException(errors);
		}
		
		if (existingAction != null)
		{
			// check if this action is used anywhere
			boolean isActionUsed = !getInvocationsForAction(existingAction, authData, env).isEmpty();
			
			if (!isActionUsed)
			{
				// delete the action
				deleteAction(existingAction, authData, env);
			}
			else
			{
				BusinessProcessSaveResult saveResult = new BusinessProcessSaveResult();
				
				// check if this action is used anywhere
				validateUsedCallable(action, existingAction, "action", saveResult, env);
				
				if (!saveResult.isValid())
				{
					throw new BusinessProcessDeclarationException(BusinessProcessDeclarationException.createErrorList(saveResult.getErrors()));
				}
				else
				{
					action.setId(existingAction.getId());
				}
			}
		}
		
		return save(action, authData, env);
	}

	/**
	 * Return invocations in which this action is used.
	 * @param action
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	private List<BusinessActionInvocation> getInvocationsForAction(BusinessAction action, AuthData authData, EnvData env) throws KommetException
	{
		// find invocations for this action
		BusinessActionInvocationFilter filter = new BusinessActionInvocationFilter();
		filter.addInvokedActionId(action.getId());
		return invocationDao.get(filter, authData, env);
	}

	/**
	 * Saves a business action and all its parameters.
	 * @param action
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException 
	 */
	@Transactional
	public BusinessAction save(BusinessAction action, AuthData authData, EnvData env) throws KommetException
	{
		action = bpaDao.save(action, authData, env);
		
		if (action.getInputs() != null)
		{
			for (BusinessProcessInput input : action.getInputs())
			{
				bpiDao.save(input, authData, env);
			}
		}
		
		if (action.getOutputs() != null)
		{
			for (BusinessProcessOutput output : action.getOutputs())
			{
				bpoDao.save(output, authData, env);
			}
		}
		
		return action;
	}

	private BusinessAction readParams(BusinessAction action, Class<?> file, List<BusinessActionDeclarationError> errors, EnvData env) throws KommetException
	{
		for (Method m : file.getDeclaredMethods())
		{
			if (m.isAnnotationPresent(Input.class))
			{
				boolean isValidInput = true;
				
				if (m.getParameterTypes().length != 1)
				{
					errors.add(new BusinessActionDeclarationError("@" + Input.class.getSimpleName() + " method " + m.getName() + " must take exactly one parameter"));
					isValidInput = false;
					
					continue;
				}
				
				Input inputAnnot = m.getAnnotation(Input.class);
				
				isValidInput &= validateParamType(m.getParameterTypes()[0], inputAnnot.name(), true, errors, env);
				
				if (Modifier.isStatic(m.getModifiers()))
				{
					errors.add(new BusinessActionDeclarationError("@" + Input.class.getSimpleName() + " method " + m.getName() + " must not be static"));
					isValidInput = false;
				}
				
				if (!m.getReturnType().equals(Void.TYPE))
				{
					errors.add(new BusinessActionDeclarationError("@" + Input.class.getSimpleName() + " method " + m.getName() + " must return void"));
					isValidInput = false;
				}
				
				if (isValidInput)
				{
					BusinessProcessInput input = new BusinessProcessInput();
					input.setName(inputAnnot.name());
					input.setDescription(StringUtils.hasText(inputAnnot.description()) ? inputAnnot.description() : null);
					input.setBusinessAction(action);
					
					ParamDataType pdt = deduceParamDataType(m.getParameterTypes()[0], env);
					input.setDataTypeId(pdt.getDataTypeId());
					input.setDataTypeName(pdt.getDataTypeName());
					
					action.addInput(input);
				}
				
			}
			else if (m.isAnnotationPresent(Output.class))
			{
				boolean isValidOutput = true;
				
				if (m.getParameterTypes().length > 0)
				{
					errors.add(new BusinessActionDeclarationError("@" + Output.class.getSimpleName() + " method " + m.getName() + " must be parameterless"));
					isValidOutput = false;
				}
				
				if (Modifier.isStatic(m.getModifiers()))
				{
					errors.add(new BusinessActionDeclarationError("@" + Output.class.getSimpleName() + " method " + m.getName() + " must not be static"));
					isValidOutput = false;
				}
				
				if (m.getReturnType().equals(Void.TYPE))
				{
					errors.add(new BusinessActionDeclarationError("@" + Output.class.getSimpleName() + " method " + m.getName() + " must not return void"));
					isValidOutput = false;
				}
				
				Output outputAnnot = m.getAnnotation(Output.class);
				
				isValidOutput &= validateParamType(m.getReturnType(), outputAnnot.name(), false, errors, env);
				
				if (isValidOutput)
				{
					BusinessProcessOutput output = new BusinessProcessOutput();
					output.setName(outputAnnot.name());
					output.setDescription(StringUtils.hasText(outputAnnot.description()) ? outputAnnot.description() : null);
					output.setBusinessAction(action);
					
					ParamDataType pdt = deduceParamDataType(m.getReturnType(), env);
					output.setDataTypeId(pdt.getDataTypeId());
					output.setDataTypeName(pdt.getDataTypeName());
					
					action.addOutput(output);
				}
			}
		}
		
		return action;
	}

	private ParamDataType deduceParamDataType(Class<?> type, EnvData env) throws KommetException
	{
		ParamDataType pdt = new ParamDataType();
		
		String typeName = type.getName();
		if (MiscUtils.isEnvSpecific(typeName))
		{
			typeName = MiscUtils.envToUserPackage(typeName, env);
			
			if (RecordProxy.class.isAssignableFrom(type))
			{
				pdt.setDataTypeId(env.getType(typeName).getKID());
			}
			else
			{
				pdt.setDataTypeName(typeName);
			}
		}
		else
		{
			pdt.setDataTypeName(typeName);
		}
		
		return pdt;
	}

	/**
	 * Make sure the parameter type is one of the allowed types
	 * @param returnType
	 * @param paramName
	 * @param errors
	 * @return
	 * @throws KommetException 
	 */
	private boolean validateParamType(Class<?> type, String paramName, boolean isInput, List<BusinessActionDeclarationError> errors, EnvData env) throws KommetException
	{
		String typeName = type.getName();
		if (MiscUtils.isEnvSpecific(typeName))
		{
			typeName = MiscUtils.envToUserPackage(typeName, env);
		}
		
		if (getAllowedParamTypes().contains(typeName) || RecordProxy.class.isAssignableFrom(type))
		{
			return true;
		}
		else
		{
			errors.add(new BusinessActionDeclarationError((isInput ? "Input" : "Output") + " parameter " + paramName + " has incorrect data type " + type.getName()));
			return false;
		}
	}
	
	private static Set<String> getAllowedParamTypes()
	{
		if (allowedParamTypes == null)
		{
			allowedParamTypes = new HashSet<String>();
			allowedParamTypes.add(Record.class.getName());
			allowedParamTypes.add(String.class.getName());
			allowedParamTypes.add(Integer.class.getName());
			allowedParamTypes.add(Double.class.getName());
		}
		return allowedParamTypes;
	}

	private void validateExecuteMethod(Class<?> file, List<BusinessActionDeclarationError> errors, EnvData env) throws KommetException
	{
		Method executeMethod = null;
		String className = MiscUtils.envToUserPackage(file.getName(), env);
		
		// make sure the class contains exactly one method annotated with @Execute and that this method is parameterless
		for (Method m : file.getDeclaredMethods())
		{
			if (m.isAnnotationPresent(Execute.class))
			{
				if (executeMethod != null)
				{
					errors.add(new BusinessActionDeclarationError("Class " + className + " contains more than one method annotated with @" + Execute.class.getSimpleName()));
				}
				
				executeMethod = m;
				
				if (!Modifier.isPublic(executeMethod.getModifiers()))
				{
					errors.add(new BusinessActionDeclarationError("@" + Execute.class.getSimpleName() + " method is not accessible in class " + className));
				}
				
				if (executeMethod.getParameterTypes().length > 0)
				{
					errors.add(new BusinessActionDeclarationError("@" + Execute.class.getSimpleName() + " method must not take any parameters"));
				}
				
				if (!executeMethod.getReturnType().equals(Void.TYPE))
				{
					errors.add(new BusinessActionDeclarationError("@" + Execute.class.getSimpleName() + " method must return void, but returns " + executeMethod.getReturnType()));
				}
				
				if (Modifier.isStatic(executeMethod.getModifiers()))
				{
					errors.add(new BusinessActionDeclarationError("@" + Execute.class.getSimpleName() + " method not be static"));
				}
			}
		}
		
		if (executeMethod == null)
		{
			errors.add(new BusinessActionDeclarationError("@" + Execute.class.getSimpleName() + " method not declared"));
		}
	}

	@Transactional
	public void deleteAction(BusinessAction action, AuthData authData, EnvData env) throws KommetException
	{
		// make sure this action is not used anywhere before removing it
		BusinessActionInvocationFilter invocationFilter = new BusinessActionInvocationFilter();
		invocationFilter.addInvokedActionId(action.getId());
		
		if (!invocationDao.get(invocationFilter, authData, env).isEmpty())
		{
			throw new BusinessProcessModificationException("Action cannot be deleted because it is referenced in business processes");
		}
		
		bpaDao.delete(Arrays.asList(action), authData, env);
	}

	@Transactional(readOnly = true)
	public BusinessAction getActionForFile(KID fileId, AuthData authData, EnvData env) throws KommetException
	{
		BusinessActionFilter filter = new BusinessActionFilter();
		filter.addFileId(fileId);
		List<BusinessAction> actions = bpaDao.get(filter, authData, env);
		return actions.isEmpty() ? null : actions.get(0);
	}

	@Transactional(readOnly = true)
	public BusinessAction getAction(String actionName, AuthData authData, EnvData env) throws KommetException
	{
		BusinessActionFilter filter = new BusinessActionFilter();
		filter.setName(actionName);
		filter.setInitParams(true);
		List<BusinessAction> actions = get(filter, authData, env);
		return actions.isEmpty() ? null : actions.get(0);
	}
	
	@Transactional(readOnly = true)
	public List<BusinessProcess> get(BusinessProcessFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		List<BusinessProcess> processes = bpDao.get(filter, authData, env);
		
		if (processes.isEmpty())
		{
			return processes;
		}
		
		Map<KID, BusinessProcess> processIds = new HashMap<KID, BusinessProcess>();
		for (BusinessProcess process : processes)
		{
			processIds.put(process.getId(), process);
		}
		
		// init action input and output parameters
		// this could not be done in the dao because it would require a two-level nesting of properties
		initProcessInvocations(processIds, filter.isInitSubprocesses(), authData, env);
		initProcessParamAssignments(processIds, authData, env);
		initProcessTransitions(processIds, authData, env);
		
		return processes;
	}
	
	private void initProcessTransitions(Map<KID, BusinessProcess> processIds, AuthData authData, EnvData env) throws KommetException
	{
		BusinessActionTransitionFilter filter = new BusinessActionTransitionFilter();
		filter.setProcessIds(processIds.keySet());
		
		List<BusinessActionTransition> transitions = transitionDao.get(filter, authData, env);
		
		for (BusinessActionTransition t : transitions)
		{
			processIds.get(t.getBusinessProcess().getId()).addTransition(t);
		}
	}
	
	private void initProcessInvocations(Map<KID, BusinessProcess> processIds, boolean initSubprocesses, AuthData authData, EnvData env) throws KommetException
	{
		BusinessActionInvocationFilter filter = new BusinessActionInvocationFilter();
		filter.setParentProcessIds(processIds.keySet());
		
		List<BusinessActionInvocation> invocations = invocationDao.get(filter, authData, env);
		
		// query business actions for each invocation - this could not be done in one query
		initCallableOnInvocations(invocations, initSubprocesses, authData, env);
		
		for (BusinessActionInvocation a : invocations)
		{
			processIds.get(a.getParentProcess().getId()).addInvocation(a);
		}
	}

	private void initCallableOnInvocations(List<BusinessActionInvocation> invocations, boolean initSubprocesses, AuthData authData, EnvData env) throws KommetException
	{
		if (invocations.isEmpty())
		{
			return;
		}
		
		Set<KID> actionIds = new HashSet<KID>();
		Set<KID> processIds = new HashSet<KID>();
		
		for (BusinessActionInvocation inv : invocations)
		{
			if (inv.getCallable() == null)
			{
				throw new BusinessProcessException("Neither process nor action set on invocation " + inv.getName());
			}
			
			if (inv.getInvokedAction() != null)
			{
				if (inv.getInvokedAction().getId() == null)
				{
					throw new BusinessProcessException("Cannot initialize action on invocation because its action is not saved");
				}
				else
				{
					actionIds.add(inv.getInvokedAction().getId());
				}
			}
			else
			{
				if (inv.getInvokedProcess().getId() == null)
				{
					throw new BusinessProcessException("Cannot initialize action on invocation because its process is not saved");
				}
				else
				{
					processIds.add(inv.getInvokedProcess().getId());
				}
			}
		}
		
		// find actions
		BusinessActionFilter actionFilter = new BusinessActionFilter();
		actionFilter.setActionIds(actionIds);
		List<BusinessAction> actions = bpaDao.get(actionFilter, authData, env);
		Map<KID, BusinessAction> actionsById = MiscUtils.mapById(actions);
		
		Map<KID, BusinessProcess> processesById = new HashMap<KID, BusinessProcess>();
		
		// find processes
		if (initSubprocesses)
		{
			// query each subprocess separately
			for (KID processId : processIds)
			{
				processesById.put(processId, this.getBusinessProcess(processId, authData, env));
			}
		}
		else
		{
			BusinessProcessFilter processFilter = new BusinessProcessFilter();
			processFilter.setProcessIds(processIds);
			List<BusinessProcess> processes = bpDao.get(processFilter, authData, env);
			processesById = MiscUtils.mapById(processes);
		}
		
		for (BusinessActionInvocation inv : invocations)
		{
			if (inv.getInvokedAction() != null)
			{
				inv.setInvokedAction(actionsById.get(inv.getInvokedAction().getId()));
			}
			else if (inv.getInvokedProcess() != null)
			{
				inv.setInvokedProcess(processesById.get(inv.getInvokedProcess().getId()));
			}
		}
	}

	private void initProcessParamAssignments(Map<KID, BusinessProcess> processIds, AuthData authData, EnvData env) throws KommetException
	{
		BusinessProcessParamAssignmentFilter filter = new BusinessProcessParamAssignmentFilter();
		filter.setProcessIds(processIds.keySet());
		
		List<BusinessProcessParamAssignment> assignments = assignmentDao.get(filter, authData, env);
		
		for (BusinessProcessParamAssignment a : assignments)
		{
			BusinessProcess process = processIds.get(a.getBusinessProcess().getId());
			
			if (a.getSourceInvocation() != null)
			{
				a.setSourceInvocation(process.getInvocation(a.getSourceInvocation().getName()));
			}
			
			if (a.getTargetInvocation() != null)
			{
				a.setTargetInvocation(process.getInvocation(a.getTargetInvocation().getName()));
			}
			
			processIds.get(a.getBusinessProcess().getId()).addParamAssignment(a);
		}
	}

	@Transactional(readOnly = true)
	public List<BusinessAction> get(BusinessActionFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		List<BusinessAction> actions = bpaDao.get(filter, authData, env);
		
		// init action input and output parameters
		// this could not be done in the dao because it would require a two-level nesting of properties
		actions = initActionParams(actions, authData, env);
		
		if (filter.isInitParams() && !actions.isEmpty())
		{
			initParamsOnActions(actions, authData, env);
		}
		
		return actions;
	}

	private List<BusinessAction> initActionParams(List<BusinessAction> actions, AuthData authData, EnvData env) throws KommetException
	{
		if (actions.isEmpty())
		{
			return actions;
		}
		
		BusinessActionFilter filter = new BusinessActionFilter();
		for (BusinessAction action : actions)
		{
			filter.addActionId(action.getId());
		}
		
		// find actions
		return bpaDao.get(filter, authData, env);
	}

	private void initParamsOnActions(List<BusinessAction> actions, AuthData authData, EnvData env) throws KommetException
	{
		Map<KID, BusinessAction> actionsById = new HashMap<KID, BusinessAction>();
		for (BusinessAction a : actions)
		{
			// clear inputs so that they are not added twice
			a.setInputs(null);
			a.setOutputs(null);
			
			actionsById.put(a.getId(), a);
		}
		
		// find inputs for these actions
		BusinessProcessParamFilter filter = new BusinessProcessParamFilter();
		filter.setActionIds(actionsById.keySet());
		
		List<BusinessProcessInput> inputs = bpiDao.get(filter, authData, env);
		
		if (inputs != null)
		{
			for (BusinessProcessInput input : inputs)
			{
				actionsById.get(input.getBusinessAction().getId()).addInput(input);
			}
		}
		
		// find outputs for these actions
		
		List<BusinessProcessOutput> outputs = bpoDao.get(filter, authData, env);
				
		if (outputs != null)
		{
			for (BusinessProcessOutput output : outputs)
			{
				actionsById.get(output.getBusinessAction().getId()).addOutput(output);
			}
		}
	}

	@Transactional(readOnly = true)
	private BusinessAction getActionByType(String type, EnvData env) throws KommetException
	{
		BusinessActionFilter filter = new BusinessActionFilter();
		filter.setType(type);
		List<BusinessAction> actions = get(filter, AuthData.getRootAuthData(env), env);
		
		if (actions.size() == 0)
		{
			throw new BusinessProcessException(type + " business action not found");
		}
		else if (actions.size() > 1)
		{
			throw new BusinessProcessException("More than one " + type + " business action found");
		}
		
		return actions.get(0);
	}

	@Transactional(readOnly = true)
	public BusinessAction getRecordCreateAction(EnvData env) throws KommetException
	{
		return getActionByType("RecordCreate", env);
	}
	
	@Transactional(readOnly = true)
	public BusinessAction getFieldUpdateAction(EnvData env) throws KommetException
	{
		return getActionByType("FieldUpdate", env);
	}
	
	@Transactional(readOnly = true)
	public BusinessAction getFieldValueAction(EnvData env) throws KommetException
	{
		return getActionByType("FieldValue", env);
	}
	
	@Transactional(readOnly = true)
	public BusinessAction getIfAction(EnvData env) throws KommetException
	{
		return getActionByType("If", env);
	}
	
	@Transactional(readOnly = true)
	public BusinessAction getRecordUpdateAction(EnvData env) throws KommetException
	{
		return getActionByType("RecordUpdate", env);
	}
	
	@Transactional(readOnly = true)
	public BusinessAction getRecordSaveAction(EnvData env) throws KommetException
	{
		return getActionByType("RecordSave", env);
	}

	@Transactional
	public void save(BusinessProcessInput input, AuthData authData, EnvData env) throws KommetException
	{
		bpiDao.save(input, authData, env);
	}
	
	@Transactional
	public void save(BusinessProcessOutput output, AuthData authData, EnvData env) throws KommetException
	{
		bpoDao.save(output, authData, env);
	}

	/**
	 * Creates standard business actions (such as Query, QueryUnique) by declaring them in code.
	 * @param env
	 * @throws KommetException 
	 */
	@Transactional
	public void createStandardBusinessActions(ClassService classService, DataService dataService, EnvData env) throws KommetException
	{
		AuthData authData = AuthData.getRootAuthData(env);
		createQueryUniqueAction(classService, dataService, authData, env);
	}

	private void createQueryUniqueAction(ClassService classService, DataService dataService, AuthData authData, EnvData env) throws KommetException
	{
		List<String> inputs = new ArrayList<String>();
		inputs.add("@" + Input.class.getSimpleName() + "(name = \"query\")\npublic void setQuery (String query) { this.query = query; }");
		
		List<String> outputs = new ArrayList<String>();
		outputs.add("@" + Output.class.getSimpleName() + "(name = \"record\")\npublic " + RecordProxy.class.getName() + " getRecord() { return this.record; }");
		
		List<String> fields = new ArrayList<String>();
		fields.add("private " + RecordProxy.class.getName() + " record;");
		fields.add("private " + String.class.getName() + " query;");
		
		String execute = "@" + Execute.class.getSimpleName() + "\npublic void execute() throws KommetException {";
		execute += "this.record = sys.queryUniqueResult(this.query);\n";
		execute += "}";
		
		List<Class<?>> imports = new ArrayList<Class<?>>();
		imports.add(String.class);
		
		kommet.basic.Class file = getBusinessActionFile("QueryUniqueAction", Constants.BUSINESS_ACTIONS_PACKAGE, "QueryUnique", "Queries database and returns a unique result", imports, inputs, outputs, execute, fields, env);
		
		// we would like to make this action system immutable, but it contains hardcoded environemt ID and needs to be updated each time the env is migrated,
		// so it cannot be made immutable
		// TODO prevent modifying this class somehow
		file.setAccessType(RecordAccessType.SYSTEM.getId());
		
		file = classService.fullSave(file, dataService, authData, env);
	}
	
	private static kommet.basic.Class getBusinessActionFile(String className, String packageName, String name, String desc, List<Class<?>> imports, List<String> inputs, List<String> outputs, String execute, List<String> fields, EnvData env) throws KommetException
	{
		StringBuilder code = new StringBuilder();
		code.append("package " + packageName).append(";\n");
		
		// add imports
		for (Class<?> cls : imports)
		{
			code.append("import ").append(cls.getName()).append(";\n");
		}
		
		code.append("import ").append(kommet.businessprocess.annotations.BusinessAction.class.getName()).append(";\n");
		code.append("import ").append(Input.class.getName()).append(";\n");
		code.append("import ").append(Output.class.getName()).append(";\n");
		code.append("import ").append(Execute.class.getName()).append(";\n");
		code.append("import ").append(KommetException.class.getName()).append(";\n");
		
		code.append("\n@" + BusinessAction.class.getSimpleName()).append("(name = \"").append(name).append("\", description = \"" + desc + "\")\n");
		
		code.append("public class ").append(className).append("\n{\n");
		
		code.append(MiscUtils.implode(fields, "\n\n"));
		code.append(MiscUtils.implode(inputs, "\n\n"));
		code.append(MiscUtils.implode(outputs, "\n\n"));
		code.append(execute);
		
		// close class
		code.append("\n}");
		
		kommet.basic.Class cls = new kommet.basic.Class();
		cls.setName(className);
		cls.setPackageName(packageName);
		cls.setKollCode(code.toString());
		cls.setIsSystem(false);
		
		return cls;
	}
	
	class ParamDataType
	{
		private String dataTypeName;
		private KID dataTypeId;
		public String getDataTypeName()
		{
			return dataTypeName;
		}
		public void setDataTypeName(String dataTypeName)
		{
			this.dataTypeName = dataTypeName;
		}
		public KID getDataTypeId()
		{
			return dataTypeId;
		}
		public void setDataTypeId(KID dataTypeId)
		{
			this.dataTypeId = dataTypeId;
		}
	}

	public BusinessAction getAction(KID rid, AuthData authData, EnvData env) throws KommetException
	{
		BusinessActionFilter filter = new BusinessActionFilter();
		filter.addActionId(rid);
		
		List<BusinessAction> actions = get(filter, authData, env);
		return actions.isEmpty() ? null : actions.get(0);
	}

	@Transactional
	public void deleteProcess(KID processId, AuthData authData,	EnvData env) throws KommetException
	{
		bpaDao.delete(processId, authData, env);
		
		// delete cached process executor
		env.removeProcessExecutor(processId);
	}
	
	public static Field getFieldValueActionSingleOutput(BusinessActionInvocation inv, Type inputRecordType) throws BusinessProcessDeclarationException
	{
		BusinessAction action = inv.getInvokedAction();
		
		if (!"FieldValue".equals(action.getType()))
		{
			throw new BusinessProcessDeclarationException(action.getName() + " is not a FieldValue action");
		}
		
		if (inputRecordType == null)
		{
			throw new BusinessProcessDeclarationException("Cannot get FieldValue input. Passed type parameter is null");
		}
		
		// validate if action has exactly one attribute
		if (inv.getAttributes() != null && !inv.getAttributes().isEmpty())
		{
			if (inv.getAttributes().size() > 1)
			{
				throw new BusinessProcessDeclarationException("FieldValue action has " + inv.getAttributes().size() + " attributes - expected exactly one");
			}
			
			String fieldName;
			try
			{
				fieldName = inv.getSingleAttributeValue(BusinessAction.FIELD_VALUE_ACTION_FIELD_NAME_INPUT);
			}
			catch (BusinessProcessException e)
			{
				throw new BusinessProcessDeclarationException(e.getMessage());
			}
			
			Field field = null;
			try
			{
				field = inputRecordType.getField(fieldName);
			}
			catch (KommetException e)
			{
				e.printStackTrace();
				throw new BusinessProcessDeclarationException("Error retrieving field " + fieldName + " from type " + inputRecordType.getQualifiedName() + ". Nested: " + e.getMessage());
			}
			
			if (field == null)
			{
				throw new BusinessProcessDeclarationException("Type " + inputRecordType.getQualifiedName() + " does not contain field " + fieldName);
			}
			
			return field;
		}
		else
		{
			throw new BusinessProcessDeclarationException("FieldValue action has no attributes - expected exactly one");
		}
	}
}