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

import kommet.auth.AuthData;
import kommet.basic.FieldHistory;
import kommet.basic.FieldHistoryOperation;
import kommet.dao.FieldHistoryDao;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.datatypes.DataType;
import kommet.env.EnvData;
import kommet.filters.FieldHistoryFilter;

@Service
public class FieldHistoryService
{
	@Inject
	FieldHistoryDao dao;
	
	/**
	 * Logs an update of a simple property.
	 * @param field
	 * @param recordId
	 * @param oldValue
	 * @param newValue
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional
	public FieldHistory logFieldUpdate (Field field, KID recordId, Object oldValue, Object newValue, AuthData authData, EnvData env) throws KommetException
	{
		FieldHistory fh = new FieldHistory();
		fh.setFieldId(field.getKID());
		fh.setRecordId(recordId);
		
		DataType dt = field.getDataType();
		fh.setOldValue(oldValue != null ? dt.getStringValue(oldValue, authData.getLocale()) : null);
		fh.setNewValue(dt.getStringValue(newValue, authData.getLocale()));
		fh.setOperation(FieldHistoryOperation.UPDATE.toString());
		
		// all users are allowed to create field history records, because all users may be saving records
		// this is why we pass a flag to skip create permission check
		return dao.save(fh, false, false, true, false, authData, env);
	}
	
	/**
	 * Logs an update of a collection field.
	 * @param field
	 * @param recordId
	 * @param oldValue
	 * @param newValue
	 * @param operation
	 * @param authData
	 * @param env
	 * @return
	 * @throws KommetException
	 */
	@Transactional
	public FieldHistory logCollectionUpdate (Field field, KID recordId, KID oldValue, KID newValue, FieldHistoryOperation operation, AuthData authData, EnvData env) throws KommetException
	{
		FieldHistory fh = new FieldHistory();
		fh.setFieldId(field.getKID());
		fh.setRecordId(recordId);
		fh.setOldValue(oldValue != null ? oldValue.getId() : null);
		fh.setNewValue(newValue != null ? newValue.getId() : null);
		fh.setOperation(operation.toString());
		
		// all users are allowed to create field history records, because all users may be saving records
		// this is why we pass a flag to skip create permission check
		return dao.save(fh, false, false, true, false, authData, env);
	}
	
	@Transactional(readOnly = true)
	public List<FieldHistory> get (FieldHistoryFilter filter, EnvData env) throws KommetException
	{
		return dao.find(filter, env);
	}
}