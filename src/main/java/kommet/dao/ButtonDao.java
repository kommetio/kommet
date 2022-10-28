/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Repository;

import kommet.auth.AuthData;
import kommet.basic.Button;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.Field;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.env.EnvData;
import kommet.filters.ButtonFilter;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class ButtonDao extends GenericDaoImpl<Button>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public ButtonDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	public List<Button> get (ButtonFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new ButtonFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.BUTTON_API_NAME)).getKID(), authData);
		c.addProperty("name, label, labelKey, url, onClick, action.id, action.url, action.name, typeId, displayCondition");
		c.addStandardSelectProperties();
		
		c.createAlias("action", "action");
		
		if (filter.getTypeId() != null)
		{
			c.add(Restriction.eq("typeId", filter.getTypeId()));
		}
		
		if (filter.getButtonId() != null)
		{
			c.add(Restriction.eq(Field.ID_FIELD_NAME, filter.getButtonId()));
		}
		
		return getObjectProxyList(c.list(), env);
	}
	
	private static List<Button> getObjectProxyList(List<Record> records, EnvData env) throws KommetException
	{
		List<Button> buttons = new ArrayList<Button>();
		
		for (Record r : records)
		{
			buttons.add(new Button(r, env));
		}
		
		return buttons;
	}
}