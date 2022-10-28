/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.utils;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class JavaCompilerUtils
{
	public static CompilationUnit getCompilationUnit (String javaSource)
	{
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setSource(javaSource.toCharArray());
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		return (CompilationUnit)parser.createAST(null);
	}
}