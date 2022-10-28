/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import kommet.auth.AuthUtil;
import kommet.basic.keetle.tags.objectlist.ListColumn;
import kommet.basic.keetle.tags.objectlist.ObjectListConfig;
import kommet.basic.keetle.tags.objectlist.ObjectListItemType;
import kommet.basic.keetle.tags.objectlist.ObjectListSource;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Type;
import kommet.data.datatypes.DataType;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.koll.compiler.KommetCompiler;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;
import kommet.web.rmparams.KmParamUtils;

@Controller
public class ObjectListController extends CommonKommetController
{
	@Inject
	EnvService envService;
	
	@Inject
	DataService dataService;
	
	@Inject
	KommetCompiler compiler;
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/reloadobjectlist", method = RequestMethod.POST)
	public ModelAndView listByPost(@RequestBody Object params, @RequestParam("mode") String mode, HttpSession session, HttpServletRequest req) throws KommetException
	{
		if (!(params instanceof LinkedHashMap<?, ?>))
		{
			throw new KommetException("Expected first parameter to be a linked hash map created from deserialized JSON object list config");
		}
		
		EnvData env = envService.getCurrentEnv(session);
		
		// deserialize map into object list config object
		ObjectListConfig config = ObjectListConfig.deserializeMap((LinkedHashMap<?,?>)params, compiler, env);
		
		// call the actual method that displays the list
		return list(config, mode, (LinkedHashMap<?, ?>)params, req, env);
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/objectlist", method = RequestMethod.GET)
	public ModelAndView listByGet(@RequestParam(value = "pageNo", required = false) Integer pageNo,
								@RequestParam(value = "pageSize", required = false) Integer pageSize,
								@RequestParam(value = "sortBy", required = false) String sortBy,
								@RequestParam(value = "type", required = false) String userDefinedTypeName,
								@RequestParam(value = "fields", required = false) String fields,
								@RequestParam(value = "sortableFields", required = false) String sortableFields,
								@RequestParam(value = "linkFields", required = false) String linkFields,
								@RequestParam(value = "dalFilter", required = false) String dalFilter,
								@RequestParam(value = "lookupId", required = false) String lookupId,
								@RequestParam(value = "mode", required = false) String mode,
								@RequestParam(value = "title", required = false) String title,
								HttpSession session,
								HttpServletRequest req) throws KommetException
	{
		if (!StringUtils.hasText(userDefinedTypeName))
		{
			addError("Type not specified in objectList tag");
		}
		
		if (!StringUtils.hasText(fields))
		{
			addError("Fields to display not specified in objectList tag");
		}
		
		EnvData env = envService.getCurrentEnv(session);
		
		ObjectListConfig config = new ObjectListConfig(ObjectListSource.QUERY, ObjectListItemType.RECORD);
		config.setPageNo(pageNo != null ? pageNo : 1);
		config.setPageSize(pageSize != null ? pageSize : 15);
		config.setSortBy(sortBy);
		config.setType(env.getType(userDefinedTypeName));
		config.setDalFilter(dalFilter);
		config.setTitle(title);
		config.setEnv(env);
		config.setLookupId(lookupId);
		
		// init RM params
		config.setRmParams(KmParamUtils.getRmParamsFromRequest(req));
		
		Set<String> sortableFieldSet = getFieldsFromParam(sortableFields);
		Set<String> linkFieldSet = getFieldsFromParam(linkFields);
		
		// turn fields into columns
		for (String field : fields.split(","))
		{
			ListColumn col = new ListColumn();
			col.setField(field);
			col.setLink(linkFieldSet.contains(field));
			col.setSortable(sortableFieldSet.contains(field));
			col.setLabel(config.getType().getField(field).getInterpretedLabel(AuthUtil.getAuthData(session)));
			
			config.addColumn(col);
		}
		
		ModelAndView mv = new ModelAndView("objectList/list");
		
		if (hasErrorMessages())
		{
			mv.addObject("errorMsgs", getErrorMsgs());
			return mv;
		}
		else
		{
			return list(config, mode, null, req, env);
		}
	}

	public ModelAndView list(ObjectListConfig config, String mode, LinkedHashMap<?, ?> params, HttpServletRequest req, EnvData env) throws KommetException
	{
		clearMessages();
		config.applyDefaults();
		
		List<String> conditions = new ArrayList<String>();
		if (StringUtils.hasText(config.getDalFilter()))
		{
			// enclose filter in brackets because it can contain OR conditions
			conditions.add("(" + config.getDalFilter() + ")");
		}
		
		// if object list was submitted in search mode, find filter fields and
		// include them in query criteria
		if ("search".equals(mode))
		{
			if (params == null)
			{
				throw new KommetException("Parameters serialized as JSON need to be passed in the request when object list is run in search mode");
			}
			
			// in search mode, the first page will always be displayed by default
			config.setPageNo(1);
			String searchCondition = getSearchCondition(config.getType(), params);
			if (StringUtils.hasText(searchCondition))
			{
				// we need to check if condition is not empty, because otherwise we would just add empty parenthesis
				// "()" as condition, which would cause an incorrect DAL query error
				conditions.add("(" + searchCondition + ")");
			}
		}
		
		config.setDalFilter(MiscUtils.implode(conditions, " AND "));
		
		ModelAndView mv = new ModelAndView("objectList/list");
		mv.addObject("config", config);
		mv.addObject("type", config.getType().getQualifiedName());
		addRmParams(mv, req, env);
		return mv;
	}
	
	/**
	 * Searches for listFilter fields passed in the request (recognized by the "filter:" prefix) and
	 * builds a DAL condition from them.
	 * 
	 * Note: field API names are not checked if they represent correct type fields. This will be verified
	 * when the prepared query is run when the list is constructed.
	 * 
	 * @param req
	 * @return
	 * @throws KommetException 
	 */
	private static String getSearchCondition(Type type, LinkedHashMap<?, ?> params) throws KommetException
	{
		List<String> conditions = new ArrayList<String>();
		
		for (Object param : params.keySet())
		{
			if (((String)param).startsWith("filter:"))
			{
				// param name has form "filter:{param-id}", so we need to extract the ID after the colon
				String filterFieldId = ((String)param).split(":")[1];
				
				// In most cases the parameter representing the field will have name = {param-id}.
				// However, if the field is an type reference, the lookup tag that renders the hidden input for
				// the pop-up list renders the field name with id appended, i.e. the parameter name will be {param-id}.id
				// This is why we need to check for both {param-id} and {param-id}.id.
				String filterFieldValue = (String)params.get(filterFieldId);
				boolean isObjectReference = false;
				
				if (!StringUtils.hasText(filterFieldValue))
				{
					// if parameter with name {param-id} not found, look for parameters with name {param-id}.id
					filterFieldValue = (String)params.get(filterFieldId + "." + Field.ID_FIELD_NAME);
					if (!StringUtils.hasText(filterFieldValue))
					{
						// if search field is not filled, do not add a criterion for it
						continue;
					}
					else
					{
						isObjectReference = true;
					}
				}
				
				// filter spec has the form {qualified_field_name}:{comparison_type}
				String filterSpec = (String)params.get(param);
				String[] filterSpecBits = filterSpec.split(":");
				String fieldApiName = filterSpecBits[0];
				String comparisonType = filterSpecBits[1];
				
				// if parameter had name = {param-id}.id, make sure the search field is an type reference,
				// because only type reference fields can have parameter names different from {param-id}
				if (isObjectReference && !type.getField(fieldApiName).getDataTypeId().equals(DataType.TYPE_REFERENCE))
				{
					throw new KommetException("Illegal parameter name " + filterFieldId + "." + Field.ID_FIELD_NAME + " - only type references can have ID field appended to search parameter name");
				}
				
				conditions.add(getDALConditionFromComparisonType (type, fieldApiName, comparisonType, filterFieldValue, isObjectReference));
			}
		}
		
		return MiscUtils.implode(conditions, " AND ");
	}
	
	/*private static String getSearchCondition(Type type, HttpServletRequest req) throws KommetException
	{
		List<String> conditions = new ArrayList<String>();
		
		for (Object param : req.getParameterMap().keySet())
		{
			if (((String)param).startsWith("filter:"))
			{
				// param name has form "filter:{param-id}", so we need to extract the ID after the colon
				String filterFieldId = ((String)param).split(":")[1];
				
				// In most cases the parameter representing the field will have name = {param-id}.
				// However, if the field is an type reference, the lookup tag that renders the hidden input for
				// the pop-up list renders the field name with id appended, i.e. the parameter name will be {param-id}.id
				// This is why we need to check for both {param-id} and {param-id}.id.
				String filterFieldValue = req.getParameter(filterFieldId);
				boolean isObjectReference = false;
				
				if (!StringUtils.hasText(filterFieldValue))
				{
					// if parameter with name {param-id} not found, look for parameters with name {param-id}.id
					filterFieldValue = req.getParameter(filterFieldId + "." + Field.ID_FIELD_NAME);
					if (!StringUtils.hasText(filterFieldValue))
					{
						// if search field is not filled, do not add a criterion for it
						continue;
					}
					else
					{
						isObjectReference = true;
					}
				}
				
				// filter spec has the form {qualified_field_name}:{comparison_type}
				String filterSpec = req.getParameter(((String)param));
				String[] filterSpecBits = filterSpec.split(":");
				String fieldApiName = filterSpecBits[0];
				String comparisonType = filterSpecBits[1];
				
				// if parameter had name = {param-id}.id, make sure the search field is an type reference,
				// because only type reference fields can have parameter names different from {param-id}
				if (isObjectReference && !type.getField(fieldApiName).getDataTypeId().equals(DataType.OBJECT_REFERENCE))
				{
					throw new KommetException("Illegal parameter name " + filterFieldId + "." + Field.ID_FIELD_NAME + " - only type references can have ID field appended to search parameter name");
				}
				
				conditions.add(getDALConditionFromComparisonType (type, fieldApiName, comparisonType, filterFieldValue, isObjectReference));
			}
		}
		
		return MiscUtils.implode(conditions, " AND ");
	}*/

	private static String getDALConditionFromComparisonType(Type type, String fieldApiName, String comparisonType, String value, boolean isObjectReference) throws KommetException
	{	
		if ("equals".equals(comparisonType))
		{
			return fieldApiName + (isObjectReference ? ("." + Field.ID_FIELD_NAME) : "") + " = '" + value + "'";
		}
		else if ("contains".equals(comparisonType))
		{
			return fieldApiName + " ILIKE '%" + value + "%'";
		}
		else if ("startsWith".equals(comparisonType))
		{
			return fieldApiName + " ILIKE '" + value + "%'";
		}
		else if ("gt".equals(comparisonType))
		{
			if (type.getField(fieldApiName).getDataTypeId().equals(DataType.NUMBER))
			{
				return fieldApiName + " > " + value;
			}
			else
			{
				return fieldApiName + " > '" + value + "'";
			}
		}
		else if ("lt".equals(comparisonType))
		{
			if (type.getField(fieldApiName).getDataTypeId().equals(DataType.NUMBER))
			{
				return fieldApiName + " < " + value;
			}
			else
			{
				return fieldApiName + " < '" + value + "'";
			}
		}
		else
		{
			throw new KommetException("Unrecognized comparison type " + comparisonType + " in list filter field tag");
		}
	}

	private Set<String> getFieldsFromParam(String sortableFields)
	{
		Set<String> sortableFieldSet = new HashSet<String>();
		
		if (sortableFields != null)
		{
			for (String field : MiscUtils.splitAndTrim(sortableFields, ","))
			{
				sortableFieldSet.add(field);
			}
		}
		
		return sortableFieldSet;
	}

	public class ListField
	{
		private String name;
		private Boolean sortable;
		private Boolean link;
		
		public ListField (String name, boolean sortable, boolean link)
		{
			this.name = name;
			this.sortable = sortable;
			this.link = link;
		}
		
		public void setName(String name)
		{
			this.name = name;
		}
		public String getName()
		{
			return name;
		}
		public void setSortable(Boolean sortable)
		{
			this.sortable = sortable;
		}
		public Boolean getSortable()
		{
			return sortable;
		}

		public void setLink(Boolean link)
		{
			this.link = link;
		}

		public Boolean getLink()
		{
			return link;
		}
	}
}