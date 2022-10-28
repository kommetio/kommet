/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.koll.compiler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import kommet.basic.Class;
import kommet.basic.SystemContextAware;
import kommet.basic.keetle.BaseController;
import kommet.basic.keetle.PageData;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.koll.ClassCompilationException;
import kommet.koll.JavaSource;
import kommet.koll.SystemContext;
import kommet.koll.SystemContextFactory;
import kommet.koll.annotations.Action;
import kommet.koll.annotations.ActionConfig;
import kommet.koll.annotations.Controller;
import kommet.koll.annotations.CrossOrigin;
import kommet.koll.annotations.Param;
import kommet.koll.annotations.Params;
import kommet.koll.annotations.ResponseBody;
import kommet.koll.annotations.Rest;
import kommet.koll.annotations.ReturnsFile;
import kommet.koll.annotations.SystemContextVar;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.PropertyUtilException;

/**
 * This class exposes different methods for accessing and compiling classes for a given environment.
 * 
 * It contains a different class loader for each environment that has access only to the given environment's
 * classes and shared libraries. This way we can be sure KOLL files will not access any unauthorized classes
 * e.g. from the virtual machine or app container.
 *  
 * @author Radek Krawiec
 * @created 10-07-2013
 *
 */
@Service
public class KommetCompiler
{
	/**
	 * The base package under which all environment specific files will be placed.
	 * Env ID will be appended to this package to create the base package name for a given env.
	 * E.g. for env with ID 3dhsa89 the base package will be kommet.env.3dhsa89.
	 * 
	 * This part of the package will not be visible to users. If they create a package
	 * com.wordgraphs, its actual name will be kommet.env.3dhsa89.com.wordgraphs.
	 * 
	 * This way we will avoid potential name clashes between envs (though they should not happen
	 * due to the use of different classloaders for different envs).
	 */
	public static final String KOLL_BASE_PACKAGE = "kommet.envs";
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	SystemContextFactory sysCtxFactory;
	
	/**
	 * List of classes that need to be imported at the beginning of every Koll controller.
	 */
	private List<java.lang.Class<?>> controllerImports;
	
	/**
	 * Map of class loaders for each environment.
	 * 
	 * We must keep the class loader for the given environment, cannot instantiate it anew every time
	 * it is referenced. If we did that, we might run into the following issue:
	 * - A class is compiled using class loader instance #1 and it contains the @Controller annotation
	 * - In another call it is checked if the class contains this annotation using class loader #2. Unfortunately,
	 * the hasAnnotation(X.class) method checks for the very same instance of class X passed as a parameter. In different classloaders
	 * we would be checking different instances of X.class, and would get hasAnnotation(X.class) == null.
	 */
	private Map<KID, KommetClassLoader> classLoadersByEnvId = new HashMap<KID, KommetClassLoader>();
	
	private static final Logger log = LoggerFactory.getLogger(KommetCompiler.class);
	
	public KommetCompiler()
	{
		// init imports
		this.controllerImports = new ArrayList<java.lang.Class<?>>();
		this.controllerImports.add(Controller.class);
		this.controllerImports.add(Action.class);
		this.controllerImports.add(ActionConfig.class);
		this.controllerImports.add(Param.class);
		this.controllerImports.add(Params.class);
		this.controllerImports.add(Rest.class);
		this.controllerImports.add(CrossOrigin.class);
		this.controllerImports.add(ResponseBody.class);
		this.controllerImports.add(ReturnsFile.class);
		this.controllerImports.add(PageData.class);
		this.controllerImports.add(BaseController.class);
		this.controllerImports.add(Type.class);
		this.controllerImports.add(KommetException.class);
		this.controllerImports.add(SystemContextAware.class);
		this.controllerImports.add(SystemContext.class);
	}
	
	public KommetClassLoader getClassLoader (EnvData env) throws KommetException
	{
		return getClassLoader(env, false);
	}
	
	/**
	 * Get or create a class loader for the given environment.
	 * @param env
	 * @return
	 * @throws  
	 * @throws PropertyUtilException
	 * @throws MalformedURLException
	 */
	private KommetClassLoader getClassLoader (EnvData env, boolean reload) throws KommetException
	{
		if (!this.classLoadersByEnvId.containsKey(env.getId()) || reload)
		{
			File envKollRoot = new File(appConfig.getKollDir() + "/" + env.getId());
			File libDir = new File(appConfig.getLibDir());
			
			KommetClassLoader classLoader;
			try
			{
				List<URL> urls = new ArrayList<URL>();
				urls.add(envKollRoot.toURI().toURL());
				urls.add(libDir.toURI().toURL());
				
				List<File> libFiles = new ArrayList<File>();
				listJarFiles(appConfig.getLibDir(), libFiles);
				for (File f : libFiles)
				{
					urls.add(f.toURI().toURL());
				}

				// inject user-agnostic sysctx, i.e. one with null auth data, because it will be reused for many users
				classLoader = KommetClassLoader.newInstance(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader(), sysCtxFactory, env);
			}
			catch (MalformedURLException e)
			{
				throw new KommetException("Error instantiating class: " + e.getMessage(), e);
			}
			
			this.classLoadersByEnvId.put(env.getId(), classLoader);
			return classLoader;
		}
		else
		{
			return this.classLoadersByEnvId.get(env.getId());
		}
	}

	public java.lang.Class<?> getClass (Class cls, boolean reload, EnvData env) throws ClassNotFoundException, MalformedURLException, KommetException
	{
		if (!StringUtils.hasText(cls.getPackageName()))
		{
			throw new KommetException("Property package on file " + cls.getName() + " is empty");
		}
		
		KommetClassLoader classLoader = getClassLoader(env);
		
		if (reload)
		{
			classLoader.reloadClass(MiscUtils.userToEnvPackage(cls.getQualifiedName(), env));
		}
		
		return java.lang.Class.forName(MiscUtils.userToEnvPackage(cls.getQualifiedName(), env), true, getClassLoader(env)); 
	}
	
	/**
	 * Gets a class object with the specified name from the Kommet compiler classpath.
	 * 
	 * This method may be useful when two versions of the same class exist, one in the apps main
	 * classpath, and another in the compiler classpath.
	 * 
	 * @param name the name of the class (with package)
	 * @return
	 * @throws KommetException 
	 * @throws ClassNotFoundException 
	 */
	public java.lang.Class<?> getClass (String name, boolean convertToEnvSpecific, EnvData env) throws KommetException, ClassNotFoundException
	{
		if (!name.contains("."))
		{
			throw new KommetException("The name of the class '" + name + "' is not qualified");
		}
		
		if (convertToEnvSpecific)
		{
			name = MiscUtils.userToEnvPackage(name, env);
		}
		
		return java.lang.Class.forName(name, true, getClassLoader(env));	
	}
	
	public Method getMethod (String className, String methodName, EnvData env) throws KommetException
	{
		try
		{
			java.lang.Class<?> cls = java.lang.Class.forName(className, true, getClassLoader(env));
			
			for (Method m : cls.getMethods())
			{
				if (m.getName().equals(methodName))
				{
					return m;
				}
			}
			
			return null;
		}
		catch (Exception e)
		{
			throw new KommetException("Error getting method '" + methodName + "' from class '" + className + "'. Nested: " + e.getMessage()); 
		}
	}

	/**
	 * Destroys and creates anew the class loader for the given environment.
	 * 
	 * This method is useful when we want to reload a class for a given class loader. Since class loaders
	 * don't allow classes to be reloaded, the only way to achieve this is by reinitializing the whole
	 * class loader.
	 * 
	 * @param env
	 * @return
	 * @throws KommetException 
	 * @throws PropertyUtilException
	 * @throws MalformedURLException
	 */
	public KommetClassLoader resetClassLoader(EnvData env) throws KommetException
	{
		return getClassLoader(env, true);
	}
	
	/**
	 * Returns the imports that must be placed at the beginning of every controller file.
	 * @return
	 */
	public String getControllerImportsSection()
	{
		StringBuilder imports = new StringBuilder();
		
		for (java.lang.Class<?> cls : this.controllerImports)
		{
			imports.append("import " + cls.getName() + ";\n");
		}
		
		return imports.toString();
	}
	
	public CompilationResult compile (Class file, EnvData env) throws KommetException
	{
		List<Class> files = new ArrayList<Class>();
		files.add(file);
		return compile(files, env);
	}
	
	public void deleteCompiledFiles (Collection<Class> files, EnvData env) throws KommetException
	{
		// make sure a KOLL dir for this env exists
		File sourceDirObj = new File(appConfig.getKollDir() + "/" + env.getId());
		if (sourceDirObj.exists())
		{
			if (!sourceDirObj.isDirectory())
			{
				throw new ClassCompilationException("Koll file directory \"" + appConfig.getKollDir() + "/" + env.getId() + "\" for env " + env.getName() + " exists but is not a directory.");
			}
			
			// remove the old version of the class (if exists)
			// TODO perhaps this could be done by properly configuring the compile task or file manager?
			for (Class file : files)
			{
				deleteCompiledClass(sourceDirObj, file.getPackageName(), file.getName());
			}
		}
	}
	
	public CompilationResult compile (Collection<Class> files, EnvData env) throws KommetException
	{	
		List<JavaSource> javaSources = new ArrayList<JavaSource>();
		
		for (Class file : files)
		{
			if (file.getName().contains("Query"))
			{
				file.getName();
			}
			
			if (!StringUtils.hasText(file.getPackageName()))
			{
				throw new KommetException("Package property is empty on koll file passed for compilation");
			}
			
			javaSources.add(new JavaSource(file.getName(), file.getJavaCode()));
		}

		Iterable<? extends JavaFileObject> compilationUnits = javaSources;
		String sourceDir = null;
		String compileClassPath = null;
		
		try
		{
			sourceDir = appConfig.getKollDir() + "/" + env.getId();
			
			// make sure a KOLL dir for this env exists
			File sourceDirObj = new File(sourceDir);
			if (sourceDirObj.exists())
			{
				if (!sourceDirObj.isDirectory())
				{
					throw new ClassCompilationException("KOLL file directory \"" + sourceDir + "\" for env " + env.getName() + " exists but is not a directory.");
				}
				
				// remove the old version of the class (if exists)
				// TODO perhaps this could be done by properly configuring the compile task or file manager?
				for (Class file : files)
				{
					deleteCompiledClass(sourceDirObj, file.getPackageName(), file.getName());
				}
			}
			else
			{
				// create dir
				sourceDirObj.mkdir();
			}
			
			// the compilation class path includes two elements: the basic class path for all envs and the env-specific class path
			compileClassPath = getCompileClassPath(env.getId());
			
			// make sure the compile class path exists
			validateClassPath(compileClassPath, appConfig.getClasspathSeparator());
		}
		catch (PropertyUtilException e)
		{
			throw new ClassCompilationException("Error reading property: " + e.getMessage());
		}
		
		/* Prepare any compilation options to be used during compilation */
		// The classpath option is necessary for the compiler to see kommet.* files. 
		String[] compileOptions = new String[] { "-d", sourceDir, "-classpath", compileClassPath };
		
		Iterable<String> compilationOptions = Arrays.asList(compileOptions);

		// Create a diagnostic controller, which holds the compilation problems
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		
		if (compiler == null)
		{
			throw new KommetException("Compiler object is null. Perhaps using JRE instead of JDK. Java Home is: " + System.getProperty("java.home") + ", Java version: " + System.getProperty("java.version"));
		}
		
		StandardJavaFileManager stdFileManager = compiler.getStandardFileManager(null, Locale.getDefault(), null);
		
		// Create a compilation task from compiler by passing in the required input objects prepared above
		CompilationTask compilerTask = compiler.getTask(null, stdFileManager, diagnostics, compilationOptions, null, compilationUnits);
		
		// Perform the compilation by calling the call method on compilerTask object.
		boolean status = compilerTask.call();
		
		try
		{
			stdFileManager.close(); // Close the file manager
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		if (!status)
		{
			return new CompilationResult(diagnostics.getDiagnostics());
		}
		else
		{
			return new CompilationResult(true);
		}
	}
	
	private void deleteCompiledClass(File sourceDir, String packageName, String className)
	{
		String classFilePath = sourceDir.getAbsolutePath() + "/" + packageName.replaceAll("\\.", "/") + "/" + className + ".class";
		File classFile = new File(classFilePath);
		if (classFile.exists() && classFile.isFile())
		{
			classFile.delete();
		}
	}

	private void validateClassPath (String compileClassPath, String separator) throws ClassCompilationException
	{
		if (compileClassPath == null)
		{
			throw new ClassCompilationException("Class path is empty");
		}
		
		String[] pathParts = compileClassPath.split(separator);
		
		for (int i = 0; i < pathParts.length; i++)
		{
			String pathPart = pathParts[i];
			
			// .jar import paths end with "/*", which has to be removed before we check if this directory exists
			if (pathPart.endsWith("/*"))
			{
				pathPart = pathPart.substring(0, pathPart.length() -2);
			}
			
			File dir = new File(pathPart);
			if (!dir.exists() || (!dir.isDirectory() && !pathPart.endsWith(".jar") && !pathPart.endsWith(".class")))
			{
				throw new ClassCompilationException("Class path element " + pathPart + " does not exist or is of invalid type. Class path elements must be directories or jar/class files.");
			}
		}
	}

	/**
	 * Deletes all cached KOLL files for the given environment 
	 * @throws KommetException 
	 */
	public void clearKollCache(EnvData env) throws KommetException
	{
		File dir = new File(appConfig.getKollDir() + "/" + env.getId().getId());
		if (dir.exists())
		{
			log.debug("Clearing KOLL directory " + dir.getAbsolutePath());
			try
			{
				FileUtils.deleteDirectory(dir);
			}
			catch (IOException e)
			{
				throw new KommetException("Error clearing KOLL directory: " + e.getMessage());
			}
		}
		dir.mkdir();
	}

	public List<java.lang.Class<?>> findSubclasses(List<Class> classes, java.lang.Class<?> superclass, EnvData env) throws KommetException
	{
		List<java.lang.Class<?>> foundClasses = new ArrayList<java.lang.Class<?>>();
		
		for (Class cls : classes)
		{
			java.lang.Class<?> clsObject;
			try
			{
				clsObject = this.getClass(cls, false, env);
			}
			catch (Exception e)
			{
				throw new KommetException("Could not find class " + cls.getQualifiedName() + " on env");
			}
			
			if (superclass.isAssignableFrom(clsObject))
			{
				foundClasses.add(clsObject);
			}
		}
		
		return foundClasses;
	}

	/**
	 * Gets all .class and .jar files from the given directory and its subdirectories
	 * @param dir
	 * @param files
	 */
	private void listJarFiles (String dir, List<File> files)
	{
		File directory = new File(dir);

		// Get all files from a directory.
		File[] fList = directory.listFiles();
		if (fList != null)
		{
			for (File file : fList)
	{
				// list only jar files
				if (file.isFile() && "jar".equals(FilenameUtils.getExtension(file.getName()).toLowerCase()))
				{
					files.add(file);
				}
				else if (file.isDirectory())
				{
					listJarFiles(file.getAbsolutePath(), files);
				}
			}
		}
	}

	public String getCompileClassPath (KID envId) throws PropertyUtilException
	{
		List<File> libFiles = new ArrayList<File>();
		listJarFiles(appConfig.getLibDir(), libFiles);
		List<String> libFileNames = new ArrayList<String>();
		for (File f : libFiles)
		{
			libFileNames.add(f.getAbsolutePath());
		}
		String libFileList = MiscUtils.implode(libFileNames, appConfig.getClasspathSeparator());
		
		return appConfig.getCompileClasspath() + appConfig.getClasspathSeparator() + appConfig.getKollDir() + "/" + envId + appConfig.getClasspathSeparator() + appConfig.getLibDir() + appConfig.getClasspathSeparator() + libFileList;
	}
	
	public static void injectSystemContext (java.lang.Class<?> cls, SystemContext sysCtx)
	{
		if (cls.isAnnotationPresent(SystemContextVar.class))
		{
			String var = ((SystemContextVar)cls.getAnnotation(SystemContextVar.class)).value();
			try
			{
				Field field = cls.getField(var);
				
				// injecting template system context, i.e. one without auth data
				field.set(null, sysCtx);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				throw new RuntimeException("Nested exception: " + e.getMessage());
			}
		}
	}
}