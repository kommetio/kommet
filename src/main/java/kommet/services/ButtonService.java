/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.services;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.Button;
import kommet.dao.ButtonDao;
import kommet.data.FieldValidationException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.env.EnvData;
import kommet.filters.ButtonFilter;
import kommet.utils.ValidationUtil;

@Service
public class ButtonService
{
	@Inject
	ButtonDao dao;
	
	@Transactional
	public Button save (Button btn, AuthData authData, EnvData env) throws KommetException
	{
		if (!ValidationUtil.isValidOptionallyQualifiedResourceName(btn.getName()))
		{
			throw new FieldValidationException("Invalid button name " + btn.getName());
		}
		
		if (!StringUtils.hasText(btn.getLabel()))
		{
			throw new FieldValidationException("Button label is empty");
		}
		
		btn = dao.save(btn, authData, env);
		env.initCustomTypeButtons(this);
		return btn;
	}
	
	@Transactional
	public void delete (KID btnId, AuthData authData, EnvData env) throws KommetException
	{
		dao.delete(btnId, authData, env);
		env.initCustomTypeButtons(this);
	}
	
	@Transactional(readOnly = true)
	public List<Button> get (ButtonFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		return dao.get(filter, authData, env);
	}
	
	@Transactional(readOnly = true)
	public Button get (KID id, AuthData authData, EnvData env) throws KommetException
	{
		ButtonFilter filter = new ButtonFilter();
		filter.setButtonId(id);
		List<Button> buttons = dao.get(filter, authData, env);
		return buttons.isEmpty() ? null : buttons.get(0);
	}
}