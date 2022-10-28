/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.auth.ProfileService;
import kommet.basic.Library;
import kommet.basic.LibraryException;
import kommet.basic.LibraryItem;
import kommet.basic.Profile;
import kommet.basic.actions.ActionService;
import kommet.basic.keetle.LayoutService;
import kommet.basic.keetle.ViewService;
import kommet.basic.keetle.tags.breadcrumbs.Breadcrumbs;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.UniqueCheckService;
import kommet.data.validationrules.ValidationRuleService;
import kommet.deployment.Deployable;
import kommet.deployment.DeploymentConfig;
import kommet.deployment.DeploymentProcess;
import kommet.deployment.DeploymentService;
import kommet.deployment.FailedPackageDeploymentException;
import kommet.deployment.FileDeploymentStatus;
import kommet.deployment.OverwriteHandling;
import kommet.deployment.PackageDeploymentStatus;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.files.FileService;
import kommet.filters.LibraryFilter;
import kommet.json.JSON;
import kommet.koll.ClassService;
import kommet.rest.RestUtil;
import kommet.scheduler.ScheduledTaskService;
import kommet.security.RestrictedAccess;
import kommet.services.AppService;
import kommet.services.LibraryService;
import kommet.services.UserGroupService;
import kommet.services.ViewResourceService;
import kommet.services.WebResourceService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.PropertyUtilException;
import kommet.utils.UrlUtil;
import kommet.utils.ValidationUtil;

@Controller
public class LibraryController extends BasicRestController
{
	/**
	 * Maximum file size to upload, in bytes.
	 */
	private Integer MAX_FILE_SIZE = 5000000;
	
	@Inject
	EnvService envService;
	
	@Inject
	LibraryService libService;
	
	@Inject
	DeploymentService deploymentService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	ClassService clsService;
	
	@Inject
	ViewService viewService;
	
	@Inject
	WebResourceService webResourceService;
	
	@Inject
	FileService fileService;
	
	@Inject
	LayoutService layoutService;
	
	@Inject
	UniqueCheckService uniqueCheckService;
	
	@Inject
	AppService appService;
	
	@Inject
	UserGroupService ugService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	ScheduledTaskService stService;
	
	@Inject
	ViewResourceService viewResourceService;
	
	@Inject
	ActionService actionService;
	
	@Inject
	DataService dataService;
	
	@Inject
	ValidationRuleService vrService;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/libraries/save", method = RequestMethod.POST)
	public ModelAndView save (@RequestParam(value = "libId", required = false) String sLibId,
								@RequestParam(value = "name", required = false) String name,
								@RequestParam(value = "provider", required = false) String provider,
								@RequestParam(value = "version", required = false) String version,
								@RequestParam(value = "accessLevel", required = false) String accessLevel,
								@RequestParam(value = "description", required = false) String description,
								HttpSession session) throws KommetException
	{
		clearMessages();
		
		AuthData authData = AuthUtil.getAuthData(session);
		EnvData env = envService.getCurrentEnv(session);
		Library lib = null;
		
		if (StringUtils.hasText(sLibId))
		{
			lib = libService.getLibrary(KID.get(sLibId), authData, env);
			
			if (lib == null)
			{
				addError("Library with ID " + sLibId + " does not exist");
			}
		}
		else
		{
			lib = new Library();
		}
		
		lib.setName(name);
		lib.setDescription(description);
		lib.setProvider(provider);
		lib.setVersion(version);
		lib.setAccessLevel(accessLevel);
		lib.setSource("Local");
		lib.setIsEnabled(false);
		lib.setStatus("Not installed");
		
		if (!StringUtils.hasLength(name))
		{
			addError("Library name is empty");
		}
		else if (!ValidationUtil.isValidLibraryName(name))
		{
			addError("Invalid library name");
		}
		
		if (!StringUtils.hasLength(description))
		{
			addError("Description is empty");
		}
		
		if (!StringUtils.hasLength(provider))
		{
			addError("Provider is empty");
		}
		
		if (!StringUtils.hasLength(version))
		{
			addError("Version is empty");
		}
		
		if (!StringUtils.hasLength(accessLevel))
		{
			addError("Please select access level");
		}
		
		if (hasErrorMessages())
		{
			ModelAndView mv = new ModelAndView("libraries/edit");
			mv.addObject("errorMsgs", getErrorMsgs());
			mv.addObject("lib", lib);
			return mv;
		}
		
		try
		{
			libService.save(lib, authData, env);
		}
		catch (KommetException e)
		{
			ModelAndView mv = new ModelAndView("libraries/edit");
			mv.addObject("errorMsgs", getMessage("Error saving library: " + e.getMessage()));
			mv.addObject("lib", lib);
			return mv;
		}
		
		return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/libraries/" + lib.getId());
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/libraries/edit/{libId}", method = RequestMethod.GET)
	public ModelAndView edit(@PathVariable("libId") String sLibId, HttpSession session) throws KommetException
	{
		clearMessages();
		
		KID libId = null;
		try
		{
			libId = KID.get(sLibId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Invalid library ID " + sLibId);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		Library lib = libService.getLibrary(libId, authData, env);
		if (lib == null)
		{
			return getErrorPage("Library with ID " + libId + " not found");
		}
		
		ModelAndView mv = new ModelAndView("libraries/edit");
		mv.addObject("lib", lib);
		mv.addObject("pageTitle", lib.getName());
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/lib/download/{libId}", method = RequestMethod.GET)
	public void download(@PathVariable("libId") String sLibId, HttpServletResponse response, HttpSession session) throws PropertyUtilException, KommetException
	{
		AuthData authData = AuthUtil.getAuthData(session);	
		EnvData env = envService.getCurrentEnv(session);
		
		Library lib = libService.getLibrary(KID.get(sLibId), authData, env);
		byte[] packageFile = libService.createLibraryPackage(lib, authData, env);
		
		try
	    {	
			// get your file as InputStream
			InputStream is = new ByteArrayInputStream(packageFile);
			
			response.setHeader("Content-Disposition", "attachment; filename=\"" + lib.getName() + ".zip\""); 
			
			// copy it to response's OutputStream
			IOUtils.copy(is, response.getOutputStream());
			//response.setContentType("application/rtf");
			response.flushBuffer();
	    }
		catch (IOException ex)
		{
			throw new KommetException("IOError writing file to output stream");
	    }
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/libitems/save", method = RequestMethod.POST)
	@ResponseBody
	public void saveLibItems(@RequestParam(value = "itemIds", required = false) String sItemIds, 
							@RequestParam(value = "libId", required = false) String sLibId,
							HttpSession session, HttpServletResponse response) throws KommetException, IOException
	{
		PrintWriter out = response.getWriter();
		AuthData authData = AuthUtil.getAuthData(session);
		EnvData env = envService.getCurrentEnv(session);
		
		// find library
		Library lib = libService.getLibrary(KID.get(sLibId), true, authData, env);
		
		if (lib == null)
		{
			out.write(RestUtil.getRestErrorResponse("Library with ID " + sLibId + " not found"));
			return;
		}
		
		boolean isEditable = lib.getSource().equals("Local") && lib.getStatus().equals("Not installed");
		
		if (!isEditable)
		{
			// only local libraries are editable
			out.write(RestUtil.getRestErrorResponse("Only local and not installed libraries can be edited"));
			return;
		}
		
		Set<KID> newItemIds = new HashSet<KID>();
		
		if (StringUtils.hasText(sItemIds))
		{
			// split item IDs
			List<String> sItemIdList = MiscUtils.splitAndTrim(sItemIds, ",");
			
			for (String sId : sItemIdList)
			{
				newItemIds.add(KID.get(sId));
			}
		}
		
		// check with library items should be removed
		List<LibraryItem> itemsToBeRemoved = new ArrayList<LibraryItem>();
		Set<KID> itemsToBeKept = new HashSet<KID>();
		for (LibraryItem item : lib.getItems())
		{
			if (!newItemIds.contains(item.getRecordId()))
			{
				itemsToBeRemoved.add(item);
			}
			else
			{
				itemsToBeKept.add(item.getRecordId());
			}
		}
		
		// remove some items
		libService.delete(itemsToBeRemoved, authData, env);
		
		
		List<KID> itemsToBeAddedIds = new ArrayList<KID>();
		
		// find new library items
		for (KID newItemId : newItemIds)
		{
			// if item not already added, add it
			if (!itemsToBeKept.contains(newItemId))
			{
				itemsToBeAddedIds.add(newItemId);
			}
		}
		
		List<LibraryItem> itemsToAdd = null;
		
		// retrieve all new items
		try
		{
			itemsToAdd = getItemsByRecordIds(itemsToBeAddedIds, authData, env);
		}
		catch (LibraryException e)
		{
			out.write(RestUtil.getRestErrorResponse(e.getMessage()));
			return;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			out.write(RestUtil.getRestErrorResponse(e.getMessage()));
			return;
		}
		
		for (LibraryItem item : itemsToAdd)
		{
			item.setLibrary(lib);
			libService.save(item, authData, env);
		}
		
		out.write(RestUtil.getRestSuccessResponse("Items added: " + itemsToAdd.size() + ", removed: " + itemsToBeRemoved.size()));
	}
	
	/**
	 * Gets library items from record IDs
	 * @param itemsToBeAddedIds
	 * @return
	 * @throws KommetException 
	 */
	private List<LibraryItem> getItemsByRecordIds(List<KID> recordIds, AuthData authData, EnvData env) throws KommetException
	{
		List<Deployable> items = new ArrayList<Deployable>();
		
		for (KID recordId : recordIds)
		{
			// handle types separately
			if (recordId.getId().startsWith(KID.TYPE_PREFIX))
			{
				items.add(env.getType(recordId));
				continue;
			}
			
			// handle fields separately
			if (recordId.getId().startsWith(KID.FIELD_PREFIX))
			{
				Field field = dataService.getField(recordId, env);
				field.setType(dataService.getType(field.getType().getId(), env));
				items.add(field);
				continue;
			}
			
			// find type
			Type type = env.getTypeByRecordId(recordId);
			
			Deployable deployable = null;
			
			if (type.getKeyPrefix().equals(KeyPrefix.get(KID.CLASS_PREFIX)))
			{ 		
				deployable = clsService.getClass(recordId, env);
			}
			else if (type.getKeyPrefix().equals(KeyPrefix.get(KID.VIEW_PREFIX)))
			{
				deployable = viewService.getView(recordId, env);
			}
			else if (type.getKeyPrefix().equals(KeyPrefix.get(KID.TYPE_PREFIX)))
			{	
				deployable = env.getType(recordId);
			}
			else if (type.getKeyPrefix().equals(KeyPrefix.get(KID.FIELD_PREFIX)))
			{	
				deployable = dataService.getField(recordId, env);
			}
			else if (type.getKeyPrefix().equals(KeyPrefix.get(KID.LAYOUT_PREFIX)))
			{	
				deployable = layoutService.getById(recordId, env);
			}
			else if (type.getKeyPrefix().equals(KeyPrefix.get(KID.VALIDATION_RULE_PREFIX)))
			{	
				deployable = vrService.get(recordId, env);
			}
			else if (type.getKeyPrefix().equals(KeyPrefix.get(KID.UNIQUE_CHECK_PREFIX)))
			{	
				deployable = uniqueCheckService.get(recordId, env);
			}
			else if (type.getKeyPrefix().equals(KeyPrefix.get(KID.APP_PREFIX)))
			{	
				deployable = appService.get(recordId, authData, env);
			}
			else if (type.getKeyPrefix().equals(KeyPrefix.get(KID.SCHEDULED_TASK_PREFIX)))
			{	
				deployable = stService.get(recordId, env);
			}
			else if (type.getKeyPrefix().equals(KeyPrefix.get(KID.PROFILE_PREFIX)))
			{	
				deployable = profileService.getProfile(recordId, env);
			}
			else if (type.getKeyPrefix().equals(KeyPrefix.get(KID.USER_GROUP_PREFIX)))
			{	
				deployable = ugService.get(recordId, authData, env);
			}
			else if (type.getKeyPrefix().equals(KeyPrefix.get(KID.VIEW_RESOURCE_PREFIX)))
			{	
				deployable = viewResourceService.get(recordId, env);
			}
			else if (type.getKeyPrefix().equals(KeyPrefix.get(KID.WEB_RESOURCE_PREFIX)))
			{	
				deployable = webResourceService.get(recordId, env);
			}
			else if (type.getKeyPrefix().equals(KeyPrefix.get(KID.ACTION_PREFIX)))
			{	
				deployable = actionService.getAction(recordId, env);
			}
			else
			{
				throw new KommetException("Unsupported component type " + type.getQualifiedName());
			}	
		
			items.add(deployable);
		}
		
		return libService.getLibraryItemsFromComponents(items, false, env);
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/libraries/deactivate", method = RequestMethod.POST)
	@ResponseBody
	public void deactivate (@RequestParam("id") String sLibId, HttpSession session, HttpServletResponse response) throws KommetException, IOException
	{	
		PrintWriter out = response.getWriter();
		
		KID libId = null;
		try
		{
			libId = KID.get(sLibId);
		}
		catch (KIDException e)
		{
			out.write(RestUtil.getRestSuccessResponse("Invalid library ID " + sLibId));
			return;
		}
		
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		try
		{
			libService.deactivateLibrary(libService.getLibrary(libId, authData, env), false, authData, env);
			out.write(RestUtil.getRestSuccessResponse("Library uninstalled"));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			out.write(RestUtil.getRestErrorResponse("Library could not be uninstalled: " + e.getMessage()));
		}
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/libraries/activate", method = RequestMethod.POST)
	@ResponseBody
	public void activate (@RequestParam("id") String sLibId, HttpSession session, HttpServletResponse response) throws KommetException, IOException
	{	
		PrintWriter out = response.getWriter();
		
		KID libId = null;
		try
		{
			libId = KID.get(sLibId);
		}
		catch (KIDException e)
		{
			out.write(RestUtil.getRestSuccessResponse("Invalid library ID " + sLibId));
			return;
		}
		
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		try
		{
			PackageDeploymentStatus status = libService.activate(libId, deploymentService, authData, env);
			
			if (status.isSuccess())
			{
				out.write(RestUtil.getRestSuccessResponse("Library re-installed"));
			}
			else
			{
				List<String> errors = new ArrayList<String>();
				for (FileDeploymentStatus err : status.getFailedStatuses())
				{
					for (String msg : err.getErrors())
					{
						errors.add(err.getFileName() + ": " + msg);
					}
				}
				
				out.write(RestUtil.getRestErrorResponse(errors));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			out.write(RestUtil.getRestErrorResponse("Library could not be activated: " + e.getMessage()));
		}
	}

	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/libraries/delete", method = RequestMethod.POST)
	@ResponseBody
	public void delete (@RequestParam("id") String sLibId, HttpSession session, HttpServletResponse response) throws KommetException, IOException
	{	
		PrintWriter out = response.getWriter();
		
		KID libId = null;
		try
		{
			libId = KID.get(sLibId);
		}
		catch (KIDException e)
		{
			out.write(RestUtil.getRestSuccessResponse("Invalid library ID " + sLibId));
			return;
		}
		
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		try
		{
			libService.delete(libId, authData, env);
			out.write(RestUtil.getRestSuccessResponse("Library deleted"));
		}
		catch (Exception e)
		{
			out.write(RestUtil.getRestErrorResponse("Library could not be deleted: " + e.getMessage()));
		}
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/libraries/{libId}", method = RequestMethod.GET)
	public ModelAndView details(@PathVariable("libId") String sLibId, HttpSession session, HttpServletRequest req) throws KommetException
	{
		clearMessages();
		
		KID libId = null;
		try
		{
			libId = KID.get(sLibId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Invalid library ID " + sLibId);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		Library lib = libService.getLibrary(libId, true, authData, env);
		if (lib == null)
		{
			return getErrorPage("Library with ID " + libId + " not found");
		}
		
		Breadcrumbs.add(req.getRequestURL().toString(), "Library: " + lib.getName(), appConfig.getBreadcrumbMax(), session);
		
		ModelAndView mv = new ModelAndView("libraries/details");
		mv.addObject("lib", lib);
		mv.addObject("selectedItemIds", getLibraryItemIds(lib.getItems()));
		
		boolean isEditable = lib.getSource().equals("Local") && lib.getStatus().equals("Not installed");
		mv.addObject("isEditable", isEditable);
		
		return mv;
	}

	private String getLibraryItemIds(List<LibraryItem> items)
	{
		List<String> serializedItems = new ArrayList<String>();
		
		for (LibraryItem item : items)
		{
			serializedItems.add("\"" + item.getRecordId() + "\": {}");
		}
		
		return "{ " + MiscUtils.implode(serializedItems, ", ") + " }";
	}

	/*private String getLibraryItemJSON(List<LibraryItem> items)
	{
		List<String> serializedItems = new ArrayList<String>();
		
		for (LibraryItem item : items)
		{
			StringBuilder sb = new StringBuilder();
			sb.append("{ \"id\": \"").append(item.getId()).append("\", ");
			sb.append("\"name\": \"").append(item.getApiName()).append("\", ");
			sb.append("\"componentType\": \"").append(ComponentType.values()[item.getComponentType()]).append("\" ");
			sb.append(" }");
			
			serializedItems.add(sb.toString());
		}
		
		return "[ " + MiscUtils.implode(serializedItems, ", ") + " ]";
	}

	private List<LibraryItem> getAllLibraryItemsOnEnv(EnvData env) throws KommetException
	{
		List<Deployable> deployables = new ArrayList<Deployable>();
		deployables.addAll(clsService.getClasses(null, env));
		deployables.addAll(viewService.getAllViews(env));
		
		return libService.getLibraryItemsFromComponents(deployables, env);
	}*/

	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/libraries/list", method = RequestMethod.GET)
	public ModelAndView list(HttpSession session, HttpServletRequest req) throws KommetException
	{
		Breadcrumbs.add(req.getRequestURL().toString(), "Libraries", appConfig.getBreadcrumbMax(), session);
		return new ModelAndView("libraries/list");
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/libraries/new", method = RequestMethod.GET)
	public ModelAndView create(HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("libraries/edit");
		
		Library lib = new Library();
		lib.setVersion("1.0");
		
		mv.addObject("lib", lib);
		mv.addObject("pageTitle", "New library");
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/libraries/import", method = RequestMethod.GET)
	public ModelAndView importLib(HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("libraries/import");
		mv.addObject("pageTitle", "Import library");
		mv.addObject("uploadItem", new UploadItem());
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/lib/marketplaceimport", method = RequestMethod.GET)
	public ModelAndView importMarketplaceLib(@RequestParam("id") String sLibId, HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("libraries/marketplaceImport");
		mv.addObject("libId", sLibId);
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/appmarketplace", method = RequestMethod.GET)
	public ModelAndView marketplace(HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("libraries/marketplace");
		return mv;
	}
	
	@RestrictedAccess(profiles = { Profile.ROOT_NAME, Profile.SYSTEM_ADMINISTRATOR_NAME })
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/lib/marketplace/doinstall", method = RequestMethod.POST)
	@ResponseBody
	public void doInstallFromMarketplace(@RequestParam("fileId") String sFileId, HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);

		// download library file
		File libFile = new File(appConfig.getFileDir() + "/tmp-lib-" + MiscUtils.getHash(10));
		FileUtils.copyURLToFile(new URL("https://kommet.io/km/download/" + sFileId), libFile);
		
		PackageDeploymentStatus deployStatus = null;
		
		DeploymentConfig config = new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE);
		byte[] packageFile = FileUtils.readFileToByteArray(libFile);
		
		Library lib = DeploymentProcess.readLibraryMetadata(packageFile);
		
		// make sure the library is not already installed on the env
		LibraryFilter filter = new LibraryFilter();
		filter.setName(lib.getName());
		List<Library> existingLibs = libService.findLibraries(filter, authData, env); 
		if (!existingLibs.isEmpty())
		{
			out.write(RestUtil.getRestErrorResponse(Arrays.asList("This library is already installed on your environment but may be disabled")));
			return;
		}
		
		try
		{
			deployStatus = deploymentService.deployZip(packageFile, config, authData, env);
		}
		catch (FailedPackageDeploymentException e)
		{
			// throwing an error is a normal way for a failed deployment to exit
			deployStatus = e.getStatus();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			out.write(RestUtil.getRestErrorResponse(Arrays.asList("Error deploying library: " + e.getMessage())));
			return;
		}
		
		if (deployStatus.isSuccess())
		{
			// save the library
			Library installedLib = deployStatus.getLibrary();
			installedLib.setSource("External (library repository)");
			installedLib = libService.save(deployStatus.getLibrary(), authData, env);
			
			out.write(RestUtil.getRestSuccessDataResponse("{ \"libraryId\": \"" + installedLib.getId() + "\" }"));
		}
		else
		{	
			List<String> errors = new ArrayList<String>();
			
			for (FileDeploymentStatus status : deployStatus.getFailedStatuses())
			{
				for (String singleErr : status.getErrors())
				{
					errors.add(status.getFileName() + ": " + JSON.escape(singleErr));
				}
			}
			
			envService.clear(env.getId());
			out.write(RestUtil.getRestErrorResponse(errors));
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/lib/doimport", method = RequestMethod.POST)
	@ResponseBody
	public void doImport (UploadItem uploadItem, BindingResult result, @RequestParam("packagePrefix") String packagePrefix, HttpServletResponse response, HttpSession session) throws KommetException, IOException
	{	
		response.setContentType("application/json; charset=UTF-8");
		PrintWriter out = response.getWriter();
		
		EnvData env = envService.getCurrentEnv(session);
		MultipartFile file = uploadItem.getFileData();
		
		if (file == null)
		{
			out.write(RestUtil.getRestErrorResponse(Arrays.asList("No library file selected for upload")));
			return;
		}
		
		if (StringUtils.hasText(packagePrefix))
		{
			// remove trailing dot
			packagePrefix = MiscUtils.trim(packagePrefix, '.');
			
			if (!ValidationUtil.isValidPackageName(packagePrefix))
			{
				out.write(RestUtil.getRestErrorResponse(Arrays.asList("Invalid package prefix " + packagePrefix)));
				return;
			}
		}
		
		AuthData authData = AuthUtil.getAuthData(session);
		
		try
		{	
			if (file.getSize() > 0)
			{	
				if (file.getSize() > MAX_FILE_SIZE)
				{
					out.write(RestUtil.getRestErrorResponse(Arrays.asList(authData.getI18n().get("files.maxsizeexceeded") + " 5Mb.")));
					return;
				}
				
				PackageDeploymentStatus deployStatus = null;
				
				DeploymentConfig config = new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE);
				config.setPackagePrefix(packagePrefix);
				
				try
				{
					deployStatus = deploymentService.deployZip(file.getBytes(), config, authData, env);
				}
				catch (FailedPackageDeploymentException e)
				{
					// throwing an error is a normal way for a failed deployment to exit
					deployStatus = e.getStatus();
				}
				catch (Exception e)
				{
					e.printStackTrace();
					out.write(RestUtil.getRestErrorResponse(Arrays.asList("Error deploying library: " + e.getMessage())));
					return;
				}
				
				if (deployStatus.isSuccess())
				{
					// save the library
					Library lib = deployStatus.getLibrary();
					lib.setSource("External (manual deployment)");
					lib = libService.save(deployStatus.getLibrary(), authData, env);
					
					out.write(RestUtil.getRestSuccessDataResponse("{ \"libraryId\": \"" + lib.getId() + "\" }"));
				}
				else
				{	
					List<String> errors = new ArrayList<String>();
					
					for (FileDeploymentStatus status : deployStatus.getFailedStatuses())
					{
						for (String singleErr : status.getErrors())
						{
							errors.add(status.getFileName() + ": " + JSON.escape(singleErr));
						}
					}
					
					out.write(RestUtil.getRestErrorResponse(errors));
				}
			}
			else
			{
				out.write(RestUtil.getRestErrorResponse(Arrays.asList(authData.getI18n().get("files.emptyfile"))));
				return;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			out.write(RestUtil.getRestErrorResponse(Arrays.asList("Library deployment failed with message: " + e.getMessage())));
			return;
		}
	}
	
	/**
	 * Deploys a package to the environment. The package may be either a regular deployment, or a library. The difference
	 * between the two is recognized by the presence of library.xml file in the zip.
	 * 
	 * If this file is present and it is a library deployment, a library record is created on the environment.
	 * 
	 * @param envId
	 * @param accessToken
	 * @param allowOverwrite
	 * @param requestEntity
	 * @param resp
	 * @param session
	 * @throws IOException
	 * @throws KommetException
	 */
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_DEPLOY_PACKAGE_URL, method = RequestMethod.POST)
	@ResponseBody
	public void deployLibrary(@RequestParam(value = "env", required = false) String envId, 
			@RequestParam(value = "access_token", required = false) String accessToken,
			@RequestParam(value = "allowOverwrite", required = false) String allowOverwrite,
			HttpEntity<byte[]> requestEntity, HttpServletResponse resp, HttpSession session)
			throws IOException, KommetException
	{
		RestInitInfo restInfo = prepareRest(envId, accessToken, session, resp);
		if (!restInfo.isSuccess())
		{
			if (restInfo.getOut() != null)
			{
				returnRestError(restInfo.getError(), restInfo.getOut());
				resp.setStatus(restInfo.getRespCode());
				return;
			}
			else
			{
				throw new KommetException(restInfo.getError());
			}
		}
		
		byte[] payload = requestEntity.getBody();

		try
		{
			OverwriteHandling overwriteHandling = "true".equals(allowOverwrite) ? OverwriteHandling.ALWAYS_OVERWRITE : OverwriteHandling.ALWAYS_REJECT;
			
			PackageDeploymentStatus status = deploymentService.deployZip(payload, new DeploymentConfig(overwriteHandling), restInfo.getAuthData(), restInfo.getEnv());
			
			if (!status.isSuccess())
			{
				List<String> errors = new ArrayList<String>();
				for (FileDeploymentStatus fileStatus : status.getFileStatuses())
				{
					if (!fileStatus.isSuccess())
					{
						for (String err : fileStatus.getErrors())
						{
							errors.add(fileStatus.getFileName() + ": " + err);
						}
					}
				}
				
				returnRestError(errors, restInfo.getOut());
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				envService.clear(restInfo.getEnv().getId());
			}
			else
			{
				resp.setStatus(HttpServletResponse.SC_OK);
				return;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			returnRestError("Error deploying library: " + e.getMessage(), restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			envService.clear(restInfo.getEnv().getId());
			return;
		}
	}
}