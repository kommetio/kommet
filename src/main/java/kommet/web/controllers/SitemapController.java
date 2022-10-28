/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.web.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import kommet.sitemap.XmlUrl;
import kommet.sitemap.XmlUrlSet;
import kommet.utils.UrlUtil;

@Controller
public class SitemapController
{	
	@RequestMapping(value = UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/sitemap.xml", method = RequestMethod.GET)
    @ResponseBody
    public XmlUrlSet showSitemap()
	{
        XmlUrlSet xmlUrlSet = new XmlUrlSet();
        /*create(xmlUrlSet, "", XmlUrl.Priority.HIGH, "daily");
        create(xmlUrlSet, "/auth/login", XmlUrl.Priority.MEDIUM, "monthly");
        create(xmlUrlSet, "/faq/search", XmlUrl.Priority.HIGH, "hourly");
        create(xmlUrlSet, "/tutorials", XmlUrl.Priority.HIGH, "hourly");
        create(xmlUrlSet, "/codesandbox", XmlUrl.Priority.HIGH, "daily");
        create(xmlUrlSet, "/contact", XmlUrl.Priority.MEDIUM, "monthly");*/

        return xmlUrlSet;
    }

	private void create(XmlUrlSet xmlUrlSet, String link, XmlUrl.Priority priority, String changeFreq)
	{
		xmlUrlSet.addUrl(new XmlUrl("http://kommet.io" + link, priority, changeFreq));
	}

}