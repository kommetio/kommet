/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web;

/**
 * This class stores keys of request attributes that are added to the request to
 * pass some data.
 * @author Radek Krawiec
 * @created 10-03-2014
 *
 */
public class RequestAttributes
{
	public static final String PAGE_DATA_ATTR_NAME = "pageData";
	public static final String AUTH_DATA_ATTR_NAME = "authData";
	public static final String APP = "app";
	public static final String TEXT_LABELS_ATTR_NAME = "textLabels";
	public static final String ENV_ATTR_NAME = "envData";
	public static final String ENV_ID_ATTR_NAME = "env";
	public static final String APP_CONFIG_ATTR_NAME = "appConfig";
	public static final String LAYOUT_ATTR_NAME = "layoutId";
}