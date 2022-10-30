/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.uch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.jdbc.InvalidResultSetAccessException;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

import kommet.auth.AuthData;
import kommet.basic.RecordProxy;
import kommet.basic.RecordProxyType;
import kommet.basic.RecordProxyUtil;
import kommet.basic.UserCascadeHierarchy;
import kommet.basic.types.SystemTypes;
import kommet.dao.UserGroupAssignmentDao;
import kommet.dao.queries.Criteria;
import kommet.dao.queries.Restriction;
import kommet.dao.queries.SortDirection;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KeyPrefixException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.env.EnvData;
import kommet.filters.QueryResultOrder;
import kommet.integration.EnvPersistenceInterface;
import kommet.integration.LocalEnvPersistenceInterface;
import kommet.koll.compiler.KommetCompiler;
import kommet.persistence.GenericDaoImpl;
import kommet.utils.MiscUtils;

@Repository
public class UserCascadeHierarchyDao extends GenericDaoImpl<UserCascadeHierarchy>
{
	@Inject
	DataService dataService;
	
	@Inject
	LocalEnvPersistenceInterface envPersistence;
	
	@Inject
	UserGroupAssignmentDao ugaDao;
	
	// list of basic properties of a UCH record to be fetched in different fetch operations
	public static final Set<String> basicUchProperties = MiscUtils.toSet("id", "activeContextName", "activeContextRank", "localeName", "env", "profile.id", "contextUser.id", "userGroup.id");
	
	// list of advanced properties of a UCH record to be fetched in different fetch operations
	public static final Set<String> additionalUchProperties = MiscUtils.toSet("profile.name", "contextUser.userName", "userGroup.name");
	
	public UserCascadeHierarchyDao()
	{
		super(RecordProxyType.STANDARD);
	}
	
	@Override
	public EnvPersistenceInterface getEnvCommunication()
	{
		return this.envPersistence;
	}
	
	public List<UserCascadeHierarchy> get (UserCascadeHierarchyFilter filter, AuthData authData, EnvData env) throws KommetException
	{
		if (filter == null)
		{
			filter = new UserCascadeHierarchyFilter();
		}
		
		Criteria c = env.getSelectCriteria(env.getType(SystemTypes.getSystemTypeQualifiedName(SystemTypes.USER_CASCADE_HIERARCHY_API_NAME)).getKID(), authData);
		c.addProperties(basicUchProperties);
		c.addProperties(additionalUchProperties);
		c.createAlias("profile", "profile");
		c.createAlias("contextUser", "contextUser");
		c.createAlias("userGroup", "userGroup");
		c.addStandardSelectProperties();
		
		if (filter.getUchIds() != null && !filter.getUchIds().isEmpty())
		{
			c.add(Restriction.in("id", filter.getUchIds()));
		}
		
		if (filter.getOrder() != null && filter.getOrderBy() != null)
		{
			c.addOrderBy(filter.getOrder().equals(QueryResultOrder.ASC) ? SortDirection.ASC : SortDirection.DESC, filter.getOrderBy());
		}
		
		List<Record> records = c.list();
		List<UserCascadeHierarchy> uchs = new ArrayList<UserCascadeHierarchy>();
		for (Record r : records)
		{
			uchs.add(new UserCascadeHierarchy(r, env));
		}
		
		return uchs;
	}

	@SuppressWarnings("unchecked")
	public <T extends RecordProxy> T getSetting(Type type, Collection<String> fieldsToFetch, String discriminatorFieldName, Object discriminatorFieldValue, KommetCompiler compiler, AuthData contextAuthData, AuthData authData, EnvData env) throws KeyPrefixException, KommetException
	{
		// find field on type related to UCH
		Field uchField = UserCascadeHierarchyUtil.getUCHField(type, env);
				
		// get ids of records to be retrieved
		List<KID> recordIds = getRecordIdsForUCH(type, uchField, discriminatorFieldName, discriminatorFieldValue, contextAuthData, env);
		
		if (recordIds.isEmpty())
		{
			// if no setting records IDs where found, return null
			return null;
		}
		
		Set<String> selectedFields = new HashSet<String>();
		selectedFields.addAll(fieldsToFetch);
		selectedFields.add(Field.ID_FIELD_NAME);
		
		Criteria c = env.getSelectCriteria(type.getKID(), authData);
		c.addProperties(selectedFields);
		c.createAlias(uchField.getApiName(), uchField.getApiName());
		c.createAlias(uchField.getApiName() + ".contextUser", uchField.getApiName() + "_contextUser");
		c.createAlias(uchField.getApiName() + ".profile", uchField.getApiName() + "_profile");
		c.createAlias(uchField.getApiName() + ".userGroup", uchField.getApiName() + "_userGroup");
		
		// get only the first record, because ids are ordered by priority, starting with the most significant one
		c.add(Restriction.eq(Field.ID_FIELD_NAME, recordIds.get(0)));
		
		return (T)RecordProxyUtil.generateCustomTypeProxy(c.list().get(0), env, compiler);
		
		// get the first record, because it is the most significant one
		
		/*for (String uchProperty : basicUchProperties)
		{
			// add nested reference to UCH properties
			c.addProperty(uchField.getApiName() + "." + uchProperty);
		}
		
		// add conditions on context that will allow for finding only records relevant to the current user
		Restriction contextRestriction = Restriction.or(
				Restriction.eq(uchField.getApiName() + ".system", true),
				Restriction.eq(uchField.getApiName() + ".env", contextAuthData.getEnvId()),
				Restriction.eq(uchField.getApiName() + ".profile.id", contextAuthData.getProfile().getId()),
				Restriction.eq(uchField.getApiName() + ".contextUser.id", contextAuthData.getUser().getId()),
				Restriction.eq(uchField.getApiName() + ".localeName", contextAuthData.getLocale().name()));
		
		UserGroupAssignmentFilter filter = new UserGroupAssignmentFilter();
		filter.addChildUserId(contextAuthData.getUserId());
		// find user groups for this user
		List<UserGroupAssignment> userGroups = ugaDao.get(filter, authData, env);
		
		if (!userGroups.isEmpty())
		{
			Set<KID> userGroupIds = new HashSet<KID>();
			for (UserGroupAssignment uga : userGroups)
			{
				// TODO this only looks at groups one level up
				userGroupIds.add(uga.getParentGroup().getId());
			}
			contextRestriction.addSubrestriction(Restriction.in(uchField.getApiName() + ".userGroup.id", userGroupIds));
		}
		
		c.add(contextRestriction);
		
		// filter additionally by field
		if (discriminatorFieldName != null)
		{
			if (discriminatorFieldValue != null)
			{
				c.add(Restriction.eq(discriminatorFieldName, discriminatorFieldValue));
			}
			else
			{
				c.add(Restriction.isNull(discriminatorFieldName));
			}
		}
		
		List<Record> records = c.list();
		
		// map records by active context
		Map<String, Record> recordsByActiveContext = new HashMap<String, Record>();
		for (Record r : records)
		{
			recordsByActiveContext.put((String)r.getField(uchField.getApiName() + ".activeContextName"), r);
		}
		
		// find most relevant context
		if (recordsByActiveContext.containsKey("user"))
		{
			return (T)ObjectProxyUtil.generateCustomTypeProxy(recordsByActiveContext.get("user"), env, compiler);
		}
		else if (recordsByActiveContext.containsKey("user group"))
		{
			return (T)ObjectProxyUtil.generateCustomTypeProxy(recordsByActiveContext.get("user group"), env, compiler);
		}
		else if (recordsByActiveContext.containsKey("locale"))
		{
			return (T)ObjectProxyUtil.generateCustomTypeProxy(recordsByActiveContext.get("locale"), env, compiler);
		}
		else if (recordsByActiveContext.containsKey("profile"))
		{
			return (T)ObjectProxyUtil.generateCustomTypeProxy(recordsByActiveContext.get("profile"), env, compiler);
		}
		else if (recordsByActiveContext.containsKey("application"))
		{
			return (T)ObjectProxyUtil.generateCustomTypeProxy(recordsByActiveContext.get("application"), env, compiler);
		}
		else if (recordsByActiveContext.containsKey("environment"))
		{
			return (T)ObjectProxyUtil.generateCustomTypeProxy(recordsByActiveContext.get("environment"), env, compiler);
		}
		else if (recordsByActiveContext.containsKey("system"))
		{
			return (T)ObjectProxyUtil.generateCustomTypeProxy(recordsByActiveContext.get("system"), env, compiler);
		}
		else
		{
			return null;
		}*/
	}

	/**
	 * NOTE: the app parameter is not supported right now
	 * @param type
	 * @param uchField
	 * @param discriminatorFieldName
	 * @param discriminatorFieldValue
	 * @param app
	 * @param contextAuthData
	 * @param env
	 * @return
	 * @throws InvalidResultSetAccessException
	 * @throws KommetException
	 */
	private List<KID> getRecordIdsForUCH(Type type, Field uchField, String discriminatorFieldName, Object discriminatorFieldValue, AuthData contextAuthData, EnvData env) throws InvalidResultSetAccessException, KommetException
	{
		Type uchType = env.getType(KeyPrefix.get(KID.USER_CASCADE_HIERARCHY_PREFIX));
		StringBuilder query = new StringBuilder();
		query.append("SELECT st.kid AS matchingid, uch.activecontextrank as rank ");
		query.append("FROM ").append(uchType.getDbTable()).append(" AS uch INNER JOIN ").append(type.getDbTable()).append(" AS st ");
		query.append("ON uch.kid = st.").append(uchField.getDbColumn()).append(" WHERE (");
		
		// conditions on uch matching it with the current user
		query.append("uch.env = true OR ");
		query.append("uch.contextuser = '").append(contextAuthData.getUserId()).append("' OR ");
		query.append("uch.profile = '").append(contextAuthData.getProfile().getId()).append("' OR ");
		query.append("uch.localename = '").append(contextAuthData.getLocale().name()).append("' OR ");
		
		// query on all user groups to which the given user belongs
		query.append("uch.usergroup IN (SELECT groupid FROM get_user_groups('").append(contextAuthData.getUserId()).append("')");
		
		// close condition on user context, order from most important context to least important
		query.append("))");
		
		if (discriminatorFieldName != null)
		{
			Field discriminatorField = type.getField(discriminatorFieldName);
			
			if (discriminatorField == null)
			{
				throw new KommetException("Discriminator field " + discriminatorFieldName + " not found");
			}
			
			query.append(" AND ").append(discriminatorField.getDbColumn()).append(" = ").append(discriminatorField.getDataType().getPostgresValue(discriminatorFieldValue));
		}
				
		query.append(" order by uch.activecontextrank ASC");
		
		SqlRowSet rowSet = env.getJdbcTemplate().queryForRowSet(query.toString());
		List<KID> ids = new ArrayList<KID>();
		
		Set<Integer> ranks = new HashSet<Integer>();
		
		// highest rank mean the most important one, which is the one with the lowest number
		Integer highestRank = 1000000;
		Integer highestDuplicateRank = 1000000;
		boolean hasDuplicates = false;
		
		while (rowSet.next())
		{
			ids.add(KID.get(rowSet.getString("matchingid")));
			Integer rank = rowSet.getInt("rank");
			
			if (ranks.contains(rank))
			{
				hasDuplicates = true;
				
				// two settings found with the same rank - this should not happen
				// throw new AmbiguousUserCascadeHierarchySettingException();
				if (rank < highestDuplicateRank)
				{
					highestDuplicateRank = rank;
				}
			}
			
			ranks.add(rank);
			
			if (rank < highestRank)
			{
				highestRank = rank;
			}
		}
		
		// if there are duplicate ranks and the conflict happens at the highest applicable level
		// (i.e. there is no setting with a higher rank than that of conflicting ranks)
		// then throw exception.
		// E.g. if there are two setting with rank 200, we will throw an exception.
		// But if there are two setting with rank 200 and one setting with a higher rank 100, then this
		// rank overrides the conflicting 200's and we're good, so no exception is thrown.
		if (hasDuplicates && highestDuplicateRank <= highestRank)
		{
			throw new AmbiguousUserCascadeHierarchySettingException();
		}
		
		return ids;
	}
}