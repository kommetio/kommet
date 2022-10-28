/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.labels;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import kommet.auth.AuthData;
import kommet.basic.TextLabel;
import kommet.basic.ValidationRule;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.validationrules.ValidationRuleDao;
import kommet.data.validationrules.ValidationRuleFilter;
import kommet.env.EnvData;
import kommet.i18n.Locale;
import kommet.utils.MiscUtils;

@Service
public class TextLabelService
{
	@Inject
	TextLabelDao labelDao;
	
	@Inject
	ValidationRuleDao vrDao;
	
	@Transactional(readOnly = true)
	public List<TextLabel> get (TextLabelFilter filter, EnvData env) throws KommetException
	{
		return labelDao.get(filter, env);
	}
	
	@Transactional
	public TextLabel save (TextLabel label, AuthData authData, EnvData env) throws KommetException
	{
		// TODO use validation rules for this check
		if (label.getKey() != null)
		{
			if (StringUtils.containsWhitespace(label.getKey()))
			{
				throw new TextLabelException("Text label key may not contain whitespace characters");
			}
		}
		
		// before saving the new label, extract its old version to check if its key has changed
		if (label.getId() != null)
		{
			TextLabel oldLabel = labelDao.get(label.getId(), env);
			if (!oldLabel.getKey().equals(label.getKey()))
			{
				// check if the old key is not used anywhere
				checkUsedLabels(oldLabel.getKey(), false, env);
			}
		}
		
		label = labelDao.save(label, authData, env);
		
		// clear label cache
		initTextLabels(env);
		
		return label;
	}
	
	@Transactional(readOnly = true)
	public void initTextLabels(EnvData env) throws KommetException
	{	
		// get all text labels
		List<TextLabel> labels = labelDao.get(new TextLabelFilter(), env);
		
		TextLabelDictionary dict = new TextLabelDictionary();
		dict.setLastSynchronized((new Date()).getTime());
		
		for (TextLabel label : labels)
		{
			dict.addLabel(label);
		}
		
		env.setTextLabelDictionary(dict);
	}
	
	@Transactional(readOnly = true)
	public TextLabel get (KID labelId, EnvData env) throws KommetException
	{
		return labelDao.get(labelId, env);
	}

	@Transactional
	public TextLabel createLabel(String key, String value, Locale locale, AuthData authData, EnvData env) throws KommetException
	{	
		TextLabel label = new TextLabel();
		label.setKey(key);
		label.setValue(value);
		label.setLocale(locale);
		return save(label, authData, env);
	}

	@Transactional
	public void delete(KID id, AuthData authData, EnvData env) throws KommetException
	{
		checkUsedLabels(id, true, env);
		labelDao.delete(id, authData, env);
	}
	
	/**
	 * Checks if this text label is not used somewhere in the system by:
	 * <ul>
	 * <li>validation rules</li>
	 * </ul>
	 * @param labelId
	 * @param env
	 * @throws KommetException
	 */
	private void checkUsedLabels(KID labelId, boolean isDelete, EnvData env) throws KommetException
	{
		// find label
		TextLabel label = labelDao.get(labelId, env);
		checkUsedLabels(label.getKey(), isDelete, env);
	}

	/**
	 * Checks if this text label is not used somewhere in the system by:
	 * <ul>
	 * <li>validation rules</li>
	 * </ul>
	 * @param labelId
	 * @param env
	 * @throws KommetException
	 */
	private void checkUsedLabels(String labelKey, boolean isDelete, EnvData env) throws KommetException
	{		
		// find all validation rules that use this label
		ValidationRuleFilter filter = new ValidationRuleFilter();
		filter.addErrorMessageLabel(labelKey);
		List<ValidationRule> vrs = vrDao.get(filter, env); 
		if (!vrs.isEmpty())
		{
			List<String> vrNames = new ArrayList<String>();
			for (ValidationRule vr : vrs)
			{
				vrNames.add(vr.getName());
			}
			throw new ManipulatingReferencedLabelException("Trying to " + (isDelete ? "delete" : "change") + " text label used by the following validation rules: " + MiscUtils.implode(vrNames, ", "), TextLabelReference.VALIDATION_RULE);
		}
	}
}