/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.util.StringUtils;

import kommet.basic.keetle.BaseController;
import kommet.businessprocess.annotations.BusinessAction;
import kommet.businessprocess.annotations.Execute;
import kommet.businessprocess.annotations.Input;
import kommet.businessprocess.annotations.Output;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.koll.KollUtil;
import kommet.koll.annotations.Controller;
import kommet.koll.annotations.triggers.BeforeDelete;
import kommet.koll.annotations.triggers.BeforeInsert;
import kommet.koll.annotations.triggers.BeforeUpdate;
import kommet.koll.annotations.triggers.Trigger;
import kommet.triggers.DatabaseTrigger;

public class CodeUtils
{
	public static String[] javaKeywords = { "abstract", "continue", "for", "new", "switch", "default", "package", "synchronized", "boolean", "do", "if", "private", "this", "break", "double", "implements", "protected", "throw",	"byte", "else", "import", "public", "throws", "case", "enum", "instanceof", "return", "transient", "catch", "extends", "int", "short", "try", "char", "final", "interface", "static", "void", "class", "finally", "long", "volatile", "float", "super", "while" };
	
	public static List<String> splitByLine(String s)
	{
		return Arrays.asList(s.split("\\r?\\n"));
	}
	
	public static List<String> getCollapsibleSections(String src)
	{
		List<String> lines = CodeUtils.splitByLine(src);
		
		Integer firstImport = null;
		Integer lastImport = null;
		
		// find lines containing imports
		int index = 0;
		for (String line : lines)
		{
			if (line.trim().startsWith("import "))
			{
				if (firstImport == null)
				{
					firstImport = index;
					lastImport = index;
				}
				lastImport++;
			}
			
			index++;
		}
		
		List<String> collapsedSections = new ArrayList<String>();
		collapsedSections.add(firstImport + "-" + lastImport);
		return collapsedSections;
	}

	public static String getCollapsibleSectionsJSON(List<String> collapsedSections)
	{
		List<String> sections = new ArrayList<String>();
		
		for (String section : collapsedSections)
		{
			String[] lines = section.split("\\-");
			sections.add("{ \"start\": " + lines[0] + ", \"end\": " + lines[1] + " }");
		}
		
		return "\"collapse\": [ " + MiscUtils.implode(sections, ", ") + " ]";
	}

	public static String getClassTemplate(String qualifiedName) throws KommetException
	{
		String packageName = "kommet.example";
		String className = "MyClass";
		
		if (StringUtils.hasText(qualifiedName))
		{
			List<String> nameParts = MiscUtils.splitByLastDot(qualifiedName);
			packageName = nameParts.get(0);
			className = nameParts.get(1);
		}
		
		StringBuilder code = new StringBuilder();
		code.append("package ").append(packageName).append(";\n\n");
		
		// add imports
		code.append(MiscUtils.implode(KollUtil.getImports(), ";\n", null, "import ") + ";\n");
		code.append("\n\n");
		
		// create class
		code.append("public class ").append(className).append("\n");
		code.append("{\n");
		
		// end class
		code.append("}");
		
		return code.toString();
	}
	
	public static String getControllerTemplate(String qualifiedName) throws KommetException
	{
		String packageName = "kommet.example";
		String className = "MyController";
		
		if (StringUtils.hasText(qualifiedName))
		{
			List<String> nameParts = MiscUtils.splitByLastDot(qualifiedName);
			packageName = nameParts.get(0);
			className = nameParts.get(1);
		}
		
		StringBuilder code = new StringBuilder();
		code.append("package ").append(packageName).append(";\n\n");
		
		// add imports
		code.append(MiscUtils.implode(KollUtil.getImports(), ";\n", null, "import ") + ";\n");
		code.append("\n\n");
		
		// create class
		code.append("@").append(Controller.class.getSimpleName()).append("\n");
		code.append("public class ").append(className).append(" extends ").append(BaseController.class.getSimpleName()).append("\n");
		code.append("{\n");
		
		// end class
		code.append("}");
		
		return code.toString();
	}
	
	public static String getViewTemplate(String qualifiedName) throws KommetException
	{
		String packageName = "kommet.views";
		String viewName = "MyView";
		
		if (StringUtils.hasText(qualifiedName))
		{
			List<String> nameParts = MiscUtils.splitByLastDot(qualifiedName);
			packageName = nameParts.get(0);
			viewName = nameParts.get(1);
		}
		
		StringBuilder code = new StringBuilder();
		code.append("<km:view name=\"").append(viewName).append("\" package=\"").append(packageName).append("\">\n");
		
		code.append(MiscUtils.indent(1)).append("<!-- view code -->\n");
		
		// end class
		code.append("</km:view>");
		
		return code.toString();
	}
	
	public static String getLayoutTemplate(String qualifiedName) throws KommetException
	{
		String packageName = "kommet.layouts";
		String layoutName = "MyLayout";
		
		if (StringUtils.hasText(qualifiedName))
		{
			List<String> nameParts = MiscUtils.splitByLastDot(qualifiedName);
			packageName = nameParts.get(0);
			layoutName = nameParts.get(1);
		}
		
		StringBuilder code = new StringBuilder();
		code.append("<km:layout name=\"").append(layoutName).append("\" package=\"").append(packageName).append("\">");
		
		int indent = 1;
		
		code.append(MiscUtils.indent(indent)).append("<km:beforeContent></km:beforeContent>\n");
		code.append(MiscUtils.indent(indent)).append("<km:afterContent></km:afterContent>\n");
		
		indent--;
		
		// end class
		code.append("</km:layout>");
		
		return code.toString();
	}
	
	public static String getBusinessActionTemplate(String qualifiedName) throws KommetException
	{
		String packageName = "kommet.example";
		String className = "MyBusinessAction";
		
		if (StringUtils.hasText(qualifiedName))
		{
			List<String> nameParts = MiscUtils.splitByLastDot(qualifiedName);
			packageName = nameParts.get(0);
			className = nameParts.get(1);
		}
		
		StringBuilder code = new StringBuilder();
		code.append("package ").append(packageName).append(";\n\n");
		
		int indent = 0;
		
		// add imports
		code.append(MiscUtils.implode(KollUtil.getImports(), ";\n", null, "import ") + ";\n");
		code.append("\n\n");
		
		// create class
		code.append("@").append(BusinessAction.class.getSimpleName()).append("(name = \"ActionName\", description = \"Action description\")").append("\n");
		code.append("public class ").append(className).append("\n");
		code.append("{\n");
		
		indent++;
		code.append(MiscUtils.indent(indent)).append("@").append(Input.class.getSimpleName()).append("(name = \"actionInput\")\n");
		code.append(MiscUtils.indent(indent)).append("public void setActionInput(String input)\n");
		code.append(MiscUtils.indent(indent)).append("{\n");
		code.append(MiscUtils.indent(indent)).append("}\n\n");
		
		code.append(MiscUtils.indent(indent)).append("@").append(Output.class.getSimpleName()).append("(name = \"actionOutput\")\n");
		code.append(MiscUtils.indent(indent)).append("public String setActionOutput()\n");
		code.append(MiscUtils.indent(indent)).append("{\n");
		
		indent++;
		code.append(MiscUtils.indent(indent)).append("return null;\n");
		indent--;
		
		code.append(MiscUtils.indent(indent)).append("}\n\n");
		
		code.append(MiscUtils.indent(indent)).append("@").append(Execute.class.getSimpleName()).append("()\n");
		code.append(MiscUtils.indent(indent)).append("public void execute()\n");
		code.append(MiscUtils.indent(indent)).append("{\n");
		
		indent++;
		code.append(MiscUtils.indent(indent)).append("// TODO add own action code here\n");
		indent--;
		
		code.append(MiscUtils.indent(indent)).append("}\n\n");
		
		// end class
		code.append("}");
		
		return code.toString();
	}
	
	public static String getTriggerTemplate(String qualifiedName, Type type) throws KommetException
	{
		String packageName = "kommet.example";
		String typeApiName = type != null ? type.getApiName() : "User";
		String typeName = type != null ? type.getQualifiedName() : "kommet.basic.User";
		
		String className = typeApiName + "Trigger";
		
		if (StringUtils.hasText(qualifiedName))
		{
			List<String> nameParts = MiscUtils.splitByLastDot(qualifiedName);
			packageName = nameParts.get(0);
			className = nameParts.get(1);
		}
		
		StringBuilder code = new StringBuilder();
		code.append("package ").append(packageName).append(";\n\n");
		
		int indent = 0;
		
		// add imports
		code.append(MiscUtils.implode(KollUtil.getImports(), ";\n", null, "import ") + ";\n");
		code.append("import ").append(type.getQualifiedName()).append(";\n");
		code.append("\n\n");
		
		// create class
		code.append("@").append(Trigger.class.getSimpleName()).append("(type = \"").append(typeName).append("\")\n");
		code.append("@").append(BeforeInsert.class.getSimpleName()).append("\n");
		code.append("@").append(BeforeUpdate.class.getSimpleName()).append("\n");
		code.append("@").append(BeforeDelete.class.getSimpleName()).append("\n");
		code.append("public class ").append(className).append(" extends ").append(DatabaseTrigger.class.getSimpleName()).append("<").append(typeName).append(">\n");
		code.append("{\n");
		
		indent++;
		
		code.append(MiscUtils.indent(indent)).append("public void execute()\n");
		code.append(MiscUtils.indent(indent)).append("{\n");
		
		indent++;
		code.append(MiscUtils.indent(indent)).append("for (").append(typeApiName).append(" record : getNewValues())\n");
		code.append(MiscUtils.indent(indent)).append("{\n");
		code.append(MiscUtils.indent(indent)).append("}\n");
		
		indent--;
		// close method
		code.append(MiscUtils.indent(indent)).append("}\n");
		indent--;
		
		// end class
		code.append("}");
		
		return code.toString();
	}
}