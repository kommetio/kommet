/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.koll;

public class AstIllegalArgumentException extends IllegalArgumentException
{
	private static final long serialVersionUID = -2850588364280179111L;
	
	private boolean isCompiledError; 
	
	public AstIllegalArgumentException (String msg, boolean isCompiledError)
	{
		this.isCompiledError = isCompiledError;
	}
	
	public boolean isCompiledError()
	{
		return this.isCompiledError;
	}
}