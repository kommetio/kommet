/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import kommet.data.FieldValidationException;
import kommet.data.KommetException;
import kommet.data.ValidationMessage;
import kommet.json.JSON;
import kommet.utils.MiscUtils;

public class RestUtil
{
	/**
	 * Returns a JSON error response containing serialized field validation errors in a format expected e.g.
	 * by rm.objectdetails
	 * @param e
	 * @return
	 * @throws KommetException
	 */
	public static String getRestErrorResponse (FieldValidationException e) throws KommetException
	{
		List<String> errors = new ArrayList<String>();
		
		for (ValidationMessage msg : e.getMessages())
		{
			errors.add("{ \"fieldId\": \"" + msg.getFieldId() + "\", \"message\": \"" + JSON.escape(msg.getText()) + "\" }");
		}
		
		return "{ \"success\": false, \"fieldErrors\": [ " + MiscUtils.implode(errors, ",") + " ] }";
	}
	
	public static String getRestErrorResponse (String err) throws KommetException
	{
		return "{ \"success\": false, \"messages\": [ \"" + JSON.escape(err) + "\" ] }";
	}
	
	public static String getRestErrorResponse (Collection<String> messages)
	{
		return "{ \"success\": false, \"messages\": [" + MiscUtils.implode(messages, ", ", "\"") + "] }";
	}
	
	public static String getRestSuccessResponse (String msg) throws KommetException
	{
		return "{ \"success\": true, \"message\": \"" + JSON.escape(msg) + "\" }";
	}
	
	public static String getRestSuccessDataResponse (String dataJSON) throws KommetException
	{
		return "{ \"success\": true, \"data\": " + dataJSON + " }";
	}
}