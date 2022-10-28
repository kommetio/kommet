/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html
 */

package kommet.koll;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import kommet.auth.AuthData;
import kommet.auth.LoginHistoryService;
import kommet.auth.LoginState;
import kommet.auth.UserService;
import kommet.basic.DocTemplate;
import kommet.basic.FieldHistory;
import kommet.basic.FieldHistoryOperation;
import kommet.basic.File;
import kommet.basic.Notification;
import kommet.basic.Profile;
import kommet.basic.RecordProxy;
import kommet.basic.RecordProxyException;
import kommet.basic.RecordProxyUtil;
import kommet.basic.User;
import kommet.basic.WebResource;
import kommet.basic.keetle.LayoutService;
import kommet.basic.keetle.PageData;
import kommet.basic.keetle.ViewService;
import kommet.comments.CommentService;
import kommet.dao.dal.DALException;
import kommet.dao.queries.QueryResult;
import kommet.data.DataAccessUtil;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KeyPrefixException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.sharing.SharingService;
import kommet.dataexport.DataExportService;
import kommet.docs.DocTemplateService;
import kommet.emailing.Attachment;
import kommet.emailing.EmailAccount;
import kommet.emailing.EmailException;
import kommet.emailing.EmailMessage;
import kommet.emailing.EmailService;
import kommet.emailing.Recipient;
import kommet.env.EnvAlreadyExistsException;
import kommet.env.EnvData;
import kommet.env.EnvService;
import kommet.files.FileFilter;
import kommet.files.FileService;
import kommet.http.HttpService;
import kommet.i18n.InternationalizationService;
import kommet.koll.compiler.KommetCompiler;
import kommet.notifications.NotificationException;
import kommet.notifications.NotificationService;
import kommet.services.FieldHistoryService;
import kommet.services.SystemSettingService;
import kommet.services.UserGroupService;
import kommet.services.ViewResourceService;
import kommet.sysctx.CommentServiceProxy;
import kommet.sysctx.SharingServiceProxy;
import kommet.sysctx.SystemSettingsServiceProxy;
import kommet.sysctx.UserCascadeSettingService;
import kommet.sysctx.UserGroupServiceProxy;
import kommet.testing.TestException;
import kommet.testing.TestResults;
import kommet.testing.TestService;
import kommet.transactions.TransactionManager;
import kommet.uch.UserCascadeHierarchyService;
import kommet.utils.AppConfig;
import kommet.utils.MiscUtils;
import kommet.utils.PropertyUtilException;
import kommet.utils.StringUtils;
import kommet.utils.UrlUtil;
import kommet.web.controllers.LoginController;

/**
 * This is a service that gathers all methods that will be made accessible to KOLL users.
 * It will be then injected into KOLL code.
 * 
 * @author Radek Krawiec
 *
 */
public class SystemContext implements CurrentAuthDataAware
{
	private EnvData env;
	private KommetCompiler compiler;
	private AppConfig appConfig;
	private EmailService emailService;
	private SharingService sharingService;
	private AuthData authData;
	private DataService dataService;
	private TransactionManager transactionManager;
	private DocTemplateService docTemplateService;
	private FieldHistoryService fieldHistoryService;
	private NotificationService notificationService;
	private UserGroupServiceProxy userGroupServiceProxy;
	private SharingServiceProxy sharingServiceProxy;
	private CommentServiceProxy commentServiceProxy;
	private SystemSettingsServiceProxy systemSettingServiceProxy;
	private UserCascadeSettingService userSettingService;
	private UserCascadeHierarchyService uchService;
	private LoginHistoryService lhService;
	private UserService userService;
	private InternationalizationService i18n;
	private ViewService viewService;
	private LayoutService layoutService;
	private FileService fileService;
	private ViewResourceService viewResourceService;
	private HttpService http;
	private EnvService envService;
	private DataExportService exportService;
	private boolean isUserAgnostic = false;
	private TestService testService;
	
	// if this is a test class, this var will store test results
	private TestResults testResults;
	
	public UserGroupServiceProxy getUserGroupService()
	{
		return this.userGroupServiceProxy;
	}
	
	public SharingServiceProxy getSharingService()
	{
		return this.sharingServiceProxy;
	}
	
	public SystemContext (KommetCompiler compiler, AppConfig appConfig, UserCascadeHierarchyService uchService, HttpService httpService, EmailService emailService,
						SystemSettingService ssService, SharingService sharingService, DataService dataService, DocTemplateService docTemplateService, 
						FieldHistoryService fhService, NotificationService notificationService, UserGroupService userGroupService, CommentService commentService,
						InternationalizationService i18n, LoginHistoryService lhService, UserService userService,
						ViewResourceService viewResourceService, ViewService viewService, LayoutService layoutService, EnvService envService, TestService testService, FileService fileService, DataExportService exportService,
						AuthData authData, EnvData env, TransactionManager transactionManager)
	{
		this.env = env;
		this.compiler = compiler;
		this.appConfig = appConfig;
		this.emailService = emailService;
		this.sharingService = sharingService;
		this.docTemplateService = docTemplateService;
		this.fieldHistoryService = fhService;
		this.authData = authData;
		this.dataService = dataService;
		this.notificationService = notificationService;
		this.userGroupServiceProxy = new UserGroupServiceProxy(userGroupService, this.dataService, this, this.env);
		this.sharingServiceProxy = new SharingServiceProxy(sharingService, this, env);
		this.commentServiceProxy = new CommentServiceProxy(commentService, this, env);
		this.systemSettingServiceProxy = new SystemSettingsServiceProxy(ssService, this, env);
		this.userSettingService = new UserCascadeSettingService(uchService, authData, env);
		this.fileService = fileService;
		this.i18n = i18n;
		this.uchService = uchService;
		this.lhService = lhService;
		this.userService = userService;
		this.viewResourceService = viewResourceService;
		this.layoutService = layoutService;
		this.viewService = viewService;
		this.http = httpService;
		this.envService = envService;
		this.testService = testService;
		this.exportService = exportService;
		
		// if auth data not passed, the system context will obtain it automatically each time it is referenced
		this.isUserAgnostic = authData == null;
		
		this.transactionManager = transactionManager;
	}
	
	public TestResults runTest (String testClassName, String method) throws KommetException
	{
		return testService.run(testClassName, MiscUtils.toList(method), env);
	}
	
	public List<Map<String, Object>> queryMap (String query, List<String> columns)
	{
		SqlRowSet rowSet = env.getJdbcTemplate().queryForRowSet(query);
		
		List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
		
		while (rowSet.next())
		{
			Map<String, Object> r = new HashMap<String, Object>();
			
			for (String col : columns)
			{
				r.put(col, rowSet.getObject(col));
			}
			
			results.add(r);
		}
		
		return results;
	}
	
	public void prepareTestEnv() throws KommetException
	{
		testService.spanTestEnv(env);
	}
	
	public EnvData createEnv(String envName, KID envId, boolean isInitEnv, String template) throws KommetException
	{
		// only admin user can create envs
		try
		{
			return envService.createEnv(envName, envId, isInitEnv, false, template);
		}
		catch (DataAccessException e)
		{
			e.printStackTrace();
			throw new KommetException("Error setting up environment: " + e.getMessage());
		}
		catch (PropertyUtilException e)
		{
			e.printStackTrace();
			throw new KommetException("Error setting up environment: " + e.getMessage());
		}
		catch (EnvAlreadyExistsException e)
		{
			throw e;
		}
	}
	
	public String getWebResourceUrl(String name)
	{
		WebResource res = env.getWebResource(name);
		return res != null ? UrlUtil.SYSTEM_ACTION_URL_PREFIX + "/download/" + res.getFile().getId() : null;
	}
	
	public Record getRecord(RecordProxy proxy, int depth) throws KommetException
	{
		if (proxy == null)
		{
			return null;
		}
		
		return RecordProxyUtil.generateRecord(proxy, env.getType(MiscUtils.envToUserPackage(proxy.getClass().getName(), env)), depth, env);
	}
	
	public Record getRecord (RecordProxy proxy) throws KommetException
	{
		return getRecord(proxy, 3);
	}
	
	public Notification createNotification (String title, String text, KID assigneeId) throws NotificationException
	{
		User assignee = null;
		
		try
		{
			assignee = new User();
			assignee.setId(assigneeId);
		}
		catch (RecordProxyException e)
		{
			throw new NotificationException("Error creating notification - could not instantiate user");
		}
		
		try
		{
			Notification n = new Notification();
			n.setAssignee(assignee);
			n.setTitle(title);
			n.setText(text);
			return notificationService.save(n, currentAuthData(), this.env);
		}
		catch (KommetException e)
		{
			throw new NotificationException("Could not save notification. Nested: " + e.getMessage());
		}
	}
	
	public Record associate(KID assocFieldId, KID recordId, KID associatedRecordId) throws KommetException
	{
		return dataService.associate(assocFieldId, recordId, associatedRecordId, currentAuthData(), this.env);
	}
	
	public void unassociate(KID associationFieldId, KID recordId, KID associatedRecordId, AuthData authData, EnvData env) throws KommetException
	{
		dataService.unassociate(associationFieldId, recordId, associatedRecordId, false, authData, this.env);
	}
	
	/**
	 * Shares record with a user or group
	 * @param recordId
	 * @param assigneeId
	 * @param reason
	 * @throws KommetException
	 */
	public void share (KID recordId, KID assigneeId, String reason) throws KommetException
	{
		if (assigneeId.getId().startsWith(KID.USER_PREFIX))
		{
			sharingService.shareRecord(recordId, assigneeId, currentAuthData(), reason, true, this.env);
		}
		else if (assigneeId.getId().startsWith(KID.USER_GROUP_PREFIX))
		{
			sharingService.shareRecordWithGroup(recordId, assigneeId, currentAuthData(), reason, true, this.env);
		}
		else
		{
			throw new KommetException("Cannot share record. Invalid user/group ID " + assigneeId);
		}
	}
	
	public void unshare (KID recordId, KID userId) throws KommetException
	{
		sharingService.unshareRecord(recordId, userId, currentAuthData(), this.env);
	}
	
	public boolean canViewRecord (KID recordId, KID userId) throws KommetException
	{
		return sharingService.canViewRecord(recordId, userId, this.env);
	}
	
	public boolean canEditRecord (KID recordId, KID userId) throws KommetException
	{
		return sharingService.canEditRecord(recordId, userId, this.env);
	}
	
	public void share (KID recordId, KID assigneeId, boolean edit, boolean delete, String reason) throws KommetException
	{
		if (assigneeId.getId().startsWith(KID.USER_PREFIX))
		{
			sharingService.shareRecord(recordId, assigneeId, edit, delete, currentAuthData(), reason, true, this.env);
		}
		else if (assigneeId.getId().startsWith(KID.USER_GROUP_PREFIX))
		{
			sharingService.shareRecordWithGroup(recordId, assigneeId, edit, delete, reason, true, currentAuthData(), this.env);
		}
		else
		{
			throw new KommetException("Cannot share record. Invalid user/group ID " + assigneeId);
		}
	}
	
	public void share (KID recordId, KID assigneeId, boolean edit, boolean delete, String reason, boolean checkForExistingSharings) throws KommetException
	{
		if (assigneeId.getId().startsWith(KID.USER_PREFIX))
		{
			sharingService.shareRecord(recordId, assigneeId, edit, delete, currentAuthData(), reason, checkForExistingSharings, this.env);
		}
		else if (assigneeId.getId().startsWith(KID.USER_GROUP_PREFIX))
		{
			sharingService.shareRecordWithGroup(recordId, assigneeId, edit, delete, reason, true, currentAuthData(), this.env);
		}
		else
		{
			throw new KommetException("Cannot share record. Invalid user/group ID " + assigneeId);
		}
	}
	
	public <T extends RecordProxy> T save (T proxy) throws KommetException
	{
		return dataService.save(proxy, currentAuthData(), env);
	}
	
	public <T extends RecordProxy> T save (T proxy, boolean skipTriggers) throws KommetException
	{
		return dataService.save(proxy, currentAuthData(), skipTriggers, false, env);
	}
	
	public <T extends RecordProxy> T save (T proxy, boolean skipTriggers, boolean skipSharing) throws KommetException
	{
		return dataService.save(proxy, currentAuthData(), skipTriggers, skipSharing, env);
	}
	
	public <T extends RecordProxy> T save (T proxy, boolean skipTriggers, boolean skipSharing, boolean asRoot) throws KommetException
	{
		AuthData authData = asRoot ? AuthData.getRootAuthData(env) : currentAuthData();
		return dataService.save(proxy, authData, skipTriggers, skipSharing, env);
	}
	
	public Record save (Record record) throws KommetException
	{
		return dataService.save(record, currentAuthData(), env);
	}
	
	/**
	 * Read emails from inbox
	 * @param acc
	 * @param startDate
	 * @return
	 * @throws KommetException
	 */
	public List<EmailMessage> readEmails (EmailAccount acc, Date startDate) throws KommetException
	{
		return this.emailService.readEmails(acc, startDate);
	}
	
	public EmailMessage sendEmail (String subject, String recipient, String content) throws EmailException
	{
		return this.emailService.sendEmail(subject, recipient, content, null);
	}
	
	public EmailMessage sendEmail (String subject, String recipientAddress, String content, String htmlContent, EmailAccount acc) throws EmailException
	{
		return this.emailService.sendEmail(subject, MiscUtils.toList(new Recipient(recipientAddress)), content, htmlContent, null, null, acc);
	}
	
	public EmailMessage sendEmail (String subject, String recipientAddress, String content, String htmlContent, List<Attachment> attachments, EmailAccount acc) throws EmailException
	{
		return this.emailService.sendEmail(subject, MiscUtils.toList(new Recipient(recipientAddress)), content, htmlContent, attachments, null, acc);
	}
	
	public EmailMessage sendEmail (String subject, String recipient, String content, String htmlContent) throws EmailException
	{
		return this.emailService.sendEmail(subject, recipient, content, htmlContent);
	}
	
	public <T extends RecordProxy> T queryUniqueResult (String query) throws KommetException
	{
		return queryUniqueResult(query, true);
	}

	public <T extends RecordProxy> T queryUniqueResult (String query, boolean applySharings) throws KommetException
	{
		List<T> results = query(query, applySharings);
		if (results.size() == 1)
		{
			return results.get(0);
		}
		else if (results.isEmpty())
		{
			return null;
		}
		else
		{
			throw new DALException("Expected one results in call to queryUniqueResult, but got " + results.size());
		}
	}

	/**
	 * Queries records using DAL.
	 * @param <T>
	 * @param query DAL query
	 * @return
	 * @throws KommetException
	 */
	public <T extends RecordProxy> List<T> query (String query) throws KommetException
	{
		return query(query, true);
	}
	
	public <T extends RecordProxy> void delete (List<T> records) throws KommetException
	{
		dataService.delete(records, currentAuthData(), env);
	}
	
	// TODO write unit tests for this method
	public void delete (KID id) throws KommetException
	{
		dataService.deleteRecord(id, currentAuthData(), env);
	}
	
	public void delete (KID id, boolean skipSharing) throws KommetException
	{
		dataService.deleteRecord(id, skipSharing ? AuthData.getRootAuthData(getEnv()) : currentAuthData(), env);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends RecordProxy> List<T> query (String query, boolean applySharings) throws KommetException
	{
		// if null is passed instead of auth data, no sharing check will be performed while retrieving records
		List<Record> records = env.getSelectCriteriaFromDAL(query, applySharings ? currentAuthData() : null).list();
		List<T> proxies = new ArrayList<T>();
		
		for (Record record : records)
		{
			proxies.add((T)RecordProxyUtil.generateCustomTypeProxy(record, env, compiler));
		}
		
		return proxies;
	}
	
	public List<QueryResult> queryAggr (String query) throws KommetException
	{
		return queryAggr(query, true);
	}
	
	public List<QueryResult> queryAggr (String query, boolean applySharings) throws KommetException
	{
		List<QueryResult> aggrResults = new ArrayList<QueryResult>();
		
		// if null is passed instead of auth data, no sharing check will be performed while retrieving records
		List<Record> records = env.getSelectCriteriaFromDAL(query, applySharings ? currentAuthData() : null).list();
		
		for (Record r : records)
		{
			if (r instanceof QueryResult)
			{
				aggrResults.add((QueryResult)r);
			}
			else
			{
				throw new KommetException("Cannot cast query result to QuertResult. Evidently the DAL query is not an aggregate query");
			}
		}
		
		return aggrResults;
	}
	
	/**
	 * Retrieves record by its ID.
	 * @param <T>
	 * @param id The id of the record
	 * @return The retrieved record with all readable fields initialized, or null, if record does not exist or is not accessible.
	 * @throws KommetException
	 */
	public <T extends RecordProxy> T get (KID id, String ... fields) throws KommetException
	{
		return get (id, true, fields);
	}

	/**
	 * Retrieves record by its ID.
	 * @param <T>
	 * @param id The id of the record
	 * @return The retrieved record with all readable fields initialized, or null, if record does not exist or is not accessible.
	 * @throws KommetException
	 */
	public <T extends RecordProxy> T get (KID id, boolean applySharings, String ... fields) throws KommetException
	{
		if (id == null)
		{
			throw new KommetException("Cannot retrieve record by null ID");
		}
		
		Type type = getEnv().getTypeByRecordId(id);
		StringBuilder query = new StringBuilder("SELECT ");
		
		if (fields == null || fields.length == 0)
		{
			throw new KommetException("Field list to query is empty");
		}
		
		query.append(MiscUtils.implode(DataAccessUtil.getFieldsNamesForDisplay(type, authData, Arrays.asList(fields), env), ", "));
		query.append(" FROM ").append(type.getQualifiedName());
		query.append(" WHERE id = '" + id + "' LIMIT 1");
		
		List<T> proxies = query(query.toString(), applySharings);
		return !proxies.isEmpty() ? proxies.get(0) : null;
	}
	
	/**
	 * TODO think if this method should be made available to users through SystemContext - is it
	 * not giving them too much control?
	 * 
	 * Logs field update.
	 * @param field
	 * @param recordId
	 * @param oldValue
	 * @param newValue
	 * @return
	 * @throws KommetException
	 */
	public FieldHistory logFieldUpdate (Field field, KID recordId, Object oldValue, Object newValue) throws KommetException
	{
		return fieldHistoryService.logFieldUpdate(field, recordId, oldValue, newValue, currentAuthData(), env);
	}
	
	public FieldHistory logCollectionUpdate (Field field, KID recordId, KID oldValue, KID newValue, FieldHistoryOperation operation) throws KommetException
	{
		return fieldHistoryService.logCollectionUpdate(field, recordId, oldValue, newValue, operation, currentAuthData(), env);
	}
	
	public LoginState login (String userName, PageData pageData) throws KommetException
	{
		if (!StringUtils.hasText(userName))
		{
			throw new KommetException("User to log in is null");
		}
		
		// find user
		User user = userService.get(userName, env);
		
		if (user == null)
		{
			throw new KommetException("User " + userName + " not found");
		}
		
		// make sure users cannot authenticate root admins
		if (user.getProfile().getId().getId().equals(Profile.ROOT_ID))
		{
			throw new KommetException("Cannot use manual log in for root profile");
		}
		
		return LoginController.setLogInState(user, null, pageData.getHttpRequest(), i18n, "EN_US", lhService, uchService, userService, appConfig, viewService, layoutService, viewResourceService, env);
	}
	
	public String fileToBase64 (KID fileId) throws KommetException
	{
		return fileToBase64(fileId, true);
	}
	
	public String fileToBase64 (KID fileId, boolean isApplySharings) throws KommetException
	{
		// get file from database
		FileFilter filter = new FileFilter();
		filter.addId(fileId);
		List<File> files = fileService.find(filter, true, true, isApplySharings ? currentAuthData() : null, env);
		
		if (files.isEmpty())
		{
			// TODO add a check for valid file ID
			throw new KommetException("File not found");
		}
		
		File file = files.get(0);
		java.io.File systemFile = new java.io.File(appConfig.getFileDir() + "/" + file.getLatestRevision().getPath());
		if (!systemFile.exists())
		{
			throw new KommetException("File not found on server");
		}
		
		try
		{
			byte[] encoded = Base64.getEncoder().encode(FileUtils.readFileToByteArray(systemFile));
		    return new String(encoded, StandardCharsets.US_ASCII);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			throw new KommetException("Error reading system file: " + e.getMessage());
		}
	}
	
	/**
	 * Returns a document template with the given name.
	 * @param name Name of the template
	 * @return
	 * @throws KommetException
	 */
	public DocTemplate getDocTemplate (String name) throws KommetException
	{
		return this.docTemplateService.getByName(name, getEnv());
	}

	public void setEnv(EnvData env)
	{
		this.env = env;
	}

	public EnvData getEnv()
	{
		return env;
	}
	
	public AuthData getAuthData()
	{
		return this.authData;
	}
	
	public KommetCompiler getCompiler()
	{
		return this.compiler;
	}

	public TransactionManager getTransactionManager()
	{
		return transactionManager;
	}

	public CommentServiceProxy getCommentService()
	{
		return commentServiceProxy;
	}

	public SystemSettingsServiceProxy getSystemSettingService()
	{
		return systemSettingServiceProxy;
	}

	public HttpService getHttp()
	{
		return http;
	}

	public UserCascadeSettingService getUserSettingService()
	{
		return userSettingService;
	}

	public boolean isUserAgnostic()
	{
		return isUserAgnostic;
	}
	
	public AuthData currentAuthData()
	{
		return this.authData != null ? this.authData : this.env.currentAuthData();
	}
	
	public String getFileDir() throws KommetException
	{
		return this.appConfig != null ? this.appConfig.getFileDir() : null;
	}

	public TestResults getTestResults()
	{
		return testResults;
	}

	public void setTestResults (TestResults testResults)
	{
		this.testResults = testResults;
	}
	
	public void assertEquals (String msg, boolean expected, boolean actual) throws TestException
	{
		if (expected != actual)
		{
			if (this.testResults == null)
			{
				throw new TestException("TestResults variable not initialized on system context");
			}
			
			this.testResults.addError("<empty>", "<empty>", msg);
		}
	}
	
	public Type getTypeByRecordId (KID recordId) throws KommetException
	{
		return env.getTypeByRecordId(recordId);
	}
	
	public Type getTypeByPrefix (String prefix) throws KeyPrefixException, KommetException
	{
		return env.getType(KeyPrefix.get(prefix));
	}
	
	public KID exportDataToPdf (Map<String, Object> data, String fileName) throws KommetException
	{
		File file = this.exportService.exportToPdfFile(data, fileName, currentAuthData(), getEnv());
		return file.getId();
	}
	
	public KID exportDataToXlsx (Map<String, Object> data, String fileName, String sheetName) throws KommetException
	{
		File file = this.exportService.exportToXlsxFile(data, fileName, sheetName, currentAuthData(), getEnv());
		return file.getId();
	}
}