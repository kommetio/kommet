/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.sitemap;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(value = XmlAccessType.NONE)
@XmlRootElement(name = "url")
public class XmlUrl
{
	public enum Priority
	{
		HIGH("1.0"), MEDIUM("0.5");

		private String value;

		Priority(String value)
		{
			this.value = value;
		}

		public String getValue()
		{
			return value;
		}
	}

	@XmlElement
	private String loc;

	@XmlElement
	private String lastmod = (new SimpleDateFormat("yyyy-MM-dd").format(new Date()));

	@XmlElement
	private String changefreq = "daily";

	@XmlElement
	private String priority;

	public XmlUrl()
	{
	}

	public XmlUrl(String loc, Priority priority, String changeFreq)
	{
		this.loc = loc;
		this.priority = priority.getValue();
		this.changefreq = changeFreq;
	}

	public String getLoc()
	{
		return loc;
	}

	public String getPriority()
	{
		return priority;
	}

	public String getChangefreq()
	{
		return changefreq;
	}

	public String getLastmod()
	{
		return lastmod;
	}
}