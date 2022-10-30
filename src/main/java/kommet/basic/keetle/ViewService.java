/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.basic.keetle;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.Layout;
import kommet.basic.View;
import kommet.data.DataService;
import kommet.data.FieldValidationException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.ValidationErrorType;
import kommet.env.EnvData;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.PropertyUtilException;
import kommet.utils.ValidationUtil;

@Service
public class ViewService
{
	private static final Logger log = LoggerFactory.getLogger(ViewService.class);
	
	@Inject
	AppConfig config;
	
	@Inject
	DataService dataService;
	
	@Inject
	ViewDao viewDao;
	
	@Inject
	LayoutDao layoutDao;
	
	@Transactional
	public void initKeetleDir(EnvData env, boolean forceInit) throws KommetException
	{
		String envKeetleDir = env.getKeetleDir(config.getKeetleDir());
		
		// create keetle directory when server starts
		File keetleDir = new File(envKeetleDir);
		
		// if directory exists, it means it has already been initialized and nothing needs to be done
		if (keetleDir.exists() && keetleDir.isDirectory())
		{
			if (!forceInit)
			{
				return;
			}
			else
			{
				// delete directory - it will be created anew
				keetleDir.delete();
			}
		}
		
		keetleDir.mkdir();
		
		log.debug("Created keetle work directory " + envKeetleDir);
		
		initKeetleViews(env);
	}
	
	/**
	 * This method stores the database and on this. The name of the view may be modified according to the
	 * value from the view tag in the passed code.
	 * @param view
	 * @param ktlCode
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional
	public View fullSave (View view, String code, boolean isUpdatePackageFromCode, AuthData authData, EnvData env) throws KommetException
	{
		try
		{
			// replace ampersands since they are not properly handled by the XML parser used in the ViewUtil.getViewPropertiesFromKeetleCode method.
			String escapedCode = code.replaceAll("\\&", "&amp;");
	
			// extract view name, package and layout from KTL code
			String[] viewProperties = ViewUtil.getViewPropertiesFromCode(ViewUtil.wrapKeetle(escapedCode));
			
			if (!ValidationUtil.isValidResourceName(viewProperties[0]))
			{
				String msg = "Invalid view name. " + ValidationUtil.INVALID_RESOURCE_ERROR_EXPLANATION;
				throw new FieldValidationException(msg, msg, null, "Name", ValidationErrorType.FIELD_INVALID_VALUE);
			}
			
			view.setName(viewProperties[0]);
			
			String packageNameFromCode = viewProperties[1];
			
			if (view.getPackageName() != null && !view.getPackageName().equals(packageNameFromCode) && !isUpdatePackageFromCode)
			{
				throw new KommetException("Package name in code is different than the one specified on the view object");
			}
			view.setPackageName(packageNameFromCode);
			
			Layout layout = null;
			if (StringUtils.hasText(viewProperties[2]))
			{
				layout = layoutDao.getByName(viewProperties[2], env);
				if (layout == null)
				{
					String msg = "Layout with name " + viewProperties[2] + " not found";
					throw new FieldValidationException(msg, msg, null, "Name", ValidationErrorType.FIELD_INVALID_VALUE);
				}
			}
			
			view.setLayout(layout);
			
			Type type = null;
			
			if (StringUtils.hasText(viewProperties[3]))
			{
				type = env.getType(viewProperties[3]);
				if (type == null)
				{
					String msg = "Object with name " + viewProperties[2] + " not found";
					throw new FieldValidationException(msg, msg, null, "Name", ValidationErrorType.FIELD_INVALID_VALUE);
				}
			}
			
			view.setTypeId(type != null ? type.getKID() : null);
		}
		catch (ViewSyntaxException e)
		{
			String msg = "View syntax error: " + e.getMessage();
			throw new FieldValidationException(msg, msg, null, "Name", ValidationErrorType.FIELD_INVALID_VALUE);
		}
		
		if (!ViewUtil.isValidView(code))
		{
			throw new ViewSyntaxException("Invalid view code:\n" + code);
		}
		
		view.initKeetleCode(code, config, env);
		view.setPath(view.getName());
		save(view, config, authData, env);
		
		// store updated view on disk
		storeView(view, env);
		
		return view;
	}
	
	/**
	 * Save the view in the database. This method does not store the view on disk.
	 * @param view
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional
	public View save (View view, AppConfig appConfig, AuthData authData, EnvData env) throws KommetException
	{
		if (!StringUtils.hasText(view.getAccessLevel()))
		{
			// set default encryption
			view.setAccessLevel("Editable");
		}
		
		validateView(view);
		
		// TODO
		// extract layout name from view tag
		
		view.setJspCode(ViewUtil.keetleToJSP(view.getKeetleCode(), appConfig, env));
		View savedView = viewDao.save(view, authData, env);
		
		// update view info on the environment
		env.addView(savedView);
		
		return savedView;
	}

	private void validateView(View view) throws FieldValidationException
	{
		if (!StringUtils.hasText(view.getName()))
		{
			throw new FieldValidationException("Field name is empty on object of type " + View.class.getSimpleName());
		}
		else
		{
			if (!ValidationUtil.isValidResourceName(view.getName()))
			{
				throw new FieldValidationException("View name is not valid. It may only contain letters, digits and an underscore character, and must not start or end with an underscore");
			}
		}
		
		if (!StringUtils.hasText(view.getPackageName()))
		{
			throw new FieldValidationException("View package name must not be empty");
		}
		
		// make sure view name is not env-specific
		if (MiscUtils.isEnvSpecific(view.getPackageName()))
		{
			throw new FieldValidationException("View package name " + view.getPackageName() + " is env-specific, while it should not be");
		}
	}

	private void initKeetleViews(EnvData env) throws KommetException
	{		
		// create all views from database
		List<View> allViews = getAllViews(env);
		for (View view : allViews)
		{
			storeView(view, env);
		}
	}

	public void storeView (View view, EnvData env) throws KommetException
	{
		ViewUtil.storeView(view, config, env);
	}
	
	@Transactional (readOnly = true)
	public List<View> getViews(ViewFilter filter, EnvData env) throws KommetException
	{
		return viewDao.find(filter, env);
	}

	@Transactional(readOnly = true)
	public List<View> getAllViews(EnvData env) throws KommetException
	{
		ViewFilter filter = new ViewFilter();
		filter.setInitCode(true);
		return viewDao.find(filter, env);
	}

	public String getEnvKeetleDir(EnvData env) throws PropertyUtilException
	{
		return config.getRelativeKeetleDir() + "/" + env.getId(); 
	}

	@Transactional(readOnly = true)
	public View getView(KID id, EnvData env) throws KommetException
	{
		ViewFilter filter = new ViewFilter();
		filter.setKID(id);
		filter.setInitCode(true);
		List<View> views = viewDao.find(filter, env);
		return views.isEmpty() ? null : views.get(0);
	}

	@Transactional(readOnly = false)
	public void deleteView(View view, EnvData env) throws KommetException
	{
		// delete view from DB
		List<View> views = new ArrayList<View>();
		views.add(view);
		viewDao.delete(views, null, env);
		
		// delete view from disk
		ViewUtil.deleteViewFromStorage(view, config, env);
	}
	
	@Transactional(readOnly = true)
	public View getView(String qualifiedName, boolean initCode, EnvData env) throws KommetException
	{
		ViewFilter filter = new ViewFilter();
		filter.setQualifiedName(qualifiedName);
		filter.setInitCode(initCode);
		List<View> views = viewDao.find(filter, env);
		return views.isEmpty() ? null : views.get(0);
	}

	@Transactional(readOnly = true)
	public View getView(String qualifiedName, EnvData env) throws KommetException
	{
		ViewFilter filter = new ViewFilter();
		filter.setQualifiedName(qualifiedName);
		List<View> views = viewDao.find(filter, env);
		return views.isEmpty() ? null : views.get(0);
	}

	@Transactional
	public void delete(List<View> views, EnvData env) throws KommetException
	{
		viewDao.delete(views, null, env);
	}

	@Transactional
	public void delete(KID id, AuthData authData, EnvData env) throws KommetException
	{
		viewDao.delete(id, authData, env);
	}
}
