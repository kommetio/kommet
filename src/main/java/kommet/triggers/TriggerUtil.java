/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.triggers;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.util.StringUtils;

import kommet.basic.Class;
import kommet.basic.TypeTrigger;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.koll.ClassService;
import kommet.koll.annotations.triggers.AfterDelete;
import kommet.koll.annotations.triggers.AfterInsert;
import kommet.koll.annotations.triggers.AfterUpdate;
import kommet.koll.annotations.triggers.BeforeDelete;
import kommet.koll.annotations.triggers.BeforeInsert;
import kommet.koll.annotations.triggers.BeforeUpdate;
import kommet.koll.annotations.triggers.Trigger;
import kommet.koll.compiler.KommetCompiler;

public class TriggerUtil
{
	public static String convertTriggerKollCode (String triggerKollCode, String triggerName, Type type, EnvData env) throws KommetException
	{
		if (!StringUtils.hasText(triggerKollCode))
		{
			throw new KommetException("Cannot convert empty trigger code");
		}
		
		// make the class extend TriggerFile
		//triggerKollCode = triggerKollCode.replaceFirst("public\\s+class\\s+" + triggerName, "public class " + triggerName + " extends " + TriggerFile.class.getName() + "<" + type.getQualifiedName() + ">");
		
		// replace type name with its ID in @Trigger annotation
		return triggerKollCode.replaceFirst("@Trigger\\s*\\(\\s*type\\s*=\\s*\"" + type.getQualifiedName() + "\"\\s*\\)", "@Trigger(type=\"" + type.getKID() + "\")");
	}
	
	public static TypeTrigger setTriggerOccurrence(TypeTrigger tt, java.lang.Class<?> trigger)
	{
		tt.setIsBeforeInsert(trigger.isAnnotationPresent(BeforeInsert.class));
		tt.setIsBeforeUpdate(trigger.isAnnotationPresent(BeforeUpdate.class));
		tt.setIsBeforeDelete(trigger.isAnnotationPresent(BeforeDelete.class));
		tt.setIsAfterInsert(trigger.isAnnotationPresent(AfterInsert.class));
		tt.setIsAfterUpdate(trigger.isAnnotationPresent(AfterUpdate.class));
		tt.setIsAfterDelete(trigger.isAnnotationPresent(AfterDelete.class));
		return tt;
	}
	
	/**
	 * Returns a list of files that can be used as triggers for the given type. Files are found by the @Trigger
	 * annotation with the <tt>type</tt> attribute pointing to the specific type.
	 * @param type
	 * @param excludeAlreadyRegistered If set to true, trigger file already registered as a trigger will not be returned.
	 * @param kollService
	 * @param compiler
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	public static List<Class> getTriggerCandidates (Type type, boolean excludeAlreadyRegistered, ClassService kollService, KommetCompiler compiler, EnvData env) throws KommetException
	{
		List<Class> kollFiles = kollService.getClasses(null, env);
		List<Class> validCandidates = new ArrayList<Class>();
		Set<KID> assignedTriggerFiles = new HashSet<KID>();
		
		if (excludeAlreadyRegistered)
		{
			for (TypeTrigger tt : env.getTriggers(type.getKID()).values())
			{
				assignedTriggerFiles.add(tt.getTriggerFile().getId());
			}
		}
		
		for (Class file : kollFiles)
		{
			java.lang.Class<?> cls;
			try
			{
				cls = compiler.getClass(file, false, env);
			}
			catch (MalformedURLException e1)
			{
				throw new KommetException("Malformed URL while getting class");
			}
			catch (ClassNotFoundException e1)
			{
				throw new KommetException("Java class for KOLL file " + file.getPackageName() + "." + file.getName() + " not compiled");
			}
			
			Trigger triggerAnnotation = cls.getAnnotation(Trigger.class);
			if (triggerAnnotation != null)
			{
				KID typeId = null;
				try
				{
					typeId = KID.get(triggerAnnotation.type());
				}
				catch (KIDException e)
				{
					throw new TriggerDeclarationException("Invalid type ID " + triggerAnnotation.type() + " in trigger annotation on class " + cls.getName());
				}
				
				if (typeId.equals(type.getKID()) && (!excludeAlreadyRegistered || !assignedTriggerFiles.contains(file.getId())))
				{
					validCandidates.add(file);
				}
			}
		}
		
		return validCandidates;
	}
}