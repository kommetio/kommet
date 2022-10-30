/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.services;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.basic.Dictionary;
import kommet.basic.DictionaryItem;
import kommet.dao.DictionaryDao;
import kommet.dao.DictionaryItemDao;
import kommet.dao.FieldDao;
import kommet.dao.FieldFilter;
import kommet.data.Field;
import kommet.data.FieldValidationException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.filters.DictionaryFilter;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.ValidationUtil;

@Service
public class DictionaryService
{
	@Inject
	DictionaryDao dao;
	
	@Inject
	DictionaryItemDao itemDao;
	
	@Inject
	FieldDao fieldDao;
	
	@Inject
	AppConfig appConfig;
	
	@Transactional(readOnly = true)
	public List<Dictionary> get (DictionaryFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		return dao.get(filter, authData, env);
	}
	
	@Transactional
	public Dictionary save (Dictionary dict, AuthData authData, EnvData env) throws KommetException
	{
		if (!ValidationUtil.isValidOptionallyQualifiedResourceName(dict.getName()))
		{
			throw new FieldValidationException("Invalid dictionary name " + dict.getName());
		}
		
		dict = dao.save(dict, authData, env);
		
		// save items
		if (dict.getItems() != null)
		{
			for (DictionaryItem item : dict.getItems())
			{
				item.setDictionary(dict);
				itemDao.save(item, authData, env);
			}
		}
		
		env.initDictionaries(this);
		
		return dict;
	}
	
	@Transactional
	public void deleteItem (KID itemId, AuthData authData, EnvData env) throws KommetException
	{
		itemDao.delete(itemId, authData, env);
		env.initDictionaries(this);
	}
	
	@Transactional
	public void delete (KID dictId, AuthData authData, EnvData env) throws KommetException
	{
		// make sure no fields reference this dictionary
		FieldFilter filter = new FieldFilter();
		filter.setDictionaryId(dictId);
		
		List<Field> referencingFields = fieldDao.getFields(filter, appConfig, env); 
		if (!referencingFields.isEmpty())
		{
			List<String> enumFields = new ArrayList<String>();
			
			for (Field f : referencingFields)
			{
				enumFields.add(env.getType(f.getType().getKID()).getQualifiedName() + "." + f.getApiName());
			}
			
			throw new KommetException("Dictionary cannot be deleted because it is referenced by enumeration field(s): " + MiscUtils.implode(enumFields, ", "));
		}
		
		dao.delete(dictId, authData, env);
		env.initDictionaries(this);
	}

	@Transactional(readOnly = true)
	public Dictionary get(KID id, AuthData authData, EnvData env) throws KommetException
	{
		DictionaryFilter filter = new DictionaryFilter();
		filter.addDictionaryId(id);
		List<Dictionary> dictionaries = get(filter, authData, env);
		return dictionaries.isEmpty() ? null : dictionaries.get(0);
	}

	@Transactional
	public DictionaryItem save(DictionaryItem item, AuthData authData, EnvData env) throws KommetException
	{
		item = itemDao.save(item, authData, env);
		env.initDictionaries(this);
		return item;
	}
}