/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.utils;

import javax.servlet.ServletContext;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import kommet.data.KommetException;

public class BeanUtils
{
	public static Object getBean (Class<?> beanClass, ServletContext sc) throws KommetException
	{
		WebApplicationContext wac = WebApplicationContextUtils.getWebApplicationContext(sc);
        AutowireCapableBeanFactory factory = wac.getAutowireCapableBeanFactory();
        
        if (factory == null)
        {
        	throw new KommetException("Bean factory is null");
        }
        
        return factory.getBean(beanClass);
	}
}