/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.rel;

public class TranslatedVar
{
	private String var;
	private boolean isField;
	
	public TranslatedVar (String var, boolean isField)
	{
		this.var = var;
		this.isField = isField;
	}
	
	public String getVar()
	{
		return var;
	}
	public void setVar(String var)
	{
		this.var = var;
	}
	public boolean isField()
	{
		return isField;
	}
	public void setField(boolean isField)
	{
		this.isField = isField;
	}
}