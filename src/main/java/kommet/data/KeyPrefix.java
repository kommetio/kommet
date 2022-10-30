/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

import kommet.utils.BaseConverter;
import kommet.utils.MiscUtils;

public class KeyPrefix
{
	public static final int LENGTH = 3;
	
	private String prefix;
	
	public static KeyPrefix get (String prefix) throws KeyPrefixException
	{
		if (prefix == null)
		{
			throw new KeyPrefixException("String to get key prefix is null");
		}
		
		if (prefix.length() != 3)
		{
			throw new KeyPrefixException("String " + prefix + " is not a valid key prefix because its length is not three characters");
		}
		return new KeyPrefix(prefix);
	}
	
	public KeyPrefix (String prefix)
	{
		this.prefix = prefix;
	}

	public void setPrefix(String prefix)
	{
		this.prefix = prefix;
	}

	public String getPrefix()
	{
		return prefix;
	}
	
	@Override
	public String toString()
	{
		return this.prefix;
	}

	public static KeyPrefix get (Long sequence)
	{
		String prefix = BaseConverter.convertToKommetBase(sequence);
		return new KeyPrefix(MiscUtils.padLeft(prefix, 3, '0'));
	}
	
	@Override
	public boolean equals (Object obj)
	{
		return obj != null && (obj instanceof KeyPrefix) && ((KeyPrefix)obj).getPrefix().equals(getPrefix());
	}
}