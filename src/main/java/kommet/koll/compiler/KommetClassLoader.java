/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.koll.compiler;

import java.net.URL;
import java.net.URLClassLoader;

import kommet.env.EnvData;
import kommet.koll.SystemContextFactory;

public class KommetClassLoader extends URLClassLoader
{
	private ClassLoader parentClassLoader;
	private SystemContextFactory sysCtxFactory;
	private EnvData env;
	
	// TODO - perhaps using a parent class loader is not necessary at all since
	// method KommetCompiler.getClass uses method Class.forName with a class loader
	// as param anyway
	public KommetClassLoader (URL[] urls, ClassLoader parentClassLoader, SystemContextFactory sysCtxFactory, EnvData env)
	{
		super(urls);
		this.parentClassLoader = parentClassLoader;
		this.sysCtxFactory = sysCtxFactory;
		this.env = env;
	}

	public static KommetClassLoader newInstance(final URL[] urls, ClassLoader parentClassLoader, SystemContextFactory sysCtxFactory, EnvData env)
	{
		return new KommetClassLoader(urls, parentClassLoader, sysCtxFactory, env);
	}
	
	@Override
	public Class<?> loadClass (String name) throws ClassNotFoundException
	{
		try
		{
			Class<?> cls = super.loadClass(name);
			KommetCompiler.injectSystemContext(cls, this.sysCtxFactory.get(null, env));
			return cls;
		}
		catch (ClassNotFoundException e)
		{
			// if class not found, try with the parent class loader
			return this.parentClassLoader.loadClass(name);
		}
	}
	
	public Class<?> reloadClass(String name) throws ClassNotFoundException
	{
		try
		{
			Class<?> cls = super.findClass(name);
			return cls;
		}
		catch (ClassNotFoundException e)
		{
			// if class not found, try with the parent class loader
			return this.parentClassLoader.loadClass(name);
		}
	}
}