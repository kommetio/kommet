/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.i18n;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.data.KommetException;
import kommet.utils.AppConfig;

@Service
public class InternationalizationService
{	
	private Map<Locale, I18nDictionary> dictionaries;
	
	@Transactional
	public void readInKeysFile (Locale locale, FileInputStream fis)
	{
		I18nDictionary dict = new I18nDictionary(locale);
		
		Scanner scan = new Scanner(fis, "UTF-8");
		while (scan.hasNextLine())
		{
			String[] bits = scan.nextLine().split("=");
			// allow empty values for keys
			dict.addKey(bits[0], bits.length > 1 ? bits[1] : "");
		}
		
		scan.close();
		
		this.dictionaries.put(locale, dict);
	}
	
	public String get (Locale locale, String key)
	{
		return this.dictionaries.get(locale).get(key);
	}

	@Inject
	public void setAppConfig(AppConfig appConfig) throws KommetException
	{
		this.dictionaries = new HashMap<Locale, I18nDictionary>();
		
		// read in i18n keys for all available locales
		for (Locale locale : Locale.values())
		{
			String localeFileName = "i18n/" + locale.name().toLowerCase() + ".properties";
			
			URI uri = null;
			try
			{
				// if resource path contains white space, we may have a problem getting the file
				// so we need to convert the path to URI which will convert all spaces encoded as
				// %20 into regular whitespace characters
				uri = new URI(this.getClass().getClassLoader().getResource(localeFileName).getFile());
			}
			catch (URISyntaxException e)
			{
				throw new KommetException("Error converting resource path to URI. Nested: " + e.getMessage());
			}
			
			File file = new File(uri.getPath());
			
			if (!file.exists())
			{
				throw new KommetException("Internationalization file " + locale.name().toLowerCase() + "(" + file.getAbsolutePath() + ") does not exist");
			}
			
			try
			{
				readInKeysFile(locale, new FileInputStream(file));
			}
			catch (FileNotFoundException e)
			{
				throw new KommetException("Locale file for locale " + locale.name() + " not found");
			}
		}
	}

	public I18nDictionary getDictionary(Locale locale)
	{
		return this.dictionaries.get(locale);
	}
}