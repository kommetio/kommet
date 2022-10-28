/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import kommet.basic.MimeTypes;
import kommet.basic.View;
import kommet.basic.ViewResource;
import kommet.basic.actions.StandardActionType;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.rel.VRELParser;
import kommet.utils.AppConfig;

public class ViewUtil
{
	public static final String EMPTY_VIEW_CONTENT = "<!-- insert your view code here -->";
	public static final String VIEW_TAG_NAMESPACE = "km";
	public static final String VIEW_TAG_NAME = "view";
	
	private static final Logger log = LoggerFactory.getLogger(ViewUtil.class);
	
	public static String getStandardListView(Type obj)
	{
		return obj.getKeyPrefix() + "_list";
	}

	public static String getStandardDetailsView(Type obj)
	{
		return obj.getKeyPrefix() + "_details";
	}
	
	/**
	 * Stores the JSP file for this view on disk.
	 * @param view
	 * @param config
	 * @param env
	 * @throws KommetException
	 */
	public static void storeView (View view, AppConfig config, EnvData env) throws KommetException
	{	
		try
		{
			// Store the view on disk. The view's name will consist of its KID + .jsp suffix.
			// This will ensure that when the view's name changes, it will still be saved into the same file on disk.
			FileOutputStream fos = new FileOutputStream(env.getKeetleDir(config.getKeetleDir()) + "/" + view.getId() + ".jsp");
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos, "UTF-8"));
			writer.write(view.getJspCode());
			writer.close();
		}
		catch (IOException e)
		{
			throw new KommetException("Error writing view to disk: " + e.getMessage());
		}
	}
		
	/**
	 * Returns code of javascript and CSS included files needed by km.js.* libraries.
	 * @return
	 */
	// TODO this method seems not to be used anywhere
	public static String includeJSLibraries(String contextPath)
	{
		StringBuilder includes = new StringBuilder();
		includes.append("<script type=\"text/javascript\" src=\"").append(contextPath).append("/km.config.js\"></script>\n");
		includes.append("<script type=\"text/javascript\" src=\"").append(contextPath).append("/km.core.js\"></script>\n");
		includes.append("<script type=\"text/javascript\" src=\"").append(contextPath).append("/km.datasource.js\"></script>\n");
		includes.append("<script type=\"text/javascript\" src=\"").append(contextPath).append("/km.data.js\"></script>\n");
		includes.append("<script type=\"text/javascript\" src=\"").append(contextPath).append("/km.utils.js\"></script>\n");
		includes.append("<script type=\"text/javascript\" src=\"").append(contextPath).append("/km.notifier.js\"></script>\n");
		includes.append("<script type=\"text/javascript\" src=\"").append(contextPath).append("/km.ui.js\"></script>\n");
		includes.append("<script type=\"text/javascript\" src=\"").append(contextPath).append("/km.table.js\"></script>\n");
		includes.append("<script type=\"text/javascript\" src=\"").append(contextPath).append("/km.itemlist.js\"></script>\n");
		includes.append("<script type=\"text/javascript\" src=\"").append(contextPath).append("/km.rel.js\"></script>\n");
		
		// include CSS
		includes.append("<link href=\"").append(contextPath).append("/resources/css/km.ui.css\" rel=\"stylesheet\" type=\"text/css\" />");
		includes.append("<link href=\"").append(contextPath).append("/resources/css/km.table.css\" rel=\"stylesheet\" type=\"text/css\" />");
		
		return includes.toString();
	}
		
	public static String getEmptyViewCode (String viewName, String viewPackage) throws ViewSyntaxException
	{
		return wrapViewCode(EMPTY_VIEW_CONTENT, viewName, viewPackage);
	}

	public static String getStandardDetailsViewCode (Type type, String viewName, String viewPackage) throws ViewSyntaxException
	{
		StringBuilder code = new StringBuilder();
		
		// include details tag
		code.append("\t<km:objectDetails record=\"{{record}}\">\n\t</km:objectDetails>");
		//code.append("</div>");
		return wrapViewCode(code.toString(), viewName, viewPackage);
	}

	public static String getStandardListViewCode(Type type, String viewName, String viewPackage, EnvData env) throws KommetException
	{
		StringBuilder code = new StringBuilder();
		code.append("\t<div class=\"ibox\">\n");
		
		code.append("\t\t<km:errors />\n");
		code.append("\t\t<km:messages />\n\n");
		
		// include details tag
		// not using km:objectList any more
		// code.append("\t\t<km:objectList title=\"").append(type.getPluralLabel()).append("\" type=\"" + type.getQualifiedName() + "\" />");
		
		code.append("\t\t<km:dataTable query=\"select id, {defaultField} from " + type.getQualifiedName() + " order by " + Field.LAST_MODIFIED_DATE_FIELD_NAME + " DESC\" paginationActive=\"true\" pageSize=\"25\" title=\"{{pluralLabel}}\">\n");
		code.append("\t\t\t<km:buttons><km:button type=\"new\" /></km:buttons>\n");
		code.append("\t\t\t<km:dataTableColumn name=\"{{type.defaultFieldApiName}}\" sortable=\"true\" linkStyle=\"true\" url=\"#{pageContext.request.contextPath}/{id}\" />\n");
		code.append("\t\t</km:dataTable>\n\n");
		
		code.append("\t</div>");
		return wrapViewCode(code.toString(), viewName, viewPackage);
	}

	/**
	 * Wraps the view code in the view tag.
	 * @param string
	 * @return
	 * @throws ViewSyntaxException 
	 */
	public static String wrapViewCode (String innerCode, String viewName, String viewPackage) throws ViewSyntaxException
	{
		StringBuilder code = new StringBuilder();
		String viewTag = VIEW_TAG_NAMESPACE + ":" + VIEW_TAG_NAME;
		
		code.append("<" + viewTag + " name=\"").append(viewName).append("\"");
		if (!StringUtils.hasText(viewPackage))
		{
			throw new ViewSyntaxException("View package name is empty");
		}
			
		code.append(" package=\"").append(viewPackage).append("\"");
		code.append(">\n");
		code.append(innerCode).append("\n");
		code.append("</" + viewTag + ">\n");
		return code.toString();
	}
	
	/**
	 * Validates the view code.
	 * @param viewCode
	 * @return
	 */
	public static final boolean isValidView (String viewCode)
	{
		String viewTag = VIEW_TAG_NAMESPACE + ":" + VIEW_TAG_NAME;
		String normalizedCode = viewCode.trim();
		if (!normalizedCode.startsWith("<" + viewTag))
		{
			return false;
		}
		if (!normalizedCode.endsWith("</" + viewTag + ">"))
		{
			return false;
		}
		return true;
	}
	
	/**
	 * Converts KTL code to JSP that can be saved to disk and executed by the container.
	 * The conversion includes:
	 * <ul>
	 * <li>Adding taglibs</li>
	 * <li>Wrapping in viewWrapper system tag</li>
	 * <li>Escaping native JSP variables, i.e. those identified by ${...}</li>
	 * <li>Translating KTL variables (#{var}) to JSP variables referencing controller data: ${pageData.var}
	 * </ul>
	 * @param keetleCode
	 * @param appConfig
	 * @param env
	 * @return
	 * @throws KommetException 
	 */
	public static String keetleToJSP(String keetleCode, AppConfig appConfig, EnvData env) throws KommetException
	{
		StringBuilder code = new StringBuilder(getViewIncludes());
		code.append("<km:viewWrapper>\n");
		code.append(getDefaultLayoutInclude(appConfig.getLayoutRelativeDir() + "/${pageData.env.id}", "before"));
		code.append(VRELParser.interpreteVREL(translateKmVars(addCDATA(escapeJspVars(keetleCode))), env));
		code.append(getDefaultLayoutInclude(appConfig.getLayoutRelativeDir() + "/${pageData.env.id}", "after"));
		code.append("\n</km:viewWrapper>");
		
		return code.toString();
	}
	
	public static String layoutCodeToJsp(String code, EnvData env) throws KommetException
	{
		return VRELParser.interpreteVREL(translateKmVars(escapeJspVars(code)), env);
	}

	private static String translateKmVars(String code)
	{
		return code.replaceAll("#\\{", "\\${");
	}
	
	/*private static String translateTextLabels(String code)
	{
		Pattern p = Pattern.compile("\\{\\{\\$Label\\.([^\\}]+)\\}\\}");
		Matcher m = p.matcher(code);
		
		StringBuffer sb = new StringBuffer();
		while (m.find())
		{
			//parsedDefinition.
			m.appendReplacement(sb, "\\${" + RequestAttributes.TEXT_LABELS_ATTR_NAME + ".get('" + m.group(1) + "', " + RequestAttributes.AUTH_DATA_ATTR_NAME + ".getLocale())}");
		}
		m.appendTail(sb);
		return sb.toString();
	}*/

	private static String escapeJspVars(String code)
	{
		return code.replaceAll("\\$\\{", "\\\\\\${");
	}
	
	/**
	 * Escape <script> tags using CDATA to ensure proper interpretation of Javascript content
	 * @param code
	 * @return
	 * @throws KommetException
	 */
	private static String addCDATA(String code) throws KommetException
	{	
		// replace all script tags with appending CDATA to them, and if there are more than one CDATA items (because they were also added by the user), replace them with one
		code = code.replaceAll("<script([^>]*)>", "<script$1>//<![CDATA[").replaceAll("//<!\\[CDATA\\[\\s*//<!\\[CDATA\\[", "//<![CDATA[");
		code = code.replaceAll("</script>", "//]]></script>").replaceAll("//\\]\\]>\\s*//\\]\\]>", "//]]>");
		return code;
	}

	private static String getDefaultLayoutInclude(String dir, String placement)
	{
		StringBuilder code = new StringBuilder();
		code.append("<c:if test=\"${not empty layoutId}\">");
		code.append("<jsp:include page=\"").append(dir).append("/${layoutId}_").append(placement).append(".jsp\" />");
		code.append("</c:if>");
		return code.toString();
	}

	/**
	 * Wraps KTL code in XML namespaces definition and root element &lt;keetle&gt; so that it can be parsed as valid XML. 
	 * @param keetleCode
	 * @return
	 */
	public static String wrapKeetle(String keetleCode)
	{
		StringBuilder code = new StringBuilder("<?xml version=\"1.0\"?><keetle ");
		code.append(getViewIncludesNamespaceBindings());
		// close keetle tag
		code.append(">\n");
		code.append(keetleCode);
		code.append("</keetle>");
		return code.toString();
	}
	
	public static String wrapLayout(String code)
	{
		return wrapKeetle(code);
	}
	
	/**
	 * This method takes Keetle code and converts it to JSP.
	 * @param keetleCode the source KTL code
	 * @return
	 */
	/*public static String convertKeetleToJsp (String keetleCode)
	{
		StringBuilder jspCode = new StringBuilder();
		
		// add taglibs at the beginning
		jspCode.append(getViewIncludes());
		
		// add the keetle code
		jspCode.append(keetleCode);
		
		return jspCode.toString();
	}*/
	
	/**
	 * Extract the view name and package (if defined) from the code.
	 * @param keetleCode
	 * @return Array of string with two items. The first item is the view name and is never null. The second item is the package name and can be null (but never an empty string) if package is not defined.
	 * @throws KommetException 
	 */
	public static String[] getViewPropertiesFromCode (String keetleCode) throws KommetException
	{
		keetleCode = addCDATA(keetleCode);
		
		if (!StringUtils.hasText(keetleCode))
		{
			throw new ViewSyntaxException("View code is null or empty");
		}
		
		SAXReader reader = new SAXReader();
        Document document;
		try
		{
			document = reader.read(new ByteArrayInputStream(keetleCode.getBytes("UTF-8")));
		}
		catch (DocumentException e)
		{
			throw new ViewSyntaxException("Error parsing Keetle code: " + e.getMessage());
		}
		catch (UnsupportedEncodingException e)
		{
			throw new KommetException("Unsupported encoding discovered in Keetle code");
		}
        Element root = document.getRootElement();
        Element view = null;
        
        // iterate through child elements of root
        int rootChildCount = 0;
        for (Iterator<?> i = root.elementIterator(); i.hasNext();)
        {
            view = (Element)i.next();
    
            if (!view.getName().equals("view"))
            {
            	throw new ViewSyntaxException("View element not found in Keetle code, instead the root element is " + root.getName());
            }
            
            // make sure view is the only child element of <keetle>
            if (rootChildCount++ > 1)
            {
            	throw new ViewSyntaxException("An element was found outside the view element. All elements must be placed within the view tag");
            }
        }
        
        if (view == null)
        {
        	throw new KommetException("View element not found in view code. The view code is: " + keetleCode);
        }
		
		//Document doc = Jsoup.parse(keetleCode, "UTF-8", Parser.xmlParser());
		//Elements viewElements = doc.getElementsByTag("km:view");
		
		String[] nameBits = new String[4];
		Attribute nameAttr = view.attribute("name");
		if (nameAttr == null)
		{
			throw new ViewSyntaxException("Attribute 'name' not found on element view");
		}
		else if (!StringUtils.hasText(nameAttr.getValue()))
		{
			throw new ViewSyntaxException("Attribute name on tag view is empty");
		}
		else
		{
			nameBits[0] = nameAttr.getValue();
		}
		
		Attribute packageAttr = view.attribute("package");
		
		if (packageAttr != null && StringUtils.hasText(packageAttr.getValue()))
		{
			nameBits[1] = packageAttr.getValue();
		}
		
		Attribute layoutAttr = view.attribute("layout");
		
		if (layoutAttr != null && StringUtils.hasText(layoutAttr.getValue()))
		{
			nameBits[2] = layoutAttr.getValue();
		}
		
		Attribute typeAttr = view.attribute("object");
		
		if (typeAttr != null && StringUtils.hasText(typeAttr.getValue()))
		{
			nameBits[3] = typeAttr.getValue();
		}
		
		return nameBits;
	}
	
	public static String getViewIncludes()
	{
		StringBuilder sb = new StringBuilder();
		
		// add taglibs
		sb.append(getTaglib("c", "http://java.sun.com/jsp/jstl/core")).append("\n");
		sb.append(getTaglib("km", "/WEB-INF/tld/km-tags.tld")).append("\n");
		sb.append(getTagDir("ko", "/WEB-INF/tags")).append("\n");
		
		// add page
		sb.append("<%@ page contentType=\"text/html;charset=UTF-8\" pageEncoding=\"UTF-8\" %>").append("\n");
		
		return sb.toString();
	}
	
	/**
	 * This method gets the code for XML namespace bindings for the JSP taglibs used in KTL code.
	 * It returns the same taglibs as method {@link#getViewIncludes()}, only as namespace binding declarations
	 * instead of JSP includes.
	 * 
	 * They need to be added to KTL code before it is validated and parsed, since these namespaces may be used in KTL itself.
	 * 
	 * TODO NOTE: if we want to restrict the use of a certain prefix in KTL, it is enough to exclude it from this method
	 * and KTL code containing this prefix will not be parsed.
	 * 
	 * @return string containing XML code for the list of namespace bindings
	 */
	private static String getViewIncludesNamespaceBindings()
	{
		StringBuilder sb = new StringBuilder();
		
		// add taglibs
		sb.append(getNamespaceBinding("c", "http://java.sun.com/jsp/jstl/core")).append(" ");
		sb.append(getNamespaceBinding("ktl", "/WEB-INF/tld/ktl-tags.tld")).append(" ");
		sb.append(getNamespaceBinding("km", "/WEB-INF/tld/km-tags.tld")).append(" ");
		sb.append(getNamespaceBinding("ko", "/WEB-INF/tags")).append(" ");
		
		return sb.toString();
	}
	
	private static String getTaglib(String prefix, String uri)
	{
		return "<%@ taglib prefix=\"" + prefix + "\" uri=\"" + uri + "\" %>";
	}
	
	private static String getNamespaceBinding(String prefix, String uri)
	{
		return "xmlns:" + prefix + "=\"" + uri + "\"";
	}
	
	private static Object getTagDir(String prefix, String dir)
	{
		return "<%@ taglib prefix=\"" + prefix + "\" tagdir=\"" + dir + "\" %>";
	}
	
	public static String getStandardErrorViewCode(String viewName, String viewPackage) throws ViewSyntaxException
	{
		StringBuilder code = new StringBuilder();
		code.append("\t<km:errors />");
		
		return wrapViewCode(code.toString(), viewName, viewPackage);
	}

	public static String getStandardEditViewCode(Type type, String viewName, String viewPackage) throws ViewSyntaxException
	{
		StringBuilder code = new StringBuilder();
		code.append("<km:form action=\"save/" + type.getKeyPrefix() + "\" method=\"post\">\n");
	    code.append("\t<km:objectDetails mode=\"edit\" failOnUninitializedFields=\"false\" record=\"{{record}}\" fieldNamePrefix=\"" + StandardObjectController.FORM_FIELD_PREFIX + ".\">\n");
	    code.append("\t</km:objectDetails>\n");                    
	    code.append("</km:form>\n");
		
		return wrapViewCode(code.toString(), viewName, viewPackage);
	}

	public static void deleteViewFromStorage(View view, AppConfig config, EnvData env) throws KommetException
	{
		File viewFile = new File(env.getKeetleDir(config.getKeetleDir()) + "/" + view.getId() + ".jsp");
		if (viewFile.exists() && viewFile.isFile())
		{
			viewFile.delete();
		}
		else
		{
			throw new KommetException("View file for view ID " + view.getId() + " does not exist or is not a regular file");
		}
	}

	public static String getStandardView(Type type, String viewName, String viewPackage, StandardActionType viewType, EnvData env) throws KommetException
	{
		switch (viewType)
		{
			case CREATE: case EDIT: return getStandardEditViewCode(type, viewName, viewPackage);
			case LIST: return getStandardListViewCode(type, viewName, viewPackage, env);
			case VIEW: return getStandardDetailsViewCode(type, viewName, viewPackage);
			default: throw new KommetException("Unsupported view type " + viewType);
		}
	}
	
	public static String includeViewResource (String resourceName, AppConfig config, String contextPath, EnvData env) throws KommetException
	{
		ViewResource resource = env.getViewResource(resourceName);
		
		if (resource == null)
		{
			log.debug("View resource '" + resourceName + "' not found");
			
			// fail silently
			return "";
		}
		
		String resourceFilePath = contextPath + "/" + env.getViewResourceEnvDir(config.getViewResourceRelativeDir()) + "/" + resource.getPath();
		
		if (MimeTypes.CSS_MIME_TYPE.equals(resource.getMimeType()))
		{
			return "<link href=\"" + resourceFilePath + "\" rel=\"stylesheet\" type=\"text/css\" />";
		}
		else if (MimeTypes.JAVASCRIPT_MIME_TYPE.equals(resource.getMimeType()))
		{
			return "<script type=\"text/javascript\" src=\"" + resourceFilePath + "\"></script>";
		}
		else
		{
			throw new KommetException("Unsupported view resource MIME type '" + resource.getMimeType() + "'. Unable to include resource.");
		}
	}
}
