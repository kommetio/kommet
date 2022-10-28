/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import kommet.auth.AuthData;
import kommet.auth.AuthUtil;
import kommet.auth.PermissionService;
import kommet.auth.ProfileService;
import kommet.auth.UserService;
import kommet.basic.BasicSetupService;
import kommet.basic.Comment;
import kommet.basic.Profile;
import kommet.basic.User;
import kommet.basic.UserGroup;
import kommet.basic.UserRecordSharing;
import kommet.basic.types.SystemTypes;
import kommet.comments.CommentService;
import kommet.dao.FieldFilter;
import kommet.dao.dal.InsufficientPrivilegesException;
import kommet.data.DataService;
import kommet.data.Field;
import kommet.data.KeyPrefix;
import kommet.data.KeyPrefixException;
import kommet.data.NotNullConstraintViolationException;
import kommet.data.KID;
import kommet.data.KommetException;
import kommet.data.Record;
import kommet.data.Type;
import kommet.data.datatypes.InverseCollectionDataType;
import kommet.data.datatypes.TypeReference;
import kommet.data.sharing.GroupRecordSharingDao;
import kommet.data.sharing.SharingService;
import kommet.data.sharing.UserRecordSharingDao;
import kommet.data.sharing.UserRecordSharingFilter;
import kommet.env.EnvData;
import kommet.koll.compiler.KommetCompiler;
import kommet.services.UserGroupService;
import kommet.tests.BaseUnitTest;
import kommet.tests.TestDataCreator;
import kommet.tests.harness.CompanyAppDataSet;
import kommet.tests.harness.StudentCourseDataSet;
import kommet.tests.harness.UserGroupHierarchyDataSet;
import kommet.utils.AppConfig;

public class SharingTest extends BaseUnitTest
{
	@Inject
	TestDataCreator dataHelper;
	
	@Inject
	BasicSetupService basicSetupService;
	
	@Inject
	DataService dataService;
	
	@Inject
	SharingService sharingService;
	
	@Inject
	ProfileService profileService;
	
	@Inject
	AppConfig appConfig;
	
	@Inject
	UserService userService;
	
	@Inject
	UserGroupService userGroupService;
	
	@Inject
	PermissionService permissionService;
	
	@Inject
	UserRecordSharingDao ursDao;
	
	@Inject
	GroupRecordSharingDao grsDao;
	
	@Inject
	KommetCompiler compiler;
	
	@Inject
	CommentService commentService;
	
	@Test
	public void testSharingOnNestedProperties() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		CompanyAppDataSet dataSet = CompanyAppDataSet.getInstance(dataService, env);
		
		// make sure field company is set on type Employee
		Field companyField = env.getType(dataSet.getEmployeeType().getKID()).getField("company");
		assertNotNull(companyField);
		assertNotNull(((TypeReference)companyField.getDataType()).getType());
		KID companyTypeId = ((TypeReference)companyField.getDataType()).getTypeId();
		assertNotNull(companyTypeId);
		assertNotNull(env.getType(companyTypeId));

		// insert some companies
		Record company1 = dataService.save(dataSet.getTestCompany("company-1", null), env);
		Record company2 = dataService.save(dataSet.getTestCompany("company-2", null), env);
		List<Record> companies = new ArrayList<Record>();
		companies.add(company1);
		companies.add(company2);
		
		// insert some employees
		Record employee1 = dataService.save(dataSet.getTestEmployee("first name 1", "last name 1", "middle name 1", company1, null), env);
		Record employee2 = dataService.save(dataSet.getTestEmployee("first name 2", "last name 2", "middle name 2", company2, null), env);
		
		// create new non-admin profile
		// create profile
		Profile profile = new Profile();
		profile.setName("RestrictedPermissionsProfile");
		profile.setLabel("RestrictedPermissionsProfile");
		profile.setSystemProfile(false);
		profile = profileService.save(profile, dataHelper.getRootAuthData(env), env);
		
		assertFalse(Profile.ROOT_ID.equals(profile.getId().getId()));
		
		// give this profile restricted access to company and employee
		permissionService.setTypePermissionForProfile(profile.getId(), dataSet.getCompanyType().getKID(), true, true, true, true, false, false, false, dataHelper.getRootAuthData(env), env);
		permissionService.setTypePermissionForProfile(profile.getId(), dataSet.getEmployeeType().getKID(), true, true, true, true, false, false, false, dataHelper.getRootAuthData(env), env);
		
		// create user
		User user = new User();
		user.setProfile(profile);
		user.setUserName("test");
		user.setEmail("test@kolmu.com");
		user.setPassword("test");
		user.setTimezone("GMT");
		user.setLocale("EN_US");
		user.setIsActive(true);
		
		user = userService.save(user, dataHelper.getRootAuthData(env), env);
		assertNotNull(user.getId());
		
		AuthData adminAuthData = dataHelper.getRootAuthData(env);
		
		// root user should always be able to edit records
		assertTrue(AuthUtil.canEditRecord(employee1.getKID(), adminAuthData, sharingService, env));
		assertTrue(AuthUtil.canEditRecord(employee2.getKID(), adminAuthData, sharingService, env));
		
		assertTrue(sharingService.canEditRecord(employee1.getKID(), adminAuthData.getUserId(), env));
		assertTrue(sharingService.canEditRecord(employee2.getKID(), adminAuthData.getUserId(), env));
		
		// share employee1 with this user, but not company1 associated with this employee
		sharingService.shareRecord(employee1.getKID(), user.getId(), dataHelper.getRootAuthData(env), "Some reason", true, env);
		
		AuthData userAuthData = new AuthData(user, env, permissionService, compiler);
		userAuthData.setUser(user);
		
		// init all user permissions
		userAuthData.initUserPermissions(env);
		
		// check edit sharing
		assertTrue(sharingService.canEditRecord(employee1.getKID(), user.getId(), env));
		assertFalse(sharingService.canEditRecord(employee2.getKID(), user.getId(), env));
		
		// now query the employee as this user
		List<Record> employees = env.getSelectCriteriaFromDAL("select id, company.id, company.name from " + dataSet.getEmployeeType().getQualifiedName() + " where id = '" + employee1.getKID().getId() + "'", userAuthData).list();
		assertEquals(1, employees.size());
		assertEquals(employee1.getKID(), employees.get(0).getKID());
		assertTrue(employees.get(0).isSet("company"));
		assertNull(employees.get(0).getField("company"));
		assertNull(employees.get(0).getField("company.id"));
		assertNull(employees.get(0).getField("company.name")); 
		
		// now revoke edit permission on record
		sharingService.shareRecord(employee1.getKID(), user.getId(), false, true, adminAuthData, "Some reason", true, env);
		employees = env.getSelectCriteriaFromDAL("select id, company.id, company.name from " + dataSet.getEmployeeType().getQualifiedName() + " where id = '" + employee1.getKID().getId() + "'", userAuthData).list();
		assertEquals(1, employees.size());
		
		// check edit sharing
		assertFalse(sharingService.canEditRecord(employee1.getKID(), user.getId(), env));
		assertFalse(sharingService.canEditRecord(employee2.getKID(), user.getId(), env));
		
		// check view sharing
		assertTrue(sharingService.canViewRecord(employee1.getKID(), user.getId(), env));
		assertFalse(sharingService.canViewRecord(employee2.getKID(), user.getId(), env));
		
		// check if admin can edit
		assertTrue(sharingService.canEditRecord(employee1.getKID(), adminAuthData.getUserId(), env));
		assertTrue(AuthUtil.canEditRecord(employee1.getKID(), adminAuthData, sharingService, env));
		
		// now remove all sharings on employee1
		sharingService.unshareRecord(employee1.getKID(), user.getId(), adminAuthData, env);
		
		// check edit sharing
		assertFalse(sharingService.canEditRecord(employee1.getKID(), user.getId(), env));
		assertFalse(sharingService.canEditRecord(employee2.getKID(), user.getId(), env));
		
		// check view sharing
		assertFalse(sharingService.canViewRecord(employee1.getKID(), user.getId(), env));
	}

	/**
	 * This method tests that when an association field is queried, and user has access to only
	 * some associated records, the returned collection will contain only them.
	 * @throws KommetException
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testAssociationWithRestrictedSharing() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		
		StudentCourseDataSet dataSet = StudentCourseDataSet.getInstance(dataService, env);
		
		// create new non-admin profile
		Profile profile = new Profile();
		profile.setName("RestrictedPermissionsProfile");
		profile.setLabel("RestrictedPermissionsProfile");
		profile.setSystemProfile(false);
		profile = profileService.save(profile, dataHelper.getRootAuthData(env), env);
		
		// give this profile restricted access to company and employee
		permissionService.setTypePermissionForProfile(profile.getId(), dataSet.getStudentType().getKID(), true, true, true, true, false, false, false, dataHelper.getRootAuthData(env), env);
		permissionService.setTypePermissionForProfile(profile.getId(), dataSet.getCourseType().getKID(), true, true, true, true, false, false, false, dataHelper.getRootAuthData(env), env);
		permissionService.setTypePermissionForProfile(profile.getId(), dataSet.getEnrollmentType().getKID(), true, true, true, true, false, false, false, dataHelper.getRootAuthData(env), env);
		
		// create user
		User user = new User();
		user.setProfile(profile);
		user.setUserName("test");
		user.setEmail("test@kolmu.com");
		user.setPassword("test");
		user.setTimezone("GMT");
		user.setLocale("EN_US");
		user.setIsActive(true);
		
		user = userService.save(user, dataHelper.getRootAuthData(env), env);
		
		// create two students
		Record student1 = new Record(dataSet.getStudentType());
		student1.setField("name", "Andrzej");
		dataService.save(student1, env);
		
		Record student2 = new Record(dataSet.getStudentType());
		student2.setField("name", "Mirek");
		dataService.save(student2, env);
		
		AuthData userAuthData = new AuthData(user, env, permissionService, compiler);
		userAuthData.setUser(user);
		
		// init all user permissions
		userAuthData.initUserPermissions(env);
		
		// share both students with user
		sharingService.shareRecord(student1.getKID(), user.getId(), dataHelper.getRootAuthData(env), "Test", true, env);
		sharingService.shareRecord(student2.getKID(), user.getId(), dataHelper.getRootAuthData(env), "Test", true, env);
		
		// create two courses
		Record course1 = new Record(dataSet.getCourseType());
		course1.setField("name", "Maths");
		dataService.save(course1, env);
		
		Record course2 = new Record(dataSet.getCourseType());
		course2.setField("name", "Algebra");
		dataService.save(course2, env);
		
		// share only the Maths course with this user
		sharingService.shareRecord(course1.getKID(), user.getId(), dataHelper.getRootAuthData(env), "Test", true, env);
		
		// make sure course field exists on type
		FieldFilter filter = new FieldFilter();
		filter.setApiName("courses");
		filter.setTypeQualifiedName(dataSet.getStudentType().getQualifiedName());
		assertEquals(1, dataService.getFields(filter, env).size());
		
		// query student that has no assigned courses
		List<Record> students = env.getSelectCriteriaFromDAL("select id, name, courses.id, courses.name FROM " + StudentCourseDataSet.PACKAGE + "." + StudentCourseDataSet.STUDENT_API_NAME + " where id = '" + student1.getKID() + "'", userAuthData).list();
		assertEquals(1, students.size());
		Record student = students.get(0);
		
		// now change sharing on enrollment so that user can read all records of this type
		permissionService.setTypePermissionForProfile(profile.getId(), dataSet.getEnrollmentType().getKID(), true, true, true, true, true, true, true, dataHelper.getRootAuthData(env), env);
		// reinit permissions to see the above change
		userAuthData.initUserPermissions(env);
		
		// query student that has no assigned courses
		students = env.getSelectCriteriaFromDAL("select id, name, courses.id, courses.name FROM " + StudentCourseDataSet.PACKAGE + "." + StudentCourseDataSet.STUDENT_API_NAME + " where id = '" + student1.getKID() + "'", userAuthData).list();
		assertEquals(1, students.size());
		student = students.get(0);
		
		assertEquals(student1.getKID(), student.getKID());
		assertNotNull(student.getField("courses"));
		assertTrue(student.getField("courses") instanceof List<?>);
		List<Record> courses = (List<Record>)student.getField("courses");
		assertTrue(courses.isEmpty());
		
		// enroll user 1 to both courses
		Record enroll1 = new Record(dataSet.getEnrollmentType());
		enroll1.setField("student", student1);
		enroll1.setField("course", course1);
		dataService.save(enroll1, env);
		
		Record enroll2 = new Record(dataSet.getEnrollmentType());
		enroll2.setField("student", student1);
		enroll2.setField("course", course2);
		dataService.save(enroll2, env);
		
		// now select user one with both courses and make sure only the first course is in the collection
		students = env.getSelectCriteriaFromDAL("select id, name, courses.id, courses.name FROM " + StudentCourseDataSet.PACKAGE + "." + StudentCourseDataSet.STUDENT_API_NAME + " where id = '" + student1.getKID() + "'", userAuthData).list();
		assertEquals(1, students.size());
		student = students.get(0);
		
		assertEquals(student1.getKID(), student.getKID());
		assertNotNull(student.getField("courses"));
		assertTrue(student.getField("courses") instanceof List<?>);
		courses = (List<Record>)student.getField("courses");
		assertEquals(1, courses.size());
		assertEquals(course1.getKID(), courses.get(0).getKID());
		
		// now try the same query without record sharing applied
		students = env.getSelectCriteriaFromDAL("select id, name, courses.id, courses.name FROM " + StudentCourseDataSet.PACKAGE + "." + StudentCourseDataSet.STUDENT_API_NAME + " where id = '" + student1.getKID() + "'").list();
		assertEquals(1, students.size());
		student = students.get(0);
		
		assertEquals(student1.getKID(), student.getKID());
		assertNotNull(student.getField("courses"));
		assertTrue(student.getField("courses") instanceof List<?>);
		courses = (List<Record>)student.getField("courses");
		assertEquals(2, courses.size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSharingOnType() throws KommetException
	{
		EnvData env = dataHelper.configureFullTestEnv();
		Type pigeonType = dataHelper.getFullPigeonType(env);
		Type addressType = dataHelper.getAddressType(env);
		
		addressType = dataService.createType(addressType, env);
		pigeonType = dataService.createType(pigeonType, env);
		
		AuthData adminAuthData = dataHelper.getRootAuthData(env);
		
		// add address reference to pigeon
		Field addressField = new Field();
		addressField.setApiName("address");
		addressField.setLabel("Address");
		addressField.setRequired(false);
		addressField.setDataType(new TypeReference(addressType));
		pigeonType.addField(addressField);
		dataService.createField(addressField, env);
		
		// add pigeon collection to address
		Field pigeonListField = new Field();
		pigeonListField.setApiName("pigeons");
		pigeonListField.setLabel("Pigeons");
		pigeonListField.setRequired(false);
		pigeonListField.setDataType(new InverseCollectionDataType(pigeonType, "address"));
		addressType.addField(pigeonListField);
		dataService.createField(pigeonListField, env);
		
		// create profile
		Profile profile = new Profile();
		profile.setName("RestrictedPermissionsProfile");
		profile.setLabel("RestrictedPermissionsProfile");
		profile.setSystemProfile(false);
		profile = profileService.save(profile, dataHelper.getRootAuthData(env), env);
		assertNotNull(profile.getId());
		
		// give profile permissions to create user groups
		permissionService.setTypePermissionForProfile(profile.getId(), env.getType(KeyPrefix.get(KID.USER_GROUP_PREFIX)).getKID(), true, true, true, true, true, true, true, AuthData.getRootAuthData(env), env);
		permissionService.setTypePermissionForProfile(profile.getId(), env.getType(KeyPrefix.get(KID.USER_GROUP_ASSIGNMENT_PREFIX)).getKID(), true, true, true, true, true, true, true, AuthData.getRootAuthData(env), env);
		
		// create user
		User user = new User();
		user.setProfile(profile);
		user.setUserName("test");
		user.setEmail("test@kolmu.com");
		user.setPassword("test");
		user.setTimezone("GMT");
		user.setLocale("EN_US");
		user.setIsActive(true);
		
		user = userService.save(user, dataHelper.getRootAuthData(env), env);
		assertNotNull(user.getId());
		
		// create auth data for user
		AuthData testUserAuthData = dataHelper.getAuthData(user, env);
		assertNotNull(testUserAuthData.getUserId());
		assertFalse(testUserAuthData.canReadType(pigeonType.getKID(), false, env));
		assertFalse(testUserAuthData.canReadAllType(pigeonType.getKID(), false, env));
		assertFalse(testUserAuthData.canEditType(pigeonType.getKID(), false, env));
		assertFalse(testUserAuthData.canEditAllType(pigeonType.getKID(), false, env));
		assertFalse(testUserAuthData.canCreateType(pigeonType.getKID(), false, env));
		assertFalse(testUserAuthData.canDeleteType(pigeonType.getKID(), false, env));
		
		// let admin create an address
		Record mragowoAddress = new Record(addressType);
		mragowoAddress.setField("city", "Mr�gowo");
		mragowoAddress.setField("street", "S�oneczna");
		mragowoAddress = dataService.save(mragowoAddress, env);
		assertNotNull(mragowoAddress.getKID());
		
		// let admin create a pigeon
		Record oldPigeon = new Record(pigeonType);
		oldPigeon.setField("name", "Bolek");
		oldPigeon.setField("age", BigDecimal.valueOf(3));
		oldPigeon.setField("address.id", mragowoAddress.getKID(), env);
		oldPigeon = dataService.save(oldPigeon, env);
		assertNotNull(oldPigeon.getKID());
		
		// let admin create a pigeon
		Record youngPigeon = new Record(pigeonType);
		youngPigeon.setField("name", "Rolek");
		youngPigeon.setField("age", BigDecimal.valueOf(55));
		youngPigeon.setField("address.id", mragowoAddress.getKID(), env);
		youngPigeon = dataService.save(youngPigeon, env);
		assertNotNull(youngPigeon.getKID());
		
		assertEquals(Long.valueOf(2), env.getSelectCriteriaFromDAL("select count(id) from " + pigeonType.getQualifiedName()).count());
		try
		{
			assertEquals(Long.valueOf(0), env.getSelectCriteriaFromDAL("select count(id) from " + pigeonType.getQualifiedName(), testUserAuthData).count());
			fail("Querying type for which user has no 'read' permission should throw an exception");
		}
		catch (InsufficientPrivilegesException e)
		{
			assertTrue(e.getMessage().startsWith("Insufficient privileges to query"));
		}
		
		try
		{
			env.getSelectCriteriaFromDAL("select id from " + pigeonType.getQualifiedName(), testUserAuthData).list();
			fail("Querying type for which user has no 'read' permission should throw an exception");
		}
		catch (InsufficientPrivilegesException e)
		{
			assertTrue(e.getMessage().startsWith("Insufficient privileges to query"));
		}
		
		// now add permission for the user to see some (but not all) records from type pigeon
		permissionService.setTypePermissionForProfile(profile.getId(), pigeonType.getKID(), true, false, false, false, false, false, false, dataHelper.getRootAuthData(env), env);
		permissionService.setTypePermissionForProfile(profile.getId(), addressType.getKID(), true, false, false, false, false, false, false, dataHelper.getRootAuthData(env), env);
		
		// update permissions
		testUserAuthData.updateTypePermissions(true, env);
		
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select id, age from " + pigeonType.getQualifiedName(), testUserAuthData).list();
		assertTrue("No records from type Pigeon are shared with user, but a query returned " + pigeons.size() + " results instead of zero", pigeons.isEmpty());
		
		// now share the old pigeon record with the user
		UserRecordSharing sharing = sharingService.shareRecord(oldPigeon.getKID(), user.getId(), dataHelper.getRootAuthData(env), "For tests", true, env);
		assertNotNull(sharing);
		
		// now try to share again and make sure the returned URS object is the same
		UserRecordSharing newSharing = sharingService.shareRecord(oldPigeon.getKID(), user.getId(), dataHelper.getRootAuthData(env), "For tests", true, env);
		assertNotNull(newSharing);
		assertEquals(newSharing.getId(), sharing.getId());
		
		// make sure now user can see only the old pigeon record, but not the address
		pigeons = env.getSelectCriteriaFromDAL("select id, address.id, address.city, address.street from " + pigeonType.getQualifiedName(), testUserAuthData).list();
		assertEquals(1, pigeons.size());
		assertEquals(oldPigeon.getKID(), pigeons.get(0).getKID());
		assertNull("User does not have access to object \"address\", so they should not see its ID", pigeons.get(0).getField("address"));
		assertNull("User does not have access to object \"address\", so they should not see its ID", pigeons.get(0).getField("address.id"));
		assertNull("User should not see type reference field Address, because only the parent record of type Pigeon is shared with them. The address is: " + pigeons.get(0).getField("address"), pigeons.get(0).attemptGetField("address.city"));
		
		// now share the address with the user as well
		sharingService.shareRecord(mragowoAddress.getKID(), user.getId(), dataHelper.getRootAuthData(env), "For tests", true, env);
		pigeons = env.getSelectCriteriaFromDAL("select id, address.id from " + pigeonType.getQualifiedName(), testUserAuthData).list();
		assertEquals(1, pigeons.size());
		assertEquals(oldPigeon.getKID(), pigeons.get(0).getKID());
		assertNotNull("User should see type reference field Address, because it is shared with them", pigeons.get(0).getField("address"));
		
		// first query as admin
		List<Record> addresses = env.getSelectCriteriaFromDAL("select id, pigeons.id, pigeons.name, pigeons.age from " + addressType.getQualifiedName()).list();
		assertEquals(1, addresses.size());
		Record address = addresses.get(0);
		assertEquals(2, ((List<Record>)address.getField("pigeons")).size());
		
		// make sure that when address is queried with its child pigeons, the child collection will contain only
		// pigeon records shared with the user
		addresses = env.getSelectCriteriaFromDAL("select id, pigeons.id, pigeons.name, pigeons.age from " + addressType.getQualifiedName(), testUserAuthData).list();
		assertEquals(1, addresses.size());
		address = addresses.get(0);
		assertEquals(1, ((List<Record>)address.getField("pigeons")).size());
		
		assertEquals(Profile.ROOT_ID, adminAuthData.getProfile().getId().getId());
		
		// now give user access to all records of type pigeon
		permissionService.setTypePermissionForProfile(profile.getId(), pigeonType.getKID(), true, false, false, false, true, false, false, adminAuthData, env);
		testUserAuthData.updateTypePermissions(true, env);
		
		String addressQuery = "select id, pigeons.id, pigeons.name, pigeons.age from " + addressType.getQualifiedName();
		addresses = env.getSelectCriteriaFromDAL(addressQuery, testUserAuthData).list();
		assertEquals(1, addresses.size());
		address = addresses.get(0);
		assertEquals("User has permission readAll on type pigeon, so they should see two pigeons", 2, ((List<Record>)address.getField("pigeons")).size());
		
		addresses = env.select(addressQuery, testUserAuthData);
		assertEquals(1, addresses.size());
		
		testGroupSharing(pigeonType, testUserAuthData, env);
		testUnshareWithAll(pigeonType, testUserAuthData, env);
		
		permissionService.setTypePermissionForProfile(profile.getId(), pigeonType.getKID(), true, true, true, true, false, false, false, dataHelper.getRootAuthData(env), env);
		
		testSharingControlledBy(pigeonType, testUserAuthData, env);
		testEditPermissionsDuringSave(pigeonType, testUserAuthData, env);
	}

	/**
	 * Makes sure edit permissions are checked when records are saved.
	 * @param pigeonType
	 * @param authData
	 * @param env
	 * @throws KommetException 
	 */
	private void testEditPermissionsDuringSave(Type pigeonType, AuthData authData, EnvData env) throws KommetException
	{
		// create profile without access to comments
		Profile restrictedProfile = new Profile();
		restrictedProfile.setName("RestrictedProfile");
		restrictedProfile.setLabel("Restricted Profile");
		restrictedProfile.setSystemProfile(false);
		restrictedProfile = profileService.save(restrictedProfile, dataHelper.getRootAuthData(env), env);
		
		User restrictedUser = dataHelper.getTestUser("restricted@kommet.io", "restricted@kommet.io", restrictedProfile, env);
		
		AuthData restrictedAuthData = dataHelper.getAuthData(restrictedUser, env);
		
		// create some pigeon
		Record pigeon1 = new Record(pigeonType);
		pigeon1.setField("name", "Alek");
		pigeon1.setField("age", BigDecimal.valueOf(2));
		pigeon1 = dataService.save(pigeon1, authData, env);
		
		assertFalse("In order for the test to make sense, we want the profile to not be able to edit all records of type pigeon", restrictedAuthData.canEditAllType(pigeonType.getKID(), true, env));
		
		try
		{
			dataService.save(pigeon1, restrictedAuthData, env);
			fail("Profile that does not have edit permission on type cannot save its records");
		}
		catch (InsufficientPrivilegesException e)
		{
			assertEquals(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_EDIT_RECORD_MSG, e.getMessage());
		}
		
		// add profile rights to edit some records of type pigeon
		permissionService.setTypePermissionForProfile(restrictedProfile.getId(), pigeonType.getKID(), true, true, false, false, false, false, false, dataHelper.getRootAuthData(env), env);
		
		try
		{
			dataService.save(pigeon1, restrictedAuthData, env);
			fail("Profile that does not have edit permission on type cannot save its records");
		}
		catch (InsufficientPrivilegesException e)
		{
			assertEquals(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_EDIT_RECORD_MSG, e.getMessage());
		}
		
		// share pigeon record with user
		sharingService.shareRecord(pigeon1.getKID(), restrictedUser.getId(), true, false, authData, "No reason", true, env);
		dataService.save(pigeon1, restrictedAuthData, env);
		sharingService.unshareRecord(pigeon1.getKID(), restrictedUser.getId(), authData, env);
		
		Long initialPigeonCount = env.getSelectCriteriaFromDAL("select count(id) from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME).count();
		
		try
		{
			testRollbackOnDbError(authData, restrictedAuthData, pigeon1, pigeonType, env);
		}
		catch (InsufficientPrivilegesException e)
		{
			// expected
		}
		
		// make sure that even though the transaction threw an exception, the inserted record is not rolled back
		// because the exception has been caught
		assertEquals((Long)(initialPigeonCount + 1), env.getSelectCriteriaFromDAL("select count(id) from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME).count());
		
		testEditAllPermissions(pigeonType, restrictedUser, restrictedAuthData, env);
		testPermissionsOnRecordDelete(pigeon1, restrictedUser, restrictedAuthData, env);
	}
	
	private void testEditAllPermissions(Type pigeonType, User restrictedUser, AuthData restrictedAuthData, EnvData env) throws KommetException
	{
		// create some pigeon
		Record pigeon1 = new Record(pigeonType);
		pigeon1.setField("name", "Maniek");
		pigeon1.setField("age", BigDecimal.valueOf(2));
		pigeon1 = dataService.save(pigeon1, dataHelper.getRootAuthData(env), env);
		
		// set proper permissions for pigeon type and profile
		permissionService.setTypePermissionForProfile(restrictedUser.getProfile().getId(), pigeonType.getKID(), true, true, true, true, false, false, false, dataHelper.getRootAuthData(env), env);
		
		// make sure user can't edit pigeon
		try
		{
			dataService.save(pigeon1, restrictedAuthData, env);
			fail("User should not be able to edit record");
		}
		catch (InsufficientPrivilegesException e)
		{
			assertEquals(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_EDIT_RECORD_MSG, e.getMessage());
		}
		
		// try to force save the pigeon and make sure this time it's possible
		dataService.save(pigeon1, false, true, restrictedAuthData, env);
		
		// now give the user's profile rights to read (but not edit) all records of this type
		permissionService.setTypePermissionForProfile(restrictedAuthData.getProfile().getId(), pigeonType.getKID(), true, true, true, true, true, false, false, dataHelper.getRootAuthData(env), env);
		
		// make sure user can't edit pigeon
		try
		{
			dataService.save(pigeon1, restrictedAuthData, env);
			fail("User should not be able to edit record");
		}
		catch (InsufficientPrivilegesException e)
		{
			assertEquals(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_EDIT_RECORD_MSG, e.getMessage());
		}
		
		// now give the user's profile rights to edit all records of this type
		permissionService.setTypePermissionForProfile(restrictedAuthData.getProfile().getId(), pigeonType.getKID(), true, true, true, true, true, true, true, dataHelper.getRootAuthData(env), env);
		dataService.save(pigeon1, restrictedAuthData, env);
	}

	private void testPermissionsOnRecordDelete (Record pigeon1, User restrictedUser, AuthData restrictedAuthData, EnvData env) throws KommetException
	{
		AuthData rootAuthData = dataHelper.getRootAuthData(env);
		
		// now give user permission to view and edit record, but not to delete it
		sharingService.shareRecord(pigeon1.getKID(), restrictedUser.getId(), true, false, rootAuthData, "No reason", true, env);
		
		permissionService.setTypePermissionForProfile(restrictedUser.getProfile().getId(), pigeon1.getType().getKID(), true, true, false, true, false, false, false, rootAuthData, env);
		
		// make sure user cannot delete the record
		try
		{
			dataService.deleteRecord(pigeon1, restrictedAuthData, env);
			fail("User should not be able to delete record due to insufficient permissions on profile and on the record");
		}
		catch (InsufficientPrivilegesException e)
		{
			assertTrue(e.getMessage().equals(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_DELETE_RECORD_MSG));
		}
		
		// now give profile permissions to delete record
		// add profile rights to edit some records of type pigeon
		permissionService.setTypePermissionForProfile(restrictedUser.getProfile().getId(), pigeon1.getType().getKID(), true, true, true, true, false, false, false, rootAuthData, env);
		
		try
		{
			// make sure user cannot delete the record due to insufficient permissions on record
			dataService.deleteRecord(pigeon1, restrictedAuthData, env);
			fail("User should not be able to delete record due to insufficient permissions on the record");
		}
		catch (InsufficientPrivilegesException e)
		{
			assertTrue(e.getMessage().equals(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_DELETE_RECORD_MSG));
		}
		
		KID oldRecordId = pigeon1.getKID();
		
		// now give the user permissions to delete record
		sharingService.shareRecord(pigeon1.getKID(), restrictedUser.getId(), true, true, rootAuthData, "No reason", true, env);
		dataService.deleteRecord(pigeon1, restrictedAuthData, env);
		
		// create the record anew
		pigeon1.setKID(null);
		pigeon1 = dataService.save(pigeon1, env);
		assertFalse(oldRecordId.equals(pigeon1.getKID()));
		
		// now give user permission to view and edit record, but not to delete it
		sharingService.shareRecord(pigeon1.getKID(), restrictedUser.getId(), true, false, rootAuthData, "No reason", true, env);
		
		// make sure user cannot delete the record
		try
		{
			dataService.deleteRecord(pigeon1, restrictedAuthData, env);
			fail("User should not be able to delete record due to insufficient permissions on profile and on the record");
		}
		catch (InsufficientPrivilegesException e)
		{
			assertTrue(e.getMessage().equals(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_DELETE_RECORD_MSG));
		}
		
		// now give profile permissions to delete record
		// add profile rights to edit all records of type pigeon
		permissionService.setTypePermissionForProfile(restrictedUser.getProfile().getId(), pigeon1.getType().getKID(), true, true, true, true, true, true, false, rootAuthData, env);
		
		try
		{
			// make sure user cannot delete the record due to insufficient permissions on record
			dataService.deleteRecord(pigeon1, restrictedAuthData, env);
			fail("User should not be able to delete record due to insufficient permissions on the record");
		}
		catch (InsufficientPrivilegesException e)
		{
			assertTrue(e.getMessage().equals(InsufficientPrivilegesException.INSUFFICIENT_PRIVILEGES_TO_DELETE_RECORD_MSG));
		}
		
		// make sure _trigger flag is not set after this unsuccessful attempt to delete record
		int invalidRecordCount = env.getJdbcTemplate().queryForObject("select count(id) from " + pigeon1.getType().getDbTable() + " where _triggerflag is not null", Integer.class);
		assertEquals(0, invalidRecordCount);
		
		// give rights to delete all records
		permissionService.setTypePermissionForProfile(restrictedUser.getProfile().getId(), pigeon1.getType().getKID(), true, true, true, true, true, true, true, rootAuthData, env);
		
		Type pigeonType = pigeon1.getType();
		
		List<Record> allPigeons = env.getSelectCriteriaFromDAL("select id from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME).list();
		for (Record pigeon : allPigeons)
		{
			// we will set the column to required, so we need to assign it some value
			pigeon.setField("father.id", pigeon1.getKID(), env);
			dataService.save(pigeon, env);
		}
		
		// now change the father field on the pigeon type to required and make sure that if this record is used
		// as a foreign key by another record, it cannot be deleted
		Field fatherField = pigeonType.getField("father");
		fatherField.setRequired(true);
		dataService.updateField(fatherField, rootAuthData, env);
		
		// create another record that uses pigeon1
		Record pigeon2 = new Record(pigeonType);
		pigeon2.setField("name", "Lilith");
		pigeon2.setField("age", 1);
		pigeon2.setField("father.id", pigeon1.getKID(), env);
		pigeon2 = dataService.save(pigeon2, env);
		assertNotNull(pigeon2.getKID());
		
		// make sure pigeon1 cannot be deleted because it is used
		try
		{
			dataService.deleteRecord(pigeon1, restrictedAuthData, env);
			fail("It should not be possible to delete a record because that would cause nullification of a not-null foreign key");
		}
		catch (NotNullConstraintViolationException e)
		{
			assertTrue(e.getMessage().startsWith("Not null constraint violation "));
		}
		
		// make sure _trigger flag is not set after this unsuccessful attempt to delete record
		invalidRecordCount = env.getJdbcTemplate().queryForObject("select count(id) from " + pigeon1.getType().getDbTable() + " where _triggerflag is not null", Integer.class);
		assertEquals(0, invalidRecordCount);
		
		allPigeons = env.getSelectCriteriaFromDAL("select id from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME).list();
		for (Record pigeon : allPigeons)
		{
			// now change the value of foreign key to reference pigeon2, so that pigeon1 can be deleted
			pigeon.setField("father.id", pigeon2.getKID(), env);
			dataService.save(pigeon, env);
		}
		
		dataService.deleteRecord(pigeon1, restrictedAuthData, env);
		
		// make the field not required again, to allow other tests to run successfully
		fatherField.setRequired(false);
		dataService.updateField(fatherField, rootAuthData, env);
	}
	
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	private void testRollbackOnDbError(AuthData authData, AuthData userAuthData, Record existingPigeon, Type pigeonType, EnvData env) throws KommetException
	{
		Record newPigeon = new Record(pigeonType);
		newPigeon.setField("name", "Irek");
		newPigeon.setField("age", BigDecimal.valueOf(2));
		newPigeon = dataService.save(newPigeon, authData, env);
		assertNotNull(newPigeon.getKID());
		
		dataService.save(existingPigeon, userAuthData, env);
		fail("Profile that does not have edit permission on type cannot save its records");
	}

	/**
	 * Makes sure comments are shared properly with users who can read a record they are related to.
	 * @param pigeonType
	 * @param authData
	 * @param env
	 * @throws KeyPrefixException
	 * @throws KommetException
	 */
	private void testSharingControlledBy(Type pigeonType, AuthData authData, EnvData env) throws KeyPrefixException, KommetException
	{
		Type commentType = env.getType(KeyPrefix.get(KID.COMMENT_PREFIX));
		commentType.setCombineRecordAndCascadeSharing(false);
		dataService.updateType(commentType, authData, env);
		assertNotNull(commentType.getSharingControlledByFieldId());
		assertNotNull(commentType.getSharingControlledByField());
		assertEquals(commentType.getField("recordId").getKID(), commentType.getSharingControlledByFieldId());
		
		// create profile without access to comments
		Profile commentReaderProfile = new Profile();
		commentReaderProfile.setName("CommentReader");
		commentReaderProfile.setLabel("Comment reader");
		commentReaderProfile.setSystemProfile(false);
		commentReaderProfile = profileService.save(commentReaderProfile, AuthData.getRootAuthData(env), env);
		
		permissionService.setTypePermissionForProfile(commentReaderProfile.getId(), pigeonType.getKID(), true, true, true, true, false, false, false, dataHelper.getRootAuthData(env), env);
		permissionService.setTypePermissionForProfile(commentReaderProfile.getId(), commentType.getKID(), true, true, true, true, false, false, false, dataHelper.getRootAuthData(env), env);
		permissionService.setTypePermissionForProfile(authData.getProfile().getId(), commentType.getKID(), true, true, true, true, false, false, false, dataHelper.getRootAuthData(env), env);
		
		User reader1 = dataHelper.getTestUser("reader@kommet.io", "reader@kommet.io", commentReaderProfile, env);
		
		AuthData reader1AuthData = dataHelper.getAuthData(reader1, env);
		
		// create some pigeon
		Record pigeon1 = new Record(pigeonType);
		pigeon1.setField("name", "Alek");
		pigeon1.setField("age", BigDecimal.valueOf(2));
		pigeon1 = dataService.save(pigeon1, reader1AuthData, env);
		
		// create another pigeon owned by root
		Record pigeon2 = new Record(pigeonType);
		pigeon2.setField("name", "Olek");
		pigeon2.setField("age", BigDecimal.valueOf(2));
		pigeon2 = dataService.save(pigeon2, authData, env);
		
		// make sure reader user can only see the first pigeon
		List<Record> pigeons = env.getSelectCriteriaFromDAL("select id, name from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME, reader1AuthData).list();
		assertEquals(1, pigeons.size());
		assertEquals(pigeon1.getKID(), pigeons.get(0).getKID());
		
		// let root add a comment to their pigeon
		Comment pigeon2Comment = new Comment();
		pigeon2Comment.setContent("Test");
		pigeon2Comment.setRecordId(pigeon2.getKID());
		commentService.save(pigeon2Comment, authData, env);
		
		// make sure reader cannot see this comment
		List<Record> comments = env.getSelectCriteriaFromDAL("select id from " + SystemTypes.COMMENT_API_NAME, reader1AuthData).list();
		assertTrue("User should not see comments of records they don't have access to", comments.isEmpty());
		
		// now add a comment to pigeon1, to which user has access
		Comment pigeon1Comment = new Comment();
		pigeon1Comment.setContent("Test");
		pigeon1Comment.setRecordId(pigeon1.getKID());
		pigeon1Comment = commentService.save(pigeon1Comment, authData, env);
		
		comments = env.getSelectCriteriaFromDAL("select id from " + SystemTypes.COMMENT_API_NAME, reader1AuthData).list();
		assertEquals("User should see comment because it has been added to a record they have rights to read", 1, comments.size());
		
		// create another user with this profile
		User reader2 = dataHelper.getTestUser("reader2@kommet.io", "reader2@kommet.io", commentReaderProfile, env);
		reader2 = userService.save(reader2, AuthData.getRootAuthData(env), env);
		
		AuthData reader2AuthData = dataHelper.getAuthData(reader2, env);
		
		comments = env.getSelectCriteriaFromDAL("select id from " + SystemTypes.COMMENT_API_NAME, dataHelper.getAuthData(reader2, env)).list();
		assertTrue("User should not see comments of records they don't have access to", comments.isEmpty());
		assertFalse(sharingService.canViewRecord(pigeon1Comment.getId(), reader2.getId(), env));
		assertFalse(sharingService.canEditRecord(pigeon1Comment.getId(), reader2.getId(), env));
		
		sharingService.shareRecord(pigeon1Comment.getId(), reader2.getId(), true, true, authData, "No reason", true, env);
		
		Type envCommentType = env.getTypeByRecordId(pigeon1Comment.getId());
		assertNotNull(envCommentType.getSharingControlledByFieldId());
		assertNotNull(envCommentType.getSharingControlledByField());
		assertFalse(envCommentType.isCombineRecordAndCascadeSharing());
		
		// share the comment with the second user and make sure this does not give them
		// access to it because option combineRecordAndCascadeSharings is set to false
		comments = env.getSelectCriteriaFromDAL("select id from " + SystemTypes.COMMENT_API_NAME, reader2AuthData).list();
		assertEquals(0, comments.size());
		assertFalse("User should not be able to read comment because combineRecordAndCascadeSharings on comment type is false and sharingControlledByField is set to recordId field", sharingService.canViewRecord(pigeon1Comment.getId(), reader2.getId(), env));
		
		commentType.setCombineRecordAndCascadeSharing(true);
		dataService.updateType(commentType, authData, env);
		
		comments = env.getSelectCriteriaFromDAL("select id from " + SystemTypes.COMMENT_API_NAME, reader2AuthData).list();
		assertEquals(1, comments.size());
		assertTrue("User should be able to read comment because combineRecordAndCascadeSharings on comment type is true", sharingService.canViewRecord(pigeon1Comment.getId(), reader2.getId(), env));
		assertTrue("User should be able to edit comment because combineRecordAndCascadeSharings on comment type is true", sharingService.canEditRecord(pigeon1Comment.getId(), reader2.getId(), env));
		
		// unshare record
		sharingService.unshareRecord(pigeon1Comment.getId(), reader2.getId(), authData, env);
		comments = env.getSelectCriteriaFromDAL("select id from " + SystemTypes.COMMENT_API_NAME, reader2AuthData).list();
		assertEquals(0, comments.size());
		assertFalse(sharingService.canViewRecord(pigeon1Comment.getId(), reader2.getId(), env));
		assertFalse(sharingService.canEditRecord(pigeon1Comment.getId(), reader2.getId(), env));
		
		// now share the pigeon (but not the comment) with the second reader and make this this gives
		// them access to comments on the shared record
		sharingService.shareRecord(pigeon1.getKID(), reader2.getId(), authData, "No reason", true, env);
		comments = env.getSelectCriteriaFromDAL("select id from " + SystemTypes.COMMENT_API_NAME, reader2AuthData).list();
		assertEquals("User should see comment because it has been added to a record shared with them", 1, comments.size());
		
		assertTrue("User should be able to read comment because combineRecordAndCascadeSharings on comment type is true", sharingService.canViewRecord(pigeon1Comment.getId(), reader2.getId(), env));
		
		commentType.setCombineRecordAndCascadeSharing(false);
		dataService.updateType(commentType, authData, env);
		
		assertTrue("User should be able to read comment because combineRecordAndCascadeSharings on comment type is true", sharingService.canViewRecord(pigeon1Comment.getId(), reader2.getId(), env));
		comments = env.getSelectCriteriaFromDAL("select id from " + SystemTypes.COMMENT_API_NAME, reader2AuthData).list();
		assertEquals("User should see comment because it has been added to a record shared with them", 1, comments.size());
	}

	private void testUnshareWithAll(Type pigeonType, AuthData authData, EnvData env) throws KommetException
	{
		List<UserGroup> userGroups = new ArrayList<UserGroup>();
		
		// create some user groups
		for (int i = 0; i < 5; i++)
		{
			UserGroup ug = new UserGroup();
			ug.setName("UG" + i);
			userGroups.add(userGroupService.save(ug, authData, env));
		}
		
		// create pigeon
		Record pigeon = new Record(pigeonType);
		pigeon.setField("name", "Lelek");
		pigeon.setField("age", BigDecimal.valueOf(4));
		pigeon = dataService.save(pigeon, env);
		
		// share record with all groups
		for (UserGroup ug : userGroups)
		{
			sharingService.shareRecordWithGroup(pigeon.getKID(), ug.getId(), authData, "empty", true, env);
		}
		
		for (UserGroup ug : userGroups)
		{
			assertTrue(sharingService.canGroupViewRecord(pigeon.getKID(), ug.getId(), env));
		}
		
		// now unshare record with all groups
		sharingService.unshareRecordWithAllGroups(pigeon.getKID(), authData, env);
		
		for (UserGroup ug : userGroups)
		{
			assertFalse(sharingService.canGroupViewRecord(pigeon.getKID(), ug.getId(), env));
		}
	}

	private void testGroupSharing (Type pigeonType, AuthData authData, EnvData env) throws KommetException
	{
		AuthData rootAuthData = dataHelper.getRootAuthData(env);
		
		// create user and group hierarchy
		UserGroupHierarchyDataSet dataSet = null;
		
		try
		{
			dataSet = dataHelper.createUserGroupHierarchy(authData, env);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw e;
		}
		
		assertTrue(userGroupService.isGroupInGroup(dataSet.getMathStudentGroup().getId(), dataSet.getStudentGroup().getId(), true, env));
		assertTrue(userGroupService.isGroupInGroup(dataSet.getAlgebraStudentGroup().getId(), dataSet.getMathStudentGroup().getId(), true, env));
		assertTrue(userGroupService.isUserInGroup(dataSet.getMathStudent1().getKID(), dataSet.getMathStudentGroup().getId(), false, env));
		
		// add profile access to type pigeon
		permissionService.setTypePermissionForProfile(dataSet.getProfile().getKID(), pigeonType.getKID(), true, true, true, true, false, false, false, rootAuthData, env);
		permissionService.setTypePermissionForProfile(dataSet.getProfile().getKID(), env.getType(KeyPrefix.get(KID.USER_GROUP_PREFIX)).getKID(), true, true, true, true, false, false, false, rootAuthData, env);
		permissionService.setTypePermissionForProfile(dataSet.getProfile().getKID(), env.getType(KeyPrefix.get(KID.USER_GROUP_ASSIGNMENT_PREFIX)).getKID(), true, true, true, true, false, false, false, rootAuthData, env);
		
		testSimpleGroupSharing(dataSet, pigeonType, authData, env);
		
		// share all groups with the math student so that they can delete it later
		sharingService.shareRecord(dataSet.getAlgebraStudentGroup().getId(), dataSet.getMathStudent1().getKID(), authData, "Sharing group", true, env);
		sharingService.shareRecord(dataSet.getMathStudentGroup().getId(), dataSet.getMathStudent1().getKID(), authData, "Sharing group", true, env);
		sharingService.shareRecord(dataSet.getStudentGroup().getId(), dataSet.getMathStudent1().getKID(), authData, "Sharing group", true, env);
		sharingService.shareRecord(dataSet.getGeometryStudentGroup().getId(), dataSet.getMathStudent1().getKID(), authData, "Sharing group", true, env);
		
		AuthData studentAuthData = dataHelper.getAuthData(userService.getUser(dataSet.getMathStudent1().getKID(), env), env);
		
		// create some pigeons
		Record youngPigeon = new Record(pigeonType);
		youngPigeon.setField("name", "Rolek");
		youngPigeon.setField("age", BigDecimal.valueOf(3));
		youngPigeon = dataService.save(youngPigeon, env);
		
		Record oldPigeon = new Record(pigeonType);
		oldPigeon.setField("name", "Nolek");
		oldPigeon.setField("age", BigDecimal.valueOf(55));
		oldPigeon = dataService.save(oldPigeon, env);
		
		// make sure none of the users can access these pigeons
		assertFalse(sharingService.canEditRecord(oldPigeon.getKID(), dataSet.getTeacher1().getKID(), env));
		assertFalse(sharingService.canEditRecord(oldPigeon.getKID(), dataSet.getTeacher1().getKID(), env));
		assertFalse(sharingService.canEditRecord(oldPigeon.getKID(), dataSet.getMathStudent1().getKID(), env));
		assertFalse(sharingService.canEditRecord(oldPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), env));
		assertFalse(sharingService.canEditRecord(oldPigeon.getKID(), dataSet.getAlgebraStudent2().getKID(), env));
		assertFalse(sharingService.canEditRecord(oldPigeon.getKID(), dataSet.getGeometryStudent1().getKID(), env));
		assertFalse(sharingService.canEditRecord(youngPigeon.getKID(), dataSet.getTeacher1().getKID(), env));
		assertFalse(sharingService.canEditRecord(youngPigeon.getKID(), dataSet.getTeacher2().getKID(), env));
		assertFalse(sharingService.canEditRecord(youngPigeon.getKID(), dataSet.getMathStudent1().getKID(), env));
		assertFalse(sharingService.canEditRecord(youngPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), env));
		assertFalse(sharingService.canEditRecord(youngPigeon.getKID(), dataSet.getAlgebraStudent2().getKID(), env));
		assertFalse(sharingService.canEditRecord(youngPigeon.getKID(), dataSet.getGeometryStudent1().getKID(), env));
		
		// now share record with all students - with the top-most group
		sharingService.shareRecordWithGroup(youngPigeon.getKID(), dataSet.getStudentGroup().getId(), false, false, "Some Reason", true, authData, env);
		
		// make sure all students have access to this record
		assertTrue(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getMathStudent1().getKID(), env));
		assertTrue(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), env));
		assertTrue(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getAlgebraStudent2().getKID(), env));
		assertTrue(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getGeometryStudent1().getKID(), env));
		
		// although in reality subgroups can view the record, the call to canGroupViewRecord will return false because otherwise we would need to maintain too many links
		assertTrue(sharingService.canGroupViewRecord(youngPigeon.getKID(), dataSet.getStudentGroup().getId(), env));
		assertFalse(sharingService.canGroupViewRecord(youngPigeon.getKID(), dataSet.getAlgebraStudentGroup().getId(), env));
		assertFalse(sharingService.canGroupViewRecord(youngPigeon.getKID(), dataSet.getMathStudentGroup().getId(), env));
		assertFalse(sharingService.canGroupViewRecord(youngPigeon.getKID(), dataSet.getGeometryStudentGroup().getId(), env));
		assertFalse(sharingService.canGroupEditRecord(youngPigeon.getKID(), dataSet.getAlgebraStudentGroup().getId(), env));
		assertFalse(sharingService.canGroupEditRecord(youngPigeon.getKID(), dataSet.getMathStudentGroup().getId(), env));
		assertFalse(sharingService.canGroupEditRecord(youngPigeon.getKID(), dataSet.getGeometryStudentGroup().getId(), env));
		
		// make sure teacher group members still can't access the record
		assertFalse(sharingService.canViewRecord(oldPigeon.getKID(), dataSet.getTeacher1().getKID(), env));
		assertFalse(sharingService.canViewRecord(oldPigeon.getKID(), dataSet.getTeacher2().getKID(), env));
		
		// find a sharing for some student and the record
		UserRecordSharingFilter ursFilter = new UserRecordSharingFilter();
		ursFilter.addRecordId(youngPigeon.getKID());
		ursFilter.addUserId(dataSet.getAlgebraStudent1().getKID());
		List<UserRecordSharing> sharings = ursDao.find(ursFilter, env);
		assertFalse(sharings.isEmpty());
		assertEquals(1, sharings.size());
		
		// make sure the created sharing has certain properties set that contain data about sharing propagation
		UserRecordSharing sharing = sharings.get(0);
		assertNotNull(sharing.getGroupRecordSharingId());
		assertNotNull(sharing.getUserGroupAssignmentId());
		assertNotNull(sharing.getGroupSharingHierarchy());
		assertFalse(sharing.getIsGeneric());
		assertEquals("Some Reason", sharing.getReason());
		
		Integer grsCount = grsDao.find(null, env).size();
		
		// now share another record with some subgroup of students
		sharingService.shareRecordWithGroup(oldPigeon.getKID(), dataSet.getAlgebraStudentGroup().getId(), false, false, "Another Reason", true, authData, env);
		
		assertEquals((Integer)(grsCount + 1), (Integer)grsDao.find(null, env).size());
		
		assertFalse(sharingService.canViewRecord(oldPigeon.getKID(), dataSet.getTeacher1().getKID(), env));
		assertFalse(sharingService.canViewRecord(oldPigeon.getKID(), dataSet.getTeacher2().getKID(), env));
		assertFalse(sharingService.canViewRecord(oldPigeon.getKID(), dataSet.getMathStudent1().getKID(), env));
		assertTrue(sharingService.canViewRecord(oldPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), env));
		assertTrue(sharingService.canViewRecord(oldPigeon.getKID(), dataSet.getAlgebraStudent2().getKID(), env));
		assertFalse(sharingService.canViewRecord(oldPigeon.getKID(), dataSet.getGeometryStudent1().getKID(), env));
		
		// check group permissions on the shared record
		assertTrue(sharingService.canGroupViewRecord(oldPigeon.getKID(), dataSet.getAlgebraStudentGroup().getId(), env));
		assertFalse(sharingService.canGroupViewRecord(oldPigeon.getKID(), dataSet.getMathStudentGroup().getId(), env));
		assertFalse(sharingService.canGroupViewRecord(oldPigeon.getKID(), dataSet.getGeometryStudentGroup().getId(), env));
		assertFalse(sharingService.canGroupEditRecord(oldPigeon.getKID(), dataSet.getAlgebraStudentGroup().getId(), env));
		assertFalse(sharingService.canGroupEditRecord(oldPigeon.getKID(), dataSet.getMathStudentGroup().getId(), env));
		assertFalse(sharingService.canGroupEditRecord(oldPigeon.getKID(), dataSet.getGeometryStudentGroup().getId(), env));
		
		// give one of the algebra students generic permission to edit the pigeon
		sharingService.shareRecord(oldPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), true, false, authData, "Fancy", true, env);
		assertTrue(sharingService.canEditRecord(oldPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), env));
		assertFalse(sharingService.canEditRecord(oldPigeon.getKID(), dataSet.getAlgebraStudent2().getKID(), env));
		
		Integer ursCount = ursDao.find(null, env).size();
		grsCount = grsDao.find(null, env).size();
		
		// now give algebra students rights to edit the old pigeon record
		sharingService.shareRecordWithGroup(oldPigeon.getKID(), dataSet.getAlgebraStudentGroup().getId(), true, false, "Another Reason Added Edit", true, authData, env);
		
		// make sure two new URS object has been added
		assertEquals((Integer)ursCount, (Integer)ursDao.find(null, env).size());
		assertEquals((Integer)grsCount, (Integer)grsDao.find(null, env).size());
		
		assertTrue(sharingService.canEditRecord(oldPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), env));
		assertTrue(sharingService.canEditRecord(oldPigeon.getKID(), dataSet.getAlgebraStudent2().getKID(), env));
		assertFalse(sharingService.canEditRecord(oldPigeon.getKID(), dataSet.getMathStudent1().getKID(), env));
		assertTrue(sharingService.canGroupEditRecord(oldPigeon.getKID(), dataSet.getAlgebraStudentGroup().getId(), env));
		
		// now revoke edit permission for algebra students
		sharingService.shareRecordWithGroup(oldPigeon.getKID(), dataSet.getAlgebraStudentGroup().getId(), false, false, "Another Reason Added Edit", true, authData, env);
		
		assertEquals((Integer)grsCount, (Integer)grsDao.find(null, env).size());
		assertEquals((Integer)ursCount, (Integer)ursDao.find(null, env).size());
		assertTrue(sharingService.canEditRecord(oldPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), env));
		assertFalse(sharingService.canEditRecord(oldPigeon.getKID(), dataSet.getAlgebraStudent2().getKID(), env));
		assertTrue(sharingService.canViewRecord(oldPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), env));
		assertTrue(sharingService.canViewRecord(oldPigeon.getKID(), dataSet.getAlgebraStudent2().getKID(), env));
		
		// revoke generic permission for algebra student
		sharingService.shareRecord(oldPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), false, false, authData, "Fancy", true, env);
		assertFalse(sharingService.canEditRecord(oldPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), env));
		sharingService.shareRecord(oldPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), true, false, authData, "Fancy", true, env);
		
		assertEquals((Integer)ursCount, (Integer)ursDao.find(null, env).size());
		
		// now revoke all permissions for student group
		sharingService.unshareRecordWithGroup(oldPigeon.getKID(), dataSet.getAlgebraStudentGroup().getId(), null, null, authData, env);
		assertTrue(sharingService.canViewRecord(oldPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), env));
		assertFalse(sharingService.canViewRecord(oldPigeon.getKID(), dataSet.getAlgebraStudent2().getKID(), env));
		
		assertEquals((Integer)(ursCount - 2), (Integer)ursDao.find(null, env).size());
		
		// and finally remove the generic sharing for this student
		sharingService.unshareRecord(oldPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), authData, env);
		assertFalse(sharingService.canViewRecord(oldPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), env));
		assertEquals((Integer)(ursCount - 3), (Integer)ursDao.find(null, env).size());
		
		// record youngPigeon is at this stage shared with the whole student group
		// we will addtionally share it with math geometry students
		sharingService.shareRecordWithGroup(youngPigeon.getKID(), dataSet.getGeometryStudentGroup().getId(), false, false, "No reason", true, authData, env);
		
		// check access
		assertTrue(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getMathStudent1().getKID(), env));
		assertTrue(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), env));
		assertTrue(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getAlgebraStudent2().getKID(), env));
		assertTrue(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getGeometryStudent1().getKID(), env));
		
		// now revoke access from the whole student group, but leave it for geometry students
		sharingService.unshareRecordWithGroup(youngPigeon.getKID(), dataSet.getStudentGroup().getId(), null, null, authData, env);
		assertFalse(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getMathStudent1().getKID(), env));
		assertFalse(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), env));
		assertFalse(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getAlgebraStudent2().getKID(), env));
		assertTrue(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getGeometryStudent1().getKID(), env));
		assertFalse(sharingService.canEditRecord(youngPigeon.getKID(), dataSet.getGeometryStudent1().getKID(), env));
		
		ursCount = ursDao.find(null, env).size();
		
		// add a generic sharing for this geometry student
		sharingService.shareRecord(youngPigeon.getKID(), dataSet.getGeometryStudent1().getKID(), authData, "No reason - geometry student", true, env);
		
		List<Record> fetchedPigeons = env.getSelectCriteriaFromDAL("select id from " + TestDataCreator.PIGEON_TYPE_QUALIFIED_NAME + " where id = '" + youngPigeon.getKID() + "'", dataHelper.getAuthData(userService.getUser(dataSet.getGeometryStudent1().getKID(), env), env)).list();
		assertEquals(1, fetchedPigeons.size());
		
		assertEquals((Integer)(ursCount + 1), (Integer)ursDao.find(null, env).size());
		
		assertEquals("Generic sharing should created additional URS except the existing propagated URS records", (Integer)(ursCount + 1), (Integer)ursDao.find(null, env).size());
		assertTrue(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getGeometryStudent1().getKID(), env));
		assertTrue(sharingService.canEditRecord(youngPigeon.getKID(), dataSet.getGeometryStudent1().getKID(), env));
		
		grsCount = grsDao.find(null, env).size();
		// now revoke permission for geometry students - but we still have the generic sharing left for this one geometry student
		sharingService.unshareRecordWithGroup(youngPigeon.getKID(), dataSet.getGeometryStudentGroup().getId(), null, null, authData, env);
		
		assertEquals((Integer)(grsCount - 1), (Integer)grsDao.find(null, env).size());
		assertEquals((Integer)ursCount, (Integer)ursDao.find(null, env).size());
		assertTrue(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getGeometryStudent1().getKID(), env));
		assertTrue(sharingService.canEditRecord(youngPigeon.getKID(), dataSet.getGeometryStudent1().getKID(), env));
		
		// now remove the generic sharing
		sharingService.unshareRecord(youngPigeon.getKID(), dataSet.getGeometryStudent1().getKID(), authData, env);
		assertEquals((Integer)(ursCount - 1), (Integer)ursDao.find(null, env).size());
		
		assertFalse(sharingService.canEditRecord(youngPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), env));
		
		UserRecordSharingFilter filter = new UserRecordSharingFilter();
		filter.addRecordId(youngPigeon.getKID());
		filter.addUserId(dataSet.getGeometryStudent1().getKID());
		
		assertFalse(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getGeometryStudent1().getKID(), env));
		assertFalse(sharingService.canEditRecord(youngPigeon.getKID(), dataSet.getGeometryStudent1().getKID(), env));
		
		// now share the record with all students again
		sharingService.shareRecordWithGroup(youngPigeon.getKID(), dataSet.getStudentGroup().getId(), authData, "Test 1.0", true, env);
		
		assertFalse(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getTeacher1().getKID(), env));
		assertFalse(sharingService.canEditRecord(youngPigeon.getKID(), dataSet.getTeacher1().getKID(), env));
		
		// add some user to algebra student group
		userGroupService.assignUserToGroup(dataSet.getTeacher1().getKID(), dataSet.getAlgebraStudentGroup().getId(), authData, env, true);
		
		// make sure the new group member can access the record shared with a supergroup
		assertTrue(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getTeacher1().getKID(), env));
		assertTrue(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getMathStudent1().getKID(), env));
		assertFalse(sharingService.canEditRecord(youngPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), env));
		assertFalse(sharingService.canEditRecord(youngPigeon.getKID(), dataSet.getTeacher1().getKID(), env));
		assertFalse(sharingService.canEditRecord(youngPigeon.getKID(), dataSet.getMathStudent1().getKID(), env));
		
		// give members of the group edit rights
		sharingService.shareRecordWithGroup(youngPigeon.getKID(), dataSet.getStudentGroup().getId(), true, false, "Test 1.0", true, authData, env);
		
		assertTrue(sharingService.canEditRecord(youngPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), env));
		assertTrue(sharingService.canEditRecord(youngPigeon.getKID(), dataSet.getTeacher1().getKID(), env));
		assertTrue(sharingService.canEditRecord(youngPigeon.getKID(), dataSet.getMathStudent1().getKID(), env));
		
		// now remove the math student from their group
		userGroupService.unassignUserFromGroup(dataSet.getMathStudent1().getKID(), dataSet.getMathStudentGroup().getId(), authData, env);
		
		assertTrue(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getTeacher1().getKID(), env));
		assertFalse(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getMathStudent1().getKID(), env));
		
		// add math user to their group again
		userGroupService.assignUserToGroup(dataSet.getMathStudent1().getKID(), dataSet.getMathStudentGroup().getId(), authData, env, true);
		assertTrue(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getMathStudent1().getKID(), env));
		
		// now unassign the whole math student group from the student group
		userGroupService.unassignUserGroupFromGroup(dataSet.getMathStudentGroup().getId(), dataSet.getStudentGroup().getId(), false, studentAuthData, env);
		
		assertFalse(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getMathStudent1().getKID(), env));
		
		assertFalse(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getTeacher1().getKID(), env));
		assertFalse(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getTeacher2().getKID(), env));
		
		// now assign the teacher group to student group and make sure teachers are now able to access the record
		userGroupService.assignGroupToGroup(dataSet.getTeacherGroup().getId(), dataSet.getStudentGroup().getId(), studentAuthData, env, true);
		
		assertTrue(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getTeacher1().getKID(), env));
		assertTrue(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getTeacher2().getKID(), env));
		assertTrue(sharingService.canEditRecord(youngPigeon.getKID(), dataSet.getTeacher1().getKID(), env));
		assertTrue(sharingService.canEditRecord(youngPigeon.getKID(), dataSet.getTeacher2().getKID(), env));
		
		// add student group rights to edit youngPigeon
		sharingService.shareRecordWithGroup(youngPigeon.getKID(), dataSet.getStudentGroup().getId(), true, false, "Any reason", true, authData, env);
		
		userGroupService.unassignUserGroupFromGroup(dataSet.getTeacherGroup().getId(), dataSet.getStudentGroup().getId(), false, authData, env);
		
		assertFalse(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getTeacher1().getKID(), env));
		assertFalse(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getTeacher2().getKID(), env));
		
		assertFalse(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getGeometryStudent1().getKID(), env));
		assertFalse(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), env));
		assertFalse(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getMathStudent1().getKID(), env));
		
		// reassign math student group to student group
		userGroupService.assignGroupToGroup(dataSet.getMathStudentGroup().getId(), dataSet.getStudentGroup().getId(), authData, env, true);
		assertTrue(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getMathStudent1().getKID(), env));
		assertTrue(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getGeometryStudent1().getKID(), env));
		assertTrue(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), env));
		
		assertTrue(userGroupService.isGroupInGroup(dataSet.getMathStudentGroup().getId(), dataSet.getStudentGroup().getId(), true, env));
		
		// now delete the math student group altogether
		userGroupService.delete(dataSet.getMathStudentGroup().getId(), studentAuthData, env);
		assertFalse(userGroupService.isGroupInGroup(dataSet.getMathStudentGroup().getId(), dataSet.getStudentGroup().getId(), true, env));
		assertFalse(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getGeometryStudent1().getKID(), env));
		assertFalse(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getAlgebraStudent1().getKID(), env));
		assertFalse(sharingService.canViewRecord(youngPigeon.getKID(), dataSet.getMathStudent1().getKID(), env));
		
		// however, make sure users have not been deleted
		assertNotNull(userService.getUser(dataSet.getMathStudent1().getKID(), env));
	}

	private void testSimpleGroupSharing(UserGroupHierarchyDataSet ds, Type pigeonType, AuthData authData, EnvData env) throws KommetException
	{
		Record geometryStudent = dataService.save(TestDataCreator.getTestUser("geometry-student-001", "geometry-student-001@kommet.io", (Record)ds.getMathStudent1().getField("profile"), env), env);
		
		Record pigeon = new Record(pigeonType);
		pigeon.setField("name", "ddjkwewj");
		pigeon.setField("age", BigDecimal.valueOf(3));
		pigeon = dataService.save(pigeon, env);
		
		sharingService.shareRecordWithGroup(pigeon.getKID(), ds.getGeometryStudentGroup().getId(), false, false, "test", true, authData, env);
		
		assertFalse(sharingService.canViewRecord(pigeon.getKID(), geometryStudent.getKID(), env));
		
		// add user to group
		try
		{
			userGroupService.assignUserToGroup(geometryStudent.getKID(), ds.getGeometryStudentGroup().getId(), authData, env, true);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw e;
		}
		
		//assertTrue(sharingService.canViewRecord(pigeon2.getKID(), geometryStudent.getKID(), env));
		assertTrue(sharingService.canViewRecord(pigeon.getKID(), geometryStudent.getKID(), env));
		
		sharingService.unshareRecordWithAllGroups(pigeon.getKID(), authData, env);
		userGroupService.unassignUserFromGroup(geometryStudent.getKID(), ds.getGeometryStudentGroup().getId(), authData, env);
		
		assertFalse(sharingService.canViewRecord(pigeon.getKID(), geometryStudent.getKID(), env));
	}
}
