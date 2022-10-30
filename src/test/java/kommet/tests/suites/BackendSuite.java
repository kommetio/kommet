/**
 * Copyright 2022, Radosław Krawiec
 * Licensed under the GNU Affera General Public License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at https://www.gnu.org/licenses/agpl-3.0.en.html 
 */

package kommet.tests.suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;

import kommet.tests.AnyRecordTest;
import kommet.tests.AppTest;
import kommet.tests.AssociationTest;
import kommet.tests.BaseConversionTest;
import kommet.tests.ButtonTest;
import kommet.tests.CommentTest;
import kommet.tests.ConfigTest;
import kommet.tests.DataSourceFactoryTest;
import kommet.tests.DictionaryTest;
import kommet.tests.EventTest;
import kommet.tests.FieldManipulationTest;
import kommet.tests.GlobalSettingsTest;
import kommet.tests.I18nTest;
import kommet.tests.InverseCollectionTest;
import kommet.tests.PersistenceMappingTest;
import kommet.tests.KIDGeneratorTest;
import kommet.tests.RecordProxyTest;
import kommet.tests.ReminderTest;
import kommet.tests.SettingValueTest;
import kommet.tests.SystemContextTest;
import kommet.tests.SystemSettingsTest;
import kommet.tests.TaskTest;
import kommet.tests.TestingTest;
import kommet.tests.UserCascadeHierarchyTest;
import kommet.tests.UserGroupTest;
import kommet.tests.ViewResourceTest;
import kommet.tests.WebResourceTest;
import kommet.tests.actions.ActionTest;
import kommet.tests.actions.ParsedURLTest;
import kommet.tests.auth.AuthTest;
import kommet.tests.auth.AuthUtilTest;
import kommet.tests.auth.ProfileTest;
import kommet.tests.auth.SystemAdministratorTest;
import kommet.tests.basic.BasicSetupTest;
import kommet.tests.basic.UserTest;
import kommet.tests.bp.BusinessProcessTest;
import kommet.tests.dal.DALCriteriaBuilderTest;
import kommet.tests.dal.DALTest;
import kommet.tests.dal.DalUtilTest;
import kommet.tests.data.CriteriaQueryTest;
import kommet.tests.data.DataServiceTest;
import kommet.tests.data.FieldHistoryTest;
import kommet.tests.data.PropertyNullificationTest;
import kommet.tests.data.RecordAccessTypeTest;
import kommet.tests.data.RecordTest;
import kommet.tests.data.SharingTest;
import kommet.tests.data.TriggerTest;
import kommet.tests.data.TypeInsertTest;
import kommet.tests.data.ValidationRuleTest;
import kommet.tests.dataimport.CsvImportTest;
import kommet.tests.datatypes.AutoNumberTest;
import kommet.tests.datatypes.DateTimeTest;
import kommet.tests.datatypes.FormulaTest;
import kommet.tests.datatypes.MultienumerationTest;
import kommet.tests.datatypes.NumberDataTypeTest;
import kommet.tests.datatypes.TypeReferenceTest;
import kommet.tests.deployment.DeploymentTest;
import kommet.tests.deployment.LibraryDeploymentRollbackTest;
import kommet.tests.deployment.LibraryTest;
import kommet.tests.docs.DocTemplateTest;
import kommet.tests.emailing.EmailTest;
import kommet.tests.envs.EnvCreationTest;
import kommet.tests.envs.EnvServiceTest;
import kommet.tests.files.FileTest;
import kommet.tests.http.HttpTest;
import kommet.tests.keetle.LayoutTest;
import kommet.tests.keetle.ViewTest;
import kommet.tests.keetle.ViewUtilTest;
import kommet.tests.koll.GenericActionTest;
import kommet.tests.koll.KollParserTest;
import kommet.tests.koll.KollTest;
import kommet.tests.koll.RecordProxyGenerationTest;
import kommet.tests.koll.SharingRuleTest;
import kommet.tests.koll.compiler.KommetClassLoaderTest;
import kommet.tests.koll.compiler.KommetCompilerTest;
import kommet.tests.labels.TextLabelTest;
import kommet.tests.notifications.NotificationTest;
import kommet.tests.performance.QueryPerformanceTest;
import kommet.tests.persistence.DaoTest;
import kommet.tests.persistence.PersistenceTest;
import kommet.tests.rel.RELParserTest;
import kommet.tests.reports.ReportTest;
import kommet.tests.scheduler.SchedulerTest;
import kommet.tests.tags.ListColumnTest;
import kommet.tests.transactions.TransactionRollbackTest;
import kommet.tests.types.TypeCodeDeclarationTest;
import kommet.tests.types.TypeManipulationTest;
import kommet.tests.types.UniqueCheckTest;
import kommet.tests.utils.DataUtilTest;
import kommet.tests.utils.JSONUtilTest;
import kommet.tests.utils.MiscUtilsTest;
import kommet.tests.utils.NumberFormatUtilTest;
import kommet.tests.utils.ValidationUtilTest;
import kommet.tests.utils.VarInterpreterTest;
import kommet.tests.vendorapis.MsExcelApiTest;

@RunWith(Suite.class)
@ContextConfiguration("/test-app-context.xml")
@Rollback
@SuiteClasses({ AuthTest.class,
	AuthUtilTest.class,
	ProfileTest.class,
	BasicSetupTest.class,
	UserTest.class,
	DALCriteriaBuilderTest.class,
	DALTest.class,
	DalUtilTest.class,
	CriteriaQueryTest.class,
	DataServiceTest.class,
	FieldHistoryTest.class,
	PropertyNullificationTest.class,
	RecordTest.class,
	SharingTest.class,
	TriggerTest.class,
	DateTimeTest.class,
	FormulaTest.class,
	NumberDataTypeTest.class,
	TypeReferenceTest.class,
	DocTemplateTest.class,
	FileTest.class,
	ViewTest.class,
	ViewUtilTest.class,
	LayoutTest.class,
	KommetClassLoaderTest.class,
	KommetCompilerTest.class,
	KollParserTest.class,
	KollTest.class,
	RecordProxyGenerationTest.class,
	ActionTest.class,
	QueryPerformanceTest.class,
	DaoTest.class,
	PersistenceTest.class,
	SchedulerTest.class,
	ListColumnTest.class,
	TransactionRollbackTest.class,
	TypeManipulationTest.class,
	UniqueCheckTest.class,
	MiscUtilsTest.class,
	ValidationUtilTest.class,
	VarInterpreterTest.class,
	AssociationTest.class,
	BaseConversionTest.class,
	CommentTest.class,
	ConfigTest.class,
	DataSourceFactoryTest.class,
	EnvServiceTest.class,
	FieldManipulationTest.class,
	GlobalSettingsTest.class,
	I18nTest.class,
	InverseCollectionTest.class,
	KIDGeneratorTest.class,
	RecordProxyTest.class,
	PersistenceMappingTest.class,
	SystemContextTest.class,
	SystemSettingsTest.class,
	TextLabelTest.class,
	RELParserTest.class,
	ValidationUtilTest.class,
	ValidationRuleTest.class,
	EmailTest.class,
	NotificationTest.class,
	DeploymentTest.class,
	DataUtilTest.class,
	ReportTest.class,
	UserCascadeHierarchyTest.class,
	UserGroupTest.class,
	SettingValueTest.class,
	WebResourceTest.class,
	ViewResourceTest.class,
	NumberFormatUtilTest.class,
	// test disabled because multienum functionality is not fully implemented yet
	MultienumerationTest.class,
	MsExcelApiTest.class,
	CsvImportTest.class,
	AppTest.class,
	TaskTest.class,
	TypeInsertTest.class,
	//LibraryTest.class,
	//LibraryDeploymentRollbackTest.class,
	EnvCreationTest.class,
	SystemAdministratorTest.class,
	HttpTest.class,
	EventTest.class,
	JSONUtilTest.class,
	AnyRecordTest.class,
	RecordAccessTypeTest.class,
	TypeCodeDeclarationTest.class,
	SharingRuleTest.class,
	BusinessProcessTest.class,
	AutoNumberTest.class,
	GenericActionTest.class,
	ButtonTest.class,
	ReminderTest.class,
	DictionaryTest.class,
	ParsedURLTest.class,
	TestingTest.class
})
public class BackendSuite
{

}
