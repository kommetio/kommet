/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.emailing;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

import kommet.utils.MiscUtils;

public class EmailUtil
{
	private static final String RECIPIENT_LIST_SEPARATOR = ",";
	
	public static List<Recipient> parseRecipients (String recipients)
	{
		List<Recipient> recipientList = new ArrayList<Recipient>();
		
		if (!StringUtils.hasText(recipients))
		{
			return recipientList;
		}
		
		String[] recipientItems = recipients.split(RECIPIENT_LIST_SEPARATOR);
		
		for (String item : recipientItems)
		{
			recipientList.add(new Recipient(item));
		}
		
		return recipientList;
	}
	
	public static String toRecipientList(List<Recipient> recipients)
	{
		List<String> sRecipients = new ArrayList<String>();
		
		if (recipients != null && !recipients.isEmpty())
		{
			for (Recipient r : recipients)
			{
				sRecipients.add(r.getAddress());
			}
		}
		
		return MiscUtils.implode(sRecipients, ", ");
	}

}