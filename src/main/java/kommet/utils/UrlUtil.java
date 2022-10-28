/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.utils;

import kommet.data.Type;

public class UrlUtil
{
	public static final String OAUTH_GET_TOKEN_URL = "/oauth/token";
	public static final String REST_API_DAL_URL = "/rest/dal";
	public static final String REST_API_DEPLOY_PACKAGE_URL = "/rest/library/deploy";
	public static final String REST_API_SAVE_VIEW_URL = "/rest/views/save";
	public static final String REST_API_SAVE_CLASS_URL = "/rest/classes/save";
	public static final String REST_API_SAVE_RECORD_URL = "/rest/record/save";
	public static final String REST_API_DELETE_RECORD_URL = "/rest/record/delete";
	public static final String REST_API_DELETE_CLASS_URL = "/rest/classes/delete";
	public static final String REST_API_QUERY_DS_URL = "/rest/jsds/query";
	public static final String REST_API_GET_JSTI_URL = "/rest/jsti";
	public static final String REST_API_GET_MOBILE_JSTI_URL = "/rest/mobile/jsti";
	public static final String REST_API_ASSOCIATE_URL = "/rest/associate";
	public static final String REST_API_UNASSOCIATE_URL = "/rest/unassociate";
	public static final String REST_API_GET_CONTROLLER_CLASSES = "/classes/controllers";
	public static final String REST_API_GET_VIEWS = "/views/all";
	public static final String REST_API_RECORD_COMMENTS_URL = "/rest/recordcomments";
	public static final String SYSTEM_ACTION_URL_PREFIX = "km";
	public static final String CONFIG_JS_URL = "/js/km.config.js";
	public static final String USER_CSS_STYLES_URL = "/resources/km.userstyles.css";
	public static final String REST_CREATE_FIELD_URL = "/rest/field/create";
	public static final String REST_RUN_TEST_URL = "/rest/test/run";
	
	public static String getRecordListUrl (Type type)
	{
		return type.getKeyPrefix().getPrefix();
	}
}