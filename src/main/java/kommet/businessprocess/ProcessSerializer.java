/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.businessprocess;

import java.util.ArrayList;
import java.util.List;

import kommet.basic.BusinessActionInvocation;
import kommet.basic.BusinessProcess;
import kommet.utils.MiscUtils;

public class ProcessSerializer
{
	public static String serialize (BusinessProcess process)
	{
		List<String> processProps = new ArrayList<String>();
		processProps.add("\"name\": \"" + process.getName() + "\"");
		processProps.add("\"label\": \"" + process.getLabel() + "\"");
		processProps.add("\"description\": \"" + process.getDescription() + "\"");
		processProps.add("\"isDraft\": " + process.getIsDraft());
		processProps.add("\"isActive\": " + process.getIsActive());
		processProps.add("\"isCallable\": " + process.getIsCallable());
		processProps.add("\"isTriggerable\": " + process.getIsTriggerable());
		
		processProps.add("\"invocations\": " + serializeInvocations(process));
		
		return "{ " + MiscUtils.implode(processProps, ", ") + " }";
	}

	private static String serializeInvocations(BusinessProcess process)
	{
		List<String> serializedInvocations = new ArrayList<String>();
		
		if (process.getInvocations() != null)
		{
			for (BusinessActionInvocation inv : process.getInvocations())
			{
				List<String> props = new ArrayList<String>();
				
				
				serializedInvocations.add("{ " + MiscUtils.implode(props, ", ") + " }");
			}
		}
		
		return "[ " + MiscUtils.implode(serializedInvocations, ", ") + " ]";
	}
}