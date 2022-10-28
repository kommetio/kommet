/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.koll;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.springframework.util.StringUtils;

import kommet.basic.types.SystemTypes;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.koll.compiler.KommetClassLoader;
import kommet.utils.MiscUtils;

public class Intellisense
{
	private KommetClassLoader classLoader;
	
	public Intellisense (KommetClassLoader classLoader)
	{
		this.classLoader = classLoader;
	}
	
	public Set<String> getHints(String code, String varName, String methodName, int line, int position, EnvData env) throws KollParserException
	{
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		parser.setStatementsRecovery(true);
		parser.setBindingsRecovery(true);
		parser.setResolveBindings(true);
		parser.setUnitName("AnyName.java");
		
		parser.setCompilerOptions(JavaCore.getOptions());
		parser.setEnvironment(new String[] { env.getCompileClassPath() }, null, null, true);
		
		CodeModificationResult res = stripVarName(code, line, position);
		code = res.getCode();
		
		parser.setSource(code.toCharArray());
		
		final CompilationUnit cu = (CompilationUnit)parser.createAST(null);
		
		AST ast = cu.getAST();
		ASTRewrite rewrite = ASTRewrite.create(ast);
		Document document = new Document(code);
		
		cu.accept(getPreparingVisitoror(env, ast, cu, varName, rewrite, null));
		
		// get text edits (updates to source code) created by the ASTRewriter
		TextEdit edits = rewrite.rewriteAST(document, null);

		try
		{
			// apply updates to the source code document
			edits.apply(document);
		}
		catch (MalformedTreeException e)
		{
			throw new KollParserException("Could not rewrite class code. Nested: " + e.getMessage());
		}
		catch (BadLocationException e)
		{
			throw new KollParserException("Could not rewrite class code. Nested: " + e.getMessage());
		}
		
		// after the preparing visitor has replaced type declarations, we can run the visitor that looks for hints
		
		// return source code with updated packages
		
		// assign updated code to parser
		parser = ASTParser.newParser(AST.JLS8);
		parser.setStatementsRecovery(true);
		parser.setBindingsRecovery(true);
		parser.setResolveBindings(true);
		parser.setUnitName("AnyName1.java");
		parser.setCompilerOptions(JavaCore.getOptions());
		parser.setEnvironment(new String[] { env.getCompileClassPath() }, null, null, true);
		parser.setSource(document.get().toCharArray());
		
		final CompilationUnit newCU = (CompilationUnit)parser.createAST(null);
		ast = newCU.getAST();
		rewrite = ASTRewrite.create(ast);
		
		Set<String> hints = new HashSet<String>();
		cu.accept(getHintVisitoror(env, ast, newCU, varName, methodName, line - res.getEmptyLinesBeforeToken(), position, rewrite, hints));
		
		return hints;
	}
	
	/**
	 * Removes everything after the last dot from the current variable name.
	 * E.g. "bee." becomes "bee", and "bee.set" also becomes "bee".
	 * @param code
	 * @param line
	 * @param position
	 * @return the modified Java code
	 */
	private CodeModificationResult stripVarName(String code, int line, int position)
	{
		List<String> codeLines = MiscUtils.splitByNewLine(code);
		
		Integer emptyLines = 0;
		
		// we expect the token to be a variable name ending with a dot, and we need to remove this dot so that the ASTParser can parse the variable
		String modifiedLine = codeLines.get(line);
		int variableEndPosition = position - 1;
		while (position >= 0 && codeLines.get(line).charAt(variableEndPosition) != '.')
		{
			variableEndPosition--;
		}
		modifiedLine = codeLines.get(line).substring(0, variableEndPosition) + codeLines.get(line).substring(position);
		
		List<String> newLines = new ArrayList<String>();
		for (int i = 0; i < codeLines.size(); i++)
		{
			if (i < line && !StringUtils.hasText(codeLines.get(i).trim()))
			{
				emptyLines++;
			}
			
			if (i != line)
			{
				newLines.add(codeLines.get(i));
			}
			else
			{
				newLines.add(modifiedLine);
			}
		}
		
		CodeModificationResult res = new CodeModificationResult();
		res.setCode(MiscUtils.implode(newLines, "\n"));
		res.setEmptyLinesBeforeToken(emptyLines);
		
		return res;
	}

	private ASTVisitor getHintVisitoror(final EnvData env, final AST ast, final CompilationUnit cu, final String token, final String methodName, final int line, final int pos, final ASTRewrite rewrite, final Set<String> hints)
	{
		return new ASTVisitor()
	    {	
			public boolean visit(SimpleName state)
	        {
				ITypeBinding binding = (ITypeBinding)state.resolveTypeBinding();
				int lineNumber = cu.getLineNumber(state.getStartPosition());
				
				if (state.toString().equals(token))
				{
					if (binding != null && binding.getTypeDeclaration() != null)
					{
						for (IMethodBinding m : binding.getTypeDeclaration().getDeclaredMethods())
						{
							if (Modifier.isPublic(m.getModifiers()) && (!StringUtils.hasText(methodName) || m.getName().toLowerCase().startsWith(methodName.toLowerCase())))
							{	
								List<String> args = new ArrayList<String>();
								
								if (m.getMethodDeclaration().getParameterTypes().length > 0)
								{
									for (ITypeBinding argBinding : m.getMethodDeclaration().getParameterTypes())
									{
										args.add("\"" + argBinding.getTypeDeclaration().getQualifiedName() + "\"");
									}
								}
								
								hints.add("{ \"name\": \"" + m.getName() + "\", \"args\": [ " + MiscUtils.implode(args, ", ") + "] }");
							}
						}
					}
				}
				
				return true;
	        }
	    };
	}

	private ASTVisitor getPreparingVisitoror(final EnvData env, final AST ast, final CompilationUnit cu, final String token, final ASTRewrite rewrite, List<Integer> lineNumbers)
	{
		return new ASTVisitor()
	    {
			public boolean visit(QualifiedName state)
	        {
				String qualifiedName = state.getFullyQualifiedName();
				
				try
				{
					if (!MiscUtils.isEnvSpecific(qualifiedName))
					{
						if (env.getType(qualifiedName) != null)
						{
							kommet.data.Type type = env.getType(qualifiedName);
							
							// do not translate system types, unless they are annotated with @Entity and are accessible to users
							if (!type.isBasic() || !SystemTypes.isInaccessibleSystemType(type))
							{
								String updatedPackageName = MiscUtils.userToEnvPackage(type.getPackage(), env);
								rewrite.replace(state, ast.newQualifiedName(ast.newName(updatedPackageName), ast.newSimpleName(type.getApiName())), null);
							}
						}
						else
						{
							// try to convert the class name to env-specific and see, it such class exists on the env
							String updatedPackageName = MiscUtils.userToEnvPackage(qualifiedName, env);
							java.lang.Class<?> cls;
							try
							{
								cls = java.lang.Class.forName(updatedPackageName, true, classLoader);
								
								// if ClassNotFoundException is not thrown, this means that the class exists
								rewrite.replace(state, ast.newQualifiedName(ast.newName(cls.getPackage().getName()), ast.newSimpleName(cls.getSimpleName())), null);
							}
							catch (ClassNotFoundException e)
							{
								// ignore
							}
						}
					}
				}
				catch (KommetException e)
				{
					e.printStackTrace();
				}
				
	            return true;
	        }
	    };
	}
	
	class CodeModificationResult
	{
		private String code;
		private Integer emptyLinesBeforeToken;
		
		public String getCode()
		{
			return code;
		}
		
		public void setCode(String code)
		{
			this.code = code;
		}
		
		public Integer getEmptyLinesBeforeToken()
		{
			return emptyLinesBeforeToken;
		}
		
		public void setEmptyLinesBeforeToken(Integer emptyLinesBeforeToken)
		{
			this.emptyLinesBeforeToken = emptyLinesBeforeToken;
		}
	}
}