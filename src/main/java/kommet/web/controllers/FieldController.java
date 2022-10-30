/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.auth.ProfileService;
import kommet.basic.FieldPermission;
import kommet.basic.Profile;
import kommet.basic.SettingValue;
import kommet.basic.UniqueCheck;
import kommet.basic.types.SystemTypes;
import kommet.config.Constants;
import kommet.config.UserSettingKeys;
import kommet.dao.FieldDefinitionException;
import kommet.dao.FieldFilter;
import kommet.dao.SettingValueFilter;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.UniqueCheckService;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.AutoNumber;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.EnumerationDataType;
import kommet.data.datatypes.FormulaDataType;
import kommet.data.datatypes.FormulaParser;
import kommet.data.datatypes.FormulaReturnType;
import kommet.data.datatypes.FormulaSyntaxException;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.NumberDataType;
import kommet.data.datatypes.TextDataType;
import kommet.data.datatypes.TypeReference;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.json.JSON;
import kommet.rest.RestUtil;
import kommet.uch.UserCascadeHierarchyContext;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.AppConfig;
import kommet.utils.DataUtil;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;
import kommet.utils.ValidationUtil;

@Controller
public class FieldController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@Inject
	DataService dataService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	UniqueCheckService uniqueCheckService;
	
	@Inject
	UserCascadeHierarchyService uchService;
	
	@Inject
	AppConfig appConfig;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/field/{rid}", method = RequestMethod.GET)
	public ModelAndView details (@PathVariable("rid") String sFieldId, HttpSession session) throws KommetException
	{
		KID fieldId = null;
		try
		{
			fieldId = KID.get(sFieldId);
		}
		catch (KIDException e)
		{
			return getErrorPage("Invalid field ID " + sFieldId + " passed in URL");
		}
		
		EnvData env = envService.getCurrentEnv(session);
		
		ModelAndView mv = new ModelAndView("fields/details");
		Field field = dataService.getField(fieldId, env);
		
		// need to fetch type separately because its KID is not fetched in the getField method above
		Type type = dataService.getById(field.getType().getId(), env);
		
		// object info is not fetched with the field, so we do it below
		field.setType(env.getType(type.getKID()));
		
		if (field.getDataTypeId().equals(DataType.ENUMERATION))
		{
			EnumerationDataType enumDT = (EnumerationDataType)field.getDataType();
			mv.addObject("enumValues", enumDT.getValues().split("\\r?\\n"));
			
			if (enumDT.getDictionary() != null)
			{
				mv.addObject("dictionaryId", enumDT.getDictionary().getId());
			}
		}
		
		boolean isUnique = false;
		for (UniqueCheck uc : uniqueCheckService.findForField(field, env, dataService))
		{
			if (uc.getParsedFieldIds().size() == 1)
			{
				isUnique = true;
				break;
			}
		}
		
		mv.addObject("field", field);
		mv.addObject("isUnique", isUnique);
		mv.addObject("canEdit", !Field.isSystemField(field.getApiName()));
		mv.addObject("isRequired", field.getDataTypeId().equals(DataType.FORMULA) ? FormulaParser.isFormulaNonNullable((FormulaDataType)field.getDataType(), type) : field.isRequired());
		mv.addObject("isCascadeDelete", field.getDataTypeId().equals(DataType.TYPE_REFERENCE) ? ((TypeReference)field.getDataType()).isCascadeDelete() : false);
		
		if (field.getDataType().isCollection())
		{
			mv.addObject("collectionDisplay", uchService.getUserSettingAsString(UserSettingKeys.KM_SYS_COLLECTION_DISPLAY_MODE + "." + field.getKID(), AuthUtil.getAuthData(session), AuthData.getRootAuthData(env), env));
		}
		

		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/field/delete", method = RequestMethod.POST)
	@ResponseBody
	public void deleteField (@RequestParam("id") String id, @RequestParam("typePrefix") String prefix, HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		PrintWriter out = resp.getWriter();
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		
		KID fieldId = null;
		try
		{
			fieldId = KID.get(id);
		}
		catch (KIDException e)
		{
			out.write(RestUtil.getRestErrorResponse("Invalid field ID " + id));
			return;
		}
		
		KeyPrefix typePrefix = null;
		typePrefix = KeyPrefix.get(prefix);
		
		Type type = env.getType(typePrefix);
		if (type == null)
		{
			out.write(RestUtil.getRestErrorResponse("Type not found"));
			return;
		}
		
		if (!authData.canEditType(type.getKID(), true, env))
		{
			out.write(RestUtil.getRestErrorResponse("Cannot delete field due to insufficient permissions"));
			return;
		}
		
		Field field = type.getField(fieldId);
		
		List<Field> formulaFields = dataService.getFormulaFieldsUsingField(fieldId, env);
		if (!formulaFields.isEmpty())
		{
			List<String> formulaFieldNames = new ArrayList<String>();
			for (Field formulaField : formulaFields)
			{
				formulaFieldNames.add(env.getType(formulaField.getType().getKID()).getQualifiedName() + "." + formulaField.getApiName());
			}
			
			out.write(RestUtil.getRestErrorResponse("Field cannot be deleted because it is used in formula fields: " + MiscUtils.implode(formulaFieldNames, ", ")));
			return;
		}
		
		if (field.getKID().equals(type.getDefaultFieldId()))
		{
			out.write(RestUtil.getRestErrorResponse("Field cannot be deleted because it is the default field for its type"));
			return;
		}
		
		dataService.deleteField(field, true, false, authData, env);
		
		out.write(RestUtil.getRestSuccessResponse("Field has been deleted"));
		return;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/field/edit/{rid}", method = RequestMethod.GET)
	public ModelAndView edit (@PathVariable("rid") String rid, HttpSession session) throws KommetException
	{
		clearMessages();
		
		KID fieldId = null;
		try
		{
			fieldId = KID.get(rid);
		}
		catch (KIDException e)
		{
			e.printStackTrace();
			return getErrorPage("Invalid object id " + rid);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		
		ModelAndView mv = new ModelAndView("fields/edit");
		Field field = dataService.getField(fieldId, env);
		
		if (Field.isSystemField(field.getApiName()))
		{
			throw new KommetException("Field " + field.getApiName() + " is a system field and cannot be edited");
		}
		
		// need to fetch type separately because its KID is not fetched in the getField method above
		Type type = dataService.getById(field.getType().getId(), env);
		
		// object info is not fetched with the field, so we do it below
		field.setType(env.getType(type.getKID()));
		
		mv = prepareEditView(mv, field, type, null, AuthUtil.getAuthData(session), env);
		
		if (field.getDataTypeId().equals(DataType.ASSOCIATION))
		{
			mv.addObject("linkingTypeId", ((AssociationDataType)field.getDataType()).getLinkingType().getKID());
		}
		
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/field/new", method = RequestMethod.GET)
	public ModelAndView newField(@RequestParam("typeId") String typeId, HttpSession session) throws KommetException
	{
		clearMessages();
		
		EnvData env = envService.getCurrentEnv(session); 
		Field field = new Field();
		Type type = env.getType(KID.get(typeId));
		field.setType(type);
		
		return prepareEditView(null, null, type, "tab", AuthUtil.getAuthData(session), env);
	}
	
	private ModelAndView prepareEditView (ModelAndView mv, Field field, Type type, String collectionDisplay, AuthData authData, EnvData env) throws KommetException
	{
		if (mv == null)
		{
			mv = new ModelAndView("fields/edit");
		}
		
		// lists all type reference properties that reference the current type
		Map<KID, List<String>> inversePropertiesByType = new LinkedHashMap<KID, List<String>>();
		
		mv.addObject("type", type);
		mv.addObject("dataTypes", getDataTypes());
		mv.addObject("typesToReference", JSON.serialize(getTypesForReference(env), authData));
		mv.addObject("typesForInverseCollection", JSON.serialize(getTypesForInverseCollection(type, inversePropertiesByType, env), authData));
		mv.addObject("inverseProperties", JSON.serialize(inversePropertiesByType, authData));
		List<CandidateLinkingType> linkingTypeCandidates = getLinkingTypeCandidates(type, env);
		
		List<String> linkingTypeJSONItems = new ArrayList<String>();
		
		List<Type> linkingTypes = new ArrayList<Type>();
		for (CandidateLinkingType candidate : linkingTypeCandidates)
		{
			linkingTypeJSONItems.add("\"" + candidate.getType().getKID() + "\": " + candidate.toJSON());
			linkingTypes.add(candidate.getType());
		}		
		
		mv.addObject("linkingTypeCandidates", JSON.serialize(linkingTypes, authData));
		mv.addObject("associatedTypes", JSON.serialize(env.getUserAccessibleTypes(), authData));
		mv.addObject("linkingTypeOptions", "{ " + MiscUtils.implode(linkingTypeJSONItems, ", ") + " }");
		
		if (field != null)
		{
			mv.addObject("field", field);
			mv.addObject("pageTitle", field.getLabel() != null ? field.getLabel() : "New field");
			mv.addObject("isUnique", !uniqueCheckService.findForField(field, env, dataService).isEmpty());
			
			if (field.getDataType() != null)
			{
				// if field is being edited, users may have left the data type unspecified
				mv.addObject("isRequired", field.getDataTypeId().equals(DataType.FORMULA) ? (((FormulaDataType)field.getDataType()).getParsedDefinition() != null ? FormulaParser.isFormulaNonNullable((FormulaDataType)field.getDataType(), type) : false) : field.isRequired());
				
				if (field.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
				{
					Type inverseType = ((InverseCollectionDataType)field.getDataType()).getInverseType();
					mv.addObject("inverseTypeId", inverseType != null ? inverseType.getKID() : null);
				}
				else if (field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
				{
					mv.addObject("referencedTypeId", ((TypeReference)field.getDataType()).getTypeId());
				}
				else if (field.getDataTypeId().equals(DataType.ENUMERATION))
				{
					EnumerationDataType enumDT = (EnumerationDataType)field.getDataType();
					mv.addObject("dictionaryId", enumDT.getDictionary() != null ? enumDT.getDictionary().getId() : null);
				}
				
				mv.addObject("inverseProperty", field.getDataTypeId().equals(DataType.INVERSE_COLLECTION) ? ((InverseCollectionDataType)field.getDataType()).getInverseProperty() : null);
				mv.addObject("decimalPlaces", field.getDataTypeId().equals(DataType.NUMBER) ? ((NumberDataType)field.getDataType()).getDecimalPlaces() : null);
				mv.addObject("javaType", field.getDataTypeId().equals(DataType.NUMBER) ? ((NumberDataType)field.getDataType()).getJavaType() : null);
				mv.addObject("isCascadeDelete", field.getDataTypeId().equals(DataType.TYPE_REFERENCE) ? ((TypeReference)field.getDataType()).isCascadeDelete() : false);
				
				if (field.getDataTypeId().equals(DataType.ASSOCIATION))
				{
					Type linkingType = ((AssociationDataType)field.getDataType()).getLinkingType();
					mv.addObject("selfLinkingFieldId", linkingType != null ? linkingType.getField(((AssociationDataType)field.getDataType()).getSelfLinkingField()).getKID() : null);
					mv.addObject("foreignLinkingFieldId", linkingType != null ? linkingType.getField(((AssociationDataType)field.getDataType()).getForeignLinkingField()).getKID() : null);
					
					mv.addObject("associatedTypeId", ((AssociationDataType)field.getDataType()).getAssociatedTypeId());
					mv.addObject("associationType", linkingType.isAutoLinkingType() ? "direct" : "through linking type");
				}
			}
			
			if (field.getDataType().isCollection())
			{
				if (!StringUtils.hasText(collectionDisplay))
				{
					collectionDisplay = uchService.getUserSettingAsString(UserSettingKeys.KM_SYS_COLLECTION_DISPLAY_MODE + "." + field.getKID(), authData, AuthData.getRootAuthData(env), env);
				}
				
				mv.addObject("collectionDisplay", collectionDisplay);
			}
		}
		else
		{
			mv.addObject("pageTitle", "New field");
		}
		
		mv.addObject("isDefaultField", field != null && type.getDefaultFieldId().equals(field.getKID()));
		
		return mv;
	}
	
	/**
	 * Get a list of types which can serve as a linking type for an association that has this type
	 * on the owner side. They must be types with an type reference field to the current type.
	 * @param type
	 * @return
	 * @throws KommetException 
	 */
	private List<CandidateLinkingType> getLinkingTypeCandidates(Type type, EnvData env) throws KommetException
	{
		// get fields that are an type reference to the current type
		FieldFilter filter = new FieldFilter();
		filter.setDataType(new TypeReference());
		filter.setObjectRefTypeId(type.getKID());
		List<Field> candidateFields = dataService.getFields(filter, env);
		
		List<CandidateLinkingType> candidateTypes = new ArrayList<CandidateLinkingType>();
		
		Set<KID> candidateTypeIds = new HashSet<KID>();
		
		// we now have a list of fields that are an type reference to the current type
		// but we need to check if their types also have at least one other type reference
		for (Field candidateField : candidateFields)
		{
			Type candidateType = env.getType(candidateField.getType().getKID());
			
			if (candidateTypeIds.contains(candidateType.getKID()))
			{
				// skip if type has already been added
				continue;
			}
			
			candidateTypeIds.add(candidateType.getKID());
			
			CandidateLinkingType linkingType = new CandidateLinkingType(candidateType);
			int objectRefFieldCount = 0;
			
			// iterate through field of the candidate type to check if it has candidate self and foreign fields
			for (Field field : candidateType.getFields())
			{
				if (!field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
				{
					continue;
				}
				
				TypeReference dt = (TypeReference)field.getDataType();
				if (!dt.isCascadeDelete())
				{
					// Cascade delete has to turned on on the type reference field. This guarantees that
					// when the type referenced by the linking type is deleted, the linking type record
					// will be deleted as well.
					continue;
				}
				
				objectRefFieldCount++;
				
				// any type reference field can serve as a foreign linking field
				linkingType.getForeignLinkingFields().add(field);
				
				if (dt.getTypeId().equals(type.getKID()))
				{
					linkingType.getSelfLinkingFields().add(field);
				}
			}
			
			// to be a valid candidate for a linking field, the type must fulfill two conditions:
			// it must contain a reference to the current type and at least one other reference
			if (objectRefFieldCount > 1)
			{
				candidateTypes.add(linkingType);
			}
		}
		
		return candidateTypes;
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/field/save", method = RequestMethod.POST)
	public ModelAndView saveField (@RequestParam(value = "fieldId", required = false) String sFieldId,
									@RequestParam(value = "typeId", required = false) String sTypeId,
									@RequestParam(value = "apiName", required = false) String apiName,
									@RequestParam(value = "label", required = false) String label,
									@RequestParam(value = "labelKey", required = false) String labelKey,
									@RequestParam(value = "dataType", required = false) Integer dataTypeId,
									@RequestParam(value = "textDataTypeLength", required = false) String textDataTypeLength,
									@RequestParam(value = "enumValues", required = false) String enumValues,
									@RequestParam(value = "referencedObject", required = false) String sReferencedTypeId,
									@RequestParam(value = "required", required = false) String required,
									@RequestParam(value = "trackHistory", required = false) String trackHistory,
									@RequestParam(value = "unique", required = false) String unique,
									@RequestParam(value = "cascadeDelete", required = false) String cascadeDelete,
									@RequestParam(value = "inverseTypeId", required = false) String sInverseTypeId,
									@RequestParam(value = "inverseProperty", required = false) String inverseProperty,
									@RequestParam(value = "selfLinkingFieldId", required = false) String sSelfLinkingFieldId,
									@RequestParam(value = "foreignLinkingFieldId", required = false) String sForeignLinkingFieldId,
									@RequestParam(value = "linkingTypeId", required = false) String sLinkingTypeId,
									@RequestParam(value = "associatedTypeId", required = false) String sAssociatedTypeId,
									@RequestParam(value = "formula", required = false) String formula,
									@RequestParam(value = "description", required = false) String description,
									@RequestParam(value = "defaultValue", required = false) String defaultValue,
									@RequestParam(value = "validateEnum", required = false) String validateEnum,
									@RequestParam(value = "decimalPlaces", required = false) Integer decimalPlaces,
									@RequestParam(value = "javaType", required = false) String javaType,
									@RequestParam(value = "textFieldDisplay", required = false) String textFieldDisplay,
									@RequestParam(value = "collectionDisplay", required = false) String collectionDisplay,
									@RequestParam(value = "isFormattedText", required = false) String sIsFormattedText,
									@RequestParam(value = "associationType", required = false) String associationType,
									@RequestParam(value = "autonumberFormat", required = false) String autonumberFormat,
									@RequestParam(value = "dictionaryId", required = false) String sDictionaryId,
									@RequestParam(value = "isDefaultField", required = false) String isDefaultField,
									HttpSession session) throws KommetException
	{
		clearMessages();
		
		KID fieldId = null;
		Field field = null;
		EnvData env = envService.getCurrentEnv(session);
		AuthData authData = AuthUtil.getAuthData(session);
		boolean newField = false;
		Type type = env.getType(KID.get(sTypeId));
		boolean textFieldLengthSetAutomatically = false;
		boolean isCreateLinkingType = false;
		
		if (StringUtils.hasText(sFieldId))
		{
			fieldId = KID.get(sFieldId);
			field = dataService.getFieldForUpdate(fieldId, env);
		}
		else
		{
			field = new Field();
			field.setType(type);
			newField = true;
		}
		
		field.setLabel(label);
		
		env.getType(type.getKeyPrefix()).getField(fieldId);
		
		ModelAndView mv = new ModelAndView("fields/edit");
		
		if (!StringUtils.hasText(label))
		{
			addError("Please enter field label");
		}
		
		if (!StringUtils.hasText(apiName))
		{
			addError("Please enter field API name");
		}
		else
		{
			if (!ValidationUtil.isValidFieldApiName(apiName))
			{
				addError("Invalid API name. Field API name must start with a letter and may contain only letters of the Latin alphabet, digits and an underscore");
			}
			else
			{
				// make sure a field with this name does not already exist
				Field existingField = type.getField(apiName);
				if (existingField != null && (fieldId == null || !fieldId.equals(existingField.getKID())))
				{
					addError("Field " + apiName + " already exists");
				}
			}
		}
		
		if (dataTypeId == null)
		{
			addError("Please select data type");
		}
		else
		{
			DataType dt = DataType.getById(dataTypeId);
			if (dt == null)
			{
				addError("Unknown data type ID " + dataTypeId);
			}
			field.setDataType(dt);
			
			if (dt.getId().equals(DataType.TEXT))
			{
				TextDataType textDT = new TextDataType();
				
				Integer maxTextLength = uchService.getUserSettingAsInt(UserSettingKeys.KM_ROOT_MAX_TEXTFIELD_LENGTH, authData, AuthData.getRootAuthData(env), env);
				
				if (maxTextLength == null)
				{
					maxTextLength = appConfig.getMaxTextFieldLength();
				}
				
				if (!StringUtils.hasText(textDataTypeLength))
				{
					textFieldLengthSetAutomatically = true;
					textDT.setLength(maxTextLength);
				}
				else
				{
					Integer length = Integer.parseInt(textDataTypeLength);
					
					if (length > maxTextLength)
					{
						addError("Text field maximum length is greater than the maximum allowed value " + maxTextLength);
					}
					
					textDT.setLength(length);
				}
				
				textDT.setLong("multiLine".equals(textFieldDisplay));
				textDT.setFormatted("true".equals(sIsFormattedText));
				
				field.setDataType(textDT);
			}
			else if (dt.getId().equals(DataType.AUTO_NUMBER))
			{
				AutoNumber autoNumberDT = new AutoNumber();
				
				if (!StringUtils.hasText(autonumberFormat))
				{
					addError("Please fill the auto-number prefix");
				}
				
				try
				{
					autoNumberDT.setFormat(autonumberFormat);
				}
				catch (FieldDefinitionException e)
				{
					// if autonumber format is invalid, error will be thrown
					if (StringUtils.hasText(autonumberFormat))
					{
						addError(e.getMessage());
					}
				}
				
				field.setDataType(autoNumberDT);
				unique = "true";
			}
			else if (dt.getId().equals(DataType.TYPE_REFERENCE))
			{
				if (!StringUtils.hasText(sReferencedTypeId))
				{
					addError("Select referenced type");
				}
				else
				{
					field.setDataType(new TypeReference(env.getType(KID.get(sReferencedTypeId))));
					if ("true".equals(cascadeDelete));
					{
						((TypeReference)field.getDataType()).setCascadeDelete(true);
					}
				}
			}
			else if (dt.getId().equals(DataType.INVERSE_COLLECTION))
			{
				InverseCollectionDataType inverseDataType = new InverseCollectionDataType();
				
				if (!StringUtils.hasText(sInverseTypeId))
				{
					addError("Select referenced type");
				}
				else
				{
					inverseDataType.setInverseType(env.getType(KID.get(sInverseTypeId)));
				}
				
				if (!StringUtils.hasText(inverseProperty))
				{
					addError("Select referenced type property");
				}
				else
				{
					inverseDataType.setInverseProperty(inverseProperty);
				}
				
				field.setDataType(inverseDataType);
			}
			else if (dt.getId().equals(DataType.ENUMERATION))
			{
				EnumerationDataType enumDataType = new EnumerationDataType(enumValues);
				
				if (StringUtils.hasText(sDictionaryId))
				{
					enumDataType.setDictionary(env.getDictionaries().get(KID.get(sDictionaryId)));
				}
				
				if (!StringUtils.hasText(enumValues))
				{
					if (!StringUtils.hasText(sDictionaryId))
					{
						// if neither dictionary not enum values are defined, add an error
						addError("At least one enumeration value is required");
					}
				}
				else
				{
					enumDataType.setValidateValues("true".equals(validateEnum));
				}
				
				field.setDataType(enumDataType);
			}
			else if (dt.getId().equals(DataType.NUMBER))
			{
				if (decimalPlaces == null)
				{
					addError("Define the precision of the numeric field");
				}
				else
				{	
					if (!StringUtils.hasText(javaType))
					{
						addError("Java type not specified");
					}
					else
					{
						Class<?> javaClassObj = null;
						try
						{
							javaClassObj = Class.forName(javaType);
							NumberDataType numberDataType = new NumberDataType(decimalPlaces, javaClassObj);
							field.setDataType(numberDataType);
						}
						catch (ClassNotFoundException e)
						{
							addError("Java class with name " + javaType + " does not exist");
						}
					}
				}
			}
			else if (dt.getId().equals(DataType.FORMULA))
			{
				if (!StringUtils.hasText(formula))
				{
					addError("Please enter formula text");
				}
				else
				{
					try
					{
						field.setDataType(new FormulaDataType(FormulaReturnType.TEXT, formula, type, env));
					}
					catch (FormulaSyntaxException e)
					{
						addError(e.getMessage());
					}
					catch (KommetException e)
					{
						addError("Error creating formula field: " + e.getMessage());
					}
				}
			}
			else if (dt.getId().equals(DataType.ASSOCIATION))
			{
				KID selfLinkingFieldId = null;
				KID foreignLinkingFieldId = null;
				Type linkingType = null;
				
				if ("linking-type".equals(associationType))
				{
					// parse linking type
					if (!StringUtils.hasText(sLinkingTypeId))
					{
						addError("Please select linking type for the association");
					}
					else
					{
						try
						{
							linkingType = env.getType(KID.get(sLinkingTypeId));
						}
						catch (KIDException e)
						{
							addError("Invalid linking type ID " + sLinkingTypeId);
						}
					}
					
					// parse self linking field
					if (!StringUtils.hasText(sSelfLinkingFieldId))
					{
						addError("Specify self linking field for the association");
					}
					else
					{
						try
						{
							selfLinkingFieldId = KID.get(sSelfLinkingFieldId);
						}
						catch (KIDException e)
						{
							addError("Invalid ID of self linking field: " + sSelfLinkingFieldId);
						}
					}
					
					// parse foreign linking field
					if (!StringUtils.hasText(sForeignLinkingFieldId))
					{
						addError("Specify foreign linking field for the association");
					}
					else
					{
						try
						{
							foreignLinkingFieldId = KID.get(sForeignLinkingFieldId);
						}
						catch (KIDException e)
						{
							addError("Invalid ID of foreign linking field: " + sForeignLinkingFieldId);
						}
					}
					
					if (selfLinkingFieldId != null && foreignLinkingFieldId != null)
					{
						Field foreignLinkingField = dataService.getField(foreignLinkingFieldId, env);
						if (foreignLinkingField == null)
						{
							addError("Foreign linking field with ID " + foreignLinkingFieldId + " not found");
						}
						else
						{
							if (!foreignLinkingField.getDataTypeId().equals(DataType.TYPE_REFERENCE))
							{
								addError("Foreign linking field must be of type type reference");
							}
							else
							{
								Field selfLinkingField = dataService.getField(selfLinkingFieldId, env);
								if (selfLinkingField == null)
								{
									addError("Self linking field with ID " + selfLinkingFieldId + " not found");
								}
								field.setDataType(new AssociationDataType(linkingType, ((TypeReference)foreignLinkingField.getDataType()).getType(), selfLinkingField.getApiName(), foreignLinkingField.getApiName()));
							}
						}
					}
				}
				else if ("direct".equals(associationType))
				{
					if (!StringUtils.hasText(sAssociatedTypeId))
					{
						addError("Please select associated type");
					}
					else
					{
						isCreateLinkingType = true;
					}
				}
				else
				{
					addError("Please select association type");
				}
			}
		}
		
		if (StringUtils.hasText(labelKey))
		{
			field.setUchLabel(labelKey);
		}
		else
		{
			field.setUchLabel(null);
		}
		
		boolean isRequired = "true".equals(required);
		
		// when data type is inverse collection, the field will never be required
		// Note: dataTypeId may be null at this point because although there was a validation of this above,
		// it did not yet exit the method
		if (dataTypeId != null && (DataType.INVERSE_COLLECTION == dataTypeId || DataType.ASSOCIATION == dataTypeId))
		{
			isRequired = false;
		}
		else if (dataTypeId == DataType.AUTO_NUMBER)
		{
			isRequired = true;
		}
		
		if (!newField)
		{
			// process non-formula fields, including fields for which (by mistake) data type has not been set
			if (dataTypeId == null || dataTypeId != DataType.FORMULA)
			{
				if (isRequired)
				{
					// make sure there are no records with null values in this field
					List<Record> records = env.getSelectCriteriaFromDAL("select id from " + field.getType().getQualifiedName() + " WHERE " + field.getApiName() + " ISNULL limit 1").list();
					if (!records.isEmpty())
					{
						addError("The field cannot be made required because there are already some records for this object");
					}
					else
					{
						field.setRequired(true);
					}
				}
				else
				{
					// make sure that the field is not a default field
					if (field.getKID().equals(type.getDefaultFieldId()))
					{
						addError("Field must remain required because it is a default field for its type");
					}
					else
					{
						field.setRequired(false);
					}
				}
			}
			// if it's a formula field
			else 
			{	
				// formula fields are never required
				field.setRequired(false);
				
				// if it's a default field, is has to be non-nullable
				if (type.getDefaultFieldId().equals(field.getKID()) && !FormulaParser.isFormulaNonNullable((FormulaDataType)field.getDataType(), type))
				{
					addError("This formula field is a default field for its type, so all of the fields it references must be required");
				}
			}
		}
		else // if it is a new field
		{
			if (isRequired)
			{
				// make sure no records exist for this type, because if they do, they will be empty and so
				// the field cannot be required
				List<Record> records = env.getSelectCriteriaFromDAL("select id from " + field.getType().getQualifiedName() + " LIMIT 1").list();
				if (!records.isEmpty())
				{
					addError("The field cannot be made required because there exist some records for this type");
				}
				else
				{
					field.setRequired(isRequired);
				}
			}
			else
			{
				field.setRequired(isRequired);
			}
		}
		
		// change field name only after it has been used in queries checking for unique values above
		field.setApiName(apiName);
		field.setDescription(description);
		field.setDefaultValue(StringUtils.hasText(defaultValue) ? defaultValue : null);
		
		if ("true".equals(trackHistory))
		{
			if (!field.getDataTypeId().equals(DataType.ASSOCIATION) && !field.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
			{
				field.setTrackHistory(true);
			}
			else
			{
				addError("Cannot track history for a collection field");
			}
		}
		else
		{
			field.setTrackHistory(false);
		}
		
		if ("true".equals(isDefaultField))
		{
			if (!DataService.isValidDefaultField(field, type))
			{
				if (!field.getDataTypeId().equals(DataType.FORMULA))
				{
					addError("Field cannot be set as default because it is not a required field");
				}
				else
				{
					addError("Formula field cannot be set as default because not all fields used in it are required");
				}
			}
		}
		
		if (hasErrorMessages())
		{
			mv = prepareEditView(null, field, type, collectionDisplay, AuthUtil.getAuthData(session), env);
			mv.addObject("errorMsgs", getErrorMsgs());
			mv.addObject("field", field);
			
			if (textFieldLengthSetAutomatically)
			{
				// user did not specify field length, so we don't display it for them
				((TextDataType)field.getDataType()).setLength(null);
			}
			
			mv.addObject("linkingTypeId", sLinkingTypeId);
			return mv;
		}
		
		Type linkingType = null;
		
		if (isCreateLinkingType)
		{
			Type associatedType = env.getType(KID.get(sAssociatedTypeId));
			
			// create a linking type between this type and the associated type
			DataService.LinkingTypeResult result = dataService.createLinkingType(type, associatedType, authData, env);
			linkingType = result.getType();
			
			
			AssociationDataType assocDT = new AssociationDataType();
			assocDT.setLinkingType(linkingType);
			assocDT.setSelfLinkingField(result.getSelfLinkingField().getApiName());
			assocDT.setForeignLinkingField(result.getForeignLinkingField().getApiName());
			assocDT.setAssociatedType(associatedType);
			field.setDataType(assocDT);
		}
		
		if (field.getKID() == null)
		{
			try
			{
				field = dataService.createField(field, authData, envService.getCurrentEnv(session));
			}
			catch (Exception e)
			{
				e.printStackTrace();
				
				// delete the linking type if it was created
				if (isCreateLinkingType)
				{
					dataService.deleteType(linkingType, authData, env);
				}
				
				mv = prepareEditView(null, field, type, collectionDisplay, AuthUtil.getAuthData(session), env);
				mv.addObject("errorMsgs", getMessage(e.getMessage()));
				mv.addObject("field", field);
				mv.addObject("linkingTypeId", sLinkingTypeId);
				return mv;
			}
		}
		else
		{
			field = dataService.updateField(field, AuthUtil.getAuthData(session), envService.getCurrentEnv(session));
		}
		
		// after the field is saved and we have its ID, we can define user settings for it
		if (field.getDataType().isCollection())
		{
			SettingValueFilter filter = new SettingValueFilter();
			filter.addKey(UserSettingKeys.KM_SYS_COLLECTION_DISPLAY_MODE + "." + field.getKID());
			List<SettingValue> settings = uchService.getSettings(filter, authData, AuthData.getRootAuthData(env), env);
			if (!settings.isEmpty())
			{
				SettingValue setting = settings.get(0);
				setting.setValue(collectionDisplay);
				uchService.saveSetting(setting, setting.getHierarchy().getActiveContext(), setting.getHierarchy().getActiveContextValue(), authData, env);
			}
			else
			{
				uchService.saveUserSetting(UserSettingKeys.KM_SYS_COLLECTION_DISPLAY_MODE + "." + field.getKID(), collectionDisplay, UserCascadeHierarchyContext.ENVIRONMENT, true, authData, env);
			}
		}
		else
		{
			// TODO remove the user setting if field data type has changed from collection to something else
		}
		
		// only after the field is saved can we add a check for it
		if ("true".equals(unique))
		{
			// check if a unique check for this field already exists
			if (uniqueCheckService.findForField(field, env, dataService).isEmpty())
			{
				UniqueCheck uniqueCheck = new UniqueCheck();
				uniqueCheck.setTypeId(field.getType().getKID());
				uniqueCheck.setFieldIds(field.getKID().getId());
				uniqueCheck.setName(Constants.UNIQUE_CHECK_FIELD_PREFIX + field.getKID());
				
				// save the unique check
				uniqueCheckService.save(uniqueCheck, AuthUtil.getAuthData(session), env);
			}
		}
		else
		{
			// delete the unique check for this field
			uniqueCheckService.deleteForField(field, env, dataService);
		}
		
		if ("true".equals(isDefaultField))
		{
			// refetch type with the newly-added field
			type = env.getType(type.getKeyPrefix());
			
			// update default field on type
			type.setDefaultFieldId(field.getKID());
			dataService.updateType(type, authData, env);
		}
		
		// redirect to field details
		return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/field/" + field.getKID());
	}

	private List<Type> getTypesForReference(EnvData env) throws KommetException
	{
		return env.getUserAccessibleTypes();
	}
	
	private List<Type> getTypesForInverseCollection(Type referencedType, Map<KID, List<String>> inversePropertiesByType, EnvData env) throws KommetException
	{
		Collection<Type> allTypes = env.getAllTypes();
		List<Type> filteredTypes = new ArrayList<Type>();
		for (Type type : allTypes)
		{
			if (!type.isAccessible())
			{
				continue;
			}
			
			inversePropertiesByType.put(type.getKID(), new ArrayList<String>());
			
			// iterate over the type's fields to see if any of the referenced the referencedType
			for (Field field : type.getFields())
			{
				if (DataType.TYPE_REFERENCE == field.getDataTypeId() && ((TypeReference)field.getDataType()).getTypeId().equals(referencedType.getKID()))
				{
					inversePropertiesByType.get(type.getKID()).add(field.getApiName());
					filteredTypes.add(type);
					break;
				}
			}
		}
		
		return filteredTypes;
	}
	
	private List<DataTypeWrapper> getDataTypes()
	{
		List<DataTypeWrapper> dataTypes = new ArrayList<DataTypeWrapper>();
		dataTypes.add(new DataTypeWrapper("Text", DataType.TEXT));
		dataTypes.add(new DataTypeWrapper("AutoNumber", DataType.AUTO_NUMBER));
		dataTypes.add(new DataTypeWrapper("Number", DataType.NUMBER));
		dataTypes.add(new DataTypeWrapper("Date/Time", DataType.DATETIME));
		dataTypes.add(new DataTypeWrapper("Date", DataType.DATE));
		dataTypes.add(new DataTypeWrapper("Boolean", DataType.BOOLEAN));
		dataTypes.add(new DataTypeWrapper("Enumeration", DataType.ENUMERATION));
		dataTypes.add(new DataTypeWrapper("Email", DataType.EMAIL));
		dataTypes.add(new DataTypeWrapper("Object Reference (many-to-one relationship)", DataType.TYPE_REFERENCE));
		dataTypes.add(new DataTypeWrapper("Collection (one-to-many relationship)", DataType.INVERSE_COLLECTION));
		dataTypes.add(new DataTypeWrapper("Association (many-to-many relationship)", DataType.ASSOCIATION));
		dataTypes.add(new DataTypeWrapper("Formula", DataType.FORMULA));
		return dataTypes;
	}

	public class DataTypeWrapper
	{
		private String name;
		private Integer id;
		
		public DataTypeWrapper (String name, Integer id)
		{
			this.name = name;
			this.id = id;
		}
		
		public String getName()
		{
			return name;
		}
		public Integer getId()
		{
			return id;
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/fieldpermissions/{fieldId}", method = RequestMethod.GET)
	public ModelAndView objectPermissions(@PathVariable("fieldId") String rid, HttpSession session) throws KommetException
	{
		KID fieldId = null;
		try
		{
			fieldId = KID.get(rid);
		}
		catch (KIDException e)
		{
			e.printStackTrace();
			return getErrorPage("Invalid object id " + rid);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		Field field = dataService.getField(fieldId, env);
		
		// find permissions for this object and all profiles
		List<Record> permissions = env.getSelectCriteriaFromDAL("select id, read, edit, profile.id, profile.name, permissionSet.id, fieldId from " + SystemTypes.FIELD_PERMISSION_API_NAME + " where fieldId = '" + field.getKID().getId() + "'").list();
		Map<KID, FieldPermission> fieldPermissionsByProfile = new HashMap<KID, FieldPermission>();
		for (Record permission : permissions)
		{
			FieldPermission fieldPerm = new FieldPermission(permission, env);
			Profile profile = new Profile(null, env);
			profile.setId((KID)permission.getField("profile.id"));
			profile.setName((String)permission.getField("profile.name"));
			fieldPerm.setProfile(profile);
			fieldPermissionsByProfile.put((KID)permission.getField("profile.id"), fieldPerm);
		}
		
		// for profiles for which we have no access, add empty access
		List<Profile> allProfiles = profileService.getProfiles(env);
		for (Profile profile : allProfiles)
		{
			if (!fieldPermissionsByProfile.containsKey(profile.getId()))
			{
				FieldPermission permission = new FieldPermission(null, env);
				permission.setRead(false);
				permission.setEdit(false);
				permission.setFieldId(field.getKID());
				permission.setProfile(profile);
				fieldPermissionsByProfile.put(profile.getId(), permission);
			}
		}
		
		AuthData authData = AuthUtil.getAuthData(session);
		
		ModelAndView mv = new ModelAndView("fields/fieldpermissions");
		mv.addObject("permissions", fieldPermissionsByProfile.values());
		mv.addObject("field", field);
		mv.addObject("canEdit", DataUtil.canEditFieldPermissions(field, authData));
		return mv;
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/fieldpermissions/save", method = RequestMethod.POST)
	public ModelAndView saveFieldPermissions(@RequestParam("fieldId") String fieldKID, HttpSession session, HttpServletRequest req) throws KommetException
	{
		KID fieldId = null;
		try
		{
			fieldId = KID.get(fieldKID);
		}
		catch (KIDException e)
		{
			e.printStackTrace();
			return getErrorPage("Invalid field id " + fieldKID);
		}
		
		EnvData env = envService.getCurrentEnv(session);
		Field field = dataService.getField(fieldId, env);
		
		// find permissions for this object and all profiles
		List<Record> permissions = env.getSelectCriteriaFromDAL("select id, read, edit, profile.id, profile.name, permissionSet.id, fieldId from " + SystemTypes.FIELD_PERMISSION_API_NAME + " where fieldId = '" + field.getKID().getId() + "'").list();
		Map<KID, FieldPermission> fieldPermissionsByProfile = new HashMap<KID, FieldPermission>();
		for (Record permission : permissions)
		{
			// Since checkbox values for granted permission access are only passed when the checkbox is
			// selected, unselected checkboxes are not passed at all and we would not know
			// that the given permission should be revoked.
			// This is why at the beginning we set all permissions to false.
			permission.setField("read", false);
			permission.setField("edit", false);
			fieldPermissionsByProfile.put((KID)permission.getField("profile.id"), new FieldPermission(permission, env));
		}
		
		Type fieldPermissionType = env.getType(KeyPrefix.get(KID.FIELD_PERMISSION_PREFIX));
		
		Enumeration<String> paramNames = req.getParameterNames();
		while (paramNames.hasMoreElements())
		{
			String param = paramNames.nextElement();
			if (param.startsWith("perm_"))
			{
				String[] val = req.getParameterValues(param);
				
				if (val != null && val.length > 0)
				{
					String fieldName = param.replaceFirst("perm_", ""); 
					if (val.length > 1)
					{
						throw new KommetException("Field " + fieldName + " appears more than one on the page");
					}
					
					String[] fieldPermissionIndex = fieldName.split("_");
					KID profileId = KID.get(fieldPermissionIndex[0]);
					
					// check if the object permission for this object and profile already exists
					FieldPermission permission = fieldPermissionsByProfile.get(profileId);
					Record permissionRec = permission != null ? permission.getRecord() : null;
					if (permission == null || permissionRec == null)
					{
						permissionRec = new Record(fieldPermissionType);
						permissionRec.setField("profile.id", profileId, env);
						permissionRec.setField("fieldId", fieldId);
						permissionRec.setField("read", false);
						permissionRec.setField("edit", false);
					}
					
					String accessType = fieldPermissionIndex[1];
					if ("read".equals(accessType))
					{
						permissionRec.setField("read", Boolean.valueOf(val[0]));
					}
					else if ("edit".equals(accessType))
					{
						permissionRec.setField("edit", Boolean.valueOf(val[0]));
					}
					else
					{
						throw new KommetException("Unknown access type '" + accessType + "'");
					}
					
					fieldPermissionsByProfile.put(profileId, new FieldPermission(permissionRec, env));
				}
			}
		}
		
		// save updated object permissions
		for (FieldPermission permission : fieldPermissionsByProfile.values())
		{
			dataService.save(permission.getRecord(), AuthUtil.getAuthData(session), env);
		}
		
		env.setLastFieldPermissionsUpdate((new Date()).getTime());
		
		return new ModelAndView("redirect:/" + UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/fieldpermissions/" + field.getKID());
	}
	
	class CandidateLinkingType
	{
		private Type type;
		private List<Field> selfLinkingFields;
		private List<Field> foreignLinkingFields;
		
		public CandidateLinkingType (Type type)
		{
			this.type = type;
			this.selfLinkingFields = new ArrayList<Field>();
			this.foreignLinkingFields = new ArrayList<Field>();
		}

		public String toJSON()
		{
			StringBuilder json = new StringBuilder("{ ");
			
			json.append(JSON.getPropertyJSON("typeId", this.type.getKID().getId())).append(", ");
			json.append(JSON.getPropertyJSON("typeName", this.type.getLabel())).append(", ");
			
			json.append("\"selfLinkingFields\": [");
			List<String> fields = new ArrayList<String>();
			
			for (Field field : this.selfLinkingFields)
			{
				String fieldJSON = " { ";
				fieldJSON += JSON.getPropertyJSON("name", field.getLabel()) + ", ";
				fieldJSON += JSON.getPropertyJSON("id", field.getKID().getId());
				fieldJSON += " }";
				fields.add(fieldJSON);
			}
			
			json.append(MiscUtils.implode(fields, ", "));
			json.append("], ");
			
			json.append("\"foreignLinkingFields\": [");
			fields = new ArrayList<String>();
			
			for (Field field : this.foreignLinkingFields)
			{
				String fieldJSON = " { ";
				fieldJSON += JSON.getPropertyJSON("name", field.getLabel()) + ", ";
				fieldJSON += JSON.getPropertyJSON("id", field.getKID().getId());
				fieldJSON += " }";
				fields.add(fieldJSON);
			}
			
			json.append(MiscUtils.implode(fields, ", "));
			json.append("] ");
			
			json.append(" }");
			return json.toString();
		}

		public List<Field> getForeignLinkingFields()
		{
			return foreignLinkingFields;
		}

		public List<Field> getSelfLinkingFields()
		{
			return selfLinkingFields;
		}

		public Type getType()
		{
			return type;
		}

		public void setType(Type type)
		{
			this.type = type;
		}
	}
}