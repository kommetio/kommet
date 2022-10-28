/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.vendorapis.jexl;

import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlContext;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.MapContext;

public class JexlScript
{
	private JexlEngine engine;
	private JexlContext jc;
	private org.apache.commons.jexl3.JexlScript script;
	
	public JexlScript(String scriptText)
	{
		this.engine = new JexlBuilder().cache(512).strict(true).silent(false).create();
		this.jc = new MapContext();
		this.script = this.engine.createScript(scriptText);
	}
	
	public void set (String prop, Object o)
	{
		this.jc.set(prop, o);
	}
	
	public Object execute()
	{
		return this.script.execute(this.jc);
	}
}