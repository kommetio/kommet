/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.deployment;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import kommet.auth.AuthData;
import kommet.auth.ProfileService;
import kommet.auth.UserService;
import kommet.basic.Action;
import kommet.basic.App;
import kommet.basic.AppUrl;
import kommet.basic.Class;
import kommet.basic.File;
import kommet.basic.Layout;
import kommet.basic.Library;
import kommet.basic.LibraryItem;
import kommet.basic.Profile;
import kommet.basic.ScheduledTask;
import kommet.basic.UniqueCheck;
import kommet.basic.UserGroup;
import kommet.basic.ValidationRule;
import kommet.basic.View;
import kommet.basic.ViewResource;
import kommet.basic.WebResource;
import kommet.basic.actions.ActionService;
import kommet.basic.keetle.LayoutService;
import kommet.basic.keetle.ViewFilter;
import kommet.basic.keetle.ViewService;
import kommet.dao.FieldFilter;
import kommet.dao.LibraryDao;
import kommet.data.ComponentType;
import kommet.data.DataService;
import kommet.data.ExceptionErrorType;
import kommet.data.Field;
import kommet.data.FileExtension;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.TypeFilter;
import kommet.data.UniqueCheckService;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.AutoNumber;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.FormulaDataType;
import kommet.data.datatypes.FormulaReturnType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.MultiEnumerationDataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.data.validationrules.ValidationRuleService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.files.FileService;
import kommet.filters.LibraryFilter;
import kommet.filters.QueryResultOrder;
import kommet.koll.ClassFilter;
import kommet.koll.ClassService;
import kommet.scheduler.ScheduledTaskException;
import kommet.scheduler.ScheduledTaskService;
import kommet.services.AppService;
import kommet.services.LibraryService;
import kommet.services.UserGroupService;
import kommet.services.ViewResourceService;
import kommet.services.WebResourceService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.XMLUtil;

/**
 * Utility for parsing XML type definitions and converting them into type/field
 * objects.
 * 
 * @author Radek Krawiec
 * @since 08/01/2016
 */
@Service
public class DeploymentProcess
{
	public static final String CLASS_DIR = "classes";
	public static final String VIEW_DIR = "views";
	public static final String TYPE_DIR = "types";
	public static final String FIELD_DIR = "fields";
	public static final String SRC_DIR = "src";
	public static final String LAYOUT_DIR = "layouts";
	public static final String VALIDATION_RULE_DIR = "validationrules";
	public static final String UNIQUE_CHECK_DIR = "uniquechecks";
	public static final String APP_DIR = "apps";
	public static final String SCHEDULED_TASK_DIR = "scheduledtasks";
	public static final String USER_GROUP_DIR = "usergroups";
	public static final String PROFILE_DIR = "profiles";
	public static final String WEB_RESOURCE_DIR = "webresources";
	public static final String VIEW_RESOURCE_DIR = "viewresources";
	public static final String ACTION_DIR = "actions";
	public static final String RECORDS_DIR = "records";
	
	@Inject
	ClassService classService;
	
	@Inject
	ViewService viewService;
	
	@Inject
	DataService dataService;
	
	@Inject
	ActionService actionService;
	
	@Inject
	WebResourceService webResourceService;
	
	@Inject
	ViewResourceService viewResourceService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	AppService appService;
	
	@Inject
	EnvService envService;
	
	@Inject
	ValidationRuleService vrService;
	
	@Inject
	UniqueCheckService uniqueCheckService;
	
	@Inject
	UserGroupService ugService;
	
	@Inject
	LayoutService layoutService;
	
	@Inject
	FileService fileService;
	
	@Inject
	ScheduledTaskService schedulerService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	UserService userService;
	
	@Inject
	LibraryDao libDao;
	
	@Resource
	private PlatformTransactionManager transactionManager;

	private DeployedField parseFieldXML(String xml, DeploymentConfig deployConfig, EnvData env) throws DeploymentException
	{
		Document doc = null;

		try
		{
			doc = loadXMLFromString(xml);
		}
		catch (Exception e)
		{
			throw new DeploymentException("Error loading XML from string. Nested: " + e.getMessage());
		}

		doc.getDocumentElement().normalize();
		
		Field field = new Field();
		
		NodeList nList = doc.getElementsByTagName("field");
		
		if (nList.getLength() != 1)
		{
			throw new DeploymentException("Expected exactly one field element in XML definition, found " + nList.getLength() + " such elements");
		}
		
		Element fieldElem = (Element)nList.item(0);

		try
		{
			field.setApiName(XMLUtil.getSingleNodeValue("apiName", fieldElem));
			field.setLabel(XMLUtil.getSingleNodeValue("label", fieldElem));
			field.setUchLabel(XMLUtil.getSingleNodeValue("uchLabel", fieldElem));
			field.setDescription(XMLUtil.getSingleNodeValue("description", fieldElem));
			field.setRequired("true".equals(XMLUtil.getSingleNodeValue("required", fieldElem)));
			field.setTrackHistory("true".equals(XMLUtil.getSingleNodeValue("trackHistory", fieldElem)));
			
			String typeName = XMLUtil.getSingleNodeValue("type", fieldElem);
			
			if (!StringUtils.hasText(typeName))
			{
				throw new DeploymentException("Type not defined on field " + field.getApiName());
			}
			else
			{
				if (StringUtils.hasText(deployConfig.getPackagePrefix()))
				{
					// add prefix to field name
					typeName = deployConfig.getPackagePrefix() + "." + typeName;
				}
				
				if (env.getType(typeName) == null)
				{
					throw new DeploymentException("Type with name " + typeName + " referenced by field " + field.getApiName() + " not found in package or on env");
				}
			}
			
			// read the type and initialize its fields - we might need them when we parse formula
			// fields that are being deployed
			Type type = dataService.getTypeByName(typeName, true, env);
			
			parseFieldDataType(field, fieldElem, type, env);
			
			DeployedField deployedField = new DeployedField();
			deployedField.setField(field);
			deployedField.setUserSpecificTypeName(typeName);
			deployedField.setDefinition(xml);
			
			return deployedField;
		}
		catch (DeploymentException e)
		{
			throw e;
		}
		catch (KommetException e1)
		{
			e1.printStackTrace();
			throw new DeploymentException(e1.getMessage());
		}
	}

	private void parseFieldDataType(Field field, Element fieldElem, Type type, EnvData env) throws KommetException
	{	
		NodeList nList = fieldElem.getElementsByTagName("dataType");
		if (nList.getLength() != 1)
		{
			throw new DeploymentException("Expected exactly one data type element in field " + field.getApiName() + ", found " + nList.getLength());
		}
		
		Element dtElem = (Element)nList.item(0);
		
		String dtName = XMLUtil.getSingleNodeValue("name", dtElem);
		if (!StringUtils.hasText(dtName))
		{
			throw new DeploymentException("Data type name not found in definition of field " + field.getApiName());
		}
		
		DataType dt = DataType.getByName(dtName);
		
		if (dt == null)
		{
			throw new DeploymentException("Unknown data type name " + dtName + " for field " + field.getApiName());
		}
		
		switch (dt.getId())
		{
			case DataType.NUMBER: 
				String sDecPlaces = XMLUtil.getSingleNodeValue("decimalPlaces", dtElem);
				
				try
				{
					((NumberDataType)dt).setDecimalPlaces(Integer.parseInt(sDecPlaces));
				}
				catch (NumberFormatException e)
				{
					throw new DeploymentException("Invalid decimal places number " + sDecPlaces + " for field " + field.getApiName());
				}
				
				String javaType = XMLUtil.getSingleNodeValue("javaType", dtElem);
				
				try
				{
					// make sure the class exists
					java.lang.Class.forName(javaType);
				}
				catch (ClassNotFoundException e1)
				{
					throw new DeploymentException("Java type " + javaType + " for numeric field " + field.getApiName() + " not found");
				}
				
				if (!NumberDataType.isValidJavaType(javaType))
				{
					throw new DeploymentException("Invalid Java type " + javaType + " for numeric field " + field.getApiName());
				}
				
				((NumberDataType)dt).setJavaType(javaType);
				
				break;
			case DataType.TEXT:
				String sTextLength = XMLUtil.getSingleNodeValue("length", dtElem);
				
				try
				{
					((TextDataType)dt).setLength(Integer.parseInt(sTextLength));
				}
				catch (NumberFormatException e)
				{
					throw new DeploymentException("Invalid text field length " + sTextLength + " for field " + field.getApiName());
				}
				
				break;
			case DataType.BOOLEAN: break;
			case DataType.DATETIME: break;
			case DataType.DATE: break;
			case DataType.KOMMET_ID: break;
			case DataType.EMAIL: break;
			case DataType.BLOB: break;
			case DataType.TYPE_REFERENCE:
					((TypeReference)dt).setCascadeDelete("true".equals(XMLUtil.getSingleNodeValue("cascadeDelete", dtElem)));
					String typeName = XMLUtil.getSingleNodeValue("type", dtElem);
					Type referencedType = dataService.getTypeByName(typeName, false, env);
					
					if (referencedType == null)
					{
						throw new DeploymentException("Type " + typeName + " referenced by type reference field " + type.getQualifiedName() + "." + field.getApiName() + " not found");
					}
					
					((TypeReference)dt).setType(referencedType);
					break;
			case DataType.ENUMERATION:
				
					((EnumerationDataType)dt).setValidateValues("true".equals(XMLUtil.getSingleNodeValue("validateValues", dtElem)));
				
					nList = dtElem.getElementsByTagName("values");
					if (nList.getLength() != 1)
					{
						throw new DeploymentException("Expected exactly one values tag in data type definition for field " + field.getApiName() + ", found " + nList.getLength() + " such tags");
					}
					
					Element valuesElem = (Element)nList.item(0);
					
					NodeList valueNodes = valuesElem.getElementsByTagName("value");
					List<String> valueStrings = new ArrayList<String>();
					for (int i = 0; i < valueNodes.getLength(); i++)
					{
						valueStrings.add(valueNodes.item(i).getTextContent());
					}
					
					((EnumerationDataType)dt).setValues(MiscUtils.implode(valueStrings, "\n"));
					
					break;
			case DataType.MULTI_ENUMERATION:
			
					nList = dtElem.getElementsByTagName("values");
					if (nList.getLength() != 1)
					{
						throw new DeploymentException("Expected exactly one values tag in data type definition for field " + field.getApiName() + ", found " + nList.getLength() + " such tags");
					}
					
					valuesElem = (Element)nList.item(0);
					
					valueNodes = valuesElem.getElementsByTagName("value");
					Set<String> valueSet = new HashSet<String>();
					for (int i = 0; i < valueNodes.getLength(); i++)
					{
						valueSet.add(valueNodes.item(i).getTextContent());
					}
					
					((MultiEnumerationDataType)dt).setValues(valueSet);
					
					break;
			case DataType.INVERSE_COLLECTION:
					String inverseTypeName = XMLUtil.getSingleNodeValue("inverseType", dtElem);
					Type inverseType = dataService.getTypeByName(inverseTypeName, false, env);
					
					if (inverseType == null)
					{
						throw new DeploymentException("Type " + inverseTypeName + " referenced by inverse collection field " + type.getQualifiedName() + "." + field.getApiName() + " not found");
					}
					
					((InverseCollectionDataType)dt).setInverseType(inverseType);
					((InverseCollectionDataType)dt).setInverseProperty(XMLUtil.getSingleNodeValue("inverseProperty", dtElem));
					break;
			case DataType.ASSOCIATION:
					String assocTypeName = XMLUtil.getSingleNodeValue("associatedType", dtElem);
					Type associatedType = dataService.getTypeByName(assocTypeName, false, env);
					
					if (associatedType == null)
					{
						throw new DeploymentException("Type " + assocTypeName + " referenced by association field " + type.getQualifiedName() + "." + field.getApiName() + " not found");
					}
					
					((AssociationDataType)dt).setAssociatedType(associatedType);
					
					String linkingTypeName = XMLUtil.getSingleNodeValue("linkingType", dtElem);
					Type linkingType = dataService.getTypeByName(linkingTypeName, false, env);
					
					if (linkingType == null)
					{
						throw new DeploymentException("Type " + linkingTypeName + " referenced by association field " + type.getQualifiedName() + "." + field.getApiName() + " not found");
					}
					
					((AssociationDataType)dt).setLinkingType(linkingType);
					
					((AssociationDataType)dt).setSelfLinkingField(XMLUtil.getSingleNodeValue("selfLinkingField", dtElem));
					((AssociationDataType)dt).setForeignLinkingField(XMLUtil.getSingleNodeValue("foreignLinkingField", dtElem));
					break;
			case DataType.FORMULA:
					FormulaReturnType returnType = FormulaReturnType.valueOf(XMLUtil.getSingleNodeValue("returnType", dtElem));
					String userDefinition = XMLUtil.getSingleNodeValue("definition", dtElem);
					dt = new FormulaDataType(returnType, userDefinition, type, env);
					break;
			case DataType.AUTO_NUMBER:
					String format = XMLUtil.getSingleNodeValue("format", dtElem);
					((AutoNumber)dt).setFormat(format);
					break;
					
			default: throw new KommetException("No data type exists for ID " + dt.getId());
		}
		
		field.setDataType(dt);
	}

	private DeployedType parseTypeXML(String xml, EnvData env) throws DeploymentException
	{
		Document doc = null;

		try
		{
			doc = loadXMLFromString(xml);
		}
		catch (Exception e)
		{
			throw new DeploymentException("Error loading XML from string. Nested: " + e.getMessage());
		}

		doc.getDocumentElement().normalize();
		
		NodeList nList = doc.getElementsByTagName("type");
		
		if (nList.getLength() != 1)
		{
			throw new DeploymentException("Expected exactly one type element in XML definition, found " + nList.getLength() + " such elements");
		}
		
		Element typeElem = (Element)nList.item(0);
		
		Type type = new Type();

		try
		{
			// read api name
			type.setApiName(XMLUtil.getSingleNodeValue("apiName", typeElem));

			// read package
			type.setPackage(XMLUtil.getSingleNodeValue("package", typeElem));

			// label
			type.setLabel(XMLUtil.getSingleNodeValue("label", typeElem));
			type.setUchLabel(XMLUtil.getSingleNodeValue("uchLabel", typeElem));
			type.setUchPluralLabel(XMLUtil.getSingleNodeValue("uchPluralLabel", typeElem));
			type.setDescription(XMLUtil.getSingleNodeValue("description", typeElem));

			// plural label
			type.setPluralLabel(XMLUtil.getSingleNodeValue("pluralLabel", typeElem));

			// Note that key prefix and DB table are not deployable
			// type.setKeyPrefix(KeyPrefix.get(XMLUtil.getSingleNodeValue("keyPrefix", doc)));
			// type.setDbTable(XMLUtil.getSingleNodeValue("dbTable", doc));
			
			type.setCombineRecordAndCascadeSharing("true".equals(XMLUtil.getSingleNodeValue("combineRecordAndCascadeSharing", typeElem)));
			
			DeployedType deployedType = new DeployedType();
			deployedType.setType(type);
			deployedType.setDefaultFieldApiName(XMLUtil.getSingleNodeValue("defaultFieldApiName", typeElem));
			deployedType.setSharingControlledByFieldApiName(XMLUtil.getSingleNodeValue("sharingControlledByFieldApiName", typeElem));

			return deployedType;
		}
		catch (Exception e)
		{
			throw new DeploymentException(e.getMessage());
		}
	}

	private static Document loadXMLFromString(String xml) throws Exception
	{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(xml));
		return builder.parse(is);
	}
	
	private static String pathToQualifiedName (String fileName)
	{
		// first unify all backslashas to slashes
		fileName = fileName.replace("\\", "/");
		
		// at this point the file name has prefix src/[item-specific-dir]/item/qualified/name.ext
		// e.g. src/classes/kommet/something/MyClass.koll
		// so we need to extract the bare qualified name from it
		
		if (fileName.startsWith(DeploymentProcess.SRC_DIR + "/"))
		{
			fileName = fileName.substring((DeploymentProcess.SRC_DIR + "/").length());
		}
		
		fileName = fileName.substring(fileName.indexOf("/") + 1);
		
		// now we have a qualified name, but it contains backslashes instead of dots
		return fileName.replace("/", ".");
	}
	
	public static Library readLibraryMetadata(byte[] packageFile) throws KommetException
	{
		return readLibraryMetadata(new ZipInputStream(new ByteArrayInputStream(packageFile)));
	}
	
	public static Library readLibraryMetadata(ZipInputStream zis) throws KommetException
	{
		try
		{
	    	ZipEntry ze = null;
	    	
	    	// iterate over files in the zip
	    	while ((ze = zis.getNextEntry()) != null)
	    	{
	    		if (ze.isDirectory())
	    		{
	    			// skip directories
	    			continue;
	    		}
	    		
	    		String fileName = pathToQualifiedName(ze.getName());
	    	    String fileContent = IOUtils.toString(zis);
	    	    
	    	    if (LibraryService.LIBRARY_METADATA_FILE.equals(fileName))
	    	    {
	    	    	return parseLibraryMetaFile(fileContent);
	    	    }
	    	    else
	    	    {
	    	    	continue;
	    	    }
	    	}
		}
		catch (IOException e)
		{
			throw new KommetException("Error reading zip file: " + e.getMessage(), e);
		}
		
		return null;
	}

	/**
	 * Deploys a zip file containing a packed directory of components.
	 * @param zipFile
	 * @throws FileNotFoundException 
	 * @throws KommetException 
	 * @throws SQLException 
	 */
	@Transactional//(propagation = Propagation.REQUIRES_NEW, rollbackFor = FailedPackageDeploymentException.class)
	public PackageDeploymentStatus deploy(ZipInputStream zis, DeploymentConfig deployConfig, DeploymentCleanupItems cleanUpItems, AuthData authData, EnvData env, EnvData sharedEnv) throws KommetException
	{	
		// if this package is deployed as a library, this var will contain the definition of the deployed library
		// otherwise, if it is a regular package deployment, the var will remain null
		Library deployedLib = null;
		
		// deployment statuses for specific files - each status object will contain the deployment action result for a single item
		List<FileDeploymentStatus> statuses = new ArrayList<FileDeploymentStatus>();
		
		// files/items mapped by their extension
		Map<String, List<ZippedFile>> zippedFilesByExt = new HashMap<String, List<ZippedFile>>();
		Map<String, File> webResourceFiles = new HashMap<String, File>();
		
		try
		{
	    	ZipEntry ze = null;
	    	
	    	// iterate over files in the zip
	    	while ((ze = zis.getNextEntry()) != null)
	    	{
	    		if (ze.isDirectory())
	    		{
	    			// skip directories
	    			continue;
	    		}
	    		
	    		String fileName = pathToQualifiedName(ze.getName());
	    	    
	    	    String[] fileNameParts = MiscUtils.splitFileName(fileName);
	    	    String ext = fileNameParts[1];
	    	    String itemName = fileNameParts[0];
	    	    String fileContent = IOUtils.toString(zis);
	    	    
	    	    if (LibraryService.LIBRARY_METADATA_FILE.equals(fileName))
	    	    {
	    	    	deployedLib = parseLibraryMetaFile(fileContent);
	    	    	
	    	    	// skip further processing of this file
	    	    	continue;
	    	    }
	    	    
	    	    if (!FileExtension.allExtensions().contains(ext))
	    	    {
	    	    	// directories may use slashes or backslashes, but we want this to be consistent
	    	    	String zeName = ze.getName().replaceAll("/", "\\\\");
	    	    	
	    	    	// The file has unrecognized extension, so it means it does not represent any deployable Kommet component.
	    	    	// However, it may be some web resource file, e.g. a png graphic. If this is the case, the full file name will start with
	    	    	// {@link DeploymentProcess.WEB_RESOURCE_DIR} followed by "\\files\\"
	    	    	if (zeName.startsWith(SRC_DIR + "\\" + WEB_RESOURCE_DIR + "\\files\\"))
	    	    	{
	    	    		java.io.File diskFile = new java.io.File(appConfig.getFileDir() + "/" + MiscUtils.getHash(30) + "." + ext);
	    	    		FileOutputStream fos = new FileOutputStream(diskFile);
	    	    		fos.write(IOUtils.toByteArray(zis));
	    	    		fos.close();
	    	    		
	    	    		// add disk file to the clean up items
	    	    		cleanUpItems.addDiskFile(diskFile.getAbsolutePath());
	    	    		
	    	    		File insertedFile = fileService.saveFile(null, itemName.substring("files.".length()), diskFile.getName(), File.PUBLIC_ACCESS, true, authData, env); 	    		
	    	    		//webResourceFiles.put(MiscUtils.trimExtension(fileName.substring("files.".length())), insertedFile);
	    	    		webResourceFiles.put(itemName.substring("files.".length()), insertedFile);
	    	    	}
	    	    	else
	    	    	{
	    	    		throw new DeploymentException("Unrecognized file extension " + ext + " in packaged file '" + fileName + "'");
	    	    	}
	    	    }
	    	    
	    	    if (!zippedFilesByExt.containsKey(ext))
    	    	{
    	    		zippedFilesByExt.put(ext, new ArrayList<ZippedFile>());
    	    	}
	    	    
	    	    zippedFilesByExt.get(ext).add(new ZippedFile(itemName, fileContent));
	    	}
		}
		catch (IOException e)
		{
			throw new KommetException("Error reading zip file: " + e.getMessage(), e);
		}
		
		// make sure a library with the given name does not already exist
		if (deployedLib != null)
		{
			// search for libraries with the same name
			LibraryFilter libFilter = new LibraryFilter();
			libFilter.setName(deployedLib.getName());
			
			List<Library> existingLibs = libDao.find(libFilter, authData, env); 
			
			if (!existingLibs.isEmpty())
			{
				deployedLib.setId(existingLibs.get(0).getId());
				
				if (!OverwriteHandling.ALWAYS_OVERWRITE.equals(deployConfig.getOverwriteHandling()))
				{
					// we allow for the new library to overwrite all the properties of the existing one
					throw new FailedPackageDeploymentException(new PackageDeploymentStatus(false, "Library with name " + deployedLib.getName() + " already exists", deployedLib));
				}
			}
		}
		
		// types inserted (not updated in this package by their env name)
		Map<String, DeployedType> newTypesByEnvName = new HashMap<String, DeploymentProcess.DeployedType>();
		
		List<DeployedType> upsertedTypes = new ArrayList<DeploymentProcess.DeployedType>();
		
		// now that we have all files buffered, we start with deploying types
		if (zippedFilesByExt.containsKey(FileExtension.TYPE_EXT))
		{
			List<ZippedFile> typeFiles = zippedFilesByExt.get(FileExtension.TYPE_EXT);
			for (ZippedFile typeFile : typeFiles)
			{
				// parse type definition from XML
				DeployedType deployedType = parseTypeXML(typeFile.getContent(), env);
				
				if (StringUtils.hasText(deployConfig.getPackagePrefix()))
				{
					// add prefix to type name
					deployedType.getType().setPackage(deployConfig.getPackagePrefix() + "." + deployedType.getType().getPackage());
				}
				
				// check if type with this name already exists
				Type existingType = dataService.getTypeByName(deployedType.getType().getQualifiedName(), false, env);
				
				// nullify field references because they reference field KIDs from the old environment
				deployedType.getType().setSharingControlledByFieldId(null);
				deployedType.getType().setDefaultFieldId(null); 
				
				if (existingType != null)
				{	
					// update the existing type
					deployedType.getType().setKID(existingType.getKID());
					deployedType.getType().setId(existingType.getId());
					deployedType.getType().setKeyPrefix(existingType.getKeyPrefix());
					deployedType.getType().setDbTable(existingType.getDbTable());
					deployedType.getType().setCreated(new Date(existingType.getCreated().getTime()));
					
					try
					{
						Type updatedType = dataService.updateType(deployedType.getType(), authData, env);
						
						// now fetch the type again, so that now it has all its fields set such as DB table, key prefix etc.
						// these values will be needed when subsequent updates are made on the type when fields are deployed
						updatedType = dataService.getType(updatedType.getKID(), env);
						deployedType.setType(updatedType);
						statuses.add(new FileDeploymentStatus(true, deployedType.getType().getQualifiedName(), typeFile.getContent(), ComponentType.TYPE, null, updatedType.getKID()));
					}
					catch (KommetException e)
					{
						e.printStackTrace();
						statuses.add(new FileDeploymentStatus(false, deployedType.getType().getQualifiedName(), typeFile.getContent(), ComponentType.TYPE, e.getMessage(), null));
						return new PackageDeploymentStatus(false, statuses, deployedLib);
					}
				}
				else
				{
					try
					{
						// insert the type
						Type insertedType = dataService.createType(deployedType.getType(), authData, env);
						
						// now fetch the type again, so that now it has all its fields set such as DB table, key prefix etc.
						// these values will be needed when subsequent updates are made on the type when fields are deployed
						insertedType = dataService.getType(insertedType.getKID(), env);
						deployedType.setType(insertedType);
						
						statuses.add(new FileDeploymentStatus(true, deployedType.getType().getQualifiedName(), typeFile.getContent(), ComponentType.TYPE, null, insertedType.getKID()));
					}
					catch (KommetException e)
					{
						statuses.add(new FileDeploymentStatus(false, deployedType.getType().getQualifiedName(), typeFile.getContent(), ComponentType.TYPE, e.getMessage(), null));
						return new PackageDeploymentStatus(false, statuses, deployedLib);
					}
					
					newTypesByEnvName.put(deployedType.getType().getQualifiedName(), deployedType);
				}
				
				upsertedTypes.add(deployedType);
			}
		}
		
		// upserted fields by their combined name, i.e. type user name + field API name
		Map<String, DeployedField> upsertedFields = new HashMap<String, DeploymentProcess.DeployedField>();
		
		// now deploy fields
		if (zippedFilesByExt.containsKey(FileExtension.FIELD_EXT))
		{
			List<ZippedFile> fieldFiles = zippedFilesByExt.get(FileExtension.FIELD_EXT);
			
			// keep fields that reference other types in separate lists, because they need to be deployed
			// after other field types, in specific order, to account for field dependencies (association fields
			// require type reference fields to be inserted earlier)
			List<DeployedField> objectRefFields = new ArrayList<DeployedField>();
			List<DeployedField> inverseFields = new ArrayList<DeployedField>();
			List<DeployedField> assocFields = new ArrayList<DeployedField>();
			List<DeployedField> otherDataTypeFields = new ArrayList<DeployedField>();
			
			for (ZippedFile fieldFile : fieldFiles)
			{
				// parse field definition from XML
				DeployedField deployedField = parseFieldXML(fieldFile.getContent(), deployConfig, env);
				
				if (DataType.TYPE_REFERENCE == deployedField.getField().getDataTypeId())
				{
					objectRefFields.add(deployedField);
				}
				else if (DataType.INVERSE_COLLECTION == deployedField.getField().getDataTypeId())
				{
					inverseFields.add(deployedField);
				}
				else if (DataType.ASSOCIATION == deployedField.getField().getDataTypeId())
				{
					assocFields.add(deployedField);
				}
				else
				{
					otherDataTypeFields.add(deployedField);
				}
			}
			
			// create a combined list of fields in specific order:
			// - first normal data types
			// - then type references
			// - inverse collections
			// - associations
			List<DeployedField> allFieldsInOrder = new ArrayList<DeploymentProcess.DeployedField>();
			allFieldsInOrder.addAll(otherDataTypeFields);
			allFieldsInOrder.addAll(objectRefFields);
			allFieldsInOrder.addAll(inverseFields);
			allFieldsInOrder.addAll(assocFields);
			
			for (DeployedField deployedField : allFieldsInOrder)
			{
				if (!StringUtils.hasText(deployedField.getUserSpecificTypeName()))
				{
					throw new DeploymentException("Field " + deployedField.getField().getApiName() + " does not have type specified");
				}
				
				String typeName = deployedField.getUserSpecificTypeName();
				
				boolean isNewField = false;
				
				// fill type information for types that were just inserted
				fillMissingTypeData(deployedField, env);
				
				// update the type reference to the type inserted to the new environment
				// if this type has been inserted with this package
				// otherwise leave the original type reference
				if (newTypesByEnvName.containsKey(typeName))
				{
					deployedField.getField().setType(newTypesByEnvName.get(typeName).getType());
					isNewField = true;
				}
				else
				{	
					Type type = dataService.getTypeByName(typeName, false, env);
					
					if (type == null)
					{
						throw new DeploymentException("Type " + typeName + " not found");
					}
					
					deployedField.getField().setType(type);
					
					// the type already existed before this deployment, so this field may also already exist on the env
					// we need to check if it exists
					FieldFilter filter = new FieldFilter();
					filter.setApiName(deployedField.getField().getApiName());
					filter.setTypeKID(type.getKID());
					List<Field> existingFields = dataService.getFields(filter, env);
					if (!existingFields.isEmpty())
					{
						Field existingField = existingFields.get(0);
						deployedField.getField().setKID(existingField.getKID());
						deployedField.getField().setDbColumn(existingField.getDbColumn());
						deployedField.getField().setCreated(existingField.getCreated());
						deployedField.getField().setId(existingField.getId());
					}
					else
					{
						isNewField = true;
					}
				}
				
				try
				{
					if (isNewField)
					{
						deployedField.setField(dataService.createField(deployedField.getField(), authData, env));
					}
					else
					{
						deployedField.setField(dataService.updateField(deployedField.getField(), authData, env));
					}
					
					statuses.add(new FileDeploymentStatus(true, deployedField.getUserSpecificTypeName() + "." + deployedField.getField().getApiName(), deployedField.getDefinition(), ComponentType.FIELD, null, deployedField.getField().getKID()));
				}
				catch (KommetException e)
				{
					statuses.add(new FileDeploymentStatus(false, deployedField.getUserSpecificTypeName() + "." + deployedField.getField().getApiName(), deployedField.getDefinition(), ComponentType.FIELD, e.getMessage(), null));
				}
				
				upsertedFields.put(deployedField.getUserSpecificTypeName() + "." + deployedField.getField().getApiName(), deployedField);
			}
		}
		
		// update field references on types (sharingControlledByFieldId, defaultFieldId)
		for (DeployedType type : upsertedTypes)
		{
			String typeName = type.getType().getQualifiedName();
			
			// refresh type from the environment to make sure it contains references to any fields that
			// may have been added in this deployment
			type.setType(dataService.getType(type.getType().getKID(), env));
			
			// get the default field for this type
			String defaultFieldCombinedName = typeName + "." + type.getDefaultFieldApiName();
			KID defaultFieldId = null;
			
			if (upsertedFields.containsKey(defaultFieldCombinedName))
			{
				defaultFieldId = upsertedFields.get(defaultFieldCombinedName).getField().getKID();
			}
			else
			{
				// this field was not added in the package, so we need to find it on the env
				FieldFilter filter = new FieldFilter();
				filter.setApiName(type.getDefaultFieldApiName());
				filter.setTypeQualifiedName(typeName);
				List<Field> fields = dataService.getFields(filter, env);
				
				if (!fields.isEmpty())
				{
					defaultFieldId = fields.get(0).getKID();
				}
				else
				{
					throw new DeploymentException("Default field " + type.getDefaultFieldApiName() + " on type " + typeName + " not found");
				}
			}
			
			type.getType().setDefaultFieldId(defaultFieldId);
			
			// get sharing controlled by field
			if (StringUtils.hasText(type.getSharingControlledByFieldApiName()))
			{
				String sharingControlledByFieldApiName = typeName + "." + type.getSharingControlledByFieldApiName();
				KID sharingControlledByFieldId = null;
				
				if (upsertedFields.containsKey(sharingControlledByFieldApiName))
				{
					sharingControlledByFieldId = upsertedFields.get(sharingControlledByFieldApiName).getField().getKID();
				}
				else
				{
					// this field was not added in the package, so we need to find it on the env
					FieldFilter filter = new FieldFilter();
					filter.setApiName(type.getSharingControlledByFieldApiName());
					filter.setTypeQualifiedName(typeName);
					List<Field> fields = dataService.getFields(filter, env);
					
					if (!fields.isEmpty())
					{
						sharingControlledByFieldId = fields.get(0).getKID();
					}
					else
					{
						throw new DeploymentException("Default field " + type.getDefaultFieldApiName() + " on type " + typeName + " not found");
					}
				}
				
				type.getType().setSharingControlledByFieldId(sharingControlledByFieldId);
			}
			
			// update type
			dataService.updateType(type.getType(), authData, env);
		}
		
		// not all nested fields on all nested types are initialized, and the safest way to initialize them
		// is to read in all types anew
		reinitAllTypes(env);
		
		// deploy layouts
		// note that layouts must be deployed before profiles, because profiles reference layouts in field defaultLayout
		// they also have to be deployed before views, because view reference layouts
		if (zippedFilesByExt.containsKey(FileExtension.LAYOUT_EXT))
		{
			List<ZippedFile> files = zippedFilesByExt.get(FileExtension.LAYOUT_EXT);
			for (ZippedFile file : files)
			{
				statuses.add(deployLayout(file.getName(), file.getContent(), deployConfig.getOverwriteHandling(), authData, env));
			}
		}
		
		// deploy views
		// make sure views are deployed before classes, because views can be referenced in classes (@View annotation)
		if (zippedFilesByExt.containsKey(FileExtension.VIEW_EXT))
		{
			List<ZippedFile> viewFiles = zippedFilesByExt.get(FileExtension.VIEW_EXT);
			for (ZippedFile viewFile : viewFiles)
			{
				statuses.add(deployView(viewFile.getName(), viewFile.getContent(), deployConfig, authData, env));
			}
		}
		
		// deploy classes
		if (zippedFilesByExt.containsKey(FileExtension.CLASS_EXT))
		{
			List<ZippedFile> classFiles = zippedFilesByExt.get(FileExtension.CLASS_EXT);
			for (ZippedFile classFile : classFiles)
			{
				statuses.add(deployClass(classFile.getName(), classFile.getContent(), deployConfig, authData, env));
			}
		}
		
		// deploy user groups
		if (zippedFilesByExt.containsKey(FileExtension.USER_GROUP_EXT))
		{
			List<ZippedFile> files = zippedFilesByExt.get(FileExtension.USER_GROUP_EXT);
			for (ZippedFile file : files)
			{
				statuses.add(deployUserGroup(file.getName(), file.getContent(), deployConfig, authData, env, sharedEnv));
			}
		}
		
		// deploy apps
		if (zippedFilesByExt.containsKey(FileExtension.APP_EXT))
		{
			List<ZippedFile> files = zippedFilesByExt.get(FileExtension.APP_EXT);
			for (ZippedFile file : files)
			{
				statuses.add(deployApp(file.getName(), file.getContent(), deployConfig, authData, env, sharedEnv));
			}
		}
		
		// deploy actions
		if (zippedFilesByExt.containsKey(FileExtension.ACTION_EXT))
		{
			List<ZippedFile> files = zippedFilesByExt.get(FileExtension.ACTION_EXT);
			for (ZippedFile file : files)
			{
				statuses.add(deployAction(file.getName(), file.getContent(), deployConfig, authData, env));
			}
		}
		
		// deploy scheduled tasks
		if (zippedFilesByExt.containsKey(FileExtension.SCHEDULED_TASK_EXT))
		{
			List<ZippedFile> files = zippedFilesByExt.get(FileExtension.SCHEDULED_TASK_EXT);
			for (ZippedFile file : files)
			{
				statuses.add(deployScheduledTask(file.getName(), file.getContent(), cleanUpItems, deployConfig, authData, env, sharedEnv));
			}
		}
		
		// deploy profiles
		if (zippedFilesByExt.containsKey(FileExtension.PROFILE_EXT))
		{
			List<ZippedFile> files = zippedFilesByExt.get(FileExtension.PROFILE_EXT);
			for (ZippedFile file : files)
			{
				statuses.add(deployProfile(file.getName(), file.getContent(), deployConfig, authData, env));
			}
		}
		
		// deploy web resources
		if (zippedFilesByExt.containsKey(FileExtension.WEB_RESOURCE_EXT))
		{
			List<ZippedFile> files = zippedFilesByExt.get(FileExtension.WEB_RESOURCE_EXT);
			for (ZippedFile file : files)
			{
				statuses.add(deployWebResources(file.getName(), file.getContent(), webResourceFiles, deployConfig, authData, env));
			}
		}
		
		// deploy view resources
		if (zippedFilesByExt.containsKey(FileExtension.VIEW_RESOURCE_EXT))
		{
			List<ZippedFile> files = zippedFilesByExt.get(FileExtension.VIEW_RESOURCE_EXT);
			for (ZippedFile file : files)
			{
				statuses.add(deployViewResource(file.getName(), file.getContent(), deployConfig, authData, env));
			}
		}
		
		// deploy unique check
		if (zippedFilesByExt.containsKey(FileExtension.UNIQUE_CHECK_EXT))
		{
			List<ZippedFile> files = zippedFilesByExt.get(FileExtension.UNIQUE_CHECK_EXT);
			for (ZippedFile file : files)
			{
				statuses.add(deployUniqueCheck(file.getName(), file.getContent(), deployConfig, authData, env));
			}
		}
		
		// deploy validation rule
		if (zippedFilesByExt.containsKey(FileExtension.VALIDATION_RULE_EXT))
		{
			List<ZippedFile> files = zippedFilesByExt.get(FileExtension.VALIDATION_RULE_EXT);
			for (ZippedFile file : files)
			{
				statuses.add(deployValidationRule(file.getName(), file.getContent(), deployConfig, authData, env));
			}
		}
		
		// deploy records
		if (zippedFilesByExt.containsKey(FileExtension.RECORD_COLLECTION_EXT))
		{
			List<ZippedFile> files = zippedFilesByExt.get(FileExtension.RECORD_COLLECTION_EXT);
			for (ZippedFile file : files)
			{
				statuses.add(deployRecords(file.getName(), file.getContent(), deployConfig, authData, env));
			}
		}
		
		// clean up if deployment failed
		boolean isDeploymentSuccessful = true;
		for (FileDeploymentStatus status : statuses)
		{
			isDeploymentSuccessful &= status.isSuccess();
		}
		
		// if deployment was not successful, the records associated with library items were not actually created
		if (!isDeploymentSuccessful)
		{
			for (FileDeploymentStatus status : statuses)
			{
				status.setDeployedComponentId(null);
			}
		}
		
		// add items to deployed lib
		if (deployedLib != null)
		{
			for (FileDeploymentStatus fileStatus : statuses)
			{
				LibraryItem item = new LibraryItem();
				item.setApiName(fileStatus.getFileName());
				item.setRecordId(fileStatus.getDeployedComponentId());
				item.setComponentType(fileStatus.getFileType().getId());
				item.setDefinition(fileStatus.getDefinition());
				deployedLib.addItem(item);
			}
		}
		
		if (!isDeploymentSuccessful)
		{
			if (deployedLib != null)
			{
				deployedLib.setStatus("Installation failed");
				deployedLib.setIsEnabled(false);
			}
			
			// if deployment failed, invalidate the environment, because it has been dirtied by the unfinished deployment
			envService.resetEnv(env.getId());
			
			cleanUpFailedDeployment(cleanUpItems, authData, env);
			throw new FailedPackageDeploymentException(new PackageDeploymentStatus(isDeploymentSuccessful, statuses, deployedLib));
		}
		else
		{	
			if (deployedLib != null)
			{
				deployedLib.setStatus("Installed");
				deployedLib.setIsEnabled(true);
			}
			return new PackageDeploymentStatus(isDeploymentSuccessful, statuses, deployedLib);
		}
	}
	
	/**
	 * Read in all type definitions from database.
	 * @param env
	 * @throws KommetException
	 */
	private void reinitAllTypes(EnvData env) throws KommetException
	{
		// query all types on env
		TypeFilter typeFilter = new TypeFilter();
		typeFilter.setOrder(QueryResultOrder.ASC);
		typeFilter.setOrderBy("id");
		
		// only after all types are read in (and the UniqueCheck object among them), can unique checks be read in for these types
		for (Type type : dataService.getTypes(typeFilter, true, true, env))
		{
			// insert the type mapping into the env's data
			env.addTypeMapping(type);
			
			// register the object with the global store
			env.registerType(type);
		}
	}

	private static Library parseLibraryMetaFile(String xml) throws KommetException
	{
		Document doc = null;

		try
		{
			doc = loadXMLFromString(xml);
		}
		catch (Exception e)
		{
			throw new DeploymentException("Error loading XML from string. Nested: " + e.getMessage());
		}

		doc.getDocumentElement().normalize();
		
		Library lib = new Library();
		
		NodeList nList = doc.getElementsByTagName("library");
		
		if (nList.getLength() != 1)
		{
			throw new DeploymentException("Expected exactly one library element in XML definition, found " + nList.getLength() + " such elements");
		}
		
		Element fieldElem = (Element)nList.item(0);

		lib.setName(XMLUtil.getSingleNodeValue("name", fieldElem));
		lib.setProvider(XMLUtil.getSingleNodeValue("provider", fieldElem));
		lib.setDescription(XMLUtil.getSingleNodeValue("description", fieldElem));
		lib.setAccessLevel(XMLUtil.getSingleNodeValue("accessLevel", fieldElem));
		lib.setVersion(XMLUtil.getSingleNodeValue("version", fieldElem));
		
		return lib;
	}

	private void cleanUpFailedDeployment(DeploymentCleanupItems cleanUpItems, AuthData authData, EnvData env) throws KommetException
	{
		if (cleanUpItems.getDiskFiles() != null)
		{
			for (String absolutePath : cleanUpItems.getDiskFiles())
			{
				(new java.io.File(absolutePath)).delete();
			}
		}
		
		if (cleanUpItems.getScheduledTasks() != null)
		{
			for (ScheduledTask task : cleanUpItems.getScheduledTasks())
			{
				//log.debug("Cleaning up scheduled job " + task.getName() + " [env " + env.getId() + "] after failed deployment");
				// unschedule task, but do not delete it, because it should already be rolled back
				schedulerService.removeScheduler(task, true, authData, env);
			}
		}
	}
	
	private FileDeploymentStatus deployLayout (String name, String content, OverwriteHandling overwriteHandling, AuthData authData, EnvData env) throws KommetException
	{
		Layout layout = layoutService.getByName(name, env);
		
		if (layout != null)
		{
			if (OverwriteHandling.ALWAYS_REJECT.equals(overwriteHandling))
			{
				return new FileDeploymentStatus(false, name, content, ComponentType.LAYOUT, FileDeploymentStatus.DEPLOYMENT_ERROR_TYPE_DUPLICATE, "Layout with name " + name + " already exists", null);
			}
		}
		else
		{
			layout = new Layout();
		}

		try
		{
			layout.setName(name);
			layout.setCode(content);
	
			// save app
			try
			{
				layout = layoutService.save(layout, authData, env);
			}
			catch (Exception e)
			{
				return new FileDeploymentStatus(false, name, content, ComponentType.LAYOUT, e.getMessage(), null);
			}
			
			return new FileDeploymentStatus(true, name, content, ComponentType.LAYOUT, null, layout.getId());
		}
		catch (Exception e)
		{
			return new FileDeploymentStatus(false, name, content, ComponentType.LAYOUT, e.getMessage(), null);
		}
	}
	
	private FileDeploymentStatus deployUserGroup (String name, String xml, DeploymentConfig config, AuthData authData, EnvData env, EnvData sharedEnv) throws KommetException
	{
		Document doc = getDocFromXmlString(xml);
		NodeList nList = doc.getElementsByTagName("userGroup");
		
		if (nList.getLength() != 1)
		{
			throw new DeploymentException("Expected exactly one userGroup element in XML definition, found " + nList.getLength() + " such elements");
		}
		
		Element rootElem = (Element)nList.item(0);
		
		UserGroup group = ugService.getByName(name, authData, env);
		
		if (group != null)
		{
			if (OverwriteHandling.ALWAYS_REJECT.equals(config.getOverwriteHandling()))
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.USER_GROUP, FileDeploymentStatus.DEPLOYMENT_ERROR_TYPE_DUPLICATE, "User group with name " + name + " already exists", null);
			}
		}
		else
		{
			group = new UserGroup();
		}

		try
		{
			group.setName(XMLUtil.getSingleNodeValue("name", rootElem));
			group.setDescription(XMLUtil.getSingleNodeValue("description", rootElem));
			
			try
			{
				group = ugService.save(group, authData, env);
			}
			catch (Exception e)
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.USER_GROUP, e.getMessage(), null);
			}
			
			return new FileDeploymentStatus(true, name, xml, ComponentType.USER_GROUP, null, group.getId());
		}
		catch (Exception e)
		{
			return new FileDeploymentStatus(false, name, xml, ComponentType.USER_GROUP, e.getMessage(), null);
		}
	}
	
	private FileDeploymentStatus deployViewResource (String name, String xml, DeploymentConfig config, AuthData authData, EnvData env) throws KommetException
	{
		Document doc = getDocFromXmlString(xml);
		NodeList nList = doc.getElementsByTagName("viewResource");
		
		if (nList.getLength() != 1)
		{
			throw new DeploymentException("Expected exactly one viewResource element in XML definition, found " + nList.getLength() + " such elements");
		}
		
		Element rootElem = (Element)nList.item(0);
		
		ViewResource res = viewResourceService.getByName(name, authData, env);
		
		if (res != null)
		{
			if (OverwriteHandling.ALWAYS_REJECT.equals(config.getOverwriteHandling()))
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.VIEW_RESOURCE, FileDeploymentStatus.DEPLOYMENT_ERROR_TYPE_DUPLICATE, "View resource with name " + name + " already exists", null);
			}
		}
		else
		{
			res = new ViewResource();
		}

		try
		{
			res.setName(XMLUtil.getSingleNodeValue("name", rootElem));
			res.setMimeType(XMLUtil.getSingleNodeValue("mimeType", rootElem));
			String content = XMLUtil.getSingleNodeValue("content", rootElem);
			res.setContent(content != null ? content.trim() : null);
			
			try
			{
				res = viewResourceService.save(res, authData, env);
			}
			catch (Exception e)
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.VIEW_RESOURCE, e.getMessage(), null);
			}
			
			return new FileDeploymentStatus(true, name, xml, ComponentType.VIEW_RESOURCE, null, res.getId());
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return new FileDeploymentStatus(false, name, xml, ComponentType.VIEW_RESOURCE, e.getMessage(), null);
		}
	}
	
	private FileDeploymentStatus deployUniqueCheck (String name, String xml, DeploymentConfig config, AuthData authData, EnvData env) throws KommetException
	{
		Document doc = getDocFromXmlString(xml);
		NodeList nList = doc.getElementsByTagName("uniqueCheck");
		
		if (nList.getLength() != 1)
		{
			throw new DeploymentException("Expected exactly one uniqueCheck element in XML definition, found " + nList.getLength() + " such elements");
		}
		
		Element rootElem = (Element)nList.item(0);
		
		UniqueCheck check = uniqueCheckService.getByName(name, dataService, authData, env);
		
		if (check != null)
		{
			if (OverwriteHandling.ALWAYS_REJECT.equals(config.getOverwriteHandling()))
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.UNIQUE_CHECK, FileDeploymentStatus.DEPLOYMENT_ERROR_TYPE_DUPLICATE, "Unique check with name " + name + " already exists", null);
			}
			
			// reset fields on the unique check, so that they can be added anew in this deployment
			check.clearFields();
		}
		else
		{
			check = new UniqueCheck();
		}

		try
		{
			check.setName(XMLUtil.getSingleNodeValue("name", rootElem));
			
			String typeName = XMLUtil.getSingleNodeValue("type", rootElem);
			
			Type type = dataService.getTypeByName(typeName, true, env);
			
			if (type == null)
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.UNIQUE_CHECK, "Type " + typeName + " referenced by unique check " + name + " not found", null);
			}
			
			check.setTypeId(type.getKID());
			
			// read fields included in the unique check
			NodeList urlList = doc.getElementsByTagName("fields");
			Element fieldsElem = null;
			
			if (urlList.getLength() == 0)
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.UNIQUE_CHECK, "No fields tag found in definition of unique check " + name, null);
			}
			else if (urlList.getLength() > 1)
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.UNIQUE_CHECK, "More than one field tag found in definition of unique check " + name, null);
			}
			
			Node node = urlList.item(0);
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				fieldsElem = (Element)node;
			}
			else
			{
				throw new DeploymentException("Node with name 'fields' is not an element");
			}
			
			NodeList fieldNodes = fieldsElem.getChildNodes();
			for (int i = 0; i < fieldNodes.getLength(); i++)
			{
				Node fieldNode = fieldNodes.item(i);
				if (fieldNode.getNodeType() == Node.ELEMENT_NODE)
				{
					Element fieldElem = (Element)fieldNode;
					
					// only "fieldName" tags are allowed within the "fieldNames" element
					if (!fieldElem.getTagName().equals("fieldName"))
					{
						return new FileDeploymentStatus(false, name, xml, ComponentType.UNIQUE_CHECK, "Invalid child element '" + fieldElem.getTagName() + "' among field nodes", null);
					}
					
					String fieldName = fieldElem.getTextContent().trim();
					Field field = type.getField(fieldName);
					if (field == null)
					{
						return new FileDeploymentStatus(false, name, xml, ComponentType.UNIQUE_CHECK, "Field " + typeName + "." + fieldName + " referenced by unique check " + name + " not found", null);
					}
					
					check.addField(field);
				}
			}
			
			try
			{
				check = uniqueCheckService.save(check, authData, env);
			}
			catch (Exception e)
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.UNIQUE_CHECK, e.getMessage(), null);
			}
			
			return new FileDeploymentStatus(true, name, xml, ComponentType.UNIQUE_CHECK, null, check.getId());
		}
		catch (Exception e)
		{
			return new FileDeploymentStatus(false, name, xml, ComponentType.UNIQUE_CHECK, e.getMessage(), null);
		}
	}
	
	private FileDeploymentStatus deployValidationRule (String name, String xml, DeploymentConfig config, AuthData authData, EnvData env) throws KommetException
	{
		Document doc = getDocFromXmlString(xml);
		NodeList nList = doc.getElementsByTagName("validationRule");
		
		if (nList.getLength() != 1)
		{
			throw new DeploymentException("Expected exactly one validationRule element in XML definition, found " + nList.getLength() + " such elements");
		}
		
		Element rootElem = (Element)nList.item(0);
		
		ValidationRule vr = vrService.getByName(name, authData, env);
		
		if (vr != null)
		{
			if (OverwriteHandling.ALWAYS_REJECT.equals(config.getOverwriteHandling()))
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.VALIDATION_RULE, FileDeploymentStatus.DEPLOYMENT_ERROR_TYPE_DUPLICATE, "Validation rule with name " + name + " already exists", null);
			}
			else if (Boolean.TRUE.equals(vr.getIsSystem()))
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.VALIDATION_RULE, FileDeploymentStatus.DEPLOYMENT_ERROR_TYPE_DUPLICATE, "Validation rule with name " + name + " already exists and is a system rule, so it cannot be overwritten", null);
			}
		}
		else
		{
			vr = new ValidationRule();
			vr.setIsSystem(false);
		}

		try
		{
			vr.setName(XMLUtil.getSingleNodeValue("name", rootElem));
			vr.setCode(XMLUtil.getSingleNodeValue("code", rootElem));
			vr.setErrorMessage(XMLUtil.getSingleNodeValue("errorMessage", rootElem));
			vr.setErrorMessageLabel(XMLUtil.getSingleNodeValue("errorLabel", rootElem));
			vr.setActive("true".equals(XMLUtil.getSingleNodeValue("isActive", rootElem)));
			
			String typeName = XMLUtil.getSingleNodeValue("type", rootElem);
			Type type = dataService.getTypeByName(typeName, false, env);
			
			if (type == null)
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.VALIDATION_RULE, "Type " + typeName + " referenced by validation rule " + name + " not found", null);
			}
			
			vr.setTypeId(type.getKID());
			
			try
			{
				vr = vrService.save(vr, authData, env);
			}
			catch (Exception e)
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.VALIDATION_RULE, e.getMessage(), null);
			}
			
			return new FileDeploymentStatus(true, name, xml, ComponentType.VALIDATION_RULE, null, vr.getId());
		}
		catch (Exception e)
		{
			return new FileDeploymentStatus(false, name, xml, ComponentType.VALIDATION_RULE, e.getMessage(), null);
		}
	}
	
	private FileDeploymentStatus deployProfile (String name, String xml, DeploymentConfig config, AuthData authData, EnvData env) throws KommetException
	{
		Document doc = getDocFromXmlString(xml);
		NodeList nList = doc.getElementsByTagName("profile");
		
		if (nList.getLength() != 1)
		{
			throw new DeploymentException("Expected exactly one profile element in XML definition, found " + nList.getLength() + " such elements");
		}
		
		Element rootElem = (Element)nList.item(0);
		
		Profile profile = profileService.getProfileByName(name, env);
		
		if (profile != null)
		{
			if (OverwriteHandling.ALWAYS_REJECT.equals(config.getOverwriteHandling()))
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.PROFILE, FileDeploymentStatus.DEPLOYMENT_ERROR_TYPE_DUPLICATE, "Profile with name " + name + " already exists", null);
			}
			
			if (Boolean.TRUE.equals(profile.getSystemProfile()))
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.PROFILE, FileDeploymentStatus.DEPLOYMENT_ERROR_TYPE_DUPLICATE, "Profile with name " + name + " already exists and is a system profile, so it cannot be overwritten", null);
			}
		}
		else
		{
			profile = new Profile();
			profile.setSystemProfile(false);
		}

		try
		{
			profile.setName(XMLUtil.getSingleNodeValue("name", rootElem));
			profile.setLabel(XMLUtil.getSingleNodeValue("label", rootElem));
			
			String defaultLayoutName = XMLUtil.getSingleNodeValue("defaultLayout", rootElem);
			
			if (StringUtils.hasText(defaultLayoutName))
			{
				// find layout with the given name
				Layout layout = layoutService.getByName(defaultLayoutName, env);
				if (layout == null)
				{
					return new FileDeploymentStatus(false, name, xml, ComponentType.PROFILE, "Layout " + defaultLayoutName + "referenced by profile " + name + " not found", null);
				}
			}
			
			try
			{
				profile = profileService.save(profile, authData, env);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return new FileDeploymentStatus(false, name, xml, ComponentType.PROFILE, e.getMessage(), null);
			}
			
			return new FileDeploymentStatus(true, name, xml, ComponentType.PROFILE, null, profile.getId());
		}
		catch (Exception e)
		{
			return new FileDeploymentStatus(false, name, xml, ComponentType.PROFILE, e.getMessage(), null);
		}
	}
	
	private FileDeploymentStatus deployScheduledTask (String name, String xml, DeploymentCleanupItems cleanUpItems, DeploymentConfig config, AuthData authData, EnvData env, EnvData sharedEnv) throws KommetException
	{
		Document doc = getDocFromXmlString(xml);
		NodeList nList = doc.getElementsByTagName("scheduledTask");
		
		if (nList.getLength() != 1)
		{
			throw new DeploymentException("Expected exactly one scheduledTask element in XML definition, found " + nList.getLength() + " such elements");
		}
		
		Element rootElem = (Element)nList.item(0);
		
		ScheduledTask task = schedulerService.getByName(name, authData, env);
		
		if (task != null)
		{
			if (OverwriteHandling.ALWAYS_REJECT.equals(config.getOverwriteHandling()))
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.SCHEDULED_TASK, FileDeploymentStatus.DEPLOYMENT_ERROR_TYPE_DUPLICATE, "Scheduled task with name " + name + " already exists", null);
			}
		}
		else
		{
			// create new task
			task = new ScheduledTask();
		}

		try
		{
			task.setName(XMLUtil.getSingleNodeValue("name", rootElem));
			task.setCronExpression(XMLUtil.getSingleNodeValue("cron", rootElem));
			task.setMethod(XMLUtil.getSingleNodeValue("classMethod", rootElem));
			
			String className = XMLUtil.getSingleNodeValue("class", rootElem);
			
			// convert user to env package
			Class classFile = classService.getClass(className, env);
			
			if (classFile == null)
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.SCHEDULED_TASK, "Class " + className + " referenced by scheduled task " + task.getName() + " not found", null);
			}
			
			task.setFile(classFile);
			
			try
			{
				task = schedulerService.schedule(task, authData, env);
				cleanUpItems.addScheduledTask(task);
			}
			catch (ScheduledTaskException e)
			{
				// remember to delete the job, unless it has not been created, as indicated by exception error code
				if (e.getErrorType() == null || !e.getErrorType().equals(ExceptionErrorType.SCHEDULING_TASK_JOB_FAILED))
				{
					cleanUpItems.addScheduledTask(task);
				}
				return new FileDeploymentStatus(false, name, xml, ComponentType.SCHEDULED_TASK, e.getMessage(), null);
			}
			catch (Exception e)
			{
				cleanUpItems.addScheduledTask(task);
				return new FileDeploymentStatus(false, name, xml, ComponentType.SCHEDULED_TASK, e.getMessage(), null);
			}
			
			return new FileDeploymentStatus(true, name, xml, ComponentType.SCHEDULED_TASK, null, task.getId());
		}
		catch (Exception e)
		{
			return new FileDeploymentStatus(false, name, xml, ComponentType.SCHEDULED_TASK, e.getMessage() != null ? e.getMessage() : "�ndefined error", null);
		}
	}
	
	private FileDeploymentStatus deployApp (String name, String xml, DeploymentConfig config, AuthData authData, EnvData env, EnvData sharedEnv) throws KommetException
	{
		Document doc = getDocFromXmlString(xml);
		NodeList nList = doc.getElementsByTagName("app");
		
		if (nList.getLength() != 1)
		{
			throw new DeploymentException("Expected exactly one app element in XML definition, found " + nList.getLength() + " such elements");
		}
		
		Element rootElem = (Element)nList.item(0);
		
		App app = appService.getAppByName(name, authData, env);
		
		if (app != null)
		{
			if (OverwriteHandling.ALWAYS_REJECT.equals(config.getOverwriteHandling()))
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.APP, FileDeploymentStatus.DEPLOYMENT_ERROR_TYPE_DUPLICATE, "App with name " + name + " already exists", null);
			}
		}
		else
		{
			app = new App();
		}

		try
		{
			app.setName(XMLUtil.getSingleNodeValue("name", rootElem));
			app.setLandingUrl(XMLUtil.getSingleNodeValue("landingURL", rootElem));
			app.setLabel(XMLUtil.getSingleNodeValue("label", rootElem));
			app.setType(XMLUtil.getSingleNodeValue("type", rootElem));
			
			// read app URLs
			NodeList urlList = doc.getElementsByTagName("urls");
			Element urlsElem = null;
			
			if (urlList.getLength() == 0)
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.APP, "No URLs tag found in definition of app " + name, null);
			}
			else if (urlList.getLength() > 1)
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.APP, "More than one URL tag found in definition of app " + name, null);
			}
			
			Node node = urlList.item(0);
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				urlsElem = (Element)node;
			}
			else
			{
				throw new DeploymentException("Node with name 'urls' is not an element");
			}
			
			List<AppUrl> appUrls = new ArrayList<AppUrl>();
			
			NodeList urlNodes = urlsElem.getChildNodes();
			for (int i = 0; i < urlNodes.getLength(); i++)
			{
				Node urlNode = urlNodes.item(i);
				if (urlNode.getNodeType() == Node.ELEMENT_NODE)
				{
					Element url = (Element)urlNode;
					
					// only "url" tags are allowed within the "urls" element
					if (!url.getTagName().equals("url"))
					{
						return new FileDeploymentStatus(false, name, xml, ComponentType.APP, "Invalid child element '" + url.getTagName() + "' among URL nodes", null);
					}
					
					AppUrl appUrl = new AppUrl();
					appUrl.setUrl(url.getTextContent());
					appUrls.add(appUrl);
				}
			}
			
			// save app
			try
			{
				app = appService.save(app, authData, env);
				
				// save app urls
				for (AppUrl url : appUrls)
				{
					url.setApp(app);
					appService.save(url, authData, env, sharedEnv);
				}
			}
			catch (Exception e)
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.APP, e.getMessage(), null);
			}
			
			
			return new FileDeploymentStatus(true, name, xml, ComponentType.APP, null, app.getId());
		}
		catch (Exception e)
		{
			return new FileDeploymentStatus(false, name, xml, ComponentType.APP, e.getMessage(), null);
		}
	}
	
	private FileDeploymentStatus deployWebResources(String name, String xml, Map<String, File> webResourceFiles, DeploymentConfig config, AuthData authData, EnvData env) throws KommetException
	{
		Document doc = getDocFromXmlString(xml);
		NodeList nList = doc.getElementsByTagName("webResource");
		
		if (nList.getLength() != 1)
		{
			throw new DeploymentException("Expected exactly one webResource element in XML definition, found " + nList.getLength() + " such elements");
		}
		
		Element rootElem = (Element)nList.item(0);
		
		WebResource resource = webResourceService.getByName(name, authData, env);
		
		if (resource != null)
		{
			if (OverwriteHandling.ALWAYS_REJECT.equals(config.getOverwriteHandling()))
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.WEB_RESOURCE, FileDeploymentStatus.DEPLOYMENT_ERROR_TYPE_DUPLICATE, "Web resource with name " + name + " already exists", null);
			}
		}
		else
		{
			resource = new WebResource();
		}

		try
		{
			resource.setName(XMLUtil.getSingleNodeValue("name", rootElem));
			resource.setMimeType(XMLUtil.getSingleNodeValue("mimeType", rootElem));
			
			// find actual resource file
			File file = webResourceFiles.get(resource.getName());
			
			if (file == null)
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.WEB_RESOURCE, "Resource file associated with web resource " + resource.getName() + " not found in package", null);
			}
			
			resource.setFile(file);
			resource = webResourceService.save(resource, false, authData, env);
			return new FileDeploymentStatus(true, name, xml, ComponentType.WEB_RESOURCE, null, resource.getId());
		}
		catch (Exception e)
		{
			return new FileDeploymentStatus(false, name, xml, ComponentType.WEB_RESOURCE, e.getMessage(), null);
		}
	}

	private FileDeploymentStatus deployAction(String name, String xml, DeploymentConfig config, AuthData authData, EnvData env) throws KommetException
	{
		Document doc = getDocFromXmlString(xml);
		NodeList nList = doc.getElementsByTagName("action");
		
		if (nList.getLength() != 1)
		{
			throw new DeploymentException("Expected exactly one action element in XML definition, found " + nList.getLength() + " such elements");
		}
		
		Element rootElem = (Element)nList.item(0);
		
		Action action = actionService.getActionByName(name, authData, env);
		
		if (action != null)
		{
			if (OverwriteHandling.ALWAYS_REJECT.equals(config.getOverwriteHandling()))
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.ACTION, FileDeploymentStatus.DEPLOYMENT_ERROR_TYPE_DUPLICATE, "Action with name " + name + " already exists", null);
			}
		}
		else
		{
			action = new Action();
		}

		try
		{
			action.setName(XMLUtil.getSingleNodeValue("name", rootElem));
			action.setControllerMethod(XMLUtil.getSingleNodeValue("controllerMethod", rootElem));
			action.setUrl(XMLUtil.getSingleNodeValue("url", rootElem));
			action.setIsPublic("true".equals(XMLUtil.getSingleNodeValue("isPublic", rootElem)));
			
			// all deployable actions are non-system
			action.setIsSystem(false);
			
			String className = XMLUtil.getSingleNodeValue("controller", rootElem);
			Class controllerClass = classService.getClass(className, env);
			if (controllerClass == null)
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.ACTION, "Controller class " + className + " referenced by action " + action.getName() + " not found", null);
			}
			action.setController(controllerClass);
			
			String viewName = XMLUtil.getSingleNodeValue("view", rootElem);
			View view = viewService.getView(viewName, env);
			if (view == null)
			{
				return new FileDeploymentStatus(false, name, xml, ComponentType.ACTION, "View " + viewName + " referenced by action " + action.getName() + " not found", null);
			}
			action.setView(view);
		}
		catch (Exception e)
		{
			return new FileDeploymentStatus(false, name, xml, ComponentType.ACTION, e.getMessage(), null);
		}
		
		try
		{
			action = actionService.save(action, authData, env);
			return new FileDeploymentStatus(true, name, xml, ComponentType.ACTION, null, action.getId());
		}
		catch (KommetException e)
		{
			return new FileDeploymentStatus(false, name, xml, ComponentType.ACTION, e.getMessage(), null);
		}
	}
	
	/**
	 * Deploys a file containing records. The structure of the file is as follows:
	 * <records>
	 * 		<record>
	 * 			<type>com.office.Project</type>
	 * 			<fields>
	 * 				<name>Test Project</name>
	 * 				<budget>2001.22</budget>
	 * 			</fields>
	 * 		</record>
	 * 		<record>
	 * 			<type>com.office.Project2</type>
	 * 			<fields>
	 * 				<name>Test Project 2</name>
	 * 				<budget>500</budget>
	 * 			</fields>
	 * 		</record>
	 * </records>
	 * @param name
	 * @param xml
	 * @param overwriteHandling
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	private FileDeploymentStatus deployRecords(String name, String xml, DeploymentConfig config, AuthData authData, EnvData env) throws KommetException
	{
		Document doc = getDocFromXmlString(xml);
		NodeList nList = doc.getElementsByTagName("records");
		int failedRecords = 0;
		
		if (nList.getLength() != 1)
		{
			throw new DeploymentException("Expected exactly one records element in XML definition, found " + nList.getLength() + " such elements");
		}
		
		Element rootElem = (Element)nList.item(0);
		
		List<RecordDeploymentStatus> recordStatuses = new ArrayList<RecordDeploymentStatus>();
		
		// find all record elements
		NodeList recordElems = rootElem.getElementsByTagName("record");
		
		for (int i = 0; i < recordElems.getLength(); i++)
		{
			Node recordNode = recordElems.item(i);
			
			if (!(recordNode instanceof Element))
			{
				recordStatuses.add(new RecordDeploymentStatus(false, null, "Record node is not an element", null));
				continue;
			}
			
			Element recordElem = (Element)recordNode;
			
			RecordDeploymentStatus recordStatus = deployRecord(recordElem, name, xml, authData, env);
			
			if (!recordStatus.isSuccess())
			{
				failedRecords++;
			}
			recordStatuses.add(recordStatus);
		}
		
		FileDeploymentStatus fileStatus = null;
		
		if (failedRecords == 0)
		{
			fileStatus = new FileDeploymentStatus(true, name, xml, ComponentType.RECORD_COLLECTION, null, null);
		}
		else
		{
			fileStatus = new FileDeploymentStatus(false, name, xml, ComponentType.RECORD_COLLECTION, "Deployment of " + failedRecords + " failed", null);
		}
		
		fileStatus.setRecordStatuses(recordStatuses);
		
		return fileStatus;
	}

	private RecordDeploymentStatus deployRecord(Element recordElem, String name, String xml, AuthData authData, EnvData env) throws KommetException
	{
		// first find type node to find out the type of this record
		String typeName = XMLUtil.getSingleNodeValue("type", recordElem);
		if (!StringUtils.hasText(typeName))
		{
			return new RecordDeploymentStatus(false, XMLUtil.xmlNodeToString(recordElem), "Type not defined on record element", null);
		}
		
		Type type = env.getType(typeName);
		if (type == null)
		{
			return new RecordDeploymentStatus(false, XMLUtil.xmlNodeToString(recordElem), "Type " + typeName + " not found on env", null);
		}
		
		// create new record that will be inserted
		Record record = new Record(type);
		
		NodeList fieldElems = recordElem.getElementsByTagName("fields");
		
		if (fieldElems.getLength() != 1)
		{
			return new RecordDeploymentStatus(false, XMLUtil.xmlNodeToString(recordElem), "Expected exactly one fields element for record element, but instead found " + fieldElems.getLength() + " such elements", null);
		}
		
		// iterate through field values
		NodeList fieldValues = ((Element)fieldElems.item(0)).getChildNodes();
		
		for (int k = 0; k < fieldValues.getLength(); k++)
		{
			Node fieldValueNode = fieldValues.item(k);
			
			if (!(fieldValueNode instanceof Element))
			{
				return new RecordDeploymentStatus(false, XMLUtil.xmlNodeToString(recordElem), "Field value node is not an element", null);
			}
			
			Element fieldValue = (Element)fieldValueNode;
			String fieldApiName = fieldValue.getNodeName();
			
			// check if such field exists
			Field field = type.getField(fieldApiName);
			if (field == null)
			{
				return new RecordDeploymentStatus(false, XMLUtil.xmlNodeToString(recordElem), "Field " + typeName + "." + fieldApiName + " does not exist", null);
			}
			
			if (Field.isSystemField(fieldApiName))
			{
				return new RecordDeploymentStatus(false, XMLUtil.xmlNodeToString(recordElem), "Cannot deploy value for system field " + typeName + "." + fieldApiName, null);
			}
			
			String sFieldValue = fieldValue.getTextContent();
			
			// assign field value to record
			record.setField(fieldApiName, field.getDataType().getJavaValue(sFieldValue));
		}
		
		try
		{
			record = dataService.save(record, authData, env);
			return new RecordDeploymentStatus(true, XMLUtil.xmlNodeToString(recordElem), null, record.getKID());
		}
		catch (KommetException e)
		{
			return new RecordDeploymentStatus(false, XMLUtil.xmlNodeToString(recordElem), e.getMessage(), null);
		}
	}

	/**
	 * Some field data types (inverse collection, type reference, association) reference other types. However,
	 * these types may be deployed in the same package as the field, so type IDs on the data type are null when
	 * package is created and needs to be updated after the referenced types have been inserted.
	 * @param deployedField
	 * @param newTypesByEnvName 
	 * @throws KommetException 
	 * @throws InvalidResultSetAccessException 
	 */
	private void fillMissingTypeData(DeployedField deployedField, EnvData env) throws InvalidResultSetAccessException, KommetException
	{
		DataType dt = deployedField.getField().getDataType();
		
		if (dt.getId().equals(DataType.TYPE_REFERENCE))
		{
			Type referencedType = dataService.getTypeByName(((TypeReference)dt).getType().getQualifiedName(), false, env);
			if (referencedType == null)
			{
				throw new DeploymentException("Referenced type " + ((TypeReference)dt).getType().getQualifiedName() + " for field " + deployedField.getField().getApiName() + " not found");
			}
			((TypeReference)dt).setType(referencedType);
		}
		else if (dt.getId().equals(DataType.INVERSE_COLLECTION))
		{
			Type inverseType = dataService.getTypeByName(((InverseCollectionDataType)dt).getInverseType().getQualifiedName(), false, env);
			if (inverseType == null)
			{
				throw new DeploymentException("Inverse type " + ((InverseCollectionDataType)dt).getInverseType().getQualifiedName() + " for field " + deployedField.getField().getApiName() + " not found");
			}
			((InverseCollectionDataType)dt).setInverseType(inverseType);
		}
		else if (dt.getId().equals(DataType.ASSOCIATION))
		{
			Type linkingType = dataService.getTypeByName(((AssociationDataType)dt).getLinkingType().getQualifiedName(), false, env);
			if (linkingType == null)
			{
				throw new DeploymentException("Linking type " + ((AssociationDataType)dt).getLinkingType().getQualifiedName() + " for field " + deployedField.getField().getApiName() + " not found");
			}
			((AssociationDataType)dt).setLinkingType(linkingType);
			
			Type associatedType = dataService.getTypeByName(((AssociationDataType)dt).getAssociatedType().getQualifiedName(), false, env);
			if (associatedType == null)
			{
				throw new DeploymentException("Associated type " + ((AssociationDataType)dt).getAssociatedType().getQualifiedName() + " for field " + deployedField.getField().getApiName() + " not found");
			}
			((AssociationDataType)dt).setAssociatedType(associatedType);
		}
	}
	
	/**
	 * Deploys a single file (KOLL class or KTL view) to the current
	 * environment.
	 * 
	 * @param qualifiedNameWithExt
	 *            Qualified file name (i.e. package name + file name)
	 * @param fileContent
	 *            File content as string
	 * @param overwriteHandling 
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	private FileDeploymentStatus deployView(String qualifiedName, String fileContent, DeploymentConfig config, AuthData authData, EnvData env) throws KommetException
	{
		String bareName = null;
		String userPackageName = null;

		if (qualifiedName.contains("."))
		{
			bareName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
			userPackageName = qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
		}
		else
		{
			bareName = qualifiedName;
		}

		ViewFilter filter = new ViewFilter();

		filter.setName(bareName);
		filter.setPackage(userPackageName);

		List<View> files = viewService.getViews(filter, env);

		View view = null;

		if (files.isEmpty())
		{
			view = new View();
			view.setName(bareName);
			view.setPackageName(userPackageName);
			view.setIsSystem(false);
		}
		else
		{
			if (OverwriteHandling.ALWAYS_REJECT.equals(config.getOverwriteHandling()))
			{
				return new FileDeploymentStatus(false, qualifiedName, fileContent, ComponentType.VIEW, FileDeploymentStatus.DEPLOYMENT_ERROR_TYPE_DUPLICATE, "View with name " + userPackageName + " aready exists", null);
			}
			else
			{
				view = files.get(0);
			}
		}

		try
		{
			view = viewService.fullSave(view, fileContent, false, authData, env);
			return new FileDeploymentStatus(true, qualifiedName, fileContent, ComponentType.VIEW, null, view.getId());
		}
		catch (KommetException e)
		{
			return new FileDeploymentStatus(false, qualifiedName, fileContent, ComponentType.VIEW, e.getMessage(), null);
		}
	}

	/**
	 * Deploys a single file (KOLL class or KTL view) to the current
	 * environment.
	 * 
	 * @param qualifiedNameWithExt
	 *            Qualified file name (i.e. package name + file name)
	 * @param fileContent
	 *            File content as string
	 * @param overwriteHandling 
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	private FileDeploymentStatus deployClass(String qualifiedName, String fileContent, DeploymentConfig config, AuthData authData, EnvData env) throws KommetException
	{
		String bareName = null;
		String packageName = null;

		if (qualifiedName.contains("."))
		{
			bareName = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
			packageName = qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
		}
		else
		{
			bareName = qualifiedName;
		}

		// find file by name
		ClassFilter filter = new ClassFilter();
		filter.setQualifiedName(qualifiedName);
		List<Class> files = classService.getClasses(filter, env);

		Class file = null;

		if (files.isEmpty())
		{
			file = new Class();
			file.setIsSystem(false);

			file.setName(bareName);
			file.setPackageName(packageName);
		}
		else
		{
			if (OverwriteHandling.ALWAYS_REJECT.equals(config.getOverwriteHandling()))
			{
				return new FileDeploymentStatus(false, qualifiedName, fileContent, ComponentType.CLASS, FileDeploymentStatus.DEPLOYMENT_ERROR_TYPE_DUPLICATE, "Class with name " + packageName + " aready exists", null);
			}
			else
			{
				file = files.get(0);
			}
		}

		file.setKollCode(fileContent);

		try
		{
			file = classService.fullSave(file, dataService, authData, env);
			return new FileDeploymentStatus(true, qualifiedName, fileContent, ComponentType.CLASS, null, file.getId());
		}
		catch (KommetException e)
		{
			return new FileDeploymentStatus(false, qualifiedName, fileContent, ComponentType.CLASS, e.getMessage(), null);
		}
	}
	
	class DeployedField
	{
		private Field field;
		private String userSpecificTypeName;
		private String definition;

		public Field getField()
		{
			return field;
		}

		public void setField(Field field)
		{
			this.field = field;
		}

		public String getUserSpecificTypeName()
		{
			return userSpecificTypeName;
		}

		public void setUserSpecificTypeName(String userSpecificTypeName)
		{
			this.userSpecificTypeName = userSpecificTypeName;
		}

		public String getDefinition()
		{
			return definition;
		}

		public void setDefinition(String definition)
		{
			this.definition = definition;
		}
	}
	
	class DeployedType
	{
		private Type type;
		private String defaultFieldApiName;
		private String sharingControlledByFieldApiName;

		public Type getType()
		{
			return type;
		}

		public void setType(Type type)
		{
			this.type = type;
		}

		public String getDefaultFieldApiName()
		{
			return defaultFieldApiName;
		}

		public void setDefaultFieldApiName(String defaultField)
		{
			this.defaultFieldApiName = defaultField;
		}

		public String getSharingControlledByFieldApiName()
		{
			return sharingControlledByFieldApiName;
		}

		public void setSharingControlledByFieldApiName(String sharingControlledByField)
		{
			this.sharingControlledByFieldApiName = sharingControlledByField;
		}
	}
	
	private static Document getDocFromXmlString (String xml) throws DeploymentException
	{
		Document doc = null;

		try
		{
			doc = loadXMLFromString(xml);
		}
		catch (Exception e)
		{
			throw new DeploymentException("Error loading XML from string. Nested: " + e.getMessage());
		}

		doc.getDocumentElement().normalize();
		return doc;
	}

	class ZippedFile
	{
		private String name;
		private String content;

		public ZippedFile(String fileName, String fileContent)
		{
			this.name = fileName;
			this.content = fileContent;
		}

		public String getName()
		{
			return name;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public String getContent()
		{
			return content;
		}

		public void setContent(String content)
		{
			this.content = content;
		}
	}
}