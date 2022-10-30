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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.codehaus.jackson.map.ObjectMapper;
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
import kommet.basic.ReportType;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.InvalidResultSetAccess;
import kommet.dao.queries.QueryResult;
import kommet.dao.queries.jcr.Grouping;
import kommet.dao.queries.jcr.JCR;
import kommet.dao.queries.jcr.JCRUtil;
import kommet.dao.queries.jcr.Property;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KIDException;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.AssociationDataType;
import kommet.data.datatypes.DataType;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.TypeReference;
import kommet.data.sharing.SharingService;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.i18n.I18nDictionary;
import kommet.i18n.I18nUtils;
import kommet.json.JSON;
import kommet.reports.ReportService;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;

@Controller
public class ReportTypeController extends CommonKommetController
{
	@Inject
	EnvService envService;

	@Inject
	ReportService reportService;
	
	@Inject
	SharingService sharingService;

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/reporttypes/new", method = RequestMethod.GET)
	public ModelAndView create(HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("reporttypes/edit");
		EnvData env = envService.getCurrentEnv(session);

		List<Type> types = new ArrayList<Type>();
		types.addAll(env.getAccessibleTypes());
		Collections.sort(types, new TypeNameComparator());
		mv.addObject("types", types);
		
		// report edit view JSP needs an empty JCR object to work correctly
		mv.addObject("jcr", "{}");
		
		mv.addObject("i18n", AuthUtil.getAuthData(session).getI18n());
		return mv;
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/reporttypes/list", method = RequestMethod.GET)
	public ModelAndView list(HttpSession session) throws KommetException
	{
		ModelAndView mv = new ModelAndView("reporttypes/list");
		mv.addObject("i18n", AuthUtil.getAuthData(session).getI18n());
		return mv;
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/reporttypes/{id}", method = RequestMethod.GET)
	public ModelAndView details(@PathVariable("id") String sReportTypeId, HttpSession session)throws KommetException
	{
		AuthData authData = AuthUtil.getAuthData(session);

		ModelAndView mv = new ModelAndView("reporttypes/details");
		mv = prepareReportTypeDetails(sReportTypeId, mv, session, authData);
		mv.addObject("i18n", authData.getI18n());
		return mv;
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/reporttypes/edit/{id}", method = RequestMethod.GET)
	public ModelAndView edit(@PathVariable("id") String sReportTypeId, HttpSession session) throws KommetException
	{
		AuthData authData = AuthUtil.getAuthData(session);

		ModelAndView mv = new ModelAndView("reporttypes/edit");
		mv = prepareReportTypeDetails(sReportTypeId, mv, session, authData);
		mv.addObject("i18n", authData.getI18n());
		mv.addObject("jcr", ((ReportType)mv.getModel().get("reportType")).getSerializedQuery());
		return mv;
	}

	/**
	 * Runs a report a prepares data for display
	 * @param sReportTypeId
	 * @param session
	 * @return
	 * @throws KommetException
	 */
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/reporttypes/run/{id}", method = RequestMethod.GET)
	public ModelAndView runReport(@PathVariable("id") String sReportTypeId, HttpSession session) throws KommetException
	{
		KID reportTypeId = null;
		AuthData authData = AuthUtil.getAuthData(session);

		try
		{
			reportTypeId = KID.get(sReportTypeId);
		}
		catch (KIDException e)
		{
			return getErrorPage(authData.getI18n().get("kolmu.rid.invalid") + ": " + sReportTypeId);
		}

		EnvData env = envService.getCurrentEnv(session);

		// get report type by ID
		ReportType rt = reportService.getReportType(reportTypeId, env);
		if (rt == null)
		{
			return getErrorPage(authData.getI18n().get("reports.reporttype.with.id.not.exists"));
		}

		JCR c = JCRUtil.deserialize(rt.getSerializedQuery());

		String dalQuery = c.getQuery(env);

		Type type = env.getType(c.getBaseTypeId());

		// create a list of select properties
		LinkedHashMap<String, String> displayedColumns = new LinkedHashMap<String, String>();
		for (Property prop : c.getProperties())
		{
			Field field = type.getField(JCRUtil.getPropertyName(prop, type, env));
			String qualifiedFieldName = JCRUtil.getPropertyName(prop, type, env);
			
			String columnLabel = null;
			if (StringUtils.hasText(prop.getAlias()))
			{
				// use alias defined by the user while the report was created
				columnLabel = prop.getAlias();
			}
			else if (Field.isSystemField(field.getApiName()))
			{
				// translate system field names
				columnLabel = I18nUtils.getSystemFieldLabel(field.getApiName(), authData.getI18n());
			}
			else
			{
				// use standard field label
				columnLabel = field.getLabel();
			}
			
			if (prop.getAggregateFunction() != null)
			{
				displayedColumns.put(prop.getAggregateFunction() + "(" + qualifiedFieldName + ")", columnLabel);
			}
			else
			{
				displayedColumns.put(qualifiedFieldName, columnLabel);
			}
		}

		// create a list of groupings
		LinkedHashMap<String, String> groupings = new LinkedHashMap<String, String>();
		Map<String, String> groupingPropertyNames = new HashMap<String, String>();
		//Map<PIR, String> groupingPirsToPropertyNames = new HashMap<PIR, String>();
		List<String> additionalGroupings = new ArrayList<String>();

		for (Grouping grouping : c.getGroupings())
		{
			String nestedProperty = JCRUtil.pirToNestedProperty(grouping.getPropertyId(), type, env);
			Field field = type.getField(nestedProperty);

			// map the property to the column name
			// use either alias of default field label as column name
			String columnLabel = null;
			if (StringUtils.hasText(grouping.getAlias()))
			{
				// use alias defined by the user while the report was created
				columnLabel = grouping.getAlias();
			}
			else if (Field.isSystemField(field.getApiName()))
			{
				// translate system field names
				columnLabel = I18nUtils.getSystemFieldLabel(field.getApiName(), authData.getI18n());
			}
			else
			{
				// use standard field label
				columnLabel = field.getLabel();
			}
			
			groupings.put(nestedProperty, columnLabel);

			String selectedProperty = new String(nestedProperty);

			if (field.getDataTypeId().equals(DataType.TYPE_REFERENCE) && !nestedProperty.endsWith("." + Field.ID_FIELD_NAME))
			{
				selectedProperty += "." + ((TypeReference) field.getDataType()).getType().getDefaultFieldApiName();
				additionalGroupings.add(selectedProperty);
			}

			// always query fields used in groupings - we will need it to be displayed in the report
			dalQuery = dalQuery.replaceAll("SELECT ", "SELECT " + selectedProperty + ",");
			groupingPropertyNames.put(nestedProperty, selectedProperty);
			//groupingPirsToPropertyNames.put(grouping.getPropertyId(), selectedProperty);
		}

		// prepare criteria from DAL
		Criteria dalCriteria = env.getSelectCriteriaFromDAL(dalQuery);
		if (!additionalGroupings.isEmpty())
		{
			for (String grouping : additionalGroupings)
			{
				dalCriteria.addGroupByProperty(grouping);
			}
		}
		
		// retrieve and wrap results
		List<ReportResult> results = wrapResults(dalCriteria.list());
		
		//List<LinkedHashMap<String, String>> jsonResults = new ArrayList<LinkedHashMap<String,String>>();
		
		/*for (ReportResult result : results)
		{
			LinkedHashMap<String, String> jsonResult = new LinkedHashMap<String, String>();
			
			// first add grouping values
			if (c.getGroupings() != null && !c.getGroupings().isEmpty())
			{
				for (Grouping group : c.getGroupings())
				{
					jsonResult.put(group.getPropertyId(), result.getValue(groupingPirsToPropertyNames.get(group.getPropertyId())));
				}
			}
		}*/

		ModelAndView mv = new ModelAndView("reporttypes/run");
		mv.addObject("displayedColumns", displayedColumns);
		mv.addObject("groupings", groupings);
		mv.addObject("groupingsToSelectedProps", groupingPropertyNames);
		mv.addObject("records", results);
		mv.addObject("reportName", rt.getName());
		mv.addObject("reportTypeId", rt.getId());
		mv.addObject("i18n", authData.getI18n());
		mv.addObject("criteria", c);
		mv.addObject("canEdit", AuthUtil.canEditRecord(rt.getId(), authData, sharingService, env));
		return mv;
	}

	private List<ReportResult> wrapResults(List<Record> records)
	{
		List<ReportResult> results = new ArrayList<ReportResult>();
		for (Record r : records)
		{
			results.add(new ReportResult(r));
		}
		return results;
	}

	private ModelAndView prepareReportTypeDetails(String sId, ModelAndView mv, HttpSession session, AuthData authData) throws KommetException
	{
		KID reportTypeId = null;

		try
		{
			reportTypeId = KID.get(sId);
		}
		catch (KIDException e)
		{
			return getErrorPage(authData.getI18n().get("kolmu.rid.invalid") + ": " + sId);
		}

		EnvData env = envService.getCurrentEnv(session);

		ReportType rt = reportService.getReportType(reportTypeId, env);
		if (rt == null)
		{
			return getErrorPage(authData.getI18n().get("reports.reporttype.with.id.not.exists"));
		}

		mv.addObject("reportType", rt);
		mv.addObject("reportTypeId", rt.getId());
		mv.addObject("reportTypeName", rt.getName());
		mv.addObject("reportTypeDesc", rt.getDescription());
		return mv;
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/reporttypes/list/data", method = RequestMethod.GET)
	@ResponseBody
	public void getReportTypes(HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		AuthData authData = AuthUtil.getAuthData(session);
		EnvData env = envService.getCurrentEnv(session);
		List<ReportType> reportTypes = reportService.getReportTypes(null, authData, env);
		resp.getWriter().write(getSuccessDataJSON(JSON.serializeObjectProxies(reportTypes, MiscUtils.toSet("id", "name", "createdDate"), authData)));
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/reporttypes/delete", method = RequestMethod.POST)
	@ResponseBody
	public void delete(@RequestParam("id") String sReportTypeId, HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		EnvData env = envService.getCurrentEnv(session);
		reportService.deleteReportType(KID.get(sReportTypeId), AuthUtil.getAuthData(session), env);
		PrintWriter out = resp.getWriter();
		out.write(getSuccessJSON("Deleted"));
	}

	/*@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/reporttypes/typefields", method = RequestMethod.GET)
	@ResponseBody
	public void getTypeFields(@RequestParam("typeId") String sTypeId, HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		EnvData env = envService.getCurrentEnv(session);

		List<String> fieldJSON = new ArrayList<String>();
		List<Field> fields = new ArrayList<Field>();
		fields.addAll(env.getType(KID.get(sTypeId)).getFields());
		Collections.sort(fields, new FieldNameComparator());

		for (Field field : fields)
		{
			StringBuilder fieldProps = new StringBuilder();
			fieldProps.append("{ \"id\": \"").append(field.getKID()).append("\", \"label\": \"");
			fieldProps.append(field.getLabel()).append("\", \"dataType\": \"");
			fieldProps.append(field.getDataType().getName()).append("\"");
			fieldProps.append(" }");
			fieldJSON.add(fieldProps.toString());
		}

		PrintWriter out = resp.getWriter();
		out.write(getSuccessDataJSON("[ " + MiscUtils.implode(fieldJSON, ", ") + " ]"));
	}*/

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/reporttypes/types/data", method = RequestMethod.GET)
	@ResponseBody
	public void getTypes(HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{
		EnvData env = envService.getCurrentEnv(session);

		List<Type> types = new ArrayList<Type>();
		types.addAll(env.getAccessibleTypes());
		Collections.sort(types, new TypeNameComparator());
	
		LinkedHashMap<String, SerializableType> serializableTypes = new LinkedHashMap<String, SerializableType>();
		
		for (Type type : types)
		{
			serializableTypes.put(type.getKeyPrefix().getPrefix(), new SerializableType(type, AuthUtil.getAuthData(session), env));
		}

		PrintWriter out = resp.getWriter();
		out.write(getSuccessDataJSON((new ObjectMapper()).writeValueAsString(serializableTypes)));
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/reporttypes/save", method = RequestMethod.POST)
	@ResponseBody
	public void saveReport(@RequestParam(value = "name", required = false) String name,
			@RequestParam(value = "desc", required = false) String desc,
			@RequestParam(value = "reportTypeId", required = false) String sReportTypeId,
			@RequestParam(value = "serializedCriteria", required = false) String serializedCriteria,
			HttpSession session, HttpServletResponse resp)
			throws KommetException, IOException
	{
		EnvData env = envService.getCurrentEnv(session);

		JCR c = JCRUtil.deserialize(serializedCriteria);
		PrintWriter out = resp.getWriter();
		AuthData authData = AuthUtil.getAuthData(session);
		
		if ((c.getProperties() == null || c.getProperties().isEmpty()) && (c.getGroupings() == null || c.getGroupings().isEmpty()))
		{
			out.write(getErrorJSON(authData.getI18n().get("reports.no.propeerties.selected")));
			return;
		}
		
		List<String> validationErrorKeys = JCRUtil.validate(c, env);
		if (!validationErrorKeys.isEmpty())
		{
			StringBuilder sb = new StringBuilder();
			for (String errKey : validationErrorKeys)
			{
				sb.append(authData.getI18n().get(errKey)).append("\n");
			}
			out.write(getErrorJSON(sb.toString()));
			return;
		}

		ReportType rt = null;

		if (StringUtils.hasText(sReportTypeId))
		{
			KID reportTypeId = KID.get(sReportTypeId);
			rt = reportService.getReportType(reportTypeId, env);
		}
		else
		{
			rt = new ReportType();
		}
		
		if (!StringUtils.hasText(name))
		{
			out.write(getErrorJSON(authData.getI18n().get("reports.reporttypename.empty")));
			return;
		}
		
		if (c.getBaseTypeId() == null)
		{
			out.write(getErrorJSON(authData.getI18n().get("reports.reporttype.basetypenotselected")));
			return;
		}

		rt.setBaseTypeId(c.getBaseTypeId());
		rt.setName(name);
		rt.setDescription(StringUtils.hasText(desc) ? desc : null);
		rt.setSerializedQuery(serializedCriteria);

		try
		{
			reportService.save(rt, authData, env);
		}
		catch (Exception e)
		{
			out.write(getErrorJSON(e.getMessage()));
			return;
		}

		out.write(getSuccessDataJSON("{ \"reportTypeId\": \"" + rt.getId() + "\" }"));
	}

	public class TypeNameComparator implements Comparator<Type>
	{
		@Override
		public int compare(Type o1, Type o2)
		{
			return o1.getLabel().compareTo(o2.getLabel());
		}
	}

	/**
	 * Compare fields by their data types and names.
	 * Non-primitive fields are always sorted after those with primitive values.
	 */
	public class FieldAndDataTypeNameComparator implements Comparator<Field>
	{
		@Override
		public int compare(Field o1, Field o2)
		{
			if (o1.getDataType().isPrimitive() && !o2.getDataType().isPrimitive())
			{
				return -1;
			}
			else if (!o1.getDataType().isPrimitive() && o2.getDataType().isPrimitive())
			{
				return 1;
			}
			else
			{
				return o1.getLabel().compareTo(o2.getLabel());
			}
		}
	}

	public class SerializableType
	{
		private String apiName;
		private String label;
		private List<SerializableField> fields;
		private String id;

		public SerializableType (Type type, AuthData authData, EnvData env) throws KommetException
		{
			this.apiName = type.getApiName();
			this.label = type.getLabel();
			this.fields = new ArrayList<SerializableField>();
			this.id = type.getKID().getId();
			
			List<Field> displayedFields = new ArrayList<Field>();
			displayedFields.addAll(type.getFields());
			Collections.sort(displayedFields, new FieldAndDataTypeNameComparator());
			
			for (Field field : displayedFields)
			{
				// while creating report types, users can only use field they have read permissions for
				if (authData.canReadField(field, false, env))
				{
					fields.add(new SerializableField(field, authData.getI18n()));
				}
			}
		}

		public String getApiName()
		{
			return apiName;
		}
		
		public String getLabel()
		{
			return label;
		}

		public List<SerializableField> getFields()
		{
			return fields;
		}
		
		public void addField (SerializableField field)
		{
			if (this.fields == null)
			{
				this.fields = new ArrayList<SerializableField>();
			}
			this.fields.add(field);
		}

		public String getId()
		{
			return id;
		}
	}
	
	public class SerializableField
	{
		private String apiName;
		private String label;
		private String typePrefix;
		private String dataType;
		private String id;

		public SerializableField(Field field, I18nDictionary i18n)
		{
			this.apiName = field.getApiName();
			// display internationalized labels for system fields
			this.label = Field.isSystemField(field.getApiName()) ? I18nUtils.getSystemFieldLabel(field.getApiName(), i18n) : field.getLabel();
			this.dataType = field.getDataType().getName();
			this.id = field.getKID().getId();
			
			if (field.getDataTypeId().equals(DataType.TYPE_REFERENCE))
			{
				this.typePrefix = ((TypeReference)field.getDataType()).getType().getKeyPrefix().getPrefix();
			}
			else if (field.getDataTypeId().equals(DataType.INVERSE_COLLECTION))
			{
				this.typePrefix = ((InverseCollectionDataType)field.getDataType()).getInverseType().getKeyPrefix().getPrefix();
			}
			else if (field.getDataTypeId().equals(DataType.ASSOCIATION))
			{
				this.typePrefix = ((AssociationDataType)field.getDataType()).getAssociatedType().getKeyPrefix().getPrefix();;
			}
		}

		public String getApiName()
		{
			return apiName;
		}

		public String getLabel()
		{
			return label;
		}

		public String getTypePrefix()
		{
			return typePrefix;
		}

		public String getDataType()
		{
			return dataType;
		}

		public String getId()
		{
			return id;
		}
	}
	
	/**
	 * Wrapper for report results. The sole reason why we need this wrapper is the method getValue(),
	 * which retrieves the values of a field or aggregate function. We need this because we don't want
	 * the JSP view to be responsible for checking whether a given field is a regular field name
	 * or an aggregate function call.
	 * @author Radek Krawiec
	 * @created 7-09-2014
	 */
	public class ReportResult
	{
		private Record record;
		
		public ReportResult(Record rec)
		{
			this.record = rec;
		}
		
		public Object getField(String field) throws KommetException
		{
			return this.record.getField(field);
		}
		
		public Object getAggregateValue (String value) throws InvalidResultSetAccess
		{
			return ((QueryResult)this.record).getAggregateValue(value);
		}
		
		public Object getValue (String value) throws KommetException
		{
			if (this.record.isSet(value))
			{
				return this.record.getField(value);
			}
			else
			{
				return ((QueryResult)this.record).getAggregateValue(value);
			}
		}
		
		/*public Object getStringValue (String value) throws KommetException
		{
			if (this.record.isSet(value))
			{
				return this.record.getFieldStringValue(value);
			}
			else
			{
				// TODO using toString here is not the right way
				return ((QueryResult)this.record).getAggregateValue(value).toString();
			}
		}*/
	}
}