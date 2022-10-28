/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data.validationrules;

import kommet.koll.compiler.CompilationResult;

public class ValidationRuleCompileException extends ValidationRuleException
{
	private static final long serialVersionUID = 3621551145076848982L;
	private CompilationResult compilationResult;
	
	public ValidationRuleCompileException(CompilationResult result)
	{
		super("Error compiling validation rule");
		this.compilationResult = result;
	}
	
	public CompilationResult getCompilationResult()
	{
		return this.compilationResult;
	}
}