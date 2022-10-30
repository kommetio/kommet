/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.triggers;

import java.net.MalformedURLException;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.basic.Class;
import kommet.basic.TypeTrigger;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.koll.ClassCompilationException;
import kommet.koll.annotations.Disabled;
import kommet.koll.annotations.triggers.Trigger;
import kommet.koll.compiler.CompilationResult;
import kommet.koll.compiler.KommetCompiler;

@Service
public class TriggerService
{
	@Inject
	TypeTriggerDao typeTriggerDao;
	
	@Inject
	KommetCompiler compiler;
	
	@Transactional(readOnly = true)
	public List<TypeTrigger> find (TypeTriggerFilter filter, EnvData env) throws KommetException
	{
		return this.typeTriggerDao.find(filter, env);
	}
	
	@Transactional
	private TypeTrigger registerTriggerWithType(Class file, Type userType, boolean isSystem, boolean isActive, AuthData authData, EnvData env) throws KommetException
	{
		return registerTriggerWithType(file, userType, isSystem, isActive, compiler, typeTriggerDao, authData, env);
	}

	public static void updateTriggerWithType (Class triggerClass, TypeTriggerDao ttDao, KommetCompiler compiler, EnvData env) throws KommetException
	{
		// make sure the trigger file is compiled
		java.lang.Class<?> compiledTrigger = null;
		
		try
		{
			compiledTrigger = compiler.getClass(triggerClass, false, env);
		}
		catch (ClassNotFoundException e)
		{
			// ignore
		}
		catch (MalformedURLException e)
		{
			throw new KommetException("Error retrieving compiled trigger", e);
		}
		
		if (compiledTrigger == null)
		{
			CompilationResult result = compiler.compile(triggerClass, env);
			if (!result.isSuccess())
			{
				throw new ClassCompilationException(result.getDescription());
			}
			
			try
			{
				compiledTrigger = compiler.getClass(triggerClass, false, env);
			}
			catch (Exception e)
			{
				throw new KommetException("Error retrieving compiled trigger", e);
			}
		}
		
		TypeTriggerFilter filter = new TypeTriggerFilter();
		filter.addTriggerFileId(triggerClass.getId());
		List<TypeTrigger> tts = ttDao.find(filter, env);
		
		if (tts.size() != 1)
		{
			throw new KommetException("Should have found exactly 1 type trigger for class " + triggerClass.getId() + " but found " + tts.size());
		}
				
		// reinit trigger flags
		env.setOldProxiesOnTypeFlag(tts.get(0).getTypeId(), compiledTrigger, tts.get(0), env);
	}
	
	/**
	 * Method exposed as static to be used by the ClassService.
	 * @param triggerClass
	 * @param type
	 * @param isSystem
	 * @param isActive
	 * @param compiler
	 * @param typeTriggerDao
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	public static TypeTrigger registerTriggerWithType(Class triggerClass, Type type, boolean isSystem, boolean isActive, KommetCompiler compiler, TypeTriggerDao typeTriggerDao, AuthData authData, EnvData env) throws KommetException
	{
		// make sure the trigger file is compiled
		java.lang.Class<?> compiledTrigger = null;
		
		try
		{
			compiledTrigger = compiler.getClass(triggerClass, false, env);
		}
		catch (ClassNotFoundException e)
		{
			// ignore
		}
		catch (MalformedURLException e)
		{
			throw new KommetException("Error retrieving compiled trigger", e);
		}
		
		if (compiledTrigger == null)
		{
			CompilationResult result = compiler.compile(triggerClass, env);
			if (!result.isSuccess())
			{
				throw new ClassCompilationException(result.getDescription());
			}
			
			try
			{
				compiledTrigger = compiler.getClass(triggerClass, false, env);
			}
			catch (Exception e)
			{
				throw new KommetException("Error retrieving compiled trigger", e);
			}
		}
		
		if (!compiledTrigger.isAnnotationPresent(Trigger.class))
		{
			throw new InvalidClassForTriggerException("Class cannot be used as trigger because it is not annotated with @" + Trigger.class.getName());
		}
		
		if (compiledTrigger.isAnnotationPresent(Disabled.class))
		{
			throw new KommetException("Trigger class with @Disabled annotation cannot be registered");
		}
		
		if (!compiledTrigger.getSuperclass().getName().equals(DatabaseTrigger.class.getName()))
		{
			throw new InvalidClassForTriggerException("Trigger class does not extend class " + DatabaseTrigger.class.getName() + ". Instead, it extends " + compiledTrigger.getSuperclass().getName());
		}
		
		// make sure the trigger class contains the execute method with proper signature
		try
		{
			compiledTrigger.getMethod("execute");
		}
		catch (Exception e)
		{
			throw new InvalidClassForTriggerException("Parameterless method execute() is not declared in class " + triggerClass.getQualifiedName());
		}
		
		TypeTrigger tt = new TypeTrigger();
		tt.setTriggerFile(triggerClass);
		tt.setTypeId(type.getKID());
		tt.setIsActive(isActive);
		tt.setIsSystem(isSystem);
		tt = TriggerUtil.setTriggerOccurrence(tt, compiledTrigger);
		tt = typeTriggerDao.save(tt, authData, env);
		
		// register trigger on the environment
		env.registerTrigger(tt);
		
		// reinit trigger flags
		env.setOldProxiesOnTypeFlag(tt.getTypeId(), compiledTrigger, tt, env);
		
		return tt;
	}

	@Transactional
	public TypeTrigger registerTriggerWithType(Class file, Type type, AuthData authData, EnvData env) throws KommetException
	{
		return registerTriggerWithType(file, type, false, true, authData, env);
	}
	
	@Transactional
	public void unregisterTriggerWithType(KID fileId, KID typeId, EnvData env) throws KommetException
	{
		unregisterTriggerWithType(fileId, typeId, typeTriggerDao, env);
	}
	
	public static void unregisterTriggerWithType(KID fileId, KID typeId, TypeTriggerDao typeTriggerDao, EnvData env) throws KommetException
	{
		TypeTriggerFilter filter = new TypeTriggerFilter();
		filter.addTriggerFileId(fileId);
		filter.addTypeId(typeId);
		
		List<TypeTrigger> triggers = typeTriggerDao.find(filter, env);
		if (triggers.isEmpty())
		{
			throw new TriggerException("No trigger found for file " + fileId + " and type " + typeId, TriggerException.TRIGGER_ERROR_NO_TRIGGER_TO_UNREGISTER);
		}
		else if (triggers.size() > 1)
		{
			throw new TriggerException("More than one trigger found for file " + fileId + " and type " + typeId);
		}
		
		typeTriggerDao.delete(triggers, true, null, env);
		env.unregisterTrigger(triggers.get(0));
	}

	@Transactional(readOnly = true)
	public TypeTrigger getById(KID id, EnvData env) throws KommetException
	{
		TypeTriggerFilter filter = new TypeTriggerFilter();
		filter.addTypeTriggerId(id);
		List<TypeTrigger> tts = typeTriggerDao.find(filter, env);
		return tts.isEmpty() ? null : tts.get(0);
	}

	public void unregisterTriggerWithType(KID typeTriggerId, EnvData env) throws KommetException
	{
		TypeTriggerFilter filter = new TypeTriggerFilter();
		filter.addTypeTriggerId(typeTriggerId);
		
		List<TypeTrigger> triggers = typeTriggerDao.find(filter, env);
		if (triggers.isEmpty())
		{
			throw new TriggerException("No trigger found for ID " + typeTriggerId);
		}
		
		typeTriggerDao.delete(triggers, true, null, env);
		env.unregisterTrigger(triggers.get(0));
	}

	public static void unregisterTrigger(KID classId, TypeTriggerDao ttDao, KommetCompiler compiler, AuthData authData, EnvData env) throws KommetException
	{
		TypeTriggerFilter filter = new TypeTriggerFilter();
		filter.addTriggerFileId(classId);
		
		List<TypeTrigger> triggers = ttDao.find(filter, env);
		if (!triggers.isEmpty())
		{
			if (triggers.size() > 1)
			{
				throw new KommetException("More than one trigger found for class " + classId);
			}
			
			ttDao.delete(triggers, authData, env);
			
			for (TypeTrigger tt : triggers)
			{	
				env.unregisterTrigger(tt);
				
				// reinitialize trigger flags
				env.initOldProxiesOnTypeFlags(tt.getTypeId(), ttDao, compiler);
			}
		}
	}
	
	@Transactional(readOnly = true)
	public boolean isTriggerRegisteredWithType(KID fileId, KID typeId, EnvData env) throws KommetException
	{
		return isTriggerRegisteredWithType(fileId, typeId, typeTriggerDao, env);
	}

	public static boolean isTriggerRegisteredWithType(KID fileId, KID typeId, TypeTriggerDao ttDao, EnvData env) throws KommetException
	{
		TypeTriggerFilter filter = new TypeTriggerFilter();
		filter.addTypeId(typeId);
		filter.addTriggerFileId(fileId);
		
		return !ttDao.find(filter, env).isEmpty();
	}
}