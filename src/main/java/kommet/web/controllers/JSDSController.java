/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.basic.MimeTypes;
import kommet.dao.dal.AggregateFunction;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.jcr.JCR;
import kommet.dao.queries.jcr.JCRUtil;
import kommet.dao.queries.jcr.Property;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.PIR;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.TypeFilter;
import kommet.env.EnvData;
import kommet.errorlog.ErrorLogService;
import kommet.errorlog.ErrorLogSeverity;
import kommet.js.jsrc.JSRC;
import kommet.js.jsrc.JSRUtil;
import kommet.js.jsti.JSTI;
import kommet.utils.MiscUtils;
import kommet.utils.UrlUtil;
import kommet.vendorapis.msoffice.excel.ExcelFormat;
import kommet.vendorapis.msoffice.excel.MsExcelApi;
import kommet.vendorapis.msoffice.excel.RecordListCreator;
import kommet.vendorapis.msoffice.excel.Sheet;
import kommet.vendorapis.msoffice.excel.Workbook;

/**
 * Controller exposing REST services for JSDS.
 * @author Radek Krawiec
 * @created 11-09-2014
 */
@Controller
public class JSDSController extends BasicRestController
{	
	@Inject
	ErrorLogService logService;
	
	@Inject
	DataService dataService;
	
	private final static int DEFAULT_NESTING_LEVEL_DEPTH = 10;
	/**
	 * Query records according to the passed JCR and return them in JSRC format.
	 * @param serializedJCR Serialized JCR criteria
	 * @param mode Defines a mode in which this action is invoked. This parameter is not obligatory. If not set,
	 * the action will return standard JSRC data. If set to "datasource", it will return a JSON object
	 * that contains more information in addition to JSRC.
	 * @param envId
	 * @param accessToken
	 * @param session
	 * @param resp
	 * @throws KommetException
	 * @throws IOException
	 */
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_QUERY_DS_URL, method = RequestMethod.POST)
	@ResponseBody
	public void query (@RequestParam(value = "jcr", required = false) String serializedJCR,
						@RequestParam(value = "mode", required = false) String mode,
						@RequestParam(value = "env", required = false) String envId,
						@RequestParam(value = "access_token", required = false) String accessToken,
						@RequestParam(value = "format", required = false) String format,
						@RequestParam(value = "exportFileName", required = false) String exportFileName,
						@RequestParam(value = "query", required = false) String query,
						HttpSession session, HttpServletResponse resp) throws KommetException, IOException
	{	
		RestInitInfo restInfo = prepareRest(envId, accessToken, session, resp, false);
		PrintWriter out = null;
		
		if (!restInfo.isSuccess())
		{
			out = resp.getWriter();
			if (restInfo.getOut() != null)
			{
				returnRestError(restInfo.getError(), out);
				resp.setStatus(restInfo.getRespCode());
				return;
			}
			else
			{
				throw new KommetException(restInfo.getError());
			}
		}
		
		restInfo.getEnv().addAuthData(restInfo.getAuthData());
		
		if (StringUtils.hasText(serializedJCR) && StringUtils.hasText(query))
		{
			returnRestError("Cannot pass both JCR and DAL query to query method", resp.getWriter());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			restInfo.getEnv().clearAuthData();
			return;
		}
		else if (!StringUtils.hasText(serializedJCR) && !StringUtils.hasText(query))
		{
			returnRestError("Neither JCR nor DAL query to query method", resp.getWriter());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			restInfo.getEnv().clearAuthData();
			return;
		} 
		
		String dalQuery = null;
		Type type = null;
		JCR jcr = null;
		
		if (StringUtils.hasText(serializedJCR))
		{
			try
			{
				jcr = JCRUtil.deserialize(serializedJCR);
			}
			catch (Exception e)
			{
				restInfo.getEnv().clearAuthData();
				logService.logException(e, ErrorLogSeverity.FATAL, this.getClass().getName(), -1, restInfo.getAuthData().getUserId(), restInfo.getEnv());
				returnRestError("Error parsing JCR: " + e.getMessage(), resp.getWriter());
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			
			try
			{
				// build query from JCR
				dalQuery = jcr.getQuery(restInfo.getEnv());
			}
			catch (Exception e)
			{
				restInfo.getEnv().clearAuthData();
				e.printStackTrace();
				returnRestError("Error creating DAL query from JCR: " + e.getMessage(), resp.getWriter());
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			
			try
			{
				type = JCRUtil.getBaseType(jcr, restInfo.getEnv());
			}
			catch (KommetException e)
			{
				restInfo.getEnv().clearAuthData();
				returnRestError(e.getMessage(), resp.getWriter());
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			catch (Exception e)
			{
				restInfo.getEnv().clearAuthData();
				returnRestError("Error getting type from JCR: " + e.getMessage(), resp.getWriter());
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
		}
		else
		{
			dalQuery = query;
		}
		
		// prepare criteria from DAL
		AuthData authData = AuthUtil.getAuthData(session);
		Criteria dalCriteria = null;
		
		try
		{
			dalCriteria = restInfo.getEnv().getSelectCriteriaFromDAL(dalQuery, authData);
		}
		catch (Exception e)
		{
			restInfo.getEnv().clearAuthData();
			returnRestError("Error creating criteria from DAL: " + dalQuery + ". Error message: "+ e.getMessage(), resp.getWriter());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		if (!StringUtils.hasText(serializedJCR))
		{
			jcr = JCRUtil.getJCRFromDALCriteria(dalCriteria, restInfo.getEnv());
			type = dalCriteria.getType();
		}
		
		List<Record> records = null;
		
		try
		{
			// execute query
			records = dalCriteria.list();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			restInfo.getEnv().clearAuthData();
			returnRestError("Error running query: " + e.getMessage(), out);
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		// set default format to JSON, if not passed explicitly in the call
		if (format == null)
		{
			format = "json";
		}
		else
		{
			format = format.toLowerCase();
		}
		
		if ("json".equals(format))
		{
			JSRC jsrc = JSRC.build(records, type, 2, restInfo.getEnv(), restInfo.getAuthData());
			out = resp.getWriter();
			resp.setContentType("text/json; charset=UTF-8");
			
			String serializedJSRC = null;
	
			try
			{
				// convert JSRC to JSON and return
				serializedJSRC = JSRC.serialize(jsrc, restInfo.getAuthData());
			}
			catch (Exception e)
			{
				restInfo.getEnv().clearAuthData();
				returnRestError("Error serializing records to JSRC: " + e.getMessage(), out);
				resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return;
			}
			
			if (!"datasource".equals(mode))
			{
				out.write(serializedJSRC);
			}
			else
			{	
				// in addition  to JSRC data, also find out the total size of the collection
				// for the given query so that this information can be used by a km.js.table
				try
				{
					Long recordCount = getRecordCount(jcr, type, restInfo.getAuthData(), restInfo.getEnv());
					out.write("{ \"jsrc\": " + serializedJSRC + ", \"recordCount\": " + recordCount + " }");
				}
				catch (Exception e)
				{
					restInfo.getEnv().clearAuthData();
					returnRestError("Error getting record count: " + e.getMessage(), out);
					resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					return;
				}
			}
		}
		else if ("xlsx".equals(format))
		{
			restInfo.getEnv().clearAuthData();
			returnXSLX(records, jcr, type, exportFileName, resp, restInfo.getAuthData(), restInfo.getEnv());
			return;
		}
		else if ("csv".equals(format))
		{
			String separator = ";";
			String quote = "\"";
			
			resp.setHeader("Content-Type", "text/csv; charset=ISO-8859-2");
			//resp.setHeader("Content-Type", "text/csv; charset=windows-1252");
			
			// get write anew with new encoding
			PrintWriter writer = resp.getWriter();
			
			if (StringUtils.isEmpty(exportFileName))
			{
				exportFileName = type.getInterpretedPluralLabel(authData);
			}
			
			resp.setHeader("Content-Disposition", "attachment; filename=\"" + exportFileName + ".csv\"");
			
			StringBuilder csv = new StringBuilder();
			
			List<String> columnHeaders = new ArrayList<String>();
			LinkedHashMap<PIR, String> fieldsByPir = new LinkedHashMap<PIR, String>();
			
			// write column names
			for (Property prop : jcr.getProperties())
			{
				String propName = JCRUtil.getPropertyName(prop, type, restInfo.getEnv());
				
				fieldsByPir.put(prop.getId() != null ? prop.getId() : PIR.get(propName, type, restInfo.getEnv()), propName);
				// TODO escape header names 
				
				if (StringUtils.hasText(prop.getAlias()))
				{
					columnHeaders.add(prop.getAlias());
				}
				else
				{
					// use property label
					columnHeaders.add(type.getField(fieldsByPir.get(prop.getId())).getInterpretedLabel(authData));
				}
			}
			
			csv.append(MiscUtils.implode(columnHeaders, separator, quote, null)).append("\n");
			
			// append records
			for (Record r : records)
			{	
				LinkedHashMap<String, Object> serializedRec = JSRUtil.recordToMap(r, type, DEFAULT_NESTING_LEVEL_DEPTH, restInfo.getEnv());
				
				// rewrite nulls to empty strings
				List<Object> values = new ArrayList<Object>();
				for (PIR pir : fieldsByPir.keySet())
				{
					if (!Field.ID_FIELD_NAME.equals(fieldsByPir.get(pir)))
					{
						Object val = JSRUtil.getFieldValue(serializedRec, pir);//serializedRec.get(pir.getValue());
						values.add(val == null ? "" : val);
					}
				}
				
				csv.append(MiscUtils.implode(values, separator, quote, null)).append("\n");
			}
			
			// write response to output
			//writer.write(new String(csv.toString().getBytes(Charset.forName("UTF-8")), Charset.forName("ISO-8859-2")));
			writer.write(csv.toString().replaceAll("ą", "a").replaceAll("ł", "l").replaceAll("Ś", "S").replaceAll("ś", "s").replaceAll("ó", "o").replaceAll("ń", "n").replaceAll("ę", "e"));
			//writer.write(csv.toString());
		}
		else
		{
			restInfo.getEnv().clearAuthData();
			returnRestError("Unsupported format " + format, restInfo.getOut());
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		
		restInfo.getEnv().clearAuthData();
	}
	
	private void returnXSLX (List<Record> records, JCR jcr, Type type, String exportFileName, HttpServletResponse resp, AuthData authData, EnvData env) throws KommetException
	{
		List<String> columnHeaders = new ArrayList<String>();
		List<String> propertyNames = new ArrayList<String>();
		LinkedHashMap<PIR, String> fieldsByPir = new LinkedHashMap<PIR, String>();
		
		// write column names
		for (Property prop : jcr.getProperties())
		{
			String propName = JCRUtil.getPropertyName(prop, type, env);
			
			fieldsByPir.put(prop.getId() != null ? prop.getId() : PIR.get(propName, type, env), propName);
			// TODO escape header names 
			
			if (StringUtils.hasText(prop.getAlias()))
			{
				columnHeaders.add(prop.getAlias());
			}
			else
			{
				// use property label
				columnHeaders.add(type.getField(fieldsByPir.get(prop.getId())).getInterpretedLabel(authData));
			}
			
			propertyNames.add(fieldsByPir.get(prop.getId()));
			//propertyNames.add(type.getField(fieldsByPir.get(prop.getId()), env).getApiName());
		}
		
		Workbook wb = new Workbook();
		Sheet sheet = (new RecordListCreator()).createRecordListSheet(records, columnHeaders, propertyNames, authData);
		sheet.setName(type.getInterpretedPluralLabel(authData));
		wb.addSheet(sheet);
		
		if (StringUtils.isEmpty(exportFileName))
		{
			exportFileName = type.getPluralLabel();
		}
		
		// return the file
		resp.setHeader("Content-Disposition", "attachment; filename=\"" + exportFileName + ".xlsx\""); 
		
		MsExcelApi excelApi = new MsExcelApi(ExcelFormat.XLSX);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		excelApi.write(wb, os);
		
		InputStream is = new ByteArrayInputStream(os.toByteArray());
		
		// copy it to response's output stream
		try
		{
			IOUtils.copy(is, resp.getOutputStream());
			resp.setContentType(MimeTypes.APPLICATION_EXCEL);
			resp.flushBuffer();
		}
		catch (IOException e)
		{
			throw new KommetException("Error writing file to output. Nested: " + e.getMessage());
		}
	}

	private Long getRecordCount(JCR jcr, Type type, AuthData authData, EnvData env) throws KommetException
	{	
		// nullify settings which are invalid in COUNT queries
		jcr.setLimit(null);
		jcr.setOffset(null);
		jcr.setOrderings(null);
		jcr.setProperties(null);
		
		Property countProperty = new Property();
		countProperty.setId(PIR.get(Field.ID_FIELD_NAME, type, env));
		countProperty.setAggregateFunction(AggregateFunction.COUNT);
		
		jcr.addProperty(countProperty);
		
		// build query from JCR
		String dalQuery = jcr.getQuery(env);
		
		if (jcr.getGroupings() == null || jcr.getGroupings().isEmpty())
		{
			return env.getSelectCriteriaFromDAL(dalQuery, authData).count();
		}
		else
		{
			// TODO we want to count the number of groups, but since as of 12/2014 DAL does
			// not support COUNT(distinct <prop>), we need to perform the actual query and 
			// get the number of results.
			// This is inefficient and needs to be optimized in the future.
			return Long.valueOf(env.getSelectCriteriaFromDAL(dalQuery).list().size());
		}
	}
	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_GET_MOBILE_JSTI_URL, method = RequestMethod.POST)
	@ResponseBody
	public void getMobileAppJSTI (@RequestParam(value = "env", required = false) String envId,
						@RequestParam(value = "access_token", required = false) String accessToken,
						HttpSession session, HttpServletResponse resp) throws KommetException, IOException
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
	
		JSTI jsti = new JSTI();
		
		// find all types to which this user has access
		for (Type type : restInfo.getEnv().getAccessibleTypes())
		{
			if (restInfo.getAuthData().canReadType(type.getKID(), false, restInfo.getEnv()))
			{
				jsti.addType(type, restInfo.getEnv(), true, false, true, restInfo.getAuthData());
			}
		}
		
		resp.setContentType("text/json; charset=UTF-8");
		restInfo.getOut().write(JSTI.serialize(jsti));
	}

	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + UrlUtil.REST_API_GET_JSTI_URL, method = RequestMethod.POST)
	@ResponseBody
	public void getJSTI (@RequestParam(value = "typePrefixes", required = false) String sTypePrefixes,
						@RequestParam(value = "typeNames", required = false) String typeNames,
						@RequestParam(value = "allUserTypes", required = false) Boolean fetchAllUserTypes,
						@RequestParam(value = "env", required = false) String envId,
						@RequestParam(value = "access_token", required = false) String accessToken,
						HttpSession session, HttpServletResponse resp) throws KommetException, IOException
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
	
		JSTI jsti = new JSTI();
		
		if (fetchAllUserTypes != null && Boolean.TRUE.equals(fetchAllUserTypes))
		{
			TypeFilter typeFilter = new TypeFilter();
			typeFilter.setIsBasic(false);
			
			for (Type type : dataService.getTypes(typeFilter, true, false, restInfo.getEnv()))
			{
				jsti.addType(type, restInfo.getEnv(), true, false, restInfo.getAuthData());
			}
		}
		else
		{
			if (StringUtils.hasText(sTypePrefixes))
			{
				String[] prefixList = sTypePrefixes.split(",");
				for (String prefix : prefixList)
				{
					Type type = restInfo.getEnv().getType(KeyPrefix.get(prefix));
					if (type == null)
					{
						returnRestError("Type with key prefix " + prefix + " not found", restInfo.getOut());
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						return;
					}
					
					jsti.addType(type, restInfo.getEnv(), true, false, restInfo.getAuthData());
				}
			}
			
			if (StringUtils.hasText(typeNames))
			{
				String[] nameList = typeNames.split(",");
				for (String typeName : nameList)
				{
					Type type = restInfo.getEnv().getType(typeName);
					if (type == null)
					{
						returnRestError("Type with name " + typeName + " not found", restInfo.getOut());
						resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
						return;
					}
					
					jsti.addType(type, restInfo.getEnv(), true, false, restInfo.getAuthData());
				}
			}
		}
	
		resp.setContentType("text/json; charset=UTF-8");
		restInfo.getOut().write(JSTI.serialize(jsti));
	}
}