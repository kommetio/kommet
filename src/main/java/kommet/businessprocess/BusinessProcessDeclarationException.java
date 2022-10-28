/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.businessprocess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import kommet.utils.MiscUtils;

public class BusinessProcessDeclarationException extends BusinessProcessException
{
	private static final long serialVersionUID = 2932420497890154047L;
	private List<BusinessActionDeclarationError> errors;
	
	public BusinessProcessDeclarationException(List<BusinessActionDeclarationError> errors)
	{
		super(getConcatenatadMessages(errors));
		this.errors = errors;
	}
	
	public static List<BusinessActionDeclarationError> createErrorList(List<String> errors)
	{
		List<BusinessActionDeclarationError> errorObjs = new ArrayList<BusinessActionDeclarationError>();
		for (String err : errors)
		{
			errorObjs.add(new BusinessActionDeclarationError(err));
		}
		
		return errorObjs;
	}
	
	public BusinessProcessDeclarationException(String err)
	{
		super(getConcatenatadMessages(Arrays.asList(new BusinessActionDeclarationError(err))));
		this.errors = Arrays.asList(new BusinessActionDeclarationError(err));
	}

	private static String getConcatenatadMessages(List<BusinessActionDeclarationError> errors)
	{
		List<String> errorMessages = new ArrayList<String>();
		for (BusinessActionDeclarationError err : errors)
		{
			errorMessages.add(err.getMessage());
		}
		
		return MiscUtils.implode(errorMessages, ", ");
	}

	public List<BusinessActionDeclarationError> getErrors()
	{
		return errors;
	}
}