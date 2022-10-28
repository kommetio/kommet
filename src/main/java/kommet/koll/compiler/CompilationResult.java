/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.koll.compiler;

import java.util.ArrayList;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

public class CompilationResult
{
	private boolean success;
	private List<CompilationError> errors;

	public CompilationResult(List<Diagnostic<? extends JavaFileObject>> diagnostics)
	{
		if (diagnostics == null || diagnostics.isEmpty())
		{
			this.success = true;
		}
		else
		{
			this.success = false;
			this.errors = new ArrayList<CompilationError>();
			for (Diagnostic<? extends JavaFileObject> d : diagnostics)
			{
				this.errors.add(new CompilationError(d));
			}
		}
	}

	public CompilationResult(boolean result)
	{
		this.success = result;
	}

	public void setSuccess(boolean success)
	{
		this.success = success;
	}

	public boolean isSuccess()
	{
		return success;
	}
	
	public void addError ()
	{
		
	}

	public void setErrors(List<CompilationError> errors)
	{
		this.errors = errors;
	}

	public List<CompilationError> getErrors()
	{
		return errors;
	}

	public String getDescription()
	{
		StringBuilder desc = new StringBuilder();
		if (this.success)
		{
			desc.append("Compilation successful");
			return desc.toString();
		}
		else
		{
			desc.append("Compilation failed\n");
			for (CompilationError error : this.errors)
			{
				desc.append("Line ").append(error.getLine()).append(": ").append(error.getMessage()).append("\n");
			}
			
			return desc.toString();
		}
	}
}