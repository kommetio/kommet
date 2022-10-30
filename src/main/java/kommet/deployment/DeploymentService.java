/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.deployment;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipInputStream;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
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
import kommet.basic.Action;
import kommet.basic.App;
import kommet.basic.AppUrl;
import kommet.basic.Profile;
import kommet.basic.RecordProxy;
import kommet.basic.RecordProxyUtil;
import kommet.basic.ScheduledTask;
import kommet.basic.UniqueCheck;
import kommet.basic.UserGroup;
import kommet.basic.ValidationRule;
import kommet.basic.ViewResource;
import kommet.basic.WebResource;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.AutoNumber;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.FormulaDataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.MultiEnumerationDataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.i18n.Locale;
import kommet.scheduler.ScheduledTaskService;
import kommet.utils.XMLUtil;

/**
 * Allows for deploying code (views, classes), type and field definitions between environments.
 * @author Radek Krawiec
 * @since 2014
 */
@Service
public class DeploymentService
{
	@Inject
	DeploymentProcess deployment;
	
	@Inject
	EnvService envService;
	
	@Inject
	ScheduledTaskService schedulerService;
	
	private PackageDeploymentStatus deploy (ZipInputStream zis, DeploymentConfig deployConfig, AuthData authData, EnvData env) throws KommetException
	{
		DeploymentCleanupItems cleanUpItems = new DeploymentCleanupItems();
		return deployment.deploy(zis, deployConfig, cleanUpItems, authData, env, envService.getMasterEnv());
	}
	
	/**
	 * Generates type XML for deployment from the given type record.
	 * @param type
	 * @return
	 * @throws KommetException 
	 */
	private static String createTypeXML(DeployableType type, EnvData env) throws KommetException
	{
		Document doc = getXmlDoc();
        Element root = doc.createElement("type");
        doc.appendChild(root);
        
        // append type-specific elements
        XMLUtil.addElement(doc, root, "apiName", type.getApiName());
        XMLUtil.addElement(doc, root, "package", type.getPackage());
        XMLUtil.addElement(doc, root, "label", type.getLabel());
        XMLUtil.addElement(doc, root, "description", type.getDescription());
        XMLUtil.addElement(doc, root, "pluralLabel", type.getPluralLabel());
        XMLUtil.addElement(doc, root, "uchLabel", type.getUchLabel());
        XMLUtil.addElement(doc, root, "uchPluralLabel", type.getUchPluralLabel());
        
        // note that key prefix and dbTable are not deployable
        
        XMLUtil.addElement(doc, root, "combineRecordAndCascadeSharing", type.isCombineRecordAndCascadeSharing());
        XMLUtil.addElement(doc, root, "defaultFieldApiName", type.getDefaultFieldApiName());
        XMLUtil.addElement(doc, root, "sharingControlledByFieldApiName", type.getSharingControlledByField() != null ? type.getSharingControlledByField().getApiName() : "");
		
        return xmlDocToString(doc);
	}
	
	private static String xmlDocToString (Document doc) throws KommetException
	{
		try
        {
			TransformerFactory tFact = TransformerFactory.newInstance();
			Transformer trans = tFact.newTransformer();
			
			// add indentation
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			DOMSource source = new DOMSource(doc);
			trans.transform(source, result);
			
			return writer.toString();
        }
        catch (TransformerException e)
        {
        	throw new KommetException("Could not transform XML into string. Nested: " + e.getMessage());
        }
	}

	private static String createFieldXML(Field field, DataService dataService, EnvData env) throws KommetException
	{
		Document doc = getXmlDoc();
        Element root = doc.createElement("field");
        doc.appendChild(root);
        
        if (field.getType() == null)
        {
        	throw new PackageGenetionException("Could not serialize field into XML. Type to which the field belongs is not set.");
        }
        
        XMLUtil.addElement(doc, root, "type", field.getType().getQualifiedName());
        
        // append type-specific elements
        XMLUtil.addElement(doc, root, "apiName", field.getApiName());
        XMLUtil.addElement(doc, root, "label", field.getLabel());
        XMLUtil.addElement(doc, root, "uchLabel", field.getUchLabel());
        XMLUtil.addElement(doc, root, "description", field.getDescription());
        XMLUtil.addElement(doc, root, "required", field.isRequired());
        XMLUtil.addElement(doc, root, "trackHistory", field.isTrackHistory());
        
        addDataTypeToFieldXML(doc, root, field, dataService, env);
        
        return xmlDocToString(doc);
	}

	private static void addDataTypeToFieldXML(Document doc, Element root, Field field, DataService dataService, EnvData env) throws KommetException
	{
		Element dtElem = doc.createElement("dataType");
		
		DataType dt = field.getDataType();
		
		XMLUtil.addElement(doc, dtElem, "name", dt.getName());
		
		switch (dt.getId())
		{
			case DataType.NUMBER: 
					XMLUtil.addElement(doc, dtElem, "decimalPlaces", ((NumberDataType)dt).getDecimalPlaces());
					XMLUtil.addElement(doc, dtElem, "javaType", ((NumberDataType)dt).getJavaType());
					break;
			case DataType.TEXT: XMLUtil.addElement(doc, dtElem, "length", ((TextDataType)dt).getLength()); break;
			case DataType.BOOLEAN: break;
			case DataType.DATETIME: break;
			case DataType.DATE: break;
			case DataType.KOMMET_ID: break;
			case DataType.EMAIL: break;
			case DataType.BLOB: break;
			case DataType.TYPE_REFERENCE: 
				
					TypeReference objRefDataType = (TypeReference)dt;
				
					XMLUtil.addElement(doc, dtElem, "cascadeDelete", objRefDataType.isCascadeDelete());
					
					Type referencedType = null;
					
					if (objRefDataType.getTypeId() != null)
					{
						referencedType = dataService.getType(objRefDataType.getTypeId(), env);
						if (referencedType == null)
						{
							throw new PackageGenetionException("Type with ID " + objRefDataType.getTypeId() + " not found"); 
						}
					}
					else if (objRefDataType.getType() != null)
					{
						referencedType = dataService.getTypeByName(objRefDataType.getType().getQualifiedName(), false, env);
						if (referencedType == null)
						{
							throw new PackageGenetionException("Type with name " + objRefDataType.getType().getQualifiedName() + " not found"); 
						}
					}
					else
					{
						throw new PackageGenetionException("Referenced type not set on type reference field " + field.getApiName());
					}
					
					XMLUtil.addElement(doc, dtElem, "type", referencedType.getQualifiedName());
					break;
			case DataType.ENUMERATION:
				
					Element valuesElem = doc.createElement("values");
					
					EnumerationDataType enumDataType = (EnumerationDataType)dt;
					XMLUtil.addElement(doc, dtElem, "validateValues", enumDataType.isValidateValues());
					List<String> values = enumDataType.getValueList();
					
					for (String val : values)
					{
						XMLUtil.addElement(doc, valuesElem, "value", val);
					}
					
					dtElem.appendChild(valuesElem);
					break;
			case DataType.MULTI_ENUMERATION:
				
				Element multiValuesElem = doc.createElement("values");
				
				MultiEnumerationDataType multiEnumDataType = (MultiEnumerationDataType)dt;
				Set<String> multiValues = multiEnumDataType.getValues();
				
				for (String val : multiValues)
				{
					XMLUtil.addElement(doc, multiValuesElem, "value", val);
				}
				
				dtElem.appendChild(multiValuesElem);
				break;
			case DataType.INVERSE_COLLECTION:
					InverseCollectionDataType icDataType = (InverseCollectionDataType)dt;
					Type inverseType = dataService.getType(icDataType.getInverseTypeId(), env);
					XMLUtil.addElement(doc, dtElem, "inverseType", inverseType.getQualifiedName());
					XMLUtil.addElement(doc, dtElem, "inverseProperty", icDataType.getInverseProperty());
					break;
			case DataType.ASSOCIATION:
					AssociationDataType associationDataType = (AssociationDataType)dt;
					Type linkingType = associationDataType.getLinkingType();
					
					if (linkingType == null)
					{
						throw new DeploymentException("Linking type " + associationDataType.getLinkingType().getQualifiedName() + " for association field " + field.getApiName() + " not set");
					}
					
					XMLUtil.addElement(doc, dtElem, "linkingType", linkingType.getQualifiedName());
					Type associatedType = associationDataType.getAssociatedType();
					
					if (associatedType == null)
					{
						throw new DeploymentException("Associated type " + associationDataType.getAssociatedType().getQualifiedName() + " for association field " + field.getApiName() + " not set");
					}
					
					XMLUtil.addElement(doc, dtElem, "associatedType", associatedType.getQualifiedName());
					XMLUtil.addElement(doc, dtElem, "selfLinkingField", associationDataType.getSelfLinkingField());
					XMLUtil.addElement(doc, dtElem, "foreignLinkingField", associationDataType.getForeignLinkingField());
					break;
			case DataType.FORMULA:
					FormulaDataType formulaDataType = (FormulaDataType)dt;
					
					if (formulaDataType.getReturnType() == null)
					{
						throw new PackageGenetionException("Formula field " + field.getApiName() + " has no return type set");
					}
					if (!StringUtils.hasText(formulaDataType.getUserDefinition()))
					{
						throw new PackageGenetionException("Formula field " + field.getApiName() + " has no user definition set ");
					}
					XMLUtil.addElement(doc, dtElem, "returnType", formulaDataType.getReturnType().name());
					XMLUtil.addElement(doc, dtElem, "definition", formulaDataType.getUserDefinition());
					break;
			case DataType.AUTO_NUMBER:
					XMLUtil.addElement(doc, dtElem, "format", ((AutoNumber)dt).getFormat());
					break;
			default: throw new KommetException("No data type exists for ID " + dt.getId());
		}
		
		root.appendChild(dtElem);
	}
	
	private static Document getXmlDoc() throws KommetException
	{
		DocumentBuilderFactory dFact = DocumentBuilderFactory.newInstance();
        DocumentBuilder build;
		
        try
		{
			build = dFact.newDocumentBuilder();
		}
		catch (ParserConfigurationException e)
		{
			throw new KommetException("Could not build document. Nested: " + e.getMessage());
		}
        
        return build.newDocument();
	}
	
	/**
	 * Returns the XML element of the records document representing a single record.
	 * @param obj
	 * @param type
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	private static <T extends RecordProxy> void serializeRecord(T obj, Document doc, Element recordsElem, Type type, AuthData authData, EnvData env) throws KommetException
	{
        Element root = doc.createElement("record");
        recordsElem.appendChild(root);
        
        Record record = RecordProxyUtil.generateRecord(obj, type, 1, env);
        
        XMLUtil.addElement(doc, root, "type", type.getQualifiedName());
        
        Element fieldsElem = doc.createElement("fields");
        Locale locale = authData.getLocale();
        
        for (String fieldName : record.getFieldValues().keySet())
		{
			XMLUtil.addElement(doc, fieldsElem, fieldName, record.getFieldStringValue(fieldName, locale));
		}
        
        root.appendChild(fieldsElem);
	}

	public static String serialize(App app, EnvData env) throws KommetException
	{
        Document doc = getXmlDoc();
        Element root = doc.createElement("app");
        doc.appendChild(root);
        
        XMLUtil.addElement(doc, root, "name", app.getName());
        XMLUtil.addElement(doc, root, "label", app.getLabel());
        XMLUtil.addElement(doc, root, "type", app.getType());
        XMLUtil.addElement(doc, root, "landingURL", app.getLandingUrl());
        
        Element urlsElem = doc.createElement("urls");
        
        if (app.getUrls() != null)
        {
	        for (AppUrl url : app.getUrls())
			{
				XMLUtil.addElement(doc, urlsElem, "url", url.getUrl());
			}
        }
        
        root.appendChild(urlsElem);
        
        return xmlDocToString(doc);
	}

	public static String serialize(ValidationRule vr, EnvData env) throws KommetException
	{
		Document doc = getXmlDoc();
        Element root = doc.createElement("validationRule");
        doc.appendChild(root);
        
        XMLUtil.addElement(doc, root, "name", vr.getName());
        XMLUtil.addElement(doc, root, "type", env.getType(vr.getTypeId()).getQualifiedName());
        XMLUtil.addElement(doc, root, "code", vr.getCode());
        XMLUtil.addElement(doc, root, "isActive", vr.getActive());
        XMLUtil.addElement(doc, root, "errorMessage", vr.getErrorMessage());
        XMLUtil.addElement(doc, root, "errorLabel", vr.getErrorMessageLabel());
        
        return xmlDocToString(doc);
	}

	public static String serialize(UniqueCheck uc, EnvData env) throws KommetException
	{
		Document doc = getXmlDoc();
        Element root = doc.createElement("uniqueCheck");
        doc.appendChild(root);
        
        XMLUtil.addElement(doc, root, "name", uc.getName());
        Type type = env.getType(uc.getTypeId());
        XMLUtil.addElement(doc, root, "type", type.getQualifiedName());
        
        Element fieldsElem = doc.createElement("fields");
        
        if (uc.getParsedFieldIds() == null || uc.getParsedFieldIds().isEmpty())
        {
        	throw new DeploymentException("Cannot serialize unique check with empty field list");
        }
        
        for (KID fieldId : uc.getParsedFieldIds())
		{
			XMLUtil.addElement(doc, fieldsElem, "fieldName", type.getField(fieldId).getApiName());
		}
        
        root.appendChild(fieldsElem);
     
        return xmlDocToString(doc);
	}

	public static String serialize(UserGroup group, EnvData env) throws KommetException
	{
		Document doc = getXmlDoc();
        Element root = doc.createElement("userGroup");
        doc.appendChild(root);
        
        XMLUtil.addElement(doc, root, "name", group.getName());
        XMLUtil.addElement(doc, root, "description", group.getDescription());
        
        return xmlDocToString(doc);
	}

	public static String serialize(DeployableType type, EnvData env) throws KommetException
	{
		return createTypeXML(type, env);
	}

	public static String serialize(Field field, DataService dataService, EnvData env) throws KommetException
	{
		return createFieldXML(field, dataService, env);
	}

	public static String serialize(Profile profile, EnvData env) throws KommetException
	{
		if (profile == null)
		{
			throw new KommetException("Cannot serialize null profile");
		}
		
		Document doc = getXmlDoc();
        Element root = doc.createElement("profile");
        doc.appendChild(root);
        
        XMLUtil.addElement(doc, root, "name", profile.getName());
        XMLUtil.addElement(doc, root, "label", profile.getLabel());
        
        return xmlDocToString(doc);
	}

	public static String serialize(ScheduledTask task, EnvData env) throws KommetException
	{
		Document doc = getXmlDoc();
        Element root = doc.createElement("scheduledTask");
        doc.appendChild(root);
        
        XMLUtil.addElement(doc, root, "name", task.getName());
        XMLUtil.addElement(doc, root, "cron", task.getCronExpression());
        XMLUtil.addElement(doc, root, "classMethod", task.getMethod());
        XMLUtil.addElement(doc, root, "class", task.getFile().getQualifiedName());
        
        return xmlDocToString(doc);
	}

	public static String serialize(ViewResource vr, EnvData env) throws KommetException
	{
		Document doc = getXmlDoc();
        Element root = doc.createElement("viewResource");
        doc.appendChild(root);
        
        XMLUtil.addElement(doc, root, "name", vr.getName());
        XMLUtil.addElement(doc, root, "mimeType", vr.getMimeType());
        XMLUtil.addElement(doc, root, "content", StringUtils.trimAllWhitespace(vr.getContent()));
        
        return xmlDocToString(doc);
	}

	public static String serialize(WebResource wr, EnvData env) throws KommetException
	{
		Document doc = getXmlDoc();
        Element root = doc.createElement("webResource");
        doc.appendChild(root);
        
        XMLUtil.addElement(doc, root, "name", wr.getName());
        XMLUtil.addElement(doc, root, "mimeType", wr.getMimeType());
        
        return xmlDocToString(doc);
	}

	public static String serialize(Action action, EnvData env) throws KommetException
	{
		Document doc = getXmlDoc();
        Element root = doc.createElement("action");
        doc.appendChild(root);
        
        XMLUtil.addElement(doc, root, "name", action.getName());
        XMLUtil.addElement(doc, root, "url", action.getUrl());
        XMLUtil.addElement(doc, root, "view", action.getView().getQualifiedName());
        XMLUtil.addElement(doc, root, "controller", action.getController().getQualifiedName());
        XMLUtil.addElement(doc, root, "controllerMethod", action.getControllerMethod());
        XMLUtil.addElement(doc, root, "isPublic", action.getIsPublic());
        
        // do not serialize type and isSystem properties on action, because it refers only to system actions
        // which are not deployable
        
        return xmlDocToString(doc);
	}
	
	/**
	 * Deploys components packages in a zip archive.
	 * @param zis
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional(rollbackFor = FailedPackageDeploymentException.class)
	public PackageDeploymentStatus deployZip (ZipInputStream zis, DeploymentConfig config, AuthData authData, EnvData env) throws KommetException
	{
		return deploy(zis, config, authData, env);
	}

	/**
	 * Deploys components packages in a zip archive.
	 * @param zis
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional(rollbackFor = FailedPackageDeploymentException.class)
	public PackageDeploymentStatus deployZip(byte[] packageFile, DeploymentConfig deployConfig, AuthData authData, EnvData env) throws KommetException
	{	
		return deploy(new ZipInputStream(new ByteArrayInputStream(packageFile)), deployConfig, authData, env);
	}

	public static String serializeRecords(Collection<? extends RecordProxy> recordProxies, Type type, AuthData authData, EnvData env) throws KommetException
	{
		Document doc = getXmlDoc();
        Element root = doc.createElement("records");
        doc.appendChild(root);
        
        for (RecordProxy obj : recordProxies)
        {
        	serializeRecord(obj, doc, root, type, authData, env);
        }
        
        return xmlDocToString(doc);
	}
}