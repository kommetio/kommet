/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.data;

/**
 * Represents a mapping between a domain and an environment
 * <p>
 * Domain mappings are stored in the shared Kommet database.
 * 
 * @author Radek Krawiec
 * @since 04/06/2015
 */
public class DomainMapping
{
	private Long id;
	private String url;
	private Env env;

	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public Env getEnv()
	{
		return env;
	}

	public void setEnv(Env env)
	{
		this.env = env;
	}

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}
}