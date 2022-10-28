/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.Layout;
import kommet.basic.RecordAccessType;
import kommet.basic.SettingValue;
import kommet.basic.UserCascadeHierarchy;
import kommet.config.Constants;
import kommet.config.UserSettingKeys;
import kommet.dao.SettingValueFilter;
import kommet.data.FieldValidationException;
import kommet.data.KeyPrefixException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.uch.UserCascadeHierarchyContext;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.AppConfig;

@Service
public class LayoutService
{
	@Inject
	LayoutDao layoutDao;
	
	@Inject
	AppConfig config;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	private static final Logger log = LoggerFactory.getLogger(LayoutService.class);
	
	@Transactional
	public Layout save (Layout layout, AuthData authData, EnvData env) throws KommetException
	{
		if ((Constants.BASIC_LAYOUT_NAME.equals(layout.getName()) || Constants.BLANK_LAYOUT_NAME.equals(layout.getName())) && !AuthUtil.isRoot(authData))
		{
			throw new KommetException("Insufficient privileges to modify basic layout");
		}
		
		validateLayout(layout);
		
		layout = layoutDao.save(layout, authData, env);
		
		// save to disk
		storeLayout(layout, config, env);
		
		return layout;
	}
	
	private void validateLayout(Layout layout) throws KommetException
	{
		String[] nameBits = getLayoutPropertiesFromCode(layout.getCode());
		
		String layoutNameFromCode = nameBits[0];
		String packageFromCode = nameBits[1];
		String fullNameFromCode = (StringUtils.hasText(packageFromCode) ? packageFromCode + "." : "") + layoutNameFromCode;
		
		if (!layout.getName().equals(fullNameFromCode))
		{
			throw new FieldValidationException("Layout name and package in code is different than layout name on the layout object");
		}
	}

	private static void storeLayout (Layout layout, AppConfig config, EnvData env) throws KommetException
	{
		try
		{
			// Store the layout on disk. The layout's name will consist of its KID + before/after suffix + .jsp suffix.
			// This will ensure that when the view's name changes, it will still be saved into the same file on disk.
			FileWriter beforeLayoutFile = new FileWriter(env.getLayoutDir(config.getLayoutDir()) + "/" + layout.getId() + "_before.jsp");
			if (StringUtils.hasText(layout.getBeforeContent()))
			{
				beforeLayoutFile.write(convertLayoutToJsp(layout.getBeforeContent(), env));
			}
			beforeLayoutFile.close();
			
			FileWriter afterLayoutFile = new FileWriter(env.getLayoutDir(config.getLayoutDir()) + "/" + layout.getId() + "_after.jsp");
			if (StringUtils.hasText(layout.getAfterContent()))
			{
				afterLayoutFile.write(convertLayoutToJsp(layout.getAfterContent(), env));
			}
			afterLayoutFile.close();
		}
		catch (IOException e)
		{
			throw new KommetException("Error writing layout to disk: " + e.getMessage());
		}
	}
	
	private static String convertLayoutToJsp (String keetleCode, EnvData env) throws KommetException
	{
		StringBuilder jspCode = new StringBuilder();
		
		// add taglibs at the beginning
		jspCode.append(ViewUtil.getViewIncludes());
		
		// wrap in viewWrapper and layout tag
		jspCode.append("<km:viewWrapper>\n\t<km:layout>\n");
		
		// add the keetle code
		jspCode.append(ViewUtil.layoutCodeToJsp(keetleCode, env)).append("\n");
		
		// end layout tag
		jspCode.append("\t</km:layout>\n</km:viewWrapper>");
		
		return jspCode.toString();
	}
	
	public static String[] getPreAndPostContent (String layoutCode) throws KommetException
	{
		if (!StringUtils.hasText(layoutCode))
		{
			throw new KommetException("Layout code is empty");
		}
		
		// escape content within beforeContent and afterContent tags
		String code = layoutCode.replaceAll("<\\s*km:beforeContent\\s*>", "<km:beforeContent><![CDATA[");
		code = code.replaceAll("</\\s*km:beforeContent\\s*>", "]]></km:beforeContent>");
		code = code.replaceAll("<\\s*km:afterContent\\s*>", "<km:afterContent><![CDATA[");
		code = code.replaceAll("</\\s*km:afterContent\\s*>", "]]></km:afterContent>");
		
		if (!StringUtils.hasText(code))
		{
			throw new ViewSyntaxException("Layout code is empty");
		}
		
		SAXReader reader = new SAXReader();
        Document document;
		try
		{
			document = reader.read(new ByteArrayInputStream(code.getBytes("UTF-8")));
		}
		catch (DocumentException e)
		{
			throw new ViewSyntaxException("Error parsing layout code: " + e.getMessage());
		}
		catch (UnsupportedEncodingException e)
		{
			throw new KommetException("Unsupported encoding discovered in layout code");
		}
        Element root = document.getRootElement();
        Element layoutElement = null;
        
        // iterate through child elements of root
        int rootChildCount = 0;
        for (Iterator<?> i = root.elementIterator(); i.hasNext();)
        {
            layoutElement = (Element)i.next();
    
            if (!layoutElement.getName().equals("layout"))
            {
            	throw new ViewSyntaxException("Layout element not found, instead the root element is " + root.getName());
            }
            
            // make sure view is the only child element of <keetle>
            if (rootChildCount++ > 1)
            {
            	throw new ViewSyntaxException("An element was found outside the view element. All elements must be placed within the view tag");
            }
        }
        
        String[] contentParts = new String[2];
        for (Iterator<?> i = layoutElement.elementIterator(); i.hasNext();)
        {
        	Element subElement = (Element)i.next();
        	
        	if (subElement.getName().equals("beforeContent"))
            {
        		if (contentParts[0] != null)
        		{
        			throw new ViewSyntaxException("Duplicate tag beforeContent in layout definition. Only one occurrence of this tag is allowed.");
        		}
        		contentParts[0] = subElement.getText();
            }
        	else if (subElement.getName().equals("afterContent"))
            {
        		if (contentParts[1] != null)
        		{
        			throw new ViewSyntaxException("Duplicate tag afterContent in layout definition. Only one occurrence of this tag is allowed.");
        		}
        		contentParts[1] = subElement.getText();
            }
        	else
        	{
        		throw new ViewSyntaxException("Invalid element " + subElement.getName() + ". Only beforeContent and afterContent are allowed as children of the layout tag");
        	}
        }
        
        return contentParts;
	}
	
	@Transactional
	public void initLayoutDir(EnvData env, boolean forceInit) throws KommetException
	{
		String envLayoutDir = env.getLayoutDir(config.getLayoutDir());
		
		// create layout directory when server starts
		File layoutDir = new File(envLayoutDir);
		
		// if directory exists, it means it has already been initialized and nothing needs to be done
		if (layoutDir.exists() && layoutDir.isDirectory())
		{
			if (!forceInit)
			{
				return;
			}
			else
			{
				// delete directory - it will be created anew
				layoutDir.delete();
			}
		}
		
		layoutDir.mkdir();
		
		log.debug("Created layout work directory " + envLayoutDir);
		
		initLayoutsOnDisk(env);
	}
	
	private void initLayoutsOnDisk(EnvData env) throws KommetException
	{		
		// create all layouts from database
		List<Layout> allLayouts = find(null, env);
		for (Layout layout : allLayouts)
		{
			storeLayout(layout, config, env);
		}
	}
	
	@Transactional(readOnly = true)
	public Layout getByName (String name, EnvData env) throws KommetException
	{
		return layoutDao.getByName(name, env);
	}
	
	@Transactional(readOnly = true)
	public Layout getById (KID id, EnvData env) throws KommetException
	{
		return layoutDao.get(id, env);
	}

	@Transactional(readOnly = true)
	public List<Layout> find(LayoutFilter filter, EnvData env) throws KommetException
	{
		return layoutDao.find(filter, env);
	}

	/**
	 * Returns an sample empty layout code with the given name.
	 * @param name
	 * @return
	 */
	public static String getEmptyLayoutCode(String name)
	{
		return getLayoutCode(name, null, null);
	}
	
	public static String getLayoutCode(String name, String beforeContent, String afterContent)
	{
		StringBuilder sb = new StringBuilder("<km:layout name=\"" + name + "\">\n");
		sb.append("\t<km:beforeContent>");
		
		if (beforeContent != null)
		{
			sb.append(beforeContent);
		}
		
		sb.append("</km:beforeContent>\n");
		sb.append("\t<km:afterContent>");
		
		if (afterContent != null)
		{
			sb.append(afterContent);
		}
		
		sb.append("</km:afterContent>\n");
		sb.append("</km:layout>");
		return sb.toString();
	}
	
	@Transactional
	public Layout createBlankLayout(AuthData authData, EnvData env) throws KommetException
	{
		Layout layout = new Layout();
		layout.setName(Constants.BLANK_LAYOUT_NAME);
		layout.setCode(getBlankLayoutCode(Constants.BLANK_LAYOUT_NAME));
		layout.setAccessType(RecordAccessType.SYSTEM.getId());
		this.save(layout, authData, env);
		
		return layout;
	}

	/**
	 * Generates code of a blank layout.
	 * @param name
	 * @return
	 */
	private String getBlankLayoutCode(String name)
	{
		String beforeContent = "<link href=\"#{pageContext.request.contextPath}/resources/themes/std/styles.css\" rel=\"stylesheet\" type=\"text/css\" />";
		return getLayoutCode(name, beforeContent, null);
	}

	@Transactional
	public void delete(KID id, AuthData authData, EnvData env) throws KommetException
	{
		layoutDao.delete(id, authData, env);
	}

	@Transactional(readOnly = true)
	public KID getDefaultLayoutId(AuthData authData, EnvData env) throws KeyPrefixException, KommetException
	{
		return uchService.getUserSettingAsKID(UserSettingKeys.KM_SYS_DEFAULT_LAYOUT_ID, authData, AuthData.getRootAuthData(env), env);
	}

	@Transactional(readOnly = true)
	public String getDefaultLayoutName(AuthData authData, EnvData env) throws KeyPrefixException, KommetException
	{
		KID layoutId = getDefaultLayoutId(authData, env);
		
		if (layoutId != null)
		{
			Layout layout = getById(layoutId, env);
			if (layout == null)
			{
				throw new KommetException("Default layout with ID " + layoutId + " not found");
			}
			
			return layout.getName();
		}
		else
		{
			return null;
		}
	}

	@Transactional
	public void setDefaultLayout(Layout layout, AuthData authData, EnvData env) throws KommetException
	{
		SettingValueFilter filter = new SettingValueFilter();
		filter.addKey(UserSettingKeys.KM_SYS_DEFAULT_LAYOUT_ID);
		List<SettingValue> settingValues = uchService.getSettings(filter, authData, AuthData.getRootAuthData(env), env);
		
		SettingValue setting = null;
		
		for (SettingValue val : settingValues)
		{
			if (val.getHierarchy().getActiveContext().equals(UserCascadeHierarchyContext.ENVIRONMENT))
			{
				setting = val;
				break;
			}
		} 
		
		if (setting == null)
		{
			setting = new SettingValue();
			setting.setKey(UserSettingKeys.KM_SYS_DEFAULT_LAYOUT_ID);
		}
		
		if (layout != null)
		{
			setting.setValue(layout.getId().getId());
		}
		else
		{
			setting.setValue(null);
			setting.nullify("value");
		}
		
		UserCascadeHierarchy uch = new UserCascadeHierarchy();
		uch.setActiveContext(UserCascadeHierarchyContext.ENVIRONMENT, true);
		setting.setHierarchy(uch);
		
		// save UCH
		uchService.saveSetting(setting, uch.getActiveContext(), uch.getActiveContextValue(), authData, env);
	}
	
	/**
	 * Extract the view name and package from layout code.
	 * @param code
	 * @return Array of string with two items. The first item is the view name and is never null. The second item is the package name and can be null (but never an empty string) if package is not defined.
	 * @throws KommetException 
	 */
	public static String[] getLayoutPropertiesFromCode (String layoutCode) throws KommetException
	{
		// extract only the layout tag from code - we do this, because layout code can contain unclosed elements, and if we tried to parse all of it, SAX parser would throw errors
		Pattern regex = Pattern.compile("<km:layout\\s+[^>]+>");
		Matcher matcher = regex.matcher(layoutCode);
		if (!matcher.find())
		{
			throw new KommetException("Layout tag not found in code");
		} 
		
		// prepend XML header so that the document can be parsed by the SAX parser
		String code = ViewUtil.wrapLayout(matcher.group(0) + "</km:layout>");
		
		if (!StringUtils.hasText(code))
		{
			throw new ViewSyntaxException("Layout code is null or empty");
		}
		
		SAXReader reader = new SAXReader();
		
        Document document;
		try
		{
			document = reader.read(new ByteArrayInputStream(code.getBytes("UTF-8")));
		}
		catch (DocumentException e)
		{
			throw new ViewSyntaxException("Error parsing code: " + e.getMessage());
		}
		catch (UnsupportedEncodingException e)
		{
			throw new KommetException("Unsupported encoding discovered in code");
		}
        Element root = document.getRootElement();
        Element layout = null;
        
        // iterate through child elements of root
        int rootChildCount = 0;
        for (Iterator<?> i = root.elementIterator(); i.hasNext();)
        {
            layout = (Element)i.next();
    
            if (!layout.getName().equals("layout"))
            {
            	throw new ViewSyntaxException("Layout element not found in code, instead the root element is " + root.getName());
            }
            
            if (rootChildCount++ > 1)
            {
            	throw new ViewSyntaxException("An element was found outside the layout element. All elements must be placed within the layout tag");
            }
        }
        
        if (layout == null)
        {
        	throw new KommetException("Layout element not found in view code. The view code is: " + code);
        }
		
		String[] nameBits = new String[2];
		Attribute nameAttr = layout.attribute("name");
		if (nameAttr == null)
		{
			throw new ViewSyntaxException("Attribute 'name' not found on element layout");
		}
		else if (!StringUtils.hasText(nameAttr.getValue()))
		{
			throw new ViewSyntaxException("Attribute name on tag layout is empty");
		}
		else
		{
			nameBits[0] = nameAttr.getValue();
		}
		
		Attribute packageAttr = layout.attribute("package");
		
		if (packageAttr != null && StringUtils.hasText(packageAttr.getValue()))
		{
			nameBits[1] = packageAttr.getValue();
		}
		
		return nameBits;
	}
}
