/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.koll.compiler;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

public class CompilationError
{
	private String message;
	private Long line;
	public CompilationError(Diagnostic<? extends JavaFileObject> d)
	{
		this.line = d.getLineNumber();
		this.message = d.getMessage(null);
		if (this.message.startsWith("string:///"))
		{
			this.message = this.message.substring(10);
		}
	}
	
	public String getMessage()
	{
		return message;
	}
	
	public Long getLine()
	{
		return line;
	}

}