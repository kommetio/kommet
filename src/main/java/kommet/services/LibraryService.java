/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.services;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import kommet.auth.AuthData;
import kommet.auth.ProfileService;
import kommet.auth.UserService;
import kommet.basic.Action;
import kommet.basic.App;
import kommet.basic.Layout;
import kommet.basic.Library;
import kommet.basic.LibraryDeactivationException;
import kommet.basic.LibraryException;
import kommet.basic.LibraryItem;
import kommet.basic.MimeTypes;
import kommet.basic.Profile;
import kommet.basic.RecordProxy;
import kommet.basic.ScheduledTask;
import kommet.basic.UniqueCheck;
import kommet.basic.UserGroup;
import kommet.basic.ValidationRule;
import kommet.basic.View;
import kommet.basic.ViewResource;
import kommet.basic.WebResource;
import kommet.basic.actions.ActionService;
import kommet.basic.keetle.LayoutService;
import kommet.basic.keetle.ViewService;
import kommet.dao.LibraryDao;
import kommet.dao.LibraryItemDao;
import kommet.data.ComponentType;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.FileExtension;
import kommet.data.NotNullConstraintViolationException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.UniqueCheckService;
import kommet.data.validationrules.ValidationRuleService;
import kommet.deployment.Deployable;
import kommet.deployment.DeployableType;
import kommet.deployment.DeploymentConfig;
import kommet.deployment.DeploymentProcess;
import kommet.deployment.DeploymentService;
import kommet.deployment.OverwriteHandling;
import kommet.deployment.PackageDeploymentStatus;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.errorlog.ErrorLogService;
import kommet.errorlog.ErrorLogSeverity;
import kommet.files.FileService;
import kommet.filters.LibraryFilter;
import kommet.filters.LibraryItemFilter;
import kommet.filters.WebResourceFilter;
import kommet.koll.ClassService;
import kommet.scheduler.ScheduledTaskService;
import kommet.utils.AppConfig;
import kommet.utils.LibraryZipUtil;
import kommet.utils.ValidationUtil;
import kommet.utils.XMLUtil;

@Service
public class LibraryService
{
	public static final String LIBRARY_METADATA_FILE = "library.xml";

	@Inject
	LibraryDao libDao;
	
	@Inject
	LibraryItemDao liDao;
	
	@Inject
	DataService dataService;
	
	@Inject
	WebResourceService webResourceService;
	
	@Inject
	FileService fileService;
	
	@Inject
	ActionService actionService;
	
	@Inject
	AppService appService;
	
	@Inject
	ViewService viewService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	ValidationRuleService vrService;
	
	@Inject
	UniqueCheckService uniqueCheckService;
	
	@Inject
	UserGroupService ugService;
	
	@Inject
	LayoutService layoutService;
	
	@Inject
	ScheduledTaskService schedulerService;
	
	@Inject
	UserService userService;
	
	@Inject
	EnvService envService;
	
	@Inject
	ViewResourceService viewResourceService;
	
	@Inject
	ClassService classService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	ErrorLogService logService;
	
	@Transactional(readOnly = true)
	public List<Library> findLibraries (LibraryFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		return libDao.find(filter, authData, env);
	}
	
	@Transactional(readOnly = true)
	public List<LibraryItem> getLibraryItems (LibraryItemFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		return liDao.find(filter, authData, env);
	}
	
	@Transactional(readOnly = true)
	public LibraryItem getLibraryItemsFromRecords (Collection<? extends RecordProxy> recordProxies, String itemName, Type type, AuthData authData, EnvData env) throws KommetException
	{
		LibraryItem item = new LibraryItem();
		item.setApiName(itemName);
		item.setComponentType(ComponentType.RECORD_COLLECTION.getId());
		item.setDefinition(DeploymentService.serializeRecords(recordProxies, type, authData, env));
		
		return item;
	}
	
	@Transactional(readOnly = true)
	public List<LibraryItem> getLibraryItemsFromComponents (Collection<Deployable> components, boolean isAddFieldsFromTypes, EnvData env) throws KommetException
	{
		List<LibraryItem> items = new ArrayList<LibraryItem>();
		
		for (Deployable c : components)
		{
			items.add(getLibraryItemFromComponent(c, env));
			
			if (c instanceof Type)
			{
				// include all non-system fields as separate deployable items
				Type type = (Type)c;
				
				if (isAddFieldsFromTypes)
				{
					for (Field field : type.getFields())
					{
						if (!Field.isSystemField(field.getApiName()))
						{
							items.add(getLibraryItemFromComponent(field, env));
						}
					}
				}
			}
		}
		
		return items;
	}
	
	private LibraryItem getLibraryItemFromComponent (Deployable c, EnvData env) throws KommetException
	{
		if (c == null)
		{
			throw new LibraryException("Cannot prepare package item for null component");
		}
		
		LibraryItem item = new LibraryItem();
		
		if (c instanceof Type)
		{
			Type type = (Type)c;
			item.setApiName(type.getQualifiedName());
			item.setRecordId(type.getKID());
			
			DeployableType dt = new DeployableType();
			dt.setApiName(type.getApiName());
			dt.setPackage(type.getPackage());
			dt.setLabel(type.getLabel());
			dt.setPluralLabel(type.getPluralLabel());
			dt.setDefaultFieldApiName(type.getDefaultFieldApiName());
			
			item.setDefinition(DeploymentService.serialize(dt, env));
		}
		else if (c instanceof Field)
		{
			Field field = (Field)c;
			item.setApiName(field.getType().getQualifiedName() + "." + field.getApiName());
			item.setDefinition(DeploymentService.serialize(field, dataService, env));
			item.setRecordId(field.getKID());
		}
		else if (c instanceof kommet.basic.Class)
		{
			kommet.basic.Class cls = (kommet.basic.Class)c;
			item.setApiName(cls.getQualifiedName());
			item.setDefinition(cls.getKollCode());
			item.setRecordId(cls.getId());
		}
		else if (c instanceof View)
		{
			View view = (View)c;
			item.setApiName(view.getQualifiedName());
			item.setDefinition(view.getKeetleCode());
			item.setRecordId(view.getId());
		}
		else if (c instanceof Layout)
		{
			Layout layout = (Layout)c;
			item.setApiName(layout.getName());
			item.setDefinition(layout.getCode());
			item.setRecordId(layout.getId());
		}
		else if (c instanceof ValidationRule)
		{
			ValidationRule vr = (ValidationRule)c;
			item.setApiName(vr.getName());
			item.setDefinition(DeploymentService.serialize(vr, env));
			item.setRecordId(vr.getId());
		}
		else if (c instanceof UniqueCheck)
		{
			UniqueCheck uc = (UniqueCheck)c;
			item.setApiName(uc.getName());
			item.setDefinition(DeploymentService.serialize(uc, env));
			item.setRecordId(uc.getId());
		}
		else if (c instanceof App)
		{
			App app = (App)c;
			item.setApiName(app.getName());
			item.setDefinition(DeploymentService.serialize(app, env));
			item.setRecordId(app.getId());
		}
		else if (c instanceof UserGroup)
		{
			UserGroup group = (UserGroup)c;
			item.setApiName(group.getName());
			item.setDefinition(DeploymentService.serialize(group, env));
			item.setRecordId(group.getId());
		}
		else if (c instanceof Profile)
		{
			Profile profile = (Profile)c;
			item.setApiName(profile.getName());
			item.setDefinition(DeploymentService.serialize(profile, env));
			item.setRecordId(profile.getId());
		}
		else if (c instanceof ScheduledTask)
		{
			ScheduledTask task = (ScheduledTask)c;
			item.setApiName(task.getName());
			item.setDefinition(DeploymentService.serialize(task, env));
			item.setRecordId(task.getId());
		}
		else if (c instanceof ViewResource)
		{
			ViewResource vr = (ViewResource)c;
			item.setApiName(vr.getName());
			item.setDefinition(DeploymentService.serialize(vr, env));
			item.setRecordId(vr.getId());
		}
		else if (c instanceof WebResource)
		{
			WebResource wr = (WebResource)c;
			item.setApiName(wr.getName());
			item.setDefinition(DeploymentService.serialize(wr, env));
			item.setRecordId(wr.getId());
		}
		else if (c instanceof Action)
		{
			Action action = (Action)c;
			item.setApiName(action.getName());
			item.setDefinition(DeploymentService.serialize(action, env));
			item.setRecordId(action.getId());
		}
		else
		{
			throw new LibraryException("Cannot serialize type " + c.getClass().getName() + " into library item");
		}
		
		item.setComponentType(c.getComponentType().getId());
		return item;
	}
	
	/**
	 * Deactivates library. It removes some of its components, while leaving others.
	 * @param lib
	 * @param authData
	 * @param env
	 * @return 
	 * @throws KommetException 
	 */
	@Transactional(rollbackFor = LibraryDeactivationException.class)
	public List<LibraryItemDeleteStatus> deactivateLibrary (Library lib, boolean isForceCleanup, AuthData authData, EnvData env) throws KommetException
	{
		if (lib.getItems() == null || lib.getItems().isEmpty())
		{
			throw new LibraryException("Attempt to deactivate library " + lib.getName() + " with no items");
		}
		
		// list of statuses for each item that is deleted
		List<LibraryItemDeleteStatus> statuses = new ArrayList<LibraryItemDeleteStatus>();
		
		// items to be deleted later - such items as types, fields and profiles are deleted at the end
		Map<ComponentType, List<LibraryItem>> delayedDeleteItems = new LinkedHashMap<ComponentType, List<LibraryItem>>();
		
		// order in the delayedDeleteItems map does matter - items will be removed in the order in which
		// their keys are added to the map below
		delayedDeleteItems.put(ComponentType.CLASS, new ArrayList<LibraryItem>());
		delayedDeleteItems.put(ComponentType.VIEW, new ArrayList<LibraryItem>());
		// fields must be deleted before types, otherwise types referenced by fields may not exist
		// at the time when the field is deleted, which will cause an error
		delayedDeleteItems.put(ComponentType.FIELD, new ArrayList<LibraryItem>());
		delayedDeleteItems.put(ComponentType.TYPE, new ArrayList<LibraryItem>());
		delayedDeleteItems.put(ComponentType.PROFILE, new ArrayList<LibraryItem>());
		
		boolean deletionFailed = false;
		
		for (LibraryItem item : lib.getItems())
		{
			if (item.getRecordId() == null)
			{
				throw new KommetException("Record ID not set for library item " + item.getId() + " / " + item.getApiName());
			}
			
			if (item.getComponentType().equals(ComponentType.TYPE.getId()))
			{
				delayedDeleteItems.get(ComponentType.TYPE).add(item);
				continue;
			}
			else if (item.getComponentType().equals(ComponentType.FIELD.getId()))
			{
				delayedDeleteItems.get(ComponentType.FIELD).add(item);
				continue;
			}
			else if (item.getComponentType().equals(ComponentType.PROFILE.getId()))
			{
				delayedDeleteItems.get(ComponentType.PROFILE).add(item);
				continue;
			}
			else if (item.getComponentType().equals(ComponentType.CLASS.getId()))
			{
				delayedDeleteItems.get(ComponentType.CLASS).add(item);
				continue;
			}
			else if (item.getComponentType().equals(ComponentType.VIEW.getId()))
			{
				delayedDeleteItems.get(ComponentType.VIEW).add(item);
				continue;
			}
			
			LibraryItemDeleteStatus deletionResult = deleteLibraryItem(item, isForceCleanup, authData, env);
			statuses.add(deletionResult);
			
			if (!deletionResult.isDeleted())
			{
				// only types, fields and profiles can be deleted with errors.
				// other items must always be deleted successfully, otherwise, the whole deactivation should fail
				deletionFailed = true;
			}
		}
		
		for (ComponentType itemType : delayedDeleteItems.keySet())
		{
			// delete items
			for (LibraryItem item : delayedDeleteItems.get(itemType))
			{
				LibraryItemDeleteStatus deletionResult = deleteLibraryItem(item, isForceCleanup, authData, env);
				statuses.add(deletionResult);
				
				if (!deletionResult.isDeleted() && !itemType.equals(ComponentType.TYPE) && !itemType.equals(ComponentType.FIELD) && !itemType.equals(ComponentType.PROFILE))
				{
					// only types, fields and profiles can be deleted with errors.
					// other items must always be deleted successfully, otherwise, the whole deactivation should fail
					deletionFailed = true;
				}
			}
		}
		
		if (deletionFailed)
		{
			// throw error instead of simply returning statuses, so that the transaction is rolled back
			throw new LibraryDeactivationException(statuses);
		}
		
		// update library status
		lib.setStatus("Installed-Deactivated");
		lib.setIsEnabled(false);
		save(lib, authData, env);
		
		// for deleted items, update their corresponding LibraryItem by setting recordId to null
		for (LibraryItemDeleteStatus status : statuses)
		{
			if (status.isDeleted())
			{
				LibraryItem item = status.getItem();
				item.setRecordId(null);
				save(item, authData, env);
			}
		}
		
		// return lists of not deleted and deleted items
		return statuses;
	}

	private LibraryItemDeleteStatus deleteLibraryItem (LibraryItem item, boolean isForceCleanup, AuthData authData, EnvData env) throws KommetException
	{
		ComponentType componentType = ComponentType.values()[item.getComponentType()];
		
		LibraryItemDeleteStatus result = new LibraryItemDeleteStatus();
		result.setItem(item);
		
		switch (componentType)
		{
			case TYPE: 
				
				try
				{
					dataService.deleteType(env.getType(item.getRecordId()), authData, env);
					result.setDeleted(true);
					return result;
				}
				catch (NotNullConstraintViolationException e)
				{
					result.setDeleted(false);
					result.setReason("Type could not be deleted because it is in use");
					return result;
				}
				catch (KommetException e)
				{
					logService.logException(e, ErrorLogSeverity.ERROR, this.getClass().getName(), 0, authData.getUserId(), authData, env);
					result.setDeleted(false);
					result.setReason("Type could not be deleted");
					return result;
				}
			
			case FIELD:
				
				try
				{
					Field field = dataService.getField(item.getRecordId(), env);
					
					if (field == null)
					{
						throw new KommetException("Field with ID " + item.getRecordId() + " not found");
					}
					
					Type fieldType = dataService.getType(field.getType().getId(), env);
					
					// by default type is not initialized on the field, so we need to fetch it
					field.setType(fieldType);
					
					// if the field is the default field for its type, deleting it will not be possible, so we need to set a different field as default for this type
					if (fieldType.getDefaultFieldId().equals(field.getKID()))
					{
						// update type
						fieldType.setDefaultFieldId(fieldType.getField(Field.ID_FIELD_NAME).getKID());
						dataService.updateType(fieldType, authData, env);
					}
					
					dataService.deleteField(field, authData, env);
					result.setDeleted(true);
					return result;
				}
				catch (NotNullConstraintViolationException e)
				{
					result.setDeleted(false);
					result.setReason("Field could not be deleted because it is in use");
					return result;
				}
				catch (KommetException e)
				{
					logService.logException(e, ErrorLogSeverity.ERROR, this.getClass().getName(), 0, authData.getUserId(), authData, env);
					result.setDeleted(false);
					result.setReason("Field could not be deleted");
					return result;
				}
				
			case PROFILE:
				
				try
				{
					profileService.delete(item.getRecordId(), authData, env);
					result.setDeleted(true);
					result.setDeleted(true);
					return result;
				}
				catch (NotNullConstraintViolationException e)
				{
					result.setDeleted(false);
					result.setReason("Profile could not be deleted because it is in use. Make sure there are no users with this profile.");
					return result;
				}
				catch (KommetException e)
				{
					logService.logException(e, ErrorLogSeverity.ERROR, this.getClass().getName(), 0, authData.getUserId(), authData, env);
					result.setDeleted(false);
					result.setReason("Profile could not be deleted");
					return result;
				}
				
			case WEB_RESOURCE:
				
				webResourceService.delete(item.getRecordId(), true, authData, env);
				result.setDeleted(true);
				return result;
				
			case ACTION:
				
				actionService.deleteAction(item.getRecordId(), authData, env);
				result.setDeleted(true);
				return result;
				
			case APP:
				
				appService.delete(item.getRecordId(), authData, env, envService.getMasterEnv());
				result.setDeleted(true);
				return result;
				
			case CLASS: 
				
				try
				{
					classService.delete(item.getRecordId(), dataService, authData, env);
					result.setDeleted(true);
					return result;
				}
				catch (NotNullConstraintViolationException e)
				{
					result.setDeleted(false);
					result.setReason("Class could not be deleted because it is in use");
					return result;
				}
				catch (KommetException e)
				{
					logService.logException(e, ErrorLogSeverity.ERROR, this.getClass().getName(), 0, authData.getUserId(), authData, env);
					result.setDeleted(false);
					result.setReason("Class could not be deleted");
					return result;
				}
				
			case VIEW:
				
				try
				{
					viewService.delete(item.getRecordId(), authData, env);
					result.setDeleted(true);
					return result;
				}
				catch (NotNullConstraintViolationException e)
				{
					result.setDeleted(false);
					result.setReason("View could not be deleted because it is in use");
					return result;
				}
				catch (KommetException e)
				{
					logService.logException(e, ErrorLogSeverity.ERROR, this.getClass().getName(), 0, authData.getUserId(), authData, env);
					result.setDeleted(false);
					result.setReason("View could not be deleted");
					return result;
				}
				
			case VALIDATION_RULE: 
				
					vrService.delete(item.getRecordId(), authData, env);
					result.setDeleted(true);
					return result;
					
			case VIEW_RESOURCE:
				
					viewResourceService.delete(item.getRecordId(), authData, env);
					result.setDeleted(true);
					return result;
					
			case LAYOUT:
				
					layoutService.delete(item.getRecordId(), authData, env);
					result.setDeleted(true);
					return result;
					
			case SCHEDULED_TASK:
				
					schedulerService.unschedule(item.getRecordId(), true, env);
					result.setDeleted(true);
					return result;
					
			case UNIQUE_CHECK:
				
					uniqueCheckService.delete(item.getRecordId(), authData, env);
					result.setDeleted(true);
					return result;
					
			case USER_GROUP:
				
					ugService.delete(item.getRecordId(), authData, env);
					result.setDeleted(true);
					return result;
					
			default: throw new LibraryException("Cannot delete item of unrecognized type " + componentType);
		}
	}

	@Transactional
	public byte[] createLibraryPackage(Library lib, AuthData authData, EnvData env) throws KommetException
	{
		Map<String, String> files = new HashMap<String, String>();
		Map<String, String> byteFilePaths = new HashMap<String, String>();
		
		for (LibraryItem item : lib.getItems())
		{
			ComponentType componentType = ComponentType.values()[item.getComponentType()];
			
			if (!StringUtils.hasText(item.getDefinition()))
			{
				throw new LibraryException("Empty definition for packaged item " + item.getApiName());
			}
			
			String itemName = item.getApiName() + "." + FileExtension.fromComponentType(componentType);
			
			// make sure the file does not already exist
			if (files.containsKey(itemName))
			{
				throw new LibraryException("Duplicate item " + itemName + " in library");
			}
			
			files.put(itemName, item.getDefinition());
			
			// if it is a web resource, the metadata XML file does not contain its actual byte content, so we want to
			// create another purely byte file for storing this web resource
			if (componentType == ComponentType.WEB_RESOURCE)
			{
				// find web resource
				WebResourceFilter filter = new WebResourceFilter();
				filter.setName(item.getApiName());
				List<WebResource> webResources = webResourceService.find(filter, env);
				
				if (webResources.isEmpty())
				{
					throw new LibraryException("Could not find web resource with name " + item.getApiName());
				}
				
				webResources = webResourceService.initFilePath(webResources, authData, env);
				
				File diskFile = new File(appConfig.getFileDir() + "/" + webResources.get(0).getDiskFilePath());
				if (!diskFile.exists())
				{
					throw new LibraryException("Could not package web resource " + item.getApiName() + ". Disk file representing the resource not found");
				}
				
				// the key in the map is the full item path within the zip file
				byteFilePaths.put(DeploymentProcess.SRC_DIR + "\\" + LibraryZipUtil.typeDir(FileExtension.WEB_RESOURCE_EXT) + "\\files\\" + item.getApiName() + "." + MimeTypes.getExtension(webResources.get(0).getMimeType()) + "", diskFile.getAbsolutePath());
			}
		}
		
		// add library metadata file to the package
		files.put(LIBRARY_METADATA_FILE, getLibraryDefinitionXML(lib));
		
		try
		{
			return LibraryZipUtil.createZip(files, byteFilePaths);
		}
		catch (IOException e)
		{
			throw new LibraryException("Could not create zip file for package. Nested: " + e.getMessage());
		}
	}

	private String getLibraryDefinitionXML(Library lib) throws LibraryException
	{
		DocumentBuilderFactory dFact = DocumentBuilderFactory.newInstance();
        DocumentBuilder build;
		
        try
		{
			build = dFact.newDocumentBuilder();
		}
		catch (ParserConfigurationException e)
		{
			throw new LibraryException("Could not build document. Nested: " + e.getMessage());
		}
		
        Document doc = build.newDocument();
        Element root = doc.createElement("library");
        doc.appendChild(root);
        
        // append type-specific elements
        XMLUtil.addElement(doc, root, "name", lib.getName());
        XMLUtil.addElement(doc, root, "version", lib.getVersion());
        XMLUtil.addElement(doc, root, "provider", lib.getProvider());
        XMLUtil.addElement(doc, root, "description", lib.getDescription());
        XMLUtil.addElement(doc, root, "accessLevel", lib.getAccessLevel());
        
        try
        {
			TransformerFactory tFact = TransformerFactory.newInstance();
			Transformer trans = tFact.newTransformer();
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			DOMSource source = new DOMSource(doc);
			trans.transform(source, result);
			
			return writer.toString();
        }
        catch (TransformerException e)
        {
        	throw new LibraryException("Could not transform type XML into string. Nested: " + e.getMessage());
        }
	}

	@Transactional
	public Library save (Library lib, AuthData authData, EnvData env) throws KommetException
	{	
		if (!StringUtils.hasText(lib.getName()))
		{
			throw new LibraryException("Library name is empty");
		}
		
		if (!ValidationUtil.isValidLibraryName(lib.getName()))
		{
			throw new LibraryException("Invalid library name. Library name must be a valid fully qualified name - must start with a package name followed by the actual name");
		}
		
		if ((lib.getItems() == null || lib.getItems().isEmpty()) && !"Local".equals(lib.getSource()))
		{
			throw new LibraryException("Cannot save non-local library with no items");
		}
		
		lib = libDao.save(lib, authData, env);
		
		if (lib.getItems() != null)
		{
			for (LibraryItem item : lib.getItems())
			{
				liDao.save(item, authData, env);
			}
		}
		
		return lib;
	}

	@Transactional
	public LibraryItem save (LibraryItem item, AuthData authData, EnvData env) throws KommetException
	{
		return liDao.save(item, authData, env);
	}
	
	public Library getLibrary(KID id, AuthData authData, EnvData env) throws KommetException
	{
		return getLibrary(id, false, authData, env);
	}

	public Library getLibrary(KID id, boolean initItems, AuthData authData, EnvData env) throws KommetException
	{
		LibraryFilter filter = new LibraryFilter();
		filter.addLibId(id);
		filter.setInitItems(true);
		List<Library> libs = libDao.find(filter, authData, env);
		
		Library lib = libs.isEmpty() ? null : libs.get(0); 
		
		if (lib != null && initItems)
		{
			LibraryItemFilter liFilter = new LibraryItemFilter();
			liFilter.setLibraryId(id);
			lib.setItems((ArrayList<LibraryItem>)liDao.find(liFilter, authData, env));
		}
		
		return lib;
	}

	@Transactional
	public void delete(KID id, AuthData authData, EnvData env) throws KommetException
	{
		libDao.delete(id, authData, env);
	}
	
	public class LibraryItemDeleteStatus
	{
		private LibraryItem item;
		private boolean isDeleted;
		private String reason;
		
		public LibraryItem getItem()
		{
			return item;
		}
		
		public void setItem(LibraryItem item)
		{
			this.item = item;
		}
		
		public String getReason()
		{
			return reason;
		}
		
		public void setReason(String reason)
		{
			this.reason = reason;
		}
		
		public boolean isDeleted()
		{
			return isDeleted;
		}
		
		public void setDeleted(boolean isDeleted)
		{
			this.isDeleted = isDeleted;
		}
	}

	@Transactional
	public void delete(List<LibraryItem> items, AuthData authData, EnvData env) throws KommetException
	{
		liDao.delete(items, authData, env);
	}

	@Transactional
	public PackageDeploymentStatus activate(KID libId, DeploymentService deploymentService, AuthData authData, EnvData env) throws KommetException
	{
		Library oldLibrary = getLibrary(libId, authData, env);
		byte[] libPackage = createLibraryPackage(oldLibrary, authData, env);
		
		// activation of a library is simply deploying it again
		PackageDeploymentStatus status = deploymentService.deployZip(libPackage, new DeploymentConfig(OverwriteHandling.ALWAYS_OVERWRITE), authData, env);
		
		if (status.isSuccess())
		{	
			// remove items from the old library
			delete(oldLibrary.getItems(), authData, env);
			
			Library newLibrary = status.getLibrary();
			
			// assign items from the new library to the old one
			for (LibraryItem newItem : newLibrary.getItems())
			{
				newItem.setLibrary(oldLibrary);
			}
			
			oldLibrary.setStatus("Installed");
			oldLibrary.setIsEnabled(true);
			oldLibrary.setItems(newLibrary.getItems());
			save(oldLibrary, authData, env);
		}
		
		return status;
	}
}