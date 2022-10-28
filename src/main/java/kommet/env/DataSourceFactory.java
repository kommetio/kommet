/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.env;

import java.util.Properties;

import javax.inject.Inject;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.XmlWebApplicationContext;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.atomikos.jdbc.nonxa.AtomikosNonXADataSourceBean;

import kommet.data.KID;
import kommet.data.KommetException;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.StringUtils;

@Service
public class DataSourceFactory implements ApplicationContextAware
{
	@Inject
	AppConfig appConfig;
	
	private ApplicationContext appContext;
	
	private static final Logger log = LoggerFactory.getLogger(DataSourceFactory.class);
	
	public DataSource getDataSource (KID envId, EnvService envService, String host, String port, String database, String username, String password, boolean isForceRecreate) throws KommetException
	{
		return getDataSource(envId, envService, host, port, database, username, password, null, isForceRecreate);
	}
	
	public void removeDataSource (KID envId, EnvService envService)
	{
		String dsName = envService.getDataSourceName(envId);
		
		if (dsName != null)
		{
			BeanFactory factory = ((XmlWebApplicationContext)appContext).getBeanFactory();
			((BeanDefinitionRegistry)factory).removeBeanDefinition(dsName);
		}
	}
	
	/**
	 * Returns a data source to access the database.
	 * @param host
	 * @param port
	 * @param database
	 * @param username
	 * @param password
	 * @param isForceRecreate 
	 * @return
	 * @throws KommetException
	 */
	private DataSource getDataSource (KID envId, EnvService envService, String host, String port, String database, String username, String password, Boolean isXaMode, boolean isForceRecreate) throws KommetException
	{
		if (isXaMode == null)
		{
			isXaMode = appConfig.isXaTransactions();
		}
		
		// check if a data source name is already defined for this env
		String dsName = envService.getDataSourceName(envId);
		
		if (StringUtils.isEmpty(dsName))
		{
			// generate a datasource name for this env
			dsName = "dataSource-" + database;
			envService.setDataSource(envId, dsName);
		}
		
		if (isXaMode)
		{
			return getXADataSource(envId, envService, dsName, host, port, database, username, password, isForceRecreate);
		}
		else
		{
			return getNonXADataSource(envId, envService, dsName, host, port, database, username, password, isForceRecreate);
		}
	}
	
	private DataSource getXADataSource (KID envId, EnvService envService, String beanId, String host, String port, String database, String username, String password, boolean isForceRecreate) throws KommetException
	{	
		Object existingBean = null;
		
		// first check if a bean with this name does not already exist in the context
		try
		{
			existingBean = appContext.getBean(beanId);
			
			if (isForceRecreate)
			{
				BeanFactory factory = ((XmlWebApplicationContext)appContext).getBeanFactory();
				((BeanDefinitionRegistry)factory).removeBeanDefinition(beanId);
				
				// create new datasource name for this env
				beanId = "dataSource-" + database + "-" + MiscUtils.getHash(5);
				envService.setDataSource(envId, beanId);
			} 
			else
			{
				// TODO remember to close connections when you are done using them
				if (existingBean instanceof AtomikosDataSourceBean)
				{
					return (AtomikosDataSourceBean)existingBean;
				}
				else
				{
					throw new KommetException("A bean with reserved name format 'database-*' (" + beanId + ") exists but is not an instance of AtomikosNonXADataSourceBean");
				}
			}
		}
		catch (Exception e)
		{
			// if fetching bean failed, that's OK, we will create it
		}
	
		BeanDefinitionBuilder ds = BeanDefinitionBuilder.rootBeanDefinition(AtomikosDataSourceBean.class);
		ds.addPropertyValue("uniqueResourceName", beanId);
		ds.addPropertyValue("xaDataSourceClassName", "org.postgresql.xa.PGXADataSource");
		ds.addPropertyValue("poolSize", 5);
		ds.addPropertyValue("minPoolSize", 20);
		ds.addPropertyValue("maxPoolSize", appConfig.getMaxConnectionPoolSize());
		ds.addPropertyValue("testQuery", "SELECT table_name FROM information_schema.tables limit 1");
		
		Properties xaProps = new Properties(); 
		xaProps.setProperty("serverName", host);
		xaProps.setProperty("user", username);
		xaProps.setProperty("password", password);
		xaProps.setProperty("databaseName", database);
		xaProps.setProperty("portNumber", port);
		
		ds.addPropertyValue("xaProperties", xaProps);
		ds.addPropertyValue("borrowConnectionTimeout", 60);
		
		if (appContext instanceof GenericApplicationContext)
		{
			((GenericApplicationContext)appContext).registerBeanDefinition(beanId, ds.getBeanDefinition());
			return (AtomikosDataSourceBean)appContext.getBean(beanId);
		}
		else if (appContext instanceof XmlWebApplicationContext)
		{	
			BeanFactory factory = ((XmlWebApplicationContext)appContext).getBeanFactory();
			((BeanDefinitionRegistry)factory).registerBeanDefinition(beanId, ds.getBeanDefinition());
			log.debug("Creating DS " + ((AtomikosDataSourceBean)((XmlWebApplicationContext)appContext).getBeanFactory().getBean(beanId)).getUniqueResourceName() + " [host=" + host + "]");
			return (AtomikosDataSourceBean)((XmlWebApplicationContext)appContext).getBeanFactory().getBean(beanId);
		}
		else
		{
			throw new KommetException("App context is of unsupported type " + appContext.getClass().getName());
		}
	}
	
	private DataSource getNonXADataSource (KID envId, EnvService envService, String beanId, String host, String port, String database, String username, String password, boolean isForceRecreate) throws KommetException
	{	
		Object existingBean = null;
		
		// try to get existing bean, if we don't want to force creation of a new one
		//	first check if a bean with this name does not already exist in the context
		try
		{
			existingBean = appContext.getBean(beanId);
			
			// TODO shouldn't we use an XA data source here?
			// TODO remember to close connections when you are done using them
			if (isForceRecreate)
			{
				BeanFactory factory = ((XmlWebApplicationContext)appContext).getBeanFactory();
				((BeanDefinitionRegistry)factory).removeBeanDefinition(beanId);
				
				// create new datasource name for this env
				beanId = "dataSource-" + database + "-" + MiscUtils.getHash(5);
				envService.setDataSource(envId, beanId);
			}
			else
			{
				if (existingBean instanceof AtomikosNonXADataSourceBean)
				{
					return (AtomikosNonXADataSourceBean)existingBean;
				}
				else
				{
					throw new KommetException("A bean with reserved name format 'database-*' (" + beanId + ") exists but is not an instance of AtomikosNonXADataSourceBean");
				}
			}
		}
		catch (NoSuchBeanDefinitionException e)
		{
			// if fetching bean failed, that's OK, we will create it
		}
	
		BeanDefinitionBuilder ds = BeanDefinitionBuilder.rootBeanDefinition(AtomikosNonXADataSourceBean.class);
		ds.addPropertyValue("url", "jdbc:postgresql://" + host + ":" + port + "/" + database);
		ds.addPropertyValue("user", username);
		ds.addPropertyValue("password", password);
		ds.addPropertyValue("borrowConnectionTimeout", 60);
		ds.addPropertyValue("uniqueResourceName", beanId);
		ds.addPropertyValue("driverClassName", "org.postgresql.Driver");
		ds.addPropertyValue("testQuery", "SELECT table_name FROM information_schema.tables limit 1");
		
		// maximum life time of a connection in the pool, in seconds
		//ds.addPropertyValue("maxLifetime", 600);
		ds.addPropertyValue("poolSize", 5);
		ds.addPropertyValue("minPoolSize", 20);
		ds.addPropertyValue("maxPoolSize", 50);
		
		if (appContext instanceof GenericApplicationContext)
		{
			((GenericApplicationContext)appContext).registerBeanDefinition(beanId, ds.getBeanDefinition());
			return (AtomikosNonXADataSourceBean)appContext.getBean(beanId);
		}
		else if (appContext instanceof XmlWebApplicationContext)
		{	
			BeanFactory factory = ((XmlWebApplicationContext)appContext).getBeanFactory();
			((BeanDefinitionRegistry)factory).registerBeanDefinition(beanId, ds.getBeanDefinition());
			log.debug("Creating DS " + ((AtomikosNonXADataSourceBean)((XmlWebApplicationContext)appContext).getBeanFactory().getBean(beanId)).getUniqueResourceName() + "[host=" + host + "]");
			return (AtomikosNonXADataSourceBean)((XmlWebApplicationContext)appContext).getBeanFactory().getBean(beanId);
		}
		else
		{
			throw new KommetException("App context is of unsupported type " + appContext.getClass().getName());
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext appContext) throws BeansException
	{
		this.appContext = appContext;
	}
}