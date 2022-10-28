/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jsoup.helper.StringUtil;
import org.springframework.stereotype.Service;

import kommet.data.KID;
import kommet.data.KIDException;
import kommet.env.EnvService;
import kommet.i18n.Locale;

/**
 * Contains configuration of the application.
 * @author Radek Krawiec
 */
@Service
public class AppConfig extends PropertySet
{
	/**
	 * Name of the file where server properties are stored. These properties refer to the whole
	 * server including all apps running on it. Some of them can be overridden by app-specific configuration.
	 * The file is located in the app's resource folder.
	 */
	private static final String SERVER_CONFIG_FILE = "config-dev.properties";

	/**
	 * Name of the package where all base system types are stored (such as Action, Class, View etc.)
	 */
	public static final String BASE_TYPE_PACKAGE = "kommet.basic";

	/**
	 * Returns the value of a given property
	 * @param name - the name of the property
	 * @return
	 * @throws PropertyUtilException
	 */
	public String getProperty (String name) throws PropertyUtilException
	{
		return getProperty(SERVER_CONFIG_FILE, name);
	}

	/**
	 * Tells whether by default only required fields should be rendered on the object details tag.
	 * @return
	 * @throws PropertyUtilException
	 */
	public boolean isRenderOnlyRequiredFieldsOnObjectDetails() throws PropertyUtilException
	{
		return "true".equals(getProperty("kommet.object.details.render.only.required.fields"));
	}

	/**
	 * Returns the name of the default Java type used for numeric field representation
	 * if this field is not an integer.
	 * @return
	 * @throws PropertyUtilException
	 */
	public String getDefaultFloatJavaType() throws PropertyUtilException
	{
		return getProperty("kommet.default.float.javatype");
	}

	public String getFontDir() throws PropertyUtilException
	{
		return getRootDir() + "/" + getProperty("kommet.font.dir");
	}

	/**
	 * Returns the ID of the default environment.
	 * @return
	 * @throws PropertyUtilException
	 */
	public KID getDefaultEnvId() throws PropertyUtilException
	{
		String envId = getEnvVar("KM_DEFAULT_ENV_ID");

		System.out.println("KM_DEFAULT_ENV_ID = " + envId);

		if (StringUtils.isEmpty(envId))
		{
			envId = getProperty("kommet.default.envid");
		}

		try
		{
			KID id = StringUtils.isEmpty(envId) ? null : KID.get(envId);

			if (EnvService.MASTER_ENV_KID.equals(id.getId()))
			{
				throw new PropertyUtilException("Default env ID cannot be the same as master env ID: " + EnvService.MASTER_ENV_KID);
			}

			return id;
		}
		catch (KIDException e)
		{
			throw new PropertyUtilException("Could not cast string '" + envId + "' to KID representing env ID");
		}
 	}

	public String getDefaultIntJavaType() throws PropertyUtilException
	{
		return getProperty("kommet.default.int.javatype");
	}

	/**
	 * Returns the name of the database trigger function that checks edit permissions for
	 * a given record basing on its user record sharings.
	 * @return
	 * @throws PropertyUtilException
	 */
	public String getCheckEditPermissionsFunction() throws PropertyUtilException
	{
		return getProperty("kommet.db.checkeditpermissions.function");
	}

	public String getDefaultCollectionDisplay() throws PropertyUtilException
	{
		return getProperty("kommet.default.collection.display");
	}

	/**
	 * Returns the default locale for this server installation.
	 * @return
	 * @throws PropertyUtilException
	 */
	public Locale getDefaultLocale() throws PropertyUtilException
	{
		String sLocale = getProperty("kommet.default.locale");
		return Locale.valueOf(sLocale);
	}

	/**
	 * Returns the name of the database trigger function that checks delete permissions for
	 * a given record basing on its user record sharings.
	 * @return
	 * @throws PropertyUtilException
	 */
	public String getCheckDeletePermissionsFunction() throws PropertyUtilException
	{
		return getProperty("kommet.db.checkdeletepermissions.function");
	}

	/**
	 * Returns a text message that should be displayed when server is unavailable.
	 * This can be used during scheduled maintenance periods.
	 * @return
	 * @throws PropertyUtilException
	 */
	public String getServerMaintenanceMessage() throws PropertyUtilException
	{
		return getProperty("kommet.server.maintenance.message");
	}

	public String getEnvDBUser() throws PropertyUtilException
	{
		return getProperty("kommet.envdb.user");
	}

	public String getRestorePasswordEmailTemplate() throws PropertyUtilException
	{
		return getProperty("kommet.doctemplate.restorepwdemail");
	}

	public char getDecimalSeparator() throws PropertyUtilException
	{
		String sep = getProperty("kommet.decimal.separator");
		if (StringUtil.isBlank(sep))
		{
			throw new PropertyUtilException("Decimal seperator is empty");
		}
		else if (sep.length() > 1)
		{
			throw new PropertyUtilException("Decimal separator consists of more than one characters: " + sep);
		}
		return getProperty("kommet.decimal.separator").charAt(0);
	}

	/**
	 * Returns a set of URLs which cannot be used in the app. URLs may contain wildcards in form of an asterisk.
	 * @return
	 * @throws PropertyUtilException
	 */
	public Set<String> getReservedURLs() throws PropertyUtilException
	{
		String urls = getProperty("kommet.reserved.urls");
		Set<String> urlSet = new HashSet<String>();

		if (StringUtils.isEmpty(urls))
		{
			return urlSet;
		}
		else
		{
			Collections.addAll(urlSet, urls.split(","));
			return urlSet;
		}
	}

	/**
	 * Maximum connection pool size.
	 * @return
	 * @throws PropertyUtilException
	 */
	public int getMaxConnectionPoolSize() throws PropertyUtilException
	{
		try
		{
			return Integer.valueOf(getProperty("kommet.maxconnpoolsize"));
		}
		catch (NumberFormatException e)
		{
			throw new PropertyUtilException("Value for setting kommet.maxconnpoolsize is not an integer");
		}
	}

	public int getMaxTextFieldLength() throws PropertyUtilException
	{
		try
		{
			return Integer.valueOf(getProperty("kommet.max.textfield.length"));
		}
		catch (NumberFormatException e)
		{
			throw new PropertyUtilException("Value for setting kommet.max.textfield.length is not an integer");
		}
	}

	public boolean isDebugKollCode() throws PropertyUtilException
	{
		return "true".equals(getProperty("kommet.debug.duration.koll"));
	}

	/**
	 * Tells whether action redirects should be redirected to HTTPS.
	 * With properly configured HTTPS servers this should always be set to true.
	 * However, in some work environments HTTPS option can be not configured, so setting this
	 * to false is required for redirects to work.
	 * @return
	 * @throws PropertyUtilException
	 */
	public boolean isRedirectToHttps() throws PropertyUtilException
	{
		return "true".equals(getProperty("kommet.redirecttohttps"));
	}

	public boolean isEmailFeatureActive() throws PropertyUtilException
	{
		return "true".equals(getProperty("kommet.email.active"));
	}

	public boolean isRequestDebug() throws PropertyUtilException
	{
		return "true".equals(getProperty("kommet.request.debug"));
	}

	public boolean isGenerateControllerCodeAnew() throws PropertyUtilException
	{
		return "true".equals(getProperty("kommet.generate.controller.code.anew"));
	}

	/**
	 * Tells whether TypeTrigger records should be stored in the database (true) or just in the
	 * environment cache (false).
	 * @return
	 * @throws PropertyUtilException
	 */
	public boolean isPersistTypeTriggerBinding() throws PropertyUtilException
	{
		return "true".equals(getProperty("kommet.persist.typetriggers"));
	}

	public boolean isCreateAnyRecords() throws PropertyUtilException
	{
		return "true".equals(getProperty("kommet.anyrecords.create"));
	}

	public int getBreadcrumbMax() throws PropertyUtilException
	{
		try
		{
			return Integer.valueOf(getProperty("kommet.breadcrumbs.max"));
		}
		catch (NumberFormatException e)
		{
			throw new PropertyUtilException("Value for setting kommet.breadcrumbs.max is not an integer");
		}
	}

	/**
	 * Tells whether DB transactions operate in XA mode.
	 * @return
	 * @throws PropertyUtilException
	 */
	public boolean isXaTransactions() throws PropertyUtilException
	{
		return "true".equals(getProperty("kommet.transactions.xa"));
	}

	public Integer getMaxMessagesDisplayed() throws PropertyUtilException
	{
		try
		{
			return Integer.parseInt(getProperty("kommet.maxmsgsdisplayed"));
		}
		catch (NumberFormatException e)
		{
			throw new PropertyUtilException("Invalid numeric value for property kommet.maxmsgsdisplayed");
		}
	}

	/**
	 * Maximum possible nested calls to login as.
	 * @return
	 * @throws PropertyUtilException
	 */
	public Integer getLoginAsMax() throws PropertyUtilException
	{
		try
		{
			return Integer.parseInt(getProperty("kommet.loginas.max"));
		}
		catch (NumberFormatException e)
		{
			throw new PropertyUtilException("Invalid numeric value for property kommet.loginas.max");
		}
	}

	public Integer getMinPasswordLength() throws PropertyUtilException
	{
		try
		{
			return Integer.parseInt(getProperty("kommet.minpwdlength"));
		}
		catch (NumberFormatException e)
		{
			throw new PropertyUtilException("Invalid numeric value for property kommet.minpwdlength");
		}
	}

	public String getClasspathSeparator() throws PropertyUtilException
	{
		return getProperty("kommet.classpath.separator");
	}

	public String getHomeURL() throws PropertyUtilException
	{
		return getProperty("kommet.home.url");
	}

	public String getAdminHomeURL() throws PropertyUtilException
	{
		return getProperty("kommet.home.admin.url");
	}

	/**
	 * Returns the default domain for this server. This should practically never be used,
	 * since a server will host multiple environments and each of them will probably have their
	 * own domain.
	 * @return default domain name, e.g. "kommet.com"
	 * @throws PropertyUtilException
	 */
	public String getDefaultDomain() throws PropertyUtilException
	{
		return getProperty("kommet.default.domain");
	}

	/**
	 * Classpath that needs to be attached when Koll files are compiled
	 * @return
	 * @throws PropertyUtilException
	 */
	public String getCompileClasspath() throws PropertyUtilException
	{
		String compileClassPath = getEnvVar("KM_COMPILE_CLASSPATH");
		return StringUtils.isEmpty(compileClassPath) ? getRootDir() + "/" + getProperty("kommet.koll.compileclasspath") : compileClassPath;
	}

	public String getEnvDBPassword() throws PropertyUtilException
	{
		String env = getEnvVar("KM_DB_PWD");
		return StringUtils.isEmpty(env) ? getProperty("kommet.envdb.password") : env;
	}

	public String getKeetleDir() throws PropertyUtilException
	{
		String dir = getEnvVar("KM_KEETLE_DIR");
		return StringUtils.isEmpty(dir) ? getRootDir() + "/" + getProperty("kommet.keetle.dir") : dir;
	}

	public String getLayoutDir() throws PropertyUtilException
	{
		String dir = getEnvVar("KM_LAYOUT_DIR");
		return StringUtils.isEmpty(dir) ? getRootDir() + "/" +  getProperty("kommet.layout.dir") : dir;
	}

	public String getTldDir() throws PropertyUtilException
	{
		return getRootDir() + "/" +  getProperty("kommet.tld.dir");
	}

	public String getLayoutRelativeDir() throws PropertyUtilException
	{
		return getProperty("kommet.layout.reldir");
	}

	/**
	 * Directory where additional installed libraries are put.
	 * @return
	 * @throws PropertyUtilException
	 */
	public String getLibDir() throws PropertyUtilException
	{
		String dir = getEnvVar("KM_LIB_DIR");
		return StringUtils.isEmpty(dir) ? getProperty("kommet.lib.dir") : dir;
	}
	
	public String getKollDir() throws PropertyUtilException
	{
		String dir = getEnvVar("KM_KOLLDIR");
		return StringUtils.isEmpty(dir) ? getProperty("kommet.koll.dir") : dir;
	}

	/**
	 * Absolute path to the directory where uploaded files are stored
	 * @return
	 * @throws PropertyUtilException
	 */
	public String getFileDir() throws PropertyUtilException
	{
		String dir = getEnvVar("KM_FILE_DIR");
		return StringUtils.isEmpty(dir) ? getProperty("kommet.file.dir") : dir;
	}

	/**
	 * Returns the view resource directory path, relative to the deployed app's home directory.
	 * @return
	 * @throws PropertyUtilException
	 */
	public String getViewResourceRelativeDir() throws PropertyUtilException
	{
		return getProperty("kommet.viewresource.relative.dir");
	}

	public String getViewResourceDir() throws PropertyUtilException
	{
		String dir = getEnvVar("KM_VIEWRESOURCE_DIR");
		return StringUtils.isEmpty(dir) ? getRootDir() + "/" + getProperty("kommet.viewresource.dir") : dir;
	}

	public String getEnvDBHost() throws PropertyUtilException
	{
		String host = getEnvVar("KM_ENVDB_HOST");
		return StringUtils.isEmpty(host) ? getProperty("kommet.envdb.host") : host;
	}

	public String getEnvDBPort() throws PropertyUtilException
	{
		String port = getEnvVar("KM_ENVDB_PORT");
		return StringUtils.isEmpty(port) ? getProperty("kommet.envdb.port") : port;
	}

	public String getMasterDBHost() throws PropertyUtilException
	{
		String host = getEnvVar("KM_ENVDB_HOST");
		System.out.println("KM_ENVDB_HOST = " + host);
		return StringUtils.isEmpty(host) ? getProperty("kommet.masterdb.host") : host;
	}

	public String getMasterDBPort() throws PropertyUtilException
	{
		String port = getEnvVar("KM_MASTERDB_PORT");
		return StringUtils.isEmpty(port) ? getProperty("kommet.masterdb.port") : port;
	}

	public String getMasterDB() throws PropertyUtilException
	{
		return getProperty("kommet.masterdb");
	}

	public String getMasterDBUser() throws PropertyUtilException
	{
		return getProperty("kommet.masterdb.user");
	}

	public String getMasterDBPassword() throws PropertyUtilException
	{
		String env = getEnvVar("KM_DB_PWD");
		return StringUtils.isEmpty(env) ? getProperty("kommet.masterdb.password") : env;
	}

	public String getTypeSeqStart() throws PropertyUtilException
	{
		return getProperty("kommet.type.sequence.start");
	}

	public String getFieldSeqStart() throws PropertyUtilException
	{
		return getProperty("kommet.field.sequence.start");
	}

	public static KID getRootUserId() throws KIDException
	{
		return KID.get(KID.USER_PREFIX, Long.valueOf(1));
	}

	public KID getDefaultRootUserIdForNewEnvs() throws KIDException, PropertyUtilException
	{
		return KID.get(getProperty("kommet.rootuserid.newenv"));
	}

	public String getRelativeKeetleDir() throws PropertyUtilException
	{
		return getProperty("kommet.keetle.reldir");
	}

	public void clearCache()
	{
		clearCachedProperties();
	}

	public String getNewEnvDbOwner() throws PropertyUtilException
	{
		return getProperty("kommet.newenvdb.owner");
	}

	public String getTemplateEnvDb() throws PropertyUtilException
	{
		return getProperty("kommet.templateenv.db");
	}

	private String getEnvVar (String name)
	{
		return System.getenv(name);
	}

	private String getRootDir() throws PropertyUtilException
	{
		String dir = getEnvVar("KM_ROOTDIR");
		return StringUtils.isEmpty(dir) ? getProperty("kommet.rootdir") : dir;
	}
}