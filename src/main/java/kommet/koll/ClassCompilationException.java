/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.koll;

import kommet.data.KommetException;
import kommet.koll.compiler.CompilationResult;

public class ClassCompilationException extends KommetException
{
	private static final long serialVersionUID = 3059368072160089223L;
	private CompilationResult result;

	public ClassCompilationException(String msg)
	{
		super(msg);
	}
	
	public ClassCompilationException(String msg, CompilationResult result)
	{
		super(msg);
		this.result = result;
	}
	
	@Override
	public String getMessage()
	{
		return super.getMessage() + (this.result != null ? ": " + this.result.getDescription() : "");
	}
	
	public CompilationResult getCompilationResult()
	{
		return this.result;
	}
}