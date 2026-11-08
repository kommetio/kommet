/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.dao;

import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;

import org.springframework.stereotype.Repository;

import kommet.auth.AuthData;
import kommet.basic.ApiToken;
import kommet.basic.RecordProxyType;
import kommet.basic.types.SystemTypes;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.data.Field;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.persistence.GenericDaoImpl;

@Repository
public class ApiTokenDao extends GenericDaoImpl<ApiToken>
{
	@Inject
	LocalEnvPersistenceInterface envPersistence;

	public ApiTokenDao()
	{
		super(RecordProxyType.STANDARD);
	}

	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return envPersistence;
	}

	public List<ApiToken> findForUser(KID userId, EnvData env) throws KommetException
	{
		Type type = env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.API_TOKEN_API_NAME));
		Criteria criteria = env.getSelectCriteria(type.getKID(), AuthData.getRootAuthData(env));
		criteria.addProperty("name, tokenPrefix, systemUser.id, expirationDate, isReadPermission, isWritePermission");
		criteria.addStandardSelectProperties();
		criteria.add(Restriction.eq("systemUser.id", userId));

		return toApiTokens(criteria.list(), env);
	}

	public ApiToken findByTokenHash(String tokenHash, EnvData env) throws KommetException
	{
		Type type = env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.API_TOKEN_API_NAME));
		Criteria criteria = env.getSelectCriteria(type.getKID(), AuthData.getRootAuthData(env));
		criteria.addProperty("name, tokenHash, tokenPrefix, systemUser.id, expirationDate, isReadPermission, isWritePermission");
		criteria.addStandardSelectProperties();
		criteria.add(Restriction.eq("tokenHash", tokenHash));
		criteria.setLimit(1);

		List<ApiToken> tokens = toApiTokens(criteria.list(), env);
		return tokens.isEmpty() ? null : tokens.get(0);
	}

	private List<ApiToken> toApiTokens(List<Record> records, EnvData env) throws KommetException
	{
		List<ApiToken> tokens = new ArrayList<ApiToken>();
		for (Record record : records)
		{
			tokens.add(new ApiToken(record, env));
		}
		return tokens;
	}
}