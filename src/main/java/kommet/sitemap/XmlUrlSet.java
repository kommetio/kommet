/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.sitemap;

import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;


@XmlAccessorType(value = XmlAccessType.NONE)
@XmlRootElement(name = "urlset")
public class XmlUrlSet
{
	@XmlElements({ @XmlElement(name = "url", type = XmlUrl.class) })
	private Collection<XmlUrl> xmlUrls = new ArrayList<XmlUrl>();
	
	@XmlAttribute
	private String xmlns = "http://www.sitemaps.org/schemas/sitemap/0.9";

	public void addUrl(XmlUrl xmlUrl)
	{
		xmlUrls.add(xmlUrl);
	}

	public Collection<XmlUrl> getXmlUrls()
	{
		return xmlUrls;
	}

	public void setXmlns(String xmlns)
	{
		this.xmlns = xmlns;
	}

	public String getXmlns()
	{
		return xmlns;
	}
}